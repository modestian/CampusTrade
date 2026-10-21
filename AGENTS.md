# AGENTS.md — 校园二手交易平台开发指南

> 本文件面向 AI 编码代理与项目开发者，说明项目架构、开发流程与协作规范。
> 动手改代码前请先通读本文件；快速启动步骤另见 [README.md](README.md)。

---

## 1. 项目概览

校园二手交易平台（CampusTrade）：Spring Boot 单体应用，**前后端分离**（前端为独立目录，由后端静态托管），包含 **学生端**（买家侧 + 卖家侧）与 **管理端** 两个端。商品图统一用**色块**渲染（`coverColor` / `avatarColor` 存 hex 值），无图片资源依赖。

| 分类 | 技术 |
|---|---|
| 框架 | Spring Boot 2.7.3 / MyBatis-Plus 3.4.3 / Druid |
| 存储 | MySQL 8（`campus_trade` 库）+ Redis 6（db2 分片）+ Redisson 3.13.6 |
| 消息 | RabbitMQ（订单超时 TTL/DLX + Transactional Outbox） |
| 认证 | JWT 双端令牌（学生 `token` / 管理 `admin-token`）+ Redis 登录态 |
| 实时 | WebSocket `/ws/{token}`（订单事件 + 私聊） |
| 定时 | Spring Task（超时订单取消 / 自动收货） |
| 前端 | Vue 2.5.16 + axios（原生多页应用，无构建步骤） |

---

## 2. 架构说明

### 2.1 模块结构

```
campus-common   通用：Result / 常量 / 枚举 / 异常 / JwtUtil / RedisIdWorker
campus-pojo     数据模型：entity / dto / vo
campus-server   Web 层：controller(student+admin) / service / mapper / interceptor / websocket / task
campus-web      前端（独立目录，非 Maven 模块）：student 9 页 / admin 2 页 / static 共享资源
sql/            建库建表 + 种子数据（12 张表）
scripts/        api-test.ps1 冒烟测试（19 项断言）
```

依赖方向：`server → pojo → common`，**不可反向依赖**；`campus-web` 与 Java 工程完全解耦。

### 2.2 请求链路

```
浏览器 → WebMvcConfig 拦截器链
          ├─ RefreshTokenInterceptor（全部路径：解析 token 并滑动续期 Redis 登录态）
          └─ LoginInterceptor（/student/**、/admin/**：登录校验，白名单放行浏览类接口）
       → Controller → Service（缓存/锁/事务）→ Mapper → MySQL/Redis
                                            → Outbox → RabbitMQ → Consumer
       → Result 统一响应（code=1 成功 / 401 未登录 / 403 无权限）
```

静态资源映射：`/student/**` → `campus-web/student/`，`/admin-ui/**` → `campus-web/admin/`，`/campus-static/**` → `campus-web/static/`。

### 2.3 核心设计（修改相关代码前必读）

| 能力 | 实现位置 | 要点 |
|---|---|---|
| 防超卖 | `OrderServiceImpl.submit` + `ProductMapper.casOnSaleToTrading` | Redisson 锁 `lock:order:{id}` + DB CAS（`WHERE status=1`），**双重保障，缺一不可** |
| 缓存三兄弟 | `ProductServiceImpl.getDetail` | 空值缓存防穿透 / Redisson 锁重建防击穿 / 随机 TTL 防雪崩 |
| 订单号 | `RedisIdWorker` | `yyyyMMdd` + Redis INCR，分布式安全 |
| 超时取消 | `MqOutboxPublishTask` + RabbitMQ TTL/DLX + `OrderTimeoutConsumer` | 订单与 Outbox 同事务；Confirm 后标记发送；手动 ACK、重试、死信、CAS 幂等；可切回 Redis |
| 订单状态机 | `OrderStatus` 枚举 | 1待支付→2待发货→3待收货→4已完成（5已取消/6退款中/7已退款），流转必须走 CAS |
| 商品状态机 | `ProductStatus` 枚举 | 0待审核→1在售→2交易中→3已售出（4已下架/5已驳回） |
| Feed 流 | `SocialServiceImpl` | 推模式：审核通过写粉丝 ZSet `feed:{userId}`，滚动分页（minTime+offset） |
| 社交缓存预热 | `RedisDataInitializer` | 启动时从 DB 重建关注/粉丝/点赞/热榜（幂等），**社交数据以 MySQL 为权威源** |
| WebSocket | `WebSocketServer` | 静态注入依赖；`sendToUser` 单机内存表（分布式改造点见第 6 节） |

---

## 3. 常用命令

```bash
# 环境（首次）
mysql -uroot -p < sql/campus_trade.sql          # 建库 + 种子数据
docker run -d --name campus-redis -p 6379:6379 -v campus-redis-data:/data redis:6-alpine
docker run -d --name campus-rabbitmq -p 5672:5672 -p 15672:15672 -v campus-rabbitmq-data:/var/lib/rabbitmq rabbitmq:4-management

# 构建与启动（每次改后端）
mvn clean package -DskipTests
cd campus-server && java -jar target/campus-server.jar

# 冒烟测试（服务运行中执行）
powershell -ExecutionPolicy Bypass -File scripts/api-test.ps1
```

| 入口 | 地址 |
|---|---|
| 学生端 | http://localhost:8082/student/index.html |
| 管理端 | http://localhost:8082/admin-ui/login.html |
| 测试账号 | 学生 `20230001`~`20230003` / 密码 `123456`；管理员 `admin` / `123456` |

> **环境隔离红线**：数据库仅用 `campus_trade`；Redis 仅用 **db2**（勿动 db0/db1）；RabbitMQ 仅用 `campus.order.*` 资源；服务仅用 **8082** 端口。本机存在其他项目（hm-dianping / sky-take-out），**任何命令不得影响它们的库、Redis 分片、MQ 资源与端口**。

