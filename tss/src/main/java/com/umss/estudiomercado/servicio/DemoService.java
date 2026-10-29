package com.umss.estudiomercado.servicio;

import com.umss.estudiomercado.modelo.RegistroBase;
import com.umss.estudiomercado.modelo.Titulado;
import com.umss.estudiomercado.servicio.Meta.CampoInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Genera registros sintéticos (origenDato = DEMO) para probar gráficos y análisis sin esperar datos reales.
 * Se pueden borrar sin tocar los datos reales.
 */
@Service
public class DemoService {
    private static final Map<String, List<String>> EXTRA = Map.of(
            "edad", List.of("Menos de 25 años", "25 a 29 años", "30 a 34 años", "35 años o más"),
            "genero", List.of("Hombre", "Mujer"));

    private final RegistroService registros;
    private final Random azar = new Random();

    public DemoService(RegistroService registros) {
        this.registros = registros;
    }

    public int generar(String modulo, int cantidad) {
        Class<?> clase = registros.clase(modulo);
        int n = Math.max(1, Math.min(cantidad, 1000));
        List<Object> lote = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Object reg;
            try {
                reg = clase.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            for (CampoInfo c : Meta.campos(clase)) Meta.set(c, reg, valor(c));
            if (reg instanceof Titulado) {
                // Coherencia: el desempleo es una fracción de los años de vida profesional
                CampoInfo vida = Meta.campo(clase, "aniosVidaProfesional");
                CampoInfo desempleo = Meta.campo(clase, "aniosDesempleo");
                Double anios = (Double) Meta.get(vida, reg);
                Meta.set(desempleo, reg, anios == null ? null : Math.round(anios * azar.nextDouble() * 0.3 * 10) / 10.0);
            }
            ((RegistroBase) reg).setOrigenDato("DEMO");
            lote.add(reg);
        }
        registros.guardarLote(modulo, lote, "demo");
        return n;
    }

    private Object valor(CampoInfo c) {
        switch (c.tipo()) {
            case CATEGORICO: {
                List<String> op = c.opciones().isEmpty() ? EXTRA.getOrDefault(c.nombre(), List.of()) : c.opciones();
                return op.isEmpty() ? null : sesgado(op);
            }
            case MULTIPLE: {
                List<String> op = new ArrayList<>(c.opciones());
                Collections.shuffle(op, azar);
                int k = 1 + azar.nextInt(Math.min(3, op.size()));
                return String.join(";", op.subList(0, k));
            }
            case BOOLEANO:
                return azar.nextDouble() < 0.6;
            case LIKERT: {
                int max = c.niveles().size();
                double u = azar.nextDouble();
                int v = (int) Math.ceil(max * Math.pow(u, 0.6)); // sesgo hacia valores altos
                return Math.max(1, Math.min(max, v));
            }
            case NUMERICO: {
                double v = -Math.log(1 - azar.nextDouble()) * 5; // exponencial: asimetría a la derecha
                return Math.round(Math.min(v, 40) * 10) / 10.0;
            }
            case TEXTO:
                return "Dato de demostración";
            default:
                return null;
        }
    }

    /** Elige con preferencia por las primeras opciones, para que haya categorías dominantes. */
    private String sesgado(List<String> op) {
        double u = azar.nextDouble();
        int i = (int) Math.floor(op.size() * u * u);
        return op.get(Math.min(i, op.size() - 1));
    }
}
