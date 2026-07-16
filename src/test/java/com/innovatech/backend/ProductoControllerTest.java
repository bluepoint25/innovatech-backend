package com.innovatech.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheckDebeResponderUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void listarProductosDebeResponder200() throws Exception {
        // No se asume una lista vacía a propósito: JUnit no garantiza el orden
        // de ejecución entre métodos de test, así que solo se valida que el
        // endpoint responde correctamente, sin depender del estado de otros tests.
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }

    @Test
    void crearProductoValidoDebeResponder201() throws Exception {
        String nuevoProducto = """
                {
                  "nombre": "Producto de prueba",
                  "precio": 10.0,
                  "stock": 5
                }
                """;

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nuevoProducto))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Producto de prueba"));
    }

    @Test
    void crearProductoConPrecioInvalidoDebeRechazar() throws Exception {
        String productoInvalido = """
                {
                  "nombre": "Producto inválido",
                  "precio": -5.0,
                  "stock": 5
                }
                """;

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoInvalido))
                .andExpect(status().isBadRequest());
    }
}
