import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse, Role, User } from './auth.models';
import { AuthStore } from './auth-store';

function authResponse(role: Role, expiresAt = '2999-01-01T00:00:00Z'): AuthResponse {
  return {
    accessToken: 'token-1',
    expiresAt,
    user: { id: 1, email: 'maria@example.com', firstName: 'Maria', lastName: 'Kirova', phone: null, city: 'Sofia', role },
  };
}

describe('AuthStore', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('starts and stores a session after login', () => {
    const store = TestBed.inject(AuthStore);
    const http = TestBed.inject(HttpTestingController);
    let signedIn: User | undefined;

    store.login({ email: 'maria@example.com', password: 'secret-123' }).subscribe((user) => (signedIn = user));
    http.expectOne('/api/auth/login').flush(authResponse('OWNER'));

    expect(signedIn?.firstName).toBe('Maria');
    expect(store.isSignedIn()).toBe(true);
    expect(store.token()).toBe('token-1');
    expect(localStorage.getItem('petpal.session')).toContain('token-1');
  });

  it('ignores an expired stored session', () => {
    localStorage.setItem('petpal.session', JSON.stringify(authResponse('OWNER', '2000-01-01T00:00:00Z')));

    const store = TestBed.inject(AuthStore);

    expect(store.isSignedIn()).toBe(false);
    expect(localStorage.getItem('petpal.session')).toBeNull();
  });

  it('restores a valid stored session and knows the home route of the role', () => {
    localStorage.setItem('petpal.session', JSON.stringify(authResponse('PROVIDER')));

    const store = TestBed.inject(AuthStore);

    expect(store.isSignedIn()).toBe(true);
    expect(store.hasRole('PROVIDER')).toBe(true);
    expect(store.homeRoute()).toBe('/workspace');
  });

  it('clears everything on sign out', () => {
    localStorage.setItem('petpal.session', JSON.stringify(authResponse('ADMIN')));
    const store = TestBed.inject(AuthStore);

    store.signOut();

    expect(store.isSignedIn()).toBe(false);
    expect(store.homeRoute()).toBe('/');
    expect(localStorage.getItem('petpal.session')).toBeNull();
  });
});
