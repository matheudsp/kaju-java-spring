package com.valedosol.kaju.service.Stripe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.repository.SubscriptionPlanRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StripeSyncService {
  private final SubscriptionPlanRepository subscriptionPlanRepository;

  public StripeSyncService(SubscriptionPlanRepository subscriptionPlanRepository) {
    this.subscriptionPlanRepository = subscriptionPlanRepository;
  }

  @PostConstruct
  public void initializeStripeProducts() {
    List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
    for (SubscriptionPlan plan : plans) {
      if (plan.getStripeProductId() == null) {
        try {
          // Create product in Stripe
          Product product = createStripeProduct(plan);
          plan.setStripeProductId(product.getId());

          // Create price for the product
          Price price = createStripePrice(plan, product.getId());
          plan.setStripePriceId(price.getId());

          subscriptionPlanRepository.save(plan);
        } catch (StripeException e) {
          log.error("Failed to create Stripe product for plan: {}", plan.getName(), e);
        }
      }
    }
  }

  private Product createStripeProduct(SubscriptionPlan plan) throws StripeException {
    Map<String, Object> params = new HashMap<>();
    params.put("name", plan.getName());
    params.put("description", "Subscription plan with " + plan.getWeeklyAllowedSends() + " weekly allowed sends");

    return Product.create(params);
  }

  private Price createStripePrice(SubscriptionPlan plan, String productId) throws StripeException {
    Map<String, Object> params = new HashMap<>();
    params.put("product", productId);
    params.put("unit_amount", (int) (plan.getPrice() * 100)); // Convert to cents
    params.put("currency", "brl");

    Map<String, Object> recurring = new HashMap<>();
    recurring.put("interval", plan.getBillingInterval());
    params.put("recurring", recurring);

    return Price.create(params);
  }
}