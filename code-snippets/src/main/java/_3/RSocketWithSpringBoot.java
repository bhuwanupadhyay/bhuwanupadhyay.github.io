package _3;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;


record Message(
        @JsonProperty("source") String source,
        @JsonProperty("message") String message,
        @JsonProperty("createdAt") long createdAt
) {
    Message {
        createdAt = Instant.now().getEpochSecond();
    }
}

@Controller
@Slf4j
class RSocketController {

    @MessageMapping("app-socket")
    public Message appSocket(Message message) {
        log.info("Received request: {}", message);
        return message;
    }
}

@SpringBootApplication
public class RSocketWithSpringBoot {

    public static void main(String[] args) {
        new SpringApplicationBuilder(RSocketWithSpringBoot.class)
                .profiles("snippet3")
                .run(args);
    }

}
