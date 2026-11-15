import { api, qs, toast, escapeHtml, confirmDialog, ApiError } from '../api.js';
import { parseXml, buildFragment, el, attr, setAttr, findFirst, newBehavior } from '../fragment.js';

let editor = null; // { file, root, sel, dirty, generatedBy }
let mode = 'table'; // table | graph | edit

export function render(container, ctx) {
  const state = ctx.state;
  const manifest = state.manifest || { behaviors: [] };
  const editable = manifest.behaviors.filter(b => b.editable);
  const wrap = document.createElement('div');
  wrap.innerHTML = `
    <div class="card">
      <div class="card-head">
        <h3>行为（Behavior）</h3>
        <span class="small muted">共 ${manifest.behaviors.length} 个 · 设计器可编辑 ${editable.length} 个</span>
        <div class="spacer"></div>
        <button class="btn btn-sm ${mode === 'table' ? 'btn-primary' : ''}" data-mode="table">表格</button>
        <button class="btn btn-sm ${mode === 'graph' ? 'btn-primary' : ''}" data-mode="graph">状态图</button>
        <button class="btn btn-sm btn-primary" id="btn-new-behavior">新建行为</button>
      </div>
    </div>
    <div id="behavior-host"></div>`;
  container.appendChild(wrap);

  wrap.querySelectorAll('[data-mode]').forEach(b => b.addEventListener('click', () => {
    mode = b.dataset.mode;
    render(container, ctx);
  }));
  wrap.querySelector('#btn-new-behavior').addEventListener('click', () => newBehaviorDialog(container, ctx));

  const host = wrap.querySelector('#behavior-host');
  if (mode === 'graph') renderGraph(host, manifest, ctx);
  else if (mode === 'edit' && editor) renderEditor(host, ctx);
  else renderTable(host, manifest, ctx);
}

/* ---------------- 表格 ---------------- */

function renderTable(host, manifest, ctx) {
  const rows = manifest.behaviors.map(b => `
    <tr data-behavior="${escapeHtml(b.name)}">
      <td><span class="mono">${escapeHtml(b.name)}</span></td>
      <td>${escapeHtml(b.action)}</td>
      <td class="num">${b.frequency}</td>
      <td>${b.hidden ? '<span class="tag">隐藏</span>' : '<span class="tag ok">菜单可见</span>'}</td>
      <td class="small">${b.next && b.next.refs.length ? (b.next.refs.length + ' 条' + (b.next.add ? '（叠加）' : '（限定）')) : '—'}</td>
      <td class="small mono" title="${escapeHtml(b.source)}">${escapeHtml(shortSource(b.source))}</td>
      <td>${b.editable ? '<button class="btn btn-sm" data-edit="' + escapeHtml(b.fragment) + '">编辑</button>' : '<span class="tag">只读</span>'}</td>
    </tr>`).join('');

  host.innerHTML = `
    <div class="card">
      <div class="card-body tight scroll">
        <table class="tbl"><thead><tr>
          <th>Name</th><th>Action</th><th class="num">Frequency</th><th>Hidden</th><th>后继列表</th><th>来源</th><th></th>
        </tr></thead><tbody>${rows}</tbody></table>
      </div>
    </div>
    <div class="card">
      <div class="card-body">
        <p class="small muted" style="margin:0">
          Frequency=0 的行为不进自主行为树（<span class="mono">isEffective()</span> 直接 false），
          只能被 <span class="mono">setBehavior</span> 按名预览；Hidden=true 不出现在原生托盘菜单。
          新建预览行为默认两者都开，保证不改变原生行为分布。
        </p>
      </div>
    </div>`;

  host.querySelectorAll('[data-edit]').forEach(btn => btn.addEventListener('click', () => loadFragment(btn.dataset.edit, ctx)));
}

/* ---------------- 状态图 ---------------- */

