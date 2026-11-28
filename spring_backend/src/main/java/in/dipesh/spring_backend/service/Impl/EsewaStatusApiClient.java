package in.dipesh.spring_backend.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.dipesh.spring_backend.config.EsewaProperties;
import in.dipesh.spring_backend.dto.EsewaStatusResponse;
import in.dipesh.spring_backend.service.EsewaSignatureService;
import in.dipesh.spring_backend.service.EsewaStatusClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class EsewaStatusApiClient implements EsewaStatusClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final EsewaProperties esewaProperties;
    private final EsewaSignatureService esewaSignatureService;
    private final ObjectMapper objectMapper;

    @Override
    public EsewaStatusResponse verify(
            String transactionUuid,
            String productCode,
            BigDecimal totalAmount
    ) {
        String verificationUrl = esewaProperties.statusUrl()
                + "/api/epay/transaction/status/?"
                + "product_code=" + encode(productCode)
                + "&total_amount=" + encode(
                        esewaSignatureService.formatAmount(totalAmount))
                + "&transaction_uuid=" + encode(transactionUuid);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(verificationUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "eSewa could not verify this transaction");
            }
            return objectMapper.readValue(response.body(), EsewaStatusResponse.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "eSewa transaction verification was interrupted",
                    exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to read eSewa verification response",
                    exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
