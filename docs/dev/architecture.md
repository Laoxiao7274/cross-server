# 架构概览

## 核心能力

- 玩家快照同步
- 全局快照同步
- 节点状态心跳
- 跨服传送 handoff
- 登录认证与 ticket
- 共享路由表覆盖

## 数据表

插件会自动创建这些表：

- `player_session`
- `player_snapshot`
- `global_snapshot`
- `player_identity`
- `economy_transaction`
- `node_status`

## 重要命名空间

- `economy`
- `inventory`
- `enderchest`
- `player-state`
- `homes`
- `auth.profile`
- `auth.ticket`
- `teleport.handoff`
- `cluster.config / teleport.routes`

## 关键模块

- `CrossServerApi`：对外服务入口
- `SyncService`：同步与失效广播
- `SessionService`：玩家会话锁与 baton-pass
- `RouteTableService`：共享路由读写与本地合并
- `NodeStatusService`：节点心跳与节点状态查询
- `CrossServerTeleportService`：跨服传送 handoff 流程

## 其他插件接入

推荐方式：

1. 通过 Bukkit `ServicesManager` 获取 `CrossServerApi`
2. 注册自己的命名空间
3. 通过 `CrossServerApi` 读取或保存数据
4. 如有需要，为指定命名空间注册同步监听器

## 验证建议

### 单服验证

1. 启动服务器
2. 执行 `/crossserver status`
3. 执行 `/sethome main`
4. 执行 `/homes` 和 `/home main`
5. 测试注册、登录、economy、reload

### 多服验证

1. 两台子服连接同一个 MySQL 和 Redis
2. `/crossserver nodes` 查看节点状态
3. 跨服 homes / transfer / auth / economy 验证
4. 修改共享路由后 reload 并再次测试跨服传送
