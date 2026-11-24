package dev.rental.photorent.controller;

import dev.rental.photorent.TestData;
import dev.rental.photorent.model.Equipment;
import dev.rental.photorent.model.EquipmentCondition;
import dev.rental.photorent.model.EquipmentStatus;
import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import dev.rental.photorent.service.RentalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
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

@SpringBootTest(classes = {RentalController.class, GlobalExceptionHandler.class})
class RentalControllerTest {

    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 5);

    @Autowired
    private RentalController controller;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @MockitoBean
    private RentalService rentalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(exceptionHandler).build();
    }

    private Rental rental(Long id, RentalStatus status) {
        Equipment equipment = TestData.equipment(1L, EquipmentStatus.AVAILABLE, EquipmentCondition.GOOD);
        return TestData.rental(id, equipment, TestData.customer(2L), START, END, status);
    }

    @Test
    void shouldReturn201WhenRentalIsBooked() throws Exception {
        when(rentalService.bookRental(1L, 2L, START, END)).thenReturn(rental(10L, RentalStatus.BOOKED));

        mockMvc.perform(post("/api/rentals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"equipmentId":1,"customerId":2,"startDate":"2026-12-01","endDate":"2026-12-05"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("BOOKED"));
    }

    @Test
    void shouldReturn400WhenDatesAreMissing() throws Exception {
        mockMvc.perform(post("/api/rentals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"equipmentId":1,"customerId":2}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(rentalService);
    }

    @Test
    void shouldReturn409WhenDatesOverlap() throws Exception {
        when(rentalService.bookRental(1L, 2L, START, END))
                .thenThrow(new IllegalStateException("Equipment is already booked for the selected dates"));

        mockMvc.perform(post("/api/rentals").contentType(MediaType.APPLICATION_JSON).content("""
                        {"equipmentId":1,"customerId":2,"startDate":"2026-12-01","endDate":"2026-12-05"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldPassFiltersToServiceWhenSearching() throws Exception {
        when(rentalService.search(2L, null, RentalStatus.ACTIVE))
                .thenReturn(List.of(rental(10L, RentalStatus.ACTIVE)));

        mockMvc.perform(get("/api/rentals").param("customerId", "2").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404WhenRentalIsMissing() throws Exception {
        when(rentalService.getById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rentals/99")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnRentalAfterEquipmentIsIssued() throws Exception {
        when(rentalService.getById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.ACTIVE)));

        mockMvc.perform(post("/api/rentals/10/issue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(rentalService).issueEquipment(10L);
    }

    @Test
    void shouldReturnRentalAfterEquipmentIsReturned() throws Exception {
        when(rentalService.getById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.RETURNED)));

        mockMvc.perform(post("/api/rentals/10/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        verify(rentalService).returnEquipment(10L);
    }

    @Test
    void shouldExtendRentalToNewDate() throws Exception {
        when(rentalService.getById(10L)).thenReturn(Optional.of(rental(10L, RentalStatus.ACTIVE)));

        mockMvc.perform(patch("/api/rentals/10/extend").contentType(MediaType.APPLICATION_JSON).content("""
                        {"newEndDate":"2026-12-08"}
                        """))
                .andExpect(status().isOk());

        verify(rentalService).extendRental(10L, LocalDate.of(2026, 12, 8));
    }

    @Test
    void shouldReturn204WhenBookingIsCancelled() throws Exception {
        mockMvc.perform(delete("/api/rentals/10")).andExpect(status().isNoContent());

        verify(rentalService).cancelBooking(10L);
    }

    @Test
    void shouldReturn409WhenCancellingNotBookedRental() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("Only BOOKED rentals can be cancelled"))
                .when(rentalService).cancelBooking(10L);

        mockMvc.perform(delete("/api/rentals/10")).andExpect(status().isConflict());
    }

    @Test
    void shouldReturnNumberOfMarkedRentals() throws Exception {
        when(rentalService.markOverdueRentals(LocalDate.of(2026, 12, 20))).thenReturn(3);

        mockMvc.perform(post("/api/rentals/overdue-check").param("today", "2026-12-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(3));
    }

    @Test
    void shouldPassNoDateToServiceWhenTodayIsNotGiven() throws Exception {
        when(rentalService.markOverdueRentals(null)).thenReturn(2);

        mockMvc.perform(post("/api/rentals/overdue-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(2));
    }

    @Test
    void shouldReturn400WhenTodayHasWrongFormat() throws Exception {
        mockMvc.perform(post("/api/rentals/overdue-check").param("today", "20.12.2026"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(rentalService);
    }
}
