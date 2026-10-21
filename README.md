# 校园二手交易平台（CampusTrade）

基于 **Spring Boot + MyBatis-Plus + Redis + RabbitMQ** 的校园二手交易平台，前后端分离架构，包含 **学生端**（买家侧 / 卖家侧）与 **管理端** 两个端。商品图片统一使用**色块渲染**（`coverColor` / `avatarColor` 存 hex 值），无需图片资源即可完整运行。

> 项目融合了两个教学项目的核心能力：仿黑马点评的缓存策略 / 分布式锁 / 社交 Feed 流 / 秒杀防超卖，以及仿苍穹外卖的工程分层 / JWT 认证 / 全局异常处理 / 定时任务。

---

## 功能特性

### 学生端（买家侧）
- 商品浏览：分类筛选、关键词搜索、最新 / 热度 / 价格排序、热门榜（Redis ZSet）
- 商品详情：浏览量统计、收藏、点赞、评价列表、举报
- 下单交易：防超卖（Redisson 锁 + DB CAS）、requestId 严格幂等、模拟支付、确认收货、退款申请
- 社交互动：关注 / 粉丝、共同关注、点赞、**关注动态 Feed 流**（推模式 + 滚动分页）
- 实时消息：WebSocket 私聊、订单状态实时提醒

### 学生端（卖家侧）
- 发布闲置：色块封面选色、成色 / 交付方式 / 面交地点
- 卖家中心：商品管理（编辑 / 上下架 / 删除）、订单管理（发货 / 处理退款）
- 数据看板：浏览量、收藏数、点赞数、成交量

### 管理端
- 商品审核（通过 / 驳回并附审核意见）
- 商品管理（违规下架 / 恢复上架）
- 分类管理、订单管理（强制关闭）、学生管理（封禁 / 解封）
- 举报处理、公告管理、数据统计（成交额趋势 / 热销 Top10）

---

## 技术栈

| 分类 | 技术 | 版本 |
|---|---|---|
| 基础框架 | Spring Boot | 2.7.3 |
| ORM | MyBatis-Plus | 3.4.3 |
| 连接池 | Druid | 1.2.1 |
| 数据库 | MySQL | 8.0 |
| 缓存 / 分布式 | Redis + Redisson（分布式锁） | 6.x / 3.13.6 |
| 消息队列 | RabbitMQ（TTL + DLX + Publisher Confirm） | 3.x / 4.x |
| 认证 | JWT（jjwt）双端令牌 | 0.9.1 |
| 实时通信 | WebSocket（@ServerEndpoint） | — |
| 定时任务 | Spring Task | — |
| 工具库 | Hutool / Fastjson / Lombok | 5.7.17 / 1.2.76 |
| 前端 | Vue 2 + axios（原生多页应用，无构建步骤） | 2.5.16 |

---

## 工程结构

```
CampusTrade
├── pom.xml                    # 父工程（聚合 + 依赖版本管理）
├── campus-common              # 通用模块：Result / 常量 / 枚举 / 异常 / JWT / RedisIdWorker
├── campus-pojo                # 实体 / DTO / VO
├── campus-server              # Web 模块（可执行 jar）
│   └── src/main
│       ├── java/com/campus
│       │   ├── config         # WebMvcConfig / RedissonConfig / RedisDataInitializer 等
│       │   ├── controller     # student 学生端 / admin 管理端
│       │   ├── service        # 业务层（含缓存 / 锁 / Feed 流核心实现）
│       │   ├── mapper         # MyBatis-Plus Mapper（含 CAS 防超卖 SQL）
│       │   ├── interceptor    # 双拦截器：Token 刷新 + 登录校验
│       │   ├── websocket      # WebSocket 服务（订单提醒 + 私聊）
│       │   ├── mq             # 可靠发布 / 订单超时消费者
│       │   └── task           # Outbox 发布 / 自动确认收货
│       └── resources
│           ├── application.yml        # 主配置（端口 8082 / JWT / 业务参数）
│           ├── application-dev.yml    # 开发环境（MySQL / Redis 连接）
│           └── mapper                 # XML（CAS 状态流转等复杂 SQL）
├── campus-web                 # 前端（前后端分离，独立目录，由 server 静态托管）
│   ├── student                # 学生端 9 个页面（H5 风格，PC 响应式适配）
│   ├── admin                  # 管理端 2 个页面（ElementUI）
│   └── static                 # vue / axios / 共享 js / 样式
├── sql/campus_trade.sql       # 建库建表 + 种子数据（含 MQ Outbox）
└── scripts/api-test.ps1       # 冒烟测试脚本（19 项断言）
```

---

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0
- Redis 6.x（本地安装或 Docker 容器均可）
- RabbitMQ 3.x / 4.x（推荐 management 镜像）

### 1. 初始化数据库

```bash
mysql -uroot -p < sql/campus_trade.sql
```

脚本会自动创建独立数据库 `campus_trade` 并写入种子数据，**不会影响其他项目的数据库**。已有数据库只需单独执行脚本中标记为 `5A. MQ 可靠消息 Outbox` 的 `CREATE TABLE` 段。

### 2. 启动 Redis

```bash
# Docker 方式（推荐，独立容器隔离）
docker run -d --name campus-redis -p 6379:6379 -v campus-redis-data:/data redis:6-alpine
```

项目使用 Redis **db2** 分片，与同机其他项目（db0/db1）互不干扰。应用启动时会自动预热社交缓存（关注 / 粉丝 / 点赞 / 热榜）。

