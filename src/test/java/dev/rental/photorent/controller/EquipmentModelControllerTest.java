package dev.rental.photorent.controller;

import dev.rental.photorent.TestData;
import dev.rental.photorent.service.EquipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {EquipmentModelController.class, GlobalExceptionHandler.class})
class EquipmentModelControllerTest {

    @Autowired
    private EquipmentModelController controller;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @MockitoBean
    private EquipmentService equipmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(exceptionHandler).build();
    }

    @Test
    void shouldReturn201WhenModelIsCreated() throws Exception {
        when(equipmentService.createModel("CAM", "Sony", "A7 IV", "Mirrorless", new BigDecimal("50")))
                .thenReturn(TestData.model(1L, "Sony", "A7 IV"));

        mockMvc.perform(post("/api/models").contentType(MediaType.APPLICATION_JSON).content("""
                        {"categoryPrefix":"CAM","brand":"Sony","name":"A7 IV",
                         "description":"Mirrorless","dailyRate":50}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryPrefix").value("CAM"));
    }

    @Test
    void shouldReturn400WhenRateIsNegative() throws Exception {
        mockMvc.perform(post("/api/models").contentType(MediaType.APPLICATION_JSON).content("""
                        {"categoryPrefix":"CAM","brand":"Sony","name":"A7 IV","dailyRate":-1}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(equipmentService);
    }

    @Test
    void shouldReturn400WhenRateHasTooManyDecimals() throws Exception {
        mockMvc.perform(post("/api/models").contentType(MediaType.APPLICATION_JSON).content("""
                        {"categoryPrefix":"CAM","brand":"Sony","name":"A7 IV","dailyRate":10.123}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(equipmentService);
    }

    @Test
    void shouldReturn400WhenRateDoesNotFitIntoColumn() throws Exception {
        mockMvc.perform(post("/api/models").contentType(MediaType.APPLICATION_JSON).content("""
                        {"categoryPrefix":"CAM","brand":"Sony","name":"A7 IV","dailyRate":123456789}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(equipmentService);
    }

    @Test
    void shouldReturn404WhenModelIsMissing() throws Exception {
        when(equipmentService.getModelById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/models/99")).andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateRateAndDescription() throws Exception {
        when(equipmentService.updateModel(1L, "New text", new BigDecimal("70")))
                .thenReturn(TestData.model(1L, "Sony", "A7 IV"));

        mockMvc.perform(put("/api/models/1").contentType(MediaType.APPLICATION_JSON).content("""
                        {"description":"New text","dailyRate":70}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenDeletingMissingModel() throws Exception {
        when(equipmentService.removeModel(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/models/99")).andExpect(status().isNotFound());
    }
}
