import { useState } from 'react'
import { createUser, bulkCreateUsers } from '@/services/adminService'
import type { CreateUserRequest } from '@/types/admin'

const AdminUsersPage = () => {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState('TEACHER')
  const [department, setDepartment] = useState('CSE')
  const [bulkUsers, setBulkUsers] = useState<CreateUserRequest[]>([])
  const [message, setMessage] = useState<string | null>(null)

  const handleCreate = async () => {
    setMessage(null)
    const req: CreateUserRequest = {
      name,
      email,
      role,
      departmentCode: department,
    }

    try {
      const res = await createUser(req)
      setMessage(`Created ${res.email} — temp: ${res.tempPassword}`)
      // Clear form
      setName('')
      setEmail('')
      setRole('TEACHER')
      setDepartment('CSE')
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Failed to create user')
    }
  }

  const addBulkUser = () => {
    setBulkUsers([...bulkUsers, { name: '', email: '', role: 'TEACHER', departmentCode: 'CSE' }])
  }

  const updateBulkUser = (index: number, field: keyof CreateUserRequest, value: string) => {
    const updated = [...bulkUsers]
    updated[index] = { ...updated[index], [field]: value }
    setBulkUsers(updated)
  }

  const removeBulkUser = (index: number) => {
    setBulkUsers(bulkUsers.filter((_, i) => i !== index))
  }

  const handleBulk = async () => {
    setMessage(null)
    if (bulkUsers.length === 0) {
      setMessage('No users to create')
      return
    }
    try {
      const res = await bulkCreateUsers(bulkUsers)
      setMessage(`Bulk created ${res.length} users`)
      setBulkUsers([])
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Bulk create failed')
    }
  }

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Admin User Management</h1>

      <section className="mb-8">
        <h2 className="font-semibold mb-2">Create single user</h2>
        <div className="grid gap-2 sm:grid-cols-4 mb-2">
          <input data-test-id="create-name" placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} className="border p-2 rounded" />
          <input data-test-id="create-email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} className="border p-2 rounded" />
          <select data-test-id="create-role" value={role} onChange={(e) => setRole(e.target.value)} className="border p-2 rounded">
            <option value="TEACHER">TEACHER</option>
            <option value="STUDENT">STUDENT</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <input data-test-id="create-dept" placeholder="Dept code" value={department} onChange={(e) => setDepartment(e.target.value)} className="border p-2 rounded" />
        </div>
        <button data-test-id="create-submit" onClick={handleCreate} className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Create user</button>
      </section>

      <section>
        <h2 className="font-semibold mb-2">Bulk create users</h2>
        <div className="mb-4">
          <button onClick={addBulkUser} className="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600">Add User</button>
        </div>
        {bulkUsers.length > 0 && (
          <div className="mb-4">
            <table className="w-full border-collapse border border-gray-300">
              <thead>
                <tr className="bg-gray-100">
                  <th className="border border-gray-300 p-2">Name</th>
                  <th className="border border-gray-300 p-2">Email</th>
                  <th className="border border-gray-300 p-2">Role</th>
                  <th className="border border-gray-300 p-2">Department</th>
                  <th className="border border-gray-300 p-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {bulkUsers.map((user, index) => (
                  <tr key={index}>
                    <td className="border border-gray-300 p-2">
                      <input
                        type="text"
                        value={user.name}
                        onChange={(e) => updateBulkUser(index, 'name', e.target.value)}
                        className="w-full p-1 border rounded"
                        placeholder="Name"
                      />
                    </td>
                    <td className="border border-gray-300 p-2">
                      <input
                        type="email"
                        value={user.email}
                        onChange={(e) => updateBulkUser(index, 'email', e.target.value)}
                        className="w-full p-1 border rounded"
                        placeholder="Email"
                      />
                    </td>
                    <td className="border border-gray-300 p-2">
                      <select
                        value={user.role}
                        onChange={(e) => updateBulkUser(index, 'role', e.target.value)}
                        className="w-full p-1 border rounded"
                      >
                        <option value="TEACHER">TEACHER</option>
                        <option value="STUDENT">STUDENT</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                    </td>
                    <td className="border border-gray-300 p-2">
                      <input
                        type="text"
                        value={user.departmentCode}
                        onChange={(e) => updateBulkUser(index, 'departmentCode', e.target.value)}
                        className="w-full p-1 border rounded"
                        placeholder="Dept"
                      />
                    </td>
                    <td className="border border-gray-300 p-2">
                      <button
                        onClick={() => removeBulkUser(index)}
                        className="bg-red-500 text-white px-2 py-1 rounded hover:bg-red-600"
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="mt-4">
              <button data-test-id="bulk-submit" onClick={handleBulk} className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600">Create All Users</button>
            </div>
          </div>
        )}
      </section>

      {message && <div data-test-id="user-mgmt-message" className="mt-4 p-2 bg-gray-100 rounded">{message}</div>}
    </div>
  )
}

export default AdminUsersPage
