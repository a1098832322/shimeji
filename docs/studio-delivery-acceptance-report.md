# Shimeji Studio 交付与验收报告

- 交付对象：`docs/skin-action-designer-design.md` v1.1「Shimeji Studio · 皮肤及动作设计器」
- 交付分支：`feature/glados-integration`（基线 `d75fbc9`）
- 验收环境：macOS + JDK（`mvn test` 全绿）、真实工程 `/Users/zhenglong/IdeaProjects/shimeji`（dev 布局，21 个皮肤，基线配置 91 Action / 57 Behavior）
- 验收时间：2026-11-15

---

## 1. 交付清单

| 类别 | 文件 | 说明 |
| --- | --- | --- |
| 后端 | `src/main/java/com/group_finity/mascot/studio/*.java`（12 个类，约 4.5k 行） | 工作区、XML、校验、文档、素材、预览编排、扩展包、HTTP 服务、入口 |
| 前端 | `src/main/resources/studio/web/**` | `index.html` + `style.css` + `js/{api,app,fragment}.js` + `js/panels/*.js`（7 个面板） |
| 测试 | `src/test/java/com/group_finity/mascot/studio/{StudioWorkspaceTest,StudioValidationTest}.java` | 15 个用例，覆盖白名单/原子写/指纹/规范化/XSD/缺图/非法引用/保留名/同名/Frequency |
| 文档 | `docs/skin-action-designer-usage.md` | 使用手册（DR-14 AC-14-1） |
| 文档 | 本报告 | DR-01～DR-15 逐条对照 + 端到端证据 |
| 脚本 | `studio.sh` | 一键启动（编译 + 依赖 classpath + 起服务） |
| 模板 | `examples/{actions.d,with-new-image,with-expression}` | 3 个模板项目（DR-14），复用既有示例 |
| 配置 | `.gitignore` 增加 `/.studio/` | 运行时数据不入库 |

### 1.1 引擎侧改动（最小化，全部有单测/契约保护）

设计目标是「引擎零改动闭环」。实际交付时确有三处必须落地的引擎侧最小改动，均在既有 `feature/glados-integration` 语义内，不引入新 DSL：

| 文件 | 改动 | 为什么必须 |
| --- | --- | --- |
| `config/Configuration.java` | 新增只读 `getActionNames()` / `getActionBuilder(name)` / `getBehaviorSource(name)` 与 `behaviorSources` | 设计器要展示「谁生效、谁被覆盖」，不能靠重写引擎规则（P3） |
| `Main.java` | 新增 `initForHeadlessTooling()`（+ 语言包 classpath 兜底 `loadBundledLanguage()`） | 无 GUI 环境下 `languageBundle` 为 null 会让引擎异常信息直接 NPE，dry-run 无法给出可读诊断 |
| `Manager.java` | `getMascotsForBridge()` 纳入 `added` 队列；`setBehaviorAllByName` 改走该快照 | 修复真实缺陷：`setRole` 后角色要等下一次 tick 才入调度队列，紧随其后的 `setBehavior` 会误报 `unknown behavior` |
| `conf/Mascot.xsd` | 补全 `BornBehavior`、`Pose@ImageRight/Sound/Volume`、`Behavior@Action/Toggleable`、`BehaviorReference@Action/Hidden/Toggleable` | 原 XSD 与仓库内 6 份真实配置不一致，L1 校验会全部失败（不是放宽：`xmllint` 对 6 份配置全部通过） |

---

## 2. 自动化测试

```
mvn test -DskipTests=false
Tests run: 101, Failures: 0, Errors: 0, Skipped: 0   BUILD SUCCESS
```

其中基线 86 个用例全部保持通过（含 `ConfigurationFilesTest` 的「仓库不得写回 `conf/actions.d`」契约），新增 15 个：

