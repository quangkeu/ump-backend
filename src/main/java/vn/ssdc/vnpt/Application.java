package vn.ssdc.vnpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by vietnq on 10/21/16.
 */
@SpringBootApplication(
        scanBasePackages = {"vn.ssdc.vnpt"}
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
