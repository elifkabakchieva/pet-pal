import { Routes } from '@angular/router';
import { WorkspaceLayout } from './workspace-layout';

export const WORKSPACE_ROUTES: Routes = [
  {
    path: '',
    component: WorkspaceLayout,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'profile' },
      {
        path: 'profile',
        title: 'Profile · Workspace · PetPal',
        loadComponent: () => import('./profile-page').then((m) => m.ProfilePage),
      },
      {
        path: 'services',
        title: 'Services · Workspace · PetPal',
        loadComponent: () => import('./services-page').then((m) => m.ServicesPage),
      },
      {
        path: 'hours',
        title: 'Weekly hours · Workspace · PetPal',
        loadComponent: () => import('./hours-page').then((m) => m.HoursPage),
      },
    ],
  },
];
