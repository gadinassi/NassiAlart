package il.raanana.nassialart.controller;

import il.raanana.nassialart.config.AlertProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final AlertProperties properties;

    public PageController(AlertProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("kioskMode", false);
        model.addAttribute("cityName", properties.getCity());
        return "index";
    }

    @GetMapping("/kiosk")
    public String kiosk(Model model) {
        model.addAttribute("kioskMode", true);
        model.addAttribute("cityName", properties.getCity());
        return "index";
    }
}
