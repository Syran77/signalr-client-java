package com.miternos.signalr;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {


    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(false).run(args);
        //SpringApplication.run(Application.class, args);
    }

    @Bean
    SignalRClient signalRClient() {
        return new SignalRClient();
    }


    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {

        return args -> {

            SignalRClient signalRClient = ctx.getBean(SignalRClient.class);
            signalRClient.connect();

        };

    }

}
