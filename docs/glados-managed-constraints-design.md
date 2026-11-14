# GLaDOS 托管模式下的 Shimeji 约束设计

- 文档版本：v1.1（2026-11-14 复核修订：回写实施进度、修正事实描述、加固回归测试）
- 编写日期：2026-11-14
- 最后复核：2026-11-14
- 状态：**Shimeji 侧已实现并完成复核加固；仅剩真机 GUI 端到端补测增殖行为（进度详见 §9.1）**
- 适用范围：shimeji 仓库（`feature/glados-integration` 分支）
- 关联文档：
  - `docs/glados-ai-integration-design.md`（GLaDOS AI 集成总设计）
  - GLaDOS 侧：`GLaDOSClaw/需求文档/3.0.x/2026-11-14-桌宠双引擎轻量级重量级-详细需求.md`

---

## 1. 背景与目标

GLaDOS 把 Shimeji 作为「重量级桌宠」引擎，以**独立子进程**拉起，通过 HTTP 控制桥管理。
此时 Shimeji 是 GLaDOS 的从属进程，用户预期它是「一个受控的桌宠」；而 Shimeji 原生
行为是为**用户直接双击运行**设计的，包含若干会破坏该预期的特性。

本轮要解决的问题有三条，**仅在 GLaDOS 托管时生效**：

| # | 约束 | 原因 |
|---|---|---|
| 1 | 不允许 Shimeji 自行检测更新 | GLaDOS 统一管理发行版本；子进程自行弹更新框会与宿主版本不一致，且用户可能被引导去装一个"独立版"。 |
| 2 | 不允许 Shimeji 自行 spawn 多个桌宠 | 桌宠数量应由 GLaDOS 决定（`setRole`/`createMascot`/`remainOne`），否则屏幕上会越掉越多，且状态桥只跟踪一个角色。 |
| 3 | 不展示 Shimeji 的托盘图标 | 托盘菜单会打开 Shimeji 自己的设置窗口，绕过 GLaDOS 设置页；且与 GLaDOS 的托盘/桌面集成重复，用户会看到两套入口。 |

**硬性要求：用户自己正常运行时（双击 jar / 命令行直接跑），上述三条必须保持原版行为，
完全不受影响。**

---

## 2. 现状核实

以下结论均来自当前 `feature/glados-integration` 分支源码，不是推测。

### 2.1 更新检查 —— 已实现

`Main.main()` 中：

```java
if (!getInstance().gladosOptions.isManaged()) {
    new Thread(() -> { ... UpdateChecker.checkUpdate(...) ... new DownloadDialog(); }).start();
} else {
    log.info("GLaDOS managed mode: skip online update check");
}
```

结论：**已完成**，托管时不联网、不弹框。本轮无需改动，仅在文档中固化该契约。

### 2.2 托盘图标 —— 已实现

`Main.run()` 中：

```java
if (!gladosOptions.isManaged()) {
    createTrayIcon();
}
```

结论：**已完成**，托管时不创建托盘。本轮无需改动。

### 2.3 禁止 spawn 多个桌宠 —— **未实现，本轮核心**

Shimeji 有两条会让角色数量增长的路径：

**(A) 自主行为树里的增殖行为**

| 行为名 | 引用动作 | 动作类 | 触发条件（behaviors.xml） |
|---|---|---|---|
| `SplitIntoTwo` | `Divide1` | `action/Breed` | `Frequency="50" Condition="#{mascot.totalCount < 50}"` |
| `PullUpShimeji` | `PullUpShimeji1` | `action/Breed` | `Frequency="50" Condition="#{mascot.totalCount < 50}"` |

`Breed` 的关键代码（`action/Breed.java`）：

```java
final Mascot mascot = new Mascot( childType );
...
action.getMascot().getManager().add( mascot );
```

