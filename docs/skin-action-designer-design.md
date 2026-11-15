# Shimeji Studio · 皮肤及动作设计器设计方案

- 文档版本：v1.1
- 编写日期：2026-11-15
- 状态：**待评审**（本文只做方案设计，不含实现承诺）
- v1.1 变更：新增 §4.4「扩展包模型」，明确**基础动作全量只读保留**、新增动作以可插拔扩展包叠加；Q-D3 定稿，补 DR-15 / R-D11 / Q-D8 与对应验收项
- 代码基线：`feature/glados-integration` @ `d75fbc9`
- 前置成果：`1f0c2d0`（M0 + M1）、`9e2d53c`（Q5 决策 + D-1 修复 + M2）、`d75fbc9`（`/status` 坐标上报）
- 关联文档：
  - `docs/action-authoring-extensibility-requirements.md`（需求与验收基线，下称「需求文档」）
  - `docs/action-design-guide.md`（面向人的动作设计指南，下称「设计指南」）
  - `docs/glados-managed-constraints-design.md`（托管约束，下称「托管约束文档」）
  - `docs/glados-ai-integration-design.md`（HTTP 桥契约）

> **一句话结论**：本轮改造已经把引擎侧的「设计回路」打通（可拆分配置 → 显式重载 → 显式预览 → 状态回读）。设计器应作为**独立的本地 Web 应用**，只读写标准 XML 片段与 PNG，不引入新 DSL、不改引擎运行时、不回写 `conf/` 主文件；引擎侧 M0 阶段**零改动**即可闭环，后续只需补两个只读 getter 与一个 manifest 端点。

---

## 目录

1. 背景与定位
2. 现状能力盘点（核实基线）
3. 总体设计：形态选型与架构
4. 数据模型与 XML 生成规则
5. 功能设计（五个工作台）
6. 接口契约
7. 需求条目与验收标准
8. 非功能、安全与兼容约束
9. 里程碑与交付物
10. 风险与缓解
11. 待决问题
12. 验收清单
13. 与既有需求文档的映射
14. 附录

---

## 1. 背景与定位

### 1.1 为什么现在可以做（本轮调整提供的五个支点）

设计器不是从零发明一套工作流，而是把本轮已落地的能力包成图形界面。逐条对应：

| 支点 | 本轮落地内容 | 对设计器的意义 |
|---|---|---|
| **可拆分** | `conf/actions.d/*.xml` / `behaviors.d/*.xml` 按字典序加载、统一 `validate()`（FR-01） | 设计器可以按「一个动作 = 一个片段文件」组织，保存时只动自己的文件，天然支持多人协作与版本管理 |
| **可覆盖** | 跨文件同名 Action 后写覆盖 + warn，单文件内重复仍报错（FR-02） | 「基础包 + 覆盖包」成为可视化的图层概念，UI 能直接展示「哪个定义生效、来自哪个文件」 |
| **可即时预览** | `reload` 命令：两阶段提交、失败保留旧配置、清图片缓存、不写 `settings.properties`、不弹框（FR-03/FR-04） | 设计器的「运行」按钮 = 一次 HTTP 请求，不需要重启进程、不需要用户点托盘 |
| **名字即绑定** | Behavior 默认找同名 Action，也可 `Action=` 显式改名；`Configuration.validate()` 校验 | UI 里不需要维护引用对象图，只需一个字符串选择器和一个「引用完整性」检查 |
| **表达力与诊断** | 帧参数表达式（FR-05）、`file:` 绝对路径（FR-06）、XSD 对齐（FR-09）、皮肤增量覆盖（FR-10）、错误带文件名与 `Action[名] Pose[序号]`（FR-08） | 参数面板、外部素材引用、静态校验、皮肤只写差异、错误可定位到画布上的具体帧，全部有了引擎侧支撑 |

额外一项：`d75fbc9` 让 `/status` 返回 `mascots[]` 的屏幕坐标与包围盒，设计器可以在预览时叠加「当前行为 + 当前位置」浮层，调试位移动作（`Velocity`）时不必再靠肉眼猜。

需求文档 §1.3 的 **G5「为后续可视化编辑器预留通道」** 与 §11 的 **Q7「可视化编辑器独立立项」** 正是本文的立项依据；需求文档 §7 第 6 条明确「本期不交付可视化编辑器」，因此本文不包含任何已实现承诺，也不修改那份文档的进度结论。

### 1.2 目标

| 编号 | 目标 | 度量方式 |
|---|---|---|
| SD-1 | 不会写 XML 的作者能独立完成一个「新动作 + 新皮肤」 | 从新建项目到桌面看到效果 ≤ 10 分钟，全程不打开文本编辑器 |
| SD-2 | 设计结果就是引擎能直接读的标准资产 | 产出物只有 `img/<角色>/*.png`、`img/<角色>/conf/*.xml`、`info.xml`、`actions.d/*.xml`、`behaviors.d/*.xml`；**无新运行时格式** |
| SD-3 | 预览所见即引擎所得 | 校验与路径解析复用引擎同一份 Java 代码，不允许设计器另写一套解析规则 |
| SD-4 | 对用户直跑零影响 | 不安装设计器时，Shimeji 启动流程、文件、性能完全不变（NFR-01） |
| SD-5 | 错误能在设计期被挡住 | 缺图、非法引用、非法表达式、非法 Duration、节点名不一致，在保存/预览前给出可定位的提示 |

### 1.3 非目标（明确不做）

1. 不引入 JSON / YAML DSL，不把 XML 换成数据库存储 —— **XML 文件是唯一事实来源**（需求文档 §7 第 7 条）。
2. 不改自主行为选择算法（保留 `Frequency` 加权随机 + `NextBehaviorList`）。
3. 不在 M0 阶段实现 FR-07（动作类型 SPI / 脚本型动作）；设计器只暴露现有 6 种类型。
4. 不做绘图软件：不提供逐像素绘制、图层、骨骼绑定、关键帧插值。PNG 由外部工具画好，设计器负责**管理、切图、锚点、镜像、编排**。
5. 不做云端 / 多人实时协同 / 账号体系；纯本地离线。
6. 不修改 `conf/*.xml` 与 `conf/settings.properties` 的既有内容（需求文档 §7 第 1 条），也不改动 `img/` 下的基础素材。仓库自带的 **91 个 Action / 57 个 Behavior / 132 个 Pose / 46 张 PNG 全量只读保留**，新增内容一律以「扩展包」形式叠加，见 §4.4。
7. 不在 Shimeji 主 jar 中引入新的第三方运行时依赖（NFR-08）。

### 1.4 用户与场景

| 编号 | 角色 | 场景 |
|---|---|---|
| US-D1 | 皮肤作者 | 有 46 张（或一套精灵图）新造型，想生成皮肤目录、设置锚点与预览图、写署名信息，然后一键在桌面上看效果 |
| US-D2 | 动作设计者 | 想把已有 `Stand` / `Sit` / `Walk` 拼成新动作，调时长与位移，试几轮节奏 |
| US-D3 | 动作设计者 | 想新画 3 帧「挥手」，用时间轴排帧、加随机时长表达式，复用现有皮肤其余图片 |
| US-D4 | 行为设计者 | 想把一组动作组织成状态链（追逐 → 面向鼠标 → 坐下），可视化 `NextBehaviorList` 并调节权重 |
| US-D5 | 维护者 / 集成方 | 想导入别人给的皮肤包，先看校验报告与冲突列表，再决定是否启用 |

---

## 2. 现状能力盘点（核实基线）

以下均为 2026-11-15 在 `d75fbc9` 上逐条读码核实的事实，不是推测。

### 2.1 已就绪的引擎能力

| 能力 | 源码位置 | 设计器可怎么用 |
|---|---|---|
| HTTP 桥只监听 `127.0.0.1`，端口写入 `--glados-home/glados-bridge.port` | `GladosBridge.java:72, 114–131` | 设计器读端口文件发现预览进程；无需固定端口 |
| `GET /status` → `role / count / paused / behavior / managed / version / mascots[]`（含 `x,y,width,height`） | `GladosBridge.java:148–168` | 预览状态浮层、判断当前播放的行为、显示角色位置 |
| `POST /control` 白名单 9 条：`setRole / setBehavior / pause / resume / createMascot / remainOne / disposeAll / reload / quit` | `GladosCommand.java:19–28`、`GladosBridge.java:221–260` | **预览所需命令已全部具备，M0 无需新增控制命令** |
| `reload` 两阶段提交：快照 → 按 `imageSet` 清缓存 → 重建全部配置 → 成功应用 / 失败 `restore(snapshot)`；重建时重新合并 `GladosBehaviors` | `Main.java:974–1026` | 「运行」按钮；失败时角色与旧配置保持可用，错误字符串可直接展示在 UI |
| 配置构建统一入口 `ConfigurationFiles.build(actions, behaviors, imageSet, baseActions, baseBehaviors)` | `ConfigurationFiles.java:107–138` | 设计器**直接调用同一个方法**做保存前 dry-run 校验，避免「UI 说通过、引擎说失败」 |
| 加载顺序：全局 actions（+`actions.d`）→ 皮肤 actions（+`actions.d`）→ 全局 behaviors → 皮肤 behaviors；统一 `validate()`；`Result.getSources()` 返回实际文件顺序 | `ConfigurationFiles.java:116–137`、`Main.java:298–329` | UI 的「图层 / 生效来源」视图就是这串顺序的直接投影 |
| 诊断：`detectNodeNameMismatch(Document, ResourceBundle)`、`collectMissingImages(Document, imageSet, source, problems)` 均为 public static | `ConfigurationFiles.java:189–244` | 校验中心的现成规则，直接复用 |
| 只读配置接口：`getActionSource(name)`、`getBehaviorNames()`、`getBehaviorBuilder(name)`、`isBehaviorHidden/Toggleable/isBehaviorEnabled`、`getInformation`、`getSchema` | `Configuration.java:362–428` | 生成 manifest 的大部分信息已有 |
| 图片路径解析 `AnimationBuilder.resolveImagePath(imageSet, imageAttr)`：`file:` 前缀取绝对路径、相对子目录可用、dev/prod 双分支 | `AnimationBuilder.java:199–234` | 素材面板与缺图预检复用同一套路径规则（FR-08 的设计意图） |
| 帧参数表达式：`Velocity` / `Duration` / `ImageAnchor` 支持 `${}` / `#{}`，加载期用 Action 参数求值；`Duration < 1` 拒绝；错误含 `Action[名] Pose[n]` | `AnimationBuilder.java:120–195` | 参数面板 + 表达式编辑器 + 「模拟参数」输入框 |
| 图片缓存 `snapshot / restore / clearForImageSet / contains / getImagePair` | `ImagePairs.java:38–108` | 替换 PNG 后由 `reload` 自动刷新，设计器无需自己管缓存 |
| 皮肤元数据 `img/<角色>/conf/info.xml` 的 `<Information>` 支持 `Name / PreviewImage / SplashImage / Artist / Scripter / Commissioner / URL / Support / Characteristics` | `Configuration.java:182–195`、`schema.properties:80–90` | 皮肤属性面板；`PreviewImage` 可**解绑** `shime1.png` 预览约定 |
| `ImageSetChooser` 默认用 `shime1.png` 作预览，有 `PreviewImage` 时优先用它 | `ImageSetChooser.java:190–216` | 设计器生成 `info.xml` 后，皮肤在原生选择器里也能正确显示 |
| `Mascot.xsd` 已对齐运行时 6 种 `Type`、Pose 三个表达式属性、`NextBehaviorList` | `conf/Mascot.xsd:53–64, 108–120, 139–158` | 保存前做 XSD 静态校验（**引擎运行时仍不校验**，这是设计器新增的价值） |
| 现有 46 张 `shimeN.png` 均为 128×128，`ImageAnchor="64,128"` | 实测 `src/main/resources/img/Shimeji/` | 画布默认尺寸与默认锚点 |

