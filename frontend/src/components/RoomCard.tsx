/**
 * RoomCard Component - T-105: Room Availability Frontend
 *
 * Displays individual room information in a card format with booking functionality.
 * Used in the room availability grid to show available rooms for booking.
 */

import React, { useState } from 'react';
import { RoomAvailabilityDto } from '../types/timetable';
import { bookRoomForToday } from '../services/timetableService';

interface RoomCardProps {
  /** Room data to display */
  room: RoomAvailabilityDto;
  /** Callback when booking is successful */
  onBookSuccess?: (room: RoomAvailabilityDto) => void;
  /** Callback when booking fails */
  onBookError?: (error: string) => void;
  /** Whether the current user is a teacher (shows book button) */
  isTeacher?: boolean;
}

/**
 * Card component displaying room availability information
 * Features optimistic UI updates for booking actions
 */
const RoomCard: React.FC<RoomCardProps> = ({
  room,
  onBookSuccess,
  onBookError,
  isTeacher = false,
}) => {
  const [isBooking, setIsBooking] = useState(false);
  const [isBooked, setIsBooked] = useState(false);

  /**
   * Handles room booking with optimistic UI updates
   * Shows loading state, then success/error feedback
   */
  const handleBookRoom = async () => {
    if (isBooking || isBooked) return;

    setIsBooking(true);
    try {
      // Optimistic UI update - mark as booked immediately
      setIsBooked(true);

      // Call the booking API
      await bookRoomForToday(room.id as any, `Booked ${room.name} for today`);

      // Notify parent component of successful booking
      onBookSuccess?.(room);
    } catch (error) {
      // Revert optimistic update on failure
      setIsBooked(false);
      const errorMessage = error instanceof Error ? error.message : 'Failed to book room';
      onBookError?.(errorMessage);
    } finally {
      setIsBooking(false);
    }
  };

  return (
    <div className={`
      bg-white rounded-lg shadow-md p-6 border-2 transition-all duration-200
      ${isBooked ? 'border-green-500 bg-green-50' : 'border-gray-200 hover:border-blue-300'}
    `}>
      {/* Room Header */}
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="text-xl font-semibold text-gray-900">{room.name}</h3>
          <p className="text-sm text-gray-600">{room.building} • Floor {room.floor}</p>
        </div>
        {isBooked && (
          <span className="px-3 py-1 bg-green-100 text-green-800 text-sm font-medium rounded-full">
            ✓ Booked
          </span>
        )}
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

      {/* Book Button - Only for teachers */}
      {isTeacher && !isBooked && (
        <button
          onClick={handleBookRoom}
          disabled={isBooking}
          className={`
            w-full py-3 px-4 rounded-lg font-medium transition-all duration-200
            ${isBooking
              ? 'bg-gray-400 cursor-not-allowed text-white'
              : 'bg-blue-600 hover:bg-blue-700 text-white shadow-md hover:shadow-lg'
            }
          `}
        >
          {isBooking ? (
            <div className="flex items-center justify-center">
              <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Booking...
            </div>
          ) : (
            'Book for Today'
          )}
        </button>
      )}

      {/* TTO/Admin view - no booking button */}
      {!isTeacher && (
        <div className="text-center py-3 px-4 bg-gray-100 rounded-lg">
          <span className="text-gray-600 text-sm">Available for booking</span>
        </div>
      )}
    </div>
  );
};

export default RoomCard;