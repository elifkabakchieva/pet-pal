export type NotificationType =
  | 'BOOKING_REQUESTED'
  | 'BOOKING_CONFIRMED'
  | 'BOOKING_DECLINED'
  | 'BOOKING_CANCELLED'
  | 'BOOKING_EXPIRED'
  | 'BOOKING_COMPLETED'
  | 'PROVIDER_VERIFIED'
  | 'PROVIDER_REJECTED'
  | 'REVIEW_RECEIVED';

export interface AppNotification {
  id: number;
  type: NotificationType;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}
