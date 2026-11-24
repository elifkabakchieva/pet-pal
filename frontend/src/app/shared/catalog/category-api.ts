import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { Category } from './category.models';

@Injectable({ providedIn: 'root' })
export class CategoryApi {
  private readonly http = inject(HttpClient);
  private readonly categories$ = this.http
    .get<Category[]>('/api/categories')
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));

  list(): Observable<Category[]> {
    return this.categories$;
  }
}
