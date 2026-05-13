import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getTeacherAssignedExams } from '@/services/examService';
import type { TeacherAssignedExamDto } from '@/types/exam';
import { TeacherScheduleDisplay } from '../components/TeacherScheduleDisplay';
import { useAuth } from '@/context/AuthContext';

/**
 * Teacher Portal Page
 *
 * Main dashboard for teachers providing access to:
 * - Personal schedule viewing
 * - Room availability lookup
 * - Other teacher-specific features
 */
const TeacherPage = () => {
  const navigate = useNavigate()
  const auth = useAuth();
  const [teacherId, setTeacherId] = useState<string>(auth.user?.userId ?? '');
  const [showSchedule, setShowSchedule] = useState(false);

  const { data: assignedExams, isLoading: examsLoading, isError: examsError, error: examsErrorMsg } = useQuery<TeacherAssignedExamDto[]>({
    queryKey: ['teacher-assigned-exams'],
    queryFn: getTeacherAssignedExams,
    refetchInterval: 20000,
  });

  const handleViewSchedule = () => {
    if (teacherId.trim()) {
      setShowSchedule(true);
    }
  };

  const formatDate = (value: string) => {
    try {
      return new Date(value).toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return value;
    }
  };

  const formatTime = (value: string) => {
    return value?.slice(0, 5) ?? value;
  };

  return (
    <div className="p-6">
      <div className="mb-8">
        <h1 data-test-id="teacher-dashboard-heading" className="text-3xl font-bold mb-4">Teacher Portal</h1>
        {auth.user?.userId ? (
          <p className="text-sm text-slate-600 mb-4">
            Your Teacher UUID: <span className="font-semibold text-slate-900">{auth.user.userId}</span>
          </p>
        ) : null}

        {/* Assigned Exams Section */}
        <div className="mb-8">
          <h2 className="text-xl font-semibold mb-4">Assigned Exams (Invigilation)</h2>
          {examsLoading ? (
            <div className="rounded-xl border border-slate-200 bg-white p-6 text-slate-600">Loading assigned exams...</div>
          ) : examsError ? (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-700">
              {(examsErrorMsg as Error)?.message ?? 'Unable to load assigned exams'}
            </div>
          ) : !assignedExams || assignedExams.length === 0 ? (
            <div className="rounded-xl border border-slate-200 bg-white p-6 text-slate-700">
              No assigned exams yet.
            </div>
          ) : (
            <div className="grid gap-4 md:grid-cols-2">
              {assignedExams.map((exam) => (
                <article
                  key={exam.examId}
                  className="cursor-pointer rounded-2xl border border-slate-200 bg-white p-5 shadow-sm hover:shadow-md transition-shadow"
                  onClick={() => navigate(`/exam/${exam.examId}/seating`)}
                >
                  <div className="mb-3 flex items-center justify-between">
                    <h3 className="text-lg font-semibold text-slate-900">{exam.examName}</h3>
                    <span className="rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-700">
                      {exam.status}
                    </span>
                  </div>
                  <div className="space-y-1 text-sm text-slate-700">
                    <p>
                      <span className="font-medium">Subject:</span> {exam.subjectCode} - {exam.subjectName}
                    </p>
                    <p>
                      <span className="font-medium">Exam:</span> {formatDate(exam.examDate)} | {formatTime(exam.startTime)} - {formatTime(exam.endTime)}
                    </p>
                    <p>
                      <span className="font-medium">Hall:</span> {exam.hallName ?? 'Pending assignment'}
                    </p>
                    <p>
                      <span className="font-medium">Room:</span> {exam.roomName ?? 'Pending assignment'}
                    </p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>

        {/* Quick Actions Navigation */}
        <div className="grid grid-cols-1 gap-4 mb-6">
          <Link
            to="/teacher/rooms"
            className="bg-blue-600 hover:bg-blue-700 text-white p-4 rounded-lg shadow-md transition-colors duration-200"
          >
            <div className="flex items-center">
              <svg className="w-6 h-6 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <div>
                <h3 className="font-semibold">View Room Availability</h3>
                <p className="text-sm opacity-90">Check which rooms are free at a given time</p>
              </div>
            </div>
          </Link>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-4">View Schedule</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Teacher ID (UUID)
              </label>
              <input
                type="text"
                value={teacherId}
                onChange={(e) => setTeacherId(e.target.value)}
                placeholder="Enter teacher UUID..."
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <p className="text-xs text-gray-500 mt-1">
                Paste a valid UUID to view that teacher's weekly schedule
              </p>
            </div>
            <button
              onClick={handleViewSchedule}
              disabled={!teacherId.trim()}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              View Schedule
            </button>
          </div>
        </div>
      </div>

      {showSchedule && teacherId && (
        <div className="bg-white p-6 rounded-lg shadow">
          <TeacherScheduleDisplay teacherId={teacherId} />
        </div>
      )}
    </div>
  );
};

export default TeacherPage;
