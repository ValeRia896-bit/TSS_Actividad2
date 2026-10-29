package com.umss.estudiomercado.estadistica;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que el motor estadístico produce los mismos resultados que el "Informe de Calidad y Validación"
 * (cuartiles por mediana de mitades, percentil por rango más cercano, moda, rango...).
 */
class EstadisticaTest {

    private static double num(Map<String, Object> m, String clave) {
        return ((Number) m.get(clave)).doubleValue();
    }

    @Test
    void descriptivaCoincideConElInformeDeCalidad() {
        double[] tiempos = {4, 5, 5, 5, 7, 8, 8, 10, 10, 12, 12, 15, 18, 20, 22};
        Map<String, Object> r = Descriptiva.calcular(tiempos, "Tiempo de espera");
        assertEquals(15, ((Number) r.get("n")).intValue());
        assertEquals(10.0, num(r, "mediana"), 1e-9);
        assertEquals(5.0, num(r, "q1"), 1e-9);
        assertEquals(15.0, num(r, "q3"), 1e-9);
        assertEquals(18.0, num(r, "rango"), 1e-9);
        assertEquals(10.7333, num(r, "media"), 1e-3);
        assertEquals(List.of(5.0), r.get("moda"));
        @SuppressWarnings("unchecked")
        Map<String, Object> perc = (Map<String, Object>) r.get("percentiles");
        assertEquals(20.0, num(perc, "P90"), 1e-9);
    }

    @Test
    void chiCuadradoDeIndependencia() {
        Map<String, Object> r = ChiCuadrado.independencia(List.of("A", "B"), List.of("X", "Y"),
                new long[][]{{10, 20}, {30, 40}}, 0.05, "v1", "v2");
        assertEquals(0.7937, num(r, "chi2"), 1e-3);
        assertEquals(1, ((Number) r.get("gl")).intValue());
        assertEquals(0.373, num(r, "p"), 1e-3);
        assertEquals(3.8415, num(r, "chi2Critico"), 1e-3);
        assertEquals(Boolean.FALSE, r.get("rechazaH0"));
    }

    @Test
    void distribucionesConValoresDeTabla() {
        assertEquals(7.8147, Distribuciones.chi2Inv(0.95, 3), 1e-3);
        assertEquals(2.1448, Distribuciones.tInv(0.975, 14), 1e-3);
    }

    @Test
    void sinDatosNoRompe() {
        Map<String, Object> r = Descriptiva.calcular(new double[0], "Años");
        assertEquals(Boolean.TRUE, r.get("sinDatos"));
        Map<String, Object> c = ChiCuadrado.independencia(List.of("A"), List.of("X"), new long[][]{{3}}, 0.05, "a", "b");
        assertEquals(Boolean.FALSE, c.get("aplicable"));
        assertTrue(c.get("interpretacion").toString().length() > 10);
    }
}
