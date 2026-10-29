'use strict';
/* Constructores de configuraciones Chart.js (y SVG para el boxplot). */
const opcionesBase = {responsive: true, maintainAspectRatio: false};

/* ---------- frecuencias ---------- */
const TIPOS_FREC = {bar: 'Barras', barh: 'Barras horizontales', doughnut: 'Dona', pie: 'Pastel', polarArea: 'Área polar', line: 'Líneas', pareto: 'Pareto'};
function tipoInicial(d) { return (!d.multiple && d.categorias.length <= 5) ? 'doughnut' : 'barh'; }

function cfgFrecuencia(d, tipo) {
  const cats = d.categorias;
  const full = cats.map(c => c.valor), labels = full.map(v => corto(v));
  const vals = cats.map(c => c.fi);
  const tip = {callbacks: {title: it => full[it[0].dataIndex], label: c => `${c.dataset.label || ''} ${c.parsed.y ?? c.parsed.r ?? c.parsed}${c.dataset.type === 'line' && c.dataset.yAxisID === 'p' ? '%' : ''}`.trim()}};
  if (['doughnut', 'pie', 'polarArea'].includes(tipo)) {
    return {type: tipo, data: {labels, datasets: [{data: vals, backgroundColor: labels.map((_, i) => color(i))}]},
      options: {...opcionesBase, plugins: {legend: {position: 'right', labels: {boxWidth: 10}}, tooltip: {callbacks: {title: it => full[it[0].dataIndex], label: c => `${c.parsed.r ?? c.parsed} (${cats[c.dataIndex].porcentaje}%)`}}}}};
  }
  if (tipo === 'pareto') {
    const orden = cats.map((c, i) => i).sort((a, b) => vals[b] - vals[a]);
    const tot = vals.reduce((a, b) => a + b, 0) || 1;
    let ac = 0;
    const acum = orden.map(i => { ac += vals[i]; return +(ac * 100 / tot).toFixed(1); });
    return {type: 'bar', data: {labels: orden.map(i => labels[i]), datasets: [
      {label: 'Frecuencia', data: orden.map(i => vals[i]), backgroundColor: color(0), yAxisID: 'y'},
      {type: 'line', label: '% acumulado', data: acum, borderColor: color(3), yAxisID: 'p', tension: .2}]},
      options: {...opcionesBase, scales: {y: {beginAtZero: true, ticks: {precision: 0}}, p: {position: 'right', min: 0, max: 100, grid: {drawOnChartArea: false}}},
        plugins: {tooltip: {callbacks: {title: it => full[orden[it[0].dataIndex]]}}}}};
  }
  const horiz = tipo === 'barh';
  const ds = {label: d.multiple ? 'Respondentes' : 'Frecuencia', data: vals, backgroundColor: tipo === 'line' ? alfaC(color(0), .2) : labels.map((_, i) => color(i)), borderColor: color(0), fill: tipo === 'line', tension: .25};
  return {type: tipo === 'line' ? 'line' : 'bar', data: {labels, datasets: [ds]},
    options: {...opcionesBase, indexAxis: horiz ? 'y' : 'x', plugins: {legend: {display: false}, tooltip: {callbacks: {title: it => full[it[0].dataIndex], label: c => `${c.parsed[horiz ? 'x' : 'y']} (${cats[c.dataIndex].porcentaje}%)`}}},
      scales: {[horiz ? 'x' : 'y']: {beginAtZero: true, ticks: {precision: 0}}}}};
}

