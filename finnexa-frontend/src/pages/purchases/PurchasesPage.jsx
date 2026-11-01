import { useEffect, useMemo, useState } from "react";
import { getProducts } from "../../services/productService";
import { getSuppliers } from "../../services/supplierService";
import {
  getPurchases,
  createPurchase,
  receivePurchase,
  cancelPurchase,
} from "../../services/purchaseService";
import "./PurchasesPage.css";

const EMPTY_ITEM = { productId: "", quantity: 1, unitCost: "" };
const PAGE_SIZE = 8;

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

function PurchasesPage() {
  const [purchases, setPurchases] = useState([]);
  const [products, setProducts] = useState([]);
  const [suppliers, setSuppliers] = useState([]);

  const [loading, setLoading] = useState(true);
  const [dependenciesLoading, setDependenciesLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [processingId, setProcessingId] = useState(null);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [page, setPage] = useState(1);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedPurchase, setSelectedPurchase] = useState(null);

  const [supplierId, setSupplierId] = useState("");
  const [items, setItems] = useState([{ ...EMPTY_ITEM }]);
  const [formError, setFormError] = useState("");

  const loadPurchases = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getPurchases();
      setPurchases(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const loadDependencies = async () => {
    setDependenciesLoading(true);

    const results = await Promise.allSettled([getProducts(), getSuppliers()]);

    if (results[0].status === "fulfilled") {
      setProducts(Array.isArray(results[0].value) ? results[0].value : []);
    } else {
      setError((previous) => previous || getErrorMessage(results[0].reason));
    }

    if (results[1].status === "fulfilled") {
      setSuppliers(Array.isArray(results[1].value) ? results[1].value : []);
    } else {
      setError((previous) => previous || getErrorMessage(results[1].reason));
    }

    setDependenciesLoading(false);
  };

  useEffect(() => {
    loadPurchases();
    loadDependencies();
  }, []);

  const filteredPurchases = useMemo(() => {
    const query = search.trim().toLowerCase();

    return purchases.filter((purchase) => {
      const matchesSearch =
        !query ||
        String(purchase.purchaseNumber || "")
          .toLowerCase()
          .includes(query) ||
        String(purchase.supplierName || "")
          .toLowerCase()
          .includes(query) ||
        String(purchase.id || "").includes(query);

      const matchesStatus =
        statusFilter === "ALL" || purchase.status === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [purchases, search, statusFilter]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredPurchases.length / PAGE_SIZE),
  );

  const currentPage = Math.min(page, totalPages);

  const visiblePurchases = filteredPurchases.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  const pendingPurchases = purchases.filter(
    (purchase) => purchase.status === "PENDING",
  );

  const receivedPurchases = purchases.filter(
    (purchase) => purchase.status === "RECEIVED",
  );

  const cancelledPurchases = purchases.filter(
    (purchase) => purchase.status === "CANCELLED",
  );

  const receivedValue = receivedPurchases.reduce(
    (total, purchase) => total + (Number(purchase.totalAmount) || 0),
    0,
  );

  const estimatedTotal = items.reduce(
    (total, item) =>
      total + (Number(item.quantity) || 0) * (Number(item.unitCost) || 0),
    0,
  );

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

  const resetForm = () => {
    setSupplierId("");
    setItems([{ ...EMPTY_ITEM }]);
    setFormError("");
  };

  const openCreateModal = () => {
    resetForm();
    setSuccess("");
    setShowCreateModal(true);
  };

  const closeCreateModal = () => {
    if (saving) return;

    setShowCreateModal(false);
    setFormError("");
  };

  const handleCreatePurchase = async (event) => {
    event.preventDefault();
    setFormError("");
    setSuccess("");

    if (!supplierId) {
      setFormError("Please select a supplier.");
      return;
    }

    if (items.some((item) => !item.productId)) {
      setFormError("Please select a product for every purchase item.");
      return;
    }

    if (
      items.some(
        (item) =>
          !Number.isInteger(Number(item.quantity)) ||
          Number(item.quantity) <= 0,
      )
    ) {
      setFormError("Each quantity must be a positive whole number.");
      return;
    }

    if (
      items.some(
        (item) =>
          item.unitCost === "" ||
          !Number.isFinite(Number(item.unitCost)) ||
          Number(item.unitCost) <= 0,
      )
    ) {
      setFormError("Each unit cost must be greater than zero.");
      return;
    }

    const productIds = items.map((item) => String(item.productId));

    if (new Set(productIds).size !== productIds.length) {
      setFormError(
        "Each product can only appear once. Change its quantity in the existing row.",
      );
      return;
    }

    const payload = {
      supplierId: Number(supplierId),
      items: items.map((item) => ({
        productId: Number(item.productId),
        quantity: Number(item.quantity),
        unitCost: Number(item.unitCost),
      })),
    };

    setSaving(true);

    try {
      const createdPurchase = await createPurchase(payload);

      await loadPurchases();

      setShowCreateModal(false);
      resetForm();

      setSuccess(
        `Purchase created successfully${
          createdPurchase?.purchaseNumber
            ? ` — ${createdPurchase.purchaseNumber}`
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

  const handlePurchaseAction = async (purchase, action) => {
    const isReceive = action === "receive";

    if (purchase.status !== "PENDING") return;

    const message = isReceive
      ? `Mark purchase "${purchase.purchaseNumber}" as received? This may increase inventory stock.`
      : `Cancel pending purchase "${purchase.purchaseNumber}"?`;

    if (!window.confirm(message)) return;

    setProcessingId(purchase.id);
    setError("");
    setSuccess("");

    try {
      const updatedPurchase = isReceive
        ? await receivePurchase(purchase.id)
        : await cancelPurchase(purchase.id);

      await loadPurchases();

      if (selectedPurchase?.id === purchase.id && updatedPurchase) {
        setSelectedPurchase(updatedPurchase);
      }

      setSuccess(
        isReceive
          ? `Purchase ${purchase.purchaseNumber} marked as received.`
          : `Purchase ${purchase.purchaseNumber} cancelled successfully.`,
      );
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setProcessingId(null);
    }
  };

  const getStatusClass = (status) => {
    switch (status) {
      case "PENDING":
        return "purchase-status-pending";
      case "RECEIVED":
        return "purchase-status-received";
      case "CANCELLED":
        return "purchase-status-cancelled";
      default:
        return "";
    }
  };

  return (
    <div className="purchases-page">
      <header className="purchases-page-header">
        <div>
          <div className="purchases-eyebrow">
            FINANCIAL MANAGEMENT / PURCHASES
          </div>
          <h1>Purchase Management</h1>
          <p>Manage supplier orders, track purchases, and receive inventory.</p>
        </div>

        <button
          type="button"
          className="purchase-primary-btn"
          onClick={openCreateModal}
        >
          <i className="bi bi-plus-lg" /> Create Purchase
        </button>
      </header>

      {error && (
        <div className="purchase-alert purchase-alert-error" role="alert">
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
        <div className="purchase-alert purchase-alert-success" role="status">
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

      <section className="purchases-stats-grid">
        <article className="purchase-stat-card">
          <div className="purchase-stat-top">
            <span>Received Purchase Value</span>
            <span className="purchase-stat-icon purchase-icon-blue">
              <i className="bi bi-currency-rupee" />
            </span>
          </div>
          <strong>{formatCurrency(receivedValue)}</strong>
          <small>Value of received purchases</small>
        </article>

        <article className="purchase-stat-card">
          <div className="purchase-stat-top">
            <span>Total Purchases</span>
            <span className="purchase-stat-icon purchase-icon-purple">
              <i className="bi bi-bag-check" />
            </span>
          </div>
          <strong>{purchases.length}</strong>
          <small>All recorded purchases</small>
        </article>

        <article className="purchase-stat-card">
          <div className="purchase-stat-top">
            <span>Pending Purchases</span>
            <span className="purchase-stat-icon purchase-icon-amber">
              <i className="bi bi-hourglass-split" />
            </span>
          </div>
          <strong>{pendingPurchases.length}</strong>
          <small>Awaiting receipt</small>
        </article>

        <article className="purchase-stat-card">
          <div className="purchase-stat-top">
            <span>Cancelled Purchases</span>
            <span className="purchase-stat-icon purchase-icon-red">
              <i className="bi bi-x-circle" />
            </span>
          </div>
          <strong>{cancelledPurchases.length}</strong>
          <small>Cancelled transactions</small>
        </article>
      </section>

      <section className="purchases-table-card">
        <div className="purchases-table-heading">
          <div>
            <h2>Purchase History</h2>
            <p>View purchase orders and manage their status.</p>
          </div>

          <button
            type="button"
            className="purchase-refresh-btn"
            onClick={() => {
              loadPurchases();
              loadDependencies();
            }}
            disabled={loading || dependenciesLoading}
          >
            <i className="bi bi-arrow-clockwise" /> Refresh
          </button>
        </div>

        <div className="purchases-toolbar">
          <label className="purchase-search-box">
            <i className="bi bi-search" />
            <input
              type="search"
              placeholder="Search purchase number or supplier..."
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(1);
              }}
            />
          </label>

          <select
            className="purchase-filter-select"
            value={statusFilter}
            onChange={(event) => {
              setStatusFilter(event.target.value);
              setPage(1);
            }}
            aria-label="Filter by purchase status"
          >
            <option value="ALL">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="RECEIVED">Received</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        <div className="purchases-table-wrapper">
          <table className="purchases-table">
            <thead>
              <tr>
                <th>Purchase Number</th>
                <th>Supplier</th>
                <th>Purchase Date</th>
                <th>Items</th>
                <th>Total Amount</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="purchase-table-message">
                    <span className="purchase-loader" />
                    Loading purchases...
                  </td>
                </tr>
              ) : visiblePurchases.length === 0 ? (
                <tr>
                  <td colSpan="7" className="purchase-table-message">
                    <div className="purchase-empty-state">
                      <i className="bi bi-bag-x" />
                      <strong>No purchases found</strong>
                      <span>
                        {purchases.length === 0
                          ? "Create your first purchase to see it here."
                          : "Try changing your search or status filter."}
                      </span>
                    </div>
                  </td>
                </tr>
              ) : (
                visiblePurchases.map((purchase) => (
                  <tr key={purchase.id}>
                    <td>
                      <button
                        type="button"
                        className="purchase-number-link"
                        onClick={() => setSelectedPurchase(purchase)}
                      >
                        {purchase.purchaseNumber || `PUR-${purchase.id}`}
                      </button>
                      <span className="purchase-subtext">
                        ID: {purchase.id}
                      </span>
                    </td>

                    <td className="purchase-supplier-name">
                      {purchase.supplierName || "—"}
                    </td>

                    <td>{formatDate(purchase.purchaseDate)}</td>

                    <td>
                      <span className="purchase-items-count">
                        {purchase.items?.length || 0}{" "}
                        {(purchase.items?.length || 0) === 1 ? "item" : "items"}
                      </span>
                    </td>

                    <td className="purchase-amount">
                      {formatCurrency(purchase.totalAmount)}
                    </td>

                    <td>
                      <span
                        className={`purchase-status-badge ${getStatusClass(
                          purchase.status,
                        )}`}
                      >
                        <span className="purchase-status-dot" />
                        {purchase.status}
                      </span>
                    </td>

                    <td>
                      <div className="purchase-row-actions">
                        <button
                          type="button"
                          className="purchase-icon-btn"
                          title="View details"
                          aria-label={`View purchase ${purchase.purchaseNumber}`}
                          onClick={() => setSelectedPurchase(purchase)}
                        >
                          <i className="bi bi-eye" />
                        </button>

                        {purchase.status === "PENDING" && (
                          <>
                            <button
                              type="button"
                              className="purchase-icon-btn purchase-receive-btn"
                              title="Receive purchase"
                              aria-label={`Receive ${purchase.purchaseNumber}`}
                              disabled={processingId === purchase.id}
                              onClick={() =>
                                handlePurchaseAction(purchase, "receive")
                              }
                            >
                              <i className="bi bi-box-seam" />
                            </button>

                            <button
                              type="button"
                              className="purchase-icon-btn purchase-cancel-btn"
                              title="Cancel purchase"
                              aria-label={`Cancel ${purchase.purchaseNumber}`}
                              disabled={processingId === purchase.id}
                              onClick={() =>
                                handlePurchaseAction(purchase, "cancel")
                              }
                            >
                              <i className="bi bi-x-circle" />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="purchases-pagination">
          <span>
            Showing{" "}
            {filteredPurchases.length === 0
              ? 0
              : (currentPage - 1) * PAGE_SIZE + 1}
            {"–"}
            {Math.min(
              currentPage * PAGE_SIZE,
              filteredPurchases.length,
            )} of {filteredPurchases.length} purchases
          </span>

          <div className="purchases-pagination-actions">
            <button
              type="button"
              disabled={currentPage <= 1}
              onClick={() => setPage((previous) => Math.max(1, previous - 1))}
            >
              <i className="bi bi-chevron-left" /> Previous
            </button>

            <span>
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
        <div className="purchase-modal-backdrop" onMouseDown={closeCreateModal}>
          <section
            className="purchase-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="purchase-create-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="purchase-modal-header">
              <div>
                <span className="purchase-modal-eyebrow">NEW ORDER</span>
                <h2 id="purchase-create-title">Create Purchase</h2>
                <p>Choose a supplier and add the products being purchased.</p>
              </div>

              <button
                type="button"
                className="purchase-modal-close"
                onClick={closeCreateModal}
                disabled={saving}
                aria-label="Close modal"
              >
                ×
              </button>
            </div>

            <form onSubmit={handleCreatePurchase}>
              <div className="purchase-modal-body">
                {formError && (
                  <div className="purchase-alert purchase-alert-error">
                    <i className="bi bi-exclamation-circle" />
                    <span>{formError}</span>
                  </div>
                )}

                {dependenciesLoading ? (
                  <div className="purchase-dependencies-loading">
                    <span className="purchase-loader" />
                    Loading products and suppliers...
                  </div>
                ) : (
                  <>
                    <div className="purchase-form-section-title">
                      <span>01</span>
                      <div>
                        <strong>Supplier information</strong>
                        <small>Select the supplier for this order</small>
                      </div>
                    </div>

                    <div className="purchase-form-field">
                      <label htmlFor="purchase-supplier">Supplier *</label>
                      <select
                        id="purchase-supplier"
                        value={supplierId}
                        onChange={(event) => setSupplierId(event.target.value)}
                        required
                      >
                        <option value="">Select a supplier</option>
                        {suppliers.map((supplier) => (
                          <option key={supplier.id} value={supplier.id}>
                            {supplier.name}
                          </option>
                        ))}
                      </select>

                      {suppliers.length === 0 && (
                        <small>
                          No suppliers found. Add a supplier before creating a
                          purchase.
                        </small>
                      )}
                    </div>

                    <div className="purchase-form-section-title purchase-items-section-title">
                      <span>02</span>
                      <div>
                        <strong>Purchase items</strong>
                        <small>Specify product, quantity, and unit cost</small>
                      </div>
                    </div>

                    {products.length === 0 ? (
                      <div className="purchase-product-empty">
                        No products found. Add products before creating a
                        purchase.
                      </div>
                    ) : (
                      <div className="purchase-sale-items">
                        {items.map((item, index) => {
                          const selectedProduct = products.find(
                            (product) =>
                              String(product.id) === String(item.productId),
                          );

                          return (
                            <div className="purchase-sale-item" key={index}>
                              <div className="purchase-product-field">
                                <label htmlFor={`purchase-product-${index}`}>
                                  Product
                                </label>

                                <select
                                  id={`purchase-product-${index}`}
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
                                      disabled={items.some(
                                        (otherItem, otherIndex) =>
                                          otherIndex !== index &&
                                          String(otherItem.productId) ===
                                            String(product.id),
                                      )}
                                    >
                                      {product.name}
                                      {product.sku ? ` (${product.sku})` : ""}
                                    </option>
                                  ))}
                                </select>

                                {selectedProduct && (
                                  <small>
                                    SKU: {selectedProduct.sku || "—"}
                                  </small>
                                )}
                              </div>

                              <div className="purchase-quantity-field">
                                <label htmlFor={`purchase-quantity-${index}`}>
                                  Quantity
                                </label>

                                <input
                                  id={`purchase-quantity-${index}`}
                                  type="number"
                                  min="1"
                                  step="1"
                                  value={item.quantity}
                                  onChange={(event) =>
                                    updateItem(
                                      index,
                                      "quantity",
                                      event.target.value,
                                    )
                                  }
                                  required
                                />
                              </div>

                              <div className="purchase-cost-field">
                                <label htmlFor={`purchase-cost-${index}`}>
                                  Unit Cost (₹)
                                </label>

                                <input
                                  id={`purchase-cost-${index}`}
                                  type="number"
                                  min="0.01"
                                  step="0.01"
                                  placeholder="0.00"
                                  value={item.unitCost}
                                  onChange={(event) =>
                                    updateItem(
                                      index,
                                      "unitCost",
                                      event.target.value,
                                    )
                                  }
                                  required
                                />
                              </div>

                              <div className="purchase-line-total">
                                <label>Subtotal</label>
                                <strong>
                                  {formatCurrency(
                                    (Number(item.quantity) || 0) *
                                      (Number(item.unitCost) || 0),
                                  )}
                                </strong>
                              </div>

                              <button
                                type="button"
                                className="purchase-remove-item"
                                onClick={() => removeItem(index)}
                                disabled={items.length === 1}
                                title="Remove item"
                                aria-label="Remove purchase item"
                              >
                                <i className="bi bi-trash3" />
                              </button>
                            </div>
                          );
                        })}

                        <button
                          type="button"
                          className="purchase-add-item-btn"
                          onClick={addItem}
                        >
                          <i className="bi bi-plus-circle" />
                          Add another product
                        </button>
                      </div>
                    )}

                    <div className="purchase-total-panel">
                      <div>
                        <span>Estimated purchase total</span>
                        <small>
                          Final total will be calculated by the backend.
                        </small>
                      </div>
                      <strong>{formatCurrency(estimatedTotal)}</strong>
                    </div>
                  </>
                )}
              </div>

              <div className="purchase-modal-footer">
                <button
                  type="button"
                  className="purchase-secondary-btn"
                  onClick={closeCreateModal}
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="purchase-primary-btn"
                  disabled={
                    saving ||
                    dependenciesLoading ||
                    suppliers.length === 0 ||
                    products.length === 0
                  }
                >
                  {saving ? (
                    <>
                      <span className="purchase-button-spinner" />
                      Creating...
                    </>
                  ) : (
                    <>
                      <i className="bi bi-check2-circle" />
                      Create Purchase
                    </>
                  )}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
      {selectedPurchase && (
        <div
          className="purchase-modal-backdrop"
          onMouseDown={() => setSelectedPurchase(null)}
        >
          <section
            className="purchase-modal purchase-details-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="purchase-details-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="purchase-modal-header">
              <div>
                <span className="purchase-modal-eyebrow">
                  PURCHASE INFORMATION
                </span>
                <h2 id="purchase-details-title">Purchase Details</h2>
                <p>{selectedPurchase.purchaseNumber || "Purchase"}</p>
              </div>

              <button
                type="button"
                className="purchase-modal-close"
                onClick={() => setSelectedPurchase(null)}
                aria-label="Close details"
              >
                ×
              </button>
            </div>

            <div className="purchase-modal-body">
              <div className="purchase-details-summary">
                <div>
                  <span>Purchase number</span>
                  <strong>{selectedPurchase.purchaseNumber || "—"}</strong>
                </div>

                <div>
                  <span>Supplier</span>
                  <strong>{selectedPurchase.supplierName || "—"}</strong>
                </div>

                <div>
                  <span>Purchase date</span>
                  <strong>{formatDate(selectedPurchase.purchaseDate)}</strong>
                </div>

                <div>
                  <span>Status</span>
                  <strong>{selectedPurchase.status || "—"}</strong>
                </div>
              </div>

              <h3 className="purchase-details-section-heading">
                Purchased products
              </h3>

              <div className="purchase-details-items">
                <div className="purchase-details-item purchase-details-item-header">
                  <span>Product</span>
                  <span>Qty</span>
                  <span>Unit cost</span>
                  <span>Subtotal</span>
                </div>

                {(selectedPurchase.items || []).map((item, index) => (
                  <div
                    className="purchase-details-item"
                    key={`${item.productId}-${index}`}
                  >
                    <div>
                      <strong>{item.productName || "Product"}</strong>
                      <small>{item.sku || "—"}</small>
                    </div>

                    <span>{item.quantity}</span>
                    <span>{formatCurrency(item.unitCost)}</span>
                    <strong>{formatCurrency(item.subtotal)}</strong>
                  </div>
                ))}

                {(!selectedPurchase.items ||
                  selectedPurchase.items.length === 0) && (
                  <p className="purchase-details-no-items">
                    No item details are available.
                  </p>
                )}
              </div>

              <div className="purchase-details-total">
                <span>Total amount</span>
                <strong>{formatCurrency(selectedPurchase.totalAmount)}</strong>
              </div>
            </div>

            <div className="purchase-modal-footer">
              <button
                type="button"
                className="purchase-secondary-btn"
                onClick={() => setSelectedPurchase(null)}
              >
                Close
              </button>

              {selectedPurchase.status === "PENDING" && (
                <>
                  <button
                    type="button"
                    className="purchase-danger-btn"
                    disabled={processingId === selectedPurchase.id}
                    onClick={() =>
                      handlePurchaseAction(selectedPurchase, "cancel")
                    }
                  >
                    <i className="bi bi-x-circle" />
                    Cancel Purchase
                  </button>

                  <button
                    type="button"
                    className="purchase-primary-btn"
                    disabled={processingId === selectedPurchase.id}
                    onClick={() =>
                      handlePurchaseAction(selectedPurchase, "receive")
                    }
                  >
                    <i className="bi bi-box-seam" />
                    Receive Purchase
                  </button>
                </>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

export default PurchasesPage;
