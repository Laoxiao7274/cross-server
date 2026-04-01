# CrossServer

**Paper 1.21+ 跨服数据同步插件** — 背包不丢、经济通用、家园跨服、Warp 共享、TPA 可跨服

> 适用于 Velocity / BungeeCord 代理下的多子服集群，开箱即用，无需手动建表。

## 亮点功能

- **背包 & 末影箱同步** — 切服自动恢复，再也不怕换服丢东西
- **跨服经济** — 兼容 Vault，所有子服共用一个钱包，支持交易流水查询
- **跨服家园** — `/sethome` 设的家在任意子服都能 `/home` 回去，自动跨服传送
- **全局 Warp** — `/warp` 命令 + GUI 菜单，支持同服直传与跨服 handoff
- **跨服 TPA** — `/tpa` `/tpahere` `/tpaccept`，支持对其他子服在线玩家发起请求
- **玩家状态同步** — 血量、饥饿、经验、等级，切服不重置
- **登录认证** — 注册/登录/改密，跨服免重登 Ticket 机制（5 分钟内切服无需重新登录）
- **跨服传送保护** — 停服保护、失败回滚、状态修复，减少卡传送与丢状态
- **节点监控** — 集群节点心跳，在线/离线状态一目了然
- **热重载** — `/crossserver reload` 不重启服务器即可生效
- **开放 API** — 其他插件可直接接入，注册命名空间、读写数据、监听同步事件

## 环境要求

| 组件 | 要求 | 说明 |
|------|------|------|
| Java | 21+ | Paper 1.21 必须 |
| 服务端 | Paper 1.21+ | 不支持 Spigot / CraftBukkit |
| MySQL | 8.0+ | 必需，存储所有跨服数据 |
| Redis | 6.0+ | 可选，开启后实现跨服实时广播通知 |
| 代理 | Velocity / BungeeCord | 需要跨服传送时必须 |

## 安装教程

### 第一步：准备 MySQL 数据库

在你的 MySQL 服务器上创建一个数据库：

```sql
CREATE DATABASE cross_server CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 插件启动时会**自动创建所有数据表**，无需手动建表。

### 第二步：准备 Redis（可选）

Redis 用于跨服之间的实时数据广播。如果你只有一台服务器或者不需要实时同步通知，可以跳过此步。

**安装 Redis：**

- **Windows**：[下载地址](https://github.com/tporadowski/redis/releases)，解压后运行 `redis-server.exe`
- **Linux**：`apt install redis-server` 或 `yum install redis`，然后 `systemctl start redis`
- **Docker**：`docker run -d -p 6379:6379 redis:7`

**验证 Redis 正常运行：**
```bash
redis-cli ping
# 应返回 PONG
```

### 第三步：下载插件

从 Releases 页面下载最新的 `cross-server-x.x-SNAPSHOT.jar`，放入每台子服的 `plugins/` 目录。

### 第四步：配置代理（Velocity）

如果你使用 Velocity 代理，在 `velocity.toml` 中确保各子服已注册：

```toml
[servers]
  server-1 = "127.0.0.1:25566"
  server-2 = "127.0.0.1:25567"
```

如果你使用 BungeeCord，在 `config.yml` 中同样注册各子服。

### 第五步：配置插件

启动服务器后会生成 `plugins/cross-server/config.yml`，按以下说明修改：

```yaml
server:
  id: "server-1"
  cluster: "my-cluster"

database:
  jdbc-url: "jdbc:mysql://你的MySQL地址:3306/cross_server?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
  username: "数据库用户名"
  password: "数据库密码"
  maximum-pool-size: 10

messaging:
  enabled: true
  redis-uri: "redis://你的Redis地址:6379/0"
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
  cooldown-seconds: 10
  gateway:
    type: "proxy-plugin-message"
    plugin-message-channel: "BungeeCord"
    connect-subchannel: "Connect"
    server-map:
      server-1: "server-1"
      server-2: "server-2"
