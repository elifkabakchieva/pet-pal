import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { applyServerErrors, problemMessage } from '../../core/api/problem';
import { StatusBadge } from '../../shared/ui/status-badge';
import { WorkspaceApi } from './workspace-api';
import { ProviderProfile } from './workspace.models';

@Component({
  selector: 'pp-profile-page',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, StatusBadge],
  templateUrl: './profile-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit {
  private readonly api = inject(WorkspaceApi);

  protected readonly minBioLength = 50;
  protected readonly profile = signal<ProviderProfile | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly submitting = signal(false);
  protected readonly message = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    displayName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    address: ['', Validators.maxLength(200)],
    yearsExperience: [0, [Validators.required, Validators.min(0), Validators.max(60)]],
    bio: ['', Validators.maxLength(2000)],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.profile().subscribe({
      next: (profile) => {
        this.show(profile);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.message.set(null);
    this.api
      .updateProfile({
        displayName: value.displayName.trim(),
        city: value.city.trim(),
        address: value.address.trim() || null,
        yearsExperience: value.yearsExperience,
        bio: value.bio.trim() || null,
      })
      .subscribe({
        next: (profile) => {
          this.saving.set(false);
          this.show(profile);
          this.message.set({ tone: 'success', text: 'Your profile is saved.' });
        },
        error: (error: unknown) => {
          this.saving.set(false);
          applyServerErrors(this.form, error);
          this.message.set({ tone: 'error', text: problemMessage(error) });
        },
      });
  }

  protected submitForVerification(): void {
    if (this.form.dirty) {
      this.message.set({ tone: 'error', text: 'Save your changes first, then send the profile for verification.' });
      return;
    }
    this.submitting.set(true);
    this.message.set(null);
    this.api.submitProfile().subscribe({
      next: (profile) => {
        this.submitting.set(false);
        this.show(profile);
        this.message.set({ tone: 'success', text: 'Sent. We will let you know when an admin has looked at it.' });
      },
      error: (error: unknown) => {
        this.submitting.set(false);
        this.message.set({ tone: 'error', text: problemMessage(error) });
      },
    });
  }

  protected canSubmit(profile: ProviderProfile): boolean {
    return profile.verificationStatus === 'DRAFT' || profile.verificationStatus === 'REJECTED';
  }

  private show(profile: ProviderProfile): void {
    this.profile.set(profile);
    this.form.reset({
      displayName: profile.displayName,
      city: profile.city,
      address: profile.address ?? '',
      yearsExperience: profile.yearsExperience,
      bio: profile.bio ?? '',
    });
  }
}
