import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pet, PetRequest } from './pet.models';

@Injectable({ providedIn: 'root' })
export class PetApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Pet[]> {
    return this.http.get<Pet[]>('/api/pets');
  }

  create(request: PetRequest): Observable<Pet> {
    return this.http.post<Pet>('/api/pets', request);
  }

  update(id: number, request: PetRequest): Observable<Pet> {
    return this.http.put<Pet>(`/api/pets/${id}`, request);
  }

  archive(id: number): Observable<void> {
    return this.http.delete<void>(`/api/pets/${id}`);
  }
}
