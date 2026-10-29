package com.umss.estudiomercado.estadistica;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.umss.estudiomercado.estadistica.Num.f;
import static com.umss.estudiomercado.estadistica.Num.mapa;
import static com.umss.estudiomercado.estadistica.Num.r4;

/**
 * Estadística descriptiva completa de una variable cuantitativa. Los métodos coinciden con los del
 * "Informe de Calidad y Validación": tabla de frecuencias, intervalos con marca de clase, media, mediana,
 * moda, rango, varianza (n-1), desviación, CV, cuartiles (mediana de mitades), percentiles (rango más cercano),
 * valores atípicos (1.5·IQR), forma de la distribución y datos para histograma, polígono, ojiva y boxplot.
 */
public final class Descriptiva {
    private Descriptiva() {}

    public static Map<String, Object> calcular(double[] raw, String etiqueta) {
        int n = raw.length;
        Map<String, Object> m = mapa("n", n);
        if (n == 0) {
            m.put("sinDatos", true);
            m.put("interpretacion", "Aún no hay datos válidos para «" + etiqueta + "».");
            return m;
        }
        double[] x = raw.clone();
        Arrays.sort(x);
        double suma = 0;
        for (double v : x) suma += v;
        double media = suma / n;
        double ss = 0;
        for (double v : x) ss += (v - media) * (v - media);
        double varianza = n > 1 ? ss / (n - 1) : 0;
        double sd = Math.sqrt(varianza);
        double cv = media != 0 ? sd / Math.abs(media) * 100 : Double.NaN;
        double mediana = mediana(x, 0, n);
        double q1, q3;
        if (n >= 2) {
            q1 = mediana(x, 0, n / 2);
            q3 = mediana(x, (n + 1) / 2, n);
        } else {
            q1 = q3 = x[0];
        }
        double iqr = q3 - q1;
        double fInf = q1 - 1.5 * iqr, fSup = q3 + 1.5 * iqr;
        List<Double> atipicos = new ArrayList<>();
        double wMin = Double.NaN, wMax = Double.NaN;
        for (double v : x) {
            if (v < fInf || v > fSup) atipicos.add(r4(v));
            else {
                if (Double.isNaN(wMin)) wMin = v;
                wMax = v;
            }
        }

        TreeMap<Double, Integer> cont = new TreeMap<>();
        for (double v : x) cont.merge(v, 1, Integer::sum);
        int maxCont = Collections.max(cont.values());
        List<Double> moda = new ArrayList<>();
        if (maxCont > 1) for (Map.Entry<Double, Integer> e : cont.entrySet()) if (e.getValue() == maxCont) moda.add(r4(e.getKey()));

        Double asimetria = null;
        if (n > 2 && sd > 0) {
            double s3 = 0;
            for (double v : x) s3 += Math.pow((v - media) / sd, 3);
            asimetria = r4(n / ((n - 1.0) * (n - 2.0)) * s3);
        }

        double min = x[0], max = x[n - 1], rango = max - min;

        m.put("media", r4(media));
        m.put("mediana", r4(mediana));
        m.put("moda", moda);
        m.put("minimo", r4(min));
        m.put("maximo", r4(max));
        m.put("rango", r4(rango));
        m.put("varianza", r4(varianza));
        m.put("desviacion", r4(sd));
        m.put("cv", r4(cv));
        m.put("variabilidad", Interpretador.variabilidad(cv));
        m.put("asimetria", asimetria);
        m.put("formaDistribucion", Interpretador.forma(media, mediana, sd));
        m.put("q1", r4(q1));
        m.put("q2", r4(mediana));
        m.put("q3", r4(q3));
        m.put("iqr", r4(iqr));
        Map<String, Object> perc = new java.util.LinkedHashMap<>();
        for (int p : new int[]{5, 10, 25, 50, 75, 90, 95}) perc.put("P" + p, r4(percentil(x, p)));
        m.put("percentiles", perc);
        if (n > 1) {
            double t = Distribuciones.tInv(0.975, n - 1);
            double se = sd / Math.sqrt(n);
            m.put("errorEstandar", r4(se));
            m.put("icMedia95", mapa("inferior", r4(media - t * se), "superior", r4(media + t * se)));
        }
        m.put("boxplot", mapa("minimo", r4(min), "whiskerInferior", r4(wMin), "q1", r4(q1), "mediana", r4(mediana),
                "q3", r4(q3), "whiskerSuperior", r4(wMax), "maximo", r4(max), "atipicos", atipicos));

        // Tabla de frecuencias por valor (solo si hay pocos valores distintos)
        List<Map<String, Object>> tabla = new ArrayList<>();
        if (cont.size() <= 30) {
            long acum = 0;
            for (Map.Entry<Double, Integer> e : cont.entrySet()) {
                acum += e.getValue();
                tabla.add(mapa("xi", r4(e.getKey()), "fi", e.getValue(), "hi", r4(e.getValue() / (double) n),
                        "Fi", acum, "Hi", r4(acum / (double) n)));
            }
        }
        m.put("tablaFrecuencia", tabla);

        // Tabla por intervalos (regla de Sturges): [a - b], (b - c], ...
        int k = Math.max(1, (int) Math.ceil(1 + 3.322 * Math.log10(n)));
        if (rango == 0) k = 1;
        double ancho = rango == 0 ? 1 : rango / k;
        long[] fi = new long[k];
        for (double v : x) {
            int idx = rango == 0 ? 0 : (int) Math.ceil((v - min) / ancho - 1e-9) - 1;
            fi[Math.max(0, Math.min(k - 1, idx))]++;
        }
        List<Map<String, Object>> intervalos = new ArrayList<>();
        long acum = 0;
        for (int i = 0; i < k; i++) {
            double li = min + i * ancho;
            double ls = rango == 0 ? li : li + ancho;
            acum += fi[i];
            intervalos.add(mapa("intervalo", (i == 0 ? "[" : "(") + f(li) + " - " + f(ls) + "]",
                    "limiteInferior", r4(li), "limiteSuperior", r4(ls), "marcaClase", r4((li + ls) / 2),
                    "fi", fi[i], "hi", r4(fi[i] / (double) n), "Fi", acum, "Hi", r4(acum / (double) n)));
        }
        m.put("intervalos", intervalos);
        m.put("amplitudIntervalo", r4(ancho));

        // Puntos respecto a la media (gráfico de dispersión / "lluvia de puntos")
        List<Map<String, Object>> puntos = new ArrayList<>();
        int dentro1 = 0, dentro2 = 0;
        for (int i = 0; i < n; i++) {
            double z = sd > 0 ? (raw[i] - media) / sd : 0;
            if (Math.abs(z) <= 1) dentro1++;
            if (Math.abs(z) <= 2) dentro2++;
            puntos.add(mapa("i", i + 1, "valor", r4(raw[i]), "desvio", r4(raw[i] - media), "z", r4(z)));
        }
        m.put("puntos", puntos);
        m.put("bandas", mapa("media", r4(media), "menos1sd", r4(media - sd), "mas1sd", r4(media + sd),
                "menos2sd", r4(media - 2 * sd), "mas2sd", r4(media + 2 * sd),
                "pctDentro1sd", r4(dentro1 * 100.0 / n), "pctDentro2sd", r4(dentro2 * 100.0 / n)));

        m.put("interpretacion", Interpretador.descriptiva(etiqueta, n, media, mediana, sd, cv, atipicos.size(), q1, q3));
        return m;
    }

