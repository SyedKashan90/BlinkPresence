import { useEffect, useRef, useState } from 'react'
import api from '../../api/client'
import { Badge, Button, Card, Select, Table } from '../../components/ui'

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://127.0.0.1:8000'

export default function TeacherSessions() {
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState('')
  const [session, setSession] = useState(null)
  const [countdown, setCountdown] = useState(0)
  const [attendees, setAttendees] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const wsRef = useRef(null)

  useEffect(() => {
    api.get('/courses/').then(({ data }) => setCourses(data.results))
    return () => wsRef.current?.close()
  }, [])

  // Countdown ticker
  useEffect(() => {
    if (!session) return undefined
    const interval = setInterval(() => {
      setCountdown((prev) => Math.max(0, prev - 1))
    }, 1000)
    return () => clearInterval(interval)
  }, [session])

  const connectSocket = (lectureId) => {
    wsRef.current?.close()
    const token = localStorage.getItem('access_token')
    const ws = new WebSocket(`${WS_BASE_URL}/ws/attendance/${lectureId}/?token=${token}`)
    ws.onmessage = (event) => {
      const payload = JSON.parse(event.data)
      setAttendees((prev) => [payload, ...prev])
    }
    wsRef.current = ws
  }

  const startSession = async () => {
    setError('')
    setBusy(true)
    try {
      const { data } = await api.post('/session/create', { course: courseId })
      setSession(data)
      setCountdown(data.expires_in_seconds)
      setAttendees([])
      connectSocket(data.lecture)
    } catch (err) {
      setError(err.response?.data?.detail || 'Failed to start session.')
    } finally {
      setBusy(false)
    }
  }

  const refreshQr = async () => {
    if (!session) return
    setBusy(true)
    try {
      const { data } = await api.post(`/sessions/${session.id}/refresh`)
      setSession(data)
      setCountdown(data.expires_in_seconds)
    } finally {
      setBusy(false)
    }
  }

  const endSession = async () => {
    if (!session) return
    setBusy(true)
    try {
      await api.post(`/sessions/${session.id}/end`)
      wsRef.current?.close()
      setSession(null)
      setCountdown(0)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-ink">Live Session</h1>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card title="Session Control">
          {!session ? (
            <div className="space-y-3">
              <Select value={courseId} onChange={(e) => setCourseId(e.target.value)}>
                <option value="">Select a course</option>
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>{c.course_code} — {c.course_name}</option>
                ))}
              </Select>
              {error && <p className="text-xs text-danger">{error}</p>}
              <Button className="w-full" disabled={!courseId || busy} onClick={startSession}>
                {busy ? 'Starting…' : 'Start Session'}
              </Button>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="flex items-center justify-center rounded-lg border border-slate-200 bg-slate-50 p-4">
                <img
                  src={`data:image/png;base64,${session.qr_image_base64}`}
                  alt="Attendance QR code"
                  className="h-48 w-48"
                />
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-500">Course</span>
                <Badge>{session.course_code}</Badge>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-500">Expires in</span>
                <Badge tone={countdown <= 30 ? 'danger' : 'success'}>{countdown}s</Badge>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" className="flex-1" disabled={busy} onClick={refreshQr}>
                  Refresh QR
                </Button>
                <Button variant="danger" className="flex-1" disabled={busy} onClick={endSession}>
                  End Session
                </Button>
              </div>
            </div>
          )}
        </Card>

        <Card title={`Live Attendance (${attendees.length})`} className="lg:col-span-2">
          {!session ? (
            <p className="py-8 text-center text-sm text-slate-400">Start a session to see students appear here in real time.</p>
          ) : (
            <Table
              columns={[
                { key: 'registration_number', header: 'Reg. No.' },
                { key: 'student_name', header: 'Name' },
                { key: 'verification_method', header: 'Method' },
                { key: 'marked_at', header: 'Marked At', render: (r) => new Date(r.marked_at).toLocaleTimeString() },
              ]}
              rows={attendees}
              empty="Waiting for students to scan…"
            />
          )}
        </Card>
      </div>
    </div>
  )
}
