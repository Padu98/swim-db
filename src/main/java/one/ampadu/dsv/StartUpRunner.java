package one.ampadu.dsv;


import one.ampadu.dsv.process.ProcessProtocols;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartUpRunner implements CommandLineRunner {
    private final ProcessProtocols process;

    public StartUpRunner(ProcessProtocols process) {
        this.process = process;
    }

    @Override
    public void run(String... args) {
        //process.start();
    }
}