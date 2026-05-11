/**
 * RoomCard Component - T-105: Room Availability Frontend
 *
 * Displays individual room information in a card format.
 * Used in the room availability grid to show rooms that are free for the selected slot.
 */

import React from 'react';
import { RoomAvailabilityDto } from '../types/timetable';

interface RoomCardProps {
  /** Room data to display */
  room: RoomAvailabilityDto;
}

/**
 * Card component displaying room availability information.
 * Read-only presentation for room availability and exam hall assignment.
 */
const RoomCard: React.FC<RoomCardProps> = ({
  room,
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
        <div className="flex items-center text-gray-700">
          <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M12 18h.01M12 22C6.477 22 2 17.523 2 12S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z" />
          </svg>
          Type: {room.roomType}
        </div>
      </div>

      <div className="text-center rounded-lg bg-gray-100 px-4 py-3">
        <span className="text-sm text-gray-600">Free for the selected time slot</span>
      </div>
    </div>
  );
};

export default RoomCard;