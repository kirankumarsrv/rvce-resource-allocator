import { useState } from 'react'
import { SubjectTable } from '@/components/scheduler/SubjectTable'
import { RoomTable } from '@/components/scheduler/RoomTable'
import { SchedulerResultPanel } from '@/components/scheduler/SchedulerResultPanel'
import { generateTimetable } from '@/services/schedulerService'
import type { SchedulerSubject, SchedulerRoom, SchedulerResult } from '@/types/scheduler'

type Tab = 'configure' | 'results'

const DEPT_OPTIONS = [
  'CSE', 'ISE', 'ECE', 'EEE', 'ME', 'CV', 'AE', 'CH', 'BT', 'MBA', 'MCA',
]

const inputCls =
  'rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-100'

const AdminSchedulerPage = () => {
  // ── Form state ──────────────────────────────────────────────────────────────
  const [department, setDepartment] = useState('CSE')
  const [daysInWeek, setDaysInWeek] = useState(5)
  const [subjects, setSubjects] = useState<SchedulerSubject[]>([])
  const [rooms, setRooms] = useState<SchedulerRoom[]>([])

  // ── Async state ─────────────────────────────────────────────────────────────
  const [isGenerating, setIsGenerating] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<SchedulerResult | null>(null)

  // ── UI state ─────────────────────────────────────────────────────────────────
  const [activeTab, setActiveTab] = useState<Tab>('configure')

  // ── Validation ───────────────────────────────────────────────────────────────
  const validate = (): string | null => {
    if (!department.trim()) return 'Department is required.'
    if (daysInWeek < 4 || daysInWeek > 6) return 'Days per week must be between 4 and 6.'
    if (subjects.length === 0) return 'Add at least one subject.'
    if (rooms.length === 0) return 'Add at least one room.'

    for (let i = 0; i < subjects.length; i++) {
      const s = subjects[i]
      if (!s.id.trim()) return `Subject ${i + 1}: ID is required.`
      if (!s.name.trim()) return `Subject ${i + 1}: Name is required.`
      if (!s.teacherId.trim()) return `Subject ${i + 1}: Teacher ID is required.`
      if (!s.year || s.year < 1) return `Subject ${i + 1}: Year is required.`
      if (!s.section.trim()) return `Subject ${i + 1}: Section is required.`
      if (!s.semester.trim()) return `Subject ${i + 1}: Semester is required.`
    }

    for (let i = 0; i < rooms.length; i++) {
      const r = rooms[i]
      if (!r.id.trim()) return `Room ${i + 1}: ID is required.`
      if (!r.name.trim()) return `Room ${i + 1}: Name is required.`
      if (!r.capacity || r.capacity < 1) return `Room ${i + 1}: Capacity must be at least 1.`
    }

    return null
  }

  // ── Submit ───────────────────────────────────────────────────────────────────
  const handleGenerate = async () => {
    setError(null)
    const validationError = validate()
    if (validationError) {
      setError(validationError)
      return
    }

    setIsGenerating(true)
    try {
      const payload = {
        department: department.trim(),
        subjects: subjects.map((s) => ({ ...s, department: department.trim() })),
        rooms,
        daysInWeek,
      }
      const res = await generateTimetable(payload)
      setResult(res)
      setActiveTab('results')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Generation failed. Please try again.')
    } finally {
      setIsGenerating(false)
    }
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Page header */}
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Timetable Scheduler</h1>
          <p className="mt-1 text-sm text-slate-500">
            Configure subjects and rooms, then generate a timetable for your department.
          </p>
        </div>
        {result && (
          <span
            className={`self-start sm:self-auto inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold border ${
              result.isFullyScheduled
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-amber-50 text-amber-700 border-amber-200'
            }`}
          >
            <span
              className={`inline-block h-2 w-2 rounded-full ${
                result.isFullyScheduled ? 'bg-emerald-500' : 'bg-amber-500'
              }`}
            />
            {result.isFullyScheduled ? 'Fully scheduled' : 'Partially scheduled'}
          </span>
        )}
      </div>

      {/* Tab bar */}
      <div className="flex border-b border-slate-200">
        {(['configure', 'results'] as Tab[]).map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => setActiveTab(tab)}
            className={`relative px-5 py-3 text-sm font-semibold capitalize transition ${
              activeTab === tab
                ? 'text-slate-900 after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-slate-900'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            {tab === 'results' && result ? (
              <span className="flex items-center gap-2">
                Results
                <span className="rounded-full bg-blue-100 px-1.5 py-0.5 text-xs font-semibold text-blue-700">
                  {result.scheduledSlots.filter((s) => !s.isLabSecondHour).length}
                </span>
              </span>
            ) : (
              tab.charAt(0).toUpperCase() + tab.slice(1)
            )}
          </button>
        ))}
      </div>

      {/* ── Configure tab ── */}
      {activeTab === 'configure' && (
        <div className="space-y-8">
          {/* Department & days card */}
          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500 mb-4">
              Department Settings
            </h2>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Department <span className="text-rose-500">*</span>
                </label>
                <div className="flex gap-2">
                  <select
                    value={DEPT_OPTIONS.includes(department) ? department : '__custom__'}
                    onChange={(e) => {
                      if (e.target.value !== '__custom__') setDepartment(e.target.value)
                    }}
                    className={`${inputCls} flex-1`}
                  >
                    {DEPT_OPTIONS.map((d) => (
                      <option key={d} value={d}>{d}</option>
                    ))}
                    <option value="__custom__">Other…</option>
                  </select>
                  <input
                    value={department}
                    onChange={(e) => setDepartment(e.target.value.toUpperCase())}
                    placeholder="or type code"
                    className={`${inputCls} w-28`}
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Days per week <span className="text-rose-500">*</span>
                </label>
                <div className="flex gap-2">
                  {[4, 5, 6].map((d) => (
                    <button
                      key={d}
                      type="button"
                      onClick={() => setDaysInWeek(d)}
                      className={`flex-1 rounded-2xl border py-3 text-sm font-semibold transition ${
                        daysInWeek === d
                          ? 'bg-slate-900 text-white border-slate-900'
                          : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      {d}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Subjects card */}
          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <SubjectTable
              subjects={subjects}
              department={department}
              onChange={setSubjects}
            />
          </div>

          {/* Rooms card */}
          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
            <RoomTable rooms={rooms} onChange={setRooms} />
          </div>

          {/* Error */}
          {error && (
            <div
              role="alert"
              className="flex items-start gap-3 rounded-2xl border border-rose-200 bg-rose-50 px-5 py-4 text-sm text-rose-700"
            >
              <svg className="h-5 w-5 flex-shrink-0 mt-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 8v4M12 16h.01" />
              </svg>
              <span>{error}</span>
            </div>
          )}

          {/* Generate button */}
          <div className="flex justify-end">
            <button
              type="button"
              onClick={handleGenerate}
              disabled={isGenerating}
              className="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-8 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isGenerating ? (
                <>
                  <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M21 12a9 9 0 1 1-6.219-8.56" />
                  </svg>
                  Generating…
                </>
              ) : (
                <>
                  <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                  Generate Timetable
                </>
              )}
            </button>
          </div>
        </div>
      )}

      {/* ── Results tab ── */}
      {activeTab === 'results' && (
        <div>
          {result ? (
              <SchedulerResultPanel result={result} daysInWeek={daysInWeek} department={department} />
          ) : (
            <div className="rounded-3xl border border-dashed border-slate-200 bg-slate-50 py-16 text-center">
              <svg className="mx-auto mb-4 h-10 w-10 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <path d="M16 2v4M8 2v4M3 10h18" />
              </svg>
              <p className="text-sm text-slate-500">No results yet.</p>
              <p className="mt-1 text-xs text-slate-400">
                Configure your department and click Generate Timetable.
              </p>
              <button
                type="button"
                onClick={() => setActiveTab('configure')}
                className="mt-4 rounded-2xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-white transition"
              >
                Go to Configure
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default AdminSchedulerPage
