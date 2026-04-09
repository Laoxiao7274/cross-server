# 命令与权限

## 管理员命令

### 内置 Web 面板

当前版本没有新增游戏内命令，面板由配置指定的主控节点统一承载。

**行为：**

- 开启 `web-panel.enabled: true` 的节点，都可参与 Web 面板集群登记
- 只有 `web-panel.master-server-id` 对应节点会真正监听 HTTP 端口
- 其他开启节点会作为受管节点登记到集群视图中
- 访问 `/` 会进入一个可交互多页面管理面板

**页面能力：**

- 查看集群状态、节点列表、Web 面板主控节点信息
- 查看并修改共享模块开关（local / shared / effective）
- 查看并修改共享路由（仅本地 / 仅共享 / 本地 + 共享覆盖）
- 查看并编辑已注册配置文档与 JSON / YAML payload
- 节点配置管理：查看各节点配置快照，在线编辑 messaging / webPanel / modules 并提交到目标节点
- 日志中心：按节点查看 CrossServer 插件同步日志
- 查看 recent transfer，并按玩家名查询转服诊断
- 在网页内请求重载当前节点，面板断开后自动尝试重连

**注意：**

- 页面写入的是共享配置中心，不等于当前节点立即热生效
- 修改共享模块开关或共享路由后，当前节点通常仍需触发一次本节点 reload
- `/crossserver reload`、路由菜单里的"重载本节点"、网页内的"重载本节点"现在都复用同一套安全排队式 reload 逻辑
- 网页触发 reload 时，页面会短暂断开后自动尝试重连

**接口：**

只读：

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

- `PUT /api/modules` — 覆盖共享模块开关
- `PUT /api/routes` — 整体覆盖共享路由表
- `PUT /api/config-documents` — 保存配置文档（支持 JSON / YAML payload）
- `POST /api/routes` — 新增或更新单条共享路由
- `DELETE /api/routes?serverId=<子服ID>` — 删除单条共享路由
- `POST /api/node-configs/apply` — 提交节点配置变更申请

**认证：**

```http
X-CrossServer-Token: 你的token
X-CrossServer-Actor: 操作者（可选）
```

不传 `X-CrossServer-Actor` 时，默认记为 `web-panel`。

**请求体示例：**

更新模块开关：

```json
{
  "auth": true,
  "homes": true,
  "warps": true,
  "tpa": true,
  "routeConfig": true,
  "transferAdmin": true,
  "economyBridge": true,
  "permissions": true
}
```

整体覆盖路由表：

```json
{
  "routes": {
    "survival": "survival",
    "spawn": "lobby"
  }
}
```

新增或更新单条路由：

```json
{
  "serverId": "spawn",
  "proxyTarget": "lobby"
}
```

提交节点配置变更：

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

保存配置文档：

```json
{
  "namespace": "my-plugin.config",
  "dataKey": "main",
  "schemaVersion": 2,
  "source": "web-panel",
  "summary": "调整默认配置",
  "payload": "{\n  \"enabled\": true,\n  \"maxHomes\": 5\n}"
}
```
