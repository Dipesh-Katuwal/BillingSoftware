package in.dipesh.spring_backend.service;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

public interface StripeCheckoutClient {
    Session create(SessionCreateParams params, String idempotencyKey);
    Session retrieve(String sessionId);
}
