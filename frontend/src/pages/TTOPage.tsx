import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AnalyticsDashboard } from '../components/AnalyticsDashboard';
import { TeacherScheduleDisplay } from '../components/TeacherScheduleDisplay';
import { RoomScheduleDisplay } from '../components/RoomScheduleDisplay';

/**
 * Time Table Officer Portal Page
 *
 * Main dashboard for TTO users providing access to:
 * - Timetable analytics and utilization metrics
 * - Teacher and room schedule viewing
 * - Room availability management
 * - Timetable upload and management features
 */
const TTOPage = () => {
  const [activeTab, setActiveTab] = useState<'analytics' | 'teacher' | 'room'>('analytics');
  const [teacherId, setTeacherId] = useState<string>('');
  const [roomId, setRoomId] = useState<number | null>(null);
  const [showTeacher, setShowTeacher] = useState(false);
  const [showRoom, setShowRoom] = useState(false);

  const handleViewTeacher = () => {
    if (teacherId.trim()) {
      setShowTeacher(true);
    }
  };

  const handleViewRoom = () => {
    if (roomId !== null && roomId > 0) {
      setShowRoom(true);
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-6">Time Table Officer Portal</h1>

      {/* Quick Actions Navigation */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <Link
          to="/tto/upload"
          className="bg-blue-600 hover:bg-blue-700 text-white p-4 rounded-lg shadow-md transition-colors duration-200"
        >
          <div className="flex items-center">
            <svg className="w-6 h-6 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            <div>
              <h3 className="font-semibold">Upload Timetable</h3>
              <p className="text-sm opacity-90">Upload CSV timetable data</p>
            </div>
          </div>
        </Link>

        <Link
          to="/tto/substitute"
          className="bg-indigo-600 hover:bg-indigo-700 text-white p-4 rounded-lg shadow-md transition-colors duration-200"
        >
          <div className="flex items-center">
            <svg className="w-6 h-6 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <div>
              <h3 className="font-semibold">Teacher Substitution</h3>
              <p className="text-sm opacity-90">Assign substitute teachers</p>
            </div>
          </div>
        </Link>

        <Link
          to="/tto/overrides"
          className="bg-emerald-600 hover:bg-emerald-700 text-white p-4 rounded-lg shadow-md transition-colors duration-200"
        >
          <div className="flex items-center">
            <svg className="w-6 h-6 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7M4 6h16M4 18h16" />
            </svg>
            <div>
              <h3 className="font-semibold">Manage Overrides</h3>
              <p className="text-sm opacity-90">Cancel or book slots</p>
            </div>
          </div>
        </Link>

        <Link
          to="/tto/rooms"
          className="bg-slate-600 hover:bg-slate-700 text-white p-4 rounded-lg shadow-md transition-colors duration-200"
        >
          <div className="flex items-center">
            <svg className="w-6 h-6 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M3 6h18M3 14h18M3 18h18" />
            </svg>
            <div>
              <h3 className="font-semibold">Manage Room Availability</h3>
              <p className="text-sm opacity-90">View and manage room bookings</p>
            </div>
          </div>
        </Link>
      </div>

      {/* Tab Navigation */}
      <div className="flex gap-2 mb-6 border-b border-gray-200">
        <button
          onClick={() => setActiveTab('analytics')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'analytics'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-600 hover:text-gray-800'
          }`}
        >
          Analytics
        </button>
        <button
          onClick={() => setActiveTab('teacher')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'teacher'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-600 hover:text-gray-800'
          }`}
        >
          Teacher Schedule
        </button>
        <button
          onClick={() => setActiveTab('room')}
          className={`px-4 py-2 font-semibold ${
            activeTab === 'room'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-600 hover:text-gray-800'
          }`}
        >
          Room Schedule
        </button>
      </div>

      {/* Analytics Tab */}
      {activeTab === 'analytics' && (
        <div className="bg-gray-50 p-6 rounded-lg">
          <AnalyticsDashboard />
        </div>
      )}

      {/* Teacher Schedule Tab */}
      {activeTab === 'teacher' && (
        <div>
          <div className="bg-white p-6 rounded-lg shadow mb-6">
            <h2 className="text-xl font-semibold mb-4">View Teacher Schedule</h2>
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
              </div>
              <button
                onClick={handleViewTeacher}
                disabled={!teacherId.trim()}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                View Schedule
              </button>
            </div>
          </div>

          {showTeacher && teacherId && (
            <div className="bg-white p-6 rounded-lg shadow">
              <TeacherScheduleDisplay teacherId={teacherId} />
            </div>
          )}
        </div>
      )}

      {/* Room Schedule Tab */}
      {activeTab === 'room' && (
        <div>
          <div className="bg-white p-6 rounded-lg shadow mb-6">
            <h2 className="text-xl font-semibold mb-4">View Room Schedule</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Room ID
                </label>
                <input
                  type="number"
                  value={roomId || ''}
                  onChange={(e) => setRoomId(e.target.value ? parseInt(e.target.value) : null)}
                  placeholder="Enter room ID..."
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <button
                onClick={handleViewRoom}
                disabled={roomId === null || roomId <= 0}
                className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
              >
                View Schedule
              </button>
            </div>
          </div>

          {showRoom && roomId && (
            <div className="bg-white p-6 rounded-lg shadow">
              <RoomScheduleDisplay roomId={roomId} />
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default TTOPage;
