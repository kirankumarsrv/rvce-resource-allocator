import type { SchedulerSubject, SubjectType } from '@/types/scheduler'

interface Props {
  subjects: SchedulerSubject[]
  department: string
  onChange: (subjects: SchedulerSubject[]) => void
}

const SUBJECT_TYPE_OPTIONS: SubjectType[] = ['THEORY', 'LAB']

const Field = ({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) => (
  <div className="flex flex-col gap-1">
    <span className="text-xs font-medium text-slate-500 uppercase tracking-wide">{label}</span>
    {children}
  </div>
)

const inputCls =
  'w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-100'

export const SubjectTable = ({ subjects, department, onChange }: Props) => {
  const update = (index: number, patch: Partial<SchedulerSubject>) => {
    const next = subjects.map((s, i) => (i === index ? { ...s, ...patch } : s))
    onChange(next)
  }

  const remove = (index: number) => onChange(subjects.filter((_, i) => i !== index))

  const add = () =>
    onChange([
      ...subjects,
      {
        id: '',
        name: '',
        department,
        year: 3,
        section: '',
        credits: 3,
        type: 'THEORY' as SubjectType,
        teacherId: '',
        semester: '',
      },
    ])

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-slate-700 uppercase tracking-wide">
          Subjects <span className="ml-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-normal text-slate-500">{subjects.length}</span>
        </h3>
        <button
          type="button"
          onClick={add}
          className="inline-flex items-center gap-1.5 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-slate-700"
        >
          <svg className="h-3.5 w-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="M12 5v14M5 12h14" /></svg>
          Add Subject
        </button>
      </div>

      {subjects.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 py-8 text-center text-sm text-slate-400">
          No subjects yet — click Add Subject to begin.
        </div>
      ) : (
        <div className="space-y-3">
          {subjects.map((s, i) => (
            <div
              key={i}
              className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
            >
              <div className="flex items-start justify-between mb-3">
                <span className="rounded-lg bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-500">
                  Subject {i + 1}
                </span>
                <button
                  type="button"
                  onClick={() => remove(i)}
                  className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600 transition"
                  aria-label="Remove subject"
                >
                  <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6 6 18M6 6l12 12" /></svg>
                </button>
              </div>

              <div className="grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-4">
                <Field label="Subject ID *">
                  <input
                    value={s.id}
                    onChange={(e) => update(i, { id: e.target.value })}
                    placeholder="e.g. 21CS51"
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Subject Name *">
                  <input
                    value={s.name}
                    onChange={(e) => update(i, { name: e.target.value })}
                    placeholder="e.g. DAA"
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Type *">
                  <select
                    value={s.type}
                    onChange={(e) => update(i, { type: e.target.value as SubjectType })}
                    className={inputCls}
                  >
                    {SUBJECT_TYPE_OPTIONS.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </Field>
                <Field label="Credits *">
                  <input
                    type="number"
                    min={1}
                    max={6}
                    value={s.credits}
                    onChange={(e) => update(i, { credits: Number(e.target.value) })}
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Year *">
                  <input
                    type="number"
                    min={1}
                    max={4}
                    value={s.year}
                    onChange={(e) => update(i, { year: Number(e.target.value) })}
                    placeholder="e.g. 3"
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Section *">
                  <input
                    value={s.section}
                    onChange={(e) => update(i, { section: e.target.value })}
                    placeholder="e.g. A"
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Semester *">
                  <input
                    value={s.semester}
                    onChange={(e) => update(i, { semester: e.target.value })}
                    placeholder="e.g. 5"
                    className={inputCls}
                    required
                  />
                </Field>
                <Field label="Teacher ID *">
                  <input
                    value={s.teacherId}
                    onChange={(e) => update(i, { teacherId: e.target.value })}
                    placeholder="UUID or T-code"
                    className={inputCls}
                    required
                  />
                </Field>
                {s.type === 'LAB' && (
                  <Field label="Lab Teacher ID">
                    <input
                      value={s.labTeacherId ?? ''}
                      onChange={(e) => update(i, { labTeacherId: e.target.value })}
                      placeholder="UUID or T-code"
                      className={inputCls}
                    />
                  </Field>
                )}
                <Field label="Batch">
                  <input
                    value={s.batch ?? ''}
                    onChange={(e) => update(i, { batch: e.target.value })}
                    placeholder="e.g. P1"
                    className={inputCls}
                  />
                </Field>
                {s.type === 'THEORY' && (
                  <Field label="Theory hrs/week">
                    <input
                      type="number"
                      min={1}
                      value={s.theoryHoursPerWeek ?? ''}
                      onChange={(e) =>
                        update(i, { theoryHoursPerWeek: e.target.value ? Number(e.target.value) : undefined })
                      }
                      placeholder="e.g. 3"
                      className={inputCls}
                    />
                  </Field>
                )}
                {s.type === 'LAB' && (
                  <Field label="Lab hrs/week">
                    <input
                      type="number"
                      min={1}
                      value={s.labHoursPerWeek ?? ''}
                      onChange={(e) =>
                        update(i, { labHoursPerWeek: e.target.value ? Number(e.target.value) : undefined })
                      }
                      placeholder="e.g. 2"
                      className={inputCls}
                    />
                  </Field>
                )}
                <Field label="Fixed Room ID">
                  <input
                    value={s.fixedRoomId ?? ''}
                    onChange={(e) => update(i, { fixedRoomId: e.target.value })}
                    placeholder="optional"
                    className={inputCls}
                  />
                </Field>
              </div>

              <div className="mt-3 flex flex-wrap gap-3">
                <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={!!s.isElective}
                    onChange={(e) => update(i, { isElective: e.target.checked })}
                    className="rounded border-slate-300"
                  />
                  Elective
                </label>
                <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={!!s.theoryOnlyFourCredit}
                    onChange={(e) => update(i, { theoryOnlyFourCredit: e.target.checked })}
                    className="rounded border-slate-300"
                  />
                  Theory-only 4-credit
                </label>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
