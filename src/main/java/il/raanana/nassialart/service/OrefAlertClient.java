package il.raanana.nassialart.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import il.raanana.nassialart.config.AlertProperties;
import il.raanana.nassialart.model.OrefAlertResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrefAlertClient {

    private final RestClient restClient;
    private final AlertProperties properties;
    private final ObjectMapper objectMapper;

    public OrefAlertClient(RestClient restClient, AlertProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public OrefAlertResponse fetchCurrentAlert() {
        String body = restClient.get()
                .uri(properties.getOrefUrl())
                .header(HttpHeaders.REFERER, properties.getReferer())
                .header("X-Requested-With", "XMLHttpRequest")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .retrieve()
                .body(String.class);

        if (body == null) {
            return null;
        }

        String normalizedBody = body
                .replace("\uFEFF", "")
                .trim();

        if (normalizedBody.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(normalizedBody, OrefAlertResponse.class);
        } catch (Exception exception) {
            return null;
        }
    }
}
