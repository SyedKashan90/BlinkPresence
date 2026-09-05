import { useEffect, useState } from 'react'
import api from '../../api/client'
import { StatTile, Card } from '../../components/ui'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api.get('/analytics/dashboard').then(({ data }) => setStats(data))
  }, [])

  return (
    <div>
      <h1 className="mb-1 text-2xl font-semibold text-ink">Admin Overview</h1>
      <p className="mb-6 text-sm text-slate-500">Institution-wide attendance snapshot.</p>

      {!stats ? (
        <p className="text-sm text-slate-400">Loading…</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          <StatTile label="Students" value={stats.total_students} />
          <StatTile label="Teachers" value={stats.total_teachers} />
          <StatTile label="Departments" value={stats.total_departments} />
          <StatTile label="Courses" value={stats.total_courses} />
          <StatTile label="Avg. Attendance" value={`${stats.overall_attendance_percentage}%`} />
          <StatTile label="Active Sessions" value={stats.active_sessions} />
        </div>
      )}

      <Card title="Getting started" className="mt-6">
        <ol className="list-decimal space-y-1 pl-5 text-sm text-slate-600">
          <li>Add departments, then teachers and students under them.</li>
          <li>Create courses and assign a teacher.</li>
          <li>Enroll students into courses.</li>
          <li>Teachers start lecture sessions from their dashboard to generate a live QR code.</li>
        </ol>
      </Card>
    </div>
  )
}
