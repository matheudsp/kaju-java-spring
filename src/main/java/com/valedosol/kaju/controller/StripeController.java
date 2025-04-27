package com.valedosol.kaju.controller;


import org.springframework.web.bind.annotation.*;

import com.valedosol.kaju.service.StripeService;

import com.stripe.model.Subscription;
import com.valedosol.kaju.dto.StripeChargeDto;
import com.valedosol.kaju.dto.StripeSubscriptionDto;
import com.valedosol.kaju.dto.StripeSubscriptionResponse;
import com.valedosol.kaju.dto.StripeTokenDto;
import com.valedosol.kaju.dto.SubscriptionCancelRecord;


import static java.util.Objects.nonNull;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {
  private final StripeService stripeService;

  public StripeController(StripeService stripeService) {
    this.stripeService = stripeService;
  }

  @PostMapping("/card/token")
  @ResponseBody
  public StripeTokenDto createCardToken(@RequestBody StripeTokenDto model) {

    return stripeService.createCardToken(model);
  }

  @PostMapping("/charge")
  @ResponseBody
  public StripeChargeDto charge(@RequestBody StripeChargeDto model) {

    return stripeService.charge(model);
  }

  @PostMapping("/customer/subscription")
  @ResponseBody
  public StripeSubscriptionResponse subscription(@RequestBody StripeSubscriptionDto model) {

    return stripeService.createSubscription(model);
  }

  @DeleteMapping("/subscription/{id}")
  @ResponseBody
  public SubscriptionCancelRecord cancelSubscription(@PathVariable String id) {

    Subscription subscription = stripeService.cancelSubscription(id);
    if (nonNull(subscription)) {

      return new SubscriptionCancelRecord(subscription.getStatus());
    }

    return null;
  }

}
