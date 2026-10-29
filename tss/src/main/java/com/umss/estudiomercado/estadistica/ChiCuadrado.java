package com.umss.estudiomercado.estadistica;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.umss.estudiomercado.estadistica.Num.mapa;
import static com.umss.estudiomercado.estadistica.Num.r4;

/** Pruebas chi-cuadrado: independencia (tabla de contingencia) y bondad de ajuste (uniforme). */
public final class ChiCuadrado {
    private ChiCuadrado() {}

    public static Map<String, Object> independencia(List<String> filas, List<String> cols, long[][] obs,
                                                    double alfa, String nombreA, String nombreB) {
        int r = filas.size(), c = cols.size();
        long n = 0;
        long[] rt = new long[r], ct = new long[c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++) {
                n += obs[i][j];
                rt[i] += obs[i][j];
                ct[j] += obs[i][j];
            }
        Map<String, Object> res = mapa("variableFilas", nombreA, "variableColumnas", nombreB,
                "filas", filas, "columnas", cols, "n", n, "alfa", alfa,
                "hipotesis", mapa("H0", "«" + nombreA + "» y «" + nombreB + "» son independientes",
                        "H1", "Existe asociación entre «" + nombreA + "» y «" + nombreB + "»"));
        if (r < 2 || c < 2 || n == 0) {
            String motivo = n == 0
                    ? "Aún no hay registros con ambas variables respondidas."
                    : "Se necesitan al menos 2 categorías en cada variable para aplicar χ² (ahora: "
                    + r + " × " + c + "). Cargue más datos con respuestas variadas.";
            res.put("aplicable", false);
            res.put("motivo", motivo);
            res.put("interpretacion", motivo);
            res.put("observado", obs);
            return res;
        }
        double[][] esp = new double[r][c], contrib = new double[r][c], resid = new double[r][c];
        double chi = 0;
        int bajas = 0;
        double minEsp = Double.MAX_VALUE;
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++) {
                esp[i][j] = rt[i] * (double) ct[j] / n;
                if (esp[i][j] < 5) bajas++;
                minEsp = Math.min(minEsp, esp[i][j]);
                double d = obs[i][j] - esp[i][j];
                contrib[i][j] = d * d / esp[i][j];
                resid[i][j] = d / Math.sqrt(esp[i][j]);
                chi += contrib[i][j];
            }
        int gl = (r - 1) * (c - 1);
        double pv = Distribuciones.chi2Sf(chi, gl);
        double crit = Distribuciones.chi2Inv(1 - alfa, gl);
        double v = Math.sqrt(chi / (n * (double) Math.min(r - 1, c - 1)));
        double cc = Math.sqrt(chi / (chi + n));
        double pctBajas = bajas * 100.0 / (r * c);
        res.put("aplicable", true);
        res.put("chi2", r4(chi));
        res.put("gl", gl);
        res.put("chi2Critico", r4(crit));
        res.put("p", Num.r(pv, 6));
        res.put("rechazaH0", pv < alfa);
        res.put("cramerV", r4(v));
        res.put("coefContingencia", r4(cc));
        res.put("celdasEsperadoMenor5", bajas);
        res.put("pctCeldasBajas", r4(pctBajas));
        res.put("fiable", pctBajas <= 20 && minEsp >= 1);
        res.put("observado", obs);
        res.put("esperado", redondear(esp));
        res.put("contribucion", redondear(contrib));
        res.put("residuosEstandarizados", redondear(resid));
        res.put("totalesFila", rt);
        res.put("totalesColumna", ct);
        res.put("interpretacion", Interpretador.chi2(nombreA, nombreB, n, gl, chi, crit, pv, alfa, v, pctBajas));
        return res;
    }

    /** Bondad de ajuste frente a una distribución uniforme (todas las categorías igual de probables). */
    public static Map<String, Object> bondad(List<String> cats, long[] obs, double alfa, String etiqueta) {
        int k = cats.size();
        long n = 0;
        for (long o : obs) n += o;
        Map<String, Object> res = mapa("variable", etiqueta, "categorias", cats, "n", n, "alfa", alfa,
                "hipotesis", mapa("H0", "Las categorías de «" + etiqueta + "» se reparten por igual",
                        "H1", "Las categorías de «" + etiqueta + "» no se reparten por igual"));
        if (k < 2 || n == 0) {
            String motivo = n == 0 ? "Aún no hay respuestas." : "Se necesitan al menos 2 categorías con respuestas.";
            res.put("aplicable", false);
            res.put("motivo", motivo);
            res.put("interpretacion", motivo);
            return res;
        }
        double e = n / (double) k, chi = 0;
        List<Double> esperado = new ArrayList<>();
        List<Double> contrib = new ArrayList<>();
        for (long o : obs) {
            double cj = (o - e) * (o - e) / e;
            chi += cj;
            esperado.add(r4(e));
            contrib.add(r4(cj));
        }
        int gl = k - 1;
        double pv = Distribuciones.chi2Sf(chi, gl);
        res.put("aplicable", true);
        res.put("observado", obs);
        res.put("esperado", esperado);
        res.put("contribucion", contrib);
        res.put("chi2", r4(chi));
        res.put("gl", gl);
        res.put("chi2Critico", r4(Distribuciones.chi2Inv(1 - alfa, gl)));
        res.put("p", Num.r(pv, 6));
        res.put("rechazaH0", pv < alfa);
        res.put("fiable", e >= 5);
        res.put("interpretacion", Interpretador.bondad(etiqueta, n, gl, chi, pv, alfa)
                + (e < 5 ? " Atención: la frecuencia esperada por categoría es menor a 5; resultado poco fiable." : ""));
        return res;
    }

    private static double[][] redondear(double[][] a) {
        double[][] o = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            o[i] = new double[a[i].length];
            for (int j = 0; j < a[i].length; j++) {
                Double v = Num.r(a[i][j], 4);
                o[i][j] = v == null ? 0 : v;
            }
        }
        return o;
    }
}
