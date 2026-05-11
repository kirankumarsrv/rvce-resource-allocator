/**
 * RoomAvailabilityPage Component - Room Availability Display (Read-Only)
 *
 * Main page for room availability search functionality.
 * Displays available rooms in a responsive grid with filter options.
 * This is a read-only informational page.
 */

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getAvailableRooms } from '../services/timetableService';
import { RoomAvailabilityQuery } from '../types/timetable';
import RoomCard from '../components/RoomCard';

interface RoomAvailabilityPageProps {
  /** User role (for context, but no role-based restrictions) */
  userRole?: 'TEACHER' | 'TTO' | 'ADMIN' | 'STUDENT' | 'DEPT_COORD';
}

/**
 * Read-only room availability display component
 * Shows available rooms based on search filters with auto-refresh
 */
const RoomAvailabilityPage: React.FC<RoomAvailabilityPageProps> = () => {
  // Form state for search filters
  const [date, setDate] = useState(() => {
    const today = new Date();
    return today.toISOString().split('T')[0]; // YYYY-MM-DD format
  });
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('10:00');
  const [minCapacity, setMinCapacity] = useState<number | undefined>();
  const [building, setBuilding] = useState<string>('');

  // Available buildings for filter dropdown
  const availableBuildings = [
    'All Buildings',
    'Block A',
    'Block B',
    'Block C',
    'Main Building',
    'Library',
    'Auditorium'
  ];

  // React Query for fetching available rooms
  const {
    data: rooms = [],
    isLoading,
    error
  } = useQuery({
    queryKey: ['available-rooms', date, startTime, endTime, minCapacity, building],
    queryFn: () => {
      const query: RoomAvailabilityQuery = {
        date,
        startTime,
        endTime,
        minCapacity: minCapacity || undefined,
        building: building === 'All Buildings' ? undefined : building,
      };
      return getAvailableRooms(query);
    },
    refetchInterval: 60000, // Auto-refresh every 60 seconds
    staleTime: 30000, // Consider data fresh for 30 seconds
  });

  /**
   * Validates time range before search
   */
  const isValidTimeRange = () => {
    return startTime < endTime;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Room Availability
          </h1>
          <p className="text-gray-600">
            Search and view available rooms (read-only information)
          </p>
        </div>

        {/* Search Filters */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <h2 className="text-xl font-semibold mb-4">Search Filters</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Date Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Date
              </label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Start Time Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Start Time
              </label>
              <input
                type="time"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* End Time Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                End Time
              </label>
              <input
                type="time"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            {/* Building Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Building
              </label>
              <select
                value={building}
                onChange={(e) => setBuilding(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                {availableBuildings.map((bldg) => (
                  <option key={bldg} value={bldg === 'All Buildings' ? '' : bldg}>
                    {bldg}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Capacity Filter */}
          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Minimum Capacity (optional)
            </label>
            <input
              type="number"
              value={minCapacity || ''}
              onChange={(e) => setMinCapacity(e.target.value ? parseInt(e.target.value) : undefined)}
              placeholder="e.g., 50"
              min="1"
              className="w-full md:w-48 px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Validation Error */}
          {!isValidTimeRange() && (
            <div className="mt-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
              End time must be after start time
            </div>
          )}
        </div>

        {/* Results Section */}
        <div className="mb-6 flex justify-between items-center">
          <h2 className="text-xl font-semibold">
            Available Rooms
            {rooms.length > 0 && (
              <span className="ml-2 text-sm text-gray-500">
                ({rooms.length} found)
              </span>
            )}
          </h2>

          {/* Auto-refresh indicator */}
          <div className="flex items-center text-sm text-gray-500">
            <svg className="w-4 h-4 mr-1 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Auto-refreshing every 60 seconds
          </div>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="bg-white rounded-lg shadow-md p-6 animate-pulse">
                <div className="h-6 bg-gray-300 rounded mb-4"></div>
                <div className="h-4 bg-gray-300 rounded mb-2"></div>
                <div className="h-4 bg-gray-300 rounded mb-4"></div>
                <div className="h-10 bg-gray-300 rounded"></div>
              </div>
            ))}
          </div>
        )}

        {/* Error State */}
        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            Failed to load available rooms. Please try again.
          </div>
        )}

        {/* Results Grid */}
        {!isLoading && !error && rooms.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {rooms.map((room) => (
              <RoomCard
                key={room.id}
                room={room}
              />
            ))}
          </div>
        )}

        {/* No Results State */}
        {!isLoading && !error && rooms.length === 0 && (
          <div className="text-center py-12">
            <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            <h3 className="mt-2 text-sm font-medium text-gray-900">No rooms available</h3>
            <p className="mt-1 text-sm text-gray-500">
              Try adjusting your search filters or check a different time slot.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default RoomAvailabilityPage;
