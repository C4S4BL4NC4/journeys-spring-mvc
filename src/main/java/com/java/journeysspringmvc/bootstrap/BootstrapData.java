package com.java.journeysspringmvc.bootstrap;

import com.java.journeysspringmvc.util.FormatUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapData implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        greeter();
    }

    private void greeter() {
        System.out.println("=".repeat(50));
        System.out.println("Hello World!");
        System.out.println(FormatUrl.of("http://localhost:8080/swagger-ui.html"));
        System.out.println("=".repeat(50));
    }
}
