# API 文档

CrossServer 通过 Bukkit `ServicesManager` 暴露 `CrossServerApi`，其他插件可以注册命名空间、读写数据、监听同步事件，也可以接入新的配置中心文档能力。

这份文档适合：

- 想让自己的插件接入 CrossServer
- 想做跨服共享配置
- 想保存自定义玩家数据
- 想复用 CrossServer 的跨服传送能力

## 先理解这件事

CrossServerApi 不是“只有几个工具方法”的辅助类，它本质上是对下面几类能力的统一封装：

- 玩家数据快照
- 全局数据快照
- 配置中心文档
- 同步监听
- 会话锁
- 跨服传送
- 经济服务
- 诊断与管理能力

如果你是第三方插件开发者，通常优先通过 `CrossServerApi` 接入，而不是直接去碰内部 Service 类。

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

## 推荐接入流程

一个第三方插件最常见的接入流程通常是：

1. 在 `onEnable()` 中获取 `CrossServerApi`
2. 注册你自己的命名空间或配置文档
3. 在需要时读写玩家数据 / 全局数据
4. 如果要跨服热更新，就注册监听器
5. 如果要跨服传送，就调用 `requestTeleport(...)`

## 什么时候用哪种能力

| 需求 | 推荐能力 |
|------|----------|
| 给某个玩家保存一份跨服数据 | `savePlayerData / loadPlayerData` |
| 给整个集群保存一份共享配置 | `saveConfigDocument / loadConfigDocument` |
| 保存一个简单的全局状态值 | `saveGlobalData / loadGlobalData` |
| 监听其他服务器写入了数据 | `registerSyncListener` |
| 让玩家跨服传送 | `requestTeleport` |
| 想看某个玩家为什么跨服卡住 | `inspectTransfer / inspectTransferDiagnostics` |

## 命名空间

所有数据操作都通过命名空间隔离。建议格式：`插件名.数据类型`。

```java
api.registerNamespace("my-plugin.player-data");
api.registerNamespace("my-plugin.config");
```

命名空间只需注册一次（通常在 `onEnable()` 中）。同一命名空间重复注册不会报错。

### 命名建议

推荐使用“插件名 + 功能名”的形式，避免与其他插件冲突：

- `my-plugin.player-data`
- `my-plugin.progress`
- `my-plugin.config`

不建议直接使用过于泛化的名字，例如：

- `data`
- `config`
- `player`

## 玩家数据

适合存储“每个玩家一份”的内容，例如：

- 自定义等级
- 任务进度
- 技能点
- 自定义 GUI 状态

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

### 常见模式：玩家加入时加载，变化时保存

```java
public void loadPlayerState(Player player) {
    try {
        Optional<PlayerSnapshot> snapshot = api.loadPlayerData(player.getUniqueId(), "my-plugin.player-data");
        if (snapshot.isEmpty()) {
            return;
        }
        String payload = snapshot.get().payload();
        // 解析 payload 并应用到你的业务对象
    } catch (Exception e) {
        getLogger().warning("加载玩家跨服数据失败: " + e.getMessage());
    }
}

public void savePlayerState(Player player, String payload) {
    try {
        api.savePlayerData(player.getUniqueId(), "my-plugin.player-data", payload);
    } catch (Exception e) {
        getLogger().warning("保存玩家跨服数据失败: " + e.getMessage());
    }
}
```

## 全局数据

适用于跨服共享的配置、状态等。

适合：

- 一个全服共享倒计时
- 某个活动状态
- 一个简单的公告开关
- 不需要元信息的全局状态值

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

### 什么时候不要直接用全局数据

如果你的需求是“共享配置文档”，而不是“一个简单的全局状态值”，更推荐使用下一节的配置中心文档。

## 配置中心文档

这是在全局数据之上的高层封装，适合第三方插件把“共享配置”接入 CrossServer 配置中心。

配置文档当前支持两种格式：

- JSON
- YAML

相比直接使用 `saveGlobalData(...)`，配置中心文档更适合配置类场景，因为它带有：

- 注册机制
- 文档元信息
- 格式识别
- 变更监听

### 注册文档

