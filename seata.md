# Seata 分布式事务说明文档

> Seata（Simple Extensible Autonomous Transaction Architecture）是阿里巴巴与蚂蚁金服于 2019 年共同开源的分布式事务解决方案，支持 AT、TCC、Saga、XA 四种模式。

---

## 一、核心架构（TC / TM / RM）

Seata 由三个核心角色构成：

| 角色 | 全称 | 部署位置 | 职责 |
|------|------|---------|------|
| **TC** | Transaction Coordinator（事务协调器）| 独立 Server 端 | 维护全局/分支事务状态，驱动全局提交或回滚 |
| **TM** | Transaction Manager（事务管理器）| 业务应用（Client 端）| 定义全局事务范围，向 TC 发起开启/提交/回滚指令 |
| **RM** | Resource Manager（资源管理器）| 业务应用（Client 端）| 管理本地资源（数据库），向 TC 注册分支事务并汇报状态 |

### 1.1 整体执行流程

```
① TM 向 TC 申请开启全局事务，TC 生成全局唯一 XID
② XID 在微服务调用链中传播（HTTP Header: TX_XID）
③ 各分支服务 RM 向 TC 注册分支事务，关联 XID
④ 各分支执行本地 SQL，生成 undo_log（AT 模式）
⑤ TM 向 TC 发起全局提交或回滚
⑥ TC 通知所有 RM 执行二阶段提交或回滚
```

---

## 二、AT 模式（推荐，无侵入）

AT 模式（Automatic Transaction）是 Seata 默认模式，**对业务代码零侵入**，基于本地 ACID 事务自动生成二阶段逻辑。

### 2.1 工作原理

#### 一阶段（Try）
Seata 代理数据源，拦截业务 SQL：
1. 解析 SQL 语义，找到待更新的数据行
2. 执行 SQL **前**保存 **before image**（快照）
3. 执行业务 SQL
4. 执行 SQL **后**保存 **after image**（快照）
5. 生成行锁，将 before/after image 写入 `undo_log` 表
6. `undo_log` 与业务 SQL **在同一本地事务内提交**，保证原子性

#### 二阶段-提交
全局事务所有分支均成功：
- TC 通知各 RM **异步删除** `undo_log` 记录
- 释放全局行锁

#### 二阶段-回滚
任意分支失败，全局事务回滚：
- TC 通知各 RM 读取 `undo_log`
- 校验 after image 与当前数据是否一致（防脏写）
  - 一致 → 用 before image 生成反向补偿 SQL，还原数据
  - 不一致 → 出现脏写，需**人工介入**处理
- 删除 `undo_log` 记录，释放行锁

### 2.2 undo_log 表结构（每个业务库都需要建）

```sql
-- 注意：Seata 0.3.0+ 增加唯一索引 ux_undo_log
CREATE TABLE `undo_log`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `branch_id`     bigint(20)   NOT NULL,
    `xid`           varchar(100) NOT NULL,
    `context`       varchar(128) NOT NULL,
    `rollback_info` longblob     NOT NULL,
    `log_status`    int(11)      NOT NULL,
    `log_created`   datetime     NOT NULL,
    `log_modified`  datetime     NOT NULL,
    `ext`           varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8;
```

| 字段 | 说明 |
|------|------|
| `xid` | 全局事务 ID |
| `branch_id` | 分支事务 ID |
| `rollback_info` | **核心字段**，存储 before image + after image（JSON，longblob）|
| `log_status` | `0`=正常；`1`=防悬挂标记（全局回滚先到时写入，阻止后续 Try 写入）|
| `context` | 序列化方式，如 `jackson` |

### 2.3 代码示例

```java
// 事务发起方（TM）：添加 @GlobalTransactional
@Service
public class BusinessServiceImpl {

    @Autowired
    private StorageService storageService;
    @Autowired
    private OrderService orderService;

    @GlobalTransactional(name = "purchase-tx", rollbackFor = Exception.class)
    public void purchase(String userId, String commodityCode, int count) {
        storageService.deduct(commodityCode, count);   // 扣库存
        orderService.create(userId, commodityCode, count); // 创建订单（内部有异常触发全局回滚）
    }
}

// 分支参与方（RM）：普通 @Transactional 即可，Seata 自动代理数据源
@Service
public class StorageServiceImpl {

    @Transactional(rollbackFor = Exception.class)
    public void deduct(String commodityCode, int count) {
        storageTblMapper.deduct(commodityCode, count);
    }
}
```

