package com.petpal.booking;

import com.petpal.catalog.CategoryCode;
import com.petpal.pet.Species;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id,
        BookingStatus status,
        Instant startAt,
        Instant endAt,
        BigDecimal price,
        String currency,
        String ownerNotes,
        boolean reviewed,
        ServiceInfo service,
        PetInfo pet,
        ProviderInfo provider,
        OwnerInfo owner,
        List<BookingAction> actions) {

    public record ServiceInfo(Long id, String title, CategoryCode categoryCode, int durationMinutes) {
    }

    public record PetInfo(Long id, String name, Species species, String breed, String careNotes) {
    }

    public record ProviderInfo(Long id, String displayName, String city, String address, String phone) {
    }

    public record OwnerInfo(Long id, String name, String phone) {
    }

    static BookingResponse of(Booking booking, BookingParty viewer, List<BookingAction> actions, String currency) {
        boolean showProviderPhone = viewer == BookingParty.PROVIDER
                || booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.COMPLETED;
        var offering = booking.getOffering();
        var pet = booking.getPet();
        var provider = booking.getProvider();
        var owner = booking.getOwner();
        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getPrice(),
                currency,
                booking.getOwnerNotes(),
                booking.isReviewed(),
                new ServiceInfo(offering.getId(), offering.getTitle(), offering.getCategory().getCode(),
                        offering.getDurationMinutes()),
                new PetInfo(pet.getId(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getCareNotes()),
                new ProviderInfo(provider.getId(), provider.getDisplayName(), provider.getCity(), provider.getAddress(),
                        showProviderPhone ? provider.getUser().getPhone() : null),
                new OwnerInfo(owner.getId(), owner.fullName(), owner.getPhone()),
                actions);
    }
}
