package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.EsewaPaymentResponse;
import in.dipesh.spring_backend.dto.PaymentResult;

public interface EsewaPaymentService {
    EsewaPaymentResponse initiatePayment(String orderId);
    PaymentResult verifyPayment(String encodedData);
    PaymentResult refreshPaymentStatus(String orderId);
}
