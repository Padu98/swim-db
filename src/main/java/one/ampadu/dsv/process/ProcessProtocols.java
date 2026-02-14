package one.ampadu.dsv.process;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.ampadu.dsv.entity.ProtocolProcessRun;
import one.ampadu.dsv.process.service.DownloadDSVProtocolService;
import one.ampadu.dsv.process.service.SaveProtocolEntriesService;
import one.ampadu.dsv.repository.ProtocolProcessRunRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessProtocols {

    private final static int FIRST_RUN_NUMBER = 162009;
    public final static int MAX_DAY_MONTH = 1500;
    public final static String DSV_PROTOCOL_ADDRESS = "https://dsvdaten.dsv.de/File.aspx?F=WKResults&File=%s.pdf";

    private final SaveProtocolEntriesService _saveProtocolEntriesService;
    private final ProtocolProcessRunRepository _protocolProcessRunRepo;
    private final DownloadDSVProtocolService _downloadDsvPdfService;


    @Scheduled(fixedRate = 5400000)
    public void start() {
        log.info("run ProcessProtocols {}", new Date());
        try {
            process();
        } catch (InterruptedException e) {
            log.error("Process was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void process() throws InterruptedException {
        int nextRun = FIRST_RUN_NUMBER;
        Optional<ProtocolProcessRun> lastRunOptional = _protocolProcessRunRepo.findFirstByOrderByIdDesc();
        if (lastRunOptional.isPresent()) {
            ProtocolProcessRun lastRun = lastRunOptional.get();
            nextRun = lastRun.getSuccess() == null ? lastRun.getNumberId() : DownloadDSVProtocolService.determineNextRunNumber(lastRun.getNumberId());
        }
        executeRun(nextRun);
    }

    private void executeRun(int nextRun) {
        DownloadDSVProtocolService.PdFDownloadResult pdFDownloadResult = _downloadDsvPdfService.execute(nextRun);

        switch (pdFDownloadResult) {
            case DownloadDSVProtocolService.Success success -> processPages(success);
            case DownloadDSVProtocolService.Error _ -> log.error("Download error for ID {}", nextRun);
        }
    }

    private void processPages(DownloadDSVProtocolService.Success success) {
        ProtocolProcessRun protocolProcessRun = _protocolProcessRunRepo.findByNumberId(success.processNumber()).orElse(new ProtocolProcessRun());
        protocolProcessRun.setNumberId(success.processNumber());
        try {
            _saveProtocolEntriesService.execute(success.data());
            protocolProcessRun.setSuccess(new Date());
            log.info("Successfully processed pages for ID {}", success.processNumber());
        } catch (Exception ex){
            log.error("Failed to process pages: {}", ex.getMessage());
        }
        _protocolProcessRunRepo.save(protocolProcessRun);
    }

}