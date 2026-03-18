package il.raanana.nassialart.model;

import java.time.Instant;
import java.util.List;

public record AlertHistorySnapshot(
        String city,
        int totalAlerts,
        int totalListItems,
        Instant lastUpdatedAt,
        List<HourlyAlertCount> hourlyDistribution,
        List<HistoryAlertItem> recentAlerts
) {
}
