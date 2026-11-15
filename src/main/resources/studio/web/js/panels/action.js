import { api, qs, toast, escapeHtml, confirmDialog, ApiError } from '../api.js';
import { parseXml, buildFragment, toXml, el, attr, setAttr, findFirst, findAll, newAction, newPose, isExpression, expressionKind } from '../fragment.js';

let editor = null;       // { kind, file, root, sel, dirty, raw, xmlText }
let rawMode = false;
let filter = '';
let playing = false;
let playTimer = null;

export function render(container, ctx) {
  const state = ctx.state;
  const wrap = document.createElement('div');
  wrap.innerHTML = `
    <div class="grid-side">
      <div class="card">
        <div class="card-head"><h3>资源</h3><div class="spacer"></div>
          <button class="btn btn-sm btn-primary" id="btn-new-fragment">新建动作</button></div>
        <div class="card-body" style="padding:8px 10px">
          <input type="text" id="fragment-filter" placeholder="搜索动作名 / 文件" value="${escapeHtml(filter)}" />
        </div>
        <div class="scroll" id="fragment-list" style="max-height:calc(100vh - 260px)"></div>
      </div>
      <div style="display:grid; gap:12px; min-width:0" id="editor-host"></div>
    </div>`;
  container.appendChild(wrap);

  wrap.querySelector('#fragment-filter').addEventListener('input', (e) => {
    filter = e.target.value.trim().toLowerCase();
    renderList(wrap, ctx);
  });
  wrap.querySelector('#btn-new-fragment').addEventListener('click', () => newFragmentDialog(wrap, ctx));

  renderList(wrap, ctx);
  renderEditor(wrap, ctx);

  // 深链：#action/50-my-dance.xml 直接打开某个片段（便于分享与排查）
  const hash = (location.hash || '').replace('#', '');
  const file = hash.includes('/') ? decodeURIComponent(hash.split('/')[1]) : '';
  if (file && (!editor || editor.file !== file)) {
    loadFragment(file, ctx, wrap);
  }
}

/* ------------------------------------------------------------------ */
/* 左侧：片段 + 基础动作                                                */
/* ------------------------------------------------------------------ */

function renderList(root, ctx) {
  const host = root.querySelector('#fragment-list');
  const state = ctx.state;
  const items = [];
  const frags = (state.fragments.actions || []).filter(f => !f.disabled);
  for (const frag of frags) {
    const names = (frag.entries || []).join('、') || '（空）';
    if (filter && !(frag.file.toLowerCase().includes(filter) || names.toLowerCase().includes(filter))) continue;
    items.push(`
      <div class="list-item ${editor && editor.file === frag.file ? 'active' : ''}" data-file="${escapeHtml(frag.file)}">
        <div style="min-width:0; flex:1">
          <div class="name">${escapeHtml(frag.file)}</div>
          <div class="sub">${escapeHtml(names)}${frag.generatedBy ? '' : ' · 手写'}</div>
        </div>
        <span class="tag ${frag.generatedBy ? 'studio' : ''}">${frag.order < 999 ? String(frag.order).padStart(2, '0') : '—'}</span>
      </div>`);
  }

  const baseline = ((state.manifest && state.manifest.actions) || []).filter(a => !a.editable);
  const filteredBaseline = baseline.filter(a => !filter || a.name.toLowerCase().includes(filter));
  const grouped = [];
  grouped.push('<div class="list-item" style="cursor:default;background:var(--surface-2)"><span class="small muted">设计器片段（' + frags.length + '）</span></div>');
  grouped.push(items.join('') || '<div class="empty small">还没有片段</div>');
  grouped.push('<div class="list-item" style="cursor:default;background:var(--surface-2)"><span class="small muted">基础动作（只读 ' + baseline.length + '）</span></div>');
  if (filteredBaseline.length) {
    grouped.push(filteredBaseline.map(a => `
      <div class="list-item" data-baseline="${escapeHtml(a.name)}" title="${escapeHtml(a.source)}">
        <div style="min-width:0; flex:1"><div class="name">${escapeHtml(a.name)}</div>
          <div class="sub">${escapeHtml(a.type)}${a.poses ? ' · ' + a.poses + ' 帧' : ''}</div></div>
        <span class="tag">只读</span>
      </div>`).join(''));
  } else {
    grouped.push('<div class="empty small">无匹配项</div>');
  }
  host.innerHTML = grouped.join('');

  host.querySelectorAll('[data-file]').forEach(item => {
    item.addEventListener('click', () => loadFragment(item.dataset.file, ctx, root));
  });
  host.querySelectorAll('[data-baseline]').forEach(item => {
    item.addEventListener('click', () => showBaseline(item.dataset.baseline, ctx));
  });
}

