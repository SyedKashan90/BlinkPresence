import { useEffect, useState } from 'react'
import api from '../../api/client'
import { StatTile, Card } from '../../components/ui'

export default function TeacherDashboard() {
  const [stats, setStats] = useState(null)

  useEffect(() => {
    api.get('/analytics/dashboard').then(({ data }) => setStats(data))
  }, [])

  return (
    <div>
      <h1 className="mb-1 text-2xl font-semibold text-ink">Teacher Overview</h1>
      <p className="mb-6 text-sm text-slate-500">Your courses at a glance.</p>

      {!stats ? (
        <p className="text-sm text-slate-400">Loading…</p>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
          <StatTile label="Active Courses" value={stats.active_courses} />
          <StatTile label="Today's Lectures" value={stats.todays_lectures} />
          <StatTile label="Avg. Attendance" value={`${stats.attendance_summary}%`} />
        </div>
      )}

      <Card title="Quick start" className="mt-6">
        <p className="text-sm text-slate-600">
          Head to <strong>Live Session</strong> to start a lecture, display the dynamic QR code, and watch
          attendance come in over WebSocket in real time.
        </p>
      </Card>
    </div>
  )
}
