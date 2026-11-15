# Shimeji 桌宠动作设计与扩展能力需求文档

- 文档版本：v1.2（M1 开发完成，进度实时回写）
- 编写日期：2026-11-15
- 最后更新：2026-11-15
- 状态：**M0 + M1 代码与测试完成，仅剩真机 GUI 验收**（进度详见 §13；需求定义与验收标准不变）
- 适用范围：shimeji 仓库，`feature/glados-integration` 分支
- 关联文档：
  - `docs/glados-ai-integration-design.md`（GLaDOS AI 集成总设计，§5 AI 行为映射）
  - `docs/glados-managed-constraints-design.md`（托管模式约束，§8 覆盖文件限制）
- 文档约定：
  - 「**必须 / 应当 / 可以**」分别对应 MUST / SHOULD / MAY。
  - 「已核实」表示结论来自当前分支源码逐行核对，不是推测；「推导」表示由静态代码路径得出、尚需运行时复核。
  - 本文描述的是**需求与验收标准**。文中给出的源码位置与候选实现方向仅用于说明影响范围，不构成对实现方案的约束；具体方案由开发评审决定。
  - 所有行号以 2026-11-15 当前工作区代码为准，重构后需同步更新附录 A。

---

## 目录

1. 背景与问题定义
2. 术语与概念模型
3. 现状核实（事实基线）
4. 用户故事与场景
5. 功能需求
6. 非功能需求
7. 约束与边界（明确不做）
8. 验收标准与测试计划
9. 风险与依赖
10. 里程碑建议
11. 待决问题
12. 附录
13. 开发进度（实时回写）

---

## 1. 背景与问题定义

### 1.1 业务背景

Shimeji 作为 GLaDOS 的「重量级桌宠」引擎运行，宿主通过 HTTP 控制桥下发角色与行为。
随着 AI 状态（idle / working / waiting / review / failed / disconnected）不断丰富，
需要**持续新增、调整、预览桌宠动作**，且动作设计者可能不是 Java 开发者。

当前项目的动作系统本身是数据驱动的：`conf/actions.xml` 声明动作、
`conf/behaviors.xml` 声明行为选择，条件表达式由 Nashorn JS 求值，
不写 Java 也能改出现有动作的表现。
但「**从零设计一套新动作并快速看到效果**」这条链路仍然是断开的。

社区侧结论一致：Shimeji-ee 官方 wiki 明确提到，修改现有配置相对容易，
但「从零写一套配置非常耗时、全靠试错」；目前不存在成熟的社区可视化动作编辑器。

### 1.2 问题清单

| # | 痛点 | 具体表现 |
|---|---|---|
| P1 | **单文件巨石** | 全部动作挤在 727 行的 `actions.xml` 里，无 include、无模块化；多个设计者或皮肤共改一份文件，冲突频繁。 |
| P2 | **无法增量覆盖** | 同名 `Action` 直接抛异常，而同名 `Behavior` 是覆盖语义，两者不对称；无法做「官方基础包 + 只覆盖几个动作」。 |
| P3 | **改完看不到** | 没有干净的重载入口：只有切换语言、托盘「选择形象」会触发 `disposeAll → loadConfiguration`，前者还会写回 `settings.properties`，后者每次弹框。 |
| P4 | **表达力受限** | 帧参数 `Velocity / Duration / ImageAnchor` 走 `Integer.parseInt`，不能写 `${}`，动作无法参数化复用，只能复制粘贴；动作类型硬编码为六种 if-else。 |
| P5 | **反馈滞后** | 名字写错、图片缺失、节点名不匹配等问题，要么启动才报错且提示模糊，要么**静默不生效**（见 §3.5 D-1）。 |

### 1.3 目标

| 编号 | 目标 | 度量方式 |
|---|---|---|
| G1 | 新增一个「组合型」动作无需写 Java、无需新增图片、无需重启进程 | 从改 XML 到屏幕上看到效果 ≤ 1 次请求，步骤数 ≤ 3 |
| G2 | 动作资产可按主题拆分文件、可增量覆盖、可归属于单个皮肤 | 支持 `actions.d/*.xml` 与皮肤级 `conf/`，且同名覆盖有明确语义与日志 |
| G3 | 设计期错误可诊断 | 引用不存在的动作/行为、缺图、非法参数在加载阶段给出含文件名与名字的错误 |
| G4 | 不牺牲现有运行方式 | 用户直接双击运行的行为与性能零变化；GLaDOS 托管约束不回归 |
| G5 | 为后续可视化编辑器预留通道 | 预览通道以稳定命令契约固化，不依赖弹框或重启 |

### 1.4 非目标

- 不重做自主行为选择算法（保留 `Frequency` 加权随机 + `NextBehaviorList`）。
- 不把动作系统改写成行为树 / 状态机 / ECS。
- 不在本期交付可视化动作编辑器（本期只保证其所需的通道与数据契约就绪）。
- 不更换 XML 为 JSON/YAML DSL（作为 P2 之后的独立议题）。
- 不改动 `conf/*.xml` 与 `conf/settings.properties` 的既有内容作为需求实现手段。

---

## 2. 术语与概念模型

### 2.1 术语表

| 术语 | 含义 | 声明位置 |
|---|---|---|
| **Action（动作）** | 一段可播放的运动/动画逻辑，6 种类型：`Embedded / Move / Stay / Animate / Sequence / Select` | `actions.xml` 的 `<ActionList>` |
| **Behavior（行为）** | 自主行为树的候选节点，带 `Frequency / Condition / Hidden / Toggleable`，以及后继行为列表 | `behaviors.xml` 的 `<BehaviorList>` |
| **ActionReference** | 动作内对另一个动作的按名引用，可携带参数覆盖 | `<Action>` 子节点 |
| **Animation / Pose** | 动画及其帧；每帧含图片路径、锚点、位移、时长 | `<Action>` 子节点 |
| **imageSet** | 当前角色皮肤目录名，配置加载时注入，决定所有图片路径前缀 | `Main.loadConfiguration(imageSet)` |
| **自主行为树** | `Configuration.buildNextBehavior()` 的加权随机选择器 | 运行时 |
| **受控行为** | `Frequency="0"`、不参与随机、仅由 `setBehavior` 按名构建的行为 | `GladosBridge` |
| **动作包** | 本文 proposed：按主题拆分、可增量叠加的一组 XML 文件 | 尚不存在 |

### 2.2 三层名字绑定关系（重要）

本系统**没有引用对象、没有注册表、没有 ID**，全部是按字符串查表：

| 层级 | 绑定方式 | 代码位置 | 校验 |
|---|---|---|---|
| Behavior → Action | 默认：**同名**。`<Behavior Name="StandUp"/>` 自动找 `<Action Name="StandUp">`；可用 `Action="Sit"` 显式改名 | `BehaviorBuilder.java:52` | `BehaviorBuilder.validate()` (123–129)，找不到 Action 启动报错 |
| Action → 子 Action | `<ActionReference Name="Sit" Duration="300"/>` 按名查 `actionBuilders` | `ActionRef.java:31, 62–66` | `ActionRef.validate()` (55–60) |
| Pose → 图片 | `<Pose Image="/shime1.png">` 拼成 `img/<imageSet>/shime1.png` 路径字符串 | `AnimationBuilder.java:92–140` | 加载时读盘，失败抛 `FailedLoadImageErrorMessage` |

**已核实的统计事实**：原生 `behaviors.xml` 的 57 个 Behavior 名字，**100% 存在同名 Action**，且整个 `conf/` 中没有一处使用显式 `Action=`。
反例恰在 GLaDOS 侧：`glados-behaviors.xml` 的 8 个 AI 行为（如 `AIWorking` → `Walk`）全部显式写 `Action=`，用于让多个行为复用同一个动作。

