import { Species } from '../../shared/models/enums';

export interface Pet {
  id: number;
  name: string;
  species: Species;
  breed: string | null;
  birthDate: string | null;
  weightKg: number | null;
  careNotes: string | null;
}

export type PetRequest = Omit<Pet, 'id'>;
