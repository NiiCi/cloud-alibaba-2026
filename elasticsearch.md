# Elasticsearch 学习文档

> Elasticsearch（简称 ES）是基于 Apache Lucene 构建的开源分布式搜索与分析引擎，支持实时全文检索、结构化查询、聚合分析，是 ELK（Elasticsearch + Logstash + Kibana）技术栈的核心组件。

---

## 一、核心概念

### 1.1 基本术语对照

| ES 概念 | 关系型数据库类比 | 说明 |
|---------|----------------|------|
| **Index（索引）** | 数据库（Database）| 存储同类文档的容器，名称必须全小写 |
| **Document（文档）** | 行（Row）| ES 中最小数据单元，以 JSON 格式存储 |
| **Field（字段）** | 列（Column）| 文档中的 key-value 键值对 |
| **Mapping（映射）** | 表结构（Schema）| 定义文档字段类型和索引方式 |
| **Shard（分片）** | 无直接类比 | 索引的物理分割单元，支持水平扩展 |
| **Replica（副本）** | 备库 | 分片的备份，提高可用性和读取吞吐量 |

### 1.2 分片与副本

```
Index: product-index
├── Shard 0 (Primary)  →  Replica 0
├── Shard 1 (Primary)  →  Replica 1
└── Shard 2 (Primary)  →  Replica 2
```

- **Primary Shard（主分片）**：负责写入，数量在创建索引时确定，**不可修改**
- **Replica Shard（副本分片）**：主分片的拷贝，负责读取和容灾，可动态调整
- **默认值**：1 个主分片、1 个副本（ES 7.0+ 以后）

> 本项目搭建的是 3 节点集群（es-node-1 / es-node-2 / es-node-3），建议主分片数设为 3，副本数设为 1，实现数据均匀分布与高可用。

### 1.3 文档的元数据字段

| 字段 | 说明 |
|------|------|
| `_index` | 文档所属索引名 |
| `_id` | 文档唯一标识（不指定则自动生成）|
| `_source` | 文档原始 JSON 数据 |
| `_score` | 查询相关度评分（越高越相关）|
| `_version` | 文档版本号，每次更新 +1 |

---

## 二、倒排索引（Inverted Index）

倒排索引是 Elasticsearch 实现**高效全文检索**的核心数据结构，是理解 ES 的最关键概念之一。

### 2.1 正排索引 vs 倒排索引

先看传统数据库的**正排索引**（Forward Index）：

| 文档 ID | 内容 |
|---------|------|
| 1 | 华为 Mate60 Pro 旗舰手机 |
| 2 | 苹果 iPhone 16 Pro 手机 |
| 3 | 华为 MatePad 平板 |

**查找"华为手机"** → 需要遍历每一行，逐行扫描，**效率极低**。

**倒排索引**反转了存储结构，以**词语**为键，以**文档列表**为值：

| 词语（Term）| 文档 ID 列表（Posting List）|
|------------|-------------------------|
| 华为 | [1, 3] |
| Mate60 | [1] |
| Pro | [1, 2] |
| 旗舰 | [1] |
| 手机 | [1, 2] |
| 苹果 | [2] |
| iPhone | [2] |
| MatePad | [3] |
| 平板 | [3] |

**查找"华为手机"** → 分词得到 `["华为", "手机"]` → 直接查表得 `[1,3]` 和 `[1,2]` → 取交集得 `[1]`，**毫秒级完成**。

### 2.2 倒排索引的完整结构

实际上每条记录不只存文档 ID，还包含更多信息：

```
Term        │ Doc ID │ 词频(TF) │ 位置(Position) │ 偏移(Offset)
────────────┼────────┼──────────┼───────────────┼─────────────
华为        │   1    │    1     │     [0]        │  [0-2]
华为        │   3    │    1     │     [0]        │  [0-2]
手机        │   1    │    1     │     [3]        │  [9-11]
手机        │   2    │    1     │     [3]        │  [12-14]
```

