import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { problemMessage } from '../../core/api/problem';
import { EmptyState } from '../../shared/ui/empty-state';
import { NotificationApi } from './notification-api';
import { AppNotification } from './notification.models';
import { NotificationStore } from './notification-store';

@Component({
  selector: 'pp-notifications-page',
  imports: [DatePipe, EmptyState],
  templateUrl: './notifications-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationsPage implements OnInit {
  private readonly api = inject(NotificationApi);
  private readonly store = inject(NotificationStore);
  private readonly router = inject(Router);

  protected readonly items = signal<AppNotification[]>([]);
  protected readonly nextPage = signal<number | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadPage(0);
  }

  protected loadPage(page: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(page).subscribe({
      next: (result) => {
        this.items.update((items) => (page === 0 ? result.content : [...items, ...result.content]));
        this.nextPage.set(result.page + 1 < result.totalPages ? result.page + 1 : null);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected open(item: AppNotification): void {
    const navigate = () => {
      if (item.link) {
        void this.router.navigateByUrl(item.link);
      }
    };
    if (item.read) {
      navigate();
      return;
    }
    this.api.markRead(item.id).subscribe({
      next: () => {
        this.items.update((items) => items.map((n) => (n.id === item.id ? { ...n, read: true } : n)));
        this.store.refresh();
        navigate();
      },
      error: () => navigate(),
    });
  }

  protected markAllRead(): void {
    this.api.markAllRead().subscribe({
      next: () => {
        this.items.update((items) => items.map((n) => ({ ...n, read: true })));
        this.store.refresh();
      },
      error: (error: unknown) => this.error.set(problemMessage(error)),
    });
  }

  protected hasUnread(): boolean {
    return this.items().some((item) => !item.read);
  }
}
