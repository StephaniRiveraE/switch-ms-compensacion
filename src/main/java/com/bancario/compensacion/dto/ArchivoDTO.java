package com.bancario.compensacion.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ArchivoDTO {
    private Long id;
    private String nombre;
    private String contenidoXml;
    private LocalDateTime fechaGeneracion;
}