### 2.2 预览通道闭环（已经是完整回路）

```
设计器保存片段文件
   → POST /control {"cmd":"reload"}       (引擎重新构建配置 + 清图片缓存)
   → POST /control {"cmd":"setBehavior","behavior":"MyDance"}
   → GET  /status                          (回读 behavior / count / mascots[])
```

三步全部走已有命令，`reload` 失败会返回 `{"ok":false,"error":"..."}` 且旧配置不变。这条回路就是需求文档 G1/G5 的落地形态，设计器只需把它包装成一个按钮 + 状态显示。

### 2.3 仍需设计器自己解决的缺口

| 编号 | 缺口 | 说明 | 本方案对应章节 |
|---|---|---|---|
| GD-1 | **桥只在 `--glados-managed` 时启动** | `Main.java:232` 的 `if (gladosOptions.isManaged())` 内才 `new GladosBridge(...).start()`；非托管直跑没有任何 HTTP 端口 | §3.3 预览进程管理；§11 Q1 |
| GD-2 | 引擎不暴露「配置清单」 | `Configuration` 有 `getBehaviorNames()`，但**没有** `getActionNames()` / `getActionBuilder(name)`；也没有 `getBehaviorSource()` | §6.3 建议新增两个只读 getter + manifest 端点 |
| GD-3 | 帧参数在**加载期**求值，不是运行时 | `AnimationBuilder` 用 `actionParams` 构造 `poseContext`；`mascot` 上下文不可用（需求文档 §13.3 已记录 AC-05-2 偏差） | §5.2 参数模拟面板，明确不提供运行时变量补全 |
| GD-4 | 皮肤预览约定仍偏死 | `shime1.png` 是兜底；`PreviewImage` 可覆盖但仓库内没有 `info.xml` 实例 | §5.1 自动生成 `info.xml` |
| GD-5 | XSD 只在 IDE 生效 | `DocumentBuilderFactory` 未开启 XSD 校验，运行时写错也能进 `validate()` 之外的路径 | §5.4 设计器强制 XSD 校验 |
| GD-6 | 同名覆盖的「谁生效」只有日志 | `getActionSource()` 有来源，但 UI 需要按加载顺序还原覆盖链 | §5.7 图层视图 + §6.3 manifest |
| GD-7 | 没有导入/导出包格式 | 现有示例靠手工复制文件 | §5.6 皮肤包 `.shimeji-skin.zip` |
| GD-8 | 图片必须重启或 `reload` 才刷新 | D-3：`ImagePairs` 只在配置构建时读盘 | §5.5 预览按钮强制串接 `reload` |
| GD-9 | **引擎没有「包」的一等概念** | `ConfigurationFiles.listFragmentFiles` 只扫主文件同级的 `actions.d/*.xml` / `behaviors.d/*.xml`，按文件名字典序、**不递归子目录**、不读任何 manifest；因此「丢一个扩展包目录进去就生效」不成立 | §4.4 扩展包模型（设计器侧摊平 + 安装/卸载，引擎零改动） |

### 2.4 关键差异：托管预览 ≠ 非托管运行（必须写进 UI 提示）

因为桥只在托管模式启动，设计器默认以托管参数拉起预览进程，因此预览环境有这些**已知差异**：

| 差异 | 来源 | 对设计的影响 |
|---|---|---|
| 无托盘、不自检更新 | `Main.java:100–122, 219–222` | 正面：设计态更干净 |
| `Breeding=false`（仅内存） | `Main.java:147–151` | 增殖动作不会真的产生新角色；`createMascot` 命令仍可用 |
| 自主树硬过滤 `SplitIntoTwo` / `PullUpShimeji` | `Configuration.java:331–349` | 预览自主行为分布时这两个行为不会出现；但 `setBehavior` 按名显式构建仍可用 |
| 注入 AI 行为表 | `GladosBehaviors.loadInto()`；`--glados-home/glados-behaviors.xml` 是**完全替换**内置表 | 内置表共 8 条（6 个 `AI*` 行为 + `SplitIntoTwo` / `PullUpShimeji` 两条禁频覆盖），会被合并进配置；设计器应把它们标为「托管注入，非皮肤资产」 |
| `/status.managed` 当前硬编码 `true` | `GladosBridge.java:154` | 若将来支持非托管预览，此字段需要改成读 `options.isManaged()` |

**缓解方案（推荐默认）**：设计器为预览进程准备一个专用 `--glados-home`，其中放一份**最小覆盖表** `glados-behaviors.xml`——只保留两条禁频覆盖与 `AIIdle` 等必要条目，从而把 AI 行为注入降到最低；硬过滤与 `Breeding=false` 是代码层保证，任何覆盖表都绕不过，正好保证设计态不会越预览越多角色。此方案零引擎改动。

---

## 3. 总体设计：形态选型与架构

### 3.1 形态选型

| 方案 | 做法 | 优点 | 否决 / 采纳理由 |
|---|---|---|---|
| A · 内嵌 Swing 面板 | 在 Shimeji 里加「设计器」窗口，用 JTable / JPanel 画时间轴 | 零外部进程、可直接访问内存配置 | ❌ 画布 / 时间轴 / 拖拽体验差；把编辑器代码塞进主 jar，影响非托管启动与体积；与需求文档 §7「独立立项」相悖 |
| **B · 独立本地 Web 应用** | 同仓新增 `studio/` 模块：Java 11 + JDK `HttpServer` 提供本地服务与文件 I/O，浏览器里用原生 HTML/Canvas/ES Modules 做 UI | Canvas 天然适合帧画布与时间轴；与引擎进程解耦；可单独发布；复用已有 `fastjson`、Nashorn、JDK zip/ImageIO，**不引入新第三方依赖** | ✅ **采纳** |
| C · 纯静态 HTML（File System Access API） | 单文件 HTML，浏览器直接读写本地目录 | 零安装 | ❌ 浏览器兼容性与权限模型不稳；无法可靠原子写 / 打包 / 拉起预览进程；只能做只读查看器，可作为 C 的降级形态保留 |

**推荐 B，并采用「同仓、独立进程、共享规则代码」的三条约束**：

1. **同仓**：`studio/` 与引擎共用一个 Maven 工程、一个 jar（通过不同 Main-Class 启动），避免版本错配；不新增第三方依赖（NFR-08）。
2. **独立进程**：`java -cp shimeji.jar com.group_finity.mascot.studio.StudioMain --workspace <dir>`，默认只监听 `127.0.0.1`，浏览器打开 `http://127.0.0.1:<port>/`。
3. **共享规则代码**：设计器把 `ConfigurationFiles.build`、`AnimationBuilder.resolveImagePath`、`collectMissingImages`、`Mascot.xsd` 当作**同一份规则**调用，禁止在前端重写 XML 解析 / 路径拼接 / 表达式求值。这是本方案最重要的正确性保证 —— 上一轮已经吃过「测试镜像逻辑、真实配置被误改仍全绿」的亏，设计器不能重演。

> 设计器自身可以有前端依赖（原生 JS 模块、少量本地 CSS），但**不进入 Shimeji 运行时 classpath**；Shimeji 主 jar 的 `pom.xml` 依赖列表保持零增长。

### 3.2 架构分层

