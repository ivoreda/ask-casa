package com.casava.demo.purchase;

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
      "casava.ai.vector-store-path=${java.io.tmpdir}/casava-quote-purchase-vs.json",
      "spring.datasource.url=jdbc:h2:mem:quote-purchase;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.ai.openai.api-key=test-key"
    })
class QuotePurchaseIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private EmbeddingModel embeddingModel;
  @MockitoBean private KnowledgeReindexService knowledgeReindexService;

  @Test
  void registerLoginQuotePurchaseListPolicies_otherUserDenied() throws Exception {
    String ownerToken =
        registerAndLogin("owner@example.com", "secret123", "Owner User");
    String otherToken =
        registerAndLogin("other@example.com", "secret123", "Other User");

    MvcResult quoteResult =
        mockMvc
            .perform(
                post("/api/quotes")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"productSlug":"device-protection","deviceValue":800000}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productSlug").value("device-protection"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.monthlyPremium").value(4000))
            .andExpect(jsonPath("$.coverAmount").value(800000))
            .andReturn();

    JsonNode quoteBody = objectMapper.readTree(quoteResult.getResponse().getContentAsString());
    String quoteId = quoteBody.get("id").asString();
    assertThat(quoteId).isNotBlank();

    MvcResult purchaseResult =
        mockMvc
            .perform(
                post("/api/purchases")
                    .header("Authorization", "Bearer " + ownerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quoteId\":\"" + quoteId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productSlug").value("device-protection"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.holderEmail").value("owner@example.com"))
            .andExpect(jsonPath("$.policyNumber").value(org.hamcrest.Matchers.startsWith("CSV-")))
            .andReturn();

    JsonNode policyBody =
        objectMapper.readTree(purchaseResult.getResponse().getContentAsString());
    String policyId = policyBody.get("id").asString();

    mockMvc
        .perform(get("/api/policies/me").header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(policyId));

    mockMvc
        .perform(
            get("/api/policies/" + policyId).header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            get("/api/quotes/" + quoteId).header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());
  }

  private String registerAndLogin(String email, String password, String name) throws Exception {
    MvcResult registerResult =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email":"%s","password":"%s","name":"%s"}
                        """
                            .formatted(email, password, name)))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
    String token = body.get("token").asString();
    assertThat(token).isNotBlank();

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"%s"}
                    """
                        .formatted(email, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value(email));

    return token;
  }
}