### 2.4 @GlobalTransactional 属性说明

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | 方法全限定名 | 全局事务名称 |
| `rollbackFor` | Class[] | `RuntimeException` | 触发全局回滚的异常类型 |
| `noRollbackFor` | Class[] | 无 | 不触发回滚的异常类型 |
| `timeoutMills` | int | 60000 | 全局事务超时时间（毫秒）|
| `propagation` | Propagation | `REQUIRED` | 事务传播行为 |

### 2.5 AT 模式的限制

- **仅支持关系型数据库**（MySQL、Oracle、PostgreSQL 等）
- 存在**全局行锁**，高并发写同一行时性能下降
- 不支持无 `undo_log` 表的数据库

---

## 三、TCC 模式（高性能，代码侵入）

TCC（Try-Confirm-Cancel）是服务化的两阶段提交协议，需要业务开发者**手动实现** Try、Confirm、Cancel 三个方法，**无全局行锁，性能优于 AT 模式**。

### 3.1 三阶段说明

| 阶段 | 方法 | 说明 |
|------|------|------|
| **一阶段** | `Try` | 资源检测与预留（如冻结资金，不真正扣款）|
| **二阶段-提交** | `Confirm` | 使用 Try 预留的资源真正执行业务 |
| **二阶段-回滚** | `Cancel` | 释放 Try 预留的资源 |

**核心原则：Try 成功，Confirm 必须能成功**（Confirm 不允许失败）

### 3.2 典型场景：账户扣款

```
Try:     检查余额 ≥ 30元 → 冻结 30元（可用余额 100 → 70，冻结金额 0 → 30）
Confirm: 从冻结金额中真正扣除（冻结金额 30 → 0，已扣款 0 → 30）
Cancel:  释放冻结金额（冻结金额 30 → 0，可用余额 70 → 100）
```

### 3.3 代码示例

```java
// 定义 TCC 接口
@LocalTCC
public interface AccountTccService {

    @TwoPhaseBusinessAction(
        name = "debit",
        commitMethod = "confirm",
        rollbackMethod = "cancel",
        useTCCFence = true  // 开启 Seata 1.5.1+ 防悬挂/幂等/空回滚
    )
    void prepare(@BusinessActionContextParameter(paramName = "userId") String userId,
                 @BusinessActionContextParameter(paramName = "money") int money);

    boolean confirm(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}

// 实现类
@Service
public class AccountTccServiceImpl implements AccountTccService {

    @Override
    @Transactional
    public void prepare(String userId, int money) {
        // 检查余额并冻结资金
        accountMapper.freeze(userId, money);
    }

    @Override
    @Transactional
    public boolean confirm(BusinessActionContext context) {
        String userId = context.getActionContext("userId").toString();
        int money = (int) context.getActionContext("money");
        // 真正扣款：从冻结金额中扣除
        accountMapper.debit(userId, money);
        return true;
    }

    @Override
    @Transactional
    public boolean cancel(BusinessActionContext context) {
        String userId = context.getActionContext("userId").toString();
        int money = (int) context.getActionContext("money");
        // 解冻资金
        accountMapper.unfreeze(userId, money);
        return true;
    }
}
```

### 3.4 三大经典问题及解决方案

| 问题 | 场景 | 解决方案 |
|------|------|---------|
| **空回滚** | Try 未执行（网络超时），Cancel 被调用 | Cancel 中检查是否有 Try 记录，无则直接返回成功 |
| **悬挂** | Cancel 比 Try 先到达，Try 后续又到达 | Try 中检查是否已回滚过（已有 Cancel 记录则不执行）|
| **幂等** | Confirm/Cancel 因网络重试被多次调用 | 检查事务状态，已完成则直接返回成功 |

