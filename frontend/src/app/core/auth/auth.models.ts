export type Role = 'OWNER' | 'PROVIDER' | 'ADMIN';
export type AccountType = 'OWNER' | 'PROVIDER';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  city: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  expiresAt: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  city: string;
  accountType: AccountType;
}

export type Session = AuthResponse;
