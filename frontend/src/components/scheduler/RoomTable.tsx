import { useEffect, useRef, useState } from 'react'
import type { SchedulerRoom } from '@/types/scheduler'
import { authenticatedFetch } from '@/services/authService'

interface RoomOption {
  id: string
  name: string
  type: 'CLASSROOM' | 'LAB'
  capacity: number
  labType?: string
}

let _roomCache: RoomOption[] | null = null

const useRooms = () => {
  const [rooms, setRooms] = useState<RoomOption[]>(_roomCache ?? [])
  const [loading, setLoading] = useState(!_roomCache)
  useEffect(() => {
    if (_roomCache) return
    authenticatedFetch('/api/rooms')
      .then((r) => r.json())
      .then((data: RoomOption[]) => { _roomCache = data; setRooms(data) })
      .finally(() => setLoading(false))
  }, [])
  return { rooms, loading }
}

const RoomSelect = ({
  onAdd,
}: {
  onAdd: (room: SchedulerRoom) => void
}) => {
  const { rooms, loading } = useRooms()
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  const filtered = query
    ? rooms.filter((r) =>
        r.name.toLowerCase().includes(query.toLowerCase()) ||
        r.id.toLowerCase().includes(query.toLowerCase()) ||
        (r.labType ?? '').toLowerCase().includes(query.toLowerCase())
      )
    : rooms

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 transition"
      >
        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
          <path d="M12 5v14M5 12h14" />
        </svg>
        {loading ? 'Loading rooms…' : 'Add Room'}
      </button>

      {open && (
        <div className="absolute z-50 mt-1 left-0 w-80 rounded-xl border border-slate-200 bg-white shadow-lg overflow-hidden">
          <div className="p-2 border-b border-slate-100">
            <input
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search room name or lab type…"
              className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm outline-none focus:border-slate-400"
            />
          </div>
          <div className="max-h-64 overflow-y-auto divide-y divide-slate-50">
            {filtered.length === 0 ? (
              <p className="px-3 py-4 text-sm text-slate-400 text-center">No rooms match</p>
            ) : (
              filtered.map((r) => (
                <button
                  key={r.id}
                  type="button"
                  onClick={() => {
                    onAdd({
                      id: r.id,
                      name: r.name,
                      type: r.type,
                      capacity: r.capacity,
                      labType: r.labType,
                    })
                    setOpen(false)
                    setQuery('')
                  }}
                  className="w-full text-left px-3 py-2.5 text-sm hover:bg-slate-50 transition flex items-center justify-between gap-2"
                >
                  <div>
                    <span className="font-medium text-slate-800">{r.id}</span>
                    <span className="ml-2 text-slate-500 text-xs">{r.name}</span>
                    {r.labType && (
                      <span className="ml-2 rounded-full bg-violet-100 text-violet-700 px-1.5 py-0.5 text-xs font-semibold">
                        {r.labType}
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-slate-400 shrink-0">{r.capacity} seats</span>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}

interface Props {
  rooms: SchedulerRoom[]
  onChange: (rooms: SchedulerRoom[]) => void
}

export const RoomTable = ({ rooms, onChange }: Props) => {
  const remove = (id: string) => onChange(rooms.filter((r) => r.id !== id))
  const add = (room: SchedulerRoom) => {
    if (rooms.find((r) => r.id === room.id)) return  // no duplicates
    onChange([...rooms, room])
  }

  return (
    <div className="space-y-3">
      {rooms.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 py-6 text-center text-sm text-slate-400">
          No rooms added — click Add Room to select from database.
        </div>
      )}

      {rooms.length > 0 && (
        <div className="rounded-2xl border border-slate-200 overflow-hidden">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-xs font-semibold uppercase tracking-wide text-slate-500">
                <th className="px-4 py-2.5 text-left">Room ID</th>
                <th className="px-4 py-2.5 text-left">Name</th>
                <th className="px-4 py-2.5 text-left">Type</th>
                <th className="px-4 py-2.5 text-left">Lab Type</th>
                <th className="px-4 py-2.5 text-right">Capacity</th>
                <th className="px-4 py-2.5" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rooms.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50">
                  <td className="px-4 py-2.5 font-mono font-semibold text-slate-800">{r.id}</td>
                  <td className="px-4 py-2.5 text-slate-600">{r.name}</td>
                  <td className="px-4 py-2.5">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                      r.type === 'LAB' ? 'bg-violet-100 text-violet-700' : 'bg-blue-100 text-blue-700'
                    }`}>{r.type}</span>
                  </td>
                  <td className="px-4 py-2.5 text-xs text-slate-500">{r.labType ?? '—'}</td>
                  <td className="px-4 py-2.5 text-right text-slate-600">{r.capacity}</td>
                  <td className="px-4 py-2.5 text-right">
                    <button type="button" onClick={() => remove(r.id)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition">
                      <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M18 6 6 18M6 6l12 12" />
                      </svg>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <RoomSelect onAdd={add} />
    </div>
  )
}