| 信息 | 说明 | 用途 |
|------|------|------|
| **Doc ID** | 文档编号 | 定位文档 |
| **词频 TF** | 该词在文档中出现次数 | 计算相关性评分 |
| **Position** | 词在文档中的位置序号 | 支持短语查询 `match_phrase` |
| **Offset** | 词在原文中的字符偏移 | 支持高亮显示 |

### 2.3 建立倒排索引的过程

文档写入时，ES 会经过**分析器（Analyzer）**处理：

```
原始文本："华为 Mate60 Pro 旗舰手机"
    ↓
Character Filter（字符过滤）：去除特殊字符
    ↓
Tokenizer（分词器）：["华为", "Mate60", "Pro", "旗舰", "手机"]
    ↓
Token Filter（词语过滤）：小写化、去停用词、同义词扩展
    ↓
写入倒排索引
```

这也是为什么 `text` 字段会被分词，而 `keyword` 字段不会 —— **`keyword` 整体作为一个 Term 写入倒排索引**。

### 2.4 为什么倒排索引查询这么快？

1. **Term Dictionary（词典）**：所有词按字典序排列，使用**二分查找**，O(log n) 定位词语
2. **Term Index（词典索引）**：对词典再建一层前缀索引（FST 数据结构），直接加载到内存，跳过磁盘 IO
3. **Posting List 压缩**：使用 **Frame of Reference** 算法压缩文档 ID 列表，减少磁盘占用
4. **Roaring Bitmap**：多个词取交集/并集时，使用位图运算，极速合并

```
查询 "华为" AND "手机"：
  华为 → [1, 3, 7, 15, ...]
  手机 → [1, 2, 7, 9, ...]
  交集 → [1, 7, ...]   ← 位图 AND 运算，极快
```

### 2.5 与 text / keyword 的关系

```
字段类型      写入方式              查询方式
─────────────────────────────────────────────
text      →  分词后建多个 Term  →  全文检索（match）
keyword   →  整体作为一个 Term  →  精确匹配（term）
```

> **一句话总结**：倒排索引就是把"文档→词语"的关系，反转为"词语→文档"的映射表，用空间换时间，让全文检索从 O(n) 线性扫描变为近似 O(1) 的查表操作。

---

## 三、字段类型（Field Types）

### 3.1 常用字段类型

| 类型分类 | 类型 | 说明 |
|---------|------|------|
| **文本类** | `text` | 全文检索字段，会被分词器分词 |
| **文本类** | `keyword` | 精确匹配字段，不分词（用于过滤、排序、聚合）|
| **数值类** | `integer` / `long` | 整型 |
| **数值类** | `float` / `double` | 浮点型 |
| **布尔类** | `boolean` | true / false |
| **日期类** | `date` | 日期，支持多种格式 |
| **对象类** | `object` | 嵌套 JSON 对象 |
| **嵌套类** | `nested` | 独立索引的对象数组，支持精确嵌套查询 |

### 3.2 text 与 keyword 的区别

```
text    → "Hello World"  ──分词──→  ["hello", "world"]   适合：全文搜索
keyword → "Hello World"  ──不分词→  ["Hello World"]       适合：精确匹配、排序
```

> **最佳实践**：对需要全文搜索又需要精确匹配的字段（如商品名称），可同时使用 `text` + `keyword`：
> ```json
> "name": {
>   "type": "text",
>   "fields": {
>     "keyword": { "type": "keyword" }
>   }
> }
> ```

---

## 四、Mapping（映射）

Mapping 相当于数据库的表结构定义，决定了字段如何被存储和索引。

### 4.1 Dynamic Mapping（动态映射）

ES 会自动根据写入的文档推断字段类型：

| 写入值 | 推断类型 |
|--------|---------|
| `"hello world"` | `text` + `keyword` |
| `123` | `long` |
| `3.14` | `float` |
| `true` | `boolean` |
| `"2024-01-01"` | `date` |

> **注意**：动态映射可能推断出不期望的类型（如数字 ID 被推断为 `long` 而非 `keyword`），生产环境建议**手动定义 Mapping**。

