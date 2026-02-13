package com.bancario.compensacion.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "archivoLiquidacion")
@Getter
@Setter
public class ArchivoLiquidacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idArchivo")
    private Long idArchivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCiclo")
    private CicloCompensacion ciclo;

    @Column(name = "nombreArchivo")
    private String nombreArchivo;

    @Column(name = "contenidoXml", columnDefinition = "TEXT")
    private String contenidoXml;

    @Column(name = "fechaGeneracion")
    private LocalDateTime fechaGeneracion;
}
