import { useEffect, useMemo, useState } from "react";
import {
  getCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
} from "../../services/customerService";
import "./CustomersPage.css";

const emptyForm = {
  name: "",
  email: "",
  phone: "",
  address: "",
};

function CustomersPage() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState("");
  const [search, setSearch] = useState("");
  const [currentPage, setCurrentPage] = useState(1);

  const [showModal, setShowModal] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formErrors, setFormErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");

  const pageSize = 5;

  // Load customers
  const loadCustomers = async () => {
    try {
      setLoading(true);
      setPageError("");

      const data = await getCustomers();
      setCustomers(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Failed to load customers:", error);

      setPageError(
        error.response?.status === 401
          ? "Your session is unauthorized. Please log in again."
          : error.response?.status === 403
            ? "You do not have permission to view customers."
            : "Unable to load customers. Check your backend connection.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCustomers();
  }, []);

  // Customer statistics
  const totalCustomers = customers.length;

  const customersWithEmail = customers.filter(
    (customer) => customer.email,
  ).length;

  const customersWithPhone = customers.filter(
    (customer) => customer.phone,
  ).length;

  // Search customers
  const filteredCustomers = useMemo(() => {
    const searchText = search.trim().toLowerCase();

    if (!searchText) return customers;

    return customers.filter((customer) => {
      return (
        customer.name?.toLowerCase().includes(searchText) ||
        customer.email?.toLowerCase().includes(searchText) ||
        customer.phone?.toLowerCase().includes(searchText) ||
        customer.address?.toLowerCase().includes(searchText)
      );
    });
  }, [customers, search]);

  // Pagination
  const totalPages = Math.max(
    1,
    Math.ceil(filteredCustomers.length / pageSize),
  );

  const displayedCustomers = filteredCustomers.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  useEffect(() => {
    setCurrentPage(1);
  }, [search]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return "—";

    const date = new Date(dateString);

    if (Number.isNaN(date.getTime())) return "—";

    return date.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  };

  // Open Add Customer modal
  const openAddModal = () => {
    setEditingCustomer(null);
    setForm(emptyForm);
    setFormErrors({});
    setFormError("");
    setSuccessMessage("");
    setShowModal(true);
  };

  // Open Edit Customer modal
  const openEditModal = (customer) => {
    setEditingCustomer(customer);

    setForm({
      name: customer.name ?? "",
      email: customer.email ?? "",
      phone: customer.phone ?? "",
      address: customer.address ?? "",
    });

    setFormErrors({});
    setFormError("");
    setSuccessMessage("");
    setShowModal(true);
  };

  // Close modal
  const closeModal = () => {
    if (submitting) return;

    setShowModal(false);
    setEditingCustomer(null);
    setForm(emptyForm);
    setFormErrors({});
    setFormError("");
  };

  // Form changes
  const handleInputChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));

    setFormErrors((previous) => ({
      ...previous,
      [name]: "",
    }));

    setFormError("");
  };

  // Validation
  const validateForm = () => {
    const errors = {};

    if (!form.name.trim()) {
      errors.name = "Customer name is required.";
    }

    if (form.email.trim()) {
      const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

      if (!emailPattern.test(form.email.trim())) {
        errors.email = "Enter a valid email address.";
      }
    }

    if (form.phone.trim()) {
      const digits = form.phone.replace(/\D/g, "");

      if (digits.length < 7 || digits.length > 15) {
        errors.phone = "Enter a valid phone number.";
      }
    }

    setFormErrors(errors);

    return Object.keys(errors).length === 0;
  };

  // Add or update customer
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!validateForm()) return;

    const customerData = {
      name: form.name.trim(),
      email: form.email.trim() || null,
      phone: form.phone.trim() || null,
      address: form.address.trim() || null,
    };

    const isEditing = Boolean(editingCustomer);

    try {
      setSubmitting(true);
      setFormError("");

      if (isEditing) {
        await updateCustomer(editingCustomer.id, customerData);
      } else {
        await createCustomer(customerData);
      }

      setShowModal(false);
      setEditingCustomer(null);
      setForm(emptyForm);
      setFormErrors({});
      setSearch("");
      setCurrentPage(1);

      setSuccessMessage(
        isEditing
          ? "Customer updated successfully."
          : "Customer added successfully.",
      );

      await loadCustomers();
    } catch (error) {
      console.error("Customer save failed:", error);

      const status = error.response?.status;
      const responseData = error.response?.data;

      setFormError(
        typeof responseData === "string"
          ? responseData
          : responseData?.message ||
              responseData?.error ||
              (status === 400
                ? "Invalid customer details. Check the form."
                : status === 401
                  ? "Your session is unauthorized. Please log in again."
                  : status === 403
                    ? `You do not have permission to ${isEditing ? "update" : "add"} customers.`
                    : status === 404
                      ? "Customer not found. Refresh the list and try again."
                      : status === 409
                        ? "A customer with these details may already exist."
                        : "Unable to save the customer. Please try again."),
      );
    } finally {
      setSubmitting(false);
    }
  };

  // Delete customer
  const handleDelete = async (customer) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete "${customer.name}"?`,
    );

    if (!confirmed) return;

    try {
      await deleteCustomer(customer.id);

      setSuccessMessage("Customer deleted successfully.");

      await loadCustomers();
    } catch (error) {
      console.error("Customer deletion failed:", error);

      window.alert(
        error.response?.data?.message ||
          "Unable to delete this customer. They may be associated with existing sales.",
      );
    }
  };

  return (
    <div className="customers-page">
      {/* Header */}
      <div className="customers-page-header">
        <div>
          <h1>Customers</h1>
          <p>Manage customer information and contact details.</p>
        </div>

        <button
          type="button"
          className="btn btn-primary customers-add-btn"
          onClick={openAddModal}
        >
          <i className="bi bi-person-plus me-2"></i>
          Add Customer
        </button>
      </div>

      {/* Success message */}
      {successMessage && (
        <div
          className="alert alert-success alert-dismissible fade show"
          role="alert"
        >
          <i className="bi bi-check-circle-fill me-2"></i>
          {successMessage}

          <button
            type="button"
            className="btn-close"
            aria-label="Close"
            onClick={() => setSuccessMessage("")}
          ></button>
        </div>
      )}

      {/* Error message */}
      {pageError && (
        <div className="alert alert-danger" role="alert">
          {pageError}

          <button
            type="button"
            className="btn btn-sm btn-outline-danger ms-3"
            onClick={loadCustomers}
          >
            Retry
          </button>
        </div>
      )}

      {/* Summary cards */}
      <div className="customers-summary-grid">
        <div className="customers-summary-card">
          <div className="customers-summary-icon customers-total-icon">
            <i className="bi bi-people"></i>
          </div>

          <div>
            <p>Total Customers</p>
            <h3>{totalCustomers}</h3>
          </div>
        </div>

        <div className="customers-summary-card">
          <div className="customers-summary-icon customers-email-icon">
            <i className="bi bi-envelope"></i>
          </div>

          <div>
            <p>With Email</p>
            <h3>{customersWithEmail}</h3>
          </div>
        </div>

        <div className="customers-summary-card">
          <div className="customers-summary-icon customers-phone-icon">
            <i className="bi bi-telephone"></i>
          </div>

          <div>
            <p>With Phone</p>
            <h3>{customersWithPhone}</h3>
          </div>
        </div>
      </div>

      {/* Customer table */}
      <div className="customers-table-card">
        <div className="customers-toolbar">
          <div className="customers-search">
            <i className="bi bi-search"></i>

            <input
              type="search"
              className="form-control"
              placeholder="Search by name, email, phone, or address..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>

          <button
            type="button"
            className="btn btn-outline-secondary customers-refresh-btn"
            onClick={loadCustomers}
            disabled={loading}
            title="Refresh customers"
          >
            <i
              className={`bi bi-arrow-clockwise ${
                loading ? "customers-spin" : ""
              }`}
            ></i>
          </button>
        </div>

        <div className="table-responsive">
          <table className="table customers-table align-middle">
            <thead>
              <tr>
                <th>Customer</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Address</th>
                <th>Created</th>
                <th className="text-end">Actions</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" className="text-center py-5">
                    <div
                      className="spinner-border spinner-border-sm text-primary me-2"
                      role="status"
                    ></div>
                    Loading customers...
                  </td>
                </tr>
              ) : displayedCustomers.length === 0 ? (
                <tr>
                  <td colSpan="6" className="text-center py-5">
                    <i className="bi bi-people fs-2 d-block mb-2 text-secondary"></i>
                    <strong>No customers found</strong>
                    <p className="text-secondary mb-0">
                      Try another search or add a customer.
                    </p>
                  </td>
                </tr>
              ) : (
                displayedCustomers.map((customer) => (
                  <tr key={customer.id}>
                    <td>
                      <div className="customers-name-cell">
                        <div className="customers-avatar">
                          {customer.name?.trim()?.charAt(0)?.toUpperCase() ||
                            "C"}
                        </div>

                        <div>
                          <strong>{customer.name}</strong>
                          <small>Customer #{customer.id}</small>
                        </div>
                      </div>
                    </td>

                    <td>
                      {customer.email ? (
                        <span className="customers-contact">
                          <i className="bi bi-envelope"></i>
                          {customer.email}
                        </span>
                      ) : (
                        <span className="customers-empty-value">
                          Not provided
                        </span>
                      )}
                    </td>

                    <td>
                      {customer.phone ? (
                        <span className="customers-contact">
                          <i className="bi bi-telephone"></i>
                          {customer.phone}
                        </span>
                      ) : (
                        <span className="customers-empty-value">
                          Not provided
                        </span>
                      )}
                    </td>

                    <td>
                      <span className="customers-address">
                        {customer.address || "Not provided"}
                      </span>
                    </td>

                    <td>
                      <span className="customers-date">
                        {formatDate(customer.createdAt)}
                      </span>
                    </td>

                    <td className="text-end">
                      <div className="d-inline-flex align-items-center gap-2">
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-primary customers-action-btn"
                          title="Edit customer"
                          aria-label={`Edit ${customer.name}`}
                          onClick={() => openEditModal(customer)}
                        >
                          <i className="bi bi-pencil-square"></i>
                        </button>

                        <button
                          type="button"
                          className="btn btn-sm btn-outline-danger customers-action-btn"
                          title="Delete customer"
                          aria-label={`Delete ${customer.name}`}
                          onClick={() => handleDelete(customer)}
                        >
                          <i className="bi bi-trash3"></i>
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
        <div className="customers-pagination">
          <span>
            Showing{" "}
            {filteredCustomers.length === 0
              ? 0
              : (currentPage - 1) * pageSize + 1}{" "}
            to {Math.min(currentPage * pageSize, filteredCustomers.length)} of{" "}
            {filteredCustomers.length} customers
          </span>

          <div className="customers-pagination-buttons">
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              disabled={currentPage === 1}
              onClick={() => setCurrentPage((page) => page - 1)}
              aria-label="Previous page"
            >
              <i className="bi bi-chevron-left"></i>
            </button>

            <span>
              Page {currentPage} of {totalPages}
            </span>

            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              disabled={currentPage >= totalPages}
              onClick={() => setCurrentPage((page) => page + 1)}
              aria-label="Next page"
            >
              <i className="bi bi-chevron-right"></i>
            </button>
          </div>
        </div>
      </div>

      {/* Add/Edit Customer Modal */}
      {showModal && (
        <div
          className="customers-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeModal();
            }
          }}
        >
          <div
            className="customers-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="customers-modal-title"
          >
            <div className="customers-modal-header">
              <div>
                <h2 id="customers-modal-title">
                  {editingCustomer ? "Edit Customer" : "Add New Customer"}
                </h2>

                <p>
                  {editingCustomer
                    ? "Update the customer's information."
                    : "Enter the customer's contact information."}
                </p>
              </div>

              <button
                type="button"
                className="customers-modal-close"
                onClick={closeModal}
                disabled={submitting}
                aria-label="Close modal"
              >
                <i className="bi bi-x-lg"></i>
              </button>
            </div>

            <form onSubmit={handleSubmit} noValidate>
              <div className="customers-modal-body">
                {formError && (
                  <div className="alert alert-danger" role="alert">
                    {formError}
                  </div>
                )}

                <div className="row g-3">
                  <div className="col-12">
                    <label htmlFor="customer-name" className="form-label">
                      Full Name <span className="text-danger">*</span>
                    </label>

                    <input
                      id="customer-name"
                      name="name"
                      type="text"
                      className={`form-control ${
                        formErrors.name ? "is-invalid" : ""
                      }`}
                      placeholder="Enter customer name"
                      value={form.name}
                      onChange={handleInputChange}
                      maxLength={150}
                      autoFocus
                    />

                    {formErrors.name && (
                      <div className="invalid-feedback">{formErrors.name}</div>
                    )}
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="customer-email" className="form-label">
                      Email Address
                    </label>

                    <input
                      id="customer-email"
                      name="email"
                      type="email"
                      className={`form-control ${
                        formErrors.email ? "is-invalid" : ""
                      }`}
                      placeholder="customer@example.com"
                      value={form.email}
                      onChange={handleInputChange}
                      maxLength={254}
                    />

                    {formErrors.email && (
                      <div className="invalid-feedback">{formErrors.email}</div>
                    )}
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="customer-phone" className="form-label">
                      Phone Number
                    </label>

                    <input
                      id="customer-phone"
                      name="phone"
                      type="tel"
                      className={`form-control ${
                        formErrors.phone ? "is-invalid" : ""
                      }`}
                      placeholder="Enter phone number"
                      value={form.phone}
                      onChange={handleInputChange}
                      maxLength={20}
                    />

                    {formErrors.phone && (
                      <div className="invalid-feedback">{formErrors.phone}</div>
                    )}
                  </div>

                  <div className="col-12">
                    <label htmlFor="customer-address" className="form-label">
                      Address
                    </label>

                    <textarea
                      id="customer-address"
                      name="address"
                      className="form-control"
                      rows="3"
                      placeholder="Enter customer address"
                      value={form.address}
                      onChange={handleInputChange}
                      maxLength={500}
                    ></textarea>
                  </div>
                </div>
              </div>

              <div className="customers-modal-footer">
                <button
                  type="button"
                  className="btn btn-light"
                  onClick={closeModal}
                  disabled={submitting}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2"></span>
                      {editingCustomer ? "Updating..." : "Saving..."}
                    </>
                  ) : (
                    <>
                      <i
                        className={`bi ${
                          editingCustomer ? "bi-check-lg" : "bi-plus-lg"
                        } me-2`}
                      ></i>

                      {editingCustomer ? "Save Changes" : "Save Customer"}
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

export default CustomersPage;
