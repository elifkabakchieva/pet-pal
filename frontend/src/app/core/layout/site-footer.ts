import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'pp-site-footer',
  template: `
    <footer class="site-footer">
      <div class="container">
        PetPal · Care for your pet from verified people nearby. · {{ year }}
      </div>
    </footer>
  `,
  styles: `
    .site-footer {
      padding: 1.25rem 0;
      border-top: 1px solid var(--line);
      color: var(--ink-soft);
      font-size: 0.875rem;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SiteFooter {
  protected readonly year = new Date().getFullYear();
}
