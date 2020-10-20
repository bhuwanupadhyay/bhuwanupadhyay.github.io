package _5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GracefulShutdown {

    public static void main(String[] args) {
        SpringApplication.run(GracefulShutdown.class, args);
    }
}