**参数传递规则及已知限制**：`Behavior` 上除 `Name / Action / Frequency / Hidden / Condition / Toggleable` 六个保留字段外的所有属性，会作为参数注入 Action 的变量表；`ActionReference` 上的属性再覆盖一层。
但 `ActionBuilder.createActions()`（152–158）给子动作传的是**空 Map**，因此**参数不会向下渗透到 `Sequence` 的子动作**——写在 Behavior 上的 `Duration` 到不了子动作，必须写在每个 `ActionReference` 上。这是设计新组合动作时最常见的踩坑点。

### 2.3 图片并非「绑定」到动作

- `Sequence` / `Select` 构造时传入的是**空 `Animation` 列表**（`ComplexAction.java:25–32`），因此组合型动作**完全没有帧、不需要任何图片**。
- 只有 `Stay / Move / Animate` 的 `<Pose>` 才引用图片；`ImagePairLoader` 在**配置加载时**就把 PNG 读进内存并按路径字符串缓存，运行时 `Pose.next()` 只是查表 `mascot.setImage(...)`。
- 多个 Action 引用同一张图片零额外成本（缓存按路径去重）。
- 不写 `ImageRight` 时程序自动水平翻转生成右朝向图（`ImagePairLoader.java:33–34`），新图只需画一个朝向。
- `ImageAnchor="64,128"` 是像素锚点，更换图片尺寸时必须同步修改。

---

## 3. 现状核实（事实基线）

### 3.1 配置加载链路（已核实）

```
Main.loadConfiguration(imageSet)                       Main.java:272
  ├─ 选 actions.xml：                                  274–286
  │    <BASE_ENV>/<imageSet>/actions.xml
  │  > <BASE_IMG>/<imageSet>/conf/actions.xml
  │  > <BASE_ENV>/actions.xml（全局回退）
  ├─ configuration.load(actions 根节点, imageSet)       294
  │    └─ 逐个 new ActionBuilder(...)，同名抛异常          68–82
  ├─ 选 behaviors.xml（同样的三级回退）                   296–304
  ├─ configuration.load(behaviors 根节点, imageSet)      311   ← 第二次调用，可重入
  ├─ configuration.validate()                           313
  └─ configurations.put(imageSet, configuration)         315

托管模式追加：GladosBehaviors.loadInto() → Configuration.loadGladosBehaviors()  115–121
  └─ 同名 Behavior 后写覆盖（put），禁增殖 Frequency=0 依赖此语义
```

`Main.refreshLanguage()`（906–929）是目前唯一较完整的重载路径：
`loadLanguageProp() → disposeAll() → 逐个 loadConfiguration() → 逐个 createMascot()`。
但它绑定在语言切换语义上，会写回语言设置，且没有对外命令入口。

### 3.2 关键数字（2026-11-15 实测）

| 项目 | 数量 | 说明 |
|---|---|---|
| `<Action Name=...>` | 91 | `Animate 4 / Embedded 12 / Move 6 / Sequence 59 / Stay 10` |
| 内联匿名 `<Action>` | 8 | 含 3 个 `Select`，`ActionBuilder.java:66–68` 支持 |
| `<ActionReference>` | 198 | 组合能力的主要来源 |
| `<Pose>` | 132 | 分布在需要帧的动作里 |
| 被引用图片 | 46 | 与 `img/Shimeji/*.png` 的 46 张一一对应 |
| `<Behavior Name=...>` | 57 | 100% 有同名 Action |
| GLaDOS AI 行为 | 8 | 全部 `Frequency=0 Hidden=true` + 显式 `Action=` |
| HTTP 命令 | 8 | `setRole / setBehavior / pause / resume / createMascot / remainOne / disposeAll / quit`，**无 reload** |

构建环境：Java 11、`nashorn-core`（条件表达式引擎）、`pom.xml` 硬编码 `<skipTests>true</skipTests>`（跑测试必须显式 `-DskipTests=false`）。

### 3.3 已具备的扩展能力（不得在改造中丢失）

| 能力 | 说明 |
|---|---|
| 数据驱动 | 动作与行为全在 XML，改表现无需编译 Java |
| 可编程条件 | `${}` / `#{}` 走 Nashorn JS；`ScriptFilter` 已开放 `com.group_finity.mascot.*` 给脚本；`Animation` 的 `Condition`、`Behavior` 的频率条件、动作参数均可写表达式 |
| 组合能力 | 91 个动作里 59 个是 `Sequence`；`ActionReference` 可携带参数覆盖 |
| 皮肤级隔离 | `Main.loadConfiguration` 支持 `img/<角色>/conf/actions.xml` 覆盖全局 `conf/`（前提是**整份替换**，不是合并） |
| 受控预览通道 | `POST /control {cmd:setBehavior, behavior:"..."}` → `Manager.setBehaviorAllByName` → `Configuration.buildBehavior(name)`；`Frequency=0` 不进自主树但可按名构建 |
| 反射型动作 | `Type="Embedded" Class="..."` 通过 `Class.forName` 加载任意 `Action` 类（无 SPI、无注册、无文档） |

### 3.4 已核实的结构性缺口

| 编号 | 缺口 | 代码位置 | 对设计工作流的影响 |
|---|---|---|---|
| G1 | 动作类型硬编码 6 分支 if-else | `ActionBuilder.java:98–136` | 新增动作类型必须改 Java 并重编译 |
| G2 | 同名 Action 抛异常，与 Behavior 覆盖语义不对称 | `Configuration.java:75–77` | 无法做基础包 + 增量覆盖 |
| G3 | 帧参数 `Velocity / Duration / ImageAnchor` 全走 `Integer.parseInt` | `AnimationBuilder.java:201–222` | 帧参数不能写 `${}`，动作无法参数化复用 |
| G4 | 图片路径写死 `img/<imageSet>/<file>` + dev/prod 双分支 | `AnimationBuilder.java:110–140` | 不支持 classpath、资源别名、精灵图；皮肤必须提供同名全套 PNG |
| G5 | 单文件、无 include、无模块化 | `Main.java:274–291` | 所有皮肤共享一份 `actions.xml` |
| G6 | 无干净的显式重载 | `Main.java:906–929`；`GladosCommand` 无 reload | 改 XML 后必须重启或弹框 |
| G7 | 图片缓存无失效机制 | `ImagePairs.clear()` / `removeAll()` 全仓零调用 | 加图、改同名 PNG 后必须重启进程 |
| G8 | 设计时校验弱 | `Configuration.validate()` 仅校验名字存在 | 缺图、非法数值、孤立引用、节点名拼错等反馈滞后或静默 |

### 3.5 已核实的缺陷（需在本期回归测试中固化）

**D-1（高）· `NextBehavior` 节点名与 schema 映射不匹配**

- `schema.properties` / `schema_en.properties` / `schema_ja.properties` 中，
  `NextBehaviourList` 分别映射为 `NextBehaviorList` / `NextBehaviorList` / `次の行動リスト`。
- `BehaviorBuilder.java:82` 用 `selectChildren(schema.getString("NextBehaviourList"))` 查找子节点；
  `Entry.selectChildren`（89–108）是**精确字符串匹配**。
- 但 `conf/behaviors.xml`、`CalmBehavior.xml`、`MischievousBehavior.xml`、`ProfessionalBehavior.xml`
  共 28 处写的是 `<NextBehavior ...>`（**不带 List**）；
  只有 `glados-behaviors.xml` 的 5 处写的是 `<NextBehaviorList>`。
- 推导结论：原生自主行为配置里的后继行为列表**不会被解析**，`nextBehaviorBuilders` 为空，
  `nextAdditive` 保持默认 `true`，行为选择退化为纯全局加权随机。
  这需要运行时日志或专项单测复核；**无论结论如何，均须在 FR-08 中加一致性回归测试**。
- 修复方向不在本文约束范围（改 schema 映射或改 XML 节点名，二选一，且必须回归全部行为配置）。

**D-2（中）· `Mascot.xsd` 的 Type 枚举过时**

