# 动作示例资产

需求文档：`docs/action-authoring-extensibility-requirements.md` FR-12
使用说明：`docs/action-design-guide.md`

这些文件**不会自动生效**，也不会写回 `conf/`。仓库里的 `src/main/resources/conf/`
保持原样，用户直接双击运行时看不到任何示例动作。

## 目录

| 路径 | 内容 |
|---|---|
| `actions.d/10-my-dance.xml` | 示例 A：零新图的组合动作（复用 `Stand` / `Sit`） |
| `behaviors.d/10-my-dance.xml` | 示例 A 的配套行为声明 |
| `with-new-image/30-wave.xml` | 示例 B：需要新 PNG 的帧动作 |
| `with-new-image/30-wave-behaviors.xml` | 示例 B 的配套行为声明 |

片段根节点都是 `<Mascot>`，与主动作文件一致；内部只含一个 `<ActionList>`
或 `<BehaviorList>` 即可。同一目录下的文件按**文件名字典序**加载，
后面的同名 `Action` 会覆盖前面的（`docs/action-design-guide.md` §3）。

## 试用示例 A（推荐先试这个，不需要任何图片）

1. 把 `actions.d/10-my-dance.xml` 复制到你**运行时**的动作包目录：
   - 开发模式（`-Ddev=true`）：`src/main/resources/conf/actions.d/`
   - 打包运行：发行目录的 `conf/actions.d/`
   - 只给某个角色用：`img/<角色>/conf/actions.d/`（皮肤级隔离）
2. 把 `behaviors.d/10-my-dance.xml` 同样复制到 `.../behaviors.d/`。
3. 让桌宠处于**地面**状态（`Stand` / `Sit` 是 `BorderType="Floor"`，
   悬空时会抛 `LostGroundException` 回落到 `Fall`）。
4. 下发重载命令，不需要重启、不会弹框：

   ```bash
   curl -X POST http://127.0.0.1:<port>/control \
        -H 'Content-Type: application/json' \
        -d '{"cmd":"reload"}'
   ```

5. 预览动作：

   ```bash
   curl -X POST http://127.0.0.1:<port>/control \
        -H 'Content-Type: application/json' \
        -d '{"cmd":"setBehavior","behavior":"MyDance"}'
   ```

   `<port>` 从 `--glados-home/glados-bridge.port` 读取。非托管模式没有 HTTP 桥，
   需要重启进程或用托盘「选择形象」触发一次配置重载。

6. 改 `Duration="300"` → 再发一次 `reload` → 再 `setBehavior`，即可看到变化。

试完把两个片段文件删掉再 `reload` 一次即可还原，**不要把这些文件提交进
`src/main/resources/conf/`**。

## 试用示例 B（需要自备图片）

1. 先按 `with-new-image/30-wave.xml` 顶部注释准备 `wave1.png` / `wave2.png`，
   放到 `img/<角色>/`。
2. 把 `30-wave.xml` 放进 `actions.d/`，`30-wave-behaviors.xml` 放进 `behaviors.d/`
   （注意改到对应目录，两个文件不属于同一类）。
3. 同示例 A 的步骤 4–5，把 `behavior` 换成 `MyWave`。

> 已知限制：图片在**配置加载时**预读进 `ImagePairs` 缓存。新增或替换 PNG 后
> 必须触发 `reload`（命令会清缓存并重新读盘）；只改 XML 而不 reload 同样不生效。