async function loadFragment(file, ctx, root) {
  try {
    const data = await api.get('/api/fragments/actions/' + encodeURIComponent(file));
    editor = {
      kind: 'actions', file, xmlText: data.xml, raw: false, sel: 0, dirty: false,
      root: parseXml(data.xml),
      generatedBy: data.generatedBy,
    };
    rawMode = false;
    renderList(root, ctx);
    renderEditor(root, ctx);
  } catch (e) {
    toast('读取片段失败：' + e.message, 'error');
  }
}

export function focusFragment(ctx, file, actionName) {
  const root = document.querySelector('#panel-action');
  if (!root) return;
  loadFragment(file, ctx, root).then(() => {
    if (actionName && editor) {
      const actions = currentActions();
      const idx = actions.findIndex(a => attr(a, 'Name') === actionName);
      if (idx >= 0) editor.sel = idx;
      renderEditor(root, ctx);
    }
  });
}

function showBaseline(name, ctx) {
  const item = ((ctx.state.manifest && ctx.state.manifest.actions) || []).find(a => a.name === name);
  if (!item) return;
  const behaviors = ((ctx.state.manifest && ctx.state.manifest.behaviors) || []).filter(b => b.action === name);
  ctx.openModal('基础动作 · ' + name,
    `<dl class="kv">
      <dt>类型</dt><dd>${escapeHtml(item.type)}</dd>
      <dt>边界</dt><dd>${escapeHtml(item.borderType || '—')}</dd>
      <dt>帧数</dt><dd>${item.poses}</dd>
      <dt>来源</dt><dd class="mono small">${escapeHtml(item.source)}</dd>
      <dt>引用它的行为</dt><dd>${behaviors.length ? behaviors.map(b => escapeHtml(b.name)).join('、') : '—'}</dd>
     </dl>
     <p class="small muted">基础动作全量只读保留（P4）：要改行为请新建扩展开关动作并以包前缀命名，避免 FR-02 静默覆盖。</p>`);
}

/* ------------------------------------------------------------------ */
/* 新建片段                                                            */
/* ------------------------------------------------------------------ */

function newFragmentDialog(root, ctx) {
  const actions = ((ctx.state.manifest && ctx.state.manifest.actions) || []).slice(0, 200);
  const { modal, close } = ctx.openModal('新建动作片段',
    `<div class="grid-2" style="grid-template-columns:1fr 1fr;gap:0 12px">
       <div class="field"><label>动作名（建议加包前缀）</label><input type="text" id="nf-name" placeholder="MyPack.Wave" /></div>
       <div class="field"><label>类型</label><select id="nf-type">
         <option value="Sequence">Sequence（组合已有动作）</option>
         <option value="Stay">Stay（原地播帧）</option>
         <option value="Move">Move（按速度位移）</option>
         <option value="Animate">Animate</option>
         <option value="Select">Select（条件选择）</option>
       </select></div>
       <div class="field"><label>文件名序号（10–40 手写/设计器，50–89 扩展包）</label><input type="number" id="nf-order" value="50" min="10" max="99" /></div>
       <div class="field"><label>参考基础动作（仅 Sequence 用）</label><input type="text" id="nf-ref" list="action-names" placeholder="Stand" /></div>
     </div>
     <datalist id="action-names">${actions.map(a => '<option value="' + escapeHtml(a.name) + '"></option>').join('')}</datalist>
     <p class="small muted">新动作会写成 <span class="mono">conf/actions.d/NN-slug.xml</span>；保存时自动补 generated-by 标记并跑 L0–L4 校验。</p>`,
    '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-create>创建</button>');

  modal.querySelector('[data-create]').addEventListener('click', async () => {
    const name = modal.querySelector('#nf-name').value.trim();
    const type = modal.querySelector('#nf-type').value;
    const order = Number(modal.querySelector('#nf-order').value) || 50;
    const ref = modal.querySelector('#nf-ref').value.trim();
    if (!name) { toast('请填写动作名', 'error'); return; }
    try {
      const slug = name.replace(/[^A-Za-z0-9._-]/g, '-').toLowerCase();
      const suggest = await api.get('/api/fragments/suggest' + qs({ kind: 'actions', slug, order }));
      const action = newAction(name, type);
      if (type === 'Sequence' && ref) {
        action.children.push(el('ActionReference', { Name: ref, Duration: '300' }));
      }
      if (type === 'Stay' || type === 'Move' || type === 'Animate') {
        const asset = ctx.state.assets[0];
        if (asset) {
          action.children.push(el('Animation', {}, [newPose(asset.name, asset.anchor.x, asset.anchor.y, 250, type === 'Move' ? '-2,0' : '0,0')]));
        }
      }
      const rootNode = el('Mascot', {}, [el('ActionList', {}, [action])]);
      editor = { kind: 'actions', file: suggest.file, root: rootNode, sel: 0, dirty: true, raw: false, generatedBy: true };
      rawMode = false;
      close();
      renderList(root, ctx);
      renderEditor(root, ctx);
      toast('片段已创建（尚未落盘）：' + suggest.file, 'ok');
    } catch (e) {
      toast('创建失败：' + e.message, 'error');
    }
  });
}

