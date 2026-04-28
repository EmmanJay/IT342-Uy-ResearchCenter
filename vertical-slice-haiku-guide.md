# ResearchCenter — Vertical Slice Refactoring & Full Regression Testing
# Instruction Guide for Claude Haiku 4.5 (VS Code Copilot)

> Activity: Vertical Slice Refactoring and Full Regression Testing  
> Due: May 9, 2026 11:59 PM  
> Project: IT342-Uy-ResearchCenter  
> Repo: https://github.com/EmmanJay/IT342-Uy-ResearchCenter.git  
> Student: Uy, Emman Jay Cañaveral | IT342-G1

---

## READ THIS FIRST — Project Context

This is a multi-platform collaborative research platform with 3 components:
- **Backend**: Spring Boot 3.5.x (Maven) — `backend/researchcenter/`
- **Web**: React 18 + Vite + TypeScript + Tailwind CSS — `web/`
- **Mobile**: Kotlin + XML Layouts (Android) — `mobile/ResearchCenter/`

Base package: `edu.cit.uy.researchcenter`

### Current Project Structure (BEFORE refactoring)

```
backend/researchcenter/src/main/java/edu/cit/uy/researchcenter/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   └── GlobalExceptionHandler.java
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── RepositoryController.java
│   ├── MaterialController.java
│   ├── RequestController.java
│   └── AdminController.java
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── GoogleAuthRequest.java
│   ├── AuthResponse.java
│   ├── CreateRepositoryRequest.java
│   ├── RepositoryResponse.java
│   ├── CreateMaterialRequest.java
│   ├── MaterialResponse.java
│   ├── CreateRequestDto.java
│   ├── FulfillRequestDto.java
│   └── RequestResponse.java
├── model/
│   ├── User.java
│   ├── Role.java
│   ├── RefreshToken.java
│   ├── Repository.java
│   ├── RepositoryMember.java
│   ├── Material.java
│   ├── MaterialTag.java
│   └── MaterialRequest.java
├── repository/
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── RefreshTokenRepository.java
│   ├── RepositoryRepo.java
│   ├── RepositoryMemberRepo.java
│   ├── MaterialRepo.java
│   ├── MaterialTagRepo.java
│   └── MaterialRequestRepo.java
├── service/
│   ├── AuthService.java
│   ├── GoogleAuthService.java
│   ├── JwtService.java
│   ├── UserService.java
│   ├── RepositoryService.java
│   ├── MaterialService.java
│   └── RequestService.java
└── ResearchcenterApplication.java

web/src/
├── api/
│   ├── axiosClient.ts
│   ├── authApi.ts
│   ├── repositoryApi.ts
│   ├── requestApi.ts
│   ├── materialApi.ts
│   ├── googleBooksApi.ts
│   └── supabaseUpload.ts
├── auth/
│   └── sessionManager.ts
├── components/
│   ├── Navbar.tsx
│   ├── Breadcrumbs.tsx
│   ├── MaterialForm.tsx
│   ├── RepositoryCard.tsx
│   └── ProtectedRoute.tsx
├── pages/
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── DashboardPage.tsx
│   ├── RepositoryDetailPage.tsx
│   ├── AddMaterialPage.tsx
│   ├── EditMaterialPage.tsx
│   └── NewRequestPage.tsx
├── services/
│   └── api.ts
└── types/
    └── index.ts
```

---

## PART 1 — BRANCH CREATION

Run this before doing anything else:

```bash
# Make sure main is up to date
git checkout main
git pull origin main

# Create the refactor branch
git checkout -b refactor/vertical-slice-architecture

# Verify you are on the right branch
git branch
```

---

## PART 2 — VERTICAL SLICE ARCHITECTURE REFACTORING

### What is Vertical Slice Architecture?

Instead of organizing by technical layer (controller/service/repository),
organize by FEATURE. Each feature folder contains everything it needs:
its own controller, service, repository, DTOs, and models.