影响范围（按源码逐文件核对）：`behaviors.xml` 与 `MischievousBehavior.xml` 中
两者均为 `Frequency="50"`；`CalmBehavior.xml` 中两者为 `"0"`；
`ProfessionalBehavior.xml` 中 **`SplitIntoTwo` 为 `"0"` 但 `PullUpShimeji` 仍为 `"50"`**，
并非天然禁用——托管时这两个名字都必须被覆盖，不能只覆盖其中一个。

**(B) 桥命令 `createMascot`**

`GladosBridge.createMascot(role)` → `Main.createMascot(imageSet)` → `new Mascot(...)` + `manager.add(...)`。

注意：这条是**GLaDOS 主动下发**的，用于"再加一个角色"，语义上与"自主增殖"不同。
按需求「不允许 shimeji 的 spawn 多个桌宠的动作」指的是**自主增殖动作**，
而 GLaDOS 显式下发的 `createMascot` 属于宿主可控行为，应当保留（否则桥能力退化）。

**因此设计上必须区分"自主增殖"与"受控创建"**，这也是本方案最重要的判断。

### 2.4 现有可复用的机制

| 机制 | 位置 | 说明 |
|---|---|---|
| `GladosOptions` 参数解析 | `glados/GladosOptions.java` | 已有 `--glados-managed` 等参数，可扩展 |
| **原生 `Breeding` 开关** | `action/Breed.java:60` | 读 `properties.getProperty("Breeding", "true")`，false 时 `isEnabled()` 返回 false，`tick()` 跳过 `breed()` |
| `Transients` 开关 | 同上 | 控制瞬态角色，与本需求无关 |
| 托盘「禁止增殖」菜单 | `Main.java:435-443` | 原版就有，说明关闭增殖是**被官方支持的运行态** |
| 行为表覆盖 | `GladosBehaviors.loadInto()` | 读 `--glados-home/glados-behaviors.xml`，无则用 jar 内置 |
| 行为按名覆盖 | `Configuration.loadBehaviors()` → `getBehaviorBuilders().put(name, builder)` | **同名后写覆盖前写** |
| 频率 0 = 不参与随机 | `BehaviorBuilder.isEffective()` | `Frequency="0"` 直接返回 false，连候选集都不进，永不被选中 |
| 单角色收敛 | 桥命令 `remainOne` | 已实现，可作兜底 |

**关键发现：Shimeji 原生就有一个 `Breeding` 开关。**

`Breed.tick()` 的增殖判定是：

```java
if (delegate.isPenultimateFrame() && delegate.isEnabled()) {
    delegate.breed();
}
```

其中 `isEnabled()` 读的是 `properties.getProperty("Breeding", "true")`。原版托盘菜单里
还有「禁止增殖」勾选项，走的就是同一个键（`Main.java:435`）。也就是说**关闭增殖是
Shimeji 官方支持的运行态**，不是我们要发明的机制。

`settings.properties` 里目前**没有** `Breeding` 这一行，因此默认值是 `"true"`。
这带来一个远比"改 Breed 源码"更干净的方案：**托管时在内存里把该属性置为 `false`**：

- 不碰 `Breed.java`；
- 不碰任何 `conf/*.xml`（用户直跑用的原始配置一个字节都不改）；
- 用户直跑时没有这一步，属性保持默认 `true`，增殖照旧。

---

## 3. 方案选型

### 3.1 约束 2 的四种做法

| 方案 | 做法 | 优点 | 缺点 | 结论 |
|---|---|---|---|---|
| **A. 复用原生 `Breeding` 开关** | 托管时 `properties.setProperty("Breeding","false")` | **零源码改动**；复用官方支持的运行态；走原生 `isEnabled()` 判定，不碰逻辑；用户直跑完全不受影响 | 只在 `Breed` 生效，若将来出现别的增殖实现需另行处理 | ✅ **主方案** |
| B. 配置层禁频 | 在 `glados-behaviors.xml` 重新声明两个行为，`Frequency="0"` | 零 Java 改动；连动作都不播 | 与 A 目的重叠；若用户自带 conf 改名会落空 | ✅ **作为补充**（双保险，且避免动作空播）|
| C. Manager 层硬拦截 | `Manager.add()` 在托管且已有角色时拒绝 | 最彻底 | 影响面过大：会连带影响 `setRole` 的 `disposeAll`+`createMascot` 时序、以及合法的 `createMascot` 命令 | ❌ 不采用 |
| D. 桥层轮询收敛 | 定时 `remainOne` | 无需改 Shimeji | 有窗口期，用户仍会看到多余角色；轮询浪费 | ❌ 不采用 |

