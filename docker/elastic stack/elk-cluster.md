# ELK 集群部署文档

> 环境：macOS M4（ARM64）+ Docker Compose
> 版本：Elasticsearch / Kibana / Logstash 8.11.4

---

## 一、架构概述

```
┌─────────────────────────────────────────────────┐
│                  elk-network                     │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ es-node-1│  │ es-node-2│  │ es-node-3│       │
│  │ :9200    │  │ :9201    │  │ :9202    │       │
│  │ :9300    │  │ :9301    │  │ :9302    │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
│       └─────────────┴─────────────┘              │
│              Transport SSL 互联                   │
│                                                  │
│  ┌──────────┐       ┌──────────┐                 │
│  │  Kibana  │       │ Logstash │                 │
│  │  :5601   │       │ :5044    │                 │
│  └──────────┘       └──────────┘                 │
└─────────────────────────────────────────────────┘
```

| 服务 | 容器名 | 对外端口 | 说明 |
|------|--------|----------|------|
| Elasticsearch Node 1 | es-node-1 | 9200 / 9300 | HTTP API / 集群通信 |
| Elasticsearch Node 2 | es-node-2 | 9201 / 9301 | HTTP API / 集群通信 |
| Elasticsearch Node 3 | es-node-3 | 9202 / 9302 | HTTP API / 集群通信 |
| Kibana | kibana | 5601 | Web 控制台 |
| Logstash | logstash | 5044 / 9600 | 数据管道 / 监控 API |

---

## 二、目录结构

```
docker/elastic stack/
├── docker-compose.yml
├── elasticsearch/
│   ├── config/
│   │   └── elasticsearch.yml       # 共享配置（CORS 等）
│   ├── certs/                      # TLS 证书目录（需手动生成）
│   │   ├── instances.yml           # 证书节点定义文件
│   │   ├── ca/
│   │   │   ├── ca.crt
│   │   │   └── ca.key
│   │   ├── es-node-1/
│   │   │   ├── es-node-1.crt
│   │   │   └── es-node-1.key
│   │   ├── es-node-2/
│   │   │   ├── es-node-2.crt
│   │   │   └── es-node-2.key
│   │   └── es-node-3/
│   │       ├── es-node-3.crt
│   │       └── es-node-3.key
│   ├── node1/data、node1/logs
│   ├── node2/data、node2/logs
│   └── node3/data、node3/logs
├── kibana/config/
│   └── kibana.yml
└── logstash/
    ├── config/logstash.yml
    └── pipeline/logstash.conf
```

---

## 三、M4 芯片适配说明

macOS M4（ARM64）运行 Elasticsearch 需禁用 SVE 指令集，否则 JVM 启动时报 `SIGILL` 崩溃。

所有 ES / Logstash 容器必须设置：

```yaml
- _JAVA_OPTIONS=-XX:UseSVE=0
```

> **注意**：必须使用 `_JAVA_OPTIONS`，ES 会忽略 `JAVA_TOOL_OPTIONS`。

---

## 四、部署步骤

### 4.1 创建证书目录

```bash
cd "/Users/niici/local/xjyc/cloud-alibaba-2026/docker/elastic stack"
mkdir -p elasticsearch/certs
```

### 4.2 创建节点证书定义文件

新建 `elasticsearch/certs/instances.yml`：

```yaml
instances:
  - name: es-node-1
    dns:
      - es-node-1
      - localhost
    ip:
      - 127.0.0.1
  - name: es-node-2
    dns:
      - es-node-2
      - localhost
    ip:
      - 127.0.0.1
  - name: es-node-3
    dns:
      - es-node-3
      - localhost
    ip:
      - 127.0.0.1
```

### 4.3 生成 TLS 证书

> 使用 ES 容器内的 `elasticsearch-certutil` 工具生成证书，M4 必须带 `_JAVA_OPTIONS`。

