package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.dto.EsewaStatusResponse;

import java.math.BigDecimal;

public interface EsewaStatusClient {
    EsewaStatusResponse verify(
            String transactionUuid,
            String productCode,
            BigDecimal totalAmount
    );
}
