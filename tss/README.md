# Estudio de mercado – Ing. de Sistemas (TSS · Actividad 2) · BACKEND + FRONT

API REST (Spring Boot 3 · Java 17 · PostgreSQL/Neon) con **dos módulos independientes**:
`titulados` (encuesta de opinión a titulados) y `empleadores` (encuesta a empleadores).

## Ejecutar

1. Java 17+ y Maven instalados.
2. La contraseña de la base **no está en el código**. Define la variable de entorno `DB_PASSWORD`:
   - Windows (PowerShell): `$env:DB_PASSWORD="tu-password"; mvn spring-boot:run`
   - Linux/Mac: `DB_PASSWORD=tu-password mvn spring-boot:run`
   - IntelliJ: Run → Edit Configurations → Environment variables → `DB_PASSWORD=...`
   - Si cambias de base: `DB_URL`, `DB_USER` y `DB_PASSWORD`.
3. Hibernate crea las tablas `titulados` y `empleadores` solo (`ddl-auto=update`).
4. Probar sin datos reales: `POST /api/titulados/demo?cantidad=80` y luego `GET /api/titulados/estadisticas/informe`.
   Los datos demo llevan `origenDato=DEMO` y se borran con `DELETE /api/titulados/demo`.

## Front

Está dentro del mismo proyecto (`src/main/resources/static`), así que **no hay nada más que instalar**:
con el servidor en marcha abre **http://localhost:8080**. Usa Chart.js desde internet (cdn.jsdelivr.net).

Pestañas (para cada módulo, Titulados / Empleadores, con el selector de arriba a la derecha):

| Pestaña | Contenido |
|---|---|
| Resumen | Registros, completitud por pregunta (datos faltantes), origen de los datos |
| Frecuencias | Un gráfico por pregunta (barras, barras horizontales, dona, pastel, polar, líneas, Pareto), cambiable por tarjeta o en todas a la vez, con tabla fi/hi/Fi/Hi |
| Numéricas | Medidas completas, boxplot, histograma, polígono, ojiva, **puntos vs. media (±1σ, ±2σ)**, diagrama de puntos, tablas por intervalos |
| Valoraciones | Radar, medias ± σ, distribución apilada de escalas Likert y comparativo de preguntas con las mismas opciones (criterios de empleadores) |
| Chi cuadrado | Independencia entre dos preguntas (barras apiladas, % por fila, curva con región de rechazo, residuos con mapa de color, esperados) y bondad de ajuste |
| Relaciones | Mapa de calor de asociaciones (V de Cramér) de todas las preguntas, dispersión con Pearson y regresión, medias por grupo con ANOVA |
| Análisis automático | Hallazgos en lenguaje natural, imprimible a PDF |
| Registrar | Formulario generado desde la estructura de la encuesta |
| Datos | Tabla, borrado, CSV |
| Excel y demo | Importar Excel con reporte; generar/borrar datos de demostración |

**Tiempo real:** el front escucha `/api/eventos`; cuando entra, cambia, se borra o se importa un registro, recalcula
los gráficos visibles en menos de un segundo (indicador «en vivo» arriba).

## Endpoints (`{modulo}` = `titulados` | `empleadores`)

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/{modulo}/meta` | Campos, tipos, opciones y niveles (para armar formularios y selectores) |
| GET / POST | `/api/{modulo}` | Listar / registrar (validado y normalizado) |
| GET / PUT / DELETE | `/api/{modulo}/{id}` | Ver / editar / borrar |
| DELETE | `/api/{modulo}?confirmar=true` | Vaciar el módulo |
| POST | `/api/{modulo}/importar` | Subir Excel (`multipart`, campo `archivo`) |
| POST / DELETE | `/api/{modulo}/demo` | Generar / borrar datos de demostración |
| GET | `/api/eventos` | **Tiempo real** (Server-Sent Events): evento `datos` en cada cambio |
| GET | `/api/{modulo}/estadisticas/resumen` | Total, completitud por campo, origen de los datos |
| GET | `.../estadisticas/frecuencias` · `/descriptivos` | Todas las frecuencias / todos los descriptivos en una sola respuesta |
| GET | `.../estadisticas/frecuencia?campo=` | fi, hi, Fi, Hi, moda (barras, torta, Pareto) |
| GET | `.../estadisticas/descriptivo?campo=` | Media, mediana, moda, varianza, desv., CV, cuartiles, percentiles, IC 95%, boxplot, intervalos con marca de clase, **puntos vs. media (z, ±1σ, ±2σ)** |
| GET | `.../estadisticas/chi2?a=&b=` | χ² de independencia: observado, esperado, contribución, residuos, p, V de Cramér |
| GET | `.../estadisticas/chi2-bondad?campo=` | χ² de bondad de ajuste (uniforme) |
| GET | `.../estadisticas/asociaciones` | χ² de **todos** los pares categóricos (mapa de calor de p / V) |
| GET | `.../estadisticas/likert` | Media, desv. y distribución de cada escala (radar, barras) |
| GET | `.../estadisticas/dispersion?x=&y=` | Nube de puntos, Pearson, recta de regresión |
| GET | `.../estadisticas/medias-por-grupo?numerico=&grupo=` | Medias por grupo + ANOVA |
| GET | `.../estadisticas/informe` | Análisis automático en lenguaje natural |

Todos los resultados incluyen un campo `interpretacion` con la lectura en texto.

## Datos vacíos y Excel

- Ningún campo es obligatorio: los vacíos se excluyen de cada cálculo y se reportan como `faltantes`.
- El Excel se lee **por el texto de la pregunta**, no por posición: sobran/faltan columnas sin problema
  (Id, Hora de inicio, etc. se ignoran). Celdas y filas vacías se toleran. Selección múltiple: separada por `;`
  o por comas (formato Google Forms). La respuesta indica columnas reconocidas, ignoradas, filas rechazadas y avisos.
- Las opciones se llevan a la forma oficial de la encuesta sin importar mayúsculas ni tildes.

## Pensando en la Actividad 3 (Informe de Calidad y Validación)

`/descriptivo` ya entrega, con los mismos métodos del PDF, la tabla de frecuencias, la tabla por intervalos
con marca de clase, medidas centrales y de dispersión, cuartiles/percentiles, atípicos, forma de la distribución y los
datos de histograma, polígono, ojiva y boxplot. La validación (sin negativos, rangos, datos faltantes, coherencia
desempleo ≤ vida profesional) está en el modelo y en `/resumen`. Las pruebas `EstadisticaTest` fijan esos resultados.

## Estructura

```
modelo/        Titulado, Empleador (cada pregunta con @Campo: tipo, opciones, claves de Excel)
repositorio/   acceso a Neon
servicio/      RegistroService (CRUD+validación), ExcelService, EstadisticaService, EventosService, DemoService
estadistica/   motor estadístico puro (sin Spring): Descriptiva, ChiCuadrado, Distribuciones, Interpretador
controlador/   REST + SSE
static/        front: index.html, css/app.css, js/(core, charts, vistas, app)
```
