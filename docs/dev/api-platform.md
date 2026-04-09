# 配置与运维 API

这页适合做：

- 共享配置插件
- 运维插件
- 外部控制台
- 自动化管理脚本
- 自定义 Web 面板 / 管理端

## 配置中心文档 API

### 这组 API 是干什么的

这是 CrossServer 最适合做“共享配置中心”的一组 API。

如果你想让多个子服共用一份配置，优先用它，不要自己手写 `global_snapshot` payload 协议。

### 常见方法

| 方法 | 作用 |
|------|------|
| `registerConfigDocument(...)` | 注册配置文档 |
| `saveConfigDocument(...)` | 保存配置文档 |
| `loadConfigDocument(...)` | 加载配置文档 |
| `loadConfigEntry(...)` | 读取文档条目元信息 |
| `validateConfigDocument(...)` | 校验 payload 是否符合 schema |
| `loadConfigDocumentHistory(...)` | 读取历史版本 |
| `rollbackConfigDocument(...)` | 回滚到历史版本 |

### 带注释示例

```java
// 1. 注册一份共享配置文档
api.registerConfigDocument("my-plugin.config", "main");

// 2. 保存配置文档
ConfigDocument saved = api.saveConfigDocument(
    "my-plugin.config",
    "main",
    new ConfigDocumentUpdate(
        payload,          // JSON 或 YAML 文本
        1,                // schemaVersion
        "my-plugin",     // updatedBy
        "my-plugin",     // source
        "更新主配置"       // summary
    )
);

// 3. 读取配置文档
Optional<ConfigDocument> loaded = api.loadConfigDocument("my-plugin.config", "main");

// 4. 校验一份 payload 是否符合 schema
api.validateConfigDocument("my-plugin.config", "main", payload);

// 5. 读取历史记录
List<Map<String, Object>> history = api.loadConfigDocumentHistory("my-plugin.config", "main");

// 6. 回滚到指定历史版本
ConfigDocument rolledBack = api.rollbackConfigDocument("my-plugin.config", "main", 12L, "my-plugin");
```

### 参数怎么理解

- `namespace`：配置文档命名空间，例如 `my-plugin.config`
- `dataKey`：文档键，例如 `main`
- `payload`：实际配置内容，可以是 JSON 或 YAML
- `schemaVersion`：你定义的配置版本号
- `updatedBy / source / summary`：元信息，用于追踪是谁改了配置

## 共享模块 API

### 这组 API 是干什么的

这组 API 用来读取、保存、回滚集群共享模块开关。

### 带注释示例

```java
// 读取共享模块配置
Optional<SharedModuleConfigSnapshot> sharedModules = api.loadSharedModules();

// 保存共享模块配置
api.saveSharedModules(snapshot, "my-plugin");

// 回滚到历史版本
SharedModuleConfigSnapshot rolledBackModules = api.rollbackSharedModules(10L, "my-plugin");
```

## 共享路由 API

### 这组 API 是干什么的

这组 API 用来操作 `serverId -> proxyTarget` 的共享路由表。

### 带注释示例

```java
// 读取共享路由
Map<String, String> sharedRoutes = api.loadSharedRoutes();

// 读取最终合并后的路由（本地 + 共享覆盖）
Map<String, String> effectiveRoutes = api.mergedRoutes();

// 设置一条共享路由
api.setSharedRoute("spawn", "lobby", "my-plugin");

// 删除一条共享路由
api.removeSharedRoute("spawn", "my-plugin");

// 回滚共享路由到历史版本
Map<String, String> rolledBackRoutes = api.rollbackSharedRoutes(8L, "my-plugin");
```

## 节点配置 API

### 这组 API 是干什么的

这组 API 用来读取节点配置快照，并向目标节点提交配置申请。

### 带注释示例

```java
// 读取所有节点配置概览
Map<String, Object> nodes = api.loadNodeConfigs();

// 读取某个节点的详细配置
Map<String, Object> detail = api.loadNodeConfigDetail("survival-1");

// 向目标节点提交配置申请
Map<String, Object> result = api.requestNodeConfigApply(
    "survival-1",
    Map.of(
        "modules", Map.of("tpa", false)
    ),
    "my-plugin"
);
```

### 参数怎么理解

- `serverId`：目标节点 ID
- `changes`：这次要修改的白名单字段
- `actorName`：操作来源标识

## Transfer 运维 API

### 这组 API 是干什么的

这组 API 用来做跨服传送诊断与修复。

### 带注释示例

```java
// 查看某个玩家当前 transfer 检查信息
Optional<TransferAdminService.TransferInspection> inspection = api.inspectTransfer(playerId);

// 查看更完整的诊断信息
TransferDiagnostics diagnostics = api.inspectTransferDiagnostics(playerId);

// 查看历史记录
List<TransferHistoryEntry> history = api.getTransferHistory(playerId, 10);
List<TransferHistoryEntry> recent = api.getRecentTransferHistory(20);

// 强制清理 transfer 卡住状态
TransferAdminService.ClearResult clearResult = api.clearTransfer(playerId, "admin");

// 尝试做保守修复
api.reconcileTransfer(playerId, playerName);
```

## Auth 管理 API

### 这组 API 是干什么的

这组 API 偏管理和运维，不是给普通玩家功能调用的。

### 带注释示例

```java
// 查看认证状态详情
AuthService.AuthAdminInspection auth = api.inspectAuth(playerId);

// 让当前 ticket 失效
String invalidateResult = api.invalidateAuthTickets(playerId, "admin");

// 强制要求玩家重新认证
String reauthResult = api.forceReauthenticate(playerId, "admin");
```

## 全局数据快照 API

如果你只是想存一个简单的共享状态值，可以直接用全局数据快照：

```java
// 保存一个简单的全局状态
api.saveGlobalData("my-plugin.config", "server-mode", payload);

// 读取这个状态
Optional<GlobalSnapshot> snapshot = api.loadGlobalData("my-plugin.config", "server-mode");
```

### 什么时候不要只用这个

如果你要的是“共享配置文档”，优先使用配置中心文档 API。  
如果你只用 `saveGlobalData(...)`，就需要自己维护格式、历史和语义。