`Mascot.xsd:56–61` 枚举为 `Embedded / Move / Pause / Fixed / Composite / Select`，
而运行时实际支持 `Embedded / Move / Stay / Animate / Sequence / Select`（`ActionBuilder.java:98–136`）。
运行时 `DocumentBuilderFactory` 并未开启 XSD 校验，因此不影响运行；
但设计者若在 IDE 里按 XSD 做补全/校验，会得到误报（`Stay/Animate/Sequence` 被判非法）。

**D-3（中）· 图片缓存永不失效**

见 G7。`ImagePairs` 提供了 `clear()` 与 `removeAll()`，但全仓无调用点。
这直接影响 FR-03「显式重载」的实际效果：只重载 XML 而不清缓存，会出现「配置改了、画面没变」。

---

## 4. 用户故事与场景

| 编号 | 角色 | 用户故事 | 关联需求 |
|---|---|---|---|
| US-01 | 动作设计者 | 我想把已有动作拼成一个新动作（例如「原地转三圈再坐下」），希望只改 XML 就能完成，不写 Java、不画新图 | FR-01, FR-11, FR-12 |
| US-02 | 动作设计者 | 改完 XML 后，我希望发一个请求就能立刻在桌面上看到新动作，不用重启、不弹框 | FR-03, FR-04, FR-11 |
| US-03 | 皮肤作者 | 我只想给某个角色覆盖 2 个动作，不想把 91 个动作全复制一份 | FR-01, FR-02, FR-10 |
| US-04 | 动作设计者 | 我想让同一个动作在不同场景下速度/时长不同，而不是复制三份只改数字 | FR-05 |
| US-05 | 集成方（GLaDOS） | 我希望 AI 状态到动作的映射能持续扩展，且不会意外进入自主行为树 | FR-11, NFR-02 |
| US-06 | 维护者 | 配置写错时，我希望启动时明确告诉我是哪个文件、哪个名字错了 | FR-08, FR-09 |
| US-07 | 终端用户 | 我直接双击运行 Shimeji，希望上述所有改造对我的使用毫无影响 | NFR-01 |

---

## 5. 功能需求

### 5.0 优先级约定

| 级别 | 含义 |
|---|---|
| **P0** | 零代码，靠规范与现有能力即可落地；本期必须形成文档与示例 |
| **P1** | 本期核心交付，改动面小、收益最大 |
| **P2** | 表达力增强，方案需单独评审 |
| **P3** | 工具链/生态，本期只保证通道就绪 |

每条需求含：描述、动机、验收标准（AC）、影响范围、依赖。

---

### 5.1 A 组 · 创作效率

#### FR-01 动作包分文件（P1）

**描述**：系统**必须**支持把一个角色的动作定义拆分到多个 XML 文件中，按确定的顺序依次加载并合并到同一个 `Configuration`；行为定义（`behaviors.xml`）**应当**同样支持。

**动机**：解除 727 行单文件的协作冲突；为皮肤增量覆盖提供载体（G5）。

**验收标准**：
- AC-01-1：存在约定的动作包目录（建议 `conf/actions.d/*.xml`）时，目录下所有 `.xml` 按**文件名字典序**依次加载；加载顺序必须确定且可日志记录。
- AC-01-2：目录下的文件可以是「只有 `<ActionList>` 的片段」，不要求每个文件都是完整 `Mascot` 文档（或明确要求完整文档并写入规范，二选一，须一致）。
- AC-01-3：目录为空或不存在时，行为与当前版本完全一致（回退到单一 `actions.xml`）。
- AC-01-4：全部文件加载完成后只执行一次 `validate()`，确保跨文件引用（A 文件引用 B 文件定义的 Action）能被正确解析。
- AC-01-5：加载日志能列出本次实际读取的每个文件路径与顺序。

**影响范围**：`Main.loadConfiguration()`（272–315）；`Configuration.load()` 已核实可重入（294、311 连续调用两次）。
**依赖**：无。
**备注**：皮肤级路径 `img/<角色>/conf/actions.d/` 是否同样生效，需与 FR-10 一并决策。

---

#### FR-02 同名 Action 覆盖（P1）

**描述**：同一 `Configuration` 中重复声明同名 `Action` 时，系统**必须**采用「后写覆盖前写」语义并输出一条 warn 日志，**不得**抛异常。

**动机**：与 Behavior 覆盖语义对齐；支持「官方基础包 + 用户覆盖包」（G2 / US-03）。

**验收标准**：
- AC-02-1：两个文件声明同名 `Action`，后加载者生效；`buildAction(name)` 返回后者定义的类型与参数。
- AC-02-2：覆盖发生时日志包含被覆盖的动作名、来源文件（能区分基础包与覆盖包）。
- AC-02-3：单一文件内重复声明同名 `Action` **仍应报错**（防止同一文件内的笔误），或明确采用同样覆盖语义并在文档中写死；两种策略必须全局一致。
- AC-02-4：`Behavior` 的同名覆盖语义保持不变（`Configuration.java:137` 的 `put`），特别是 `glados-behaviors.xml` 的 `Frequency=0` 禁增殖覆盖不得回归。
- AC-02-5：`ActionReference` 对被引用的覆盖后动作解析正确，`validate()` 通过。

**影响范围**：`Configuration.java:68–82`。
**依赖**：FR-01（否则没有「后写」的来源）。
**风险**：覆盖是全局替换而非字段合并，需在文档中明确，避免设计者以为可以只改一个属性。

---

#### FR-03 显式配置重载（P1）

**描述**：系统**必须**提供一个不依赖 UI 弹框、不重启进程的显式重载入口，重新加载动作与行为配置并使当前角色应用新配置。

**动机**：把「改 XML → 看效果」从「重启 + 重新拖角色」缩短为一次调用（G1 / US-02）。

**验收标准**：
- AC-03-1：通过 HTTP 控制桥下发重载命令后返回明确结果（成功 / 失败原因 JSON）。
- AC-03-2：重载后新声明的 Action / Behavior 可被 `setBehavior` 按名构建；被删除的名字返回 `unknown behavior`。
- AC-03-3：重载期间不得导致进程退出。当前 `refreshLanguage()` 通过临时关闭 `exitOnLastRemoved`（914–928）实现，重载实现必须保留等价保护。
- AC-03-4：重载**不得**写回 `conf/settings.properties`，不得修改语言设置，不得弹出任何 Swing 窗口。
- AC-03-5：重载失败（XML 解析错误 / 校验失败）时，**必须**保持旧配置继续可用，并返回含文件名与错误原因的响应；不得留下「角色全被 dispose 掉、新配置又没加载」的中间态。
- AC-03-6：命令必须纳入现有命令白名单校验（`GladosCommand.fromWireName` / `validate`），未知命令仍被拒绝。
- AC-03-7：非托管模式（用户直接运行）下，该入口可以只提供本地/程序内调用，不必暴露 HTTP；但行为语义一致。

**影响范围**：`Main.refreshLanguage()`（906–929）逻辑抽取；`GladosCommand` 枚举（当前 8 个命令）；`GladosBridge` 命令分发。
**依赖**：FR-04（否则图片相关改动不会生效）。
**备注**：`Configuration` 实例缓存在 `Main.configurations` map 中，重载需保证该 map 被正确替换，且 `Main.getConfiguration(imageSet)`（931–934）返回新实例。

---

#### FR-04 重载时刷新图片缓存（P1）

**描述**：显式重载**必须**同时使图片缓存失效，使新增或内容变更的 PNG 在下一次渲染时生效。

**动机**：消除 D-3 / G7 导致的「配置改了、画面没变」（US-02）。

**验收标准**：
- AC-04-1：重载后新增一张 PNG 并在 Pose 中引用，画面显示新图（不重启进程）。
- AC-04-2：重载后替换同名 PNG 的内容，画面显示新内容。
- AC-04-3：刷新范围限于本次重载涉及的 `imageSet`，不得影响其他已加载角色的画面；或明确全量刷新并说明可接受的短暂闪烁。
- AC-04-4：刷新不得与正在渲染的帧产生并发异常（`ImagePairs` 底层是 `ConcurrentHashMap`）。

