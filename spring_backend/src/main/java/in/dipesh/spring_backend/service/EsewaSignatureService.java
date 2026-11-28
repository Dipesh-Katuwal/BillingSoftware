package in.dipesh.spring_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import in.dipesh.spring_backend.config.EsewaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EsewaSignatureService {
    public static final String SIGNED_FIELD_NAMES =
            "total_amount,transaction_uuid,product_code";
    private static final List<String> RESPONSE_SIGNED_FIELDS = List.of(
            "transaction_code",
            "status",
            "total_amount",
            "transaction_uuid",
            "product_code",
            "signed_field_names");

    private final EsewaProperties esewaProperties;

    public String createSignature(
            BigDecimal totalAmount,
            String transactionUuid
    ) {
        String signedPayload = "total_amount=" + formatAmount(totalAmount)
                + ",transaction_uuid=" + transactionUuid
                + ",product_code=" + esewaProperties.productCode();

        return hmacSha256(signedPayload, requiredSecretKey());
    }

    public String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public boolean isValidResponseSignature(JsonNode response) {
        String signedFieldNames = response.path("signed_field_names").asText();
        String suppliedSignature = response.path("signature").asText();
        List<String> signedFields = Arrays.asList(signedFieldNames.split(",", -1));

        if (!signedFields.equals(RESPONSE_SIGNED_FIELDS) || suppliedSignature.isBlank()) {
            return false;
        }

        StringBuilder payload = new StringBuilder();
        for (String field : signedFields) {
            JsonNode value = response.get(field);
            if (value == null || value.isNull()) {
                return false;
            }
            if (!payload.isEmpty()) {
                payload.append(',');
            }
            payload.append(field).append('=').append(value.asText());
        }

        String expectedSignature = hmacSha256(payload.toString(), requiredSecretKey());
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                suppliedSignature.getBytes(StandardCharsets.US_ASCII));
    }

    private String requiredSecretKey() {
        String secretKey = esewaProperties.secretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "ESEWA_TEST_SECRET_KEY is not configured");
        }
        return secretKey;
    }

    private String hmacSha256(String payload, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key =
                    new SecretKeySpec(
                            secretKey.getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256");
            mac.init(key);
            byte[] result = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to create eSewa signature", exception);
        }
    }
}
