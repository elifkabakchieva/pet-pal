import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { problemMessage, problemOf } from '../../core/api/problem';
import { AuthStore } from '../../core/auth/auth-store';
import { BOOKING_ACTION_LABELS, SPECIES_LABELS, STATUS_LABELS, durationLabel } from '../../shared/format/labels';
import { StatusBadge } from '../../shared/ui/status-badge';
import { NotificationStore } from '../notifications/notification-store';
import { BookingApi } from './booking-api';
import { Booking, BookingAction, BookingDetail } from './booking.models';
import { ReviewForm } from './review-form';

type ButtonAction = Exclude<BookingAction, 'REVIEW'>;

const CONFIRM_QUESTIONS: Record<ButtonAction, string | null> = {
  CONFIRM: null,
  COMPLETE: null,
  DECLINE: 'Decline this request? The owner will be notified.',
  CANCEL: 'Cancel this booking? The other side will be notified.',
};

@Component({
  selector: 'pp-booking-detail-page',
  imports: [CurrencyPipe, DatePipe, RouterLink, StatusBadge, ReviewForm],
  templateUrl: './booking-detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingDetailPage {
  private readonly api = inject(BookingApi);
  private readonly notifications = inject(NotificationStore);
  protected readonly auth = inject(AuthStore);

  readonly id = input.required<string>();

  protected readonly actionLabels = BOOKING_ACTION_LABELS;
  protected readonly speciesLabels = SPECIES_LABELS;
  protected readonly statusLabels = STATUS_LABELS;
  protected readonly durationLabel = durationLabel;

  protected readonly detail = signal<BookingDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly reason = signal('');

  protected readonly isProvider = computed(() => this.auth.hasRole('PROVIDER'));
  protected readonly buttons = computed<ButtonAction[]>(
    () => (this.detail()?.booking.actions ?? []).filter((action): action is ButtonAction => action !== 'REVIEW'),
  );
  protected readonly canReview = computed(() => this.detail()?.booking.actions.includes('REVIEW') ?? false);
  protected readonly needsReason =computed(() => this.buttons().some((a) => a === 'DECLINE' || a === 'CANCEL'));

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.load(id));
    });
  }

  protected load(id = Number(this.id())): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.get(id).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected reasonInput(event: Event): void {
    this.reason.set((event.target as HTMLTextAreaElement).value);
  }

  protected reviewed(): void {
    this.notice.set('Thank you. Your review is published on the provider page.');
    this.load();
  }

  protected perform(action: ButtonAction): void {
    const booking = this.detail()?.booking;
    const question = CONFIRM_QUESTIONS[action];
    if (!booking || (question && !confirm(question))) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.call(action, booking).subscribe({
      next: () => {
        this.busy.set(false);
        this.reason.set('');
        this.notifications.refresh();
        this.load(booking.id);
      },
      error: (error: unknown) => {
        this.busy.set(false);
        if (problemOf(error).code === 'CONCURRENT_UPDATE') {
          this.notice.set('This booking was changed meanwhile. Here is the current state.');
          this.load(booking.id);
        } else {
          this.error.set(problemMessage(error));
        }
      },
    });
  }

  private call(action: ButtonAction, booking: Booking): Observable<Booking> {
    const reason = this.reason().trim() || null;
    switch (action) {
      case 'CONFIRM':
        return this.api.confirm(booking.id);
      case 'DECLINE':
        return this.api.decline(booking.id, reason);
      case 'CANCEL':
        return this.api.cancel(booking.id, reason);
      case 'COMPLETE':
        return this.api.complete(booking.id);
    }
  }
}