### 3. 启动 RabbitMQ

```bash
docker run -d --name campus-rabbitmq -p 5672:5672 -p 15672:15672 -v campus-rabbitmq-data:/var/lib/rabbitmq rabbitmq:4-management
```

管理控制台：http://localhost:15672，开发环境默认账号和密码均为 `guest`。项目使用固定 TTL 队列与 DLX，不依赖延迟消息插件。

### 4. 按需修改配置

`campus-server/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_trade
    username: root
    password: "123456"      # 改成你的密码
  redis:
    host: localhost
    port: 6379
    database: 2             # 独立分片，勿改为 db0
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

订单超时默认使用 RabbitMQ；紧急回退 Redis ZSet 时，可将 `campus.order.timeout-strategy` 改为 `redis`。

### 5. 打包并启动

```bash
mvn clean package -DskipTests
cd campus-server
java -jar target/campus-server.jar
```

服务端口 **8082**，前端静态资源由 campus-server 直接托管（`campus.web.base-dir` 指向 `../campus-web`）。

### 6. 访问入口

| 入口 | 地址 |
|---|---|
| 学生端 | http://localhost:8082/student/index.html |
| 管理端 | http://localhost:8082/admin-ui/login.html |

---

## 测试账号

| 角色 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 学生 | 20230001 | 123456 | 测试同学（买家为主） |
| 学生 | 20230002 | 123456 | 卖家小张（卖家为主） |
| 学生 | 20230003 | 123456 | 毕业学长（有粉丝，用于 Feed 流演示） |
| 管理员 | admin | 123456 | 管理端专用账号体系 |

---

## 核心技术点

| 能力 | 实现方案 |
|---|---|
| **防超卖** | Redisson 分布式锁 `lock:order:{productId}` + 数据库 CAS（`UPDATE ... WHERE status=1`），双重保障 |
| **下单幂等** | 客户端一次下单复用同一 `requestId`；服务端锁内当前读返回原订单；`(buyer_id, product_id, request_id)` 唯一索引兜底 |
| **缓存三兄弟** | 空值缓存防穿透 / Redisson 互斥锁重建防击穿 / 随机 TTL 防雪崩 |
| **唯一订单号** | RedisIdWorker：`yyyyMMdd` + Redis INCR 12 位序列（分布式安全） |
| **超时订单** | MySQL Transactional Outbox + RabbitMQ TTL/DLX；Publisher Confirm、手动 ACK、延迟重试与死信队列，消费端 CAS 幂等 |
| **自动收货** | Spring Task 每 10 分钟扫描，发货满 7 天自动确认收货 |
| **Feed 流** | 推模式：发布商品审核通过后写入粉丝 ZSet，滚动分页（minTime + offset） |
| **双端认证** | JWT（学生 token / 管理端 admin-token 双密钥）+ Redis 登录态 + RefreshTokenInterceptor 滑动续期 |
| **实时推送** | WebSocket `/ws/{token}`，11 种事件类型（新订单 / 支付 / 发货 / 收货 / 聊天消息等） |
| **前端工程** | 色块渲染（无图片资源）、Vue.mixin 统一注册全局工具函数、静态资源版本化 + 协商缓存 |

---

## 接口冒烟测试

项目自带可复用的冒烟测试脚本（PowerShell），覆盖 19 项断言：

```bash
# 服务启动后执行
powershell -ExecutionPolicy Bypass -File scripts/api-test.ps1
```

- **Part A（8 项）**：Feed 流全链路（发布 → 审核 → 粉丝收流）
- **Part B（7 项）**：前端页面 / 静态资源访问
- **Part C（4 项）**：核心接口回归 + 匿名鉴权拦截

> 完整交易链路（下单 → 支付 → 发货 → 收货 → 评价）、防超卖、管理端审核与统计均已在开发过程验证通过。

---

## 环境隔离说明

为避免污染同机其他项目，本项目采用三层隔离：

| 资源 | 隔离方式 |
|---|---|
| MySQL | 独立数据库 `campus_trade`（12 张表） |
| Redis | 独立容器 `campus-redis`，仅使用 db2 分片 |
| RabbitMQ | 独立容器 `campus-rabbitmq`，业务队列统一使用 `campus.order.*` 前缀 |
| 端口 | 8082（不占用 80 / 8080 等常用端口） |

---

## 分布式部署（扩展说明）

核心业务已支持多实例下的订单超时竞争消费（RabbitMQ + 状态 CAS）；登录态、锁、缓存和订单号基于共享 Redis/MySQL。水平扩展时仍需补充两处改造：

1. **WebSocket 跨实例推送**：当前在线会话表为单机内存，多实例下需引入 **Redis Pub/Sub 消息总线**——发送时 publish，各实例订阅后检查目标用户是否在本机连接并推送；
2. **普通定时任务去重**：自动收货和 Outbox 扫描在多实例会重复执行；业务已通过 CAS/Publisher Confirm 保证安全，但可进一步用 Redisson 锁选主减少重复工作。

部署架构建议：

```
浏览器 → Nginx（托管 campus-web 静态资源 + 负载均衡，转发 WebSocket Upgrade 头）
            ├─→ campus-server 实例1 :8082 ─┐
            └─→ campus-server 实例2 :8083 ─┤
                                           ├─→ MySQL（共享）
                                           ├─→ Redis（共享）
                                           └─→ RabbitMQ（共享）
```