**采用 A 为主 + B 作为补充**：
- A 保证"不会真的产生新角色"（逻辑层，与行为名无关）；
- B 保证"连增殖动作都不播"（表现层，用户不会看到角色做分裂姿势）。

两者都不影响非托管运行：A 只在托管时执行一次赋值，B 的 `glados-behaviors.xml`
只在托管时加载（`Main.run()` 里 `if (gladosOptions.isManaged())` 才调 `GladosBehaviors.loadInto`）。

### 3.2 三条约束的统一形态

三条约束本质都是「托管时关闭某些原生能力」，且**都已在托管分支上**：

| 约束 | 实现点 | 形态 |
|---|---|---|
| 1 更新检查 | `Main.main()` | `if (!managed)` 才起更新线程 —— **已实现** |
| 2 禁止增殖 | `Main.run()` | 新增：`if (managed)` 置 `Breeding=false` —— **本轮实现** |
| 3 托盘图标 | `Main.run()` | `if (!managed)` 才 `createTrayIcon()` —— **已实现** |

三者都收敛在 `Main` 的托管分支里，**不需要给 `GladosOptions` 新增任何字段或静态开关**。
这比前一版方案（新增静态 `autonomousBreedingAllowed`）更小、更贴合现有结构。

---

## 4. 详细设计

### 4.1 核心改动：托管时关闭 `Breeding`

**文件**：`src/main/java/com/group_finity/mascot/Main.java`
**位置**：`run()` 中 properties 加载之后（当前第 140 行附近）

现状：

```java
//load setting
properties = OriginEngineFix.getInstance().getInstanceSettingProp();

// load langauges
try {
    loadLanguageProp();
} catch (Exception ex) { ... }
```

改为：

```java
//load setting
properties = OriginEngineFix.getInstance().getInstanceSettingProp();

// GLaDOS 托管模式：禁止自主增殖（SplitIntoTwo / PullUpShimeji）。
// 复用原版 Breeding 开关（Breed.isEnabled() 会读它，托盘菜单的「禁止增殖」也是同一个键），
// 因此不需要改动 Breed 或任何 conf/*.xml。
// 非托管时这段不执行，properties 保持 settings.properties 的原值 → 用户直接运行不受影响。
if (gladosOptions.isManaged()) {
    properties.setProperty("Breeding", "false");
    log.info("GLaDOS managed mode: disable autonomous breeding");
}

// load langauges
try {
    loadLanguageProp();
} catch (Exception ex) { ... }
```

为什么放在这里：
- `properties` 刚加载完，任何后续读取 `Breeding` 的地方都已生效；
- 在 `getInstance().run()` 的早期，早于 `createMascot` 和 `Manager.start()`；
- 只改**内存中的** `Properties` 对象，**不写回 `settings.properties` 文件**——
  这点很重要：如果写回文件，用户之后再自己直跑同一个 `conf/` 就会继承 `Breeding=false`，
  违背"不影响用户自己运行"。

`Breed.isEnabled()` 的现状无需改动：

```java
boolean isEnabled() throws VariableException {
    return ( getBornTransient()
        ? Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Transients", "true"))
        : Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Breeding", "true")));
}
```

`tick()` 里本就只在 `isEnabled()` 为真时才 `breed()`：

```java
if (delegate.isPenultimateFrame() && delegate.isEnabled())
    delegate.breed();
```

因此置 false 后：**不产生新角色，但动作动画照常播完**（角色会做个分裂姿势）。

### 4.2 补充：连增殖动作也不播

