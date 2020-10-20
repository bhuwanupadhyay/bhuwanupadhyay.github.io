package _1;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

/*

In this code snippet, I will show how to implement and inject the ResponseErrorHandler
interface in a RestTemplate instance — to gracefully handle HTTP errors returned by remote APIs.

*/

class ClientRestException extends RuntimeException {

    public ClientRestException(String message) {
        super(message);
    }
}

class ClientRestTemplateErrorHandler extends DefaultResponseErrorHandler {

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody()))) {
                String httpBodyResponse = reader.lines().collect(Collectors.joining(""));
                throw new ClientRestException(httpBodyResponse);
            }
        }
    }
}

record OrderInfo(@JsonProperty("customerId") String customerId,
                 @JsonProperty("itemId") String itemId,
                 @JsonProperty("quantity") Integer quantity) {

}

@Service
class OrderServiceClient {

    private final String orderServiceUrl;
    private final RestTemplate restTemplate;

    public OrderServiceClient(@Value("${order-service.url:http://localhost:8080}") String orderServiceUrl) {
        this.orderServiceUrl = orderServiceUrl;
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new ClientRestTemplateErrorHandler());
    }

    public OrderInfo getOrderInfo(String orderId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(orderServiceUrl)
                .path("/orders/{orderId}").queryParam("expired", "NO").build(orderId);

        RequestEntity<Void> getByOrderId = RequestEntity.get(uri).build();

        return restTemplate.exchange(getByOrderId, OrderInfo.class).getBody();
    }
}

@SpringBootApplication
public class ErrorHandlerWithRestTemplate {

    private final OrderServiceClient orderServiceClient;

    public ErrorHandlerWithRestTemplate(OrderServiceClient orderServiceClient) {
        this.orderServiceClient = orderServiceClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(ErrorHandlerWithRestTemplate.class, args);
    }

    @EventListener
    public void on(ApplicationReadyEvent event) {
        orderServiceClient.getOrderInfo("123");
    }

}
