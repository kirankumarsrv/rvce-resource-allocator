import type { SchedulerRoom, RoomType } from '@/types/scheduler'

interface Props {
  rooms: SchedulerRoom[]
  onChange: (rooms: SchedulerRoom[]) => void
}

const ROOM_TYPE_OPTIONS: RoomType[] = ['CLASSROOM', 'LAB']

const inputCls =
  'w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-100'

const Field = ({ label, children }: { label: string; children: React.ReactNode }) => (
  <div className="flex flex-col gap-1">
    <span className="text-xs font-medium text-slate-500 uppercase tracking-wide">{label}</span>
    {children}
  </div>
)

export const RoomTable = ({ rooms, onChange }: Props) => {
  const update = (index: number, patch: Partial<SchedulerRoom>) => {
    onChange(rooms.map((r, i) => (i === index ? { ...r, ...patch } : r)))
  }

  const remove = (index: number) => onChange(rooms.filter((_, i) => i !== index))

  const add = () =>
    onChange([
      ...rooms,
      { id: '', name: '', type: 'CLASSROOM' as RoomType, capacity: 60 },
    ])

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">
          Rooms{' '}
          <span className="ml-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-normal text-slate-500">
            {rooms.length}
          </span>
        </h3>
        <button
          type="button"
          onClick={add}
          className="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-slate-700"
        >
          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Add Room
        </button>
      </div>

      {rooms.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 py-8 text-center text-sm text-slate-400">
          No rooms yet — click Add Room to begin.
        </div>
      ) : (
        <div className="space-y-3">
          {rooms.map((r, i) => (
            <div
              key={i}
              className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
            >
              <div className="flex items-start justify-between mb-3">
                <span className="rounded-lg bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-500">
                  Room {i + 1}
                </span>
                <button
                  type="button"
                  onClick={() => remove(i)}
                  className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600 transition"
                  aria-label="Remove room"
                >
                  <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M18 6 6 18M6 6l12 12" />
                  </svg>
                </button>
              </div>

              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                <Field label="Room ID *">
                  <input
                    value={r.id}
                    onChange={(e) => update(i, { id: e.target.value })}
                    placeholder="e.g. CS-101"
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Room Name *">
                  <input
                    value={r.name}
                    onChange={(e) => update(i, { name: e.target.value })}
                    placeholder="e.g. CS-101"
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Type *">
                  <select
                    value={r.type}
                    onChange={(e) => update(i, { type: e.target.value as RoomType })}
                    className={inputCls}
                  >
                    {ROOM_TYPE_OPTIONS.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </Field>
                <Field label="Capacity *">
                  <input
                    type="number"
                    min={1}
                    value={r.capacity}
                    onChange={(e) => update(i, { capacity: Number(e.target.value) })}
                    className={inputCls}
                    required
                  />
                </Field>
                {r.type === 'LAB' && (
                  <Field label="Lab Type">
                    <input
                      value={r.labType ?? ''}
                      onChange={(e) => update(i, { labType: e.target.value })}
                      placeholder="e.g. NETWORKING"
                      className={inputCls}
                    />
                  </Field>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