如果希望托管时**连分裂动画都不出现**（避免用户看到"它想分裂但没成功"的怪异感），
在 `glados-behaviors.xml` 追加同名覆盖、频率置 0：

**文件**：`src/main/resources/glados-behaviors.xml`

```xml
<!--
    GLaDOS 托管：禁用自主增殖行为。
    与 Main.run() 里的 Breeding=false 构成双保险：
    - 本文件 Frequency=0  → 不被自主行为树选中（连动作都不播）
    - Breeding=false      → 即使被显式构建也不会产生新角色
    注意：GLaDOS 主动下发的 createMascot 命令不受影响。
    本文件仅在托管模式下被加载，用户直跑不会读到。
-->
<Behavior Name="SplitIntoTwo" Frequency="0" Hidden="true" Action="Sit" />
<Behavior Name="PullUpShimeji" Frequency="0" Hidden="true" Action="Sit" />
```

要点：
- `Configuration.loadBehaviors()` 用 `put(name, builder)`，**同名覆盖**，因此这两行会顶掉
  `behaviors.xml` / `MischievousBehavior.xml` 里 `Frequency="50"` 的原始声明。
- 显式给 `Action="Sit"`：避免 `Configuration.validate()` 对缺 `Action` 的行为报错
  （与现有 `AIIdle` 的写法一致，稳妥）。
- `Frequency="0"` 时 `BehaviorBuilder.isEffective()` 开头直接 `return false`，连
  `candidates` 都进不去，因此 `buildNextBehavior()` 永不选中它（比"不累加权重"更严格）。

**已核实的加载顺序**（`Main.run()`）：

```
第 193 行  loadConfiguration(imageSet)        → Configuration.load(behaviors.xml)
                                              behaviors.xml 里 SplitIntoTwo = 50
第 209 行  GladosBehaviors.loadInto(config)    → loadGladosBehaviors(glados-behaviors.xml)
                                              → behaviorBuilders.put("SplitIntoTwo", 新builder)
                                              同名覆盖 → 实际频率 = 0
```

因 `put` 是后写覆盖，托管模式下最终生效的一定是 `Frequency="0"`。这条链路已在源码层确认。

### 4.3 保持不动（明确记录契约）

| 项 | 现状 | 结论 |
|---|---|---|
| 更新检查 `UpdateChecker` | `if (!managed)` 才检查 | 不改 |
| 托盘 `createTrayIcon()` | `if (!managed)` 才创建 | 不改 |
| `action/Breed.java` | 已有 `Breeding` 开关 | **不改**（复用） |
| `Manager.add()` | 通用增删 | 不改 |
| `GladosBridge.createMascot` | 显式受控创建 | 不改（GLaDOS 需要） |
| `conf/*.xml`（用户直跑配置） | 原始 | **一个字节都不改** |
| `ImageSetChooser`（无角色时弹选择框） | 受管时会弹 | **Shimeji 侧不改** |

关于 `ImageSetChooser`：曾考虑在受管时跳过弹框，但那是错的方向——正确做法是
GLaDOS 侧保证有皮肤才启动（已在 `startHeavy()` 前置校验 `ENGINE_MISSING`/`SKIN_MISSING`）。
Shimeji 保持原样，用户直跑时缺角色仍可选择，符合"不影响用户自己运行"。

---

## 5. 与 GLaDOS 侧的配合

Shimeji 侧改动全部是"托管时收紧"，GLaDOS 侧无需为这三条新增调用，但需要两处配合：

### 5.1 GLaDOS 侧：无需新增参数

现有启动命令已足够：

```
java -jar shimeji.jar --glados-managed --glados-port 0 --glados-home <dir>
```

`isManaged() == true` 即同时满足：跳过更新、不建托盘、禁止自主增殖。

### 5.2 GLaDOS 侧：状态桥需容忍"角色不会自行增殖"

现状 `ShimejiBridge` 会推送 `setBehavior`，并把 `count` 读回来展示。
由于增殖被禁用，`count` 将只会因 GLaDOS 的显式 `createMascot` / `remainOne` 变化。
**这是期望行为**，无需改代码；建议在 GLaDOS 侧需求文档中补一句说明，避免后续误判为 bug。

