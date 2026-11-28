package in.dipesh.spring_backend.service.Impl;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.param.checkout.SessionCreateParams;
import in.dipesh.spring_backend.config.StripeProperties;
import in.dipesh.spring_backend.entity.*;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.StripeCheckoutClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StripePaymentServiceTest {
    private static final String SECRET = "whsec_unit_test_fixture";
    private final OrderRepository orders = mock(OrderRepository.class);
    private final StripeCheckoutClient client = mock(StripeCheckoutClient.class);
    private final StripeProperties properties = new StripeProperties("sk_test_fixture", SECRET, "http://localhost:5173");
    private final StripePaymentService service = new StripePaymentService(orders, client, properties);
    private OrderEntity order;

    @BeforeEach
    void setup() {
        order = OrderEntity.builder().orderId("ORD-123").grandTotal(new BigDecimal("372.30")).build();
        when(orders.findByOrderId("ORD-123")).thenReturn(Optional.of(order));
    }

    private Session session(String status, String paymentStatus) {
        Session s = new Session();
        s.setId("cs_test_123");
        s.setObject("checkout.session");
        s.setClientReferenceId("ORD-123");
        s.setMetadata(Map.of("orderId", "ORD-123"));
        s.setCurrency("npr");
        s.setAmountTotal(37230L);
        s.setMode("payment");
        s.setLivemode(false);
        s.setStatus(status);
        s.setPaymentStatus(paymentStatus);
        s.setUrl("https://checkout.stripe.com/c/pay/cs_test_123");
        return s;
    }

    private void pending() {
        order.setPaymentMethod(PaymentMethod.STRIPE);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStripeSessionId("cs_test_123");
    }

    @Test
    void createsSessionFromStoredTotalWithReferenceAndStableIdempotencyKey() {
        when(client.create(any(), anyString())).thenReturn(session("open", "unpaid"));
        var result = service.initiatePayment("ORD-123");
        var params = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(client).create(params.capture(), eq("billing-stripe-checkout-ORD-123"));
        var request = params.getValue();
        assertEquals(37230L, request.getLineItems().get(0).getPriceData().getUnitAmount());
        assertEquals("npr", request.getLineItems().get(0).getPriceData().getCurrency());
        assertEquals(1L, request.getLineItems().get(0).getQuantity());
        assertEquals("ORD-123", request.getClientReferenceId());
        assertEquals("ORD-123", request.getMetadata().get("orderId"));
        assertEquals(request.getSuccessUrl(), request.getCancelUrl());
        assertFalse(request.getSuccessUrl().contains("status=PAID"));
        assertEquals("cs_test_123", result.sessionId());
        assertEquals(result.sessionId(), order.getStripeSessionId());
        assertEquals(PaymentMethod.STRIPE, order.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"23.46", "46.92", "99.99"})
    void rejectsSmallStoredTotalsBeforeContactingStripe(String total) {
        order.setGrandTotal(new BigDecimal(total));
        var exception = assertThrows(ResponseStatusException.class,
                () -> service.initiatePayment("ORD-123"));
        assertTrue(exception.getReason().contains("100.00"));
        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
        assertNull(order.getPaymentMethod());
        assertNull(order.getStripeSessionId());
        verifyNoInteractions(client);
        verify(orders, never()).save(any());
    }

    @Test
    void exactMinimumUsesTheUnchangedStoredTotal() {
        order.setGrandTotal(new BigDecimal("100.00"));
        Session session = session("open", "unpaid");
        session.setAmountTotal(10000L);
        when(client.create(any(), anyString())).thenReturn(session);
        service.initiatePayment("ORD-123");
        var params = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(client).create(params.capture(), anyString());
        assertEquals(10000L, params.getValue().getLineItems().get(0).getPriceData().getUnitAmount());
        assertEquals(new BigDecimal("100.00"), order.getGrandTotal());
    }

    @Test
    void checkoutOptionsExposeTheConfiguredMinimumWithoutSecrets() {
        var custom = new StripePaymentService(orders, client,
                new StripeProperties("sk_test_fixture", SECRET, "http://localhost:5173", new BigDecimal("150.00")));
        assertEquals("NPR", custom.checkoutOptions().currency());
        assertEquals(new BigDecimal("150.00"), custom.checkoutOptions().minimumTotal());
        assertFalse(custom.checkoutOptions().toString().contains(SECRET));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.001", "9223372036854775808"})
    void rejectsInvalidOrInexactAmounts(String amount) {
        order.setGrandTotal(new BigDecimal(amount));
        assertThrows(ResponseStatusException.class, () -> service.initiatePayment("ORD-123"));
        verifyNoInteractions(client);
    }

    @Test
    void reusesPendingSessionWithoutCreatingAnother() {
        pending();
        when(client.retrieve("cs_test_123")).thenReturn(session("open", "unpaid"));
        assertEquals("cs_test_123", service.initiatePayment("ORD-123").sessionId());
        verify(client, never()).create(any(), anyString());
    }

    @Test
    void rejectsPaidOrdersAndConflictingPendingMethods() {
        order.setPaymentStatus(PaymentStatus.PAID);
        assertThrows(ResponseStatusException.class, () -> service.initiatePayment("ORD-123"));
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.KHALTI);
        assertThrows(ResponseStatusException.class, () -> service.initiatePayment("ORD-123"));
        verifyNoInteractions(client);
    }

    @Test
    void requiresWebhookConfigurationBeforeCreatingCheckout() {
        var unconfigured = new StripePaymentService(orders, client,
                new StripeProperties("sk_test_fixture", "", "http://localhost:5173"));
        assertTrue(assertThrows(ResponseStatusException.class,
                () -> unconfigured.initiatePayment("ORD-123")).getReason().contains("STRIPE_WEBHOOK_SECRET"));
        verifyNoInteractions(client);
    }

    private String event(Session session, String type) {
        return "{\"id\":\"evt_fixture\",\"object\":\"event\",\"api_version\":\"" + Stripe.API_VERSION
                + "\",\"type\":\"" + type + "\",\"data\":{\"object\":"
                + ApiResource.GSON.toJson(session) + "}}";
    }

    private String sign(String body, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(
                mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    private void deliver(Session session, String type) throws Exception {
        String body = event(session, type);
        service.webhook(body, sign(body, Instant.now().getEpochSecond()));
    }

    @Test
    void verifiedPaidWebhookIsIdempotentAndCannotBeDowngradedByExpiry() throws Exception {
        pending();
        deliver(session("complete", "paid"), "checkout.session.completed");
        deliver(session("complete", "paid"), "checkout.session.completed");
        deliver(session("expired", "unpaid"), "checkout.session.expired");
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        verify(orders, times(1)).save(order);
        verifyNoInteractions(client);
    }

    @Test
    void rejectsMissingInvalidStaleAndTamperedSignatures() throws Exception {
        pending();
        String body = event(session("complete", "paid"), "checkout.session.completed");
        assertThrows(ResponseStatusException.class, () -> service.webhook(body, null));
        assertThrows(ResponseStatusException.class, () -> service.webhook(body, "t=1,v1=bad"));
        assertThrows(ResponseStatusException.class, () -> service.webhook(body, sign(body, 1)));
        assertThrows(ResponseStatusException.class, () -> service.webhook(body + " ", sign(body, Instant.now().getEpochSecond())));
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verify(orders, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "currency", "reference", "metadata", "session", "live", "mode"})
    void rejectsVerifiedButMismatchedSession(String mismatch) throws Exception {
        pending();
        Session s = session("complete", "paid");
        switch (mismatch) {
            case "amount" -> s.setAmountTotal(1L);
            case "currency" -> s.setCurrency("usd");
            case "reference" -> s.setClientReferenceId("ORD-other");
            case "metadata" -> s.setMetadata(Map.of("orderId", "ORD-other"));
            case "session" -> s.setId("cs_test_other");
            case "live" -> s.setLivemode(true);
            case "mode" -> s.setMode("subscription");
        }
        assertThrows(ResponseStatusException.class, () -> deliver(s, "checkout.session.completed"));
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verify(orders, never()).save(any());
    }

    @Test
    void completedButUnpaidWebhookStaysPending() throws Exception {
        pending();
        deliver(session("complete", "unpaid"), "checkout.session.completed");
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    void signedExpiryAndAsyncFailureAreFinalFailures() throws Exception {
        pending();
        deliver(session("expired", "unpaid"), "checkout.session.expired");
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        pending();
        deliver(session("complete", "unpaid"), "checkout.session.async_payment_failed");
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }

    @Test
    void refreshOnBrowserCancellationStaysPendingWhileProviderSessionIsOpen() {
        pending();
        when(client.retrieve("cs_test_123")).thenReturn(session("open", "unpaid"));
        assertEquals(PaymentStatus.PENDING, service.refreshPaymentStatus("ORD-123").paymentStatus());
        verify(orders, never()).save(any());
    }

    @Test
    void refreshRecognizesExpiryAndConfirmedPayment() {
        pending();
        when(client.retrieve("cs_test_123")).thenReturn(session("expired", "unpaid"));
        assertEquals(PaymentStatus.FAILED, service.refreshPaymentStatus("ORD-123").paymentStatus());
        when(client.retrieve("cs_test_123")).thenReturn(session("complete", "paid"));
        assertEquals(PaymentStatus.PAID, service.refreshPaymentStatus("ORD-123").paymentStatus());
    }

    @Test
    void refreshRejectsWrongAmountAndNonStripeOrders() {
        pending();
        Session wrong = session("complete", "paid");
        wrong.setAmountTotal(10L);
        when(client.retrieve("cs_test_123")).thenReturn(wrong);
        assertThrows(ResponseStatusException.class, () -> service.refreshPaymentStatus("ORD-123"));
        order.setPaymentMethod(PaymentMethod.KHALTI);
        assertThrows(ResponseStatusException.class, () -> service.refreshPaymentStatus("ORD-123"));
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    void terminalSessionCannotBeReplaced() {
        pending();
        order.setPaymentStatus(PaymentStatus.FAILED);
        assertThrows(ResponseStatusException.class, () -> service.initiatePayment("ORD-123"));
        verifyNoInteractions(client);
    }
}
