package one.ampadu.dsv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DsvDBApplication {

	public static void main(String[] args) {
		SpringApplication.run(DsvDBApplication.class, args);
	}
	//test comment for testing the pipelines
}
