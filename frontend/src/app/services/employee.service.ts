import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ApiError,
  Employee,
  EmployeeRequest,
  PageResponse,
} from '../models/employee';

export interface EmployeeListParams {
  q?: string;
  department?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/employees`;

  list(params: EmployeeListParams = {}): Observable<PageResponse<Employee>> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 10));

    if (params.q?.trim()) {
      httpParams = httpParams.set('q', params.q.trim());
    }
    if (params.department?.trim()) {
      httpParams = httpParams.set('department', params.department.trim());
    }

    return this.http
      .get<PageResponse<Employee>>(this.baseUrl, { params: httpParams })
      .pipe(catchError((err) => this.handleError(err)));
  }

  getById(id: string): Observable<Employee> {
    return this.http
      .get<Employee>(`${this.baseUrl}/${id}`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  create(payload: EmployeeRequest): Observable<Employee> {
    return this.http
      .post<Employee>(this.baseUrl, payload)
      .pipe(catchError((err) => this.handleError(err)));
  }

  update(id: string, payload: EmployeeRequest): Observable<Employee> {
    return this.http
      .put<Employee>(`${this.baseUrl}/${id}`, payload)
      .pipe(catchError((err) => this.handleError(err)));
  }

  delete(id: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${id}`)
      .pipe(catchError((err) => this.handleError(err)));
  }

  private handleError(error: HttpErrorResponse) {
    const body = error.error as ApiError | undefined;
    const message =
      body?.message ||
      (typeof error.error === 'string' ? error.error : null) ||
      error.message ||
      'Unexpected error talking to the API';
    return throwError(() => ({
      status: error.status,
      message,
      fieldErrors: body?.fieldErrors,
    }) as ApiError);
  }
}
