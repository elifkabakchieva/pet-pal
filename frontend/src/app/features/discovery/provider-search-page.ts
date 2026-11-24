import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { problemMessage } from '../../core/api/problem';
import { CategoryApi } from '../../shared/catalog/category-api';
import { CATEGORY_LABELS, CURRENCY } from '../../shared/format/labels';
import { CategoryCode } from '../../shared/models/enums';
import { Page } from '../../shared/models/page';
import { EmptyState } from '../../shared/ui/empty-state';
import { RatingStars } from '../../shared/ui/rating-stars';
import { DiscoveryApi } from './discovery-api';
import { ProviderSearch, ProviderSort, ProviderSummary } from './discovery.models';

const SORTS: { value: ProviderSort; label: string }[] = [
  { value: 'RATING', label: 'Best rated' },
  { value: 'REVIEWS', label: 'Most reviews' },
  { value: 'EXPERIENCE', label: 'Most experience' },
];

function toNumber(value: string | undefined): number | undefined {
  if (value === undefined || value === '') {
    return undefined;
  }
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
}

@Component({
  selector: 'pp-provider-search-page',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, EmptyState, RatingStars],
  templateUrl: './provider-search-page.html',
  styleUrl: './provider-search-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderSearchPage {
  private readonly api = inject(DiscoveryApi);
  private readonly router = inject(Router);

  readonly category = input<string>();
  readonly city = input<string>();
  readonly maxPrice = input<string>();
  readonly minRating = input<string>();
  readonly q = input<string>();
  readonly sort = input<string>();
  readonly page = input<string>();

  protected readonly categories = toSignal(inject(CategoryApi).list(), { initialValue: [] });
  protected readonly categoryLabels = CATEGORY_LABELS;
  protected readonly currency = CURRENCY;
  protected readonly sorts = SORTS;

  protected readonly result = signal<Page<ProviderSummary> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    category: [''],
    city: [''],
    maxPrice: [''],
    minRating: [''],
    q: [''],
    sort: ['RATING'],
  });

  protected readonly search = computed<ProviderSearch>(() => ({
    category: (this.category() || undefined) as CategoryCode | undefined,
    city: this.city() || undefined,
    maxPrice: toNumber(this.maxPrice()),
    minRating: toNumber(this.minRating()),
    q: this.q() || undefined,
    sort: (this.sort() as ProviderSort | undefined) || 'RATING',
    page: toNumber(this.page()) ?? 0,
  }));

  constructor() {
    effect(() => {
      const search = this.search();
      untracked(() => {
        this.form.reset({
          category: search.category ?? '',
          city: search.city ?? '',
          maxPrice: search.maxPrice?.toString() ?? '',
          minRating: search.minRating?.toString() ?? '',
          q: search.q ?? '',
          sort: search.sort,
        });
        this.load(search);
      });
    });
  }

  protected apply(): void {
    const value = this.form.getRawValue();
    void this.router.navigate(['/providers'], {
      queryParams: {
        category: value.category || null,
        city: value.city.trim() || null,
        maxPrice: value.maxPrice || null,
        minRating: value.minRating || null,
        q: value.q.trim() || null,
        sort: value.sort === 'RATING' ? null : value.sort,
      },
    });
  }

  protected goToPage(page: number): void {
    void this.router.navigate([], { queryParams: { page: page > 0 ? page : null }, queryParamsHandling: 'merge' });
  }

  protected retry(): void {
    this.load(this.search());
  }

  private load(search: ProviderSearch): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.search(search).subscribe({
      next: (result) => {
        this.result.set(result);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(problemMessage(error));
        this.loading.set(false);
      },
    });
  }
}
