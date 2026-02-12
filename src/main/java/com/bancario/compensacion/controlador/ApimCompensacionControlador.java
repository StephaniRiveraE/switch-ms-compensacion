package com.bancario.compensacion.controlador;

import com.bancario.compensacion.servicio.CompensacionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador compatible con AWS APIM para servicio de Compensación.
 * Expone endpoints en /api/v2/compensation/* configurados en el APIM.
 * 
 * NOTA: El endpoint de upload de archivos aún no está implementado en el
 * backend.
 * Este controlador solo proporciona el health check por ahora.
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
     * Por ahora retorna un mensaje indicando que la funcionalidad está en
     * desarrollo.
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
     * GET /health
     * Health check para ALB Target Group.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "switch-ms-compensacion",
                "version", "3.0.0"));
    }
}