import { Routes } from '@angular/router';
import { EmployeeDetail } from './pages/employee-detail/employee-detail';
import { EmployeeForm } from './pages/employee-form/employee-form';
import { EmployeeList } from './pages/employee-list/employee-list';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  { path: 'employees', component: EmployeeList },
  { path: 'employees/new', component: EmployeeForm },
  { path: 'employees/:id', component: EmployeeDetail },
  { path: 'employees/:id/edit', component: EmployeeForm },
  { path: '**', redirectTo: 'employees' },
];