/* ------------------------------------------------------------------ */
/* 编辑器                                                              */
/* ------------------------------------------------------------------ */

function currentActions() {
  if (!editor) return [];
  const list = findFirst(editor.root, 'ActionList');
  return list ? list.children.filter(c => c.tag === 'Action') : [];
}

function currentAction() {
  const actions = currentActions();
  return actions[editor.sel] || actions[0] || null;
}

function renderEditor(root, ctx) {
  const host = root.querySelector('#editor-host');
  if (!editor) {
    host.innerHTML = `<div class="card"><div class="card-body"><div class="empty">
      <strong>选择左侧片段开始编辑</strong>或点击「新建动作」。没有 XML 基础也能做：填名字、选类型、加帧或引用已有动作。</div></div></div>`;
    return;
  }
  const actions = currentActions();
  const action = currentAction();
  const tabBar = actions.map((a, i) =>
    `<button class="btn btn-sm ${i === editor.sel ? 'btn-primary' : ''}" data-tab="${i}">${escapeHtml(attr(a, 'Name', '(未命名)'))}</button>`).join('');

  host.innerHTML = `
    <div class="card">
      <div class="card-head">
        <h3>${escapeHtml(editor.file)}</h3>
        <span class="tag ${editor.generatedBy ? 'studio' : 'warn'}">${editor.generatedBy ? '设计器文件' : '手写文件（覆盖需确认）'}</span>
        <div class="spacer"></div>
        <label class="inline-check"><input type="checkbox" id="chk-raw" ${rawMode ? 'checked' : ''} /> 原始 XML</label>
        <button class="btn btn-sm" id="btn-validate">校验</button>
        <button class="btn btn-sm btn-primary" id="btn-save">保存</button>
        <button class="btn btn-sm btn-danger" id="btn-delete">删除</button>
      </div>
      <div class="card-body" id="editor-body"></div>
    </div>
    <div class="card" id="diag-card" style="display:none">
      <div class="card-head"><h3>校验结果</h3></div>
      <div id="diag-list"></div>
    </div>`;

  root.querySelector('#chk-raw').addEventListener('change', (e) => {
    rawMode = e.target.checked;
    if (rawMode) editor.xmlText = buildXml();
    renderEditor(root, ctx);
  });
  root.querySelector('#btn-save').addEventListener('click', () => save(ctx, {}));
  root.querySelector('#btn-validate').addEventListener('click', () => runValidate(ctx));
  root.querySelector('#btn-delete').addEventListener('click', () => deleteFragment(ctx));
  root.querySelectorAll('[data-tab]').forEach(b => b.addEventListener('click', () => {
    editor.sel = Number(b.dataset.tab);
    renderEditor(root, ctx);
  }));

  const body = root.querySelector('#editor-body');
  if (rawMode) {
    body.innerHTML = `
      <div class="field"><label>XML（保存时会规范化：补声明、补 generated-by、缩进 2，未知属性原样保留）</label>
        <textarea id="raw-xml" style="min-height:340px">${escapeHtml(editor.xmlText)}</textarea></div>
      <p class="small muted">改完点「保存」即可；服务端会先做 L0–L4 校验，失败不落盘。</p>`;
    body.querySelector('#raw-xml').addEventListener('input', (e) => { editor.xmlText = e.target.value; editor.dirty = true; });
    return;
  }
  if (!action) {
    body.innerHTML = '<div class="empty"><strong>这个片段里还没有动作</strong>点下面的「添加动作」。</div>' +
      '<div style="text-align:center;padding-bottom:14px"><button class="btn btn-sm" id="btn-add-action">添加动作</button></div>';
    body.querySelector('#btn-add-action').addEventListener('click', () => addAction(ctx, root));
    return;
  }

  body.innerHTML = `
    <div class="row" style="margin-bottom:10px; gap:6px; flex-wrap:wrap">
      ${tabBar}
      <button class="btn btn-sm" id="btn-add-action">+ 动作</button>
    </div>
    <div class="grid-2" style="grid-template-columns:minmax(0,1fr) 300px">
      <div id="action-main"></div>
      <div id="action-side"></div>
    </div>`;

  body.querySelector('#btn-add-action').addEventListener('click', () => addAction(ctx, root));
  renderActionMain(body.querySelector('#action-main'), action, ctx, root);
  renderActionSide(body.querySelector('#action-side'), action, ctx, root);
}

