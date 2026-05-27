import { useState, useEffect, useMemo } from 'react';
import { adminApi } from './api/adminApi';
import type { User } from '../../shared/types';
import LoadingScreen from '../../shared/components/LoadingScreen';
import { Users } from 'lucide-react';
import ConfirmModal from '../../shared/components/ConfirmModal';
import AdminTableControls from './components/AdminTableControls';

const PAGE_SIZE = 10;

export default function AdminUsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showConfirm, setShowConfirm] = useState(false);
  const [userToSuspend, setUserToSuspend] = useState<User | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
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
    return users.filter((u) => {
      const searchable = `${u.id} ${u.firstname || ''} ${u.lastname || ''} ${u.email || ''} ${u.role || ''}`.toLowerCase();
      const matchesSearch = !query || searchable.includes(query);
      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && !u.suspended) ||
        (statusFilter === 'SUSPENDED' && u.suspended) ||
        String(u.role).toUpperCase() === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [users, search, statusFilter]);

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
          resultCount={filteredUsers.length}
          totalCount={users.length}
          currentPage={Math.min(currentPage, totalPages)}
          totalPages={totalPages}
          onPreviousPage={() => setCurrentPage((page) => Math.max(1, page - 1))}
          onNextPage={() => setCurrentPage((page) => Math.min(totalPages, page + 1))}
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
                    <button
                      onClick={() => handleSuspendClick(u)}
                      className={`transition-colors p-2 rounded-full ${u.suspended ? 'text-green-600 hover:bg-green-50' : 'text-orange-600 hover:bg-orange-50'}`}
                      title={u.suspended ? "Unsuspend User" : "Suspend User"}
                    >
                      {u.suspended ? <span className="font-medium text-xs">Unsuspend</span> : <span className="font-medium text-xs">Suspend</span>}
                    </button>
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
