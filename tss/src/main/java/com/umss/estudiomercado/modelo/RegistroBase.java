package com.umss.estudiomercado.modelo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;

/** Campos comunes de todo registro de encuesta. */
@MappedSuperclass
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
public abstract class RegistroBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaRegistro;

    /** FORMULARIO, EXCEL o DEMO (trazabilidad). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(length = 20)
    private String origenDato = "FORMULARIO";

    @PrePersist
    void alCrear() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }

    public String getOrigenDato() { return origenDato; }

    public void setOrigenDato(String origenDato) { this.origenDato = origenDato; }
}