    /** Resumen corto (n, media, mediana, desviación, mínimo, máximo) para comparar grupos. */
    public static Map<String, Object> resumen(double[] raw) {
        int n = raw.length;
        if (n == 0) return mapa("n", 0);
        double[] x = raw.clone();
        Arrays.sort(x);
        double suma = 0;
        for (double v : x) suma += v;
        double media = suma / n, ss = 0;
        for (double v : x) ss += (v - media) * (v - media);
        double sd = n > 1 ? Math.sqrt(ss / (n - 1)) : 0;
        return mapa("n", n, "media", r4(media), "mediana", r4(mediana(x, 0, n)), "desviacion", r4(sd),
                "minimo", r4(x[0]), "maximo", r4(x[n - 1]));
    }

    /** ANOVA de un factor. Devuelve null si no es calculable. */
    public static Map<String, Object> anova(List<double[]> grupos, String numerico, String grupo) {
        List<double[]> g = new ArrayList<>();
        for (double[] a : grupos) if (a.length > 0) g.add(a);
        int k = g.size();
        int N = 0;
        double suma = 0;
        for (double[] a : g) {
            N += a.length;
            for (double v : a) suma += v;
        }
        if (k < 2 || N <= k) return null;
        double gran = suma / N, ssb = 0, ssw = 0;
        for (double[] a : g) {
            double s = 0;
            for (double v : a) s += v;
            double mg = s / a.length;
            ssb += a.length * (mg - gran) * (mg - gran);
            for (double v : a) ssw += (v - mg) * (v - mg);
        }
        int g1 = k - 1, g2 = N - k;
        if (ssw == 0) return null;
        double F = (ssb / g1) / (ssw / g2);
        double pv = Distribuciones.fSf(F, g1, g2);
        return mapa("F", r4(F), "gl1", g1, "gl2", g2, "p", Num.r(pv, 6), "significativo", pv < 0.05,
                "interpretacion", Interpretador.anova(numerico, grupo, F, pv, g1, g2));
    }

    /** Correlación de Pearson y recta de regresión por mínimos cuadrados. */
    public static Map<String, Object> correlacion(double[] x, double[] y, String ex, String ey) {
        int n = x.length;
        if (n < 3) return mapa("aplicable", false, "n", n,
                "interpretacion", "Se necesitan al menos 3 pares de datos para calcular la correlación.");
        double mx = 0, my = 0;
        for (int i = 0; i < n; i++) { mx += x[i]; my += y[i]; }
        mx /= n;
        my /= n;
        double sxx = 0, syy = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
            sxy += (x[i] - mx) * (y[i] - my);
        }
        if (sxx == 0 || syy == 0) return mapa("aplicable", false, "n", n,
                "interpretacion", "Una de las variables no varía (todos los valores son iguales): no hay correlación calculable.");
        double r = sxy / Math.sqrt(sxx * syy);
        double pend = sxy / sxx, inter = my - pend * mx;
        double pv;
        if (Math.abs(r) >= 1) pv = 0;
        else pv = Distribuciones.tDosColas(r * Math.sqrt((n - 2) / (1 - r * r)), n - 2);
        return mapa("aplicable", true, "n", n, "r", r4(r), "r2", r4(r * r), "pendiente", r4(pend),
                "intercepto", r4(inter), "p", Num.r(pv, 6), "mediaX", r4(mx), "mediaY", r4(my),
                "interpretacion", Interpretador.correlacion(ex, ey, n, r, pv));
    }

    static double mediana(double[] x, int desde, int hasta) {
        int len = hasta - desde;
        if (len <= 0) return Double.NaN;
        int mid = desde + len / 2;
        return len % 2 == 1 ? x[mid] : (x[mid - 1] + x[mid]) / 2.0;
    }

    /** Percentil por rango más cercano (nearest rank). */
    static double percentil(double[] x, int p) {
        int idx = (int) Math.ceil(p / 100.0 * x.length) - 1;
        return x[Math.max(0, Math.min(x.length - 1, idx))];
    }
}
