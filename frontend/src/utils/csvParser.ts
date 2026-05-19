/**
 * CSV Parser Utility
 * Handles parsing of exam timetable CSV files
 */

import type { CreateExamSessionRequest } from '@/types/exam'

export interface ParsedExam extends CreateExamSessionRequest {
  semester: number
}

/**
 * Parse CSV data into exam records
 * Expected columns (case-insensitive, can have variations):
 * - subject / subjectCode / subject_code / code
 * - subjectName / subject_name / subject / name
 * - examName / exam_name / exam
 * - startTime / start_time / start / from
 * - endTime / end_time / end / to
 * - date / examDate / exam_date
 * - semester (optional)
 * - section (optional)
 */
export const parseExamCSV = (csvContent: string): ParsedExam[] => {
  const lines = csvContent.trim().split('\n')
  if (lines.length < 2) {
    throw new Error('CSV must contain header and at least one data row')
  }

  // Parse header row (case-insensitive)
  const headerLine = lines[0].toLowerCase()
  const headers = headerLine.split(',').map((h) => h.trim())

  // Find column indices
  const findColumnIndex = (patterns: string[]): number => {
    for (let i = 0; i < headers.length; i++) {
      const header = headers[i]
      if (patterns.some((p) => header.includes(p))) {
        return i
      }
    }
    return -1
  }

  const subjectCodeIdx = findColumnIndex(['subject', 'code', 'subject_code'])
  const subjectNameIdx = findColumnIndex(['subject_name', 'name'])
  const examNameIdx = findColumnIndex(['exam', 'exam_name'])
  const startTimeIdx = findColumnIndex(['start', 'from', 'time', 'start_time'])
  const endTimeIdx = findColumnIndex(['end', 'to', 'end_time'])
  const dateIdx = findColumnIndex(['date', 'exam_date'])
  const semesterIdx = findColumnIndex(['semester', 'sem'])
  const sectionIdx = findColumnIndex(['section', 'sec'])

  if (
    subjectCodeIdx === -1 ||
    subjectNameIdx === -1 ||
    startTimeIdx === -1 ||
    endTimeIdx === -1 ||
    dateIdx === -1
  ) {
    throw new Error(
      'CSV must contain columns for: subject code, subject name, start time, end time, and date'
    )
  }

  const exams: ParsedExam[] = []

  // Parse data rows
  for (let i = 1; i < lines.length; i++) {
    const line = lines[i].trim()
    if (!line) continue // Skip empty lines

    const values = line.split(',').map((v) => v.trim())

    const subjectCode = values[subjectCodeIdx]?.trim()
    const subjectName = values[subjectNameIdx]?.trim()
    const examName = values[examNameIdx]?.trim() || `${subjectCode} Exam`
    const startTime = values[startTimeIdx]?.trim()
    const endTime = values[endTimeIdx]?.trim()
    const date = values[dateIdx]?.trim()
    const semester = Number(values[semesterIdx]) || 5
    const section = values[sectionIdx]?.trim() || null

    if (!subjectCode || !subjectName || !startTime || !endTime || !date) {
      throw new Error(`Row ${i + 1}: Missing required fields`)
    }

    // Validate date format (YYYY-MM-DD)
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date) && !/^\d{2}\/\d{2}\/\d{4}$/.test(date)) {
      throw new Error(
        `Row ${i + 1}: Invalid date format. Use YYYY-MM-DD or DD/MM/YYYY`
      )
    }

    // Convert date if needed
    let examDate = date
    if (/^\d{2}\/\d{2}\/\d{4}$/.test(date)) {
      const [day, month, year] = date.split('/')
      examDate = `${year}-${month}-${day}`
    }

    // Validate time format (HH:MM)
    if (!/^\d{2}:\d{2}$/.test(startTime) || !/^\d{2}:\d{2}$/.test(endTime)) {
      throw new Error(
        `Row ${i + 1}: Invalid time format. Use HH:MM`
      )
    }

    exams.push({
      name: examName,
      subjectCode,
      subjectName,
      section,
      semester,
      examDate,
      startTime,
      endTime,
    })
  }

  if (exams.length === 0) {
    throw new Error('No valid exam records found in CSV')
  }

  return exams
}

/**
 * Validate exam array before batch creation
 */
export const validateExams = (exams: ParsedExam[]): void => {
  if (!Array.isArray(exams) || exams.length === 0) {
    throw new Error('No exams to create')
  }

  exams.forEach((exam, idx) => {
    if (!exam.name || !exam.subjectCode || !exam.subjectName || !exam.examDate || !exam.startTime || !exam.endTime) {
      throw new Error(`Exam ${idx + 1}: Missing required fields`)
    }

    if (exam.semester < 1 || exam.semester > 8) {
      throw new Error(`Exam ${idx + 1}: Semester must be between 1 and 8`)
    }
  })
}
