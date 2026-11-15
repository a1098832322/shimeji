# Shimeji 动作设计指南

- 编写日期：2026-11-15
- 适用版本：`feature/glados-integration` 分支，M1 已实现后的代码
- 需求与验收标准：`docs/action-authoring-extensibility-requirements.md`
- 可运行示例：`examples/README.md`

本文面向**不写 Java 也能设计桌宠动作**的人。阅读前先建立一个关键认知：
这套系统里没有任何「引用对象」「注册表」「ID」，**所有关联都靠字符串名字查表**。

---

## 1. 心智模型：三层名字绑定

| 层级 | 写法 | 查找方式 | 写错的后果 |
|---|---|---|---|
| Behavior → Action | `<Behavior Name="StandUp" .../>` | 默认找**同名** `<Action Name="StandUp">`；也可写 `Action="Sit"` 显式改名 | `Configuration.validate()` 启动报错 `NoActionFound` |
| Action → 子 Action | `<ActionReference Name="Sit" Duration="300"/>` | 按 `Name` 查 `actionBuilders` 表 | `validate()` 报错（错误信息含来源文件） |
| Pose → 图片 | `<Pose Image="/shime1.png" .../>` | 拼成 `img/<角色>/shime1.png` 路径字符串 | 加载时报 `FailedLoadImage`，信息含完整路径 |

**已核实的事实**：原生 `conf/behaviors.xml` 的 57 个 Behavior 名字，100% 都有同名 Action，
而且整个 `conf/` 里一处 `Action=` 都没写。反倒是 GLaDOS 的 8 个 AI 行为
（`AIWorking` → `Walk` 等）全部显式写了 `Action=`，用来让多个行为复用同一个动作。

`Sequence` / `Select` 构造时传入的是**空 `Animation` 列表**，所以组合型动作
**没有帧，也不需要任何图片**。

---

## 2. 最短路径：做一个新动作

以「站一下、坐一下、再站一下」为例，**零 Java、零新图片**：

```xml
<!-- 放到 <配置目录>/actions.d/10-my-dance.xml -->
<Mascot>
  <ActionList>
    <Action Name="MyDance" Type="Sequence" Loop="true">
      <ActionReference Name="Stand" Duration="300" />
      <ActionReference Name="Sit"   Duration="300" />
      <ActionReference Name="Stand" Duration="300" />
    </Action>
  </ActionList>
</Mascot>

<!-- 放到 <配置目录>/behaviors.d/10-my-dance.xml -->
<Mascot>
  <BehaviorList>
    <Behavior Name="MyDance" Frequency="0" Hidden="true" />
  </BehaviorList>
</Mascot>
```

然后：

```bash
curl -X POST http://127.0.0.1:<port>/control \
     -H 'Content-Type: application/json' -d '{"cmd":"reload"}'
curl -X POST http://127.0.0.1:<port>/control \
     -H 'Content-Type: application/json' \
     -d '{"cmd":"setBehavior","behavior":"MyDance"}'
```

`<port>` 从 `--glados-home/glados-bridge.port` 读取。完整试用步骤见 `examples/README.md`。

三个必须注意的点：

1. 片段根节点必须是 `<Mascot>`，内部可只含一个 `<ActionList>` / `<BehaviorList>`。
2. `Stand` / `Sit` 是 `BorderType="Floor"` 的 `Stay`，**角色必须在地面**，
   悬空会抛 `LostGroundException` 回落到 `Fall`。
3. `Duration` 写在 `ActionReference` 上，不要写在 `Behavior` 上（见 §4.2）。

---

## 3. 六种动作类型

| Type | 作用 | 是否需要 `<Animation>` / `<Pose>` | 典型用途 |
|---|---|---|---|
| `Stay` | 原地停留 | 需要 | 站、坐、趴 |
| `Move` | 带位移 | 需要 | 走、跑、爬 |
| `Animate` | 播放帧序列（有时长限制） | 需要 | 一次性动画 |
| `Sequence` | 顺序播放子动作 | **不需要** | 组合动作 |
| `Select` | 按条件挑一个子动作 | **不需要** | 分支 |
| `Embedded` | 反射加载 Java 类 | 可选 | 拖拽、交互、增殖等内置逻辑 |

`Sequence` 的 `Loop="true"` 会循环子动作；`Select` 靠子动作的 `Condition` 决定选中哪个。
`<Action>` 也可以内联嵌套，不必全部命名（`ActionBuilder.java:66-68`）。

> `Mascot.xsd` 里的 `Type` 枚举过时（仍写着 `Pause/Fixed/Composite`），
> 运行时并不按 XSD 校验。以本表为准，IDE 报「非法 Type」可忽略（已知缺陷 D-2）。

---

## 4. 组合动作与参数

### 4.1 引用与覆盖

`ActionReference` 可以覆盖被引用动作的参数：

