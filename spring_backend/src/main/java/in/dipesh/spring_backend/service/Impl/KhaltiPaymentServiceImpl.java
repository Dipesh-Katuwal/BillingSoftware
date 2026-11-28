package in.dipesh.spring_backend.service.Impl;

import in.dipesh.spring_backend.config.KhaltiProperties;
import in.dipesh.spring_backend.dto.KhaltiInitiateApiResponse;
import in.dipesh.spring_backend.dto.KhaltiInitiateRequest;
import in.dipesh.spring_backend.dto.KhaltiLookupResponse;
import in.dipesh.spring_backend.dto.KhaltiPaymentResponse;
import in.dipesh.spring_backend.dto.PaymentResult;
import in.dipesh.spring_backend.entity.OrderEntity;
import in.dipesh.spring_backend.entity.PaymentMethod;
import in.dipesh.spring_backend.entity.PaymentStatus;
import in.dipesh.spring_backend.repository.OrderRepository;
import in.dipesh.spring_backend.service.KhaltiApiClient;
import in.dipesh.spring_backend.service.KhaltiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KhaltiPaymentServiceImpl implements KhaltiPaymentService {
    private static final BigDecimal MINIMUM_AMOUNT_PAISA = new BigDecimal("1000");
    private static final Set<String> PAYMENT_HOSTS =
            Set.of("test-pay.khalti.com", "pay.khalti.com");

    private final OrderRepository orderRepository;
    private final KhaltiProperties properties;
    private final KhaltiApiClient khaltiApiClient;

    @Override
    @Transactional
    public KhaltiPaymentResponse initiatePayment(String orderId) {
        OrderEntity order = findOrder(orderId);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid");
        }

        if (order.getPaymentStatus() == PaymentStatus.PENDING) {
            if (order.getPaymentMethod() == PaymentMethod.KHALTI
                    && order.getKhaltiPidx() != null
                    && order.getKhaltiPaymentUrl() != null) {
                return new KhaltiPaymentResponse(
                        order.getKhaltiPidx(),
                        order.getKhaltiPaymentUrl());
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another payment for this order is already pending");
        }

        long amountPaisa;
        try {
            amountPaisa = order.getGrandTotal()
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order total must be an exact amount in paisa",
                    exception);
        }
        if (BigDecimal.valueOf(amountPaisa).compareTo(MINIMUM_AMOUNT_PAISA) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khalti sandbox requires a payment of at least NPR 10");
        }

        KhaltiInitiateRequest request = new KhaltiInitiateRequest(
                properties.returnUrl(),
                properties.websiteUrl(),
                amountPaisa,
                order.getOrderId(),
                "Billing order " + order.getOrderId());
        KhaltiInitiateApiResponse response = khaltiApiClient.initiate(request);
        if (response == null
                || response.pidx() == null
                || response.pidx().isBlank()
                || !isAllowedPaymentUrl(response.paymentUrl())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Khalti returned an invalid payment session");
        }

        order.setPaymentMethod(PaymentMethod.KHALTI);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setKhaltiPidx(response.pidx());
        order.setKhaltiPaymentUrl(response.paymentUrl());
        orderRepository.save(order);

        return new KhaltiPaymentResponse(response.pidx(), response.paymentUrl());
    }

    @Override
    @Transactional
    public PaymentResult verifyCallback(String pidx, String purchaseOrderId) {
        if (pidx == null || pidx.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khalti callback is missing pidx");
        }
        OrderEntity order = orderRepository.findByKhaltiPidx(pidx)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No order matches this Khalti payment"));
        if (purchaseOrderId != null
                && !purchaseOrderId.isBlank()
                && !order.getOrderId().equals(purchaseOrderId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khalti order reference does not match");
        }
        return refreshOrderStatus(order);
    }

    @Override
    @Transactional
    public PaymentResult refreshPaymentStatus(String orderId) {
        OrderEntity order = findOrder(orderId);
        if (order.getPaymentMethod() != PaymentMethod.KHALTI
                || order.getKhaltiPidx() == null
                || order.getKhaltiPidx().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has no Khalti payment to check");
        }
        return refreshOrderStatus(order);
    }

    private PaymentResult refreshOrderStatus(OrderEntity order) {
        if (order.getPaymentMethod() != PaymentMethod.KHALTI
                || order.getKhaltiPidx() == null
                || order.getKhaltiPidx().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order does not have an active Khalti payment");
        }

        KhaltiLookupResponse verification;
        try {
            verification = khaltiApiClient.lookup(order.getKhaltiPidx());
        } catch (ResponseStatusException exception) {
            return new PaymentResult(
                    order.getOrderId(),
                    order.getPaymentStatus(),
                    "Payment status could not be verified yet. Please check again.");
        }

        long expectedAmountPaisa = order.getGrandTotal()
                .movePointRight(2)
                .longValueExact();
        if (verification == null
                || !order.getKhaltiPidx().equals(verification.pidx())
                || verification.totalAmount() == null
                || verification.totalAmount() != expectedAmountPaisa) {
            return new PaymentResult(
                    order.getOrderId(),
                    order.getPaymentStatus(),
                    "Payment status could not be verified yet. Please check again.");
        }

        String providerStatus = verification.status();
        PaymentStatus nextStatus = order.getPaymentStatus();
        if ("Completed".equalsIgnoreCase(providerStatus)
                && !Boolean.TRUE.equals(verification.refunded())) {
            nextStatus = PaymentStatus.PAID;
        } else if (isFinalFailure(providerStatus)
                && nextStatus != PaymentStatus.PAID) {
            nextStatus = PaymentStatus.FAILED;
        }

        if (nextStatus != order.getPaymentStatus()) {
            order.setPaymentStatus(nextStatus);
            orderRepository.save(order);
        }

        String message;
        if (Boolean.TRUE.equals(verification.refunded())
                || "Refunded".equalsIgnoreCase(providerStatus)
                || "Partially Refunded".equalsIgnoreCase(providerStatus)) {
            message = "Khalti reports a refunded transaction. Contact support before proceeding.";
        } else {
            message = switch (nextStatus) {
                case PAID -> "Payment confirmed.";
                case FAILED -> "Payment was not completed.";
                case PENDING -> "Payment is still pending.";
                case UNPAID -> "Payment has not been initiated.";
            };
        }
        return new PaymentResult(order.getOrderId(), nextStatus, message);
    }

    private boolean isFinalFailure(String status) {
        return "User canceled".equalsIgnoreCase(status)
                || "Canceled".equalsIgnoreCase(status)
                || "Failed".equalsIgnoreCase(status)
                || "Expired".equalsIgnoreCase(status);
    }

    private boolean isAllowedPaymentUrl(String paymentUrl) {
        if (paymentUrl == null || paymentUrl.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(paymentUrl);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && PAYMENT_HOSTS.contains(uri.getHost().toLowerCase());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private OrderEntity findOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));
    }
}
