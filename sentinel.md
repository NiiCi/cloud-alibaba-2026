# Sentinel 规则说明文档

## 一、流控规则（Flow Control Rules）

流控规则用于控制流量，防止系统被瞬时的流量高峰冲垮。

### 1.1 阈值类型

- **QPS（每秒查询数）**：当每秒请求数超过设定阈值时触发限流
- **线程数**：当并发线程数超过阈值时触发限流

### 1.2 流控模式

#### 直接模式（Direct）
- 直接对当前资源进行限流
- 达到阈值后，直接拒绝请求

#### 关联模式（Relate）
- 当关联资源达到阈值时，限流当前资源
- 适用场景：读写分离，当写操作频繁时限制读操作

#### 链路模式（Chain）
- 只记录指定链路上的流量
- 可对同一资源的不同调用链路分别限流

### 1.3 流控效果

#### 快速失败（Fail Fast）
- 直接抛出异常
- 默认方式，适合对响应时间敏感的场景

#### Warm Up（预热/冷启动）
- 根据 coldFactor（默认3）的值，从阈值/coldFactor开始逐渐增加到阈值
- 适用场景：秒杀系统、防止冷启动时大量请求压垮系统
- 预热时间：系统达到最大阈值所需时间

#### 排队等待（Queue）
- 匀速排队，让请求以均匀的速度通过
- 适用场景：消息队列削峰填谷
- 超时时间：请求等待的最大时间，超时则直接拒绝

### 1.4 Nacos JSON 配置示例