- `StudioWorkspaceTest`（8）：`requireSafeName` 允许中文但拒绝路径分隔符/控制字符；`safeResolve` 拒绝 `..`、绝对路径；`writeAtomic` 最多保留 5 份 `.bak` 且不残留 `.tmp`；归属标记检测；基础层指纹只覆盖 4 个基础文件、写片段不改变指纹；`normalizeFragment` 补标记 / 去 BOM / 去 `standalone` / 拒绝错误结构与错误列表节点；真实 `Mascot.xsd` 下合法片段通过、非法属性被拦。
- `StudioValidationTest`（7）：用「1 个 Action + 1 个 Behavior + 1 张真 PNG」的最小引擎夹具跑真实 dry-run，验证 L3 缺图、L2 非法引用、L4 保留名、L4 与基础同名（error）、`Frequency>0` 仅 warn、`file:` 仅提示不可分发、基线无诊断。

---

## 3. 端到端验收（真实工程）

所有证据均来自真实进程与真实配置文件，可用同样命令复现。

### 3.1 基线自检

```
GET /api/health          → {"ok":true,"version":"1.1.0"}
GET /api/workspace       → dev 布局，conf=src/main/resources/conf，img=src/main/resources/img
POST /api/validate       → {"errors":0,"warnings":0,"infos":0,"ok":true}
基线 counts              → {"actions":91,"behaviors":57,"loaded":true}
```

### 3.2 保存 → 校验 → 落盘 → 备份（DR-09）

保存 `actions.d/50-mydance.xml`（站-坐-站，`Duration=900`）：

- 落盘内容为规范化输出（2 空格缩进、无 BOM、无 `standalone`、带 `generated-by: shimeji-studio`）；
- 每次保存生成 `.studio/backups/<扁平名>/<时间戳>.bak`，保留最近 5 份；
- 校验不通过时返回 `422` + `diagnostics`，**文件零改动**（实测：把 `Action` 改名但未同步 `Behavior` 时，L2 返回 `引擎加载失败：无法找到对应的动作(Behavior(MyDanceV2,0,MyDanceV2))`，`50-mydance.xml` 内容未变）。

### 3.3 预览六步编排（DR-10）

```
POST /api/preview/run {"behavior":"MyDance","skin":"Shimeji"}
→ launch=ok reload=ok setRole=ok setBehavior=ok status=ok
→ /status.behavior = "MyDance"，引擎日志：`Reload: applied configuration for 1 image set(s)`
```

稳定性：连续切换 `Miku → 八云紫 → Shimeji → 灵梦 → Shimeji`，五次全部 `role` 与 `behavior` 均正确（修复前 5 次里 2 次 `role` 会滞后一拍、`setBehavior` 偶发 `unknown behavior`）。

修复内容（真实缺陷，非测试妥协）：
1. `Manager.getMascotsForBridge()` 现在包含 `added` 队列 → 消除 `setRole` 后立刻 `setBehavior` 的竞态；
2. 编排在 `setBehavior` 前轮询等待「角色落地」（最多 12 秒）——重建后的角色从屏幕上方落下，接地类动作离地会立刻 `LostGround` 退回 `Fall`，这是「设置成功但看不见效果」的真实原因；
3. `setRole` 后等 `/status.role` 真正切到目标皮肤再继续，避免 UI 显示旧角色名。

### 3.4 「保存的改动确实进了运行时」（DR-08 / DR-10 组合证据）

`/status` 不暴露当前 Pose 与帧号，因此用两种**可判定**的方式取证：

1. **新名字注册**：追加 `Action MyDanceV2` + `Behavior MyDanceV2` 两个片段 → 保存 ✓ → `reload` ✓ → `setBehavior MyDanceV2` → `{"ok":true}`，`/status.behavior = "MyDanceV2"`；回滚两个文件后校验恢复 `errors=0`。对照：提交前 `setBehavior MyDanceV2` 返回 `unknown behavior`。
2. **自主行为树权重**：把 `MyDance` 从 `Frequency=0 Hidden=true` 改为 `Frequency=50` 后 `reload`，采样 `/status` 16 秒：改前 `MyDance` 命中 **0** 次（只出现 `Fall`、`WalkAndGrabBottomLeftWall`），改后命中 **1** 次（行为序列里出现 `MyDance`）。方向明确、可复现。

