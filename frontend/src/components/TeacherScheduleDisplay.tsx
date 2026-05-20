/**
 * TeacherScheduleDisplay Component - T-105: Teacher Schedule Visualization
 *
 * Displays a teacher's complete weekly schedule in a tabular format.
 * Shows all scheduled slots across 7 days with room and subject information.
 * Used in both Teacher and TTO portals for schedule viewing.
 */

import { useEffect, useState } from 'react';
import { TeacherWeeklySchedule, TeacherScheduleDto } from '../types/timetable';
import { getTeacherWeeklySchedule } from '../services/timetableService';

interface ScheduleDisplayProps {
  /** UUID of the teacher whose schedule to display */
  teacherId: string;
}

const dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

/**
 * Get color classes based on department for visual differentiation
 */
const getDepartmentColor = (department: string) => {
  switch (department?.toUpperCase()) {
    case 'CSE':
    case 'COMPUTER SCIENCE':
      return 'border-blue-500 bg-blue-50';
    case 'ECE':
    case 'ELECTRONICS':
      return 'border-green-500 bg-green-50';
    case 'MECH':
    case 'MECHANICAL':
      return 'border-orange-500 bg-orange-50';
    case 'CIVIL':
      return 'border-yellow-500 bg-yellow-50';
    case 'EEE':
    case 'ELECTRICAL':
      return 'border-purple-500 bg-purple-50';
    default:
      return 'border-gray-500 bg-gray-50';
  }
};

/**
 * Component that fetches and displays a teacher's weekly schedule
 * Handles loading states, errors, and empty schedules gracefully
 */
export const TeacherScheduleDisplay = ({ teacherId }: ScheduleDisplayProps) => {
  const [schedule, setSchedule] = useState<TeacherWeeklySchedule | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'list' | 'calendar'>('list');

  useEffect(() => {
    const fetchSchedule = async () => {
      try {
        setLoading(true);
        const data = await getTeacherWeeklySchedule(teacherId);
        setSchedule(data);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load schedule');
        setSchedule(null);
      } finally {
        setLoading(false);
      }
    };

    if (teacherId) fetchSchedule();
  }, [teacherId]);

  if (loading) return <div className="text-center p-4">Loading schedule...</div>;
  if (error) return <div className="bg-red-100 text-red-800 p-4 rounded">{error}</div>;

  const renderListView = () => (
    <div className="space-y-4">
      {schedule && Object.entries(schedule).map(([dayNum, slots]) => (
        <div key={dayNum} className="border rounded-lg p-4 bg-white shadow">
          <h3 className="font-semibold text-lg mb-3 text-blue-600">
            {dayNames[parseInt(dayNum) - 1]}
          </h3>
          {slots && slots.length > 0 ? (
            <div className="space-y-2">
              {slots.map((slot: TeacherScheduleDto) => (
                <div
                  key={slot.slotId}
                  className={`p-3 rounded border-l-4 ${getDepartmentColor(slot.department)}`}
                >
                  <div className="font-semibold">{slot.subject}</div>
                  <div className="text-sm text-gray-600">
                    {slot.startTime} - {slot.endTime}
                  </div>
                  <div className="text-sm text-gray-600">
                    {slot.roomName} ({slot.roomBuilding})
                  </div>
                  <div className="text-xs text-gray-500">Dept: {slot.department}</div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500">No classes scheduled</p>
          )}
        </div>
      ))}
    </div>
  );

  const renderCalendarView = () => {
    const timeSlots = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'];
    
    return (
      <div className="overflow-x-auto">
        <table className="min-w-full bg-white border border-gray-300 text-sm">
          <thead>
            <tr>
              <th className="border border-gray-300 px-2 py-1 bg-gray-100 text-xs">Time</th>
              {dayNames.map(day => (
                <th key={day} className="border border-gray-300 px-2 py-1 bg-gray-100 text-xs hidden sm:table-cell">{day.slice(0, 3)}</th>
              ))}
              {/* Mobile view: show abbreviated days */}
              <th className="border border-gray-300 px-2 py-1 bg-gray-100 text-xs sm:hidden">Mon-Fri</th>
            </tr>
          </thead>
          <tbody>
            {timeSlots.map(time => (
              <tr key={time}>
                <td className="border border-gray-300 px-2 py-1 font-semibold bg-gray-50 text-xs">{time}</td>
                {/* Desktop view: full week */}
                {dayNames.map((day, dayIndex) => {
                  const dayNum = dayIndex + 1;
                  const daySlots = schedule?.[dayNum] || [];
                  const slotAtTime = daySlots.find(slot => slot.startTime.startsWith(time));
                  
                  return (
                    <td key={`${day}-${time}`} className="border border-gray-300 px-1 py-1 min-h-[50px] align-top hidden sm:table-cell">
                      {slotAtTime ? (
                        <div className={`p-1 rounded text-xs ${getDepartmentColor(slotAtTime.department)}`}>
                          <div className="font-semibold truncate">{slotAtTime.subject}</div>
                          <div className="text-gray-600 text-xs">
                            {slotAtTime.startTime}-{slotAtTime.endTime}
                          </div>
                          <div className="text-gray-600 truncate text-xs">
                            {slotAtTime.roomName}
                          </div>
                        </div>
                      ) : (
                        <div className="text-gray-300 text-xs">-</div>
                      )}
                    </td>
                  );
                })}
                {/* Mobile view: Mon-Fri combined */}
                <td className="border border-gray-300 px-1 py-1 min-h-[50px] align-top sm:hidden">
                  <div className="space-y-1">
                    {[1, 2, 3, 4, 5].map(dayNum => {
                      const daySlots = schedule?.[dayNum] || [];
                      const slotAtTime = daySlots.find(slot => slot.startTime.startsWith(time));
                      return slotAtTime ? (
                        <div key={dayNum} className={`p-1 rounded text-xs mb-1 ${getDepartmentColor(slotAtTime.department)}`}>
                          <div className="font-semibold truncate">{dayNames[dayNum-1].slice(0, 3)}: {slotAtTime.subject}</div>
                          <div className="text-gray-600 text-xs truncate">
                            {slotAtTime.roomName}
                          </div>
                        </div>
                      ) : null;
                    })}
                    {![1, 2, 3, 4, 5].some(dayNum => {
                      const daySlots = schedule?.[dayNum] || [];
                      return daySlots.some(slot => slot.startTime.startsWith(time));
                    }) && <div className="text-gray-300 text-xs">-</div>}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Weekly Schedule</h2>
        <div className="flex space-x-2">
          <button
            onClick={() => setViewMode('list')}
            className={`px-4 py-2 rounded ${viewMode === 'list' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700'}`}
          >
            List View
          </button>
          <button
            onClick={() => setViewMode('calendar')}
            className={`px-4 py-2 rounded ${viewMode === 'calendar' ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700'}`}
          >
            Calendar View
          </button>
        </div>
      </div>
      
      {viewMode === 'list' ? renderListView() : renderCalendarView()}
    </div>
  );
};
