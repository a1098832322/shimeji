import { api, qs, toast, escapeHtml, openModal, confirmDialog, postForBlob, fileToBase64, ApiError } from '../api.js';

let selectedAsset = null;
let selectedSkin = null;
let showRawInfo = false;

export function render(container, ctx) {
  const state = ctx.state;
  if (!state.skin) {
    container.innerHTML = '<div class="card"><div class="card-body"><div class="empty"><strong>还没有可用皮肤</strong>' +
      '新建一个皮肤目录，或把已有形象放进 img/ 下。</div></div></div>';
    return;
  }
  if (selectedSkin !== state.skin) {
    selectedSkin = state.skin;
    const preferred = state.assets.find(a => /^shime1\.(png|gif)$/i.test(a.name))
      || state.assets.find(a => a.width === 128 && a.height === 128)
      || state.assets[0];
    selectedAsset = preferred ? preferred.name : null;
  }
  const skin = state.skins.find(s => s.id === state.skin) || { id: state.skin };

  const wrap = document.createElement('div');
  wrap.innerHTML = `
    <div class="grid-side">
      <div class="card">
        <div class="card-head"><h3>形象（img/）</h3><div class="spacer"></div>
          <button class="btn btn-sm" id="btn-new-skin">新建</button></div>
        <div class="list scroll" id="skin-list" style="max-height:calc(100vh - 220px)"></div>
      </div>

      <div style="display:grid; gap:12px; min-width:0">
        <div class="card">
          <div class="card-head"><h3>${escapeHtml(skin.id)} · 元数据（info.xml）</h3>
            <div class="spacer"></div>
            <span class="small muted" id="info-state"></span>
            <button class="btn btn-sm" id="btn-toggle-raw">原始 XML</button>
            <button class="btn btn-sm btn-primary" id="btn-save-info">保存</button>
          </div>
          <div class="card-body" id="info-body"></div>
        </div>

        <div class="card">
          <div class="card-head"><h3>素材</h3><div class="spacer"></div>
            <span class="small muted" id="asset-count"></span>
            <label class="btn btn-sm" for="file-import">导入 PNG</label>
            <input type="file" id="file-import" accept="image/png,image/gif,image/bmp" multiple class="hidden" />
          </div>
          <div class="card-body">
            <div class="row small muted" style="margin-bottom:10px; gap:14px; flex-wrap:wrap">
              <label class="inline-check"><input type="checkbox" id="chk-grid" /> 精灵图切图</label>
              <span id="grid-opts" class="row hidden" style="gap:6px">
                <span>行</span><input type="number" id="grid-rows" value="1" min="1" style="width:56px" />
                <span>列</span><input type="number" id="grid-cols" value="4" min="1" style="width:56px" />
                <span>命名前缀</span><input type="text" id="grid-prefix" value="shime" style="width:90px" />
              </span>
              <span class="grow"></span>
              <span>拖入图片也可导入</span>
            </div>
            <div class="asset-grid" id="asset-grid"></div>
            <div class="dropzone" id="dropzone" style="margin-top:10px">把 PNG 拖到这里导入到 img/${escapeHtml(skin.id)}/</div>
          </div>
        </div>

        <div class="grid-2">
          <div class="card">
            <div class="card-head"><h3>锚点</h3><div class="spacer"></div>
              <span class="small muted" id="anchor-hint"></span></div>
            <div class="card-body" id="anchor-body"></div>
          </div>
          <div class="card">
            <div class="card-head"><h3>包</h3></div>
            <div class="card-body">
              <p class="small muted">导出 = img/&lt;角色&gt;/** + info.xml + 可选设计器片段；引擎永不读 manifest.json。</p>
              <div class="field"><label class="inline-check"><input type="checkbox" id="chk-shared" checked /> 同时导出设计器片段（shared/）</label></div>
              <button class="btn" id="btn-export">导出皮肤包</button>
              <div class="hr"></div>
              <label class="btn" for="file-skin-zip">导入皮肤包</label>
              <input type="file" id="file-skin-zip" accept=".zip" class="hidden" />
              <p class="small muted" style="margin-bottom:0">导入前会先给出「将新增 / 将覆盖 / 冲突」清单。</p>
            </div>
          </div>
        </div>
      </div>
    </div>`;
  container.appendChild(wrap);

  renderSkinList(wrap, ctx);
  renderInfo(wrap, ctx);
  renderAssets(wrap, ctx);
  renderAnchor(wrap, ctx);
  bindActions(wrap, ctx);
}

