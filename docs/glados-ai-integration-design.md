# Shimeji × GLaDOS AI 能力集成设计（重量级桌宠）

- 文档版本：v1.1
- 编写日期：2026-11-14
- 状态：**已实现（本分支开发完成，待 GLaDOS 侧联调）**
- 分支：`feature/glados-integration`
- 关联文档：GLaDOS 侧 `GLaDOSClaw-桌宠双引擎轻量级重量级-详细需求-2026-11-14.md`
- 许可：本改造沿用 Shimeji-ee 的 GPL v3；GLaDOS 侧已确认采用兼容开源许可

## 1. 背景

GLaDOS 桌宠分两档：轻量级（现有 sprite2d 引擎包）与重量级（Shimeji）。本仓库（shimeji）负责重量级档位，并作为后续「Shimeji 引用 GLaDOS AI 能力」的宿主。

当前 Shimeji 是完全自主的桌面宠物：`Manager` 40ms 主循环驱动 `Mascot`（透明窗口 + 行为树），行为由 `behaviors.xml` 加权随机选择，不感知外部任何状态。

目标：在不破坏自主行为的前提下，让 Shimeji 能接收 GLaDOS 的 AI 状态与指令，把「AI 正在处理 / 等待审批 / 任务失败 / 用户说了句话」映射成 Shimeji 行为。首期单向（GLaDOS → Shimeji），后续演进到对话闭环与双向感知。

## 2. 目标与非目标

### 2.1 目标

1. Shimeji 作为独立 Java 进程被 GLaDOS 启动/停止（生命周期由 GLaDOS 管）。
2. Shimeji 暴露一个**外部控制接口**（HTTP），GLaDOS 可查询状态、切换角色、切换行为、暂停/恢复、退出。
3. 新增 **AI 行为映射**：GLaDOS 任务状态 → Shimeji 行为，复用现有动作，不碰自主行为树。
4. 保留 Shimeji 完全自主运行的能力（不依赖 GLaDOS 也能玩）。
5. 为后续「对话闭环」「双向感知」预留清晰扩展点。

### 2.2 非目标

- 不在本阶段做 Shimeji → GLaDOS 的回调（第二期）。
- 不在本阶段做自然语言意图解析（第二期）。
- 不把 GLaDOS 的 AI 引擎搬进 Java 进程。
- 不改变 Shimeji 的自主行为模型（behaviors.xml 的加权随机 + 条件）。

## 3. 架构

```
┌─ GLaDOS Desktop ────────────────────────────────────────┐
│  ShimejiManager（主进程）                                │
│    spawn / stop / 崩溃检测 / 退出联动                    │
│    状态桥：任务状态 → POST /control                      │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP 127.0.0.1:随机端口
                         ▼
┌─ Shimeji 进程（复用 GLaDOS 捆绑 JRE）───────────────────┐
│  Main（改造）                                           │
│    ├─ 去掉 com/wishes 在线更新                          │
│    ├─ 新增 GladosBridge（JDK HttpServer 监听）          │
│    └─ 新增 GladosBehaviors.xml（AI 行为映射）           │
│  Manager（40ms 主循环，已有 setBehaviorAll/remainOne/   │
│           togglePauseAll/disposeAll/createMascot）      │
│  Mascot 透明窗口（JNA）                                 │
└─────────────────────────────────────────────────────────┘
```

**关键设计**：Shimeji 的 `Manager` 已具备全部控制能力（`setBehaviorAll` 切换行为、`remainOne` 单角色、`togglePauseAll` 暂停、`disposeAll` 清空、`createMascot` 加角色）。桥只是把这些方法暴露成 HTTP，**不新增行为引擎逻辑**。

## 4. 外部控制接口（GladosBridge）

### 4.1 启动参数

GLaDOS 启动 Shimeji 时传入：

```text
java -jar shimeji.jar --glados-managed --glados-port 0 --glados-home <dir>
```

- `--glados-managed`：受 GLaDOS 管理模式。开启后：
  - 不显示 Shimeji 自带的设置窗口/托盘菜单入口（或最小化）；
  - 不进行在线更新检查；
  - 退出时走优雅退出（收到 quit 命令后 disposeAll + exit）。
- `--glados-port 0`：监听端口，0 表示随机；启动后把实际端口写到 `--glados-home` 下的 `glados-bridge.port` 文件，GLaDOS 读取。
- `--glados-home`：GLaDOS 数据目录，用于端口文件、日志、AI 行为映射的覆盖文件。

### 4.2 HTTP 端点

所有端点 `POST /control`（body JSON），`GET /status` 查询。

#### 状态查询

```
GET /status
→ { "role": "魔理沙", "count": 1, "paused": false, "behavior": "AIIdle", "managed": true, "version": "1.0.4-glados" }
```

#### 控制命令

```
POST /control  { "cmd": "setRole",     "role": "魔理沙" }
POST /control  { "cmd": "setBehavior", "behavior": "AIWorking" }
POST /control  { "cmd": "pause" }
POST /control  { "cmd": "resume" }
POST /control  { "cmd": "createMascot", "role": "灵梦" }
POST /control  { "cmd": "remainOne",   "role": "魔理沙" }
POST /control  { "cmd": "disposeAll" }
POST /control  { "cmd": "quit" }
```

