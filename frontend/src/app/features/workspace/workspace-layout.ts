import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'pp-workspace-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <section class="container">
      <div class="page-head">
        <div>
          <p class="overline">Workspace</p>
          <h1>Your business on PetPal</h1>
        </div>
      </div>
      <nav class="tabs" aria-label="Workspace">
        <a routerLink="profile" routerLinkActive="active">Profile</a>
        <a routerLink="services" routerLinkActive="active">Services</a>
        <a routerLink="hours" routerLinkActive="active">Weekly hours</a>
      </nav>
      <router-outlet />
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceLayout {}
