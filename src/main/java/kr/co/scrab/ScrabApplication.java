package kr.co.scrab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = {"kr.co.scrab.*"})
@EnableScheduling
public class ScrabApplication {

	private static final Logger logger = LoggerFactory.getLogger(ScrabApplication.class);
	
	public static void main(String[] args) {
		
				
		SpringApplication.run(ScrabApplication.class, args);
	}
	
}