/* ---------------- 皮肤列表 ---------------- */

function renderSkinList(root, ctx) {
  const list = root.querySelector('#skin-list');
  list.innerHTML = ctx.state.skins.map(s => `
    <div class="list-item ${s.id === ctx.state.skin ? 'active' : ''}" data-skin="${escapeHtml(s.id)}">
      <img src="${escapeHtml(s.previewUrl || '')}" alt="" style="width:26px;height:26px;object-fit:contain;image-rendering:pixelated" />
      <div style="min-width:0">
        <div class="name">${escapeHtml(s.id)}</div>
        <div class="sub">${s.assetCount} 图${s.hasIncrement ? ' · 含增量' : ''}</div>
      </div>
    </div>`).join('');
  list.querySelectorAll('[data-skin]').forEach(item => {
    item.addEventListener('click', async () => {
      if (item.dataset.skin === ctx.state.skin) return;
      ctx.state.skin = item.dataset.skin;
      document.getElementById('skin-select').value = item.dataset.skin;
      await ctx.refresh();
    });
  });
}

/* ---------------- 元数据 ---------------- */

function renderInfo(root, ctx) {
  const body = root.querySelector('#info-body');
  const stateEl = root.querySelector('#info-state');
  // 通过 /api/info 读取一次（skins 列表里只有摘要）
  api.get('/api/info' + qs({ skin: ctx.state.skin })).then(info => {
    stateEl.textContent = info.exists ? (info.generatedBy ? '设计器生成' : '用户手写（只读保护）') : '尚未创建';
    if (showRawInfo) {
      body.innerHTML = `<div class="field"><label>info.xml 原文</label>
        <textarea id="info-raw" style="min-height:180px">${escapeHtml(info.raw || defaultInfoXml(ctx.state.skin))}</textarea></div>`;
      return;
    }
    const f = (key, label, placeholder = '') => `
      <div class="field"><label>${label}</label>
        <input type="text" id="info-${key}" value="${escapeHtml(info[key] || '')}" placeholder="${escapeHtml(placeholder)}" /></div>`;
    body.innerHTML = `
      <div class="grid-2" style="grid-template-columns:1fr 1fr; gap:0 14px">
        <div>
          ${f('displayName', '显示名称 Name')}
          ${f('previewImage', '预览图 PreviewImage', 'shime1.png')}
          ${f('splashImage', '启动图 SplashImage')}
        </div>
        <div>
          ${f('artist', '作者 Artist')}
          ${f('artistUrl', '作者链接 URL')}
          ${f('scripter', '脚本 Scripter')}
        </div>
      </div>
      <p class="small muted" style="margin:0">PreviewImage 留空时，原生选择器仍按 shime1.png 兜底；设置后会优先使用该图。</p>`;
  });
}

function defaultInfoXml(skin) {
  return '<?xml version="1.0" encoding="UTF-8" ?>\n<Mascot>\n  <Information>\n    <Name>' + skin + '</Name>\n  </Information>\n</Mascot>\n';
}

/* ---------------- 素材 ---------------- */

function renderAssets(root, ctx) {
  const grid = root.querySelector('#asset-grid');
  const count = root.querySelector('#asset-count');
  const assets = ctx.state.assets;
  count.textContent = assets.length + ' 张';
  if (!assets.length) {
    grid.innerHTML = '<div class="empty" style="grid-column:1/-1"><strong>还没有素材</strong>导入 PNG，或新建皮肤后再导入。</div>';
    return;
  }
  grid.innerHTML = assets.map(a => `
    <div class="asset ${a.name === selectedAsset ? 'active' : ''}" data-asset="${escapeHtml(a.name)}">
      <div class="thumb"><img src="${escapeHtml(a.thumbUrl)}" alt="" loading="lazy" /></div>
      <div class="nm" title="${escapeHtml(a.name)}">${escapeHtml(a.name)}</div>
      <div class="meta"><span>${a.width}×${a.height}</span><span>${a.anchor ? a.anchor.x + ',' + a.anchor.y : ''}</span></div>
    </div>`).join('');
  grid.querySelectorAll('[data-asset]').forEach(el => {
    el.addEventListener('click', () => {
      selectedAsset = el.dataset.asset;
      renderAssets(root, ctx);
      renderAnchor(root, ctx);
    });
  });
}

