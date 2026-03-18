package il.raanana.nassialart.model;

import java.time.Instant;

public record HistoryAlertItem(
        long id,
        String location,
        String title,
        int category,
        Instant alertDate
) {
}