> **推荐方案**：使用 Seata 1.5.1+ 的 `useTCCFence = true`，框架自动通过 `tcc_fence_log` 表解决以上三个问题，无需手动处理。

#### tcc_fence_log 表（使用 useTCCFence 时需建）

```sql
CREATE TABLE `tcc_fence_log`
(
    `xid`           varchar(128) NOT NULL,
    `branch_id`     bigint       NOT NULL,
    `action_name`   varchar(64)  NOT NULL,
    `status`        tinyint      NOT NULL,
    `gmt_create`    datetime(3)  NOT NULL,
    `gmt_modified`  datetime(3)  NOT NULL,
    PRIMARY KEY (`xid`, `branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

---

## 四、Saga 模式（长事务，最终一致）

Saga 模式通过**补偿机制**实现分布式事务，适合**长流程业务**（如跨系统调用外部接口等无法回滚的场景）。

### 4.1 工作原理

```
正向流程：T1 → T2 → T3 → ... → Tn（全部成功则提交）
补偿流程：Cn → ... → C2 → C1（任意 Ti 失败，逆序执行补偿）
```

每个参与者需实现：
- **正向操作 Ti**：执行业务逻辑
- **补偿操作 Ci**：撤销 Ti 对数据的影响

### 4.2 两种实现方式

#### 状态机编排（官方推荐）
通过 JSON 配置文件定义状态机，描述事务流程和补偿流程：

```json
{
  "Name": "purchaseSaga",
  "Comment": "购买业务Saga",
  "StartState": "DeductStorage",
  "Version": "0.0.1",
  "States": {
    "DeductStorage": {
      "Type": "ServiceTask",
      "ServiceName": "storageService",
      "ServiceMethod": "deduct",
      "CompensateState": "CompensateDeductStorage",
      "Next": "CreateOrder"
    },
    "CreateOrder": {
      "Type": "ServiceTask",
      "ServiceName": "orderService",
      "ServiceMethod": "create",
      "CompensateState": "CompensateCreateOrder",
      "Next": "Succeed"
    },
    "CompensateDeductStorage": {
      "Type": "ServiceTask",
      "ServiceName": "storageService",
      "ServiceMethod": "compensateDeduct"
    },
    "CompensateCreateOrder": {
      "Type": "ServiceTask",
      "ServiceName": "orderService",
      "ServiceMethod": "compensateCreate"
    },
    "Succeed": { "Type": "Succeed" },
    "Fail":    { "Type": "Fail" }
  }
}
```

#### 注解驱动（简单场景）
业务代码自行编排，在 catch 中调用补偿服务。

### 4.3 Saga 模式注意事项

- **无全局行锁**，性能最高，但只保证**最终一致性**
- 补偿操作必须保证**幂等**
- 不适合要求强一致性的金融核心场景

---

## 五、XA 模式（强一致，性能低）

XA 模式基于数据库原生 XA 协议，是**强一致性**方案，适用于对一致性要求极高且对性能要求不高的场景。

### 5.1 工作原理

```
一阶段：TC 通知所有 RM 执行 XA Start → 执行 SQL → XA End → XA Prepare（资源锁定）
二阶段-提交：TC 通知所有 RM 执行 XA Commit
二阶段-回滚：TC 通知所有 RM 执行 XA Rollback
```

> 一阶段结束后，数据库资源一直**处于锁定状态**直到二阶段完成，锁持有时间长，并发性能差。

### 5.2 代码示例

```java
// 数据源代理改为 XA 模式
@Bean
@Primary
public DataSource dataSource(DruidDataSource druidDataSource) {
    return new DataSourceProxyXA(druidDataSource);
}
```

或通过配置开启：

```yaml
seata:
  data-source-proxy-mode: XA
```

---

## 六、四种模式对比

