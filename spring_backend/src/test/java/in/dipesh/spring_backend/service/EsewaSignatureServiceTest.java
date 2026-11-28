package in.dipesh.spring_backend.service;

import in.dipesh.spring_backend.config.EsewaProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EsewaSignatureServiceTest {

    @Test
    void createsBase64HmacSha256Signature() {
        EsewaProperties properties = new EsewaProperties(
                "https://sandbox.example",
                "https://status.example",
                "EPAYTEST",
                "test-secret",
                "http://localhost:8080/success",
                "http://localhost:8080/failure",
                "http://localhost:5173");
        EsewaSignatureService service = new EsewaSignatureService(properties);

        String signature = service.createSignature(
                new BigDecimal("448.80"),
                "ORD1795837039284");

        assertEquals(
                "XgwbJy/DFkuO0H6Kxrak+Rumhm8XqT32Vs6UAZ84r9k=",
                signature);
    }

    @Test
    void formatsAmountWithTwoDecimalPlaces() {
        EsewaProperties properties = new EsewaProperties(
                "https://sandbox.example",
                "https://status.example",
                "EPAYTEST",
                "test-secret",
                "http://localhost:8080/success",
                "http://localhost:8080/failure",
                "http://localhost:5173");
        EsewaSignatureService service = new EsewaSignatureService(properties);

        assertEquals("448.80", service.formatAmount(new BigDecimal("448.8")));
    }

    @Test
    void rejectsMissingSecretKeyInsteadOfGeneratingInvalidSignature() {
        EsewaProperties properties = new EsewaProperties(
                "https://sandbox.example",
                "https://status.example",
                "EPAYTEST",
                "",
                "http://localhost:8080/success",
                "http://localhost:8080/failure",
                "http://localhost:5173");
        EsewaSignatureService service = new EsewaSignatureService(properties);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.createSignature(
                        new BigDecimal("448.80"),
                        "ORD1795837039284"));

        assertEquals("ESEWA_TEST_SECRET_KEY is not configured", exception.getMessage());
    }
}
