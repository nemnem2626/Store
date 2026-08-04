package com.poly.asm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = "com.poly.asm")
@ComponentScan(basePackages = "com.poly.asm")
@EntityScan(basePackages = {"com.poly.asm.entitys"})
public class AsmApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsmApplication.class, args);
	}

}
