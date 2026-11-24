import { BookingStatus, CategoryCode, DayOfWeek, Species, VerificationStatus } from '../models/enums';

export const CURRENCY = 'EUR';

export const SPECIES_LABELS: Record<Species, string> = {
  DOG: 'Dog',
  CAT: 'Cat',
  BIRD: 'Bird',
  RABBIT: 'Rabbit',
  REPTILE: 'Reptile',
  OTHER: 'Other',
};

export const CATEGORY_LABELS: Record<CategoryCode, string> = {
  DAYCARE: 'Daycare',
  GROOMING: 'Grooming',
  VETERINARY: 'Veterinary',
  WALKING: 'Walking',
  TRAINING: 'Training',
};

export const STATUS_LABELS: Record<VerificationStatus | BookingStatus, string> = {
  DRAFT: 'Draft',
  PENDING: 'Pending',
  VERIFIED: 'Verified',
  REJECTED: 'Not verified',
  REQUESTED: 'Requested',
  CONFIRMED: 'Confirmed',
  DECLINED: 'Declined',
  CANCELLED: 'Cancelled',
  COMPLETED: 'Completed',
};

export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday',
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday',
};

export function durationLabel(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest === 0 ? `${hours} h` : `${hours} h ${rest} min`;
}

export function timeLabel(time: string): string {
  return time.slice(0, 5);
}

export function ageLabel(birthDate: string | null, today: Date = new Date()): string | null {
  if (!birthDate) {
    return null;
  }
  const born = new Date(`${birthDate}T00:00:00`);
  let months = (today.getFullYear() - born.getFullYear()) * 12 + (today.getMonth() - born.getMonth());
  if (today.getDate() < born.getDate()) {
    months -= 1;
  }
  if (months >= 24) {
    return `${Math.floor(months / 12)} years`;
  }
  if (months >= 12) {
    return '1 year';
  }
  if (months >= 1) {
    return months === 1 ? '1 month' : `${months} months`;
  }
  const weeks = Math.max(0, Math.floor((today.getTime() - born.getTime()) / (7 * 24 * 3600 * 1000)));
  return weeks === 1 ? '1 week' : `${weeks} weeks`;
}

export const BOOKING_ACTION_LABELS: Record<'CONFIRM' | 'DECLINE' | 'CANCEL' | 'COMPLETE', string> = {
  CONFIRM: 'Confirm',
  DECLINE: 'Decline',
  CANCEL: 'Cancel booking',
  COMPLETE: 'Mark as completed',
};
