package il.raanana.nassialart.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class DemoAlertRequest {

    @NotBlank
    private String title = "ירי רקטות וטילים";

    @NotBlank
    private String description = "היכנסו למרחב המוגן ושהו בו 10 דקות";

    @Positive
    private int secondsToLive = 180;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSecondsToLive() {
        return secondsToLive;
    }

    public void setSecondsToLive(int secondsToLive) {
        this.secondsToLive = secondsToLive;
    }
}
