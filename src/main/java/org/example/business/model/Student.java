package org.example.business.model;

import org.example.persistence.annotations.Id;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @Column(name = "id")
    private Long id;
    @Column(name = "first_name")
    private String firstName;


    public Student(String firstName) {
        this.firstName = firstName;
    }

    private Student() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + firstName + '\'' +
                '}';
    }
}
