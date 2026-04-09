# 配置与运维 API

这页适合做：

- 运维插件
- 外部控制台
- 共享配置插件
- 自动化管理工具

## 配置中心文档 API

### 注册文档

```java
api.registerConfigDocument("my-plugin.config", "main");
```

### 注册文档并带 schema

```java
api.registerConfigDocument(
    "my-plugin.config",
    "main",
    new ConfigDocumentSchema(
        "MyPluginConfig",
        1,
        List.of("enabled", "limits.maxHomes"),
        Map.of(
            "enabled", "boolean",
            "limits", "object",
            "limits.maxHomes", "integer"
        ),
        Map.of(),
        Map.of(),
        Map.of(),
        List.of(),
        Map.of(),
        true,
        "enabled: true\nlimits:\n  maxHomes: 3\n",
        "我的插件共享配置"
    )
);
```

### 文档读写

```java
ConfigDocument saved = api.saveConfigDocument(
    "my-plugin.config",
    "main",
    new ConfigDocumentUpdate(payload, 1, "my-plugin", "my-plugin", "更新主配置")
);

Optional<ConfigDocument> loaded = api.loadConfigDocument("my-plugin.config", "main");
Optional<ConfigEntry> entry = api.loadConfigEntry("my-plugin.config", "main");
```

### 校验 / 历史 / 回滚

```java
api.validateConfigDocument("my-plugin.config", "main", payload);

List<Map<String, Object>> history = api.loadConfigDocumentHistory("my-plugin.config", "main");
ConfigDocument rolledBack = api.rollbackConfigDocument("my-plugin.config", "main", 12L, "my-plugin");
```

## 共享模块 API

```java
Optional<SharedModuleConfigSnapshot> sharedModules = api.loadSharedModules();
api.saveSharedModules(snapshot, "my-plugin");
SharedModuleConfigSnapshot rolledBackModules = api.rollbackSharedModules(10L, "my-plugin");
```

## 共享路由 API

```java
Map<String, String> sharedRoutes = api.loadSharedRoutes();
Map<String, String> effectiveRoutes = api.mergedRoutes();

api.setSharedRoute("spawn", "lobby", "my-plugin");
api.removeSharedRoute("spawn", "my-plugin");
Map<String, String> rolledBackRoutes = api.rollbackSharedRoutes(8L, "my-plugin");
```

## 节点配置 API

```java
Map<String, Object> nodes = api.loadNodeConfigs();
Map<String, Object> detail = api.loadNodeConfigDetail("survival-1");

Map<String, Object> result = api.requestNodeConfigApply(
    "survival-1",
    Map.of(
        "modules", Map.of("tpa", false)
    ),
    "my-plugin"
);
```

## Transfer 运维 API

```java
Optional<TransferAdminService.TransferInspection> inspection = api.inspectTransfer(playerId);
TransferDiagnostics diagnostics = api.inspectTransferDiagnostics(playerId);
List<TransferHistoryEntry> history = api.getTransferHistory(playerId, 10);
List<TransferHistoryEntry> recent = api.getRecentTransferHistory(20);

TransferAdminService.ClearResult clearResult = api.clearTransfer(playerId, "admin");
api.reconcileTransfer(playerId, playerName);
```

## Auth 管理 API

```java
AuthService.AuthAdminInspection auth = api.inspectAuth(playerId);
String invalidateResult = api.invalidateAuthTickets(playerId, "admin");
String reauthResult = api.forceReauthenticate(playerId, "admin");
```

## 全局数据快照 API

适合简单共享状态值：

```java
api.saveGlobalData("my-plugin.config", "server-mode", payload);
Optional<GlobalSnapshot> snapshot = api.loadGlobalData("my-plugin.config", "server-mode");
```

如果你需要的是“共享配置文档”，优先使用上面的配置中心文档 API，而不是只存一段裸 payload。
