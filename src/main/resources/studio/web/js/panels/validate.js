import { api, toast, escapeHtml } from '../api.js';

let lastResult = null;

export function render(container, ctx) {
  const state = ctx.state;
  if (!lastResult) lastResult = state.lastValidation ? normalize(state.lastValidation) : null;

  container.innerHTML = `
    <div class="card">
      <div class="card-head">
        <h3>校验中心</h3>
        <span class="small muted">L0 结构 → L1 XSD → L2 引擎 dry-run → L3 诊断 → L4 冲突与兼容</span>
        <div class="spacer"></div>
        <button class="btn btn-sm" id="btn-full-validate">全量校验当前工作区</button>
      </div>
      <div class="summary-bar" id="summary"></div>
      <div id="diag-list"></div>
    </div>
    <div class="grid-2">
      <div class="card">
        <div class="card-head"><h3>检查项说明</h3></div>
        <div class="card-body small">
          <table class="tbl">
            <thead><tr><th>层</th><th>内容</th><th>实现来源</th></tr></thead>
            <tbody>
              <tr><td>L0</td><td>XML well-formed、根节点 &lt;Mascot&gt;、只含一种列表</td><td>DOM 解析 + 结构检查</td></tr>
              <tr><td>L1</td><td>Mascot.xsd（引擎运行时不校验，设计器补上）</td><td>javax.xml.validation</td></tr>
              <tr><td>L2</td><td>引擎加载：跨片段引用、缺图、非法表达式、Duration&lt;1</td><td><span class="mono">ConfigurationFiles.build</span>（沙箱 dry-run）</td></tr>
              <tr><td>L3</td><td>缺图全貌、节点名不一致（旧 NextBehavior）</td><td><span class="mono">collectMissingImages</span> / <span class="mono">detectNodeNameMismatch</span></td></tr>
              <tr><td>L4</td><td>基础名/保留名冲突、Frequency&gt;0、<span class="mono">file:</span> 外部引用</td><td>设计器 + 安装前名字差集</td></tr>
            </tbody>
          </table>
          <p class="small muted">任何 error 都会阻止保存与安装；warning 允许继续但会在预览前再次提示。</p>
        </div>
      </div>
      <div class="card">
        <div class="card-head"><h3>当前生效配置</h3></div>
        <div class="card-body" id="effective"></div>
      </div>
    </div>`;

  container.querySelector('#btn-full-validate').addEventListener('click', async () => {
    try {
      lastResult = normalize(await api.post('/api/validate', { skin: state.skin, fragments: [] }));
      paint(container, ctx);
      toast(lastResult.summary.ok ? '校验通过' : '发现 ' + lastResult.summary.errors + ' 个错误',
        lastResult.summary.ok ? 'ok' : 'error');
    } catch (e) {
      toast('校验失败：' + e.message, 'error');
    }
  });

  paint(container, ctx);
}

function normalize(result) {
  return {
    summary: result.summary || { errors: 0, warnings: 0, infos: 0, ok: true },
    diagnostics: result.diagnostics || [],
    baseline: result.baseline,
    effective: result.effective,
    sources: result.sources || [],
  };
}

function paint(container, ctx) {
  const summaryEl = container.querySelector('#summary');
  const listEl = container.querySelector('#diag-list');
  const effectiveEl = container.querySelector('#effective');
  if (!lastResult) {
    summaryEl.innerHTML = '<span class="muted small">还没有校验结果。点右上角「全量校验当前工作区」，或保存片段时自动校验。</span>';
    listEl.innerHTML = '';
    effectiveEl.innerHTML = '';
    return;
  }
  const s = lastResult.summary;
  summaryEl.innerHTML = `
    <span class="tag ${s.errors ? 'err' : 'ok'}">${s.errors} 个错误</span>
    <span class="tag ${s.warnings ? 'warn' : ''}">${s.warnings} 个警告</span>
    <span class="tag">${s.infos} 条提示</span>
    <span class="small muted">${s.ok ? '所有 error 已清零，可以保存 / 运行' : '存在 error：保存与安装会被阻止'}</span>`;

  listEl.innerHTML = lastResult.diagnostics.length
    ? lastResult.diagnostics.map((d, i) => `
      <div class="diag ${d.severity}" data-diag="${i}">
        <div class="sev">${d.severity === 'error' ? 'ERROR' : d.severity === 'warning' ? 'WARN' : 'INFO'}</div>
        <div>
          <div class="msg">${escapeHtml(d.message)}</div>
          <div class="loc">${escapeHtml(d.file || '')}${d.action ? ' · Action[' + escapeHtml(d.action) + ']' : ''}${d.pose ? ' · ' + escapeHtml(d.pose) : ''}${d.hint ? ' · ' + escapeHtml(d.hint) : ''}</div>
        </div>
        <div class="layer">${escapeHtml(d.layer || '')}</div>
      </div>`).join('')
    : '<div class="empty"><strong>没有问题</strong>当前工作区通过 L0–L4 全部检查。</div>';

  listEl.querySelectorAll('[data-diag]').forEach(el => el.addEventListener('click', () => {
    ctx.jumpToDiagnostic(lastResult.diagnostics[Number(el.dataset.diag)]);
  }));

  effectiveEl.innerHTML = `
    <dl class="kv">
      <dt>基线</dt><dd>${lastResult.baseline ? (lastResult.baseline.actions + ' Action / ' + lastResult.baseline.behaviors + ' Behavior') : '—'}</dd>
      <dt>校验后</dt><dd>${lastResult.effective ? (lastResult.effective.actions + ' Action / ' + lastResult.effective.behaviors + ' Behavior') : '—'}</dd>
      <dt>加载文件</dt><dd class="mono small">${(lastResult.sources || []).map(escapeHtml).join('<br>') || '—'}</dd>
    </dl>`;
}

export async function refreshIfNeeded(ctx) {
  // 保存/安装后由调用方决定是否重跑；这里保持轻量
}
