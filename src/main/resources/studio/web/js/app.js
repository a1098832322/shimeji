import { api, qs, toast, escapeHtml, openModal, confirmDialog, downloadBytes, fileToBase64, ApiError } from './api.js';
import * as skinPanel from './panels/skin.js';
import * as actionPanel from './panels/action.js';
import * as behaviorPanel from './panels/behavior.js';
import * as validatePanel from './panels/validate.js';
import * as previewPanel from './panels/preview.js';
import * as packsPanel from './panels/packs.js';
import * as layersPanel from './panels/layers.js';

const panels = {
  skin: skinPanel,
  action: actionPanel,
  behavior: behaviorPanel,
  validate: validatePanel,
  preview: previewPanel,
  packs: packsPanel,
  layers: layersPanel,
};

export const state = {
  panel: 'skin',
  workspace: null,
  schema: null,
  skins: [],
  skin: null,
  assets: [],
  manifest: null,
  fragments: { actions: [], behaviors: [] },
  preview: null,
  selectedBehavior: null,
  lastRun: null,
  lastValidation: null,
};

export const ctx = {
  state,
  api,
  qs,
  toast,
  escapeHtml,
  openModal,
  confirmDialog,
  downloadBytes,
  fileToBase64,
  goto,
  refresh,
  refreshManifest,
  refreshAssets,
  setBehaviorTarget,
  saveIfDirty,
  runPreview,
  jumpToDiagnostic,
};

/* ------------------------------------------------------------------ */
/* 数据加载                                                            */
/* ------------------------------------------------------------------ */

async function loadWorkspace() {
  state.workspace = await api.get('/api/workspace');
  state.preview = state.workspace.preview;
  const skinData = await api.get('/api/skins');
  state.skins = skinData.skins || [];
  if (!state.skin || !state.skins.some(s => s.id === state.skin)) {
    state.skin = state.skins.length ? state.skins[0].id : null;
  }
  state.schema = state.schema || await api.get('/api/schema');
}

export async function refreshAssets() {
  if (!state.skin) { state.assets = []; return; }
  const data = await api.get('/api/skins/' + encodeURIComponent(state.skin) + '/assets');
  state.assets = data.assets || [];
}

export async function refreshManifest() {
  if (!state.skin) { state.manifest = null; return; }
  state.manifest = await api.get('/api/manifest' + qs({ skin: state.skin }));
}

async function loadFragments() {
  state.fragments = await api.get('/api/fragments');
}

export async function refresh() {
  try {
    await loadWorkspace();
    await refreshAssets();
    await refreshManifest();
    await loadFragments();
    renderHeader();
    renderActive();
  } catch (e) {
    toast('加载失败：' + e.message, 'error');
  }
}

/* ------------------------------------------------------------------ */
/* 头部                                                                */
/* ------------------------------------------------------------------ */

function renderHeader() {
  const w = state.workspace;
  const chips = [];
  if (w) {
    chips.push('<span class="chip ' + (w.devMode ? 'dev' : '') + '" title="' + escapeHtml(w.confDir) + '">生效 conf：<strong>' + escapeHtml(w.relativeConf) + '</strong></span>');
    chips.push('<span class="chip" title="' + escapeHtml(w.imgDir) + '">生效 img：<strong>' + escapeHtml(w.relativeImg) + '</strong></span>');
    chips.push('<span class="chip" title="' + escapeHtml(w.engineRoot) + '">引擎根：<strong>' + escapeHtml(shortPath(w.engineRoot)) + '</strong></span>');
    if (w.layoutError) {
      chips.push('<span class="chip warn">' + escapeHtml(w.layoutError) + '</span>');
    }
  }
  document.getElementById('env-chips').innerHTML = chips.join('');

  const select = document.getElementById('skin-select');
  const current = state.skin;
  select.innerHTML = state.skins.map(s =>
    '<option value="' + escapeHtml(s.id) + '"' + (s.id === current ? ' selected' : '') + '>' +
    escapeHtml(s.id) + (s.displayName ? '（' + escapeHtml(s.displayName) + '）' : '') + '</option>').join('');

  document.getElementById('side-version').textContent = w ? ('引擎 ' + w.version + ' · ' + w.layout) : '';
  renderPreviewPill();
}

function shortPath(p) {
  if (!p) return '';
  const parts = String(p).split('/');
  return parts.length > 3 ? '…/' + parts.slice(-2).join('/') : p;
}

function renderPreviewPill() {
  const pill = document.getElementById('preview-pill');
  const preview = state.preview || {};
  const st = preview.state || 'stopped';
  const map = {
    stopped: ['预览：未启动', ''],
    starting: ['预览：启动中…', ''],
    running: ['预览：运行中' + (preview.port ? ' :' + preview.port : ''), 'running'],
    offline: ['预览：离线（编辑不受影响）', 'offline'],
  };
  const [text, cls] = map[st] || map.stopped;
  pill.textContent = text;
  pill.className = 'pill ' + cls;
  document.getElementById('btn-start-preview').textContent = st === 'running' ? '重启预览' : '启动预览';
}

/* ------------------------------------------------------------------ */
/* 导航                                                                */
/* ------------------------------------------------------------------ */

export function goto(panel) {
  state.panel = panel;
  if (location.hash !== '#' + panel) {
    history.replaceState(null, '', '#' + panel);
  }
  document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.panel === panel));
  document.querySelectorAll('.panel').forEach(p => p.classList.toggle('hidden', p.id !== 'panel-' + panel));
  renderActive();
}

