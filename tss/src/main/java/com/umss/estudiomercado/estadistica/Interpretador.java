package com.umss.estudiomercado.estadistica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.umss.estudiomercado.estadistica.Num.f;
import static com.umss.estudiomercado.estadistica.Num.p;
import static com.umss.estudiomercado.estadistica.Num.pct;

/** Genera la lectura en lenguaje natural de cada resultado estadístico. */
public final class Interpretador {
    private Interpretador() {}

    public static String forma(double media, double mediana, double sd) {
        if (sd == 0) return "constante (todos los valores son iguales)";
        double d = media - mediana;
        if (Math.abs(d) <= 0.1 * sd) return "aproximadamente simétrica (media ≈ mediana)";
        return d > 0
                ? "asimétrica a la derecha (media > mediana: hay valores altos que jalan el promedio)"
                : "asimétrica a la izquierda (media < mediana: hay valores bajos que jalan el promedio)";
    }

    public static String variabilidad(double cv) {
        if (Double.isNaN(cv)) return "no calculable";
        return cv < 15 ? "baja" : cv <= 30 ? "moderada" : "alta";
    }

    public static String descriptiva(String et, int n, double media, double mediana, double sd, double cv,
                                     int atipicos, double q1, double q3) {
        StringBuilder sb = new StringBuilder();
        sb.append("«").append(et).append("» (n=").append(n).append("): promedio ").append(f(media))
                .append(", mediana ").append(f(mediana)).append(". ");
        sb.append("La distribución es ").append(forma(media, mediana, sd)).append(". ");
        if (sd > 0 && !Double.isNaN(cv)) {
            sb.append("La desviación estándar es ").append(f(sd)).append(" (CV ").append(f(cv))
                    .append("%: variabilidad ").append(variabilidad(cv)).append("). ");
        }
        sb.append("El 50% central de los datos está entre ").append(f(q1)).append(" y ").append(f(q3)).append(". ");
        sb.append(atipicos > 0
                ? atipicos + " valor(es) atípico(s) detectado(s) (criterio 1.5·IQR) que pueden estar afectando el promedio."
                : "No se detectan valores atípicos.");
        return sb.toString();
    }

    public static String frecuencia(String et, int resp, List<String> cats, List<Long> fi, boolean multiple) {
        if (resp == 0 || cats.isEmpty()) return "Aún no hay respuestas para «" + et + "».";
        int im = 0;
        for (int i = 1; i < fi.size(); i++) if (fi.get(i) > fi.get(im)) im = i;
        double h = fi.get(im) / (double) resp;
        StringBuilder sb = new StringBuilder();
        sb.append("«").append(et).append("» (n=").append(resp).append("): la opción más frecuente es «")
                .append(cats.get(im)).append("» con ").append(fi.get(im)).append(" (").append(pct(h)).append("). ");
        if (multiple) {
            sb.append("Es de selección múltiple: los porcentajes se calculan sobre los respondentes y pueden sumar más de 100%.");
            return sb.toString();
        }
        if (cats.size() >= 2) {
            List<Long> orden = new ArrayList<>(fi);
            orden.sort(Collections.reverseOrder());
            double top2 = (orden.get(0) + orden.get(1)) / (double) resp;
            if (h >= 0.5) sb.append("Representa la mayoría absoluta. ");
            sb.append(top2 >= 0.7
                    ? "Las dos opciones principales concentran " + pct(top2) + " de las respuestas (concentración alta)."
                    : "Las dos opciones principales suman " + pct(top2) + ": las respuestas están repartidas entre varias opciones.");
        } else {
            sb.append("Todas las respuestas coinciden en una sola opción.");
        }
        return sb.toString();
    }

    public static String likert(String et, double media, double sd, int max, int n) {
        double prop = max > 1 ? (media - 1) / (max - 1) : 1;
        String nivel = prop >= 0.75 ? "muy favorable" : prop >= 0.5 ? "favorable"
                : prop >= 0.25 ? "desfavorable (requiere atención)" : "crítica";
        return "«" + et + "»: media " + f(media) + " de " + max + " (desv. est. " + f(sd) + ", n=" + n
                + "). Valoración " + nivel + "; brecha respecto al máximo: " + f(max - media) + " puntos.";
    }

    public static String chi2(String a, String b, long n, int gl, double chi, double crit, double pv, double alfa,
                              double v, double pctBajas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Prueba χ² de independencia entre «").append(a).append("» y «").append(b).append("» (n=").append(n)
                .append(", gl=").append(gl).append(", χ²=").append(f(chi)).append(", valor crítico=").append(f(crit))
                .append(", p ").append(pv < 0.0001 ? p(pv) : "= " + p(pv)).append("). ");
        if (pv < alfa) {
            sb.append("Se rechaza H0: con α=").append(f(alfa)).append(" hay evidencia de asociación significativa entre las variables. ");
            String fuerza = v < 0.1 ? "despreciable" : v < 0.3 ? "débil" : v < 0.5 ? "moderada" : "fuerte";
            sb.append("La intensidad de la asociación (V de Cramér=").append(f(v)).append(") es ").append(fuerza).append(". ");
        } else {
            sb.append("No se rechaza H0: con α=").append(f(alfa))
                    .append(" no hay evidencia suficiente de que las variables estén asociadas (se comportan como independientes). ");
        }
        if (pctBajas > 20) {
            sb.append("Atención: el ").append(f(pctBajas))
                    .append("% de las celdas tiene frecuencia esperada menor a 5, por lo que el resultado es poco fiable; agrupe categorías o reúna más datos.");
        }
        return sb.toString().trim();
    }

    public static String bondad(String et, long n, int gl, double chi, double pv, double alfa) {
        String base = "Prueba χ² de bondad de ajuste de «" + et + "» frente a una distribución uniforme (n=" + n + ", gl=" + gl
                + ", χ²=" + f(chi) + ", p " + (pv < 0.0001 ? p(pv) : "= " + p(pv)) + "). ";
        return base + (pv < alfa
                ? "Se rechaza H0: las respuestas NO se reparten por igual; hay opciones claramente preferidas."
                : "No se rechaza H0: no hay evidencia de que las preferencias difieran de un reparto uniforme.");
    }

    public static String correlacion(String x, String y, int n, double r, double pv) {
        double a = Math.abs(r);
        String fuerza = a < 0.1 ? "nula" : a < 0.3 ? "débil" : a < 0.5 ? "moderada" : a < 0.7 ? "fuerte" : "muy fuerte";
        String sentido = r > 0 ? "positiva" : "negativa";
        return "Correlación de Pearson entre «" + x + "» y «" + y + "» (n=" + n + "): r=" + f(r) + " (" + fuerza
                + (a < 0.1 ? "" : ", " + sentido) + "), R²=" + f(r * r) + ". "
                + (pv < 0.05 ? "Es estadísticamente significativa (p " + (pv < 0.0001 ? p(pv) : "= " + p(pv)) + ")."
                : "No es estadísticamente significativa (p = " + p(pv) + ").");
    }

    public static String anova(String num, String grupo, double F, double pv, int g1, int g2) {
        return "ANOVA de «" + num + "» según «" + grupo + "»: F(" + g1 + ", " + g2 + ")=" + f(F) + ", p "
                + (pv < 0.0001 ? p(pv) : "= " + p(pv)) + ". "
                + (pv < 0.05 ? "Hay diferencias significativas entre las medias de los grupos."
                : "No hay evidencia de diferencias entre las medias de los grupos.");
    }
}
