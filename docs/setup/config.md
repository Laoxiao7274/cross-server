# 配置指南

## 配置文件位置

`plugins/cross-server/config.yml`

## 完整配置示例

```yaml
server:
  id: "server-1"
  cluster: "qingchuan"

database:
  jdbc-url: "jdbc:mysql://127.0.0.1:3306/cross_server?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4"
  username: "root"
  password: "password"
  maximum-pool-size: 10

session:
  lock-seconds: 30
  heartbeat-seconds: 10
  kick-message: "你的跨服会话正在同步中，请稍后重试"

messaging:
  enabled: true
  redis-uri: "redis://127.0.0.1:6379/0"
  channel: "cross-server:sync"

node:
  heartbeat-seconds: 15
  offline-seconds: 45

teleport:
  handoff-seconds: 30
  arrival-check-delay-ticks: 10
  gateway:
    type: "proxy-plugin-message"
    plugin-message-channel: "BungeeCord"
    connect-subchannel: "Connect"
    server-map:
      server-1: "server-1"
      server-2: "server-2"
```

## 配置项详解

### server — 服务器节点标识

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `id` | String | `server-1` | 当前子服的唯一标识，所有子服必须不同 |
| `cluster` | String | `qingchuan` | 集群组名，同一集群的子服使用相同的 cluster 名称 |

> `server.id` 必须是集群内唯一的，否则启动时 `NodeIdentityGuardService` 会阻止重复 ID 的服务器启动。

### database — 数据库连接

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `jdbc-url` | String | — | MySQL JDBC 连接字符串，包含数据库名、SSL、时区、字符集等参数 |
| `username` | String | `root` | 数据库用户名 |
| `password` | String | `password` | 数据库密码 |
| `maximum-pool-size` | Int | `10` | HikariCP 连接池最大连接数 |

插件会自动创建 `player_session`、`player_snapshot`、`global_snapshot`、`player_identity`、`economy_transaction`、`node_status` 等数据表。

### session — 玩家会话锁

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `lock-seconds` | Int | `30` | 会话锁持有时间（秒），超时后自动释放 |
| `heartbeat-seconds` | Int | `10` | 玩家会话心跳间隔（秒） |
| `kick-message` | String | `你的跨服会话正在同步中，请稍后重试` | 当玩家数据正在同步时尝试加入服务器，显示的提示信息 |

### messaging — 跨服消息通信

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `enabled` | Boolean | `true` | 是否启用 Redis 跨服消息通信。设为 `false` 时将使用 Noop 模式 |
| `redis-uri` | String | `redis://127.0.0.1:6379/0` | Redis 连接 URI |
| `channel` | String | `cross-server:sync` | Redis Pub/Sub 通道名，用于跨服广播与失效通知 |

> 不启用 Redis 时，跨服广播（如玩家数据失效通知）将无法工作，数据同步依赖定时心跳轮询。

### node — 节点心跳

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `heartbeat-seconds` | Int | `15` | 节点发送心跳的间隔时间（秒） |
| `offline-seconds` | Int | `45` | 节点超过此时间未发送心跳，则被标记为离线 |

### teleport — 跨服传送

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `handoff-seconds` | Int | `30` | 一次传送 handoff 流程的最大允许时间（秒），超时后清理 |
| `arrival-check-delay-ticks` | Int | `10` | 玩家到达目标服后，延迟多少 tick 再检查传送是否成功 |

#### gateway — 传送网关

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `type` | String | `proxy-plugin-message` | 传送网关类型。目前支持 `proxy-plugin-message`（通过代理 Plugin Message） |
| `plugin-message-channel` | String | `BungeeCord` | 代理侧的 Plugin Message 通道 |
| `connect-subchannel` | String | `Connect` | BungeeCord Connect 子通道名 |
| `server-map` | Map | — | 内部服务器 ID 到 Velocity 后端服务器名的映射 |

> `server-map` 的 key 是插件内部目标服 ID（即 `server.id`），value 是 Velocity 后端服务器名。两者可以不同，只要映射正确即可。