```
┌──────────────────────────────────────────────────────────────┐
│ 浏览器 UI（原生 HTML / CSS / ES Modules / Canvas）              │
│  皮肤工作台 │ 动作时间轴 │ 行为状态图 │ 校验面板 │ 预览浮层      │
└───────────────────────────┬──────────────────────────────────┘
                            │ HTTP / JSON（仅 127.0.0.1）
┌───────────────────────────▼──────────────────────────────────┐
│ Studio Service（JDK HttpServer）                              │
│  ├─ WorkspaceManager   工作区 / 项目 / 备份 / 原子写            │
│  ├─ AssetService       PNG 导入、切图、镜像、缩略图、锚点       │
│  ├─ DocumentService    XML 片段读写（DOM）、排序、命名          │
│  ├─ ValidationService  XSD + 引擎加载器 dry-run + 诊断汇总      │
│  ├─ PreviewOrchestrator  spawn → reload → setBehavior → status │
│  └─ PackageService     zip 导入 / 导出、manifest               │
└──────────┬────────────────────────────────┬──────────────────┘
           │ 直接调用（同一 JVM，同一份规则）   │ HTTP /control、/status
┌──────────▼──────────────────┐   ┌──────────▼──────────────────┐
│ 引擎规则层（只读复用）         │   │ 预览进程（Shimeji 本体）      │
│ ConfigurationFiles.build     │   │ --glados-managed             │
│ AnimationBuilder.resolve...  │   │ --glados-port 0              │
│ Mascot.xsd / schema.properties│  │ --glados-home <studio/home>  │
│ ImagePairs / Configuration   │   │ 读 glados-bridge.port 发现端口│
└─────────────────────────────┘   └──────────────────────────────┘
           │                                    │
           └──────────── 文件系统 ──────────────┘
        <workspace>/img/<角色>/…  <workspace>/conf/actions.d/…
```

分层要点：

- **UI 层无状态权威**：前端持有的 JSON 只是 DOM 的投影；每次保存都从 XML 重新序列化，任何时刻刷新页面都以文件为准。
- **规则层不复制**：`ValidationService` 调用引擎的 `ConfigurationFiles.build(...)` 构建一个**临时 `Configuration` 对象**（不塞进 `Main.configurations`、不碰角色、不弹框），拿到 `validate()` 结果与诊断列表后丢弃。
- **预览进程是黑盒**：设计器只通过 `reload` / `setBehavior` / `/status` 三个既有命令交互，不读它的内存、不注入代码。

### 3.3 进程模型与预览生命周期

```
StudioMain 启动
  ├─ 读 --workspace（必填）、--port（默认 0 随机）、--engine-jar（默认同 jar）
  ├─ 启动本地 HTTP 服务，写 <workspace>/.studio/studio.port
  └─ 用户点「启动预览」时：
       1. 准备 <workspace>/.studio/home/glados-behaviors.xml（最小表）
       2. spawn: java -Ddev=true -cp <engine-jar> com.group_finity.mascot.Main \
                 --glados-managed --glados-port 0 --glados-home <.../home>
       3. 轮询 <.../home>/glados-bridge.port（超时 15s，失败给可读错误）
       4. GET /status 确认 role / count
  用户点「停止预览」→ POST /control {"cmd":"quit"}；超时则 kill 进程
```

- 预览进程**由设计器全权管理**（spawn / 端口发现 / 健康探测 / quit / 兜底 kill），不要求用户手工开两个程序。
- 设计器写入的文件路径必须与预览进程的 `conf/` 解析路径一致：`-Ddev=true` 时是 `src/main/resources/conf/`，打包运行时是发行目录 `conf/`。因此 WorkspaceManager 需要显式配置「引擎根目录」，并在 UI 顶部显示当前生效路径（避免「改了文件、预览没变」的常见问题）。
- 预览进程崩溃 / 端口失联时，设计器进入「离线编辑」模式：仍可编辑与校验，只是「运行」按钮禁用并提示。

### 3.4 四条不可妥协的原则

| 原则 | 内容 | 违反的后果 |
|---|---|---|
| **P1 · 单一事实来源** | XML 文件是唯一权威；不建平行 JSON 数据库、不生成运行时要读的中间格式 | 又一处「镜像逻辑」，改了 UI 数据但引擎读的是文件，出现假成功 |
| **P2 · 只写自有片段** | 设计器创建/修改的文件全部带 `studio-` 前缀或位于自己创建的目录；**绝不编辑** `conf/actions.xml`、`conf/behaviors.xml`、`conf/settings.properties` | 违反需求文档 §7 第 1 条与托管约束文档确立的「不回写用户配置」原则 |
| **P3 · 校验走引擎规则** | 任何「通过/不通过」结论必须来自 `ConfigurationFiles.build` + `Mascot.xsd` + `resolveImagePath` | 设计器自己写一套解析，迟早与引擎分叉 |
| **P4 · 基础层不可变** | 仓库自带的 91 个 Action / 57 个 Behavior / 132 个 Pose / 46 张 PNG 是**兼容性基线**，设计器与扩展包一律只读引用；扩展包只做「加法」，不得同名覆盖、不得改 `settings.properties` | FR-02 的后写覆盖会**静默替换**基础定义，社区皮肤与既有配置在无任何报错的情况下行为突变 |

P2 的一个推论：皮肤增量 `img/<角色>/conf/actions.xml` 属于**用户资产**，设计器可以创建它，但一旦检测到文件已存在且不是设计器生成的（无 `<!-- generated-by: shimeji-studio -->` 标记），必须**只读 + 显式确认**才能覆盖，且覆盖前自动留 `.bak`。

P4 的一个推论：扩展包的「安装」是纯文件加法，「卸载」是纯文件减法；任何时刻删除扩展包片段并 `reload` 一次，配置必须逐字节回到纯净基线。这条可逆性是「最大兼容性」的可验证定义，落为 AC-15-4。

---

## 4. 数据模型与 XML 生成规则

### 4.1 概念关系

```
Project（工作区）
 ├─ Skin*               一个角色目录
 │   ├─ Asset*          PNG（128×128 默认，支持子目录与 file: 外部引用）
 │   ├─ Anchor          每图一个像素锚点，默认 (64,128)
 │   ├─ Info            info.xml 元数据（Name/PreviewImage/Artist/...）
 │   └─ Increments      皮肤级 actions.xml / behaviors.xml（只写差异，FR-10）
 ├─ Fragment*           actions.d/*.xml 或 behaviors.d/*.xml 片段
 │   ├─ Action*         Name / Type / 参数 / Animation* / ActionReference*
 │   │    └─ Pose*      Image / ImageRight / ImageAnchor / Velocity / Duration / Sound
 │   └─ Behavior*       Name / Action / Frequency / Hidden / Condition / NextBehaviorList
 └─ Diagnostics         校验结果（severity / file / line / action / pose / message）
```

### 4.2 字段表（前端模型 → XML 属性）

| 模型 | 关键字段 | 映射到的 XML | 备注 |
|---|---|---|---|
| `Skin` | `id`, `displayName`, `previewAsset`, `artist`, `url`, `characteristics` | `img/<id>/`、`conf/info.xml` 的 `<Information>` | `previewAsset` 写 `PreviewImage`，解绑 `shime1.png`；无 `info.xml` 时保留 `shime1.png` 兜底 |
| `Asset` | `path`, `width`, `height`, `anchor{x,y}`, `mirrored`, `sourceRef` | `<Pose Image="/xxx.png" ImageAnchor="x,y"/>` | `mirrored=true` 时生成 `ImageRight`；相对子目录与 `file:` 外部路径都按 `resolveImagePath` 规则显示 |
| `Action` | `name`, `type`, `borderType`, `loop`, `params{}` | `<Action Name Type BorderType Loop ...>` | `type` 严格取 XSD 的 6 种；`Embedded` 的 `Class` 只读展示（需求文档 §7 第 5 条） |
| `Animation` | `condition`, `isTurn`, `poses[]`, `hotspots[]` | `<Animation Condition IsTurn>` | `Condition` 用 `${}`/`#{}`；Hotspot 在 XSD 有定义但仓库无用例，列为高级功能 |
| `Pose` | `image`, `imageRight`, `imageAnchor`, `velocity`, `duration`, `sound` | `<Pose .../>` | 三个数值字段允许表达式；`Duration < 1` 前端即拒绝（引擎会抛） |
| `ActionReference` | `name`, `paramOverrides{}` | `<ActionReference Name Duration .../>` | 参数**不会向下渗透**（设计指南 §4.2），UI 必须把覆盖参数写在引用上 |
| `Behavior` | `name`, `action`, `frequency`, `hidden`, `toggleable`, `condition`, `next{add,refs[]}` | `<Behavior .../>` + `<NextBehaviorList Add>` | D-1 修复后统一用 `NextBehaviorList`；旧名只作为导入时的兼容别名并提示改名 |
| `Fragment` | `kind(action/behavior)`, `fileName`, `order`, `generatedBy` | `actions.d/<NN>-<slug>.xml` | 文件名字典序 = 加载顺序，NN 用两位数字；片段根节点固定 `<Mascot>` |
| `Diagnostic` | `severity`, `file`, `action`, `poseIndex`, `field`, `message` | — | 直接消费 `ConfigurationException` / `collectMissingImages` 的文本 |

### 4.3 生成规则（硬约束）

1. 片段文件根节点必须是 `<Mascot>`，内部只含一个 `<ActionList>` 或 `<BehaviorList>`（与 `examples/` 现有写法一致）。
2. 文件名格式 `NN-<slug>.xml`（NN 为 10/20/30…），因为 `ConfigurationFiles.listFragmentFiles` 按**文件名字典序**加载（FR-01 AC-01-1）。UI 提供拖拽排序，重排时自动重命名并提示覆盖语义变化。
3. 每个片段首行写 `<!-- generated-by: shimeji-studio v1.0; do not hand-edit -->`；P2 的归属判定依赖此标记。
4. 新行为默认 `Frequency="0" Hidden="true"`（不进自主树、不进托盘菜单，只能 `setBehavior` 预览）—— 这是设计指南 §2 的最小路径，也是避免「一保存就污染自主行为分布」的保护。
5. 保存采用**原子写**：写临时文件 → XSD 校验 → 引擎 dry-run → `Files.move(ATOMIC_MOVE)` → 旧文件留 `.bak`（保留最近 5 份）。任何一步失败都不动原文件。
6. 序列化使用 DOM 而非字符串拼接；未知属性（未来引擎新增字段）**保留原样**，不得因「设计器不认识」而丢弃。
7. 不生成 `conf/actions.xml` / `conf/behaviors.xml` 的副本；皮肤增量只在用户显式选择「仅此角色生效」时才写到 `img/<角色>/conf/`（利用 FR-10 的全局基础 + 皮肤增量）。

