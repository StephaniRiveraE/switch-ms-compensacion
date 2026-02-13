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

<<<<<<< HEAD
    @Column(name = "fechaGeneracion")
=======
    @Column(name = "canal_envio")
    private String canalEnvio;

    private String estado;

    @Column(name = "fecha_generacion")
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a
    private LocalDateTime fechaGeneracion;
}
