import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Page } from '../../shared/models/page';
import { ProviderDetail, ProviderSearch, ProviderSummary, Review, Slot } from './discovery.models';

@Injectable({ providedIn: 'root' })
export class DiscoveryApi {
  private readonly http = inject(HttpClient);

  search(search: ProviderSearch, size = 12): Observable<Page<ProviderSummary>> {
    let params = new HttpParams().set('sort', search.sort).set('page', search.page).set('size', size);
    if (search.category) {
      params = params.set('category', search.category);
    }
    if (search.city) {
      params = params.set('city', search.city);
    }
    if (search.maxPrice !== undefined) {
      params = params.set('maxPrice', search.maxPrice);
    }
    if (search.minRating !== undefined) {
      params = params.set('minRating', search.minRating);
    }
    if (search.q) {
      params = params.set('q', search.q);
    }
    return this.http.get<Page<ProviderSummary>>('/api/providers', { params });
  }

  provider(id: number): Observable<ProviderDetail> {
    return this.http.get<ProviderDetail>(`/api/providers/${id}`);
  }

  reviews(providerId: number, page = 0): Observable<Page<Review>> {
    const params = new HttpParams().set('page', page).set('size', 5);
    return this.http.get<Page<Review>>(`/api/providers/${providerId}/reviews`, { params });
  }

  slots(providerId: number, offeringId: number, date: string): Observable<Slot[]> {
    const params = new HttpParams().set('offeringId', offeringId).set('date', date);
    return this.http.get<Slot[]>(`/api/providers/${providerId}/slots`, { params });
  }
}
