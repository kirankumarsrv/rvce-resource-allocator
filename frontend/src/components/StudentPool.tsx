/**
 * StudentPool Component
 * Lists unassigned students with search, filter, and multi-select
 */

import { useMemo, useState } from 'react'
import type { DragEvent } from 'react'
import type { StudentGroupDto, UnassignedStudentDto, SeatDto } from '@/types/exam'
import { Search, Trash2 } from 'lucide-react'

interface StudentPoolProps {
  unassignedStudents: UnassignedStudentDto[]
  onStudentSelect: (student: UnassignedStudentDto) => void
  onStudentDeselect: () => void
  selectedStudent: UnassignedStudentDto | null
  pendingAssignments?: Map<string, SeatDto>
  onQuickAssign: (students: UnassignedStudentDto[]) => void
  onStudentDragStart: (
    studentOrGroup: UnassignedStudentDto | StudentGroupDto,
    event: DragEvent<HTMLButtonElement>
  ) => void
  isQuickAssignPending?: boolean
}

export const StudentPool = ({
  unassignedStudents,
  onStudentSelect,
  onStudentDeselect,
  selectedStudent,
  pendingAssignments = new Map(),
  onQuickAssign,
  onStudentDragStart,
  isQuickAssignPending = false,
}: StudentPoolProps) => {
  const [searchTerm, setSearchTerm] = useState('')
  const [filterBranch, setFilterBranch] = useState<string | null>(null)
  const [filterAccessibility, setFilterAccessibility] = useState(false)

  const branches = useMemo(() => {
    return [...new Set(unassignedStudents.map((s) => s.branchCode))].sort()
  }, [unassignedStudents])

  // Get set of already-assigned student IDs from pending assignments
  const assignedStudentKeys = useMemo(() => {
    return new Set(
      Array.from(pendingAssignments.values()).map((seat) => seat.studentId ?? seat.usn ?? '')
    )
  }, [pendingAssignments])

  const filteredStudents = useMemo(() => {
    return unassignedStudents.filter((student) => {
      const studentKey = student.studentId ?? student.entryId ?? student.usn
      // Skip students already assigned
      if (assignedStudentKeys.has(studentKey)) {
        return false
      }

      const matchesSearch =
        student.usn.toLowerCase().includes(searchTerm.toLowerCase()) ||
        student.studentName.toLowerCase().includes(searchTerm.toLowerCase())

      const matchesBranch = !filterBranch || student.branchCode === filterBranch

      return matchesSearch && matchesBranch
    })
  }, [unassignedStudents, searchTerm, filterBranch, filterAccessibility, assignedStudentKeys])

  const groupedStudents = useMemo(() => {
    const groups = new Map<string, StudentGroupDto>()

    filteredStudents.forEach((student) => {
      const groupKey = `${student.branchCode}||${student.studentClass ?? ''}||${student.section ?? ''}`
      const label = [student.branchCode, student.studentClass, student.section]
        .filter(Boolean)
        .join(' / ')

      if (!groups.has(groupKey)) {
        groups.set(groupKey, {
          groupId: groupKey,
          label: label || 'Student group',
          students: [student],
        })
      } else {
        groups.get(groupKey)!.students.push(student)
      }
    })

    return Array.from(groups.values()).sort((a, b) => a.label.localeCompare(b.label))
  }, [filteredStudents])

  const branchColorMap: Record<string, string> = {
    CSE: 'bg-blue-50 border-blue-200',
    ISE: 'bg-green-50 border-green-200',
    ECE: 'bg-yellow-50 border-yellow-200',
    MECH: 'bg-red-50 border-red-200',
    CIVIL: 'bg-purple-50 border-purple-200',
    EEE: 'bg-orange-50 border-orange-200',
  }

  return (
    <div className="flex flex-col gap-3 border-2 border-gray-300 rounded-lg p-4 bg-gray-50 w-full max-w-xs">
      {/* Header */}
      <div>
        <h3 className="text-lg font-bold text-gray-800">
          Unassigned Students ({filteredStudents.length})
        </h3>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-2 top-2.5 text-gray-400" size={18} />
        <input
          type="text"
          placeholder="Search by USN or name"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded text-sm"
        />
      </div>

      {/* Filters */}
      <div className="flex flex-col gap-2">
        {/* Branch filter */}
        <div>
          <label className="text-xs font-semibold text-gray-700 block mb-1">
            Branch
          </label>
          <select
            value={filterBranch || ''}
            onChange={(e) => setFilterBranch(e.target.value || null)}
            className="w-full px-2 py-1 border border-gray-300 rounded text-sm"
          >
            <option value="">All branches</option>
            {branches.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
        </div>

        {/* Accessibility filter */}
        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            id="accessibility"
            checked={filterAccessibility}
            onChange={(e) => setFilterAccessibility(e.target.checked)}
            className="w-4 h-4 border-gray-300 rounded"
          />
          <label
            htmlFor="accessibility"
            className="text-xs font-semibold text-gray-700 cursor-pointer"
          >
            Front row only
          </label>
        </div>

        <button
          type="button"
          onClick={() => onQuickAssign(filteredStudents.filter((student) => student.studentId))}
          disabled={filteredStudents.filter((student) => student.studentId).length === 0 || isQuickAssignPending}
          className="mt-2 px-3 py-2 rounded bg-blue-600 text-white text-xs font-semibold hover:bg-blue-700 transition disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isQuickAssignPending ? 'Assigning...' : 'Assign filtered students to selected hall'}
        </button>
      </div>

      {/* Student list */}
      <div className="flex-1 overflow-y-auto max-h-96 space-y-1">
        {groupedStudents.length === 0 ? (
          <div className="text-xs text-gray-500 text-center py-4">
            No students match filters
          </div>
        ) : (
          groupedStudents.map((group) => {
            const groupStudentIds = new Set(group.students.map((student) => student.entryId))
            const isSelected = selectedStudent ? groupStudentIds.has(selectedStudent.entryId) : false
            const sampleNames = group.students.slice(0, 3).map((student) => student.studentName)
            const validStudents = group.students.filter((student) => student.studentId)
            const hasNoAccount = validStudents.length === 0
            const bgColor = branchColorMap[group.students[0].branchCode] || 'bg-gray-100'

            return (
              <button
                key={group.groupId}
                type="button"
                draggable={!hasNoAccount}
                onDragStart={(event) => !hasNoAccount && onStudentDragStart(group, event)}
                className={
                  `
                    w-full text-left p-3 border-2 rounded transition
                    ${bgColor}
                    ${hasNoAccount ? 'opacity-70 cursor-not-allowed border-dashed' : 'cursor-pointer'}
                    ${isSelected ? 'border-green-500 ring-2 ring-green-200' : 'border-gray-200 hover:border-gray-400'}
                  `
                }
                onClick={() => !hasNoAccount && onStudentSelect(group.students[0])}
                disabled={hasNoAccount}
                title={
                  hasNoAccount
                    ? 'This group has no linked student accounts and cannot be assigned.'
                    : 'Drag this group to a rule card to assign a matching seat.'
                }
              >
                <div className="flex items-center justify-between gap-2">
                  <div>
                    <div className="text-xs font-bold text-gray-800 truncate">
                      {group.label}
                    </div>
                    <div className="text-[11px] text-gray-600 mt-1">
                      {group.students.length} student{group.students.length > 1 ? 's' : ''}
                    </div>
                  </div>
                  <div className="text-[10px] rounded-full bg-white/80 px-2 py-1 font-semibold text-slate-700 border border-slate-200">
                    Group
                  </div>
                </div>
                <div className="mt-2 text-[11px] text-slate-700">
                  {sampleNames.join(', ')}{group.students.length > 3 ? ` +${group.students.length - 3} more` : ''}
                </div>
                {validStudents.length < group.students.length && (
                  <div className="mt-2 inline-flex rounded-full bg-yellow-100 px-2 py-1 text-[10px] text-yellow-800 font-semibold">
                    {group.students.length - validStudents.length} unlinked
                  </div>
                )}
              </button>
            )
          })
        )}
      </div>

      {/* Clear selection button */}
      {selectedStudent && (
        <button
          onClick={onStudentDeselect}
          className="flex items-center justify-center gap-2 px-3 py-2 bg-red-100 text-red-700 text-xs font-semibold rounded border border-red-300 hover:bg-red-200 transition"
        >
          <Trash2 size={14} />
          Clear Selection
        </button>
      )}
    </div>
  )
}