### 4.4 扩展包（Pack）：基础层只读 + 增量可插拔

> **结论（2026-11-15 用户决策，Q-D3 定稿）**：可以，而且这正是当前引擎最自然的形态。`Configuration.load()` 是**累加语义**，`ConfigurationFiles.build()` 先加载全局基础、再叠加片段、最后统一 `validate()`；扩展包做的就是「加法」，基础 XML 与 PNG 一个字节都不用改。

#### 4.4.1 三层加载优先级（引擎现状，非设想）

```
① 全局基础层   conf/actions.xml + conf/behaviors.xml
               + conf/actions.d/*.xml + conf/behaviors.d/*.xml
        ↓ 累加；同名按 FR-02 后写覆盖
② 皮肤增量层   img/<角色>/conf/actions.xml + 同目录 actions.d/ behaviors.d/
               ← 「仅此角色生效」的扩展包安装位
        ↓ 累加；同名按 FR-02 后写覆盖
③ 托管行为表   GladosBehaviors.loadInto()（最后合并，优先级最高）
        ↓
   Configuration.validate() 一次性校验全部跨文件引用
```

- 顺序来自 `ConfigurationFiles.build()`：全局 actions → 皮肤 actions → 全局 behaviors → 皮肤 behaviors；托管表在 `Main.loadConfiguration()` 之后合并。
- **兼容性关键点**：扩展包新增的 Behavior 默认 `Frequency="0"`，`BehaviorBuilder.isEffective()` 直接返回 `false`，根本不进自主加权随机树。因此原生 57 个行为的权重分布、状态链、托盘菜单（`Hidden="true"`）**完全不变**；只有 `setBehavior` 显式预览、或作者主动把 `Frequency` 调成 >0 才会影响自主行为。
- 托管层不可被扩展包覆盖：`AI*` / `SplitIntoTwo` / `PullUpShimeji` 列为保留名，`isBehaviorEnabled()` 另有与配置来源无关的硬过滤。

#### 4.4.2 落法 A（推荐：零引擎改动，S0 即可用）

扩展包在**设计器侧**是一个逻辑包；安装时把片段摊平进引擎能发现的目录，`manifest.json` 只给设计器读（引擎永不读，不违反 P1）：

```
packs/<pack-id>/
 ├─ manifest.json                    // id / version / author / engineBaseline / 文件清单 / SHA-256
 ├─ actions.d/50-<pack-id>-*.xml     // 安装 → <conf>/actions.d/
 ├─ behaviors.d/50-<pack-id>-*.xml   // 安装 → <conf>/behaviors.d/
 └─ img/<pack-id>-*.png             // 安装 → img/<目标角色>/ 或其子目录
```

| 操作 | 设计器做的事 | 引擎侧 |
|---|---|---|
| 安装 | L0–L4 校验 → 原子复制片段与 PNG → `POST /control reload` | 只是多读几个 `.xml`，**零改动** |
| 停用 / 启用 | 片段改名为 `*.xml.disabled`（不符合 `.xml` 后缀即不被扫到）或移出目录 → `reload` | 零改动 |
| 卸载 | 按 manifest 清单删除自有文件 → `reload` | 配置回到纯净基线 |
| 预览 | `setBehavior` + `/status` | 零改动 |

文件名用 `<NN>-<pack-id>-<slug>.xml`：`NN` 占住一段字典序（建议扩展包统一 50–89，基础与用户手写片段用 10–40），`pack-id` 前缀保证可按包批量识别、完整卸载，且不与其他包抢覆盖顺序。

#### 4.4.3 三条必须写进实现的引擎约束

1. **发现范围**：`listFragmentFiles()` 只列出主文件**同级** `actions.d/` / `behaviors.d/` 下直接以 `.xml` 结尾的文件，**不递归子目录、不读任何 manifest**。所以 `conf/extensions/<pack>/` 这类目录不会被加载 —— 这正是落法 A 必须「摊平」的原因（GD-9）。
2. **皮肤级扩展包需要启用标记**：`Main.resolveConfigPaths()` 只有检测到 `img/<角色>/conf/actions.xml` 存在，才把该皮肤注册为增量层。因此「只对某个角色生效」的扩展包必须同时放一份 `actions.xml`（内容可为空 `<Mascot><ActionList/></Mascot>`）作启用标记，否则 `img/<角色>/conf/actions.d/` 根本不会被扫描。设计器新建这类包时自动生成该文件。
3. **图片归属**：`AnimationBuilder.resolveImagePath()` 把相对 `Image` 一律解析到 `img/<当前 imageSet>/` 下（支持相对子目录）。扩展包自带的 PNG 必须安装进目标角色目录，不能放在包目录里直接引用；`file:` 绝对路径可用但不可分发，导出时按 DR-12 阻止或转换。

#### 4.4.4 兼容护栏（P4 的强制检查）

| 护栏 | 原因 |
|---|---|
| 扩展包内所有 Action / Behavior 名字不得与基础 91 / 57 个名字相同，不得使用保留名 `AI*` / `SplitIntoTwo` / `PullUpShimeji`；建议统一带包前缀（如 `MyPack.Wave`） | FR-02 是**后写覆盖且只 warn 不报错**，同名会静默替换基础定义 —— 这是唯一能破坏兼容性的口子（R-D11） |
| 不得重定义已有 Behavior 来挂 `NextBehaviorList` | `loadBehaviors()` 是 `put(name, builder)`，重定义 = 整条基础状态链被替换，D-1 刚修好的经典状态链会失效 |
| 新行为默认 `Frequency="0" Hidden="true"` | 不进自主树、不进托盘；作者主动调高时 UI 必须警告「将改变原生行为分布」 |
| 安装前跑 L4「基础名冲突 / 保留名 / 加载顺序」检查，并 dry-run 比对安装前后 Action 与 Behavior 名字差集 | 冲突即拒绝安装且不落盘（DR-15） |
| 导入第三方包先给「将新增 / 将覆盖」清单，非 `generated-by` 文件默认不覆盖 | §5.6 既有规则扩展到包级 |

#### 4.4.5 落法 B（引擎原生包目录，S4 再评审）

让 `ConfigurationFiles` 额外扫描 `extensions.d/<pack>/*.xml`（递归、按包目录名字典序），使「增删一个目录」即可启停，包也不必摊平。优点是卸载更干净、包结构自包含；缺点是要改加载路径规则、需补 FR-01 / FR-10 / NFR-01 全套回归，并打破「M0 引擎零改动」原则。**S0–S3 采用 A；B 列入 Q-D8 延后决策。**

---

## 5. 功能设计（五个工作台）

### 5.1 皮肤工作台

| 能力 | 设计 |
|---|---|
| 新建皮肤 | 输入 `id` → 生成 `img/<id>/`、可选 `conf/info.xml`；不复制 46 张图，允许「先建空壳、再导入」 |
| 素材导入 | 支持多选 PNG、拖入、以及**精灵图切图**（指定行列或格子尺寸 → 自动切成 N 张，按 `shimeN.png` 或自定义名命名） |
| 画布 | 128×128 默认画布（读 PNG 实际尺寸），叠加透明棋盘、中心线、**可拖拽锚点**；锚点值实时写入 `ImageAnchor` |
| 尺寸校验 | 非 128×128 的图给出警告（锚点按比例换算需人工确认），不强制缩放 —— 引擎按原始像素渲染 |
| 左右朝向 | 「自动镜像」开关：开启时省略 `ImageRight`（引擎自动水平翻转）；关闭时允许指定右朝向图 |
| 缺图检查 | 列出当前配置引用的所有 `Image` / `ImageRight`，逐个调 `resolveImagePath` 判定，缺失项在素材面板标红并可一键导入 |
| 预览图与元数据 | 选一张作 `PreviewImage`，写 `info.xml`；填 `Name / Artist / Scripter / URL / Characteristics`；生成后原生 `ImageSetChooser` 也能正确显示 |
| 外部素材 | 允许 `file:` 绝对路径引用（FR-06）；UI 明确提示「此类引用不可打包分发，导出时自动转为皮肤内相对路径或阻止导出」 |

### 5.2 动作工作台

- **左侧资源树**：来自 manifest 的 91 个基础 Action（只读、标来源文件）+ 本项目片段（可编辑、标 `generated-by`）。同名条目折叠显示覆盖链：`全局 actions.xml → actions.d/20-xxx.xml（生效）`。
- **中间时间轴**：
  - `Sequence` / `Select`：一行一个 `ActionReference`，可拖拽排序、点开填参数覆盖（含 `Duration`）。
  - `Animate` / `Stay` / `Move`：一行一个 `Pose`，每格显示缩略图 + `Duration`，时间轴刻度按累计 ms 绘制。
  - 播放头可拖动，前端用 Canvas 按帧序列做**静态预览动画**（仅视觉模拟，不代表引擎物理/边界判定，UI 需标注）。
- **右侧属性面板**：`Name / Type / BorderType / Loop / Condition`，以及 Pose 的 `Image / ImageRight / ImageAnchor / Velocity / Duration / Sound`。
- **表达式编辑器**：
  - 对三个数值字段提供「字面量 / 表达式」切换，表达式用 `${...}` 或 `#{...}`，附语法提示与常用片段（`${500+Math.random()*1000}`、`${-stepX}`）。
  - **参数模拟输入框**：因为帧参数在加载期用 Action 参数求值（GD-3），UI 允许填一组 `key=value` 模拟参数，点「试算」调引擎 `Variable.parse` 得到实际数值；不提供 `mascot` 等运行时变量（明确标注不可用）。
  - `${}` 缓存结果、`#{}` 每帧重算（来自 `Script.clearAtInitFrame`），UI 用图标区分，避免作者误用导致「随机值只算一次」。
- **引用完整性**：改名时自动扫描所有 `ActionReference` 与 `Behavior` 的 `Action=`、所有 `NextBehaviorList` 的引用，列出影响并批量改写（因为一切靠字符串名字）。

