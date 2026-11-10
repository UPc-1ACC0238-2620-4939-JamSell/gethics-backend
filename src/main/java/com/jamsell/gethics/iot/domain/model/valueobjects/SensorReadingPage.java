package com.jamsell.gethics.iot.domain.model.valueobjects;

import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import java.util.List;

/** US-37: una pagina de lecturas de un dispositivo, sin depender del tipo de paginacion de la infraestructura. */
public record SensorReadingPage(List<SensorReading> content, int page, int size, long totalElements, int totalPages) {

    public SensorReadingPage {
        content = List.copyOf(content);
    }
}