```
BEFORE (layered):               AFTER (vertical slice):
controller/                     features/
  AuthController.java             auth/
  MaterialController.java           AuthController.java
service/                            AuthService.java
  AuthService.java                  GoogleAuthService.java
  MaterialService.java              JwtService.java
model/                              dto/
  User.java                           RegisterRequest.java
  Material.java                       LoginRequest.java
                                      GoogleAuthRequest.java
                                      AuthResponse.java
                                  model/
                                    User.java
                                    Role.java
                                    RefreshToken.java
                                  repository/
                                    UserRepository.java
                                    RoleRepository.java
                                    RefreshTokenRepository.java
                                materials/
                                  MaterialController.java
                                  MaterialService.java
                                  dto/...
                                  model/...
                                  repository/...
```

---

### BACKEND REFACTORING

Reorganize `backend/researchcenter/src/main/java/edu/cit/uy/researchcenter/`
into this new feature-based structure:

```
edu/cit/uy/researchcenter/
├── ResearchcenterApplication.java     ← stays at root
├── shared/                            ← cross-cutting concerns
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthFilter.java
│   │   └── CorsConfig.java
│   ├── exception/
│   │   └── GlobalExceptionHandler.java
│   └── response/
│       └── ApiResponse.java           ← standard response wrapper
└── features/
    ├── auth/
    │   ├── AuthController.java
    │   ├── AuthService.java
    │   ├── GoogleAuthService.java
    │   ├── JwtService.java
    │   ├── dto/
    │   │   ├── RegisterRequest.java
    │   │   ├── LoginRequest.java
    │   │   ├── GoogleAuthRequest.java
    │   │   └── AuthResponse.java
    │   ├── model/
    │   │   ├── User.java
    │   │   ├── Role.java
    │   │   └── RefreshToken.java
    │   └── repository/
    │       ├── UserRepository.java
    │       ├── RoleRepository.java
    │       └── RefreshTokenRepository.java
    ├── repository/
    │   ├── RepositoryController.java
    │   ├── RepositoryService.java
    │   ├── dto/
    │   │   ├── CreateRepositoryRequest.java
    │   │   └── RepositoryResponse.java
    │   ├── model/
    │   │   ├── Repository.java
    │   │   └── RepositoryMember.java
    │   └── repository/
    │       ├── RepositoryRepo.java
    │       └── RepositoryMemberRepo.java
    ├── material/
    │   ├── MaterialController.java
    │   ├── MaterialService.java
    │   ├── dto/
    │   │   ├── CreateMaterialRequest.java
    │   │   └── MaterialResponse.java
    │   ├── model/
    │   │   ├── Material.java
    │   │   └── MaterialTag.java
    │   └── repository/
    │       ├── MaterialRepo.java
    │       └── MaterialTagRepo.java
    ├── request/
    │   ├── RequestController.java
    │   ├── RequestService.java
    │   ├── dto/
    │   │   ├── CreateRequestDto.java
    │   │   ├── FulfillRequestDto.java
    │   │   └── RequestResponse.java
    │   ├── model/
    │   │   └── MaterialRequest.java
    │   └── repository/
    │       └── MaterialRequestRepo.java
    ├── user/
    │   ├── UserController.java
    │   └── UserService.java
    └── admin/
        └── AdminController.java
```

#### Backend Refactoring Steps:

1. Create the new folder structure above
2. Move each file to its feature folder — update the `package` declaration at the top of each file to match the new path
3. Update all `import` statements in every file to reference the new package paths
4. Shared models (User, Role) that are used across features stay in `shared/` or `auth/` — other features import from there
5. Do NOT change any logic, method names, or endpoint paths — only move files and update packages/imports
6. Verify the app still compiles: `./mvnw clean compile`

#### Package rename examples:

