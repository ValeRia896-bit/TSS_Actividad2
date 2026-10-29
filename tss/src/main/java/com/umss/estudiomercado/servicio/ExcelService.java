package com.umss.estudiomercado.servicio;

import com.umss.estudiomercado.estadistica.Num;
import com.umss.estudiomercado.modelo.RegistroBase;
import com.umss.estudiomercado.servicio.Meta.CampoInfo;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importa Excel (.xlsx/.xls) reconociendo las columnas por el texto de la pregunta (no por la posición),
 * tolerando columnas faltantes, celdas vacías, filas vacías y columnas extra (Id, Hora de inicio, etc.).
 */
@Service
public class ExcelService {
    private static final int MAX_MENSAJES = 50;
    private static final Pattern NUMERO = Pattern.compile("-?\\d+(?:[.,]\\d+)?");
    /** Coma fuera de paréntesis: separa opciones de una selección múltiple exportada por Google Forms. */
    private static final Pattern COMA_EXTERNA = Pattern.compile(",\\s*(?![^()]*\\))");

    private final RegistroService registros;

    public ExcelService(RegistroService registros) {
        this.registros = registros;
    }

    public Map<String, Object> importar(String modulo, MultipartFile archivo) {
        Class<?> clase = registros.clase(modulo);
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        if (archivo.isEmpty()) throw new IllegalArgumentException("El archivo está vacío.");
        if (!nombre.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls|xlsm)$"))
            throw new IllegalArgumentException("Formato no soportado: suba un archivo Excel (.xlsx o .xls).");

        List<String> avisos = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        List<Object> validos = new ArrayList<>();
        List<Map<String, Object>> reconocidas = new ArrayList<>();
        List<String> ignoradas = new ArrayList<>();
        int leidas = 0, vacias = 0, rechazadas = 0, totalAvisos = 0, totalErrores = 0;
        String hoja;
        int filaEncabezado;

        try (InputStream in = archivo.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = null;
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet s = wb.getSheetAt(i);
                if (s.getPhysicalNumberOfRows() > 0) { sheet = s; break; }
            }
            if (sheet == null) throw new IllegalArgumentException("El Excel no contiene datos.");
            hoja = sheet.getSheetName();
            DataFormatter fmt = new DataFormatter(Locale.US);
            FormulaEvaluator ev = wb.getCreationHelper().createFormulaEvaluator();

            // Fila de encabezados: la primera con al menos 2 celdas con texto (dentro de las 10 primeras)
            Row cab = null;
            for (int r = sheet.getFirstRowNum(); r <= Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + 10); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int llenas = 0;
                for (Cell c : row) if (!texto(fmt, ev, c).isBlank()) llenas++;
                if (llenas >= 2) { cab = row; break; }
            }
            if (cab == null) throw new IllegalArgumentException("No se encontró una fila de encabezados en el Excel.");
            filaEncabezado = cab.getRowNum() + 1;

            // Emparejar columnas con campos
            Map<Integer, CampoInfo> columnas = new LinkedHashMap<>();
            Map<String, Integer> usados = new LinkedHashMap<>();
            for (Cell c : cab) {
                String enc = texto(fmt, ev, c);
                if (enc.isBlank()) continue;
                CampoInfo campo = emparejar(clase, enc);
                if (campo == null || usados.containsKey(campo.nombre())) { ignoradas.add(enc); continue; }
                usados.put(campo.nombre(), c.getColumnIndex());
                columnas.put(c.getColumnIndex(), campo);
                reconocidas.add(Num.mapa("columna", enc, "campo", campo.nombre(), "etiqueta", campo.etiqueta()));
            }
            if (columnas.isEmpty())
                throw new IllegalArgumentException("Ninguna columna del Excel coincide con las preguntas de la encuesta de " + modulo + ".");

            for (int r = cab.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) { vacias++; continue; }
                Map<Integer, String> valores = new LinkedHashMap<>();
                boolean algo = false;
                for (Integer col : columnas.keySet()) {
                    String v = texto(fmt, ev, row.getCell(col)).trim();
                    valores.put(col, v);
                    if (!v.isEmpty()) algo = true;
                }
                if (!algo) { vacias++; continue; }
                leidas++;
                int numFila = r + 1;

