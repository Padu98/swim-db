package one.ampadu.dsv.process.service;

import lombok.extern.slf4j.Slf4j;
import one.ampadu.dsv.process.ProcessProtocols;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DownloadDSVProtocolService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PdFDownloadResult execute(int nextRun) {

        try {
            HttpResponse<byte[]> response = searchNextFile(nextRun);

            List<String> pages = new ArrayList<>();
            try (PDDocument document = Loader.loadPDF(response.body())) {
                PDFTextStripper stripper = new PDFTextStripper();
                int pageCount = document.getNumberOfPages();

                for (int i = 1; i <= pageCount; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);

                    String text = stripper.getText(document);
                    pages.add(text);
                }
            }

            log.info("Successfully extracted {} pages from PDF.", pages.size());
            return new Success(pages);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        return new Error();
    }


    private HttpResponse<byte[]> searchNextFile(int nextRun) throws InterruptedException, IOException {

        HttpResponse<byte[]> response;
        do {
            String fileUrl = ProcessProtocols.DSV_PROTOCOL_ADDRESS.formatted(nextRun);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .GET()
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.error("Download failed with status code: {}", response.statusCode());
            }
            long waitTime = ThreadLocalRandom.current().nextLong(1000, 6001);
            TimeUnit.MILLISECONDS.sleep(waitTime);
            nextRun = determineNextRunNumber(nextRun);
        } while (response.statusCode() != 200 && !isPdf(response.body()));

        return response;
    }

    public static int determineNextRunNumber(int lastRun){
        String lastRunAsString = String.valueOf(lastRun);

        if ( lastRunAsString.length() < 4) {
            throw new RuntimeException("number of lastRun is to small");
        }

        int splitIndex = lastRunAsString.length() - 4;

        int dayMonth = Integer.parseInt(lastRunAsString.substring(0, splitIndex));
        int year = Integer.parseInt(lastRunAsString.substring(splitIndex));

        if(dayMonth >= ProcessProtocols.MAX_DAY_MONTH){
            dayMonth = 1;
            year += 1;
        }else {
            dayMonth += 1;
        }

        String updatedNumber = String.valueOf(dayMonth) + year;
        return Integer.parseInt(updatedNumber);
    }


    private boolean isPdf(byte [] body) {
        if (body.length < 4 || body[0] != 0x25 || body[1] != 0x50 || body[2] != 0x44 || body[3] != 0x46) {
            log.error("File has no valid pdf signature.");
            return false;
        }
        return true;
    }

    public sealed interface PdFDownloadResult permits Success, Error {}
    public record Success(List<String> data) implements PdFDownloadResult {}
    public record Error() implements PdFDownloadResult {}
}