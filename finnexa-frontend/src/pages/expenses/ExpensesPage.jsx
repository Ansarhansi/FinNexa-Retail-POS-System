import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getExpenses,
  createExpense,
  updateExpense,
  deleteExpense,
} from "../../services/expenseService";
import "./ExpensesPage.css";

const CATEGORIES = [
  "RENT",
  "SALARIES",
  "UTILITIES",
  "INVENTORY",
  "TRANSPORTATION",
  "MARKETING",
  "MAINTENANCE",
  "OFFICE_SUPPLIES",
  "MISCELLANEOUS",
];

const PAYMENT_METHODS = ["CASH", "CARD", "UPI", "BANK_TRANSFER"];

const EMPTY_FORM = {
  title: "",
  description: "",
  amount: "",
  category: "",
  paymentMethod: "",
  expenseDate: new Date().toISOString().slice(0, 10),
};

const formatCurrency = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value) || 0);

const formatDate = (date) => {
  if (!date) return "—";

  const parsed = new Date(`${date}T00:00:00`);

  if (Number.isNaN(parsed.getTime())) return date;

  return parsed.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

const formatLabel = (value) =>
  String(value || "")
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());

const getErrorMessage = (error) => {
  const data = error.response?.data;

  if (typeof data === "string") return data;

  if (data?.message) return data.message;

  if (data?.errors && typeof data.errors === "object") {
    return Object.values(data.errors).join(", ");
  }

  return error.message || "Something went wrong. Please try again.";
};

