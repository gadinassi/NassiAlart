package il.raanana.nassialart.controller;

import il.raanana.nassialart.model.AlertHistorySnapshot;
import il.raanana.nassialart.model.AlertSnapshot;
import il.raanana.nassialart.model.DemoAlertRequest;
import il.raanana.nassialart.service.AlertHistoryService;
import il.raanana.nassialart.service.AlertStateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertApiController {

    private final AlertStateService alertStateService;
    private final AlertHistoryService alertHistoryService;

    public AlertApiController(AlertStateService alertStateService, AlertHistoryService alertHistoryService) {
        this.alertStateService = alertStateService;
        this.alertHistoryService = alertHistoryService;
    }

    @GetMapping("/current")
    public AlertSnapshot currentAlert() {
        return alertStateService.getSnapshot();
    }

    @GetMapping("/history")
    public AlertHistorySnapshot history() {
        return alertHistoryService.getHistorySnapshot();
    }

    @PostMapping("/demo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void triggerDemo(@Valid @RequestBody DemoAlertRequest request) {
        alertStateService.activateDemoAlert(request);
    }

    @PostMapping("/demo/clear")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void clearDemo() {
        alertStateService.clearDemoAlert();
    }
}
