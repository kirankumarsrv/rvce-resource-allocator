import { useState } from 'react'
import { uploadTimetable } from '../services/timetableService'
import { UploadResultDto } from '../types/timetable'

const UploadTimetablePage = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [uploadResult, setUploadResult] = useState<UploadResultDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isUploading, setIsUploading] = useState(false)

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setError(null)
    setUploadResult(null)
    const file = event.target.files?.[0] ?? null
    setSelectedFile(file)
  }

  const handleUpload = async () => {
    if (!selectedFile) return

    setIsUploading(true)
    setError(null)
    setUploadResult(null)

    try {
      const result = await uploadTimetable(selectedFile)
      setUploadResult(result)
    } catch (uploadError: unknown) {
      const errorMessage = uploadError instanceof Error ? uploadError.message : 'Unable to upload timetable'
      setError(errorMessage)
    } finally {
      setIsUploading(false)
    }
  }

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Timetable Upload</h1>
      <div className="bg-white rounded-xl shadow p-6 max-w-3xl">
        <p className="text-gray-600 mb-6">
          Upload a CSV timetable file to parse and persist schedule data. This action is available to TTO users.
        </p>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">CSV File</label>
            <input
              type="file"
              accept=".csv"
              onChange={handleFileChange}
              className="block w-full text-sm text-gray-700 file:border file:border-gray-300 file:px-4 file:py-2 file:rounded-md file:bg-slate-50"
            />
          </div>

          <button
            disabled={!selectedFile || isUploading}
            onClick={handleUpload}
            className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-6 py-3 text-white transition hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {isUploading ? 'Uploading…' : 'Upload Timetable'}
          </button>

          {error && (
            <div className="rounded-lg bg-red-50 border border-red-200 p-4 text-sm text-red-700">
              {error}
            </div>
          )}

          {uploadResult && (
            <div className="rounded-lg bg-green-50 border border-green-200 p-4 text-sm text-green-700">
              <p className="font-semibold">Upload completed successfully</p>
              <p>Success count: {uploadResult.insertedCount}</p>
              <p>Error count: {uploadResult.errorCount}</p>
              {uploadResult.errors && uploadResult.errors.length > 0 && (
                <div className="mt-3 space-y-2">
                  <p className="font-semibold">Errors</p>
                  <ul className="list-disc list-inside text-sm text-slate-700">
                    {uploadResult.errors.map((item, index) => (
                      <li key={index}>{item}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default UploadTimetablePage
