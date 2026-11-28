package in.dipesh.spring_backend.service.Impl;

import com.stripe.StripeClient;
import com.stripe.net.*;
import com.stripe.param.checkout.SessionCreateParams;
import in.dipesh.spring_backend.config.StripeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StripeCheckoutClientImplTest {
    @ParameterizedTest
    @CsvSource({
            "400,amount_too_small,Order is below the minimum charge amount,minimum charge",
            "400,invalid_request,Types must be activated but card is invalid because total must convert to at least 50 cents,minimum charge",
            "400,currency_not_supported,Unsupported currency,cannot accept NPR",
            "401,invalid_api_key,Invalid API Key provided sk_test_private_fixture,rejected the test secret key",
            "403,account_invalid,Account is not permitted,not enabled",
            "400,parameter_invalid_empty,Please activate card payments,not enabled",
            "400,parameter_unknown,Unrecognized request parameter,req_fixture123"
    })
    void exposesUsefulErrorsWithoutLeakingProviderMessages(int status, String code,
            String providerMessage, String expected) throws Exception {
        HttpClient network = mock(HttpClient.class);
        when(network.requestWithRetries(any())).thenReturn(new StripeResponse(status,
                HttpHeaders.of(Map.of("Request-Id", java.util.List.of("req_fixture123"))),
                "{\"error\":{\"type\":\"invalid_request_error\",\"code\":\"" + code
                        + "\",\"message\":\"" + providerMessage + "\"}}"));
        var sdk = StripeClient.builder().setApiKey("sk_test_fixture").setHttpClient(network).build();
        var client = spy(new StripeCheckoutClientImpl(new StripeProperties("sk_test_fixture", "whsec_fixture", "http://localhost:5173")));
        doReturn(sdk).when(client).client();
        var exception = assertThrows(ResponseStatusException.class,
                () -> client.create(SessionCreateParams.builder().build(), "fixture-order"));
        assertTrue(exception.getReason().contains(expected), exception.getReason());
        assertFalse(exception.getReason().contains("sk_test_private_fixture"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"sk_live_fixture", "invalid"})
    void rejectsMissingOrNonTestKeysWithoutNetwork(String key) {
        var client = new StripeCheckoutClientImpl(new StripeProperties(key, "whsec_fixture", "http://localhost:5173"));
        assertThrows(ResponseStatusException.class, () -> client.retrieve("cs_test_fixture"));
    }

    @Test
    void officialSdkUsesMockedNetworkForCreationAndRetrieval() throws Exception {
        HttpClient network = mock(HttpClient.class);
        when(network.requestWithRetries(any())).thenReturn(new StripeResponse(200, HttpHeaders.of(Map.of()),
                "{\"id\":\"cs_test_fixture\",\"object\":\"checkout.session\",\"currency\":\"npr\",\"amount_total\":37230}"));
        var sdk = StripeClient.builder().setApiKey("sk_test_fixture").setHttpClient(network).build();
        var client = spy(new StripeCheckoutClientImpl(new StripeProperties("sk_test_fixture", "whsec_fixture", "http://localhost:5173")));
        doReturn(sdk).when(client).client();
        var params = SessionCreateParams.builder().setClientReferenceId("ORD-123")
                .setMode(SessionCreateParams.Mode.PAYMENT).build();
        assertEquals("cs_test_fixture", client.create(params, "order-fixture").getId());
        assertEquals(37230L, client.retrieve("cs_test_fixture").getAmountTotal());
        var requests = ArgumentCaptor.forClass(StripeRequest.class);
        verify(network, times(2)).requestWithRetries(requests.capture());
        assertEquals("order-fixture", requests.getAllValues().get(0).options().getIdempotencyKey());
        assertEquals("ORD-123", requests.getAllValues().get(0).params().get("client_reference_id"));
        assertTrue(requests.getAllValues().get(1).url().getPath().endsWith("/checkout/sessions/cs_test_fixture"));
    }
}
