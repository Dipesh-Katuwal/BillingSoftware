package in.dipesh.spring_backend.controller;

import in.dipesh.spring_backend.config.EsewaProperties;
import in.dipesh.spring_backend.dto.EsewaPaymentResponse;
import in.dipesh.spring_backend.dto.PaymentResult;
import in.dipesh.spring_backend.service.EsewaPaymentService;
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
public class EsewaPaymentController {
    private final EsewaPaymentService esewaPaymentService;
    private final EsewaProperties esewaProperties;

    @PostMapping("/payments/esewa/initiate/{orderId}")
    public ResponseEntity<EsewaPaymentResponse> initiatePayment(
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(esewaPaymentService.initiatePayment(orderId));
    }

    @GetMapping("/payments/esewa/success")
    public ResponseEntity<Void> paymentSuccess(@RequestParam String data) {
        return redirectToResult(esewaPaymentService.verifyPayment(data));
    }

    @GetMapping("/payments/esewa/failure")
    public ResponseEntity<Void> paymentFailure(
            @RequestParam("transaction_uuid") String orderId
    ) {
        return redirectToResult(esewaPaymentService.refreshPaymentStatus(orderId));
    }

    @GetMapping("/payments/esewa/status/{orderId}")
    public ResponseEntity<PaymentResult> refreshPaymentStatus(
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(esewaPaymentService.refreshPaymentStatus(orderId));
    }

    private ResponseEntity<Void> redirectToResult(PaymentResult result) {
        URI location = UriComponentsBuilder
                .fromUriString(esewaProperties.frontendUrl())
                .path("/payment-result")
                .queryParam("orderId", result.orderId())
                .queryParam("status", result.paymentStatus())
                .queryParam("message", result.message())
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(location)
                .build();
    }
}
