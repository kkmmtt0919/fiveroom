# FiveRoom

基于 Spring Boot 和原生 WebSocket 开发的双人在线五子棋后端项目。

项目目标是完成从创建房间、双人入局到实时对战的后端 MVP，并支持玩家断线后通过快照恢复棋局。当前不包含前端页面。

## 技术栈

- Java 17
- Spring Boot 4.1.2（以 `pom.xml` 配置为准）
- Maven / Maven Wrapper
- Spring WebMVC、Jakarta Bean Validation
- 原生 WebSocket（Spring WebSocket，未使用 STOMP）
- JUnit 5
- Lombok
- ConcurrentHashMap 内存状态管理

## 已实现功能

- 创建、加入、查询房间，一间房间支持两名对战玩家。
- 通过玩家 ID 和 Token 验证身份，由房主开始游戏。
- 15×15 棋盘，黑白双方交替落子。
- 服务端校验玩家身份、当前回合、坐标范围及位置占用情况。
- 横向、纵向和两个斜向的五连胜负判定，以及棋盘满后的平局判定。
- WebSocket 实时落子广播和游戏结束通知。
- 玩家上下线状态同步、断线重连与房间及棋局快照恢复。
- 基于 `requestId` 的 MOVE 消息幂等处理。
- 落子消息与棋局快照携带 `sequence` 状态版本号。
- 单元测试、业务集成测试与并发测试用例。

## 项目结构

```text
fiveroom/
├── src/
│   ├── main/
│   │   ├── java/com/hw/fiveroom/
│   │   │   ├── game/          # 棋盘、对局、落子规则与胜负判定
│   │   │   ├── player/        # 玩家信息
│   │   │   ├── room/          # 房间管理、业务服务与 HTTP 接口
│   │   │   ├── system/        # 健康检查接口
│   │   │   ├── websocket/     # WebSocket 认证、会话与消息处理
│   │   │   └── FiveroomApplication.java
│   │   └── resources/application.properties
│   └── test/                 # 自动化测试
├── .mvn/                     # Maven Wrapper 配置
├── mvnw
├── mvnw.cmd
├── README.md
└── pom.xml
```

HTTP 请求由 `RoomController` 调用 `RoomService`，通过 `RoomManager` 查找和管理房间。WebSocket 消息由 `GameWebSocketHandler` 处理，结合 `SessionManager` 管理连接，并调用 `RoomService` 完成认证和落子。

`Room` 负责房间生命周期，`Game` 负责对局状态；`GameEngine` 结合 `Board` 和 `WinChecker` 执行落子及胜负判定。落子业务通过 `synchronized (room)` 保护同一房间内的状态修改。

## 本地运行

### 环境准备

1. 安装 JDK 17，并配置 `JAVA_HOME`。
2. 确保能够访问 Maven 依赖仓库；首次使用 Maven Wrapper 时需要下载 Maven 和项目依赖。
3. 在项目根目录执行命令，或使用 IntelliJ IDEA 导入根目录的 `pom.xml`。

### 使用 IntelliJ IDEA

等待 Maven 依赖加载完成后，运行：

```text
com.hw.fiveroom.FiveroomApplication
```

### 使用命令行

Windows PowerShell：

```powershell
.\mvnw.cmd spring-boot:run
```

macOS / Linux：

```bash
./mvnw spring-boot:run
```

如果已安装并配置 Maven，也可以使用：

```bash
mvn spring-boot:run
```

默认 HTTP 地址为 `http://localhost:8080`，WebSocket 地址为 `ws://localhost:8080/ws/game`。

访问 `GET http://localhost:8080/api/health`，当前健康检查接口返回纯文本 `halo`。

WebSocket 当前允许的浏览器 Origin 为 `http://localhost:5173`，配置位于 `WebSocketConfig`。前端接入时需与该配置保持一致。

## 接口概览