```java
// BEFORE
package edu.cit.uy.researchcenter.controller;
import edu.cit.uy.researchcenter.model.User;
import edu.cit.uy.researchcenter.service.AuthService;

// AFTER
package edu.cit.uy.researchcenter.features.auth;
import edu.cit.uy.researchcenter.features.auth.model.User;
import edu.cit.uy.researchcenter.features.auth.AuthService;
```

---

### WEB FRONTEND REFACTORING

Reorganize `web/src/` into this feature-based structure:

```
web/src/
├── main.tsx                          ← stays
├── App.tsx                           ← stays
├── shared/
│   ├── components/
│   │   ├── Navbar.tsx
│   │   ├── Breadcrumbs.tsx
│   │   ├── ProtectedRoute.tsx
│   │   └── LoadingSpinner.tsx
│   ├── auth/
│   │   └── sessionManager.ts
│   ├── api/
│   │   └── axiosClient.ts
│   └── types/
│       └── index.ts
└── features/
    ├── auth/
    │   ├── LoginPage.tsx
    │   ├── RegisterPage.tsx
    │   └── api/
    │       └── authApi.ts
    ├── dashboard/
    │   ├── DashboardPage.tsx
    │   └── components/
    │       └── RepositoryCard.tsx
    ├── repository/
    │   ├── RepositoryDetailPage.tsx
    │   └── api/
    │       └── repositoryApi.ts
    ├── material/
    │   ├── AddMaterialPage.tsx
    │   ├── EditMaterialPage.tsx
    │   ├── components/
    │   │   └── MaterialForm.tsx
    │   └── api/
    │       ├── materialApi.ts
    │       ├── googleBooksApi.ts
    │       └── supabaseUpload.ts
    └── request/
        ├── NewRequestPage.tsx
        └── api/
            └── requestApi.ts
```

#### Web Refactoring Steps:

1. Create the new folder structure above inside `web/src/`
2. Move each file to its feature folder
3. Update all import paths in every file (use relative paths)
4. Update `App.tsx` route imports to reference new paths
5. Do NOT change any component logic, API calls, or UI — only move files and update imports
6. Verify the app still runs: `npm run dev`

#### Import update example:

```typescript
// BEFORE (in RepositoryDetailPage.tsx)
import { repositoryApi } from '../api/repositoryApi';
import { SessionManager } from '../auth/sessionManager';
import { Material } from '../types';

// AFTER
import { repositoryApi } from './api/repositoryApi';
import { SessionManager } from '../../shared/auth/sessionManager';
import { Material } from '../../shared/types';
```

---

### MOBILE REFACTORING (if applicable)

Current mobile structure is already relatively feature-organized.
Apply vertical slice by grouping into feature packages:

```
mobile/ResearchCenter/app/src/main/java/com/example/researchcenterlabact/
├── shared/
│   ├── api/
│   │   └── ApiClient.kt
│   └── auth/
│       └── SessionManager.kt
└── features/
    ├── auth/
    │   ├── LoginActivity.kt
    │   ├── RegisterActivity.kt
    │   └── SplashActivity.kt
    ├── dashboard/
    │   └── DashboardActivity.kt
    └── profile/
        └── ProfileActivity.kt
```

Move Kotlin files and update package declarations accordingly.

---

## PART 3 — TEST PLAN CREATION

### Backend Tests (Spring Boot — JUnit 5 + MockMvc)

Add these test files under:
`backend/researchcenter/src/test/java/edu/cit/uy/researchcenter/`

Add test dependencies to `pom.xml` if not present:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

#### Test File 1: `features/auth/AuthControllerTest.java`

```java
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
            .andExpect(jsonPath("$.data.user.email").value(testEmail));
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
```

#### Test File 2: `features/repository/RepositoryControllerTest.java`

```java
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
```

#### Test File 3: `features/material/MaterialControllerTest.java`

```java
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
```

#### Test File 4: `features/request/RequestControllerTest.java`

