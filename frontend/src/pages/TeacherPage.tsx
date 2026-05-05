import { useState } from 'react';
import { Link } from 'react-router-dom';
import { TeacherScheduleDisplay } from '../components/TeacherScheduleDisplay';

/**
 * Teacher Portal Page
 *
 * Main dashboard for teachers providing access to:
 * - Personal schedule viewing
 * - Room availability and booking
 * - Other teacher-specific features
 */
const TeacherPage = () => {
  const [teacherId, setTeacherId] = useState<string>('');
  const [showSchedule, setShowSchedule] = useState(false);

  const handleViewSchedule = () => {
    if (teacherId.trim()) {
      setShowSchedule(true);
    }
  };

  return (
    <div className="p-6">
      <div className="mb-8">
        <h1 className="text-3xl font-bold mb-4">Teacher Portal</h1>

        {/* Quick Actions Navigation */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <Link
            to="/teacher/rooms"
            className="bg-blue-600 hover:bg-blue-700 text-white p-4 rounded-lg shadow-md transition-colors duration-200"
          >
            <div className="flex items-center">
              <svg className="w-6 h-6 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
              </svg>
              <div>
                <h3 className="font-semibold">Find Available Rooms</h3>
                <p className="text-sm opacity-90">Book rooms for your classes</p>
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
