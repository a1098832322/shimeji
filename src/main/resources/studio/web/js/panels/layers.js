import { escapeHtml, toast } from '../api.js';

export function render(container, ctx) {
  const manifest = ctx.state.manifest;
  if (!manifest) {
    container.innerHTML = '<div class="card"><div class="card-body"><div class="empty">还没有清单数据</div></div></div>';
    return;
  }
  const layers = manifest.layers || [];

  container.innerHTML = `
    <div class="card">
      <div class="card-head">
        <h3>加载顺序（图层）</h3>
        <span class="small muted">全局基础 → 皮肤增量 → 托管行为表；同名按后写覆盖</span>
      </div>
      <div class="card-body tight">
        ${layers.map((l, i) => `
          <div class="list-item" style="cursor:default">
            <span class="tag">${i + 1}</span>
            <span class="mono small" style="flex:1;overflow:hidden;text-overflow:ellipsis">${escapeHtml(l.file)}</span>
            <span class="tag ${l.role === 'skin' ? 'warn' : ''}">${l.role === 'skin' ? '皮肤增量' : '全局'}</span>
            ${l.generatedBy ? '<span class="tag studio">设计器</span>' : '<span class="tag">只读</span>'}
            ${l.error ? '<span class="tag err" title="' + escapeHtml(l.error) + '">解析失败</span>' : ''}
          </div>`).join('')}
      </div>
    </div>

    <div style="display:grid;gap:12px">
      <div class="card">
        <div class="card-head"><h3>动作生效链</h3><div class="spacer"></div>
          <input type="text" id="layer-filter-a" placeholder="过滤" style="width:160px" /></div>
        <div class="card-body tight scroll" style="max-height:calc(100vh - 300px)">
          <table class="tbl"><thead><tr><th>Name</th><th>Type</th><th>生效来源</th><th>被覆盖的定义</th></tr></thead>
            <tbody id="layer-actions"></tbody></table>
        </div>
      </div>
      <div class="card">
        <div class="card-head"><h3>行为生效链</h3><div class="spacer"></div>
          <input type="text" id="layer-filter-b" placeholder="过滤" style="width:160px" /></div>
        <div class="card-body tight scroll" style="max-height:calc(100vh - 300px)">
          <table class="tbl"><thead><tr><th>Name</th><th>F</th><th>生效来源</th><th>被覆盖的定义</th></tr></thead>
            <tbody id="layer-behaviors"></tbody></table>
        </div>
      </div>
    </div>`;

  const paintActions = (kw) => {
    const rows = (manifest.actions || [])
      .filter(a => !kw || a.name.toLowerCase().includes(kw))
      .map(a => `<tr>
        <td class="mono">${escapeHtml(a.name)}</td>
        <td>${escapeHtml(a.type)}${a.poses ? ' <span class="small muted">' + a.poses + '帧</span>' : ''}</td>
        <td class="small mono" title="${escapeHtml(a.source)}">${escapeHtml(short(a.source))}${a.editable ? ' <span class="tag studio">可编辑</span>' : ''}</td>
        <td class="small muted">${(a.overriddenBy || []).map(short).map(escapeHtml).join('<br>') || '—'}</td>
      </tr>`).join('');
    container.querySelector('#layer-actions').innerHTML = rows || '<tr><td colspan="4" class="empty small">无匹配</td></tr>';
  };
  const paintBehaviors = (kw) => {
    const rows = (manifest.behaviors || [])
      .filter(b => !kw || b.name.toLowerCase().includes(kw))
      .map(b => `<tr>
        <td class="mono">${escapeHtml(b.name)}</td>
        <td class="num">${b.frequency}</td>
        <td class="small mono" title="${escapeHtml(b.source)}">${escapeHtml(short(b.source))}${b.editable ? ' <span class="tag studio">可编辑</span>' : ''}</td>
        <td class="small muted">${(b.overriddenBy || []).map(short).map(escapeHtml).join('<br>') || '—'}</td>
      </tr>`).join('');
    container.querySelector('#layer-behaviors').innerHTML = rows || '<tr><td colspan="4" class="empty small">无匹配</td></tr>';
  };
  paintActions('');
  paintBehaviors('');
  container.querySelector('#layer-filter-a').addEventListener('input', (e) => paintActions(e.target.value.trim().toLowerCase()));
  container.querySelector('#layer-filter-b').addEventListener('input', (e) => paintBehaviors(e.target.value.trim().toLowerCase()));
}

function short(source) {
  if (!source) return '';
  const parts = String(source).split('/');
  return parts.slice(-2).join('/');
}
