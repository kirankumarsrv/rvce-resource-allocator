/**
 * Component to display timetable analytics and utilization metrics
 */

import { useEffect, useState } from 'react';
import { TimetableAnalyticsDto } from '../types/timetable';
import { getTimetableAnalytics } from '../services/timetableService';

const dayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export const AnalyticsDashboard = () => {
  const [analytics, setAnalytics] = useState<TimetableAnalyticsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        setLoading(true);
        const data = await getTimetableAnalytics();
        setAnalytics(data);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load analytics');
        setAnalytics(null);
      } finally {
        setLoading(false);
      }
    };

    fetchAnalytics();
  }, []);

  if (loading) return <div className="text-center p-4">Loading analytics...</div>;
  if (error) return <div className="bg-red-100 text-red-800 p-4 rounded">{error}</div>;
  if (!analytics) return <div className="text-center p-4">No data available</div>;

  const utilizationPercent = Math.round(analytics.utilizationRate * 100);

  return (
    <div className="space-y-6">
      <h2 className="text-3xl font-bold mb-6">Timetable Analytics</h2>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-blue-50 p-6 rounded-lg shadow">
          <div className="text-sm text-gray-600">Total Slots</div>
          <div className="text-3xl font-bold text-blue-600">{analytics.totalSlots}</div>
        </div>
        <div className="bg-green-50 p-6 rounded-lg shadow">
          <div className="text-sm text-gray-600">Active Slots</div>
          <div className="text-3xl font-bold text-green-600">{analytics.activeSlots}</div>
        </div>
        <div className="bg-purple-50 p-6 rounded-lg shadow">
          <div className="text-sm text-gray-600">Unique Teachers</div>
          <div className="text-3xl font-bold text-purple-600">{analytics.uniqueTeachers}</div>
        </div>
        <div className="bg-orange-50 p-6 rounded-lg shadow">
          <div className="text-sm text-gray-600">Unique Rooms</div>
          <div className="text-3xl font-bold text-orange-600">{analytics.uniqueRooms}</div>
        </div>
      </div>

      {/* Utilization Metrics */}
      <div className="bg-white p-6 rounded-lg shadow">
        <h3 className="text-xl font-semibold mb-4">Utilization Rate</h3>
        <div className="flex items-center gap-4">
          <div className="flex-1">
            <div className="w-full bg-gray-200 rounded-full h-8">
              <div
                className="bg-gradient-to-r from-blue-500 to-blue-600 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm"
                style={{ width: `${utilizationPercent}%` }}
              >
                {utilizationPercent}%
              </div>
            </div>
          </div>
          <div className="text-2xl font-bold text-blue-600">{utilizationPercent}%</div>
        </div>
        <p className="text-sm text-gray-500 mt-2">
          {analytics.activeSlots} active slots out of {analytics.totalSlots} total
        </p>
      </div>

      {/* Time Distribution */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-lg shadow">
          <h3 className="text-xl font-semibold mb-4">Time Slot Distribution</h3>
          <div className="space-y-3">
            <div>
              <div className="flex justify-between mb-1">
                <span>Morning (6:00-12:00)</span>
                <span className="font-semibold">{analytics.morningSlots}</span>
              </div>
              <div className="w-full bg-gray-200 rounded h-2">
                <div
                  className="bg-yellow-500 h-2 rounded"
                  style={{
                    width: `${(analytics.morningSlots / analytics.totalSlots) * 100}%`,
                  }}
                />
              </div>
            </div>
            <div>
              <div className="flex justify-between mb-1">
                <span>Afternoon (12:00-18:00)</span>
                <span className="font-semibold">{analytics.afternoonSlots}</span>
              </div>
              <div className="w-full bg-gray-200 rounded h-2">
                <div
                  className="bg-blue-500 h-2 rounded"
                  style={{
                    width: `${(analytics.afternoonSlots / analytics.totalSlots) * 100}%`,
                  }}
                />
              </div>
            </div>
            <div>
              <div className="flex justify-between mb-1">
                <span>Evening (18:00-23:59)</span>
                <span className="font-semibold">{analytics.eveningSlots}</span>
              </div>
              <div className="w-full bg-gray-200 rounded h-2">
                <div
                  className="bg-indigo-500 h-2 rounded"
                  style={{
                    width: `${(analytics.eveningSlots / analytics.totalSlots) * 100}%`,
                  }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Day of Week Distribution */}
        <div className="bg-white p-6 rounded-lg shadow">
          <h3 className="text-xl font-semibold mb-4">Day of Week Distribution</h3>
          <div className="space-y-2">
            {Object.entries(analytics.dayOfWeekDistribution).map(([dayNum, count]) => (
              <div key={dayNum} className="flex items-center gap-2">
                <span className="w-24 text-sm">{dayNames[parseInt(dayNum) - 1]}:</span>
                <div className="flex-1 bg-gray-200 rounded h-6">
                  <div
                    className="bg-blue-500 h-6 rounded flex items-center justify-end pr-2 text-white text-xs font-semibold"
                    style={{
                      width: `${(count / Math.max(...Object.values(analytics.dayOfWeekDistribution))) * 100}%`,
                    }}
                  >
                    {count > 5 ? count : ''}
                  </div>
                </div>
                <span className="w-12 text-right font-semibold">{count}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Additional Metrics */}
      <div className="bg-white p-6 rounded-lg shadow">
        <h3 className="text-xl font-semibold mb-4">Additional Metrics</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <div className="text-sm text-gray-600 mb-1">Inactive Slots</div>
            <div className="text-2xl font-bold text-gray-700">{analytics.inactiveSlots}</div>
          </div>
          <div>
            <div className="text-sm text-gray-600 mb-1">Avg Room Capacity</div>
            <div className="text-2xl font-bold text-gray-700">
              {Math.round(analytics.averageRoomCapacity)}
            </div>
          </div>
          <div>
            <div className="text-sm text-gray-600 mb-1">Capacity per Room</div>
            <div className="text-2xl font-bold text-gray-700">
              {(analytics.averageRoomCapacity / analytics.uniqueRooms).toFixed(1)}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