function renderGraph(host, manifest, ctx) {
  const nodes = manifest.behaviors.filter(b => b.next && b.next.refs.length);
  const byName = new Map(manifest.behaviors.map(b => [b.name, b]));
  if (!nodes.length) {
    host.innerHTML = '<div class="card"><div class="card-body"><div class="empty"><strong>没有后继行为列表</strong>当前配置里没有 NextBehaviorList 声明。</div></div></div>';
    return;
  }
  const radius = 210;
  const cx = 340;
  const cy = 250;
  const positions = new Map();
  nodes.forEach((b, i) => {
    const angle = (Math.PI * 2 * i) / nodes.length - Math.PI / 2;
    positions.set(b.name, { x: cx + radius * Math.cos(angle), y: cy + radius * Math.sin(angle) });
  });

  const edges = [];
  nodes.forEach(b => {
    (b.next.refs || []).forEach(ref => {
      const from = positions.get(b.name);
      const to = positions.get(ref.name);
      if (!to) return;
      const stroke = b.next.add ? '#2f6feb' : '#9a6700';
      const dash = b.next.add ? '' : ' stroke-dasharray="5 4"';
      const mx = (from.x + to.x) / 2;
      const my = (from.y + to.y) / 2;
      edges.push(`<path d="M${from.x.toFixed(1)},${from.y.toFixed(1)} Q${mx.toFixed(1)},${(my - 25).toFixed(1)} ${to.x.toFixed(1)},${to.y.toFixed(1)}"
        fill="none" stroke="${stroke}" stroke-width="1.4"${dash} marker-end="url(#arrow)" />
        <text x="${mx.toFixed(1)}" y="${(my - 28).toFixed(1)}" font-size="10" fill="#667085" text-anchor="middle">${ref.frequency}</text>`);
    });
  });

  const nodeEls = nodes.map(b => {
    const p = positions.get(b.name);
    const label = b.name.length > 16 ? b.name.slice(0, 15) + '…' : b.name;
    return `<g><circle cx="${p.x.toFixed(1)}" cy="${p.y.toFixed(1)}" r="34" fill="#fff" stroke="#cfd4da" stroke-width="1.4" />
      <text x="${p.x.toFixed(1)}" y="${(p.y + 3).toFixed(1)}" font-size="10.5" text-anchor="middle" fill="#1f2328">${escapeHtml(label)}</text>
      <text x="${p.x.toFixed(1)}" y="${(p.y + 46).toFixed(1)}" font-size="9.5" text-anchor="middle" fill="#8a94a6">F=${b.frequency}</text></g>`;
  }).join('');

  host.innerHTML = `
    <div class="grid-2" style="grid-template-columns:minmax(0,1fr) 300px">
      <div class="card"><div class="card-body" style="overflow:auto">
        <svg viewBox="0 0 680 510" width="100%" style="max-height:calc(100vh - 240px)">
          <defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#8a94a6" /></marker></defs>
          ${edges.join('')}
          ${nodeEls}
        </svg>
      </div></div>
      <div class="card">
        <div class="card-head"><h3>图例</h3></div>
        <div class="card-body small">
          <p><span style="display:inline-block;width:26px;border-top:2px solid #2f6feb"></span> Add=true：叠加候选（全局池 + 后继）</p>
          <p><span style="display:inline-block;width:26px;border-top:2px dashed #9a6700"></span> Add=false：限定候选集（清空全局池）</p>
          <p class="muted">边上的数字是 BehaviorReference 的 Frequency，影响加权随机的权重。</p>
          <p class="muted">D-1 修复后 <span class="mono">NextBehaviorList</span> 才真正生效；导入旧 <span class="mono">&lt;NextBehavior&gt;</span> 会在校验中心提示改名。</p>
        </div>
      </div>
    </div>`;
}

/* ---------------- 编辑器 ---------------- */

async function loadFragment(file, ctx) {
  try {
    const data = await api.get('/api/fragments/behaviors/' + encodeURIComponent(file));
    editor = { file, root: parseXml(data.xml), sel: 0, dirty: false, generatedBy: data.generatedBy };
    mode = 'edit';
    render(document.querySelector('#panel-behavior'), ctx);
  } catch (e) {
    toast('读取失败：' + e.message, 'error');
  }
}

export function focusFragment(ctx, file) {
  loadFragment(file, ctx);
}

function behaviorsOf(root) {
  const list = findFirst(root, 'BehaviorList');
  return list ? list.children.filter(c => c.tag === 'Behavior') : [];
}

