package il.raanana.nassialart;

import il.raanana.nassialart.config.AlertProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = AlertProperties.class)
public class NassiAlertApplication {

    public static void main(String[] args) {
        SpringApplication.run(NassiAlertApplication.class, args);
    }
}
