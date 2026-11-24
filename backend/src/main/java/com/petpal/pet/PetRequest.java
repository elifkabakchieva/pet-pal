package com.petpal.pet;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull Species species,
        @Size(max = 80) String breed,
        @PastOrPresent LocalDate birthDate,
        @DecimalMin("0.1") @DecimalMax("200.00") @Digits(integer = 3, fraction = 2) BigDecimal weightKg,
        @Size(max = 1000) String careNotes) {
}
