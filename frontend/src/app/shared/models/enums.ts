export type Species = 'DOG' | 'CAT' | 'BIRD' | 'RABBIT' | 'REPTILE' | 'OTHER';
export const SPECIES: readonly Species[] = ['DOG', 'CAT', 'BIRD', 'RABBIT', 'REPTILE', 'OTHER'];

export type CategoryCode = 'DAYCARE' | 'GROOMING' | 'VETERINARY' | 'WALKING' | 'TRAINING';
export const CATEGORY_CODES: readonly CategoryCode[] = ['DAYCARE', 'GROOMING', 'VETERINARY', 'WALKING', 'TRAINING'];

export type VerificationStatus = 'DRAFT' | 'PENDING' | 'VERIFIED' | 'REJECTED';

export type BookingStatus = 'REQUESTED' | 'CONFIRMED' | 'DECLINED' | 'CANCELLED' | 'COMPLETED';

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export const DAYS: readonly DayOfWeek[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
];