function renderEditor(host, ctx) {
  const behaviors = behaviorsOf(editor.root);
  if (!behaviors[editor.sel]) editor.sel = 0;
  const behavior = behaviors[editor.sel];
  const actionOptions = ((ctx.state.manifest && ctx.state.manifest.actions) || []).map(a => a.name);
  const behaviorOptions = ((ctx.state.manifest && ctx.state.manifest.behaviors) || []).map(b => b.name);

  host.innerHTML = `
    <div class="card">
      <div class="card-head">
        <h3>${escapeHtml(editor.file)}</h3>
        <span class="tag ${editor.generatedBy ? 'studio' : 'warn'}">${editor.generatedBy ? '设计器文件' : '手写文件'}</span>
        <div class="spacer"></div>
        <button class="btn btn-sm" id="btn-back">返回列表</button>
        <button class="btn btn-sm btn-primary" id="btn-save-behavior">保存</button>
        <button class="btn btn-sm btn-danger" id="btn-del-behavior">删除片段</button>
      </div>
      <div class="card-body">
        <div class="row" style="gap:6px;flex-wrap:wrap;margin-bottom:12px">
          ${behaviors.map((b, i) => `<button class="btn btn-sm ${i === editor.sel ? 'btn-primary' : ''}" data-btab="${i}">${escapeHtml(attr(b, 'Name', '(未命名)'))}</button>`).join('')}
          <button class="btn btn-sm" id="btn-add-behavior">+ 行为</button>
        </div>
        ${behavior ? `
        <div class="grid-2" style="grid-template-columns:minmax(0,1fr) 320px">
          <div>
            <div class="grid-2" style="grid-template-columns:1fr 1fr; gap:0 12px">
              <div class="field"><label>Name</label><input type="text" id="b-name" value="${escapeHtml(attr(behavior, 'Name'))}" /></div>
              <div class="field"><label>Action（留空 = 同名）</label><input type="text" id="b-action" list="action-names" value="${escapeHtml(attr(behavior, 'Action'))}" /></div>
              <div class="field"><label>Frequency</label><input type="number" id="b-frequency" value="${escapeHtml(attr(behavior, 'Frequency', '0'))}" /></div>
              <div class="field"><label>Hidden</label>
                <select id="b-hidden">
                  <option value="true" ${attr(behavior, 'Hidden') === 'true' ? 'selected' : ''}>true（不进托盘菜单）</option>
                  <option value="false" ${attr(behavior, 'Hidden') !== 'true' ? 'selected' : ''}>false</option>
                </select></div>
            </div>
            <div class="field"><label>Condition（可选表达式）</label><input type="text" id="b-condition" value="${escapeHtml(attr(behavior, 'Condition'))}" /></div>
            <div class="hr"></div>
            <div class="row"><h3 style="margin:0;font-size:12.5px">后继行为列表</h3><div class="spacer"></div>
              <label class="inline-check"><input type="checkbox" id="b-next-add" ${nextAdditive(behavior) ? 'checked' : ''} /> Add=true（叠加候选）</label>
              <button class="btn btn-sm" id="btn-add-ref">+ 引用</button></div>
            <table class="tbl" style="margin-top:8px"><thead><tr>
              <th>Name</th><th style="width:100px">Frequency</th><th style="width:90px">Action 覆盖</th><th style="width:60px"></th>
            </tr></thead><tbody>${refRows(behavior, behaviorOptions)}</tbody></table>
            <p class="small muted">Add=false 会清空全局候选池，因此后继列表不能为空，否则角色会被传送回屏幕顶端（R4）。</p>
          </div>
          <div class="card" style="box-shadow:none">
            <div class="card-head"><h3>预览这个行为</h3></div>
            <div class="card-body">
              <p class="small muted">预览走 <span class="mono">setBehavior</span>；Frequency=0 的行为也能按名构建。</p>
              <button class="btn btn-sm" id="btn-preview-behavior" style="width:100%">在桌面预览</button>
              <div class="hr"></div>
              <p class="small muted" style="margin:0">托管预览差异：不显示托盘、不自主增殖、AI 行为表被最小化注入。</p>
            </div>
          </div>
        </div>` : '<div class="empty">片段里还没有行为</div>'}
      </div>
    </div>
    <datalist id="action-names">${actionOptions.map(n => '<option value="' + escapeHtml(n) + '"></option>').join('')}</datalist>
    <datalist id="behavior-names">${behaviorOptions.map(n => '<option value="' + escapeHtml(n) + '"></option>').join('')}</datalist>`;

  host.querySelector('#btn-back').addEventListener('click', () => { mode = 'table'; render(document.querySelector('#panel-behavior'), ctx); });
  host.querySelectorAll('[data-btab]').forEach(b => b.addEventListener('click', () => {
    editor.sel = Number(b.dataset.btab);
    renderEditor(host, ctx);
  }));
  host.querySelector('#btn-add-behavior').addEventListener('click', () => {
    const list = findFirst(editor.root, 'BehaviorList');
    list.children.push(newBehavior('NewBehavior' + (list.children.length + 1), null, 0, true));
    editor.sel = list.children.length - 1;
    editor.dirty = true;
    renderEditor(host, ctx);
  });
  if (behavior) {
    bindField(host, '#b-name', behavior, 'Name');
    bindField(host, '#b-action', behavior, 'Action');
    bindField(host, '#b-frequency', behavior, 'Frequency');
    bindField(host, '#b-condition', behavior, 'Condition');
    host.querySelector('#b-hidden').addEventListener('change', (e) => setAttr(behavior, 'Hidden', e.target.value));
    host.querySelector('#b-next-add').addEventListener('change', (e) => {
      const list = ensureNext(behavior);
      setAttr(list, 'Add', e.target.checked ? 'true' : 'false');
      editor.dirty = true;
    });
    host.querySelector('#btn-add-ref').addEventListener('click', () => {
      const list = ensureNext(behavior);
      list.children.push(el('BehaviorReference', { Name: attr(behavior, 'Name'), Frequency: '1' }));
      editor.dirty = true;
      renderEditor(host, ctx);
    });
    host.querySelectorAll('[data-ref-name]').forEach(input => input.addEventListener('input', () => {
      const list = ensureNext(behavior);
      setAttr(list.children[Number(input.dataset.refName)], 'Name', input.value.trim());
      editor.dirty = true;
    }));
    host.querySelectorAll('[data-ref-freq]').forEach(input => input.addEventListener('input', () => {
      const list = ensureNext(behavior);
      setAttr(list.children[Number(input.dataset.refFreq)], 'Frequency', input.value.trim());
      editor.dirty = true;
    }));
    host.querySelectorAll('[data-ref-action]').forEach(input => input.addEventListener('input', () => {
      const list = ensureNext(behavior);
      setAttr(list.children[Number(input.dataset.refAction)], 'Action', input.value.trim());
    }));
    host.querySelectorAll('[data-ref-del]').forEach(btn => btn.addEventListener('click', () => {
      const list = ensureNext(behavior);
      list.children.splice(Number(btn.dataset.refDel), 1);
      editor.dirty = true;
      renderEditor(host, ctx);
    }));
    host.querySelector('#btn-preview-behavior').addEventListener('click', async () => {
      await api.put('/api/fragments/behaviors/' + encodeURIComponent(editor.file), {
        skin: ctx.state.skin, xml: buildXml(), force: !!editor.forceOverwrite,
      }).then(() => {
        ctx.setBehaviorTarget(attr(behavior, 'Name'));
        document.getElementById('btn-run').click();
      }).catch(async (e) => {
        if (e.status === 409 && !editor.forceOverwrite) {
          const ok = await confirmDialog('覆盖确认', '<p class="small">该文件不是设计器生成的，覆盖前会留 .bak。</p>');
          if (ok) {
            editor.forceOverwrite = true;
            host.querySelector('#btn-preview-behavior').click();
          }
          return;
        }
        toast(e.message, 'error');
      });
    });
  }
  host.querySelector('#btn-save-behavior').addEventListener('click', () => save(ctx));
  host.querySelector('#btn-del-behavior').addEventListener('click', async () => {
    if (!await confirmDialog('删除片段', '<p class="small">将删除 ' + escapeHtml(editor.file) + '</p>')) return;
    try {
      await api.del('/api/fragments/behaviors/' + encodeURIComponent(editor.file));
      editor = null;
      mode = 'table';
      await ctx.refresh();
    } catch (e) {
      toast('删除失败：' + e.message, 'error');
    }
  });
}

