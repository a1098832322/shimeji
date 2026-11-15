# Shimeji Studio · 皮肤及动作设计器 使用手册

> 版本：v1.1.0（与 `docs/skin-action-designer-design.md` v1.1 对应）
> 术语与 `docs/action-design-guide.md` 保持一致：**Action / Behavior / Pose / Sequence / ActionReference / NextBehaviorList / imageSet（皮肤）**。

---

## 1. 它是什么

Shimeji Studio 是一个**独立运行的本地 Web 应用**：它不修改引擎运行时，只做三件事——

1. 把「动作片段」「行为片段」「素材 / 皮肤」写成引擎本来就认识的 XML 与 PNG 文件；
2. 在保存前用**引擎自身的加载器**跑一遍 dry-run，判断这份配置引擎到底吃不吃得下；
3. 起一个**受管预览进程**，让改动立刻出现在桌面上。

设计边界（P4 原则）：`conf/actions.xml`、`conf/behaviors.xml`、`conf/settings.properties` 以及 91 个基础 Action / 57 个基础 Behavior **全量只读**，设计器永不改写；新增内容一律以片段（`actions.d/`、`behaviors.d/`）或扩展包的形式**叠加**。

---

## 2. 启动

### 2.1 一键启动

```bash
./studio.sh                 # 默认：当前目录为工作区，自动挑端口，自动打开浏览器
./studio.sh --port 7799     # 固定端口
./studio.sh --workspace /path/to/shimeji --no-browser
```

首次启动会 `mvn compile` 并生成依赖 classpath（`target/studio-classpath.txt`），之后启动在 1～2 秒内完成。

### 2.2 手工启动（IDE 里调试）

```bash
mvn -q -DskipTests compile
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -Ddev=true -cp "target/classes:$(cat target/cp.txt)" \
     com.group_finity.mascot.studio.StudioMain --port 7799
```

### 2.3 参数

| 参数 | 说明 |
| --- | --- |
| `--workspace <dir>` | 工作区，默认当前目录；必须能找到 `conf/` 或 `src/main/resources/conf/` |
| `--engine-root <dir>` | 引擎根目录（默认等于 workspace）。预览进程与设计器共享这个 cwd |
| `--port <n>` | 设计器监听端口，`0` = 随机 |
| `--engine-jar <path>` | 预览进程的 classpath，默认沿用当前进程 |
| `--no-browser` | 不自动打开浏览器 |

启动成功后终端会打印**生效的 `conf/` 与 `img/` 绝对路径**——顶部环境条里同样能看到（DR-01）。若布局不完整（缺 `actions.xml` / `behaviors.xml` / `img/`），保存类按钮会被禁用并给出原因。

> **关键约束**：引擎解析图片用的是「当前工作目录 + `./img/<皮肤>/`」。设计器因此强制以引擎根目录为 cwd 启动，并以同样的 `-Ddev` 取值拉起预览进程；两者不一致时会出现「校验通过但桌面缺图」。

---

## 3. 五个工作台

左侧导航共 7 个入口，其中 5 个是编辑台，2 个是运行/观测台。

### 3.1 皮肤与素材（DR-02 / DR-03 / DR-04）

- 皮肤列表直接读 `img/` 下的目录，新建皮肤会生成 `img/<名字>/`，原生 `ImageSetChooser` 立刻能识别。
- 素材网格支持：多选导入、拖拽导入、精灵图按「行 × 列」切图逐张预览、缩略图按需生成（落 `.studio/thumbs`）。
- **锚点画布**：拖拽十字准星即写入 `ImageAnchor`，锚点预设可保存/复用；勾选「自动镜像」时不写 `ImageRight`，取消勾选并给出右图时写入该属性。
- `info.xml`：可设置 `PreviewImage` 等字段；文件不存在时按需创建，已存在且**不是设计器生成的**文件必须显式确认（`force`）才会被覆盖。

### 3.2 动作编辑（DR-05 / DR-06）

