package com.bancario.compensacion.servicio;

import com.bancario.compensacion.dto.ArchivoDTO;
import com.bancario.compensacion.dto.CicloDTO;
import com.bancario.compensacion.dto.PosicionDTO;
import com.bancario.compensacion.mapper.CompensacionMapper;
import com.bancario.compensacion.modelo.*;
import com.bancario.compensacion.repositorio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensacionServicio {

    private final CicloCompensacionRepositorio cicloRepo;
    private final PosicionInstitucionRepositorio posicionRepo;
    private final ArchivoLiquidacionRepositorio archivoRepo;
    private final DetalleCompensacionRepositorio detalleRepo;
    private final CompensacionMapper mapper;
    private final RestTemplate restTemplate;

    @Value("${service.contabilidad.url:http://ms-contabilidad:8083}")
    private String contabilidadUrl;

    private final org.springframework.scheduling.TaskScheduler taskScheduler;
    private java.util.concurrent.ScheduledFuture<?> scheduledTask;

    @Transactional
    public void registrarOperacion(com.bancario.compensacion.dto.RegistroOperacionDTO req) {
        CicloCompensacion cicloAbierto = cicloRepo.findByEstado("ABIERTO")
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay ciclo abierto para compensar"));

        DetalleCompensacion detalle = new DetalleCompensacion();
        detalle.setIdInstruccion(req.getIdInstruccion());
        detalle.setIdInstruccionOriginal(req.getIdInstruccionOriginal());
        detalle.setCiclo(cicloAbierto);
        detalle.setTipoOperacion(req.getTipoOperacion());
        detalle.setBicEmisor(req.getBicEmisor());
        detalle.setBicReceptor(req.getBicReceptor());
        detalle.setMonto(req.getMonto());
        detalle.setCodigoReferencia(req.getCodigoReferencia());
        detalle.setEstadoLiquidacion("INCLUIDO");
        detalleRepo.save(detalle);

        if ("REVERSO".equalsIgnoreCase(req.getTipoOperacion())) {
            acumularTransaccion(cicloAbierto.getIdCiclo(), req.getBicEmisor(), req.getMonto(), false);
            acumularTransaccion(cicloAbierto.getIdCiclo(), req.getBicReceptor(), req.getMonto(), true);
        } else {
            acumularTransaccion(cicloAbierto.getIdCiclo(), req.getBicEmisor(), req.getMonto(), true);
            acumularTransaccion(cicloAbierto.getIdCiclo(), req.getBicReceptor(), req.getMonto(), false);
        }
    }

    @Transactional
    public void acumularEnCicloAbierto(String bic, BigDecimal monto, boolean esDebito) {
        CicloCompensacion cicloAbierto = cicloRepo.findByEstado("ABIERTO")
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay ciclo abierto para compensar (Auto)"));

        acumularTransaccion(cicloAbierto.getIdCiclo(), bic, monto, esDebito);
    }

    @Transactional
    public void acumularTransaccion(Integer cicloId, String bic, BigDecimal monto, boolean esDebito) {
        PosicionInstitucion posicion = posicionRepo.findByCicloIdCicloAndBic(cicloId, bic)
                .orElseGet(() -> crearPosicionVacia(cicloId, bic));

        if (esDebito) {
            posicion.setTotalDebitos(posicion.getTotalDebitos().add(monto));
        } else {
            posicion.setTotalCredits(posicion.getTotalCredits().add(monto));
        }

        posicion.recalcularNeto();
        posicionRepo.save(posicion);
    }

    private PosicionInstitucion crearPosicionVacia(Integer cicloId, String bic) {
        PosicionInstitucion p = new PosicionInstitucion();
        p.setCiclo(cicloRepo.getReferenceById(cicloId));
        p.setBic(bic);
        p.setTotalDebitos(BigDecimal.ZERO);
        p.setTotalCredits(BigDecimal.ZERO);
        p.setPosicionNeta(BigDecimal.ZERO);
        return posicionRepo.save(p);
    }

    @Transactional
    public ArchivoDTO realizarCierreDiario(Integer cicloId, Integer minutosProximoCiclo) {
        log.info(">>> INICIANDO CIERRE DEL CICLO: {}", cicloId);

        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }

        CicloCompensacion cicloActual = cicloRepo.findById(cicloId)
                .orElseThrow(() -> new RuntimeException("Ciclo no encontrado"));

        if (!"ABIERTO".equals(cicloActual.getEstado())) {
            throw new RuntimeException("El ciclo ya está cerrado");
        }

        recalcularPosicionesDesdeDetalles(cicloActual);

        List<PosicionInstitucion> posiciones = posicionRepo.findByCicloIdCiclo(cicloId);

        BigDecimal sumaNetos = posiciones.stream()
                .map(PosicionInstitucion::getPosicionNeta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumaNetos.abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new RuntimeException("ALERTA: El sistema no cuadra. Suma Netos: " + sumaNetos);
        }

        String xml = generarXML(cicloActual, posiciones);

        ArchivoLiquidacion archivo = new ArchivoLiquidacion();
        archivo.setCiclo(cicloActual);
        archivo.setNombreArchivo("LIQ_CICLO_" + cicloActual.getNumeroCiclo() + ".xml");
        archivo.setContenidoXml(xml);
        archivo.setFechaGeneracion(LocalDateTime.now(java.time.ZoneOffset.UTC));
        archivo = archivoRepo.save(archivo);

        cicloActual.setEstado("CERRADO");
        cicloActual.setFechaCierre(LocalDateTime.now(java.time.ZoneOffset.UTC));
        cicloRepo.save(cicloActual);

        enviarLiquidacionAContabilidad(cicloId, posiciones);
        iniciarSiguienteCiclo(cicloActual, posiciones, minutosProximoCiclo);

        return mapper.toDTO(archivo);
    }

    private void recalcularPosicionesDesdeDetalles(CicloCompensacion ciclo) {
        log.info("Ejecutando algoritmo de neteo para ciclo {}", ciclo.getIdCiclo());

        List<PosicionInstitucion> posiciones = posicionRepo.findByCicloIdCiclo(ciclo.getIdCiclo());
        for (PosicionInstitucion p : posiciones) {
            p.setTotalDebitos(BigDecimal.ZERO);
            p.setTotalCredits(BigDecimal.ZERO);
            p.setPosicionNeta(BigDecimal.ZERO);
        }
        Map<String, PosicionInstitucion> mapaPosiciones = posiciones.stream()
                .collect(Collectors.toMap(PosicionInstitucion::getBic, p -> p));

        List<DetalleCompensacion> detalles = detalleRepo.findByCicloIdCiclo(ciclo.getIdCiclo());

        for (DetalleCompensacion d : detalles) {
            if ("EXCLUIDO".equalsIgnoreCase(d.getEstadoLiquidacion())
                    || "PENDIENTE".equalsIgnoreCase(d.getEstadoLiquidacion()))
                continue;

            PosicionInstitucion posEmisor = mapaPosiciones.computeIfAbsent(d.getBicEmisor(),
                    k -> crearPosicionVacia(ciclo.getIdCiclo(), k));
            PosicionInstitucion posReceptor = mapaPosiciones.computeIfAbsent(d.getBicReceptor(),
                    k -> crearPosicionVacia(ciclo.getIdCiclo(), k));

            if ("REVERSO".equalsIgnoreCase(d.getTipoOperacion())) {
                posEmisor.setTotalCredits(posEmisor.getTotalCredits().add(d.getMonto()));
                posReceptor.setTotalDebitos(posReceptor.getTotalDebitos().add(d.getMonto()));
            } else {
                posEmisor.setTotalDebitos(posEmisor.getTotalDebitos().add(d.getMonto()));
                posReceptor.setTotalCredits(posReceptor.getTotalCredits().add(d.getMonto()));
            }
        }

        for (PosicionInstitucion p : mapaPosiciones.values()) {
            p.recalcularNeto();
            posicionRepo.save(p);
        }
    }

    private void iniciarSiguienteCiclo(CicloCompensacion anterior, List<PosicionInstitucion> saldosAnteriores,
            Integer minutosDuracion) {
        CicloCompensacion nuevo = new CicloCompensacion();
        nuevo.setNumeroCiclo(anterior.getNumeroCiclo() + 1);
        nuevo.setEstado("ABIERTO");
        nuevo.setFechaApertura(LocalDateTime.now(java.time.ZoneOffset.UTC));
        CicloCompensacion guardado = cicloRepo.save(nuevo);

        for (PosicionInstitucion posAnt : saldosAnteriores) {
            PosicionInstitucion posNueva = new PosicionInstitucion();
            posNueva.setCiclo(guardado);
            posNueva.setBic(posAnt.getBic());
            posNueva.setTotalDebitos(BigDecimal.ZERO);
            posNueva.setTotalCredits(BigDecimal.ZERO);
            posNueva.recalcularNeto();
            posicionRepo.save(posNueva);
        }

        programarCierreAutomatico(guardado.getIdCiclo(), minutosDuracion != null ? minutosDuracion : 10);
    }

    public void programarCierreAutomatico(Integer cicloId, int minutos) {
        CicloCompensacion ciclo = cicloRepo.findById(cicloId)
                .orElseThrow(() -> new RuntimeException("Ciclo " + cicloId + " no existe."));

        if (!"ABIERTO".equals(ciclo.getEstado()))
            return;

        Runnable tareaCierre = () -> {
            try {
                realizarCierreDiario(cicloId, 60);
            } catch (Exception e) {
                log.error("Error en cierre automático: {}", e.getMessage());
            }
        };

        java.time.Instant fechaEjecucion = ciclo.getFechaApertura().toInstant(java.time.ZoneOffset.UTC)
                .plus(java.time.Duration.ofMinutes(minutos));

        if (fechaEjecucion.isBefore(java.time.Instant.now())) {
            fechaEjecucion = java.time.Instant.now().plusSeconds(10);
        }

        this.scheduledTask = taskScheduler.schedule(tareaCierre, fechaEjecucion);
    }

    private void enviarLiquidacionAContabilidad(Integer cicloId, List<PosicionInstitucion> posiciones) {
        try {
            java.util.List<java.util.Map<String, Object>> posicionesDTO = new java.util.ArrayList<>();
            for (PosicionInstitucion p : posiciones) {
                java.util.Map<String, Object> pos = new java.util.HashMap<>();
                pos.put("bic", p.getBic());
                pos.put("totalDebitos", p.getTotalDebitos());
                pos.put("totalCreditos", p.getTotalCredits());
                pos.put("posicionNeta", p.getPosicionNeta());
                posicionesDTO.add(pos);
            }

            java.util.Map<String, Object> solicitud = new java.util.HashMap<>();
            solicitud.put("cicloId", cicloId);
            solicitud.put("posiciones", posicionesDTO);

            restTemplate.postForEntity(contabilidadUrl + "/api/v1/ledger/compensar", solicitud, Void.class);
        } catch (Exception e) {
            log.error("Fallo al enviar liquidación a Contabilidad: {}", e.getMessage());
            throw new RuntimeException("Error en Disparo Contable: " + e.getMessage());
        }
    }

    public byte[] generarReportePDF(Integer cicloId) {
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            CicloCompensacion ciclo = cicloRepo.findById(cicloId).orElseThrow();
            document.add(new com.lowagie.text.Paragraph("Ciclo: " + ciclo.getNumeroCiclo()));
            document.add(new com.lowagie.text.Paragraph("Estado: " + ciclo.getEstado()));
            document.add(new com.lowagie.text.Paragraph("Fecha Apertura: " + ciclo.getFechaApertura()));

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("Banco (BIC)");
            table.addCell("Débitos");
            table.addCell("Créditos");
            table.addCell("Posición Neta");

            List<PosicionInstitucion> posiciones = posicionRepo.findByCicloIdCiclo(cicloId);
            for (PosicionInstitucion p : posiciones) {
                table.addCell(p.getBic());
                table.addCell(p.getTotalDebitos().toString());
                table.addCell(p.getTotalCredits().toString());
                table.addCell(p.getPosicionNeta().toString());
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF: " + e.getMessage());
        }
    }

    private String generarXML(CicloCompensacion ciclo, List<PosicionInstitucion> posiciones) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<SettlementFile>\n");
        for (PosicionInstitucion p : posiciones) {
            sb.append("  <Tx>\n");
            sb.append("    <BankBIC>").append(p.getBic()).append("</BankBIC>\n");
            sb.append("    <NetPosition>").append(p.getPosicionNeta()).append("</NetPosition>\n");
            sb.append("  </Tx>\n");
        }
        sb.append("</SettlementFile>");
        return sb.toString();
    }

    public List<CicloDTO> listarCiclos() {
        List<CicloCompensacion> ciclos = cicloRepo.findAll();
        if (ciclos.isEmpty()) {
            CicloCompensacion primerCiclo = new CicloCompensacion();
            primerCiclo.setNumeroCiclo(1);
            primerCiclo.setEstado("ABIERTO");
            primerCiclo.setFechaApertura(LocalDateTime.now(java.time.ZoneOffset.UTC));
            CicloCompensacion guardado = cicloRepo.save(primerCiclo);
            programarCierreAutomatico(guardado.getIdCiclo(), 60);
            ciclos.add(guardado);
        }
        return ciclos.stream().map(mapper::toDTO).toList();
    }

    public List<PosicionDTO> obtenerPosicionesCiclo(Integer cicloId) {
        return mapper.toPosicionList(posicionRepo.findByCicloIdCiclo(cicloId));
    }
}