function refRows(behavior, options) {
  const list = nextList(behavior);
  if (!list || !list.children.length) {
    return '<tr><td colspan="4" class="empty small">没有后继引用</td></tr>';
  }
  return list.children.map((ref, i) => `
    <tr>
      <td><input type="text" data-ref-name="${i}" list="behavior-names" value="${escapeHtml(attr(ref, 'Name'))}" /></td>
      <td><input type="text" data-ref-freq="${i}" value="${escapeHtml(attr(ref, 'Frequency', '1'))}" /></td>
      <td><input type="text" data-ref-action="${i}" list="action-names" value="${escapeHtml(attr(ref, 'Action'))}" /></td>
      <td><button class="btn btn-sm btn-danger" data-ref-del="${i}">×</button></td>
    </tr>`).join('');
}

function bindField(host, selector, node, name) {
  const input = host.querySelector(selector);
  if (!input) return;
  input.addEventListener('input', () => {
    setAttr(node, name, input.value.trim());
    editor.dirty = true;
  });
}

function nextList(behavior) {
  return behavior.children.find(c => c.tag === 'NextBehaviorList' || c.tag === 'NextBehavior') || null;
}

function nextAdditive(behavior) {
  const list = nextList(behavior);
  if (!list) return true;
  const add = attr(list, 'Add');
  return add === '' || add === 'true';
}

