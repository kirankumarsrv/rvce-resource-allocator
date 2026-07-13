import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { SchedulerSubject, SubjectType } from '@/types/scheduler'
import { authenticatedFetch } from '@/services/authService'

// ─── Teacher fetch (cached) ───────────────────────────────────────────────────
interface TeacherOption { id: string; text: string }
let _teacherCache: TeacherOption[] | null = null

const useTeachers = () => {
  const [teachers, setTeachers] = useState<TeacherOption[]>(_teacherCache ?? [])
  const [loading, setLoading] = useState(!_teacherCache)
  useEffect(() => {
    if (_teacherCache) return
    authenticatedFetch('/api/timetable/teachers')
      .then((r) => r.json())
      .then((data: TeacherOption[]) => { _teacherCache = data; setTeachers(data) })
      .finally(() => setLoading(false))
  }, [])
  return { teachers, loading }
}

// Searchable teacher dropdown
const TeacherSelect = ({
  value,
  onChange,
  placeholder = 'Select teacher',
}: {
  value: string
  onChange: (id: string) => void
  placeholder?: string
}) => {
  const { teachers, loading } = useTeachers()
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [menuStyle, setMenuStyle] = useState<React.CSSProperties>({})
  const buttonRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  const selected = teachers.find((t) => t.id === value)
  const filtered = query
    ? teachers.filter((t) => t.text.toLowerCase().includes(query.toLowerCase()))
    : teachers

  // Position the menu using the button's real screen position, then render it
  // through a portal to document.body. This is the fix for the dropdown only
  // showing ~1 row: a parent card further up uses overflow-hidden (for its
  // rounded corners), which was clipping the absolutely-positioned menu even
  // though the menu itself allows scrolling. A portal escapes that entirely.
  useEffect(() => {
    if (!open || !buttonRef.current) return
    const updatePosition = () => {
      const rect = buttonRef.current!.getBoundingClientRect()
      const spaceBelow = window.innerHeight - rect.bottom
      const openUpward = spaceBelow < 260 && rect.top > 260
      setMenuStyle({
        position: 'fixed',
        left: rect.left,
        width: rect.width,
        ...(openUpward
          ? { bottom: window.innerHeight - rect.top + 4 }
          : { top: rect.bottom + 4 }),
        zIndex: 9999,
      })
    }
    updatePosition()
    window.addEventListener('scroll', updatePosition, true)
    window.addEventListener('resize', updatePosition)
    return () => {
      window.removeEventListener('scroll', updatePosition, true)
      window.removeEventListener('resize', updatePosition)
    }
  }, [open])

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      const target = e.target as Node
      if (buttonRef.current?.contains(target)) return
      if (menuRef.current?.contains(target)) return
      setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  return (
    <div className="relative">
      <button
        ref={buttonRef}
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-left outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-100 flex items-center justify-between gap-2"
      >
        <span className={selected ? 'text-slate-900 truncate' : 'text-slate-400 truncate'}>
          {loading ? 'Loading…' : selected ? selected.text : placeholder}
        </span>
        <svg className="h-4 w-4 text-slate-400 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>

      {open && createPortal(
        <div
          ref={menuRef}
          style={menuStyle}
          className="min-w-[240px] rounded-xl border border-slate-200 bg-white shadow-lg overflow-hidden"
        >
          <div className="p-2 border-b border-slate-100">
            <input
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search teacher…"
              className="w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm outline-none focus:border-slate-400"
            />
          </div>
          <div className="max-h-48 overflow-y-auto">
            {value && (
              <button
                type="button"
                onClick={() => { onChange(''); setOpen(false); setQuery('') }}
                className="w-full text-left px-3 py-2 text-xs text-slate-400 hover:bg-slate-50"
              >
                Clear selection
              </button>
            )}
            {filtered.length === 0 ? (
              <p className="px-3 py-4 text-sm text-slate-400 text-center">No match</p>
            ) : (
              filtered.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => { onChange(t.id); setOpen(false); setQuery('') }}
                  className={`w-full text-left px-3 py-2 text-sm transition hover:bg-slate-50 ${t.id === value ? 'bg-slate-100 font-medium' : ''}`}
                >
                  {t.text}
                </button>
              ))
            )}
          </div>
        </div>,
        document.body
      )}
    </div>
  )
}

