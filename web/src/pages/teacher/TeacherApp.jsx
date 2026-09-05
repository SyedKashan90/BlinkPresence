import { Route, Routes } from 'react-router-dom'
import DashboardLayout from '../../components/DashboardLayout'
import TeacherDashboard from './TeacherDashboard'
import TeacherSessions from './TeacherSessions'
import TeacherReports from './TeacherReports'

const links = [
  { to: '/teacher', label: 'Dashboard', end: true },
  { to: '/teacher/sessions', label: 'Live Session' },
  { to: '/teacher/reports', label: 'Reports' },
]

export default function TeacherApp() {
  return (
    <DashboardLayout title="Teacher" links={links}>
      <Routes>
        <Route index element={<TeacherDashboard />} />
        <Route path="sessions" element={<TeacherSessions />} />
        <Route path="reports" element={<TeacherReports />} />
      </Routes>
    </DashboardLayout>
  )
}
