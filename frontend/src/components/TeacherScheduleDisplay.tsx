/**
 * TeacherScheduleDisplay Component - T-105: Teacher Schedule Visualization
 *
 * Displays a teacher's complete weekly schedule in a tabular format.
 * Shows all scheduled slots across 7 days with room and subject information.
 * Used in both Teacher and TTO portals for schedule viewing.
 */

import { useEffect, useState } from 'react';
import { TeacherWeeklySchedule } from '../types/timetable';
import { getTeacherWeeklySchedule } from '../services/timetableService';

interface ScheduleDisplayProps {
  /** UUID of the teacher whose schedule to display */
  teacherId: string;
}

const dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

/**
 * Component that fetches and displays a teacher's weekly schedule
 * Handles loading states, errors, and empty schedules gracefully
 */
export const TeacherScheduleDisplay = ({ teacherId }: ScheduleDisplayProps) => {
  const [schedule, setSchedule] = useState<TeacherWeeklySchedule | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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

  return (
    <div className="space-y-4">
      <h2 className="text-2xl font-bold mb-4">Weekly Schedule</h2>
      {schedule && Object.entries(schedule).map(([dayNum, slots]) => (
        <div key={dayNum} className="border rounded-lg p-4 bg-white shadow">
          <h3 className="font-semibold text-lg mb-3 text-blue-600">
            {dayNames[parseInt(dayNum) - 1]}
          </h3>
          {slots && slots.length > 0 ? (
            <div className="space-y-2">
              {slots.map((slot: any) => (
                <div
                  key={slot.slotId}
                  className="bg-gray-50 p-3 rounded border-l-4 border-blue-500"
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
};
