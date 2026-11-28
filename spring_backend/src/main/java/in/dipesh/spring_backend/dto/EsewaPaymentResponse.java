package in.dipesh.spring_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsewaPaymentResponse {
    private String paymentUrl;
    private EsewaPaymentForm form;
}
