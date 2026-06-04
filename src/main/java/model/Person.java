package model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import model.value.BirthDate;

import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2, max = 40)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Column(nullable = false)
    private String surname;

    @Valid
    @NotNull
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "birth_date", nullable = false))
    private BirthDate birthDate;

    @ElementCollection
    @CollectionTable(
            name = "person_emails",
            joinColumns = @JoinColumn(name = "person_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = "email")
    )
    @Column(name = "email", nullable = false)
    @Size(min = 1, max = 3)
    private Set<@Email @NotBlank String> emails = new HashSet<>();

    @Email
    private String primaryEmail;

    @NotBlank
    @Column(nullable = false)
    private String passwordHash = "a109e36947ad56de1dca1cc49f0ef8ac9ad9a7b1aa0df41fb3c4cb73c1ff01ea";

    protected Person() {
    }

    protected Person(String name, String surname, BirthDate birthDate, Set<String> emails) {
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
        if (emails != null) {
            this.emails.addAll(emails);
            this.primaryEmail = this.emails.stream().sorted().findFirst().orElse(null);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public BirthDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(BirthDate birthDate) {
        this.birthDate = birthDate;
    }

    public Set<String> getEmails() {
        return emails;
    }

    public void setEmails(Set<String> emails) {
        this.emails = emails;
    }

    public String getPrimaryEmailValue() {
        return primaryEmail;
    }

    public void setPrimaryEmailValue(String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public void setPrimaryEmail(String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPrimaryEmail() {
        if (primaryEmail != null && !primaryEmail.isBlank()) {
            return primaryEmail;
        }
        return emails.stream().sorted().findFirst().orElse("");
    }

    public int getAge() {
        return birthDate == null ? 0 : birthDate.getAge();
    }
}
