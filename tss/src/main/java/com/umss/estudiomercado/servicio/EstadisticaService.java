package com.umss.estudiomercado.servicio;

import com.umss.estudiomercado.estadistica.ChiCuadrado;
import com.umss.estudiomercado.estadistica.Descriptiva;
import com.umss.estudiomercado.estadistica.Interpretador;
import com.umss.estudiomercado.estadistica.Num;
import com.umss.estudiomercado.modelo.Campo.Tipo;
import com.umss.estudiomercado.modelo.RegistroBase;
import com.umss.estudiomercado.servicio.Meta.CampoInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cálculo de estadísticas sobre los registros. Cada método lee los datos actuales de la base, de modo que
 * el front obtiene gráficos al día cada vez que recibe un evento de tiempo real. Los datos vacíos
 * (null) simplemente se excluyen: nunca rompen un gráfico.
 */
@Service
public class EstadisticaService {
    private static final double ALFA = 0.05;

    private final RegistroService registros;

    public EstadisticaService(RegistroService registros) {
        this.registros = registros;
    }

    // ------------------------------------------------------------------ catálogo

    public List<Map<String, Object>> meta(String modulo) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CampoInfo c : Meta.campos(registros.clase(modulo))) {
            out.add(Num.mapa("nombre", c.nombre(), "etiqueta", c.etiqueta(), "tipo", c.tipo().name(),
                    "opciones", c.opciones(), "niveles", c.niveles(), "graficable", c.graficable()));
        }
        return out;
    }

    // ------------------------------------------------------------------ resumen y calidad de datos

    public Map<String, Object> resumen(String modulo) {
        List<Object> filas = registros.listar(modulo);
        int total = filas.size();
        List<Map<String, Object>> campos = new ArrayList<>();
        long respondidasTotal = 0, posibles = 0;
        for (CampoInfo c : Meta.campos(registros.clase(modulo))) {
            int resp = 0;
            for (Object f : filas) if (Meta.get(c, f) != null) resp++;
            respondidasTotal += resp;
            posibles += total;
            campos.add(Num.mapa("campo", c.nombre(), "etiqueta", c.etiqueta(), "tipo", c.tipo().name(),
                    "respondidos", resp, "faltantes", total - resp,
                    "completitudPct", total == 0 ? 0.0 : Num.r2(resp * 100.0 / total)));
        }
        Map<String, Integer> porOrigen = new LinkedHashMap<>();
        LocalDateTime ultimo = null;
        for (Object f : filas) {
            RegistroBase r = (RegistroBase) f;
            porOrigen.merge(r.getOrigenDato() == null ? "?" : r.getOrigenDato(), 1, Integer::sum);
            if (r.getFechaRegistro() != null && (ultimo == null || r.getFechaRegistro().isAfter(ultimo)))
                ultimo = r.getFechaRegistro();
        }
        return Num.mapa("modulo", modulo, "totalRegistros", total,
                "completitudGlobalPct", posibles == 0 ? 0.0 : Num.r2(respondidasTotal * 100.0 / posibles),
                "porOrigen", porOrigen, "ultimoRegistro", ultimo, "campos", campos);
    }

    // ------------------------------------------------------------------ frecuencias

    public Map<String, Object> frecuencia(String modulo, String campo) {
        return frecuencia(registros.listar(modulo), Meta.campo(registros.clase(modulo), campo));
    }

    private Map<String, Object> frecuencia(List<Object> filas, CampoInfo c) {
        if (c.tipo() == Tipo.NUMERICO)
            throw new IllegalArgumentException("«" + c.etiqueta() + "» es numérico: use /estadisticas/descriptivo.");
        if (!c.graficable())
            throw new IllegalArgumentException("«" + c.etiqueta() + "» es texto libre y no admite tabla de frecuencias.");
        boolean multiple = c.tipo() == Tipo.MULTIPLE;
        Map<String, Long> cont = new LinkedHashMap<>();
        if (c.tipo() == Tipo.BOOLEANO) { cont.put("Sí", 0L); cont.put("No", 0L); }
        if (c.tipo() == Tipo.LIKERT) for (String nv : c.niveles()) cont.put(nv, 0L);
        int resp = 0;
        for (Object f : filas) {
            Object v = Meta.get(c, f);
            if (v == null) continue;
            if (multiple) {
                boolean alguno = false;
                for (String p : v.toString().split(";")) {
                    if (p.isBlank()) continue;
                    cont.merge(p.trim(), 1L, Long::sum);
                    alguno = true;
                }
                if (alguno) resp++;
            } else {
                String e = etiquetaValor(c, v);
                if (e == null) continue;
                cont.merge(e, 1L, Long::sum);
                resp++;
            }
        }
        List<Map.Entry<String, Long>> ent = new ArrayList<>(cont.entrySet());
        if (c.tipo() == Tipo.CATEGORICO || multiple) ent.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        List<Map<String, Object>> categorias = new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        List<Long> fis = new ArrayList<>();
        long acum = 0;
        for (Map.Entry<String, Long> e : ent) {
            acum += e.getValue();
            nombres.add(e.getKey());
            fis.add(e.getValue());
            Map<String, Object> fila = Num.mapa("valor", e.getKey(), "fi", e.getValue(),
                    "hi", resp == 0 ? 0.0 : Num.r4(e.getValue() / (double) resp),
                    "porcentaje", resp == 0 ? 0.0 : Num.r2(e.getValue() * 100.0 / resp));
            if (!multiple) {
                fila.put("Fi", acum);
                fila.put("Hi", resp == 0 ? 0.0 : Num.r4(acum / (double) resp));
            }
            categorias.add(fila);
        }
        String moda = null;
        long max = 0;
        for (Map.Entry<String, Long> e : ent) if (e.getValue() > max) { max = e.getValue(); moda = e.getKey(); }
        return Num.mapa("campo", c.nombre(), "etiqueta", c.etiqueta(), "tipo", c.tipo().name(),
                "multiple", multiple, "n", resp, "faltantes", filas.size() - resp, "totalRegistros", filas.size(),
                "categorias", categorias, "moda", moda,
                "interpretacion", Interpretador.frecuencia(c.etiqueta(), resp, nombres, fis, multiple));
    }

    /** Tablas de frecuencia de todas las preguntas graficables no numéricas (una sola lectura de la base). */
    public List<Map<String, Object>> frecuencias(String modulo) {
        List<Object> filas = registros.listar(modulo);
        List<Map<String, Object>> out = new ArrayList<>();
        for (CampoInfo c : Meta.campos(registros.clase(modulo)))
            if (c.graficable() && c.tipo() != Tipo.NUMERICO) out.add(frecuencia(filas, c));
        return out;
    }

    /** Descriptivo de todas las variables numéricas y Likert (una sola lectura de la base). */
    public List<Map<String, Object>> descriptivos(String modulo) {
        List<Object> filas = registros.listar(modulo);
        List<Map<String, Object>> out = new ArrayList<>();
        for (CampoInfo c : Meta.campos(registros.clase(modulo))) if (c.numerico()) out.add(descriptivo(filas, c));
        return out;
    }

    // ------------------------------------------------------------------ descriptivo numérico

    public Map<String, Object> descriptivo(String modulo, String campo) {
        return descriptivo(registros.listar(modulo), Meta.campo(registros.clase(modulo), campo));
    }

    private Map<String, Object> descriptivo(List<Object> filas, CampoInfo c) {
        if (!c.numerico())
            throw new IllegalArgumentException("«" + c.etiqueta() + "» no es numérico (use frecuencia).");
        double[] datos = numeros(filas, c);
        Map<String, Object> out = Num.mapa("campo", c.nombre(), "etiqueta", c.etiqueta(), "tipo", c.tipo().name(),
                "faltantes", filas.size() - datos.length, "totalRegistros", filas.size());
        out.putAll(Descriptiva.calcular(datos, c.etiqueta()));
        return out;
    }

    // ------------------------------------------------------------------ chi-cuadrado

    public Map<String, Object> chi2(String modulo, String a, String b, Double alfa) {
        Class<?> clase = registros.clase(modulo);
        CampoInfo ca = Meta.campo(clase, a), cb = Meta.campo(clase, b);
        exigirCategorico(ca);
        exigirCategorico(cb);
        if (ca.nombre().equals(cb.nombre()))
            throw new IllegalArgumentException("Elija dos variables distintas para χ² de independencia.");
        Tabla t = tabla(ca, cb, registros.listar(modulo));
        return ChiCuadrado.independencia(t.filas, t.cols, t.obs, alfa(alfa), ca.etiqueta(), cb.etiqueta());
    }

    public Map<String, Object> bondad(String modulo, String campo, Double alfa) {
        CampoInfo c = Meta.campo(registros.clase(modulo), campo);
        if (!c.categorico() && c.tipo() != Tipo.MULTIPLE)
            throw new IllegalArgumentException("«" + c.etiqueta() + "» no es categórica.");
        Map<String, Object> f = frecuencia(registros.listar(modulo), c);
        List<String> cats = new ArrayList<>();
        List<Long> obs = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> categorias = (List<Map<String, Object>>) f.get("categorias");
        for (Map<String, Object> fila : categorias) {
            cats.add((String) fila.get("valor"));
            obs.add((Long) fila.get("fi"));
        }
        long[] o = obs.stream().mapToLong(Long::longValue).toArray();
        Map<String, Object> res = ChiCuadrado.bondad(cats, o, alfa(alfa), c.etiqueta());
        res.put("campo", c.nombre());
        return res;
    }

    /** Todos los pares de variables categóricas con su χ², p y V de Cramér (para un mapa de calor de asociaciones). */
    public Map<String, Object> asociaciones(String modulo, Double alfa) {
        Class<?> clase = registros.clase(modulo);
        List<Object> filas = registros.listar(modulo);
        double a = alfa(alfa);
        List<CampoInfo> cs = new ArrayList<>();
        for (CampoInfo c : Meta.campos(clase)) if (c.categorico()) cs.add(c);
        List<Map<String, Object>> pares = new ArrayList<>();
        for (int i = 0; i < cs.size(); i++)
            for (int j = i + 1; j < cs.size(); j++) {
                Tabla t = tabla(cs.get(i), cs.get(j), filas);
                Map<String, Object> r = ChiCuadrado.independencia(t.filas, t.cols, t.obs, a,
                        cs.get(i).etiqueta(), cs.get(j).etiqueta());
                if (!Boolean.TRUE.equals(r.get("aplicable"))) continue;
                pares.add(Num.mapa("a", cs.get(i).nombre(), "b", cs.get(j).nombre(),
                        "etiquetaA", cs.get(i).etiqueta(), "etiquetaB", cs.get(j).etiqueta(),
                        "n", r.get("n"), "gl", r.get("gl"), "chi2", r.get("chi2"), "p", r.get("p"),
                        "cramerV", r.get("cramerV"), "significativa", r.get("rechazaH0"), "fiable", r.get("fiable")));
            }
        pares.sort(Comparator.comparingDouble(m -> m.get("p") == null ? 1.0 : ((Number) m.get("p")).doubleValue()));
        List<Map<String, Object>> campos = new ArrayList<>();
        for (CampoInfo c : cs) campos.add(Num.mapa("nombre", c.nombre(), "etiqueta", c.etiqueta()));
        return Num.mapa("alfa", a, "campos", campos, "pares", pares);
    }

    // ------------------------------------------------------------------ escalas Likert (radar / barras de medias)

    public Map<String, Object> likert(String modulo) {
        List<Object> filas = registros.listar(modulo);
        List<Map<String, Object>> items = new ArrayList<>();
        for (CampoInfo c : Meta.campos(registros.clase(modulo))) {
            if (c.tipo() != Tipo.LIKERT) continue;
            double[] d = numeros(filas, c);
            int max = c.niveles().size();
            Map<String, Object> dist = new LinkedHashMap<>();
            for (int i = 0; i < max; i++) {
                int nivel = i + 1;
                long k = 0;
                for (double v : d) if ((int) v == nivel) k++;
                dist.put(c.niveles().get(i), k);
            }
            Map<String, Object> r = Descriptiva.resumen(d);
            Object media = r.get("media"), sd = r.get("desviacion");
            items.add(Num.mapa("campo", c.nombre(), "etiqueta", c.etiqueta(), "escalaMax", max,
                    "n", d.length, "media", media, "desviacion", sd, "distribucion", dist,
                    "interpretacion", d.length == 0 ? "Aún no hay respuestas para «" + c.etiqueta() + "»."
                            : Interpretador.likert(c.etiqueta(), ((Number) media).doubleValue(),
                            ((Number) sd).doubleValue(), max, d.length)));
        }
        return Num.mapa("totalRegistros", filas.size(), "items", items);
    }

    // ------------------------------------------------------------------ dispersión / correlación

    public Map<String, Object> dispersion(String modulo, String x, String y) {
        Class<?> clase = registros.clase(modulo);
        CampoInfo cx = Meta.campo(clase, x), cy = Meta.campo(clase, y);
        if (!cx.numerico() || !cy.numerico())
            throw new IllegalArgumentException("La dispersión requiere dos variables numéricas o Likert.");
        List<Double> xs = new ArrayList<>(), ys = new ArrayList<>();
        List<Map<String, Object>> puntos = new ArrayList<>();
        for (Object f : registros.listar(modulo)) {
            Object vx = Meta.get(cx, f), vy = Meta.get(cy, f);
            if (vx == null || vy == null) continue;
            double dx = ((Number) vx).doubleValue(), dy = ((Number) vy).doubleValue();
            xs.add(dx);
            ys.add(dy);
            puntos.add(Num.mapa("x", dx, "y", dy));
        }
        Map<String, Object> res = Descriptiva.correlacion(
                xs.stream().mapToDouble(Double::doubleValue).toArray(),
                ys.stream().mapToDouble(Double::doubleValue).toArray(), cx.etiqueta(), cy.etiqueta());
        Map<String, Object> out = Num.mapa("x", cx.nombre(), "y", cy.nombre(),
                "etiquetaX", cx.etiqueta(), "etiquetaY", cy.etiqueta(), "puntos", puntos);
        out.putAll(res);
        return out;
    }

    // ------------------------------------------------------------------ medias por grupo (+ ANOVA)

    public Map<String, Object> mediasPorGrupo(String modulo, String numerico, String grupo) {
        Class<?> clase = registros.clase(modulo);
        CampoInfo cn = Meta.campo(clase, numerico), cg = Meta.campo(clase, grupo);
        if (!cn.numerico()) throw new IllegalArgumentException("«" + cn.etiqueta() + "» no es numérica.");
        exigirCategorico(cg);
        Map<String, List<Double>> por = new HashMap<>();
        for (Object f : registros.listar(modulo)) {
            Object v = Meta.get(cn, f);
            String g = etiquetaValor(cg, Meta.get(cg, f));
            if (v == null || g == null) continue;
            por.computeIfAbsent(g, k -> new ArrayList<>()).add(((Number) v).doubleValue());
        }
        List<Map<String, Object>> grupos = new ArrayList<>();
        List<double[]> arrays = new ArrayList<>();
        for (String g : ordenar(cg, por.keySet())) {
            double[] d = por.get(g).stream().mapToDouble(Double::doubleValue).toArray();
            arrays.add(d);
            Map<String, Object> r = Num.mapa("grupo", g);
            r.putAll(Descriptiva.resumen(d));
            grupos.add(r);
        }
        return Num.mapa("numerico", cn.nombre(), "etiquetaNumerico", cn.etiqueta(),
                "grupo", cg.nombre(), "etiquetaGrupo", cg.etiqueta(), "grupos", grupos,
                "anova", Descriptiva.anova(arrays, cn.etiqueta(), cg.etiqueta()));
    }

    // ------------------------------------------------------------------ informe automático

    /** Lectura automática de todos los datos: hallazgos en lenguaje natural, listos para mostrar o exportar. */
    public Map<String, Object> informe(String modulo) {
        Class<?> clase = registros.clase(modulo);
        List<Object> filas = registros.listar(modulo);
        List<Map<String, Object>> hallazgos = new ArrayList<>();
        if (filas.isEmpty()) {
            return Num.mapa("modulo", modulo, "totalRegistros", 0, "generado", LocalDateTime.now(),
                    "mensaje", "Aún no hay registros. Cargue datos (formulario o Excel) para generar el análisis.",
                    "hallazgos", hallazgos);
        }
        for (CampoInfo c : Meta.campos(clase)) {
            if (!c.graficable()) continue;
            Map<String, Object> r;
            String tipo;
            if (c.tipo() == Tipo.NUMERICO) { r = descriptivo(filas, c); tipo = "descriptivo"; }
            else { r = frecuencia(filas, c); tipo = "frecuencia"; }
            if (((Number) r.get("n")).intValue() == 0) continue;
            hallazgos.add(Num.mapa("tipo", tipo, "campo", c.nombre(), "titulo", c.etiqueta(),
                    "texto", r.get("interpretacion")));
        }
        for (Map<String, Object> l : listaLikert(modulo, filas)) hallazgos.add(l);

        Map<String, Object> aso = asociaciones(modulo, ALFA);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pares = (List<Map<String, Object>>) aso.get("pares");
        int cuenta = 0;
        for (Map<String, Object> p : pares) {
            if (!Boolean.TRUE.equals(p.get("significativa")) || !Boolean.TRUE.equals(p.get("fiable"))) continue;
            hallazgos.add(Num.mapa("tipo", "asociacion", "campo", p.get("a") + " × " + p.get("b"),
                    "titulo", p.get("etiquetaA") + " × " + p.get("etiquetaB"),
                    "texto", "Asociación significativa (χ², p=" + Num.p(((Number) p.get("p")).doubleValue())
                            + ", V de Cramér=" + p.get("cramerV") + ", n=" + p.get("n") + ")."));
            if (++cuenta >= 10) break;
        }
        return Num.mapa("modulo", modulo, "totalRegistros", filas.size(), "generado", LocalDateTime.now(),
                "asociacionesSignificativas", cuenta, "hallazgos", hallazgos);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listaLikert(String modulo, List<Object> filas) {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> l = likert(modulo);
        for (Map<String, Object> it : (List<Map<String, Object>>) l.get("items")) {
            if (((Number) it.get("n")).intValue() == 0) continue;
            out.add(Num.mapa("tipo", "likert", "campo", it.get("campo"), "titulo", it.get("etiqueta"),
                    "texto", it.get("interpretacion")));
        }
        return out;
    }

    // ------------------------------------------------------------------ utilidades

    private static double alfa(Double a) {
        return a == null || a <= 0 || a >= 1 ? ALFA : a;
    }

    private static void exigirCategorico(CampoInfo c) {
        if (!c.categorico())
            throw new IllegalArgumentException("«" + c.etiqueta() + "» no es una variable categórica "
                    + "(se admiten opción única, Sí/No y escalas Likert).");
    }

    private static String etiquetaValor(CampoInfo c, Object v) {
        if (v == null) return null;
        switch (c.tipo()) {
            case BOOLEANO:
                return ((Boolean) v) ? "Sí" : "No";
            case LIKERT: {
                int i = ((Number) v).intValue();
                return i >= 1 && i <= c.niveles().size() ? c.niveles().get(i - 1) : String.valueOf(i);
            }
            default: {
                String s = v.toString().trim();
                return s.isEmpty() ? null : s;
            }
        }
    }

    private static double[] numeros(List<Object> filas, CampoInfo c) {
        List<Double> l = new ArrayList<>();
        for (Object f : filas) {
            Object v = Meta.get(c, f);
            if (v instanceof Number n) l.add(n.doubleValue());
        }
        return l.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private record Tabla(List<String> filas, List<String> cols, long[][] obs) {}

    private Tabla tabla(CampoInfo a, CampoInfo b, List<Object> rows) {
        Map<String, Map<String, Long>> m = new HashMap<>();
        Set<String> fs = new HashSet<>(), cs = new HashSet<>();
        for (Object r : rows) {
            String x = etiquetaValor(a, Meta.get(a, r)), y = etiquetaValor(b, Meta.get(b, r));
            if (x == null || y == null) continue;
            fs.add(x);
            cs.add(y);
            m.computeIfAbsent(x, k -> new HashMap<>()).merge(y, 1L, Long::sum);
        }
        List<String> fl = ordenar(a, fs), cl = ordenar(b, cs);
        long[][] obs = new long[fl.size()][cl.size()];
        for (int i = 0; i < fl.size(); i++)
            for (int j = 0; j < cl.size(); j++) {
                Map<String, Long> fila = m.get(fl.get(i));
                obs[i][j] = fila == null ? 0L : fila.getOrDefault(cl.get(j), 0L);
            }
        return new Tabla(fl, cl, obs);
    }

    /** Ordena categorías según el orden natural de la pregunta (niveles Likert, opciones de la encuesta). */
    private static List<String> ordenar(CampoInfo c, Collection<String> valores) {
        List<String> ref = c.tipo() == Tipo.LIKERT ? c.niveles()
                : c.tipo() == Tipo.BOOLEANO ? List.of("Sí", "No") : c.opciones();
        List<String> l = new ArrayList<>(valores);
        l.sort(Comparator.comparingInt((String s) -> {
            int i = ref.indexOf(s);
            return i < 0 ? Integer.MAX_VALUE : i;
        }).thenComparing(Comparator.naturalOrder()));
        return l;
    }
}
