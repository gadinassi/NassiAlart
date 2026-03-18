package il.raanana.nassialart.model;

public record HourlyAlertCount(
        int hour,
        String label,
        long count
) {
}
