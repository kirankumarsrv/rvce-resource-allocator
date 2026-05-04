import { useState } from 'react';
import { TeacherScheduleDisplay } from '../components/TeacherScheduleDisplay';

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
