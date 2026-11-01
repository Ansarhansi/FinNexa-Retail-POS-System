import { useEffect, useMemo, useState } from "react";
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
} from "../../services/productService";
import "./ProductsPage.css";

const emptyForm = {
  name: "",
  sku: "",
  description: "",
  price: "",
  quantity: "",
};

function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState("");
  const [search, setSearch] = useState("");
  const [stockFilter, setStockFilter] = useState("all");
  const [currentPage, setCurrentPage] = useState(1);

  const [showModal, setShowModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const pageSize = 5;

  // Load products from the backend
  const loadProducts = async () => {
    try {
      setLoading(true);
      setPageError("");

      const data = await getProducts();
      setProducts(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Failed to load products:", error);

      setPageError(
        error.response?.status === 401
          ? "Your session is unauthorized. Please log in again."
          : error.response?.status === 403
            ? "You do not have permission to view products."
            : "Unable to load products. Check your backend connection and try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProducts();
  }, []);

  // Summary cards
  const totalProducts = products.length;

  const lowStockProducts = products.filter(
    (product) => product.quantity > 0 && product.quantity <= 10,
  ).length;

  const outOfStockProducts = products.filter(
    (product) => product.quantity === 0,
  ).length;

  // Search and stock filtering
  const filteredProducts = useMemo(() => {
    return products.filter((product) => {
      const searchText = search.toLowerCase();

      const matchesSearch =
        product.name?.toLowerCase().includes(searchText) ||
        product.sku?.toLowerCase().includes(searchText) ||
        product.description?.toLowerCase().includes(searchText);

      let matchesStock = true;

      if (stockFilter === "low") {
        matchesStock = product.quantity > 0 && product.quantity <= 10;
      } else if (stockFilter === "out") {
        matchesStock = product.quantity === 0;
      }

      return matchesSearch && matchesStock;
    });
  }, [products, search, stockFilter]);

  // Pagination
  const totalPages = Math.max(1, Math.ceil(filteredProducts.length / pageSize));

  const displayedProducts = filteredProducts.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize,
  );

  useEffect(() => {
    setCurrentPage(1);
  }, [search, stockFilter]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  // Formatting helpers
  const formatCurrency = (amount) =>
    new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 2,
    }).format(Number(amount) || 0);

  const getStockBadge = (quantity) => {
    if (quantity === 0) {
      return <span className="stock-badge stock-out">Out of Stock</span>;
    }

    if (quantity <= 10) {
      return <span className="stock-badge stock-low">Low Stock</span>;
    }

    return <span className="stock-badge stock-available">In Stock</span>;
  };

  // Open modal for adding a product
  const openAddModal = () => {
    setEditingProduct(null);
    setForm(emptyForm);
    setFormErrors({});
    setFormError("");
    setSuccessMessage("");
    setShowModal(true);
  };

  // Open modal for editing a product
  const openEditModal = (product) => {
    setEditingProduct(product);

    setForm({
      name: product.name ?? "",
      sku: product.sku ?? "",
      description: product.description ?? "",
      price: String(product.price ?? ""),
      quantity: String(product.quantity ?? ""),
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
    setEditingProduct(null);
    setForm(emptyForm);
    setFormErrors({});
    setFormError("");
  };

  // Handle form changes
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

  // Validate form
  const validateForm = () => {
    const errors = {};

    if (!form.name.trim()) {
      errors.name = "Product name is required.";
    }

    if (!form.sku.trim()) {
      errors.sku = "SKU is required.";
    }

    if (form.price === "" || !Number.isFinite(Number(form.price))) {
      errors.price = "Enter a valid price.";
    } else if (Number(form.price) <= 0) {
      errors.price = "Price must be greater than zero.";
    }

    if (form.quantity === "" || !Number.isInteger(Number(form.quantity))) {
      errors.quantity = "Enter a valid whole number.";
    } else if (Number(form.quantity) < 0) {
      errors.quantity = "Quantity cannot be negative.";
    }

    setFormErrors(errors);

    return Object.keys(errors).length === 0;
  };

  // Add or update product
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!validateForm()) return;

    const productData = {
      name: form.name.trim(),
      sku: form.sku.trim(),
      description: form.description.trim(),
      price: Number(form.price),
      quantity: Number(form.quantity),
    };

    const isEditing = Boolean(editingProduct);

    try {
      setSubmitting(true);
      setFormError("");

      if (isEditing) {
        await updateProduct(editingProduct.id, productData);
      } else {
        await createProduct(productData);
      }

      // Close the modal only after a successful API response
      setShowModal(false);
      setEditingProduct(null);
      setForm(emptyForm);
      setFormErrors({});
      setCurrentPage(1);
      setSearch("");
      setStockFilter("all");

      setSuccessMessage(
        isEditing
          ? "Product updated successfully."
          : "Product added successfully.",
      );

      await loadProducts();
    } catch (error) {
      console.error(
        isEditing ? "Failed to update product:" : "Failed to create product:",
        error,
      );

      const status = error.response?.status;
      const responseData = error.response?.data;

      const message =
        typeof responseData === "string"
          ? responseData
          : responseData?.message ||
            responseData?.error ||
            (status === 400
              ? "Invalid product details. Please check the form."
              : status === 401
                ? "Your session is unauthorized. Please log in again."
                : status === 403
                  ? `You do not have permission to ${isEditing ? "update" : "add"} products.`
                  : status === 404
                    ? "Product not found. Refresh the product list and try again."
                    : status === 409
                      ? "This SKU may already exist. Please use a different SKU."
                      : `Unable to ${isEditing ? "update" : "add"} the product. Please try again.`);

      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  };

  // Delete product
  const handleDelete = async (product) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete "${product.name}"?`,
    );

    if (!confirmed) return;

    try {
      await deleteProduct(product.id);

      setSuccessMessage("Product deleted successfully.");

      await loadProducts();
    } catch (error) {
      console.error("Failed to delete product:", error);

      window.alert(
        error.response?.data?.message ||
          "Unable to delete this product. Please try again.",
      );
    }
  };

  return (
    <div className="products-page">
      {/* Page Header */}
      <div className="products-page-header">
        <div>
          <h1>Products</h1>
          <p>Manage your products, pricing, and stock levels.</p>
        </div>

        <button
          type="button"
          className="btn btn-primary add-product-btn"
          onClick={openAddModal}
        >
          <i className="bi bi-plus-lg me-2"></i>
          Add Product
        </button>
      </div>

      {/* Success Message */}
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

      {/* Page Error */}
      {pageError && (
        <div className="alert alert-danger" role="alert">
          {pageError}

          <button
            type="button"
            className="btn btn-sm btn-outline-danger ms-3"
            onClick={loadProducts}
          >
            Retry
          </button>
        </div>
      )}

      {/* Summary Cards */}
      <div className="product-summary-grid">
        <div className="product-summary-card">
          <div className="product-summary-icon total-icon">
            <i className="bi bi-box-seam"></i>
          </div>

          <div>
            <p>Total Products</p>
            <h3>{totalProducts}</h3>
          </div>
        </div>

        <div className="product-summary-card">
          <div className="product-summary-icon low-icon">
            <i className="bi bi-exclamation-triangle"></i>
          </div>

          <div>
            <p>Low Stock</p>
            <h3>{lowStockProducts}</h3>
          </div>
        </div>

        <div className="product-summary-card">
          <div className="product-summary-icon out-icon">
            <i className="bi bi-box-seam-fill"></i>
          </div>

          <div>
            <p>Out of Stock</p>
            <h3>{outOfStockProducts}</h3>
          </div>
        </div>
      </div>

      {/* Products Table */}
      <div className="products-table-card">
        {/* Search and Filters */}
        <div className="products-toolbar">
          <div className="products-search">
            <i className="bi bi-search"></i>

            <input
              type="search"
              className="form-control"
              placeholder="Search by name, SKU, or description..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>

          <div className="products-toolbar-actions">
            <select
              className="form-select stock-filter"
              value={stockFilter}
              onChange={(event) => setStockFilter(event.target.value)}
              aria-label="Filter products by stock"
            >
              <option value="all">All Stock</option>
              <option value="low">Low Stock</option>
              <option value="out">Out of Stock</option>
            </select>

            <button
              type="button"
              className="btn btn-outline-secondary refresh-btn"
              onClick={loadProducts}
              disabled={loading}
              title="Refresh products"
            >
              <i
                className={`bi bi-arrow-clockwise ${loading ? "spin" : ""}`}
              ></i>
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="table-responsive">
          <table className="table products-table align-middle">
            <thead>
              <tr>
                <th>Product</th>
                <th>SKU</th>
                <th>Price</th>
                <th>Quantity</th>
                <th>Status</th>
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
                    Loading products...
                  </td>
                </tr>
              ) : displayedProducts.length === 0 ? (
                <tr>
                  <td colSpan="6" className="text-center py-5">
                    <i className="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>

                    <strong>No products found</strong>

                    <p className="text-secondary mb-0">
                      Try changing your search or add a new product.
                    </p>
                  </td>
                </tr>
              ) : (
                displayedProducts.map((product) => (
                  <tr key={product.id}>
                    <td>
                      <div className="product-name-cell">
                        <div className="product-table-icon">
                          <i className="bi bi-box"></i>
                        </div>

                        <div>
                          <strong>{product.name}</strong>

                          <small>
                            {product.description || "No description"}
                          </small>
                        </div>
                      </div>
                    </td>

                    <td>
                      <span className="product-sku">{product.sku}</span>
                    </td>

                    <td className="product-price">
                      {formatCurrency(product.price)}
                    </td>

                    <td>{product.quantity}</td>

                    <td>{getStockBadge(product.quantity)}</td>

                    <td className="text-end">
                      <div className="d-inline-flex align-items-center gap-2">
                        {/* Edit Button */}
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-primary product-action-btn"
                          title="Edit product"
                          aria-label={`Edit ${product.name}`}
                          onClick={() => openEditModal(product)}
                        >
                          <i className="bi bi-pencil-square"></i>
                        </button>

                        {/* Delete Button */}
                        <button
                          type="button"
                          className="btn btn-sm btn-outline-danger product-action-btn"
                          title="Delete product"
                          aria-label={`Delete ${product.name}`}
                          onClick={() => handleDelete(product)}
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
        <div className="products-pagination">
          <span>
            Showing{" "}
            {filteredProducts.length === 0
              ? 0
              : (currentPage - 1) * pageSize + 1}{" "}
            to {Math.min(currentPage * pageSize, filteredProducts.length)} of{" "}
            {filteredProducts.length} products
          </span>

          <div className="pagination-buttons">
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

      {/* Add/Edit Product Modal */}
      {showModal && (
        <div
          className="products-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeModal();
            }
          }}
        >
          <div
            className="products-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="product-modal-title"
          >
            {/* Modal Header */}
            <div className="products-modal-header">
              <div>
                <h2 id="product-modal-title">
                  {editingProduct ? "Edit Product" : "Add New Product"}
                </h2>

                <p>
                  {editingProduct
                    ? "Update the product information below."
                    : "Enter the details to add a product to your inventory."}
                </p>
              </div>

              <button
                type="button"
                className="modal-close-btn"
                onClick={closeModal}
                disabled={submitting}
                aria-label="Close modal"
              >
                <i className="bi bi-x-lg"></i>
              </button>
            </div>

            {/* Product Form */}
            <form onSubmit={handleSubmit} noValidate>
              <div className="products-modal-body">
                {formError && (
                  <div className="alert alert-danger" role="alert">
                    <i className="bi bi-exclamation-circle me-2"></i>
                    {formError}
                  </div>
                )}

                <div className="row g-3">
                  {/* Product Name */}
                  <div className="col-md-6">
                    <label htmlFor="product-name" className="form-label">
                      Product Name <span className="text-danger">*</span>
                    </label>

                    <input
                      id="product-name"
                      name="name"
                      type="text"
                      className={`form-control ${
                        formErrors.name ? "is-invalid" : ""
                      }`}
                      placeholder="e.g. Wireless Mouse"
                      value={form.name}
                      onChange={handleInputChange}
                      maxLength={150}
                      autoFocus
                    />

                    {formErrors.name && (
                      <div className="invalid-feedback">{formErrors.name}</div>
                    )}
                  </div>

                  {/* SKU */}
                  <div className="col-md-6">
                    <label htmlFor="product-sku" className="form-label">
                      SKU <span className="text-danger">*</span>
                    </label>

                    <input
                      id="product-sku"
                      name="sku"
                      type="text"
                      className={`form-control ${
                        formErrors.sku ? "is-invalid" : ""
                      }`}
                      placeholder="e.g. FN-MOUSE-001"
                      value={form.sku}
                      onChange={handleInputChange}
                      maxLength={100}
                    />

                    {formErrors.sku && (
                      <div className="invalid-feedback">{formErrors.sku}</div>
                    )}
                  </div>

                  {/* Description */}
                  <div className="col-12">
                    <label htmlFor="product-description" className="form-label">
                      Description
                    </label>

                    <textarea
                      id="product-description"
                      name="description"
                      className="form-control"
                      rows="3"
                      placeholder="Describe the product (optional)"
                      value={form.description}
                      onChange={handleInputChange}
                      maxLength={1000}
                    ></textarea>
                  </div>

                  {/* Price */}
                  <div className="col-md-6">
                    <label htmlFor="product-price" className="form-label">
                      Price (₹) <span className="text-danger">*</span>
                    </label>

                    <input
                      id="product-price"
                      name="price"
                      type="number"
                      min="0.01"
                      step="0.01"
                      className={`form-control ${
                        formErrors.price ? "is-invalid" : ""
                      }`}
                      placeholder="0.00"
                      value={form.price}
                      onChange={handleInputChange}
                    />

                    {formErrors.price && (
                      <div className="invalid-feedback">{formErrors.price}</div>
                    )}
                  </div>

                  {/* Quantity */}
                  <div className="col-md-6">
                    <label htmlFor="product-quantity" className="form-label">
                      Quantity <span className="text-danger">*</span>
                    </label>

                    <input
                      id="product-quantity"
                      name="quantity"
                      type="number"
                      min="0"
                      step="1"
                      className={`form-control ${
                        formErrors.quantity ? "is-invalid" : ""
                      }`}
                      placeholder="0"
                      value={form.quantity}
                      onChange={handleInputChange}
                    />

                    {formErrors.quantity && (
                      <div className="invalid-feedback">
                        {formErrors.quantity}
                      </div>
                    )}

                    <small className="text-secondary">
                      Enter 0 if the product is currently out of stock.
                    </small>
                  </div>
                </div>
              </div>

              {/* Modal Footer */}
              <div className="products-modal-footer">
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
                      {editingProduct ? "Updating..." : "Saving..."}
                    </>
                  ) : (
                    <>
                      <i
                        className={`bi ${
                          editingProduct ? "bi-check-lg" : "bi-plus-lg"
                        } me-2`}
                      ></i>

                      {editingProduct ? "Save Changes" : "Save Product"}
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

export default ProductsPage;
