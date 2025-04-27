package com.valedosol.kaju.controller;

import com.stripe.model.Subscription;
import com.valedosol.kaju.dto.SessionDto;
import com.valedosol.kaju.service.StripeService;

import lombok.AllArgsConstructor;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/stripe")
@AllArgsConstructor
public class StripeV2Controller {
  
  
  private final StripeService stripeService;

  @PostMapping("/session/payment")
  @ResponseBody
  public SessionDto sessionPayment(@RequestBody SessionDto model) {

    return stripeService.createPaymentSession(model);
  }

  @PostMapping("/session/subscription")
  @ResponseBody
  public SessionDto createSubscriptionSession(@RequestBody SessionDto model) {

    return stripeService.createSubscriptionSession(model);
  }

}
