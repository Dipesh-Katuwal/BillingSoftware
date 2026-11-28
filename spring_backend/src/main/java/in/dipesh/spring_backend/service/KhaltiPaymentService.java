package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.KhaltiPaymentResponse;
import in.dipesh.spring_backend.dto.PaymentResult;

public interface KhaltiPaymentService {
    KhaltiPaymentResponse initiatePayment(String orderId);
    PaymentResult verifyCallback(String pidx, String purchaseOrderId);
    PaymentResult refreshPaymentStatus(String orderId);
}
