package com.umss.estudiomercado.servicio;

import com.umss.estudiomercado.modelo.Campo;
import com.umss.estudiomercado.modelo.Campo.Tipo;

import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Acceso por reflexión a los campos anotados con @Campo. */
public final class Meta {
    private Meta() {}

    public record CampoInfo(String nombre, String etiqueta, Tipo tipo, List<String> opciones,
                            List<String> niveles, List<String> claves, Field field) {
        /** Admite tabla de frecuencias y chi-cuadrado como variable categórica. */
        public boolean categorico() {
            return tipo == Tipo.CATEGORICO || tipo == Tipo.BOOLEANO || tipo == Tipo.LIKERT;
        }

        /** Admite estadística descriptiva numérica. */
        public boolean numerico() {
            return tipo == Tipo.NUMERICO || tipo == Tipo.LIKERT;
        }

        public boolean graficable() {
            return tipo != Tipo.TEXTO && tipo != Tipo.CONTACTO;
        }
    }

    private static final Map<Class<?>, List<CampoInfo>> CACHE = new ConcurrentHashMap<>();

    public static List<CampoInfo> campos(Class<?> clase) {
        return CACHE.computeIfAbsent(clase, c -> {
            List<CampoInfo> lista = new ArrayList<>();
            for (Field f : c.getDeclaredFields()) {
                Campo a = f.getAnnotation(Campo.class);
                if (a == null) continue;
                f.setAccessible(true);
                lista.add(new CampoInfo(f.getName(), a.etiqueta(), a.tipo(), List.of(a.opciones()),
                        List.of(a.niveles()), List.of(a.claves()), f));
            }
            return List.copyOf(lista);
        });
    }

    public static CampoInfo campo(Class<?> clase, String nombre) {
        return campos(clase).stream().filter(c -> c.nombre().equals(nombre)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Campo desconocido: '" + nombre + "'. Consulte GET /api/{modulo}/meta para ver los campos disponibles."));
    }

    public static Object get(CampoInfo c, Object objeto) {
        try {
            return c.field().get(objeto);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void set(CampoInfo c, Object objeto, Object valor) {
        try {
            c.field().set(objeto, valor);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Minúsculas, sin tildes ni signos, espacios simples. Sirve para comparar textos y cabeceras de Excel. */
    public static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
