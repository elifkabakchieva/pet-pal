import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { problemMessage } from '../../core/api/problem';
import { AuthStore } from '../../core/auth/auth-store';
import { BookingStatus } from '../../shared/models/enums';
import { Page } from '../../shared/models/page';
import { EmptyState } from '../../shared/ui/empty-state';
import { StatusBadge } from '../../shared/ui/status-badge';
import { BookingApi } from './booking-api';
import { Booking, BookingListSort } from './booking.models';

type TabKey = 'requests' | 'upcoming' | 'history';

interface Tab {
  key: TabKey;
  label: string;
  statuses: readonly BookingStatus[];
  sort: BookingListSort;
  empty: string;
}

const TABS: readonly Tab[] = [
  { key: 'requests', label: 'Requests', statuses: ['REQUESTED'], sort: 'UPCOMING', empty: 'No requests waiting.' },
  { key: 'upcoming', label: 'Upcoming', statuses: ['CONFIRMED'], sort: 'UPCOMING', empty: 'Nothing confirmed yet.' },
  {
    key: 'history',
    label: 'History',
    statuses: ['COMPLETED', 'CANCELLED', 'DECLINED'],
    sort: 'RECENT',
    empty: 'Finished, cancelled and declined bookings will appear here.',
  },
];

@Component({
  selector: 'pp-bookings-page',
  imports: [CurrencyPipe, DatePipe, RouterLink, EmptyState, StatusBadge],
  templateUrl: './bookings-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingsPage {
  private readonly api = inject(BookingApi);
  protected readonly auth = inject(AuthStore);

  readonly tab = input<string>();

  protected readonly tabs = TABS;
  protected readonly current = computed(() => TABS.find((tab) => tab.key === this.tab()) ?? TABS[0]);
  protected readonly isProvider = computed(() => this.auth.hasRole('PROVIDER'));

  protected readonly page = signal<Page<Booking> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const tab = this.current();
      untracked(() => this.load(tab, 0));
    });
  }

  protected load(tab: Tab = this.current(), pageNumber = 0): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(tab.statuses, tab.sort, pageNumber).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }
}
