import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth-guards';

export const routes: Routes = [
  {
    path: '',
    title: 'PetPal — pet care near you',
    loadComponent: () => import('./features/home/home-page').then((m) => m.HomePage),
  },
  {
    path: 'login',
    title: 'Sign in · PetPal',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'register',
    title: 'Create account · PetPal',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'providers',
    title: 'Find care · PetPal',
    loadComponent: () =>
      import('./features/discovery/provider-search-page').then((m) => m.ProviderSearchPage),
  },
  {
    path: 'providers/:id',
    title: 'Provider · PetPal',
    loadComponent: () =>
      import('./features/discovery/provider-detail-page').then((m) => m.ProviderDetailPage),
  },
  {
    path: 'pets',
    title: 'My pets · PetPal',
    canActivate: [roleGuard('OWNER')],
    loadComponent: () => import('./features/pets/pets-page').then((m) => m.PetsPage),
  },
  {
    path: 'bookings',
    title: 'Bookings · PetPal',
    canActivate: [roleGuard('OWNER', 'PROVIDER')],
    loadComponent: () => import('./features/bookings/bookings-page').then((m) => m.BookingsPage),
  },
  {
    path: 'bookings/:id',
    title: 'Booking · PetPal',
    canActivate: [roleGuard('OWNER', 'PROVIDER')],
    loadComponent: () => import('./features/bookings/booking-detail-page').then((m) => m.BookingDetailPage),
  },
  {
    path: 'notifications',
    title: 'Notifications · PetPal',
    canActivate: [authGuard],
    loadComponent: () => import('./features/notifications/notifications-page').then((m) => m.NotificationsPage),
  },
  {
    path: 'workspace',
    canActivate: [roleGuard('PROVIDER')],
    loadChildren: () => import('./features/workspace/workspace.routes').then((m) => m.WORKSPACE_ROUTES),
  },
  {
    path: 'admin/verification',
    title: 'Verification · PetPal',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/verification-page').then((m) => m.VerificationPage),
  },
  {
    path: '**',
    title: 'Not found · PetPal',
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
  },
];
