import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BookingStatus } from '../../shared/models/enums';
import { Page } from '../../shared/models/page';
import { Review } from '../discovery/discovery.models';
import { Booking, BookingDetail, BookingListSort, CreateBookingRequest, ReviewRequest } from './booking.models';

@Injectable({ providedIn: 'root' })
export class BookingApi {
  private readonly http = inject(HttpClient);

  request(request: CreateBookingRequest): Observable<Booking> {
    return this.http.post<Booking>('/api/bookings', request);
  }

  list(statuses: readonly BookingStatus[], sort: BookingListSort, page = 0): Observable<Page<Booking>> {
    let params = new HttpParams().set('sort', sort).set('page', page).set('size', 20);
    for (const status of statuses) {
      params = params.append('status', status);
    }
    return this.http.get<Page<Booking>>('/api/bookings', { params });
  }

  get(id: number): Observable<BookingDetail> {
    return this.http.get<BookingDetail>(`/api/bookings/${id}`);
  }

  confirm(id: number): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/confirm`, {});
  }

  decline(id: number, reason: string | null): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/decline`, { reason });
  }

  cancel(id: number, reason: string | null): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/cancel`, { reason });
  }

  complete(id: number): Observable<Booking> {
    return this.http.post<Booking>(`/api/bookings/${id}/complete`, {});
  }

  review(bookingId: number, request: ReviewRequest): Observable<Review> {
    return this.http.post<Review>(`/api/bookings/${bookingId}/review`, request);
  }
}
