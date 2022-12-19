package org.example.persistence.ormanager;

import lombok.extern.slf4j.Slf4j;
import org.assertj.db.type.Source;
import org.assertj.db.type.Table;
import org.example.domain.model.Student;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.sql.*;

import static org.assertj.db.output.Outputs.output;

@Slf4j
class ORManagerTest {
    private static final String STUDENTS_TABLE = """
            CREATE TABLE students
            (
            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            first_name VARCHAR(30) NOT NULL
            )
            """;
    DataSource dataSource;
    static Source source;
    static Connection conn;
    Student student;
    private static final String DB_URL = "jdbc:h2:mem:test";

    @BeforeAll
    static void setUp() throws SQLException {
        conn = DriverManager.getConnection(DB_URL);
        conn.prepareStatement(STUDENTS_TABLE).executeUpdate();
        source = new Source("jdbc:h2:mem:test", "", "");
    }

    @BeforeEach
    void init() {
        student = new Student("Bob");
    }

    @Test
    void DBTesting() {
        Table table = new Table(source, "students");
        output(table).toConsole();
    }

    @AfterAll
    static void close() throws SQLException {
        conn.close();
    }
}