响应统一：

```json
{ "ok": true }
{ "ok": false, "error": "unknown behavior: AIWorking" }
```

### 4.3 命令 → Manager 映射

| cmd | Manager 方法 | 说明 |
|---|---|---|
| `setRole` | 重建角色（disposeAll + createMascot） | 切换形象 |
| `setBehavior` | `setBehaviorAll(config, name, imageSet)` | 切行为，见 §5 |
| `pause` / `resume` | `togglePauseAll()` | 暂停/恢复全部 |
| `createMascot` | `createMascot(imageSet)` | 增加角色 |
| `remainOne` | `remainOne(imageSet)` | 只留指定角色 |
| `disposeAll` | `disposeAll()` | 清空 |
| `quit` | 优雅退出 | 见 §6 |

### 4.4 安全

- 只监听 `127.0.0.1`，不绑外部网卡。
- 端口随机（0），避免固定端口冲突与预知。
- 命令白名单：只允许上述 cmd，未知命令返回错误。
- `--glados-managed` 未开启时**不启动桥**（保持原版完全自主）。

## 5. AI 行为映射（GladosBehaviors.xml）

### 5.1 设计原则

- **不碰 `behaviors.xml`**（自主行为树保持原样）。
- 新增一张映射表：GLaDOS 任务状态 → Shimeji 行为。
- 每个 AI 行为**复用现有动作**（Walk/SpinHead/LieDown/SitAndLookUp...），不新增动作。
- AI 行为是「一次性触发」还是「持续状态」由 GLaDOS 决定（见 §5.3）。

### 5.2 行为表

| GLaDOS 状态 | Shimeji 行为名 | 复用动作 | 说明 |
|---|---|---|---|
| `idle`（无任务） | `AIIdle` | 自主行为树 | 回到自主模式 |
| `running`（AI 处理中） | `AIWorking` | Walk + SpinHead | 兴奋/忙碌 |
| `waiting`（等待审批/回答） | `AIWaiting` | SitAndLookUp | 期待地看着你 |
| `review`（完成） | `AIReview` | SitAndSpinHead | 庆祝 |
| `failed`（失败） | `AIFailed` | LieDown | 沮丧趴下 |
| `disconnected`（断连） | `AIDisconnected` | 静止 | 失联 |

### 5.3 行为语义

`setBehavior` 的语义分两种：

- **状态型**（`AIWorking`/`AIWaiting`/`AIFailed`/`AIDisconnected`）：GLaDOS 持续推送，直到状态变化。Shimeji 保持该行为，不回到自主。
- **触发型**（`AIReview`）：一次性，播完回到上一个状态行为或自主。

实现：`GladosBehaviors.xml` 里每个行为是一个 `Behavior`（含 `NextBehavior` 指向自身或自主行为），`setBehaviorAll` 切换即可。触发型由 GLaDOS 侧在播完后再发一次回退命令（第一期简单处理）。

### 5.4 覆盖文件

GLaDOS 可在 `--glados-home` 下放置 `glados-behaviors.xml` 覆盖默认映射（如用户想把 `AIWorking` 映射成别的动作）。Shimeji 优先读覆盖文件，没有则用内置默认。

## 6. 生命周期与退出

### 6.1 受管模式退出

```
GLaDOS quit → POST /control { cmd: "quit" }
Shimeji     → disposeAll() → System.exit(0)
```

GLaDOS 侧兜底：`quit` 命令后 5s 未退出则 `kill` 进程。

### 6.2 崩溃检测

Shimeji 进程异常退出时，GLaDOS 侧 `ShimejiManager` 检测 exit 事件：
- 桌宠仍启用 → 提示 + 自动重启（可配置，默认开）。
- 桌宠已停用 → 不重启。

### 6.3 与 GLaDOS 退出联动

GLaDOS `quitApp` 的 `stopBackendOnce` 中增加 `shimejiManager.stop()`，保证主程序退出时 Shimeji 一并退出。

## 7. Shimeji 侧代码改动清单（本分支）