function renderActionMain(host, action, ctx, root) {
  const type = attr(action, 'Type', 'Stay');
  const isSeq = type === 'Sequence' || type === 'Select';
  host.innerHTML = isSeq ? sequenceHtml(action, ctx) : animationHtml(action, ctx);
  if (isSeq) bindSequence(host, action, ctx, root);
  else bindAnimation(host, action, ctx, root);
}

function sequenceHtml(action, ctx) {
  const refs = action.children.filter(c => c.tag === 'ActionReference' || c.tag === 'Action');
  const rows = refs.map((ref, i) => `
    <tr data-row="${i}">
      <td>${i + 1}</td>
      <td><input type="text" data-ref-name="${i}" list="action-names" value="${escapeHtml(attr(ref, 'Name'))}" ${ref.tag === 'Action' ? 'disabled' : ''} /></td>
      <td><input type="text" data-ref-dur="${i}" value="${escapeHtml(attr(ref, 'Duration'))}" placeholder="继承" style="width:90px" /></td>
      <td class="nowrap">
        <button class="btn btn-sm" data-up="${i}" ${i === 0 ? 'disabled' : ''}>↑</button>
        <button class="btn btn-sm" data-down="${i}" ${i === refs.length - 1 ? 'disabled' : ''}>↓</button>
        <button class="btn btn-sm btn-danger" data-del="${i}">×</button>
      </td>
    </tr>`).join('');
  return `
    <datalist id="action-names">${((ctx.state.manifest && ctx.state.manifest.actions) || []).map(a => '<option value="' + escapeHtml(a.name) + '"></option>').join('')}</datalist>
    <div class="card-head" style="padding:0 0 10px; border:0"><h3>子动作序列（ActionReference）</h3><div class="spacer"></div>
      <button class="btn btn-sm" id="btn-add-ref">+ 引用动作</button></div>
    <table class="tbl"><thead><tr><th>#</th><th>动作名</th><th>Duration 覆盖</th><th></th></tr></thead>
      <tbody>${rows || '<tr><td colspan="4" class="empty small">还没有子动作</td></tr>'}</tbody></table>
    <p class="small muted" style="margin-top:8px">参数不会向下渗透：要覆盖子动作时长，必须写在引用节点上（设计指南 §4.2）。</p>`;
}

function bindSequence(host, action, ctx, root) {
  const refs = action.children.filter(c => c.tag === 'ActionReference' || c.tag === 'Action');
  const refresh = () => renderActionMain(host, action, ctx, root);
  host.querySelector('#btn-add-ref').addEventListener('click', () => {
    const first = ((ctx.state.manifest && ctx.state.manifest.actions) || [])[0];
    action.children.push(el('ActionReference', { Name: first ? first.name : 'Stand', Duration: '300' }));
    editor.dirty = true;
    refresh();
  });
  host.querySelectorAll('[data-ref-name]').forEach(input => {
    input.addEventListener('input', () => {
      setAttr(refs[Number(input.dataset.refName)], 'Name', input.value.trim());
      editor.dirty = true;
    });
  });
  host.querySelectorAll('[data-ref-dur]').forEach(input => {
    input.addEventListener('input', () => {
      setAttr(refs[Number(input.dataset.refDur)], 'Duration', input.value.trim());
      editor.dirty = true;
    });
  });
  host.querySelectorAll('[data-up]').forEach(b => b.addEventListener('click', () => moveChild(action, refs[Number(b.dataset.up)], -1, refresh)));
  host.querySelectorAll('[data-down]').forEach(b => b.addEventListener('click', () => moveChild(action, refs[Number(b.dataset.down)], 1, refresh)));
  host.querySelectorAll('[data-del]').forEach(b => b.addEventListener('click', () => {
    const node = refs[Number(b.dataset.del)];
    action.children.splice(action.children.indexOf(node), 1);
    editor.dirty = true;
    refresh();
  }));
}