```xml
<ActionReference Name="Stand" Duration="${500+Math.random()*1000}" />
<ActionReference Name="Falling" InitialVX="${mascot.environment.cursor.dx}" />
```

参数优先级：**Action 自身属性 < 上层传入参数 < `ActionReference` 上的属性**。

### 4.2 最容易踩的坑：参数不会向下渗透

`ActionBuilder.createActions()` 给子动作传的是**空 Map**。也就是说：

```xml
<!-- 错误：Duration 到不了子动作 -->
<Behavior Name="MyDance" Duration="300" ... />
<Action Name="MyDance" Type="Sequence">
  <ActionReference Name="Stand" />   <!-- 会一直播下去 -->
</Action>
```

`Duration` 必须写在每一个 `ActionReference` 上。这是当前实现的限制，
不是配置写错（需求文档 §2.2 有源码依据）。

### 4.3 同名覆盖（M1 新增）

同一份配置里，来自**不同文件**的同名 `Action` 采用「后写覆盖前写」，
日志会输出 `Action 'X' from <新文件> overrides the earlier definition from <旧文件>`。
来自**同一文件**的重复声明仍会报错（防笔误）。

注意覆盖是**整体替换**，不是按属性合并：覆盖文件里只写 `Duration` 不会保留原动作的子动作，
必须把整个 `<Action>` 写完整。

---

## 5. 条件与表达式

`${}` 与 `#{}` 里跑的是 Nashorn JavaScript，脚本可访问
`mascot`、`mascot.environment`、`mascot.anchor` 等对象（`ScriptFilter` 白名单）。

| 写法 | 求值时机 | 适用 |
|---|---|---|
| `${expr}` | 每个动作实例**求值一次**，之后缓存到 `init()` | 随机时长、初始速度 |
| `#{expr}` | **每帧**重新求值 | 跟随鼠标、实时位置判断 |

```xml
<Animation Condition="#{mascot.environment.cursor.y &lt; mascot.environment.screen.height/2}">
  <Pose Image="/shime26.png" ImageAnchor="64,128" Velocity="0,0" Duration="250" />
</Animation>
```

XML 里 `<` 必须写成 `&lt;`。

> ⚠️ **本期尚未实现 FR-05**：`<Pose>` 的 `Velocity` / `Duration` / `ImageAnchor`
> 仍走 `Integer.parseInt`，**不能写 `${}`**。表达式目前只能用在
> `Animation/@Condition`、`Behavior/@Condition` 和动作参数（含 `ActionReference`）上。

---

## 6. 行为与频率

| 属性 | 含义 |
|---|---|
| `Frequency` | 自主加权随机的权重；**`0` = 不参与随机**（`isEffective()` 直接返回 false） |
| `Hidden` | 不出现在托盘菜单 |
| `Toggleable` | 允许用户按角色禁用该行为（`DisabledBehaviours.<imageSet>`） |
| `Condition` | 生效条件表达式 |
| `NextBehaviorList` | 限定当前行为结束后可选的后续行为 |

`Frequency="0"` 的行为**仍能**被 `Configuration.buildBehavior(name)` 按名构建——
这正是 GLaDOS `setBehavior` 能预览 AI 动作的原因，也是新动作推荐的注册方式。

`NextBehavior` 特例：命令 `{"cmd":"setBehavior","behavior":"idle"}` 会回到
`buildNextBehavior(null, mascot)` 自主树，**不要**用它来预览新动作。

> ⚠️ **已知缺陷 D-1**：`conf/*.xml` 里写的是 `<NextBehavior>`（28 处），
> 而 `schema*.properties` 映射的是 `NextBehaviorList`，精确匹配下这些节点不会被解析。
> M1 只加了诊断日志与契约测试，**没有改变运行时行为**（修复方案见需求文档 Q5）。

---

## 7. 图片

- **不是「绑定」**：`<Pose Image="/shime1.png">` 只是一个路径字符串，
  加载时拼成 `img/<imageSet>/shime1.png`，由 `ImagePairLoader` 预读进 `ImagePairs` 缓存；
  运行时 `Pose.next()` 只是查表 `mascot.setImage(...)`。
- **文件名自由**：`shime1.png`…`shime46.png` 只是现有资源的命名习惯。
  唯一硬编码是 `ImageSetChooser` 用 `shime1.png` 做皮肤预览图，新建皮肤至少要留这一张。
- **相对子目录可用**：`Image="/sub/wave1.png"` 会解析到 `img/<角色>/sub/wave1.png`。
- **`ImageRight` 可省略**：不写时程序自动水平翻转左图生成右朝向版本；
  左右姿态不对称时再显式提供。
- **`ImageAnchor` 是像素锚点**：`64,128` 表示 128×128 图的底部中心，换尺寸必须同步改。
- **皮肤必须提供同名文件**：`imageSet` 是配置加载时注入的目录名，同一份 `actions.xml`
  被所有皮肤共用，每个皮肤目录都得有被引用到的全部 PNG。
