package in.dipesh.spring_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KhaltiInitiateRequest(
        @JsonProperty("return_url") String returnUrl,
        @JsonProperty("website_url") String websiteUrl,
        long amount,
        @JsonProperty("purchase_order_id") String purchaseOrderId,
        @JsonProperty("purchase_order_name") String purchaseOrderName
) {
}
