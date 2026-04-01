# 命令与权限

## crossserver

```text
/crossserver help
/crossserver status
/crossserver nodes [page]
/crossserver node <serverId>
/crossserver route list
/crossserver route menu
/crossserver route set <serverId> <proxyServer>
/crossserver route remove <serverId>
/crossserver transfer <player>
/crossserver transfer help
/crossserver transfer info <player>
/crossserver transfer clear <player>
/crossserver transfer menu <player>
/crossserver transfer history <player>
/crossserver transfer recent [page]
/crossserver transfer reconcile <player>
/crossserver auth inspect <player>
/crossserver auth invalidate <player>
/crossserver auth forcereauth <player>
/crossserver reload
```

### 权限

- `crossserver.command`
- `crossserver.status.view`
- `crossserver.nodes.view`
- `crossserver.node.view`
- `crossserver.route.view`
- `crossserver.route.edit`
- `crossserver.transfer.view`
- `crossserver.transfer.menu`
- `crossserver.transfer.clear`
- `crossserver.transfer.reconcile`
- `crossserver.auth.admin`
- `crossserver.reload`

## economy

```text
/economy balance <player>
/economy set <player> <amount>
/economy deposit <player> <amount>
/economy withdraw <player> <amount>
/economy history <player> [limit]
```

### 权限

- `crossserver.economy.balance`
- `crossserver.economy.history`
- `crossserver.economy.set`
- `crossserver.economy.deposit`
- `crossserver.economy.withdraw`

## homes

```text
/home [name]
/homes
/sethome <name>
/delhome <name>
/setdefaulthome <name>
```

### 说明

- `/homes` 会打开 homes 菜单 UI
- `homes` 数据跨服共享
- 当前服务器上的 home 仍然直接本地传送
- 如果目标 home 位于其他服务器，会进入 `teleport.handoff` 准备流程

### 权限

- `crossserver.homes.list`
- `crossserver.homes.menu`
- `crossserver.homes.teleport`
- `crossserver.homes.set`
- `crossserver.homes.delete`
- `crossserver.homes.default`

## auth

```text
/login <password>
/l <password>
/register <password> <confirm>
/reg <password> <confirm>
/changepassword <old> <new>
```

### 说明

- 玩家可通过命令登录，也可直接在聊天栏输入密码完成登录
- 登录期间会显示 Title、ActionBar、BossBar
- 登录成功后会生成短时 ticket，用于跨服免重登
- 未认证阶段会限制移动、交互、背包、丢弃/拾取、非白名单命令等行为

### 权限

- `crossserver.auth.login`
- `crossserver.auth.register`
- `crossserver.auth.changepassword`
