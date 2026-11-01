import { useEffect, useMemo, useState } from "react";
import { getProducts } from "../../services/productService";
import { getSales, createSale, cancelSale } from "../../services/saleService";
import "./SalesPage.css";

const EMPTY_ITEM = { productId: "", quantity: 1 };

const formatCurrency = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value) || 0);

const formatDate = (value) => {
  if (!value) return "—";

  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? "—"
    : date.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
};

const getErrorMessage = (error) =>
  error.response?.data?.message ||
  error.response?.data?.error ||
  error.message ||
  "Something went wrong. Please try again.";

function SalesPage() {
  const [sales, setSales] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [productsLoading, setProductsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [paymentFilter, setPaymentFilter] = useState("ALL");
  const [page, setPage] = useState(1);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedSale, setSelectedSale] = useState(null);
  const [saving, setSaving] = useState(false);
  const [cancellingId, setCancellingId] = useState(null);
  const [formError, setFormError] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("CASH");
  const [items, setItems] = useState([{ ...EMPTY_ITEM }]);

  const PAGE_SIZE = 8;

  const loadSales = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getSales();
      setSales(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const loadProducts = async () => {
    setProductsLoading(true);

    try {
      const data = await getProducts();
      setProducts(Array.isArray(data) ? data : []);
    } catch (err) {
      setError((previous) => previous || getErrorMessage(err));
    } finally {
      setProductsLoading(false);
    }
  };

  useEffect(() => {
    loadSales();
    loadProducts();
  }, []);

  const filteredSales = useMemo(() => {
    const query = search.trim().toLowerCase();

    return sales.filter((sale) => {
      const matchesSearch =
        !query ||
        String(sale.invoiceNumber || "")
          .toLowerCase()
          .includes(query) ||
        String(sale.id || "").includes(query);

      const matchesStatus =
        statusFilter === "ALL" || sale.status === statusFilter;

      const matchesPayment =
        paymentFilter === "ALL" || sale.paymentMethod === paymentFilter;

      return matchesSearch && matchesStatus && matchesPayment;
    });
  }, [sales, search, statusFilter, paymentFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredSales.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const visibleSales = filteredSales.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  const completedSales = sales.filter((sale) => sale.status === "COMPLETED");

  const cancelledSales = sales.filter((sale) => sale.status === "CANCELLED");

  const completedRevenue = completedSales.reduce(
    (total, sale) => total + (Number(sale.totalAmount) || 0),
    0,
  );

  const totalItems = items.reduce((total, item) => {
    const product = products.find(
      (entry) => String(entry.id) === String(item.productId),
    );

    return (
      total +
      (Number(product?.sellingPrice ?? product?.price) || 0) *
        (Number(item.quantity) || 0)
    );
  }, 0);

  const updateItem = (index, field, value) => {
    setItems((previous) =>
      previous.map((item, itemIndex) =>
        itemIndex === index ? { ...item, [field]: value } : item,
      ),
    );
  };

  const addItem = () => {
    setItems((previous) => [...previous, { ...EMPTY_ITEM }]);
  };

  const removeItem = (index) => {
    setItems((previous) =>
      previous.length === 1
        ? previous
        : previous.filter((_, itemIndex) => itemIndex !== index),
    );
  };

  const resetCreateForm = () => {
    setPaymentMethod("CASH");
    setItems([{ ...EMPTY_ITEM }]);
    setFormError("");
  };

  const openCreateModal = () => {
    resetCreateForm();
    setSuccess("");
    setShowCreateModal(true);
  };

  const closeCreateModal = () => {
    if (saving) return;
    setShowCreateModal(false);
    setFormError("");
  };

  const handleCreateSale = async (event) => {
    event.preventDefault();
    setFormError("");
    setSuccess("");

    const validItems = items.filter(
      (item) => item.productId && Number(item.quantity) > 0,
    );

    if (validItems.length === 0) {
      setFormError("Add at least one product with a valid quantity.");
      return;
    }

    if (validItems.length !== items.length) {
      setFormError("Select a product and valid quantity for every row.");
      return;
    }

    const productIds = validItems.map((item) => String(item.productId));

    if (new Set(productIds).size !== productIds.length) {
      setFormError(
        "Each product can only appear once. Update its quantity in the existing row.",
      );
      return;
    }

    const unavailableItem = validItems.find((item) => {
      const product = products.find(
        (entry) => String(entry.id) === String(item.productId),
      );

      return !product || product.stockQuantity === 0;
    });

    if (unavailableItem) {
      setFormError(
        "One or more selected products are unavailable. Check the product inventory.",
      );
      return;
    }

    const payload = {
      paymentMethod,
      items: validItems.map((item) => ({
        productId: Number(item.productId),
        quantity: Number(item.quantity),
      })),
    };

    setSaving(true);

    try {
      const createdSale = await createSale(payload);

      await loadSales();

      setShowCreateModal(false);
      resetCreateForm();
      setSuccess(
        `Sale created successfully${
          createdSale?.invoiceNumber
            ? ` — Invoice ${createdSale.invoiceNumber}`
            : ""
        }.`,
      );
      setPage(1);
    } catch (err) {
      setFormError(getErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const handleCancelSale = async (sale) => {
    if (sale.status !== "COMPLETED") return;

    const confirmed = window.confirm(
      `Are you sure you want to cancel invoice "${sale.invoiceNumber}"?`,
    );

    if (!confirmed) return;

    setCancellingId(sale.id);
    setError("");
    setSuccess("");

    try {
      await cancelSale(sale.id);
      await loadSales();
      setSuccess(`Invoice ${sale.invoiceNumber} cancelled successfully.`);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setCancellingId(null);
    }
  };

  const openSaleDetails = (sale) => {
    setSelectedSale(sale);
  };

  const closeSaleDetails = () => {
    setSelectedSale(null);
  };

  return (
    <div className="sales-page">
      <div className="sales-page-header">
        <div>
          <div className="sales-eyebrow">FINANCIAL MANAGEMENT / SALES</div>
          <h1>Sales Management</h1>
          <p>Manage transactions, track invoices, and monitor sales revenue.</p>
        </div>

        <button
          type="button"
          className="sales-primary-btn"
          onClick={openCreateModal}
        >
          <i className="bi bi-plus-lg" /> Create Sale
        </button>
      </div>

      {error && (
        <div className="sales-alert sales-alert-error" role="alert">
          <i className="bi bi-exclamation-circle" />
          <span>{error}</span>
          <button
            type="button"
            onClick={() => setError("")}
            aria-label="Dismiss"
          >
            ×
          </button>
        </div>
      )}

      {success && (
        <div className="sales-alert sales-alert-success" role="status">
          <i className="bi bi-check-circle" />
          <span>{success}</span>
          <button
            type="button"
            onClick={() => setSuccess("")}
            aria-label="Dismiss"
          >
            ×
          </button>
        </div>
      )}

      <section className="sales-stats-grid">
        <article className="sales-stat-card">
          <div className="sales-stat-top">
            <span>Total Revenue</span>
            <span className="sales-stat-icon revenue-icon">
              <i className="bi bi-currency-rupee" />
            </span>
          </div>
          <strong>{formatCurrency(completedRevenue)}</strong>
          <small>Completed sales only</small>
        </article>

        <article className="sales-stat-card">
          <div className="sales-stat-top">
            <span>Total Transactions</span>
            <span className="sales-stat-icon transaction-icon">
              <i className="bi bi-receipt" />
            </span>
          </div>
          <strong>{sales.length}</strong>
          <small>All recorded sales</small>
        </article>

        <article className="sales-stat-card">
          <div className="sales-stat-top">
            <span>Completed Sales</span>
            <span className="sales-stat-icon completed-icon">
              <i className="bi bi-check2-circle" />
            </span>
          </div>
          <strong>{completedSales.length}</strong>
          <small>Successfully completed</small>
        </article>

        <article className="sales-stat-card">
          <div className="sales-stat-top">
            <span>Cancelled Sales</span>
            <span className="sales-stat-icon cancelled-icon">
              <i className="bi bi-x-circle" />
            </span>
          </div>
          <strong>{cancelledSales.length}</strong>
          <small>Cancelled transactions</small>
        </article>
      </section>

      <section className="sales-table-card">
        <div className="sales-table-heading">
          <div>
            <h2>Sales History</h2>
            <p>Review invoices and manage transactions.</p>
          </div>

          <button
            type="button"
            className="sales-refresh-btn"
            onClick={() => {
              loadSales();
              loadProducts();
            }}
            disabled={loading}
            title="Refresh sales"
          >
            <i
              className={`bi bi-arrow-clockwise ${loading ? "sales-spin" : ""}`}
            />
            Refresh
          </button>
        </div>

        <div className="sales-toolbar">
          <label className="sales-search-box">
            <i className="bi bi-search" />
            <input
              type="search"
              placeholder="Search invoice number or sale ID..."
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(1);
              }}
            />
          </label>

          <select
            className="sales-filter-select"
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setPage(1);
            }}
            aria-label="Filter by status"
          >
            <option value="ALL">All statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>

          <select
            className="sales-filter-select"
            value={paymentFilter}
            onChange={(event) => {
              setPaymentFilter(event.target.value);
              setPage(1);
            }}
            aria-label="Filter by payment method"
          >
            <option value="ALL">All payments</option>
            <option value="CASH">Cash</option>
            <option value="CARD">Card</option>
            <option value="UPI">UPI</option>
            <option value="BANK_TRANSFER">Bank transfer</option>
          </select>
        </div>

        <div className="sales-table-wrapper">
          <table className="sales-table">
            <thead>
              <tr>
                <th>Invoice</th>
                <th>Date</th>
                <th>Items</th>
                <th>Payment</th>
                <th>Total Amount</th>
                <th>Status</th>
                <th className="sales-actions-column">Actions</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="sales-table-message">
                    <span className="sales-loader" />
                    Loading sales...
                  </td>
                </tr>
              ) : visibleSales.length === 0 ? (
                <tr>
                  <td colSpan="7" className="sales-table-message">
                    <div className="sales-empty-state">
                      <i className="bi bi-receipt-cutoff" />
                      <strong>No sales found</strong>
                      <span>
                        {sales.length === 0
                          ? "Create your first sale to see it here."
                          : "Try changing your search or filters."}
                      </span>
                    </div>
                  </td>
                </tr>
              ) : (
                visibleSales.map((sale) => (
                  <tr key={sale.id}>
                    <td>
                      <button
                        type="button"
                        className="sales-invoice-link"
                        onClick={() => openSaleDetails(sale)}
                      >
                        {sale.invoiceNumber || `SALE-${sale.id}`}
                      </button>
                      <span className="sales-subtext">ID: {sale.id}</span>
                    </td>

                    <td>{formatDate(sale.saleDate)}</td>

                    <td>
                      <span className="sales-items-count">
                        {sale.items?.length || 0}{" "}
                        {(sale.items?.length || 0) === 1 ? "item" : "items"}
                      </span>
                    </td>

                    <td>
                      <span className="sales-payment-label">
                        {String(sale.paymentMethod || "").replaceAll("_", " ")}
                      </span>
                    </td>

                    <td className="sales-amount">
                      {formatCurrency(sale.totalAmount)}
                    </td>

                    <td>
                      <span
                        className={`sales-status-badge ${
                          sale.status === "COMPLETED"
                            ? "status-completed"
                            : "status-cancelled"
                        }`}
                      >
                        <span className="sales-status-dot" />
                        {sale.status || "UNKNOWN"}
                      </span>
                    </td>

                    <td>
                      <div className="sales-row-actions">
                        <button
                          type="button"
                          className="sales-icon-btn"
                          title="View sale details"
                          aria-label={`View invoice ${sale.invoiceNumber}`}
                          onClick={() => openSaleDetails(sale)}
                        >
                          <i className="bi bi-eye" />
                        </button>

                        {sale.status === "COMPLETED" && (
                          <button
                            type="button"
                            className="sales-icon-btn sales-cancel-btn"
                            title="Cancel sale"
                            aria-label={`Cancel invoice ${sale.invoiceNumber}`}
                            disabled={cancellingId === sale.id}
                            onClick={() => handleCancelSale(sale)}
                          >
                            <i className="bi bi-x-circle" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="sales-pagination">
          <span>
            Showing{" "}
            {filteredSales.length === 0 ? 0 : (currentPage - 1) * PAGE_SIZE + 1}
            {"–"}
            {Math.min(currentPage * PAGE_SIZE, filteredSales.length)} of{" "}
            {filteredSales.length} sales
          </span>

          <div className="sales-pagination-actions">
            <button
              type="button"
              disabled={currentPage <= 1}
              onClick={() => setPage((previous) => Math.max(1, previous - 1))}
            >
              <i className="bi bi-chevron-left" /> Previous
            </button>

            <span className="sales-page-number">
              {currentPage} / {totalPages}
            </span>

            <button
              type="button"
              disabled={currentPage >= totalPages}
              onClick={() =>
                setPage((previous) => Math.min(totalPages, previous + 1))
              }
            >
              Next <i className="bi bi-chevron-right" />
            </button>
          </div>
        </div>
      </section>

      {showCreateModal && (
        <div className="sales-modal-backdrop" onMouseDown={closeCreateModal}>
          <section
            className="sales-modal sales-create-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sales-create-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="sales-modal-header">
              <div>
                <span className="sales-modal-eyebrow">NEW TRANSACTION</span>
                <h2 id="sales-create-title">Create Sale</h2>
                <p>Add products and choose the payment method.</p>
              </div>

              <button
                type="button"
                className="sales-modal-close"
                onClick={closeCreateModal}
                disabled={saving}
                aria-label="Close modal"
              >
                ×
              </button>
            </div>

            <form onSubmit={handleCreateSale}>
              <div className="sales-modal-body">
                {formError && (
                  <div className="sales-alert sales-alert-error">
                    <i className="bi bi-exclamation-circle" />
                    <span>{formError}</span>
                  </div>
                )}

                <div className="sales-form-section-title">
                  <span>01</span>
                  <div>
                    <strong>Products</strong>
                    <small>Select products and quantities</small>
                  </div>
                </div>

                {productsLoading ? (
                  <div className="sales-product-loading">
                    <span className="sales-loader" />
                    Loading products...
                  </div>
                ) : products.length === 0 ? (
                  <div className="sales-product-empty">
                    No products available. Add products in the Products module
                    before creating a sale.
                  </div>
                ) : (
                  <div className="sales-sale-items">
                    {items.map((item, index) => {
                      const selectedProduct = products.find(
                        (product) =>
                          String(product.id) === String(item.productId),
                      );

                      const unitPrice = Number(
                        selectedProduct?.sellingPrice ??
                          selectedProduct?.price ??
                          0,
                      );

                      return (
                        <div className="sales-sale-item" key={index}>
                          <div className="sales-product-select-wrap">
                            <label htmlFor={`sales-product-${index}`}>
                              Product
                            </label>
                            <select
                              id={`sales-product-${index}`}
                              value={item.productId}
                              onChange={(event) =>
                                updateItem(
                                  index,
                                  "productId",
                                  event.target.value,
                                )
                              }
                              required
                            >
                              <option value="">Select a product</option>
                              {products.map((product) => (
                                <option
                                  key={product.id}
                                  value={product.id}
                                  disabled={
                                    product.stockQuantity === 0 ||
                                    items.some(
                                      (otherItem, otherIndex) =>
                                        otherIndex !== index &&
                                        String(otherItem.productId) ===
                                          String(product.id),
                                    )
                                  }
                                >
                                  {product.name}
                                  {product.sku ? ` (${product.sku})` : ""}
                                  {product.stockQuantity !== undefined
                                    ? ` — Stock: ${product.stockQuantity}`
                                    : ""}
                                </option>
                              ))}
                            </select>

                            {selectedProduct && (
                              <small>
                                Unit price: {formatCurrency(unitPrice)}
                              </small>
                            )}
                          </div>

                          <div className="sales-quantity-wrap">
                            <label htmlFor={`sales-quantity-${index}`}>
                              Quantity
                            </label>
                            <input
                              id={`sales-quantity-${index}`}
                              type="number"
                              min="1"
                              step="1"
                              required
                              value={item.quantity}
                              onChange={(event) =>
                                updateItem(
                                  index,
                                  "quantity",
                                  event.target.value,
                                )
                              }
                            />
                          </div>

                          <div className="sales-line-total">
                            <label>Subtotal</label>
                            <strong>
                              {formatCurrency(
                                unitPrice * (Number(item.quantity) || 0),
                              )}
                            </strong>
                          </div>

                          <button
                            type="button"
                            className="sales-remove-item"
                            onClick={() => removeItem(index)}
                            disabled={items.length === 1}
                            aria-label="Remove product"
                            title="Remove product"
                          >
                            <i className="bi bi-trash3" />
                          </button>
                        </div>
                      );
                    })}

                    <button
                      type="button"
                      className="sales-add-item-btn"
                      onClick={addItem}
                    >
                      <i className="bi bi-plus-circle" /> Add another product
                    </button>
                  </div>
                )}

                <div className="sales-form-section-title sales-payment-section">
                  <span>02</span>
                  <div>
                    <strong>Payment method</strong>
                    <small>Choose how the customer paid</small>
                  </div>
                </div>

                <div className="sales-payment-options">
                  {[
                    { value: "CASH", label: "Cash", icon: "bi-cash" },
                    { value: "CARD", label: "Card", icon: "bi-credit-card" },
                    { value: "UPI", label: "UPI", icon: "bi-phone" },
                    {
                      value: "BANK_TRANSFER",
                      label: "Bank Transfer",
                      icon: "bi-bank",
                    },
                  ].map((method) => (
                    <button
                      key={method.value}
                      type="button"
                      className={`sales-payment-option ${
                        paymentMethod === method.value ? "selected" : ""
                      }`}
                      onClick={() => setPaymentMethod(method.value)}
                    >
                      <i className={`bi ${method.icon}`} />
                      <span>{method.label}</span>
                      {paymentMethod === method.value && (
                        <i className="bi bi-check-circle-fill sales-payment-check" />
                      )}
                    </button>
                  ))}
                </div>

                <div className="sales-total-panel">
                  <div>
                    <span>Estimated total</span>
                    <small>Final amount is calculated by the server.</small>
                  </div>
                  <strong>{formatCurrency(totalItems)}</strong>
                </div>
              </div>

              <div className="sales-modal-footer">
                <button
                  type="button"
                  className="sales-secondary-btn"
                  onClick={closeCreateModal}
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="sales-primary-btn"
                  disabled={saving || productsLoading || products.length === 0}
                >
                  {saving ? (
                    <>
                      <span className="sales-button-spinner" />
                      Creating...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-check2-circle" />
                      Create Sale
                    </>
                  )}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}

      {selectedSale && (
        <div className="sales-modal-backdrop" onMouseDown={closeSaleDetails}>
          <section
            className="sales-modal sales-details-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="sales-details-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="sales-modal-header">
              <div>
                <span className="sales-modal-eyebrow">TRANSACTION DETAILS</span>
                <h2 id="sales-details-title">Invoice Details</h2>
                <p>{selectedSale.invoiceNumber || `SALE-${selectedSale.id}`}</p>
              </div>

              <button
                type="button"
                className="sales-modal-close"
                onClick={closeSaleDetails}
                aria-label="Close details"
              >
                ×
              </button>
            </div>

            <div className="sales-modal-body">
              <div className="sales-invoice-summary">
                <div>
                  <span>Invoice number</span>
                  <strong>{selectedSale.invoiceNumber || "—"}</strong>
                </div>
                <div>
                  <span>Sale date</span>
                  <strong>{formatDate(selectedSale.saleDate)}</strong>
                </div>
                <div>
                  <span>Payment method</span>
                  <strong>
                    {String(selectedSale.paymentMethod || "").replaceAll(
                      "_",
                      " ",
                    )}
                  </strong>
                </div>
                <div>
                  <span>Status</span>
                  <strong>{selectedSale.status || "UNKNOWN"}</strong>
                </div>
              </div>

              <h3 className="sales-details-section-heading">Purchased items</h3>

              <div className="sales-details-items">
                <div className="sales-details-item sales-details-item-header">
                  <span>Product</span>
                  <span>Qty</span>
                  <span>Unit price</span>
                  <span>Subtotal</span>
                </div>

                {(selectedSale.items || []).map((item, index) => (
                  <div
                    className="sales-details-item"
                    key={`${item.productId}-${index}`}
                  >
                    <div>
                      <strong>{item.productName || "Product"}</strong>
                      <small>{item.sku || "—"}</small>
                    </div>
                    <span>{item.quantity}</span>
                    <span>{formatCurrency(item.unitPrice)}</span>
                    <strong>{formatCurrency(item.subtotal)}</strong>
                  </div>
                ))}

                {(!selectedSale.items || selectedSale.items.length === 0) && (
                  <p className="sales-details-no-items">
                    No item details are available.
                  </p>
                )}
              </div>

              <div className="sales-details-total">
                <span>Total amount</span>
                <strong>{formatCurrency(selectedSale.totalAmount)}</strong>
              </div>
            </div>

            <div className="sales-modal-footer">
              <button
                type="button"
                className="sales-secondary-btn"
                onClick={closeSaleDetails}
              >
                Close
              </button>

              {selectedSale.status === "COMPLETED" && (
                <button
                  type="button"
                  className="sales-danger-btn"
                  disabled={cancellingId === selectedSale.id}
                  onClick={async () => {
                    await handleCancelSale(selectedSale);
                    closeSaleDetails();
                  }}
                >
                  <i className="bi bi-x-circle" />
                  Cancel Sale
                </button>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

export default SalesPage;