function animationHtml(action, ctx) {
  const animations = action.children.filter(c => c.tag === 'Animation');
  const anim = animations[0];
  const poses = anim ? anim.children.filter(c => c.tag === 'Pose') : [];
  const rows = poses.map((pose, i) => poseRowHtml(pose, i, ctx)).join('');
  const total = poses.reduce((sum, p) => sum + (parseInt(attr(p, 'Duration'), 10) || 0), 0);

  return `
    <div class="card-head" style="padding:0 0 10px; border:0">
      <h3>时间轴（Animation / Pose）</h3><div class="spacer"></div>
      <span class="small muted">总时长 ${total} ms（字面量部分）</span>
      <button class="btn btn-sm" id="btn-play">▶ 静态预览</button>
      <button class="btn btn-sm" id="btn-add-pose">+ 帧</button>
    </div>
    <div class="timeline" id="timeline">${poses.map((p, i) => frameHtml(p, i, ctx)).join('') || '<span class="small muted">还没有帧</span>'}</div>
    <table class="tbl pose-table"><thead><tr>
        <th style="width:44px">#</th><th style="width:44px"></th><th>Image</th>
        <th style="width:96px">Anchor</th><th style="width:96px">Velocity</th><th style="width:110px">Duration</th><th style="width:130px"></th>
      </tr></thead><tbody>${rows || '<tr><td colspan="7" class="empty small">用「+ 帧」从素材里加一帧</td></tr>'}</tbody></table>
    <p class="small muted" style="margin-top:8px">
      <span class="tag">${'${}'}</span> 加载期求值一次（缓存）；
      <span class="tag">${'#{}'}</span> 每帧重算。Duration 必须 ≥ 1。
      「试算」会把表达式交给引擎 <span class="mono">Variable.parse</span>，不会在前端执行。
    </p>`;
}

function frameHtml(pose, i, ctx) {
  const img = attr(pose, 'Image');
  const dur = attr(pose, 'Duration');
  return `<div class="frame" data-frame="${i}">
      <img src="${escapeHtml(imageUrl(ctx, img))}" alt="" />
      <div class="label">${escapeHtml(basename(img))}</div>
      <div class="dur">${escapeHtml(dur)}</div>
    </div>`;
}

function poseRowHtml(pose, i, ctx) {
  const image = attr(pose, 'Image');
  const anchor = attr(pose, 'ImageAnchor');
  const velocity = attr(pose, 'Velocity');
  const duration = attr(pose, 'Duration');
  const badge = (value) => isExpression(value)
    ? '<span class="tag ' + (expressionKind(value) === 'per-frame' ? 'warn' : 'ok') + '">' + (expressionKind(value) === 'per-frame' ? '#{}' : '${}') + '</span>'
    : '';
  return `<tr data-pose="${i}">
      <td class="num">${i + 1}</td>
      <td><img class="pose-thumb" src="${escapeHtml(imageUrl(ctx, image))}" alt="" /></td>
      <td><input type="text" data-pose-image="${i}" list="asset-names" value="${escapeHtml(stripSlash(image))}" /></td>
      <td><input type="text" data-pose-anchor="${i}" value="${escapeHtml(anchor)}" /></td>
      <td><input type="text" data-pose-velocity="${i}" value="${escapeHtml(velocity)}" /></td>
      <td><div class="row" style="gap:4px"><input type="text" data-pose-duration="${i}" value="${escapeHtml(duration)}" />${badge(duration)}</div></td>
      <td class="nowrap">
        <button class="btn btn-sm" data-try="${i}" title="用引擎试算 Duration">试算</button>
        <button class="btn btn-sm" data-pose-up="${i}" ${i === 0 ? 'disabled' : ''}>↑</button>
        <button class="btn btn-sm btn-danger" data-pose-del="${i}">×</button>
      </td>
    </tr>`;
}

