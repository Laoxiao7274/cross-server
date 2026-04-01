# API 文档

CrossServer 通过 Bukkit `ServicesManager` 暴露 `CrossServerApi`，其他插件可以注册命名空间、读写数据、监听同步事件。

## 获取 API

```java
import org.xiaoziyi.crossserver.api.CrossServerApi;
import org.bukkit.Bukkit;

CrossServerApi api = Bukkit.getServicesManager()
    .load(CrossServerApi.class);

if (api == null) {
    // CrossServer 未加载
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

## 同步监听

当其他服务器更新了指定命名空间的数据时，监听器会被触发。

### 注册命名空间监听器

```java
api.registerSyncListener("my-plugin.player-data", (namespace, playerId, dataKey) -> {
    // namespace: 被更新的命名空间
    // playerId:  null 表示全局数据，非 null 表示玩家 UUID
    // dataKey:   数据键
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

// 不再需要时卸载
handle.run();
```

## 会话管理

手动控制玩家的会话锁。通常插件不需要手动调用，数据操作会自动管理会话。

```java
// 尝试获取会话锁
boolean locked = api.openPlayerSession(playerId);
if (!locked) {
    player.kick(Component.text("你的跨服会话正在同步中，请稍后重试"));
    return;
}

try {
    // 执行数据操作...
} finally {
    // 释放会话锁
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

## 经济服务

```java
import org.xiaoziyi.crossserver.economy.EconomyService;

EconomyService economy = api.getEconomyService();

// 查询余额
economy.getBalance(player).thenAccept(balance -> {
    player.sendMessage("余额: " + balance);
});

// 存款（异步）
economy.deposit(player, new BigDecimal("100.00")).thenAccept(result -> {
    if (result.success()) {
        player.sendMessage("存款成功");
    }
});

// 取款（异步）
economy.withdraw(player, new BigDecimal("50.00")).thenAccept(result -> {
    if (result.success()) {
        player.sendMessage("取款成功");
    } else {
        player.sendMessage("余额不足");
    }
});

// 交易记录
economy.getTransactionHistory(player, 20).thenAccept(history -> {
    for (EconomyTransactionEntry entry : history) {
        getLogger().info(entry.type() + " " + entry.amount());
    }
});
```

## 传送管理

```java
// 查看玩家当前传送状态
Optional<TransferAdminService.TransferInspection> inspection = api.inspectTransfer(playerId);

// 详细诊断
TransferDiagnostics diagnostics = api.inspectTransferDiagnostics(playerId);

// 传送历史
List<TransferHistoryEntry> history = api.getTransferHistory(playerId, 10);

// 全服最近传送
List<TransferHistoryEntry> recent = api.getRecentTransferHistory(20);

// 清理卡住的传送
TransferAdminService.ClearResult result = api.clearTransfer(playerId, "admin");
```

## 认证管理

```java
// 查看认证状态
AuthService.AuthAdminInspection auth = api.inspectAuth(playerId);

// 作废免重登票据
api.invalidateAuthTickets(playerId, "admin");

// 强制重新认证
api.forceReauthenticate(playerId, "admin");
```
