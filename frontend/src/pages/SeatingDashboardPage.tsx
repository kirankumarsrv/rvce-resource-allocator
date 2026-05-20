/**
 * SeatingDashboard Page
 * Main interactive seating arrangement interface
 * Handles: load state, seat placement, auto-save, bulk save, publish
 */

import { useEffect, useState, useRef, useCallback } from 'react'
import type { DragEvent } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Save, AlertCircle, CheckCircle, Loader, Upload, Plus } from 'lucide-react'

import type {
  SeatDto,
  UnassignedStudentDto,
  StudentGroupDto,
  ExamHallConfigRequest,
  ExamHallDto,
  HallGridDto,
  ExamSeatDto,
  ExamSessionDto,
} from '@/types/exam'
import {
  loadSeatingDashboardState,
  getExamSession,
  bulkSaveSeats,
  publishExam,
  clearHall,
  deleteExamHall,
  uploadStudentsCSV,
  addExamHall,
} from '@/services/examService'
import { StudentPool } from '@/components/StudentPool'
import { ClassroomGrid } from '@/components/ClassroomGrid'
import ErrorBoundary from '@/components/ErrorBoundary'
import { AddHallModal } from '@/components/AddHallModal'
import { AllocationRuleCard, type AllocationRuleId } from '@/components/AllocationRuleCard'
import { useDashboardStore } from '@/store/seatingStore'
import { applyAllocationRule } from '@/utils/allocationRules'