// ─── Wizard types ─────────────────────────────────────────────────────────────
interface CourseTemplate {
  _key: string
  subjectCode: string
  name: string
  type: SubjectType
  credits: number
  isElective: boolean
  electiveSlot: string
  theoryOnlyFourCredit: boolean
  requiredLabType: string
  labBatches: number        // only for LAB
}

interface SectionTeachers {
  courseKey: string
  section: string
  teacherId: string
  labTeacherIds: string[]   // per batch
}

const SECTIONS = ['A', 'B', 'C', 'D', 'E', 'F']
const uid = () => Math.random().toString(36).slice(2, 8)

const inputCls =
  'w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-100'

const Label = ({ children }: { children: React.ReactNode }) => (
  <span className="block text-xs font-medium text-slate-500 uppercase tracking-wide mb-1">{children}</span>
)

// ─── Step indicator ───────────────────────────────────────────────────────────
const Steps = ({ current }: { current: 1 | 2 | 3 }) => (
  <div className="flex items-center mb-6">
    {(['Year & Sections', 'Courses', 'Assign Teachers'] as const).map((label, i) => {
      const n = (i + 1) as 1 | 2 | 3
      const done = current > n
      const active = current === n
      return (
        <div key={n} className="flex items-center flex-1 last:flex-none">
          <div className="flex items-center gap-2 shrink-0">
            <div className={`h-7 w-7 rounded-full flex items-center justify-center text-xs font-bold transition ${done ? 'bg-emerald-500 text-white' : active ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-400'}`}>
              {done ? '✓' : n}
            </div>
            <span className={`text-xs font-semibold ${active ? 'text-slate-900' : 'text-slate-400'}`}>{label}</span>
          </div>
          {i < 2 && <div className="flex-1 mx-3 h-px bg-slate-200" />}
        </div>
      )
    })}
  </div>
)

// ─── Step 1 ───────────────────────────────────────────────────────────────────
const Step1 = ({
  year, setYear, semester, setSemester, sectionCount, setSectionCount, onNext,
}: {
  year: number; setYear: (v: number) => void
  semester: string; setSemester: (v: string) => void
  sectionCount: number; setSectionCount: (v: number) => void
  onNext: () => void
}) => (
  <div className="space-y-5">
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <div>
        <Label>Year *</Label>
        <select value={year} onChange={(e) => setYear(Number(e.target.value))} className={inputCls}>
          {[1, 2, 3, 4].map((y) => <option key={y} value={y}>Year {y}</option>)}
        </select>
      </div>
      <div>
        <Label>Semester *</Label>
        <select value={semester} onChange={(e) => setSemester(e.target.value)} className={inputCls}>
          <option value="">Select</option>
          {['1','2','3','4','5','6','7','8'].map((s) => <option key={s} value={s}>Sem {s}</option>)}
        </select>
      </div>
      <div>
        <Label>Number of Sections *</Label>
        <div className="flex gap-1">
          {[1,2,3,4,5,6].map((n) => (
            <button key={n} type="button" onClick={() => setSectionCount(n)}
              className={`flex-1 rounded-xl border py-2 text-sm font-semibold transition ${sectionCount === n ? 'bg-slate-900 text-white border-slate-900' : 'border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700'}`}>
              {n}
            </button>
          ))}
        </div>
      </div>
    </div>
    <div className="flex justify-end">
      <button type="button" onClick={onNext} disabled={!semester}
        className="rounded-2xl bg-slate-900 px-6 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed transition">
        Next: Add Courses →
      </button>
    </div>
  </div>
)

