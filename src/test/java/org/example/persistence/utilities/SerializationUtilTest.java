package org.example.persistence.utilities;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.model.Student;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class SerializationUtilTest {

    @RepeatedTest(3)
    void serialization() {
        Student st = new Student("Jorji");
        st.setId(3L);
        SerializationUtil.serialize(st);

        log.atDebug().log("before serialization: {}, {}", st, st.hashCode());
    }

    @Test
    void deserialization() {
        List<Object> emptyStudentsList = null;
        emptyStudentsList = SerializationUtil.deserialize(Student.class);

        log.atDebug().log("after deserialization: {}", emptyStudentsList);
    }

    @Test
    void WhenSerializeAndTheDeserializeObjectThenReturnThanTheyAreEquals() {
        Student student = new Student("Jack");
        student.setId(33L);
        Student student2 = new Student("Black");
        student.setId(248L);

        SerializationUtil.serialize(student);
        SerializationUtil.serialize(student2);
        List<Object> deserializedStudents = SerializationUtil.deserialize(Student.class);

        assertThat(student).usingRecursiveComparison().isEqualTo(deserializedStudents.get(0));
        assertThat(student).isEqualTo(deserializedStudents.get(0));
        assertThat(student2).usingRecursiveComparison().isEqualTo(deserializedStudents.get(1));
        assertThat(student2).isEqualTo(deserializedStudents.get(1));

        log.atDebug().log("student's hashcode: {}\n" +
                        "student2's hashcode: {}\n" +
                        "deserializedStudents's hashcode: {}, {}",
                student.hashCode(), student2.hashCode(),
                deserializedStudents.get(0).hashCode(), deserializedStudents.get(1).hashCode());
    }

}