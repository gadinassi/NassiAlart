package il.raanana.nassialart.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import il.raanana.nassialart.model.OrefHistoryRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OrefHistoryClient {

    private static final String HISTORY_URL = "https://alerts-history.oref.org.il/Shared/Ajax/GetAlarmsHistory.aspx?lang=he&mode=1";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OrefHistoryClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public List<OrefHistoryRecord> fetchHistory() {
        byte[] body = restClient.get()
                .uri(HISTORY_URL)
                .header(HttpHeaders.REFERER, "https://www.oref.org.il/")
                .header("X-Requested-With", "XMLHttpRequest")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN)
                .retrieve()
                .body(byte[].class);

        if (body == null) {
            return List.of();
        }

        String normalizedBody = new String(body, StandardCharsets.UTF_8)
                .replace("\uFEFF", "")
                .replace("\u0000", "")
                .trim();

        if (normalizedBody.isEmpty()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(normalizedBody, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
