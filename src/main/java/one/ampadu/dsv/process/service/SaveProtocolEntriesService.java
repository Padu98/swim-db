package one.ampadu.dsv.process.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.extern.slf4j.Slf4j;
import one.ampadu.dsv.llm.LLM;
import one.ampadu.dsv.entity.ProtocolEntry;
import one.ampadu.dsv.repository.ProtocolEntryRepository;
import one.ampadu.dsv.util.JsonUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SaveProtocolEntriesService {

    private static final String PROMPT_PROTOCOL_ENTRY = """
            Role: You are a specialized data extraction assistant for competitive swimming protocols.
            
            Task: Extract all individual race results from the provided text, which is a single page from a PDF swimming protocol.
            
            Data Fields to Extract:
            firstName: The swimmer's first name.
            lastName: The swimmer's last name.
            ageGroup: The birth year or age category (extract as a number).
            sex: "MALE" or "FEMALE".
            time: The race time. Convert the format "MM:SS.hh" or "SS.hh" into total milliseconds (long).
            distance: The race distance in meters (e.g., 50, 100, 200, 400, 800, 1500).
            stroke: The swimming style (e.g., "Freestyle", "Breaststroke", "Backstroke", "Butterfly", "Medley").
            club: The club the swimmer starts for, if apparent. If not, simply an empty string. Always remove all double quotes from the club name. That is very important!
            
            It is absolutely forbidden to add any additional fields to the JSON structure like a secondLastName or lastName2!!! If a person really has two second names you can just combine them in the field lastName!
            
            Rules:
            AgeGroup Filter: You must extract the birth year (e.g., 1999 or Jahrgang 98). If the birth year is older than 1970 (e.g., 1965, 1960) or if the value is a relative age (e.g., AK20, AK25, AK30) rather than a birth year, discard the entire record.
            Zero-Entry Handling: If the page contains no race results (e.g., only cover page, TOC, or general info), return an empty array [].
            Distance/Stroke Context: Usually, the distance and stroke are mentioned once as a header for a block of results. Apply this context to all swimmers listed under that header.
            Removing double quotes from strings: Remove double quotes from every club (e.g. "S.C./"Hellas/" Einbeck e.V." becomes "club": "S.C.Hellas Einbeck e.V.").
            Output Format: Return strictly valid JSON. Do not include any conversational text, markdown formatting (unless requested), or explanations.
            Fields of the json: It is absolutely forbidden to add any additional fields to the JSON structure like secondLastName or a lastName2! Only the fields listed below are allowed.
            
            JSON Structure:
            [
              {
                "firstName": "String",
                "lastName": "String",
                "ageGroup": number,
                "sex": "String",
                "time": number,
                "distance": number,
                "stroke": "String",
                "club": "String"
              }
            ]
            
            If you can't guess the stroke of the first element, I've provided you the last used stroke: %s
            
            Input Text: %s
            """;

    private static final String YEAR_AND_PLACE_PROMPT = """
            Role: You are a data extraction specialist for sports event documentation.
            
            Task: Analyze the provided text from the first two pages of a swimming competition protocol and extract the Year and the Location (City/Place) of the event.
            Also extract the pool distance which ofcourse can only be 25 or 50 meters.
            
            Extraction Rules:
            Year: Look for the year the competition took place (e.g., 2023, 2024). Return only the 4-digit number.
            Place: Extract the city or specific venue where the competition was held. If you cant find it the place should be the name of the competition.
            poolDistance: competitions can be either long course (50m) or short course (25m). Only extract the number! -> 25 or 50
            Format: Return strictly valid JSON. No markdown code blocks (unless specified), no preamble, no conversational filler.
            
            JSON Structure:
   
            {
              "year": number,
              "place": "String",
              "poolDistance": number,
            }
            Special Instruction: If the information is missing or ambiguous, return null for that specific field. But always return a JSON in the given format! That is very important.
            
            Input Text from first page: %s
            
            Input Text from second page: %s
            
            Input Text from third page: %s
            """;

    public SaveProtocolEntriesService(List<LLM> llmList, ProtocolEntryRepository protocolRepo){
        _llmList = llmList;
        _protocolRepo = protocolRepo;
        _currentLLM = _llmList.stream().filter(LLM::isFree).findFirst().orElse(null);
    }

    private final List<LLM> _llmList;
    private LLM _currentLLM;
    private final ProtocolEntryRepository _protocolRepo;
    private final ObjectMapper _mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .build();

    public void execute(List<String> pages){
        List<ProtocolEntry> entries = new ArrayList<>();
        String lastStroke = "None";
        CompetitionMeta competitionMeta = extractPlaceYearAndPoolLength(pages.getFirst(), pages.get(1), pages.get(2));
        if (competitionMeta.poolDistance == 0) {
            competitionMeta = extractPlaceYearAndPoolLength(pages.getFirst(), pages.get(1), pages.get(2) + pages.get(3) + pages.get(4));
        }

        for (int i = 0; i < pages.size(); i++) {
            String page = pages.get(i);
            if (!entries.isEmpty()) {
                lastStroke = entries.getLast().getStroke();
            }

            log.info("Parsing page {}/{}", i + 1, pages.size());
            String json = executePrompt(PROMPT_PROTOCOL_ENTRY.formatted(lastStroke, page));
            log.info("Got result from LLM");

            List<ProtocolEntry> entriesFromJson = processProtocolJson(json, competitionMeta);
            entries.addAll(entriesFromJson);
            log.info("New entries collected: {}", entries.size());
        }

        log.info("Saving {} entries", entries.size());
        _protocolRepo.saveAll(entries);
    }

    private String executePrompt(String prompt) {
        LLM.ExecutionResult result = _currentLLM.execute(prompt);
        return switch (result) {
            case LLM.Success s -> s.data();
            case LLM.ChangedModel modelWithSameProvider -> {
                reInitializeCurrentLLM(modelWithSameProvider);
                yield executePrompt(prompt);
            }
            case LLM.Error _ -> throw new RuntimeException("Error while executing prompt");
        };
    }

    private void reInitializeCurrentLLM(LLM.ChangedModel modelWithSameProvider) {
        if(modelWithSameProvider.name() == null){
            Optional<LLM> firstCostFreeLLM = _llmList.stream()
                    .filter(llm -> llm.getProvider() != _currentLLM.getProvider())
                    .filter(LLM::isFree)
                    .findFirst();

            _currentLLM = firstCostFreeLLM.orElseGet(() -> _llmList.stream().filter(llm -> !llm.isFree()).findFirst().orElseThrow());
        }
    }

    private CompetitionMeta extractPlaceYearAndPoolLength(String firstPage, String secondPage, String thirdPage){
        try {
            String jsonFromLlm = executePrompt(YEAR_AND_PLACE_PROMPT.formatted(firstPage, secondPage, thirdPage));
            log.info("Competition meta JSON: {}", jsonFromLlm);
            return _mapper.readValue(JsonUtil.cleanMetaDataJson(jsonFromLlm), CompetitionMeta.class);
        } catch (Exception e) {
            log.error("Failed to parse competition meta data", e);
            return new CompetitionMeta(0, "Unknown", 0);
        }
    }

    private record CompetitionMeta(int year, String place, int poolDistance) {}

    private List<ProtocolEntry> processProtocolJson(String jsonResponse, CompetitionMeta competitionMeta) {
        try {
            String cleanedJson = JsonUtil.cleanJsonArrayString(JsonUtil.fixClubFieldQuotes(jsonResponse));
            List<ProtocolEntry> entries = _mapper.readValue(
                    cleanedJson,
                    new TypeReference<>() {}
            );
            for (ProtocolEntry entry : entries) {
                entry.setCalendarYear(competitionMeta.year);
                entry.setPlace(competitionMeta.place);
                entry.setPoolDistance(competitionMeta.poolDistance);
            }
            return entries;

        } catch (Exception e) {
            log.error("Invalid json", e);
            log.error("Raw LLM response: {}", jsonResponse);
            throw new RuntimeException("Invalid json", e);
        }
    }
}
