import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SPECIES_LABELS } from '../../shared/format/labels';
import { SPECIES, Species } from '../../shared/models/enums';
import { Pet, PetRequest } from './pet.models';

@Component({
  selector: 'pp-pet-form',
  imports: [ReactiveFormsModule],
  templateUrl: './pet-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PetForm {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly pet = input<Pet | null>(null);
  readonly pending = input(false);
  readonly saved = output<PetRequest>();
  readonly cancelled = output<void>();

  protected readonly species = SPECIES;
  protected readonly speciesLabels = SPECIES_LABELS;
  protected readonly today = new Date().toISOString().slice(0, 10);

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
    species: ['DOG' as Species, Validators.required],
    breed: ['', Validators.maxLength(80)],
    birthDate: [''],
    weightKg: this.fb.control<number | null>(null, [Validators.min(0.1), Validators.max(200)]),
    careNotes: ['', Validators.maxLength(1000)],
  });

  constructor() {
    effect(() => {
      const pet = this.pet();
      this.form.reset({
        name: pet?.name ?? '',
        species: pet?.species ?? 'DOG',
        breed: pet?.breed ?? '',
        birthDate: pet?.birthDate ?? '',
        weightKg: pet?.weightKg ?? null,
        careNotes: pet?.careNotes ?? '',
      });
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saved.emit({
      name: value.name.trim(),
      species: value.species,
      breed: value.breed.trim() || null,
      birthDate: value.birthDate || null,
      weightKg: value.weightKg,
      careNotes: value.careNotes.trim() || null,
    });
  }
}