```

### 第六步：重启服务器

配置完成后重启所有子服，插件会自动建表并开始工作。可以使用 `/crossserver status` 确认插件状态，`/crossserver nodes` 确认各节点是否在线。

## 命令列表

### 玩家命令

| 命令 | 说明 |
|------|------|
| `/home [名称]` | 传送到家园（可跨服） |
| `/homes` | 打开家园管理菜单 |
| `/warp [名称]` | 直接传送到 Warp，或打开 Warp GUI |
| `/setwarp <名称>` | 设置全局 Warp |
| `/delwarp <名称>` | 删除全局 Warp |
| `/tpa <玩家>` | 请求传送到目标玩家 |
| `/tpahere <玩家>` | 邀请目标玩家传送到你这里 |
| `/tpaccept [玩家]` | 接受最近一条或指定玩家的请求 |
| `/tpdeny [玩家]` | 拒绝最近一条或指定玩家的请求 |
| `/tpcancel` | 取消你发出的所有请求 |
| `/login <密码>` | 登录（简写 `/l`） |
| `/register <密码> <确认>` | 注册（简写 `/reg`） |
| `/changepassword <旧密码> <新密码>` | 修改密码 |
| `/economy balance` | 查看余额 |
| `/economy history` | 查看交易记录 |

### 管理员命令

| 命令 | 说明 |
|------|------|
| `/crossserver status` | 查看插件运行状态 |
| `/crossserver nodes` | 查看集群所有节点 |
| `/crossserver node <serverId>` | 查看指定节点详情 |
| `/crossserver transfer info <玩家>` | 查看玩家跨服传送状态 |
| `/crossserver transfer menu <玩家>` | 打开传送管理 GUI 菜单 |
| `/crossserver transfer history <玩家>` | 查看玩家传送历史 |
| `/crossserver transfer recent` | 查看最近传送事件 |
| `/crossserver transfer clear <玩家>` | 清理卡住的传送 |
| `/crossserver transfer reconcile <玩家>` | 保守修复传送状态 |
| `/crossserver auth inspect <玩家>` | 查看认证信息 |
| `/crossserver auth invalidate <玩家>` | 作废免重登票据 |
| `/crossserver route list` | 查看共享路由表 |
| `/crossserver route menu` | 打开路由配置 GUI |
| `/crossserver route set <key> <value>` | 设置路由 |
| `/crossserver route remove <key>` | 删除路由 |
| `/economy set <玩家> <金额>` | 设置余额 |
| `/economy deposit <玩家> <金额>` | 增加余额 |
| `/economy withdraw <玩家> <金额>` | 扣除余额 |
| `/crossserver reload` | 热重载配置 |

## 权限节点

| 权限 | 默认 | 说明 |
|------|------|------|
| `crossserver.homes.*` | 所有玩家 | 家园相关命令 |
| `crossserver.warps.list` | 所有玩家 | 查看 Warp 列表 / 打开 GUI |
| `crossserver.warps.teleport` | 所有玩家 | 传送到 Warp |
| `crossserver.warps.set` | OP | 设置 Warp |
| `crossserver.warps.delete` | OP | 删除 Warp |
| `crossserver.tpa.use` | 所有玩家 | `/tpa` `/tpaccept` `/tpdeny` `/tpcancel` |
| `crossserver.tpa.here` | 所有玩家 | `/tpahere` |
| `crossserver.auth.*` | 所有玩家 | 登录/注册/改密 |
| `crossserver.economy.balance` | OP | 查看余额 |
| `crossserver.economy.set` | OP | 设置余额 |
| `crossserver.economy.deposit` | OP | 增加余额 |
| `crossserver.economy.withdraw` | OP | 扣除余额 |
| `crossserver.economy.history` | OP | 交易流水 |
| `crossserver.status.view` | OP | 查看状态 |
| `crossserver.nodes.view` | OP | 查看节点 |
| `crossserver.transfer.*` | OP | 传送管理 |
| `crossserver.route.*` | OP | 路由管理 |
| `crossserver.reload` | OP | 热重载 |

## 稳定性说明

- 跨服传送失败时会自动回滚 inventory、ender chest 与 player-state
- 插件关闭时会收口正在进行中的 handoff，避免遗留 prepared session
- Warp 与 TPA 的跨服分支都复用同一条 handoff 主链路
- TPA 请求支持聊天消息、Title、ActionBar 与音效反馈

## 常见问题

### 插件报错 `Communications link failure`（连不上数据库）

检查 `config.yml` 中的 `database.jdbc-url` 是否正确。常见原因：
- MySQL 地址/端口写错
- MySQL 没启动
- 防火墙阻止了连接
- 数据库名 `cross_server` 不存在（需要先手动 `CREATE DATABASE`）

### 插件报错 `Connection refused`（连不上 Redis）

如果 `messaging.enabled: true` 但 Redis 没装或没启动，会报此错误。
- 如果你不需要跨服广播，直接改成 `messaging.enabled: false`
- 如果需要，先启动 Redis 并确认 `redis-cli ping` 返回 PONG

### 跨服传送卡住了

使用 `/crossserver transfer clear <玩家名>` 清理卡住的传送记录，然后让玩家重新尝试。

### TPA 请求没反应

检查：
- 目标玩家是否真的在线
- `teleport.handoff-seconds` 是否过短，导致请求很快过期
- Redis 未开启时，各服对请求状态更新的感知会更慢

### Warp 菜单是空的

确认至少执行过一次 `/setwarp <name>`，并且你拥有 `crossserver.warps.list` 权限。

### 家园菜单是空的

家园数据按玩家 UUID 存储，如果刚安装插件，需要玩家先 `/sethome` 设置家园。

### 经济命令不生效

经济功能依赖 Vault，确保服务器安装了 [Vault](https://www.spigotmc.org/resources/vault.34315/) 插件。

## 其他插件开发者接入

通过 Bukkit ServicesManager 获取 API 实例：

```java
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.bukkit.Bukkit;

CrossServerApi api = Bukkit.getServicesManager().load(CrossServerApi.class);

api.registerNamespace("my-plugin");
api.savePlayerData(player.getUniqueId(), "my-plugin", "{\"level\": 5}");
api.saveGlobalData("my-plugin", "config", "{\"enabled\": true}");
```