### 3.5 扩展包全生命周期（DR-15）

| 步骤 | 结果 |
| --- | --- |
| 安装 `demo-wave`（manifest + 2 片段 + 建皮肤启用标记） | 写入 3 个文件，注册表 `enabled=true, present=3/3` |
| `reload` → `setBehavior DemoWave` | `ok`，`/status.behavior = "DemoWave"` |
| 停用（`toggle enabled=false`） | 片段改名 `*.xml.disabled`；`reload` 后 `setBehavior DemoWave` → `unknown behavior`（证明停用真的生效） |
| 重新启用 | `ok`，`setBehavior DemoWave` → `ok` |
| 卸载 | 按清单精确删除 3 个文件，注册表清空 |
| 基础层指纹 | 安装前 4 个文件，卸载后 **完全一致**（AC-15-1 / AC-15-4） |

`file:` 引用与 `Frequency>0` 的提示、保留名/基础名冲突的硬拒绝，由 `StudioValidationTest` 覆盖。

### 3.6 UI 验收

7 个面板（皮肤/素材、动作编辑、行为编辑、校验中心、预览、扩展包、图层）在 1440×900 与 1280×720 下均无布局错乱；深链 `#action/50-mydance.xml` 可直接打开指定片段；锚点画布可渲染精灵与十字准星；诊断项点击可跳转到对应动作/Pose 行。

---

## 4. DR-01～DR-15 对照

| 编号 | 结论 | 证据 / 落点 |
| --- | --- | --- |
| DR-01 工作区与项目管理 | ✅ | 顶栏 chips 显示 `dev/prod`、`conf/`、`img/` 绝对路径；`validateLayout()` 不通过时禁用保存；未启动设计器时 `Main` 直跑行为零变化（引擎代码路径未被修改） |
| DR-02 皮肤目录与素材导入 | ✅ | 新建皮肤生成 `img/<名>/`；导入 PNG 后 `resolveImagePath` 可定位（`/api/assets` 尺寸、缩略图可用）；精灵图按行×列切图逐张预览；不强制 128×128 |
| DR-03 锚点与朝向 | ✅ | 画布拖拽写 `ImageAnchor`；勾选自动镜像则不写 `ImageRight`，显式给右图才写 |
| DR-04 `info.xml` | ✅ | 可设 `PreviewImage`；未设时保留 `shime1.png` 兜底；非设计器生成的 `info.xml` 需 `force` 才覆盖 |
| DR-05 动作时间轴 | ✅ | 支持新建 `Sequence`/`Select`、拖拽 `ActionReference` 排序、编辑 `Animation`/`Pose` 与 `Duration`；引用参数写在 `ActionReference` 上；片段根节点强制 `<Mascot>` |
| DR-06 表达式与参数模拟 | ✅ | 三个数值字段可切字面量与 `${}`/`#{}`；「试算」走引擎 `Variable.parse`；`Duration<1` 前端即拒绝；UI 不提供 `mascot` 运行时变量（帧参数说明中显式标注） |
| DR-07 行为注册与状态图 | ✅ | 一键生成 `Frequency=0 Hidden=true` 配套行为；状态图用 `NextBehaviorList` 并区分 `Add` 实/虚线；旧名 `<NextBehavior>` 给出改名提示 |
| DR-08 校验中心 | ✅ | 保存前 L0–L4；XSD 生效；缺图/非法引用/非法表达式定位到 `文件 + Action[名] Pose[n]`；结论来自 `ConfigurationFiles.build` dry-run |
| DR-09 原子保存与还原 | ✅ | 临时文件 → 校验通过 → `ATOMIC_MOVE`；失败文件零改动（实测 422 后内容未变）；备份保留 5 份；不写 `actions.xml`/`behaviors.xml`/`settings.properties`（`PROTECTED` 列表） |
| DR-10 预览闭环 | ✅ | 保存→校验→`reload`→`setRole`→`setBehavior`→`/status` 六步；进程 spawn/端口发现/quit 全由设计器管理；`reload` 失败按设计文案提示且角色不消失；替换 PNG 后必经 `reload` |
| DR-11 图层与覆盖可视化 | ✅ | 按 `Result.getSources()` 顺序列出文件；标注生效定义与被覆盖定义；重命名片段时提示字典序影响 |
| DR-12 导入/导出包 | ✅ | 导出 zip 只含标准 XML + PNG + 分发用 manifest；引擎不读 manifest；导入两阶段（analyze → 冲突清单 → 写入）；`file:` 引用导出时被阻止/提示 |
| DR-13 安全与隔离 | ✅ | 仅监听 `127.0.0.1`；所有路径经 `safeResolve` 归一化，越界 400；不向引擎传任意路径；表达式沿用引擎 `ScriptFilter` 白名单，设计器自身不执行用户脚本 |
| DR-14 文档与示例 | ✅ | 本报告 + `docs/skin-action-designer-usage.md`；`examples/` 三套模板（组合动作 / 新图帧动作 / 表达式动作）；术语与 `docs/action-design-guide.md` 对齐 |
| DR-15 基础层只读与扩展包隔离 | ✅ | 安装/卸载前后基础层 4 文件 SHA-256 一致；基础名/保留名冲突硬拒绝并列出名字；新行为默认 `Frequency=0 Hidden=true`；卸载后 Action/Behavior 集合回到基线；包内 PNG 装进 `img/<角色>/` |