### 5.3 行为工作台

- 表格视图：`Name / Action(默认同名) / Frequency / Hidden / Toggleable / Condition / 来源`。
- **状态图视图**：把 `NextBehaviorList` 画成有向图（节点 = Behavior，边 = `BehaviorReference`，边上标 `Frequency` 与 `Condition`），`Add="false"` 用不同颜色表示「限定候选集」而非叠加。D-1 修复后这些边才真正生效，UI 应给出说明链接到需求文档 §11.1。
- 频率直方图：按 `Frequency` 显示进入自主树的权重，一眼看出 `Frequency=0` 的预览专用行为不会进入。
- 一键「注册为预览行为」：生成 `Frequency="0" Hidden="true"` 的配套 Behavior 片段，名字与 Action 同名。

### 5.4 校验中心

保存或预览前按顺序执行，任一失败即阻止并高亮到具体 UI 元素：

| 层 | 检查 | 实现来源 |
|---|---|---|
| L0 · 结构 | XML well-formed；根节点 `Mascot`；片段只含一种 List | `DocumentBuilderFactory` |
| L1 · Schema | 按 `conf/Mascot.xsd` 校验（**引擎运行时不校验，设计器补上**）；`Type` 必须是 6 种之一；Pose 三属性符合 `NumericPairOrExpression` | `javax.xml.validation.SchemaFactory` |
| L2 · 引擎加载 | 调 `ConfigurationFiles.build(...)` 构建临时 `Configuration`（不安装到 `Main`），捕获 `ConfigurationException`；跨片段引用、缺图、非法表达式、`Duration<1` 全在这一层暴露 | 同一份引擎代码 |
| L3 · 诊断 | `detectNodeNameMismatch`（旧 `NextBehavior` 别名建议改名）、`collectMissingImages`（汇总全部缺图）、同名覆盖 warn | `ConfigurationFiles` public static |
| L4 · 分发与兼容检查 | `file:` 外部引用、非皮肤内图片、**基础名 / 保留名冲突**（§4.4.4）、包名冲突、片段文件排序变化 | Studio 自身 + 安装前 dry-run 名字差集比对 |

结果面板每条含：严重级别、文件、行/元素、`Action[名] Pose[n]` 定位、修复建议（如「导入缺失图片」「把 Duration 改成 ≥1」「把 NextBehavior 改名」）。点击可跳转到对应工作台并选中元素。

### 5.5 预览中心

一个「运行」按钮串起完整回路，并把每一步结果展示成时间线：

```
① 原子保存（§4.3）           → 成功 / 失败（失败即停，文件未变）
② L0–L4 校验                 → 有 error 即停，warning 可继续
③ POST /control reload       → 解析 {"ok":false,"error":...} 并原样展示
④ POST /control setBehavior  → 预览目标行为；AIIdle 特殊：回自主树，不作为动作预览
⑤ GET /status                → 显示 role / count / behavior / mascots[] 坐标
⑥ 浮层叠加                   → 桌面截图区域里画出角色包围盒与当前行为名（可选）
```

- 失败语义直接继承引擎：`reload` 失败时旧配置与角色保持可用（FR-03 AC-03-5），UI 应明确写「上一次可用配置仍在运行」，并提供「还原到上次成功」按钮（用 `.bak`）。
- 「停止预览」用 `quit`；「暂停/继续」用 `pause` / `resume`；「切换皮肤」用 `setRole`（会先 `disposeAll` 再 `createMascot`，需提示角色会重建）。
- 图片替换后**必须**走 `reload`（GD-8），UI 不允许只改 PNG 就点 `setBehavior`。

### 5.6 资产与包管理

两种包要分清：**皮肤包**（`.shimeji-skin.zip`，含 PNG + `info.xml` + 皮肤级增量，见下）与**动作扩展包**（§4.4，可跨皮肤共享、只含片段）。前者解决「长什么样」，后者解决「做什么动作」；一个皮肤包可以声明依赖若干扩展包，但引擎对两者一视同仁 —— 都只是 `actions.d/` / `behaviors.d/` 里的片段，基础层始终只读。

- **扩展包安装 / 卸载**（§4.4 落法 A）：`GET /api/packs` 列出已装包与启用状态；`POST /api/packs/install` 先跑 L0–L4 + 基础名冲突检查，再原子摊平片段与 PNG，最后 `reload`；`POST /api/packs/{id}/toggle` 改 `.xml.disabled` 后缀并 `reload`；`DELETE /api/packs/{id}` 按 manifest 清单删除并 `reload`。任何一步失败都不留半安装状态，已复制文件按清单回滚。
- **扩展包导出**：`.shimeji-pack.zip` 只含片段 + 可选 PNG + `manifest.json`；导出时按 DR-12 阻止 `file:` 外部引用，并把 `Frequency>0` 的行为标为「将改变原生自主行为分布」提示。
- **导出（皮肤包）**：`.shimeji-skin.zip`，结构固定：
  ```
  manifest.json          // 仅分发清单（版本、作者、引擎兼容版本、文件列表、校验和）
  img/<skin_id>/*.png
  img/<skin_id>/conf/info.xml
  img/<skin_id>/conf/actions.d/*.xml        // 皮肤专属增量
  img/<skin_id>/conf/behaviors.d/*.xml
  shared/actions.d/*.xml                    // 可选：跨皮肤共享片段
  shared/behaviors.d/*.xml
  ```
  `manifest.json` **只在导入时被设计器读取，引擎永远不读**，不违反 P1。
- **导入**：先解压到临时目录 → 跑 L0–L4 → 展示「将新增 / 将覆盖 / 冲突」清单（尤其同名 Action 覆盖顺序）→ 用户逐项确认 → 原子写入。带 `generated-by` 标记且由本设计器生成的文件可安全覆盖；其余一律默认不覆盖。
- **版本**：`manifest.json` 记 `engineBaseline`（如 `d75fbc9`）与 `schemaVersion`；导入时若 XSD 枚举与当前引擎不一致，给出警告而非静默丢弃未知属性。

### 5.7 项目、图层与撤销

- 工作区顶部常驻显示：**引擎根目录 / 生效 conf 路径 / 当前皮肤 / 预览进程状态 / 端口**，避免路径错位（§3.3）。
- 图层视图按 `Result.getSources()` 的真实顺序列出所有文件，标出每个 Action / Behavior 的「生效定义」与「被覆盖的定义」；这正是 FR-02 与 FR-10 的可视化。
- 撤销栈基于文件快照（`.bak`），不做内存级 undo；每次保存生成一个可命名的还原点，支持「还原到某次成功预览」。

---

## 6. 接口契约

### 6.1 设计器服务 API（新增，仅 127.0.0.1）

统一响应包装：`{"ok":true,"data":...}` / `{"ok":false,"error":"...","diagnostics":[...]}`。

| 方法 | 路径 | 用途 | 关键参数 / 返回 |
|---|---|---|---|
| `GET` | `/api/workspace` | 工作区状态 | 返回 `engineRoot`、`confDir`、`imgDir`、`devMode`、`preview{state,port,role,count,behavior}` |
| `GET` | `/api/skins` | 皮肤列表 | 每项含 `id`、预览图 URL、`info`、素材数、是否有增量配置 |
| `POST` | `/api/skins` | 新建皮肤目录与 `info.xml` | `{id, displayName, previewAsset, artist...}` |
| `GET` | `/api/skins/{id}/assets` | 素材清单 | `path/size/width/height/anchor/mirrored/missing` |
| `POST` | `/api/assets/import` | 导入 PNG / 精灵图切图 | multipart 或 `{base64, grid}`；返回生成的文件列表 |
| `GET` | `/assets/{skin}/{...path}` | 图片静态服务（缩略图 `?w=64`） | 仅允许工作区内路径 |
| `GET` | `/api/manifest` | 合并后的配置清单 | `actions[]{name,type,source,overriddenBy,poses}`、`behaviors[]{name,action,frequency,hidden,source,next[]}`、`fragments[]{file,order,generatedBy}` |
| `GET` | `/api/fragments` | 片段列表与加载顺序 | 含 `actions.d/` 与 `behaviors.d/` 两组 |
| `PUT` | `/api/fragments/{kind}/{name}` | 原子写入片段 | body 为**完整 XML 文本**；先校验后落盘；返回 `diagnostics[]` 与 `.bak` 路径 |
| `DELETE` | `/api/fragments/{kind}/{name}` | 删除自有片段 | 非 `generated-by` 文件需 `force=true` |
| `POST` | `/api/validate` | 仅校验不落盘 | 返回 L0–L4 全量 `diagnostics[]` |
| `POST` | `/api/preview/start` | 启动预览进程 | `{engineJar, devMode}`；返回发现到的桥端口 |
| `POST` | `/api/preview/stop` | 优雅退出预览 | `quit` + 超时兜底 |
| `POST` | `/api/preview/run` | §5.5 六步编排 | `{behavior, saveFirst}`；返回每步结果 + `/status` 快照 |
| `GET` | `/api/preview/status` | 代理 `/status` | 原样透传，附 `mascots[]` 坐标 |
| `POST` | `/api/export` | 导出皮肤包 | `{skinIds, includeShared}` → zip 下载 |
| `POST` | `/api/import` | 导入皮肤包（先预览冲突） | 两阶段：`analyze=true` 返回冲突清单，确认后写入 |
| `GET` | `/api/packs` | 已安装扩展包清单（§4.4） | `id / enabled / files[] / engineBaseline / conflicts[]` |
| `POST` | `/api/packs/install` | 安装扩展包（原子） | 先 L0–L4 + 基础名冲突检查 → 摊平片段与 PNG → `reload`；失败按清单回滚 |
| `POST` | `/api/packs/{id}/toggle` | 启用 / 停用扩展包 | 改 `.xml.disabled` 后缀 + `reload`；不删文件 |
| `DELETE` | `/api/packs/{id}` | 卸载扩展包 | 按 `manifest.json` 清单删除自有文件 + `reload`；非自有文件不碰 |
| `GET` | `/api/schema` | XSD 与 schema 映射 | 供前端做属性补全与枚举约束 |