### 4.2 手动创建索引并定义 Mapping

```json
PUT /product-index
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "keyword"
      },
      "name": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "price": {
        "type": "double"
      },
      "stock": {
        "type": "integer"
      },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "createTime": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      },
      "tags": {
        "type": "keyword"
      }
    }
  }
}
```

---

## 五、REST API 基本操作（CRUD）

> 本项目集群中，HTTP 接口在 9200（node-1）、9201（node-2）、9202（node-3）端口暴露，开启了 x-pack security，API 调用需携带认证信息：
> ```bash
> # 基础认证格式
> curl -u elastic:Es@2026#Admin http://localhost:9200/...
> ```

### 5.1 索引操作

```bash
# 查看所有索引
GET /_cat/indices?v

# 创建索引
PUT /product-index

# 删除索引
DELETE /product-index

# 查看索引 Mapping
GET /product-index/_mapping

# 查看索引配置
GET /product-index/_settings
```

### 5.2 文档写入（新增）

```bash
# 指定 ID 写入
PUT /product-index/_doc/1
{
  "id": "1",
  "name": "华为 Mate60 Pro",
  "price": 6999.00,
  "stock": 100,
  "tags": ["手机", "华为", "旗舰"],
  "createTime": "2024-01-01 10:00:00"
}

# 不指定 ID，ES 自动生成
POST /product-index/_doc
{
  "name": "苹果 iPhone 16",
  "price": 7999.00
}
```

### 5.3 文档查询（Read）

```bash
# 根据 ID 查询
GET /product-index/_doc/1

# 查询全部（默认返回前 10 条）
GET /product-index/_search
{
  "query": {
    "match_all": {}
  }
}
```

### 5.4 文档更新（Update）

```bash
# 全量替换（覆盖整个文档，版本号 +1）
PUT /product-index/_doc/1
{
  "name": "华为 Mate60 Pro",
  "price": 6499.00
}

# 局部更新（只更新指定字段）
POST /product-index/_update/1
{
  "doc": {
    "price": 6299.00,
    "stock": 80
  }
}
```

### 5.5 文档删除（Delete）

```bash
# 根据 ID 删除
DELETE /product-index/_doc/1

# 根据查询条件删除
POST /product-index/_delete_by_query
{
  "query": {
    "term": { "tags": "过期" }
  }
}
```

---

## 六、Query DSL（查询语法）

### 6.1 查询分类

| 分类 | 说明 |
|------|------|
| **全文查询** | 对 `text` 字段进行分词后匹配，如 `match`、`match_phrase` |
| **精确查询** | 对 `keyword`/数值/日期字段精确匹配，如 `term`、`range` |
| **复合查询** | 组合多个查询条件，如 `bool` |
| **过滤查询** | 不计算相关性评分，效率更高，如 `filter` |

### 6.2 全文查询

```bash
# match：分词后查询，任一分词命中即可（OR 关系）
GET /product-index/_search
{
  "query": {
    "match": {
      "name": "华为手机"
    }
  }
}

# match_phrase：短语匹配，分词后必须按顺序全部命中
GET /product-index/_search
{
  "query": {
    "match_phrase": {
      "name": "华为 Mate"
    }
  }
}

# multi_match：在多个字段上执行 match 查询
GET /product-index/_search
{
  "query": {
    "multi_match": {
      "query": "华为旗舰",
      "fields": ["name", "description"]
    }
  }
}
```

### 6.3 精确查询

```bash
# term：精确匹配单个值（不分词）
GET /product-index/_search
{
  "query": {
    "term": {
      "tags": "华为"
    }
  }
}

# terms：精确匹配多个值（IN 查询）
GET /product-index/_search
{
  "query": {
    "terms": {
      "tags": ["华为", "苹果"]
    }
  }
}

# range：范围查询
GET /product-index/_search
{
  "query": {
    "range": {
      "price": {
        "gte": 5000,
        "lte": 8000
      }
    }
  }
}
```

