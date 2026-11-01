import { useEffect, useMemo, useState } from "react";
import {
  getSuppliers,
  createSupplier,
  updateSupplier,
  deleteSupplier,
} from "../../services/supplierService";
import "./SuppliersPage.css";

const EMPTY_FORM = {
  name: "",
  contactPerson: "",
  email: "",
  phone: "",
  address: "",
};

const PAGE_SIZE = 5;

function SuppliersPage() {
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);

  const [showModal, setShowModal] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState(null);
  const [form, setForm] = useState({ ...EMPTY_FORM });

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Fetch suppliers
  const loadSuppliers = async (showLoader = true) => {
    if (showLoader) {
      setLoading(true);
    }

    try {
      const data = await getSuppliers();

      setSuppliers(Array.isArray(data) ? data : []);
      setError("");
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Unable to load suppliers. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSuppliers();
  }, []);

  // Search suppliers
  const filteredSuppliers = useMemo(() => {
    const term = search.trim().toLowerCase();

    if (!term) {
      return suppliers;
    }

    return suppliers.filter((supplier) => {
      const fields = [
        supplier.name,
        supplier.contactPerson,
        supplier.email,
        supplier.phone,
        supplier.address,
      ];

      return fields.some((value) =>
        String(value ?? "")
          .toLowerCase()
          .includes(term),
      );
    });
  }, [suppliers, search]);

  // Pagination
  const totalPages = Math.max(
    1,
    Math.ceil(filteredSuppliers.length / PAGE_SIZE),
  );

  const currentPage = Math.min(page, totalPages);
  const startIndex = (currentPage - 1) * PAGE_SIZE;

  const visibleSuppliers = filteredSuppliers.slice(
    startIndex,
    startIndex + PAGE_SIZE,
  );

  // Format date safely
  const formatDate = (value) => {
    if (!value) {
      return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return "—";
    }

    return date.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  };

  // Open Add Supplier modal
  const openAddModal = () => {
    setEditingSupplier(null);
    setForm({ ...EMPTY_FORM });
    setError("");
    setSuccess("");
    setShowModal(true);
  };

  // Open Edit Supplier modal
  const openEditModal = (supplier) => {
    setEditingSupplier(supplier);

    setForm({
      name: supplier.name ?? "",
      contactPerson: supplier.contactPerson ?? "",
      email: supplier.email ?? "",
      phone: supplier.phone ?? "",
      address: supplier.address ?? "",
    });

    setError("");
    setSuccess("");
    setShowModal(true);
  };

  // Close modal
  const closeModal = () => {
    if (saving) {
      return;
    }

    setShowModal(false);
    setEditingSupplier(null);
    setForm({ ...EMPTY_FORM });
    setError("");
  };

  // Handle input changes
  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  // Add or update supplier
  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");
    setSuccess("");

    if (!form.name.trim()) {
      setError("Supplier name is required.");
      return;
    }

    if (
      form.email.trim() &&
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())
    ) {
      setError("Please enter a valid email address.");
      return;
    }

    const payload = {
      name: form.name.trim(),
      contactPerson: form.contactPerson.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      address: form.address.trim(),
    };

    setSaving(true);

    try {
      if (editingSupplier) {
        await updateSupplier(editingSupplier.id, payload);
        setSuccess("Supplier updated successfully.");
      } else {
        await createSupplier(payload);
        setSuccess("Supplier added successfully.");
      }

      setShowModal(false);
      setEditingSupplier(null);
      setForm({ ...EMPTY_FORM });
      setPage(1);

      await loadSuppliers(false);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          `Unable to ${
            editingSupplier ? "update" : "add"
          } supplier. Please try again.`,
      );
    } finally {
      setSaving(false);
    }
  };

  // Delete supplier — only one implementation
  const handleDelete = async (supplier) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete "${supplier.name}"?`,
    );
    if (!confirmed) return;

    try {
      setError("");
      setSuccess("");

      await deleteSupplier(supplier.id);

      setSuccess("Supplier deleted successfully.");
      await loadSuppliers(false);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Unable to delete supplier. Please try again.",
      );
    }
  };

  return (
    <div className="suppliers-page">
      {/* Page heading */}
      <div className="suppliers-heading">
        <div>
          <div className="suppliers-eyebrow">SUPPLIER MANAGEMENT</div>

          <h1>Suppliers</h1>

          <p>Manage your suppliers and maintain your business relationships.</p>
        </div>

        <button
          type="button"
          className="supplier-primary-btn"
          onClick={openAddModal}
        >
          <i className="bi bi-plus-lg" />
          Add Supplier
        </button>
      </div>

      {/* Success message */}
      {success && (
        <div className="supplier-alert supplier-alert-success" role="status">
          <i className="bi bi-check-circle-fill" />
          <span>{success}</span>

          <button
            type="button"
            onClick={() => setSuccess("")}
            aria-label="Dismiss success message"
          >
            &times;
          </button>
        </div>
      )}

      {/* Page-level error */}
      {error && !showModal && (
        <div className="supplier-alert supplier-alert-error" role="alert">
          <i className="bi bi-exclamation-circle-fill" />
          <span>{error}</span>

          <button
            type="button"
            onClick={() => setError("")}
            aria-label="Dismiss error message"
          >
            &times;
          </button>
        </div>
      )}

      {/* Summary cards */}
      <div className="supplier-stats">
        <div className="supplier-stat-card">
          <div className="supplier-stat-icon supplier-icon-purple">
            <i className="bi bi-truck" />
          </div>

          <div>
            <span>Total Suppliers</span>
            <strong>{suppliers.length}</strong>
            <small>Registered suppliers</small>
          </div>
        </div>

        <div className="supplier-stat-card">
          <div className="supplier-stat-icon supplier-icon-blue">
            <i className="bi bi-envelope" />
          </div>

          <div>
            <span>With Email</span>
            <strong>
              {suppliers.filter((supplier) => supplier.email?.trim()).length}
            </strong>
            <small>Email contact available</small>
          </div>
        </div>

        <div className="supplier-stat-card">
          <div className="supplier-stat-icon supplier-icon-green">
            <i className="bi bi-telephone" />
          </div>

          <div>
            <span>With Phone</span>
            <strong>
              {suppliers.filter((supplier) => supplier.phone?.trim()).length}
            </strong>
            <small>Phone contact available</small>
          </div>
        </div>
      </div>

      {/* Suppliers table */}
      <section className="supplier-table-card">
        <div className="supplier-table-header">
          <div>
            <h2>All Suppliers</h2>
            <p>View and manage your supplier directory.</p>
          </div>

          <button
            type="button"
            className="supplier-refresh-btn"
            onClick={() => loadSuppliers()}
            disabled={loading}
          >
            <i
              className={`bi bi-arrow-clockwise ${
                loading ? "supplier-spin" : ""
              }`}
            />
            Refresh
          </button>
        </div>

        {/* Search */}
        <div className="supplier-toolbar">
          <div className="supplier-search">
            <i className="bi bi-search" />

            <input
              type="search"
              placeholder="Search name, contact, email, phone..."
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(1);
              }}
              aria-label="Search suppliers"
            />

            {search && (
              <button
                type="button"
                onClick={() => {
                  setSearch("");
                  setPage(1);
                }}
                aria-label="Clear search"
              >
                &times;
              </button>
            )}
          </div>

          <span className="supplier-result-count">
            {filteredSuppliers.length} supplier
            {filteredSuppliers.length !== 1 ? "s" : ""}
          </span>
        </div>

        {/* Supplier table */}
        <div className="supplier-table-wrapper">
          <table className="supplier-table">
            <thead>
              <tr>
                <th>SUPPLIER</th>
                <th>CONTACT PERSON</th>
                <th>CONTACT DETAILS</th>
                <th>ADDRESS</th>
                <th>DATE ADDED</th>
                <th className="supplier-actions-heading">ACTIONS</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} className="supplier-empty">
                    <div className="supplier-loader" />
                    Loading suppliers...
                  </td>
                </tr>
              ) : visibleSuppliers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="supplier-empty">
                    <i className="bi bi-truck" />
                    <strong>No suppliers found</strong>

                    <span>
                      {search
                        ? "Try changing your search."
                        : "Add your first supplier to get started."}
                    </span>
                  </td>
                </tr>
              ) : (
                visibleSuppliers.map((supplier) => (
                  <tr key={supplier.id}>
                    <td>
                      <div className="supplier-name-cell">
                        <div className="supplier-avatar">
                          {(supplier.name || "S")
                            .trim()
                            .charAt(0)
                            .toUpperCase()}
                        </div>

                        <div>
                          <strong>{supplier.name || "Unnamed supplier"}</strong>

                          <span>
                            SUP-{String(supplier.id).padStart(4, "0")}
                          </span>
                        </div>
                      </div>
                    </td>

                    <td>{supplier.contactPerson || "—"}</td>

                    <td>
                      <div className="supplier-contact-cell">
                        {supplier.email && (
                          <span>
                            <i className="bi bi-envelope" />
                            {supplier.email}
                          </span>
                        )}

                        {supplier.phone && (
                          <span>
                            <i className="bi bi-telephone" />
                            {supplier.phone}
                          </span>
                        )}

                        {!supplier.email && !supplier.phone && "—"}
                      </div>
                    </td>

                    <td className="supplier-address-cell">
                      {supplier.address || "—"}
                    </td>

                    <td>{formatDate(supplier.createdAt)}</td>

                    <td>
                      <div className="supplier-row-actions">
                        <button
                          type="button"
                          className="supplier-edit-btn"
                          onClick={() => openEditModal(supplier)}
                          title="Edit supplier"
                          aria-label={`Edit ${supplier.name}`}
                        >
                          <i className="bi bi-pencil-square" />
                        </button>

                        <button
                          type="button"
                          className="supplier-delete-btn"
                          onClick={() => handleDelete(supplier)}
                          title="Delete supplier"
                          aria-label={`Delete ${supplier.name}`}
                        >
                          <i className="bi bi-trash3" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {!loading && filteredSuppliers.length > 0 && (
          <div className="supplier-pagination">
            <span>
              Showing {startIndex + 1}–
              {Math.min(startIndex + PAGE_SIZE, filteredSuppliers.length)} of{" "}
              {filteredSuppliers.length}
            </span>

            <div className="supplier-page-buttons">
              <button
                type="button"
                onClick={() => setPage((previous) => Math.max(1, previous - 1))}
                disabled={currentPage === 1}
                aria-label="Previous page"
              >
                <i className="bi bi-chevron-left" />
              </button>

              <span>
                Page {currentPage} of {totalPages}
              </span>

              <button
                type="button"
                onClick={() =>
                  setPage((previous) => Math.min(totalPages, previous + 1))
                }
                disabled={currentPage === totalPages}
                aria-label="Next page"
              >
                <i className="bi bi-chevron-right" />
              </button>
            </div>
          </div>
        )}
      </section>

      {/* Add/Edit Supplier modal */}
      {showModal && (
        <div
          className="supplier-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeModal();
            }
          }}
        >
          <div
            className="supplier-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="supplier-modal-title"
          >
            <div className="supplier-modal-header">
              <div>
                <span className="supplier-modal-eyebrow">
                  SUPPLIER MANAGEMENT
                </span>

                <h2 id="supplier-modal-title">
                  {editingSupplier ? "Edit Supplier" : "Add New Supplier"}
                </h2>

                <p>
                  {editingSupplier
                    ? "Update the supplier's contact information."
                    : "Enter the details to register a new supplier."}
                </p>
              </div>

              <button
                type="button"
                className="supplier-modal-close"
                onClick={closeModal}
                disabled={saving}
                aria-label="Close modal"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              {error && (
                <div
                  className="supplier-alert supplier-alert-error"
                  role="alert"
                >
                  <i className="bi bi-exclamation-circle-fill" />
                  <span>{error}</span>
                </div>
              )}

              <div className="supplier-form-grid">
                <div className="supplier-form-field supplier-field-full">
                  <label htmlFor="supplier-name">
                    Supplier Name <span>*</span>
                  </label>

                  <input
                    id="supplier-name"
                    name="name"
                    value={form.name}
                    onChange={handleChange}
                    placeholder="e.g. TechWorld Distributors"
                    required
                    maxLength={150}
                  />
                </div>

                <div className="supplier-form-field">
                  <label htmlFor="supplier-contact">Contact Person</label>

                  <input
                    id="supplier-contact"
                    name="contactPerson"
                    value={form.contactPerson}
                    onChange={handleChange}
                    placeholder="Contact person's name"
                    maxLength={150}
                  />
                </div>

                <div className="supplier-form-field">
                  <label htmlFor="supplier-phone">Phone Number</label>

                  <input
                    id="supplier-phone"
                    name="phone"
                    type="tel"
                    value={form.phone}
                    onChange={handleChange}
                    placeholder="Enter phone number"
                    maxLength={20}
                  />
                </div>

                <div className="supplier-form-field supplier-field-full">
                  <label htmlFor="supplier-email">Email Address</label>

                  <input
                    id="supplier-email"
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={handleChange}
                    placeholder="supplier@example.com"
                    maxLength={150}
                  />
                </div>

                <div className="supplier-form-field supplier-field-full">
                  <label htmlFor="supplier-address">Address</label>

                  <textarea
                    id="supplier-address"
                    name="address"
                    value={form.address}
                    onChange={handleChange}
                    placeholder="Enter supplier's address"
                    rows={3}
                    maxLength={500}
                  />
                </div>
              </div>

              <div className="supplier-modal-footer">
                <button
                  type="button"
                  className="supplier-cancel-btn"
                  onClick={closeModal}
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="supplier-primary-btn"
                  disabled={saving}
                >
                  {saving ? (
                    <>
                      <span className="supplier-button-spinner" />
                      Saving...
                    </>
                  ) : (
                    <>
                      <i
                        className={`bi ${
                          editingSupplier ? "bi-check-lg" : "bi-plus-lg"
                        }`}
                      />

                      {editingSupplier ? "Save Changes" : "Add Supplier"}
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

export default SuppliersPage;
