import { DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError, Employee } from '../../models/employee';
import { EmployeeService } from '../../services/employee.service';

@Component({
  selector: 'app-employee-list',
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList implements OnInit {
  private readonly employeeService = inject(EmployeeService);

  employees: Employee[] = [];
  loading = false;
  errorMessage = '';
  searchQuery = '';
  departmentFilter = '';
  page = 0;
  size = 10;
  totalElements = 0;
  totalPages = 0;

  departments = ['Engineering', 'HR', 'Finance', 'Operations', 'Research', 'Sales', 'IT'];

  deleteTarget: Employee | null = null;
  deleting = false;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.employeeService
      .list({
        q: this.searchQuery,
        department: this.departmentFilter,
        page: this.page,
        size: this.size,
      })
      .subscribe({
        next: (result) => {
          this.employees = result.content;
          this.totalElements = result.totalElements;
          this.totalPages = result.totalPages;
          this.loading = false;
        },
        error: (err: ApiError) => {
          this.errorMessage = err.message || 'Failed to load employees';
          this.loading = false;
        },
      });
  }

  onSearch(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.departmentFilter = '';
    this.page = 0;
    this.load();
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page -= 1;
      this.load();
    }
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.page += 1;
      this.load();
    }
  }

  confirmDelete(employee: Employee): void {
    this.deleteTarget = employee;
  }

  cancelDelete(): void {
    this.deleteTarget = null;
    this.deleting = false;
  }

  performDelete(): void {
    if (!this.deleteTarget) {
      return;
    }
    this.deleting = true;
    this.employeeService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteTarget = null;
        this.load();
      },
      error: (err: ApiError) => {
        this.deleting = false;
        this.errorMessage = err.message || 'Failed to delete employee';
        this.deleteTarget = null;
      },
    });
  }
}
