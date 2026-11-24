package dev.rental.photorent.controller;

import dev.rental.photorent.TestData;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.service.EquipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {EquipmentController.class, GlobalExceptionHandler.class})
class EquipmentControllerTest {

    @Autowired
    private EquipmentController controller;

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
    void shouldPassFiltersToServiceWhenSearching() throws Exception {
        when(equipmentService.search("CAM", EquipmentStatus.AVAILABLE, null, "sony"))
                .thenReturn(List.of(TestData.equipment(1L, EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD)));

        mockMvc.perform(get("/api/equipment")
                        .param("categoryPrefix", "CAM").param("status", "AVAILABLE").param("name", "sony"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].serialNumber").value("CAM-00001"))
                .andExpect(jsonPath("$[0].brand").value("Sony"));
    }

    @Test
    void shouldReturn400WhenStatusFilterIsInvalid() throws Exception {
        mockMvc.perform(get("/api/equipment").param("status", "BROKEN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(equipmentService);
    }

    @Test
    void shouldReturn404WhenEquipmentIsMissing() throws Exception {
        when(equipmentService.getById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/equipment/99")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenNoEquipmentIsAvailable() throws Exception {
        when(equipmentService.findBestAvailableByName("A7")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/equipment/best").param("name", "A7")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn201WhenEquipmentIsCreated() throws Exception {
        when(equipmentService.addEquipment(1L, EquipmentCondition.GOOD))
                .thenReturn(TestData.equipment(7L, EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD));

        mockMvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content("""
                        {"modelId":1,"condition":"GOOD"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void shouldReturn404WhenModelIsUnknown() throws Exception {
        when(equipmentService.addEquipment(42L, EquipmentCondition.NEW))
                .thenThrow(new NoSuchElementException("Equipment model not found: 42"));

        mockMvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content("""
                        {"modelId":42,"condition":"NEW"}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateConditionAndReturnEquipment() throws Exception {
        when(equipmentService.getById(7L)).thenReturn(Optional.of(
                TestData.equipment(7L, EquipmentStatus.AVAILABLE, EquipmentCondition.WORN)));

        mockMvc.perform(patch("/api/equipment/7/condition").contentType(MediaType.APPLICATION_JSON).content("""
                        {"condition":"WORN","staffId":2,"reason":"Scratched body"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("WORN"));

        verify(equipmentService).updateCondition(7L, EquipmentCondition.WORN, 2L, "Scratched body");
    }

    @Test
    void shouldReturn400WhenReasonIsBlank() throws Exception {
        mockMvc.perform(patch("/api/equipment/7/condition").contentType(MediaType.APPLICATION_JSON).content("""
                        {"condition":"WORN","staffId":2,"reason":" "}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(equipmentService);
    }

    @Test
    void shouldReturn409WhenConditionRuleIsViolated() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("Cannot decommission equipment with unfinished rentals"))
                .when(equipmentService).updateCondition(7L, EquipmentCondition.DECOMMISSIONED, 1L, "Broken");

        mockMvc.perform(patch("/api/equipment/7/condition").contentType(MediaType.APPLICATION_JSON).content("""
                        {"condition":"DECOMMISSIONED","staffId":1,"reason":"Broken"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn204WhenEquipmentIsDeleted() throws Exception {
        when(equipmentService.removeById(7L)).thenReturn(true);

        mockMvc.perform(delete("/api/equipment/7")).andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingMissingEquipment() throws Exception {
        when(equipmentService.removeById(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/equipment/99")).andExpect(status().isNotFound());
    }
}
