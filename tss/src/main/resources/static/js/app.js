'use strict';
/* Navegación, módulos, tiempo real (SSE) y arranque. */
const TABS = [
  ['resumen', 'Resumen', vistaResumen, true], ['frecuencias', 'Frecuencias', vistaFrecuencias, true],
  ['numericas', 'Numéricas', vistaNumericas, true], ['likert', 'Valoraciones', vistaLikert, true],
  ['chi2', 'Chi cuadrado', vistaChi2, true], ['relaciones', 'Relaciones', vistaRelaciones, true],
  ['informe', 'Análisis automático', vistaInforme, true], ['captura', 'Registrar', vistaCaptura, false],
  ['datos', 'Datos', vistaDatos, true], ['excel', 'Excel y demo', vistaExcel, false]
];
let renderId = 0;

function pintarTabs() {
  $('#tabs').innerHTML = TABS.map(t => `<button data-t="${t[0]}" class="${t[0] === estado.tab ? 'activo' : ''}">${t[1]}</button>`).join('');
  $$('#tabs button').forEach(b => b.onclick = () => irTab(b.dataset.t));
  $$('#modulos button').forEach(b => b.classList.toggle('activo', b.dataset.m === estado.modulo));
}
function irTab(t) { estado.tab = t; pintarTabs(); render(); }

async function render(silencioso = false) {
  const mi = ++renderId;
  const tab = TABS.find(t => t[0] === estado.tab);
  const scroll = window.scrollY;
  if (!silencioso) V().innerHTML = '<p class="cargando">Cargando…</p>';
  try {
    limpiarCharts();
    await tab[2]();
    if (silencioso) window.scrollTo(0, scroll);
  } catch (e) {
    if (mi !== renderId) return;
    V().innerHTML = `<div class="msg err"><b>No se pudo cargar esta vista.</b><br>${esc(e.message)}<br><br>
      Verifique que el servidor esté en marcha y la base de datos accesible. <button class="btn sec" onclick="render()">Reintentar</button></div>`;
  }
}

async function contadores() {
  for (const m of ['titulados', 'empleadores']) {
    try { $('#cnt-' + m).textContent = (await api(`/${m}/estadisticas/resumen`)).totalRegistros; } catch (e) { $('#cnt-' + m).textContent = '?'; }
  }
}

/* ---- tiempo real: ante cualquier cambio se recalculan los gráficos visibles ---- */
let temporizador = null;
function conectarEventos() {
  const live = $('#live'), txt = $('#live span');
  const es = new EventSource(BASE + '/api/eventos');
  es.onopen = () => { live.className = 'live on'; txt.textContent = 'en vivo'; };
  es.onerror = () => { live.className = 'live off'; txt.textContent = 'reconectando…'; };
  es.addEventListener('datos', ev => {
    clearTimeout(temporizador);
    temporizador = setTimeout(() => {
      contadores();
      const tab = TABS.find(t => t[0] === estado.tab);
      let d = {}; try { d = JSON.parse(ev.data); } catch (e) { /* ignorar */ }
      if (tab[3] && (!d.modulo || d.modulo === estado.modulo)) render(true);
    }, 450);
  });
}

function iniciar() {
  Chart.defaults.color = getComputedStyle(document.body).color;
  Chart.defaults.borderColor = getComputedStyle(document.body).getPropertyValue('--bor').trim() || '#e5e7eb';
  Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
  $$('#modulos button').forEach(b => b.onclick = () => { estado.modulo = b.dataset.m; estado.a = estado.b = estado.x = estado.y = estado.num = estado.grp = estado.bondad = null; pintarTabs(); render(); });
  pintarTabs();
  render();
  contadores();
  conectarEventos();
}
window.addEventListener('DOMContentLoaded', () => {
  if (typeof Chart === 'undefined') {
    V().innerHTML = '<div class="msg err">No se pudo cargar Chart.js desde internet (cdn.jsdelivr.net). Verifique su conexión y recargue.</div>';
    return;
  }
  iniciar();
});
