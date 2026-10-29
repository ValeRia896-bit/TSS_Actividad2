'use strict';
/* Una función por pestaña. Cada una pinta #vista y crea sus gráficos con datos al día de la API. */
const estado = {modulo: 'titulados', tab: 'resumen', a: null, b: null, alfa: 0.05, bondad: null, x: null, y: null, num: null, grp: null};
const V = () => document.getElementById('vista');
const M = () => estado.modulo;
const destruir = ids => ids.forEach(id => { if (chartsPorId[id]) { chartsPorId[id].destroy(); delete chartsPorId[id]; } });

function vacioDatos() {
  return vacio('Aún no hay registros en este módulo.',
    `<button class="btn" onclick="demo()">Generar datos de demostración</button>
     <button class="btn sec" onclick="irTab('captura')">Registrar manualmente</button>
     <button class="btn sec" onclick="irTab('excel')">Importar Excel</button>`);
}
async function demo() {
  try { const r = await api(`/${M()}/demo?cantidad=80`, {method: 'POST'}); toast(`${r.generados} registros de demostración creados`); } catch (e) { toast(e.message, 'err'); }
}

/* ======================= RESUMEN ======================= */
async function vistaResumen() {
  const r = await api(`/${M()}/estadisticas/resumen`);
  if (!r.totalRegistros) { V().innerHTML = vacioDatos(); return; }
  const sinDatos = r.campos.filter(c => c.respondidos === 0).length;
  const h = r.campos.length * 24 + 70;
  const o = uid(), c = uid();
  V().innerHTML = `
    <h2>Resumen y calidad de los datos</h2><p class="lead">Se actualiza solo cada vez que entra, cambia o se importa un registro.</p>
    <div class="kpis">
      <div class="kpi"><b>${r.totalRegistros}</b><span>registros</span></div>
      <div class="kpi"><b>${fmt(r.completitudGlobalPct, 1)}%</b><span>completitud global</span></div>
      <div class="kpi"><b>${sinDatos}</b><span>preguntas sin ninguna respuesta</span></div>
      <div class="kpi"><b style="font-size:16px">${r.ultimoRegistro ? new Date(r.ultimoRegistro).toLocaleString() : '—'}</b><span>último registro</span></div>
    </div>
    <div class="grid2">
      <div class="card"><h3>Completitud por pregunta (%)</h3><div style="position:relative;height:${h}px"><canvas id="${c}"></canvas></div>
        <p class="interp">Los campos vacíos no rompen ningún gráfico: simplemente se excluyen del cálculo.</p></div>
      <div class="card"><h3>Origen de los datos</h3><div class="alto s"><canvas id="${o}"></canvas></div></div>
    </div>`;
  const campos = [...r.campos];
  crearChart(c, {type: 'bar', data: {labels: campos.map(x => corto(x.etiqueta, 34)), datasets: [{label: 'Completitud %', data: campos.map(x => x.completitudPct), backgroundColor: campos.map(x => x.completitudPct >= 90 ? '#54A24B' : x.completitudPct >= 50 ? '#EECA3B' : '#E45756')}]},
    options: {...opcionesBase, indexAxis: 'y', plugins: {legend: {display: false}}, scales: {x: {min: 0, max: 100}}}});
  const origen = Object.entries(r.porOrigen);
  crearChart(o, {type: 'doughnut', data: {labels: origen.map(x => x[0]), datasets: [{data: origen.map(x => x[1]), backgroundColor: origen.map((_, i) => color(i))}]}, options: {...opcionesBase, plugins: {legend: {position: 'right'}}}});
}

/* ======================= FRECUENCIAS ======================= */
async function vistaFrecuencias() {
  const lista = await api(`/${M()}/estadisticas/frecuencias`);
  if (!lista.length || lista.every(d => d.n === 0)) { V().innerHTML = vacioDatos(); return; }
  const opts = Object.entries(TIPOS_FREC).map(([k, v]) => `<option value="${k}">${v}</option>`).join('');
  const cards = lista.map(d => {
    const id = uid();
    d._id = id;
    if (d.n === 0) return `<div class="card"><h3>${esc(d.etiqueta)}</h3><p class="lead">Sin respuestas todavía.</p></div>`;
    const t = tipoInicial(d);
    const filas = d.categorias.map(c => [esc(c.valor), c.fi, fmt(c.hi, 3), c.Fi ?? '—', c.Hi != null ? fmt(c.Hi, 3) : '—']);
    return `<div class="card"><h3>${esc(d.etiqueta)}<span class="chip">n=${d.n}</span></h3>
      <div class="barra"><select data-id="${id}">${opts.replace(`value="${t}"`, `value="${t}" selected`)}</select></div>
      <div class="alto"><canvas id="${id}"></canvas></div>${interp(d.interpretacion)}
      <details><summary>Tabla de frecuencias</summary>${tabla(['Categoría', 'fi', 'hi', 'Fi', 'Hi'], filas)}</details></div>`;
  }).join('');
  V().innerHTML = `<h2>Frecuencias</h2><p class="lead">Una gráfica por pregunta. Cambie el tipo de gráfico en cada tarjeta o en todas a la vez.</p>
    <div class="barra"><label>Tipo de gráfico para todas<select id="tipo-global"><option value="">— elegir —</option>${opts}</select></label></div>
    <div class="grid">${cards}</div>`;
  const pintar = (d, tipo) => crearChart(d._id, cfgFrecuencia(d, tipo));
  lista.forEach(d => { if (d.n > 0) pintar(d, tipoInicial(d)); });
  $$('select[data-id]').forEach(s => s.onchange = () => pintar(lista.find(d => d._id === s.dataset.id), s.value));
  $('#tipo-global').onchange = e => { if (!e.target.value) return; lista.forEach(d => { if (d.n > 0) pintar(d, e.target.value); }); $$('select[data-id]').forEach(s => s.value = e.target.value); };
}

