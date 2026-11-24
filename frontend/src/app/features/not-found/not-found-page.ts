import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'pp-not-found-page',
  imports: [RouterLink],
  template: `
    <section class="container">
      <p class="overline">Page not found</p>
      <h1>We could not find this page.</h1>
      <p class="lead muted">The link may be old, or the page has moved.</p>
      <a class="btn" routerLink="/">Back to the home page</a>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundPage {}
