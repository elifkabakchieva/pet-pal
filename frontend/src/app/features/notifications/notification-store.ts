import { Injectable, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, of, switchMap, timer } from 'rxjs';
import { AuthStore } from '../../core/auth/auth-store';
import { NotificationApi } from './notification-api';

const POLL_INTERVAL_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class NotificationStore {
  private readonly api = inject(NotificationApi);
  private readonly auth = inject(AuthStore);

  readonly unread = signal(0);

  constructor() {
    toObservable(this.auth.isSignedIn)
      .pipe(
        switchMap((signedIn) => {
          if (!signedIn) {
            this.unread.set(0);
            return EMPTY;
          }
          return timer(0, POLL_INTERVAL_MS).pipe(
            switchMap(() => this.api.unreadCount().pipe(catchError(() => of(null)))),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((response) => {
        if (response) {
          this.unread.set(response.count);
        }
      });
  }

  refresh(): void {
    if (!this.auth.isSignedIn()) {
      return;
    }
    this.api.unreadCount().subscribe({
      next: (response) => this.unread.set(response.count),
      error: () => undefined,
    });
  }
}
