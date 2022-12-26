package org.example.domain.model;

import lombok.Data;
import org.example.persistence.annotations.Column;
import org.example.persistence.annotations.Entity;
import org.example.persistence.annotations.Id;
import org.example.persistence.annotations.Table;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Table(name = "students")
public class Student implements Serializable {
    @Serial
    private static final long serialVersionUID = 42L;
    @Id
    @Column(name = "id", unique = true)
    private Long id;
    @Column(name = "first_name", nullable = false, unique = true)
    private String firstName;

    public Student(String firstName) {
        this.firstName = firstName;
    }

    Student() {
    }

}