- 左侧是片段列表（`actions.d/*.xml`，按文件名字典序，即真实加载顺序），右侧编辑当前片段。
- `Sequence` / `Select`：拖拽 `ActionReference` 排序，参数覆盖直接写在 `ActionReference` 上（不做「向下渗透」）。
- `Animation` / `Pose`：表格与时间轴双向编辑 `Image` / `ImageAnchor` / `Velocity` / `Duration`。
- 三个数值字段（`Velocity` / `Duration` / `ImageAnchor`）可切换**字面量 / 表达式**：`${}` 加载期求值一次、`#{}` 每帧求值；右侧「试算」调用引擎 `Variable.parse` 得到结果，与运行时完全同源。
- 保存时强制：根节点 `<Mascot>`、一个 `<ActionList>`、自动补 `generated-by: shimeji-studio` 归属标记。
- 原始 XML 模式可绕过表单直接编辑，未知属性会原样往返保留。

### 3.3 行为编辑（DR-07）

- 表格视图 + 状态图视图（`Add` 实线、`Add=false` 虚线）。
- 新建行为的默认值是 `Frequency="0" Hidden="true"`——即「可被显式调用，但不参与自主行为树随机」，这样预览不会改变原生行为分布（DR-15 AC-15-3）。
- 导入旧配置里的 `<NextBehavior>` 时给出改名提示：统一改用规范名 `<NextBehaviorList>`。

### 3.4 校验中心（DR-08）

分层报告，全部结论来自引擎同一份规则：

| 层 | 检查内容 | 实现 |
| --- | --- | --- |
| L0 | 结构：根节点、唯一列表节点、文件名合法性 | 设计器自身 |
| L1 | XSD 静态校验 | `conf/Mascot.xsd`（引擎运行时不校验，设计器补上） |
| L2 | 引擎 dry-run | 复制真实 `conf/` 到沙箱 → `ConfigurationFiles.build` → 失败即报引擎原文 |
| L3 | 缺图与节点名诊断 | 逐文件扫描，定位到 `文件 + Action[名] Pose[n]` |
| L4 | 命名与兼容护栏 | 基础名冲突、保留名（`AI*` / `SplitIntoTwo` / `PullUpShimeji`）、`Frequency>0` 提示、`file:` 不可分发提示 |

点击任意诊断会跳到对应面板与字段。保存动作的规则是：**L0–L4 有 error 就一个字都不写**（warn 不阻断）。

### 3.5 预览与运行（DR-10）

「运行」= 六步编排，任一步失败即停在当步，并把原因显示在步骤条上：

1. `launch` — 需要时 spawn 预览进程（`Main --glados-managed`），读 `glados-bridge.port` 完成端口发现；
2. `reload` — `/control {cmd:reload}`，失败时引擎自动回滚，角色不会消失；
3. `setRole` — 切皮肤（同皮肤时跳过）；
4. `setBehavior` — 设置行为（等角色落地后再设，见 §6）；
5. `status` — 回读 `/status` 作为闭环证据；
6. 结果与 `/status` 一起返回 UI。

快捷命令：`pause` / `resume` / `reload` / `AIIdle`（交还自主行为树）/ `remainOne`。

### 3.6 图层（DR-11）

按 `Result.getSources()` 的真实顺序列出参与构建的文件，并标注每个 Action / Behavior 名字的**生效定义**与**被覆盖定义**。片段按文件名字典序加载，后写覆盖，所以重命名片段会改变覆盖顺序——面板对此给出提示。

### 3.7 扩展包（DR-12 / DR-15）

- 安装 `.shimeji-pack.zip`：`manifest.json` + `actions.d/*.xml` + `behaviors.d/*.xml` + 可选 `img/<皮肤>/*.png`。
- 安装前后对 `conf/actions.xml`、`conf/behaviors.xml`、`conf/settings.properties`、`conf/Mascot.xsd` 做 SHA-256 比对，**不一致立即中止**（AC-15-1）。
- 冲突文件（已存在且非设计器生成）返回 409 并列出清单。
- 停用 = 把片段改名为 `*.xml.disabled`（引擎不再加载，文件仍在）；卸载 = 按注册表清单精确删除自己写过的文件。
- 导出皮肤包：`img/<皮肤>/**` + 分发用 `manifest.json`（引擎永不读 manifest）。

---

## 4. 一次完整的编辑流程

以「做一个站-坐-站的组合动作，并在桌面看到它」为例：