```java
package edu.cit.uy.researchcenter.features.request;

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
class RequestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    static String token;
    static Long repoId;
    static Long requestId;

    @BeforeAll
    static void setup(@Autowired MockMvc mvc, @Autowired ObjectMapper mapper) throws Exception {
        String email = "reqtest_" + System.currentTimeMillis() + "@test.com";
        var reg = Map.of("email", email, "password", "Test1234!", "firstname", "Req", "lastname", "Tester");
        MvcResult r1 = mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(reg))).andReturn();
        token = mapper.readTree(r1.getResponse().getContentAsString()).at("/data/accessToken").asText();

        var repo = Map.of("name", "Request Test Repo", "description", "for request tests");
        MvcResult r2 = mvc.perform(post("/api/v1/repositories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(repo))).andReturn();
        repoId = mapper.readTree(r2.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    @Test @Order(1)
    @DisplayName("TC-REQ-001: Create request returns 201 with OPEN status")
    void testCreateRequest() throws Exception {
        var body = Map.of(
            "repositoryId", repoId,
            "title", "Looking for Deep Learning book",
            "description", "Need the Goodfellow 2016 edition"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/requests")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andReturn();
        requestId = objectMapper.readTree(res.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    @Test @Order(2)
    @DisplayName("TC-REQ-002: Get requests by repository returns list")
    void testGetRequests() throws Exception {
        mockMvc.perform(get("/api/v1/repositories/" + repoId + "/requests")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test @Order(3)
    @DisplayName("TC-REQ-003: Owner cannot fulfill own request (403)")
    void testOwnerCannotFulfillOwnRequest() throws Exception {
        // Owner IS the requester here — should get 403
        var body = Map.of("materialId", 999);
        mockMvc.perform(post("/api/v1/requests/" + requestId + "/fulfill")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            // Owner CAN fulfill per business rules — skip this if owner is allowed
            .andExpect(status().isOk()); // adjust based on your rule
    }

    @Test @Order(4)
    @DisplayName("TC-REQ-004: Close request returns CLOSED status")
    void testCloseRequest() throws Exception {
        var body = Map.of("note", "Found it elsewhere");
        mockMvc.perform(post("/api/v1/requests/" + requestId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test @Order(5)
    @DisplayName("TC-REQ-005: Cannot close an already-CLOSED request")
    void testCannotRecloseRequest() throws Exception {
        var body = Map.of("note", "Trying again");
        mockMvc.perform(post("/api/v1/requests/" + requestId + "/close")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict()); // or whatever your error returns
    }
}
```

---

### Run Tests

```bash
cd backend/researchcenter

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthControllerTest

# Run with verbose output and save to file
./mvnw test | tee test-results.txt

# Generate Surefire report
./mvnw surefire-report:report
# Report appears at: target/site/surefire-report.html
```

---

## PART 4 — FULL REGRESSION TEST CHECKLIST

After refactoring, manually verify every feature still works.
Check each item and record PASS / FAIL:

### Authentication
- [ ] Register new user with email/password → user created, JWT returned
- [ ] Register duplicate email → 409 error, AUTH-002
- [ ] Login with valid credentials → JWT returned
- [ ] Login with wrong password → 401 error, AUTH-001
- [ ] Login with Google → new user created OR existing user logged in
- [ ] GET /users/me with valid JWT → user data returned (no password)
- [ ] GET /users/me without token → 401

### Repository Management
- [ ] Create repository → appears in dashboard
- [ ] List repositories → shows owned and member repos
- [ ] Get repository detail → shows members, material count
- [ ] Update repository name/description → changes reflected
- [ ] Delete repository → removed from list
- [ ] Access other user's repository → 403

### Members
- [ ] Search user by email in invite flow → user found
- [ ] Invite user → user added as MEMBER
- [ ] Invite already-member user → conflict shown
- [ ] Remove member → removed from list
- [ ] Owner cannot be removed → confirm Remove not shown on owner card
- [ ] Non-owner cannot see invite section → confirm UI hidden

