import { BookingStatus, CategoryCode, Species } from '../../shared/models/enums';

export type BookingAction = 'CONFIRM' | 'DECLINE' | 'CANCEL' | 'COMPLETE' | 'REVIEW';
export type BookingListSort = 'UPCOMING' | 'RECENT';

export interface Booking {
  id: number;
  status: BookingStatus;
  startAt: string;
  endAt: string;
  price: number;
  currency: string;
  ownerNotes: string | null;
  reviewed: boolean;
  service: { id: number; title: string; categoryCode: CategoryCode; durationMinutes: number };
  pet: { id: number; name: string; species: Species; breed: string | null; careNotes: string | null };
  provider: { id: number; displayName: string; city: string; address: string | null; phone: string | null };
  owner: { id: number; name: string; phone: string | null };
  actions: BookingAction[];
}

export interface TimelineEntry {
  fromStatus: BookingStatus | null;
  toStatus: BookingStatus;
  actorName: string;
  note: string | null;
  occurredAt: string;
}

export interface BookingDetail {
  booking: Booking;
  timeline: TimelineEntry[];
}

export interface CreateBookingRequest {
  offeringId: number;
  petId: number;
  startAt: string;
  notes: string | null;
}

export interface ReviewRequest {
  rating: number;
  comment: string | null;
}
