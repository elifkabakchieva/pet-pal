import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { problemMessage } from '../../core/api/problem';
import { DAY_LABELS, timeLabel } from '../../shared/format/labels';
import { DAYS, DayOfWeek } from '../../shared/models/enums';
import { AvailabilityWindow } from '../../shared/models/provider';
import { WorkspaceApi } from './workspace-api';

interface EditableWindow extends AvailabilityWindow {
  key: number;
}

@Component({
  selector: 'pp-hours-page',
  templateUrl: './hours-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HoursPage implements OnInit {
  private readonly api = inject(WorkspaceApi);
  private nextKey = 1;

  protected readonly days = DAYS;
  protected readonly dayLabels = DAY_LABELS;
  protected readonly windows = signal<EditableWindow[]>([]);
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly message = signal<{ tone: 'success' | 'error'; text: string } | null>(null);

  ngOnInit(): void {
    this.api.hours().subscribe({
      next: (windows) => {
        this.setWindows(windows);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.message.set({ tone: 'error', text: problemMessage(error) });
        this.loading.set(false);
      },
    });
  }

  protected windowsOf(day: DayOfWeek): EditableWindow[] {
    return this.windows().filter((window) => window.dayOfWeek === day);
  }

  protected add(day: DayOfWeek): void {
    const last = this.windowsOf(day).at(-1);
    const startTime = last ? last.endTime : '09:00';
    const endTime = last ? plusMinutes(last.endTime, 60) : '17:00';
    this.windows.update((list) => [...list, { key: this.nextKey++, dayOfWeek: day, startTime, endTime }]);
  }

  protected remove(key: number): void {
    this.windows.update((list) => list.filter((window) => window.key !== key));
  }

  protected change(key: number, field: 'startTime' | 'endTime', event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.windows.update((list) => list.map((window) => (window.key === key ? { ...window, [field]: value } : window)));
  }

  protected save(): void {
    const invalid = this.windows().find((window) => !window.startTime || !window.endTime || window.endTime <= window.startTime);
    if (invalid) {
      this.message.set({
        tone: 'error',
        text: `Check ${DAY_LABELS[invalid.dayOfWeek]}: every time window needs an end after its start.`,
      });
      return;
    }
    this.saving.set(true);
    this.message.set(null);
    const payload = this.windows().map(({ dayOfWeek, startTime, endTime }) => ({ dayOfWeek, startTime, endTime }));
    this.api.saveHours(payload).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.setWindows(saved);
        this.message.set({ tone: 'success', text: 'Your weekly hours are saved.' });
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.message.set({ tone: 'error', text: problemMessage(error) });
      },
    });
  }

  private setWindows(windows: AvailabilityWindow[]): void {
    this.windows.set(
      windows.map((window) => ({
        key: this.nextKey++,
        dayOfWeek: window.dayOfWeek,
        startTime: timeLabel(window.startTime),
        endTime: timeLabel(window.endTime),
      })),
    );
  }
}

function plusMinutes(time: string, minutes: number): string {
  const [hours, mins] = time.split(':').map(Number);
  const total = Math.min(hours * 60 + mins + minutes, 23 * 60 + 45);
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}