```java
api.registerConfigDocument("my-plugin.config", "main");
```

### 注册文档并附带 schema

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
        true,
        "enabled: true\nlimits:\n  maxHomes: 3\n",
        "我的插件共享配置"
    )
);
```

保存时，CrossServer 会根据 schema 做服务端校验；如果缺少必填字段或字段类型不匹配，会直接拒绝保存。

当前 schema 第一版额外支持：

- 枚举值校验
- 数值最小 / 最大值校验
- 非空字段校验
- 数组元素类型校验

### 保存文档

```java
ConfigDocument document = api.saveConfigDocument(
    "my-plugin.config",
    "main",
    new ConfigDocumentUpdate(
        "{\"enabled\":true,\"maxHomes\":3}",
        1,
        "my-plugin",
        "my-plugin",
        "更新主配置"
    )
);
```

### 更推荐的保存方式

如果你想明确带上版本、来源和摘要，推荐始终用 `ConfigDocumentUpdate` 这个重载，而不是只传一个字符串 payload。

写入后，CrossServer 会自动把以下元信息补进配置文档：

- `schemaVersion`
- `updatedBy`
- `updatedAt`
- `source`
- `summary`

如果原始 payload 是 YAML，保存后会继续按 YAML 写回；如果原始 payload 是 JSON，保存后会继续按 JSON 写回。

### 加载文档

```java
Optional<ConfigDocument> document = api.loadConfigDocument("my-plugin.config", "main");
if (document.isPresent()) {
    String payload = document.get().payload();
    ConfigDocumentFormat format = document.get().format();
    long version = document.get().version();
    String updatedBy = document.get().updatedBy();
}
```

### 监听文档变更

```java
Runnable handle = api.registerConfigDocumentListener("my-plugin.config", "main", event -> {
    getLogger().info("config changed: " + event.namespace() + "/" + event.dataKey());
});

// 取消监听
handle.run();
```

### 常见模式：配置热更新

```java
private Runnable configListenerHandle;

public void setupSharedConfig() {
    api.registerConfigDocument("my-plugin.config", "main");
    configListenerHandle = api.registerConfigDocumentListener("my-plugin.config", "main", event -> {
        getLogger().info("共享配置已更新，版本=" + event.version());
        reloadMySharedConfig();
    });
}