---

## 4. 开发流程

标准节奏：**整理 → 规划 → 编码 → 测试 → 提交**。禁止跳过测试环节直接提交。

### 4.1 整理（理解现状）

- 改任何模块前，先读目标文件与相邻实现，确认现有约定（拦截器/异常/缓存键/状态枚举）；
- 涉及数据变更时先查数据库现状（`SELECT` 确认结构与数据），涉及缓存时先查 Redis 键（`docker exec campus-redis redis-cli -n 2 KEYS "*"`）；
- 排查问题优先看证据：服务日志、浏览器控制台（Vue 报错）、网络请求，而非猜测。

### 4.2 规划

- 多步任务先列清单（Todo），逐项推进、逐项验证；
- 涉及 3 个以上文件或跨前后端的改动，先明确：改哪些文件、接口是否变化、如何验证；
- 有方案分歧时**先说明取舍再动手**（例如"加参数" vs "改数据表"）。

### 4.3 编码

- 遵循现有分层：Controller 只做参数校验与转发；业务规则在 Service；SQL 复杂时写 XML；
- 事务方法注意缓存一致性：**先更新 DB 再删缓存**；
- 前端改动见第 6 节"前端三条铁律"；
- 一次修改聚焦一件事，不要把多个不相关改动混在一起。

### 4.4 测试（验证闭环）

| 层级 | 手段 | 通过标准 |
|---|---|---|
| 编译 | `mvn clean package -DskipTests` | BUILD SUCCESS |
| 接口 | `scripts/api-test.ps1` | 19/19 PASS，无回归 |
| 前端 | 浏览器实测（含 PC 宽度 ≥768px 与窄屏） | 控制台无报错、交互闭环 |
| 数据 | 直接查 MySQL / Redis 复核 | 与预期状态一致 |

改动核心链路（下单/支付/审核/退款）后**必须**跑冒烟测试；每次验证通过后再进行下一步。

### 4.5 提交

测试通过后提交，规范见下一节。

---

## 5. Git 提交流程

### 5.1 提交信息规范（Conventional Commits）

格式：`<type>(<scope>): <subject>`

- **type**：`feat` 新功能 / `fix` 修复 / `docs` 文档 / `refactor` 重构 / `perf` 性能 / `test` 测试 / `build` 构建 / `chore` 杂务
- **subject**：祈使句、≤50 字符、不加句号；**scope** 常用：`chat` / `product` / `order` / `social` / `ui` / `web` / `auth`
- **body**（可选）：说明"是什么、为什么"，不写"怎么做"
- 示例：
  - `feat(order): 支持退款申请与卖家处理流程`
  - `fix(chat): 修复返回按钮在聊天窗口失效的问题`
  - `docs: 补充 AGENTS.md 开发指南`

### 5.2 提交粒度与流程

1. **一个提交只做一件事**：独立的功能/修复单独提交；跨前后端的同一问题可合并为一个提交；
2. 提交前检查：
   ```bash
   git status        # 确认无 target/、无临时文件、无 IDE 文件
   git diff --staged # 复核将要提交的内容
   ```
3. 本项目文件组织约定：
   - 新增前端页面 → 放 `campus-web/student|admin/`，脚本引用带版本参数；
   - 新增后端接口 → 按端放 `controller/student|admin/`，统一返回 `Result`；
   - 数据库变更 → 只改 `sql/campus_trade.sql` 并注明增量 SQL。

### 5.3 常见操作注意

- 取消暂存（**本仓库当前为零提交状态**，`git reset HEAD` 不可用）：
  ```bash
  git rm -r --cached .            # 全部取消（文件保留在工作区）
  git rm --cached <路径>          # 定向取消
  ```
  有提交历史后可用：`git restore --staged <file>`（等价旧写法 `git reset HEAD <file>`）；
- 禁止对共享分支 force push；禁止提交 `target/`、`.idea/`、密码明文之外的敏感信息；
- `git commit` 必须由开发者本人确认后执行（AI 代理不得擅自提交）。

---

## 6. 项目关键约定与避坑清单

### 前端三条铁律（均踩过坑）

1. **模板中使用的工具函数必须经 `app.js` 的 `Vue.mixin` 注册**（含 `location`）；模板表达式只在实例作用域求值，未注册会 `TypeError` 导致整页空白；
2. **修改 `app.js` / `style.css` 后必须把 HTML 引用版本号 +1**（当前 `?v=4` → 下一版 `?v=5`），否则浏览器缓存导致改动不生效；
3. **返回按钮统一用 `goBack(fallback)`**（mixin 提供，智能判断 referrer）；页面内视图切换用 `history.replaceState`，勿用 `location.href`。

### 其他约定

- 列表页从详情返回需刷新数据时，监听 `pageshow`（bfcache 恢复）重新拉取，保证浏览量等实时性；
- 静态资源响应头已配置 `Cache-Control: no-cache` 协商缓存（开发期），普通刷新即可生效；
- WebSocket 推送事件类型集中在 `WsMessageType` 枚举，前后端各自维护映射，新增事件需同步两端；
- 测试数据清理：临时验证产生的数据要删除（如 `DELETE` 测试消息），勿留脏数据。

### 已知的分布式改造点（扩展时处理）

- `WebSocketServer.ONLINE_SESSIONS` 为单机内存表 → 多实例需引入 Redis Pub/Sub 广播；
- 自动收货与 Outbox 扫描等 `@Scheduled` 任务多实例会重复执行；当前依靠状态 CAS / MQ 幂等保证安全，仍可用 Redisson 锁选主减少重复工作；
- 前端静态资源多实例部署 → 移交 Nginx 托管并配置负载均衡与 WebSocket `Upgrade` 头转发。
