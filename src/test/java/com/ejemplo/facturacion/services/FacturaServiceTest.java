package com.ejemplo.facturacion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ejemplo.facturacion.valueobjects.Articulo;
import com.ejemplo.facturacion.valueobjects.Factura;
import com.ejemplo.facturacion.valueobjects.Orden;

/**
 * Pruebas unitarias de FacturaService (Reto: Mejoramiento de un API).
 * Autor: Steve Tort
 *
 * Se prueba cada uno de los cuatro métodos públicos de la clase:
 *   1. generarFactura            (síncrono)
 *   2. iniciarFacturaAsincrona   (asíncrono - inicio)
 *   3. obtenerFacturaAsincrona   (consulta de estado)
 *   4. crearFacturaAsincrona     (asíncrono - generación)
 *
 * Patrón GIVEN-WHEN-THEN. Para los métodos asíncronos se valida primero
 * el estado "en proceso" y después, con una espera activa acotada, el
 * estado final de la factura generada en el hilo independiente.
 */
@SpringBootTest
class FacturaServiceTest {

    @Autowired
    private FacturaService facturaService;

    private Orden orden;

    /** GIVEN: una orden con 2 pantalones ($4.55) y 3 camisas ($7.88). */
    @BeforeEach
    void prepararPruebas() {
        Articulo pantalon = new Articulo();
        pantalon.setProducto("PANTALON");
        pantalon.setCantidad(2);
        pantalon.setPrecioUnitario(BigDecimal.valueOf(4.55));

        Articulo camisa = new Articulo();
        camisa.setProducto("CAMISA");
        camisa.setCantidad(3);
        camisa.setPrecioUnitario(BigDecimal.valueOf(7.88));

        orden = new Orden();
        orden.setUsuario("egarza");
        orden.setArticulos(List.of(pantalon, camisa));
    }

    /** Prueba 1: generarFactura calcula subtotal, IVA (16%) y total correctos. */
    @Test
    void testGenerarFactura() throws InterruptedException {
        // WHEN
        Factura factura = facturaService.generarFactura(orden);

        // THEN: subtotal = 2*4.55 + 3*7.88 = 32.74; IVA = 5.24 (redondeo UP); total = 37.98
        assertNotNull(factura);
        assertNotNull(factura.getId());
        assertEquals(new BigDecimal("32.74"), factura.getSubtotal());
        assertEquals(new BigDecimal("5.24"), factura.getIva());
        assertEquals(new BigDecimal("37.98"), factura.getTotal());
        assertEquals(2, factura.getArticulos().size());
    }

    /** Prueba 2: iniciarFacturaAsincrona regresa un id de inmediato y registra la factura en proceso. */
    @Test
    void testIniciarFacturaAsincrona() throws InterruptedException {
        // WHEN
        long inicio = System.currentTimeMillis();
        String idFactura = facturaService.iniciarFacturaAsincrona(orden);
        long duracion = System.currentTimeMillis() - inicio;

        // THEN: responde de inmediato (mucho antes de los 5 s del proceso)
        assertNotNull(idFactura);
        assertTrue(duracion < 5000, "El inicio asíncrono no debe bloquear el hilo principal");
        // y la factura queda registrada (en proceso o terminada)
        assertNotNull(facturaService.getFacturas().get(idFactura));
    }

    /** Prueba 3: obtenerFacturaAsincrona distingue en proceso / generada / inexistente. */
    @Test
    void testObtenerFacturaAsincrona() throws InterruptedException {
        // WHEN: id inexistente
        assertNull(facturaService.obtenerFacturaAsincrona("id-que-no-existe"));

        // WHEN: factura recién iniciada -> en proceso (Optional.empty)
        String idFactura = facturaService.iniciarFacturaAsincrona(orden);
        Optional<Factura> enProceso = facturaService.obtenerFacturaAsincrona(idFactura);
        assertNotNull(enProceso);
        assertTrue(enProceso.isEmpty(), "Debe reportarse en proceso justo después de iniciar");

        // THEN: al terminar el hilo asíncrono, la factura está disponible
        Optional<Factura> factura = esperarFactura(idFactura, 10000);
        assertTrue(factura.isPresent(), "La factura debe generarse dentro del tiempo esperado");
        assertEquals(idFactura, factura.get().getId());
    }

    /** Prueba 4: crearFacturaAsincrona genera la factura con los cálculos correctos en un hilo aparte. */
    @Test
    void testCrearFacturaAsincrona() throws InterruptedException {
        // GIVEN: un id registrado manualmente
        String idFactura = "id-prueba-async";
        facturaService.getFacturas().put(idFactura, Optional.empty());

        // WHEN
        facturaService.crearFacturaAsincrona(idFactura, orden);

        // THEN: la factura queda publicada con los totales correctos
        Optional<Factura> factura = esperarFactura(idFactura, 10000);
        assertTrue(factura.isPresent());
        assertEquals(new BigDecimal("32.74"), factura.get().getSubtotal());
        assertEquals(new BigDecimal("5.24"), factura.get().getIva());
        assertEquals(new BigDecimal("37.98"), factura.get().getTotal());
    }

    /** Espera activa acotada para validar resultados de métodos asíncronos. */
    private Optional<Factura> esperarFactura(String idFactura, long timeoutMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + timeoutMs;
        Optional<Factura> factura = facturaService.obtenerFacturaAsincrona(idFactura);
        while ((factura == null || factura.isEmpty()) && System.currentTimeMillis() < limite) {
            Thread.sleep(250);
            factura = facturaService.obtenerFacturaAsincrona(idFactura);
        }
        return factura == null ? Optional.empty() : factura;
    }
}
