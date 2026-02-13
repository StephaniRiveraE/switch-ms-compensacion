package com.bancario.compensacion.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ArchivoDTO {
    private Long id;
    private String nombre;
<<<<<<< HEAD
    private String contenidoXml;
=======
    private String xmlContenido;

    private String canalEnvio;
    private String estado;
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a
    private LocalDateTime fechaGeneracion;
}
