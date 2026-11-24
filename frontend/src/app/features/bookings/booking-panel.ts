import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { problemMessage, problemOf } from '../../core/api/problem';
import { AuthStore } from '../../core/auth/auth-store';
import { addDays, localIsoDate } from '../../shared/format/dates';
import { durationLabel } from '../../shared/format/labels';
import { Offering } from '../../shared/models/provider';
import { DiscoveryApi } from '../discovery/discovery-api';
import { Slot } from '../discovery/discovery.models';
import { NotificationStore } from '../notifications/notification-store';
import { PetApi } from '../pets/pet-api';
import { Pet } from '../pets/pet.models';
import { BookingApi } from './booking-api';

@Component({
  selector: 'pp-booking-panel',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './booking-panel.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingPanel {
  private readonly discoveryApi = inject(DiscoveryApi);
  private readonly bookingApi = inject(BookingApi);
  private readonly petApi = inject(PetApi);
  private readonly notifications = inject(NotificationStore);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthStore);

  readonly providerId = input.required<number>();
  readonly services = input.required<Offering[]>();
  readonly currency = input.required<string>();

  protected readonly durationLabel = durationLabel;
  protected readonly firstDay = localIsoDate(new Date());
  protected readonly lastDay = localIsoDate(addDays(new Date(), 60));
  protected readonly canBook = computed(() => this.auth.hasRole('OWNER'));
  protected readonly returnUrl = computed(() => `/providers/${this.providerId()}`);

  protected readonly offeringId = signal<number | null>(null);
  protected readonly date = signal(localIsoDate(addDays(new Date(), 1)));
  protected readonly slots = signal<Slot[]>([]);
  protected readonly selectedSlot = signal<Slot | null>(null);
  protected readonly pets = signal<Pet[]>([]);
  protected readonly petId = signal<number | null>(null);
  protected readonly notes = signal('');
  protected readonly loadingSlots = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly selectedService = computed(
    () => this.services().find((service) => service.id === this.offeringId()) ?? null,
  );

  constructor() {
    effect(() => {
      const services = this.services();
      untracked(() => {
        if (this.offeringId() === null && services.length > 0) {
          this.offeringId.set(services[0].id);
        }
      });
    });
    effect(() => {
      const providerId = this.providerId();
      const offeringId = this.offeringId();
      const date = this.date();
      if (offeringId !== null && date) {
        untracked(() => this.loadSlots(providerId, offeringId, date));
      }
    });
    effect(() => {
      if (this.canBook()) {
        untracked(() => this.loadPets());
      }
    });
  }

  protected selectService(event: Event): void {
    this.offeringId.set(Number((event.target as HTMLSelectElement).value));
  }

  protected selectDate(event: Event): void {
    this.date.set((event.target as HTMLInputElement).value);
  }

  protected selectPet(event: Event): void {
    this.petId.set(Number((event.target as HTMLSelectElement).value));
  }

  protected notesInput(event: Event): void {
    this.notes.set((event.target as HTMLTextAreaElement).value);
  }

  protected send(): void {
    const slot = this.selectedSlot();
    const offeringId = this.offeringId();
    const petId = this.petId();
    if (!slot || offeringId === null || petId === null) {
      this.error.set('Choose a service, a time and your pet first.');
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    this.bookingApi
      .request({ offeringId, petId, startAt: slot.startAt, notes: this.notes().trim() || null })
      .subscribe({
        next: (booking) => {
          this.notifications.refresh();
          void this.router.navigate(['/bookings', booking.id]);
        },
        error: (error: unknown) => {
          this.sending.set(false);
          this.error.set(problemMessage(error));
          const code = problemOf(error).code;
          if (code === 'SLOT_TAKEN' || code === 'SLOT_UNAVAILABLE') {
            this.loadSlots(this.providerId(), offeringId, this.date());
          }
        },
      });
  }

  private loadSlots(providerId: number, offeringId: number, date: string): void {
    this.loadingSlots.set(true);
    this.selectedSlot.set(null);
    this.discoveryApi.slots(providerId, offeringId, date).subscribe({
      next: (slots) => {
        this.slots.set(slots);
        this.loadingSlots.set(false);
      },
      error: (error: unknown) => {
        this.slots.set([]);
        this.error.set(problemMessage(error));
        this.loadingSlots.set(false);
      },
    });
  }

  private loadPets(): void {
    this.petApi.list().subscribe({
      next: (pets) => {
        this.pets.set(pets);
        if (pets.length > 0 && this.petId() === null) {
          this.petId.set(pets[0].id);
        }
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