/* ======================= NUMÉRICAS ======================= */
async function vistaNumericas() {
  const lista = (await api(`/${M()}/estadisticas/descriptivos`)).filter(d => d.tipo === 'NUMERICO');
  if (!lista.length) { V().innerHTML = vacio('Este módulo no tiene variables numéricas continuas (la encuesta de empleadores es categórica). Vea Frecuencias, χ² y Relaciones.'); return; }
  if (lista.every(d => d.sinDatos)) { V().innerHTML = vacioDatos(); return; }
  const pend = [];
  const bloques = lista.map(d => {
    if (d.sinDatos) return `<div class="card"><h3>${esc(d.etiqueta)}</h3><p class="lead">Sin datos todavía.</p></div>`;
    const ids = {h: uid(), p: uid(), o: uid(), pt: uid(), st: uid()};
    pend.push([d, ids]);
    const kv = [['n', d.n], ['Media', fmt(d.media)], ['Mediana', fmt(d.mediana)], ['Moda', d.moda.length ? d.moda.map(x => fmt(x)).join(', ') : 'sin moda'],
      ['Mínimo / Máximo', `${fmt(d.minimo)} / ${fmt(d.maximo)}`], ['Rango', fmt(d.rango)], ['Varianza', fmt(d.varianza)], ['Desv. estándar', fmt(d.desviacion)],
      ['CV', `${fmt(d.cv)}% (${d.variabilidad})`], ['Q1 / Q2 / Q3', `${fmt(d.q1)} / ${fmt(d.q2)} / ${fmt(d.q3)}`], ['IQR', fmt(d.iqr)], ['P90', fmt(d.percentiles.P90)],
      ['Asimetría', fmt(d.asimetria)], ['IC 95% de la media', d.icMedia95 ? `${fmt(d.icMedia95.inferior)} – ${fmt(d.icMedia95.superior)}` : '—'],
      ['% dentro de ±1σ / ±2σ', `${fmt(d.bandas.pctDentro1sd, 1)}% / ${fmt(d.bandas.pctDentro2sd, 1)}%`]];
    return `<section><h2>${esc(d.etiqueta)}</h2>${interp(d.interpretacion)}
      <div class="grid" style="margin-top:12px">
        <div class="card"><h3>Medidas</h3><div class="scroll"><table class="kv"><tbody>${kv.map(r => `<tr><td>${r[0]}</td><td>${r[1]}</td></tr>`).join('')}</tbody></table></div></div>
        <div class="card"><h3>Diagrama de caja (boxplot)</h3>${boxSvg(d)}<p class="interp">◆ media · línea roja = mediana · círculos = valores atípicos (1.5·IQR). Forma: ${esc(d.formaDistribucion)}.</p></div>
        <div class="card"><h3>Histograma</h3><div class="alto"><canvas id="${ids.h}"></canvas></div></div>
        <div class="card"><h3>Polígono de frecuencias</h3><div class="alto"><canvas id="${ids.p}"></canvas></div></div>
        <div class="card"><h3>Ojiva (frecuencia acumulada)</h3><div class="alto"><canvas id="${ids.o}"></canvas></div></div>
        <div class="card"><h3>¿Dónde caen los puntos respecto a la media?</h3><div class="alto"><canvas id="${ids.pt}"></canvas></div>
          <p class="interp">Azul: dentro de ±1σ · naranja: entre 1σ y 2σ · rojo: fuera de 2σ.</p></div>
        <div class="card"><h3>Diagrama de puntos sobre el eje del valor</h3><div class="alto"><canvas id="${ids.st}"></canvas></div></div>
        <div class="card"><h3>Tablas</h3>
          <details open><summary>Por intervalos (Sturges, amplitud ${fmt(d.amplitudIntervalo)})</summary>${tabla(['Intervalo', 'Marca', 'fi', 'hi', 'Fi'], d.intervalos.map(i => [i.intervalo, fmt(i.marcaClase), i.fi, fmt(i.hi * 100, 1) + '%', i.Fi]))}</details>
          ${d.tablaFrecuencia.length ? `<details><summary>Por valor</summary>${tabla(['xi', 'fi', 'hi', 'Fi', 'Hi'], d.tablaFrecuencia.map(t => [fmt(t.xi), t.fi, fmt(t.hi, 3), t.Fi, fmt(t.Hi, 3)]))}</details>` : ''}
        </div>
      </div></section>`;
  }).join('<hr style="border:0;border-top:1px solid var(--bor);margin:24px 0">');
  V().innerHTML = `<h2>Variables numéricas</h2><p class="lead">Estadística descriptiva completa de las variables cuantitativas.</p>${bloques}`;
  pend.forEach(([d, ids]) => {
    crearChart(ids.h, cfgHistograma(d)); crearChart(ids.p, cfgPoligono(d)); crearChart(ids.o, cfgOjiva(d));
    crearChart(ids.pt, cfgPuntos(d)); crearChart(ids.st, cfgStrip(d));
  });
}

