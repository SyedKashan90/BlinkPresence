import { useEffect, useState } from 'react'
import api from '../../api/client'
import { Button, Card, Input, Select, Table } from '../../components/ui'

const initialCourseForm = { course_code: '', course_name: '', credit_hours: 3, department: '', teacher: '', semester: 1 }

export default function Courses() {
  const [courses, setCourses] = useState([])
  const [departments, setDepartments] = useState([])
  const [teachers, setTeachers] = useState([])
  const [students, setStudents] = useState([])
  const [form, setForm] = useState(initialCourseForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [enrollForm, setEnrollForm] = useState({ student: '', course: '' })
  const [enrollMsg, setEnrollMsg] = useState('')

  const load = () => api.get('/courses/').then(({ data }) => setCourses(data.results))

  useEffect(() => {
    load()
    api.get('/departments/').then(({ data }) => setDepartments(data.results))
    api.get('/teachers/').then(({ data }) => setTeachers(data.results))
    api.get('/students/').then(({ data }) => setStudents(data.results))
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api.post('/courses/', form)
      setForm(initialCourseForm)
      load()
    } catch (err) {
      setError(JSON.stringify(err.response?.data ?? { detail: 'Failed to create course.' }))
    } finally {
      setBusy(false)
    }
  }

  const handleEnroll = async (e) => {
    e.preventDefault()
    setEnrollMsg('')
    try {
      await api.post('/enrollments/', enrollForm)
      setEnrollMsg('Enrolled successfully.')
      setEnrollForm({ student: '', course: '' })
    } catch (err) {
      const data = err.response?.data
      if (!data) {
        setEnrollMsg('Enrollment failed. Check network connection.')
      } else if (typeof data === 'string') {
        setEnrollMsg(data)
      } else if (data.non_field_errors?.[0]) {
        setEnrollMsg(data.non_field_errors[0])
      } else if (data.detail) {
        setEnrollMsg(data.detail)
      } else {
        const msgs = Object.entries(data)
          .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
          .join(' | ')
        setEnrollMsg(msgs || 'Enrollment failed.')
      }
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-ink">Courses</h1>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card title="Add Course">
          <form onSubmit={handleSubmit} className="space-y-3">
            <Input placeholder="Course code (e.g. CS101)" required value={form.course_code} onChange={(e) => setForm({ ...form, course_code: e.target.value })} />
            <Input placeholder="Course name" required value={form.course_name} onChange={(e) => setForm({ ...form, course_name: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input type="number" min={1} max={6} placeholder="Credit hours" required value={form.credit_hours} onChange={(e) => setForm({ ...form, credit_hours: Number(e.target.value) })} />
              <Input type="number" min={1} max={12} placeholder="Semester" required value={form.semester} onChange={(e) => setForm({ ...form, semester: Number(e.target.value) })} />
            </div>
            <Select required value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })}>
              <option value="">Select department</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.department_name}</option>
              ))}
            </Select>
            <Select required value={form.teacher} onChange={(e) => setForm({ ...form, teacher: e.target.value })}>
              <option value="">Assign teacher</option>
              {teachers.map((t) => (
                <option key={t.id} value={t.id}>{t.full_name}</option>
              ))}
            </Select>
            {error && <p className="break-words text-xs text-danger">{error}</p>}
            <Button type="submit" disabled={busy} className="w-full">
              {busy ? 'Saving…' : 'Add Course'}
            </Button>
          </form>
        </Card>

        <Card title={`All Courses (${courses.length})`} className="lg:col-span-2">
          <Table
            columns={[
              { key: 'course_code', header: 'Code' },
              { key: 'course_name', header: 'Name' },
              { key: 'teacher_detail', header: 'Teacher', render: (r) => r.teacher_detail?.full_name || '—' },
              { key: 'department_detail', header: 'Department', render: (r) => r.department_detail?.department_name },
              { key: 'semester', header: 'Sem.' },
            ]}
            rows={courses}
          />
        </Card>

        <Card title="Enroll Student in Course" className="lg:col-span-3">
          <form onSubmit={handleEnroll} className="flex flex-col gap-3 md:flex-row md:items-end">
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-slate-500">Student</label>
              <Select required value={enrollForm.student} onChange={(e) => setEnrollForm({ ...enrollForm, student: e.target.value })}>
                <option value="">Select student</option>
                {students.map((s) => (
                  <option key={s.id} value={s.id}>{s.registration_number} — {s.full_name}</option>
                ))}
              </Select>
            </div>
            <div className="flex-1">
              <label className="mb-1 block text-xs font-medium text-slate-500">Course</label>
              <Select required value={enrollForm.course} onChange={(e) => setEnrollForm({ ...enrollForm, course: e.target.value })}>
                <option value="">Select course</option>
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>{c.course_code} — {c.course_name}</option>
                ))}
              </Select>
            </div>
            <Button type="submit">Enroll</Button>
          </form>
          {enrollMsg && <p className="mt-2 text-sm text-slate-500">{enrollMsg}</p>}
        </Card>
      </div>
    </div>
  )
}
