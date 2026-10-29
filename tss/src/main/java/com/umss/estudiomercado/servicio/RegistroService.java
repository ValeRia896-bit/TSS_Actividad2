package com.umss.estudiomercado.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umss.estudiomercado.modelo.Empleador;
import com.umss.estudiomercado.modelo.RegistroBase;
import com.umss.estudiomercado.modelo.Titulado;
import com.umss.estudiomercado.repositorio.EmpleadorRepository;
import com.umss.estudiomercado.repositorio.RegistroRepository;
import com.umss.estudiomercado.repositorio.TituladoRepository;
import com.umss.estudiomercado.servicio.Meta.CampoInfo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** CRUD genérico de ambos módulos (titulados / empleadores) con normalización y validación. */
@Service
public class RegistroService {
    private final Map<String, RegistroRepository<Object>> repos = new HashMap<>();
    private final Map<String, Class<?>> clases = Map.of("titulados", Titulado.class, "empleadores", Empleador.class);
    private final ObjectMapper mapper;
    private final Validator validator;
    private final EventosService eventos;

    @SuppressWarnings("unchecked")
    public RegistroService(TituladoRepository titulados, EmpleadorRepository empleadores,
                           ObjectMapper mapper, Validator validator, EventosService eventos) {
        repos.put("titulados", (RegistroRepository<Object>) (RegistroRepository<?>) titulados);
        repos.put("empleadores", (RegistroRepository<Object>) (RegistroRepository<?>) empleadores);
        this.mapper = mapper;
        this.validator = validator;
        this.eventos = eventos;
    }

    public Class<?> clase(String modulo) {
        Class<?> c = clases.get(modulo);
        if (c == null) throw new NoSuchElementException("Módulo desconocido: '" + modulo + "' (use 'titulados' o 'empleadores')");
        return c;
    }

    public List<Object> listar(String modulo) {
        clase(modulo);
        return repos.get(modulo).findAll(Sort.by("id"));
    }

    public Object obtener(String modulo, Long id) {
        clase(modulo);
        return repos.get(modulo).findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe el registro " + id + " en " + modulo));
    }

    public Object crear(String modulo, Map<String, Object> datos) {
        Object registro = convertir(modulo, datos);
        prepararYValidar(registro);
        ((RegistroBase) registro).setOrigenDato("FORMULARIO");
        Object guardado = repos.get(modulo).save(registro);
        eventos.publicar(modulo, "creado");
        return guardado;
    }

    public Object actualizar(String modulo, Long id, Map<String, Object> datos) {
        Object existente = obtener(modulo, id);
        Object nuevo = convertir(modulo, datos);
        prepararYValidar(nuevo);
        for (CampoInfo c : Meta.campos(existente.getClass())) Meta.set(c, existente, Meta.get(c, nuevo));
        Object guardado = repos.get(modulo).save(existente);
        eventos.publicar(modulo, "actualizado");
        return guardado;
    }

    public void eliminar(String modulo, Long id) {
        obtener(modulo, id);
        repos.get(modulo).deleteById(id);
        eventos.publicar(modulo, "eliminado");
    }

    public long eliminarTodos(String modulo) {
        clase(modulo);
        long n = repos.get(modulo).count();
        repos.get(modulo).deleteAllInBatch();
        eventos.publicar(modulo, "vaciado");
        return n;
    }

    public long eliminarPorOrigen(String modulo, String origen) {
        clase(modulo);
        long n = repos.get(modulo).deleteByOrigenDato(origen);
        eventos.publicar(modulo, "vaciado");
        return n;
    }

    public void guardarLote(String modulo, List<Object> registros, String accion) {
        clase(modulo);
        if (registros.isEmpty()) return;
        repos.get(modulo).saveAll(registros);
        eventos.publicar(modulo, accion);
    }

    // ---------- Normalización y validación ----------

    private Object convertir(String modulo, Map<String, Object> datos) {
        try {
            return mapper.convertValue(datos, clase(modulo));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formato de datos inválido: " + e.getMessage());
        }
    }

    private void prepararYValidar(Object registro) {
        List<String> problemas = new ArrayList<>();
        normalizar(registro, problemas);
        problemas.addAll(validar(registro));
        if (!problemas.isEmpty()) throw new DatosInvalidosException(problemas);
    }

    /**
     * Limpia espacios, vacíos a null y lleva cada texto a la opción oficial de la encuesta
     * (sin importar mayúsculas ni tildes). Los valores de opción única que no coinciden con ninguna
     * opción se agregan a {@code noReconocidos}.
     */
    public void normalizar(Object registro, List<String> noReconocidos) {
        for (CampoInfo c : Meta.campos(registro.getClass())) {
            Object v = Meta.get(c, registro);
            if (!(v instanceof String s)) continue;
            String t = s.trim().replaceAll("\\s+", " ");
            if (t.isEmpty()) {
                Meta.set(c, registro, null);
                continue;
            }
            switch (c.tipo()) {
                case CATEGORICO -> {
                    String canon = canonico(c.opciones(), t);
                    if (canon != null) t = canon;
                    else if (!c.opciones().isEmpty())
                        noReconocidos.add("«" + c.etiqueta() + "»: valor no reconocido '" + t + "'");
                    Meta.set(c, registro, t);
                }
                case MULTIPLE -> {
                    Set<String> set = new LinkedHashSet<>();
                    for (String parte : t.split(";")) {
                        String q = parte.trim();
                        if (q.isEmpty()) continue;
                        String canon = canonico(c.opciones(), q);
                        set.add(canon != null ? canon : q); // lo no listado se conserva como "Otro"
                    }
                    Meta.set(c, registro, set.isEmpty() ? null : String.join(";", set));
                }
                default -> Meta.set(c, registro, t);
            }
        }
    }

    private static String canonico(List<String> opciones, String valor) {
        String n = Meta.norm(valor);
        for (String o : opciones) if (Meta.norm(o).equals(n)) return o;
        return null;
    }

    /** Validaciones de rango y coherencia (Bean Validation). Devuelve la lista de errores. */
    public List<String> validar(Object registro) {
        List<String> errores = new ArrayList<>();
        for (ConstraintViolation<Object> v : validator.validate(registro)) {
            String propiedad = v.getPropertyPath().toString();
            String etiqueta = Meta.campos(registro.getClass()).stream()
                    .filter(c -> c.nombre().equals(propiedad)).map(CampoInfo::etiqueta).findFirst().orElse(null);
            errores.add(etiqueta != null ? "«" + etiqueta + "»: " + v.getMessage() : v.getMessage());
        }
        return errores;
    }
}