/* ---------- numéricas ---------- */
function cfgHistograma(d) {
  return {type: 'bar', data: {labels: d.intervalos.map(i => i.intervalo), datasets: [{label: 'Frecuencia', data: d.intervalos.map(i => i.fi), backgroundColor: alfaC(color(0), .75), borderColor: color(0), borderWidth: 1, barPercentage: 1, categoryPercentage: 1}]},
    options: {...opcionesBase, plugins: {legend: {display: false}}, scales: {y: {beginAtZero: true, ticks: {precision: 0}, title: {display: true, text: 'fi'}}, x: {title: {display: true, text: d.etiqueta}}}}};
}
function cfgPoligono(d) {
  const a = d.amplitudIntervalo || 1, iv = d.intervalos;
  const pts = [{x: iv[0].marcaClase - a, y: 0}, ...iv.map(i => ({x: i.marcaClase, y: i.fi})), {x: iv[iv.length - 1].marcaClase + a, y: 0}];
  return {type: 'line', data: {datasets: [{label: 'Polígono de frecuencias', data: pts, borderColor: color(1), backgroundColor: alfaC(color(1), .15), fill: true, tension: 0, pointRadius: 4}]},
    options: {...opcionesBase, plugins: {legend: {display: false}}, scales: {x: {type: 'linear', title: {display: true, text: 'Marca de clase'}}, y: {beginAtZero: true, ticks: {precision: 0}}}}};
}
function cfgOjiva(d) {
  const iv = d.intervalos;
  const pts = [{x: iv[0].limiteInferior, y: 0}, ...iv.map(i => ({x: i.limiteSuperior, y: i.Fi}))];
  return {type: 'line', data: {datasets: [{label: 'Frecuencia acumulada', data: pts, borderColor: color(2), backgroundColor: alfaC(color(2), .15), fill: true, pointRadius: 4}]},
    options: {...opcionesBase, plugins: {legend: {display: false}}, scales: {x: {type: 'linear', title: {display: true, text: 'Límite superior'}}, y: {beginAtZero: true, ticks: {precision: 0}, title: {display: true, text: 'Fi'}}}}};
}
function lineaH(label, y, x1, x2, dash, col) {
  return {type: 'line', label, data: [{x: x1, y}, {x: x2, y}], borderColor: col, borderDash: dash, pointRadius: 0, borderWidth: 2, fill: false};
}
/* "¿Dónde caen los puntos respecto a la media?": valor de cada registro con la media y bandas de ±1σ, ±2σ */
function cfgPuntos(d) {
  const n = Math.max(d.puntos.length, 2), b = d.bandas;
  return {type: 'scatter', data: {datasets: [
    {label: 'Registros', data: d.puntos.map(p => ({x: p.i, y: p.valor})), pointRadius: 5,
      backgroundColor: d.puntos.map(p => Math.abs(p.z) > 2 ? '#E45756' : Math.abs(p.z) > 1 ? '#F58518' : '#4C78A8')},
    lineaH('Media', b.media, 1, n, [], '#54A24B'),
    lineaH('+1σ', b.mas1sd, 1, n, [6, 4], '#9D755D'), lineaH('−1σ', b.menos1sd, 1, n, [6, 4], '#9D755D'),
    lineaH('+2σ', b.mas2sd, 1, n, [2, 3], '#B279A2'), lineaH('−2σ', b.menos2sd, 1, n, [2, 3], '#B279A2')]},
    options: {...opcionesBase, scales: {x: {type: 'linear', title: {display: true, text: 'N.º de registro'}}, y: {title: {display: true, text: d.etiqueta}}},
      plugins: {tooltip: {callbacks: {label: c => c.dataset.type === 'line' ? `${c.dataset.label}: ${fmt(c.parsed.y)}` : `Registro ${c.parsed.x}: ${fmt(c.parsed.y)} (z = ${fmt(d.puntos[c.dataIndex].z)})`}}}}};
}
/* Diagrama de puntos sobre el eje del valor, con la media como línea vertical */
function cfgStrip(d) {
  const b = d.bandas;
  const vert = (label, x, dash, col) => ({type: 'line', label, data: [{x, y: 0}, {x, y: 1}], borderColor: col, borderDash: dash, pointRadius: 0, borderWidth: 2});
  return {type: 'scatter', data: {datasets: [
    {label: 'Registros', data: d.puntos.map((p, i) => ({x: p.valor, y: 0.1 + ((i * 37) % 80) / 100})), pointRadius: 6,
      backgroundColor: d.puntos.map(p => alfaC(Math.abs(p.z) > 2 ? '#E45756' : Math.abs(p.z) > 1 ? '#F58518' : '#4C78A8', .75))},
    vert('Media', b.media, [], '#54A24B'), vert('−1σ', b.menos1sd, [6, 4], '#9D755D'), vert('+1σ', b.mas1sd, [6, 4], '#9D755D')]},
    options: {...opcionesBase, scales: {x: {type: 'linear', title: {display: true, text: d.etiqueta}}, y: {display: false, min: 0, max: 1}},
      plugins: {tooltip: {callbacks: {label: c => c.dataset.type === 'line' ? `${c.dataset.label}: ${fmt(c.parsed.x)}` : `Valor: ${fmt(c.parsed.x)}`}}}}};
}
/* Diagrama de caja en SVG */
function boxSvg(d) {
  const b = d.boxplot, W = 620, H = 130, m = 30, lo = b.minimo, hi = b.maximo, rg = (hi - lo) || 1;
  const X = v => m + (v - lo) / rg * (W - 2 * m), y0 = 40, y1 = 80, ym = 60;
  let s = `<svg class="box" viewBox="0 0 ${W} ${H}" width="100%" role="img" aria-label="Diagrama de caja de ${esc(d.etiqueta)}">`;
  for (let i = 0; i <= 5; i++) {
    const v = lo + rg * i / 5;
    s += `<line x1="${X(v)}" x2="${X(v)}" y1="100" y2="104" stroke="#888"/><text x="${X(v)}" y="118" text-anchor="middle">${fmt(v, 1)}</text>`;
  }
  s += `<line x1="${m}" x2="${W - m}" y1="100" y2="100" stroke="#888"/>`;
  s += `<line x1="${X(b.whiskerInferior)}" x2="${X(b.q1)}" y1="${ym}" y2="${ym}" stroke="#4C78A8" stroke-width="2"/>`;
  s += `<line x1="${X(b.q3)}" x2="${X(b.whiskerSuperior)}" y1="${ym}" y2="${ym}" stroke="#4C78A8" stroke-width="2"/>`;
  s += `<line x1="${X(b.whiskerInferior)}" x2="${X(b.whiskerInferior)}" y1="50" y2="70" stroke="#4C78A8" stroke-width="2"/>`;
  s += `<line x1="${X(b.whiskerSuperior)}" x2="${X(b.whiskerSuperior)}" y1="50" y2="70" stroke="#4C78A8" stroke-width="2"/>`;
  s += `<rect x="${X(b.q1)}" y="${y0}" width="${Math.max(2, X(b.q3) - X(b.q1))}" height="${y1 - y0}" fill="#4C78A855" stroke="#4C78A8" stroke-width="2"/>`;
  s += `<line x1="${X(b.mediana)}" x2="${X(b.mediana)}" y1="${y0}" y2="${y1}" stroke="#E45756" stroke-width="3"/>`;
  const mx = X(d.media);
  s += `<polygon points="${mx},${ym - 7} ${mx + 7},${ym} ${mx},${ym + 7} ${mx - 7},${ym}" fill="#54A24B"/>`;
  (b.atipicos || []).forEach(v => { s += `<circle cx="${X(v)}" cy="${ym}" r="5" fill="none" stroke="#E45756" stroke-width="2"/>`; });
  s += `<text x="${X(b.q1)}" y="32" text-anchor="middle">Q1 ${fmt(b.q1)}</text><text x="${X(b.mediana)}" y="22" text-anchor="middle">Mediana ${fmt(b.mediana)}</text><text x="${X(b.q3)}" y="32" text-anchor="middle">Q3 ${fmt(b.q3)}</text>`;
  s += `<text x="${mx}" y="92" text-anchor="middle" style="fill:#54A24B">◆ media ${fmt(d.media)}</text></svg>`;
  return s;
}