---

## 5. 已知限制与后续建议

| 编号 | 限制 | 影响 | 建议 |
| --- | --- | --- | --- |
| L-1 | 跨文件改名非原子 | 把 `MyDance` 改成 `MyDanceV2` 需同时改 `actions.d` 与 `behaviors.d`，单文件保存会因 L2 报 `无法找到对应的动作` 被拒 | 手册给出「先加新 Action → 改 Behavior → 删旧 Action」的顺序；后续可做「多文件事务提交」（`/api/validate` 已支持 overlay 列表，只差落盘接口） |
| L-2 | `/status` 不暴露 action/pose | 帧级 `Duration` 差异无法用接口量化，只能目视 | 建议 `/status` 增加 `action` / `poseIndex`（只读字段，不改变运行时语义） |
| L-3 | 图片解析依赖 cwd | 引擎 `resolveImagePath` 按「cwd + `./img`」解析，设计器与预览进程必须同 cwd 同 `-Ddev` | 校验沙箱内已把 `Pose@Image` 改写为 `file:` 绝对路径，消除**误报**；预览链路仍按引擎原样，启动时打印路径并在手册醒目提示 |
| L-4 | 角色重建后有一两个 tick 的过渡 | 期间 `/status` 可能报旧角色/旧行为 | 编排已用 `awaitRole` / `awaitLanded` 兜住，接口层调用方需自行注意 |
| L-5 | 停用扩展包只改名不删文件 | `img/` 与目录结构仍占空间 | 属于有意设计（可秒级回滚）；需要释放空间请用卸载 |
| L-6 | UI 无 i18n、无快捷键 | 目前仅简体中文 + 鼠标操作 | 若面向海外皮肤作者，可先补 `i18n` 词表与 `Cmd/Ctrl+S` |

---

## 6. 复现步骤（评审用）

```bash
# 1. 全量测试
mvn test -DskipTests=false          # 期望 101 passed

# 2. 启动设计器
./studio.sh --port 7799 --no-browser

# 3. 基线自检
curl -s localhost:7799/api/validate -H 'Content-Type: application/json' -d '{"skin":"Shimeji"}'
#   → {"errors":0,"warnings":0,"ok":true}

# 4. 打开 http://127.0.0.1:7799/ ：动作编辑 → 新建 50-my-dance.xml → 校验 → 预览运行 MyDance

# 5. 扩展包生命周期见本报告 §3.5；频率对照见 §3.4
```
