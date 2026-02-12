package com.bancario.compensacion.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PosicionDTO {
    private Long id;
    private Integer idCiclo;
    private String codigoBic;
    private BigDecimal totalDebitos;
    private BigDecimal totalCreditos;
    private BigDecimal posicionNeta;
}
