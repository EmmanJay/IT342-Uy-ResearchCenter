import { useState, useEffect, useMemo } from 'react';
import { adminApi } from './api/adminApi';
import type { User } from '../../shared/types';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { Users } from 'lucide-react';
import ConfirmModal from '../../shared/components/ConfirmModal';
import AdminTableControls from './components/AdminTableControls';
import { SessionManager } from '../../shared/auth/sessionManager';

const PAGE_SIZE = 10;

export default function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const currentUser = SessionManager.getUser();
  const [error, setError] = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [userToSuspend, setUserToSuspend] = useState<User | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortOrder, setSortOrder] = useState('LATEST');
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getUsers();
      if (res.success) {
        setUsers(res.data);
      }
    } catch (err) {
      setError('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleSuspendClick = (u: User) => {
    setUserToSuspend(u);
    setShowConfirm(true);
  };

  useEffect(() => {
    setCurrentPage(1);
  }, [search, statusFilter]);

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    const filtered = users.filter((u) => {
      const role = String(u.role || '').toUpperCase();
      const status = u.suspended ? 'SUSPENDED' : 'ACTIVE';
      const searchable = `${u.id} ${u.firstname || ''} ${u.lastname || ''} ${u.email || ''} ${role} ${status}`.toLowerCase();
      const matchesSearch = !query || searchable.includes(query);
      const matchesStatus = statusFilter === 'ALL' || 
                           (statusFilter === 'SUSPENDED' && status === 'SUSPENDED') ||
                           (statusFilter === 'ACTIVE' && status === 'ACTIVE') ||
                           (statusFilter === 'ADMIN' && role === 'ADMIN') ||
                           (statusFilter === 'RESEARCHER' && role === 'RESEARCHER');
      return matchesSearch && matchesStatus;
    });
    
    // Sort logic based on filter
    return filtered.sort((a, b) => {
      // Admins first
      if (a.role === 'ADMIN' && b.role !== 'ADMIN') return -1;
      if (a.role !== 'ADMIN' && b.role === 'ADMIN') return 1;

      // Then sort by ID or creation date (assuming ID is sequential or we can fallback to string comparison)
      // Since ID might be a string (e.g., UUID or numeric string), we handle it carefully.
      const idA = String(a.id);
      const idB = String(b.id);
      
      if (sortOrder === 'LATEST') {
        // Descending
        return idB.localeCompare(idA, undefined, { numeric: true });
      } else {
        // Ascending
        return idA.localeCompare(idB, undefined, { numeric: true });
      }
    });
  }, [users, search, statusFilter, sortOrder]);

  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE));
  const paginatedUsers = filteredUsers.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const confirmSuspend = async () => {
    if (!userToSuspend) return;
    try {
      const res = await adminApi.suspendUser(userToSuspend.id);
      if (res.success) {
        setUsers(users.map((u) => (u.id === userToSuspend.id ? { ...u, suspended: res.data.suspended } : u)));
      }
    } catch (err) {
      setError('Failed to update user status');
    } finally {
      setShowConfirm(false);
      setUserToSuspend(null);
    }
  };

  if (loading) {
    return <LoadingScreen label="Loading users" fullScreen={false} />;
  }

  if (error) {
    return <div className="p-8 text-center text-red-500">{error}</div>;
  }

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-blue-100 text-blue-600 rounded-lg">
            <Users className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Manage Users</h1>
        </div>
        <div className="text-sm text-gray-500">
          Total Users: {users.length}
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <AdminTableControls
          search={search}
          onSearchChange={setSearch}
          filterLabel="Filter users"
          filterValue={statusFilter}
          onFilterChange={setStatusFilter}
          filterOptions={[
            { label: 'All users', value: 'ALL' },
            { label: 'Active', value: 'ACTIVE' },
            { label: 'Suspended', value: 'SUSPENDED' },
            { label: 'Admins', value: 'ADMIN' },
            { label: 'Researchers', value: 'RESEARCHER' },
          ]}
          sortValue={sortOrder}
          onSortChange={setSortOrder}
          sortOptions={[
            { label: 'Latest First', value: 'LATEST' },
            { label: 'Oldest First', value: 'OLDEST' },
          ]}
          resultCount={filteredUsers.length}
          totalCount={users.length}
        />
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-gray-600">
            <thead className="bg-gray-50 border-b border-gray-200 text-gray-700 uppercase font-medium">
              <tr>
                <th className="px-6 py-4">ID</th>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Email</th>
                <th className="px-6 py-4">Role</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {paginatedUsers.map((u) => (
                <tr key={u.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4 font-medium text-gray-900">{u.id}</td>
                  <td className="px-6 py-4">{u.firstname} {u.lastname}</td>
                  <td className="px-6 py-4">{u.email}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                      ${u.role === 'ADMIN' ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-800'}`}>
                      {u.role}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                      ${u.suspended ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}`}>
                      {u.suspended ? 'Suspended' : 'Active'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    {String(u.id) === String(currentUser?.id) ? (
                      <span className="text-gray-400 text-xs font-medium cursor-not-allowed uppercase tracking-wider">Current User</span>
                    ) : (
                      <button
                        onClick={() => handleSuspendClick(u)}
                        className={`font-medium ${u.suspended ? 'text-green-600 hover:text-green-900' : 'text-red-600 hover:text-red-900'} cursor-pointer`}
                      >
                        {u.suspended ? 'Unsuspend' : 'Suspend'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-gray-500">
                    No users found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {totalPages > 1 && (
          <div className="flex justify-center items-center gap-4 py-4 border-t border-gray-200">
            <button
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
              disabled={currentPage === 1}
              className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 hover:text-green-700 cursor-pointer"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7"/></svg>
              Previous
            </button>
            <span className="text-sm text-gray-600">Page {currentPage} of {totalPages}</span>
            <button
              onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
              disabled={currentPage === totalPages}
              className="flex items-center gap-1 text-sm font-medium text-gray-500 disabled:opacity-50 hover:text-green-700 cursor-pointer"
            >
              Next
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7"/></svg>
            </button>
          </div>
        )}
      </div>

      <ConfirmModal
        isOpen={showConfirm}
        title={userToSuspend?.suspended ? "Unsuspend User" : "Suspend User"}
        message={`Are you sure you want to ${userToSuspend?.suspended ? "unsuspend" : "suspend"} ${userToSuspend?.firstname} ${userToSuspend?.lastname}?`}
        confirmText={userToSuspend?.suspended ? "Unsuspend" : "Suspend"}
        onConfirm={confirmSuspend}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
}
