package com.petpal;

import static org.assertj.core.api.Assertions.assertThat;

import com.petpal.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

class PetPalApplicationTests extends IntegrationTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void contextLoadsAndSchemaMigrates() {
        long categories = jdbc.sql("select count(*) from service_categories").query(Long.class).single();
        long tables = jdbc.sql("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_type = 'BASE TABLE'
                  and table_name <> 'flyway_schema_history'
                """).query(Long.class).single();

        assertThat(categories).isEqualTo(5);
        assertThat(tables).isEqualTo(10);
    }

    @Test
    void unknownPathReturnsProblemWithCode() {
        TestUser owner = registerOwner();

        assertThat(mvc.get().uri("/api/does-not-exist").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }
}
