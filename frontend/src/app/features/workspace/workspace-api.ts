import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AvailabilityWindow, Offering } from '../../shared/models/provider';
import { OfferingRequest, ProviderProfile, ProviderProfileRequest } from './workspace.models';

@Injectable({ providedIn: 'root' })
export class WorkspaceApi {
  private readonly http = inject(HttpClient);

  profile(): Observable<ProviderProfile> {
    return this.http.get<ProviderProfile>('/api/provider/profile');
  }

  updateProfile(request: ProviderProfileRequest): Observable<ProviderProfile> {
    return this.http.put<ProviderProfile>('/api/provider/profile', request);
  }

  submitProfile(): Observable<ProviderProfile> {
    return this.http.post<ProviderProfile>('/api/provider/profile/submit', {});
  }

  services(): Observable<Offering[]> {
    return this.http.get<Offering[]>('/api/provider/services');
  }

  createService(request: OfferingRequest): Observable<Offering> {
    return this.http.post<Offering>('/api/provider/services', request);
  }

  updateService(id: number, request: OfferingRequest): Observable<Offering> {
    return this.http.put<Offering>(`/api/provider/services/${id}`, request);
  }

  deactivateService(id: number): Observable<void> {
    return this.http.delete<void>(`/api/provider/services/${id}`);
  }

  hours(): Observable<AvailabilityWindow[]> {
    return this.http.get<AvailabilityWindow[]>('/api/provider/availability');
  }

  saveHours(windows: AvailabilityWindow[]): Observable<AvailabilityWindow[]> {
    return this.http.put<AvailabilityWindow[]>('/api/provider/availability', { windows });
  }
}