| 模式 | 一致性 | 侵入性 | 性能 | 适用场景 |
|------|--------|--------|------|---------|
| **AT** | 最终一致（默认隔离 Read Uncommitted，加锁后 Read Committed）| 无侵入 | 中 | 常规关系型数据库业务，追求简单接入 |
| **TCC** | 最终一致 | 高（需实现 Try/Confirm/Cancel）| 高 | 高并发、核心金融场景、需要精细控制 |
| **Saga** | 最终一致 | 中（需实现补偿逻辑）| 最高 | 长事务、跨系统调用、不可逆操作 |
| **XA** | 强一致 | 无侵入 | 低 | 强一致性要求、并发量小的场景 |

---

## 七、Server 端部署

### 7.1 存储模式

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| `file` | 单机模式，数据存内存+本地文件，性能高 | 开发/测试 |
| `db` | 高可用模式，数据存数据库，多节点共享 | **生产推荐** |
| `redis` | Seata 1.3+，性能高，存在数据丢失风险 | 高性能场景 |
| `raft` | Seata 2.0+，内置 Raft 集群，无需外部存储 | 生产推荐（2.x+）|

### 7.2 db 模式 Server 端建表（TC 服务器数据库）

```sql
-- 全局事务表
CREATE TABLE `global_table` (
    `xid`                       VARCHAR(128) NOT NULL,
    `transaction_id`            BIGINT,
    `status`                    TINYINT      NOT NULL,
    `application_id`            VARCHAR(32),
    `transaction_service_group` VARCHAR(32),
    `transaction_name`          VARCHAR(128),
    `timeout`                   INT,
    `begin_time`                BIGINT,
    `application_data`          VARCHAR(2000),
    `gmt_create`                DATETIME,
    `gmt_modified`              DATETIME,
    PRIMARY KEY (`xid`),
    KEY `idx_status_gmt_modified` (`status`, `gmt_modified`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 分支事务表
CREATE TABLE `branch_table` (
    `branch_id`         BIGINT       NOT NULL,
    `xid`               VARCHAR(128) NOT NULL,
    `transaction_id`    BIGINT,
    `resource_group_id` VARCHAR(32),
    `resource_id`       VARCHAR(256),
    `branch_type`       VARCHAR(8),
    `status`            TINYINT,
    `client_id`         VARCHAR(64),
    `application_data`  VARCHAR(2000),
    `gmt_create`        DATETIME(6),
    `gmt_modified`      DATETIME(6),
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 全局锁表
CREATE TABLE `lock_table` (
    `row_key`        VARCHAR(128) NOT NULL,
    `xid`            VARCHAR(128),
    `transaction_id` BIGINT,
    `branch_id`      BIGINT       NOT NULL,
    `resource_id`    VARCHAR(256),
    `table_name`     VARCHAR(32),
    `pk`             VARCHAR(36),
    `status`         TINYINT      NOT NULL DEFAULT 0,
    `gmt_create`     DATETIME,
    `gmt_modified`   DATETIME,
    PRIMARY KEY (`row_key`),
    KEY `idx_status` (`status`),
    KEY `idx_branch_id` (`branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

### 7.3 Server 端 application.yml（Nacos 注册 + DB 存储）

```yaml
server:
  port: 7091

spring:
  application:
    name: seata-server

seata:
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      namespace: ""
      group: SEATA_GROUP
      username: nacos
      password: nacos
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      username: nacos
      password: nacos
  store:
    mode: db
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true&rewriteBatchedStatements=true
      user: root
      password: root
      min-conn: 10
      max-conn: 100
      global-table: global_table
      branch-table: branch_table
      lock-table: lock_table
      distributed-lock-table: distributed_lock
```

### 7.4 启动命令

```bash
# Linux/macOS
seata-server.sh -h 127.0.0.1 -p 7091 -m db

# Windows
seata-server.bat -h 127.0.0.1 -p 7091 -m db
```

---

## 八、Client 端配置（application.yml）

```yaml
spring:
  application:
    name: seata-order
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/order_db?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

seata:
  # 事务分组名称，需与 Seata Server 配置一致
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default  # 映射到 Seata Server 的集群名称
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      namespace: ""
      group: SEATA_GROUP
      application: seata-server
      username: nacos
      password: nacos
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      namespace: ""
      group: SEATA_GROUP
      username: nacos
      password: nacos
  # AT 模式默认开启数据源自动代理，XA 模式需指定
  # data-source-proxy-mode: XA
```