export const SeatingDashboard = () => {
  const { examId } = useParams<{ examId: string }>()
  if (!examId) return <div>Invalid exam ID</div>

  // ===== Store =====
  const {
    selectedStudent,
    setSelectedStudent,
    pendingAssignments,
    addPendingAssignment,
    addPendingAssignments,
    removePendingAssignment,
    clearPendingAssignments,
    removedAssignments,
    addRemovedAssignment,
    clearRemovedAssignments,
    isDirty,
    activeHallId,
    setActiveHallId,
  } = useDashboardStore()

  // ===== Local State =====
  const [saveMessage, setSaveMessage] = useState<string | null>(null)
  const [isDragOver, setIsDragOver] = useState(false)
  const [isAddHallModalOpen, setIsAddHallModalOpen] = useState(false)
  const autoSaveTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // ===== Queries =====
  const { data: dashboardState, isLoading, error, refetch } = useQuery({
    queryKey: ['seating-dashboard', examId],
    queryFn: () => loadSeatingDashboardState(examId),
    refetchInterval: 5000, // Poll every 5 seconds for live updates
  })

  const {
    data: examSession,
    isLoading: isExamLoading,
    error: examError,
  } = useQuery<ExamSessionDto>({
    queryKey: ['exam-session', examId],
    queryFn: () => getExamSession(examId),
  })

  const uploadMutation = useMutation({
    mutationFn: (file: File) => uploadStudentsCSV(examId, file),
    onSuccess: (result) => {
      setSaveMessage(`✓ Upload complete: ${result.inserted} inserted, ${result.skipped} skipped`)
      refetch()
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      setTimeout(() => setSaveMessage(null), 4000)
    },
    onError: (err) => {
      setSaveMessage('✗ Upload failed: ' + (err instanceof Error ? err.message : 'Unknown error'))
      setTimeout(() => setSaveMessage(null), 5000)
    },
  })

  const addHallMutation = useMutation({
    mutationFn: (config: ExamHallConfigRequest) => addExamHall(examId, config),
    onSuccess: () => {
      setSaveMessage('✓ Hall added successfully')
      setIsAddHallModalOpen(false)
      refetch()
      setTimeout(() => setSaveMessage(null), 4000)
    },
    onError: (err) => {
      setSaveMessage('✗ Failed to add hall: ' + (err instanceof Error ? err.message : 'Unknown error'))
      setTimeout(() => setSaveMessage(null), 5000)
    },
  })

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (!dashboardState) return

      const removedPositionKeys = new Set(Array.from(removedAssignments))
      const assignmentMap = new Map<string, SeatDto>()
      const makePositionKey = (seat: { benchRow: number; benchCol: number; benchSeatIndex: number }) =>
        `${seat.benchRow}:${seat.benchCol}:${seat.benchSeatIndex}`

      assignedSeats.forEach((seat) => {
        const key = makePositionKey(seat)
        if (!removedPositionKeys.has(key)) {
          assignmentMap.set(key, {
            ...seat,
            usn: seat.usn ?? null,
            studentName: seat.studentName ?? null,
            branchCode: seat.branchCode ?? null,
            isManualOverride: seat.manualOverride ?? false,
          })
        }
      })

      pendingAssignments.forEach((seat) => {
        const key = makePositionKey(seat)
        assignmentMap.set(key, seat)
      })

      const assignments = Array.from(assignmentMap.values())
        .filter((seat) => seat.studentId)
        .map((seat) => ({
          studentId: seat.studentId as string,
          hallId: seat.hallId,
          benchRow: seat.benchRow,
          benchCol: seat.benchCol,
          benchSeatIndex: seat.benchSeatIndex,
        }))

      if (assignments.length === 0 && (pendingAssignments.size > 0 || removedAssignments.size > 0)) {
        await bulkSaveSeats(examId, { assignments: [] })
        return
      }

      if (assignments.length > 0) {
        await bulkSaveSeats(examId, { assignments })
      }
    },
    onSuccess: () => {
      clearPendingAssignments()
      clearRemovedAssignments()
      setSaveMessage('✓ Saved successfully')
      refetch()
      setTimeout(() => setSaveMessage(null), 3000)
    },
    onError: (err) => {
      setSaveMessage('✗ Save failed: ' + (err instanceof Error ? err.message : 'Unknown error'))
      setTimeout(() => setSaveMessage(null), 5000)
    },
  })

  const publishMutation = useMutation({
    mutationFn: () => publishExam(examId),
    onSuccess: () => {
      setSaveMessage('✓ Exam published!')
      setTimeout(() => setSaveMessage(null), 3000)
    },
    onError: (err) => {
      setSaveMessage(
        '✗ Publish failed: ' + (err instanceof Error ? err.message : 'Unknown error')
      )
      setTimeout(() => setSaveMessage(null), 5000)
    },
  })

  const clearHallMutation = useMutation({
    mutationFn: (hallId: string) => clearHall(examId, hallId),
    onSuccess: () => {
      setSaveMessage('✓ Hall cleared')
      refetch()
      setTimeout(() => setSaveMessage(null), 3000)
    },
    onError: (err) => {
      setSaveMessage('✗ Clear failed: ' + (err instanceof Error ? err.message : 'Unknown error'))
      setTimeout(() => setSaveMessage(null), 5000)
    },
  })

  const deleteHallMutation = useMutation({
    mutationFn: (hallId: string) => deleteExamHall(examId, hallId),
    onSuccess: (_data, hallId) => {
      setSaveMessage('✓ Hall deleted')
      if (displayHall?.hallId === hallId) {
        const nextHall = hallDtos.find((hall) => hall.hallId !== hallId)
        if (nextHall) setActiveHallId(nextHall.hallId)
      }
      refetch()
      setTimeout(() => setSaveMessage(null), 3000)
    },
    onError: (err) => {
      setSaveMessage('✗ Delete failed: ' + (err instanceof Error ? err.message : 'Unknown error'))
      setTimeout(() => setSaveMessage(null), 5000)
    },
  })

  const quickAssignMutation = useMutation({
    mutationFn: async (assignments: SeatDto[]) => {
      const placementRequests = assignments.map(seat => ({
        studentId: seat.studentId as string,
        hallId: seat.hallId,
        benchRow: seat.benchRow,
        benchCol: seat.benchCol,
        benchSeatIndex: seat.benchSeatIndex,
      }))
      return bulkSaveSeats(examId, { assignments: placementRequests })
    },
    onSuccess: () => {
      setSaveMessage('✓ Quick assign completed successfully')
      refetch()
      setTimeout(() => setSaveMessage(null), 4000)
    },
    onError: (err) => {
      setSaveMessage('✗ Quick assign failed: ' + (err instanceof Error ? err.message : 'Unknown error'))
      setTimeout(() => setSaveMessage(null), 5000)
    },
  })

  // ===== Handlers =====
  const assignedSeats: ExamSeatDto[] = dashboardState?.assignedSeats ?? []

  const positionKey = (seat: { benchRow: number; benchCol: number; benchSeatIndex: number }) =>
    `${seat.benchRow}:${seat.benchCol}:${seat.benchSeatIndex}`

  const currentAssignedSeats = assignedSeats.filter(
    (seat) => !removedAssignments.has(positionKey(seat))
  )

  const handleSeatClick = useCallback(
    (seat: SeatDto, student: UnassignedStudentDto | null) => {
      const seatPosition = positionKey(seat)
      const isCurrentlyAssigned = Boolean(seat.studentId || seat.usn)

      if (!student) {
        if (!isCurrentlyAssigned) {
          return
        }

        removePendingAssignment(seat.seatId)
        addRemovedAssignment(seatPosition)
        setSelectedStudent(null)
        setSaveMessage('✓ Student removed from seat')
        setTimeout(() => setSaveMessage(null), 3500)
        return
      }

      if (!student.studentId) {
        setSaveMessage(
          '✗ Cannot assign a student without a linked user account. Please link the student first.'
        )
        setTimeout(() => setSaveMessage(null), 5000)
        return
      }

      const studentKey = student.studentId
      const alreadyAssigned = Array.from(pendingAssignments.values()).some((s) => {
        return s.studentId === studentKey || s.usn === student.usn
      }) || assignedSeats.some((s) => s.studentId === studentKey)

      if (alreadyAssigned && seat.studentId !== studentKey) {
        setSaveMessage(
          '✗ Student already assigned to another seat. Remove them first.'
        )
        setTimeout(() => setSaveMessage(null), 4000)
        return
      }

      if (isCurrentlyAssigned) {
        addRemovedAssignment(seatPosition)
      }

      const assignment: SeatDto = {
        ...seat,
        studentId: student.studentId,
        usn: student.usn,
        studentName: student.studentName,
        branchCode: student.branchCode,
        isManualOverride: true,
      }

      addPendingAssignment(assignment)
      setSelectedStudent(null)
    },
    [addPendingAssignment, addRemovedAssignment, assignedSeats, pendingAssignments, removePendingAssignment, setSelectedStudent]
  )

  const allocationRules: Array<{
    id: AllocationRuleId
    title: string
    description: string
  }> = [
    {
      id: 'two-seater-left',
      title: '2-seater bench left',
      description: 'Assign a student to the left seat of a 2-seater bench.',
    },
    {
      id: 'two-seater-right',
      title: '2-seater bench right',
      description: 'Assign a student to the right seat of a 2-seater bench.',
    },
    {
      id: 'three-seater-middle',
      title: '3-seater bench middle',
      description: 'Place a student in the middle seat of a 3-seater bench.',
    },
    {
      id: 'three-seater-left-corner',
      title: '3-seater bench left corner',
      description: 'Place a student in the left corner seat of a 3-seater bench.',
    },
    {
      id: 'three-seater-right-corner',
      title: '3-seater bench right corner',
      description: 'Place a student in the right corner seat of a 3-seater bench.',
    },
    {
      id: 'three-seater-corners',
      title: '3-seater bench corner pair',
      description: 'Use one of the corner seats on a 3-seater bench.',
    },
  ]

  const handleStudentDragStart = (
    studentOrGroup: UnassignedStudentDto | StudentGroupDto,
    event: DragEvent<HTMLButtonElement>
  ) => {
    const dataType = 'groupId' in studentOrGroup ? 'application/student-group' : 'application/student'
    event.dataTransfer.setData(dataType, JSON.stringify(studentOrGroup))
    event.dataTransfer.effectAllowed = 'move'
  }

  const handleRuleDrop = (
    ruleId: AllocationRuleId,
    payload: UnassignedStudentDto | StudentGroupDto
  ) => {
    if (!displayHall || !displayHallGrid) {
      setSaveMessage('✗ No hall selected for rule-based assignment.')
      setTimeout(() => setSaveMessage(null), 4000)
      return
    }

    // Handle group allocation
    if ('students' in payload) {
      const assignments = applyAllocationRule(displayHall, displayHallGrid, payload, ruleId, {
        assignedSeats,
        pendingAssignments,
        removedAssignments,
        positionKey,
      })

      if (assignments.length === 0) {
        setSaveMessage('✗ No available seats match the selected rule.')
        setTimeout(() => setSaveMessage(null), 4000)
        return
      }

      // Check for conflicts - students already assigned
      const conflictingStudents = assignments.filter(assignment => {
        const studentKey = assignment.studentId
        return Array.from(pendingAssignments.values()).some((s) => {
          return s.studentId === studentKey || s.usn === assignment.usn
        }) || assignedSeats.some((s) => s.studentId === studentKey)
      })

      if (conflictingStudents.length > 0) {
        const conflictMessage = conflictingStudents.length === 1
          ? `✗ Student ${conflictingStudents[0].usn} is already assigned.`
          : `✗ ${conflictingStudents.length} students are already assigned.`
        setSaveMessage(conflictMessage)
        setTimeout(() => setSaveMessage(null), 5000)
        return
      }

      // Add all assignments atomically
      addPendingAssignments(assignments)

      const ruleTitle = allocationRules.find((rule) => rule.id === ruleId)?.title
      setSaveMessage(`✓ Assigned ${assignments.length} students using rule "${ruleTitle}".`)
      setTimeout(() => setSaveMessage(null), 4000)
      return
    }

    // Handle single student allocation (legacy behavior)
    const student = payload
    if (!student.studentId) {
      setSaveMessage('✗ Cannot assign a student without a linked account.')
      setTimeout(() => setSaveMessage(null), 5000)
      return
    }

    const singleStudentKey = student.studentId
    const singleAlreadyAssigned = Array.from(pendingAssignments.values()).some((s) => {
      return s.studentId === singleStudentKey || s.usn === student.usn
    }) || assignedSeats.some((s) => s.studentId === singleStudentKey)

    if (singleAlreadyAssigned) {
      setSaveMessage('✗ Student is already assigned to a seat.')
      setTimeout(() => setSaveMessage(null), 4000)
      return
    }

    // Use the bulk allocation engine for single students too
    const singleStudentGroup: StudentGroupDto = {
      groupId: `single-${student.studentId}`,
      label: student.studentName,
      students: [student]
    }

    const assignments = applyAllocationRule(displayHall, displayHallGrid, singleStudentGroup, ruleId, {
      assignedSeats,
      pendingAssignments,
      removedAssignments,
      positionKey,
    })

    if (assignments.length === 0) {
      setSaveMessage('✗ No available seats match the selected rule.')
      setTimeout(() => setSaveMessage(null), 4000)
      return
    }

    // Add the single assignment
    addPendingAssignments([assignments[0]])
    setSelectedStudent(null)
    const ruleTitle = allocationRules.find((rule) => rule.id === ruleId)?.title
    setSaveMessage(`✓ Assigned ${student.usn} using rule "${ruleTitle}".`)
    setTimeout(() => setSaveMessage(null), 4000)
  }
  const exportSeatAssignmentsAsCsv = () => {
    const assignmentMap = new Map<string, SeatDto>()
    assignedSeats.forEach((seat) => {
      const key = positionKey(seat)
      if (!removedAssignments.has(key)) {
        assignmentMap.set(key, {
          ...seat,
          usn: seat.usn ?? null,
          studentName: seat.studentName ?? null,
          branchCode: seat.branchCode ?? null,
          
          isManualOverride: seat.manualOverride ?? false,
        })
      }
    })
    pendingAssignments.forEach((seat) => assignmentMap.set(positionKey(seat), seat))

    if (assignmentMap.size === 0) {
      setSaveMessage('✗ No seating assignments available to export.')
      setTimeout(() => setSaveMessage(null), 4000)
      return
    }

    const hallMap = new Map(hallDtos.map((hall) => [hall.hallId, hall]))

    const rows = Array.from(assignmentMap.values()).sort((a, b) => {
      if (a.hallId !== b.hallId) return a.hallId.localeCompare(b.hallId)
      if (a.benchRow !== b.benchRow) return a.benchRow - b.benchRow
      if (a.benchCol !== b.benchCol) return a.benchCol - b.benchCol
      return a.benchSeatIndex - b.benchSeatIndex
    })

    const quote = (value: string | number | boolean | null | undefined) => {
      if (value === null || value === undefined) return ''
      const text = String(value).replace(/"/g, '""')
      return `"${text}"`
    }

    const csv = [
      ['Hall ID', 'Hall Name', 'Room Display Name', 'Building', 'Floor', 'Bench', 'Seat', 'USN', 'Student Name', 'Branch'].join(','),
      ...rows.map((seat) => {
        const hall = hallMap.get(seat.hallId)
        const hallName = hall?.roomName || ''
        const roomDisplayName = hall?.roomDisplayName || ''
        const building = hall?.building || ''
        const floor = hall?.floor ?? ''
        return [
          seat.hallId,
          hallName,
          roomDisplayName,
          building,
          floor,
          seat.benchNumber,
          seat.benchSeatIndex + 1,
          seat.usn,
          seat.studentName,
          seat.branchCode,
        ].map(quote).join(',')
      }),
    ].join('\n')

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `seating-assignments-${examId}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  const handleUploadClick = () => {
    fileInputRef.current?.click()
  }

  const uploadCsvFile = (file: File) => {
    const isCsv =
      file.type === 'text/csv' ||
      file.name.toLowerCase().endsWith('.csv')
    if (!isCsv) {
      setSaveMessage('✗ Please upload a .csv file only')
      setTimeout(() => setSaveMessage(null), 4000)
      return
    }
    uploadMutation.mutate(file)
  }

  const handleFileSelected = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return
    uploadCsvFile(file)
  }

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setIsDragOver(true)
  }

  const handleDragLeave = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setIsDragOver(false)
  }

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setIsDragOver(false)
    const file = event.dataTransfer.files?.[0]
    if (!file) return
    uploadCsvFile(file)
  }

  // ===== Auto-save =====
  useEffect(() => {
    if (!isDirty) return

    if (autoSaveTimeoutRef.current) {
      clearTimeout(autoSaveTimeoutRef.current)
    }

    autoSaveTimeoutRef.current = setTimeout(() => {
      saveMutation.mutate()
    }, 2000)

    return () => {
      if (autoSaveTimeoutRef.current) {
        clearTimeout(autoSaveTimeoutRef.current)
      }
    }
  }, [isDirty, saveMutation])

  // ===== Render =====
  if (isLoading || isExamLoading)
    return (
      <div className="flex items-center justify-center p-8">
        <Loader className="animate-spin" /> Loading dashboard...
      </div>
    )

  if (error || examError || !dashboardState)
    return (
      <div className="flex items-center justify-center p-8 text-red-600">
        <AlertCircle className="mr-2" />
        Failed to load seating dashboard
      </div>
    )

  const unassignedStudents = dashboardState.unassignedStudents ?? []
  const totalStudents = Number.isFinite(dashboardState.totalCount)
    ? dashboardState.totalCount
    : 0
  const examName = examSession?.name || 'Exam'
  const statusLabel = examSession?.status || 'UNKNOWN'
  const examSchedule = examSession
    ? `${examSession.examDate} | ${String(examSession.startTime).slice(0, 5)} - ${String(examSession.endTime).slice(0, 5)}`
    : 'Schedule not available'
  const lastModified = examSession?.updatedAt
    ? new Date(examSession.updatedAt).toLocaleString()
    : examSession?.createdAt
    ? new Date(examSession.createdAt).toLocaleString()
    : 'Unknown'

  const hallDtos: ExamHallDto[] = dashboardState.halls ?? []
  const hallGrids: HallGridDto[] = dashboardState.hallGrids ?? []
  const hallGridsById = new Map(hallGrids.map((grid) => [grid.hallId, grid]))
  const currentHall = hallDtos.find((h) => h.hallId === activeHallId)
  const displayHall = currentHall || hallDtos[0] || null
  const displayHallGrid = displayHall
    ? hallGridsById.get(displayHall.hallId) || hallGrids[0] || null
    : hallGrids[0] || null

  const currentAssignedCount = currentAssignedSeats.length + pendingAssignments.size
  const hallAssignedCount = new Map<string, number>()
  hallDtos.forEach((hall) => hallAssignedCount.set(hall.hallId, 0))
  currentAssignedSeats.forEach((seat) => {
    hallAssignedCount.set(seat.hallId, (hallAssignedCount.get(seat.hallId) ?? 0) + 1)
  })
  pendingAssignments.forEach((seat) => {
    hallAssignedCount.set(seat.hallId, (hallAssignedCount.get(seat.hallId) ?? 0) + 1)
  })

  const isPublished = statusLabel === 'PUBLISHED'
  const canPublish = currentAssignedCount === totalStudents && totalStudents > 0
  const allAssignedMessage =
    currentAssignedCount === totalStudents
      ? 'All students assigned ✓'
      : `${totalStudents - currentAssignedCount} students not assigned`

  if (hallDtos.length === 0) {
    return (
      <div className="flex flex-col gap-4 p-6 bg-gray-100 min-h-screen">
        <div className="flex justify-between items-start bg-white p-4 rounded-lg shadow">
          <div>
            <h1 className="text-2xl font-bold text-gray-800">{examName}</h1>
            <div className="text-sm text-gray-600">
              Status: <span className="font-semibold">{statusLabel}</span>
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Exam schedule: {examSchedule}
            </div>
            <div className="text-sm text-gray-600 mt-1">
              Last modified: {lastModified}
            </div>
          </div>
          <div className="text-right">
            <div className="text-3xl font-bold text-green-600">
              {currentAssignedCount}/{totalStudents}
            </div>
            <div className="text-sm text-gray-600">{allAssignedMessage}</div>
          </div>
        </div>

        <div className="flex-1 flex items-center justify-center bg-white rounded-lg shadow p-8 text-gray-700">
          No halls are configured yet for this exam. Add a hall to begin seating.
        </div>

        <div className="flex justify-end bg-white p-4 rounded-lg shadow">
          <button
            onClick={() => setIsAddHallModalOpen(true)}
            className="px-4 py-2 rounded bg-green-600 text-white font-semibold hover:bg-green-700 transition"
          >
            Add Hall
          </button>
        </div>

        <AddHallModal
          examId={examId}
          isOpen={isAddHallModalOpen}
          onClose={() => setIsAddHallModalOpen(false)}
          onSubmit={(config) => addHallMutation.mutate(config)}
          isSubmitting={addHallMutation.isPending}
        />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 p-6 bg-gray-100 min-h-screen">
      {/* Header */}
      <div className="flex justify-between items-start bg-white p-4 rounded-lg shadow">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">{examName}</h1>
          <div className="text-sm text-gray-600">
            Status: <span className="font-semibold">{statusLabel}</span>
          </div>
          <div className="text-sm text-gray-600 mt-1">
            Exam schedule: {examSchedule}
          </div>
          <div className="text-sm text-gray-600 mt-1">
            Last modified: {lastModified}
          </div>
          {displayHall?.invigilatorName ? (
            <div className="mt-2 rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-700 border border-blue-100">
              <span className="font-semibold">Hall Invigilator:</span> {displayHall.invigilatorName}
            </div>
          ) : null}
        </div>

        {/* Stats */}
        <div className="text-right">
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,text/csv"
            className="hidden"
            onChange={handleFileSelected}
          />
          <button
            onClick={handleUploadClick}
            className="mb-2 inline-flex items-center gap-2 rounded border border-blue-300 bg-blue-50 px-3 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-100 disabled:opacity-60"
            disabled={uploadMutation.isPending}
          >
            <Upload size={16} />
            {uploadMutation.isPending ? 'Uploading...' : 'Upload Students CSV'}
          </button>
          <div
            role="button"
            tabIndex={0}
            onClick={handleUploadClick}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                handleUploadClick()
              }
            }}
            className={`mb-3 rounded border border-dashed px-3 py-2 text-xs transition ${
              isDragOver
                ? 'border-blue-500 bg-blue-100 text-blue-800'
                : 'border-blue-300 bg-blue-50 text-blue-700 hover:bg-blue-100'
            }`}
          >
            Drag & drop CSV here, or click to browse
          </div>
          <div className="text-3xl font-bold text-green-600">
            {currentAssignedCount}/{totalStudents}
          </div>
          <div className="text-sm text-gray-600">{allAssignedMessage}</div>
        </div>
      </div>

      {/* Messages */}
      {saveMessage && (
        <div
          className={`p-3 rounded flex items-center gap-2 ${
            saveMessage.startsWith('✓')
              ? 'bg-green-100 text-green-700 border border-green-300'
              : 'bg-red-100 text-red-700 border border-red-300'
          }`}
        >
          {saveMessage.startsWith('✓') ? (
            <CheckCircle size={18} />
          ) : (
            <AlertCircle size={18} />
          )}
          {saveMessage}
        </div>
      )}

      {/* Hall tabs */}
      <div className="flex gap-2 overflow-x-auto items-center">
        {hallDtos.map((hall) => (
          <button
            key={hall.hallId}
            onClick={() => setActiveHallId(hall.hallId)}
            className={`px-4 py-2 rounded font-semibold text-sm transition whitespace-nowrap ${
              activeHallId === hall.hallId
                ? 'bg-blue-600 text-white'
                : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
            }`}
          >
            <div className="flex items-center gap-2">
              <span>{hall.roomName || 'Unknown room'}</span>
              {hall.invigilatorName ? (
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.08em] text-slate-600">
                  {hall.invigilatorName}
                </span>
              ) : null}
            </div>
            <span className="ml-2 text-xs">
              {(hallAssignedCount.get(hall.hallId) ?? 0)}/{hall.totalCapacity ?? 0}
            </span>
          </button>
        ))}
        <button
          onClick={() => setIsAddHallModalOpen(true)}
          className="px-4 py-2 rounded bg-green-100 text-green-700 border border-green-300 hover:bg-green-200 font-semibold text-sm transition flex items-center gap-2 whitespace-nowrap"
        >
          <Plus size={18} />
          Add Hall
        </button>
      </div>

      {/* Allocation Rules Section - Horizontal Layout */}
      <ErrorBoundary>
        <div>
          <div className="mb-4">
            <div className="text-sm font-semibold text-gray-800">Allocation rules</div>
            <p className="text-xs text-gray-500 mt-1">
              Drag a student card onto a rule card to assign them automatically.
            </p>
          </div>
          <div className="flex gap-3 overflow-x-auto pb-2 mb-6 rounded-lg bg-white p-3 shadow-sm">
            {allocationRules.map((rule) => (
              <div key={rule.id} className="flex-shrink-0 w-48">
                <AllocationRuleCard
                  rule={rule}
                  onRuleDrop={handleRuleDrop}
                />
              </div>
            ))}
          </div>
        </div>
      </ErrorBoundary>

      {/* Main content - Student Pool + Classroom Grid */}
      <ErrorBoundary>
        <div className="flex flex-col gap-4 lg:flex-row">
          {/* Left: Student Pool */}
          <div className="w-full lg:w-96 flex-shrink-0 overflow-y-auto max-h-[calc(100vh-500px)]">
            <StudentPool
              unassignedStudents={unassignedStudents}
              onStudentSelect={setSelectedStudent}
              onStudentDeselect={() => setSelectedStudent(null)}
              selectedStudent={selectedStudent}
              pendingAssignments={pendingAssignments}
              onStudentDragStart={handleStudentDragStart}
              isQuickAssignPending={quickAssignMutation.isPending}
              onQuickAssign={() => {
                setSaveMessage('✗ Quick assign is not available yet.')
                setTimeout(() => setSaveMessage(null), 3000)
              }}
            />
          </div>

          {/* Right: Classroom Grid */}
          {displayHallGrid ? (
            <div className="flex-1 overflow-auto max-h-[calc(100vh-500px)]">
              <ClassroomGrid
                hallGrid={displayHallGrid}
                assignedSeats={assignedSeats}
                selectedStudent={selectedStudent}
                pendingAssignments={pendingAssignments}
                removedAssignments={removedAssignments}
                onSeatClick={handleSeatClick}
              />
            </div>
          ) : (
            <div className="flex-1 rounded-lg border border-gray-300 bg-white p-6 text-gray-600">
              No hall layout available for the selected room.
            </div>
          )}
        </div>
      </ErrorBoundary>

      {/* Footer: Action buttons */}
      <div className="flex justify-end gap-2 bg-white p-4 rounded-lg shadow">
        {/* Clear Hall button */}
        <button
          onClick={() => {
            if (
              confirm(
                'Are you sure? This will remove all assignments from this hall.'
              )
            ) {
              if (displayHall?.hallId) {
                clearHallMutation.mutate(displayHall.hallId)
              }
            }
          }}
          className="px-4 py-2 bg-red-100 text-red-700 font-semibold rounded border border-red-300 hover:bg-red-200 transition disabled:opacity-50"
          disabled={!displayHall?.hallId || clearHallMutation.isPending}
        >
          {clearHallMutation.isPending ? 'Clearing...' : 'Clear Hall'}
        </button>

        {/* Delete Hall button */}
        <button
          onClick={() => {
            if (
              confirm(
                'Delete this hall and all associated seating data? This cannot be undone.'
              )
            ) {
              if (displayHall?.hallId) {
                deleteHallMutation.mutate(displayHall.hallId)
              }
            }
          }}
          className="px-4 py-2 bg-red-700 text-white font-semibold rounded border border-red-800 hover:bg-red-800 transition disabled:opacity-50"
          disabled={!displayHall?.hallId || deleteHallMutation.isPending}
        >
          {deleteHallMutation.isPending ? 'Deleting...' : 'Delete Hall'}
        </button>

        {/* Save button */}
        <button
          onClick={() => saveMutation.mutate()}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white font-semibold rounded hover:bg-blue-700 transition disabled:opacity-50"
          disabled={!isDirty || saveMutation.isPending}
        >
          <Save size={18} />
          {saveMutation.isPending ? 'Saving...' : 'Save & Exit'}
        </button>

        {/* Export CSV button */}
        <button
          onClick={exportSeatAssignmentsAsCsv}
          className="px-4 py-2 bg-slate-600 text-white font-semibold rounded border border-slate-700 hover:bg-slate-700 transition"
        >
          Export seating CSV
        </button>

        {/* Publish button */}
        <button
          onClick={() => {
            if (canPublish) {
              publishMutation.mutate()
            } else {
              alert('Cannot publish: not all students are assigned.')
            }
          }}
          className={`px-4 py-2 font-semibold rounded transition ${
            canPublish
              ? 'bg-green-600 text-white hover:bg-green-700'
              : 'bg-gray-300 text-gray-600 cursor-not-allowed'
          }`}
          disabled={!canPublish || publishMutation.isPending || isPublished}
        >
          {isPublished ? '✓ Published' : publishMutation.isPending ? 'Publishing...' : 'Publish'}
        </button>
      </div>

      {/* Add Hall Modal */}
      <AddHallModal
        examId={examId}
        isOpen={isAddHallModalOpen}
        onClose={() => setIsAddHallModalOpen(false)}
        onSubmit={(config) => addHallMutation.mutate(config)}
        isSubmitting={addHallMutation.isPending}
      />
    </div>
  )
}




