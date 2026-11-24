package com.petpal.pet;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetResponse(
        Long id, String name, Species species, String breed, LocalDate birthDate, BigDecimal weightKg,
        String careNotes) {

    public static PetResponse from(Pet pet) {
        return new PetResponse(pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getBirthDate(),
                pet.getWeightKg(), pet.getCareNotes());
    }
}
