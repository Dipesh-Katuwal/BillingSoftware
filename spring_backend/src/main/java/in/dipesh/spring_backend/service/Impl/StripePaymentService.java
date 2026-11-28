package in.dipesh.spring_backend.service.Impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import in.dipesh.spring_backend.config.StripeProperties;
import in.dipesh.spring_backend.dto.PaymentResult;
import in.dipesh.spring_backend.dto.StripePaymentResponse;
import in.dipesh.spring_backend.dto.StripeCheckoutOptions;
import in.dipesh.spring_backend.entity.*;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.StripeCheckoutClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StripePaymentService {
    private static final Set<String> EVENTS = Set.of("checkout.session.completed",
            "checkout.session.async_payment_succeeded", "checkout.session.async_payment_failed",
            "checkout.session.expired");
    private final OrderRepository orders;
    private final StripeCheckoutClient client;
    private final StripeProperties properties;

    public StripeCheckoutOptions checkoutOptions() {
        return new StripeCheckoutOptions("NPR", properties.minimumTotalNpr());
    }

    @Transactional
    public StripePaymentResponse initiatePayment(String orderId) {
        requireWebhookSecret();
        OrderEntity order = findOrder(orderId);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw conflict("This order has already been paid");
        }
        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            if (order.getPaymentMethod() != PaymentMethod.STRIPE || order.getStripeSessionId() == null) {
                throw conflict("Another payment for this order is already pending");
            }
            Session existing = client.retrieve(order.getStripeSessionId());
            validate(order, existing);
            return checkoutResponse(existing);
        }
        // A stable idempotency key recovers an ambiguous network failure. A finished
        // Stripe attempt is never replaced; use a new order after expiry/failure.
        if (order.getStripeSessionId() != null) {
            throw conflict("This Stripe checkout has finished. Check its status or create a new order.");
        }
        long amount = amount(order);
        if (order.getGrandTotal().compareTo(properties.minimumTotalNpr()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stripe checkout in this app requires an order total of at least NPR "
                            + properties.minimumTotalNpr().toPlainString()
                            + ". Add more items or choose cash or Khalti.");
        }
        String resultUrl = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                .path("/payment-result").queryParam("method", "STRIPE")
                .queryParam("orderId", orderId).build().encode().toUriString();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addAllowedPaymentMethodType(SessionCreateParams.AllowedPaymentMethodType.CARD)
                .setClientReferenceId(orderId).putMetadata("orderId", orderId)
                .setSuccessUrl(resultUrl).setCancelUrl(resultUrl)
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("npr").setUnitAmount(amount)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Billing order " + orderId).build()).build()).build())
                .build();
        Session session = client.create(params, "billing-stripe-checkout-" + orderId);
        StripePaymentResponse response = checkoutResponse(session);
        // Validate Stripe's response before persisting the association.
        order.setStripeSessionId(session.getId());
        order.setPaymentMethod(PaymentMethod.STRIPE);
        validate(order, session);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orders.save(order);
        return response;
    }

    @Transactional
    public PaymentResult refreshPaymentStatus(String orderId) {
        OrderEntity order = findOrder(orderId);
        if (order.getPaymentMethod() != PaymentMethod.STRIPE || order.getStripeSessionId() == null) {
            throw conflict("This order has no Stripe payment to check");
        }
        Session session = client.retrieve(order.getStripeSessionId());
        validate(order, session);
        update(order, session, false);
        String message = switch (order.getPaymentStatus()) {
            case PAID -> "Stripe payment confirmed.";
            case FAILED -> "Stripe checkout expired or payment failed. Create a new order to try again.";
            default -> "Stripe payment is pending. Returning or canceling in the browser does not cancel the session. Check again after payment or expiry.";
        };
        return new PaymentResult(orderId, order.getPaymentStatus(), message);
    }

    @Transactional
    public void webhook(String rawBody, String signature) {
        requireWebhookSecret();
        Event event;
        try {
            if (signature == null || signature.isBlank()) throw new IllegalArgumentException();
            event = Webhook.constructEvent(rawBody, signature, properties.webhookSecret());
        } catch (SignatureVerificationException | IllegalArgumentException | com.google.gson.JsonParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature or payload");
        }
        if (!EVENTS.contains(event.getType())) return;
        Object object = event.getDataObjectDeserializer().getObject().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stripe event API version does not match the SDK; configure the webhook API version"));
        if (!(object instanceof Session session) || session.getClientReferenceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe Checkout Session");
        }
        OrderEntity order = findOrder(session.getClientReferenceId());
        validate(order, session);
        update(order, session, "checkout.session.async_payment_failed".equals(event.getType()));
    }

    private void validate(OrderEntity order, Session session) {
        if (session == null || !Boolean.FALSE.equals(session.getLivemode())
                || order.getPaymentMethod() != PaymentMethod.STRIPE
                || order.getStripeSessionId() == null
                || !order.getStripeSessionId().equals(session.getId())
                || !order.getOrderId().equals(session.getClientReferenceId())
                || session.getMetadata() == null
                || !order.getOrderId().equals(session.getMetadata().get("orderId"))
                || !"payment".equals(session.getMode())
                || !"npr".equals(session.getCurrency())
                || session.getAmountTotal() == null || session.getAmountTotal() != amount(order)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stripe session does not match the stored order, amount, currency or test mode");
        }
    }

    private void update(OrderEntity order, Session session, boolean failed) {
        if (order.getPaymentStatus() == PaymentStatus.PAID) return;
        PaymentStatus next = order.getPaymentStatus();
        if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
            next = PaymentStatus.PAID;
        } else if ("expired".equals(session.getStatus()) || failed) {
            next = PaymentStatus.FAILED;
        }
        if (next != order.getPaymentStatus()) {
            order.setPaymentStatus(next);
            orders.save(order);
        }
    }

    private StripePaymentResponse checkoutResponse(Session session) {
        if (session == null || session.getId() == null || !"open".equals(session.getStatus())) {
            throw conflict("Stripe checkout is no longer open. Refresh the payment status.");
        }
        try {
            URI url = URI.create(session.getUrl());
            if (!"https".equals(url.getScheme()) || !"checkout.stripe.com".equals(url.getHost())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe returned an invalid checkout URL");
        }
        return new StripePaymentResponse(session.getId(), session.getUrl());
    }

    private long amount(OrderEntity order) {
        try {
            long amount = order.getGrandTotal().movePointRight(2).longValueExact();
            if (amount <= 0) throw new ArithmeticException();
            return amount;
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stripe requires a positive order total in exact NPR paisa");
        }
    }

    private void requireWebhookSecret() {
        if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe is unavailable: configure STRIPE_WEBHOOK_SECRET");
        }
    }

    private OrderEntity findOrder(String orderId) {
        return orders.findByOrderId(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