**影响范围**：`ImagePairs`（提供 `clear()` 与 `removeAll()`，当前零调用）；重载入口。
**依赖**：FR-03。

---

### 5.2 B 组 · 表达力

#### FR-05 帧参数表达式化（P2）

**描述**：`<Pose>` 的 `Velocity` / `Duration` / `ImageAnchor` **应当**支持 `${}` / `#{}` 表达式求值，与 `Animation.Condition`、动作参数保持一致。

**动机**：同一个动作可按不同速度/时长复用，消除复制粘贴（G3 / US-04）。

**验收标准**：
- AC-05-1：`Duration="${500+Math.random()*1000}"` 可正常求值，纯整数字面量行为不变。
- AC-05-2：表达式中可访问已有变量上下文（如 `mascot`、Action 参数），非法表达式在加载阶段报出含 Pose 位置的错误。
- AC-05-3：`Velocity` 的两个分量、`ImageAnchor` 的两个分量分别求值；求值结果参与现有 `scaling` 换算逻辑（`AnimationBuilder.java:220–221`），不得绕过缩放。
- AC-05-4：现有 132 个 Pose 全部仍能加载成功，且表现与改造前一致。

**影响范围**：`AnimationBuilder.loadPose()`（183–222）。
**依赖**：无。
**风险**：`Duration` 若被求值为 0 或负数，`Animation.getDuration()`（70–79）可能除零；必须定义取值下限并校验。

---

#### FR-06 图片路径解绑（P2）

**描述**：系统**应当**支持除 `img/<imageSet>/<file>` 之外的资源定位方式，至少包含相对子目录；**可以**进一步支持 classpath 资源别名。

**动机**：解除「每个皮肤必须提供同名全套 PNG」的强耦合（G4），降低新皮肤成本。

**验收标准**：
- AC-06-1：`Image="sub/foo.png"` 能正确解析到 `<imageSet>/sub/foo.png`（现有 `Paths.resolve` 已具备此能力，需补测试固化）。
- AC-06-2：图片文件缺失时，错误信息包含**完整解析后路径**与所在 Action / Pose，而非泛化的 `FailedLoadImageErrorMessage`。
- AC-06-3：dev / prod 两种路径分支（`Constant.isDevEnvironment`）行为一致。
- AC-06-4：现有 46 张图片与 132 个 Pose 的解析结果不变。
- AC-06-5：不引入对 `shimeN.png` 命名的新依赖；`ImageSetChooser.java:194, 214` 用 `shime1.png` 做皮肤预览的既有约定需保留或提供替代预览图配置。

**影响范围**：`AnimationBuilder.java:99–182`；`ImageSetChooser`。
**依赖**：FR-08（缺图诊断）。

---

#### FR-07 动作类型可扩展（P2）

**描述**：新增动作类型**应当**无需修改 `ActionBuilder` 的 if-else 链；**可以**提供基于脚本的动作类型，让设计者用 Nashorn JS 编写简单动作逻辑。

**动机**：G1；让「设计一个全新动作」不必回到 Java 编译。

**验收标准**：
- AC-07-1：存在可注册的工厂机制（如 `ServiceLoader` 或配置化映射），第三方动作类型可声明式接入。
- AC-07-2：现有 6 种类型（`Embedded / Move / Stay / Animate / Sequence / Select`）行为零变化。
- AC-07-3：若实现脚本型动作，必须明确其可访问对象范围，不得绕过 `ScriptFilter` 已有的包级白名单；脚本执行异常不得导致进程退出。
- AC-07-4：新类型的 `validate()` 语义（是否允许无 Animation、是否允许子动作）有明确定义与测试。
- AC-07-5：`Embedded` 的 `Class.forName` 反射路径保持兼容。

**影响范围**：`ActionBuilder.java:98–136`；`ScriptFilter`。
**依赖**：FR-08。
**备注**：本条为 P2，允许本期只产出方案评审与接口预留，不要求实现。

---

### 5.3 C 组 · 可靠性与工具链

#### FR-08 设计时校验与可诊断错误（P1）

**描述**：配置加载阶段**必须**能检出并明确报告：引用不存在的 Action / Behavior、图片文件缺失、非法数值、schema 与节点名不一致。

**动机**：P5；避免 D-1 这类「静默不生效」问题再次发生。

**验收标准**：
- AC-08-1：所有错误信息包含**来源文件名 + 元素名 + 期望值**，能定位到具体一处配置。
- AC-08-2：新增一致性检查：`schema.properties` 中每个标签映射值，与实际 XML 中出现的同名/近名节点进行比对；发现 `<NextBehavior>` 与 `NextBehaviorList` 不匹配时，至少输出 warn 日志。
- AC-08-3：加载阶段预检所有 Pose 引用的图片文件是否存在，汇总报告全部缺失项（而不是遇到第一张就中断）。
- AC-08-4：新增单元测试覆盖 D-1：`conf/*.xml` 中所有后继行为节点名必须与 `schema.getString("NextBehaviourList")` 一致。
- AC-08-5：校验失败信息对非开发者可读，避免只打印异常堆栈。

**影响范围**：`Configuration.validate()`（180–188）、`BehaviorBuilder.validate()`、`AnimationBuilder.loadPose()`。
**依赖**：无。
**备注**：D-1 的**修复**可作为独立缺陷单处理，但 AC-08-4 的回归测试属于本期必须交付项。

---

#### FR-09 XSD 与运行时对齐（P2）

**描述**：`Mascot.xsd` 的 `Type` 枚举**必须**与运行时支持的 6 种类型一致；运行时校验策略（是否启用 XSD）**应当**明确并写入文档。

**动机**：D-2；让 IDE 校验成为助力而非误报源。

**验收标准**：
- AC-09-1：XSD 枚举包含且仅包含 `Embedded / Move / Stay / Animate / Sequence / Select`。
- AC-09-2：现有 `conf/*.xml` 全部通过 XSD 校验（若决定启用运行时校验）。
- AC-09-3：文档中说明「运行时是否按 XSD 校验」以及设计者应如何在 IDE 中启用校验。

**影响范围**：`src/main/resources/conf/Mascot.xsd:56–61`。

---

#### FR-10 皮肤级增量覆盖（P2）

**描述**：单个皮肤**应当**能够只声明少量动作/行为增量，其余继承全局配置，而不是整份 `actions.xml` 替换。

**动机**：US-03；当前 `Main.loadConfiguration` 的皮肤级路径是「找到就用、整份替换」（274–286），皮肤作者必须复制全部 91 个动作。

**验收标准**：
- AC-10-1：皮肤目录只有增量文件时，未声明的动作仍可从全局配置解析。
- AC-10-2：增量中同名 Action / Behavior 的覆盖优先级明确：全局 < 皮肤 < 托管覆盖表（`glados-behaviors.xml`）。
- AC-10-3：`--glados-home/glados-behaviors.xml` 存在时的「完全替换内置 AI 表」语义保持不变（见 `docs/glados-managed-constraints-design.md` §8），本需求不得改变该契约。
- AC-10-4：多个皮肤各自加载，互不影响；`configurations` map 的 key 规则不变。

**影响范围**：`Main.loadConfiguration()`；`GladosBehaviors.loadInto()`。
**依赖**：FR-01、FR-02。

---

#### FR-11 预览通道契约固化（P0）

**描述**：**必须**以文档与回归测试固化「新增动作如何被即时预览」的标准做法，并明确 `Frequency=0` 的语义。

**动机**：US-01 / US-05；这是 G1 达成的关键，也是未来可视化编辑器的接入点。

