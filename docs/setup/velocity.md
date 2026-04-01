# Velocity 代理配置

CrossServer 的跨服传送依赖 Velocity / BungeeCord / Waterfall 代理。本文档说明如何配置代理以实现跨服 `/home`、跨服传送等功能。

## 原理

跨服传送的工作流程：

1. 玩家在 Server A 执行 `/home base`（base 在 Server B）
2. 插件通过 Bukkit Plugin Message 告诉代理：把玩家切到 Server B
3. 代理将玩家移动到 Server B
4. Server B 接收传送数据，将玩家传送到目标坐标

使用的协议：通道 `BungeeCord`，子通道 `Connect`。

## 网络结构

```
玩家
  └─> Velocity (代理端口 25565)
        ├─> server-1 (Paper, 127.0.0.1:25566)
        └─> server-2 (Paper, 127.0.0.1:25567)
```

## Velocity 配置

### velocity.toml

```toml
bind = "0.0.0.0:25565"

[servers]
  server-1 = "127.0.0.1:25566"
  server-2 = "127.0.0.1:25567"

try = ["server-1"]

[player-info-forwarding]
  mode = "modern"
  forwarding-secret = "你的随机密钥"
```

### Paper 子服配置

每台 Paper 子服的 `config/paper-global.yml`：

```yaml
proxies:
  bungee-cord:
    online-mode: false          # Velocity 处理在线验证
  velocity:
    enabled: true
    online-mode: true
    secret: "你的随机密钥"      # 必须和 velocity.toml 中的 forwarding-secret 一致
```

> `forwarding-secret` / `secret` 两边必须完全一致，否则玩家数据转发会失败。

## BungeeCord 配置

如果你使用 BungeeCord 代替 Velocity：

### config.yml

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

Paper 子服的 `config/paper-global.yml`：

```yaml
proxies:
  bungee-cord:
    online-mode: true
  velocity:
    enabled: false
```

## server-map 映射

`server-map` 的 value 必须与代理中注册的服务器名**完全一致**（区分大小写）。

### 示例 1：ID 和代理名相同

```yaml
# 两台子服的 config.yml 中都包含：
server-map:
  server-1: "server-1"
  server-2: "server-2"
```

### 示例 2：ID 和代理名不同

```yaml
# Server A (server.id: survival-01)
server-map:
  survival-01: "survival-a"
  survival-02: "survival-b"

# Server B (server.id: survival-02)
server-map:
  survival-01: "survival-a"
  survival-02: "survival-b"
```

> 所有子服的 `server-map` 配置应该**完全一致**，包含集群中所有子服的映射。

## 安全注意事项

- Velocity 应开启 `online-mode: true` 进行正版验证
- `forwarding-secret` 应使用随机生成的强密钥，不要用默认值
- Paper 子服不应暴露直连端口给玩家，确保玩家只能通过代理进入
- 防火墙只放行代理端口（25565），子服端口仅允许代理服务器 IP 访问

## 故障排查

如果跨服 `/home` 不生效，按以下顺序排查：

| 步骤 | 检查项 | 命令/方法 |
|------|--------|-----------|
| 1 | 玩家是否通过代理进服 | 检查子服是否开放直连端口给玩家 |
| 2 | server-map 是否正确 | 对比 `server-map` 的 value 和代理配置中的服务器名 |
| 3 | server.id 是否唯一 | 检查每台子服的 `server.id` |
| 4 | MySQL 是否共享 | 所有子服必须连接同一个 MySQL |
| 5 | Redis 是否共享 | 如果 `messaging.enabled: true`，所有子服必须连接同一个 Redis |
| 6 | 目标 world 是否存在 | 目标服必须有 home 所在的 world |
| 7 | 节点是否在线 | `/crossserver nodes` 查看 |
| 8 | 传送状态 | `/crossserver transfer info <玩家>` 查看 handoff 状态 |
| 9 | forwarding-secret 是否一致 | 对比 Velocity 和 Paper 配置中的密钥 |