### 5.3 版本兼容

老的 Shimeji jar（不含本次改动）被 GLaDOS 加载时会继续增殖。
GLaDOS 已有 jar 内容校验（检查 `GladosBridge.class`），但无法区分"是否含禁止增殖改动"。
建议（可选，非阻塞）：

- 在 `/status` 响应里增加 `capabilities: ["noAutonomousBreed"]`；
- GLaDOS 读不到该能力时，退化为启动后调用一次 `remainOne` 作为兜底。

本方案将其列为**后续增强**，不在本轮实现，避免扩大改动面。

---

## 6. 验证清单

### 6.1 托管模式（GLaDOS 拉起）

```bash
java -jar target/shimeji-1.0.4.jar \
  --glados-managed --glados-port 0 --glados-home /tmp/h
```

| # | 验证项 | 期望 |
|---|---|---|
| 1 | 日志出现 `skip online update check` | ✅ |
| 2 | 日志出现 `disable autonomous breeding` | ✅ |
| 3 | 无任何更新弹窗 | ✅ 无窗口 |
| 4 | 无托盘图标 | ✅ Dock/托盘无 Shimeji 项 |
| 5 | 静置 5 分钟 | `GET /status` 的 `count` 恒为 1 |
| 6 | 反复拖拽、扔到墙上、闲逛 5 分钟 | `count` 仍为 1 |
| 7 | 观察是否出现分裂/增殖动作 | 不应出现（`glados-behaviors.xml` 已禁频）|
| 8 | `POST /control {cmd:createMascot}` | `count` 变为 2（受控创建仍可用）|
| 9 | `POST /control {cmd:remainOne}` | `count` 回到 1 |
| 10 | `POST /control {cmd:setRole}` | 正常切换，`count` 仍为 1 |
| 11 | 检查 `conf/settings.properties` 文件 | **不得被修改**（`Breeding` 不应写进文件）|

第 11 条很关键：验证我们只改了内存中的 `Properties`，没有回写文件。

### 6.2 非托管模式（用户直接运行）

```bash
java -jar target/shimeji-1.0.4.jar
```

| # | 验证项 | 期望 |
|---|---|---|
| 1 | 更新检查照常 | 有联网请求/弹窗（视版本）|
| 2 | 托盘图标存在 | ✅ |
| 3 | 托盘菜单能打开设置 | ✅ |
| 4 | 静置一段时间（或手动拖拽触发） | **会出现新角色**（增殖未被禁用）|
| 5 | 托盘菜单「禁止增殖」勾选状态 | 显示为**未勾选**（即允许增殖）|
| 6 | 无角色时启动 | 弹出 `ImageSetChooser` |

第 4、5 条是本方案最关键的回归验证：**必须确认非托管下增殖仍然工作**，
否则说明托管分支泄漏到了正常路径。

### 6.3 单元测试（新增）

建议新增 `src/test/java/com/group_finity/mascot/glados/GladosManagedConstraintsTest.java`：

| 用例 | 断言 |
|---|---|
| `parse` 无 `--glados-managed` | `isManaged() == false` |
| `parse` 含 `--glados-managed` | `isManaged() == true` |
| 托管时应用 `Breeding=false` 后 | `properties.getProperty("Breeding")` 为 `"false"` |
| 非托管时（不执行该分支）| `properties.getProperty("Breeding","true")` 仍为 `"true"` |
| `Breed.isEnabled()` 在 `Breeding=false` 下 | 返回 `false` |
| `Breed.isEnabled()` 在未设该键时 | 返回 `true`（默认值保底）|

注：`Breed.Delegate.isEnabled()` 是包私有，测试可放在同包下，或通过
`Main.getInstance().getProperties()` 间接断言。

