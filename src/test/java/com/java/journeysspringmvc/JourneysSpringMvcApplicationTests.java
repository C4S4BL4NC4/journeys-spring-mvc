/*
 * Smoke test: starts the whole application context against the in-memory H2 database.
 *
 * If any bean can't be created (bad wiring, broken Flyway migration, entity/schema mismatch
 * under ddl-auto=validate...), this fails, catching configuration errors early.
 */
package com.java.journeysspringmvc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JourneysSpringMvcApplicationTests {

    @Test
    void contextLoads() {
    }

}
