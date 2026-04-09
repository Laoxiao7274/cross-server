# CrossServer

**Paper 1.21+ 跨服数据同步插件** — 背包不丢、经济通用、家园跨服、Warp 共享、TPA 可跨服

> 适用于 Velocity / BungeeCord 代理下的多子服集群，开箱即用，无需手动建表。

## 亮点功能

- **背包 & 末影箱同步** — 切服自动恢复，再也不怕换服丢东西
- **跨服经济** — 兼容 Vault，所有子服共用一个钱包，支持交易流水查询
- **跨服家园** — `/sethome` 设的家在任意子服都能 `/home` 回去，自动跨服传送
- **全局 Warp** — `/warp` 命令 + GUI 菜单，支持同服直传与跨服 handoff
- **跨服 TPA** — `/tpa` `/tpahere` `/tpaccept`，支持对其他子服在线玩家发起请求
- **玩家状态同步** — 血量、饥饿、经验、等级、游戏模式，切服不重置
- **权限同步模块** — 可选同步 `crossserver.*` 权限节点，支持集群统一开关
- **登录认证** — 注册/登录/改密，跨服免重登 Ticket 机制（5 分钟内切服无需重新登录）
- **跨服传送保护** — 停服保护、失败回滚、状态修复，减少卡传送与丢状态
- **模块开关 + 共享配置中心** — 主要功能支持本地默认 + 集群共享覆盖，统一管理后通过 `/crossserver reload` 生效
- **共享路由 + 共享模块配置** — 路由表和模块开关可集中存储到集群配置中心，避免每台子服重复改配置
- **内置 Web 面板主控节点** — 由你指定的主控节点对外提供可交互管理面板，其他开启节点只负责登记成员状态，适合大厅服统一总管
- **节点配置远程管理** — 主控节点可在 Web 面板中查看并修改各子服的 messaging、面板、模块开关配置，写回目标节点 config.yml 并排队重载
- **日志中心** — Web 面板可按节点查看 CrossServer 插件日志，方便集群排障
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

### 第三步：下载插件

从 Releases 页面下载最新的 `cross-server-x.x-SNAPSHOT.jar`，放入每台子服的 `plugins/` 目录。

### 第四步：配置代理（Velocity）

如果你使用 Velocity 代理，在 `velocity.toml` 中确保各子服已注册。

### 第五步：配置插件

启动服务器后会生成 `plugins/cross-server/config.yml`，按以下说明修改关键配置：

```yaml
server:
  id: "lobby"                    # 当前节点唯一标识，集群内不能重复
  cluster: "my-cluster"          # 集群名称，同一集群子服保持一致

database:
  jdbc-url: "jdbc:mysql://MySQL地址:3306/cross_server?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
  username: "数据库用户名"
  password: "数据库密码"

messaging:
  enabled: true
  redis-uri: "redis://Redis地址:6379/0"
  channel: "cross-server:sync"

teleport:
  gateway:
    server-map:
      lobby: "lobby"             # 本服 ID → 代理中注册的服务器名
      survival: "survival"

modules:
  auth: true
  homes: true
  warps: true
  tpa: true
  economy-bridge: true
  permissions: false

web-panel:
  enabled: true
  host: "127.0.0.1"
  port: 8765
  token: "请改成强随机令牌"
  master-server-id: "lobby"      # 指定主控节点（通常填大厅服）
  cluster-lease-seconds: 30
  cluster-heartbeat-seconds: 10
```

> 每台子服的 `server.id` 和 `server-map` 需要对应修改。完整配置项说明见 [配置指南](docs/setup/config.md)。

### 第六步：Web 面板集群行为

只要**有一个或多个子服开启 `web-panel.enabled: true`**：

- 插件会把这些节点登记到共享配置里
- 只有 `web-panel.master-server-id` 指定的节点会真正监听 `host:port`
- 其他开启节点只负责上报成员状态，不会重复绑定端口
- 节点关闭时会主动注销自己的面板成员记录

