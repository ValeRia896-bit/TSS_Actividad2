package com.umss.estudiomercado.modelo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadatos de cada pregunta de la encuesta. Con esto el sistema sabe, sin código extra, cómo
 * validarla, importarla desde Excel y qué gráficos/estadísticos admite.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Campo {
    enum Tipo {
        /** Opción única (texto). */
        CATEGORICO,
        /** Selección múltiple: valores separados por ';'. */
        MULTIPLE,
        /** Número continuo (años, tiempos...). */
        NUMERICO,
        /** Escala ordinal guardada como entero 1..n según 'niveles'. */
        LIKERT,
        /** Sí / No. */
        BOOLEANO,
        /** Texto libre (no se grafica). */
        TEXTO,
        /** Dato de contacto (no se grafica). */
        CONTACTO
    }

    Tipo tipo();

    /** Pregunta abreviada, para etiquetas de gráficos. */
    String etiqueta();

    /** Opciones cerradas permitidas (dropdown). Vacío = libre. */
    String[] opciones() default {};

    /** Para LIKERT: nivel 1, 2, 3... en orden. */
    String[] niveles() default {};

    /** Fragmentos (sin tildes ni mayúsculas) que identifican la columna en un Excel. */
    String[] claves() default {};
}
