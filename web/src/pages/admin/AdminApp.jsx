import { Route, Routes } from 'react-router-dom'
import DashboardLayout from '../../components/DashboardLayout'
import AdminDashboard from './AdminDashboard'
import Departments from './Departments'
import Teachers from './Teachers'
import Students from './Students'
import Courses from './Courses'

const links = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/departments', label: 'Departments' },
  { to: '/admin/teachers', label: 'Teachers' },
  { to: '/admin/students', label: 'Students' },
  { to: '/admin/courses', label: 'Courses & Enrollment' },
]

export default function AdminApp() {
  return (
    <DashboardLayout title="Admin" links={links}>
      <Routes>
        <Route index element={<AdminDashboard />} />
        <Route path="departments" element={<Departments />} />
        <Route path="teachers" element={<Teachers />} />
        <Route path="students" element={<Students />} />
        <Route path="courses" element={<Courses />} />
      </Routes>
    </DashboardLayout>
  )
}
