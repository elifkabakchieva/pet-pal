package com.petpal.pet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import com.petpal.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class PetIntegrationTest extends IntegrationTest {

    @Test
    void ownerManagesPetsFromCreationToArchive() {
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Max");

        assertThat(mvc.put().uri("/api/pets/{id}", petId)
                .header(AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Max", "species": "DOG", "breed": "Labrador", "weightKg": 31.5,
                         "careNotes": "Afraid of hair dryers."}
                        """))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.breed").isEqualTo("Labrador");

        assertThat(mvc.delete().uri("/api/pets/{id}", petId).header(AUTHORIZATION, bearer(owner)))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri("/api/pets/{id}", petId).header(AUTHORIZATION, bearer(owner)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("PET_NOT_FOUND");
        MvcTestResult list = mvc.get().uri("/api/pets").header(AUTHORIZATION, bearer(owner)).exchange();
        assertThat(JsonPath.<List<Object>>read(body(list), "$")).isEmpty();
    }

    @Test
    void petsAreListedByName() {
        TestUser owner = registerOwner();
        createPet(owner, "Rex");
        createPet(owner, "Bella");

        MvcTestResult result = mvc.get().uri("/api/pets").header(AUTHORIZATION, bearer(owner)).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(body(result), "$[*].name")).containsExactly("Bella", "Rex");
    }

    @Test
    void invalidPetIsRejectedWithFieldErrors() {
        TestUser owner = registerOwner();

        MvcTestResult result = mvc.post().uri("/api/pets")
                .header(AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "weightKg": 0, "birthDate": "2999-01-01"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<List<String>>read(body(result), "$.errors[*].field"))
                .contains("name", "species", "weightKg", "birthDate");
    }

    @Test
    void ownerCannotSeeOrChangeAnotherOwnersPet() {
        TestUser alice = registerOwner();
        TestUser bob = registerOwner();
        long alicesPet = createPet(alice, "Luna");

        assertThat(mvc.get().uri("/api/pets/{id}", alicesPet).header(AUTHORIZATION, bearer(bob)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.put().uri("/api/pets/{id}", alicesPet)
                .header(AUTHORIZATION, bearer(bob))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Stolen", "species": "CAT"}
                        """))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/pets/{id}", alicesPet).header(AUTHORIZATION, bearer(bob)))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
