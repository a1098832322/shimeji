// 统一 API 客户端：错误对象带上 diagnostics，UI 可以直接展示（SD-NFR-6 错误原样透传）
export class ApiError extends Error {
  constructor(message, payload, status) {
    super(message);
    this.payload = payload || {};
    this.status = status || 0;
    this.diagnostics = (payload && payload.diagnostics) || [];
  }
}

async function request(path, options = {}) {
  const opts = { method: options.method || 'GET', headers: {} };
  if (options.json !== undefined) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(options.json);
  }
  const response = await fetch(path, opts);
  const text = await response.text();
  let payload = null;
  try {
    payload = text ? JSON.parse(text) : null;
  } catch (e) {
    throw new ApiError('响应不是合法 JSON：' + text.slice(0, 200), null, response.status);
  }
  if (!response.ok || (payload && payload.ok === false)) {
    throw new ApiError((payload && payload.error) || ('HTTP ' + response.status), payload, response.status);
  }
  return payload ? payload.data : null;
}

export const api = {
  get: (path) => request(path),
  post: (path, json) => request(path, { method: 'POST', json: json || {} }),
  put: (path, json) => request(path, { method: 'PUT', json: json || {} }),
  del: (path) => request(path, { method: 'DELETE' }),
};

export function qs(params) {
  const usable = Object.entries(params || {}).filter(([, v]) => v !== undefined && v !== null && v !== '');
  if (!usable.length) return '';
  return '?' + usable.map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v)).join('&');
}

export function escapeHtml(value) {
  return String(value === undefined || value === null ? '' : value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

export function toast(message, kind = '') {
  const wrap = document.getElementById('toasts');
  const el = document.createElement('div');
  el.className = 'toast ' + kind;
  el.textContent = message;
  wrap.appendChild(el);
  setTimeout(() => el.remove(), kind === 'error' ? 6000 : 3200);
}

export function openModal(title, bodyHtml, footHtml) {
  const backdrop = document.getElementById('modal-backdrop');
  const modal = document.getElementById('modal');
  modal.innerHTML =
    '<div class="modal-head"><h3>' + escapeHtml(title) + '</h3></div>' +
    '<div class="modal-body">' + bodyHtml + '</div>' +
    '<div class="modal-foot">' + (footHtml || '<button class="btn" data-close>关闭</button>') + '</div>';
  backdrop.classList.remove('hidden');
  const close = () => backdrop.classList.add('hidden');
  modal.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', close));
  backdrop.onclick = (e) => { if (e.target === backdrop) close(); };
  return { modal, close };
}

export function confirmDialog(title, html) {
  return new Promise(resolve => {
    const { modal, close } = openModal(title, html,
      '<button class="btn" data-no>取消</button><button class="btn btn-primary" data-yes>继续</button>');
    modal.querySelector('[data-no]').onclick = () => { close(); resolve(false); };
    modal.querySelector('[data-yes]').onclick = () => { close(); resolve(true); };
  });
}

export function downloadBytes(bytes, filename, contentType = 'application/zip') {
  const blob = new Blob([bytes], { type: contentType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result || '');
      resolve(result.slice(result.indexOf(',') + 1));
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

export async function postForBlob(path, json) {
  const response = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(json || {}),
  });
  if (!response.ok) {
    let message = 'HTTP ' + response.status;
    try {
      const payload = await response.json();
      message = payload.error || message;
    } catch (e) { /* ignore */ }
    throw new ApiError(message, null, response.status);
  }
  return response.blob();
}

export function debounce(fn, wait = 250) {
  let timer = null;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}
