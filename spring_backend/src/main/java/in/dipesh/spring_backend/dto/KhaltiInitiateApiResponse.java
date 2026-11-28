package in.dipesh.spring_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KhaltiInitiateApiResponse(
        String pidx,
        @JsonProperty("payment_url") String paymentUrl
) {
}
