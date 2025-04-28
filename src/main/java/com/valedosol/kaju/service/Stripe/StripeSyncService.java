package com.valedosol.kaju.service.Stripe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.repository.SubscriptionPlanRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class StripeSyncService {
  private final SubscriptionPlanRepository subscriptionPlanRepository;
  
  @Value("${stripe.api.key}")
  private String stripeApiKey;

  public StripeSyncService(SubscriptionPlanRepository subscriptionPlanRepository) {
    this.subscriptionPlanRepository = subscriptionPlanRepository;
  }

  @PostConstruct
  public void initializeStripeProducts() {
    if (stripeApiKey == null || stripeApiKey.isEmpty()) {
      log.warn("Stripe API key not configured. Skipping product initialization.");
      return;
    }
    
    Stripe.apiKey = stripeApiKey;
    List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
    
    for (SubscriptionPlan plan : plans) {
      try {
        // Create or update product in Stripe
        String productId = plan.getStripeProductId();
        Product product;
        
        if (productId == null || productId.isEmpty()) {
          // Create new product
          product = createStripeProduct(plan);
          plan.setStripeProductId(product.getId());
          log.info("Created new Stripe product: {} with ID: {}", plan.getName(), product.getId());
        } else {
          // Product exists, just log it
          log.info("Using existing Stripe product ID: {} for plan: {}", productId, plan.getName());
          product = Product.retrieve(productId);
        }
        
        // Create or update price
        String priceId = plan.getStripePriceId();
        if (priceId == null || priceId.isEmpty()) {
          Price price = createStripePrice(plan, product.getId());
          plan.setStripePriceId(price.getId());
          log.info("Created new Stripe price: {} with ID: {}", price.getUnitAmountDecimal(), price.getId());
        } else {
          log.info("Using existing Stripe price ID: {} for plan: {}", priceId, plan.getName());
        }
        
        subscriptionPlanRepository.save(plan);
      } catch (StripeException e) {
        log.error("Failed to sync Stripe product for plan: {}", plan.getName(), e);
      }
    }
  }

  private Product createStripeProduct(SubscriptionPlan plan) throws StripeException {
    Map<String, Object> params = new HashMap<>();
    params.put("name", plan.getName());
    params.put("description", "Plano de assinatura com " + plan.getWeeklyAllowedSends() + " envios semanais.");
    
    // Add metadata for better tracking
    Map<String, String> metadata = new HashMap<>();
    metadata.put("plan_id", plan.getId().toString());
    params.put("metadata", metadata);

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
    
    // Add metadata
    Map<String, String> metadata = new HashMap<>();
    metadata.put("plan_id", plan.getId().toString());
    params.put("metadata", metadata);

    return Price.create(params);
  }
}