**复核补充（2026-11-14）**：上述 8 个用例验证的是策略语义，但都在测试类里重写了
`applyManagedPolicy`，并未读取真实配置文件——`glados-behaviors.xml` 的两行禁频
覆盖被误删时，测试仍会全绿。为此补了 4 个"配置契约"用例，直接读 classpath 资源、
不触碰 `Main`/Swing（headless 安全）：

1. 内置行为表含 `SplitIntoTwo` / `PullUpShimeji` 两条覆盖，`Frequency="0"` 且 `Action="Sit"`；
2. `conf/behaviors.xml`、`MischievousBehavior.xml` 中两者仍为 `Frequency="50"`，
   并锁定 `CalmBehavior.xml` / `ProfessionalBehavior.xml` 的真实取值（见 §2.3）；
3. `conf/settings.properties` 不含 `Breeding` 键（不回写契约）；
4. `Divide1` / `PullUpShimeji1` 均为 `action.Breed`，且**未设** `BornTransient=true`
   ——否则 `isEnabled()` 会去读 `Transients` 而不是 `Breeding`，A 方案将被绕过。

### 6.4 回归：现有测试不得破坏

```bash
export PATH="$HOME/.local/maven/bin:$PATH"
mvn test -DskipTests=false    # 注意：pom.xml 里硬编码了 skipTests=true
```

⚠️ **重要**：`pom.xml` 第 18 行有 `<skipTests>true</skipTests>`，
直接 `mvn test` 会显示 "Tests are skipped" 而实际上一个测试都没跑。
必须显式传 `-DskipTests=false` 才能执行。

当前基线（复核补充后实测）：

```
Tests run: 20, Failures: 0, Errors: 0  -- GladosBridgeTest
Tests run: 12, Failures: 0, Errors: 0  -- GladosManagedConstraintsTest
Tests run: 32, Failures: 0, Errors: 0
BUILD SUCCESS
```

---

### 6.5 本轮已实测的结论

以下是 2026-11-14 在本机（macOS + Java 17）实际跑出来的结果，供实现时对照。

| 验证方式 | 结论 |
|---|---|
| 托管启动日志 | ✅ 同时出现 `skip online update check` 与 `disable autonomous breeding` |
| 托管启动日志 | ✅ `GLaDOS AI behaviors loaded from built-in resource: /glados-behaviors.xml` |
| 非托管启动日志 | ✅ **不出现**上述两条托管日志 |
| 非托管托盘 | ✅ 日志出现 `create a tray icon`；托管时无此日志 |
| `conf/settings.properties` | ✅ 两种模式跑完后文件内均**未出现** `Breeding` 键（确认只改内存、不回写） |
| 受控创建 | ✅ 托管下 `createMascot` 后 `count` 1→2 |
| 受控收敛 | ✅ `remainOne` 后 `count` 2→1 |
| 托管静置观察 | ✅ 90 秒内 `count` 恒为 1，且行为正常流转（StandUp→SitDown→RunAlongWorkAreaFloor→HoldOntoWall） |
| 单元测试 | ✅ 28 项全通过（当时新增 8 项；复核后为 32 项，见 §6.4） |

### 未能在本机完整实测的部分（需真机补测）

**自主增殖的端到端触发**未能在 headless/自动化环境观察到：

- `SplitIntoTwo` / `PullUpShimeji` 在 `behaviors.xml` 中被包在外层
  `<Condition Condition="#{mascot.environment.floor.isOn(mascot.anchor) || ...}">` 里，
  **只有角色站在地面或 IE 顶边框上时才有机会被选中**；
- 自动化环境下角色常停留在 `Fall` / `HoldOntoWall`，未稳定落到地面，
  因此即使把频率临时调到 1000 也没触发（非托管对照组同样没触发）。

**这意味着"托管下增殖被禁用"目前由以下三层证据支撑，而非端到端复现**：

1. 源码链路：`Breed.tick()` → `delegate.isEnabled()` → `properties.getProperty("Breeding","true")`；
2. `Main.run()` 托管分支确实把该属性置为 `"false"`（日志已验证）；
3. 行为覆盖：`loadConfiguration` 后再 `GladosBehaviors.loadInto`，`put` 同名覆盖
   使 `SplitIntoTwo`/`PullUpShimeji` 频率变为 0（加载顺序已在源码确认）。

