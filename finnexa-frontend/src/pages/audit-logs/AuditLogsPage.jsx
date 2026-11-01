import { useCallback, useEffect, useMemo, useState } from "react";
import { getAuditLogs } from "../../services/auditLogService";
import "./AuditLogsPage.css";

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];

const formatDate = (value) => {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return value;

  return date.toLocaleString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const getActionClass = (action) => {
  const value = String(action || "").toUpperCase();

  if (value.includes("CREATE") || value.includes("ADD")) {
    return "audit-created";
  }

  if (value.includes("UPDATE") || value.includes("EDIT")) {
    return "audit-updated";
  }

  if (value.includes("DELETE") || value.includes("REMOVE")) {
    return "audit-deleted";
  }

  if (value.includes("LOGIN")) {
    return "audit-login";
  }

  return "audit-default";
};

const getLocalDate = (value) => {
  if (!value) return null;

  const date = new Date(value);

  return Number.isNaN(date.getTime()) ? null : date;
};

export default function AuditLogsPage() {
  const [logs, setLogs] = useState([]);

  // Search and filters
  const [search, setSearch] = useState("");
  const [actionFilter, setActionFilter] = useState("ALL");
  const [userFilter, setUserFilter] = useState("ALL");
  const [entityFilter, setEntityFilter] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // UI states
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [selectedLog, setSelectedLog] = useState(null);

  // Load audit logs
  const loadAuditLogs = useCallback(async (refresh = false) => {
    if (refresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setError("");

    try {
      const response = await getAuditLogs();

      setLogs(Array.isArray(response) ? response : []);
    } catch (err) {
      setError(
        err.response?.status === 403
          ? "Access denied. Only administrators can view audit logs."
          : err.response?.data?.message || "Unable to load audit logs.",
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadAuditLogs();
  }, [loadAuditLogs]);

  // Unique filter options
  const uniqueActions = useMemo(
    () =>
      [
        ...new Set(
          logs
            .map((log) => String(log.action || "").toUpperCase())
            .filter(Boolean),
        ),
      ].sort(),
    [logs],
  );

  const uniqueUsers = useMemo(
    () => [...new Set(logs.map((log) => log.username).filter(Boolean))].sort(),
    [logs],
  );

  const uniqueEntities = useMemo(
    () =>
      [...new Set(logs.map((log) => log.entityType).filter(Boolean))].sort(),
    [logs],
  );

  // Apply all filters
  const filteredLogs = useMemo(() => {
    const searchValue = search.trim().toLowerCase();

    return logs.filter((log) => {
      const matchesSearch = [
        log.username,
        log.action,
        log.entityType,
        log.entityId,
        log.details,
      ].some((value) =>
        String(value ?? "")
          .toLowerCase()
          .includes(searchValue),
      );

      const matchesAction =
        actionFilter === "ALL" ||
        String(log.action || "").toUpperCase() === actionFilter;

      const matchesUser = userFilter === "ALL" || log.username === userFilter;

      const matchesEntity =
        entityFilter === "ALL" || log.entityType === entityFilter;

      const logDate = getLocalDate(log.createdAt);

      let matchesStartDate = true;
      let matchesEndDate = true;

      if (startDate) {
        const start = new Date(`${startDate}T00:00:00`);
        matchesStartDate = logDate && logDate >= start;
      }

      if (endDate) {
        const end = new Date(`${endDate}T23:59:59.999`);
        matchesEndDate = logDate && logDate <= end;
      }

      return (
        matchesSearch &&
        matchesAction &&
        matchesUser &&
        matchesEntity &&
        matchesStartDate &&
        matchesEndDate
      );
    });
  }, [
    logs,
    search,
    actionFilter,
    userFilter,
    entityFilter,
    startDate,
    endDate,
  ]);

  // Latest activity first
  const sortedLogs = useMemo(() => {
    return [...filteredLogs].sort((a, b) => {
      const dateA = getLocalDate(a.createdAt)?.getTime() || 0;
      const dateB = getLocalDate(b.createdAt)?.getTime() || 0;

      return dateB - dateA;
    });
  }, [filteredLogs]);

  // Reset page whenever filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [
    search,
    actionFilter,
    userFilter,
    entityFilter,
    startDate,
    endDate,
    pageSize,
  ]);

  // Pagination calculations
  const totalPages = Math.max(1, Math.ceil(sortedLogs.length / pageSize));

  const safePage = Math.min(currentPage, totalPages);

  const startIndex = (safePage - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, sortedLogs.length);

  const paginatedLogs = sortedLogs.slice(startIndex, endIndex);

  // Clear filters
  const clearFilters = () => {
    setSearch("");
    setActionFilter("ALL");
    setUserFilter("ALL");
    setEntityFilter("ALL");
    setStartDate("");
    setEndDate("");
    setCurrentPage(1);
  };

  const hasActiveFilters =
    search ||
    actionFilter !== "ALL" ||
    userFilter !== "ALL" ||
    entityFilter !== "ALL" ||
    startDate ||
    endDate;

  // Export filtered data
  const handleExport = () => {
    if (!sortedLogs.length) {
      alert("No audit logs available to export.");
      return;
    }

    const headers = [
      "ID",
      "Username",
      "Action",
      "Entity Type",
      "Entity ID",
      "Details",
      "Created At",
    ];

    const escapeCSV = (value) => `"${String(value ?? "").replace(/"/g, '""')}"`;

    const rows = sortedLogs.map((log) => [
      log.id,
      log.username,
      log.action,
      log.entityType,
      log.entityId,
      log.details,
      log.createdAt,
    ]);

    const csv = [
      headers.map(escapeCSV).join(","),
      ...rows.map((row) => row.map(escapeCSV).join(",")),
    ].join("\r\n");

    const blob = new Blob(["\uFEFF" + csv], {
      type: "text/csv;charset=utf-8;",
    });

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = "finnexa-audit-logs.csv";

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <div className="audit-page">
        <div className="audit-loading">
          <div className="audit-spinner" />
          <p>Loading audit logs...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="audit-page">
      {/* Header */}
      <header className="audit-header">
        <div>
          <div className="audit-breadcrumb">
            <span>FinNexa</span>
            <span>/</span>
            <span>Audit Logs</span>
          </div>

          <h1>Audit Logs</h1>

          <p>Monitor user activities and track important system operations.</p>
        </div>

        <div className="audit-header-actions">
          <button
            className="audit-export-btn"
            onClick={handleExport}
            disabled={!sortedLogs.length}
          >
            <i className="bi bi-download" />
            Export CSV
          </button>

          <button
            className="audit-refresh-btn"
            onClick={() => loadAuditLogs(true)}
            disabled={refreshing}
          >
            <i className="bi bi-arrow-clockwise" />
            {refreshing ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </header>

      {/* Error */}
      {error && (
        <div className="audit-error">
          <i className="bi bi-exclamation-circle" />
          {error}
        </div>
      )}

      {/* Statistics */}
      <section className="audit-stats-grid">
        <article className="audit-stat-card">
          <span>Total Activities</span>
          <h2>{logs.length}</h2>
          <i className="bi bi-activity" />
        </article>

        <article className="audit-stat-card">
          <span>Unique Users</span>
          <h2>{uniqueUsers.length}</h2>
          <i className="bi bi-people" />
        </article>

        <article className="audit-stat-card">
          <span>Activity Types</span>
          <h2>{uniqueActions.length}</h2>
          <i className="bi bi-list-check" />
        </article>

        <article className="audit-stat-card">
          <span>Filtered Results</span>
          <h2>{sortedLogs.length}</h2>
          <i className="bi bi-funnel" />
        </article>
      </section>

      {/* Advanced Filters */}
      <section className="audit-filter-card">
        <div className="audit-filter-heading">
          <div>
            <h3>
              <i className="bi bi-funnel" /> Advanced Filters
            </h3>
            <p>Find specific activities using multiple filters.</p>
          </div>

          <button
            className="audit-clear-btn"
            onClick={clearFilters}
            disabled={!hasActiveFilters}
          >
            <i className="bi bi-arrow-counterclockwise" />
            Clear Filters
          </button>
        </div>

        <div className="audit-filter-grid">
          {/* Search */}
          <div className="audit-filter-field audit-search-field">
            <label>Search Activities</label>

            <div className="audit-search">
              <i className="bi bi-search" />

              <input
                type="text"
                placeholder="Search username, action, entity..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
            </div>
          </div>

          {/* Action */}
          <div className="audit-filter-field">
            <label>Action Type</label>

            <select
              value={actionFilter}
              onChange={(event) => setActionFilter(event.target.value)}
            >
              <option value="ALL">All Actions</option>

              {uniqueActions.map((action) => (
                <option key={action} value={action}>
                  {action}
                </option>
              ))}
            </select>
          </div>

          {/* User */}
          <div className="audit-filter-field">
            <label>Username</label>

            <select
              value={userFilter}
              onChange={(event) => setUserFilter(event.target.value)}
            >
              <option value="ALL">All Users</option>

              {uniqueUsers.map((user) => (
                <option key={user} value={user}>
                  {user}
                </option>
              ))}
            </select>
          </div>

          {/* Entity */}
          <div className="audit-filter-field">
            <label>Entity Type</label>

            <select
              value={entityFilter}
              onChange={(event) => setEntityFilter(event.target.value)}
            >
              <option value="ALL">All Entities</option>

              {uniqueEntities.map((entity) => (
                <option key={entity} value={entity}>
                  {entity}
                </option>
              ))}
            </select>
          </div>

          {/* Start Date */}
          <div className="audit-filter-field">
            <label>From Date</label>

            <input
              type="date"
              value={startDate}
              max={endDate || undefined}
              onChange={(event) => setStartDate(event.target.value)}
            />
          </div>

          {/* End Date */}
          <div className="audit-filter-field">
            <label>To Date</label>

            <input
              type="date"
              value={endDate}
              min={startDate || undefined}
              onChange={(event) => setEndDate(event.target.value)}
            />
          </div>
        </div>

        <div className="audit-filter-footer">
          <span>
            <i className="bi bi-check-circle" />
            {sortedLogs.length} matching records found
          </span>
        </div>
      </section>

      {/* Audit Table */}
      <section className="audit-table-card">
        <div className="audit-table-heading">
          <div>
            <h2>Activity History</h2>
            <p>Recorded system activities in chronological order.</p>
          </div>

          <span>{sortedLogs.length} records</span>
        </div>

        <div className="audit-table-wrapper">
          <table className="audit-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>USER</th>
                <th>ACTION</th>
                <th>ENTITY</th>
                <th>ENTITY ID</th>
                <th>DATE & TIME</th>
                <th>DETAILS</th>
              </tr>
            </thead>

            <tbody>
              {paginatedLogs.map((log) => (
                <tr key={log.id}>
                  <td>#{log.id}</td>

                  <td>
                    <div className="audit-user">
                      <span className="audit-avatar">
                        {log.username?.charAt(0)?.toUpperCase() || "U"}
                      </span>

                      {log.username || "Unknown"}
                    </div>
                  </td>

                  <td>
                    <span
                      className={`audit-action ${getActionClass(log.action)}`}
                    >
                      {log.action || "—"}
                    </span>
                  </td>

                  <td>{log.entityType || "—"}</td>

                  <td>{log.entityId ?? "—"}</td>

                  <td>{formatDate(log.createdAt)}</td>

                  <td>
                    <button
                      className="audit-view-btn"
                      onClick={() => setSelectedLog(log)}
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {sortedLogs.length === 0 && (
            <div className="audit-empty">
              <i className="bi bi-journal-x" />
              <h3>No audit logs found</h3>
              <p>Try changing your search or filter criteria.</p>

              <button className="audit-clear-btn" onClick={clearFilters}>
                Clear Filters
              </button>
            </div>
          )}
        </div>

        {/* Pagination */}
        {sortedLogs.length > 0 && (
          <div className="audit-pagination">
            <div className="audit-pagination-info">
              Showing <strong>{startIndex + 1}</strong>–
              <strong>{endIndex}</strong> of{" "}
              <strong>{sortedLogs.length}</strong> records
            </div>

            <div className="audit-pagination-controls">
              <select
                value={pageSize}
                onChange={(event) => {
                  setPageSize(Number(event.target.value));
                }}
              >
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>
                    {size} per page
                  </option>
                ))}
              </select>

              <button
                onClick={() => setCurrentPage(1)}
                disabled={safePage === 1}
                aria-label="First page"
              >
                <i className="bi bi-chevron-bar-left" />
              </button>

              <button
                onClick={() => setCurrentPage((page) => page - 1)}
                disabled={safePage === 1}
                aria-label="Previous page"
              >
                <i className="bi bi-chevron-left" />
              </button>

              <span className="audit-page-number">
                {safePage} / {totalPages}
              </span>

              <button
                onClick={() => setCurrentPage((page) => page + 1)}
                disabled={safePage === totalPages}
                aria-label="Next page"
              >
                <i className="bi bi-chevron-right" />
              </button>

              <button
                onClick={() => setCurrentPage(totalPages)}
                disabled={safePage === totalPages}
                aria-label="Last page"
              >
                <i className="bi bi-chevron-bar-right" />
              </button>
            </div>
          </div>
        )}
      </section>

      {/* Details Modal */}
      {selectedLog && (
        <div
          className="audit-modal-overlay"
          onClick={() => setSelectedLog(null)}
        >
          <div
            className="audit-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="audit-modal-header">
              <div>
                <h2>Activity Details</h2>
                <p>Audit record #{selectedLog.id}</p>
              </div>

              <button onClick={() => setSelectedLog(null)} aria-label="Close">
                <i className="bi bi-x-lg" />
              </button>
            </div>

            <div className="audit-detail-list">
              <div>
                <span>Username</span>
                <strong>{selectedLog.username || "—"}</strong>
              </div>

              <div>
                <span>Action</span>
                <strong>{selectedLog.action || "—"}</strong>
              </div>

              <div>
                <span>Entity Type</span>
                <strong>{selectedLog.entityType || "—"}</strong>
              </div>

              <div>
                <span>Entity ID</span>
                <strong>{selectedLog.entityId ?? "—"}</strong>
              </div>

              <div>
                <span>Date & Time</span>
                <strong>{formatDate(selectedLog.createdAt)}</strong>
              </div>

              <div className="audit-detail-description">
                <span>Details</span>
                <p>{selectedLog.details || "No additional details."}</p>
              </div>
            </div>

            <button
              className="audit-modal-close"
              onClick={() => setSelectedLog(null)}
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
