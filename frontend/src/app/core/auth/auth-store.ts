import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { AuthApi } from './auth-api';
import { AuthResponse, LoginRequest, RegisterRequest, Role, Session, User } from './auth.models';

const STORAGE_KEY = 'petpal.session';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApi);
  private readonly session = signal<Session | null>(readStoredSession());

  readonly user = computed<User | null>(() => this.session()?.user ?? null);
  readonly token = computed<string | null>(() => this.session()?.accessToken ?? null);
  readonly role = computed<Role | null>(() => this.user()?.role ?? null);
  readonly isSignedIn = computed(() => this.session() !== null);

  login(request: LoginRequest): Observable<User> {
    return this.api.login(request).pipe(
      tap((response) => this.start(response)),
      map((response) => response.user),
    );
  }

  register(request: RegisterRequest): Observable<User> {
    return this.api.register(request).pipe(
      tap((response) => this.start(response)),
      map((response) => response.user),
    );
  }

  signOut(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
    }
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  homeRoute(): string {
    switch (this.role()) {
      case 'OWNER':
        return '/providers';
      case 'PROVIDER':
        return '/workspace';
      case 'ADMIN':
        return '/admin/verification';
      default:
        return '/';
    }
  }

  private start(response: AuthResponse): void {
    this.session.set(response);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    } catch {
    }
  }
}

function readStoredSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    if (!session.accessToken || Date.parse(session.expiresAt) <= Date.now()) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}
