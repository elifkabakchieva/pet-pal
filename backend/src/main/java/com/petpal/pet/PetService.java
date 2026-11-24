package com.petpal.pet;

import com.petpal.common.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetService {

    private final PetRepository pets;

    PetService(PetRepository pets) {
        this.pets = pets;
    }

    @Transactional(readOnly = true)
    public List<PetResponse> list(Long ownerId) {
        return pets.findByOwnerIdAndArchivedFalseOrderByNameAsc(ownerId).stream().map(PetResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PetResponse get(Long ownerId, Long petId) {
        return PetResponse.from(requireActivePet(ownerId, petId));
    }

    @Transactional
    public PetResponse create(Long ownerId, PetRequest request) {
        Pet pet = Pet.create(ownerId, request.name().trim(), request.species(), trimToNull(request.breed()),
                request.birthDate(), request.weightKg(), trimToNull(request.careNotes()));
        return PetResponse.from(pets.save(pet));
    }

    @Transactional
    public PetResponse update(Long ownerId, Long petId, PetRequest request) {
        Pet pet = requireActivePet(ownerId, petId);
        pet.update(request.name().trim(), request.species(), trimToNull(request.breed()), request.birthDate(),
                request.weightKg(), trimToNull(request.careNotes()));
        return PetResponse.from(pet);
    }

    @Transactional
    public void archive(Long ownerId, Long petId) {
        requireActivePet(ownerId, petId).archive();
    }

    @Transactional(readOnly = true)
    public Pet requireActivePet(Long ownerId, Long petId) {
        return pets.findByIdAndOwnerIdAndArchivedFalse(petId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("PET_NOT_FOUND", "This pet does not exist."));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
