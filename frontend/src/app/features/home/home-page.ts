import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

interface HomeCategory {
  code: string;
  name: string;
  description: string;
}

const CATEGORIES: readonly HomeCategory[] = [
  { code: 'DAYCARE', name: 'Daycare', description: 'Supervised care and play while you are at work or away for the day.' },
  { code: 'GROOMING', name: 'Grooming', description: 'Bathing, brushing, trimming, nail clipping and coat care.' },
  { code: 'VETERINARY', name: 'Veterinary', description: 'Check-ups, vaccinations and advice from licensed veterinarians.' },
  { code: 'WALKING', name: 'Walking', description: 'Regular walks and outdoor exercise, alone or in small groups.' },
  { code: 'TRAINING', name: 'Training', description: 'Obedience, behaviour and socialisation sessions with a trainer.' },
];

@Component({
  selector: 'pp-home-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomePage {
  private readonly router = inject(Router);

  protected readonly categories = CATEGORIES;
  protected readonly form = inject(NonNullableFormBuilder).group({
    category: [''],
    city: [''],
  });

  protected search(): void {
    const { category, city } = this.form.getRawValue();
    void this.router.navigate(['/providers'], {
      queryParams: { category: category || null, city: city.trim() || null },
    });
  }
}
