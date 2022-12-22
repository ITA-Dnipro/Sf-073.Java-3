package org.example.persistence.ormanager;

import lombok.extern.slf4j.Slf4j;
import org.assertj.db.type.Source;
import org.assertj.db.type.Table;
import org.assertj.db.type.ValueType;
import org.example.domain.model.Student;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.db.api.Assertions.assertThat;
import static org.assertj.db.output.Outputs.output;

@Slf4j
@ExtendWith(MockitoExtension.class)
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
    static Connection conn;
    static PreparedStatement stmt;
    static Source source;
    static Table table;
    static Student student;
    static Path path;

    @BeforeAll
    static void setUp() throws SQLException {
        conn = org.example.persistence.ormanager.DataSource.getConnection();
        conn.prepareStatement(STUDENTS_TABLE).execute();

        source = new Source("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "");
        student = new Student("Bob");
        path = Path.of("db/properties/h2.properties");
    }

    @AfterAll
    static void close() throws SQLException {
        conn.close();
        if (stmt != null) {
            stmt.close();
        }
    }

    @Test
    void DoesFileExist() {
        assertThat(path).exists();
    }

    @Test
    @DisplayName("try saving to database")
    void save() throws SQLException {
        stmt = conn.prepareStatement(SQL_ADD_ONE);
        Student st2 = new Student("Ani");
        Student st3 = new Student("Dale");
        Student st4 = new Student("Laura");
        Student st5 = new Student("Shelly");

        stmt.setString(1, student.getFirstName());
        stmt.executeUpdate();
        stmt.setString(1, st2.getFirstName());
        stmt.executeUpdate();
        stmt.setString(1, st3.getFirstName());
        stmt.executeUpdate();
        stmt.setString(1, st4.getFirstName());
        stmt.executeUpdate();
        stmt.setString(1, st5.getFirstName());
        stmt.executeUpdate();

        table = new Table(source, "students", new String[]{"id", "first_name"}, null);
//        - choose the order of the Row:
//        table = new Table(source, "students", new Table.Order[]{Table.Order.asc("first_name")});

        assertThat(table).row(0)
                .value().isEqualTo(1L)
                .value().isEqualTo("Bob");

        assertThat(table).column("first_name").isOfType(ValueType.TEXT, true);

        output(table).toConsole();
    }

    @Test
    void CheckIfIdIsSet() {
        assertThat(student.getId()).as("User \"%s\" has no ID set yet", student.getFirstName()).isNull();
    }

    @Test
    void ComparingObjectsFieldByField() {
        Student student2 = new Student("Bob");
        assertThat(student).usingRecursiveComparison().isEqualTo(student2);

        //checking that the two objects aren't equal
        log.atDebug().log("{}", student.equals(student2));
    }

}