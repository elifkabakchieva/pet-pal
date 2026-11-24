import { CategoryCode } from '../../shared/models/enums';
import { AvailabilityWindow, Offering } from '../../shared/models/provider';

export type ProviderSort = 'RATING' | 'REVIEWS' | 'EXPERIENCE';

export interface ProviderSummary {
  id: number;
  displayName: string;
  city: string;
  bioExcerpt: string | null;
  ratingAverage: number;
  reviewCount: number;
  yearsExperience: number;
  categories: CategoryCode[];
  startingPrice: number | null;
}

export interface ProviderDetail {
  id: number;
  displayName: string;
  bio: string | null;
  city: string;
  address: string | null;
  yearsExperience: number;
  ratingAverage: number;
  reviewCount: number;
  memberSince: string;
  currency: string;
  services: Offering[];
  weeklyHours: AvailabilityWindow[];
}

export interface Slot {
  startAt: string;
  endAt: string;
}

export interface ProviderSearch {
  category?: CategoryCode;
  city?: string;
  maxPrice?: number;
  minRating?: number;
  q?: string;
  sort: ProviderSort;
  page: number;
}

export interface Review {
  id: number;
  rating: number;
  comment: string | null;
  authorName: string;
  createdAt: string;
}