/* ---------------- 锚点画布 ---------------- */

function renderAnchor(root, ctx) {
  const body = root.querySelector('#anchor-body');
  const hint = root.querySelector('#anchor-hint');
  const asset = ctx.state.assets.find(a => a.name === selectedAsset);
  if (!asset) {
    body.innerHTML = '<div class="empty">选择左侧一张素材来设置锚点</div>';
    hint.textContent = '';
    return;
  }
  hint.textContent = asset.name + ' · 锚点 ' + asset.anchor.x + ',' + asset.anchor.y +
    (asset.anchorSource === 'preset' ? '（预设）' : '（默认：底边中点）');

  body.innerHTML = `
    <div class="anchor-canvas-wrap">
      <canvas class="anchor-canvas" id="anchor-canvas" width="${asset.width + 24}" height="${asset.height + 24}"></canvas>
    </div>
    <div class="row" style="margin-top:10px; gap:8px">
      <div class="field" style="margin:0; width:88px"><label>锚点 X</label><input type="number" id="anchor-x" value="${asset.anchor.x}" /></div>
      <div class="field" style="margin:0; width:88px"><label>锚点 Y</label><input type="number" id="anchor-y" value="${asset.anchor.y}" /></div>
      <div class="grow"></div>
      <button class="btn btn-sm" id="btn-anchor-save">保存锚点</button>
    </div>
    <p class="small muted" style="margin:8px 0 0">拖拽或点击画布设置锚点；锚点会写进新建帧的 <span class="mono">ImageAnchor</span>。引擎按原始像素渲染，不缩放。</p>`;

  const canvas = body.querySelector('#anchor-canvas');
  const g = canvas.getContext('2d');
  const img = new Image();
  let ax = asset.anchor.x;
  let ay = asset.anchor.y;
  const offset = 12;

  const draw = () => {
    g.clearRect(0, 0, canvas.width, canvas.height);
    // 棋盘格
    const cell = 8;
    for (let y = 0; y < canvas.height; y += cell) {
      for (let x = 0; x < canvas.width; x += cell) {
        g.fillStyle = ((x / cell + y / cell) % 2 === 0) ? '#ffffff' : '#eef0f3';
        g.fillRect(x, y, cell, cell);
      }
    }
    if (img.complete && img.naturalWidth) {
      g.imageSmoothingEnabled = false;
      g.drawImage(img, offset, offset, asset.width, asset.height);
    }
    // 中心线
    g.strokeStyle = 'rgba(47,111,235,.25)';
    g.beginPath();
    g.moveTo(offset + asset.width / 2, offset);
    g.lineTo(offset + asset.width / 2, offset + asset.height);
    g.moveTo(offset, offset + asset.height / 2);
    g.lineTo(offset + asset.width, offset + asset.height / 2);
    g.stroke();
    // 锚点
    g.strokeStyle = '#d1242f';
    g.lineWidth = 2;
    const px = offset + ax;
    const py = offset + ay;
    g.beginPath();
    g.moveTo(px - 8, py); g.lineTo(px + 8, py);
    g.moveTo(px, py - 8); g.lineTo(px, py + 8);
    g.stroke();
    g.beginPath();
    g.arc(px, py, 4, 0, Math.PI * 2);
    g.stroke();
    g.lineWidth = 1;
  };

  img.onload = draw;
  img.src = asset.url;
  draw();

  const pick = (event) => {
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const x = Math.round((event.clientX - rect.left) * scaleX) - offset;
    const y = Math.round((event.clientY - rect.top) * scaleY) - offset;
    ax = Math.max(0, Math.min(asset.width, x));
    ay = Math.max(0, Math.min(asset.height, y));
    body.querySelector('#anchor-x').value = ax;
    body.querySelector('#anchor-y').value = ay;
    draw();
  };
  canvas.addEventListener('click', pick);
  canvas.addEventListener('pointerdown', (e) => {
    canvas.setPointerCapture(e.pointerId);
    pick(e);
    const move = (ev) => pick(ev);
    const up = () => {
      canvas.removeEventListener('pointermove', move);
      canvas.removeEventListener('pointerup', up);
    };
    canvas.addEventListener('pointermove', move);
    canvas.addEventListener('pointerup', up);
  });

  body.querySelector('#btn-anchor-save').addEventListener('click', async () => {
    const x = Number(body.querySelector('#anchor-x').value);
    const y = Number(body.querySelector('#anchor-y').value);
    try {
      await api.post('/api/assets/anchor', { skin: ctx.state.skin, asset: asset.name, x, y });
      toast('锚点已保存', 'ok');
      await ctx.refreshAssets();
      renderAnchor(root, ctx);
    } catch (e) {
      toast('保存锚点失败：' + e.message, 'error');
    }
  });
}