function renderActive() {
  const module = panels[state.panel];
  const container = document.getElementById('panel-' + state.panel);
  container.innerHTML = '';
  try {
    module.render(container, ctx);
  } catch (e) {
    container.innerHTML = '<div class="card"><div class="card-body"><div class="empty"><strong>面板渲染失败</strong>' +
      escapeHtml(e.message) + '</div></div></div>';
    console.error(e);
  }
}

export function setBehaviorTarget(name) {
  state.selectedBehavior = name;
}

/* ------------------------------------------------------------------ */
/* 运行（保存 → 校验 → reload → setRole → setBehavior → status）        */
/* ------------------------------------------------------------------ */

async function runPreview() {
  const btn = document.getElementById('btn-run');
  btn.disabled = true;
  try {
    const module = panels[state.panel];
    if (module && typeof module.save === 'function') {
      await module.save(ctx, { silent: true });
    }
    const result = await api.post('/api/preview/run', {
      behavior: state.selectedBehavior || null,
      skin: state.skin,
    });
    state.lastRun = result;
    state.preview = result.preview || state.preview;
    renderPreviewPill();
    const ok = result.steps.every(s => s.ok);
    toast(ok ? '已应用到桌面预览' : '预览未完全成功，请看预览中心', ok ? 'ok' : 'error');
    if (state.panel !== 'preview') {
      goto('preview');
    } else {
      renderActive();
    }
  } catch (e) {
    if (e instanceof ApiError && e.diagnostics.length) {
      state.lastValidation = { diagnostics: e.diagnostics, summary: (e.payload && e.payload.summary) || null };
      toast(e.message, 'error');
      goto('validate');
    } else {
      toast('运行失败：' + e.message, 'error');
    }
  } finally {
    btn.disabled = false;
  }
}

/** 保存当前面板的编辑内容；校验失败时抛出，由调用方决定 UI 反馈。 */
async function saveIfDirty() {
  const module = panels[state.panel];
  if (!module || typeof module.save !== 'function') return true;
  try {
    await module.save(ctx, { silent: true });
    return true;
  } catch (e) {
    toast(e.message || '保存失败', 'error');
    return false;
  }
}

export function jumpToDiagnostic(diag) {
  if (!diag) return;
  const file = String(diag.file || '');
  if (file.includes('behaviors.d')) {
    state.panel = 'behavior';
    goto('behavior');
    if (typeof panels.behavior.focusFragment === 'function') {
      panels.behavior.focusFragment(ctx, baseName(file));
    }
    return;
  }
  if (file.includes('actions.d') || file.includes('info.xml') || file.endsWith('.png')) {
    if (file.includes('info.xml') || file.endsWith('.png')) {
      goto('skin');
      return;
    }
    goto('action');
    if (typeof panels.action.focusFragment === 'function') {
      panels.action.focusFragment(ctx, baseName(file), diag.action);
    }
  }
}

function baseName(path) {
  const parts = String(path).split('/');
  return parts[parts.length - 1];
}

/* ------------------------------------------------------------------ */
/* 启动                                                                */
/* ------------------------------------------------------------------ */

function bindShell() {
  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', () => goto(btn.dataset.panel));
  });

  document.getElementById('skin-select').addEventListener('change', async (e) => {
    state.skin = e.target.value;
    state.selectedBehavior = null;
    await refresh();
  });

  document.getElementById('btn-run').addEventListener('click', runPreview);

  document.getElementById('btn-start-preview').addEventListener('click', async () => {
    try {
      const preview = await api.post('/api/preview/start', {});
      state.preview = preview;
      renderPreviewPill();
      if (preview.state === 'running') {
        toast('预览进程已就绪（端口 ' + preview.port + '）', 'ok');
      } else {
        toast(preview.error || '预览启动未完成', 'error');
      }
      if (state.panel === 'preview') renderActive();
      else goto('preview');
    } catch (e) {
      toast('启动失败：' + e.message, 'error');
    }
  });

  setInterval(async () => {
    try {
      const preview = await api.get('/api/preview/status');
      const changed = !state.preview || state.preview.state !== preview.state || state.preview.port !== preview.port;
      state.preview = preview;
      if (changed) {
        renderPreviewPill();
        if (state.panel === 'preview') renderActive();
      }
    } catch (e) { /* 静默：轮询失败不打扰用户 */ }
  }, 3000);
}

(async function main() {
  bindShell();
  const initial = (location.hash || '').replace('#', '').split('/')[0];
  if (panels[initial]) state.panel = initial;
  document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.panel === state.panel));
  document.querySelectorAll('.panel').forEach(p => p.classList.toggle('hidden', p.id !== 'panel-' + state.panel));
  await refresh();
  window.addEventListener('hashchange', () => {
    const target = (location.hash || '').replace('#', '').split('/')[0];
    if (panels[target] && target !== state.panel) goto(target);
  });
  // 启动时若布局不完整，给出明确提示（AC-01-2：不允许保存）
  if (state.workspace && state.workspace.layoutError) {
    openModal('工作区不可用',
      '<p class="small">' + escapeHtml(state.workspace.layoutError) + '</p>' +
      '<p class="small muted">请用 <span class="mono">--workspace</span> 指向包含 conf/ 或 src/main/resources/conf 的引擎根目录，并从该目录启动。</p>',
      '<button class="btn" data-close>知道了</button>');
  }
})();
