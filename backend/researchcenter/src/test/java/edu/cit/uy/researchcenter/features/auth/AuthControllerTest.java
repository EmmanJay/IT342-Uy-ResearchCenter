package edu.cit.uy.researchcenter.features.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String testEmail = "testuser_" + System.currentTimeMillis() + "@test.com";
    static String testPassword = "Test1234!";

    @Test @Order(1)
    @DisplayName("TC-AUTH-001: Register with valid data returns 201")
    void testRegisterSuccess() throws Exception {
        var body = Map.of(
            "email", testEmail,
            "password", testPassword,
            "firstname", "Test",
            "lastname", "User"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andExpect(jsonPath("$.data.email").value(testEmail));
    }

    @Test @Order(2)
    @DisplayName("TC-AUTH-002: Register with duplicate email returns 409")
    void testRegisterDuplicateEmail() throws Exception {
        var body = Map.of(
            "email", testEmail,
            "password", testPassword,
            "firstname", "Test",
            "lastname", "User"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTH-002"));
    }

    @Test @Order(3)
    @DisplayName("TC-AUTH-003: Login with valid credentials returns 200 and JWT")
    void testLoginSuccess() throws Exception {
        var body = Map.of("email", testEmail, "password", testPassword);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test @Order(4)
    @DisplayName("TC-AUTH-004: Login with wrong password returns 401 AUTH-001")
    void testLoginWrongPassword() throws Exception {
        var body = Map.of("email", testEmail, "password", "wrongpassword");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("AUTH-001"));
    }

    @Test @Order(5)
    @DisplayName("TC-AUTH-005: Register with missing fields returns 400 VALID-001")
    void testRegisterMissingFields() throws Exception {
        var body = Map.of("email", "incomplete@test.com");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALID-001"));
    }
}
