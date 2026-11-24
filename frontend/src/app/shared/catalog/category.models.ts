import { CategoryCode } from '../models/enums';

export interface Category {
  code: CategoryCode;
  name: string;
  description: string;
}
