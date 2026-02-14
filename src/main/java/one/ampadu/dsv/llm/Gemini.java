package one.ampadu.dsv.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.ampadu.dsv.entity.LLMModel;
import one.ampadu.dsv.repository.AIModelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class Gemini implements LLM {

    private final AIModelRepository _aiModelRepo;
    private LLMModel _activeModel;
    private Client _gemini;
    private GenerateContentConfig _config;

    @Value("${gemini.api.key}")
    private String apiKey;

    @PostConstruct
    private void init() {
        _gemini = Client.builder()
                .apiKey(apiKey)
                .build();
        _config = null;
        _activeModel = loadAvailableModel().orElse(null);
    }

    private Optional<LLMModel> loadAvailableModel() {
        return _aiModelRepo.findNextAvailableByProvider(getProvider().name(), LocalDateTime.now());
    }


    @Override
    public ExecutionResult execute(String prompt) {
        if (_activeModel == null) {
            _activeModel = loadAvailableModel().orElseThrow();
        }

        try {
            GenerateContentResponse response = _gemini.models.generateContent(_activeModel.getName(), prompt, _config);
            return new Success(response.text());
        } catch (Exception ex) {
            return resetModel(ex.getMessage());
        }
    }

    @Override
    public Provider getProvider() {
        return Provider.Gemini;
    }

    @Override
    public boolean isFree() {
        return true;
    }

    private ExecutionResult resetModel(String ex) {
        log.warn("Resetting model due to exception: {}", ex);
        if (ex.contains("429")) {
            Instant blockUntil = Instant.now().plus(1, ChronoUnit.DAYS);
            if (ex.contains("Please retry in") && !ex.contains("limit: 0")) {
                blockUntil = Instant.now().plus(3, ChronoUnit.MINUTES);
            }
            _activeModel.setBlocked(Date.from(blockUntil));
            _aiModelRepo.save(_activeModel);


            Optional<LLMModel> aiModel = loadAvailableModel();
            _activeModel = aiModel.orElse(null);

            return new ChangedModel(_activeModel == null ? null : _activeModel.getName());
        }

        log.error("Unknown exception: {}", ex);
        return new ChangedModel(null);
    }
}