/* ======================= LIKERT ======================= */
async function vistaLikert() {
  const [lk, meta, freq] = await Promise.all([api(`/${M()}/estadisticas/likert`), getMeta(M()), api(`/${M()}/estadisticas/frecuencias`)]);
  const items = lk.items.filter(i => i.n > 0);
  // Grupos de preguntas que comparten las mismas opciones (p. ej. los criterios de los empleadores)
  const grupos = {};
  meta.filter(c => c.tipo === 'CATEGORICO' && c.opciones.length).forEach(c => (grupos[c.opciones.join('|')] ||= []).push(c));
  const comparativos = Object.values(grupos).filter(g => g.length >= 3);
  if (!items.length && !comparativos.length) { V().innerHTML = vacioDatos(); return; }
  const porCampo = Object.fromEntries(freq.map(f => [f.campo, f]));
  let html = '<h2>Escalas de valoración</h2><p class="lead">Medias, dispersión y distribución de las preguntas tipo Likert o de valoración.</p><div class="grid">';
  const pend = [];
  if (items.length) {
    const a = uid(), b = uid(), c = uid();
    html += `<div class="card"><h3>Radar: media como % del máximo de la escala</h3><div class="alto l"><canvas id="${a}"></canvas></div></div>
      <div class="card"><h3>Media por pregunta (± 1 desv. est.)</h3><div class="alto l"><canvas id="${b}"></canvas></div></div>
      <div class="card" style="grid-column:1/-1"><h3>Distribución de respuestas (% por nivel)</h3><div style="position:relative;height:${items.length * 38 + 80}px"><canvas id="${c}"></canvas></div></div>
      <div class="card" style="grid-column:1/-1"><h3>Detalle</h3>${tabla(['Pregunta', 'n', 'Media', 'Desv.', 'Lectura'], items.map(i => [esc(i.etiqueta), i.n, fmt(i.media), fmt(i.desviacion), `<span style="white-space:normal;display:block;text-align:left">${esc(i.interpretacion)}</span>`]))}</div>`;
    pend.push(() => {
      crearChart(a, {type: 'radar', data: {labels: items.map(i => corto(i.etiqueta, 30)), datasets: [{label: 'Media % del máximo', data: items.map(i => +(i.media / i.escalaMax * 100).toFixed(1)), backgroundColor: alfaC(color(0), .25), borderColor: color(0)}]},
        options: {...opcionesBase, scales: {r: {min: 0, max: 100}}, plugins: {legend: {display: false}}}});
      crearChart(b, {type: 'bar', data: {labels: items.map(i => corto(i.etiqueta, 34)), datasets: [
        {label: 'Media', data: items.map(i => i.media), backgroundColor: color(0)},
        {type: 'line', label: 'Media + 1σ', data: items.map(i => +(i.media + i.desviacion).toFixed(2)), showLine: false, pointStyle: 'triangle', borderColor: color(3), backgroundColor: color(3)},
        {type: 'line', label: 'Media − 1σ', data: items.map(i => +(i.media - i.desviacion).toFixed(2)), showLine: false, pointStyle: 'rectRot', borderColor: color(1), backgroundColor: color(1)}]},
        options: {...opcionesBase, indexAxis: 'y', scales: {x: {min: 0, max: Math.max(...items.map(i => i.escalaMax))}}}});
      const maxN = Math.max(...items.map(i => i.escalaMax));
      const cols = ['#E45756', '#F58518', '#EECA3B', '#54A24B'];
      crearChart(c, {type: 'bar', data: {labels: items.map(i => corto(i.etiqueta, 34)), datasets: Array.from({length: maxN}, (_, k) => ({
        label: `Nivel ${k + 1}`, backgroundColor: cols[Math.round(k * 3 / Math.max(1, maxN - 1))],
        data: items.map(i => { const v = Object.values(i.distribucion)[k]; return v == null ? 0 : +(v * 100 / i.n).toFixed(1); })}))},
        options: {...opcionesBase, indexAxis: 'y', scales: {x: {stacked: true, max: 100}, y: {stacked: true}},
          plugins: {tooltip: {callbacks: {title: it => items[it[0].dataIndex].etiqueta, label: c => { const k = c.datasetIndex; const niv = Object.keys(items[c.dataIndex].distribucion)[k]; return `${niv ?? 'Nivel ' + (k + 1)}: ${c.parsed.x}%`; }}}}}});
    });
  }
  comparativos.forEach(g => {
    const id = uid(), ops = g[0].opciones, activos = g.filter(c => porCampo[c.nombre] && porCampo[c.nombre].n > 0);
    if (!activos.length) return;
    html += `<div class="card" style="grid-column:1/-1"><h3>Comparativo: ${activos.length} preguntas con las mismas opciones (% de respuestas)</h3><div style="position:relative;height:${activos.length * 38 + 90}px"><canvas id="${id}"></canvas></div></div>`;
    pend.push(() => crearChart(id, {type: 'bar', data: {labels: activos.map(c => corto(c.etiqueta, 40)), datasets: ops.map((o, k) => ({label: o, backgroundColor: color(k),
      data: activos.map(c => { const cat = porCampo[c.nombre].categorias.find(x => x.valor === o); return cat ? cat.porcentaje : 0; })}))},
      options: {...opcionesBase, indexAxis: 'y', scales: {x: {stacked: true, max: 100}, y: {stacked: true}}}}));
  });
  V().innerHTML = html + '</div>';
  pend.forEach(f => f());
}