**验收标准**：
- AC-11-1：文档给出可复制的最小示例（见附录 B）：一个同名 `Action` + 一个 `Frequency="0" Hidden="true"` 的 `Behavior` + 一次 `setBehavior` 请求。
- AC-11-2：新增回归测试断言 `BehaviorBuilder.isEffective()` 对 `Frequency=0` 返回 false，但 `Configuration.buildBehavior(name)` 仍能按名构建（当前实现 144–161、293–299 已具备）。
- AC-11-3：明确 `setBehavior` 的 `idle` 特例会回到 `buildNextBehavior(null, mascot)` 自主树（`GladosBridge.java:279–293`），新动作不得依赖 idle 路径预览。
- AC-11-4：明确 `Stay` / `Move` 的 `BorderType="Floor"` 约束：角色不在地面时会抛 `LostGroundException` 回落 `Fall`，预览需在地面进行。
- AC-11-5：明确参数不透传限制（§2.2），示例中 `Duration` 写在 `ActionReference` 上而非 `Behavior` 上。

**影响范围**：`GladosBridge.setBehavior()`（275–300）；`Configuration`；文档与测试。
**依赖**：无。

---

#### FR-12 文档与示例资产（P0）

**描述**：**必须**提供面向动作设计者的使用文档与可运行示例。

**验收标准**：
- AC-12-1：一份「动作设计指南」，覆盖：名字绑定规则、6 种动作类型、`Sequence`/`Select` 组合、`${}`/`#{}` 表达式语法、参数传递与不透传限制、图片引用与命名、`ImageRight` 自动翻转、`ImageAnchor` 含义。
- AC-12-2：至少 2 个可运行示例：①零新图的组合动作（附录 B）；②带新 PNG 的帧动作。
- AC-12-3：一份排障清单：名字拼错、缺图、节点名不匹配、图片缓存未刷新、角色不在地面等常见现象与对应日志关键字。
- AC-12-4：示例不得写回 `conf/` 或 `conf/settings.properties`；示例资产放在独立目录，用户直接运行不受影响。

**影响范围**：`docs/`、`src/main/resources/` 下新增示例目录。
**依赖**：无。

---

## 6. 非功能需求

| 编号 | 类别 | 要求 | 验证方式 |
|---|---|---|---|
| NFR-01 | 兼容性 | 用户直接双击运行（非托管）时，启动流程、自主行为、托盘、更新检查、画面表现**零变化** | 托管/非托管双跑对照，§8.4 |
| NFR-02 | 托管约束 | `docs/glados-managed-constraints-design.md` 的三条约束（不自检更新 / 不展示托盘 / 不自主增殖）不得回归；`Configuration.isBehaviorEnabled` 的硬过滤与 `glados-behaviors.xml` 的 `Frequency=0` 覆盖均保持有效 | 运行现有 `GladosManagedConstraintsTest` 与新增契约用例 |
| NFR-03 | 启动性能 | 动作包拆分为 ≤ 30 个文件时，配置加载耗时相对基线增长 ≤ 20%；`validate()` 仍为单次全量 | 计时对比 |
| NFR-04 | 内存 | 图片缓存按路径去重，不因动作包拆分而重复加载同一 PNG；重载后旧缓存可被回收 | 计数/内存采样 |
| NFR-05 | 可测试性 | 所有 P1 需求必须有 headless 单元测试；注意 `pom.xml` 硬编码 `skipTests=true`，CI 必须显式 `-DskipTests=false` | CI 配置核查 |
| NFR-06 | 安全性 | 脚本表达式沿用 `ScriptFilter` 的包级白名单；新增命令纳入 `GladosCommand` 白名单；重载接口不得接受任意文件路径 | 代码审查 + 越权用例 |
| NFR-07 | 可观测性 | 加载/覆盖/重载/缓存刷新/校验失败均输出结构化日志，含文件名与名字 | 日志断言 |
| NFR-08 | 依赖约束 | 不引入新的第三方运行时依赖；Java 11 兼容 | `pom.xml` diff 检查 |

---

## 7. 约束与边界（明确不做）

1. **不回写用户配置**：任何设计结果、示例、运行时状态都不得写回 `conf/*.xml` 与 `conf/settings.properties`。这是托管约束方案已确立的原则（只在内存或只读外部表中生效）。
2. **不改自主行为选择算法**：保留 `Frequency` 加权随机与 `NextBehaviorList`，不引入行为树。
3. **不破坏 GLaDOS 覆盖语义**：`--glados-home/glados-behaviors.xml` 是**完全替换**内置 AI 行为表而非合并；FR-01/FR-02/FR-10 的动作包语义变更不得波及此契约。
4. **不删除既有资源约定**：46 张 `shimeN.png` 与 `ImageSetChooser` 的 `shime1.png` 预览约定继续保留。
5. **不改 `Embedded` 反射路径**：`Class.forName` 兼容现有 12 个嵌入式动作。
6. **本期不交付可视化编辑器**：只保证预览通道（FR-03/FR-11）与数据契约就绪。
7. **不更换 XML 格式**：JSON/YAML DSL 属独立议题。
8. **不修改工作区中不属于本需求的改动**：实现时不得顺带重构 `GladosBridge` 既有逻辑。

---

## 8. 验收标准与测试计划

### 8.1 测试分层

| 层 | 范围 | 环境 |
|---|---|---|
| L1 单元 | `Configuration` / `ActionBuilder` / `AnimationBuilder` / `BehaviorBuilder` / `ImagePairs` | headless，JUnit 4 |
| L2 契约 | 读真实 classpath 配置文件，断言 XML 内容与 schema 一致（防止误删误改） | headless |
| L3 集成 | HTTP 控制桥命令 → 配置重载 → `setBehavior` 构建 | headless（不依赖窗口渲染） |
| L4 手工 | 桌面 GUI 下画面表现、组合动作播放、图片替换生效 | 带桌面的真机 |

> 现有教训：`GladosManagedConstraintsTest` 早期 8 个用例全部在测试类内镜像实现了策略逻辑，
> 真实配置文件被误删测试仍全绿。**所有 P1 需求必须包含 L2 契约用例**，不得只测镜像逻辑。

### 8.2 用例矩阵

| 编号 | 对应 AC | 层 | 用例 |
|---|---|---|---|
| TC-01 | AC-01-1~5 | L1/L2 | 3 个动作包文件按字典序加载，跨文件 `ActionReference` 可解析，日志含全部路径 |
| TC-02 | AC-01-3 | L1 | 动作包目录不存在时回退单一 `actions.xml` |
| TC-03 | AC-02-1~5 | L1 | 同名 Action 后写覆盖；日志含名字与来源；Behavior 覆盖语义不变 |
| TC-04 | AC-02-4 | L2 | `glados-behaviors.xml` 中 `SplitIntoTwo` / `PullUpShimeji` 仍为 `Frequency=0 Action="Sit"` |
| TC-05 | AC-03-2/5 | L3 | 重载后新行为可构建；构造一份坏 XML，断言旧配置仍可用且返回错误 JSON |
| TC-06 | AC-03-4 | L2/L3 | 重载前后 `conf/settings.properties` 字节一致；无语言设置变更 |
| TC-07 | AC-04-1/2 | L4 | 新增 PNG / 替换同名 PNG 后重载，画面更新 |
| TC-08 | AC-05-1/4 | L1 | 帧参数表达式求值与 132 个既有 Pose 回归 |
| TC-09 | AC-06-1/4 | L2 | `sub/foo.png` 解析；46 张既有图片解析结果不变 |
| TC-10 | **AC-08-4** | L2 | 断言 4 份 `conf/*Behavior.xml` 的后继节点名与 `schema.getString("NextBehaviourList")` 一致（**当前预期失败，用于暴露 D-1**） |
| TC-11 | AC-08-3 | L2 | 临时构造缺图 Pose，错误信息含完整路径与 Action / Pose 定位 |
| TC-12 | AC-09-1 | L2 | XSD 的 Type 枚举集合等于运行时 6 种类型 |
| TC-13 | AC-10-1~3 | L2 | 皮肤增量配置继承与优先级；`--glados-home` 替换语义不变 |
| TC-14 | AC-11-2 | L1 | `Frequency=0` 不进 `buildNextBehavior` 候选，但 `buildBehavior(name)` 成功 |
| TC-15 | NFR-01 | L4 | 托管 / 非托管双跑对照：启动日志、托盘、更新检查、自主行为 |
| TC-16 | NFR-02 | L1/L2 | 现有 `GladosManagedConstraintsTest` 全量回归 |

