package il.raanana.nassialart.service;

import il.raanana.nassialart.config.AlertProperties;
import il.raanana.nassialart.model.AlertSnapshot;
import il.raanana.nassialart.model.DemoAlertRequest;
import il.raanana.nassialart.model.OrefAlertResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AlertStateService {

    private static final String PRE_ALERT_TEXT = "בדקות הקרובות צפויות להתקבל התרעות באזורך";

    private final OrefAlertClient orefAlertClient;
    private final AlertProperties properties;
    private final AtomicReference<State> state;

    public AlertStateService(OrefAlertClient orefAlertClient, AlertProperties properties) {
        this.orefAlertClient = orefAlertClient;
        this.properties = properties;
        this.state = new AtomicReference<>(State.idle(properties.getCity()));
    }

    @PostConstruct
    void initialize() {
        refresh();
    }

    @Scheduled(fixedDelayString = "${app.alert.poll-interval-seconds:5}000")
    public void refresh() {
        State current = state.get();
        if (current.demoMode() && current.activeUntil().isAfter(Instant.now())) {
            return;
        }

        try {
            OrefAlertResponse response = orefAlertClient.fetchCurrentAlert();
            Instant now = Instant.now();
            List<String> matchedLocations = matchLocations(response);

            if (!matchedLocations.isEmpty()) {
                state.set(State.active(
                        properties.getCity(),
                        safeText(response.getTitle(), "התרעה פעילה"),
                        safeText(response.getDescription(), "היכנסו למרחב המוגן ופעלו לפי הנחיות פיקוד העורף"),
                        matchedLocations,
                        now,
                        now.plus(properties.activeAlertRetention()),
                        now,
                        false,
                        "live",
                        "התקבלה התרעה פעילה מפיקוד העורף"
                ));
                return;
            }

            state.set(current.toIdle(now, "live", "אין כרגע התרעה לרעננה"));
        } catch (Exception exception) {
            Instant now = Instant.now();
            state.set(current.withPollFailure(now, "offline", "כשל בגישה לשירות פיקוד העורף: " + exception.getClass().getSimpleName()));
        }
    }

    public AlertSnapshot getSnapshot() {
        State current = state.get();
        Instant now = Instant.now();
        if (!current.demoMode() && current.active() && current.activeUntil().isBefore(now)) {
            State expired = current.toIdle(current.lastSuccessfulPollAt(), current.sourceStatus(), "פג תוקף חלון ההתרעה האחרון");
            state.compareAndSet(current, expired);
            current = state.get();
        }
        return current.toSnapshot();
    }

    public void activateDemoAlert(DemoAlertRequest request) {
        Instant now = Instant.now();
        state.set(State.active(
                properties.getCity(),
                request.getTitle(),
                request.getDescription(),
                List.of(properties.getCity()),
                now,
                now.plusSeconds(request.getSecondsToLive()),
                now,
                true,
                "demo",
                "הדמיית התרעה מקומית פעילה"
        ));
    }

    public void clearDemoAlert() {
        State current = state.get();
        state.set(current.toIdle(current.lastSuccessfulPollAt(), "demo", "הדמיית ההתרעה נוקתה"));
    }

    private List<String> matchLocations(OrefAlertResponse response) {
        if (response == null || response.getData() == null) {
            return List.of();
        }

        return response.getData().stream()
                .filter(Objects::nonNull)
                .filter(location -> properties.getCityAliases().stream().anyMatch(location::contains))
                .toList();
    }

    private String safeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private record State(
            boolean active,
            boolean demoMode,
            String city,
            String title,
            String description,
            List<String> matchedLocations,
            Instant lastAlertActivatedAt,
            Instant alertReceivedAt,
            Instant activeUntil,
            Instant lastSuccessfulPollAt,
            String sourceStatus,
            String sourceMessage
    ) {
        private static State idle(String city) {
            return new State(
                    false,
                    false,
                    city,
                    "אין התרעה",
                    "אין כרגע התרעה לרעננה",
                    List.of(),
                    null,
                    null,
                    Instant.EPOCH,
                    null,
                    "starting",
                    "המערכת עולה"
            );
        }

        private static State active(
                String city,
                String title,
                String description,
                List<String> matchedLocations,
                Instant alertReceivedAt,
                Instant activeUntil,
                Instant lastSuccessfulPollAt,
                boolean demoMode,
                String sourceStatus,
                String sourceMessage
        ) {
            return new State(
                    true,
                    demoMode,
                    city,
                    title,
                    description,
                    matchedLocations,
                    alertReceivedAt,
                    alertReceivedAt,
                    activeUntil,
                    lastSuccessfulPollAt,
                    sourceStatus,
                    sourceMessage
            );
        }

        private State toIdle(Instant lastSuccessfulPollAt, String sourceStatus, String sourceMessage) {
            return new State(
                    false,
                    false,
                    city,
                    "אין התרעה",
                    "אין כרגע התרעה לרעננה",
                    List.of(),
                    lastAlertActivatedAt,
                    null,
                    Instant.EPOCH,
                    lastSuccessfulPollAt,
                    sourceStatus,
                    sourceMessage
            );
        }

        private State withPollFailure(Instant pollTime, String sourceStatus, String sourceMessage) {
            if (active && activeUntil != null && activeUntil.isAfter(Instant.now())) {
                return new State(
                        true,
                        demoMode,
                        city,
                        title,
                        description,
                        matchedLocations,
                        lastAlertActivatedAt,
                        alertReceivedAt,
                        activeUntil,
                        lastSuccessfulPollAt,
                        sourceStatus,
                        sourceMessage
                );
            }

            return new State(
                    false,
                    false,
                    city,
                    "אין התרעה",
                    "מצב השירות אינו זמין כרגע",
                    List.of(),
                    lastAlertActivatedAt,
                    null,
                    Instant.EPOCH,
                    lastSuccessfulPollAt != null ? lastSuccessfulPollAt : pollTime,
                    sourceStatus,
                    sourceMessage
            );
        }

        private AlertSnapshot toSnapshot() {
            String trafficLightState;
            String trafficLightLabel;

            if (active && isPreAlert()) {
                trafficLightState = "YELLOW";
                trafficLightLabel = "צהוב - לשהות בקרבת מקלט";
            } else if (active) {
                trafficLightState = "RED";
                trafficLightLabel = "אדום - שהייה במקלט";
            } else if ("offline".equalsIgnoreCase(sourceStatus) || "starting".equalsIgnoreCase(sourceStatus)) {
                trafficLightState = "YELLOW";
                trafficLightLabel = "צהוב - לשהות בקרבת מקלט";
            } else {
                trafficLightState = "GREEN";
                trafficLightLabel = "ירוק - יציאה";
            }

            return new AlertSnapshot(
                    active,
                    demoMode,
                    city,
                    trafficLightState,
                    trafficLightLabel,
                    title,
                    description,
                    active && isPreAlert()
                            ? "להישאר בקרבת מרחב מוגן ולהיערך לאפשרות של התרעה."
                            : active
                            ? "להיכנס למרחב מוגן ולהמתין להנחיות מעודכנות."
                            : "המשך מעקב רגיל. המסך יתרענן אוטומטית.",
                    matchedLocations,
                    lastAlertActivatedAt,
                    alertReceivedAt,
                    activeUntil,
                    lastSuccessfulPollAt,
                    sourceStatus,
                    sourceMessage
            );
        }

        private boolean isPreAlert() {
            return title != null && title.contains(PRE_ALERT_TEXT);
        }
    }
}
