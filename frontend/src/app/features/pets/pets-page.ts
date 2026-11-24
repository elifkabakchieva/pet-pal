import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { problemMessage } from '../../core/api/problem';
import { SPECIES_LABELS, ageLabel } from '../../shared/format/labels';
import { EmptyState } from '../../shared/ui/empty-state';
import { PetApi } from './pet-api';
import { PetForm } from './pet-form';
import { Pet, PetRequest } from './pet.models';

@Component({
  selector: 'pp-pets-page',
  imports: [PetForm, EmptyState],
  templateUrl: './pets-page.html',
  styleUrl: './pets-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PetsPage implements OnInit {
  private readonly petApi = inject(PetApi);

  protected readonly speciesLabels = SPECIES_LABELS;
  protected readonly pets = signal<Pet[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly formOpen = signal(false);
  protected readonly editing = signal<Pet | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.petApi.list().subscribe({
      next: (pets) => {
        this.pets.set(pets);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected startCreate(): void {
    this.editing.set(null);
    this.formError.set(null);
    this.formOpen.set(true);
  }

  protected startEdit(pet: Pet): void {
    this.editing.set(pet);
    this.formError.set(null);
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editing.set(null);
  }

  protected save(request: PetRequest): void {
    const current = this.editing();
    const call = current ? this.petApi.update(current.id, request) : this.petApi.create(request);
    this.saving.set(true);
    this.formError.set(null);
    call.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.formError.set(problemMessage(error));
      },
    });
  }

  protected remove(pet: Pet): void {
    if (!confirm(`Remove ${pet.name} from your pets? Existing bookings will still show ${pet.name}.`)) {
      return;
    }
    this.petApi.archive(pet.id).subscribe({
      next: () => this.load(),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected age(pet: Pet): string | null {
    return ageLabel(pet.birthDate);
  }
}
