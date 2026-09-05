import { useEffect, useState } from 'react'
import api from '../../api/client'
import { Button, Card, Input, Table } from '../../components/ui'

export default function Departments() {
  const [departments, setDepartments] = useState([])
  const [form, setForm] = useState({ department_name: '', department_code: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => api.get('/departments/').then(({ data }) => setDepartments(data.results))

  useEffect(() => {
    load()
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await api.post('/departments/', form)
      setForm({ department_name: '', department_code: '' })
      load()
    } catch (err) {
      setError(JSON.stringify(err.response?.data ?? { detail: 'Failed to create department.' }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-semibold text-ink">Departments</h1>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card title="Add Department">
          <form onSubmit={handleSubmit} className="space-y-3">
            <Input
              placeholder="Department name"
              required
              value={form.department_name}
              onChange={(e) => setForm({ ...form, department_name: e.target.value })}
            />
            <Input
              placeholder="Code (e.g. CS)"
              required
              value={form.department_code}
              onChange={(e) => setForm({ ...form, department_code: e.target.value })}
            />
            {error && <p className="break-words text-xs text-danger">{error}</p>}
            <Button type="submit" disabled={busy} className="w-full">
              {busy ? 'Saving…' : 'Add Department'}
            </Button>
          </form>
        </Card>
        <Card title={`All Departments (${departments.length})`} className="lg:col-span-2">
          <Table
            columns={[
              { key: 'department_code', header: 'Code' },
              { key: 'department_name', header: 'Name' },
            ]}
            rows={departments}
          />
        </Card>
      </div>
    </div>
  )
}
