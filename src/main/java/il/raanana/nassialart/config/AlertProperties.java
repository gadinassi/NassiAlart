package il.raanana.nassialart.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.alert")
public class AlertProperties {

    @NotBlank
    private String city = "רעננה";

    @NotEmpty
    private List<String> cityAliases = List.of("רעננה");

    @NotBlank
    private String orefUrl = "https://www.oref.org.il/WarningMessages/alert/alerts.json";

    @NotBlank
    private String referer = "https://www.oref.org.il/12481-he/Pakar.aspx";

    @Min(2)
    @Max(60)
    private int pollIntervalSeconds = 5;

    @Min(10)
    @Max(1800)
    private int activeAlertRetentionSeconds = 300;

    @Min(1)
    @Max(3600)
    private int connectTimeoutSeconds = 5;

    @Min(1)
    @Max(3600)
    private int readTimeoutSeconds = 5;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getCityAliases() {
        return cityAliases;
    }

    public void setCityAliases(List<String> cityAliases) {
        this.cityAliases = cityAliases;
    }

    public String getOrefUrl() {
        return orefUrl;
    }

    public void setOrefUrl(String orefUrl) {
        this.orefUrl = orefUrl;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public int getActiveAlertRetentionSeconds() {
        return activeAlertRetentionSeconds;
    }

    public void setActiveAlertRetentionSeconds(int activeAlertRetentionSeconds) {
        this.activeAlertRetentionSeconds = activeAlertRetentionSeconds;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public Duration activeAlertRetention() {
        return Duration.ofSeconds(activeAlertRetentionSeconds);
    }

    public Duration connectTimeout() {
        return Duration.ofSeconds(connectTimeoutSeconds);
    }

    public Duration readTimeout() {
        return Duration.ofSeconds(readTimeoutSeconds);
    }
}
