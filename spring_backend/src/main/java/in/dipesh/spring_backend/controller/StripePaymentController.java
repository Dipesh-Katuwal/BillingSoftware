package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.dto.PaymentResult;
import in.dipesh.spring_backend.dto.StripePaymentResponse;
import in.dipesh.spring_backend.dto.StripeCheckoutOptions;
import in.dipesh.spring_backend.service.Impl.StripePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/stripe")
@RequiredArgsConstructor
public class StripePaymentController {
    private final StripePaymentService service;

    @GetMapping("/options")
    public StripeCheckoutOptions options() {
        return service.checkoutOptions();
    }

    @PostMapping("/initiate/{orderId}")
    public StripePaymentResponse initiate(@PathVariable String orderId) {
        return service.initiatePayment(orderId);
    }

    @GetMapping("/status/{orderId}")
    public PaymentResult status(@PathVariable String orderId) {
        return service.refreshPaymentStatus(orderId);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        service.webhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
