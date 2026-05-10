/**
 * RoomCard Component - T-105: Room Availability Frontend
 *
 * Displays individual room information in a card format with booking functionality.
 * Used in the room availability grid to show available rooms for booking.
 */

import React from 'react';
import { RoomAvailabilityDto } from '../types/timetable';

interface RoomCardProps {
  /** Room data to display */
  room: RoomAvailabilityDto;
  /** Whether the current user is a teacher (shows book button) */
  isTeacher?: boolean;
}

/**
 * Card component displaying room availability information
 * Features optimistic UI updates for booking actions
 */
const RoomCard: React.FC<RoomCardProps> = ({
  room,
  isTeacher = false,
}) => {
  return (
    <div className={`
      bg-white rounded-lg shadow-md p-6 border-2 transition-all duration-200
      border-gray-200 hover:border-blue-300
    `}>
      {/* Room Header */}
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="text-xl font-semibold text-gray-900">{room.name}</h3>
          <p className="text-sm text-gray-600">{room.building} • Floor {room.floor}</p>
        </div>
      </div>

      {/* Room Details */}
      <div className="space-y-2 mb-4">
        <div className="flex items-center text-gray-700">
          <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
          </svg>
          Capacity: {room.capacity} students
        </div>
      </div>

      {isTeacher ? (
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          This page only checks room availability for the selected date and time window.
          Direct booking is not supported here because the backend override flow is slot-based.
        </div>
      ) : (
        <div className="text-center py-3 px-4 bg-gray-100 rounded-lg">
          <span className="text-gray-600 text-sm">Available for booking</span>
        </div>
      )}
    </div>
  );
};

export default RoomCard;