package com.bancario.compensacion.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "cicloCompensacion")
@Getter
@Setter
public class CicloCompensacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCiclo")
    private Integer idCiclo;

    @Column(name = "numeroCiclo")
    private Integer numeroCiclo;

    @Column(length = 20)
    private String estado; // OPEN, CLOSED, SETTLED

    @Column(name = "fechaApertura")
    private LocalDateTime fechaApertura;

    @Column(name = "fechaCierre")
    private LocalDateTime fechaCierre;
}
