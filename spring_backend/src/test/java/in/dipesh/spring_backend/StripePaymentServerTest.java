package in.dipesh.spring_backend;

import in.dipesh.spring_backend.Utils.JwtUtils;
import in.dipesh.spring_backend.entity.*;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.Impl.AppUserDetailsService;
import in.dipesh.spring_backend.service.StripeCheckoutClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

// Use a real servlet container: MockMvc does not reproduce ERROR redispatches.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"stripe.webhook-secret=whsec_server_fixture", "server.error.include-message=never"})
@ActiveProfiles("test")
class StripePaymentServerTest {
    @Value("${local.server.port}") int port;
    @Autowired JwtUtils jwt;
    @Autowired OrderRepository orders;
    @MockitoBean AppUserDetailsService users;
    @MockitoBean StripeCheckoutClient stripe;
    private final HttpClient http = HttpClient.newHttpClient();

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    private HttpRequest.Builder authenticated(String path) {
        var user = User.withUsername("stripe-server-fixture@example.invalid")
                .password("unused-fixture").roles("USER").build();
        when(users.loadUserByUsername(user.getUsername())).thenReturn(user);
        return request(path).header("Authorization", "Bearer " + jwt.generateToken(user));
    }

    @Test
    void missingAuthenticationReturns401() throws Exception {
        var response = http.send(request("/payments/stripe/status/ORD-absent").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        verifyNoInteractions(stripe);
    }

    @Test
    void freshJwtReachesStripeService() throws Exception {
        var response = http.send(authenticated("/payments/stripe/status/ORD-absent").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void checkoutErrorIsNotReplacedByAnAuthentication403DuringErrorDispatch() throws Exception {
        var order = orders.saveAndFlush(OrderEntity.builder().grandTotal(new BigDecimal("372.30"))
                .paymentMethod(PaymentMethod.STRIPE).paymentStatus(PaymentStatus.PENDING)
                .stripeSessionId("cs_test_server_error").build());
        when(stripe.retrieve("cs_test_server_error")).thenThrow(new IllegalStateException("Provider test fixture failure"));
        var response = http.send(authenticated("/payments/stripe/status/" + order.getOrderId()).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(500, response.statusCode());
        verify(stripe).retrieve("cs_test_server_error");
    }
}