### Materials
- [ ] Add LINK material → appears in materials tab
- [ ] Add PDF material → uploaded to Supabase, fileUrl saved
- [ ] Google Books fetch auto-fills title and description
- [ ] Tags saved correctly (lowercase, trimmed)
- [ ] Edit material (own) → changes reflected
- [ ] Edit material (other's, as owner) → changes reflected
- [ ] Edit material (other's, as member) → Edit button not shown
- [ ] Delete material (own) → removed
- [ ] Delete material (other's, as owner) → removed
- [ ] Delete material (other's, as member) → Delete button not shown
- [ ] Filter by tag → correct materials shown
- [ ] Search by title → correct materials shown
- [ ] Filter by status → correct materials shown
- [ ] Sort latest/oldest → correct order
- [ ] Update status in View Info modal → status badge updates

### Requests
- [ ] Create request → appears with OPEN status
- [ ] Member cannot fulfill own request → Fulfill button hidden for own requests
- [ ] Member can fulfill other's request → material attached, status FULFILLED
- [ ] Owner can fulfill any request → including own
- [ ] Close request with note → status CLOSED, note shown
- [ ] CLOSED request is read-only → no edit/fulfill/close buttons shown
- [ ] Delete OPEN request (own) → removed
- [ ] Cannot delete CLOSED request → confirm behavior

### Navigation & UI
- [ ] Breadcrumb shows correct path at each depth
- [ ] All buttons have cursor-pointer
- [ ] Modals have backdrop blur (not black)
- [ ] Toast notifications appear and auto-dismiss after 3 seconds
- [ ] Navbar shows user initials avatar
- [ ] Logout clears session and redirects to login

---

## PART 5 — COMMIT STRATEGY

Make separate meaningful commits as you go:

```bash
# After creating branch
git commit -m "chore: create vertical slice refactor branch"

# Backend refactoring
git commit -m "refactor(backend): reorganize auth feature into vertical slice"
git commit -m "refactor(backend): reorganize repository feature into vertical slice"
git commit -m "refactor(backend): reorganize material feature into vertical slice"
git commit -m "refactor(backend): reorganize request feature into vertical slice"
git commit -m "refactor(backend): move shared config and exception handler"

# Web refactoring
git commit -m "refactor(web): reorganize auth feature into vertical slice"
git commit -m "refactor(web): reorganize dashboard and repository features"
git commit -m "refactor(web): reorganize material and request features"
git commit -m "refactor(web): update shared components and types"

# Tests
git commit -m "test(backend): add AuthControllerTest with 5 test cases"
git commit -m "test(backend): add RepositoryControllerTest with 6 test cases"
git commit -m "test(backend): add MaterialControllerTest with 4 test cases"
git commit -m "test(backend): add RequestControllerTest with 5 test cases"

# After regression testing
git commit -m "test: full regression test completed - all features passing"

# Final
git commit -m "docs: update project structure documentation"
```

---

## NOTES FOR CLAUDE HAIKU

- Base package is `edu.cit.uy.researchcenter` — never change this
- User entity fields: `firstname`/`lastname` (no camelCase) — getters are `getFirstname()` / `getLastname()`
- All API endpoints stay the same — only internal code structure changes
- The standard response wrapper format must stay: `{ success, data, error, timestamp }`
- `ddl-auto: update` — database schema auto-updates, no SQL migrations needed
- JWT secret and DB credentials are in `.env` which is gitignored — never commit it
- When refactoring web imports, use relative paths only — no absolute paths
- After every major file move, run `./mvnw clean compile` (backend) or `npm run dev` (web) to verify nothing broke
- Test classes use `@SpringBootTest` with `@AutoConfigureMockMvc` — this starts the full Spring context
- Tests hit the actual database (Supabase) — use unique emails per test run with `System.currentTimeMillis()`
- Do NOT change any business logic, endpoint URLs, or database schema during refactoring
- Mobile refactoring is optional — only do it if time permits
- The `shared/` folder in both backend and web holds cross-cutting concerns used by multiple features