**实现后必须补做**：在带图形界面的真机上，把角色拖到地面静置数分钟，
确认非托管会增殖、托管不增殖（即 §6.1 第 6 条与 §6.2 第 4 条）。
这是唯一能把结论从"逻辑正确"提升到"行为正确"的验证。

---

## 7. 改动清单汇总

| 文件 | 类型 | 说明 | 状态 |
|---|---|---|---|
| `src/main/java/com/group_finity/mascot/Main.java` | 修改（+6 行） | `run()` 中 properties 加载后，托管时置 `Breeding=false`（仅内存） | ✅ 已实现 |
| `src/main/resources/glados-behaviors.xml` | 修改（+13 行） | 追加 `SplitIntoTwo` / `PullUpShimeji`，`Frequency="0"` | ✅ 已实现 |
| `src/test/.../GladosManagedConstraintsTest.java` | 新增（8 用例）+ 复核补 4 用例 | 托管/非托管分支、`Breeding` 语义、真实配置文件契约 | ✅ 12 用例全通过 |
| `src/main/java/com/group_finity/mascot/config/Configuration.java` | Javadoc 修正（复核） | `loadGladosBehaviors` 原注释写「重名保留已有」，与 `put` 覆盖实现相反，而本方案正依赖覆盖语义 | ✅ 已修正，零行为改动 |
| `docs/glados-managed-constraints-design.md` | 新增 | 本文档（复核时修正 §2.3 事实、精确化 §4.2、补充 §8 限制） | ✅ 已实现 |

**不改动**：
- `Main.java` 的更新检查与托盘逻辑（已正确，保持原样）
- `action/Breed.java`（复用原生 `Breeding` 开关，**零改动**）
- `Manager.java`、`GladosBridge.java`、`GladosOptions.java` 源码（零改动）
- `Configuration.java` 源码逻辑（仅修正 Javadoc 措辞，无行为变更）
- **任何 `conf/*.xml` 与 `conf/settings.properties`**（用户直跑用的原始配置一个字节都不动）

改动量评估：Java 侧 6 行（+1 处注释修正），XML 侧 13 行（含注释），加 12 个测试与本文档。
这是本需求能达到的最小改动面。

---

## 8. 风险与取舍

| 风险 | 说明 | 缓解 |
|---|---|---|
| `Breeding` 是全局开关 | 它同时影响 `Transients` 之外的**所有** Breed 动作，若将来新增别的增殖动作也会一并被禁 | 这正是本需求要的效果；且原版托盘菜单就是用它，语义一致 |
| 只改内存不回写 | 若某处代码在运行期读文件而非 `properties`，会读不到 | 已确认 `Breed` 走 `Main.getInstance().getProperties()`，即内存对象 |
| 分裂动作仍会播 | A 方案只阻止"产生角色"，不阻止"播动作" | 由 B 方案（`glados-behaviors.xml` 禁频）消除 |
| 用户自带 conf 改名 | 用户若用自己的 `behaviors.xml` 把增殖行为改名，B 方案会落空 | A 方案与行为名无关，仍然生效 |
| 老 jar 兼容 | 老版本 Shimeji 无此改动，被 GLaDOS 加载时仍会增殖 | 见 §5.3，可选增强 |
| `--glados-home` 覆盖文件绕过 B 方案 | GLaDOS 若在 home 放自定义 `glados-behaviors.xml`，`GladosBehaviors.loadInto()` 会**完全替换**内置表（不是合并），本轮追加的两条 `Frequency="0"` 便不再生效 | A 方案 `Breeding=false` 与行为名、行为表无关，仍保证不产生新角色，只是可能播出分裂动画。若要求连动画也禁，GLaDOS 侧的覆盖文件需保留这两行，或后续把该约束移到代码层强制施加 |

### 关于"为什么不用新参数"

