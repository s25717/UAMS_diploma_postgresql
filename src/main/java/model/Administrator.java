package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import model.value.BirthDate;

import java.util.Set;

@Entity
public class Administrator extends Person {
    @NotBlank
    @Column(name = "admin_employee_number", nullable = false, unique = true)
    private String employeeNumber;

    protected Administrator() {
    }

    public Administrator(String name, String surname, BirthDate birthDate, Set<String> emails, String employeeNumber) {
        super(name, surname, birthDate, emails);
        this.employeeNumber = employeeNumber;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }
}