/* ======================= χ² ======================= */
async function vistaChi2() {
  const meta = await getMeta(M());
  const cats = meta.filter(c => ['CATEGORICO', 'BOOLEANO', 'LIKERT'].includes(c.tipo));
  const multi = meta.filter(c => ['CATEGORICO', 'BOOLEANO', 'LIKERT', 'MULTIPLE'].includes(c.tipo));
  if (cats.length < 2) { V().innerHTML = vacio('No hay suficientes variables categóricas.'); return; }
  if (!cats.find(c => c.nombre === estado.a)) estado.a = cats[0].nombre;
  if (!cats.find(c => c.nombre === estado.b) || estado.b === estado.a) estado.b = (cats.find(c => c.nombre !== estado.a) || cats[1]).nombre;
  if (!multi.find(c => c.nombre === estado.bondad)) estado.bondad = multi[0].nombre;
  const sel = (id, lista, val) => `<select id="${id}">${lista.map(c => `<option value="${c.nombre}" ${c.nombre === val ? 'selected' : ''}>${esc(corto(c.etiqueta, 60))}</option>`).join('')}</select>`;
  V().innerHTML = `<h2>Chi cuadrado</h2><p class="lead">Prueba de independencia entre dos preguntas y bondad de ajuste de una pregunta.</p>
    <div class="card"><h3>χ² de independencia</h3>
      <div class="barra"><label>Variable A (filas)${sel('chi-a', cats, estado.a)}</label><label>Variable B (columnas)${sel('chi-b', cats, estado.b)}</label>
      <label>Nivel de significancia α<select id="chi-alfa">${[0.01, 0.05, 0.10].map(a => `<option ${a === estado.alfa ? 'selected' : ''}>${a}</option>`).join('')}</select></label></div>
      <div id="chi-res"><p class="cargando">Calculando…</p></div></div>
    <div class="card" style="margin-top:16px"><h3>χ² de bondad de ajuste (frente a un reparto uniforme)</h3>
      <div class="barra"><label>Pregunta${sel('bon-c', multi, estado.bondad)}</label></div><div id="bon-res"><p class="cargando">Calculando…</p></div></div>`;
  $('#chi-a').onchange = e => { estado.a = e.target.value; correrChi2(); };
  $('#chi-b').onchange = e => { estado.b = e.target.value; correrChi2(); };
  $('#chi-alfa').onchange = e => { estado.alfa = parseFloat(e.target.value); correrChi2(); };
  $('#bon-c').onchange = e => { estado.bondad = e.target.value; correrBondad(); };
  await Promise.all([correrChi2(), correrBondad()]);
}
function matriz(r, mat, f, heat) {
  const cab = ['', ...r.columnas.map(c => corto(c, 22)), 'Total'];
  const filas = r.filas.map((fl, i) => [esc(corto(fl, 30)), ...mat[i].map((v, j) => `<span style="display:block;padding:0 4px;${heat ? `background:${colResid(heat[i][j])}` : ''}">${f(v)}</span>`), r.totalesFila[i]]);
  filas.push(['Total', ...r.totalesColumna, r.n]);
  return tabla(cab, filas);
}
async function correrChi2() {
  const dest = $('#chi-res');
  destruir(['chi-stack', 'chi-pct', 'chi-dens']);
  try {
    const r = await api(`/${M()}/estadisticas/chi2?a=${estado.a}&b=${estado.b}&alfa=${estado.alfa}`);
    if (!r.aplicable) { dest.innerHTML = `<div class="msg info">${esc(r.motivo)}</div>`; return; }
    dest.innerHTML = `
      <div class="kpis"><div class="kpi"><b>${fmt(r.chi2)}</b><span>χ² calculado</span></div><div class="kpi"><b>${fmt(r.chi2Critico)}</b><span>valor crítico (gl=${r.gl}, α=${r.alfa})</span></div>
        <div class="kpi"><b>${r.p < 0.0001 ? '< 0.0001' : fmt(r.p, 4)}</b><span>valor p</span></div><div class="kpi"><b>${fmt(r.cramerV, 3)}</b><span>V de Cramér</span></div>
        <div class="kpi"><b style="color:var(${r.rechazaH0 ? '--ok' : '--warn'})">${r.rechazaH0 ? 'Se rechaza H0' : 'No se rechaza H0'}</b><span>${r.rechazaH0 ? 'hay asociación' : 'independientes'}</span></div></div>
      ${interp(r.interpretacion)}
      <p style="font-size:13px;color:var(--mut)"><b>H0:</b> ${esc(r.hipotesis.H0)} · <b>H1:</b> ${esc(r.hipotesis.H1)}</p>
      <div class="grid2" style="margin-top:12px">
        <div class="card"><h3>Frecuencias observadas</h3><div class="alto"><canvas id="chi-stack"></canvas></div></div>
        <div class="card"><h3>Distribución porcentual por fila</h3><div class="alto"><canvas id="chi-pct"></canvas></div></div>
        <div class="card"><h3>Región de rechazo</h3><div class="alto"><canvas id="chi-dens"></canvas></div></div>
        <div class="card"><h3>Residuos estandarizados</h3>${matriz(r, r.residuosEstandarizados, v => fmt(v), r.residuosEstandarizados)}<p class="interp">Rojo: hay más casos de los esperados · azul: menos. |residuo| &gt; 2 señala celdas que explican la asociación.</p></div>
        <div class="card"><h3>Observado</h3>${matriz(r, r.observado, v => v)}</div>
        <div class="card"><h3>Esperado (si fueran independientes)</h3>${matriz(r, r.esperado, v => fmt(v))}<p class="interp">${r.celdasEsperadoMenor5} celda(s) con esperado &lt; 5 (${fmt(r.pctCeldasBajas, 1)}%). ${r.fiable ? 'La prueba es fiable.' : 'Prueba poco fiable: agrupe categorías o reúna más datos.'}</p></div>
      </div>`;
    crearChart('chi-stack', {type: 'bar', data: {labels: r.filas.map(f => corto(f, 28)), datasets: r.columnas.map((c, j) => ({label: corto(c, 30), data: r.observado.map(f => f[j]), backgroundColor: color(j)}))}, options: {...opcionesBase, scales: {x: {stacked: true}, y: {stacked: true, ticks: {precision: 0}}}}});
    crearChart('chi-pct', {type: 'bar', data: {labels: r.filas.map(f => corto(f, 28)), datasets: r.columnas.map((c, j) => ({label: corto(c, 30), data: r.observado.map((f, i) => +(f[j] * 100 / (r.totalesFila[i] || 1)).toFixed(1)), backgroundColor: color(j)}))}, options: {...opcionesBase, scales: {x: {stacked: true}, y: {stacked: true, max: 100}}}});
    crearChart('chi-dens', cfgDensidadChi2(r));
  } catch (e) { dest.innerHTML = `<div class="msg err">${esc(e.message)}</div>`; }
}
async function correrBondad() {
  const dest = $('#bon-res');
  destruir(['bon-chart']);
  try {
    const r = await api(`/${M()}/estadisticas/chi2-bondad?campo=${estado.bondad}&alfa=${estado.alfa}`);
    if (!r.aplicable) { dest.innerHTML = `<div class="msg info">${esc(r.motivo)}</div>`; return; }
    dest.innerHTML = `<div class="kpis"><div class="kpi"><b>${fmt(r.chi2)}</b><span>χ² (gl=${r.gl})</span></div><div class="kpi"><b>${fmt(r.chi2Critico)}</b><span>valor crítico</span></div>
      <div class="kpi"><b>${r.p < 0.0001 ? '< 0.0001' : fmt(r.p, 4)}</b><span>valor p</span></div></div>${interp(r.interpretacion)}
      <div class="alto" style="margin-top:12px"><canvas id="bon-chart"></canvas></div>`;
    crearChart('bon-chart', {type: 'bar', data: {labels: r.categorias.map(c => corto(c, 30)), datasets: [
      {label: 'Observado', data: r.observado, backgroundColor: color(0)},
      {type: 'line', label: 'Esperado (uniforme)', data: r.esperado, borderColor: color(3), borderDash: [6, 4], pointRadius: 3}]}, options: {...opcionesBase, scales: {y: {beginAtZero: true}}}});
  } catch (e) { dest.innerHTML = `<div class="msg err">${esc(e.message)}</div>`; }
}
function irChi2(a, b) { estado.a = a; estado.b = b; irTab('chi2'); }

