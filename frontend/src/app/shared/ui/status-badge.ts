import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { STATUS_LABELS } from '../format/labels';
import { BookingStatus, VerificationStatus } from '../models/enums';

type Tone = 'clay' | 'pine' | 'moss' | 'berry' | 'stone';

const TONES: Record<VerificationStatus | BookingStatus, Tone> = {
  DRAFT: 'stone',
  PENDING: 'clay',
  REQUESTED: 'clay',
  VERIFIED: 'pine',
  CONFIRMED: 'pine',
  COMPLETED: 'moss',
  REJECTED: 'berry',
  DECLINED: 'berry',
  CANCELLED: 'berry',
};

@Component({
  selector: 'pp-status-badge',
  template: `<span [class]="'badge badge-' + tone()">{{ label() }}</span>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusBadge {
  readonly status = input.required<VerificationStatus | BookingStatus>();

  protected readonly tone = computed(() => TONES[this.status()]);
  protected readonly label = computed(() => STATUS_LABELS[this.status()]);
}
