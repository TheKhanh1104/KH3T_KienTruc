package fit.iuh.kh3tshopbe.service;

import fit.iuh.kh3tshopbe.exception.GeminiRetryableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public GeminiService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Retryable(include = GeminiRetryableException.class, maxAttempts = 3, backoff = @Backoff(delay = 4000))
    public String generateText(String prompt) {
        String url = "/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || !response.containsKey("candidates")) {
                throw new GeminiRetryableException("Gemini API response is empty or invalid.");
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new GeminiRetryableException("No candidates returned from Gemini API.");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                throw new GeminiRetryableException("Gemini API returned an empty content payload.");
            }

            Object text = parts.get(0).get("text");
            if (text == null) {
                throw new GeminiRetryableException("Gemini API response did not contain generated text.");
            }

            return text.toString();

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429) {
                throw new GeminiRetryableException("Transient Gemini API error: " + e.getStatusCode(), e);
            }
            return "Gemini API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString();
        } catch (WebClientRequestException e) {
            throw new GeminiRetryableException("Failed to call Gemini API.", e);
        } catch (RuntimeException e) {
            throw new GeminiRetryableException("Unexpected error calling Gemini API.", e);
        }
    }

    @Recover
    public String recover(GeminiRetryableException exception, String prompt) {
        return "Gemini service is temporarily unavailable after retries. Please try again later.";
    }
}
