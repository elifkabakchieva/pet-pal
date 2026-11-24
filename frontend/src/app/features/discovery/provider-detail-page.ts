import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { problemMessage } from '../../core/api/problem';
import { DAY_LABELS, durationLabel, timeLabel } from '../../shared/format/labels';
import { DAYS } from '../../shared/models/enums';
import { RatingStars } from '../../shared/ui/rating-stars';
import { BookingPanel } from '../bookings/booking-panel';
import { DiscoveryApi } from './discovery-api';
import { ProviderDetail, Review } from './discovery.models';

@Component({
  selector: 'pp-provider-detail-page',
  imports: [CurrencyPipe, DatePipe, RatingStars, BookingPanel],
  templateUrl: './provider-detail-page.html',
  styleUrl: './provider-detail-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderDetailPage {
  private readonly api = inject(DiscoveryApi);
  private readonly title = inject(Title);

  readonly id = input.required<string>();

  protected readonly durationLabel = durationLabel;
  protected readonly timeLabel = timeLabel;
  protected readonly provider = signal<ProviderDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly reviews = signal<Review[]>([]);
  protected readonly nextReviewPage = signal<number | null>(null);

  protected readonly hoursByDay = computed(() => {
    const hours = this.provider()?.weeklyHours ?? [];
    return DAYS.map((day) => ({
      day,
      label: DAY_LABELS[day],
      windows: hours.filter((window) => window.dayOfWeek === day),
    }));
  });

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.load(id));
    });
  }

  protected loadReviews(providerId: number, page = 0): void {
    this.api.reviews(providerId, page).subscribe({
      next: (result) => {
        this.reviews.update((current) => (page === 0 ? result.content : [...current, ...result.content]));
        this.nextReviewPage.set(result.page + 1 < result.totalPages ? result.page + 1 : null);
      },
      error: () => this.nextReviewPage.set(null),
    });
  }

  protected load(id = Number(this.id())): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.provider(id).subscribe({
      next: (provider) => {
        this.provider.set(provider);
        this.loadReviews(provider.id);
        this.title.setTitle(`${provider.displayName} · PetPal`);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }
}
