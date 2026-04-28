package edu.cit.uy.researchcenter.features.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepositoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String token;
    static Long repoId;

    @BeforeAll
    static void setup(@Autowired MockMvc mvc, @Autowired ObjectMapper mapper) throws Exception {
        // Register and login to get token
        String email = "repotest_" + System.currentTimeMillis() + "@test.com";
        var reg = Map.of("email", email, "password", "Test1234!", "firstname", "Repo", "lastname", "Tester");
        MvcResult res = mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg)))
            .andReturn();
        var body = mapper.readTree(res.getResponse().getContentAsString());
        token = body.at("/data/accessToken").asText();
    }

    @Test @Order(1)
    @DisplayName("TC-REPO-001: Create repository returns 201")
    void testCreateRepository() throws Exception {
        var body = Map.of("name", "Test Repo", "description", "Test description");
        MvcResult res = mockMvc.perform(post("/api/v1/repositories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Test Repo"))
            .andReturn();
        var json = objectMapper.readTree(res.getResponse().getContentAsString());
        repoId = json.at("/data/id").asLong();
    }

    @Test @Order(2)
    @DisplayName("TC-REPO-002: Get all repositories returns list")
    void testGetRepositories() throws Exception {
        mockMvc.perform(get("/api/v1/repositories")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test @Order(3)
    @DisplayName("TC-REPO-003: Get repository by ID returns detail")
    void testGetRepositoryById() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repoId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(repoId));
    }

    @Test @Order(4)
    @DisplayName("TC-REPO-004: Access repository without token returns 401")
    void testGetRepositoryUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/repositories"))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(5)
    @DisplayName("TC-REPO-005: Update repository name")
    void testUpdateRepository() throws Exception {
        var body = Map.of("name", "Updated Repo", "description", "Updated desc");
        mockMvc.perform(put("/api/v1/repositories/" + repoId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Updated Repo"));
    }

    @Test @Order(6)
    @DisplayName("TC-REPO-006: Delete repository")
    void testDeleteRepository() throws Exception {
        mockMvc.perform(delete("/api/v1/repositories/" + repoId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
