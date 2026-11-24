package com.petpal.pet;

import com.petpal.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pets")
@Tag(name = "Pets")
class PetController {

    private final PetService petService;

    PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    List<PetResponse> list(AuthenticatedUser user) {
        return petService.list(user.id());
    }

    @GetMapping("/{id}")
    PetResponse get(AuthenticatedUser user, @PathVariable Long id) {
        return petService.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PetResponse create(AuthenticatedUser user, @Valid @RequestBody PetRequest request) {
        return petService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    PetResponse update(AuthenticatedUser user, @PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return petService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archive(AuthenticatedUser user, @PathVariable Long id) {
        petService.archive(user.id(), id);
    }
}