| 文件 | 改动 |
|---|---|
| `src/main/java/com/group_finity/mascot/Main.java` | 解析 `--glados-managed/--glados-port/--glados-home`；受管模式跳过更新检查、跳过托盘设置窗口；`loadConfiguration` 改 public；启动桥；`exit()` 停桥；受管模式 `setExitOnLastRemoved(false)` 防止 setRole 竞态退出 |
| `src/main/java/com/group_finity/mascot/glados/GladosBridge.java`（新） | JDK `HttpServer` 监听 127.0.0.1；命令 → Manager 映射；写端口文件；`setRole`/`createMascot` 前 `ensureConfiguration` 自动加载目标角色配置 |
| `src/main/java/com/group_finity/mascot/glados/GladosCommand.java`（新） | 命令枚举 + 参数校验（白名单） |
| `src/main/java/com/group_finity/mascot/glados/GladosOptions.java`（新） | 启动参数解析 |
| `src/main/java/com/group_finity/mascot/glados/GladosBehaviors.java`（新） | 加载/覆盖 AI 行为映射（`--glados-home/glados-behaviors.xml` 优先，内置兜底） |
| `src/main/resources/glados-behaviors.xml`（新） | 默认 AI 行为表（AIIdle/AIWorking/AIWaiting/AIReview/AIFailed/AIDisconnected，复用现有动作） |
| `src/main/java/com/group_finity/mascot/config/Configuration.java` | 新增 `loadGladosBehaviors(Entry)` 合并 AI 行为表 |
| `src/main/java/com/group_finity/mascot/Manager.java` | 新增 `setBehaviorAllByName`（直接按行为名构建，绕过 schema 限制）、`getMascotsForBridge`（只读列表）、`remainOneOnly`（只留指定角色，修正原 `remainOne(String)` 语义）、`getActiveRole` |
| `src/test/java/com/group_finity/mascot/glados/GladosBridgeTest.java`（新） | 20 个单元测试（参数解析/命令白名单/参数校验/端口文件） |
| `pom.xml` | 无改动（新资源自动打入，JDK HttpServer 无新依赖） |

**不动的**：`behavior/`、`action/`、`animation/`、`environment/`、`win/`、`mac/`、`Mascot.java` 核心逻辑。自主行为完全保留。

## 8. 后续演进（第二期及以后）

### 8.1 对话闭环（第二期）

```
用户对 Shimeji 说话（或点击气泡）
  → Shimeji 把文本 POST 给 GLaDOS（新增 /glados/chat 回调）
  → GLaDOS AI 解析意图（"集合"/"跳舞"/"安静"）
  → GLaDOS 回 POST /control { cmd: "setBehavior", behavior: "..." }
  → Shimeji 播对应行为 + 显示 AI 回复气泡
```

Shimeji 侧需要：气泡 UI（现有 `Mascot` 右键菜单可参考）、文本输入（或语音，后续）。

### 8.2 双向感知（第三期）

```
Shimeji 行为事件（点击/拖拽/长时间 idle）→ 回调 GLaDOS
  → GLaDOS 决定是否唤起桌宠对话/通知
```

### 8.3 扩展点预留

- 桥协议用 JSON，天然可扩展字段。
- `GladosBehaviors.xml` 是纯数据，加行为不改代码。
- 命令白名单是数组，加命令改一处。

## 9. 测试矩阵

### 9.1 单元测试

| 测试 | 期望 |
|---|---|
| `--glados-managed` 解析 | 正确设置受管模式 |
| `--glados-port 0` | 随机端口，写端口文件 |
| 未知 cmd | 返回 `{ ok: false, error }` |
| `setBehavior` 不存在的行为 | 返回错误，不崩溃 |
| 非受管模式 | 不启动桥 |

### 9.2 集成测试（Windows / macOS 实机）

| 场景 | 期望 |
|---|---|
| GLaDOS 启动 → Shimeji 出现 | 角色正常显示 |
| GLaDOS 任务 running | Shimeji 播 AIWorking |
| GLaDOS 任务 failed | Shimeji 播 AIFailed |
| GLaDOS 切角色 | Shimeji 换形象 |
| GLaDOS 退出 | Shimeji 一并退出，无孤儿进程 |
| Shimeji 手动 kill | GLaDOS 检测崩溃并重启 |
| 无 GLaDOS 直接跑 shimeji.jar | 原版自主行为，无桥 |

### 9.3 回归

- 原版自主行为（ChaseMouse/Fall/Dragged）不受影响；
- 多角色、繁殖、拖拽、抛出行为正常；
- 21 套形象均可加载。

## 10. 完成定义

- [x] `--glados-managed` 受管模式可用，桥监听 127.0.0.1 随机端口；（2026-11-14 实测通过，端口文件写入正常）
- [x] 全部控制命令（setRole/setBehavior/pause/resume/createMascot/remainOne/disposeAll/quit）可用；（2026-11-14 实测通过）
- [x] AI 行为映射表生效，GLaDOS 任务状态能切换 Shimeji 行为；（2026-11-14 实测 AIWorking/AIWaiting/AIReview/AIFailed/AIIdle 全部生效）
- [x] 非受管模式完全保持原版行为；（未加 `--glados-managed` 时不启动桥，代码路径与原来一致）
- [ ] GLaDOS 退出联动、崩溃重启验证通过；（需 GLaDOS 侧 ShimejiManager 实现后联调）
- [ ] Windows + macOS 实机验收通过；（macOS 已实测，Windows 待验）
- [x] 单元 + 集成 + 回归测试全绿；（20 个单元测试通过；集成测试 macOS 实测通过；回归需完整跑原版行为）

## 11. 风险与缓解

| 风险 | 缓解 |
|---|---|
| macOS 透明窗口需辅助功能授权 | 实测；GLaDOS 设置页提示授权 |
| JDK HttpServer 在 Swing 主线程冲突 | 桥用独立线程池，不碰 EDT |
| 行为映射与自主行为竞争 | AI 行为是显式 setBehaviorAll，自主行为只在 AIIdle 时恢复 |
| 端口文件竞态 | GLaDOS 轮询读取，超时重试 |