/* ---------------- 交互绑定 ---------------- */

function bindActions(root, ctx) {
  const gridCheck = root.querySelector('#chk-grid');
  gridCheck.addEventListener('change', () => {
    root.querySelector('#grid-opts').classList.toggle('hidden', !gridCheck.checked);
  });

  root.querySelector('#file-import').addEventListener('change', async (e) => {
    await importFiles(Array.from(e.target.files || []), root, ctx);
    e.target.value = '';
  });

  const dz = root.querySelector('#dropzone');
  ['dragenter', 'dragover'].forEach(ev => dz.addEventListener(ev, (e) => {
    e.preventDefault();
    dz.classList.add('over');
  }));
  ['dragleave', 'drop'].forEach(ev => dz.addEventListener(ev, (e) => {
    e.preventDefault();
    dz.classList.remove('over');
  }));
  dz.addEventListener('drop', async (e) => {
    const files = Array.from((e.dataTransfer && e.dataTransfer.files) || []).filter(f => /\.(png|gif|bmp)$/i.test(f.name));
    await importFiles(files, root, ctx);
  });

  root.querySelector('#btn-toggle-raw').addEventListener('click', () => {
    showRawInfo = !showRawInfo;
    renderInfo(root, ctx);
  });

  root.querySelector('#btn-save-info').addEventListener('click', async () => {
    const raw = root.querySelector('#info-raw');
    const payload = { skin: ctx.state.skin };
    if (raw) {
      payload.raw = raw.value;
    } else {
      ['displayName', 'previewImage', 'splashImage', 'artist', 'artistUrl', 'scripter'].forEach(k => {
        const input = root.querySelector('#info-' + k);
        if (input) payload[k] = input.value;
      });
    }
    try {
      await api.put('/api/info', payload);
      toast('info.xml 已保存', 'ok');
      await ctx.refresh();
    } catch (e) {
      if (e.status === 409 && await ctx.confirmDialog('覆盖确认',
        '<p class="small">info.xml 不是设计器生成的（可能是手写资产）。覆盖前会自动留 <span class="mono">.bak</span> 备份。</p>')) {
        try {
          await api.put('/api/info', { ...payload, force: true });
          toast('已覆盖并备份', 'ok');
          await ctx.refresh();
        } catch (e2) {
          toast('保存失败：' + e2.message, 'error');
        }
      } else if (e.status !== 409) {
        toast('保存失败：' + e.message, 'error');
      }
    }
  });

  root.querySelector('#btn-new-skin').addEventListener('click', () => {
    const { modal, close } = ctx.openModal('新建皮肤',
      `<div class="field"><label>皮肤 id（目录名，可中文）</label><input type="text" id="new-skin-id" placeholder="MySkin" /></div>
       <div class="field"><label>显示名称</label><input type="text" id="new-skin-name" placeholder="我的皮肤" /></div>
       <p class="small muted">只创建空壳目录，不会复制 46 张基础图；之后可导入 PNG。</p>`,
      '<button class="btn" data-close>取消</button><button class="btn btn-primary" data-create>创建</button>');
    modal.querySelector('[data-create]').addEventListener('click', async () => {
      const id = modal.querySelector('#new-skin-id').value.trim();
      const displayName = modal.querySelector('#new-skin-name').value.trim();
      if (!id) { toast('请填写皮肤 id', 'error'); return; }
      try {
        await api.post('/api/skins', { id, displayName, previewImage: 'shime1.png' });
        close();
        ctx.state.skin = id;
        toast('皮肤已创建：' + id, 'ok');
        await ctx.refresh();
      } catch (e) {
        toast('创建失败：' + e.message, 'error');
      }
    });
  });

  root.querySelector('#btn-export').addEventListener('click', async () => {
    try {
      const includeStudioFragments = root.querySelector('#chk-shared').checked;
      const blob = await postForBlob('/api/export', { skin: ctx.state.skin, includeStudioFragments });
      ctx.downloadBytes(await blob.arrayBuffer(), 'skin-' + ctx.state.skin + '.shimeji-skin.zip');
      toast('已导出皮肤包', 'ok');
    } catch (e) {
      toast('导出失败：' + e.message, 'error');
    }
  });

  root.querySelector('#file-skin-zip').addEventListener('change', async (e) => {
    const file = (e.target.files || [])[0];
    e.target.value = '';
    if (!file) return;
    try {
      const zipBase64 = await ctx.fileToBase64(file);
      const analysis = await api.post('/api/import/analyze', { zipBase64 });
      const rows = (analysis.entries || []).map(it =>
        `<tr><td class="mono small">${escapeHtml(it.target)}</td><td class="num">${it.bytes}</td>
         <td>${it.exists ? (it.overwritable ? '<span class="tag warn">将覆盖（设计器文件）</span>' : '<span class="tag err">冲突</span>') : '<span class="tag ok">新增</span>'}</td></tr>`).join('');
      const ok = await ctx.confirmDialog('导入预览',
        `<p class="small">共 ${analysis.entries.length} 个文件，${(analysis.conflicts || []).length} 个已存在。</p>
         <div class="scroll" style="max-height:300px"><table class="tbl"><thead><tr><th>目标路径</th><th>字节</th><th>动作</th></tr></thead><tbody>${rows}</tbody></table></div>
         <p class="small muted">非设计器生成的文件默认不覆盖；设计器文件会在确认后覆盖（写入前自动备份）。</p>`);
      if (!ok) return;
      const result = await api.post('/api/import/skin', { zipBase64, overwriteGenerated: true });
      toast('导入完成：写入 ' + result.written.length + ' 个，跳过 ' + result.skipped.length + ' 个', 'ok');
      await ctx.refresh();
    } catch (err) {
      toast('导入失败：' + err.message, 'error');
    }
  });
}

async function importFiles(files, root, ctx) {
  if (!files.length) return;
  const useGrid = root.querySelector('#chk-grid').checked;
  const payload = {
    skin: ctx.state.skin,
    files: [],
    prefix: root.querySelector('#grid-prefix') ? root.querySelector('#grid-prefix').value.trim() : '',
  };
  if (useGrid) {
    payload.grid = {
      rows: Number(root.querySelector('#grid-rows').value) || 1,
      cols: Number(root.querySelector('#grid-cols').value) || 1,
    };
  }
  try {
    for (const file of files) {
      payload.files.push({ name: file.name, base64: await ctx.fileToBase64(file) });
    }
    const result = await api.post('/api/assets/import', payload);
    toast('已导入 ' + result.written.length + ' 张' + (result.warnings.length ? '（' + result.warnings.length + ' 条提示）' : ''), 'ok');
    if (result.warnings.length) {
      ctx.openModal('导入提示', '<ul class="small">' + result.warnings.map(w => '<li>' + escapeHtml(w) + '</li>').join('') + '</ul>');
    }
    await ctx.refresh();
  } catch (e) {
    toast('导入失败：' + e.message, 'error');
  }
}
