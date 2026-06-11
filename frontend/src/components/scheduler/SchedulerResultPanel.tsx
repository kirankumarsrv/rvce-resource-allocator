import {
  type SchedulerResult,
  type ScheduledSlot,
  ALL_DAYS,
  ALL_TIME_SLOTS,
  DAY_LABELS,
  TIME_SLOT_LABELS,
} from '@/types/scheduler'

interface Props {
  result: SchedulerResult
  daysInWeek: number
}

// ─── Summary cards ────────────────────────────────────────────────────────────

const SummaryCard = ({
  label,
  value,
  accent,
}: {
  label: string
  value: string | number
  accent?: 'green' | 'rose' | 'blue' | 'slate'
}) => {
  const colors = {
    green: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    rose: 'bg-rose-50 text-rose-700 border-rose-200',
    blue: 'bg-blue-50 text-blue-700 border-blue-200',
    slate: 'bg-slate-50 text-slate-700 border-slate-200',
  }
  return (
    <div className={`rounded-2xl border p-4 ${colors[accent ?? 'slate']}`}>
      <p className="text-xs font-semibold uppercase tracking-wide opacity-70">{label}</p>
      <p className="mt-1 text-2xl font-bold">{value}</p>
    </div>
  )
}

// ─── Timetable grid ───────────────────────────────────────────────────────────

