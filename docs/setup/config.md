# 配置指南

配置文件位置：`plugins/cross-server/config.yml`

## 完整配置示例

```yaml
web-panel:
  enabled: false
  host: "127.0.0.1"
  port: 8765
  token: "change-this-token"
  master-server-id: "server-1"
  cluster-lease-seconds: 30
  cluster-heartbeat-seconds: 10
```

## 配置项详解

### web-panel — 内置面板主控节点

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | Boolean | `false` | 是否让当前节点加入 Web 面板集群登记 |
| `host` | String | `127.0.0.1` | 当前节点如果就是主控节点时绑定的地址 |
| `port` | Int | `8765` | 当前节点如果就是主控节点时监听的端口 |
| `token` | String | `change-this-token` | 访问令牌，请务必改成强随机值 |
| `master-server-id` | String | 当前 `server.id` | 指定哪个节点是 Web 面板主控节点，通常填大厅服 |
| `cluster-lease-seconds` | Int | `30` | 面板成员登记过期时间，超过后会从集群视图中剔除 |
| `cluster-heartbeat-seconds` | Int | `10` | 面板成员向共享配置中心刷新自身状态的间隔 |

**集群行为：**

- 开启 `web-panel.enabled: true` 的节点会把自己登记到共享配置中心
- 只有 `web-panel.master-server-id` 对应的节点会真正监听 HTTP 端口
- 其他已开启节点会作为受管节点登记到面板集群中，供主控面板展示
- 节点正常关闭时会主动注销自己的面板成员记录
- 若主控节点未开启或离线，面板页面不会由其他节点自动接管，需由配置指定的主控节点恢复服务

**建议：**

- 把 `master-server-id` 固定为大厅服或专门的管理服，作为集群总管
- 所有开启面板的节点应保持同一组 `token`
- 推荐仍然绑定 `127.0.0.1`，由反向代理统一暴露
- Web 面板已支持可交互网页，可直接查看主控节点、成员状态、模块、路由、配置文档、节点配置、日志中心与转服诊断
- Web 面板现在也支持在网页内请求重载当前节点，重载期间页面会短暂断开并自动尝试重连
- 保存共享模块开关或共享路由后，当前节点通常仍需触发一次本节点 reload 才会立即生效
- `/crossserver reload`、路由菜单里的"重载本节点"、网页内的"重载本节点"现在都使用同一套安全排队式 reload 逻辑

### 节点配置远程管理

主控节点可在 Web 面板的"节点配置"标签页中，查看各子服上报的配置快照，并在线修改以下白名单字段：

| 分类 | 可编辑字段 | 说明 |
|------|-----------|------|
| `messaging` | `enabled`, `redisUri`, `channel` | Redis 消息层配置 |
| `webPanel` | `enabled`, `host`, `port`, `masterServerId`, `token` | Web 面板配置（token 留空表示不修改） |
| `modules.*` | `auth`, `homes`, `warps`, `tpa`, `routeConfig`, `transferAdmin`, `economyBridge`, `permissions` | 各模块开关 |

**工作流程：**

1. 目标节点启动后自动发布配置快照到集群配置中心（命名空间 `node.config`）
2. 主控节点在面板中查看快照并编辑白名单字段
3. 主控节点提交"申请"到目标节点
4. 目标节点收到申请后，将变更写入本地 `config.yml` 并排队重载

**安全限制：**

- 只能修改白名单内的字段，不会意外覆盖数据库连接、代理配置等敏感信息
- 每次申请只影响一个目标节点
- 申请状态可在面板中实时追踪（pending -> applying -> applied / failed）

### 日志中心

Web 面板的"日志中心"标签页可按节点查看 CrossServer 插件同步到配置中心的日志，便于集群排障。日志通过共享配置中心的 `web.panel.log` 命名空间同步，主控和受管节点均可参与。