/* ======================= RELACIONES ======================= */
async function vistaRelaciones() {
  const [aso, meta] = await Promise.all([api(`/${M()}/estadisticas/asociaciones`), getMeta(M())]);
  const nums = meta.filter(c => c.tipo === 'NUMERICO' || c.tipo === 'LIKERT');
  const cats = meta.filter(c => ['CATEGORICO', 'BOOLEANO', 'LIKERT'].includes(c.tipo));
  const sel = (id, lista, val) => `<select id="${id}">${lista.map(c => `<option value="${c.nombre}" ${c.nombre === val ? 'selected' : ''}>${esc(corto(c.etiqueta, 55))}</option>`).join('')}</select>`;
  // --- mapa de calor de asociaciones
  let heat = vacio('Se necesitan más datos con respuestas variadas para calcular asociaciones.');
  if (aso.pares.length) {
    const mapa = {};
    aso.pares.forEach(p => { mapa[p.a + '|' + p.b] = p; mapa[p.b + '|' + p.a] = p; });
    const cs = aso.campos;
    heat = `<div class="scroll" style="max-height:none"><table class="heat"><thead><tr><th></th>${cs.map((c, i) => `<th class="v" title="${esc(c.etiqueta)}"><span>${i + 1}. ${esc(corto(c.etiqueta, 26))}</span></th>`).join('')}</tr></thead><tbody>` +
      cs.map((fa, i) => `<tr><th class="f" title="${esc(fa.etiqueta)}">${i + 1}. ${esc(corto(fa.etiqueta, 34))}</th>${cs.map((fb, j) => {
        if (i === j) return '<td style="background:var(--bor)"></td>';
        const p = mapa[fa.nombre + '|' + fb.nombre];
        if (!p) return '<td class="n" style="color:var(--mut)">·</td>';
        const v = p.cramerV || 0;
        return `<td class="n ${p.significativa ? 'sig' : ''}" style="background:rgba(37,99,235,${Math.min(1, v * 1.6).toFixed(2)});opacity:${p.fiable ? 1 : .55}" title="${esc(p.etiquetaA)} × ${esc(p.etiquetaB)}&#10;V=${fmt(p.cramerV, 3)} · p=${p.p < 0.0001 ? '<0.0001' : fmt(p.p, 4)} · n=${p.n}${p.fiable ? '' : ' (poco fiable)'}" onclick="irChi2('${fa.nombre}','${fb.nombre}')">${fmt(v, 2).replace('0.', '.')}</td>`;
      }).join('')}</tr>`).join('') + '</tbody></table></div>';
  }
  const top = aso.pares.slice(0, 15).map(p => [esc(p.etiquetaA), esc(p.etiquetaB), p.n, fmt(p.chi2), p.p < 0.0001 ? '&lt; 0.0001' : fmt(p.p, 4), fmt(p.cramerV, 3), p.significativa ? (p.fiable ? '✔' : '✔ (poco fiable)') : '—',
    `<button class="btn sec" onclick="irChi2('${p.a}','${p.b}')">ver</button>`]);
  V().innerHTML = `<h2>Relaciones entre variables</h2><p class="lead">Mapa de asociaciones χ² de todas las preguntas, nubes de dispersión con correlación y comparación de medias por grupo.</p>
    <div class="card"><h3>Mapa de calor: V de Cramér entre todas las preguntas categóricas</h3>${heat}
      <p class="interp">Más azul = asociación más fuerte. Borde = significativa (α=${aso.alfa}). Opaco = prueba poco fiable (esperados &lt; 5). Clic en una celda para abrir su χ².</p></div>
    ${top.length ? `<div class="card" style="margin-top:16px"><h3>15 asociaciones con menor valor p</h3>${tabla(['Variable A', 'Variable B', 'n', 'χ²', 'p', 'V', 'Signif.', ''], top)}</div>` : ''}
    <div class="card" style="margin-top:16px"><h3>Dispersión y correlación de Pearson</h3>
      ${nums.length >= 2 ? `<div class="barra"><label>Eje X${sel('dsp-x', nums, estado.x || nums[0].nombre)}</label><label>Eje Y${sel('dsp-y', nums, estado.y || nums[1].nombre)}</label></div><div id="dsp-res"></div>`
        : vacio('Se necesitan al menos dos variables numéricas o Likert (el módulo de empleadores es categórico).')}</div>
    <div class="card" style="margin-top:16px"><h3>Media por grupos + ANOVA</h3>
      ${nums.length && cats.length ? `<div class="barra"><label>Variable numérica${sel('grp-n', nums, estado.num || nums[0].nombre)}</label><label>Agrupar por${sel('grp-g', cats, estado.grp || cats[0].nombre)}</label></div><div id="grp-res"></div>`
        : vacio('No hay variables numéricas para comparar.')}</div>`;
  if (nums.length >= 2) {
    estado.x = $('#dsp-x').value; estado.y = $('#dsp-y').value === estado.x && nums[1] ? nums.find(c => c.nombre !== estado.x).nombre : $('#dsp-y').value;
    $('#dsp-y').value = estado.y;
    $('#dsp-x').onchange = e => { estado.x = e.target.value; correrDispersion(nums); };
    $('#dsp-y').onchange = e => { estado.y = e.target.value; correrDispersion(nums); };
    correrDispersion(nums);
  }
  if (nums.length && cats.length) {
    estado.num = $('#grp-n').value; estado.grp = $('#grp-g').value;
    $('#grp-n').onchange = e => { estado.num = e.target.value; correrGrupos(); };
    $('#grp-g').onchange = e => { estado.grp = e.target.value; correrGrupos(); };
    correrGrupos();
  }
}
async function correrDispersion(nums) {
  const dest = $('#dsp-res');
  destruir(['dsp-chart']);
  try {
    const r = await api(`/${M()}/estadisticas/dispersion?x=${estado.x}&y=${estado.y}`);
    if (!r.puntos.length) { dest.innerHTML = '<div class="msg info">Aún no hay registros con ambas variables respondidas.</div>'; return; }
    dest.innerHTML = `${interp(r.interpretacion)}<div class="alto l" style="margin-top:10px"><canvas id="dsp-chart"></canvas></div>`;
    const esLik = n => (nums.find(c => c.nombre === n) || {}).tipo === 'LIKERT';
    const jx = esLik(estado.x) ? .12 : 0, jy = esLik(estado.y) ? .12 : 0;
    const pts = r.puntos.map((p, i) => ({x: p.x + jx * (((i * 37) % 100) / 100 - .5), y: p.y + jy * (((i * 53) % 100) / 100 - .5)}));
    const xs = r.puntos.map(p => p.x), x1 = Math.min(...xs), x2 = Math.max(...xs);
    const ds = [{label: 'Registros', data: pts, backgroundColor: alfaC(color(0), .7), pointRadius: 5}];
    if (r.aplicable) ds.push({type: 'line', label: `Regresión: y = ${fmt(r.pendiente, 3)}x + ${fmt(r.intercepto, 3)}`, data: [{x: x1, y: r.pendiente * x1 + r.intercepto}, {x: x2, y: r.pendiente * x2 + r.intercepto}], borderColor: color(3), pointRadius: 0});
    crearChart('dsp-chart', {type: 'scatter', data: {datasets: ds}, options: {...opcionesBase, scales: {x: {title: {display: true, text: r.etiquetaX}}, y: {title: {display: true, text: r.etiquetaY}}}}});
  } catch (e) { dest.innerHTML = `<div class="msg err">${esc(e.message)}</div>`; }
}
async function correrGrupos() {
  const dest = $('#grp-res');
  destruir(['grp-chart']);
  try {
    const r = await api(`/${M()}/estadisticas/medias-por-grupo?numerico=${estado.num}&grupo=${estado.grp}`);
    if (!r.grupos.length) { dest.innerHTML = '<div class="msg info">Aún no hay datos para comparar.</div>'; return; }
    dest.innerHTML = `${r.anova ? interp(r.anova.interpretacion) : '<p class="interp">ANOVA no calculable con los datos actuales (se necesitan ≥ 2 grupos y variación).</p>'}
      <div class="alto" style="margin-top:10px"><canvas id="grp-chart"></canvas></div>
      <details><summary>Tabla</summary>${tabla(['Grupo', 'n', 'Media', 'Mediana', 'Desv.', 'Mín', 'Máx'], r.grupos.map(g => [esc(g.grupo), g.n, fmt(g.media), fmt(g.mediana), fmt(g.desviacion), fmt(g.minimo), fmt(g.maximo)]))}</details>`;
    crearChart('grp-chart', {type: 'bar', data: {labels: r.grupos.map(g => corto(g.grupo, 30)), datasets: [
      {label: 'Media', data: r.grupos.map(g => g.media), backgroundColor: r.grupos.map((_, i) => color(i))},
      {type: 'line', label: 'Media + 1σ', data: r.grupos.map(g => +(g.media + g.desviacion).toFixed(2)), showLine: false, pointStyle: 'triangle', borderColor: '#111827', backgroundColor: '#111827'},
      {type: 'line', label: 'Media − 1σ', data: r.grupos.map(g => +(g.media - g.desviacion).toFixed(2)), showLine: false, pointStyle: 'rectRot', borderColor: '#6b7280', backgroundColor: '#6b7280'}]},
      options: {...opcionesBase, scales: {y: {title: {display: true, text: r.etiquetaNumerico}}}}});
  } catch (e) { dest.innerHTML = `<div class="msg err">${esc(e.message)}</div>`; }
}

