# 路由管理

## 共享路由模型

当前插件支持“本地配置 + 数据库共享覆盖层”的路由合并模型：

- 本地 `config.yml` 中的 `teleport.gateway.server-map` 是默认基线
- 数据库共享配置 `cluster.config / teleport.routes` 可以覆盖同名 `serverId` 的路由
- `/crossserver reload` 后重新读取并生效

## 菜单入口

```text
/crossserver route menu
```

## 菜单功能

- 左键现有条目：修改代理目标
- 右键现有条目：删除共享覆盖输入
- `新增路由` 按钮：新增 `serverId -> proxyTarget`
- `重载本节点` 按钮：立即执行本节点 reload
- `刷新` 按钮：重新读取共享路由与本地合并结果

## 聊天输入规则

- 输入新的代理服名：保存共享路由
- 输入 `remove`：删除当前共享覆盖
- 输入 `cancel`：取消编辑
- 新增路由时输入格式：`<serverId> <proxyTarget>`

## 来源显示

菜单中会显示来源：

- `仅本地`
- `仅共享`
- `本地 + 共享覆盖`

## 生效方式

共享路由保存后，需要在节点执行 reload 才会让当前节点立即生效。
