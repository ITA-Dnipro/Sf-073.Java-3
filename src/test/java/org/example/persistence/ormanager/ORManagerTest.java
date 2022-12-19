package org.example.persistence.ormanager;

import org.assertj.db.type.Source;
import org.assertj.db.type.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

class ORManagerTest {
    DataSource dataSource;
    Source source;

    @BeforeEach
    void init() {

        source = new Source("jdbc:h2:mem:test", "sa", "");
    }

    @Test
    void DBTesting() {
        Table table = new Table(dataSource, "students");
    }
}