                Object registro;
                try {
                    registro = clase.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                List<String> problemas = new ArrayList<>();
                for (Map.Entry<Integer, String> e : valores.entrySet()) {
                    if (e.getValue().isEmpty()) continue;
                    asignar(columnas.get(e.getKey()), registro, e.getValue(), problemas);
                }
                registros.normalizar(registro, problemas);
                for (String p : problemas) {
                    totalAvisos++;
                    if (avisos.size() < MAX_MENSAJES) avisos.add("Fila " + numFila + ": " + p);
                }
                List<String> invalidos = registros.validar(registro);
                if (!invalidos.isEmpty()) {
                    rechazadas++;
                    totalErrores += invalidos.size();
                    for (String m : invalidos) if (errores.size() < MAX_MENSAJES) errores.add("Fila " + numFila + ": " + m);
                    continue;
                }
                ((RegistroBase) registro).setOrigenDato("EXCEL");
                validos.add(registro);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo Excel: " + e.getMessage());
        }

        registros.guardarLote(modulo, validos, "importado");
        return Num.mapa("archivo", nombre, "hoja", hoja, "filaEncabezado", filaEncabezado,
                "filasLeidas", leidas, "importadas", validos.size(), "rechazadas", rechazadas, "filasVacias", vacias,
                "columnasReconocidas", reconocidas, "columnasIgnoradas", ignoradas,
                "totalAdvertencias", totalAvisos, "advertencias", avisos,
                "totalErrores", totalErrores, "errores", errores);
    }

    private static String texto(DataFormatter fmt, FormulaEvaluator ev, Cell c) {
        if (c == null) return "";
        try {
            return fmt.formatCellValue(c, ev);
        } catch (RuntimeException e) {
            return fmt.formatCellValue(c);
        }
    }

    /** Campo cuya clave (la más larga) está contenida en el encabezado, o cuyo nombre/etiqueta coincide. */
    private static CampoInfo emparejar(Class<?> clase, String encabezado) {
        String h = Meta.norm(encabezado);
        CampoInfo mejor = null;
        int largo = 0;
        for (CampoInfo c : Meta.campos(clase)) {
            if (h.equals(Meta.norm(c.nombre())) || h.equals(Meta.norm(c.etiqueta()))) return c;
            for (String clave : c.claves()) {
                String k = Meta.norm(clave);
                if (!k.isEmpty() && h.contains(k) && k.length() > largo) { mejor = c; largo = k.length(); }
            }
        }
        return mejor;
    }

    private void asignar(CampoInfo c, Object registro, String crudo, List<String> problemas) {
        switch (c.tipo()) {
            case NUMERICO -> {
                Matcher m = NUMERO.matcher(crudo);
                if (m.find()) Meta.set(c, registro, Double.parseDouble(m.group().replace(',', '.')));
                else problemas.add("«" + c.etiqueta() + "»: valor no numérico '" + crudo + "' (ignorado)");
            }
            case LIKERT -> {
                String n = Meta.norm(crudo);
                int idx = -1;
                for (int i = 0; i < c.niveles().size(); i++) if (Meta.norm(c.niveles().get(i)).equals(n)) idx = i + 1;
                if (idx < 0 && n.matches("\\d+")) idx = Integer.parseInt(n);
                if (idx > 0) Meta.set(c, registro, idx);
                else problemas.add("«" + c.etiqueta() + "»: valor no reconocido '" + crudo + "' (ignorado)");
            }
            case BOOLEANO -> {
                String n = Meta.norm(crudo);
                if (n.equals("si") || n.equals("s") || n.equals("true") || n.equals("yes") || n.equals("1"))
                    Meta.set(c, registro, Boolean.TRUE);
                else if (n.equals("no") || n.equals("false") || n.equals("0")) Meta.set(c, registro, Boolean.FALSE);
                else problemas.add("«" + c.etiqueta() + "»: se esperaba Sí/No y llegó '" + crudo + "' (ignorado)");
            }
            case MULTIPLE -> {
                String v = crudo.contains(";") ? crudo : String.join(";", COMA_EXTERNA.split(crudo));
                Meta.set(c, registro, v);
            }
            default -> Meta.set(c, registro, crudo);
        }
    }
}