### 8.1 关键依赖

```xml
<!-- Spring Cloud Alibaba Seata（推荐方式，内置 XID 传播）-->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>

<!-- 若需单独控制 Seata 版本 -->
<dependency>
    <groupId>org.apache.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>2.x.x</version>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.apache.seata</groupId>
            <artifactId>seata-spring-boot-starter</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

---

## 九、常见问题排查

### 9.1 脏写导致回滚失败

**现象**：`after-image check failed`，事务无法自动回滚

**原因**：Seata 记录 after image 后，另一事务绕过 Seata 直接修改了同一行数据

**解决**：
1. 确保所有写操作都经过 Seata 代理数据源
2. 开启全局锁（会降低并发性能）

### 9.2 XID 未传播，分支不受全局事务管控

**现象**：某服务异常，其他服务数据未回滚

**原因**：XID 未通过 HTTP Header（`TX_XID`）传递到下游服务

**解决**：
- 使用 `spring-cloud-starter-alibaba-seata`（自动拦截 Feign 请求传递 XID）
- 手动传递：`RootContext.bind(xid)`

### 9.3 事务分组不匹配

**现象**：启动正常但事务不生效，日志显示 `no available service`

**原因**：Client 的 `tx-service-group` 与 Server 端集群名称不匹配

**解决**：检查 `seata.service.vgroup-mapping.{tx-service-group}` 的值是否与 Server 注册的集群名一致

### 9.4 数据源代理未生效，undo_log 无数据

**现象**：undo_log 表始终为空，回滚后数据未还原

**原因**：数据源未被 Seata 代理（可能手动配置了数据源覆盖了自动代理）

**解决**：
- 使用自动代理：确保 `seata.enable-auto-data-source-proxy=true`（默认值）
- 不要同时使用自动和手动代理

### 9.5 TCC 悬挂/空回滚问题

**现象**：TCC 模式下部分数据不一致

**解决**：升级到 Seata 1.5.1+，在 `@TwoPhaseBusinessAction` 中设置 `useTCCFence = true`，并在各业务库创建 `tcc_fence_log` 表

---

## 十、最佳实践

### 10.1 事务设计原则

1. **最短事务原则**：`@GlobalTransactional` 方法内只放必要的 DB 操作，避免网络调用
2. **幂等设计**：所有分支操作需支持幂等，防止重试导致数据重复
3. **合理超时**：根据链路长度设置 `timeoutMills`，默认 60s 可根据业务缩短
4. **避免大事务**：分布式事务链路越长，锁持有时间越长，并发性能越差
5. **降级兜底**：核心事务链路需有熔断降级机制（结合 Sentinel）

### 10.2 模式选型指南

```
是否使用关系型数据库？
  ├─ 是 → 是否要求强一致性？
  │         ├─ 是 → XA 模式（性能差，谨慎使用）
  │         └─ 否 → 是否高并发写热点数据？
  │                   ├─ 是 → TCC 模式（无全局锁，性能高）
  │                   └─ 否 → AT 模式（无侵入，首选）
  └─ 否 → 是否是长流程 / 外部接口？
            ├─ 是 → Saga 模式（补偿机制，最终一致）
            └─ 否 → TCC 模式
```

### 10.3 生产环境注意事项

1. Seata Server 必须使用 **db 或 raft 模式**，file 模式不保证高可用
2. `undo_log` 表需定期清理，防止数据膨胀（Seata 会自动清理已完成的记录）
3. 事务超时时间需大于整个调用链最长耗时，避免误回滚
4. 建议开启 Seata 监控（结合 Prometheus + Grafana）

---

## 相关链接

- Apache Seata 官方文档：https://seata.apache.org/zh-cn/
- GitHub：https://github.com/apache/incubator-seata
- Spring Cloud Alibaba 文档：https://sca.aliyun.com/
- Seata 部署指南：https://seata.apache.org/zh-cn/docs/ops/deploy-guide-beginner/
- Seata 参数配置：https://seata.apache.org/zh-cn/docs/user/configurations
