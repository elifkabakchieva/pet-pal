import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { problemMessage } from '../../core/api/problem';
import { VerificationStatus } from '../../shared/models/enums';
import { EmptyState } from '../../shared/ui/empty-state';
import { StatusBadge } from '../../shared/ui/status-badge';
import { AdminApi, AdminProvider } from './admin-api';

@Component({
  selector: 'pp-verification-page',
  imports: [DatePipe, EmptyState, StatusBadge],
  templateUrl: './verification-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerificationPage implements OnInit {
  private readonly api = inject(AdminApi);

  protected readonly tabs: { status: VerificationStatus; label: string }[] = [
    { status: 'PENDING', label: 'Waiting' },
    { status: 'REJECTED', label: 'Not verified' },
    { status: 'VERIFIED', label: 'Verified' },
  ];
  protected readonly status = signal<VerificationStatus>('PENDING');
  protected readonly providers = signal<AdminProvider[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly busyId = signal<number | null>(null);
  protected readonly rejectingId = signal<number | null>(null);
  protected readonly rejectNote = signal('');

  ngOnInit(): void {
    this.load();
  }

  protected select(status: VerificationStatus): void {
    this.status.set(status);
    this.notice.set(null);
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.providers(this.status()).subscribe({
      next: (providers) => {
        this.providers.set(providers);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected approve(provider: AdminProvider): void {
    this.busyId.set(provider.id);
    this.api.approve(provider.id).subscribe({
      next: () => this.done(`${provider.displayName} is verified and now visible to owners.`),
      error: (error: unknown) => this.failed(error),
    });
  }

  protected startReject(provider: AdminProvider): void {
    this.rejectingId.set(provider.id);
    this.rejectNote.set('');
  }

  protected noteInput(event: Event): void {
    this.rejectNote.set((event.target as HTMLTextAreaElement).value);
  }

  protected confirmReject(provider: AdminProvider): void {
    const note = this.rejectNote().trim();
    if (note.length < 5) {
      this.error.set('Write a short reason (at least 5 characters) so the provider knows what to fix.');
      return;
    }
    this.busyId.set(provider.id);
    this.api.reject(provider.id, note).subscribe({
      next: () => this.done(`${provider.displayName} was not verified. They can see your note.`),
      error: (error: unknown) => this.failed(error),
    });
  }

  private done(message: string): void {
    this.busyId.set(null);
    this.rejectingId.set(null);
    this.notice.set(message);
    this.load();
  }

  private failed(error: unknown): void {
    this.busyId.set(null);
    this.error.set(problemMessage(error));
  }
}
