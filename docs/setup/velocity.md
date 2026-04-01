# Velocity 代理

## 先说结论

如果你要使用当前插件的跨服传送能力（包括跨服 `/home`），前面必须接 Velocity / BungeeCord / Waterfall 这类代理。

当前插件的跨服切服方式是：

- 子服通过 Bukkit Plugin Message
- 通道：`BungeeCord`
- 子通道：`Connect`
- 把玩家从当前 Paper 子服切到代理配置里的另一个后端服

## 推荐结构

```text
玩家
  -> Velocity
      -> server-1 (Paper)
      -> server-2 (Paper)
```

## 关键要求

1. 所有玩家都先进入 Velocity，再由 Velocity 转发到子服
2. 所有子服共用同一个 MySQL
3. 如启用广播，所有子服共用同一个 Redis
4. 每台子服的 `server.id` 必须唯一
5. `teleport.gateway.server-map` 的 value 必须与 Velocity 中的后端服务器名一致

## Velocity 后端示例

```toml
[servers]
server-1 = "127.0.0.1:25566"
server-2 = "127.0.0.1:25567"
try = ["server-1"]
```

## 子服配置示例

### server-1

```yml
server:
  id: "server-1"
  cluster: "qingchuan"

teleport:
  gateway:
    type: "proxy-plugin-message"
    plugin-message-channel: "BungeeCord"
    connect-subchannel: "Connect"
    server-map:
      server-1: "server-1"
      server-2: "server-2"
```

### server-2

```yml
server:
  id: "server-2"
  cluster: "qingchuan"

teleport:
  gateway:
    type: "proxy-plugin-message"
    plugin-message-channel: "BungeeCord"
    connect-subchannel: "Connect"
    server-map:
      server-1: "server-1"
      server-2: "server-2"
```

## server-map 含义

```text
插件内部目标服 ID -> Velocity 后端服名
```

例如：

```yml
server-map:
  server-1: "server-1"
  server-2: "server-2"
```

如果 `server.id` 与 Velocity 后端名不同，也没问题，只要映射正确：

```yml
server:
  id: "paper-survival-01"

teleport:
  gateway:
    server-map:
      paper-survival-01: "survival-a"
      paper-survival-02: "survival-b"
```

## 安全转发

至少要确保：

- Velocity 开启现代转发（modern forwarding）
- 配好 forwarding secret
- Paper 子服按 Velocity 要求开启代理支持
- 子服不要允许玩家绕过代理直连

## 推荐排查顺序

如果跨服 `/home` 不生效，按这个顺序查：

1. 玩家是不是通过 Velocity 进服，而不是直连 Paper
2. Velocity 的后端服务器名是否与 `server-map.value` 一致
3. 每台子服的 `server.id` 是否唯一
4. 所有子服是否连接同一个 MySQL
5. 所有子服是否连接同一个 Redis（如果启用 messaging）
6. 目标服 world 名是否存在
7. `/crossserver nodes` 是否能看到所有节点在线
8. `/crossserver transfer info <player>` 是否能看到 handoff 状态推进
