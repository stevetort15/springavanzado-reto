package com.ejemplo.facturacion.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ejemplo.facturacion.valueobjects.Articulo;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

@Service
public class FacturaService {
    private Map<String, Optional<Factura>> facturas = new HashMap<>();

    @Autowired @Lazy
    private FacturaService autoReferencia;

    public Factura generarFactura(final Orden orden) throws InterruptedException {
        Factura factura = new Factura();

        String idFactura = generarIdFactura();
        facturas.put(idFactura, Optional.empty());

        Thread.sleep(5000);

        BigDecimal subtotal = calcularSubtotal(orden.getArticulos());
        BigDecimal iva = subtotal.multiply(BigDecimal.valueOf(0.16)).setScale(2, RoundingMode.UP);
        BigDecimal total = subtotal.add(iva);

        factura.setId(idFactura);
        factura.setArticulos(orden.getArticulos());
        factura.setSubtotal(subtotal);
        factura.setIva(iva);
        factura.setTotal(total);

        facturas.put(factura.getId(), Optional.of(factura));

        return factura;
    }

    /**
     * Inicia la generación asíncrona de una factura: registra el id de la
     * factura como "en proceso" (Optional.empty) y delega la creación al
     * método asíncrono a través de la autorreferencia, para que Spring
     * ejecute la tarea en un hilo independiente del pool async-task.
     * Regresa de inmediato el id para su consulta posterior (modelo Polling).
     */
    public String iniciarFacturaAsincrona(final Orden orden) throws InterruptedException {
        String idFactura = generarIdFactura();
        facturas.put(idFactura, Optional.empty());
        autoReferencia.crearFacturaAsincrona(idFactura, orden);
        return idFactura;
    }

    /**
     * Consulta el estado de una factura generada de forma asíncrona:
     *  - Optional con la factura si ya fue generada.
     *  - Optional.empty si el id existe pero la factura sigue en proceso.
     *  - null si el id no existe.
     */
    public Optional<Factura> obtenerFacturaAsincrona(final String idFactura) {
        return facturas.get(idFactura);
    }

    /**
     * Genera la factura en un hilo independiente (@Async). Duplica la
     * funcionalidad de generarFactura sin alterar la versión original:
     * simula el procesamiento lento, calcula subtotal, IVA (16%) y total,
     * y publica la factura terminada en el mapa de facturas.
     */
    @Async
    public void crearFacturaAsincrona(final String idFactura, final Orden orden) throws InterruptedException {
        Thread.sleep(5000);

        BigDecimal subtotal = calcularSubtotal(orden.getArticulos());
        BigDecimal iva = subtotal.multiply(BigDecimal.valueOf(0.16)).setScale(2, RoundingMode.UP);
        BigDecimal total = subtotal.add(iva);

        Factura factura = new Factura(idFactura, orden.getArticulos(), subtotal, iva, total);

        facturas.put(idFactura, Optional.of(factura));
    }

    private BigDecimal calcularSubtotal(List<Articulo> articulos) {
        BigDecimal subtotal = BigDecimal.ZERO;

        if (articulos != null) {
            for (final Articulo articulo : articulos) {
                BigDecimal precioUnitario = articulo.getPrecioUnitario();
                BigDecimal cantidad = BigDecimal.valueOf(articulo.getCantidad());
                BigDecimal totalArticulo = precioUnitario.multiply(cantidad);
                subtotal = subtotal.add(totalArticulo);
            }
        }

        return subtotal;
    }

    private String generarIdFactura() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    public Map<String, Optional<Factura>> getFacturas() {
        return facturas;
    }

    public void setFacturas(Map<String, Optional<Factura>> facturas) {
        this.facturas = facturas;
    }
}
