package in.dipesh.spring_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsewaPaymentForm {
    private BigDecimal amount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal productServiceCharge;
    private BigDecimal productDeliveryCharge;
    private String transactionUuid;
    private String productCode;
    private String signedFieldNames;
    private String signature;
    private String successUrl;
    private String failureUrl;
}
