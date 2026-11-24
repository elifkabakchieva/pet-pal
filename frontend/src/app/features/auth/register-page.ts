import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { applyServerErrors, problemMessage } from '../../core/api/problem';
import { AccountType } from '../../core/auth/auth.models';
import { AuthStore } from '../../core/auth/auth-store';

@Component({
  selector: 'pp-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPage implements OnInit {
  private readonly auth = inject(AuthStore);
  private readonly router = inject(Router);

  readonly type = input<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    accountType: ['OWNER' as AccountType, Validators.required],
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    phone: ['', [Validators.pattern(/^[+0-9 ()-]{6,30}$/)]],
  });
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    if (this.type() === 'PROVIDER') {
      this.form.controls.accountType.setValue('PROVIDER');
    }
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.pending.set(true);
    this.error.set(null);
    this.auth.register({ ...value, phone: value.phone.trim() || null }).subscribe({
      next: (user) => void this.router.navigateByUrl(user.role === 'OWNER' ? '/pets' : this.auth.homeRoute()),
      error: (error: unknown) => {
        this.pending.set(false);
        this.error.set(problemMessage(error));
        applyServerErrors(this.form, error);
      },
    });
  }
}
