package com.valedosol.kaju.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.valedosol.kaju.model.Account;
import com.valedosol.kaju.model.StripeSubscription;
import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.repository.StripeSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StripeService {

    @Value("${stripe.public.key}")
    private String stripePublicKey;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final StripeSubscriptionRepository stripeSubscriptionRepository;

    public StripeService(StripeSubscriptionRepository stripeSubscriptionRepository) {
        this.stripeSubscriptionRepository = stripeSubscriptionRepository;
    }

    public String getPublicKey() {
        return stripePublicKey;
    }

    // Cria ou obtém cliente no Stripe
    public Customer createOrGetCustomer(Account account) throws StripeException {
        // Busca no banco de dados se já existe um customer ID para este usuário
        StripeSubscription existingSubscription = stripeSubscriptionRepository
                .findByAccountId(account.getId())
                .orElse(null);

        if (existingSubscription != null && existingSubscription.getStripeCustomerId() != null) {
            return Customer.retrieve(existingSubscription.getStripeCustomerId());
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(account.getEmail())
                .setName(account.getNickname())
                .build();

        Customer customer = Customer.create(params);
        
        // Se não existir inscrição ainda, criar uma nova entrada
        if (existingSubscription == null) {
            existingSubscription = new StripeSubscription();
            existingSubscription.setAccount(account);
        }
        
        existingSubscription.setStripeCustomerId(customer.getId());
        stripeSubscriptionRepository.save(existingSubscription);
        
        return customer;
    }

    // Método para criar um produto no Stripe (representa um plano de assinatura)
    public Product createProduct(SubscriptionPlan plan) throws StripeException {
        Map<String, Object> productParams = new HashMap<>();
        productParams.put("name", plan.getName());
        productParams.put("description", "Plano " + plan.getName() + ": " + plan.getWeeklyAllowedSends() + " envios semanais");
        
        return Product.create(productParams);
    }

    // Método para criar um preço vinculado a um produto (define o valor do plano)
    public Price createPrice(String productId, SubscriptionPlan plan) throws StripeException {
        Map<String, Object> priceParams = new HashMap<>();
        priceParams.put("product", productId);
        priceParams.put("unit_amount", (int)(plan.getPrice() * 100)); // Stripe trabalha em centavos
        priceParams.put("currency", "brl");
        priceParams.put("recurring", Map.of(
            "interval", "month",   // Cobrança mensal
            "interval_count", 1    // A cada 1 mês
        ));
        
        return Price.create(priceParams);
    }

    // Cria uma sessão de checkout do Stripe
    public Map<String, Object> createCheckoutSession(Account account, SubscriptionPlan plan, String successUrl, String cancelUrl) throws StripeException {
        // Assegurar que o cliente existe no Stripe
        Customer customer = createOrGetCustomer(account);

        // Criar produto se necessário (ou usar ID existente em um sistema de produção)
        Product product = createProduct(plan);
        
        // Criar preço para o produto
        Price price = createPrice(product.getId(), plan);
        
        // Cria a sessão de checkout
        List<Object> lineItems = new ArrayList<>();
        Map<String, Object> lineItem = new HashMap<>();
        lineItem.put("price", price.getId());
        lineItem.put("quantity", 1);
        lineItems.add(lineItem);
        
        Map<String, Object> params = new HashMap<>();
        params.put("customer", customer.getId());
        params.put("payment_method_types", List.of("card"));
        params.put("line_items", lineItems);
        params.put("mode", "subscription");
        params.put("success_url", successUrl);
        params.put("cancel_url", cancelUrl);
        
        com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(params);
        
        // Retorna os dados necessários para o frontend
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("sessionId", session.getId());
        responseData.put("publicKey", stripePublicKey);
        
        return responseData;
    }

    // Método para processar eventos de webhook do Stripe
    public void processWebhookEvent(String payload, String sigHeader) throws StripeException {
  
    Event event = null;
    
    try {
        event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
        throw new StripeException("Invalid signature", null, null, null, null) {};
    }
    
    // Processar o evento baseado no tipo
    switch (event.getType()) {
        case "checkout.session.completed":
            // A sessão de checkout foi completada com sucesso
            com.stripe.model.checkout.Session session = 
                (com.stripe.model.checkout.Session) event.getDataObjectDeserializer().getObject().get();
            handleSuccessfulPayment(session);
            break;
            
        case "invoice.paid":
            // Fatura paga (renovação de assinatura)
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().get();
            handleSuccessfulRenewal(invoice);
            break;
            
        case "customer.subscription.updated":
            // Assinatura atualizada
            com.stripe.model.Subscription subscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().get();
            handleSubscriptionUpdate(subscription);
            break;
            
        case "customer.subscription.deleted":
            // Assinatura cancelada
            com.stripe.model.Subscription canceledSubscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().get();
            handleSubscriptionCancellation(canceledSubscription);
            break;
            
        default:
            System.out.println("Unhandled event type: " + event.getType());
    }
}

private void handleSuccessfulPayment(com.stripe.model.checkout.Session session) {
    // Ativar a assinatura do usuário
    String subscriptionId = session.getSubscription();
    try {
        activateSubscription(subscriptionId);
    } catch (StripeException e) {
        e.printStackTrace();
    }
}

private void handleSuccessfulRenewal(Invoice invoice) {
    // Processar renovação de assinatura
    String subscriptionId = invoice.getSubscription();
    try {
        // Renovar assinatura no sistema
        activateSubscription(subscriptionId);
    } catch (StripeException e) {
        e.printStackTrace();
    }
}

private void handleSubscriptionUpdate(com.stripe.model.Subscription subscription) {
    // Atualizar status da assinatura no sistema
    // Exemplo: mudança de plano, pausa, etc.
}

private void handleSubscriptionCancellation(com.stripe.model.Subscription subscription) {
    // Marcar assinatura como cancelada no sistema
    StripeSubscription stripeSubscription = stripeSubscriptionRepository
        .findByStripeSubscriptionId(subscription.getId())
        .orElse(null);
    
    if (stripeSubscription != null) {
        stripeSubscription.setStatus("canceled");
        stripeSubscriptionRepository.save(stripeSubscription);
        
        // Você também pode atualizar o plano do usuário para gratuito
        Account account = stripeSubscription.getAccount();
        account.setSubscriptionPlan(null);
        // accountRepository.save(account);
    }
}

    // Método para ativar uma assinatura após pagamento bem-sucedido
    public void activateSubscription(String stripeSubscriptionId) throws StripeException {
        com.stripe.model.Subscription stripeSubscription = 
            com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        
        StripeSubscription subscription = stripeSubscriptionRepository
            .findByStripeSubscriptionId(stripeSubscriptionId)
            .orElse(null);
        
        if (subscription != null && "active".equals(stripeSubscription.getStatus())) {
            // Atualizar status da assinatura
            subscription.setStatus("active");
            
            // Atualizar próxima data de cobrança
            long nextBillingTimestamp = stripeSubscription.getCurrentPeriodEnd();
            LocalDateTime nextBillingDate = 
                LocalDateTime.ofInstant(Instant.ofEpochSecond(nextBillingTimestamp), ZoneId.systemDefault());
            subscription.setNextBillingDate(nextBillingDate);
            
            stripeSubscriptionRepository.save(subscription);
            
            // Atualizar o plano do usuário no sistema
            Account account = subscription.getAccount();
            account.setSubscriptionPlan(subscription.getSubscriptionPlan());
            account.setRemainingWeeklySends(subscription.getSubscriptionPlan().getWeeklyAllowedSends());
            // Salvar account em AccountRepository
        }
    }

    // Método para cancelar uma assinatura
    public void cancelSubscription(String stripeSubscriptionId) throws StripeException {
        com.stripe.model.Subscription subscription = 
            com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        subscription.cancel();
        
        // Atualizar o status da assinatura no banco de dados
        StripeSubscription stripeSubscription = stripeSubscriptionRepository
            .findByStripeSubscriptionId(stripeSubscriptionId)
            .orElse(null);
        
        if (stripeSubscription != null) {
            stripeSubscription.setStatus("canceled");
            stripeSubscriptionRepository.save(stripeSubscription);
        }
    }
}