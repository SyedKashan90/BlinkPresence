import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function DashboardLayout({ title, links, children }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const linkClass = ({ isActive }) =>
    `block rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
      isActive ? 'bg-primary text-white' : 'text-slate-600 hover:bg-slate-100'
    }`

  return (
    <div className="flex min-h-screen bg-surface">
      <aside className="flex w-64 shrink-0 flex-col border-r border-slate-200 bg-white">
        <div className="flex items-center gap-3 border-b border-slate-200 px-5 py-4">
          <img src="/logo.png" alt="Blink Presence Logo" className="h-10 w-10 object-contain rounded-lg shadow-sm" />
          <div>
            <p className="text-sm font-bold text-ink tracking-tight">Blink Presence</p>
            <p className="text-xs text-slate-400 font-medium">{title}</p>
          </div>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end} className={linkClass}>
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-200 p-4">
          <p className="truncate text-xs text-slate-500">{user?.email}</p>
          <button
            type="button"
            onClick={handleLogout}
            className="mt-2 w-full rounded-lg border border-slate-200 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50"
          >
            Log out
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-y-auto p-8">{children}</main>
    </div>
  )
}