```bash
cd "/Users/niici/local/xjyc/cloud-alibaba-2026/docker/elastic stack"

docker run --rm \
  -e _JAVA_OPTIONS="-XX:UseSVE=0" \
  -v "$(pwd)/elasticsearch/certs:/certs" \
  elasticsearch:8.11.4 \
  bash -c "
    elasticsearch-certutil ca --silent --pem --out /certs/ca.zip && \
    unzip /certs/ca.zip -d /certs && \
    elasticsearch-certutil cert --silent --pem \
      --ca-cert /certs/ca/ca.crt \
      --ca-key /certs/ca/ca.key \
      --in /certs/instances.yml \
      --out /certs/certs.zip && \
    unzip /certs/certs.zip -d /certs && \
    chmod -R 755 /certs
  "
```

生成后证书结构如下：

```
certs/
  ca/ca.crt、ca.key
  es-node-1/es-node-1.crt、es-node-1.key
  es-node-2/es-node-2.crt、es-node-2.key
  es-node-3/es-node-3.crt、es-node-3.key
```

### 4.4 创建各节点数据目录

```bash
cd "/Users/niici/local/xjyc/cloud-alibaba-2026/docker/elastic stack"

mkdir -p elasticsearch/node1/data elasticsearch/node1/logs
mkdir -p elasticsearch/node2/data elasticsearch/node2/logs
mkdir -p elasticsearch/node3/data elasticsearch/node3/logs
```

### 4.5 启动集群

```bash
cd "/Users/niici/local/xjyc/cloud-alibaba-2026/docker/elastic stack"

docker-compose up -d
```

---

## 五、配置文件说明

### 5.1 elasticsearch.yml（三节点共享）

```yaml
# 集群名称（与 docker-compose 环境变量保持一致）
cluster.name: "es-dev-cluster"
# 监听所有网络接口
network.host: 0.0.0.0
# 开启跨域支持
http.cors.enabled: true
# 注意：* 号不能通过环境变量传递，必须写在此文件中
http.cors.allow-origin: "*"
http.cors.allow-headers: "Authorization,Content-Type"
```

### 5.2 docker-compose.yml 关键配置说明

**集群发现配置：**

| 环境变量 | 说明 |
|----------|------|
| `cluster.name` | 所有节点必须相同 |
| `node.name` | 每个节点唯一标识 |
| `discovery.seed_hosts` | 列出其余节点地址，用于互相发现 |
| `cluster.initial_master_nodes` | 首次启动时的 master 候选节点，集群形成后忽略 |

**安全配置：**

**JVM 内存配置：**

| 参数 | 值 | 说明 |
|------|----|------|
| `ES_JAVA_OPTS` | -Xms256m -Xmx256m | 每节点堆内存 256MB，3 节点共 768MB |

> **macOS 内存建议**：Docker Desktop 默认仅分配 2GB，运行 3 节点 ES 集群需将 Docker 内存调整至 **≥ 6GB**（Settings → Resources → Memory）。如 Mac 总内存 ≥ 16GB，可将每节点调至 512MB。

**安全配置：**

| 环境变量 | 值 | 说明 |
|----------|----|------|
| `xpack.security.enabled` | true | 启用安全认证 |
| `xpack.security.http.ssl.enabled` | false | HTTP 层不加密（开发环境） |
| `xpack.security.transport.ssl.enabled` | true | 传输层必须开启 SSL（集群模式强制要求） |
| `xpack.security.transport.ssl.verification_mode` | certificate | 验证证书，不验证主机名 |
| `ELASTIC_PASSWORD` | Es@2026#Admin | elastic 超级用户密码 |

> **重要**：ES 8.x 集群模式下，开启安全认证（`xpack.security.enabled=true`）必须同时开启传输层 SSL，否则 bootstrap check 会阻止启动（exit code 78）。

### 5.3 kibana.yml

```yaml
server.host: "0.0.0.0"
server.port: 5601
server.name: "kibana-dev"

# 连接集群多节点，任一节点不可用时自动切换
elasticsearch.hosts: ["http://es-node-1:9200", "http://es-node-2:9200", "http://es-node-3:9200"]

# Service Account Token（集群重建后需重新生成）
elasticsearch.serviceAccountToken: "<token>"

i18n.locale: "zh-CN"
logging.root.level: info
```

> **注意**：ES 8.x 禁止 Kibana 直接使用 `elastic` 用户名密码，必须使用 Service Account Token。

