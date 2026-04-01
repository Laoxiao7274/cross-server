# 路由管理

CrossServer 支持"本地配置 + 数据库共享覆盖层"的路由合并模型，方便运维人员在不修改子服配置文件的情况下管理跨服传送路由。

## 路由合并规则

1. **本地基线**：`config.yml` 中的 `teleport.gateway.server-map` 是默认路由
2. **共享覆盖**：数据库中存储的共享路由可以覆盖同名 key
3. **优先级**：共享路由 > 本地配置
4. **生效方式**：共享路由保存后需要执行 `/crossserver reload` 才会在当前节点生效

## GUI 菜单管理

执行 `/crossserver route menu` 打开路由配置菜单。

### 菜单操作

| 操作 | 方式 | 效果 |
|------|------|------|
| 修改路由 | 左键已有条目 | 进入聊天输入模式，输入新的代理目标服务器名 |
| 删除共享覆盖 | 右键已有条目 | 删除该路由的共享覆盖（恢复本地配置） |
| 新增路由 | 点击 `新增路由` 按钮 | 输入 `serverId proxyTarget` 格式 |
| 立即生效 | 点击 `重载本节点` 按钮 | 执行 reload，使修改立即生效 |
| 刷新列表 | 点击 `刷新` 按钮 | 重新读取数据库并合并显示 |

### 聊天输入规则

进入编辑模式后，在聊天栏输入：

- **新的代理服务器名**：保存修改
- **`remove`**：删除当前共享覆盖
- **`cancel`**：取消编辑

### 来源显示

菜单中每条路由会显示其来源：

- `仅本地` — 路由只存在于本地 config.yml
- `仅共享` — 路由只存在于数据库共享配置
- `本地 + 共享覆盖` — 本地有配置，但被数据库共享配置覆盖了

## 命令管理

```bash
# 查看当前生效的路由表
/crossserver route list

# 设置路由：server-3 映射到代理中的 backend-3
/crossserver route set server-3 backend-3

# 删除路由
/crossserver route remove server-3

# 使修改生效
/crossserver reload
```

## 使用场景

### 场景 1：动态添加子服

新增一台子服 server-3 后，不想手动修改所有子服的 `config.yml`：

```bash
/crossserver route set server-3 server-3
/crossserver reload
```

所有子服执行 reload 后即可路由到 server-3。

### 场景 2：临时下线子服

将某个子服的路由指向一个不存在的目标，或直接删除：

```bash
/crossserver route remove server-3
/crossserver reload
```

传送到该子服的请求将失败，但不会卡住（会显示错误提示）。

### 场景 3：子服迁移

子服 server-3 迁移到新机器后，代理名变为 server-3-new：

```bash
/crossserver route set server-3 server-3-new
/crossserver reload
```

> 注意：共享路由存储在数据库的 `global_snapshot` 表中，命名空间 `cluster.config`，数据键 `teleport.routes`。重启所有子服后共享路由自动生效，无需手动操作。
