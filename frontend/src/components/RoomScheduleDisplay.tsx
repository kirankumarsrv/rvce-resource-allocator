/**
 * RoomScheduleDisplay Component - T-105: Room Schedule Visualization
 *
 * Displays a room's complete weekly schedule in a tabular format.
 * Shows all scheduled slots across 7 days with teacher and subject information.
 * Used in TTO portal for room utilization monitoring and scheduling.
 */

import { useEffect, useState } from 'react';
import { RoomWeeklySchedule, RoomScheduleDto } from '../types/timetable';
import { getRoomWeeklySchedule } from '../services/timetableService';

interface RoomScheduleProps {
  /** Numeric ID of the room whose schedule to display */
  roomId: number;
}

const dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

/**
 * Component that fetches and displays a room's weekly schedule
 * Handles loading states, errors, and empty schedules gracefully
 * Shows teacher assignments and subjects for each time slot
 */
export const RoomScheduleDisplay = ({ roomId }: RoomScheduleProps) => {
  const [schedule, setSchedule] = useState<RoomWeeklySchedule | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSchedule = async () => {
      try {
        setLoading(true);
        const data = await getRoomWeeklySchedule(roomId);
        setSchedule(data);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load schedule');
        setSchedule(null);
      } finally {
        setLoading(false);
      }
    };

    if (roomId) fetchSchedule();
  }, [roomId]);

  if (loading) return <div className="text-center p-4">Loading room schedule...</div>;
  if (error) return <div className="bg-red-100 text-red-800 p-4 rounded">{error}</div>;

  return (
    <div className="space-y-4">
      <h2 className="text-2xl font-bold mb-4">Room Schedule</h2>
      {schedule && Object.entries(schedule).map(([dayNum, slots]) => (
        <div key={dayNum} className="border rounded-lg p-4 bg-white shadow">
          <h3 className="font-semibold text-lg mb-3 text-green-600">
            {dayNames[parseInt(dayNum) - 1]}
          </h3>
          {slots && slots.length > 0 ? (
            <div className="space-y-2">
              {slots.map((slot: RoomScheduleDto) => (
                <div
                  key={slot.slotId}
                  className="bg-gray-50 p-3 rounded border-l-4 border-green-500"
                >
                  <div className="font-semibold">{slot.subject}</div>
                  <div className="text-sm text-gray-600">
                    {slot.startTime} - {slot.endTime}
                  </div>
                  <div className="text-sm text-gray-600">
                    Teacher ID: {slot.teacherId.substring(0, 8)}...
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
