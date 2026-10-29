package com.umss.estudiomercado.controlador;

import com.umss.estudiomercado.estadistica.Num;
import com.umss.estudiomercado.servicio.DemoService;
import com.umss.estudiomercado.servicio.EstadisticaService;
import com.umss.estudiomercado.servicio.ExcelService;
import com.umss.estudiomercado.servicio.RegistroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * CRUD, importación de Excel y datos demo. {modulo} = "titulados" | "empleadores".
 */
@RestController
@RequestMapping("/api/{modulo}")
public class RegistroController {
    private final RegistroService registros;
    private final ExcelService excel;
    private final DemoService demo;
    private final EstadisticaService estadistica;

    public RegistroController(RegistroService registros, ExcelService excel, DemoService demo,
                              EstadisticaService estadistica) {
        this.registros = registros;
        this.excel = excel;
        this.demo = demo;
        this.estadistica = estadistica;
    }

    /** Estructura del formulario: campos, tipos, opciones y niveles (para construir formularios y selectores). */
    @GetMapping("/meta")
    public List<Map<String, Object>> meta(@PathVariable String modulo) {
        return estadistica.meta(modulo);
    }

    @GetMapping
    public List<Object> listar(@PathVariable String modulo) {
        return registros.listar(modulo);
    }

    @GetMapping("/{id}")
    public Object obtener(@PathVariable String modulo, @PathVariable Long id) {
        return registros.obtener(modulo, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Object crear(@PathVariable String modulo, @RequestBody Map<String, Object> datos) {
        return registros.crear(modulo, datos);
    }

    @PutMapping("/{id}")
    public Object actualizar(@PathVariable String modulo, @PathVariable Long id, @RequestBody Map<String, Object> datos) {
        return registros.actualizar(modulo, id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String modulo, @PathVariable Long id) {
        registros.eliminar(modulo, id);
    }

    /** Borra TODOS los registros del módulo. Exige confirmar=true. */
    @DeleteMapping
    public Map<String, Object> eliminarTodos(@PathVariable String modulo,
                                             @RequestParam(defaultValue = "false") boolean confirmar) {
        if (!confirmar) throw new IllegalArgumentException("Para borrar todos los registros agregue ?confirmar=true");
        return Num.mapa("eliminados", registros.eliminarTodos(modulo));
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> importar(@PathVariable String modulo, @RequestParam("archivo") MultipartFile archivo) {
        return excel.importar(modulo, archivo);
    }

    /** Genera registros sintéticos (origenDato=DEMO) para probar gráficos. */
    @PostMapping("/demo")
    public Map<String, Object> generarDemo(@PathVariable String modulo, @RequestParam(defaultValue = "60") int cantidad) {
        return Num.mapa("generados", demo.generar(modulo, cantidad));
    }

    @DeleteMapping("/demo")
    public Map<String, Object> borrarDemo(@PathVariable String modulo) {
        return Num.mapa("eliminados", registros.eliminarPorOrigen(modulo, "DEMO"));
    }
}