**安全**：所有路径参数必须经 `WorkspaceManager.safeResolve()` 归一化后校验仍在工作区根内；拒绝 `..`、绝对路径、符号链接越界；`/control` 的 `reload` 不接受任何路径参数（NFR-06），设计器也绝不把用户输入的路径传给引擎。

### 6.2 复用的引擎桥端点（零改动）

```bash
# 端口来自 --glados-home/glados-bridge.port
PORT=$(cat <workspace>/.studio/home/glados-bridge.port)

curl -X POST "http://127.0.0.1:$PORT/control" \
  -H 'Content-Type: application/json' -d '{"cmd":"reload"}'
# → {"ok":true} 或 {"ok":false,"error":"failed to reload configuration: ..."}

curl -X POST "http://127.0.0.1:$PORT/control" \
  -H 'Content-Type: application/json' -d '{"cmd":"setBehavior","behavior":"MyDance"}'

curl "http://127.0.0.1:$PORT/status"
# → {"role":"Shimeji","count":1,"paused":false,"behavior":"MyDance",
#    "managed":true,"version":"...","mascots":[{"x":..,"y":..,"width":..,"height":..}]}
```

### 6.3 建议的引擎侧改动（**均非 M0 必须，需单独评审**）

按「投入 / 收益」排序，全部保持默认行为不变：

| 编号 | 改动 | 位置 | 收益 | 风险 |
|---|---|---|---|---|
| E-1 | 新增 `getActionNames()` 与 `getActionBuilder(name)` 两个**只读** getter | `Configuration.java:383–396`（`getActionBuilders` / `getActionSources` 附近） | 设计器不必自己解析 91 个 Action 的名字与类型 | 极低，纯读 |
| E-2 | 新增 `getBehaviorSource(name)`（与 `getActionSource` 对称） | `Configuration.java` | 行为图层视图能显示来源 | 极低 |
| E-3 | `GET /design/manifest`：由内存配置生成结构化清单 | `GladosBridge.java:74–75` 附近新增 context | 设计器拿到的是**引擎实际加载结果**，彻底消除镜像偏差（P3） | 低；需限定 127.0.0.1、只读、加白名单 |
| E-4 | `/status.managed` 改为 `options.isManaged()` | `GladosBridge.java:154` | 若 Q1 选择支持非托管预览，状态字段才可信 | 低；现有测试需同步 |
| E-5 | 新增 `--designer-port`：非托管模式下也启动桥，供纯非托管预览 | `GladosOptions.parse` + `Main.run():232` | 消除 §2.4 的全部托管差异（增殖、AI 行为表、托盘） | **中**：触碰启动分支，必须补 NFR-01 回归；建议放到 S3 之后单独评审 |

> E-1/E-2 不改变任何行为语义，E-3 是新增只读端点。M0 阶段设计器可以用「自己解析 XML + 调 `ConfigurationFiles.build` 校验」的方式先跑通，E-1~E-3 作为 S2 的收敛项。**E-5 明确不纳入 M0**，理由见 §11 Q1。

### 6.4 预览编排的失败语义

| 失败点 | 引擎行为 | 设计器响应 |
|---|---|---|
| 保存前校验失败 | 文件未改动 | 阻止，定位到 UI 元素，不调用 `reload` |
| `reload` 返回 `ok:false` | 快照回滚、旧配置与角色保持可用 | 展示原始 error，标「上次可用配置仍在运行」，提供还原按钮 |
| `setBehavior` 返回 `unknown behavior` | 角色行为不变 | 提示行为未注册（常见原因：`Frequency=0` 但片段没放进正确目录） |
| 预览进程失联 | — | 切「离线编辑」，禁用运行按钮，提供重启预览 |
| `setRole` 切皮肤 | `disposeAll` + `createMascot` | 提示角色会重建、当前行为会丢失，需重新 `setBehavior` |

---

## 7. 需求条目与验收标准

采用 `DR-xx`（Designer Requirement）编号，与需求文档的 `FR-xx` 区分。优先级：P0 = 最小可用闭环，P1 = 完整设计体验，P2 = 生态。

| 编号 | 需求 | 优先级 | 验收标准（AC） |
|---|---|---|---|
| DR-01 | 工作区与项目管理 | P0 | AC-01-1 指定引擎根目录后，UI 同时显示 dev/prod 生效的 `conf/` 与 `img/` 路径；AC-01-2 不指定时不允许保存；AC-01-3 设计器未运行时，Shimeji 直跑行为零变化 |
| DR-02 | 皮肤目录与素材导入 | P0 | AC-02-1 新建皮肤生成可被 `ImageSetChooser` 识别的目录；AC-02-2 导入 PNG 后能用 `resolveImagePath` 定位；AC-02-3 精灵图按格子切图后逐张可预览；AC-02-4 不强制修改 128×128 尺寸 |
| DR-03 | 锚点与朝向编辑 | P1 | AC-03-1 画布拖拽锚点写入 `ImageAnchor`；AC-03-2 自动镜像时省略 `ImageRight`；AC-03-3 显式右图时生成该属性 |
| DR-04 | `info.xml` 元数据 | P1 | AC-04-1 设置 `PreviewImage` 后原生选择器显示该图；AC-04-2 未设置时保留 `shime1.png` 兜底；AC-04-3 不破坏已有非设计器生成的 `info.xml` |
| DR-05 | 动作时间轴编辑 | P0 | AC-05-1 能新建 `Sequence`/`Select` 并拖拽 `ActionReference` 排序；AC-05-2 能编辑 `Animate` 的 Pose 序列与 `Duration`；AC-05-3 参数覆盖写在 `ActionReference` 上（不依赖向下渗透）；AC-05-4 生成的片段根节点为 `<Mascot>` |
| DR-06 | 表达式与参数模拟 | P1 | AC-06-1 三个数值字段可切换字面量/表达式；AC-06-2 试算结果来自引擎 `Variable.parse`；AC-06-3 `Duration<1` 前端即拒绝；AC-06-4 UI 明确不提供 `mascot` 运行时变量 |
| DR-07 | 行为注册与状态图 | P1 | AC-07-1 一键生成 `Frequency=0 Hidden=true` 的配套 Behavior；AC-07-2 状态图使用 `NextBehaviorList` 规范名并可视化 `Add`；AC-07-3 导入旧 `NextBehavior` 时给出改名提示 |
| DR-08 | 校验中心 | P0 | AC-08-1 保存前跑 L0–L4；AC-08-2 XSD 校验生效（引擎运行时不校验）；AC-08-3 缺图、非法引用、非法表达式均能定位到文件 + `Action[名] Pose[n]`；AC-08-4 校验使用 `ConfigurationFiles.build` 而非前端重写规则 |
| DR-09 | 原子保存与还原 | P0 | AC-09-1 写临时文件 → 校验通过 → `ATOMIC_MOVE`；AC-09-2 失败时原文件不变；AC-09-3 保留最近 5 份 `.bak` 且可还原；AC-09-4 不写 `conf/actions.xml`、`conf/behaviors.xml`、`settings.properties` |
| DR-10 | 预览闭环 | P0 | AC-10-1 一键完成 保存→校验→`reload`→`setBehavior`→`/status`；AC-10-2 预览进程由设计器 spawn / 端口发现 / quit 全权管理；AC-10-3 `reload` 失败时按 §6.4 展示且角色不消失；AC-10-4 替换 PNG 后必经 `reload` |
| DR-11 | 图层与覆盖可视化 | P1 | AC-11-1 按 `Result.getSources()` 顺序列出文件；AC-11-2 标出每个名字的生效定义与被覆盖定义；AC-11-3 重排片段时提示字典序变化影响 |
| DR-12 | 导入 / 导出包 | P2 | AC-12-1 导出的 zip 只含标准 XML 与 PNG + 分发用 manifest；AC-12-2 引擎不读 manifest；AC-12-3 导入先给冲突清单再写入；AC-12-4 `file:` 外部引用在导出时被阻止或转换 |
| DR-13 | 安全与隔离 | P0 | AC-13-1 只监听 127.0.0.1；AC-13-2 所有路径经工作区白名单归一化，越界返回 400；AC-13-3 不向引擎传递任意文件路径；AC-13-4 脚本表达式沿用 `ScriptFilter` 包级白名单，设计器自身不执行用户脚本 |
| DR-14 | 文档与示例 | P1 | AC-14-1 提供《设计器使用手册》与 3 个模板项目（零新图组合动作 / 新图帧动作 / 表达式动作，复用 `examples/`）；AC-14-2 手册与 `docs/action-design-guide.md` 术语一致 |
| DR-15 | 基础层只读与扩展包隔离（§4.4） | P0 | AC-15-1 安装前后 `conf/actions.xml`、`conf/behaviors.xml`、`conf/settings.properties` 与基础 PNG 的字节哈希完全一致；AC-15-2 扩展包名与基础 91 个 Action / 57 个 Behavior 或保留名冲突时，安装/保存被**硬拒绝**并列出冲突名；AC-15-3 新行为默认 `Frequency=0 Hidden=true`，安装后自主行为分布与基线一致（原生行为权重不变）；AC-15-4 卸载扩展包并 `reload` 后，Action / Behavior 集合与基线完全一致；AC-15-5 扩展包自带 PNG 安装进 `img/<角色>/`，`file:` 引用在导出时被阻止 |

---

## 8. 非功能、安全与兼容约束

