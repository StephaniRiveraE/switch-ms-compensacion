package com.bancario.compensacion.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "posicionInstitucion")
@Getter
@Setter
public class PosicionInstitucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPosicion")
    private Long idPosicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCiclo")
    private CicloCompensacion ciclo;

    @Column(name = "bic", length = 20)
    private String bic;

    @Column(name = "totalDebitos", precision = 18, scale = 2)
    private BigDecimal totalDebitos = BigDecimal.ZERO;

    @Column(name = "totalCredits", precision = 18, scale = 2)
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "posicionNeta", precision = 18, scale = 2)
    private BigDecimal posicionNeta = BigDecimal.ZERO;

    public void recalcularNeto() {
        this.posicionNeta = this.totalCredits.subtract(this.totalDebitos);
    }
}