export default function ExpensesPage() {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const [showForm, setShowForm] = useState(false);
  const [editingExpense, setEditingExpense] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const loadExpenses = useCallback(async (showRefresh = false) => {
    if (showRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    setError("");

    try {
      const data = await getExpenses();
      setExpenses(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadExpenses();
  }, [loadExpenses]);

  const currentMonthExpenses = useMemo(() => {
    const now = new Date();

    return expenses.filter((expense) => {
      if (!expense.expenseDate) return false;

      const date = new Date(`${expense.expenseDate}T00:00:00`);

      return (
        date.getFullYear() === now.getFullYear() &&
        date.getMonth() === now.getMonth()
      );
    });
  }, [expenses]);

  const totalExpenses = expenses.reduce(
    (total, expense) => total + Number(expense.amount || 0),
    0,
  );

  const monthlyExpenses = currentMonthExpenses.reduce(
    (total, expense) => total + Number(expense.amount || 0),
    0,
  );

  const averageExpense =
    expenses.length > 0 ? totalExpenses / expenses.length : 0;

  const filteredExpenses = useMemo(() => {
    const query = search.trim().toLowerCase();

    return expenses.filter((expense) => {
      const matchesSearch =
        !query ||
        String(expense.title || "")
          .toLowerCase()
          .includes(query) ||
        String(expense.description || "")
          .toLowerCase()
          .includes(query);

      const matchesCategory =
        categoryFilter === "ALL" || expense.category === categoryFilter;

      const matchesStartDate =
        !startDate || (expense.expenseDate && expense.expenseDate >= startDate);

      const matchesEndDate =
        !endDate || (expense.expenseDate && expense.expenseDate <= endDate);

      return (
        matchesSearch && matchesCategory && matchesStartDate && matchesEndDate
      );
    });
  }, [expenses, search, categoryFilter, startDate, endDate]);

  const totalPages = Math.max(1, Math.ceil(filteredExpenses.length / pageSize));

  const paginatedExpenses = filteredExpenses.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  useEffect(() => {
    setCurrentPage(1);
  }, [search, categoryFilter, startDate, endDate, pageSize]);

  const resetFilters = () => {
    setSearch("");
    setCategoryFilter("ALL");
    setStartDate("");
    setEndDate("");
  };

  const openCreateForm = () => {
    setEditingExpense(null);
    setForm({ ...EMPTY_FORM });
    setError("");
    setSuccess("");
    setShowForm(true);
  };

  const openEditForm = (expense) => {
    setEditingExpense(expense);
    setForm({
      title: expense.title || "",
      description: expense.description || "",
      amount: String(expense.amount ?? ""),
      category: expense.category || "",
      paymentMethod: expense.paymentMethod || "",
      expenseDate: expense.expenseDate || new Date().toISOString().slice(0, 10),
    });
    setError("");
    setSuccess("");
    setShowForm(true);
  };

  const closeForm = () => {
    if (saving) return;

    setShowForm(false);
    setEditingExpense(null);
    setForm({ ...EMPTY_FORM });
  };

  const handleFormChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!form.title.trim()) {
      setError("Please enter an expense title.");
      return;
    }

    if (!form.description.trim()) {
      setError("Please enter an expense description.");
      return;
    }

    if (!Number.isFinite(Number(form.amount)) || Number(form.amount) <= 0) {
      setError("Expense amount must be greater than zero.");
      return;
    }

    if (!form.category || !form.paymentMethod || !form.expenseDate) {
      setError("Please complete all required fields.");
      return;
    }

    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      amount: Number(form.amount),
      category: form.category,
      paymentMethod: form.paymentMethod,
      expenseDate: form.expenseDate,
    };

    setSaving(true);

    try {
      if (editingExpense) {
        await updateExpense(editingExpense.id, payload);
        setSuccess("Expense updated successfully.");
      } else {
        await createExpense(payload);
        setSuccess("Expense created successfully.");
      }

      setShowForm(false);
      setEditingExpense(null);
      setForm({ ...EMPTY_FORM });

      await loadExpenses();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (expense) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete "${expense.title}"? This action cannot be undone.`,
    );

    if (!confirmed) return;

    setError("");
    setSuccess("");
    setDeletingId(expense.id);

    try {
      await deleteExpense(expense.id);
      setSuccess("Expense deleted successfully.");
      await loadExpenses(true);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  };

  const clearNotifications = () => {
    setError("");
    setSuccess("");
  };

  if (loading) {
    return (
      <div className="expenses-page">
        <div className="expenses-loading">
          <div className="expenses-spinner" />
          <p>Loading expenses...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="expenses-page">
      <div className="expenses-page-header">
        <div>
          <div className="expenses-breadcrumb">
            <span>FinNexa</span>
            <span>/</span>
            <span>Expenses</span>
          </div>

          <h1>Expense Management</h1>

          <p className="expenses-subtitle">
            Track business spending and manage your expense records.
          </p>
        </div>

        <div className="expenses-header-actions">
          <button
            type="button"
            className="expenses-refresh-btn"
            onClick={() => loadExpenses(true)}
            disabled={refreshing}
          >
            <i
              className={`bi bi-arrow-clockwise ${
                refreshing ? "expenses-rotating" : ""
              }`}
            />
            Refresh
          </button>

          <button
            type="button"
            className="expenses-primary-btn"
            onClick={openCreateForm}
          >
            <i className="bi bi-plus-lg" />
            Add Expense
          </button>
        </div>
      </div>

      {(error || success) && (
        <div
          className={`expenses-alert ${
            error ? "expenses-alert-error" : "expenses-alert-success"
          }`}
          role="alert"
        >
          <i
            className={`bi ${
              error ? "bi-exclamation-circle" : "bi-check-circle"
            }`}
          />
          <span>{error || success}</span>
          <button
            type="button"
            onClick={clearNotifications}
            aria-label="Dismiss notification"
          >
            <i className="bi bi-x-lg" />
          </button>
        </div>
      )}

      <section className="expenses-stats-grid">
        <article className="expenses-stat-card">
          <div className="expenses-stat-top">
            <span>Total Expenses</span>
            <span className="expenses-stat-icon total-icon">
              <i className="bi bi-wallet2" />
            </span>
          </div>
          <h2>{formatCurrency(totalExpenses)}</h2>
          <p>Across all recorded expenses</p>
        </article>

        <article className="expenses-stat-card">
          <div className="expenses-stat-top">
            <span>This Month</span>
            <span className="expenses-stat-icon monthly-icon">
              <i className="bi bi-calendar3" />
            </span>
          </div>
          <h2>{formatCurrency(monthlyExpenses)}</h2>
          <p>Expenses dated this month</p>
        </article>

        <article className="expenses-stat-card">
          <div className="expenses-stat-top">
            <span>Total Records</span>
            <span className="expenses-stat-icon records-icon">
              <i className="bi bi-receipt" />
            </span>
          </div>
          <h2>{expenses.length}</h2>
          <p>Recorded expense entries</p>
        </article>

        <article className="expenses-stat-card">
          <div className="expenses-stat-top">
            <span>Average Expense</span>
            <span className="expenses-stat-icon average-icon">
              <i className="bi bi-graph-up-arrow" />
            </span>
          </div>
          <h2 className="expenses-average-value">
            {formatCurrency(averageExpense)}
          </h2>
          <p>Average amount per record</p>
        </article>
      </section>

      <section className="expenses-table-card">
        <div className="expenses-table-heading">
          <div>
            <h2>Expense Records</h2>
            <p>Review, search, and manage your expenses.</p>
          </div>

          <span className="expenses-record-count">
            {filteredExpenses.length} records
          </span>
        </div>

        <div className="expenses-filters">
          <div className="expenses-search">
            <i className="bi bi-search" />
            <input
              type="search"
              placeholder="Search title or description..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>

          <div className="expenses-filter-field">
            <i className="bi bi-funnel" />
            <select
              value={categoryFilter}
              onChange={(event) => setCategoryFilter(event.target.value)}
              aria-label="Filter by category"
            >
              <option value="ALL">All categories</option>
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {formatLabel(category)}
                </option>
              ))}
            </select>
          </div>

          <div className="expenses-date-field">
            <label htmlFor="expenses-start-date">From</label>
            <input
              id="expenses-start-date"
              type="date"
              value={startDate}
              max={endDate || undefined}
              onChange={(event) => setStartDate(event.target.value)}
            />
          </div>

          <div className="expenses-date-field">
            <label htmlFor="expenses-end-date">To</label>
            <input
              id="expenses-end-date"
              type="date"
              value={endDate}
              min={startDate || undefined}
              onChange={(event) => setEndDate(event.target.value)}
            />
          </div>

          {(search || categoryFilter !== "ALL" || startDate || endDate) && (
            <button
              type="button"
              className="expenses-clear-btn"
              onClick={resetFilters}
            >
              Clear
            </button>
          )}
        </div>

        <div className="expenses-table-wrapper">
          <table className="expenses-table">
            <thead>
              <tr>
                <th>EXPENSE</th>
                <th>CATEGORY</th>
                <th>DATE</th>
                <th>PAYMENT</th>
                <th>AMOUNT</th>
                <th>ACTIONS</th>
              </tr>
            </thead>

            <tbody>
              {paginatedExpenses.map((expense) => (
                <tr key={expense.id}>
                  <td>
                    <div className="expenses-title-cell">
                      <span className="expenses-row-icon">
                        <i className="bi bi-receipt-cutoff" />
                      </span>
                      <div>
                        <strong>{expense.title}</strong>
                        <small>{expense.description}</small>
                      </div>
                    </div>
                  </td>

                  <td>
                    <span className="expenses-category-badge">
                      {formatLabel(expense.category)}
                    </span>
                  </td>

                  <td className="expenses-date-cell">
                    {formatDate(expense.expenseDate)}
                  </td>

                  <td>
                    <span className="expenses-payment">
                      {formatLabel(expense.paymentMethod)}
                    </span>
                  </td>

                  <td>
                    <strong className="expenses-amount">
                      {formatCurrency(expense.amount)}
                    </strong>
                  </td>

                  <td>
                    <div className="expenses-row-actions">
                      <button
                        type="button"
                        className="expenses-edit-btn"
                        title="Edit expense"
                        aria-label={`Edit ${expense.title}`}
                        onClick={() => openEditForm(expense)}
                      >
                        <i className="bi bi-pencil-square" />
                      </button>

                      <button
                        type="button"
                        className="expenses-delete-btn"
                        title="Delete expense"
                        aria-label={`Delete ${expense.title}`}
                        disabled={deletingId === expense.id}
                        onClick={() => handleDelete(expense)}
                      >
                        <i className="bi bi-trash3" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}

              {paginatedExpenses.length === 0 && (
                <tr>
                  <td colSpan="6">
                    <div className="expenses-empty">
                      <span>
                        <i className="bi bi-receipt" />
                      </span>
                      <h3>No expenses found</h3>
                      <p>
                        {search ||
                        categoryFilter !== "ALL" ||
                        startDate ||
                        endDate
                          ? "Try changing your search or filters."
                          : "Start by adding your first expense record."}
                      </p>

                      {expenses.length === 0 ? (
                        <button
                          type="button"
                          className="expenses-primary-btn"
                          onClick={openCreateForm}
                        >
                          <i className="bi bi-plus-lg" />
                          Add Expense
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="expenses-secondary-btn"
                          onClick={resetFilters}
                        >
                          Clear filters
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="expenses-pagination">
          <span className="expenses-pagination-info">
            Showing{" "}
            {filteredExpenses.length === 0
              ? 0
              : (currentPage - 1) * pageSize + 1}
            {"–"}
            {Math.min(currentPage * pageSize, filteredExpenses.length)} of{" "}
            {filteredExpenses.length}
          </span>

          <div className="expenses-pagination-controls">
            <label htmlFor="expenses-page-size">Rows:</label>

            <select
              id="expenses-page-size"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>

            <button
              type="button"
              aria-label="Previous page"
              disabled={currentPage <= 1}
              onClick={() => setCurrentPage((page) => Math.max(1, page - 1))}
            >
              <i className="bi bi-chevron-left" />
            </button>

            <span>
              {currentPage} / {totalPages}
            </span>

            <button
              type="button"
              aria-label="Next page"
              disabled={currentPage >= totalPages}
              onClick={() =>
                setCurrentPage((page) => Math.min(totalPages, page + 1))
              }
            >
              <i className="bi bi-chevron-right" />
            </button>
          </div>
        </div>
      </section>

      {showForm && (
        <div
          className="expenses-modal-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeForm();
          }}
        >
          <section
            className="expenses-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="expenses-modal-title"
          >
            <div className="expenses-modal-header">
              <div>
                <span className="expenses-modal-icon">
                  <i
                    className={`bi ${
                      editingExpense ? "bi-pencil-square" : "bi-receipt"
                    }`}
                  />
                </span>

                <div>
                  <h2 id="expenses-modal-title">
                    {editingExpense ? "Edit Expense" : "Add New Expense"}
                  </h2>
                  <p>
                    {editingExpense
                      ? "Update the expense details below."
                      : "Enter the details to record a business expense."}
                  </p>
                </div>
              </div>

              <button
                type="button"
                className="expenses-modal-close"
                onClick={closeForm}
                disabled={saving}
                aria-label="Close form"
              >
                <i className="bi bi-x-lg" />
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="expenses-modal-body">
                {error && (
                  <div className="expenses-form-error" role="alert">
                    <i className="bi bi-exclamation-circle" />
                    {error}
                  </div>
                )}

                <div className="expenses-form-group">
                  <label htmlFor="expense-title">
                    Expense Title <span>*</span>
                  </label>
                  <input
                    id="expense-title"
                    name="title"
                    type="text"
                    maxLength={150}
                    value={form.title}
                    onChange={handleFormChange}
                    placeholder="e.g. Office rent"
                    required
                  />
                </div>

                <div className="expenses-form-group">
                  <label htmlFor="expense-description">
                    Description <span>*</span>
                  </label>
                  <textarea
                    id="expense-description"
                    name="description"
                    maxLength={500}
                    rows={3}
                    value={form.description}
                    onChange={handleFormChange}
                    placeholder="Describe the expense..."
                    required
                  />
                  <small>{form.description.length}/500 characters</small>
                </div>

                <div className="expenses-form-grid">
                  <div className="expenses-form-group">
                    <label htmlFor="expense-amount">
                      Amount (₹) <span>*</span>
                    </label>
                    <input
                      id="expense-amount"
                      name="amount"
                      type="number"
                      min="0.01"
                      step="0.01"
                      value={form.amount}
                      onChange={handleFormChange}
                      placeholder="0.00"
                      required
                    />
                  </div>

                  <div className="expenses-form-group">
                    <label htmlFor="expense-date">
                      Expense Date <span>*</span>
                    </label>
                    <input
                      id="expense-date"
                      name="expenseDate"
                      type="date"
                      value={form.expenseDate}
                      onChange={handleFormChange}
                      required
                    />
                  </div>

                  <div className="expenses-form-group">
                    <label htmlFor="expense-category">
                      Category <span>*</span>
                    </label>
                    <select
                      id="expense-category"
                      name="category"
                      value={form.category}
                      onChange={handleFormChange}
                      required
                    >
                      <option value="">Select category</option>
                      {CATEGORIES.map((category) => (
                        <option key={category} value={category}>
                          {formatLabel(category)}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="expenses-form-group">
                    <label htmlFor="expense-payment">
                      Payment Method <span>*</span>
                    </label>
                    <select
                      id="expense-payment"
                      name="paymentMethod"
                      value={form.paymentMethod}
                      onChange={handleFormChange}
                      required
                    >
                      <option value="">Select payment method</option>
                      {PAYMENT_METHODS.map((method) => (
                        <option key={method} value={method}>
                          {formatLabel(method)}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              <div className="expenses-modal-footer">
                <button
                  type="button"
                  className="expenses-secondary-btn"
                  onClick={closeForm}
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="expenses-primary-btn"
                  disabled={saving}
                >
                  {saving ? (
                    <>
                      <span className="expenses-button-spinner" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <i
                        className={`bi ${
                          editingExpense ? "bi-check2" : "bi-plus-lg"
                        }`}
                      />
                      {editingExpense ? "Save Changes" : "Create Expense"}
                    </>
                  )}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
