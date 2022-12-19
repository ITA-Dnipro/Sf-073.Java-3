package org.example.persistence.ormanager;

import lombok.extern.slf4j.Slf4j;
import org.assertj.db.type.Source;
import org.assertj.db.type.Table;
import org.example.domain.model.Student;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.db.output.Outputs.output;

@Slf4j
class ORManagerTest {
    //language=H2
    private static final String STUDENTS_TABLE = """
            CREATE TABLE students
            (
            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            first_name VARCHAR(30) NOT NULL
            )
            """;
    //language=H2
    private static final String SQL_ADD_ONE = "INSERT INTO students (first_name) VALUES(?)";
    private static final String DB_URL = "jdbc:h2:mem:test";
    static Source source;
    static Connection conn;
    static Table table;
    static Student student1;

    @BeforeAll
    static void setUp() throws SQLException {
//        conn = DataSource.getConnection();
        conn = DriverManager.getConnection(DB_URL);
        conn.prepareStatement(STUDENTS_TABLE).execute();
        source = new Source("jdbc:h2:mem:test", "", "");
        table = new Table(source, "students");
        student1 = new Student("Bob");
    }

    @AfterAll
    static void close() throws SQLException {
        conn.close();
    }

    @Test
    void saveToDB() {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_ADD_ONE)) {
            Student st2 = new Student("Ani");
            Student st3 = new Student("Dale");
            Student st4 = new Student("Laura");
            Student st5 = new Student("Shelly");

            stmt.setString(1, student1.getFirstName());
            stmt.executeUpdate();
            stmt.setString(1, st2.getFirstName());
            stmt.executeUpdate();
            stmt.setString(1, st3.getFirstName());
            stmt.executeUpdate();
            stmt.setString(1, st4.getFirstName());
            stmt.executeUpdate();
            stmt.setString(1, st5.getFirstName());
            stmt.executeUpdate();
            output(table).toConsole();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}