### 5.4 logstash.conf（pipeline）

```
output {
  elasticsearch {
    # 配置集群所有节点，自动负载均衡，任一节点宕机自动切换
    hosts => ["http://es-node-1:9200", "http://es-node-2:9200", "http://es-node-3:9200"]
    user => "${ES_USER}"
    password => "${ES_PASS}"
    index => "%{[@metadata][beat]}-%{[@metadata][version]}-%{+YYYY.MM.dd}"
  }
}
```

---

## 六、验证集群状态

> **zsh 注意**：URL 中含 `?` 或 `#` 时必须用单引号包裹，否则 zsh 将其解析为通配符或注释导致报错。

### 6.1 查看集群健康状态

```bash
curl -u 'elastic:Es@2026#Admin' 'http://localhost:9200/_cluster/health?pretty'
```

正常输出（`status: green` 表示所有副本分片正常）：

```json
{
  "cluster_name" : "es-dev-cluster",
  "status" : "green",
  "number_of_nodes" : 3,
  "number_of_data_nodes" : 3,
  "active_primary_shards" : 0,
  "active_shards" : 0,
  "active_shards_percent_as_number" : 100.0
}
```

### 6.2 查看节点列表

```bash
curl -u 'elastic:Es@2026#Admin' 'http://localhost:9200/_cat/nodes?v'
```

### 6.3 查看 master 节点

```bash
curl -u 'elastic:Es@2026#Admin' 'http://localhost:9200/_cat/master?v'
```

---

## 七、生成 Kibana Service Account Token

集群首次启动成功（health: green）后执行：

```bash
curl -s -X POST -u 'elastic:Es@2026#Admin' \
  'http://localhost:9200/_security/service/elastic/kibana/credential/token/kibana-cluster-token' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['token']['value'])"
```

将输出的 token 字符串填入 `kibana/config/kibana.yml` 的 `elasticsearch.serviceAccountToken` 字段，然后重启 Kibana：

```bash
cd "/Users/niici/local/xjyc/cloud-alibaba-2026/docker/elastic stack"
docker-compose restart kibana
```

> **注意**：每次重建集群（清空数据目录）后必须重新生成 Token，旧 Token 会失效。

---

## 八、常见问题

### Q1：exit code 78 / bootstrap check failure

**原因**：集群模式下开启安全认证，但未开启传输层 SSL。

**解决**：确保三个节点都配置了 `xpack.security.transport.ssl.enabled=true` 且证书文件路径正确挂载。

### Q2：Kibana ECONNREFUSED

**原因**：ES 集群尚未完成 master 选举，通常需要 30~60 秒。

**解决**：等待集群健康状态变为 green 后 Kibana 会自动连接。

### Q3：certutil 在 M4 上崩溃

**原因**：ARM64 SVE 指令集不兼容。

**解决**：生成证书时必须添加 `-e _JAVA_OPTIONS="-XX:UseSVE=0"` 环境变量。

### Q4：数据目录权限问题

**原因**：ES 容器内用户 UID 为 1000，挂载目录无写权限。

**解决**：

```bash
chmod -R 777 elasticsearch/node1 elasticsearch/node2 elasticsearch/node3
```

### Q5：exit code 137 / 节点反复重启（OOM）

**原因**：Docker Desktop 内存不足，ES 节点被强制终止（137 = OOM kill）。

**解决**：
1. 打开 Docker Desktop → Settings → Resources → Memory，调整至 **≥ 6GB**
2. 或降低 JVM 堆大小（每节点 256MB 为最低推荐值）：

```yaml
- ES_JAVA_OPTS=-Xms256m -Xmx256m
```

### Q6：重建集群（清空数据重新初始化）

```bash
cd "/Users/niici/local/xjyc/cloud-alibaba-2026/docker/elastic stack"

docker-compose down

rm -rf elasticsearch/node1/data/* elasticsearch/node1/logs/*
rm -rf elasticsearch/node2/data/* elasticsearch/node2/logs/*
rm -rf elasticsearch/node3/data/* elasticsearch/node3/logs/*

docker-compose up -d
```

> 重建后需重新生成 Kibana Token，参考第七章。