> `range` 操作符：`gt`（大于）、`gte`（大于等于）、`lt`（小于）、`lte`（小于等于）

### 6.4 bool 复合查询（最常用）

```bash
GET /product-index/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "华为" } }
      ],
      "filter": [
        { "range": { "price": { "lte": 8000 } } },
        { "term": { "tags": "旗舰" } }
      ],
      "must_not": [
        { "term": { "tags": "停产" } }
      ],
      "should": [
        { "match": { "description": "5G" } }
      ]
    }
  }
}
```

| 子句 | 说明 | 影响评分 |
|------|------|---------|
| `must` | 必须满足（AND）| ✅ 是 |
| `filter` | 必须满足（AND），但不计算评分，性能更好 | ❌ 否 |
| `must_not` | 必须不满足（NOT）| ❌ 否 |
| `should` | 至少满足一个（OR），可提升评分 | ✅ 是 |

### 6.5 分页与排序

```bash
GET /product-index/_search
{
  "query": {
    "match_all": {}
  },
  "from": 0,
  "size": 10,
  "sort": [
    { "price": { "order": "asc" } },
    { "_score": { "order": "desc" } }
  ],
  "_source": ["name", "price", "tags"]
}
```

| 参数 | 说明 |
|------|------|
| `from` | 起始偏移量（默认 0）|
| `size` | 返回条数（默认 10，最大 10000）|
| `sort` | 排序字段，`asc`/`desc` |
| `_source` | 只返回指定字段（减少网络传输）|

> **深度分页限制**：`from + size > 10000` 时会报错，深度分页应使用 `search_after` 或 `scroll`。

---

## 七、聚合分析（Aggregations）

聚合类似 SQL 中的 `GROUP BY` + 聚合函数，可对数据进行统计分析。

### 7.1 指标聚合（Metrics）

```bash
# 对价格字段统计最大值、最小值、平均值、总和
GET /product-index/_search
{
  "size": 0,
  "aggs": {
    "max_price": { "max": { "field": "price" } },
    "min_price": { "min": { "field": "price" } },
    "avg_price": { "avg": { "field": "price" } },
    "total_price": { "sum": { "field": "price" } },
    "stats_price": { "stats": { "field": "price" } }
  }
}
```

### 7.2 桶聚合（Bucket）

```bash
# 按 tags 字段分组统计数量（类似 GROUP BY）
GET /product-index/_search
{
  "size": 0,
  "aggs": {
    "group_by_tag": {
      "terms": {
        "field": "tags",
        "size": 10
      },
      "aggs": {
        "avg_price": {
          "avg": { "field": "price" }
        }
      }
    }
  }
}
```

---

## 八、分析器（Analyzer）

分析器决定了 `text` 字段如何被分词，影响全文检索的结果。

### 8.1 分析过程

```
原始文本  →  Character Filter（字符过滤）→  Tokenizer（分词）→  Token Filter（词语过滤）→  Token 列表
```

### 8.2 内置分析器

| 分析器 | 说明 | 示例（"Hello World"）|
|--------|------|---------------------|
| `standard` | 默认，按 Unicode 分词，小写化 | ["hello", "world"] |
| `whitespace` | 按空格分词，不小写化 | ["Hello", "World"] |
| `keyword` | 不分词，整体作为一个 token | ["Hello World"] |
| `ik_max_word` | 中文分词（需安装 IK 插件），最细粒度 | ["华为", "手机", "华为手机"] |
| `ik_smart` | 中文分词（需安装 IK 插件），最粗粒度 | ["华为手机"] |

### 8.3 测试分析器

```bash
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "华为 Mate60 Pro 旗舰手机"
}
```

---

## 九、Spring Data Elasticsearch 集成

本项目使用 `Spring Data Elasticsearch 5.2.5` 与 ES 8.x 集成。

### 9.1 依赖配置（pom.xml）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

### 9.2 连接配置（application.yml）

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200,http://localhost:9201,http://localhost:9202
    username: elastic
    password: Es@2026#Admin
    connection-timeout: 5s
    socket-timeout: 30s
