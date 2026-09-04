package com.casava.demo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ChatController.class)
@TestPropertySource(properties = "spring.ai.openai.api-key=")
class ChatControllerMissingKeyTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RagChatService ragChatService;

  @Test
  void emitsErrorEventWhenApiKeyBlank() throws Exception {
    MvcResult mvcResult =
        mockMvc
            .perform(
                post("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"sessionId":"s1","message":"What is covered?"}
                        """))
            .andExpect(request().asyncStarted())
            .andReturn();

    mvcResult.getAsyncResult(10_000);

    String body =
        mockMvc
            .perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).contains("\"type\":\"error\"");
    assertThat(body).containsIgnoringCase("OPENROUTER_API_KEY");
    verifyNoInteractions(ragChatService);
  }
}
