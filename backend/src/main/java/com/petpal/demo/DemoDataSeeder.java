package com.petpal.demo;

import com.petpal.catalog.CategoryCode;
import com.petpal.catalog.CategoryService;
import com.petpal.identity.Role;
import com.petpal.identity.User;
import com.petpal.identity.UserRepository;
import com.petpal.pet.Pet;
import com.petpal.pet.PetRepository;
import com.petpal.pet.Species;
import com.petpal.provider.AvailabilityRule;
import com.petpal.provider.AvailabilityRuleRepository;
import com.petpal.provider.ProviderProfile;
import com.petpal.provider.ProviderProfileRepository;
import com.petpal.provider.ServiceOffering;
import com.petpal.provider.ServiceOfferingRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("dev")
class DemoDataSeeder implements ApplicationRunner {

    static final String DEMO_PASSWORD = "petpal-demo";
    private static final String OWNER_EMAIL = "maria@demo.petpal";
    private static final Set<DayOfWeek> WEEKDAYS =
            EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final PetRepository pets;
    private final ProviderProfileRepository profiles;
    private final ServiceOfferingRepository offerings;
    private final AvailabilityRuleRepository availabilityRules;
    private final CategoryService categories;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private int nextPhoneSuffix = 101;

    DemoDataSeeder(UserRepository users, PetRepository pets, ProviderProfileRepository profiles,
            ServiceOfferingRepository offerings, AvailabilityRuleRepository availabilityRules,
            CategoryService categories, PasswordEncoder passwordEncoder, TransactionTemplate transactionTemplate,
            Clock clock) {
        this.users = users;
        this.pets = pets;
        this.profiles = profiles;
        this.offerings = offerings;
        this.availabilityRules = availabilityRules;
        this.categories = categories;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByEmail(OWNER_EMAIL)) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> seed());
        log.info("Seeded demo data (password of all demo accounts: {})", DEMO_PASSWORD);
    }

    private void seed() {
        User maria = account(OWNER_EMAIL, "Maria", "Kirova", "+359 888 123 456", "Sofia", Role.OWNER);
        pets.save(Pet.create(maria.getId(), "Max", Species.DOG, "Labrador", LocalDate.of(2022, 5, 14),
                new BigDecimal("31.50"), "Afraid of hair dryers. Loves tennis balls."));
        pets.save(Pet.create(maria.getId(), "Luna", Species.CAT, "European shorthair", LocalDate.of(2020, 9, 2),
                new BigDecimal("4.20"), null));

        provider("georgi@demo.petpal", "Georgi", "Petrov", "Georgi's Grooming", "Sofia", "ul. Shipka 12", 9,
                "Calm, patient grooming for dogs and cats of all sizes. Nine years in the trade, trained in "
                        + "hand stripping and coat care for sensitive skin. Quiet dryers, no rush.",
                List.of(
                        new Service(CategoryCode.GROOMING, "Full groom, medium dog",
                                "Bath, haircut, nails, ears and a gentle blow dry.", "45.00", 90),
                        new Service(CategoryCode.GROOMING, "Bath and brush", "For short coats between full grooms.",
                                "25.00", 45),
                        new Service(CategoryCode.TRAINING, "Puppy basics, one session",
                                "Sit, stay, recall and calm handling. Bring treats.", "30.00", 60)),
                EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                        DayOfWeek.SATURDAY),
                "09:00-13:00", "14:00-18:00");

        provider("elena@demo.petpal", "Elena", "Dimitrova", "Dr. Elena Dimitrova, veterinarian", "Sofia",
                "bul. Vitosha 88", 14,
                "Licensed veterinarian with fourteen years in small animal practice. Check-ups, vaccinations and "
                        + "honest advice on food, weight and dental care.",
                List.of(
                        new Service(CategoryCode.VETERINARY, "General check-up",
                                "Full examination with a short written summary.", "40.00", 30),
                        new Service(CategoryCode.VETERINARY, "Vaccination", "Core vaccines and passport entry.",
                                "35.00", 30)),
                WEEKDAYS, "08:00-12:00", "15:00-18:00");

        provider("ivan@demo.petpal", "Ivan", "Stoyanov", "Ivan walks dogs", "Sofia", null, 3,
                "Morning and evening walks around Borisova gradina and South Park. Small groups of at most three "
                        + "dogs, photos after every walk.",
                List.of(
                        new Service(CategoryCode.WALKING, "One hour walk", "Up to three dogs per group.", "15.00", 60),
                        new Service(CategoryCode.WALKING, "Long walk", "Two hours, for energetic dogs.", "25.00", 120)),
                EnumSet.allOf(DayOfWeek.class), "07:00-10:00", "17:00-20:00");

        provider("nadia@demo.petpal", "Nadia", "Georgieva", "Happy Paws Daycare", "Plovdiv", "bul. Ruski 40", 6,
                "A bright daycare with a fenced garden in central Plovdiv. Dogs are grouped by size and energy, "
                        + "and there is always a quiet room for naps.",
                List.of(
                        new Service(CategoryCode.DAYCARE, "Full day", "08:00 to 16:00, lunch break included.",
                                "30.00", 480),
                        new Service(CategoryCode.DAYCARE, "Half day", "Four hours of play and rest.", "18.00", 240)),
                WEEKDAYS, "08:00-18:00");
    }

    private record Service(CategoryCode category, String title, String description, String price, int minutes) {
    }

    private void provider(String email, String firstName, String lastName, String displayName, String city,
            String address, int years, String bio, List<Service> services, Set<DayOfWeek> days, String... windows) {
        User user = account(email, firstName, lastName, "+359 887 000 " + nextPhoneSuffix++, city, Role.PROVIDER);
        ProviderProfile profile = ProviderProfile.createDraft(user, displayName, city);
        profile.updateDetails(displayName, bio, city, address, years);
        profile = profiles.save(profile);

        for (Service service : services) {
            offerings.save(ServiceOffering.create(profile, categories.require(service.category()), service.title(),
                    service.description(), new BigDecimal(service.price()), service.minutes()));
        }
        List<AvailabilityRule> rules = new ArrayList<>();
        for (DayOfWeek day : days) {
            for (String window : windows) {
                String[] parts = window.split("-");
                rules.add(AvailabilityRule.of(profile.getId(), day, LocalTime.parse(parts[0]), LocalTime.parse(parts[1])));
            }
        }
        availabilityRules.saveAll(rules);

        Instant now = clock.instant();
        profile.submitForVerification(services.size(), rules.size(), now);
        profile.approve(now);
    }

    private User account(String email, String firstName, String lastName, String phone, String city, Role role) {
        return users.save(User.register(email, passwordEncoder.encode(DEMO_PASSWORD), firstName, lastName, phone,
                city, role));
    }
}
