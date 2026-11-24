import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'pp-rating-stars',
  imports: [DecimalPipe],
  template: `
    @if (count() > 0) {
      <span class="rating">
        <span class="star" aria-hidden="true">★</span>
        <span class="visually-hidden">Rated</span>
        {{ rating() | number: '1.1-1' }}
        <span class="muted">· {{ count() }} {{ count() === 1 ? 'review' : 'reviews' }}</span>
      </span>
    } @else {
      <span class="muted">New on PetPal</span>
    }
  `,
  styles: `
    .rating {
      white-space: nowrap;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RatingStars {
  readonly rating = input.required<number>();
  readonly count = input.required<number>();
}
