import { useEffect, useState } from 'react'
import api from '../../api/client'
import { Button, Card, Input, Select, Table } from '../../components/ui'

const initialForm = { email: '', password: '', employee_id: '', full_name: '', department: '', designation: '' }

export default function Teachers() {
  const [teachers, setTeachers] = useState([])
  const [departments, setDepartments] = useState([])
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => api.get('/teachers/').then(({ data }) => setTeachers(data.results))

  useEffect(() => {
    load()
    api.get('/departments/').then(({ data }) => setDepartments(data.results))
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api.post('/teachers/', form)
      setForm(initialForm)
      load()
    } catch (err) {
      setError(JSON.stringify(err.response?.data ?? { detail: 'Failed to create teacher.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-ink">Teachers</h1>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card title="Add Teacher">
          <form onSubmit={handleSubmit} className="space-y-3">
            <Input placeholder="Full name" required value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} />
            <Input placeholder="Employee ID" required value={form.employee_id} onChange={(e) => setForm({ ...form, employee_id: e.target.value })} />
            <Input type="email" placeholder="Email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            <Input type="password" placeholder="Temporary password" required minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
            <Select required value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })}>
              <option value="">Select department</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.department_name}</option>
              ))}
            </Select>
            <Input placeholder="Designation (e.g. Lecturer)" value={form.designation} onChange={(e) => setForm({ ...form, designation: e.target.value })} />
            {error && <p className="break-words text-xs text-danger">{error}</p>}
            <Button type="submit" disabled={busy} className="w-full">
              {busy ? 'Saving…' : 'Add Teacher'}
            </Button>
          </form>
        </Card>
        <Card title={`All Teachers (${teachers.length})`} className="lg:col-span-2">
          <Table
            columns={[
              { key: 'employee_id', header: 'ID' },
              { key: 'full_name', header: 'Name' },
              { key: 'email', header: 'Email' },
              { key: 'department_detail', header: 'Department', render: (r) => r.department_detail?.department_name },
              { key: 'designation', header: 'Designation' },
            ]}
            rows={teachers}
          />
        </Card>
      </div>
    </div>
  )
}
