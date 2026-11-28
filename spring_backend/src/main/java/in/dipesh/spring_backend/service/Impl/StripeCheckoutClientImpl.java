package in.dipesh.spring_backend.service.Impl;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import in.dipesh.spring_backend.config.StripeProperties;
import in.dipesh.spring_backend.service.StripeCheckoutClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class StripeCheckoutClientImpl implements StripeCheckoutClient {
    private final StripeProperties properties;

    StripeClient client() {
        String key = properties.testSecretKey();
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe test checkout is unavailable: configure STRIPE_TEST_SECRET_KEY");
        }
        if (!key.startsWith("sk_test_")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Stripe checkout requires a test secret key (sk_test_)");
        }
        return StripeClient.builder().setApiKey(key)
                .setConnectTimeout(10000).setReadTimeout(20000).setMaxNetworkRetries(2).build();
    }

    @Override
    public Session create(SessionCreateParams params, String idempotencyKey) {
        try {
            return client().v1().checkout().sessions().create(params,
                    RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
        } catch (StripeException exception) {
            // Do not log provider responses or credentials.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    checkoutError(exception));
        }
    }

    private String checkoutError(StripeException exception) {
        var error = exception.getStripeError();
        String code = error == null || error.getCode() == null ? "" : error.getCode();
        // Inspect provider text only to select fixed messages. Never expose raw
        // authentication errors, which can contain part of an API key.
        String message = error == null || error.getMessage() == null
                ? "" : error.getMessage().toLowerCase(java.util.Locale.ROOT);
        if ("amount_too_small".equals(code) || message.contains("minimum charge amount")
                || message.contains("below the minimum") || message.contains("must convert to at least")) {
            return "The order total is below Stripe's minimum charge for NPR. Add more items and try again. The currency has not been changed.";
        }
        if ("currency_not_supported".equals(code) || message.contains("unsupported currency")) {
            return "This Stripe account cannot accept NPR. Check its supported payment currencies; the currency has not been changed.";
        }
        if (exception instanceof com.stripe.exception.PermissionException
                || message.contains("capabilit") || message.contains("activate")
                || message.contains("accept payments")) {
            return "Stripe payments are not enabled for this test account. Complete its test payment setup and enable card payments in the Stripe Dashboard.";
        }
        if (exception instanceof com.stripe.exception.AuthenticationException) {
            return "Stripe rejected the test secret key. Check STRIPE_TEST_SECRET_KEY and restart the backend.";
        }
        if (exception instanceof com.stripe.exception.ApiConnectionException) {
            return "The backend could not connect to Stripe. Check its internet connection and try again.";
        }
        String requestId = exception.getRequestId();
        return "Stripe rejected NPR checkout. Check the request in Stripe Dashboard's API logs."
                + (requestId != null && requestId.matches("req_[A-Za-z0-9]+")
                ? " Request ID: " + requestId : "");
    }

    @Override
    public Session retrieve(String sessionId) {
        try {
            return client().v1().checkout().sessions().retrieve(sessionId);
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Stripe status is temporarily unavailable. Please check again.");
        }
    }
}
