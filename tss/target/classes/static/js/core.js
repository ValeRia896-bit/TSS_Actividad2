'use strict';
/* Utilidades comunes: API, formato, toasts, registro de gráficos. */
const BASE = window.API_BASE || '';
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
const fmt = (n, d = 2) => (n == null || Number.isNaN(Number(n))) ? '—' : Number(n).toLocaleString('en-US', {maximumFractionDigits: d});
const corto = (s, n = 28) => { s = String(s); return s.length > n ? s.slice(0, n - 1) + '…' : s; };
let _uid = 0;
const uid = () => 'c' + (++_uid);

const PALETA = ['#4C78A8', '#F58518', '#54A24B', '#E45756', '#72B7B2', '#B279A2', '#FF9DA6', '#9D755D', '#EECA3B', '#BAB0AC'];
const color = i => PALETA[i % PALETA.length];
const alfaC = (hex, a) => hex + Math.round(a * 255).toString(16).padStart(2, '0');

/* ---- gráficos: se destruyen al cambiar de vista o refrescar ---- */
const charts = [];
const chartsPorId = {};
function limpiarCharts() { charts.splice(0).forEach(c => c.destroy()); Object.keys(chartsPorId).forEach(k => delete chartsPorId[k]); }
function crearChart(id, cfg) {
  const el = document.getElementById(id);
  if (!el) return null;
  if (chartsPorId[id]) { chartsPorId[id].destroy(); const i = charts.indexOf(chartsPorId[id]); if (i >= 0) charts.splice(i, 1); }
  const c = new Chart(el, cfg);
  charts.push(c);
  chartsPorId[id] = c;
  return c;
}

/* ---- API ---- */
async function api(ruta, opciones) {
  const r = await fetch(BASE + '/api' + ruta, opciones);
  let j = null;
  try { j = await r.json(); } catch (e) { /* sin cuerpo (204) */ }
  if (!r.ok) {
    const err = new Error((j && j.error) || r.statusText || 'Error');
    err.detalles = j && j.detalles;
    throw err;
  }
  return j;
}
const metaCache = {};
async function getMeta(m) { return metaCache[m] || (metaCache[m] = await api(`/${m}/meta`)); }

function toast(msg, tipo = 'ok') {
  const t = document.createElement('div');
  t.className = 'toast ' + (tipo === 'ok' ? '' : tipo);
  t.textContent = msg;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 4200);
}

/* ---- piezas HTML ---- */
function cardCanvas(titulo, alto = '', extra = '') {
  const id = uid();
  return {id, html: `<div class="card"><h3>${esc(titulo)}</h3><div class="alto ${alto}"><canvas id="${id}"></canvas></div>${extra}</div>`};
}
const interp = t => t ? `<p class="interp">${esc(t)}</p>` : '';
function tabla(cabeceras, filas) {
  return `<div class="scroll"><table><thead><tr>${cabeceras.map(h => `<th>${esc(h)}</th>`).join('')}</tr></thead><tbody>` +
    filas.map(f => `<tr>${f.map(c => `<td>${c}</td>`).join('')}</tr>`).join('') + '</tbody></table></div>';
}
const vacio = (t, btn = '') => `<div class="vacio"><p>${t}</p>${btn}</div>`;
