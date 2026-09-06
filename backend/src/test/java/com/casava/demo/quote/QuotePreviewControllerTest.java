package com.casava.demo.quote;

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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "casava.knowledge.reindex-on-startup=false",
      "casava.jwt.secret=test-secret-must-be-long-enough-123456",
      "casava.jwt.expiration-ms=3600000",
      "casava.ai.vector-store-path=${java.io.tmpdir}/casava-quote-preview-vs.json",
      "spring.datasource.url=jdbc:h2:mem:quote-preview;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.ai.openai.api-key=test-key"
    })
class QuotePreviewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private EmbeddingModel embeddingModel;
  @MockitoBean private KnowledgeReindexService knowledgeReindexService;

  @Test
  void previewQuoteWithoutAuthReturnsPremium() throws Exception {
    mockMvc
        .perform(
            post("/api/quotes/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productSlug":"device-protection","deviceValue":800000}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.monthlyPremium").isNumber())
        .andExpect(jsonPath("$.coverAmount").value(800000))
        .andExpect(jsonPath("$.id").doesNotExist());
  }

  @Test
  void createQuoteWithoutAuthReturnsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"productSlug":"device-protection","deviceValue":800000}
                    """))
        .andExpect(status().isUnauthorized());
  }
}