### 8.3 测试基线与命令

```bash
mvn test -DskipTests=false       # pom 硬编码 skipTests=true，必须显式覆盖
```

当前基线（M1 开发后实测，2026-11-15）：全仓 **61 项测试通过 / BUILD SUCCESS**，
其中本需求新增 `ConfigurationFilesTest` 11 项 + `GladosReloadCommandTest` 1 项。明细：

```
ConfigurationFilesTest        11   (FR-01/02/03/04/08 契约与单元)
GladosReloadCommandTest        1   (FR-03 命令入口)
GladosBreedingOverrideTest     7
GladosBridgeTest             20
GladosInteractionTest          9
GladosManagedConstraintsTest  11
GladosStatusBodyTest           2   (并行改动，非本需求)
```

> 文档 v1.0 时的基线为 32 项；其余增量来自其他并行工作，非本需求引入。

### 8.4 真机 GUI 验收清单

1. 非托管直接运行：角色正常出现、托盘存在、自主行为与增殖表现与改造前一致。
2. 托管运行：不出现托盘、不检查更新、角色数量恒为 1（`createMascot` 显式命令仍可增加）。
3. 把角色拖到地面静置，用 `setBehavior` 预览附录 B 的示例动作，确认播放符合预期。
4. 修改示例的 `Duration` 并触发重载，确认变化在**不重启**的情况下生效。
5. 新增/替换 PNG 后重载，确认画面更新（FR-04）。
6. 构造错误配置触发重载，确认角色未消失、旧配置仍可播放、错误响应可读。

---

## 9. 风险与依赖

| 编号 | 风险 | 影响 | 缓解 |
|---|---|---|---|
| R1 | 同名 Action 覆盖可能掩盖笔误 | 设计者误把两个不同动作写成同名，后者静默吞掉前者 | FR-02 强制 warn 日志 + 单文件内重复仍报错（AC-02-3） |
| R2 | 重载实现若先 `disposeAll` 再加载，失败时角色全消失 | 桌面桌宠凭空消失，托管状态错乱 | AC-03-5 要求「校验通过后再切换」或等价原子性；保留 `exitOnLastRemoved` 保护 |
| R3 | `Configuration` 缓存 map 未替换 | 重载后 `getConfiguration()` 仍返回旧实例 | AC-03-2 显式断言；FR-03 备注 |
| R4 | D-1 修复会改变自主行为分布 | 修复后 `NextBehavior` 生效，行为序列与长期观察结果不同 | 单独立项、先在测试环境对照；FR-08 只强制回归测试，不强制本期修复 |
| R5 | 图片缓存全量清空导致闪烁或并发异常 | 多角色画面短暂空白 | AC-04-3/4 限制刷新范围并保证并发安全 |
| R6 | 表达式化帧参数引入除零/负值 | `Animation.getDuration()` 取模异常 | AC-05-4 与 FR-05 风险条：定义取值下限 |
| R7 | 皮肤增量覆盖与 `--glados-home` 替换语义相互作用 | 托管禁增殖约束被绕过 | AC-10-3；`Configuration.isBehaviorEnabled` 的代码层硬过滤作为最终兜底 |
| R8 | 工作区存在并行改动 | 实现时冲突 | 实现前确认分支状态，不改动非本需求文件 |

---

## 10. 里程碑建议

按依赖顺序分四批，**不绑定具体日期**，每批给出退出条件。

| 批次 | 内容 | 退出条件 |
|---|---|---|
| **M0 · 规范与示例** | FR-11、FR-12、FR-08 的 TC-10（暴露 D-1） | 文档与示例可复制运行；D-1 有明确结论与独立缺陷单 |
| **M1 · 三件套（核心）** | FR-01、FR-02、FR-03、FR-04、FR-08 | 改 XML → 一次请求 → 看到效果；全部 L1/L2 用例通过；非托管回归通过 |
| **M2 · 表达力** | FR-05、FR-06、FR-09、FR-10 | 帧参数可表达式化；皮肤增量覆盖可用；XSD 对齐 |
| **M3 · 生态** | FR-07 及可视化编辑器预研 | 新动作类型可声明式接入；或产出「不做」的书面结论 |

M1 是本文的最小可交付闭环：它单独就能把 G1 从「不可能」变成「一次请求」。

---

## 11. 待决问题

| 编号 | 问题 | 影响需求 | 建议默认值 |
|---|---|---|---|
| Q1 | 动作包文件必须是完整 `Mascot` 文档，还是可只含 `<ActionList>` 片段？ | FR-01 AC-01-2 | 允许片段，降低编写成本 |
| Q2 | 单文件内同名 Action 是覆盖还是报错？ | FR-02 AC-02-3 | 报错（防笔误），跨文件才允许覆盖 |
| Q3 | 重载失败时是「保留旧配置」还是「回滚到上一次成功快照」？ | FR-03 AC-03-5 | 保留旧配置，不做快照，实现更简单 |
| Q4 | 图片缓存刷新是全量还是按 `imageSet`？ | FR-04 AC-04-3 | 按 `imageSet`（`removeAll` 已有实现） |
| Q5 | D-1 是改 schema 映射（改成 `NextBehavior`）还是改 4 份 XML（改成 `NextBehaviorList`）？ | FR-08 / R4 | 改 XML 更贴近 schema 定义，但需回归 28 处；由缺陷单决策 |
| Q6 | 皮肤增量覆盖是否需要同时支持 `behaviors.xml` 增量？ | FR-10 | 需要，与 Action 一致 |
| Q7 | 可视化编辑器是否在本仓库内实现？ | FR-07 / M3 | 本期只保证通道，编辑器独立立项 |

---

## 12. 附录

### 附录 A · 关键源码索引

| 文件 | 行号 | 说明 |
|---|---|---|
| `Main.java` | 272–315 | `loadConfiguration()`：三级路径回退、两次 `load()`、`validate()`、缓存 map |
| `Main.java` | 906–929 | `refreshLanguage()`：目前唯一完整重载路径 |
| `Main.java` | 931–934 | `getConfiguration(imageSet)` |
| `ImageSetChooser.java` | 194, 214 | 皮肤预览图固定读取 `shime1.png` |
| `Configuration.java` | 42–96 | `load()`：Action 同名抛异常（75–77） |
| `Configuration.java` | 123–140 | `loadBehaviors()`：Behavior 同名 `put` 覆盖（137） |
| `Configuration.java` | 115–121 | `loadGladosBehaviors()`：后写覆盖，禁增殖依赖此语义 |
| `Configuration.java` | 142–150 | `buildAction(name, params)` |
| `Configuration.java` | 180–188 | `validate()` |
| `Configuration.java` | 190–264 | `buildNextBehavior()`：加权随机 + 后继列表；`totalFrequency==0` 回落 `Fall` |
| `Configuration.java` | 266–299 | `buildBehavior(name)`：按名构建，受控预览通道入口 |
| `Configuration.java` | 301–322 | `isBehaviorEnabled()`：托管模式增殖硬过滤 |
| `BehaviorBuilder.java` | 52 | **同名绑定默认值** |
| `BehaviorBuilder.java` | 82 | 后继行为节点查找（D-1 关键点） |
| `BehaviorBuilder.java` | 123–141 | `validate()` / `buildBehavior()` |
| `BehaviorBuilder.java` | 144–161 | `isEffective()`：`frequency==0` 直接 false |
| `ActionBuilder.java` | 98–136 | 6 种类型 if-else + `Embedded` 反射 |
| `ActionBuilder.java` | 152–158 | `createActions()` 传空 Map（参数不透传） |
| `ActionBuilder.java` | 168–177 | `createVariables()`：外部参数后写覆盖 |
| `ActionRef.java` | 31, 55–66 | 按名引用与参数覆盖 |
| `AnimationBuilder.java` | 92–182 | 图片路径拼接、dev/prod 分支 |
| `AnimationBuilder.java` | 199–222 | 图片预加载、`Integer.parseInt` 帧参数 |
| `ComplexAction.java` | 25–32 | 组合动作传空 `Animation` 列表 |
| `Pose.java` | 70–76 | `mascot.setImage(ImagePairs.getImage(...))` |
| `ImagePairLoader.java` | 26–41 | 按路径缓存；无 `ImageRight` 自动翻转 |
| `ImagePairs.java` | 15–37 | `clear()` / `removeAll()`（全仓零调用） |
| `GladosCommand.java` | 14–21 | 8 个命令白名单，无 reload |
| `GladosBridge.java` | 275–300 | `setBehavior()`：idle 特例与按名构建 |
| `conf/schema.properties` | 61 | `NextBehaviourList=NextBehaviorList` |
| `conf/Mascot.xsd` | 56–61 | 过时的 Type 枚举 |