function ensureNext(behavior) {
  let list = nextList(behavior);
  if (!list) {
    list = el('NextBehaviorList', { Add: 'false' }, []);
    behavior.children.push(list);
  }
  return list;
}

function buildXml() {
  const list = findFirst(editor.root, 'BehaviorList');
  return buildFragment('behaviors', list ? list.children : []);
}

export async function save(ctx, opts = {}) {
  if (!editor) {
    if (!opts.silent) toast('没有正在编辑的片段', 'error');
    return false;
  }
  try {
    await api.put('/api/fragments/behaviors/' + encodeURIComponent(editor.file), {
      skin: ctx.state.skin, xml: buildXml(), force: !!editor.forceOverwrite,
    });
    editor.dirty = false;
    editor.generatedBy = true;
    if (!opts.silent) toast('已保存 ' + editor.file, 'ok');
    await ctx.refresh();
    return true;
  } catch (e) {
    if (e instanceof ApiError && e.diagnostics.length) {
      ctx.state.lastValidation = { diagnostics: e.diagnostics, summary: e.payload && e.payload.summary };
      if (!opts.silent) {
        toast('校验未通过：' + e.diagnostics.filter(d => d.severity === 'error').length + ' 个错误', 'error');
        ctx.goto('validate');
      } else {
        throw e;
      }
    } else {
      toast('保存失败：' + e.message, 'error');
    }
    return false;
  }
}

/* ---------------- 新建行为 ---------------- */

function newBehaviorDialog(container, ctx) {
  const actions = ((ctx.state.manifest && ctx.state.manifest.actions) || []).filter(a => a.type !== 'Embedded' || true);
  const { modal, close } = ctx.openModal('新建行为',
    `<div class="field"><label>行为名（= 动作名，若要显式绑定请填 Action）</label><input type="text" id="nb-name" list="nb-actions" placeholder="MyPack.Wave" /></div>
     <datalist id="nb-actions">${actions.map(a => '<option value="' + escapeHtml(a.name) + '"></option>').join('')}</datalist>
     <div class="field"><label>绑定动作 Action（留空 = 同名）</label><input type="text" id="nb-action" list="nb-actions" /></div>
     <div class="row" style="gap:14px">
       <div class="field" style="flex:1"><label>Frequency</label><input type="number" id="nb-freq" value="0" /></div>
       <div class="field" style="flex:1"><label>Hidden</label><select id="nb-hidden"><option value="true">true</option><option value="false">false</option></select></div>
     </div>
     <p class="small muted">预览用行为建议 Frequency=0 Hidden=true：不进自主行为树、不进托盘菜单，只能 setBehavior 预览。</p>`,
    '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-create>创建</button>');
  modal.querySelector('[data-create]').addEventListener('click', async () => {
    const name = modal.querySelector('#nb-name').value.trim();
    const action = modal.querySelector('#nb-action').value.trim();
    const freq = Number(modal.querySelector('#nb-freq').value) || 0;
    const hidden = modal.querySelector('#nb-hidden').value === 'true';
    if (!name) { toast('请填写行为名', 'error'); return; }
    try {
      const slug = name.replace(/[^A-Za-z0-9._-]/g, '-').toLowerCase();
      const suggest = await api.get('/api/fragments/suggest' + qs({ kind: 'behaviors', slug, order: 50 }));
      const node = el('Behavior', { Name: name, Action: action || undefined, Frequency: String(freq), Hidden: String(hidden) });
      const result = await api.put('/api/fragments/behaviors/' + encodeURIComponent(suggest.file), {
        skin: ctx.state.skin, xml: buildFragment('behaviors', [node]),
      });
      close();
      ctx.setBehaviorTarget(name);
      toast('行为已创建：' + suggest.file, 'ok');
      await ctx.refresh();
    } catch (e) {
      const msg = e instanceof ApiError && e.diagnostics.length
        ? e.diagnostics.map(d => d.message).slice(0, 3).join('；') : e.message;
      toast('创建失败：' + msg, 'error');
    }
  });
}

function shortSource(source) {
  if (!source) return '';
  const parts = String(source).split('/');
  return parts.slice(-2).join('/');
}
