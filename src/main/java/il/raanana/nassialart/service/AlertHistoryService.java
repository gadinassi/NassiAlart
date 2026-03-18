package il.raanana.nassialart.service;

import il.raanana.nassialart.config.AlertProperties;
import il.raanana.nassialart.model.AlertHistorySnapshot;
import il.raanana.nassialart.model.HistoryAlertItem;
import il.raanana.nassialart.model.HourlyAlertCount;
import il.raanana.nassialart.model.OrefHistoryRecord;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

@Service
public class AlertHistoryService {

    private static final DateTimeFormatter HISTORY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final ZoneId ISRAEL_ZONE = ZoneId.of("Asia/Jerusalem");

    private final OrefHistoryClient orefHistoryClient;
    private final AlertProperties properties;
    private final AtomicReference<CachedHistory> cache = new AtomicReference<>(CachedHistory.empty());

    public AlertHistoryService(OrefHistoryClient orefHistoryClient, AlertProperties properties) {
        this.orefHistoryClient = orefHistoryClient;
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        refresh();
    }

    @Scheduled(fixedDelay = 300000)
    public void refresh() {
        List<HistoryAlertItem> historyItems = orefHistoryClient.fetchHistory().stream()
                .filter(Objects::nonNull)
                .filter(this::matchesCity)
                .map(this::toHistoryItem)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(HistoryAlertItem::alertDate).reversed())
                .toList();

        List<HistoryAlertItem> alertOnlyItems = historyItems.stream()
                .filter(item -> item.category() != 13)
                .toList();

        Map<Integer, Long> groupedByHour = alertOnlyItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> LocalDateTime.ofInstant(item.alertDate(), ISRAEL_ZONE).getHour(),
                        java.util.stream.Collectors.counting()
                ));

        List<HourlyAlertCount> hourlyCounts = IntStream.range(0, 24)
                .mapToObj(hour -> new HourlyAlertCount(hour, String.format(Locale.ROOT, "%02d:00", hour), groupedByHour.getOrDefault(hour, 0L)))
                .toList();

        List<HistoryAlertItem> recentAlerts = historyItems.stream()
                .limit(25)
                .toList();

        cache.set(new CachedHistory(new AlertHistorySnapshot(
                properties.getCity(),
                alertOnlyItems.size(),
                historyItems.size(),
                Instant.now(),
                hourlyCounts,
                recentAlerts
        )));
    }

    public AlertHistorySnapshot getHistorySnapshot() {
        CachedHistory cachedHistory = cache.get();
        if (cachedHistory.lastUpdatedAt().plusSeconds(300).isBefore(Instant.now())) {
            refresh();
            cachedHistory = cache.get();
        }
        return cachedHistory.snapshot();
    }

    private boolean matchesCity(OrefHistoryRecord record) {
        return record.getData() != null
                && properties.getCityAliases().stream().anyMatch(alias -> record.getData().contains(alias));
    }

    private HistoryAlertItem toHistoryItem(OrefHistoryRecord record) {
        try {
            Instant alertDate = LocalDateTime.parse(record.getAlertDate(), HISTORY_DATE_FORMAT)
                    .atZone(ISRAEL_ZONE)
                    .toInstant();
            return new HistoryAlertItem(
                    record.getRecordId(),
                    record.getData(),
                    record.getCategoryDescription(),
                    record.getCategory(),
                    alertDate
            );
        } catch (Exception exception) {
            return null;
        }
    }

    private record CachedHistory(AlertHistorySnapshot snapshot, Instant lastUpdatedAt) {
        private static CachedHistory empty() {
            return new CachedHistory(
                    new AlertHistorySnapshot("רעננה", 0, 0, Instant.EPOCH, List.of(), List.of()),
                    Instant.EPOCH
            );
        }

        private CachedHistory(AlertHistorySnapshot snapshot) {
            this(snapshot, Instant.now());
        }
    }
}