/* ======================= INFORME ======================= */
async function vistaInforme() {
  const r = await api(`/${M()}/estadisticas/informe`);
  if (!r.totalRegistros) { V().innerHTML = vacioDatos(); return; }
  const nombres = {frecuencia: 'Distribución de respuestas', descriptivo: 'Variables numéricas', likert: 'Valoraciones', asociacion: 'Asociaciones significativas (χ²)'};
  const orden = ['descriptivo', 'frecuencia', 'likert', 'asociacion'];
  V().innerHTML = `<div class="barra"><div><h2 style="margin:0">Análisis automático</h2><p class="lead" style="margin:0">Generado a partir de ${r.totalRegistros} registros · ${new Date(r.generado).toLocaleString()}</p></div>
    <button class="btn sec" style="margin-left:auto" onclick="window.print()">Imprimir / PDF</button></div>` +
    orden.map(t => {
      const hs = r.hallazgos.filter(h => h.tipo === t);
      return hs.length ? `<h3 style="margin-top:20px">${nombres[t]}</h3>` + hs.map(h => `<div class="hallazgo ${t}"><b>${esc(h.titulo)}</b><p>${esc(h.texto)}</p></div>`).join('') : '';
    }).join('');
}

/* ======================= CAPTURA (formulario) ======================= */
function campoHtml(c) {
  const id = 'f_' + c.nombre;
  let ctl;
  switch (c.tipo) {
    case 'CATEGORICO':
      ctl = c.opciones.length ? `<select id="${id}"><option value="">— sin respuesta —</option>${c.opciones.map(o => `<option>${esc(o)}</option>`).join('')}</select>` : `<input type="text" id="${id}">`; break;
    case 'BOOLEANO': ctl = `<select id="${id}"><option value="">— sin respuesta —</option><option value="1">Sí</option><option value="0">No</option></select>`; break;
    case 'LIKERT': ctl = `<select id="${id}"><option value="">— sin respuesta —</option>${c.niveles.map((n, i) => `<option value="${i + 1}">${esc(n)}</option>`).join('')}</select>`; break;
    case 'NUMERICO': ctl = `<input type="number" id="${id}" min="0" step="0.1">`; break;
    case 'MULTIPLE': ctl = `<div class="multi" id="${id}">${c.opciones.map(o => `<label><input type="checkbox" value="${esc(o)}"> ${esc(o)}</label>`).join('')}<input type="text" placeholder="Otro (opcional)" data-otro></div>`; break;
    case 'TEXTO': ctl = `<textarea id="${id}" rows="2"></textarea>`; break;
    default: ctl = `<input type="text" id="${id}">`;
  }
  return `<label class="campo"><span>${esc(c.etiqueta)}</span>${ctl}</label>`;
}
async function vistaCaptura() {
  const meta = await getMeta(M());
  V().innerHTML = `<h2>Registrar una respuesta · ${M() === 'titulados' ? 'Titulado' : 'Empleador'}</h2><p class="lead">Ninguna pregunta es obligatoria; lo que quede vacío se excluye de los cálculos.</p>
    <div class="card"><div class="form" id="form">${meta.map(campoHtml).join('')}</div><div id="form-msg"></div>
    <button class="btn" id="enviar">Guardar respuesta</button> <button class="btn sec" id="limpiar">Limpiar</button></div>`;
  $('#limpiar').onclick = () => vistaCaptura();
  $('#enviar').onclick = async () => {
    const datos = {};
    meta.forEach(c => {
      const el = document.getElementById('f_' + c.nombre);
      if (c.tipo === 'MULTIPLE') {
        const v = [...$$('input[type=checkbox]:checked', el).map(i => i.value), ...($('[data-otro]', el).value.trim() ? [$('[data-otro]', el).value.trim()] : [])];
        if (v.length) datos[c.nombre] = v.join(';');
      } else {
        const v = el.value.trim();
        if (v === '') return;
        datos[c.nombre] = c.tipo === 'BOOLEANO' ? v === '1' : (c.tipo === 'LIKERT' ? parseInt(v, 10) : (c.tipo === 'NUMERICO' ? parseFloat(v) : v));
      }
    });
    if (!Object.keys(datos).length) { $('#form-msg').innerHTML = '<div class="msg err">Responda al menos una pregunta.</div>'; return; }
    try {
      await api(`/${M()}`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(datos)});
      toast('Respuesta guardada');
      vistaCaptura();
    } catch (e) {
      $('#form-msg').innerHTML = `<div class="msg err">${esc(e.message)}${e.detalles ? '<ul>' + e.detalles.map(d => `<li>${esc(d)}</li>`).join('') + '</ul>' : ''}</div>`;
    }
  };
}

