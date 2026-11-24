import { CategoryCode, DayOfWeek } from './enums';

export interface Offering {
  id: number;
  categoryCode: CategoryCode;
  categoryName: string;
  title: string;
  description: string | null;
  price: number;
  durationMinutes: number;
  active: boolean;
}

export interface AvailabilityWindow {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
}
