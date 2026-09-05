package com.casava.demo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.casava.demo.auth.SecurityConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

@WebMvcTest(
    controllers = ChatController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      ServletWebSecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class
    },
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.ai.openai.api-key=test-key")
class ChatControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RagChatService ragChatService;

  @Test
  void streamsStatusTokenAndDoneEvents() throws Exception {
    Citation citation =
        new Citation("Intentional damage", "exclusion", "Device Protection", "device-protection");
    when(ragChatService.chat(eq("s1"), eq("What is covered?")))
        .thenReturn(new RagChatResult(Flux.just("Theft ", "is covered."), List.of(citation)));

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

    assertThat(body).contains("\"type\":\"status\"");
    assertThat(body).contains("searching knowledge");
    assertThat(body).contains("\"type\":\"token\"");
    assertThat(body).contains("Theft ");
    assertThat(body).contains("is covered.");
    assertThat(body).contains("\"type\":\"done\"");
    assertThat(body).contains("device-protection");
  }
}