const TimetableGrid = ({
  slots,
  daysInWeek,
}: {
  slots: ScheduledSlot[]
  daysInWeek: number
}) => {
  const activeDays = ALL_DAYS.slice(0, daysInWeek)

  const slotMap = new Map<string, ScheduledSlot[]>()
  for (const s of slots) {
    if (s.isLabSecondHour) continue // only render first hour of labs
    const key = `${s.day}__${s.timeSlot}`
    const existing = slotMap.get(key) ?? []
    slotMap.set(key, [...existing, s])
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full border-collapse text-xs">
        <thead>
          <tr>
            <th className="border border-slate-200 bg-slate-50 px-3 py-2 text-left text-xs font-semibold text-slate-500 w-24">
              Time
            </th>
            {activeDays.map((day) => (
              <th
                key={day}
                className="border border-slate-200 bg-slate-50 px-3 py-2 text-center font-semibold text-slate-700"
              >
                {DAY_LABELS[day]}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {ALL_TIME_SLOTS.map((ts) => (
            <tr key={ts} className="group">
              <td className="border border-slate-200 bg-slate-50 px-3 py-2 font-medium text-slate-500 whitespace-nowrap">
                {TIME_SLOT_LABELS[ts]}
              </td>
              {activeDays.map((day) => {
                const cellSlots = slotMap.get(`${day}__${ts}`) ?? []
                return (
                  <td
                    key={day}
                    className="border border-slate-100 px-2 py-1.5 align-top min-w-[120px]"
                  >
                    {cellSlots.length === 0 ? (
                      <span className="text-slate-200">—</span>
                    ) : (
                      <div className="space-y-1">
                        {cellSlots.map((slot, idx) => (
                          <div
                            key={idx}
                            className={`rounded-lg px-2 py-1.5 text-xs leading-tight ${
                              slot.subject.type === 'LAB'
                                ? 'bg-violet-50 border border-violet-200 text-violet-800'
                                : 'bg-blue-50 border border-blue-200 text-blue-800'
                            }`}
                          >
                            <p className="font-semibold truncate">{slot.subject.name}</p>
                            <p className="opacity-70 truncate">
                              {slot.subject.section} · {slot.room.name}
                            </p>
                            <p className="opacity-60 truncate">{slot.subject.teacherId}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </td>
                )
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// ─── Flat slot table ─────────────────────────────────────────────────────────

const SlotTable = ({ slots }: { slots: ScheduledSlot[] }) => {
  const visible = slots.filter((s) => !s.isLabSecondHour)
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full border-collapse text-sm">
        <thead>
          <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-500">
            <th className="px-4 py-3 text-left">Day</th>
            <th className="px-4 py-3 text-left">Time</th>
            <th className="px-4 py-3 text-left">Subject</th>
            <th className="px-4 py-3 text-left">Section</th>
            <th className="px-4 py-3 text-left">Type</th>
            <th className="px-4 py-3 text-left">Room</th>
            <th className="px-4 py-3 text-left">Teacher</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {visible.map((s, i) => (
            <tr key={i} className="hover:bg-slate-50 transition">
              <td className="px-4 py-2.5 font-medium text-slate-700">{DAY_LABELS[s.day]}</td>
              <td className="px-4 py-2.5 text-slate-600">{TIME_SLOT_LABELS[s.timeSlot]}</td>
              <td className="px-4 py-2.5 text-slate-900 font-medium">{s.subject.name}</td>
              <td className="px-4 py-2.5 text-slate-600">{s.subject.section}</td>
              <td className="px-4 py-2.5">
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                    s.subject.type === 'LAB'
                      ? 'bg-violet-100 text-violet-700'
                      : 'bg-blue-100 text-blue-700'
                  }`}
                >
                  {s.subject.type}
                </span>
              </td>
              <td className="px-4 py-2.5 font-mono text-xs text-slate-600">{s.room.name}</td>
              <td className="px-4 py-2.5 font-mono text-xs text-slate-600">{s.subject.teacherId}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// ─── Main component ──────────────────────────────────────────────────────────

export const SchedulerResultPanel = ({ result, daysInWeek }: Props) => {
  const unscheduledEntries = Object.entries(result.unscheduledHours ?? {}).filter(
    ([, hours]) => hours > 0
  )
  const teacherEntries = Object.entries(result.teacherLoadSummary ?? {})
  const totalSlots = result.scheduledSlots.filter((s) => !s.isLabSecondHour).length

  return (
    <div className="space-y-8">
      {/* Status banner */}
      <div
        className={`flex items-center gap-3 rounded-2xl px-5 py-4 border ${
          result.isFullyScheduled
            ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
            : 'bg-amber-50 border-amber-200 text-amber-800'
        }`}
      >
        {result.isFullyScheduled ? (
          <svg className="h-5 w-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 12l2 2 4-4" />
            <circle cx="12" cy="12" r="9" />
          </svg>
        ) : (
          <svg className="h-5 w-5 flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M12 9v4M12 17h.01" />
            <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
          </svg>
        )}
        <div>
          <p className="font-semibold">
            {result.isFullyScheduled
              ? 'Timetable fully scheduled'
              : 'Timetable partially scheduled — some hours could not be placed'}
          </p>
          <p className="text-sm opacity-80 mt-0.5">
            Results have been saved to the database.
          </p>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <SummaryCard
          label="Scheduled slots"
          value={totalSlots}
          accent="blue"
        />
        <SummaryCard
          label="Unscheduled subjects"
          value={unscheduledEntries.length}
          accent={unscheduledEntries.length > 0 ? 'rose' : 'green'}
        />
        <SummaryCard
          label="Teachers loaded"
          value={teacherEntries.length}
          accent="slate"
        />
        <SummaryCard
          label="Days/week"
          value={daysInWeek}
          accent="slate"
        />
      </div>

      {/* Unscheduled hours */}
      {unscheduledEntries.length > 0 && (
        <section>
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500 mb-3">
            Unscheduled Hours
          </h3>
          <div className="rounded-2xl border border-rose-100 bg-rose-50 overflow-hidden">
            <table className="min-w-full border-collapse text-sm">
              <thead>
                <tr className="border-b border-rose-100 text-xs font-semibold uppercase tracking-wide text-rose-500">
                  <th className="px-4 py-3 text-left">Subject ID</th>
                  <th className="px-4 py-3 text-right">Unscheduled Hours</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rose-100">
                {unscheduledEntries.map(([subjectId, hours]) => (
                  <tr key={subjectId}>
                    <td className="px-4 py-2.5 font-mono text-rose-700">{subjectId}</td>
                    <td className="px-4 py-2.5 text-right font-semibold text-rose-700">{hours}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Teacher load summary */}
      {teacherEntries.length > 0 && (
        <section>
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500 mb-3">
            Teacher Load Summary
          </h3>
          <div className="rounded-2xl border border-slate-200 overflow-hidden">
            <table className="min-w-full border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-500">
                  <th className="px-4 py-3 text-left">Teacher ID</th>
                  <th className="px-4 py-3 text-right">Total Hours</th>
                  <th className="px-4 py-3 text-right">Load bar</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {teacherEntries
                  .sort(([, a], [, b]) => b - a)
                  .map(([teacherId, hours]) => {
                    const max = Math.max(...teacherEntries.map(([, h]) => h))
                    const pct = max > 0 ? Math.round((hours / max) * 100) : 0
                    return (
                      <tr key={teacherId} className="hover:bg-slate-50">
                        <td className="px-4 py-2.5 font-mono text-slate-700">{teacherId}</td>
                        <td className="px-4 py-2.5 text-right font-semibold text-slate-700">{hours}</td>
                        <td className="px-4 py-2.5">
                          <div className="flex items-center justify-end gap-2">
                            <div className="w-24 h-2 bg-slate-100 rounded-full overflow-hidden">
                              <div
                                className="h-full bg-blue-500 rounded-full transition-all"
                                style={{ width: `${pct}%` }}
                              />
                            </div>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Timetable grid view */}
      {result.scheduledSlots.length > 0 && (
        <section>
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500 mb-3">
            Weekly Timetable Grid
          </h3>
          <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
            <TimetableGrid slots={result.scheduledSlots} daysInWeek={daysInWeek} />
          </div>
          <p className="mt-2 text-xs text-slate-400">
            <span className="inline-block w-3 h-3 rounded bg-blue-100 border border-blue-200 mr-1 align-middle" />Theory
            <span className="inline-block w-3 h-3 rounded bg-violet-100 border border-violet-200 ml-3 mr-1 align-middle" />Lab
          </p>
        </section>
      )}

      {/* Flat slot table */}
      {result.scheduledSlots.length > 0 && (
        <section>
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500 mb-3">
            All Scheduled Slots
          </h3>
          <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
            <SlotTable slots={result.scheduledSlots} />
          </div>
        </section>
      )}
    </div>
  )
}
