package in.dipesh.spring_backend.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.dipesh.spring_backend.config.KhaltiProperties;
import in.dipesh.spring_backend.dto.KhaltiInitiateApiResponse;
import in.dipesh.spring_backend.dto.KhaltiInitiateRequest;
import in.dipesh.spring_backend.dto.KhaltiLookupRequest;
import in.dipesh.spring_backend.dto.KhaltiLookupResponse;
import in.dipesh.spring_backend.service.KhaltiApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class KhaltiApiClientImpl implements KhaltiApiClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final KhaltiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public KhaltiInitiateApiResponse initiate(KhaltiInitiateRequest request) {
        return post(
                "/epayment/initiate/",
                request,
                KhaltiInitiateApiResponse.class,
                false);
    }

    @Override
    public KhaltiLookupResponse lookup(String pidx) {
        return post(
                "/epayment/lookup/",
                new KhaltiLookupRequest(pidx),
                KhaltiLookupResponse.class,
                true);
    }

    private <T> T post(
            String path,
            Object body,
            Class<T> responseType,
            boolean allowBadRequestResponse
    ) {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Khalti sandbox secret key is not configured");
        }

        String baseUrl = properties.baseUrl().replaceAll("/+$", "");
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "key " + properties.secretKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body)))
                    .build();
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to prepare Khalti request",
                    exception);
        }

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());
            boolean successful = response.statusCode() >= 200
                    && response.statusCode() < 300;
            boolean documentedLookupStatus = allowBadRequestResponse
                    && response.statusCode() == 400;
            if (!successful && !documentedLookupStatus) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Khalti could not process the payment request");
            }
            return objectMapper.readValue(response.body(), responseType);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Khalti request was interrupted",
                    exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to read Khalti response",
                    exception);
        }
    }
}
