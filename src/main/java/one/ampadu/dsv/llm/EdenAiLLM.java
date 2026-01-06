package one.ampadu.dsv.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EdenAiLLM implements LLM {

    private static final String EDEN_URL = "https://api.edenai.run/v2/text/chat";

    public static final String DEFAULT_MODEL  = "deepseek/deepseek-chat";

    private static final Map<Integer, String> GEMINI_MODEL_MAP = Map.of(
            5, "openai/gpt-5-mini",
            4, "meta/llama3-1-70b-instruct-v1:0",
            3, "google/gemini-2.5-flash",
            2, "google/gemini-2.0-flash",
            1, "google/gemini-1.5-flash-latest"
            );

    private final HttpClient _httpClient = HttpClient.newHttpClient();

    private final ObjectMapper _mapper = new ObjectMapper();

    @Value("${edenai.api.key.paid}")
    private String edenAiApiKey;


    @Override
    public ExecutionResult execute(String prompt)  {

        String generatedText;
        try {
            HttpRequest request = buildRequest(prompt, DEFAULT_MODEL);
            generatedText = sendRequestToEdenAI(request, prompt, DEFAULT_MODEL.split("/")[0],  5);
        } catch (Exception e) {
            log.error("Failed to execute paid prompt: {}", e.getMessage());
            return new Error();
        }
        return new Success(generatedText);
    }

    private HttpRequest buildRequest(String prompt, String model) {
        String[] split = model.split("/");


        ArrayNode messages = _mapper.createArrayNode();
        messages.add(_mapper.createObjectNode()
                .put("role", "user")
                .put("message", prompt));

        ObjectNode payload = _mapper.createObjectNode();
        payload.put("providers", split[0]);
        payload.set("settings", _mapper.createObjectNode().put(split[0], split[1]));

        payload.put("text", prompt);
        payload.put("max_tokens", 25000);

        return HttpRequest.newBuilder()
                .uri(URI.create(EDEN_URL))
                .header("Authorization", edenAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
    }


    private String sendRequestToEdenAI(HttpRequest request, String prompt, String provider, int retries) throws InterruptedException {
        HttpResponse<String> response;
        try {
            response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200){
                throw new RuntimeException("Request to eden ai failed: " + response.statusCode());
            }

            JsonNode root = _mapper.readTree(response.body());
            String generatedText = root.path(provider).path("generated_text").asText();

            if (generatedText == null || generatedText.isBlank()) {
                if (root.path(provider).path("error") != null) {
                    String errorMessage = root.path(provider).path("error").path("message").toString();
                    log.error(errorMessage);
                }

                if(retries > 0){
                    String model = GEMINI_MODEL_MAP.get(retries);
                    log.warn("Failed to generate text. Retrying with model: {}", model);
                    HttpRequest httpRequest = buildRequest(prompt, model);
                    return sendRequestToEdenAI(httpRequest, prompt, model.split("/")[0], retries - 1);
                }

                throw new RuntimeException("Failed to generate text. No retries.");
            }

            return generatedText;

        } catch (IOException | InterruptedException e) {
            if (retries > 0) {
                log.warn("Network error (possibly GOAWAY). Retrying... Error: {}", e.getMessage());
                TimeUnit.MILLISECONDS.sleep(5000);
                return sendRequestToEdenAI(request, prompt, provider, retries - 1);
            }
            throw new RuntimeException("Failed after retries due to network error: " + e.getMessage());
        }
    }

    @Override
    public Provider getProvider() {
        return Provider.EdenAiPaid;
    }

    @Override
    public boolean isFree() {
        return false;
    }
}