```

### 9.3 实体类定义

```java
@Document(indexName = "product-index", createIndex = true)
@Setting(shards = 3, replicas = 1)
public class Product {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createTime;

    // getter / setter
}
```

### 9.4 Repository 接口

```java
@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {

    // 根据名称模糊查询
    List<Product> findByNameContaining(String name);

    // 根据价格区间查询
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);

    // 根据标签精确查询
    List<Product> findByTags(String tag);
}
```

### 9.5 ElasticsearchOperations 高级查询

```java
@Service
public class ProductSearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public SearchHits<Product> search(String keyword, Double minPrice, Double maxPrice, int page, int size) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 全文检索
        if (StringUtils.hasText(keyword)) {
            boolQuery.must(m -> m.match(mq -> mq
                    .field("name")
                    .query(keyword)));
        }

        // 价格范围过滤（filter 不影响评分，性能更好）
        if (minPrice != null || maxPrice != null) {
            RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("price");
            if (minPrice != null) rangeQuery.gte(JsonData.of(minPrice));
            if (maxPrice != null) rangeQuery.lte(JsonData.of(maxPrice));
            boolQuery.filter(f -> f.range(rangeQuery.build()));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery.build()))
                .withPageable(PageRequest.of(page, size))
                .withSort(Sort.by(Sort.Direction.DESC, "_score"))
                .build();

        return elasticsearchOperations.search(query, Product.class);
    }
}
```

### 9.6 常用注解说明

| 注解 | 说明 |
|------|------|
| `@Document` | 标记实体类对应的索引，`indexName` 为索引名 |
| `@Id` | 标记文档 ID 字段 |
| `@Field` | 定义字段的 ES 类型、分析器等 |
| `@Setting` | 设置索引的分片数、副本数等 |

---

## 十、本项目集群配置说明

### 10.1 docker-compose.yml 关键配置解析

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `cluster.name` | `es-dev-cluster` | 集群名称，所有节点必须一致 |
| `discovery.seed_hosts` | 其他节点地址 | 节点发现，用于建立集群 |
| `cluster.initial_master_nodes` | 3 个节点 | 初次启动时的主节点候选列表 |
| `xpack.security.enabled` | `true` | 开启安全认证 |
| `xpack.security.http.ssl.enabled` | `false` | HTTP 层不开 SSL（开发环境方便调试）|
| `xpack.security.transport.ssl.enabled` | `true` | 节点间传输层开启 SSL（集群必须）|
| `ELASTIC_PASSWORD` | `Es@2026#Admin` | elastic 超级用户密码 |
| `ES_JAVA_OPTS` | `-Xms512m -Xmx512m` | 每个节点堆内存 512m |
| `_JAVA_OPTIONS` | `-XX:UseSVE=0` | macOS M 系列芯片（ARM64）禁用 SVE 指令集，解决 SIGILL 崩溃 |

### 10.2 端口映射

| 容器 | 宿主机端口 | 容器端口 | 用途 |
|------|-----------|---------|------|
| es-node-1 | 9200 | 9200 | HTTP API |
| es-node-2 | 9201 | 9200 | HTTP API |
| es-node-3 | 9202 | 9200 | HTTP API |
| es-node-1 | 9300 | 9300 | 节点间通信（Transport）|
| kibana | 5601 | 5601 | Kibana 可视化界面 |
| logstash | 5044 | 5044 | Beats 数据接收 |
| logstash | 9600 | 9600 | Logstash 监控 API |

### 10.3 集群健康检查

```bash
# 检查集群健康状态
curl -u elastic:Es@2026#Admin http://localhost:9200/_cluster/health?pretty

# 查看所有节点
curl -u elastic:Es@2026#Admin http://localhost:9200/_cat/nodes?v

# 查看分片分配情况
curl -u elastic:Es@2026#Admin http://localhost:9200/_cat/shards?v
```

集群状态说明：