**M1 新增/改动的入口（详见 §13）**：

| 文件 | 关键位置 | 说明 |
|---|---|---|
| `config/ConfigurationFiles.java` | 全文 | `build()` / `listFragmentFiles()` / `detectNodeNameMismatch()` / `collectMissingImages()` |
| `config/Configuration.java` | `load(Entry, String, String)` | FR-02 跨来源同名覆盖；`getActionSource()` |
| `config/AnimationBuilder.java` | `resolveImagePath()` | 可复用的图片路径解析（FR-06 前置） |
| `image/ImagePairs.java` | `snapshot()` / `restore()` / `clearForImageSet()` | FR-04 缓存刷新与失败回滚 |
| `Main.java` | `resolveConfigPaths()` / `reloadConfigurations()` | 路径回退复用；两阶段重载（FR-03） |
| `glados/GladosCommand.java` | `RELOAD("reload")` | 白名单新增命令 |
| `glados/GladosBridge.java` | `case RELOAD` / `reload()` | 命令分发与错误 JSON |

### 附录 B · 最小示例（零新图 / 零 Java）

```xml
<!-- actions.xml 中追加；不要另起文件重复定义同名 Action（当前会抛 DuplicateAction） -->
<Action Name="MyDance" Type="Sequence" Loop="true">
    <ActionReference Name="Stand" Duration="300" />
    <ActionReference Name="Sit"   Duration="300" />
</Action>

<!-- behaviors.xml 中追加：同名绑定；Frequency=0 不进自主树，Hidden 不进托盘菜单 -->
<Behavior Name="MyDance" Frequency="0" Hidden="true" />
```

预览：

```bash
curl -X POST http://127.0.0.1:<port>/control \
     -H 'Content-Type: application/json' \
     -d '{"cmd":"setBehavior","behavior":"MyDance"}'
```

注意点：
1. `Stand` / `Sit` 是 `BorderType="Floor"` 的 `Stay`，角色需在地面，否则抛 `LostGroundException` 回落 `Fall`。
2. `Duration` 必须写在 `ActionReference` 上，写在 `Behavior` 上不会下传（§2.2）。
3. `Sequence` 没有 `<Animation>`，因此**不需要任何新图片**。
4. 修改 `Duration` 后需重载（FR-03）才能生效。

### 附录 C · 现状统计复核命令

```bash
grep -c '<Action Name='            src/main/resources/conf/actions.xml     # 91
grep -c '<Behavior Name='          src/main/resources/conf/behaviors.xml   # 57
grep -c '<Pose '                   src/main/resources/conf/actions.xml     # 132
grep -c '<ActionReference '        src/main/resources/conf/actions.xml     # 198
grep -o 'Image="[^"]*"'            src/main/resources/conf/actions.xml | sort -u | wc -l   # 46
grep -rn 'ImagePairs\.\(clear\|removeAll\)' src/main/java/                  # 无输出
grep -rn '<NextBehavior '          src/main/resources/conf/*.xml           # 28 处（D-1）
```

### 附录 D · 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-11-15 | 初稿：基于动作系统调研与绑定链路核实，产出 12 条功能需求、8 条非功能需求、16 项测试用例；记录 D-1/D-2/D-3 三项已核实缺陷 |
| v1.1 | 2026-11-15 | 开始开发，新增 §13 实时进度章节 |
| v1.2 | 2026-11-15 | M0 + M1 完成：批次 1–5 进度回写；测试基线 32 → 61；附录 A 补充 M1 新增源码入口；明确 Q5 待决与真机验收未完成 |

---

## 13. 开发进度（实时回写）

> 本章随开发批次更新，记录**实际落地了什么、验证到什么程度、还剩什么**。
> 需求定义与验收标准（§5–§8）不因实现而改写；若实现与 AC 有偏差，在批次记录里显式标注。

### 13.1 总览

| 批次 | 范围 | 状态 | 说明 |
|---|---|---|---|
| M0 · 规范与示例 | FR-11 / FR-12 / D-1 诊断 | ✅ 完成 | 诊断、契约测试、动作设计指南、2 组可运行示例全部交付 |
| M1 · 核心三件套 | FR-01 / FR-02 / FR-03 / FR-04 / FR-08 | ✅ **代码与测试完成** | 批次 1–5 全部落地，全量 61 项测试通过；**仅剩真机 GUI 验收（§8.4）** |
| M2 · 表达力 | FR-05 / FR-06 / FR-09 / FR-10 | 未开始 | P2，方案需单独评审（§10）；`resolveImagePath` 已为 FR-06 铺路 |
| M3 · 生态 | FR-07 与可视化编辑器 | 未开始 | P3，预览通道（reload + setBehavior）已就绪 |

### 13.2 批次记录

#### 批次 1 · FR-01 动作包 / FR-02 同名覆盖（2026-11-15，编译通过）

| 文件 | 改动 |
|---|---|
| `config/Configuration.java` | 新增 `load(Entry, imageSet, source)` 重载；同名 Action **跨文件后写覆盖 + warn**、**同文件内重复仍抛异常**（AC-02-1~3、AC-02-5）；新增 `getActionSource()` 诊断接口；重复错误信息在 headless 下有可读兜底 |
| `config/AnimationBuilder.java` | 抽出 `public static resolveImagePath(imageSet, imageAttr)`，供缺图预检复用同一套路径规则；相对子目录由 `Paths.resolve` 自然支持（FR-06 前置，行为不变） |
| `config/ConfigurationFiles.java` | **新增**。`actions.d/*.xml` / `behaviors.d/*.xml` 按字典序加载（AC-01-1/3/4）、记录全部来源（AC-01-5）、全部加载完才 `validate()`、跨片段收集 `BornMascot`；内置 D-1 节点名一致性诊断与缺图汇总预检（FR-08 AC-08-2/3） |
| `Main.java` | 三级路径回退抽为 `resolveConfigPaths()`；`loadConfiguration()` 改走 `ConfigurationFiles.build()`；启动行为与 `configurations` 的 key 规则保持原样 |

- 验证：`mvn -q compile` → **COMPILE_OK**。
- 片段格式约定（Q1 决策）：片段根节点仍为 `<Mascot>`，内部可只含一个 `<ActionList>` / `<BehaviorList>`；已写入类 Javadoc 与后续指南。
- 待办：对应单元测试（批次 5）。

#### 批次 2 · FR-03 显式重载 / FR-04 图片缓存刷新（2026-11-15，编译通过）