// ─── Step 2 ───────────────────────────────────────────────────────────────────
const Step2 = ({
  courses, setCourses, onBack, onNext,
}: {
  courses: CourseTemplate[]
  setCourses: (c: CourseTemplate[]) => void
  onBack: () => void
  onNext: () => void
}) => {
  const update = (key: string, patch: Partial<CourseTemplate>) =>
    setCourses(courses.map((c) => c._key === key ? { ...c, ...patch } : c))
  const remove = (key: string) => setCourses(courses.filter((c) => c._key !== key))
  const add = () => setCourses([...courses, {
    _key: uid(), subjectCode: '', name: '', type: 'THEORY', credits: 3,
    isElective: false, electiveSlot: '', theoryOnlyFourCredit: false,
    requiredLabType: '', labBatches: 2,
  }])

  const canProceed = courses.length > 0 && courses.every((c) => c.name.trim() && c.subjectCode.trim())

  return (
    <div className="space-y-4">
      <p className="text-xs text-slate-500">Add each course once — teacher assignment per section is the next step.</p>

      {courses.length === 0 && (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 py-8 text-center text-sm text-slate-400">
          No courses yet — click Add Course.
        </div>
      )}

      {courses.map((c, i) => (
        <div key={c._key} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-slate-400">Course {i + 1}</span>
            <button type="button" onClick={() => remove(c._key)}
              className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition">
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6 6 18M6 6l12 12" /></svg>
            </button>
          </div>

          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <div>
              <Label>Subject Code *</Label>
              <input value={c.subjectCode} onChange={(e) => update(c._key, { subjectCode: e.target.value })}
                placeholder="e.g. 21CS51" className={inputCls} />
            </div>
            <div className="md:col-span-2">
              <Label>Course Name *</Label>
              <input value={c.name} onChange={(e) => update(c._key, { name: e.target.value })}
                placeholder="e.g. Compiler Design" className={inputCls} />
            </div>
            <div>
              <Label>Credits *</Label>
              <input type="number" min={1} max={6} value={c.credits}
                onChange={(e) => update(c._key, { credits: Number(e.target.value) })} className={inputCls} />
            </div>
            <div>
              <Label>Type *</Label>
              <select value={c.type}
                onChange={(e) => update(c._key, { type: e.target.value as SubjectType, labBatches: e.target.value === 'LAB' ? 2 : 0 })}
                className={inputCls}>
                <option value="THEORY">THEORY</option>
                <option value="LAB">LAB</option>
              </select>
            </div>
            {c.type === 'LAB' && (
              <div>
                <Label>Batches per Section</Label>
                <div className="flex gap-1">
                  {[1,2,3].map((n) => (
                    <button key={n} type="button" onClick={() => update(c._key, { labBatches: n })}
                      className={`flex-1 rounded-xl border py-2 text-sm font-semibold transition ${c.labBatches === n ? 'bg-slate-900 text-white border-slate-900' : 'border-slate-200 bg-slate-50 hover:bg-slate-100'}`}>
                      {n}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {c.type === 'LAB' && (
              <div>
                <Label>Required Lab Type</Label>
                <input value={c.requiredLabType}
                  onChange={(e) => update(c._key, { requiredLabType: e.target.value })}
                  placeholder="e.g. CS_LAB" className={inputCls} />
              </div>
            )}
          </div>

          <div className="mt-3 flex flex-wrap gap-4">
            <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer select-none">
              <input type="checkbox" checked={c.isElective}
                onChange={(e) => update(c._key, { isElective: e.target.checked, electiveSlot: '' })}
                className="rounded border-slate-300" />
              Elective
            </label>
            {c.isElective && (
              <div className="flex items-center gap-2">
                <Label>Elective Slot</Label>
                <input value={c.electiveSlot}
                  onChange={(e) => update(c._key, { electiveSlot: e.target.value })}
                  placeholder="e.g. WEDNESDAY_3,THURSDAY_4"
                  className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm outline-none focus:border-slate-400 w-64" />
              </div>
            )}
            {c.type === 'THEORY' && (
              <label className="flex items-center gap-2 text-sm text-slate-600 cursor-pointer select-none">
                <input type="checkbox" checked={c.theoryOnlyFourCredit}
                  onChange={(e) => update(c._key, { theoryOnlyFourCredit: e.target.checked })}
                  className="rounded border-slate-300" />
                Theory-only 4-credit
              </label>
            )}
          </div>
        </div>
      ))}

      <button type="button" onClick={add}
        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 transition">
        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><path d="M12 5v14M5 12h14" /></svg>
        Add Course
      </button>

      <div className="flex justify-between pt-2">
        <button type="button" onClick={onBack}
          className="rounded-2xl border border-slate-200 px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition">
          ← Back
        </button>
        <button type="button" onClick={onNext} disabled={!canProceed}
          className="rounded-2xl bg-slate-900 px-6 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 disabled:opacity-40 disabled:cursor-not-allowed transition">
          Next: Assign Teachers →
        </button>
      </div>
    </div>
  )
}

// ─── Step 3 ───────────────────────────────────────────────────────────────────
const Step3 = ({
  courses, sectionCount, assignments, setAssignments, onBack, onDone,
}: {
  courses: CourseTemplate[]
  sectionCount: number
  assignments: SectionTeachers[]
  setAssignments: (a: SectionTeachers[]) => void
  onBack: () => void
  onDone: () => void
}) => {
  const sections = SECTIONS.slice(0, sectionCount)

  const get = (courseKey: string, section: string): SectionTeachers =>
    assignments.find((a) => a.courseKey === courseKey && a.section === section) ??
    { courseKey, section, teacherId: '', labTeacherIds: [] }

  const set = (courseKey: string, section: string, patch: Partial<SectionTeachers>) => {
    const existing = get(courseKey, section)
    const merged = { ...existing, ...patch }
    setAssignments([
      ...assignments.filter((a) => !(a.courseKey === courseKey && a.section === section)),
      merged,
    ])
  }

  const setLabTeacher = (courseKey: string, section: string, batchIdx: number, id: string) => {
    const existing = get(courseKey, section)
    const labTeacherIds = [...(existing.labTeacherIds ?? [])]
    labTeacherIds[batchIdx] = id
    set(courseKey, section, { labTeacherIds })
  }

  const allDone = courses.every((course) =>
    sections.every((section) => {
      const a = get(course._key, section)
      if (!a.teacherId) return false
      if (course.type === 'LAB') {
        return Array.from({ length: course.labBatches }, (_, i) => a.labTeacherIds?.[i]).every(Boolean)
      }
      return true
    })
  )

  return (
    <div className="space-y-6">
      <p className="text-xs text-slate-500">
        Assign teachers for each course × section.
        {courses.some((c) => c.type === 'LAB') && ' Lab courses need one teacher per batch.'}
      </p>

      {courses.map((course) => (
        <div key={course._key} className="rounded-2xl border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="bg-slate-50 border-b border-slate-200 px-4 py-3 flex items-center gap-3 flex-wrap">
            <span className="font-semibold text-slate-800 text-sm">{course.name}</span>
            <span className="text-xs text-slate-400">{course.subjectCode}</span>
            <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${course.type === 'LAB' ? 'bg-violet-100 text-violet-700' : 'bg-blue-100 text-blue-700'}`}>
              {course.type}
            </span>
            {course.isElective && <span className="rounded-full px-2 py-0.5 text-xs font-semibold bg-amber-100 text-amber-700">Elective</span>}
            <span className="ml-auto text-xs text-slate-400">{course.credits} credits</span>
          </div>

          <div className="divide-y divide-slate-100">
            {sections.map((section) => {
              const a = get(course._key, section)
              return (
                <div key={section} className="px-4 py-3 flex items-start gap-4 flex-wrap md:flex-nowrap">
                  <div className="w-16 shrink-0 pt-1">
                    <span className="inline-flex items-center justify-center h-7 w-7 rounded-lg bg-slate-100 text-xs font-bold text-slate-600">
                      {section}
                    </span>
                  </div>
                  <div className="flex-1 space-y-2 min-w-0">
                    <div>
                      <Label>{course.type === 'LAB' ? 'Theory teacher' : 'Teacher'}</Label>
                      <TeacherSelect value={a.teacherId} onChange={(id) => set(course._key, section, { teacherId: id })} />
                    </div>
                    {course.type === 'LAB' && Array.from({ length: course.labBatches }, (_, bIdx) => (
                      <div key={bIdx}>
                        <Label>Batch {bIdx + 1} lab teacher</Label>
                        <TeacherSelect
                          value={(a.labTeacherIds ?? [])[bIdx] ?? ''}
                          onChange={(id) => setLabTeacher(course._key, section, bIdx, id)}
                          placeholder={`Batch ${bIdx + 1} lab teacher`}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      ))}

      <div className="flex justify-between pt-2">
        <button type="button" onClick={onBack}
          className="rounded-2xl border border-slate-200 px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition">
          ← Back
        </button>
        <button type="button" onClick={onDone} disabled={!allDone}
          className="rounded-2xl bg-emerald-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-40 disabled:cursor-not-allowed transition">
          ✓ Done — Preview & Generate
        </button>
      </div>
    </div>
  )
}

// ─── Main export ──────────────────────────────────────────────────────────────
interface Props {
  subjects: SchedulerSubject[]
  department: string
  onChange: (subjects: SchedulerSubject[]) => void
}

export const SubjectTable = ({ subjects, department, onChange }: Props) => {
  const [step, setStep] = useState<1 | 2 | 3>(1)
  const [year, setYear] = useState(3)
  const [semester, setSemester] = useState('')
  const [sectionCount, setSectionCount] = useState(5)
  const [courses, setCourses] = useState<CourseTemplate[]>([])
  const [assignments, setAssignments] = useState<SectionTeachers[]>([])

  const buildAndEmit = () => {
    const sections = SECTIONS.slice(0, sectionCount)
    const expanded: SchedulerSubject[] = []

    for (const course of courses) {
      for (const section of sections) {
        const a = assignments.find((x) => x.courseKey === course._key && x.section === section)
        if (!a) continue

        if (course.type === 'THEORY') {
          expanded.push({
            id: `${course.subjectCode}_${section}`,
            name: course.name,
            department,
            year,
            section,
            credits: course.credits,
            type: 'THEORY',
            teacherId: a.teacherId,
            semester,
            isElective: course.isElective,
            electiveSlot: course.electiveSlot || undefined,
            theoryOnlyFourCredit: course.theoryOnlyFourCredit,
            fixedRoomId: undefined,
          })
        } else {
          // One row per batch
          for (let bIdx = 0; bIdx < course.labBatches; bIdx++) {
            expanded.push({
              id: `${course.subjectCode}_${section}_P${bIdx + 1}`,
              name: course.name,
              department,
              year,
              section,
              batch: `${bIdx + 1}`,
              credits: course.credits,
              type: 'LAB',
              teacherId: a.teacherId,
              labTeacherId: a.labTeacherIds?.[bIdx] ?? '',
              semester,
              isElective: course.isElective,
              requiredLabType: course.requiredLabType || undefined,
              labHoursPerWeek: 2,
            })
          }
        }
      }
    }

    onChange(expanded)
  }

  const handleDone = () => {
    buildAndEmit()
  }

  return (
    <div>
      <Steps current={step} />

      {step === 1 && (
        <Step1
          year={year} setYear={setYear}
          semester={semester} setSemester={setSemester}
          sectionCount={sectionCount} setSectionCount={setSectionCount}
          onNext={() => setStep(2)}
        />
      )}
      {step === 2 && (
        <Step2
          courses={courses} setCourses={setCourses}
          onBack={() => setStep(1)}
          onNext={() => setStep(3)}
        />
      )}
      {step === 3 && (
        <Step3
          courses={courses}
          sectionCount={sectionCount}
          assignments={assignments}
          setAssignments={setAssignments}
          onBack={() => setStep(2)}
          onDone={handleDone}
        />
      )}

      {subjects.length > 0 && (
        <div className="mt-4 rounded-2xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-700">
          <span className="font-semibold">{subjects.length} subject slots</span> ready — scroll down to add rooms and generate.
        </div>
      )}
    </div>
  )
}