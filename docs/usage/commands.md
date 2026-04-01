# 命令与权限

## crossserver

```text
/crossserver help                         # 显示此帮助
/crossserver status                       # 查看当前节点与同步状态
/crossserver nodes [page]                 # 分页查看节点列表
/crossserver node <serverId>              # 查看单个节点详情
```

### 权限

| 权限节点 | 默认 | 说明 |
|---|---|---|
| `crossserver.command` | op | 使用 `/crossserver` 的基础权限 |
| `crossserver.status.view` | op | 查看节点同步状态 |
| `crossserver.nodes.view` | op | 查看分页节点列表 |
| `crossserver.node.view` | op | 查看单个节点详情 |

## transfer 传送管理

```text
/crossserver transfer help                # 查看 transfer 帮助
/crossserver transfer info <player>       # 查看玩家传送诊断详情
/crossserver transfer <player>            # 按玩家名快速查看传送状态
/crossserver transfer history <player>    # 查看玩家最近传送历史
/crossserver transfer menu <player>       # 打开指定玩家的传送管理菜单
/crossserver transfer recent [page]       # 打开最近传送管理菜单
/crossserver transfer reconcile <player>  # 触发一次保守修复
/crossserver transfer clear <player>      # 清理玩家的 handoff 与传送状态
```

### 权限

| 权限节点 | 默认 | 说明 |
|---|---|---|
| `crossserver.transfer.view` | op | 查看传送诊断/历史 |
| `crossserver.transfer.clear` | op | 清理传送状态 |
| `crossserver.transfer.menu` | op | 打开传送管理菜单 |
| `crossserver.transfer.reconcile` | op | 触发传送修复 |

## auth 认证管理

### 管理命令

```text
/crossserver auth inspect <player>        # 查看玩家认证运行时与快照状态
/crossserver auth invalidate <player>     # 使玩家跨服 ticket 失效
/crossserver auth forcereauth <player>    # 强制玩家重新认证
```

### 玩家命令

```text
/login <password>                         # 登录
/l <password>                             # 登录（缩写）
/register <password> <confirm>            # 注册并创建跨服登录账号
/reg <password> <confirm>                 # 注册（缩写）
/changepassword <old> <new>               # 修改跨服登录密码
```

### 说明

- 玩家可通过命令登录，也可直接在聊天栏输入密码完成登录
- 登录期间会显示 Title、ActionBar、BossBar
- 登录成功后会生成短时 ticket，用于跨服免重登
- 未认证阶段会限制移动、交互、背包、丢弃/拾取、非白名单命令等行为

### 权限

| 权限节点 | 默认 | 说明 |
|---|---|---|
| `crossserver.auth.admin` | — | 认证管理命令（inspect / invalidate / forcereauth） |
| `crossserver.auth.login` | true | 使用 `/login` |
| `crossserver.auth.register` | true | 使用 `/register` |
| `crossserver.auth.changepassword` | true | 使用 `/changepassword` |

## economy 经济

```text
/economy balance <player>                 # 查看玩家余额
/economy set <player> <amount>            # 设置玩家余额
/economy deposit <player> <amount>        # 存入
/economy withdraw <player> <amount>       # 取出
/economy history <player> [limit]         # 查看交易记录
```

### 权限

| 权限节点 | 默认 | 说明 |
|---|---|---|
| `crossserver.economy.balance` | op | 查看余额 |
| `crossserver.economy.history` | op | 查看交易记录 |
| `crossserver.economy.set` | op | 设置余额 |
| `crossserver.economy.deposit` | op | 存入 |
| `crossserver.economy.withdraw` | op | 取出 |

## homes 家园

```text
/home [name]                              # 传送到默认家园或指定名称
/homes                                    # 查看家园列表（打开 UI 菜单）
/sethome <name>                           # 设置当前位置为家园
/delhome <name>                           # 删除家园
/setdefaulthome <name>                    # 设置默认家园
```

### 说明

- `/homes` 会打开 homes 菜单 UI
- `homes` 数据跨服共享
- 当前服务器上的 home 仍然直接本地传送
- 如果目标 home 位于其他服务器，会进入 `teleport.handoff` 准备流程

### 权限

| 权限节点 | 默认 | 说明 |
|---|---|---|
| `crossserver.homes.list` | true | 使用 `/homes` |
| `crossserver.homes.menu` | true | 打开家园 UI 菜单 |
| `crossserver.homes.teleport` | true | 使用 `/home` 传送 |
| `crossserver.homes.set` | true | 使用 `/sethome` |
| `crossserver.homes.delete` | true | 使用 `/delhome` |
| `crossserver.homes.default` | true | 使用 `/setdefaulthome` |

## reload 热重载

```text
/crossserver reload                       # 热重载配置与内部服务
```

### 权限

| 权限节点 | 默认 | 说明 |
|---|---|---|
| `crossserver.reload` | op | 热重载插件 |
