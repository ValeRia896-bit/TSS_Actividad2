package com.umss.estudiomercado.controlador;

import com.umss.estudiomercado.servicio.EventosService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Canal de tiempo real (Server-Sent Events). El front se suscribe con
 * {@code new EventSource('/api/eventos')} y, al recibir el evento "datos", vuelve a pedir sus gráficos.
 */
@RestController
@RequestMapping("/api/eventos")
public class EventosController {
    private final EventosService eventos;

    public EventosController(EventosService eventos) {
        this.eventos = eventos;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir() {
        return eventos.suscribir();
    }
}