前一版设计考虑过给 `GladosOptions` 加静态开关 `autonomousBreedingAllowed`。否决理由：

1. Shimeji 已经有一个语义完全对应的 `Breeding` 开关，再造一个等于双轨；
2. 静态可变状态在测试里需要额外钩子复位，容易泄漏；
3. 复用 `Breeding` 后，`GladosOptions` 一个字段都不用加，`Breed.java` 一行都不用改。

---

## 9. 实施进度与顺序

### 9.1 进度总览（截至 2026-11-14）

| 阶段 | 内容 | 状态 | 证据 / 备注 |
|---|---|---|---|
| 需求设计与方案选型 | 三条约束；A（复用 `Breeding`）为主 + B（禁频）为补充 | ✅ 完成 | §1–§3 |
| 约束 1 不检测更新 | `Main.main()` 中 `if (!managed)` | ✅ 完成 | 提交 `ac42658`；托管日志 `skip online update check` |
| 约束 2 不展示托盘 | `Main.run()` 中 `if (!managed)` | ✅ 完成 | 提交 `ac42658`；`create a tray icon` 仅非托管出现 |
| 约束 3 禁止增殖（A 逻辑层） | `Main.run()` 置 `Breeding=false`，仅改内存 | ✅ 完成 | 提交 `4255ccc`；日志 `disable autonomous breeding` |
| 约束 3 禁止增殖（B 表现层） | `glados-behaviors.xml` 两条 `Frequency=0` 同名覆盖 | ✅ 完成 | 提交 `4255ccc`；加载顺序见 §4.2 |
| 托管回归验证 | 无更新框 / 无托盘、静置 `count` 恒为 1、受控创建 1→2→1、`settings.properties` 未回写 | ✅ 完成 | §6.1 / §6.5 |
| 非托管回归验证 | 更新检查、托盘与菜单正常、`conf/` 未被污染 | ✅ 完成 | §6.2 / §6.5（增殖端到端除外） |
| 单元测试 | 32 项全绿 | ✅ 完成 | §6.4；`mvn test -DskipTests=false` |
| 复核加固 | 覆盖语义 Javadoc 修正、4 个配置契约用例、文档事实修正与限制补充 | ✅ 完成 | 本轮复核（随本次提交），见 §6.3 / §7 / §8 |
| **真机端到端增殖对照** | 角色拖到地面静置，确认非托管会增殖、托管不增殖 | ⏳ **待办** | §6.5；headless 下角色不落到地面，无法复现，需 GUI 真机 |
| GLaDOS 侧联调 | 打包交付；启动参数无需变更 | ⏳ 待办 | §5.1；老 jar 能力探测为可选增强，见 §5.3 |

**当前状态**：代码、配置、文档、测试均已就绪，可按 §5.1 的启动参数直接交 GLaDOS 侧联调；
唯一未闭环的是「自主增殖」的端到端行为验证。该结论目前由源码链路、启动日志、
配置覆盖三层证据支撑，补测是把结论从「逻辑正确」提升到「行为正确」的最后一步。

### 9.2 原定实施顺序及执行情况

1. 非托管基线（记录一次分裂日志/截图）—— ⏳ 未取得，即 §6.5 所述 headless 限制。
2. `Main.run()` 置 `Breeding=false`，托管验证 §6.1 第 1–7 条 —— ✅ 完成。
3. `glados-behaviors.xml` 禁频 —— ✅ 完成（动作不播由 §4.2 覆盖链路保证，真机观察待补）。
4. 受控创建仍可用（§6.1 第 8–10 条）—— ✅ 完成（`count` 1→2→1、`setRole` 正常）。
5. `conf/settings.properties` 未被回写（§6.1 第 11 条）—— ✅ 完成，并已加契约用例锁定。
6. §6.2 非托管全部 6 条 —— ✅ 完成 1–3、5–6；第 4 条（会出现新角色）待真机补测。
7. `mvn test` 全绿后打包联调 —— ✅ 测试已全绿（32 项）；打包与联调待 GLaDOS 侧。