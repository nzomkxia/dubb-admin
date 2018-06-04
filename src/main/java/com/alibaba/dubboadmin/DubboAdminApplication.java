package com.alibaba.dubboadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DubboAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(DubboAdminApplication.class, args);
	}
}
