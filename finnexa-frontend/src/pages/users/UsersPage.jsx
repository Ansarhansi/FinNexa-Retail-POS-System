import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getUsers,
  updateUserRole,
  updateUserStatus,
} from "../../services/userService";
import "./UsersPage.css";

const ROLES = ["ROLE_ADMIN", "ROLE_CASHIER", "ROLE_INVENTORY_MANAGER"];

const formatRole = (role = "") =>
  role.replace(/^ROLE_/, "").replaceAll("_", " ");

const formatDate = (date) => {
  if (!date) return "—";

  const parsedDate = new Date(date);

  if (Number.isNaN(parsedDate.getTime())) return "—";

  return parsedDate.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

const getInitials = (name = "") =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join("") || "U";

const getRoleClass = (role = "") =>
  role
    .toLowerCase()
    .replace(/^role_/, "")
    .replaceAll("_", "-");

function UsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(8);

  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedRole, setSelectedRole] = useState("");
  const [savingRole, setSavingRole] = useState(false);
  const [updatingStatusId, setUpdatingStatusId] = useState(null);

  const [notice, setNotice] = useState({
    type: "",
    message: "",
  });

  const [roleModalOpen, setRoleModalOpen] = useState(false);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getUsers();
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Unable to load users. Check your login permissions and backend connection.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    if (!notice.message) return undefined;

    const timer = setTimeout(() => {
      setNotice({ type: "", message: "" });
    }, 3500);

    return () => clearTimeout(timer);
  }, [notice]);

  const stats = useMemo(() => {
    const total = users.length;
    const active = users.filter((user) => user.enabled).length;
    const inactive = total - active;
    const admins = users.filter((user) => user.role === "ROLE_ADMIN").length;

    return { total, active, inactive, admins };
  }, [users]);

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();

    return users.filter((user) => {
      const matchesSearch =
        !query ||
        String(user.fullName || "")
          .toLowerCase()
          .includes(query) ||
        String(user.email || "")
          .toLowerCase()
          .includes(query) ||
        String(user.id || "").includes(query);

      const matchesRole = roleFilter === "ALL" || user.role === roleFilter;

      const matchesStatus =
        statusFilter === "ALL" ||
        (statusFilter === "ACTIVE" && user.enabled) ||
        (statusFilter === "INACTIVE" && !user.enabled);

      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [users, search, roleFilter, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / pageSize));

  const currentPage = Math.min(page, totalPages);

  const paginatedUsers = filteredUsers.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  useEffect(() => {
    setPage(1);
  }, [search, roleFilter, statusFilter, pageSize]);

  const openRoleModal = (user) => {
    setSelectedUser(user);
    setSelectedRole(user.role);
    setRoleModalOpen(true);
  };

  const closeRoleModal = () => {
    if (savingRole) return;

    setRoleModalOpen(false);
    setSelectedUser(null);
    setSelectedRole("");
  };

  const handleRoleUpdate = async (event) => {
    event.preventDefault();

    if (!selectedUser || !selectedRole) return;

    if (selectedRole === selectedUser.role) {
      closeRoleModal();
      return;
    }

    setSavingRole(true);
    setNotice({ type: "", message: "" });

    try {
      const updatedUser = await updateUserRole(selectedUser.id, selectedRole);

      setUsers((previousUsers) =>
        previousUsers.map((user) =>
          user.id === selectedUser.id ? { ...user, ...updatedUser } : user,
        ),
      );

      setRoleModalOpen(false);
      setSelectedUser(null);

      setNotice({
        type: "success",
        message: `Role updated for ${updatedUser.fullName || selectedUser.fullName}.`,
      });
    } catch (err) {
      setNotice({
        type: "error",
        message:
          err.response?.data?.message || "Unable to update the user's role.",
      });
    } finally {
      setSavingRole(false);
    }
  };

  const handleStatusToggle = async (user) => {
    const action = user.enabled ? "disable" : "enable";

    const confirmed = window.confirm(
      `Are you sure you want to ${action} the account for ${user.fullName}?`,
    );

    if (!confirmed) return;

    setUpdatingStatusId(user.id);
    setNotice({ type: "", message: "" });

    try {
      const updatedUser = await updateUserStatus(user.id, !user.enabled);

      setUsers((previousUsers) =>
        previousUsers.map((item) =>
          item.id === user.id ? { ...item, ...updatedUser } : item,
        ),
      );

      setNotice({
        type: "success",
        message: `${user.fullName}'s account has been ${
          !user.enabled ? "enabled" : "disabled"
        }.`,
      });
    } catch (err) {
      setNotice({
        type: "error",
        message:
          err.response?.data?.message || `Unable to ${action} this account.`,
      });
    } finally {
      setUpdatingStatusId(null);
    }
  };

  const clearFilters = () => {
    setSearch("");
    setRoleFilter("ALL");
    setStatusFilter("ALL");
    setPage(1);
  };

  return (
    <div className="users-page">
      <div className="users-header">
        <div>
          <div className="users-eyebrow">
            <i className="bi bi-shield-check" />
            ADMINISTRATION
          </div>

          <h1 className="users-title">User Management</h1>

          <p className="users-subtitle">
            Manage user accounts, assign roles, and control access to your
            FinNexa workspace.
          </p>
        </div>

        <button
          type="button"
          className="users-btn users-btn-secondary"
          onClick={loadUsers}
          disabled={loading}
        >
          <i
            className={`bi ${loading ? "bi-arrow-repeat" : "bi-arrow-clockwise"}`}
          />
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      {notice.message && (
        <div className={`users-notice ${notice.type}`}>
          <i
            className={`bi ${
              notice.type === "success"
                ? "bi-check-circle-fill"
                : "bi-exclamation-circle-fill"
            }`}
          />
          <span>{notice.message}</span>
          <button
            type="button"
            aria-label="Dismiss notification"
            onClick={() => setNotice({ type: "", message: "" })}
          >
            <i className="bi bi-x-lg" />
          </button>
        </div>
      )}

      <section className="users-stats-grid">
        <div className="users-stat-card">
          <div className="users-stat-icon total">
            <i className="bi bi-people-fill" />
          </div>
          <div>
            <span className="users-stat-label">Total Users</span>
            <strong className="users-stat-value">{stats.total}</strong>
            <span className="users-stat-caption">Registered accounts</span>
          </div>
        </div>

        <div className="users-stat-card">
          <div className="users-stat-icon active">
            <i className="bi bi-person-check-fill" />
          </div>
          <div>
            <span className="users-stat-label">Active Users</span>
            <strong className="users-stat-value">{stats.active}</strong>
            <span className="users-stat-caption">Enabled accounts</span>
          </div>
        </div>

        <div className="users-stat-card">
          <div className="users-stat-icon inactive">
            <i className="bi bi-person-x-fill" />
          </div>
          <div>
            <span className="users-stat-label">Inactive Users</span>
            <strong className="users-stat-value">{stats.inactive}</strong>
            <span className="users-stat-caption">Disabled accounts</span>
          </div>
        </div>

        <div className="users-stat-card">
          <div className="users-stat-icon admin">
            <i className="bi bi-shield-lock-fill" />
          </div>
          <div>
            <span className="users-stat-label">Administrators</span>
            <strong className="users-stat-value">{stats.admins}</strong>
            <span className="users-stat-caption">Accounts with admin role</span>
          </div>
        </div>
      </section>

      <section className="users-panel">
        <div className="users-panel-header">
          <div>
            <h2 className="users-panel-title">All Users</h2>
            <p className="users-panel-description">
              View accounts and manage access permissions.
            </p>
          </div>

          <span className="users-count-badge">
            {filteredUsers.length}{" "}
            {filteredUsers.length === 1 ? "user" : "users"}
          </span>
        </div>

        <div className="users-toolbar">
          <div className="users-search">
            <i className="bi bi-search" />
            <input
              type="search"
              placeholder="Search by name, email, or ID..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              aria-label="Search users"
            />

            {search && (
              <button
                type="button"
                onClick={() => setSearch("")}
                aria-label="Clear search"
              >
                <i className="bi bi-x-circle-fill" />
              </button>
            )}
          </div>

          <div className="users-filter-group">
            <select
              value={roleFilter}
              onChange={(event) => setRoleFilter(event.target.value)}
              aria-label="Filter by role"
            >
              <option value="ALL">All Roles</option>
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {formatRole(role)}
                </option>
              ))}
            </select>

            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              aria-label="Filter by status"
            >
              <option value="ALL">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>

            {(search || roleFilter !== "ALL" || statusFilter !== "ALL") && (
              <button
                type="button"
                className="users-clear-btn"
                onClick={clearFilters}
              >
                Clear
              </button>
            )}
          </div>
        </div>

        {error && (
          <div className="users-error">
            <i className="bi bi-exclamation-triangle-fill" />
            <div>
              <strong>Unable to load users</strong>
              <p>{error}</p>
              <button
                type="button"
                className="users-btn users-btn-secondary"
                onClick={loadUsers}
              >
                Try Again
              </button>
            </div>
          </div>
        )}

        {loading && !error ? (
          <div className="users-loading">
            <span className="users-spinner" />
            <span>Loading users...</span>
          </div>
        ) : !error && paginatedUsers.length === 0 ? (
          <div className="users-empty">
            <div className="users-empty-icon">
              <i className="bi bi-people" />
            </div>
            <h3>
              {users.length === 0 ? "No users found" : "No matching users"}
            </h3>
            <p>
              {users.length === 0
                ? "There are currently no user accounts to display."
                : "Try changing your search or filter settings."}
            </p>

            {users.length > 0 && (
              <button
                type="button"
                className="users-btn users-btn-secondary"
                onClick={clearFilters}
              >
                Clear Filters
              </button>
            )}
          </div>
        ) : (
          !error && (
            <>
              <div className="users-table-wrapper">
                <table className="users-table">
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>User ID</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th>Joined</th>
                      <th className="users-actions-heading">Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {paginatedUsers.map((user) => (
                      <tr key={user.id}>
                        <td>
                          <div className="users-identity">
                            <div className="users-avatar">
                              {getInitials(user.fullName)}
                            </div>

                            <div className="users-identity-details">
                              <span className="users-full-name">
                                {user.fullName || "Unnamed User"}
                              </span>
                              <span className="users-email">{user.email}</span>
                            </div>
                          </div>
                        </td>

                        <td>
                          <span className="users-id">#{user.id}</span>
                        </td>

                        <td>
                          <span
                            className={`users-role-badge ${getRoleClass(
                              user.role,
                            )}`}
                          >
                            <i className="bi bi-shield-check" />
                            {formatRole(user.role)}
                          </span>
                        </td>

                        <td>
                          <span
                            className={`users-status ${
                              user.enabled ? "enabled" : "disabled"
                            }`}
                          >
                            <span className="users-status-dot" />
                            {user.enabled ? "Active" : "Inactive"}
                          </span>
                        </td>

                        <td>
                          <span className="users-date">
                            {formatDate(user.createdAt)}
                          </span>
                        </td>

                        <td>
                          <div className="users-actions">
                            <button
                              type="button"
                              className="users-action-btn edit-role"
                              title="Change user role"
                              aria-label={`Change role for ${user.fullName}`}
                              onClick={() => openRoleModal(user)}
                            >
                              <i className="bi bi-person-gear" />
                              <span>Role</span>
                            </button>

                            <button
                              type="button"
                              className={`users-action-btn ${
                                user.enabled ? "disable-user" : "enable-user"
                              }`}
                              title={
                                user.enabled
                                  ? "Disable account"
                                  : "Enable account"
                              }
                              aria-label={`${
                                user.enabled ? "Disable" : "Enable"
                              } ${user.fullName}`}
                              disabled={updatingStatusId === user.id}
                              onClick={() => handleStatusToggle(user)}
                            >
                              <i
                                className={`bi ${
                                  updatingStatusId === user.id
                                    ? "bi-arrow-repeat"
                                    : user.enabled
                                      ? "bi-person-dash"
                                      : "bi-person-check"
                                }`}
                              />
                              <span>
                                {updatingStatusId === user.id
                                  ? "Saving..."
                                  : user.enabled
                                    ? "Disable"
                                    : "Enable"}
                              </span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="users-pagination">
                <div className="users-pagination-left">
                  <span>Rows per page</span>
                  <select
                    value={pageSize}
                    onChange={(event) =>
                      setPageSize(Number(event.target.value))
                    }
                    aria-label="Rows per page"
                  >
                    <option value={5}>5</option>
                    <option value={8}>8</option>
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                  </select>

                  <span className="users-pagination-info">
                    Showing{" "}
                    {filteredUsers.length === 0
                      ? 0
                      : (currentPage - 1) * pageSize + 1}
                    –{Math.min(currentPage * pageSize, filteredUsers.length)} of{" "}
                    {filteredUsers.length}
                  </span>
                </div>

                <div className="users-pagination-controls">
                  <button
                    type="button"
                    className="users-page-btn"
                    disabled={currentPage === 1}
                    onClick={() => setPage(1)}
                    aria-label="First page"
                  >
                    <i className="bi bi-chevron-double-left" />
                  </button>

                  <button
                    type="button"
                    className="users-page-btn"
                    disabled={currentPage === 1}
                    onClick={() => setPage((value) => value - 1)}
                    aria-label="Previous page"
                  >
                    <i className="bi bi-chevron-left" />
                  </button>

                  <span className="users-page-number">
                    {currentPage} / {totalPages}
                  </span>

                  <button
                    type="button"
                    className="users-page-btn"
                    disabled={currentPage === totalPages}
                    onClick={() => setPage((value) => value + 1)}
                    aria-label="Next page"
                  >
                    <i className="bi bi-chevron-right" />
                  </button>

                  <button
                    type="button"
                    className="users-page-btn"
                    disabled={currentPage === totalPages}
                    onClick={() => setPage(totalPages)}
                    aria-label="Last page"
                  >
                    <i className="bi bi-chevron-double-right" />
                  </button>
                </div>
              </div>
            </>
          )
        )}
      </section>

      <div className="users-security-note">
        <i className="bi bi-shield-lock-fill" />
        <p>
          <strong>Access control:</strong> Only administrators can manage user
          roles and account status. Role changes affect the permissions assigned
          to each account.
        </p>
      </div>

      {roleModalOpen && selectedUser && (
        <div
          className="users-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeRoleModal();
            }
          }}
        >
          <div
            className="users-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="users-role-modal-title"
          >
            <div className="users-modal-header">
              <div className="users-modal-icon">
                <i className="bi bi-person-gear" />
              </div>

              <button
                type="button"
                className="users-modal-close"
                onClick={closeRoleModal}
                disabled={savingRole}
                aria-label="Close dialog"
              >
                <i className="bi bi-x-lg" />
              </button>
            </div>

            <h2 id="users-role-modal-title">Change User Role</h2>

            <p className="users-modal-description">
              Update the role assigned to this user.
            </p>

            <div className="users-modal-user">
              <div className="users-avatar">
                {getInitials(selectedUser.fullName)}
              </div>

              <div>
                <strong>{selectedUser.fullName}</strong>
                <span>{selectedUser.email}</span>
              </div>
            </div>

            <form onSubmit={handleRoleUpdate}>
              <label className="users-modal-label" htmlFor="users-role-select">
                Select Role
              </label>

              <select
                id="users-role-select"
                className="users-role-select"
                value={selectedRole}
                onChange={(event) => setSelectedRole(event.target.value)}
                disabled={savingRole}
                required
              >
                {ROLES.map((role) => (
                  <option key={role} value={role}>
                    {formatRole(role)}
                  </option>
                ))}
              </select>

              <div className="users-role-warning">
                <i className="bi bi-info-circle-fill" />
                <span>
                  Changing a role changes the permissions available to this
                  account.
                </span>
              </div>

              <div className="users-modal-actions">
                <button
                  type="button"
                  className="users-btn users-btn-secondary"
                  onClick={closeRoleModal}
                  disabled={savingRole}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="users-btn users-btn-primary"
                  disabled={savingRole || selectedRole === selectedUser.role}
                >
                  {savingRole ? (
                    <>
                      <span className="users-button-spinner" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-check2" />
                      Save Changes
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default UsersPage;
