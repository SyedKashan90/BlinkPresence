import { useEffect, useState } from 'react'
import api from '../../api/client'
import { Button, Card, Select } from '../../components/ui'

async function downloadReport(url, filename) {
  const { data } = await api.get(url, { responseType: 'blob' })
  const blobUrl = window.URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(blobUrl)
}

export default function TeacherReports() {
  const [courses, setCourses] = useState([])
  const [courseId, setCourseId] = useState('')
  const [busy, setBusy] = useState('')

  useEffect(() => {
    api.get('/courses/').then(({ data }) => setCourses(data.results))
  }, [])

  const handleDownload = async (key, url, ext) => {
    setBusy(key)
    try {
      await downloadReport(url, `${key}.${ext}`)
    } catch {
      alert('Could not generate report.')
    } finally {
      setBusy('')
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-ink">Reports</h1>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card title="Course Attendance Report">
          <div className="space-y-3">
            <Select value={courseId} onChange={(e) => setCourseId(e.target.value)}>
              <option value="">Select a course</option>
              {courses.map((c) => (
                <option key={c.id} value={c.id}>{c.course_code} — {c.course_name}</option>
              ))}
            </Select>
            <div className="flex gap-2">
              <Button
                variant="ghost"
                className="flex-1"
                disabled={!courseId || busy}
                onClick={() => handleDownload('course-report-pdf', `/reports/course/${courseId}?export=pdf`, 'pdf')}
              >
                {busy === 'course-report-pdf' ? 'Generating…' : 'Download PDF'}
              </Button>
              <Button
                variant="ghost"
                className="flex-1"
                disabled={!courseId || busy}
                onClick={() => handleDownload('course-report-excel', `/reports/course/${courseId}?export=excel`, 'xlsx')}
              >
                {busy === 'course-report-excel' ? 'Generating…' : 'Download Excel'}
              </Button>
            </div>
          </div>
        </Card>

        <Card title="Daily Report (Today)">
          <p className="mb-3 text-sm text-slate-500">Attendance across all your courses for today.</p>
          <div className="flex gap-2">
            <Button variant="ghost" className="flex-1" disabled={busy} onClick={() => handleDownload('daily-report-pdf', '/reports/daily?export=pdf', 'pdf')}>
              {busy === 'daily-report-pdf' ? 'Generating…' : 'Download PDF'}
            </Button>
            <Button variant="ghost" className="flex-1" disabled={busy} onClick={() => handleDownload('daily-report-excel', '/reports/daily?export=excel', 'xlsx')}>
              {busy === 'daily-report-excel' ? 'Generating…' : 'Download Excel'}
            </Button>
          </div>
        </Card>

        <Card title="Weekly Report">
          <p className="mb-3 text-sm text-slate-500">Attendance for the current week.</p>
          <div className="flex gap-2">
            <Button variant="ghost" className="flex-1" disabled={busy} onClick={() => handleDownload('weekly-report-pdf', '/reports/weekly?export=pdf', 'pdf')}>
              PDF
            </Button>
            <Button variant="ghost" className="flex-1" disabled={busy} onClick={() => handleDownload('weekly-report-excel', '/reports/weekly?export=excel', 'xlsx')}>
              Excel
            </Button>
          </div>
        </Card>

        <Card title="Monthly Report">
          <p className="mb-3 text-sm text-slate-500">Attendance for the current month.</p>
          <div className="flex gap-2">
            <Button variant="ghost" className="flex-1" disabled={busy} onClick={() => handleDownload('monthly-report-pdf', '/reports/monthly?export=pdf', 'pdf')}>
              PDF
            </Button>
            <Button variant="ghost" className="flex-1" disabled={busy} onClick={() => handleDownload('monthly-report-excel', '/reports/monthly?export=excel', 'xlsx')}>
              Excel
            </Button>
          </div>
        </Card>
      </div>
    </div>
  )
}
