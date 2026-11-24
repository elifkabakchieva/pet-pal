package com.petpal.pet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerIdAndArchivedFalseOrderByNameAsc(Long ownerId);

    Optional<Pet> findByIdAndOwnerIdAndArchivedFalse(Long id, Long ownerId);
}
