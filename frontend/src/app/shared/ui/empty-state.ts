import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'pp-empty-state',
  template: `
    <div class="empty">
      <h3>{{ title() }}</h3>
      <p class="muted">{{ text() }}</p>
      <ng-content />
    </div>
  `,
  styles: `
    .empty {
      padding: 2rem 1.25rem;
      border: 1px dashed var(--line-strong);
      border-radius: var(--radius);
      background: var(--surface);
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly text = input.required<string>();
}