| 编号 | 类别 | 要求 |
|---|---|---|
| SD-NFR-1 | 兼容性 | 不安装 / 不启动设计器时，Shimeji 的启动、托盘、自主行为、性能、文件**零变化**（承接 NFR-01）；M0 阶段引擎侧零改动 |
| SD-NFR-2 | 托管约束 | 不得回归三条托管约束（不自检更新 / 不展示托盘 / 不自主增殖）；设计器生成的预览配置不得绕过 `isBehaviorEnabled` 硬过滤与 `Breeding=false` |
| SD-NFR-3 | 依赖 | 引擎主 jar 不新增第三方依赖（NFR-08），Java 11 兼容；设计器 UI 使用原生浏览器能力，不依赖 CDN（离线可用） |
| SD-NFR-4 | 安全 | 仅 127.0.0.1；路径白名单；原子写；设计器不执行用户 JS（表达式由引擎在加载期按 `ScriptFilter` 白名单求值） |
| SD-NFR-5 | 性能 | 91 Action / 57 Behavior / 132 Pose 规模下，清单生成 ≤ 200ms、一次完整校验 ≤ 1s；缩略图按需生成并缓存 |
| SD-NFR-6 | 可观测 | 保存、校验、reload、setBehavior、进程启停均记结构化日志，含文件名与动作名；错误原样透传到 UI |
| SD-NFR-7 | 可测试 | `ValidationService` / `DocumentService` / `WorkspaceManager` 必须有 headless 单测；注意 `pom.xml` 硬编码 `skipTests=true`，CI 需 `-DskipTests=false` |
| SD-NFR-8 | 数据完整性 | 未知 XML 属性保留；非设计器生成的文件默认只读；所有覆盖留备份 |

---

## 9. 里程碑与交付物

| 里程碑 | 范围 | 交付物 | 完成判据 |
|---|---|---|---|
| **S0 · 打通闭环（最小可用）** | DR-01/02/05/08/09/10/13/15 | `StudioMain`、工作区管理、片段编辑、**扩展包安装/卸载**、引擎 dry-run 校验、预览进程编排、一个极简 UI | 用设计器做出「站-坐-站」组合动作并作为扩展包安装，点一次运行桌面上看到效果；卸载后回到纯净基线；全程零引擎改动 |
| **S1 · 皮肤工作台** | DR-02/03/04 | 素材导入与切图、锚点画布、镜像、`info.xml`、缩略图服务 | 新建皮肤、导入 3 张图、设锚点与预览图，原生选择器可识别 |
| **S2 · 表达力与图层** | DR-06/07/11 + E-1~E-3 | 表达式编辑器与参数模拟、状态图、图层视图、manifest 端点 | 表达式试算结果与引擎一致；覆盖链可视化准确；引擎侧仅新增只读接口 |
| **S3 · 包与生态** | DR-12/14 | zip 导入导出、冲突清单、模板项目、使用手册 | 两个工作区之间能完整迁移一个皮肤 + 动作包，导入后校验全绿 |
| **S4 · 可选增强** | E-5 / FR-07 / 可视化编辑器联动 | 非托管预览开关、动作类型 SPI 评估 | 单独评审后立项，不在本文承诺范围 |

**目录结构草案**（新增，不动现有 `src/main/java/com/group_finity/mascot/` 之外的运行逻辑）：

```
studio/
 ├─ java/com/group_finity/mascot/studio/
 │   ├─ StudioMain.java            // 独立入口
 │   ├─ http/StudioServer.java     // JDK HttpServer + 路由 + 统一响应
 │   ├─ WorkspaceManager.java      // 路径白名单 / 原子写 / 备份
 │   ├─ AssetService.java          // PNG / 切图 / 镜像 / 缩略图
 │   ├─ DocumentService.java       // DOM 读写 / 片段排序 / 未知名保留
 │   ├─ ValidationService.java     // L0–L4，复用引擎规则
 │   ├─ PreviewOrchestrator.java   // spawn / port / reload / setBehavior / status
 │   └─ PackageService.java        // zip 导入导出
 ├─ web/                           // 原生 HTML/CSS/ES Modules/Canvas，无构建步骤
 │   ├─ index.html
 │   ├─ skin.js  action.js  behavior.js  validate.js  preview.js
 │   └─ style.css
 └─ templates/                     // 3 个模板项目（复用 examples/ 的写法）
docs/skin-action-designer-usage.md // S3 交付
```

---

## 10. 风险与缓解

| 编号 | 风险 | 影响 | 缓解 |
|---|---|---|---|
| R-D1 | **路径错位**：设计器写到 `src/main/resources/conf/`，预览进程却读发行目录 `conf/` | 最常见、最迷惑的「改了没反应」 | UI 常驻显示生效路径；预览启动时用同一份 `devMode` 配置；保存后 `/api/manifest` 回读确认片段确实被加载 |
| R-D2 | **镜像逻辑复发**：前端自己解析 XML 得出与引擎不同的结论 | 假成功、校验漏判 | P3 原则；`ValidationService` 直调 `ConfigurationFiles.build`；S2 补 E-3 manifest 端点，让清单来自引擎内存态 |
| R-D3 | 托管预览差异被误当成 bug | 作者以为增殖动作 / AI 行为异常 | §2.4 差异表写进 UI 帮助与手册；行为条目标注「托管注入」；Q1 决策后再考虑 E-5 |
| R-D4 | 覆盖语义被 UI 隐藏 | 用户不知道后写覆盖，改了「不生效」的那个文件 | 图层视图（DR-11）显式标注生效定义；重排片段时提示字典序变化 |
| R-D5 | 图片缓存导致「换了 PNG、画面没变」 | D-3 复现 | 预览编排强制 `reload`；禁止跳过 reload 直接 `setBehavior` |
| R-D6 | 覆盖用户手写的配置文件 | 数据丢失 | P2 + `generated-by` 标记 + 默认只读 + `.bak` + 导入冲突清单 |
| R-D7 | 表达式在设计器里被执行 | 安全风险（NFR-06） | 设计器只做静态高亮与「把文本交给引擎求值」；试算走引擎 `Variable.parse`，不 `eval` 前端字符串 |
| R-D8 | XSD 与运行时再次分叉 | IDE/设计器误报 | `Mascot.xsd` 已在 M2 对齐；设计器从 `/api/schema` 动态取枚举，不硬编码；每次引擎升级跑一次一致性测试 |
| R-D9 | 预览进程残留 | 端口文件过期、桌宠多开 | 设计器独占 `--glados-home`、退出时 `quit`、超时兜底 kill、启动时清理陈旧端口文件 |
| R-D10 | 多语言 schema 标签 | `schema_ja.properties` 把节点名映射为日文 | 设计器生成统一用英文规范名（与仓库内 `conf/` 一致）；导入其他语言配置时按 `ResourceBundle` 映射识别，标为「实验性」 |
| R-D11 | **扩展包与基础动作同名，触发 FR-02 后写覆盖** | 基础定义被静默替换（只有一行 warn），社区皮肤与既有配置行为突变 —— 直接违背「最大兼容性」 | §4.4.4 护栏：包前缀命名、L4 基础名冲突检查硬拒绝、安装前 dry-run 比对名字差集、图层视图标注覆盖链；AC-15-2/15-4 做回归 |
| R-D12 | 皮肤级扩展包缺 `actions.xml` 启用标记 | `resolveConfigPaths()` 不注册增量层，片段看似装上了却完全不生效 | 设计器生成皮肤包时自动写入空 `ActionList` 标记；安装后 `/api/manifest` 回读确认片段确实在 `Result.getSources()` 里 |

---

## 11. 待决问题

| 编号 | 问题 | 建议默认值 | 影响 |
|---|---|---|---|
| Q-D1 | 预览固定用托管模式，还是新增 `--designer-port` 支持非托管预览（E-5）？ | **S0–S3 固定托管**；E-5 推迟到 S4 单独评审，因为它触碰 `Main.run()` 启动分支，需要 NFR-01 全套回归 | §2.4 差异是否长期存在 |
| Q-D2 | 设计器与引擎共用 jar，还是独立 artifact？ | 共用同一 Maven 工程、同一 jar、不同 Main-Class；独立 artifact 留到需要单独分发时再拆 | 构建复杂度、依赖边界 |
| Q-D3 | 是否需要「可视化编辑器」直接改 `conf/actions.xml`（91 个基础动作）？ | **已定（2026-11-15 用户决策）：不需要。** 基础动作/行为/图片**全量只读保留，保证最大兼容性**；新增动作一律作为**可插拔扩展包**叠加（§4.4）。要改基础动作请走代码评审 | 定稿 P4 边界、DR-15、GD-6 图层设计与 §4.4 全节 |
| Q-D4 | 皮肤包格式是否要兼容社区既有 Shimeji 皮肤（纯 `img/<名>/` 无 conf）？ | 需要：导入时允许「无 conf」皮肤，自动补一份最小 `info.xml`，动作全部继承全局 | S3 导入范围 |
| Q-D5 | 前端是否允许引入构建工具（Vite / TypeScript）？ | S0–S1 用原生 ES Modules 零构建；若 UI 复杂度超过阈值再单独评审 | NFR-3 离线与依赖约束 |
| Q-D6 | 是否实现 Hotspot（点击热区）可视化？ | 暂不实现：XSD 有定义但仓库 0 用例，保留数据结构不丢属性即可 | 高级功能排期 |
| Q-D7 | `manifest.json` 是否要记录校验和防篡改？ | 记录每个文件的 SHA-256，导入时校验并报告差异，但不强制阻止 | 包完整性 |
| Q-D8 | 扩展包是否要引擎原生目录发现（§4.4.5 落法 B）？ | **S0–S3 用落法 A**（设计器摊平片段，引擎零改动）；落法 B 推迟到 S4 单独评审，因为它要改 `ConfigurationFiles.listFragmentFiles()` 并跑 FR-01 / FR-10 / NFR-01 全套回归 | 是否能「增删目录即启停」、是否打破 M0 零改动 |

---

## 12. 验收清单

### 12.1 自动化（headless，CI 必跑）

1. `mvn test -DskipTests=false` 全绿（本文编写时实测基线 **86 项 / exit=0**；
   `9e2d53c` 时为 82 项，`d75fbc9` 的坐标上报新增 4 项。设计器新增用例后需更新此数字）。
