import { Component, OnInit, inject } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError } from '../../models/employee';
import { EmployeeService } from '../../services/employee.service';

@Component({
  selector: 'app-employee-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './employee-form.html',
  styleUrl: './employee-form.scss',
})
export class EmployeeForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);

  employeeId: string | null = null;
  loading = false;
  saving = false;
  errorMessage = '';
  fieldErrors: Record<string, string> = {};

  departments = ['Engineering', 'HR', 'Finance', 'Operations', 'Research', 'Sales', 'IT'];

  form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    department: ['', [Validators.required, Validators.maxLength(100)]],
    designation: ['', [Validators.required, Validators.maxLength(100)]],
  });

  get isEdit(): boolean {
    return !!this.employeeId;
  }

  ngOnInit(): void {
    this.employeeId = this.route.snapshot.paramMap.get('id');
    if (this.employeeId) {
      this.loadEmployee(this.employeeId);
    }
  }

  loadEmployee(id: string): void {
    this.loading = true;
    this.employeeService.getById(id).subscribe({
      next: (employee) => {
        this.form.patchValue({
          fullName: employee.fullName,
          email: employee.email,
          department: employee.department,
          designation: employee.designation,
        });
        this.loading = false;
      },
      error: (err: ApiError) => {
        this.errorMessage = err.message || 'Failed to load employee';
        this.loading = false;
      },
    });
  }

  submit(): void {
    this.errorMessage = '';
    this.fieldErrors = {};
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.getRawValue();
    this.saving = true;

    const request$ = this.employeeId
      ? this.employeeService.update(this.employeeId, payload)
      : this.employeeService.create(payload);

    request$.subscribe({
      next: (employee) => {
        this.saving = false;
        void this.router.navigate(['/employees', employee.id]);
      },
      error: (err: ApiError) => {
        this.saving = false;
        this.errorMessage = err.message || 'Save failed';
        this.fieldErrors = err.fieldErrors || {};
      },
    });
  }

  controlError(name: keyof typeof this.form.controls): string | null {
    if (this.fieldErrors[name]) {
      return this.fieldErrors[name];
    }
    const control = this.form.controls[name];
    if (!control.touched || !control.errors) {
      return null;
    }
    if (control.errors['required']) {
      return 'This field is required';
    }
    if (control.errors['email']) {
      return 'Enter a valid email';
    }
    if (control.errors['maxlength']) {
      return 'Value is too long';
    }
    return 'Invalid value';
  }
}