/* ======================= DATOS ======================= */
function celda(c, v) {
  if (v == null) return '<span style="color:var(--mut)">—</span>';
  if (c.tipo === 'BOOLEANO') return v ? 'Sí' : 'No';
  if (c.tipo === 'LIKERT') return esc(c.niveles[v - 1] ?? v);
  if (c.tipo === 'MULTIPLE') return esc(corto(String(v).split(';').join(', '), 70));
  return esc(typeof v === 'number' ? fmt(v) : corto(v, 60));
}
async function vistaDatos() {
  const [meta, filas] = await Promise.all([getMeta(M()), api(`/${M()}`)]);
  if (!filas.length) { V().innerHTML = vacioDatos(); return; }
  const ult = [...filas].reverse().slice(0, 300);
  V().innerHTML = `<div class="barra"><div><h2 style="margin:0">Registros (${filas.length})</h2><p class="lead" style="margin:0">Se muestran los 300 más recientes.</p></div>
    <button class="btn sec" style="margin-left:auto" id="csv">Descargar CSV</button><button class="btn rojo" id="vaciar">Vaciar módulo</button></div>
    <div class="card"><div class="scroll" style="max-height:70vh"><table class="datos"><thead><tr><th>#</th><th>Fecha</th><th>Origen</th>${meta.map(c => `<th title="${esc(c.etiqueta)}">${esc(corto(c.etiqueta, 24))}</th>`).join('')}<th></th></tr></thead><tbody>
    ${ult.map(f => `<tr><td>${f.id}</td><td>${f.fechaRegistro ? new Date(f.fechaRegistro).toLocaleString() : ''}</td><td>${esc(f.origenDato)}</td>${meta.map(c => `<td>${celda(c, f[c.nombre])}</td>`).join('')}<td><button class="btn sec" data-del="${f.id}">✕</button></td></tr>`).join('')}
    </tbody></table></div></div>`;
  $$('[data-del]').forEach(b => b.onclick = async () => {
    if (!confirm('¿Borrar el registro ' + b.dataset.del + '?')) return;
    try { await api(`/${M()}/${b.dataset.del}`, {method: 'DELETE'}); } catch (e) { toast(e.message, 'err'); }
  });
  $('#vaciar').onclick = async () => {
    if (!confirm(`¿Borrar TODOS los registros de ${M()}? No se puede deshacer.`)) return;
    try { const r = await api(`/${M()}?confirmar=true`, {method: 'DELETE'}); toast(`${r.eliminados} registros eliminados`); } catch (e) { toast(e.message, 'err'); }
  };
  $('#csv').onclick = () => {
    const q = v => `"${String(v ?? '').replace(/"/g, '""')}"`;
    const lin = [['id', 'fecha', 'origen', ...meta.map(c => c.etiqueta)].map(q).join(',')];
    filas.forEach(f => lin.push([f.id, f.fechaRegistro, f.origenDato, ...meta.map(c => { const v = f[c.nombre]; return c.tipo === 'BOOLEANO' && v != null ? (v ? 'Sí' : 'No') : c.tipo === 'LIKERT' && v != null ? (c.niveles[v - 1] ?? v) : v; })].map(q).join(',')));
    const a = document.createElement('a');
    a.href = URL.createObjectURL(new Blob(['\ufeff' + lin.join('\n')], {type: 'text/csv;charset=utf-8'}));
    a.download = `${M()}.csv`; a.click();
  };
}

