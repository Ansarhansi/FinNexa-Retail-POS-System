import { useCallback, useEffect, useMemo, useState } from "react";
import {
  getSalesReport,
  getPurchasesReport,
  getExpensesReport,
  getInventoryReport,
} from "../../services/reportService";
import { exportToCSV, exportToPDF } from "../../utils/exportUtils";
import "./ReportsPage.css";

const formatCurrency = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(Number(value) || 0);

const formatDate = (value) => {
  if (!value) return "—";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) return String(value);

  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

const getAmount = (item) => Number(item.totalAmount ?? item.amount ?? 0);

const getDate = (item, type) =>
  type === "sales"
    ? item.saleDate
    : type === "purchases"
      ? item.purchaseDate
      : type === "expenses"
        ? item.expenseDate
        : (item.updatedAt ?? item.createdAt);

const isWithinRange = (value, startDate, endDate) => {
  if (!startDate && !endDate) return true;
  if (!value) return false;

  const date = String(value).slice(0, 10);

  return (!startDate || date >= startDate) && (!endDate || date <= endDate);
};

export default function ReportsPage() {
  const [sales, setSales] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [expenses, setExpenses] = useState([]);
  const [inventory, setInventory] = useState([]);

  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [activeReport, setActiveReport] = useState("sales");

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  // Load reports
  const loadReports = useCallback(async (refresh = false) => {
    refresh ? setRefreshing(true) : setLoading(true);
    setError("");

    try {
      const results = await Promise.allSettled([
        getSalesReport(),
        getPurchasesReport(),
        getExpensesReport(),
        getInventoryReport(),
      ]);

      const labels = ["Sales", "Purchases", "Expenses", "Inventory"];

      const setters = [setSales, setPurchases, setExpenses, setInventory];

      const failures = [];

      results.forEach((result, index) => {
        if (result.status === "fulfilled") {
          setters[index](Array.isArray(result.value) ? result.value : []);
        } else {
          failures.push(labels[index]);
        }
      });

      if (failures.length === results.length) {
        throw new Error(
          "Unable to load reports. Please check your backend connection.",
        );
      }

      if (failures.length) {
        setError(
          `Some data could not be loaded: ${failures.join(
            ", ",
          )}. Other reports remain available.`,
        );
      }
    } catch (err) {
      setError(
        err.response?.data?.message || err.message || "Unable to load reports.",
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  // Filter sales
  const filteredSales = useMemo(
    () =>
      sales.filter(
        (item) =>
          isWithinRange(getDate(item, "sales"), startDate, endDate) &&
          String(item.status ?? "COMPLETED").toUpperCase() === "COMPLETED",
      ),
    [sales, startDate, endDate],
  );

  // Filter purchases
  const filteredPurchases = useMemo(
    () =>
      purchases.filter((item) =>
        isWithinRange(getDate(item, "purchases"), startDate, endDate),
      ),
    [purchases, startDate, endDate],
  );

  // Filter expenses
  const filteredExpenses = useMemo(
    () =>
      expenses.filter((item) =>
        isWithinRange(getDate(item, "expenses"), startDate, endDate),
      ),
    [expenses, startDate, endDate],
  );

  const filteredInventory = useMemo(() => inventory, [inventory]);

  // Financial totals
  const totalSales = filteredSales.reduce(
    (sum, item) => sum + getAmount(item),
    0,
  );

  const totalPurchases = filteredPurchases
    .filter((item) => String(item.status ?? "").toUpperCase() !== "CANCELLED")
    .reduce((sum, item) => sum + getAmount(item), 0);

  const totalExpenses = filteredExpenses.reduce(
    (sum, item) => sum + getAmount(item),
    0,
  );
  // Financial summary
  const totalOutflow = totalPurchases + totalExpenses;

  const netCashMovement = totalSales - totalOutflow;
  const inventoryValue = filteredInventory.reduce((sum, item) => {
    const quantity = Number(
      item.quantity ??
        item.stockQuantity ??
        item.currentStock ??
        item.availableQuantity ??
        0,
    );

    const price = Number(
      item.sellingPrice ??
        item.unitPrice ??
        item.price ??
        item.product?.sellingPrice ??
        item.product?.price ??
        0,
    );

    return sum + quantity * price;
  }, 0);

  // Report data
  const reportData = {
    sales: filteredSales,
    purchases: filteredPurchases,
    expenses: filteredExpenses,
    inventory: filteredInventory,
  };

  const currentRows = reportData[activeReport];

  const reportTitles = {
    sales: "Sales Report",
    purchases: "Purchase Report",
    expenses: "Expense Report",
    inventory: "Inventory Report",
  };

  // Prepare clean export data
  const getExportData = () => {
    switch (activeReport) {
      case "sales":
        return filteredSales.map((item) => ({
          invoiceNumber: item.invoiceNumber ?? item.id,
          date: formatDate(item.saleDate),
          paymentMethod: item.paymentMethod ?? "—",
          status: item.status ?? "COMPLETED",
          amount: getAmount(item),
        }));

      case "purchases":
        return filteredPurchases.map((item) => ({
          purchaseNumber: item.purchaseNumber ?? item.id,
          date: formatDate(item.purchaseDate),
          supplier: item.supplierName ?? "—",
          status: item.status ?? "—",
          amount: getAmount(item),
        }));

      case "expenses":
        return filteredExpenses.map((item) => ({
          title: item.title ?? "—",
          date: formatDate(item.expenseDate),
          category: item.category ?? "—",
          paymentMethod: item.paymentMethod ?? "—",
          amount: getAmount(item),
        }));

      case "inventory":
        return filteredInventory.map((item) => {
          const quantity = Number(
            item.quantity ??
              item.stockQuantity ??
              item.currentStock ??
              item.availableQuantity ??
              0,
          );

          const price = Number(
            item.sellingPrice ??
              item.unitPrice ??
              item.price ??
              item.product?.sellingPrice ??
              item.product?.price ??
              0,
          );

          return {
            product:
              item.productName ?? item.name ?? item.product?.name ?? "Unknown",
            sku: item.sku ?? item.productSku ?? item.product?.sku ?? "—",
            quantity,
            sellingPrice: price,
            estimatedValue: quantity * price,
          };
        });

      default:
        return [];
    }
  };

  // Export selected report as CSV
  const handleExportCSV = () => {
    const data = getExportData();

    exportToCSV(data, `finnexa-${activeReport}-report`);
  };

  // Export selected report as PDF
  const handleExportPDF = () => {
    const data = getExportData();

    exportToPDF(reportTitles[activeReport], data);
  };

  // Loading screen
  if (loading) {
    return (
      <div className="reports-page">
        <div className="reports-loading">
          <div className="reports-spinner" />
          <p>Preparing your reports...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="reports-page">
      {/* Header */}
      <header className="reports-header">
        <div>
          <div className="reports-breadcrumb">
            <span>FinNexa</span>
            <span>/</span>
            <span>Reports</span>
          </div>

          <h1>Financial Reports</h1>

          <p>Review sales, purchases, expenses, and inventory performance.</p>
        </div>

        <button
          type="button"
          className="reports-refresh-btn"
          onClick={() => loadReports(true)}
          disabled={refreshing}
        >
          <i
            className={`bi bi-arrow-clockwise ${
              refreshing ? "reports-rotating" : ""
            }`}
          />
          {refreshing ? "Refreshing..." : "Refresh"}
        </button>
      </header>

      {/* Error */}
      {error && (
        <div className="reports-alert" role="alert">
          <i className="bi bi-exclamation-circle" />
          <span>{error}</span>
        </div>
      )}

      {/* Date Filter */}
      <section className="reports-filter-card">
        <div>
          <h2>Report Period</h2>
          <p>Choose a date range to filter transaction reports.</p>
        </div>

        <div className="reports-date-filters">
          <label>
            From
            <input
              type="date"
              value={startDate}
              max={endDate || undefined}
              onChange={(event) => setStartDate(event.target.value)}
            />
          </label>

          <label>
            To
            <input
              type="date"
              value={endDate}
              min={startDate || undefined}
              onChange={(event) => setEndDate(event.target.value)}
            />
          </label>

          <button
            type="button"
            className="reports-clear-btn"
            onClick={() => {
              setStartDate("");
              setEndDate("");
            }}
          >
            Clear dates
          </button>
        </div>
      </section>

      {/* Statistics */}
      <section className="reports-stats-grid">
        <article className="reports-stat-card">
          <div className="reports-stat-heading">
            <span>Total Sales</span>
            <span className="reports-stat-icon sales-icon">
              <i className="bi bi-graph-up-arrow" />
            </span>
          </div>

          <h2>{formatCurrency(totalSales)}</h2>
          <p>{filteredSales.length} completed sales</p>
        </article>

        <article className="reports-stat-card">
          <div className="reports-stat-heading">
            <span>Total Purchases</span>
            <span className="reports-stat-icon purchases-icon">
              <i className="bi bi-cart3" />
            </span>
          </div>

          <h2>{formatCurrency(totalPurchases)}</h2>
          <p>{filteredPurchases.length} purchase records</p>
        </article>

        <article className="reports-stat-card">
          <div className="reports-stat-heading">
            <span>Total Expenses</span>
            <span className="reports-stat-icon expenses-icon">
              <i className="bi bi-wallet2" />
            </span>
          </div>

          <h2>{formatCurrency(totalExpenses)}</h2>
          <p>{filteredExpenses.length} expense records</p>
        </article>

        <article className="reports-stat-card">
          <div className="reports-stat-heading">
            <span>Inventory Value</span>
            <span className="reports-stat-icon inventory-icon">
              <i className="bi bi-box-seam" />
            </span>
          </div>

          <h2>{formatCurrency(inventoryValue)}</h2>
          <p>Estimated selling value</p>
        </article>
      </section>
      {/* Financial Summary */}

      <section className="financial-summary-section">
        {" "}
        <div className="financial-summary-heading">
          {" "}
          <div>
            {" "}
            <h2>Financial Summary</h2>{" "}
            <p>Overview of your business cash movement.</p>{" "}
          </div>{" "}
          <span className="financial-summary-badge">
            {" "}
            <i className="bi bi-calculator me-1"></i> Calculated Summary{" "}
          </span>{" "}
        </div>
        <div className="financial-summary-grid">
          {" "}
          <article className="financial-summary-card">
            {" "}
            <span>Total Revenue</span> <h3>{formatCurrency(totalSales)}</h3>{" "}
            <i className="bi bi-graph-up-arrow"></i>{" "}
          </article>
          <article className="financial-summary-card">
            <span>Purchase Outflow</span>
            <h3>{formatCurrency(totalPurchases)}</h3>
            <i className="bi bi-cart3"></i>
          </article>
          <article className="financial-summary-card">
            <span>Operating Expenses</span>
            <h3>{formatCurrency(totalExpenses)}</h3>
            <i className="bi bi-wallet2"></i>
          </article>
          <article className="financial-summary-card">
            <span>Total Outflow</span>
            <h3>{formatCurrency(totalOutflow)}</h3>
            <i className="bi bi-arrow-down-circle"></i>
          </article>
          <article className="financial-summary-card net-movement-card">
            <span>Net Cash Movement</span>
            <h3>{formatCurrency(netCashMovement)}</h3>
            <i className="bi bi-currency-rupee"></i>
            <small>
              {netCashMovement >= 0
                ? "Positive cash movement"
                : "Negative cash movement"}
            </small>
          </article>
        </div>{" "}
      </section>

      {/* Report Details */}
      <section className="reports-table-card">
        <div className="reports-table-heading">
          <div>
            <h2>Report Details</h2>
            <p>Choose a report to view or export its records.</p>
          </div>

          {/* CSV and PDF Export Buttons */}
          <div className="report-export-actions">
            <button
              type="button"
              className="btn btn-outline-success"
              onClick={handleExportCSV}
              disabled={currentRows.length === 0}
            >
              <i className="bi bi-filetype-csv me-2" />
              Export CSV
            </button>

            <button
              type="button"
              className="btn btn-outline-danger"
              onClick={handleExportPDF}
              disabled={currentRows.length === 0}
            >
              <i className="bi bi-file-earmark-pdf me-2" />
              Export PDF
            </button>
          </div>
        </div>

        {/* Report Tabs */}
        <div className="reports-tabs" role="tablist">
          {Object.entries(reportTitles).map(([key, title]) => (
            <button
              type="button"
              role="tab"
              aria-selected={activeReport === key}
              className={activeReport === key ? "active" : ""}
              key={key}
              onClick={() => setActiveReport(key)}
            >
              {title}
              <span>{reportData[key].length}</span>
            </button>
          ))}
        </div>

        {/* Report Tables */}
        <div className="reports-table-wrapper">
          <table className="reports-table">
            {/* Sales */}
            {activeReport === "sales" && (
              <>
                <thead>
                  <tr>
                    <th>INVOICE</th>
                    <th>DATE</th>
                    <th>PAYMENT</th>
                    <th>STATUS</th>
                    <th>AMOUNT</th>
                  </tr>
                </thead>

                <tbody>
                  {currentRows.map((item) => (
                    <tr key={item.id}>
                      <td>{item.invoiceNumber ?? `#${item.id}`}</td>
                      <td>{formatDate(item.saleDate)}</td>
                      <td>{item.paymentMethod ?? "—"}</td>
                      <td>
                        <span className="reports-status">
                          {item.status ?? "COMPLETED"}
                        </span>
                      </td>
                      <td>{formatCurrency(getAmount(item))}</td>
                    </tr>
                  ))}
                </tbody>
              </>
            )}

            {/* Purchases */}
            {activeReport === "purchases" && (
              <>
                <thead>
                  <tr>
                    <th>PURCHASE NUMBER</th>
                    <th>DATE</th>
                    <th>SUPPLIER</th>
                    <th>STATUS</th>
                    <th>AMOUNT</th>
                  </tr>
                </thead>

                <tbody>
                  {currentRows.map((item) => (
                    <tr key={item.id}>
                      <td>{item.purchaseNumber ?? `#${item.id}`}</td>
                      <td>{formatDate(item.purchaseDate)}</td>
                      <td>{item.supplierName ?? "—"}</td>
                      <td>
                        <span className="reports-status">
                          {item.status ?? "—"}
                        </span>
                      </td>
                      <td>{formatCurrency(getAmount(item))}</td>
                    </tr>
                  ))}
                </tbody>
              </>
            )}

            {/* Expenses */}
            {activeReport === "expenses" && (
              <>
                <thead>
                  <tr>
                    <th>TITLE</th>
                    <th>DATE</th>
                    <th>CATEGORY</th>
                    <th>PAYMENT</th>
                    <th>AMOUNT</th>
                  </tr>
                </thead>

                <tbody>
                  {currentRows.map((item) => (
                    <tr key={item.id}>
                      <td>{item.title ?? "—"}</td>
                      <td>{formatDate(item.expenseDate)}</td>
                      <td>{item.category ?? "—"}</td>
                      <td>{item.paymentMethod ?? "—"}</td>
                      <td>{formatCurrency(getAmount(item))}</td>
                    </tr>
                  ))}
                </tbody>
              </>
            )}

            {/* Inventory */}
            {activeReport === "inventory" && (
              <>
                <thead>
                  <tr>
                    <th>PRODUCT</th>
                    <th>SKU</th>
                    <th>QUANTITY</th>
                    <th>UNIT PRICE</th>
                    <th>ESTIMATED VALUE</th>
                  </tr>
                </thead>

                <tbody>
                  {currentRows.map((item, index) => {
                    const quantity = Number(
                      item.quantity ??
                        item.stockQuantity ??
                        item.currentStock ??
                        item.availableQuantity ??
                        0,
                    );

                    const price = Number(
                      item.sellingPrice ??
                        item.unitPrice ??
                        item.price ??
                        item.product?.sellingPrice ??
                        item.product?.price ??
                        0,
                    );

                    return (
                      <tr key={item.productId ?? item.id ?? index}>
                        <td>
                          {item.productName ??
                            item.name ??
                            item.product?.name ??
                            "Unknown"}
                        </td>

                        <td>
                          {item.sku ??
                            item.productSku ??
                            item.product?.sku ??
                            "—"}
                        </td>

                        <td>{quantity}</td>
                        <td>{formatCurrency(price)}</td>
                        <td>{formatCurrency(quantity * price)}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </>
            )}

            {/* Empty State */}
            {currentRows.length === 0 && (
              <tbody>
                <tr>
                  <td colSpan="5">
                    <div className="reports-empty">
                      <i className="bi bi-bar-chart" />
                      <h3>No records found</h3>
                      <p>
                        There are no records for this report and date range.
                      </p>
                    </div>
                  </td>
                </tr>
              </tbody>
            )}
          </table>
        </div>

        <div className="reports-table-footer">
          Displaying {currentRows.length} records in{" "}
          {reportTitles[activeReport]}.
        </div>
      </section>

      {/* Disclaimer */}
      <div className="reports-disclaimer">
        <i className="bi bi-info-circle" />
        <p>
          These reports summarize operational data. Inventory valuation is
          estimated from available selling prices. Purchase totals include
          non-cancelled purchases, including pending purchases. These figures
          should not be treated as audited financial statements or net profit.
        </p>
      </div>
    </div>
  );
}