```json
[
  {
    "resource": "/order/flow",
    "limitApp": "default",
    "grade": 1,
    "count": 5,
    "strategy": 0,
    "controlBehavior": 0,
    "warmUpPeriodSec": 10,
    "maxQueueingTimeMs": 500,
    "clusterMode": false
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| resource | String | 资源名称，对应 `@SentinelResource` 的 value 或请求路径 |
| limitApp | String | 针对来源，`default` 表示不区分来源 |
| grade | int | 阈值类型：`0`=线程数，`1`=QPS |
| count | double | 限流阈值 |
| strategy | int | 流控模式：`0`=直接，`1`=关联，`2`=链路 |
| refResource | String | 关联资源或入口资源，strategy=1 或 2 时填写 |
| controlBehavior | int | 流控效果：`0`=快速失败，`1`=Warm Up，`2`=排队等待 |
| warmUpPeriodSec | int | 预热时长（秒），controlBehavior=1 时生效 |
| maxQueueingTimeMs | int | 最大排队等待时间（毫秒），controlBehavior=2 时生效 |
| clusterMode | boolean | 是否开启集群限流 |

## 二、降级规则（Degrade Rules）

降级规则用于在系统不稳定时，对调用进行限制，让系统尽快恢复。

### 2.1 降级策略

#### 慢调用比例（Slow Request Ratio）
- **最大RT（响应时间）**：请求响应时间超过该值被认为是慢调用
- **比例阈值**：慢调用比例超过该值触发熔断（0.0-1.0）
- **最小请求数**：触发熔断的最小请求数
- **熔断时长**：熔断持续时间，时间过后进入半开状态
- **统计时长**：统计的时间窗口长度

#### 异常比例（Error Ratio）
- **比例阈值**：异常比例超过该值触发熔断（0.0-1.0）
- **最小请求数**：触发熔断的最小请求数
- **熔断时长**：熔断持续时间
- **统计时长**：统计的时间窗口长度

#### 异常数（Error Count）
- **异常数阈值**：统计时长内异常数超过该值触发熔断
- **最小请求数**：触发熔断的最小请求数
- **熔断时长**：熔断持续时间
- **统计时长**：统计的时间窗口长度（1-120分钟）

### 2.2 熔断状态

1. **关闭（Closed）**：正常状态，不进行熔断
2. **开启（Open）**：触发熔断，所有请求直接失败
3. **半开（Half-Open）**：熔断时长过后进入半开状态，允许一次请求探测
   - 探测成功：关闭熔断
   - 探测失败：重新开启熔断

### 2.3 Nacos JSON 配置示例

```json
[
  {
    "resource": "/order/degrade",
    "grade": 0,
    "count": 500,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000,
    "slowRatioThreshold": 0.5
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| resource | String | 资源名称 |
| grade | int | 降级策略：`0`=慢调用比例，`1`=异常比例，`2`=异常数 |
| count | double | grade=0 时为最大RT（毫秒）；grade=1 时为异常比例阈值（0.0-1.0）；grade=2 时为异常数阈值 |
| timeWindow | int | 熔断时长（秒），熔断后经过此时间进入半开状态 |
| minRequestAmount | int | 触发熔断的最小请求数，请求数低于此值时不触发 |
| statIntervalMs | int | 统计时长（毫秒），默认 1000ms |
| slowRatioThreshold | double | 慢调用比例阈值（0.0-1.0），grade=0 时生效 |

## 三、热点参数限流（Hot Param Flow Rules）

热点参数限流针对热点数据进行限流，比如对某个商品ID限流。

### 3.1 配置参数

- **参数索引**：第几个参数（从0开始）
- **单机阈值**：该参数的QPS阈值
- **统计窗口时长**：统计的时间窗口

### 3.2 参数例外项

为特定参数值设置独立的限流阈值。

- **参数类型**：基本类型（int、long、String等）
- **参数值**：具体的参数值
- **限流阈值**：该参数值的独立阈值

### 3.3 适用场景

- 热门商品限流
- 特定用户限流
- 热点数据访问控制

### 3.4 Nacos JSON 配置示例

```json
[
  {
    "resource": "hot",
    "paramIdx": 0,
    "grade": 1,
    "count": 2,
    "durationInSec": 1,
    "paramFlowItemList": [
      {
        "classType": "int",
        "object": "1001",
        "count": 10
      }
    ]
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| resource | String | 资源名称，必须与 `@SentinelResource` 的 value 一致 |
| paramIdx | int | 参数索引，从 0 开始，表示对方法的第几个参数限流 |
| grade | int | 阈值类型，热点参数仅支持 `1`=QPS |
| count | int | 默认单机 QPS 阈值 |
| durationInSec | int | 统计窗口时长（秒） |
| paramFlowItemList | array | 参数例外项列表 |
| paramFlowItemList[].classType | String | 参数类型，如 `int`、`long`、`String` |
| paramFlowItemList[].object | String | 特定参数值 |
| paramFlowItemList[].count | int | 该参数值的独立限流阈值 |

## 四、系统规则（System Rules）

系统规则从整体维度对应用入口流量进行控制，防止系统负载过高。

### 4.1 系统指标

#### Load（系统负载）
- 当系统 Load1 超过阈值，且并发线程数超过系统容量时触发
- 仅对 Linux/Unix 系统生效
- 建议值：CPU核数 × 2.5

#### CPU使用率
- 当系统 CPU 使用率超过阈值时触发（0.0-1.0）
- 建议值：0.7-0.8

#### 平均RT（响应时间）
- 所有入口流量的平均 RT 达到阈值时触发
- 单位：毫秒

#### 并发线程数
- 所有入口流量的并发线程数达到阈值时触发

#### 入口QPS
- 所有入口流量的 QPS 达到阈值时触发

### 4.2 注意事项

- 系统规则是全局规则，对所有入口资源生效
- 建议谨慎使用，避免影响正常业务
- 应与应用级别的流控规则配合使用

### 4.3 Nacos JSON 配置示例

```json
[
  {
    "highestSystemLoad": -1,
    "highestCpuUsage": 0.8,
    "qps": 100,
    "avgRt": -1,
    "maxThread": -1
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| highestSystemLoad | double | 系统 Load1 阈值，`-1` 表示不启用；仅 Linux/Unix 生效，建议值：CPU核数 × 2.5 |
| highestCpuUsage | double | CPU 使用率阈值（0.0-1.0），`-1` 表示不启用，建议值：0.7-0.8 |
| qps | double | 入口总 QPS 阈值，`-1` 表示不启用 |
| avgRt | double | 所有入口资源的平均 RT 阈值（毫秒），`-1` 表示不启用 |
| maxThread | long | 入口并发线程数阈值，`-1` 表示不启用 |

## 五、授权规则（Authority Rules）

授权规则用于控制调用方的访问权限。

### 5.1 流控应用

- **流控应用名**：调用方的应用名称
- 通过 `RequestOriginParser` 接口解析请求来源

### 5.2 授权类型

#### 白名单（White）
- 只允许名单中的调用方访问
- 其他调用方直接拒绝

#### 黑名单（Black）
- 拒绝名单中的调用方访问
- 其他调用方正常通过

### 5.3 Nacos JSON 配置示例

```json
[
  {
    "resource": "/order/auth",
    "limitApp": "appA,appB",
    "strategy": 0
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| resource | String | 资源名称 |
| limitApp | String | 授权的应用名称，多个用英文逗号分隔 |
| strategy | int | 授权类型：`0`=白名单（只允许 limitApp 中的应用访问），`1`=黑名单（拒绝 limitApp 中的应用访问） |

### 5.4 配置方式

- 多个应用名用逗号分隔
- 需要实现 `RequestOriginParser` 接口来解析请求来源

```java
@Component
public class CustomOriginParser implements RequestOriginParser {
    @Override
    public String parseOrigin(HttpServletRequest request) {
        // 从请求头或参数中获取来源标识
        return request.getHeader("origin");
    }
}
```

## 六、网关流控规则（Gateway Flow Rules）

针对 Spring Cloud Gateway 的流控规则。

### 6.1 路由维度

- 基于路由ID进行限流
- 可对整个路由的流量进行控制

### 6.2 API分组维度

- 自定义API分组
- 可将多个API归为一组进行统一限流

### 6.3 参数限流

- **URL参数限流**：根据URL参数值限流
- **Header限流**：根据请求头限流
- **Cookie限流**：根据Cookie值限流
- **客户端IP限流**：根据来源IP限流

## 七、集群流控

集群流控用于在集群环境下进行全局流控。

### 7.1 工作模式

#### Token Client
- 请求 Token Server 获取令牌
- 根据令牌决定是否通过请求

#### Token Server
- 独立部署的令牌服务
- 统计整个集群的调用总量

### 7.2 配置方式

- **单机阈值模式**：阈值 = 集群阈值 / 机器数
- **总体阈值模式**：直接设置整个集群的阈值

### 7.3 适用场景

- 微服务集群环境
- 需要精确控制整体流量
- 防止单机限流导致流量分配不均

## 八、规则持久化

### 8.1 持久化方式

#### 文件持久化
- 将规则持久化到本地文件
- 简单但不推荐生产使用

#### Nacos持久化
- 推荐方式
- 规则存储在 Nacos 配置中心
- 支持动态更新

#### 数据库持久化
- 将规则存储到数据库
- 需要自行实现数据源接口

#### Apollo持久化
- 规则存储在 Apollo 配置中心
- 支持动态更新

### 8.2 Nacos持久化配置示例

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        # 流控规则
        flow:
          nacos:
            server-addr: localhost:8848
            data-id: ${spring.application.name}-flow-rules
            group-id: SENTINEL_GROUP
            rule-type: flow
        # 降级规则
        degrade:
          nacos:
            server-addr: localhost:8848
            data-id: ${spring.application.name}-degrade-rules
            group-id: SENTINEL_GROUP
            rule-type: degrade
```

## 九、最佳实践

### 9.1 规则设计原则

1. **分层限流**：网关层、服务层、方法层多层限流
2. **降级优先**：优先配置降级规则，保护核心服务
3. **合理阈值**：根据压测结果设置阈值，留出安全余量
4. **监控告警**：配置监控和告警，及时发现问题

### 9.2 常见场景配置

#### 秒杀场景
- 使用 Warm Up 预热
- 配置热点参数限流
- 设置排队等待

#### 服务保护
- 配置慢调用比例降级
- 设置异常比例降级
- 使用系统规则保护整体

#### 接口限流
- 直接模式 + 快速失败
- 根据QPS设置阈值
- 配置友好的降级处理

### 9.3 注意事项

1. 规则不要过于复杂，影响性能
2. 合理设置统计时长和熔断时长
3. 降级后要有合理的fallback逻辑
4. 生产环境必须配置规则持久化
5. 定期review和调整规则配置

## 十、监控与Dashboard

### 10.1 Dashboard功能

- 实时监控：查看实时流量、QPS、响应时间
- 规则管理：动态配置各类规则
- 机器管理：查看接入的应用列表
- 簇点链路：查看资源调用链路

### 10.2 监控指标

- **通过QPS**：成功通过的请求数
- **拒绝QPS**：被限流拒绝的请求数
- **异常QPS**：发生异常的请求数
- **平均RT**：平均响应时间
- **并发线程数**：当前并发线程数

---

## 相关链接

- Sentinel 官方文档：https://sentinelguard.io/
- GitHub：https://github.com/alibaba/Sentinel