| 文件 | 改动 |
|---|---|
| `image/ImagePairs.java` | 新增 `snapshot()` / `restore()`（失败时精确回滚缓存）与 `clearForImageSet()`（按目录名片段匹配，避开既有 `removeAll()` 依赖固定路径层级 `getName(2)` 在 dev/prod 下不可靠的问题） |
| `Main.java` | 新增 `reloadConfigurations()`：两阶段提交，全部构建+校验成功后才替换 `configurations` 并重建角色；失败返回错误描述且旧配置、旧缓存、现有角色全部不动（AC-03-5）；保留 `exitOnLastRemoved` 保护（AC-03-3）；不读语言、不写 `settings.properties`、不弹框（AC-03-4）；托管模式每次重载重新合并 `GladosBehaviors.loadInto()`，防止 `Frequency=0` 禁增殖覆盖丢失（NFR-02） |
| `glados/GladosCommand.java` | 白名单新增 `RELOAD("reload")`（AC-03-6），未知命令仍被拒绝 |
| `glados/GladosBridge.java` | `execute()` 新增 `RELOAD` 分支与 `reload()` 实现，成功 `{"ok":true}`、失败返回含原因的 error JSON；**以精确插入方式保留并行改动中的 `/status` mascots 数组** |

- **实现中发现并规避的关键陷阱**：图片在 `AnimationBuilder` 构造（即配置构建）时预加载，且 `ImagePairLoader.load` 对已缓存路径直接短路。若「先构建、后清缓存」，新配置会引用一个被清空的缓存，画面全黑。最终顺序为 **快照 → 清缓存 → 构建（重新读盘）→ 成功应用 / 失败 `restore(snapshot)`**。
- 验证：`mvn -q compile` → **COMPILE_OK**。

#### 批次 3 · FR-08 诊断落地 + 契约与单元测试（2026-11-15，全量测试通过）

| 文件 | 改动 |
|---|---|
| `config/ConfigurationFiles.java` | `configuration.load(...)` 抛出的 `ConfigurationException` / `IOException` 统一加上来源文件前缀（AC-08-1） |
| `test/config/ConfigurationFilesTest.java` | **新增 11 项 headless 测试**，见下表 |

| 用例 | 覆盖 AC / TC | 断言要点 |
|---|---|---|
| `actionPackageLoadsInLexicographicOrderAndResolvesCrossFileReferences` | AC-01-1/4/5、TC-01 | 片段按 `10-first` → `20-second` 加载，跨文件 `ActionReference` 在统一 `validate()` 中通过 |
| `missingPackageDirectoryFallsBackToSingleFile` | AC-01-3、TC-02 | 无 `actions.d/` 时只读 2 个主文件 |
| `bornMascotIsCollectedFromPackageFragments` | FR-01 附加 | 片段里的 `BornMascot` 同样被收集 |
| `crossFileSameNameActionIsOverriddenByLaterFile` | AC-02-1/2、TC-03 | **断言覆盖后的 `getType()` 真从 `Stay` 变 `Move`**，且 `getActionSource()` 指向覆盖文件（非镜像逻辑） |
| `duplicateWithinSameFileStillFailsAndNamesTheSource` | AC-02-3、AC-08-1 | 单文件内重复抛异常，错误信息含文件名与动作名 |
| `d1MismatchIsDetectableInRealBehaviorsXml` | AC-08-4、TC-10 | 读**真实** `conf/behaviors.xml`，断言能检出 `<NextBehavior>` 与 schema 的 `NextBehaviorList` 不一致 |
| `missingImagesAreReportedAllAtOnce` | AC-08-3、TC-11 | 一次汇总 2 个缺失图片，信息含来源与 `Action[A]` |
| `resolveImagePathSupportsRelativeSubdirectory` | AC-06-1、TC-09 | `Image="/sub/foo.png"` 正确拼出 `Shimeji/sub/foo.png` |
| `reloadCommandIsWhitelisted` | AC-03-6 | `reload` 在白名单，`reloadAll` / `null` 仍被拒绝 |
| `imageCacheClearIsScopedAndCanBeRestoredFromSnapshot` | AC-04-3/4、D-3 | 只清 `Shimeji` 不清 `Miku`；`restore(snapshot)` 精确回滚 |
| `repositoryConfigFilesRemainUntouched` | AC-02-4 / AC-03-4 / AC-12-4、TC-04/06 | `conf/actions.d` 不存在、`glados-behaviors.xml` 禁增殖仍在、`settings.properties` 无 `Breeding=` |

- 验证：`mvn test -DskipTests=false -q` → **60 项全通过 / exit=0**（基线见 §8.3）。
- 测试类刻意放在 `com.group_finity.mascot.config` 同包，以访问包私有的 `getActionBuilders()`，
  避免重蹈「测试镜像逻辑、真实配置被误改仍全绿」的覆辙（§8.1 的教训）。
- **TC-10 采用绿灯写法**：断言「诊断能检出 D-1」而非断言「配置正确」。D-1 的修复仍留给 Q5 决策，
  本期不改变运行时行为语义；这样既固化了事实，也不会留下一个常红的测试。

#### 批次 4 · FR-12 示例资产与动作设计指南（2026-11-15）

| 文件 | 内容 |
|---|---|
| `docs/action-design-guide.md` | 动作设计指南（AC-12-1）：三层名字绑定、6 种动作类型、`Sequence`/`Select` 组合、`${}` 与 `#{}` 的求值时机差异、参数不透传限制、图片规则与命名、`ImageRight` 自动翻转、`ImageAnchor` 语义、动作包与覆盖、预览/重载工作流 |
| `docs/action-design-guide.md` §10 | 排障清单（AC-12-3）：9 类常见现象、原因与对应日志关键字 |
| `examples/actions.d/10-my-dance.xml` + `examples/behaviors.d/10-my-dance.xml` | 示例 A：**零新图**组合动作，可复制试跑（AC-12-2） |
| `examples/with-new-image/30-wave.xml` + `30-wave-behaviors.xml` | 示例 B：带新 PNG 的帧动作，含图片规格与锚点说明 |
| `examples/README.md` | 完整试用流程（复制位置 → `reload` → `setBehavior` → 还原） |

- 所有示例位于 `examples/` 独立目录，**不写回 `conf/`**（AC-12-4）；
  `ConfigurationFilesTest.repositoryConfigFilesRemainUntouched` 断言 `conf/actions.d` 不存在。
- 指南明确标注本期未实现项（FR-05 帧参数表达式化等 M2 需求），避免设计者误用。

#### 批次 5 · reload 入口测试与并行改动核对（2026-11-15）

- `GladosBridge.reload()` 改为包私有（附测试辅助注释），新增
  `GladosReloadCommandTest`：headless 下直接调用命令入口，断言无已加载角色时返回
  可读的 `{"ok":false,"error":"no image set loaded"}`，不抛异常、不触碰窗口逻辑（AC-03-1 / AC-03-5）。
- 并行改动核对：`/status` mascots 数组与 `GladosStatusBodyTest` 已由另一会话提交为
  `0889305`。当前 `git diff` 中 `GladosBridge.java` **只剩本需求的 3 处改动**
  （Javadoc 命令列表、`RELOAD` 分支、`reload()` 方法），并行内容完整保留。
- 验证：`mvn test -DskipTests=false -q` → **61 项全通过 / exit=0**；`git diff --check` 通过。

### 13.3 本期实施边界

1. 本期交付 **M0 + M1**；M2 / M3 为后续里程碑，不在本轮改动范围。
2. **D-1 只做诊断、不修数据**：Q5（改 schema 还是改 4 份 XML）未决，且任一修法都会激活 28 处后继行为列表、改变自主行为分布，与 NFR-01「非托管行为零变化」冲突。诊断日志与契约测试先固化事实，修复单独立项。
3. 并行改动（`/status` mascots 数组、`GladosStatusBodyTest.java`）已由另一会话提交为 `0889305`；本轮以精确插入方式修改 `GladosBridge.java`，未覆盖其内容，也未替它提交任何文件。
4. **本轮未执行 `git commit`**：所有改动留在工作区，等待确认后再提交。
5. **未完成项**：§8.4 真机 GUI 验收 6 条（尤其「改 XML → reload → 画面变化」「替换 PNG → 画面更新」需要带桌面的环境）；M2 / M3 需求尚未开发。
6. **Q5 仍待决策**：D-1 的修复方向。M1 只提供诊断与回归测试，不改变自主行为分布（NFR-01）。
