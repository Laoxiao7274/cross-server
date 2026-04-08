# 安装教程

本文档将手把手教你从零开始部署 CrossServer 插件。

## 前置条件

在开始之前，请确认你已准备好以下环境：

| 组件 | 要求 | 是否必须 |
|------|------|----------|
| Java | 21+ | 必须 |
| 服务端 | Paper 1.21+ | 必须（不支持 Spigot / CraftBukkit） |
| MySQL | 8.0+ | 必须 |
| Redis | 6.0+ | 可选（需要跨服实时广播时必须） |
| 代理 | Velocity / BungeeCord | 需要跨服传送时必须 |
| Vault | 最新版 | 需要经济功能时必须 |

## 第一步：准备 MySQL 数据库

### 安装 MySQL

如果还没有 MySQL，可以使用 Docker 快速启动：

```bash
docker run -d \
  --name cross-server-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=你的密码 \
  -e MYSQL_DATABASE=cross_server \
  --restart unless-stopped \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

### 创建数据库

用你喜欢的 MySQL 客户端（如 Navicat、DataGrip、命令行）执行：

```sql
CREATE DATABASE cross_server CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> 插件启动时会自动创建所有数据表，你只需要准备好数据库即可。

## 第二步：准备 Redis（可选）

Redis 用于跨服之间的实时数据广播。如果只有一台服务器或不需要实时同步，可以跳过。

### 安装 Redis

**Docker 方式（推荐）：**

```bash
docker run -d \
  --name cross-server-redis \
  -p 6379:6379 \
  --restart unless-stopped \
  redis:7
```

**Windows：** 从 [tporadowski/redis](https://github.com/tporadowski/redis/releases) 下载，解压后运行 `redis-server.exe`。

**Linux：**
```bash
# Ubuntu / Debian
sudo apt install redis-server
sudo systemctl start redis

# CentOS / RHEL
sudo yum install redis
sudo systemctl start redis
```

### 验证 Redis

```bash
redis-cli ping
# 应该返回 PONG
```

如果返回 `PONG`，说明 Redis 正常运行。

## 第三步：下载并安装插件

1. 从 Releases 页面下载最新的 `cross-server-x.x-SNAPSHOT.jar`
2. 将 JAR 文件放入**每台**子服的 `plugins/` 目录
3. 确保所有子服使用的插件版本一致

## 第四步：配置代理

### Velocity

在 `velocity.toml` 中注册所有子服：

```toml
[servers]
  server-1 = "127.0.0.1:25566"
  server-2 = "127.0.0.1:25567"
  # 按需添加更多子服

try = ["server-1"]
```

确保开启现代转发（modern forwarding）：

```toml
[player-info-forwarding]
  mode = "modern"
  forwarding-secret = "你的密钥"
```

### BungeeCord

在 `config.yml` 中注册子服：

```yaml
servers:
  server-1:
    address: 127.0.0.1:25566
    restricted: false
  server-2:
    address: 127.0.0.1:25567
    restricted: false
ip_forward: true
```

## 第五步：配置插件

启动服务器后会生成 `plugins/cross-server/config.yml`，然后按以下说明修改。

### Server 1 的配置

```yaml
server:
  id: "server-1"
  cluster: "my-cluster"

database:
  jdbc-url: "jdbc:mysql://MySQL地址:3306/cross_server?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
  username: "数据库用户名"
  password: "数据库密码"
  maximum-pool-size: 10

messaging:
  enabled: true
  redis-uri: "redis://Redis地址:6379/0"
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

modules:
  auth: true
  homes: true
  warps: true
  tpa: true
  route-config: true
  transfer-admin: true
  economy-bridge: true
  permissions: false

web-panel:
  enabled: true
  host: "127.0.0.1"
  port: 8765
  token: "请改成强随机令牌"
  master-server-id: "server-1"
  cluster-lease-seconds: 30
  cluster-heartbeat-seconds: 10
```

### Server 2 的配置

其他配置完全一样，只改 `server.id`：

```yaml
server:
  id: "server-2"

web-panel:
  enabled: true
  master-server-id: "server-1"
```

## 第六步：重启并验证

1. 重启所有子服
2. 在任意子服执行 `/crossserver status` — 确认插件正常加载
3. 执行 `/crossserver nodes` — 确认所有节点在线
4. 测试基本功能：
   - `/sethome test` → `/home test` — 测试跨服家园
   - `/setwarp spawn` → `/warp` → 点击 Warp GUI — 测试 Warp 命令与菜单
   - 对另一台子服在线玩家执行 `/tpa <player>` 或 `/tpahere <player>` — 测试跨服 TPA
   - `/economy balance` — 确认经济系统正常
5. 访问 Web 面板：
   - 浏览器打开 `http://127.0.0.1:8765/`（对应主控节点的 host:port）
   - 输入配置的 `token`，确认面板正常加载
   - 检查"仪表盘"标签页是否显示所有节点
   - 在"节点配置"标签页确认各节点配置快照已上报
6. 验证稳定性：
   - 跨服过程中停服 / 重载，确认不会遗留卡住的 handoff
   - 人为制造目标世界缺失，确认失败后会回滚玩家状态

## 故障排查

### 插件报错 `Communications link failure`

连不上 MySQL。检查：

- `database.jdbc-url` 中的地址、端口、数据库名是否正确
- MySQL 是否已启动
- 防火墙是否放行了 3306 端口
- 数据库 `cross_server` 是否已创建

### 插件报错 `Connection refused`

连不上 Redis。检查：

- `redis-cli ping` 是否返回 PONG
- 如果不需要 Redis，将 `messaging.enabled` 改为 `false`

### 跨服传送卡住

执行 `/crossserver transfer clear <玩家名>` 清理卡住的传送状态。

### 跨服传送后位置不对

检查 `server-map` 的 value 是否和代理中注册的服务器名**完全一致**（大小写敏感）。

### TPA 请求看起来“没反应”

检查：

- 目标玩家是否真的在线且位置快照新鲜
- `teleport.handoff-seconds` 是否过短，导致请求很快过期
- Redis 未开启时，各服对请求更新的感知会更慢

### Warp 列表为空

确认至少有一名管理员执行过 `/setwarp <name>`，并且你有 `crossserver.warps.list` 权限。

### 节点显示离线

- 所有子服是否连接了同一个 MySQL
- 检查各子服的 `server.id` 是否唯一（不能重复）
