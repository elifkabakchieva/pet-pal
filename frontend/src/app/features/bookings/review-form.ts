import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { problemMessage } from '../../core/api/problem';
import { BookingApi } from './booking-api';

@Component({
  selector: 'pp-review-form',
  imports: [ReactiveFormsModule],
  templateUrl: './review-form.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewForm {
  private readonly api = inject(BookingApi);

  readonly bookingId = input.required<number>();
  readonly providerName = input.required<string>();
  readonly submitted = output<void>();

  protected readonly ratings = [5, 4, 3, 2, 1];
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', Validators.maxLength(1000)],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Choose a rating from 1 to 5 stars.');
      return;
    }
    const value = this.form.getRawValue();
    this.sending.set(true);
    this.error.set(null);
    this.api.review(this.bookingId(), { rating: Number(value.rating), comment: value.comment.trim() || null }).subscribe({
      next: () => {
        this.sending.set(false);
        this.submitted.emit();
      },
      error: (error: unknown) => {
        this.sending.set(false);
        this.error.set(problemMessage(error));
      },
    });
  }
}
