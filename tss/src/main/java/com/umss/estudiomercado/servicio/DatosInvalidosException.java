package com.umss.estudiomercado.servicio;

import java.util.List;

public class DatosInvalidosException extends RuntimeException {
    private final List<String> detalles;

    public DatosInvalidosException(List<String> detalles) {
        super("Datos inválidos: " + String.join("; ", detalles));
        this.detalles = detalles;
    }

    public List<String> getDetalles() { return detalles; }
}