public void shutdownSharedConfig() {
    if (configListenerHandle != null) {
        configListenerHandle.run();
        configListenerHandle = null;
    }
}
```

### 查询已注册文档

```java
Set<RegisteredConfigDocument> documents = api.getRegisteredConfigDocuments();
```

### 查询文档历史

```java
List<Map<String, Object>> history = api.loadConfigDocumentHistory("my-plugin.config", "main");
```

当前默认会保留最近 20 次文档版本历史，适合：

- 在 Web 面板查看最近变更
- 自己做简单审计展示
- 排查“谁把配置改坏了”

## 同步监听

当其他服务器更新了指定命名空间的数据时，监听器会被触发。

### 注册命名空间监听器

```java
api.registerSyncListener("my-plugin.player-data", message -> {
    getLogger().info("Data updated: " + message.namespace() + "/" + message.targetId());
});
```

### 注册全局监听器

```java
api.registerSyncListener(message -> {
    // 所有命名空间的变更都会触发
});
```

### 可卸载的监听器

```java
Runnable handle = api.registerSyncListenerHandle("my-plugin.player-data", listener);
handle.run();
```

### 使用建议

- 长生命周期监听器优先用 `registerSyncListenerHandle(...)`
- 插件关闭时记得调用返回的 `Runnable` 解除监听
- 监听器里不要做重 I/O，必要时自己切异步

## 会话管理

手动控制玩家的会话锁。通常插件不需要手动调用，数据操作会自动管理会话。

只有在你要做一组“必须保证原子性”的玩家跨服数据操作时，才建议手动管理会话。

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

### 推荐使用场景

- 自定义副本入口
- 跨服活动大厅
- 匹配服 / 对战服传送
- 你的插件自定义的跨服菜单入口

### 使用建议

- `causeRef` 尽量带上你的插件标识，便于诊断
- 例如：`my-plugin:arena-join`、`my-plugin:dungeon-enter`

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

如果你的插件已经兼容 Vault，也可以直接依赖 Bukkit 的经济服务；但如果你明确想依赖 CrossServer 的内部经济同步语义，直接走 `api.getEconomyService()` 更直接。

## 传送管理

```java
Optional<TransferAdminService.TransferInspection> inspection = api.inspectTransfer(playerId);
TransferDiagnostics diagnostics = api.inspectTransferDiagnostics(playerId);
List<TransferHistoryEntry> history = api.getTransferHistory(playerId, 10);
List<TransferHistoryEntry> recent = api.getRecentTransferHistory(20);
TransferAdminService.ClearResult result = api.clearTransfer(playerId, "admin");
```

这组接口更适合：

- 管理插件
- 运维工具
- 自定义诊断面板

## 认证管理

```java
AuthService.AuthAdminInspection auth = api.inspectAuth(playerId);
api.invalidateAuthTickets(playerId, "admin");
api.forceReauthenticate(playerId, "admin");
```

## 节点配置同步

通过 `NodeConfigSyncService` 管理跨节点的配置快照与变更申请。

### 命名空间

`node.config` — 全局快照，每个节点以 `serverId` 为 data_key 存储配置快照。

快照格式：

```json
{
  "server": { "id": "survival-1", "cluster": "my-cluster" },
  "messaging": { "enabled": true, "redisUri": "redis://...", "channel": "crossserver" },
  "webPanel": { "enabled": true, "host": "127.0.0.1", "port": 8765, "masterServerId": "lobby", "tokenConfigured": true },
  "modules": { "auth": true, "homes": true, ... }
}
```

### 工作流程

1. 节点启动时 `publishLocalSnapshot()` 将可编辑配置快照发布到 `node.config`
2. 主控节点调用 `loadClusterNodeConfigs()` 获取所有节点快照
3. 主控节点调用 `loadNodeConfigDetail(serverId)` 获取特定节点详情
4. 主控节点调用 `requestApply(serverId, changes, actor)` 提交变更
5. 目标节点收到变更后写入 `config.yml` 并排队重载

### 安全限制

- 仅白名单字段可修改（messaging / webPanel / modules）
- 不暴露数据库、代理等敏感配置
- 每次申请仅影响一个目标节点

## 一个完整示例：做一个跨服共享配置插件

下面是一个很典型的接入思路：

```java
public final class MyPlugin extends JavaPlugin {
    private CrossServerApi api;
    private Runnable configListenerHandle;

    @Override
    public void onEnable() {
        api = Bukkit.getServicesManager().load(CrossServerApi.class);
        if (api == null) {
            getLogger().warning("CrossServer API 不可用，跳过共享配置接入");
            return;
        }

        api.registerConfigDocument("my-plugin.config", "main");
        configListenerHandle = api.registerConfigDocumentListener("my-plugin.config", "main", event -> {
            getLogger().info("检测到共享配置更新，重新加载中...");
            reloadSharedConfig();
        });

        reloadSharedConfig();
    }

    @Override
    public void onDisable() {
        if (configListenerHandle != null) {
            configListenerHandle.run();
            configListenerHandle = null;
        }
    }

    private void reloadSharedConfig() {
        try {
            Optional<ConfigDocument> document = api.loadConfigDocument("my-plugin.config", "main");
            if (document.isEmpty()) {
                getLogger().info("共享配置尚未创建，继续使用本地默认值");
                return;
            }

            String payload = document.get().payload();
            // 解析 JSON / YAML payload
            getLogger().info("已加载共享配置，版本=" + document.get().version());
        } catch (Exception e) {
            getLogger().warning("加载共享配置失败: " + e.getMessage());
        }
    }
}
```

## 最后的建议

### 1. 想存玩家状态，优先用玩家数据快照

不要把玩家私有数据硬塞进全局快照。

### 2. 想做共享配置，优先用配置中心文档

不要自己重复造一套全局 JSON 存储协议。

### 3. 想做跨服传送，优先复用 `requestTeleport(...)`

不要自己在插件里重复实现 handoff / rollback / session transfer 逻辑。
