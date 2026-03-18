package il.raanana.nassialart.model;

import java.time.Instant;
import java.util.List;

public record AlertSnapshot(
        boolean active,
        boolean demoMode,
        String city,
        String trafficLightState,
        String trafficLightLabel,
        String title,
        String description,
        String instruction,
        List<String> matchedLocations,
        Instant lastAlertActivatedAt,
        Instant alertReceivedAt,
        Instant activeUntil,
        Instant lastSuccessfulPollAt,
        String sourceStatus,
        String sourceMessage
) {
}
