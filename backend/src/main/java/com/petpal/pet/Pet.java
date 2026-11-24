package com.petpal.pet;

import com.petpal.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pets")
public class Pet extends BaseEntity {

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Species species;

    @Column(length = 80)
    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "care_notes", length = 1000)
    private String careNotes;

    @Column(nullable = false)
    private boolean archived;

    protected Pet() {
    }

    public static Pet create(Long ownerId, String name, Species species, String breed, LocalDate birthDate,
            BigDecimal weightKg, String careNotes) {
        Pet pet = new Pet();
        pet.ownerId = ownerId;
        pet.update(name, species, breed, birthDate, weightKg, careNotes);
        return pet;
    }

    public void update(String name, Species species, String breed, LocalDate birthDate, BigDecimal weightKg,
            String careNotes) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.weightKg = weightKg;
        this.careNotes = careNotes;
    }

    public void archive() {
        this.archived = true;
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId.equals(userId);
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public Species getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public String getCareNotes() {
        return careNotes;
    }

    public boolean isArchived() {
        return archived;
    }
}
