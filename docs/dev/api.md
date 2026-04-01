# API 文档

CrossServer 通过 Bukkit `ServicesManager` 暴露 `CrossServerApi`，其他插件可以注册命名空间、读写数据、监听同步事件。

## 获取 API

```java
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.bukkit.Bukkit;

CrossServerApi api = Bukkit.getServicesManager()
    .load(CrossServerApi.class);

if (api == null) {
    return;
}
```

建议在插件的 `onEnable()` 中获取并缓存实例。

## 命名空间

所有数据操作都通过命名空间隔离。建议格式：`插件名.数据类型`。

```java
api.registerNamespace("my-plugin.player-data");
api.registerNamespace("my-plugin.config");
```

命名空间只需注册一次（通常在 `onEnable()` 中）。同一命名空间重复注册不会报错。

## 玩家数据

### 保存

```java
UUID playerId = player.getUniqueId();
api.savePlayerData(playerId, "my-plugin.player-data", "{\"level\": 5, \"score\": 100}");
```

### 加载

```java
Optional<PlayerSnapshot> snapshot = api.loadPlayerData(playerId, "my-plugin.player-data");
if (snapshot.isPresent()) {
    String json = snapshot.get().payload();
    long version = snapshot.get().version();
}
```

`PlayerSnapshot` 包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| `playerId` | UUID | 玩家 UUID |
| `namespace` | String | 命名空间 |
| `payload` | String | JSON 数据 |
| `version` | long | 版本号（每次写入递增） |
| `updatedAt` | Instant | 最后更新时间 |

## 全局数据

适用于跨服共享的配置、状态等。

### 保存

```java
api.saveGlobalData("my-plugin.config", "server-mode", "{\"mode\": \"hardcore\"}");
```

### 加载

```java
Optional<GlobalSnapshot> snapshot = api.loadGlobalData("my-plugin.config", "server-mode");
if (snapshot.isPresent()) {
    String json = snapshot.get().payload();
}
```

Warp 与 TPA 当前都复用了这个能力：

- `warps / teleport.warps` — 全局 Warp 列表
- `teleport.requests / tpa.requests` — 跨服 TPA 请求

## 同步监听

当其他服务器更新了指定命名空间的数据时，监听器会被触发。

### 注册命名空间监听器

```java
api.registerSyncListener("my-plugin.player-data", (namespace, playerId, dataKey) -> {
    getLogger().info("Data updated: " + namespace + "/" + dataKey);
});
```

### 注册全局监听器

```java
api.registerSyncListener((namespace, playerId, dataKey) -> {
    // 所有命名空间的变更都会触发
});
```

### 可卸载的监听器

```java
Runnable handle = api.registerSyncListenerHandle("my-plugin.player-data", listener);
handle.run();
```

## 会话管理

手动控制玩家的会话锁。通常插件不需要手动调用，数据操作会自动管理会话。

```java
boolean locked = api.openPlayerSession(playerId);
if (!locked) {
    player.kick(Component.text("你的跨服会话正在同步中，请稍后重试"));
    return;
}

try {
    // 执行数据操作...
} finally {
    api.closePlayerSession(playerId);
}
```

## 跨服传送

```java
import org.xiaoziyi.crossserver.teleport.TeleportTarget;
import org.xiaoziyi.crossserver.teleport.TeleportInitiationResult;

TeleportTarget target = new TeleportTarget("server-2", "world", 0, 64, 0, 0, 0);
TeleportInitiationResult result = api.requestTeleport(playerId, target, "my-plugin-transfer");

if (result.success()) {
    player.sendMessage("正在传送到 server-2...");
} else {
    player.sendMessage("传送失败: " + result.message());
}
```

当前 `/home`、`/warp`、`/tpa`、`/tpahere` 最终都会复用这条主链路。

## 经济服务

```java
import org.xiaoziyi.crossserver.economy.EconomyService;

EconomyService economy = api.getEconomyService();

economy.getBalance(player).thenAccept(balance -> {
    player.sendMessage("余额: " + balance);
});

economy.deposit(player, new BigDecimal("100.00")).thenAccept(result -> {
    if (result.success()) {
        player.sendMessage("存款成功");
    }
});

economy.withdraw(player, new BigDecimal("50.00")).thenAccept(result -> {
    if (result.success()) {
        player.sendMessage("取款成功");
    } else {
        player.sendMessage("余额不足");
    }
});
```

## 传送管理

```java
Optional<TransferAdminService.TransferInspection> inspection = api.inspectTransfer(playerId);
TransferDiagnostics diagnostics = api.inspectTransferDiagnostics(playerId);
List<TransferHistoryEntry> history = api.getTransferHistory(playerId, 10);
List<TransferHistoryEntry> recent = api.getRecentTransferHistory(20);
TransferAdminService.ClearResult result = api.clearTransfer(playerId, "admin");
```

## 认证管理

```java
AuthService.AuthAdminInspection auth = api.inspectAuth(playerId);
api.invalidateAuthTickets(playerId, "admin");
api.forceReauthenticate(playerId, "admin");
```
