import { CategoryCode, VerificationStatus } from '../../shared/models/enums';

export interface ProviderProfile {
  id: number;
  displayName: string;
  bio: string | null;
  city: string;
  address: string | null;
  yearsExperience: number;
  verificationStatus: VerificationStatus;
  verificationNote: string | null;
  submittedAt: string | null;
  verifiedAt: string | null;
  ratingAverage: number;
  reviewCount: number;
}

export interface ProviderProfileRequest {
  displayName: string;
  bio: string | null;
  city: string;
  address: string | null;
  yearsExperience: number;
}

export interface OfferingRequest {
  categoryCode: CategoryCode;
  title: string;
  description: string | null;
  price: number;
  durationMinutes: number;
}
