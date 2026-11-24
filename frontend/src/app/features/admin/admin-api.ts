import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { VerificationStatus } from '../../shared/models/enums';

export interface AdminProvider {
  id: number;
  displayName: string;
  bio: string | null;
  city: string;
  address: string | null;
  yearsExperience: number;
  verificationStatus: VerificationStatus;
  verificationNote: string | null;
  submittedAt: string | null;
  contactName: string;
  email: string;
  phone: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  providers(status: VerificationStatus): Observable<AdminProvider[]> {
    return this.http.get<AdminProvider[]>('/api/admin/providers', { params: new HttpParams().set('status', status) });
  }

  approve(id: number): Observable<AdminProvider> {
    return this.http.post<AdminProvider>(`/api/admin/providers/${id}/approve`, {});
  }

  reject(id: number, note: string): Observable<AdminProvider> {
    return this.http.post<AdminProvider>(`/api/admin/providers/${id}/reject`, { note });
  }
}