function bindAnimation(host, action, ctx, root) {
  const anim = action.children.find(c => c.tag === 'Animation');
  const poses = anim ? anim.children.filter(c => c.tag === 'Pose') : [];
  const refresh = () => renderActionMain(host, action, ctx, root);

  const ensureAnim = () => {
    let node = action.children.find(c => c.tag === 'Animation');
    if (!node) {
      node = el('Animation', {}, []);
      action.children.push(node);
    }
    return node;
  };

  host.querySelector('#btn-add-pose').addEventListener('click', () => {
    const asset = ctx.state.assets[0];
    if (!asset) { toast('当前皮肤没有素材，先去皮肤工作台导入 PNG', 'error'); return; }
    ensureAnim().children.push(newPose(asset.name, asset.anchor.x, asset.anchor.y, 250,
      attr(action, 'Type') === 'Move' ? '-2,0' : '0,0'));
    editor.dirty = true;
    refresh();
  });

  host.querySelector('#btn-play').addEventListener('click', (e) => {
    if (playing) {
      playing = false;
      clearInterval(playTimer);
      e.target.textContent = '▶ 静态预览';
      return;
    }
    if (!poses.length) return;
    playing = true;
    e.target.textContent = '■ 停止';
    let idx = 0;
    const frames = host.querySelectorAll('.frame');
    const tick = () => {
      frames.forEach((f, i) => f.classList.toggle('active', i === idx));
      const dur = Math.max(60, parseInt(attr(poses[idx], 'Duration'), 10) || 250);
      idx = (idx + 1) % poses.length;
      playTimer = setTimeout(tick, dur);
    };
    tick();
  });

  const bindValue = (selector, attrName, dataKey) => {
    host.querySelectorAll(selector).forEach(input => {
      input.addEventListener('input', () => {
        const i = Number(input.dataset[dataKey]);
        setAttr(poses[i], attrName, input.value.trim());
        editor.dirty = true;
        if ('Duration' === attrName) {
          const frame = host.querySelectorAll('.frame')[i];
          if (frame) frame.querySelector('.dur').textContent = input.value.trim();
        }
      });
    });
  };
  bindValue('[data-pose-image]', 'Image', 'poseImage');
  bindValue('[data-pose-anchor]', 'ImageAnchor', 'poseAnchor');
  bindValue('[data-pose-velocity]', 'Velocity', 'poseVelocity');
  bindValue('[data-pose-duration]', 'Duration', 'poseDuration');

  host.querySelectorAll('[data-pose-image]').forEach(input => {
    input.addEventListener('change', () => {
      const i = Number(input.dataset.poseImage);
      const value = input.value.trim();
      setAttr(poses[i], 'Image', value.startsWith('/') || value.startsWith('file:') ? value : '/' + value);
      editor.dirty = true;
      refresh();
    });
  });

  host.querySelectorAll('[data-try]').forEach(btn => btn.addEventListener('click', async () => {
    const pose = poses[Number(btn.dataset.try)];
    const expression = attr(pose, 'Duration');
    const params = {};
    Object.entries(action.attrs || {}).forEach(([k, v]) => { params[k] = v; });
    try {
      const result = await api.post('/api/simulate', { expression, params });
      toast('Duration 试算 = ' + result.value + '（' + result.type + '）', result.intValue < 1 ? 'error' : 'ok');
    } catch (e) {
      toast(e.message, 'error');
    }
  }));

  host.querySelectorAll('[data-pose-up]').forEach(btn => btn.addEventListener('click', () => {
    const i = Number(btn.dataset.poseUp);
    const parent = poses[i].__parent || ensureAnim();
    moveChild(parent, poses[i], -1, refresh);
  }));
  host.querySelectorAll('[data-pose-del]').forEach(btn => btn.addEventListener('click', () => {
    const i = Number(btn.dataset.poseDel);
    ensureAnim().children.splice(ensureAnim().children.indexOf(poses[i]), 1);
    editor.dirty = true;
    refresh();
  }));
}

function moveChild(parent, node, delta, after) {
  const index = parent.children.indexOf(node);
  const target = index + delta;
  if (index < 0 || target < 0 || target >= parent.children.length) return;
  parent.children.splice(index, 1);
  parent.children.splice(target, 0, node);
  if (editor) editor.dirty = true;
  after();
}

