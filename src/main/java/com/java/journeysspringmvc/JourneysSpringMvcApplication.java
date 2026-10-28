/*
 * Entry point of the Journeys API, a Spring Boot port of Jonas Schmedtmann's Natours.
 *
 * Running main() starts Spring Boot, which scans this package and everything below it
 * for components (controllers, services, repositories, config) and wires them together,
 * then starts the embedded web server on the port from application.properties.
 */
package com.java.journeysspringmvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JourneysSpringMvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(JourneysSpringMvcApplication.class, args);
    }

}
