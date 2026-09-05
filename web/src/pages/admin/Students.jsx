import { useEffect, useState } from 'react'
import api from '../../api/client'
import { Button, Card, Input, Select, Table } from '../../components/ui'

const initialForm = {
  email: '', password: '', registration_number: '', full_name: '', department: '', semester: 1, batch: '',
}

export default function Students() {
  const [students, setStudents] = useState([])
  const [departments, setDepartments] = useState([])
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => api.get('/students/').then(({ data }) => setStudents(data.results))

  useEffect(() => {
    load()
    api.get('/departments/').then(({ data }) => setDepartments(data.results))
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api.post('/students/', form)
      setForm(initialForm)
      load()
    } catch (err) {
      const data = err.response?.data
      if (!data) {
        setError('Failed to create student. Check connection.')
      } else if (typeof data === 'string') {
        setError(data)
      } else if (data.detail) {
        setError(data.detail)
      } else {
        const msgs = Object.entries(data)
          .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(', ') : v}`)
          .join(' | ')
        setError(msgs || 'Failed to create student.')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-ink">Students</h1>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card title="Add Student">
          <form onSubmit={handleSubmit} className="space-y-3">
            <Input placeholder="Full name" required value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} />
            <Input placeholder="Registration number" required value={form.registration_number} onChange={(e) => setForm({ ...form, registration_number: e.target.value })} />
            <Input type="email" placeholder="Email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            <Input type="password" placeholder="Temporary password" required minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            <Select required value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })}>
              <option value="">Select department</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.department_name}</option>
              ))}
            </Select>
            <div className="grid grid-cols-2 gap-3">
              <Input type="number" min={1} max={12} placeholder="Semester" required value={form.semester} onChange={(e) => setForm({ ...form, semester: Number(e.target.value) })} />
              <Input placeholder="Batch (e.g. 2022)" value={form.batch} onChange={(e) => setForm({ ...form, batch: e.target.value })} />
            </div>
            {error && <p className="break-words text-xs text-danger">{error}</p>}
            <Button type="submit" disabled={busy} className="w-full">
              {busy ? 'Saving…' : 'Add Student'}
            </Button>
          </form>
        </Card>
        <Card title={`All Students (${students.length})`} className="lg:col-span-2">
          <Table
            columns={[
              { key: 'registration_number', header: 'Reg. No.' },
              { key: 'full_name', header: 'Name' },
              { key: 'email', header: 'Email' },
              { key: 'department_detail', header: 'Department', render: (r) => r.department_detail?.department_name },
              { key: 'semester', header: 'Sem.' },
              { key: 'batch', header: 'Batch' },
            ]}
            rows={students}
          />
        </Card>
      </div>
    </div>
  )
}
