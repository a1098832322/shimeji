import { api, toast, escapeHtml } from '../api.js';

export function render(container, ctx) {
  const state = ctx.state;
  const preview = state.preview || {};
  const status = (preview.status && typeof preview.status === 'object') ? preview.status : {};
  const run = state.lastRun;

  container.innerHTML = `
    <div class="grid-2" style="grid-template-columns:minmax(0,1fr) 340px">
      <div style="display:grid;gap:12px;min-width:0">
        <div class="card">
          <div class="card-head">
            <h3>预览编排</h3>
            <span class="small muted">保存 → 校验 → reload → setRole → setBehavior → /status</span>
            <div class="spacer"></div>
            <button class="btn btn-sm" id="btn-run-now">运行当前目标</button>
          </div>
          <div class="card-body">
            ${run ? stepsHtml(run) : '<div class="empty"><strong>还没有运行记录</strong>选中一个动作/行为后点顶部「运行」，或用右侧的快捷命令。</div>'}
            ${run && run.steps.every(x => x.ok) ? '<p class="small muted" style="margin:10px 0 0">桌面上的角色应已切换到目标行为；状态见右侧。</p>' : ''}
            ${run && !run.steps.every(x => x.ok) ? '<p class="small" style="margin:10px 0 0;color:var(--danger)">失败时引擎已按快照回滚：上一次可用配置仍在运行，角色不会消失。</p>' : ''}
          </div>
        </div>

        <div class="card">
          <div class="card-head"><h3>预览进程</h3><div class="spacer"></div>
            <span class="pill ${preview.state === 'running' ? 'running' : preview.state === 'offline' ? 'offline' : ''}">${escapeHtml(preview.state || 'stopped')}</span>
          </div>
          <div class="card-body">
            <dl class="kv">
              <dt>端口</dt><dd class="mono">${preview.port || '—'}</dd>
              <dt>PID</dt><dd class="mono">${preview.pid || '—'}</dd>
              <dt>glados-home</dt><dd class="mono small">${escapeHtml(preview.home || '')}</dd>
              <dt>错误</dt><dd>${escapeHtml(preview.error || '—')}</dd>
            </dl>
            <div class="hr"></div>
            <div class="row" style="flex-wrap:wrap;gap:6px">
              <button class="btn btn-sm" id="btn-p-start">启动</button>
              <button class="btn btn-sm" id="btn-p-stop">停止</button>
              <button class="btn btn-sm" id="btn-p-reload">reload</button>
              <button class="btn btn-sm" id="btn-p-pause">暂停</button>
              <button class="btn btn-sm" id="btn-p-resume">继续</button>
              <button class="btn btn-sm" id="btn-p-idle">回自主行为</button>
              <button class="btn btn-sm" id="btn-p-one">只留一个角色</button>
            </div>
          </div>
        </div>
      </div>

      <div style="display:grid;gap:12px">
        <div class="card">
          <div class="card-head"><h3>/status</h3><div class="spacer"></div>
            <button class="btn btn-sm" id="btn-refresh-status">刷新</button></div>
          <div class="card-body">
            <dl class="kv">
              <dt>role</dt><dd>${escapeHtml(status.role || '—')}</dd>
              <dt>count</dt><dd>${status.count === undefined ? '—' : status.count}</dd>
              <dt>paused</dt><dd>${status.paused === undefined ? '—' : String(status.paused)}</dd>
              <dt>behavior</dt><dd class="mono">${escapeHtml(status.behavior || '—')}</dd>
              <dt>managed</dt><dd>${status.managed === undefined ? '—' : String(status.managed)}</dd>
              <dt>mascots</dt><dd class="small mono">${mascotsText(status.mascots)}</dd>
            </dl>
          </div>
        </div>

        <div class="card">
          <div class="card-head"><h3>当前预览目标</h3></div>
          <div class="card-body">
            <div class="field"><label>行为名（会被 setBehavior）</label>
              <input type="text" id="pv-behavior" value="${escapeHtml(ctx.state.selectedBehavior || '')}" placeholder="例如 MyDance" /></div>
            <button class="btn btn-sm btn-primary" id="btn-save-target">设为当前目标</button>
            <p class="small muted" style="margin:8px 0 0">AIIdle 是特殊值：回到自主行为树，不作为动作预览。</p>
          </div>
        </div>

        <div class="card">
          <div class="card-head"><h3>托管预览差异</h3></div>
          <div class="card-body small muted">
            <p style="margin-top:0">预览进程以 <span class="mono">--glados-managed</span> 启动，因此：</p>
            <ul style="margin:0;padding-left:16px">
              <li>无托盘、不自检更新</li>
              <li><span class="mono">Breeding=false</span>：增殖动作不会真的产生新角色</li>
              <li>自主树硬过滤 SplitIntoTwo / PullUpShimeji</li>
              <li>AI 行为表注入最小集（仅 AIIdle），标注为「托管注入，非皮肤资产」</li>
            </ul>
          </div>
        </div>
      </div>
    </div>`;

  container.querySelector('#btn-run-now').addEventListener('click', () => document.getElementById('btn-run').click());
  container.querySelector('#btn-p-start').addEventListener('click', async () => {
    try { ctx.state.preview = await api.post('/api/preview/start', {}); render(container, ctx); } catch (e) { toast(e.message, 'error'); }
  });
  container.querySelector('#btn-p-stop').addEventListener('click', async () => {
    try { ctx.state.preview = await api.post('/api/preview/stop', {}); toast('预览已停止', 'ok'); render(container, ctx); } catch (e) { toast(e.message, 'error'); }
  });
  container.querySelector('#btn-p-reload').addEventListener('click', () => command(container, ctx, 'reload'));
  container.querySelector('#btn-p-pause').addEventListener('click', () => command(container, ctx, 'pause'));
  container.querySelector('#btn-p-resume').addEventListener('click', () => command(container, ctx, 'resume'));
  container.querySelector('#btn-p-idle').addEventListener('click', () => command(container, ctx, 'setBehavior', { behavior: 'AIIdle' }));
  container.querySelector('#btn-p-one').addEventListener('click', () => command(container, ctx, 'remainOne', {}));
  container.querySelector('#btn-refresh-status').addEventListener('click', async () => {
    try {
      ctx.state.preview = await api.get('/api/preview/status');
      render(container, ctx);
    } catch (e) { toast(e.message, 'error'); }
  });
  container.querySelector('#btn-save-target').addEventListener('click', () => {
    ctx.setBehaviorTarget(container.querySelector('#pv-behavior').value.trim());
    toast('预览目标已设为：' + (ctx.state.selectedBehavior || '（仅 reload）'), 'ok');
    render(container, ctx);
  });
}

async function command(container, ctx, cmd, extra) {
  try {
    await api.post('/api/preview/command', { cmd, ...(extra || {}) });
    toast(cmd + ' 已执行', 'ok');
    ctx.state.preview = await api.get('/api/preview/status');
    render(container, ctx);
  } catch (e) {
    toast(cmd + ' 失败：' + e.message, 'error');
  }
}

function stepsHtml(run) {
  return '<div class="steps">' + run.steps.map(s => `
    <div class="step ${s.ok ? 'ok' : 'fail'}">
      <div class="dot">${s.ok ? '✓' : '!'}</div>
      <div class="nm">${escapeHtml(s.step)}</div>
      <div class="detail">${escapeHtml(s.error || s.note || '')}</div>
    </div>`).join('') + '</div>';
}

function mascotsText(mascots) {
  if (!Array.isArray(mascots) || !mascots.length) return '—';
  return mascots.map(m => `(${m.x},${m.y}) ${m.width}×${m.height}`).join('<br>');
}

export async function save() {
  // 预览面板没有需要落盘的内容
}
