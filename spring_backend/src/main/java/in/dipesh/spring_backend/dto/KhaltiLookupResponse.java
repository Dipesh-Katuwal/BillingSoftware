package in.dipesh.spring_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KhaltiLookupResponse(
        String pidx,
        @JsonProperty("total_amount") Long totalAmount,
        String status,
        @JsonProperty("transaction_id") String transactionId,
        Boolean refunded
) {
}