1. **动作编辑 → 新建片段**，文件名 `50-my-dance.xml`（`NN-slug` 的前缀决定加载顺序）。
2. 新建 `Action`：`Name=MyDance`、`Type=Sequence`、`Loop=true`；加入 3 个 `ActionReference`，分别指向 `Stand`、`Sit`、`Stand`，`Duration=900`。
3. **行为编辑 → 为 MyDance 注册行为**：`Name=MyDance`、`Frequency=0`、`Hidden=true`。
4. **校验中心** → 期望 `errors=0`；若出现 L3 缺图，按提示把图片导入 `img/<皮肤>/`。
5. **预览 → 运行**，目标行为填 `MyDance`，皮肤选当前皮肤 → 六步全绿，桌面上的角色开始站-坐-站。
6. 想微调节奏：回到动作编辑改 `Duration`，再点一次「运行」——`reload` 会重建角色并重新读取片段。

---

## 5. 接口（给二次开发用）

设计器只监听 `127.0.0.1`，统一返回 `{"ok":true,"data":…}` / `{"ok":false,"error":…}`，校验失败用 `422` + `diagnostics`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` `/api/workspace` `/api/baseline` | 健康检查、环境信息、基础层指纹 |
| GET | `/api/skins` `/api/assets` `/api/assets/thumb` `/api/info` | 皮肤与素材、缩略图、`info.xml` |
| GET | `/api/fragments` `/api/fragments/<kind>/<file>` | 片段列表与内容 |
| PUT/DELETE | `/api/fragments/<kind>/<file>` | 保存（先校验后落盘）/ 删除 |
| GET | `/api/manifest?skin=` | 生效清单：加载顺序、每个名字的生效定义与覆盖链 |
| POST | `/api/validate` `/api/simulate` | 校验 L0–L4 / 表达式试算 |
| POST | `/api/preview/start｜stop｜run｜command`，GET `/api/preview/status` | 预览编排 |
| GET/POST | `/api/packs`、`/api/packs/install`、`/api/packs/<id>/toggle`、`DELETE /api/packs/<id>`、`/api/packs/export` | 扩展包 |
| POST | `/api/export`、`/api/import/analyze`、`/api/import/skin` | 皮肤包导出与两阶段导入 |

---

## 6. 常见问题

**Q：校验全绿，但桌面上角色动作没变化？**
先看「预览」面板的步骤条与 `/status.behavior`：
- 单次行为播完会自动沿 `NextBehaviorList` 交还自主行为树，`/status.behavior` 很快就不是它了，这是正常调度；
- 接地类动作（`Stand` / `Sit` / `Sprawl`）在角色**离地**时会立刻抛 `LostGround` 退回 `Fall`。所以设计器在 `setBehavior` 前会等角色落地（最多 12 秒）；手工用 `/control` 调接口时也要注意这一点。

**Q：改了 `Duration`，为什么 `/status` 看不出区别？**
`/status` 只暴露 `role` / `behavior` / `count` / `mascots[x,y,w,h]`，不暴露当前 Pose 与帧号，因此帧级时长差异只能在桌面上肉眼确认。要在接口层量化「配置改动确实生效」，推荐用两种可观测手段：
- 新增一个行为名后 `reload`，再 `setBehavior` —— 旧名字返回 `unknown behavior`，新名字返回 `ok`；
- 把行为从 `Frequency=0 Hidden=true` 改为 `Frequency=50`，采样 `/status` 会看到它被自主行为树选中。

**Q：能改 `conf/actions.xml` 里的基础动作吗？**
不能，也不该——基础层全量只读（P4）。要改行为请加一个同名的设计器片段，它会在加载顺序里覆盖基础定义（L4 会以 error 提醒你「与基础同名」，这条护栏就是防止误覆盖）。

**Q：片段改名为什么要改两个文件？**
`Behavior` 默认绑定同名 `Action`。把 `MyDance` 改成 `MyDanceV2` 需要同时改 `actions.d` 与 `behaviors.d` 两个文件，而单文件保存是原子的、无法跨文件提交；直接改会得到 L2 报错 `无法找到对应的动作`。推荐的顺序：先加新名字的 Action → 改 Behavior 指向它 → 再删旧 Action。

**Q：`.studio/` 里是什么？**
运行时数据：`backups/`（最近 5 份 `.bak`）、`home/`（预览进程的 `glados-home`）、`logs/`、`thumbs/`、`packs/registry.json`。已加入 `.gitignore`，可以随时删除（会丢失备份与扩展包注册表）。
