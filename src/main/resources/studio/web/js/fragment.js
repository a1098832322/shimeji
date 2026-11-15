// 片段 = 通用 XML 树：属性全部保留，设计器只编辑自己认识的字段（SD-NFR-8 未知名往返不丢）
//
// 为什么用通用树而不是强类型模型：Action/Pose/Behavior 的属性集合会随引擎演进，
// 强类型模型会静默丢弃新字段。这里只做「读到什么就写回什么」。

export function el(tag, attrs = {}, children = []) {
  return { tag, attrs: { ...attrs }, children };
}

export function parseXml(text) {
  const doc = new DOMParser().parseFromString(text, 'application/xml');
  const err = doc.querySelector('parsererror');
  if (err) {
    throw new Error('XML 解析失败：' + err.textContent.split('\n')[0]);
  }
  return fromDom(doc.documentElement);
}

function fromDom(node) {
  const attrs = {};
  for (const attr of Array.from(node.attributes || [])) {
    if (attr.name === 'xmlns' || attr.name.startsWith('xmlns:')) continue;
    attrs[attr.name] = attr.value;
  }
  const children = [];
  let text = '';
  for (const child of Array.from(node.childNodes)) {
    if (child.nodeType === 1) children.push(fromDom(child));
    else if (child.nodeType === 3 && child.nodeValue.trim()) text += child.nodeValue;
  }
  return { tag: node.nodeName, attrs, children, text: text.trim() ? text : '' };
}

export function escapeAttr(value) {
  return String(value === undefined || value === null ? '' : value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

export function toXml(node, depth = 0) {
  const pad = '  '.repeat(depth);
  const attrs = Object.entries(node.attrs || {})
    .filter(([, v]) => v !== undefined && v !== null && String(v) !== '')
    .map(([k, v]) => ' ' + k + '="' + escapeAttr(v) + '"')
    .join('');
  if (!node.children || node.children.length === 0) {
    return pad + '<' + node.tag + attrs + ' />';
  }
  const inner = node.children.map(c => toXml(c, depth + 1)).join('\n');
  return pad + '<' + node.tag + attrs + '>\n' + inner + '\n' + pad + '</' + node.tag + '>';
}

/** 从「动作列表」生成规范片段文本。 */
export function buildFragment(kind, roots) {
  const listTag = kind === 'behaviors' ? 'BehaviorList' : 'ActionList';
  const body = roots.map(r => toXml(r, 2)).join('\n');
  return '<?xml version="1.0" encoding="UTF-8" ?>\n' +
    '<!-- generated-by: shimeji-studio v1.0; do not hand-edit -->\n' +
    '<Mascot>\n  <' + listTag + '>\n' + body + '\n  </' + listTag + '>\n</Mascot>\n';
}

export function findFirst(node, tag) {
  return (node.children || []).find(c => c.tag === tag) || null;
}

export function findAll(node, tag) {
  return (node.children || []).filter(c => c.tag === tag);
}

export function attr(node, name, fallback = '') {
  return node && node.attrs && node.attrs[name] !== undefined ? node.attrs[name] : fallback;
}

export function setAttr(node, name, value) {
  if (!node.attrs) node.attrs = {};
  if (value === undefined || value === null || String(value) === '') delete node.attrs[name];
  else node.attrs[name] = String(value);
}

export function actionNodes(root) {
  const list = findFirst(root, 'ActionList');
  return list ? list.children.filter(c => c.tag === 'Action') : [];
}

export function behaviorNodes(root) {
  const list = findFirst(root, 'BehaviorList');
  return list ? list.children.filter(c => c.tag === 'Behavior') : [];
}

/** 新建一个动作节点（只写最小必需属性，其余交给引擎默认值）。 */
export function newAction(name, type) {
  const attrs = { Name: name, Type: type };
  if (type === 'Sequence' || type === 'Select') {
    return el('Action', attrs, []);
  }
  if (type === 'Stay' || type === 'Move' || type === 'Animate') {
    attrs.BorderType = 'Floor';
    attrs.Loop = 'true';
    return el('Action', attrs, []);
  }
  return el('Action', attrs, []);
}

export function newPose(asset, anchorX = 64, anchorY = 128, duration = 250, velocity = '0,0') {
  return el('Pose', {
    Image: '/' + asset,
    ImageAnchor: anchorX + ',' + anchorY,
    Velocity: velocity,
    Duration: String(duration),
  });
}

export function newBehavior(name, actionName, frequency = 0, hidden = true) {
  return el('Behavior', {
    Name: name,
    Action: actionName === name ? undefined : actionName,
    Frequency: String(frequency),
    Hidden: hidden ? 'true' : 'false',
  });
}

/** 判断某个数值字段是否是表达式（${} / #{}）。 */
export function isExpression(value) {
  return typeof value === 'string' && /^[$#]\{.*\}$/.test(value.trim());
}

export function expressionKind(value) {
  if (!isExpression(value)) return 'literal';
  return value.trim().startsWith('#{') ? 'per-frame' : 'cached';
}
