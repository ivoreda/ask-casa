package com.casava.demo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.casava.demo.knowledge.KnowledgeReindexService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "casava.knowledge.reindex-on-startup=false",
      "casava.jwt.secret=test-secret-must-be-long-enough-123456",
      "casava.jwt.expiration-ms=3600000",
      "casava.ai.vector-store-path=${java.io.tmpdir}/casava-auth-controller-vs.json",
      "spring.datasource.url=jdbc:h2:mem:auth-controller;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.ai.openai.api-key=test-key"
    })
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private EmbeddingModel embeddingModel;
  @MockitoBean private KnowledgeReindexService knowledgeReindexService;

  @Test
  void registerThenMeWithBearerReturnsEmail() throws Exception {
    MvcResult registerResult =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"ada@example.com","password":"secret123","name":"Ada Lovelace"}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.email").value("ada@example.com"))
            .andReturn();

    JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
    String token = body.get("token").asString();
    assertThat(token).isNotBlank();

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("ada@example.com"))
        .andExpect(jsonPath("$.name").value("Ada Lovelace"));
  }

  @Test
  void meWithoutTokenReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void loginWithBadPasswordReturnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"bob@example.com","password":"secret123","name":"Bob"}
                    """))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"bob@example.com","password":"wrong-password"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void duplicateRegisterReturnsConflict() throws Exception {
    String payload =
        """
        {"email":"carol@example.com","password":"secret123","name":"Carol"}
        """;

    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Email already registered"));
  }
}