function renderActionSide(host, action, ctx, root) {
  const type = attr(action, 'Type', 'Stay');
  const actions = ((ctx.state.manifest && ctx.state.manifest.actions) || []);
  host.innerHTML = `
    <div class="card">
      <div class="card-head"><h3>动作属性</h3></div>
      <div class="card-body">
        <div class="field"><label>Name</label><input type="text" id="a-name" value="${escapeHtml(attr(action, 'Name'))}" /></div>
        <div class="field"><label>Type</label>
          <select id="a-type">${(ctx.state.schema.actionTypes || []).map(t =>
            '<option value="' + t.value + '"' + (t.value === type ? ' selected' : '') + (t.readOnly ? ' disabled' : '') + '>' + t.value + '</option>').join('')}
          </select></div>
        <div class="field"><label>BorderType</label>
          <select id="a-border">
            <option value="">（默认）</option>
            ${(ctx.state.schema.borderTypes || []).map(b => '<option value="' + b + '"' + (attr(action, 'BorderType') === b ? ' selected' : '') + '>' + b + '</option>').join('')}
          </select></div>
        <div class="field"><label>Loop</label>
          <select id="a-loop">
            <option value="">（默认）</option>
            <option value="true" ${attr(action, 'Loop') === 'true' ? 'selected' : ''}>true</option>
            <option value="false" ${attr(action, 'Loop') === 'false' ? 'selected' : ''}>false</option>
          </select></div>
        ${type === 'Embedded' ? `<p class="small muted">Class: <span class="mono">${escapeHtml(attr(action, 'Class'))}</span>（内置动作只读）</p>` : ''}
      </div>
    </div>
    <div class="card">
      <div class="card-head"><h3>快捷操作</h3></div>
      <div class="card-body">
        <button class="btn btn-sm" id="btn-register-behavior" style="width:100%;margin-bottom:8px">注册为预览行为（Frequency=0）</button>
        <button class="btn btn-sm" id="btn-run-action" style="width:100%">在桌面预览这个动作</button>
        <p class="small muted" style="margin:8px 0 0">行为注册会生成配套 behaviors.d 片段，名字与动作同名。</p>
      </div>
    </div>
    <div class="card">
      <div class="card-head"><h3>引用完整性</h3></div>
      <div class="card-body small" id="ref-info"></div>
    </div>`;

  host.querySelector('#a-name').addEventListener('input', (e) => {
    setAttr(action, 'Name', e.target.value.trim());
    editor.dirty = true;
  });
  host.querySelector('#a-type').addEventListener('change', (e) => {
    setAttr(action, 'Type', e.target.value);
    editor.dirty = true;
    renderEditor(root, ctx);
  });
  host.querySelector('#a-border').addEventListener('change', (e) => setAttr(action, 'BorderType', e.target.value));
  host.querySelector('#a-loop').addEventListener('change', (e) => setAttr(action, 'Loop', e.target.value));

  host.querySelector('#btn-register-behavior').addEventListener('click', async () => {
    const name = attr(action, 'Name');
    if (!name) { toast('先给动作起个名字', 'error'); return; }
    await ctx.saveIfDirty();
    const ok = await ctx.confirmDialog('注册预览行为',
      `<p class="small">将创建 <span class="mono">conf/behaviors.d/NN-${escapeHtml(slug(name))}.xml</span>，
       内容为 <span class="mono">&lt;Behavior Name="${escapeHtml(name)}" Frequency="0" Hidden="true" /&gt;</span>。</p>
       <p class="small muted">Frequency=0 不进自主行为树，只能按名预览，不会改变原生行为分布。</p>`);
    if (!ok) return;
    try {
      const suggest = await api.get('/api/fragments/suggest' + qs({ kind: 'behaviors', slug: slug(name), order: 50 }));
      const rootNode = el('Mascot', {}, [el('BehaviorList', {}, [el('Behavior', { Name: name, Frequency: '0', Hidden: 'true' })])]);
      await api.put('/api/fragments/behaviors/' + encodeURIComponent(suggest.file), {
        skin: ctx.state.skin, xml: buildFragment('behaviors', rootNode.children[0].children),
      });
      ctx.setBehaviorTarget(name);
      toast('已注册行为：' + name, 'ok');
      await ctx.refresh();
    } catch (e) {
      const msg = e instanceof ApiError && e.diagnostics.length
        ? e.diagnostics.map(d => d.message).join('；') : e.message;
      toast('注册失败：' + msg, 'error');
    }
  });

  host.querySelector('#btn-run-action').addEventListener('click', async () => {
    const name = attr(action, 'Name');
    await ctx.saveIfDirty();
    ctx.setBehaviorTarget(name);
    document.getElementById('btn-run').click();
  });

  // 引用完整性：谁引用了这个动作名（展示用；权威结论来自服务端校验）
  const name = attr(action, 'Name');
  const behaviors = ((ctx.state.manifest && ctx.state.manifest.behaviors) || []).filter(b => b.action === name);
  const info = host.querySelector('#ref-info');
  info.innerHTML = behaviors.length
    ? '被这些行为引用：<br>' + behaviors.map(b => '<span class="tag">' + escapeHtml(b.name) + '</span>').join(' ')
    : '<span class="muted">暂无行为引用这个动作。改名不会破坏引用（名字即绑定），但会影响 Behavior 的 Action=。</span>';
}

/* ------------------------------------------------------------------ */
/* 保存 / 校验 / 删除                                                   */
/* ------------------------------------------------------------------ */

function buildXml() {
  if (rawMode && editor.xmlText) return editor.xmlText;
  const list = findFirst(editor.root, 'ActionList');
  return buildFragment('actions', list ? list.children : []);
}

