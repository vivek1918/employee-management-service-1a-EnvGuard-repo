export interface Employee {
  id: string;
  fullName: string;
  email: string;
  department: string;
  designation: string;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeRequest {
  fullName: string;
  email: string;
  department: string;
  designation: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiError {
  timestamp?: string;
  status: number;
  error?: string;
  message: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}