| 状态 | 颜色 | 说明 |
|------|------|------|
| `green` | 绿色 | 所有主分片和副本分片正常 |
| `yellow` | 黄色 | 所有主分片正常，部分副本分片异常 |
| `red` | 红色 | 存在主分片未分配，数据不完整 |

---

### 10.4 分片在节点上的实际分布（Multi Elasticsearch Head 可视化）

以下是通过 **Multi Elasticsearch Head** 浏览器插件观察到的 `product-index` 索引分片分布情况（3 主分片 + 1 副本）：

![es-节点间分片及副本分布](./docker/elastic%20stack/es-节点间分片及副本分布.png)

> **颜色说明**：亮绿色方块为**主分片（Primary）**，负责数据写入；暗绿色方块为**副本分片（Replica）**，负责读取分担与容灾。

| 节点 | 节点角色 | 承载分片             |
|------|---------|------------------|
| **es-node-1** | ★ Master + Data | 分片 0（副本）、分片 1（主） |
| **es-node-2** | Data | 分片 0（主）、分片 2（副本） |
| **es-node-3** | Data | 分片 1（副本）、分片 2（主） |

**分布规则说明：**

- ES 自动保证**同一分片的主副本不会落在同一节点**，防止单点故障丢失数据
- 3 个节点各承载 **2 个分片**（共 6 个分片 = 3 主 + 3 副本），负载均衡
- ★ 符号代表当前集群的 **Master 节点**（es-node-1），负责集群元数据管理

> 当任意一个节点宕机时，ES 会自动将该节点上的主分片对应的副本提升为新主分片，保证集群持续可用（状态由 green → yellow，待节点恢复后自动重新平衡）。

---

## 十一、常用运维操作

### 11.1 启动与停止集群

```bash
# 进入 docker-compose 目录（注意目录名含空格需要引号）
cd "/Users/niici/local/xjyc/cloud-alibaba-2026/docker/elastic stack"

# 启动所有服务
docker-compose up -d

# 只启动 ES 节点（不含 Kibana/Logstash）
docker-compose up -d es-node-1 es-node-2 es-node-3

# 停止集群
docker-compose down

# 查看服务日志
docker logs es-node-1 -f
```

### 11.2 索引管理

```bash
# 查看索引占用空间
GET /_cat/indices?v&h=index,docs.count,store.size&s=store.size:desc

# 强制合并分片（减少段文件，提升查询性能，对只读索引效果最佳）
POST /product-index/_forcemerge?max_num_segments=1

# 刷新索引（使最新写入的数据立即可搜索）
POST /product-index/_refresh

# 重建索引（修改不可变的 Mapping 时使用）
POST /_reindex
{
  "source": { "index": "product-index" },
  "dest": { "index": "product-index-v2" }
}
```

### 11.3 扩大主分片数量（Reindex 方案）

> **主分片数量在索引创建时确定，不可直接修改**。需要通过「新建索引 + 迁移数据 + 切换别名」三步完成扩容，整个过程对业务无感知。

**第一步：创建新索引（指定目标分片数）**

```bash
PUT /product-index-v2
{
  "settings": {
    "number_of_shards": 6,
    "number_of_replicas": 1
  },
  "mappings": {
    "properties": {
      "id":          { "type": "keyword" },
      "name":        { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "price":       { "type": "double" },
      "stock":       { "type": "integer" },
      "description": { "type": "text", "analyzer": "ik_max_word" },
      "createTime":  { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis" },
      "tags":        { "type": "keyword" }
    }
  }
}
```

**第二步：将旧索引数据迁移到新索引**

```bash
POST /_reindex
{
  "source": {
    "index": "product-index"
  },
  "dest": {
    "index": "product-index-v2"
  }
}
```

> 数据量大时，可加 `"conflicts": "proceed"` 跳过冲突，加 `"size": 1000` 控制批次大小。

**第三步：通过别名（Alias）无缝切换**

使用别名可以让业务代码始终访问同一个名称，切换时无需改代码：

