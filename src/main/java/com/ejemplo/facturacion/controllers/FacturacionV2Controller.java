package com.ejemplo.facturacion.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ejemplo.facturacion.services.FacturaService;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

/**
 * Versión 2 del API de facturación: generación asíncrona de facturas
 * siguiendo el modelo Polling.
 *  - POST /v2/factura: inicia la generación y regresa 202 (Accepted) con el
 *    encabezado Location apuntando al endpoint de consulta.
 *  - GET /v2/factura/{idFactura}: 200 con la factura si ya fue generada,
 *    204 si sigue en proceso, 404 si el id no existe.
 */
@RestController
public class FacturacionV2Controller {
    @Autowired FacturaService facturaService;

    @PostMapping("/v2/factura")
    public ResponseEntity<String> calcularFactura(@RequestBody Orden orden) throws InterruptedException {
        String idFactura = facturaService.iniciarFacturaAsincrona(orden);

        MultiValueMap<String, String> encabezados = new LinkedMultiValueMap<>();
        encabezados.add(HttpHeaders.LOCATION, "/v2/factura/" + idFactura);

        return new ResponseEntity<>(idFactura, encabezados, HttpStatus.ACCEPTED);
    }

    @GetMapping("/v2/factura/{idFactura}")
    public ResponseEntity<Factura> buscarFactura(@PathVariable String idFactura) {
        Optional<Factura> factura = facturaService.obtenerFacturaAsincrona(idFactura);

        if (factura == null) {
            // El id no existe
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (factura.isEmpty()) {
            // El id es válido pero la factura aún se está generando
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        // La factura ya fue generada
        return new ResponseEntity<>(factura.get(), HttpStatus.OK);
    }
}
