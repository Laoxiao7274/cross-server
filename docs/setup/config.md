# 配置指南

配置文件位置：`plugins/cross-server/config.yml`

## 完整配置示例

```yaml
server:
  id: "server-1"
  cluster: "qingchuan"

database:
  jdbc-url: "jdbc:mysql://127.0.0.1:3306/cross_server?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
  username: "root"
  password: "password"
  maximum-pool-size: 10

messaging:
  enabled: true
  redis-uri: "redis://127.0.0.1:6379/0"
  channel: "cross-server:sync"

session:
  lock-seconds: 30
  heartbeat-seconds: 10
  kick-message: "你的跨服会话正在同步中，请稍后重试"

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
|--------|------|--------|------|
| `id` | String | `server-1` | 当前子服的唯一标识，**所有子服必须不同** |
| `cluster` | String | `qingchuan` | 集群组名，同一集群的子服使用相同的名称 |

> `server.id` 必须是集群内唯一的。如果两个子服使用相同的 ID，`NodeIdentityGuardService` 会在启动时阻止启动，防止数据写冲突。

### database — 数据库连接

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `jdbc-url` | String | — | MySQL JDBC 连接字符串 |
| `username` | String | `root` | 数据库用户名 |
| `password` | String | `password` | 数据库密码 |
| `maximum-pool-size` | Int | `10` | HikariCP 连接池最大连接数 |

**jdbc-url 格式说明：**

```
jdbc:mysql://地址:端口/数据库名?参数
```

常用参数：

| 参数 | 建议值 | 说明 |
|------|--------|------|
| `useSSL` | `false` | 本地开发可关闭 SSL |
| `serverTimezone` | `Asia/Shanghai` | 时区，避免时间显示错误 |
| `characterEncoding` | `utf8` | 字符编码 |

插件启动时自动创建以下表，无需手动操作：

- `player_session` — 玩家会话锁
- `player_snapshot` — 玩家数据快照
- `global_snapshot` — 全局数据快照
- `player_identity` — UUID 与名称映射
- `economy_transaction` — 经济交易记录
- `transfer_history` — 传送历史记录
- `node_status` — 节点心跳状态

### messaging — 跨服消息通信

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | Boolean | `true` | 是否启用 Redis 广播 |
| `redis-uri` | String | `redis://127.0.0.1:6379/0` | Redis 连接 URI |
| `channel` | String | `cross-server:sync` | Pub/Sub 频道名 |

**`enabled: false` 时的影响：**

- 跨服数据变更广播不工作，各子服之间无法实时感知数据变化
- 数据同步依赖定时心跳轮询，延迟较高
- 适用于单服务器或不需要实时同步的场景

### session — 玩家会话锁

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `lock-seconds` | Int | `30` | 会话锁持有时间（秒），超时后自动释放 |
| `heartbeat-seconds` | Int | `10` | 玩家会话心跳间隔（秒） |
| `kick-message` | String | `你的跨服会话正在同步中，请稍后重试` | 数据同步时尝试加入的提示 |

会话锁的作用是防止同一玩家在两个服务器上同时被写入数据。当玩家从 Server A 切到 Server B 时，Server A 会持有锁直到数据同步完成。如果 Server B 在锁未释放时尝试写入，玩家会被提示等待。

### node — 节点心跳

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `heartbeat-seconds` | Int | `15` | 节点发送心跳的间隔（秒） |
| `offline-seconds` | Int | `45` | 超过此时间未心跳则标记为离线 |

节点心跳用于集群健康监控。`/crossserver nodes` 命令会显示每个节点的在线/离线状态。

### teleport — 跨服传送

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `handoff-seconds` | Int | `30` | 一次传送流程的最大允许时间（秒） |
| `arrival-check-delay-ticks` | Int | `10` | 到达目标服后延迟多少 tick 检查传送结果 |

#### gateway — 传送网关

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `type` | String | `proxy-plugin-message` | 网关类型（目前仅支持此值） |
| `plugin-message-channel` | String | `BungeeCord` | Plugin Message 通道 |
| `connect-subchannel` | String | `Connect` | BungeeCord Connect 子通道 |
| `server-map` | Map | — | 插件内部 ID 到代理服务器名的映射 |

**server-map 详解：**

```yaml
server-map:
  # key = 插件内部的 server.id
  # value = 代理（Velocity/BungeeCord）中注册的服务器名
  server-1: "server-1"
  server-2: "server-2"
```

两者可以不同。例如：

```yaml
server:
  id: "survival-01"

teleport:
  gateway:
    server-map:
      survival-01: "survival-a"    # 插件ID survival-01 对应代理中的 survival-a
      survival-02: "survival-b"
```

> 如果 `server-map` 缺少某个子服的映射，跨服传送到该子服会失败。
