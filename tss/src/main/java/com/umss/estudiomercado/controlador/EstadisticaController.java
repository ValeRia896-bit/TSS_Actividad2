package com.umss.estudiomercado.controlador;

import com.umss.estudiomercado.servicio.EstadisticaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Datos para los gráficos y el análisis. Todo se calcula al momento sobre los datos actuales. */
@RestController
@RequestMapping("/api/{modulo}/estadisticas")
public class EstadisticaController {
    private final EstadisticaService servicio;

    public EstadisticaController(EstadisticaService servicio) {
        this.servicio = servicio;
    }

    /** Total de registros, completitud por campo (datos faltantes) y origen de los datos. */
    @GetMapping("/resumen")
    public Map<String, Object> resumen(@PathVariable String modulo) {
        return servicio.resumen(modulo);
    }

    /** Tabla de frecuencias (fi, hi, Fi, Hi) de una pregunta categórica, Sí/No, Likert o múltiple. */
    @GetMapping("/frecuencia")
    public Map<String, Object> frecuencia(@PathVariable String modulo, @RequestParam String campo) {
        return servicio.frecuencia(modulo, campo);
    }

    /** Todas las tablas de frecuencia del módulo en una sola respuesta. */
    @GetMapping("/frecuencias")
    public List<Map<String, Object>> frecuencias(@PathVariable String modulo) {
        return servicio.frecuencias(modulo);
    }

    /** Descriptivo de todas las variables numéricas del módulo en una sola respuesta. */
    @GetMapping("/descriptivos")
    public List<Map<String, Object>> descriptivos(@PathVariable String modulo) {
        return servicio.descriptivos(modulo);
    }

    /** Estadística descriptiva completa de una variable numérica (histograma, boxplot, puntos vs media...). */
    @GetMapping("/descriptivo")
    public Map<String, Object> descriptivo(@PathVariable String modulo, @RequestParam String campo) {
        return servicio.descriptivo(modulo, campo);
    }

    /** χ² de independencia entre dos variables categóricas (tabla de contingencia, esperados, residuos). */
    @GetMapping("/chi2")
    public Map<String, Object> chi2(@PathVariable String modulo, @RequestParam String a, @RequestParam String b,
                                    @RequestParam(required = false) Double alfa) {
        return servicio.chi2(modulo, a, b, alfa);
    }

    /** χ² de bondad de ajuste (distribución uniforme) de una variable categórica. */
    @GetMapping("/chi2-bondad")
    public Map<String, Object> bondad(@PathVariable String modulo, @RequestParam String campo,
                                      @RequestParam(required = false) Double alfa) {
        return servicio.bondad(modulo, campo, alfa);
    }

    /** χ² de todos los pares de variables categóricas, ordenado por p (mapa de calor de asociaciones). */
    @GetMapping("/asociaciones")
    public Map<String, Object> asociaciones(@PathVariable String modulo, @RequestParam(required = false) Double alfa) {
        return servicio.asociaciones(modulo, alfa);
    }

    /** Media, desviación y distribución de todas las escalas Likert (radar / barras). */
    @GetMapping("/likert")
    public Map<String, Object> likert(@PathVariable String modulo) {
        return servicio.likert(modulo);
    }

    /** Dispersión de dos variables numéricas con correlación de Pearson y recta de regresión. */
    @GetMapping("/dispersion")
    public Map<String, Object> dispersion(@PathVariable String modulo, @RequestParam String x, @RequestParam String y) {
        return servicio.dispersion(modulo, x, y);
    }

    /** Media de una variable numérica por grupos de una categórica, con ANOVA. */
    @GetMapping("/medias-por-grupo")
    public Map<String, Object> mediasPorGrupo(@PathVariable String modulo, @RequestParam String numerico,
                                              @RequestParam String grupo) {
        return servicio.mediasPorGrupo(modulo, numerico, grupo);
    }

    /** Análisis automático en lenguaje natural de todos los datos. */
    @GetMapping("/informe")
    public Map<String, Object> informe(@PathVariable String modulo) {
        return servicio.informe(modulo);
    }
}