| Method | Path | 功能 |
| --- | --- | --- |
| POST | `/api/rooms` | 创建房间 |
| POST | `/api/rooms/{roomId}/join` | 加入房间 |
| GET | `/api/rooms/{roomId}` | 查询房间 |
| POST | `/api/rooms/{roomId}/start` | 房主开始游戏 |
| GET | `/api/health` | 健康检查 |

创建或加入房间时，请求体使用 JSON：

```json
{
  "nickname": "玩家一"
}
```

创建和加入成功后会返回 `roomId`、`playerId`、`playerToken` 和 `status`。客户端需要保存各自的身份信息；Token 不应公开或提交到 Git。

开始游戏时，房主需在请求头中提供 `X-Player-Id` 和 `X-Player-Token`。落子通过 WebSocket 发送，棋盘行列坐标均为 `0` 至 `14`。

基本对战流程：

1. 玩家一创建房间，玩家二使用房间号加入。
2. 两名玩家连接 `/ws/game`，各自发送包含 `type`、`roomId`、`playerId`、`playerToken` 的 `AUTH` 消息。
3. 认证成功后接收纯文本 `AUTH_SUCCESS` 和 JSON 格式的 `ROOM_SNAPSHOT`。
4. 房主调用开始游戏接口，两名玩家随后按回合发送 `MOVE`。
5. `MOVE` 包含 `type`、`requestId`、`roomId`、`row`、`col`；成功后接收 `MOVE_APPLIED` 广播，游戏结束时接收 `GAME_OVER`。
6. 断线后重新连接并发送 `AUTH`，通过 `ROOM_SNAPSHOT` 恢复状态。

当前协议混合使用 JSON 事件与纯文本状态消息，客户端需要分别处理。`requestId` 为非空且不超过 64 个字符的字符串；同一玩家重试同一落子时复用原值，下一次落子使用新值。重复成功请求返回纯文本 `MOVE_DUPLICATE`，不会再次落子；同一 `requestId` 改用不同坐标会被拒绝。

## 测试

Windows PowerShell 执行全部测试：

```powershell
.\mvnw.cmd test
```

交付前清理构建产物并重新测试：

```powershell
.\mvnw.cmd clean test
```

macOS / Linux 使用 `./mvnw clean test`；已安装 Maven 时可使用 `mvn clean test`。也可以在 IDEA 的 Maven 面板中执行 `test`。

现有测试覆盖应用上下文加载、落子规则、胜负判定、房间与对局状态同步、业务集成及并发场景。执行后应检查控制台的测试结果，报告位于 `target/surefire-reports/`。此处列出的是测试执行方式，不代表最近一次测试已通过。

## 项目限制与待解决事项

- 房间、对局及会话使用内存管理，服务器重启后房间和对局数据不保留。
- 当前仅支持双人对战，没有注册登录系统、历史战绩、自动匹配和观战功能。
- 暂未实现多实例部署与跨实例状态同步。
- `sequence` 当前对应成功落子次数，是对局状态版本号；并发广播的严格顺序投递仍需进一步验证和完善。
- 已处理落子请求的幂等缓存尚需增加容量限制。
- 房间与连接资源的清理机制仍需完善。

## 后续规划

- 开发房间页面与 15×15 可交互棋盘，接入现有 HTTP 和 WebSocket 接口。
- 完善广播排序、请求缓存容量控制及资源清理。
- 增加历史棋局持久化与回放。
- 按需求扩展自动匹配和观战功能。

## Git 交付检查

提交前运行全量测试，并通过 `git status` 检查待提交文件。不要提交真实玩家 Token、`.env`、IDE 缓存或 `target/` 构建产物。当前 `.gitignore` 已包含 `target/` 和常见 IDE 文件；新增本地配置时仍需单独核对忽略规则。

文档编写、最终测试与 Git 提交均完成后，再将 Day 6 进度更新为 **4/4 · 100%**。
