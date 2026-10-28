import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiError, Employee } from '../../models/employee';
import { EmployeeService } from '../../services/employee.service';

@Component({
  selector: 'app-employee-detail',
  imports: [RouterLink, DatePipe],
  templateUrl: './employee-detail.html',
  styleUrl: './employee-detail.scss',
})
export class EmployeeDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employeeService = inject(EmployeeService);

  employee: Employee | null = null;
  loading = false;
  errorMessage = '';
  showDeleteConfirm = false;
  deleting = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.errorMessage = 'Missing employee id';
      return;
    }
    this.load(id);
  }

  load(id: string): void {
    this.loading = true;
    this.errorMessage = '';
    this.employeeService.getById(id).subscribe({
      next: (employee) => {
        this.employee = employee;
        this.loading = false;
      },
      error: (err: ApiError) => {
        this.errorMessage = err.message || 'Failed to load employee';
        this.loading = false;
      },
    });
  }

  confirmDelete(): void {
    this.showDeleteConfirm = true;
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
  }

  performDelete(): void {
    if (!this.employee) {
      return;
    }
    this.deleting = true;
    this.employeeService.delete(this.employee.id).subscribe({
      next: () => {
        this.deleting = false;
        void this.router.navigate(['/employees']);
      },
      error: (err: ApiError) => {
        this.deleting = false;
        this.showDeleteConfirm = false;
        this.errorMessage = err.message || 'Failed to delete employee';
      },
    });
  }
}
