package model.value;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.time.Period;

@Embeddable
public class BirthDate {
    @NotNull
    @Past
    private LocalDate value;

    protected BirthDate() {
    }

    public BirthDate(LocalDate value) {
        this.value = value;
    }

    public LocalDate getValue() {
        return value;
    }

    public void setValue(LocalDate value) {
        this.value = value;
    }

    public int getAge() {
        if (value == null) {
            return 0;
        }
        return Period.between(value, LocalDate.now()).getYears();
    }
}
