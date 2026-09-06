package com.casava.demo.quote;

import com.casava.demo.auth.CurrentUser;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

  private final QuoteService quoteService;

  public QuoteController(QuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @PostMapping("/preview")
  public QuotePreviewResponse preview(@RequestBody QuoteRequest request) {
    return quoteService.preview(request);
  }

  @PostMapping
  public QuoteResponse create(@RequestBody QuoteRequest request) {
    UUID userId = CurrentUser.requireUserId();
    return QuoteResponse.from(quoteService.create(userId, request));
  }

  @GetMapping("/{id}")
  public QuoteResponse get(@PathVariable UUID id) {
    UUID userId = CurrentUser.requireUserId();
    return QuoteResponse.from(quoteService.getOwned(userId, id));
  }
}
