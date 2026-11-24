import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl } from '@angular/forms';

export interface FieldViolation {
  field: string;
  message: string;
}

export interface ProblemDetail {
  status: number;
  title?: string;
  detail?: string;
  code?: string;
  errors?: FieldViolation[];
}

export function problemOf(error: unknown): ProblemDetail {
  if (error instanceof HttpErrorResponse) {
    const body: unknown = error.error;
    if (body !== null && typeof body === 'object' && 'status' in body) {
      return body as ProblemDetail;
    }
    return { status: error.status };
  }
  return { status: -1 };
}

export function problemMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  const problem = problemOf(error);
  if (problem.detail) {
    return problem.detail;
  }
  if (problem.status === 0) {
    return 'PetPal cannot be reached right now. Check your connection and try again.';
  }
  if (problem.status === 403) {
    return 'You are not allowed to do this.';
  }
  return fallback;
}

export function applyServerErrors(form: AbstractControl, error: unknown): void {
  for (const violation of problemOf(error).errors ?? []) {
    const control = form.get(violation.field);
    if (control) {
      control.setErrors({ server: violation.message });
      control.markAsTouched();
    }
  }
}