export async function save(ctx, opts = {}) {
  if (!editor) {
    if (!opts.silent) toast('没有正在编辑的片段', 'error');
    return false;
  }
  const xml = buildXml();
  try {
    const result = await api.put('/api/fragments/actions/' + encodeURIComponent(editor.file), {
      skin: ctx.state.skin, xml, force: !!editor.forceOverwrite,
    });
    editor.dirty = false;
    editor.generatedBy = true;
    await ctx.refreshManifest();
    await ctx.refresh();
    if (!opts.silent) toast('已保存 ' + editor.file + (result.backup ? '（已留备份）' : ''), 'ok');
    showDiagnostics(ctx, result.diagnostics || [], result.summary);
    return true;
  } catch (e) {
    if (e.status === 409 && !editor.forceOverwrite) {
      const ok = await ctx.confirmDialog('覆盖确认',
        '<p class="small">该片段不是设计器生成的（可能是手写资产）。覆盖前会自动留 <span class="mono">.bak</span> 备份。</p>');
      if (ok) {
        editor.forceOverwrite = true;
        return save(ctx, opts);
      }
      return false;
    }
    if (e instanceof ApiError && e.diagnostics.length) {
      showDiagnostics(ctx, e.diagnostics, e.payload && e.payload.summary);
      if (!opts.silent) toast('校验未通过，文件未改动', 'error');
      else throw e;
    } else {
      toast('保存失败：' + e.message, 'error');
    }
    return false;
  }
}

async function runValidate(ctx) {
  try {
    const result = await api.post('/api/validate', {
      skin: ctx.state.skin,
      fragments: [{ kind: 'actions', file: editor.file, xml: buildXml() }],
    });
    showDiagnostics(ctx, result.diagnostics || [], result.summary);
    toast(result.summary.ok ? '校验通过' : ('发现 ' + result.summary.errors + ' 个错误'), result.summary.ok ? 'ok' : 'error');
  } catch (e) {
    toast('校验失败：' + e.message, 'error');
  }
}

function showDiagnostics(ctx, diagnostics, summary) {
  ctx.state.lastValidation = { diagnostics, summary };
  const card = document.querySelector('#diag-card');
  const list = document.querySelector('#diag-list');
  if (!card || !list) {
    if (diagnostics && diagnostics.length) {
      toast('发现 ' + diagnostics.length + ' 条问题，去校验中心查看', 'error');
    }
    return;
  }
  card.style.display = diagnostics && diagnostics.length ? '' : 'none';
  list.innerHTML = (diagnostics || []).map(d => `
    <div class="diag ${d.severity}">
      <div class="sev">${d.severity === 'error' ? 'ERROR' : d.severity === 'warning' ? 'WARN' : 'INFO'}</div>
      <div><div class="msg">${escapeHtml(d.message)}</div>
        <div class="loc">${escapeHtml(d.file || '')}${d.action ? ' · Action[' + escapeHtml(d.action) + ']' : ''}${d.hint ? ' · ' + escapeHtml(d.hint) : ''}</div></div>
      <div class="layer">${escapeHtml(d.layer || '')}</div>
    </div>`).join('');
}

async function deleteFragment(ctx) {
  if (!editor) return;
  const ok = await ctx.confirmDialog('删除片段', '<p class="small">将删除 <span class="mono">' + escapeHtml(editor.file) + '</span>。删除后需要 <b>reload</b> 才会从引擎消失。</p>');
  if (!ok) return;
  try {
    await api.del('/api/fragments/actions/' + encodeURIComponent(editor.file));
    toast('已删除', 'ok');
    editor = null;
    await ctx.refresh();
  } catch (e) {
    if (e.status === 409) {
      const force = await ctx.confirmDialog('非设计器文件', '<p class="small">该文件不是设计器生成的，确认要删除吗？</p>');
      if (force) {
        await api.del('/api/fragments/actions/' + encodeURIComponent(editor.file) + '?force=true');
        editor = null;
        await ctx.refresh();
        return;
      }
      return;
    }
    toast('删除失败：' + e.message, 'error');
  }
}

function addAction(ctx) {
  const name = prompt('新动作名（建议加包前缀，如 MyPack.Wave）');
  if (!name) return;
  const action = newAction(name.trim(), 'Stay');
  const list = findFirst(editor.root, 'ActionList');
  list.children.push(action);
  editor.sel = list.children.length - 1;
  editor.dirty = true;
  renderEditor(document.querySelector('#panel-action'), ctx);
}

/* ------------------------------------------------------------------ */
/* 工具                                                                */
/* ------------------------------------------------------------------ */

function imageUrl(ctx, image) {
  if (!image) return '';
  const clean = String(image).replace(/^file:/, '');
  const parts = clean.split('/');
  const name = parts[parts.length - 1];
  if (!name) return '';
  return '/api/assets/thumb' + qs({ skin: ctx.state.skin, path: name, w: 96 });
}

function stripSlash(image) {
  return String(image || '').replace(/^\//, '');
}

function basename(path) {
  const parts = String(path || '').split('/');
  return parts[parts.length - 1];
}

function slug(name) {
  return String(name).replace(/[^A-Za-z0-9._-]/g, '-').toLowerCase();
}
