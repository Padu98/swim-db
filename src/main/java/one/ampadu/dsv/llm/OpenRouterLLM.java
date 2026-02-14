package one.ampadu.dsv.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.ampadu.dsv.entity.LLMModel;
import one.ampadu.dsv.repository.AIModelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterLLM implements LLM{

    @PostConstruct
    private void init() {
        _activeModel = loadAvailableModel().orElse(null);
    }


    private final ObjectMapper _mapper = new ObjectMapper();
    private static final String OPEN_ROUTER_URL = "https://api.openrouter.ai/v1/chat/completions";
    private final HttpClient _httpClient = HttpClient.newHttpClient();

    private LLMModel _activeModel;
    private final AIModelRepository _aiModelRepo;

    private Optional<LLMModel> loadAvailableModel() {
        return _aiModelRepo.findNextAvailableByProvider(getProvider().name(), LocalDateTime.now());
    }

    @Value("${router.api.key}")
    private String openRouterApiKey;


    @Override
    public ExecutionResult execute(String prompt) {
        if (_activeModel == null) {
            _activeModel = loadAvailableModel().orElseThrow();
        }
        return null;
    }

    private ExecutionResult resetModel(String ex) {
        return null;
    }

    @Override
    public Provider getProvider() {
        return Provider.OpenRouter;
    }

    @Override
    public boolean isFree() {
        return true;
    }

    private HttpRequest buildRequest(String prompt, String model) {
        ArrayNode messages = _mapper.createArrayNode();
        messages.add(_mapper.createObjectNode()
                .put("role", "user")
                .put("content", prompt));

        ObjectNode reasoning = _mapper.createObjectNode();
        reasoning.put("enabled", true);

        ObjectNode payload = _mapper.createObjectNode();
        payload.put("model", model);
        payload.set("messages", messages);
        payload.set("reasoning", reasoning);

        return HttpRequest.newBuilder()
                .uri(URI.create(OPEN_ROUTER_URL))
                .header("Authorization", "Bearer " + openRouterApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
    }


    private String sendRequest(HttpRequest request, String prompt, String provider, int retries) throws InterruptedException {
        HttpResponse<String> response;
        try {
            response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200){
                throw new RuntimeException("Request to eden ai failed: " + response.statusCode());
            }

            JsonNode root = _mapper.readTree(response.body());
            String generatedText = root.path(provider).path("generated_text").asText();

            if (generatedText == null || generatedText.isBlank()) {
                //TODO
            }

            return generatedText;

        } catch (IOException | InterruptedException e) {
            if (retries > 0) {

            }
            throw new RuntimeException("Failed after retries due to network error: " + e.getMessage());
        }
    }
}
