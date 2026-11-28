package in.dipesh.spring_backend;

import com.stripe.model.checkout.Session;
import in.dipesh.spring_backend.entity.*;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.StripeCheckoutClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(properties = "stripe.webhook-secret=whsec_http_fixture")
@ActiveProfiles("test")
class StripePaymentHttpTest {
    MockMvc mvc;
    @Autowired WebApplicationContext context;
    @Autowired OrderRepository orders;
    @MockitoBean StripeCheckoutClient client;

    @BeforeEach
    void setupSecurity() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void providerCallbackIsPublicButSignatureIsRequired() throws Exception {
        mvc.perform(post("/payments/stripe/webhook").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }

    @Test
    void initiationAndRefreshRequireAuthentication() throws Exception {
        mvc.perform(get("/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/orders/paid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/orders/ORD-any/receipt")).andExpect(status().isUnauthorized());
        mvc.perform(get("/payments/stripe/options")).andExpect(status().isUnauthorized());
        mvc.perform(post("/payments/stripe/initiate/ORD-any")).andExpect(status().is4xxClientError());
        mvc.perform(get("/payments/stripe/status/ORD-any")).andExpect(status().is4xxClientError());
        verifyNoInteractions(client);
    }

    @Test
    @WithMockUser(username = "cashier@example.invalid", roles = "USER")
    void dashboardAndEmailAreAvailableToAnAuthenticatedUser() throws Exception {
        mvc.perform(get("/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cashier@example.invalid"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("NPR"))
                .andExpect(jsonPath("$.date").value(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kathmandu")).toString()))
                .andExpect(jsonPath("$.paidOrders").value((int) orders.countByPaymentStatus(PaymentStatus.PAID)))
                .andExpect(jsonPath("$.pendingPayments").value((int) orders.countByPaymentStatus(PaymentStatus.PENDING)))
                .andExpect(jsonPath("$.totalSales").isNumber())
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.recentOrders[?(@.paymentStatus != 'PAID')]").isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    @WithMockUser(roles = "USER")
    void historyReturnsOnlyPaidOrdersNewestFirstAcrossPaymentMethods() throws Exception {
        var date = java.time.LocalDateTime.of(2099, 1, 1, 12, 0);
        var cash = historyOrder("ORD-history-cash", PaymentStatus.PAID, PaymentMethod.CASH, date);
        var stripe = historyOrder("ORD-history-stripe", PaymentStatus.PAID, PaymentMethod.STRIPE, date.plusDays(1));
        historyOrder("ORD-history-unpaid", PaymentStatus.UNPAID, null, date.plusDays(2));
        historyOrder("ORD-history-pending", PaymentStatus.PENDING, PaymentMethod.STRIPE, date.plusDays(3));
        historyOrder("ORD-history-failed", PaymentStatus.FAILED, PaymentMethod.KHALTI, date.plusDays(4));
        mvc.perform(get("/orders/paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(stripe.getOrderId()))
                .andExpect(jsonPath("$[1].orderId").value(cash.getOrderId()))
                .andExpect(jsonPath("$[0].currency").value("NPR"))
                .andExpect(jsonPath("$[0].grandTotal").value(117.30))
                .andExpect(jsonPath("$[0].createdAt").isString())
                .andExpect(jsonPath("$[?(@.paymentStatus != 'PAID')]").isEmpty())
                .andExpect(jsonPath("$[0].stripeSessionId").doesNotExist());
        verifyNoInteractions(client);
    }

    private OrderEntity historyOrder(String orderId, PaymentStatus status, PaymentMethod method,
            java.time.LocalDateTime createdAt) {
        var order = orders.saveAndFlush(OrderEntity.builder().customerName("History customer")
                .phoneNumber("9800000000").grandTotal(new BigDecimal("117.30"))
                .paymentMethod(method).paymentStatus(status).build());
        order.setOrderId(orderId);
        order.setCreatedAt(createdAt);
        return orders.saveAndFlush(order);
    }

    @Test
    @WithMockUser(roles = "USER")
    void receiptEndpointReadsSavedOrderAndItsPersistedItems() throws Exception {
        var order = orders.saveAndFlush(OrderEntity.builder().customerName("Receipt customer")
                .phoneNumber("9800000000").grandTotal(new BigDecimal("117.30"))
                .subTotal(new BigDecimal("115.00")).tax(new BigDecimal("2.30"))
                .paymentMethod(PaymentMethod.STRIPE).paymentStatus(PaymentStatus.PAID)
                .stripeSessionId("cs_test_receipt_http")
                .items(java.util.List.of(OrderItemEntity.builder().itemId("tea").name("Tea")
                        .price(new BigDecimal("23.00")).quantity(5).build())).build());
        long countBeforeReceipt = orders.count();
        mvc.perform(get("/orders/" + order.getOrderId() + "/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getOrderId()))
                .andExpect(jsonPath("$.customerName").value("Receipt customer"))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.grandTotal").value(117.30))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.items[0].lineTotal").value(115.00));
        org.junit.jupiter.api.Assertions.assertEquals(countBeforeReceipt, orders.count());
        verifyNoInteractions(client);
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedCheckoutOptionsReturnMinimumAndCurrencyOnly() throws Exception {
        mvc.perform(get("/payments/stripe/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("NPR"))
                .andExpect(jsonPath("$.minimumTotal").value(100))
                .andExpect(jsonPath("$.testSecretKey").doesNotExist())
                .andExpect(jsonPath("$.webhookSecret").doesNotExist());
        verifyNoInteractions(client);
    }

    @Test
    @WithMockUser(roles = "USER")
    void authenticatedRefreshUsesProviderAndIgnoresBrowserSuccessParameters() throws Exception {
        OrderEntity order = orders.saveAndFlush(OrderEntity.builder().grandTotal(new BigDecimal("372.30"))
                .paymentMethod(PaymentMethod.STRIPE).paymentStatus(PaymentStatus.PENDING)
                .stripeSessionId("cs_test_http").build());
        Session session = new Session();
        session.setId("cs_test_http");
        session.setMode("payment");
        session.setLivemode(false);
        session.setClientReferenceId(order.getOrderId());
        session.setMetadata(Map.of("orderId", order.getOrderId()));
        session.setCurrency("npr");
        session.setAmountTotal(37230L);
        session.setStatus("open");
        session.setPaymentStatus("unpaid");
        when(client.retrieve("cs_test_http")).thenReturn(session);
        mvc.perform(get("/payments/stripe/status/" + order.getOrderId()).param("status", "PAID").param("canceled", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        verify(client).retrieve("cs_test_http");
    }
}
