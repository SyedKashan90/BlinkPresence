import { useEffect, useState } from 'react'
import api from '../../api/client'
import { Button, Card, Input, PasswordInput, Select, Table } from '../../components/ui'

const initialForm = {
  email: '', password: '', registration_number: '', full_name: '', department: '', semester: 1, batch: '',
}

export default function Students() {
  const [students, setStudents] = useState([])
  const [departments, setDepartments] = useState([])
  const [form, setForm] = useState(initialForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  // Edit / Reset Modal State
  const [editingStudent, setEditingStudent] = useState(null)
  const [editForm, setEditForm] = useState({ full_name: '', email: '', registration_number: '', department: '', semester: 1, batch: '', phone: '' })
  const [newPassword, setNewPassword] = useState('')
  const [modalMsg, setModalMsg] = useState({ type: '', text: '' })
  const [modalBusy, setModalBusy] = useState(false)

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

  const openEditModal = (student) => {
    setEditingStudent(student)
    setEditForm({
      full_name: student.full_name || '',
      email: student.email || '',
      registration_number: student.registration_number || '',
      department: student.department || '',
      semester: student.semester || 1,
      batch: student.batch || '',
      phone: student.phone || '',
    })
    setNewPassword('')
    setModalMsg({ type: '', text: '' })
  }

  const handleUpdateDetails = async (e) => {
    e.preventDefault()
    setModalMsg({ type: '', text: '' })
    setModalBusy(true)
    try {
      await api.patch(`/students/${editingStudent.id}/`, editForm)
      setModalMsg({ type: 'success', text: 'Student details updated successfully!' })
      load()
    } catch (err) {
      const data = err.response?.data
      const msg = typeof data === 'string' ? data : (data?.detail || JSON.stringify(data || 'Failed to update details.'))
      setModalMsg({ type: 'error', text: msg })
    } finally {
      setModalBusy(false)
    }
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()
    if (!newPassword || newPassword.length < 8) {
      setModalMsg({ type: 'error', text: 'Password must be at least 8 characters long.' })
      return
    }
    setModalMsg({ type: '', text: '' })
    setModalBusy(true)
    try {
      await api.post(`/students/${editingStudent.id}/reset-password/`, { new_password: newPassword })
      setModalMsg({ type: 'success', text: 'Password reset successfully!' })
      setNewPassword('')
    } catch (err) {
      const data = err.response?.data
      const msg = typeof data === 'string' ? data : (data?.detail || data?.new_password?.[0] || 'Failed to reset password.')
      setModalMsg({ type: 'error', text: msg })
    } finally {
      setModalBusy(false)
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
            <PasswordInput placeholder="Temporary password" required minLength={8} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
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
              {
                key: 'full_name',
                header: 'Name',
                render: (r) => (
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => openEditModal(r)}
                      className="rounded bg-slate-100 p-1 text-slate-600 hover:bg-slate-200 hover:text-primary transition-colors"
                      title="Edit student profile or reset password"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                      </svg>
                    </button>
                    <span className="font-medium">{r.full_name}</span>
                  </div>
                ),
              },
              { key: 'email', header: 'Email' },
              { key: 'department_detail', header: 'Department', render: (r) => r.department_detail?.department_name },
              { key: 'semester', header: 'Sem.' },
              { key: 'batch', header: 'Batch' },
            ]}
            rows={students}
          />
        </Card>
      </div>

      {/* Edit & Password Reset Modal */}
      {editingStudent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4 overflow-y-auto">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl border border-slate-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
              <h3 className="text-lg font-semibold text-ink">Manage Student Account</h3>
              <button
                type="button"
                onClick={() => setEditingStudent(null)}
                className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
              >
                ✕
              </button>
            </div>

            {modalMsg.text && (
              <div className={`mb-4 rounded-lg p-3 text-xs font-medium ${modalMsg.type === 'error' ? 'bg-red-50 text-danger' : 'bg-green-50 text-emerald-700'}`}>
                {modalMsg.text}
              </div>
            )}

            <div className="space-y-6">
              {/* Profile details update section */}
              <form onSubmit={handleUpdateDetails} className="space-y-3">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">Account Profile & Credentials</h4>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">Full Name</label>
                  <Input required value={editForm.full_name} onChange={(e) => setEditForm({ ...editForm, full_name: e.target.value })} />
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">Email Address</label>
                  <Input type="email" required value={editForm.email} onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-600 mb-1">Reg Number</label>
                    <Input required value={editForm.registration_number} onChange={(e) => setEditForm({ ...editForm, registration_number: e.target.value })} />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-600 mb-1">Department</label>
                    <Select required value={editForm.department} onChange={(e) => setEditForm({ ...editForm, department: e.target.value })}>
                      <option value="">Select department</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.id}>{d.department_name}</option>
                      ))}
                    </Select>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-600 mb-1">Semester</label>
                    <Input type="number" min={1} max={12} required value={editForm.semester} onChange={(e) => setEditForm({ ...editForm, semester: Number(e.target.value) })} />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-600 mb-1">Batch</label>
                    <Input value={editForm.batch} onChange={(e) => setEditForm({ ...editForm, batch: e.target.value })} />
                  </div>
                </div>
                <Button type="submit" disabled={modalBusy} className="w-full">
                  {modalBusy ? 'Saving…' : 'Save Details'}
                </Button>
              </form>

              <hr className="border-slate-100" />

              {/* Reset Password section */}
              <form onSubmit={handleResetPassword} className="space-y-3">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">Admin Password Reset</h4>
                <p className="text-xs text-slate-500">Assign a new password if the student has forgotten theirs.</p>
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">New Password</label>
                  <PasswordInput
                    placeholder="Enter new password (min 8 chars)"
                    minLength={8}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                  />
                </div>
                <Button type="submit" variant="secondary" disabled={modalBusy} className="w-full">
                  {modalBusy ? 'Updating Password…' : 'Reset Password'}
                </Button>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

