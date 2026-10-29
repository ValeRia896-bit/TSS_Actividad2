package com.umss.estudiomercado.estadistica;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Utilidades de redondeo / formato. Nunca devuelve NaN ni Infinity (devuelve null) para no romper el JSON. */
public final class Num {
    private Num() {}

    public static Double r(double v, int decimales) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        return BigDecimal.valueOf(v).setScale(decimales, RoundingMode.HALF_UP).doubleValue();
    }

    public static Double r2(double v) { return r(v, 2); }

    public static Double r4(double v) { return r(v, 4); }

    /** Número legible para textos: 11.3, 10, 0.25 */
    public static String f(double v) {
        Double x = r(v, 2);
        if (x == null) return "—";
        return BigDecimal.valueOf(x).stripTrailingZeros().toPlainString();
    }

    public static String pct(double fraccion) { return f(fraccion * 100) + "%"; }

    public static String p(double p) {
        return p < 0.0001 ? "< 0.0001" : String.format(Locale.ROOT, "%.4f", p);
    }

    /** Mapa ordenado a partir de pares clave, valor. */
    public static Map<String, Object> mapa(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
