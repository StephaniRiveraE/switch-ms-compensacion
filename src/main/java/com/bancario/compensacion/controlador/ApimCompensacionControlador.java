package com.bancario.compensacion.controlador;

import com.bancario.compensacion.dto.CicloDTO;
import com.bancario.compensacion.dto.PosicionDTO;
import com.bancario.compensacion.servicio.CompensacionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador compatible con AWS APIM para servicio de Compensación.
 * Expone endpoints en /api/v2/compensation/* configurados en el APIM.
 *
 * Actúa como fachada v2 que delega al servicio de compensación interno.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/compensation")
@RequiredArgsConstructor
@Tag(name = "APIM Compensación", description = "Endpoints de compensación expuestos via AWS API Gateway")
public class ApimCompensacionControlador {

    private final CompensacionServicio compensacionServicio;

    /**
     * POST /api/v2/compensation/upload
     * Ruta configurada en APIM para subir archivos de compensación.
     *
     * TODO: Implementar lógica de procesamiento de archivos CSV/Excel.
     */
    @PostMapping("/upload")
    @Operation(summary = "Subir Archivo de Compensación", description = "Recibe archivos CSV/Excel con datos de compensación para procesamiento batch (EN DESARROLLO)")
    public ResponseEntity<Map<String, String>> uploadArchivo() {
        log.warn("[APIM] Endpoint /upload llamado pero aún no implementado");

        return ResponseEntity.ok(Map.of(
                "status", "NOT_IMPLEMENTED",
                "message",
                "La funcionalidad de upload de archivos está en desarrollo. Use /api/v1/compensacion/operaciones para registrar operaciones individuales."));
    }

    /**
     * GET /api/v2/compensation/health
     * Health check para ALB Target Group.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "switch-ms-compensacion",
                "version", "3.0.0"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Endpoints Administrativos de Compensación (Panel Admin)
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v2/compensation/ciclos
     * Historial de ciclos de compensación.
     */
    @GetMapping("/ciclos")
    @Operation(summary = "Listar ciclos de compensación", description = "Obtiene el historial de todos los ciclos operativos (abiertos, cerrados, liquidados).")
    public ResponseEntity<List<CicloDTO>> listarCiclos() {
        log.info("[APIM] GET /ciclos - Listando ciclos de compensación");
        return ResponseEntity.ok(compensacionServicio.listarCiclos());
    }

    /**
     * GET /api/v2/compensation/ciclos/{cicloId}/posiciones
     * Detalle de posiciones netas por banco en un ciclo.
     */
    @GetMapping("/ciclos/{cicloId}/posiciones")
    @Operation(summary = "Detalle de posiciones por ciclo", description = "Ver acumulados netos (débitos/créditos) por banco participante en un ciclo específico.")
    public ResponseEntity<List<PosicionDTO>> obtenerPosiciones(@PathVariable Integer cicloId) {
        log.info("[APIM] GET /ciclos/{}/posiciones", cicloId);
        return ResponseEntity.ok(compensacionServicio.obtenerPosicionesCiclo(cicloId));
    }

    /**
     * POST /api/v2/compensation/ciclos/{cicloId}/cierre
     * Ejecución manual de cierre de ciclo (Settlement).
     */
    @PostMapping("/ciclos/{cicloId}/cierre")
    @Operation(summary = "Ejecutar cierre de ciclo (Settlement)", description = "Cierra el ciclo especificado y programa el siguiente. Parámetro opcional: proximoCicloEnMinutos (default 10).")
    public ResponseEntity<?> cerrarCiclo(
            @PathVariable Integer cicloId,
            @RequestParam(required = false, defaultValue = "10") Integer proximoCicloEnMinutos) {
        log.info("[APIM] POST /ciclos/{}/cierre - proximoCiclo={}min", cicloId, proximoCicloEnMinutos);
        try {
            return ResponseEntity.ok(compensacionServicio.realizarCierreDiario(cicloId, proximoCicloEnMinutos));
        } catch (Exception e) {
            log.error("[APIM] Error cerrando ciclo {}: ", cicloId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "codigo", "SETTLEMENT_ERROR",
                    "mensaje", e.getMessage()));
        }
    }

    /**
     * GET /api/v2/compensation/reporte/pdf/{cicloId}
     * Descarga de reporte PDF de liquidación.
     */
    @GetMapping("/reporte/pdf/{cicloId}")
    @Operation(summary = "Descargar Reporte PDF de Liquidación", description = "Genera y descarga el reporte PDF con el detalle de compensación del ciclo.")
    public ResponseEntity<byte[]> descargarReportePDF(@PathVariable Integer cicloId) {
        log.info("[APIM] GET /reporte/pdf/{}", cicloId);
        byte[] pdf = compensacionServicio.generarReportePDF(cicloId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Reporte_Ciclo_" + cicloId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}