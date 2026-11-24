import { TestBed } from '@angular/core/testing';
import { StatusBadge } from './status-badge';

describe('StatusBadge', () => {
  it.each([
    ['CONFIRMED', 'Confirmed', 'badge-pine'],
    ['REQUESTED', 'Requested', 'badge-clay'],
    ['REJECTED', 'Not verified', 'badge-berry'],
    ['COMPLETED', 'Completed', 'badge-moss'],
  ] as const)('renders %s as "%s" with %s', async (status, label, toneClass) => {
    const fixture = TestBed.createComponent(StatusBadge);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    await fixture.whenStable();

    const badge = (fixture.nativeElement as HTMLElement).querySelector('span');
    expect(badge?.textContent?.trim()).toBe(label);
    expect(badge?.classList.contains(toneClass)).toBe(true);
    expect(badge?.classList.contains('badge')).toBe(true);
  });
});
