import { useState } from 'react'
import type { DragEvent } from 'react'
import type { StudentGroupDto, UnassignedStudentDto } from '@/types/exam'

export type AllocationRuleId =
  | 'two-seater-left'
  | 'two-seater-right'
  | 'three-seater-middle'
  | 'three-seater-left-corner'
  | 'three-seater-right-corner'
  | 'three-seater-corners'

interface AllocationRule {
  id: AllocationRuleId
  title: string
  description: string
}

interface AllocationRuleCardProps {
  rule: AllocationRule
  onRuleDrop: (ruleId: AllocationRuleId, payload: UnassignedStudentDto | StudentGroupDto) => void
}

export const AllocationRuleCard = ({ rule, onRuleDrop }: AllocationRuleCardProps) => {
  const [isDragOver, setIsDragOver] = useState(false)

  const handleDragOver = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setIsDragOver(true)
  }

  const handleDragLeave = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setIsDragOver(false)
  }

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setIsDragOver(false)
    const studentPayload = event.dataTransfer.getData('application/student')
    const groupPayload = event.dataTransfer.getData('application/student-group')
    const payload = studentPayload || groupPayload
    if (!payload) {
      if (studentPayload || groupPayload) {
        // It is possible for the dataTransfer payload not to be valid JSON.
        // eslint-disable-next-line no-console
        console.warn('AllocationRuleCard drop had payload but no usable JSON', {
          ruleId: rule.id,
          studentPayload,
          groupPayload,
        })
      }
      return
    }

    try {
      const parsed = JSON.parse(payload) as UnassignedStudentDto | StudentGroupDto
      onRuleDrop(rule.id, parsed)
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('AllocationRuleCard failed to parse drop payload', {
        ruleId: rule.id,
        payload,
        error,
      })
    }
  }

  return (
    <div
      className={`rounded-lg border p-4 bg-white shadow-sm transition ${
        isDragOver ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300 hover:bg-slate-50'
      }`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <h4 className="text-sm font-semibold text-gray-800">{rule.title}</h4>
      <p className="text-xs text-gray-600 mt-2">{rule.description}</p>
      <div className="mt-3 rounded bg-slate-100 px-2 py-1 text-[11px] text-slate-700 font-semibold">
        Drop a student here
      </div>
    </div>
  )
}