2. `ValidationService` 用例：缺图 / 非法引用 / 非法表达式 / `Duration<1` / 旧 `NextBehavior` 别名 / 同名覆盖顺序，每项断言**结论来自引擎加载器**。
3. `WorkspaceManager` 用例：`..`、绝对路径、符号链接越界一律 400；原子写失败时原文件字节不变；`.bak` 上限 5 份。
4. `DocumentService` 用例：未知 XML 属性往返不丢失；片段排序与 `listFragmentFiles` 字典序一致；生成的 XML 通过 `Mascot.xsd`。
5. 契约用例：设计器生成的片段能被 `ConfigurationFiles.build` 加载且 `validate()` 通过；`resolveImagePath` 对 `file:` 与相对子目录的结果与 UI 展示一致。
6. 回归：非托管直跑的启动日志、托盘、自主行为与基线一致（SD-NFR-1）；三条托管约束用例仍绿（SD-NFR-2）。
7. 兼容性契约（AC-15-1/15-4）：以仓库自带 `conf/actions.xml`、`conf/behaviors.xml`、`img/Shimeji/` 计算安装前哈希；安装示例扩展包 → 断言 91 个基础 Action / 57 个基础 Behavior 的定义与哈希不变；卸载 + `reload` → 断言 Action / Behavior 名字集合回到基线（不多不少）。
8. 冲突契约（AC-15-2）：构造一个定义了 `Stand`（基础名）与 `AIIdle`（保留名）的扩展包，断言安装被拒绝、错误信息列出冲突名、且没有任何文件落盘；`Frequency>0` 的行为在导出时给出警告。

### 12.2 真机 GUI（人工）

1. S0 闭环：用设计器新建 `MyDance`（零新图），点运行 → 桌面播放站-坐-站；改 `Duration` → 再点运行 → 节奏变化，全程不重启、不弹框。
2. 换 PNG 后点运行 → 画面更新（验证 `reload` 清缓存）。
3. 故意写错配置 → 点运行 → 得到含文件名与 `Action[名] Pose[n]` 的错误，角色不消失、旧配置仍可播放。
4. 新建皮肤、导入 3 张图、设锚点与 `PreviewImage` → 原生「选择形象」里显示该预览图。
5. 皮肤只放一个增量 `actions.xml`（覆盖 1 个动作）→ 其余 90 个全局动作仍可播放（FR-10 回归）。
6. 表达式动作：`Duration="${500+Math.random()*1000}"` 反复 reload，节奏确实变化；`Duration="0"` 被前端拒绝。
7. 导出包 → 在另一个干净工作区导入 → 冲突清单正确 → 校验全绿 → 预览一致。
8. 托管预览差异提示：UI 明确标注 AI 行为条目来源，且角色数量恒为 1。
9. 安装一个「零新图」扩展包（`MyDance`）→ 桌面预览正常 → 观察原生桌宠自主行为分布与安装前无差异（新行为 `Frequency=0` 不进自主树）。
10. 停用 / 卸载该扩展包 → `reload` → 托盘与自主行为完全回到基线；把包改成只对某一皮肤生效，验证 `img/<角色>/conf/actions.xml` 启用标记存在时片段才被加载（§4.4.3 第 2 条）。

---

## 13. 与既有需求文档的映射

| 既有条目 | 本文如何承接 |
|---|---|
| G5「为可视化编辑器预留通道」 | §2.2 直接把 `reload + setBehavior + /status` 作为设计器的预览回路 |
| Q7「可视化编辑器独立立项」 | 本文即该立项的设计方案；独立进程、独立里程碑 |
| FR-01 动作包 | §4.3「一个动作 = 一个片段」、§5.7 图层视图 |
| FR-02 同名覆盖 | §5.7 覆盖链可视化、DR-11 |
| FR-03/04 重载与缓存 | §5.5 预览编排、R-D5 |
| FR-05/06 表达式与图片路径 | §5.2 表达式编辑器、§5.1 外部素材、DR-06 |
| FR-08 设计时校验 | §5.4 校验中心（在引擎已有诊断之上补 XSD 静态校验） |
| FR-09 XSD 对齐 | §5.4 L1 层；本文是 XSD 对齐后的**第一个消费者** |
| FR-10 皮肤增量 | §5.6 包结构、§5.1 皮肤工作台 |
| FR-11/12 指南与示例 | §9 S3 模板项目直接复用 `examples/` |
| FR-07（M3） | 明确不纳入 M0；S4 可选评审（§11 Q-D1 相邻） |
| NFR-01/02/06/08 | §8 SD-NFR-1/2/4/3 |
| §8.4 真机验收 9 条 | §12.2 复用其中第 3/4/5/6/8/9 条作为设计器验收的对照基线 |
| §11.1 Q5 决策 | §5.3 状态图统一用 `NextBehaviorList`，导入旧名给提示 |
| 托管约束文档 §8 的限制 | §2.4 最小覆盖表策略；设计器不依赖「覆盖文件合并」语义 |
| 用户决策「基础动作保留 + 扩展包」 | §3.4 P4、§4.4 全节、DR-15、Q-D3 定稿、Q-D8 备选路径 |

---

## 14. 附录

### 附录 A · 关键源码索引（以 `d75fbc9` 为准）

| 文件 | 行 / 符号 | 用途 |
|---|---|---|
| `Main.java` | 232–242（桥启动条件）、298–329（`resolveConfigPaths`）、331–360（`loadConfiguration`）、974–1026（`reloadConfigurations`） | 预览进程与配置路径规则 |
| `GladosBridge.java` | 72、74–75、114–131、148–168、221–260、329–332 | 端口发现、`/status`、`/control`、`reload` |
| `GladosCommand.java` | 19–28 | 命令白名单 9 条 |
| `GladosOptions.java` | 46–80（未知参数忽略） | 新增 `--designer-*` 参数可零侵入 |
| `ConfigurationFiles.java` | 107–138（`build`）、144–159（片段字典序）、189–244（两项诊断） | 校验中心复用的全部规则 |
| `Configuration.java` | 331–349（托管硬过滤）、362–428（只读接口） | manifest 数据来源；E-1/E-2 落点 |
| `AnimationBuilder.java` | 120–195（表达式 / 错误定位）、199–234（`resolveImagePath`） | 参数模拟与素材定位 |
| `ImagePairs.java` | 38–108 | 缓存刷新由 `reload` 负责 |
| `ImageSetChooser.java` | 190–216 | `info.xml` 与 `shime1.png` 预览约定 |
| `Mascot.xsd` | 53–64、108–120、139–158 | L1 XSD 校验依据 |
| `schema.properties` | 80–90 | `Information` 元数据字段 |
| `Script.java` / `Variable.java` | `${}` 缓存、`#{}` 每帧重算 | 表达式编辑器语义 |
| `ScriptFilter.java` | 10 | 包级白名单 |

### 附录 B · 设计器产出的最小示例（与 `examples/` 等价）

```xml
<!-- <conf>/actions.d/10-my-dance.xml （generated-by: shimeji-studio） -->
<Mascot>
  <ActionList>
    <Action Name="MyDance" Type="Sequence" Loop="true">
      <ActionReference Name="Stand" Duration="300" />
      <ActionReference Name="Sit"   Duration="300" />
      <ActionReference Name="Stand" Duration="300" />
    </Action>
  </ActionList>
</Mascot>
```

```xml
<!-- <conf>/behaviors.d/10-my-dance.xml -->
<Mascot>
  <BehaviorList>
    <Behavior Name="MyDance" Frequency="0" Hidden="true" />
  </BehaviorList>
</Mascot>
```

```xml
<!-- img/<skin>/conf/info.xml -->
<Mascot>
  <Information>
    <Name>My Skin</Name>
    <PreviewImage>shime1.png</PreviewImage>
    <Artist>...</Artist>
  </Information>
</Mascot>
```

### 附录 C · 术语

| 术语 | 含义 |
|---|---|
| 片段 / Fragment | `actions.d/*.xml` 或 `behaviors.d/*.xml` 中的一个文件，根节点 `<Mascot>` |
| 生效定义 | 按加载顺序最后一个同名 `Action`（FR-02 后写覆盖） |
| 图层 | 设计器对「全局基础 → 皮肤增量 → 托管行为表」这串加载顺序的可视化称呼 |
| 预览进程 | 设计器 spawn 的独立 Shimeji 进程，以 `--glados-managed` 启动，提供 HTTP 桥 |
| dry-run 校验 | 构建临时 `Configuration` 做 `validate()`，不安装到 `Main`、不影响运行中的角色 |
| `generated-by` 标记 | 设计器写在自有片段首行的注释，用于区分用户手写资产（P2） |
| 基础层 | 仓库自带的 91 个 Action / 57 个 Behavior / 132 个 Pose / 46 张 PNG，以及 `conf/*.xml` 主文件；扩展包对它**只读引用**（P4） |
| 扩展包 / Pack | 一组新增 Action / Behavior 片段 + 可选 PNG + `manifest.json` 构成的可安装、可停用、可卸载单元；引擎只看见摊平后的 `actions.d/` / `behaviors.d/` 片段（§4.4） |
| 纯净基线 | 未安装任何扩展包时的配置状态；卸载 + `reload` 必须逐字节回到该状态（AC-15-4） |

### 附录 D · 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-11-15 | 初稿。基于 M0+M1（`1f0c2d0`）、Q5 与 M2（`9e2d53c`）、`/status` 坐标上报（`d75fbc9`）的代码事实完成形态选型、架构、数据模型、API 契约、14 条需求、里程碑与验收清单；明确 M0 阶段引擎零改动 |
| v1.1 | 2026-11-15 | 按用户评审决策「基础动作保留、保证最大兼容性，新增动作作为扩展包」补 §4.4 扩展包模型（三层优先级 / 落法 A 与 B / 三条引擎约束 / 兼容护栏），新增 P4 原则、GD-9、DR-15、R-D11~R-D12、`/api/packs*` 四端点、Q-D8 与对应验收项；Q-D3 定稿。仅文档改动，未改代码、未提交 |
