package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.config.KhaltiProperties;
import in.dipesh.spring_backend.dto.KhaltiPaymentResponse;
import in.dipesh.spring_backend.dto.PaymentResult;
import in.dipesh.spring_backend.service.KhaltiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class KhaltiPaymentController {
    private final KhaltiPaymentService khaltiPaymentService;
    private final KhaltiProperties khaltiProperties;

    @PostMapping("/payments/khalti/initiate/{orderId}")
    public ResponseEntity<KhaltiPaymentResponse> initiatePayment(
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(khaltiPaymentService.initiatePayment(orderId));
    }

    @GetMapping("/payments/khalti/callback")
    public ResponseEntity<Void> paymentCallback(
            @RequestParam String pidx,
            @RequestParam(name = "purchase_order_id", required = false)
            String purchaseOrderId
    ) {
        return redirectToResult(
                khaltiPaymentService.verifyCallback(pidx, purchaseOrderId));
    }

    @GetMapping("/payments/khalti/status/{orderId}")
    public ResponseEntity<PaymentResult> refreshPaymentStatus(
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(
                khaltiPaymentService.refreshPaymentStatus(orderId));
    }

    private ResponseEntity<Void> redirectToResult(PaymentResult result) {
        URI location = UriComponentsBuilder
                .fromUriString(khaltiProperties.frontendUrl())
                .path("/payment-result")
                .queryParam("orderId", result.orderId())
                .queryParam("status", result.paymentStatus())
                .queryParam("method", "KHALTI")
                .queryParam("message", result.message())
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(location)
                .build();
    }
}
