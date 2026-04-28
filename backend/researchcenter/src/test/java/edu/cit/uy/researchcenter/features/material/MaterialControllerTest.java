package edu.cit.uy.researchcenter.features.material;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MaterialControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String token;
    static Long repoId;
    static Long materialId;

    @BeforeAll
    static void setup(@Autowired MockMvc mvc, @Autowired ObjectMapper mapper) throws Exception {
        String email = "mattest_" + System.currentTimeMillis() + "@test.com";
        var reg = Map.of("email", email, "password", "Test1234!", "firstname", "Mat", "lastname", "Tester");
        MvcResult r1 = mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg))).andReturn();
        token = mapper.readTree(r1.getResponse().getContentAsString()).at("/data/accessToken").asText();

        var repo = Map.of("name", "Mat Test Repo", "description", "for material tests");
        MvcResult r2 = mvc.perform(post("/api/v1/repositories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(repo))).andReturn();
        repoId = mapper.readTree(r2.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    @Test @Order(1)
    @DisplayName("TC-MAT-001: Add LINK material returns 201")
    void testAddLinkMaterial() throws Exception {
        var body = Map.of(
            "repositoryId", repoId,
            "title", "Test Link Material",
            "description", "A test material",
            "materialType", "LINK",
            "url", "https://example.com",
            "tags", List.of("test", "link"),
            "status", "TO_READ"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/materials")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.title").value("Test Link Material"))
            .andExpect(jsonPath("$.data.materialType").value("LINK"))
            .andReturn();
        materialId = objectMapper.readTree(res.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    @Test @Order(2)
    @DisplayName("TC-MAT-002: Get materials by repository returns list")
    void testGetMaterials() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repoId + "/materials")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test @Order(3)
    @DisplayName("TC-MAT-003: Get material by ID returns detail")
    void testGetMaterialById() throws Exception {
        mockMvc.perform(get("/api/v1/materials/" + materialId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(materialId));
    }

    @Test @Order(4)
    @DisplayName("TC-MAT-004: Delete material by uploader returns 200")
    void testDeleteMaterial() throws Exception {
        mockMvc.perform(delete("/api/v1/materials/" + materialId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