```bash
# 原子操作：移除旧索引的别名，同时为新索引添加别名
POST /_aliases
{
  "actions": [
    { "remove": { "index": "product-index",    "alias": "product" } },
    { "add":    { "index": "product-index-v2", "alias": "product" } }
  ]
}
```

业务代码统一访问别名 `product`，扩容后直接切到 `product-index-v2`，**完全无感知**。

**第四步：验证并删除旧索引**

```bash
# 验证新旧索引数据量是否一致
GET /_cat/indices/product-index*?v&h=index,docs.count,store.size

# 确认无误后删除旧索引
DELETE /product-index
```

**整体流程总结：**

```
旧索引（3分片）                新索引（6分片）
product-index   →  _reindex  →  product-index-v2
     ↑                                ↑
  别名 product  ── aliases 切换 ──  别名 product
```

### 11.4 调整副本数量

副本数不影响数据路由，**可以随时动态修改**，无需重建索引：

```bash
# 增加副本数（提高读取吞吐量和容灾能力）
PUT /product-index/_settings
{
  "number_of_replicas": 2
}

# 减少副本数（降低存储占用）
PUT /product-index/_settings
{
  "number_of_replicas": 1
}

# 临时关闭副本（大批量导入数据时，加速写入；导完后再恢复）
PUT /product-index/_settings
{
  "number_of_replicas": 0
}
```

> 修改后可通过集群健康检查确认副本是否分配完成：
> ```bash
> GET /_cluster/health/product-index?pretty
> # 等待 status 变为 green 即表示副本全部就绪
> ```

**主分片 vs 副本分片 修改能力对比：**

| | 主分片（Shard）| 副本（Replica）|
|---|---|---|
| 创建时指定 | ✅ 必须 | ✅ 必须 |
| 创建后修改 | ❌ 不可修改 | ✅ 随时可改 |
| 扩容方案 | Reindex 重建 | 直接改 `_settings` |

### 11.5 快照备份

```bash
# 注册快照仓库
PUT /_snapshot/my_backup
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backup"
  }
}

# 创建快照
PUT /_snapshot/my_backup/snapshot_1

# 恢复快照
POST /_snapshot/my_backup/snapshot_1/_restore
```

---

## 十二、最佳实践

### 12.1 Mapping 设计原则

1. **明确定义 Mapping**：不依赖动态映射，避免类型推断错误
2. **keyword vs text**：ID、状态、标签等精确匹配字段用 `keyword`；标题、描述等搜索字段用 `text`
3. **关闭不需要的特性**：不需要全文搜索的字段设置 `"index": false`；不需要聚合的字段设置 `"doc_values": false`
4. **避免 mapping 爆炸**：控制字段数量，避免动态映射产生大量未预期字段

### 12.2 查询优化原则

1. **filter 优先于 query**：不需要相关性评分的条件放入 `filter`，ES 会缓存 filter 结果
2. **避免深度分页**：`from + size` 超过 1000 时考虑使用 `search_after`
3. **只取需要的字段**：使用 `_source` 过滤，减少网络传输
4. **合理使用缓存**：`filter` 上下文的结果会被 ES 缓存，提升重复查询性能

### 12.3 写入优化原则

1. **批量写入**：使用 `_bulk` API 批量写入，比逐条写入效率高 5-10 倍
2. **合理刷新间隔**：大批量导入数据时，可临时设置 `"refresh_interval": "-1"` 关闭自动刷新
3. **副本数归零导入**：大批量导入时临时设置 `"number_of_replicas": 0`，导完再恢复

---

## 相关链接

- Elasticsearch 官方文档：https://www.elastic.co/guide/en/elasticsearch/reference/8.11/index.html
- Spring Data Elasticsearch 文档：https://docs.spring.io/spring-data/elasticsearch/reference/
- IK 中文分词插件：https://github.com/medcl/elasticsearch-analysis-ik
- Kibana 下载与使用：https://www.elastic.co/guide/en/kibana/8.11/index.html
- ELK 集群部署参考：./docker/elastic stack/elk-cluster.md
