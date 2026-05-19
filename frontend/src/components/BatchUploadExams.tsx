/**
 * Batch Upload Exams from CSV Component
 * Allows users to upload a timetable CSV and create multiple exams at once
 */

import { useState, useRef } from 'react'
import { Upload, AlertCircle, CheckCircle, Loader, X } from 'lucide-react'
import { parseExamCSV, validateExams } from '@/utils/csvParser'
import { batchCreateExamSessions } from '@/services/examService'
import type { CreateExamSessionRequest, ExamSessionDto } from '@/types/exam'

interface BatchUploadExamsProps {
  onSuccess?: (exams: ExamSessionDto[]) => void
}

type UploadState = 'idle' | 'parsing' | 'creating' | 'success' | 'error'

export const BatchUploadExams = ({ onSuccess }: BatchUploadExamsProps) => {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [state, setState] = useState<UploadState>('idle')
  const [message, setMessage] = useState<string>('')
  const [results, setResults] = useState<{
    created: ExamSessionDto[]
    failed: Array<{ exam: CreateExamSessionRequest; error: string }>
  } | null>(null)
  const [isExpanded, setIsExpanded] = useState(false)

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    if (!file.name.endsWith('.csv')) {
      setMessage('Please select a CSV file')
      setState('error')
      return
    }

    setState('parsing')
    setMessage('Parsing CSV...')

    try {
      const content = await file.text()
      const exams = parseExamCSV(content)
      validateExams(exams)

      setState('creating')
      setMessage(`Creating ${exams.length} exam(s)...`)

      const batchResults = await batchCreateExamSessions(exams)
      setResults(batchResults)

      if (batchResults.failed.length === 0) {
        setState('success')
        setMessage(`✓ Successfully created ${batchResults.created.length} exam(s)`)
        onSuccess?.(batchResults.created)
      } else if (batchResults.created.length > 0) {
        setState('success')
        setMessage(
          `✓ Created ${batchResults.created.length}/${exams.length} exam(s). ${batchResults.failed.length} failed.`
        )
        onSuccess?.(batchResults.created)
      } else {
        setState('error')
        setMessage(`✗ Failed to create exams. See details below.`)
      }

      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    } catch (error) {
      setState('error')
      setMessage(error instanceof Error ? error.message : 'Failed to process CSV')
    }
  }

  const handleReset = () => {
    setState('idle')
    setMessage('')
    setResults(null)
    setIsExpanded(false)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  return (
    <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center justify-between px-4 py-3 font-semibold text-gray-800 hover:bg-gray-50 transition"
      >
        <div className="flex items-center gap-2">
          <Upload size={18} />
          Batch Upload Exams from CSV
        </div>
        <span className="text-sm text-gray-500">{isExpanded ? '▼' : '▶'}</span>
      </button>

      {isExpanded && (
        <div className="border-t border-gray-200 px-4 py-4">
          {/* CSV Template Info */}
          <div className="mb-4 p-3 bg-blue-50 rounded border border-blue-200">
            <p className="text-sm text-blue-900 font-medium mb-2">CSV Format Required:</p>
            <p className="text-xs text-blue-800 font-mono mb-2">
              subject_code, subject_name, exam_name, start_time, end_time, date, [semester], [section]
            </p>
            <p className="text-xs text-blue-800 mb-2">
              <strong>Example:</strong>
            </p>
            <p className="text-xs text-blue-800 font-mono">
              21CS51, Design and Analysis of Algorithms, DAA Exam, 10:00, 12:00, 2026-12-15, 5, A
            </p>
            <p className="text-xs text-blue-800 mt-2">
              • <strong>Date format:</strong> YYYY-MM-DD or DD/MM/YYYY
            </p>
            <p className="text-xs text-blue-800">
              • <strong>Time format:</strong> HH:MM (24-hour)
            </p>
            <p className="text-xs text-blue-800">
              • Semester and section are optional (defaults: semester=5, section=null)
            </p>
          </div>

          {/* File Upload Input */}
          <div className="mb-4">
            <label className="block">
              <div className="flex items-center justify-center px-4 py-6 border-2 border-dashed border-gray-300 rounded-lg hover:border-blue-400 cursor-pointer transition bg-gray-50 hover:bg-blue-50">
                <div className="text-center">
                  <Upload className="mx-auto h-8 w-8 text-gray-400 mb-2" />
                  <p className="text-sm font-medium text-gray-700">
                    {state === 'parsing' || state === 'creating' ? 'Processing...' : 'Click to select CSV file'}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">or drag and drop</p>
                </div>
              </div>
              <input
                ref={fileInputRef}
                type="file"
                accept=".csv"
                onChange={handleFileSelect}
                disabled={state === 'parsing' || state === 'creating'}
                className="hidden"
              />
            </label>
          </div>

          {/* Status Message */}
          {message && (
            <div
              className={`mb-4 p-3 rounded-lg flex items-start gap-2 ${
                state === 'success'
                  ? 'bg-green-50 border border-green-200'
                  : state === 'error'
                    ? 'bg-red-50 border border-red-200'
                    : 'bg-blue-50 border border-blue-200'
              }`}
            >
              {state === 'parsing' || state === 'creating' ? (
                <Loader className="h-5 w-5 animate-spin flex-shrink-0 mt-0.5 text-blue-600" />
              ) : state === 'success' ? (
                <CheckCircle className="h-5 w-5 flex-shrink-0 mt-0.5 text-green-600" />
              ) : state === 'error' ? (
                <AlertCircle className="h-5 w-5 flex-shrink-0 mt-0.5 text-red-600" />
              ) : null}
              <div className="flex-1">
                <p
                  className={`text-sm font-medium ${
                    state === 'success'
                      ? 'text-green-900'
                      : state === 'error'
                        ? 'text-red-900'
                        : 'text-blue-900'
                  }`}
                >
                  {message}
                </p>
              </div>
            </div>
          )}

          {/* Results Details */}
          {results && (
            <div className="mb-4 space-y-3 max-h-64 overflow-y-auto">
              {results.created.length > 0 && (
                <div>
                  <p className="text-sm font-semibold text-green-700 mb-2">
                    ✓ Successfully Created ({results.created.length}):
                  </p>
                  <div className="space-y-1">
                    {results.created.map((exam) => (
                      <p key={exam.examId} className="text-xs text-green-600 bg-green-50 p-2 rounded">
                        {exam.name} ({exam.subjectCode})
                      </p>
                    ))}
                  </div>
                </div>
              )}

              {results.failed.length > 0 && (
                <div>
                  <p className="text-sm font-semibold text-red-700 mb-2">
                    ✗ Failed ({results.failed.length}):
                  </p>
                  <div className="space-y-1">
                    {results.failed.map((failure, idx) => (
                      <p key={idx} className="text-xs text-red-600 bg-red-50 p-2 rounded">
                        <strong>{failure.exam.name || 'Unknown'}</strong>: {failure.error}
                      </p>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Action Buttons */}
          {state !== 'idle' && (
            <div className="flex justify-end gap-2">
              <button
                onClick={handleReset}
                className="flex items-center gap-2 px-3 py-2 bg-gray-100 text-gray-700 font-medium rounded hover:bg-gray-200 transition text-sm"
              >
                <X size={16} />
                Reset
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default BatchUploadExams
