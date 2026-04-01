# API 文档

`CrossServerApi` 通过 Bukkit `ServicesManager` 注册为服务，其他插件可通过标准方式获取。

## 获取 API

```java
CrossServerApi api = Bukkit.getServicesManager()
    .load(CrossServerApi.class)
    .orElseThrow(() -> new IllegalStateException("cross-server not loaded"));
```

## 数据同步

### 命名空间注册

```java
api.registerNamespace("my-plugin.player-data");
```

所有数据操作都需要指定命名空间。推荐格式：`插件名.数据类型`，如 `economy`、`inventory`、`homes` 等。

### 保存玩家数据

```java
api.savePlayerData(playerUniqueId, "my-plugin.player-data", "{\"key\": \"value\"}");
```

### 加载玩家数据

```java
String payload = api.loadPlayerData(playerUniqueId, "my-plugin.player-data");
```

### 保存全局数据

```java
api.saveGlobalData("my-plugin.global", "server-mode", "{\"mode\": \"hardcore\"}");
```

### 加载全局数据

```java
String payload = api.loadGlobalData("my-plugin.global", "server-mode");
```

## 同步监听

### 注册命名空间监听器

```java
api.registerSyncListener("my-plugin.player-data", (namespace, playerUuid, payload) -> {
    // 当该命名空间的玩家数据变更时触发
    logger.info("Player {} data updated: {}", playerUuid, payload);
});
```

### 注册全局监听器

```java
api.registerSyncListener((namespace, playerUuid, payload) -> {
    // 所有命名空间的数据变更都会触发
});
```

### 带可卸载句柄的监听器

```java
Runnable handle = api.registerSyncListenerHandle("my-plugin.player-data", listener);
// 不再需要时卸载
handle.run();
```

## 会话管理

### 开启玩家会话锁

```java
boolean locked = api.openPlayerSession(playerUniqueId);
if (!locked) {
    // 会话锁获取失败，可能正在同步中
    player.kick("你的跨服会话正在同步中，请稍后重试");
}
```

### 关闭玩家会话锁

```java
api.closePlayerSession(playerUniqueId);
```

## 传送系统

### 发起跨服传送

```java
TeleportTarget target = new TeleportTarget("server-2", location);
api.requestTeleport(playerUniqueId, target, "command");
```

## 传送管理

```java
// 查看玩家当前传送状态
api.inspectTransfer(playerUniqueId);

// 查看详细诊断信息
api.inspectTransferDiagnostics(playerUniqueId);

// 查询最近传送历史
api.getTransferHistory(playerUniqueId, 10);

// 查询全服最近传送
api.getRecentTransferHistory(20);

// 清理卡住的传送状态
api.clearTransfer(playerUniqueId, "admin");
```

## 认证管理

```java
// 查看玩家认证状态
api.inspectAuth(playerUniqueId);

// 使玩家 ticket 失效
api.invalidateAuthTickets(playerUniqueId, "admin");

// 强制重新认证
api.forceReauthenticate(playerUniqueId, "admin");
```

## 经济服务

```java
EconomyService economy = api.getEconomyService();

// 查询余额
BigDecimal balance = economy.getBalance(player);

// 设置余额
economy.setBalance(player, bigDecimalValue);

// 存款
economy.deposit(player, amount);

// 取款
economy.withdraw(player, amount);

// 获取交易记录
List<EconomyTransactionEntry> history = economy.getTransactionHistory(player, 50);
```

## 同步命名空间参考

以下是插件内置的同步命名空间，供其他插件接入时参考：

| 命名空间 | 说明 |
|---|---|
| `economy` | 经济数据 |
| `inventory` | 背包数据 |
| `enderchest` | 末影箱数据 |
| `player-state` | 玩家状态（血量、饥饿值等） |
| `homes` | 家园位置数据 |
| `auth.profile` | 认证账号信息 |
| `auth.ticket` | 认证 ticket |
| `teleport.handoff` | 传送 handoff 数据 |
| `cluster.config` | `teleport.routes` 共享路由表 |