- **缓存会过期问题**：图片只在配置加载时读盘。`reload` 命令会清缓存并重新读盘；
  但如果你直接改文件而不 reload，画面不会变（已知缺陷 D-3 的缓解手段）。

需要新造型的完整示例见 `examples/with-new-image/30-wave.xml`。

---

## 8. 动作包与皮肤隔离（M1 新增）

配置加载顺序固定为：

```
actions.xml  →  actions.d/*.xml（按文件名字典序）
behaviors.xml  →  behaviors.d/*.xml（按文件名字典序）
→ 统一 validate() → （托管模式）合并 glados-behaviors.xml
```

- `actions.d` / `behaviors.d` 与各自的主文件**同目录**。
- 目录不存在或为空时，行为与改造前完全一致。
- 所有片段加载完才 `validate()`，所以 A 文件引用 B 文件定义的 Action 是合法的。
- 皮肤级隔离：`img/<角色>/conf/actions.xml` + `img/<角色>/conf/actions.d/`
  可覆盖全局配置（当前皮肤级路径仍是「整份替换」，增量继承 FR-10 属 M2 待办）。
- 加载日志会列出本次实际读取的每个文件路径与顺序。

---

## 9. 预览与重载工作流

```
改 XML 片段
   ↓  {"cmd":"reload"}      ← 不重启、不弹框、不写 settings.properties
   ↓  失败：返回 {"ok":false,"error":"..."}；旧配置、旧图片缓存、现有角色全部保持可用
   ↓  成功：清图片缓存 → 重新读盘 → 替换配置 → 重建角色
   ↓
   ↓  {"cmd":"setBehavior","behavior":"MyDance"}
   ↓
桌面看到效果
```

要点：

- `reload` 在 `GladosCommand` 白名单内，近似命令（如 `reloadAll`）会被拒绝。
- 托管模式下每次重载都会**重新合并** `glados-behaviors.xml`，
  否则 `Frequency=0` 的禁增殖覆盖会丢失。
- 非托管模式没有 HTTP 桥，需要重启进程或用托盘「选择形象」触发一次配置重载。

---

## 10. 排障清单

| 现象 | 常见原因 | 查日志关键字 |
|---|---|---|
| 启动就报错，动作没加载 | 名字拼错 / 引用了不存在的 Action | `NoActionFound`、`NoBehaviourFound`、`DuplicateAction`；M1 起错误信息含来源文件路径 |
| 动作一直不结束 | `Duration` 写在 `Behavior` 上，没下传到子动作 | 无报错，见 §4.2 |
| 角色一播就掉下去 | `BorderType="Floor"` 的动作在悬空时播放 | `LostGroundException`，回落到 `Fall` |
| 图片不显示 / 启动时报加载失败 | PNG 缺失或 `imageSet` 目录下没有同名文件 | `FailedLoadImage`、`Configuration diagnostics`（M1 起会汇总所有缺失项） |
| 改了 XML 没变化 | 没发 `reload` | `Loaded configuration fragment` 是否重新出现 |
| 换了 PNG 但画面还是旧的 | 图片缓存未刷 | 确认 `reload` 的 `cleared N cached image(s)` 日志 |
| 新动作在自主行为里自己播出来了 | `Frequency` 没写 `0` | 用 `setBehavior` 显式预览即可 |
| `setBehavior` 返回 `unknown behavior` | Behavior 片段没放进 `behaviors.d/`，或 reload 失败 | `reload` 的 error JSON |
| `NextBehavior` 后继不生效 | 已知缺陷 D-1 | `does not match schema NextBehaviourList` |
| IDE 报 Type 非法 | `Mascot.xsd` 枚举过时（D-2） | 运行时不校验 XSD，可忽略 |

---

## 11. 已实现 / 未实现

**M1 已实现**：动作包分文件（FR-01）、同名 Action 覆盖（FR-02）、
显式 `reload` 命令（FR-03）、重载时刷新图片缓存（FR-04）、
设计时诊断与错误信息增强（FR-08 主体）、预览通道契约（FR-11）、示例与本文档（FR-12）。

**M2 待办（需单独评审）**：帧参数表达式化（FR-05）、图片路径进一步解绑（FR-06）、
动作类型 SPI / 脚本型动作（FR-07）、XSD 与运行时对齐（FR-09）、皮肤级增量覆盖（FR-10）。

**已知缺陷（单独立项）**：D-1 后继行为节点名不一致、D-2 XSD 枚举过时、
D-3 图片缓存需显式刷新（M1 的 reload 已缓解，其他路径仍未处理）。

---

## 12. 参考

- 需求与验收标准：`docs/action-authoring-extensibility-requirements.md`
- 关键源码索引：需求文档附录 A
- 示例资产：`examples/README.md`
- 核心类：`ConfigurationFiles`、`Configuration`、`BehaviorBuilder`、
  `ActionBuilder`、`ActionRef`、`AnimationBuilder`、`ImagePairs`