/* ---------- χ² ---------- */
function lnGamma(z) {
  const c = [76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5];
  let y = z, t = z + 5.5; t -= (z + .5) * Math.log(t);
  let s = 1.000000000190015;
  for (const cj of c) s += cj / ++y;
  return -t + Math.log(2.5066282746310005 * s / z);
}
const pdfChi2 = (x, k) => x <= 0 ? 0 : Math.exp((k / 2 - 1) * Math.log(x) - x / 2 - (k / 2) * Math.log(2) - lnGamma(k / 2));
function cfgDensidadChi2(r) {
  const gl = r.gl, xmax = Math.max(r.chi2 * 1.4, r.chi2Critico * 1.6, gl + 5 * Math.sqrt(2 * gl), 6);
  const pts = [];
  for (let i = 1; i <= 160; i++) { const x = xmax * i / 160; pts.push({x, y: pdfChi2(x, gl)}); }
  const ymax = Math.max(...pts.filter(p => p.x > xmax * .06).map(p => p.y)) * 1.15;
  const clip = pts.map(p => ({x: p.x, y: Math.min(p.y, ymax)}));
  return {type: 'line', data: {datasets: [
    {label: `Densidad χ² (gl=${gl})`, data: clip, borderColor: color(0), pointRadius: 0, fill: false},
    {label: `Región de rechazo (x ≥ ${fmt(r.chi2Critico)})`, data: clip.filter(p => p.x >= r.chi2Critico), borderColor: color(3), backgroundColor: alfaC(color(3), .35), pointRadius: 0, fill: 'origin'},
    {label: `χ² calculado = ${fmt(r.chi2)}`, data: [{x: r.chi2, y: 0}, {x: r.chi2, y: ymax}], borderColor: '#111827', borderDash: [6, 4], pointRadius: 0}]},
    options: {...opcionesBase, scales: {x: {type: 'linear', min: 0, max: xmax, title: {display: true, text: 'χ²'}}, y: {min: 0, max: ymax, ticks: {display: false}}}}};
}
function colResid(r) {
  const t = Math.max(-1, Math.min(1, r / 3));
  return t >= 0 ? `rgba(228,87,86,${(t * .7).toFixed(2)})` : `rgba(76,120,168,${(-t * .7).toFixed(2)})`;
}