/* ======================= EXCEL / DEMO ======================= */
async function vistaExcel() {
  V().innerHTML = `<h2>Importar Excel y datos de prueba</h2><p class="lead">El Excel se lee por el texto de cada pregunta (no por posición): pueden faltar o sobrar columnas y haber celdas vacías.</p>
    <div class="grid2"><div class="card"><h3>Importar Excel (.xlsx / .xls) a ${M()}</h3>
      <div class="barra"><input type="file" id="xl" accept=".xlsx,.xls,.xlsm"><button class="btn" id="xlbtn">Subir e importar</button></div><div id="xlres"></div></div>
    <div class="card"><h3>Datos de demostración</h3><p class="lead">Genera respuestas aleatorias (marcadas como DEMO) para ver todos los gráficos. Se pueden borrar sin tocar los datos reales.</p>
      <div class="barra"><label>Cantidad<input type="number" id="dm-n" value="80" min="1" max="1000"></label><button class="btn" id="dm-go">Generar</button><button class="btn rojo" id="dm-del">Borrar datos demo</button></div></div></div>`;
  $('#dm-go').onclick = async () => { try { const r = await api(`/${M()}/demo?cantidad=${$('#dm-n').value}`, {method: 'POST'}); toast(`${r.generados} registros demo creados`); } catch (e) { toast(e.message, 'err'); } };
  $('#dm-del').onclick = async () => { try { const r = await api(`/${M()}/demo`, {method: 'DELETE'}); toast(`${r.eliminados} registros demo eliminados`); } catch (e) { toast(e.message, 'err'); } };
  $('#xlbtn').onclick = async () => {
    const f = $('#xl').files[0], dest = $('#xlres');
    if (!f) { dest.innerHTML = '<div class="msg err">Elija un archivo Excel.</div>'; return; }
    const fd = new FormData(); fd.append('archivo', f);
    dest.innerHTML = '<p class="cargando">Importando…</p>';
    try {
      const r = await api(`/${M()}/importar`, {method: 'POST', body: fd});
      const lista = (t, a) => a.length ? `<details><summary>${t} (${a.length})</summary><ul>${a.map(x => `<li>${esc(x)}</li>`).join('')}</ul></details>` : '';
      dest.innerHTML = `<div class="kpis"><div class="kpi"><b>${r.importadas}</b><span>importadas</span></div><div class="kpi"><b>${r.rechazadas}</b><span>rechazadas</span></div>
        <div class="kpi"><b>${r.filasLeidas}</b><span>filas leídas</span></div><div class="kpi"><b>${r.filasVacias}</b><span>filas vacías omitidas</span></div></div>
        <div class="msg ${r.importadas ? 'ok' : 'err'}">Hoja «${esc(r.hoja)}», encabezados en la fila ${r.filaEncabezado}. ${r.columnasReconocidas.length} columnas reconocidas.</div>
        ${lista('Columnas reconocidas', r.columnasReconocidas.map(c => `${c.columna} → ${c.etiqueta}`))}${lista('Columnas ignoradas', r.columnasIgnoradas)}
        ${lista(`Advertencias (${r.totalAdvertencias})`, r.advertencias)}${lista(`Errores (${r.totalErrores})`, r.errores)}`;
      if (r.importadas) toast(`${r.importadas} registros importados`);
    } catch (e) { dest.innerHTML = `<div class="msg err">${esc(e.message)}</div>`; }
  };
}
