package dev.rental.photorent.controller;

import dev.rental.photorent.TestData;
import dev.rental.photorent.model.UserRole;
import dev.rental.photorent.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {UserController.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private UserController controller;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @MockitoBean
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(exceptionHandler).build();
    }

    @Test
    void shouldReturn201WhenUserIsCreated() throws Exception {
        when(userService.register("john", "john@example.com", "secret", UserRole.CUSTOMER))
                .thenReturn(TestData.customer(5L));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"john","email":"john@example.com","password":"secret","role":"CUSTOMER"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void shouldReturn400AndSkipServiceWhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"john","email":"not-an-email","password":"secret","role":"CUSTOMER"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturn409WhenUsernameIsTaken() throws Exception {
        when(userService.register("john", "john@example.com", "secret", UserRole.CUSTOMER))
                .thenThrow(new IllegalStateException("Username already taken: john"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"john","email":"john@example.com","password":"secret","role":"CUSTOMER"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFilterUsersByRole() throws Exception {
        when(userService.getByRole(UserRole.ADMIN)).thenReturn(List.of(TestData.admin(1L)));

        mockMvc.perform(get("/api/users").param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("admin1"));
    }

    @Test
    void shouldReturn400WhenRoleIsUnknown() throws Exception {
        mockMvc.perform(get("/api/users").param("role", "BOSS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUserIsMissing() throws Exception {
        when(userService.getById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnUpdatedUser() throws Exception {
        when(userService.update(3L, "new_name", "new@example.com")).thenReturn(TestData.customer(3L));

        mockMvc.perform(put("/api/users/3").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"new_name","email":"new@example.com"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void shouldReturn204WhenUserIsDeleted() throws Exception {
        when(userService.removeById(3L)).thenReturn(true);

        mockMvc.perform(delete("/api/users/3")).andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404WhenDeletingMissingUser() throws Exception {
        when(userService.removeById(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/users/99")).andExpect(status().isNotFound());
    }
}