推荐做法：

- 把 `master-server-id` 固定为大厅服，例如 `lobby`
- 所有开启面板的节点使用同一组 token
- 只有主控节点需要对外暴露面板端口，其他节点只参与集群登记

### 第七步：访问面板

浏览器访问主控节点的 `http://host:port/` 后，会进入一个内置的多页面管理面板。

面板会先要求你输入：

```http
X-CrossServer-Token: 你的token
X-CrossServer-Actor: 操作者（可选）
```

- `X-CrossServer-Token` 用于所有 API 请求鉴权
- `X-CrossServer-Actor` 不传时默认记为 `web-panel`
- 页面会把你输入的 token / actor 保存在浏览器本地，便于下次继续使用

当前页面支持：

- 查看集群状态、节点列表、Web 面板主控节点与成员信息
- 查看并修改共享模块开关（local / shared / effective）
- 查看并修改共享路由（来源标签与游戏内路由菜单一致）
- 查看并编辑已注册配置文档的 JSON / YAML payload、schema/source/summary
- 节点配置管理：查看各节点配置快照，在线编辑 messaging / webPanel / modules 白名单字段并提交到目标节点
- 日志中心：按节点查看 CrossServer 插件同步日志
- 查看 recent transfer，并按玩家名查询转服诊断
- 在网页内请求重载当前节点，并在短暂断开后自动尝试重连

> 注意：在网页里保存共享模块开关或共享路由后，只是写入共享配置中心；当前节点通常仍需触发一次本节点 reload 才会立即生效。
> 
> 现在可用的 reload 入口已经统一：`/crossserver reload`、路由菜单里的“重载本节点”、网页里的“重载本节点”都会走同一套安全排队逻辑。
> 
> 这仍然是一次完整的 CrossServer 重载：若从网页触发，面板会短暂断开，然后自动尝试恢复连接。

接口：

只读：

- `/`
- `GET /api/overview`
- `GET /api/status`
- `GET /api/modules`
- `GET /api/routes`
- `GET /api/config-documents`
- `GET /api/logs`
- `GET /api/node-configs`
- `GET /api/node-configs/detail?serverId=<节点ID>`
- `GET /api/transfers/recent`
- `GET /api/transfers/player?player=<玩家名>`

可写：

- `PUT /api/modules`
- `PUT /api/routes`
- `PUT /api/config-documents`
- `POST /api/routes`
- `DELETE /api/routes?serverId=<子服ID>`
- `POST /api/node-configs/apply`

示例：

```json
{
  "serverId": "spawn",
  "proxyTarget": "lobby"
}
```

节点配置应用：

```json
{
  "serverId": "survival-1",
  "changes": {
    "messaging": {
      "enabled": true,
      "redisUri": "redis://127.0.0.1:6379/0",
      "channel": "crossserver"
    },
    "webPanel": {
      "enabled": true,
      "host": "127.0.0.1",
      "port": 8765,
      "masterServerId": "lobby"
    },
    "modules": {
      "auth": true,
      "homes": true,
      "warps": true,
      "tpa": true,
      "routeConfig": true,
      "transferAdmin": true,
      "economyBridge": true,
      "permissions": false
    }
  }
}
```

配置文档保存：

```yaml
namespace: my-plugin.config
dataKey: main
schemaVersion: 2
source: web-panel
summary: 调整默认配置
payload: |
  enabled: true
  maxHomes: 5
```

## 自动构建与发版

仓库已经配置好 GitHub Actions：

- 推送到 `master`：自动执行 `mvn clean package`
- 推送 tag（如 `v1.0.2`）：自动构建 JAR、创建 GitHub Release、上传发布产物
- 推送文档改动到 `master`：自动执行 `mkdocs build` 并部署文档站点

推荐发版流程：

1. 把 `pom.xml` 的版本号提升一个小版本，例如 `1.0.2`
2. 推送 `master`
3. 创建并推送同版本 tag，例如 `v1.0.2`
