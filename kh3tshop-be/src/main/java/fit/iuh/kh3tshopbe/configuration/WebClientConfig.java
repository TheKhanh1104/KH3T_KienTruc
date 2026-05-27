package fit.iuh.kh3tshopbe.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("Content-Type", "application/json")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public String geminiApiKey() {
        return apiKey;
    }
}


