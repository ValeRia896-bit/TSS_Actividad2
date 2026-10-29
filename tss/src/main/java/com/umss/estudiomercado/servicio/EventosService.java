package com.umss.estudiomercado.servicio;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tiempo real mediante Server-Sent Events: cada vez que cambian los datos se avisa a todos los
 * navegadores conectados para que vuelvan a pedir sus gráficos.
 */
@Service
public class EventosService {
    private final List<SseEmitter> emisores = new CopyOnWriteArrayList<>();

    public SseEmitter suscribir() {
        SseEmitter emisor = new SseEmitter(30 * 60 * 1000L);
        emisores.add(emisor);
        emisor.onCompletion(() -> emisores.remove(emisor));
        emisor.onTimeout(() -> emisores.remove(emisor));
        emisor.onError(e -> emisores.remove(emisor));
        try {
            emisor.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (IOException e) {
            emisores.remove(emisor);
        }
        return emisor;
    }

    public void publicar(String modulo, String accion) {
        Map<String, Object> datos = Map.of("modulo", modulo, "accion", accion, "ts", System.currentTimeMillis());
        for (SseEmitter e : emisores) {
            try {
                e.send(SseEmitter.event().name("datos").data(datos));
            } catch (Exception ex) {
                emisores.remove(e);
            }
        }
    }

    /** Latido para que proxies y navegadores no cierren la conexión por inactividad. */
    @Scheduled(fixedRate = 25000)
    void latido() {
        for (SseEmitter e : emisores) {
            try {
                e.send(SseEmitter.event().comment("ping"));
            } catch (Exception ex) {
                emisores.remove(e);
            }
        }
    }
}
