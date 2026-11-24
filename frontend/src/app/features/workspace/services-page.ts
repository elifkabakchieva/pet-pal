import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { applyServerErrors, problemMessage } from '../../core/api/problem';
import { CategoryApi } from '../../shared/catalog/category-api';
import { CURRENCY, durationLabel } from '../../shared/format/labels';
import { CategoryCode } from '../../shared/models/enums';
import { Offering } from '../../shared/models/provider';
import { EmptyState } from '../../shared/ui/empty-state';
import { WorkspaceApi } from './workspace-api';

const DURATIONS = [15, 30, 45, 60, 75, 90, 105, 120, 150, 180, 240, 300, 360, 420, 480];

@Component({
  selector: 'pp-services-page',
  imports: [ReactiveFormsModule, CurrencyPipe, EmptyState],
  templateUrl: './services-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServicesPage implements OnInit {
  private readonly api = inject(WorkspaceApi);

  protected readonly categories = toSignal(inject(CategoryApi).list(), { initialValue: [] });
  protected readonly durations = DURATIONS;
  protected readonly durationLabel = durationLabel;
  protected readonly currency = CURRENCY;

  protected readonly services = signal<Offering[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly formOpen = signal(false);
  protected readonly editing = signal<Offering | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    categoryCode: ['GROOMING' as CategoryCode, Validators.required],
    title: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    description: ['', Validators.maxLength(1000)],
    price: [0, [Validators.required, Validators.min(1), Validators.max(10000)]],
    durationMinutes: [60, Validators.required],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.services().subscribe({
      next: (services) => {
        this.services.set(services);
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
    this.form.reset({ categoryCode: 'GROOMING', title: '', description: '', price: 0, durationMinutes: 60 });
    this.formOpen.set(true);
  }

  protected startEdit(service: Offering): void {
    this.editing.set(service);
    this.formError.set(null);
    this.form.reset({
      categoryCode: service.categoryCode,
      title: service.title,
      description: service.description ?? '',
      price: service.price,
      durationMinutes: service.durationMinutes,
    });
    this.formOpen.set(true);
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.editing.set(null);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const request = {
      categoryCode: value.categoryCode,
      title: value.title.trim(),
      description: value.description.trim() || null,
      price: Number(value.price),
      durationMinutes: Number(value.durationMinutes),
    };
    const current = this.editing();
    const call = current ? this.api.updateService(current.id, request) : this.api.createService(request);
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
        applyServerErrors(this.form, error);
        this.formError.set(problemMessage(error));
      },
    });
  }

  protected deactivate(service: Offering): void {
    if (!confirm(`Stop offering "${service.title}"? Existing bookings are kept.`)) {
      return;
    }
    this.api.deactivateService(service.id).subscribe({
      next: () => this.load(),
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }
}
