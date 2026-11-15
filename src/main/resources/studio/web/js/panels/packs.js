import { api, toast, escapeHtml, confirmDialog, postForBlob, ApiError } from '../api.js';

export function render(container, ctx) {
  const state = ctx.state;

  container.innerHTML = `
    <div class="card">
      <div class="card-head">
        <h3>扩展包（Pack）</h3>
        <span class="small muted">基础层只读，扩展包只做加法；引擎零改动</span>
        <div class="spacer"></div>
        <label class="btn btn-sm btn-primary" for="pack-zip">安装扩展包</label>
        <input type="file" id="pack-zip" accept=".zip" class="hidden" />
      </div>
      <div id="pack-list"></div>
    </div>

    <div class="grid-2">
      <div class="card">
        <div class="card-head"><h3>基础层完整性（AC-15-1）</h3><div class="spacer"></div>
          <button class="btn btn-sm" id="btn-baseline">计算指纹</button></div>
        <div class="card-body" id="baseline-body"><p class="small muted">安装/卸载前后对比 conf/actions.xml、behaviors.xml、settings.properties 的 SHA-256，确认基础层一个字节都没变。</p></div>
      </div>
      <div class="card">
        <div class="card-head"><h3>规则</h3></div>
        <div class="card-body small muted">
          <ul style="margin:0;padding-left:16px">
            <li>包内片段安装进 <span class="mono">conf/actions.d/</span>、<span class="mono">conf/behaviors.d/</span>，文件名补齐 NN 号段（50–89）</li>
            <li>与基础 91 个动作 / 57 个行为同名 → 安装被硬拒绝（FR-02 后写覆盖会静默替换）</li>
            <li>AI* / SplitIntoTwo / PullUpShimeji 为保留名，禁止占用</li>
            <li>停用 = 改名为 <span class="mono">*.xml.disabled</span>（引擎只扫 .xml），不删文件</li>
            <li>卸载 = 按 manifest 清单删除自有文件，配置回到纯净基线</li>
            <li>manifest.json 只给设计器读，引擎永不读</li>
          </ul>
        </div>
      </div>
    </div>`;

  container.querySelector('#pack-zip').addEventListener('change', async (e) => {
    const file = (e.target.files || [])[0];
    e.target.value = '';
    if (!file) return;
    try {
      const zipBase64 = await ctx.fileToBase64(file);
      const result = await api.post('/api/packs/install', { zipBase64 });
      toast('已安装 ' + result.id + '（' + result.files.length + ' 个文件）', 'ok');
      if (result.warnings && result.warnings.length) {
        const errors = result.warnings.filter(w => w.severity === 'error');
        if (errors.length) toast('安装后仍有 ' + errors.length + ' 个错误，请查看校验中心', 'error');
      }
      await ctx.refresh();
    } catch (err) {
      if (err instanceof ApiError && err.diagnostics.length) {
        ctx.state.lastValidation = { diagnostics: err.diagnostics, summary: err.payload && err.payload.summary };
        toast(err.message, 'error');
        ctx.goto('validate');
      } else {
        toast('安装失败：' + err.message, 'error');
      }
    }
  });

  container.querySelector('#btn-baseline').addEventListener('click', async () => {
    const body = container.querySelector('#baseline-body');
    try {
      const data = await api.get('/api/baseline');
      const rows = Object.entries(data.files || {}).map(([file, hash]) =>
        `<tr><td class="mono small">${escapeHtml(file)}</td><td class="mono small">${escapeHtml(hash.slice(0, 16))}…</td></tr>`).join('');
      body.innerHTML = `<p class="small muted" style="margin-top:0">${escapeHtml(data.note || '')}</p>
        <table class="tbl"><thead><tr><th>文件</th><th>SHA-256</th></tr></thead><tbody>${rows}</tbody></table>`;
    } catch (e) {
      toast('读取失败：' + e.message, 'error');
    }
  });

  loadPacks(container, ctx);
}

async function loadPacks(container, ctx) {
  const host = container.querySelector('#pack-list');
  try {
    const data = await api.get('/api/packs');
    const packs = data.packs || [];
    if (!packs.length) {
      host.innerHTML = '<div class="empty"><strong>还没有安装扩展包</strong>把 actions.d/、behaviors.d/ 与 manifest.json 打成 zip 后安装；也可以先用动作工作台建片段，再导出。</div>';
      return;
    }
    host.innerHTML = '<table class="tbl"><thead><tr><th>包</th><th>版本</th><th>目标皮肤</th><th>文件</th><th>状态</th><th>安装时间</th><th></th></tr></thead><tbody>' +
      packs.map(p => `
        <tr data-pack="${escapeHtml(p.id)}">
          <td><div class="mono">${escapeHtml(p.id)}</div><div class="small muted">${escapeHtml(p.name || '')}</div></td>
          <td>${escapeHtml(p.version || '—')}</td>
          <td>${escapeHtml(p.targetSkin || '全局')}</td>
          <td class="num">${p.presentCount}/${p.fileCount}</td>
          <td>${p.enabled ? '<span class="tag ok">启用</span>' : '<span class="tag warn">停用</span>'}</td>
          <td class="small">${escapeHtml((p.installedAt || '').replace('T', ' ').slice(0, 19))}</td>
          <td class="nowrap">
            <button class="btn btn-sm" data-toggle="${escapeHtml(p.id)}" data-enabled="${p.enabled}">${p.enabled ? '停用' : '启用'}</button>
            <button class="btn btn-sm" data-export="${escapeHtml(p.id)}">导出</button>
            <button class="btn btn-sm btn-danger" data-uninstall="${escapeHtml(p.id)}">卸载</button>
          </td>
        </tr>`).join('') + '</tbody></table>';

    host.querySelectorAll('[data-toggle]').forEach(btn => btn.addEventListener('click', async () => {
      const enable = btn.dataset.enabled !== 'true';
      try {
        await api.post('/api/packs/' + encodeURIComponent(btn.dataset.toggle) + '/toggle', { enabled: enable });
        toast(enable ? '已启用（已 reload）' : '已停用（已 reload）', 'ok');
        loadPacks(container, ctx);
      } catch (e) {
        toast('切换失败：' + e.message, 'error');
      }
    }));

    host.querySelectorAll('[data-uninstall]').forEach(btn => btn.addEventListener('click', async () => {
      const ok = await confirmDialog('卸载扩展包',
        '<p class="small">将按 manifest 清单删除 <span class="mono">' + escapeHtml(btn.dataset.uninstall) + '</span> 自有文件并 reload，配置回到纯净基线。非本包文件不会被触碰。</p>');
      if (!ok) return;
      try {
        const result = await api.del('/api/packs/' + encodeURIComponent(btn.dataset.uninstall));
        toast('已卸载，删除 ' + result.removed.length + ' 个文件', 'ok');
        loadPacks(container, ctx);
      } catch (e) {
        toast('卸载失败：' + e.message, 'error');
      }
    }));

    host.querySelectorAll('[data-export]').forEach(btn => btn.addEventListener('click', async () => {
      try {
        const blob = await postForBlob('/api/packs/export', { id: btn.dataset.export });
        ctx.downloadBytes(await blob.arrayBuffer(), btn.dataset.export + '.shimeji-pack.zip');
      } catch (e) {
        toast('导出失败：' + e.message, 'error');
      }
    }));
  } catch (e) {
    host.innerHTML = '<div class="empty">读取失败：' + escapeHtml(e.message) + '</div>';
  }
}
