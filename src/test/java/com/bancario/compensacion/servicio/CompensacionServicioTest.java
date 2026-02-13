package com.bancario.compensacion.servicio;

import com.bancario.compensacion.dto.ArchivoDTO;
import com.bancario.compensacion.mapper.CompensacionMapper;
import com.bancario.compensacion.modelo.*;
import com.bancario.compensacion.repositorio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompensacionServicioTest {

    @Mock
    private CicloCompensacionRepositorio cicloRepo;
    @Mock
    private PosicionInstitucionRepositorio posicionRepo;
    @Mock
    private ArchivoLiquidacionRepositorio archivoRepo;
    // SeguridadServicio is no longer used
    @Mock
    private DetalleCompensacionRepositorio detalleRepo;
    @Mock
    private CompensacionMapper mapper;
    @Mock
    private TaskScheduler taskScheduler;

    @InjectMocks
    private CompensacionServicio servicio;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRealizarCierreDiario_NettingLogic() {
        // Arrange
        Integer cicloId = 1;
        CicloCompensacion ciclo = new CicloCompensacion();
<<<<<<< HEAD
        ciclo.setIdCiclo(cicloId);
=======
        ciclo.setId(cicloId);
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a
        ciclo.setNumeroCiclo(100);
        ciclo.setEstado("ABIERTO");

        when(cicloRepo.findById(cicloId)).thenReturn(Optional.of(ciclo));
        when(cicloRepo.save(any(CicloCompensacion.class))).thenAnswer(i -> i.getArguments()[0]);

        // Mock Positions (initial state - will be reset)
        PosicionInstitucion posBankA = new PosicionInstitucion();
<<<<<<< HEAD
        posBankA.setBic("BANKA");
        posBankA.setPosicionNeta(BigDecimal.ZERO);
        posBankA.setTotalDebitos(BigDecimal.ZERO);
        posBankA.setTotalCredits(BigDecimal.ZERO);

        PosicionInstitucion posBankB = new PosicionInstitucion();
        posBankB.setBic("BANKB");
        posBankB.setPosicionNeta(BigDecimal.ZERO);
        posBankB.setTotalDebitos(BigDecimal.ZERO);
        posBankB.setTotalCredits(BigDecimal.ZERO);

        List<PosicionInstitucion> posiciones = Arrays.asList(posBankA, posBankB);
        when(posicionRepo.findByCicloIdCiclo(cicloId)).thenReturn(posiciones);

        // Mock Details
=======
        posBankA.setCodigoBic("BANKA");
        posBankA.setNeto(BigDecimal.ZERO);
        posBankA.setTotalDebitos(BigDecimal.ZERO);
        posBankA.setTotalCreditos(BigDecimal.ZERO);

        PosicionInstitucion posBankB = new PosicionInstitucion();
        posBankB.setCodigoBic("BANKB");
        posBankB.setNeto(BigDecimal.ZERO);
        posBankB.setTotalDebitos(BigDecimal.ZERO);
        posBankB.setTotalCreditos(BigDecimal.ZERO);

        List<PosicionInstitucion> posiciones = Arrays.asList(posBankA, posBankB);
        when(posicionRepo.findByCicloId(cicloId)).thenReturn(posiciones);

        // Mock Details
        // 1. PAGO: A pays B 100. -> A Debit 100, B Credit 100.
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a
        DetalleCompensacion det1 = new DetalleCompensacion();
        det1.setTipoOperacion("PAGO");
        det1.setBicEmisor("BANKA");
        det1.setBicReceptor("BANKB");
        det1.setMonto(new BigDecimal("100.00"));
        det1.setEstadoLiquidacion("INCLUIDO");

<<<<<<< HEAD
=======
        // 2. REVERSO: A (original sender) gets refund from B?
        // Logic: "REVERSO" means Credit Emisor, Debit Receptor.
        // If "Emisor" in detail is still BANKA (the original sender), then:
        // BANKA Credit 20 (refund), BANKB Debit 20 (pay back).
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a
        DetalleCompensacion det2 = new DetalleCompensacion();
        det2.setTipoOperacion("REVERSO");
        det2.setBicEmisor("BANKA");
        det2.setBicReceptor("BANKB");
        det2.setMonto(new BigDecimal("20.00"));
        det2.setEstadoLiquidacion("INCLUIDO");

<<<<<<< HEAD
        when(detalleRepo.findByCicloIdCiclo(cicloId)).thenReturn(Arrays.asList(det1, det2));
=======
        when(detalleRepo.findByCicloId(cicloId)).thenReturn(Arrays.asList(det1, det2));
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a

        when(archivoRepo.save(any(ArchivoLiquidacion.class))).thenAnswer(i -> i.getArguments()[0]);
        when(mapper.toDTO(any(ArchivoLiquidacion.class))).thenReturn(ArchivoDTO.builder().build());

        // Act
        servicio.realizarCierreDiario(cicloId, 10);

        // Assert Netting Logic
<<<<<<< HEAD
        assertEquals(new BigDecimal("100.00"), posBankA.getTotalDebitos());
        assertEquals(new BigDecimal("20.00"), posBankA.getTotalCredits());
        assertEquals(new BigDecimal("-80.00"), posBankA.getPosicionNeta());

        assertEquals(new BigDecimal("20.00"), posBankB.getTotalDebitos());
        assertEquals(new BigDecimal("100.00"), posBankB.getTotalCredits());
        assertEquals(new BigDecimal("80.00"), posBankB.getPosicionNeta());

        // Assert XML
        verify(archivoRepo).save(argThat(
                archivo -> archivo.getContenidoXml().contains("<NetPosition>-80.00</NetPosition>") &&
                        archivo.getContenidoXml().contains("<NetPosition>80.00</NetPosition>")));
=======
        // BANKA:
        // PAGO (Debit 100) -> -100
        // REVERSO (Credit 20) -> +20
        // Net: -80
        // Expected TotalDebitos = 100, TotalCreditos = 20
        assertEquals(new BigDecimal("100.00"), posBankA.getTotalDebitos());
        assertEquals(new BigDecimal("20.00"), posBankA.getTotalCreditos());
        assertEquals(new BigDecimal("-80.00"), posBankA.getNeto());

        // BANKB:
        // PAGO (Credit 100) -> +100
        // REVERSO (Debit 20) -> -20
        // Net: +80
        // Expected TotalDebitos = 20, TotalCreditos = 100
        assertEquals(new BigDecimal("20.00"), posBankB.getTotalDebitos());
        assertEquals(new BigDecimal("100.00"), posBankB.getTotalCreditos());
        assertEquals(new BigDecimal("80.00"), posBankB.getNeto());

        // Assert XML Signature Removal
        verify(archivoRepo).save(argThat(
                archivo -> archivo.getXmlContenido().contains("<NetPosition currency=\"USD\">-80.00</NetPosition>") &&
                        archivo.getXmlContenido().contains("<NetPosition currency=\"USD\">80.00</NetPosition>")));
>>>>>>> e802ba3afe0f10f8ab8394d63f83f08d1ca3003a
    }
}
