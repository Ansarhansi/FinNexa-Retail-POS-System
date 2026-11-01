import { useEffect, useState } from "react";
import { getDashboard } from "../../services/dashboardService";
import { getSales } from "../../services/saleService";
import { getPurchases } from "../../services/purchaseService";
import SalesAnalytics from "../../components/dashboard/SalesAnalytics";
import PurchaseAnalytics from "../../components/dashboard/PurchaseAnalytics";
import "./DashboardPage.css";
import ExpenseAnalytics from "../../components/dashboard/ExpenseAnalytics";
import FinancialPerformance from "../../components/dashboard/FinancialPerformance";
const summaryCards = [
  {
    title: "Sales Revenue",
    field: "totalSalesRevenue",
    icon: "bi-currency-rupee",
    color: "blue",
    currency: true,
  },
  {
    title: "Total Expenses",
    field: "totalExpensesAmount",
    icon: "bi-wallet2",
    color: "orange",
    currency: true,
  },
  {
    title: "Total Products",
    field: "totalProducts",
    icon: "bi-box-seam",
    color: "purple",
  },
  {
    title: "Low Stock Products",
    field: "lowStockProducts",
    icon: "bi-exclamation-triangle",
    color: "green",
  },
];

function formatValue(value, currency = false) {
  if (value === null || value === undefined) {
    return currency ? "₹0.00" : "0";
  }

  if (currency) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 2,
    }).format(Number(value));
  }

  return new Intl.NumberFormat("en-IN").format(Number(value));
}

function formatDate(date) {
  if (!date) return "N/A";

  const parsedDate = new Date(date);

  if (Number.isNaN(parsedDate.getTime())) return "N/A";

  return parsedDate.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

function getStatusClass(status) {
  switch (status?.toUpperCase()) {
    case "COMPLETED":
    case "RECEIVED":
      return "completed";

    case "PENDING":
      return "pending";

    case "CANCELLED":
      return "cancelled";

    default:
      return "default";
  }
}

// Date range helper
function getDateRange(preset, customStart, customEnd) {
  const today = new Date();

  const start = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate(),
  );

  const end = new Date(start);

  const formatDate = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");

    return `${year}-${month}-${day}`;
  };

  switch (preset) {
    case "TODAY":
      break;

    case "LAST_7_DAYS":
      start.setDate(start.getDate() - 6);
      break;

    case "THIS_MONTH":
      start.setDate(1);
      break;

    case "LAST_MONTH":
      start.setDate(1);
      start.setMonth(start.getMonth() - 1);
      end.setDate(0);
      break;

    case "THIS_YEAR":
      start.setMonth(0, 1);
      break;

    case "CUSTOM":
      if (!customStart || !customEnd) return null;

      if (customStart > customEnd) return null;

      return {
        startDate: customStart,
        endDate: customEnd,
      };

    default:
      break;
  }

  return {
    startDate: formatDate(start),
    endDate: formatDate(end),
  };
}

function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [recentTransactions, setRecentTransactions] = useState([]);

  // Date filter states
  const [datePreset, setDatePreset] = useState("THIS_MONTH");
  const [customStartDate, setCustomStartDate] = useState("");
  const [customEndDate, setCustomEndDate] = useState("");

  useEffect(() => {
    let active = true;

    async function loadDashboard() {
      try {
        const [dashboardResult, salesResult, purchasesResult] =
          await Promise.allSettled([
            getDashboard(),
            getSales(),
            getPurchases(),
          ]);

        if (!active) return;

        if (dashboardResult.status === "fulfilled") {
          setDashboard(dashboardResult.value);
          setError("");
        } else {
          throw dashboardResult.reason;
        }

        const sales =
          salesResult.status === "fulfilled" && Array.isArray(salesResult.value)
            ? salesResult.value.map((sale) => ({
                id: `sale-${sale.id}`,
                reference: sale.invoiceNumber,
                type: "Sale",
                amount: sale.totalAmount,
                status: sale.status,
                date: sale.saleDate,
              }))
            : [];

        const purchases =
          purchasesResult.status === "fulfilled" &&
          Array.isArray(purchasesResult.value)
            ? purchasesResult.value.map((purchase) => ({
                id: `purchase-${purchase.id}`,
                reference: purchase.purchaseNumber,
                type: "Purchase",
                amount: purchase.totalAmount,
                status: purchase.status,
                date: purchase.purchaseDate,
              }))
            : [];

        const transactions = [...sales, ...purchases]
          .sort(
            (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime(),
          )
          .slice(0, 8);

        setRecentTransactions(transactions);
      } catch (err) {
        if (active) {
          setError(
            err.response?.status === 401 || err.response?.status === 403
              ? "Authentication is required to load dashboard data."
              : "Unable to load dashboard data. Check the backend and API configuration.",
          );
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadDashboard();

    return () => {
      active = false;
    };
  }, []);

  const dateRange = getDateRange(datePreset, customStartDate, customEndDate);

  return (
    <div className="dashboard-page">
      {/* PAGE HEADING */}

      <div className="page-heading">
        <div>
          <span className="eyebrow">OVERVIEW</span>
          <h2>Welcome to FinNexa</h2>
          <p>Here's an overview of your business.</p>
        </div>

        <span className="dashboard-badge">
          <i className="bi bi-bar-chart me-2" />
          Business Dashboard
        </span>
      </div>

      {/* LOADING */}

      {loading && (
        <div className="alert alert-info">Loading dashboard data...</div>
      )}

      {/* ERROR */}

      {!loading && error && (
        <div className="alert alert-warning" role="alert">
          {error}

          <button
            type="button"
            className="btn btn-sm btn-outline-dark ms-3"
            onClick={() => window.location.reload()}
          >
            Retry
          </button>
        </div>
      )}

      {/* SUMMARY CARDS */}

      <div className="summary-grid">
        {summaryCards.map((card) => (
          <article className="summary-card" key={card.field}>
            <div className={`summary-icon ${card.color}`}>
              <i className={`bi ${card.icon}`} />
            </div>

            <p className="summary-title">{card.title}</p>

            <h3>
              {loading
                ? "—"
                : formatValue(dashboard?.[card.field], card.currency)}
            </h3>
          </article>
        ))}
      </div>

      {/* BUSINESS STATISTICS */}

      <section className="business-stats">
        <h5>Business Statistics</h5>

        <div className="business-stats-grid">
          <div className="business-stat-item">
            <span>Total Customers</span>
            <strong>
              {loading ? "—" : formatValue(dashboard?.totalCustomers)}
            </strong>
          </div>

          <div className="business-stat-item">
            <span>Total Suppliers</span>
            <strong>
              {loading ? "—" : formatValue(dashboard?.totalSuppliers)}
            </strong>
          </div>

          <div className="business-stat-item">
            <span>Total Sales</span>
            <strong>
              {loading ? "—" : formatValue(dashboard?.totalSales)}
            </strong>
          </div>

          <div className="business-stat-item">
            <span>Total Purchases</span>
            <strong>
              {loading ? "—" : formatValue(dashboard?.totalPurchases)}
            </strong>
          </div>

          <div className="business-stat-item">
            <span>Total Expense Records</span>
            <strong>
              {loading ? "—" : formatValue(dashboard?.totalExpenses)}
            </strong>
          </div>

          <div className="business-stat-item">
            <span>Estimated Profit Before COGS</span>
            <strong>
              {loading
                ? "—"
                : formatValue(
                    dashboard?.estimatedProfitBeforeCostOfGoodsSold,
                    true,
                  )}
            </strong>
          </div>
        </div>
      </section>

      {/* BUSINESS PERFORMANCE */}

      <section className="dashboard-panel">
        <div>
          <span className="panel-icon">
            <i className="bi bi-graph-up-arrow" />
          </span>

          <div>
            <h5>Business Performance</h5>

            <p>
              {loading
                ? "Retrieving business metrics..."
                : error
                  ? "Business metrics could not be retrieved."
                  : `Your business currently has ${formatValue(
                      dashboard?.totalCustomers,
                    )} customers, ${formatValue(
                      dashboard?.totalSales,
                    )} sales and ${formatValue(
                      dashboard?.totalPurchases,
                    )} purchases.`}
            </p>
          </div>
        </div>

        {!loading && !error && (
          <span className="coming-soon-badge">
            <i className="bi bi-check-circle me-2" />
            Live API Data
          </span>
        )}
      </section>

      {/* RECENT TRANSACTIONS */}

      <section className="recent-transactions">
        <div className="transactions-heading">
          <div>
            <h5>Recent Transactions</h5>
            <p>Your latest sales and purchase activities.</p>
          </div>

          <span className="transactions-count">
            {recentTransactions.length} Transactions
          </span>
        </div>

        {loading ? (
          <div className="transactions-empty">Loading transactions...</div>
        ) : error ? (
          <div className="transactions-empty">
            Transactions could not be loaded.
          </div>
        ) : recentTransactions.length === 0 ? (
          <div className="transactions-empty">
            <i className="bi bi-receipt-cutoff"></i>
            <p>No transactions available yet.</p>
          </div>
        ) : (
          <div className="transactions-table-wrapper">
            <table className="transactions-table">
              <thead>
                <tr>
                  <th>REFERENCE</th>
                  <th>TYPE</th>
                  <th>DATE</th>
                  <th>AMOUNT</th>
                  <th>STATUS</th>
                </tr>
              </thead>

              <tbody>
                {recentTransactions.map((transaction) => (
                  <tr key={transaction.id}>
                    <td className="transaction-reference">
                      {transaction.reference || "N/A"}
                    </td>

                    <td>
                      <span
                        className={`transaction-type ${transaction.type.toLowerCase()}`}
                      >
                        <i
                          className={`bi ${
                            transaction.type === "Sale"
                              ? "bi-cart-check"
                              : "bi-box-seam"
                          }`}
                        />
                        {transaction.type}
                      </span>
                    </td>

                    <td>{formatDate(transaction.date)}</td>

                    <td className="transaction-amount">
                      {formatValue(transaction.amount, true)}
                    </td>

                    <td>
                      <span
                        className={`transaction-status ${getStatusClass(
                          transaction.status,
                        )}`}
                      >
                        {transaction.status || "N/A"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* ANALYTICS DATE FILTER */}

      <section className="analytics-filter-section">
        <div>
          <span className="analytics-eyebrow">REPORTING PERIOD</span>

          <h4>Analytics Overview</h4>

          <p>Filter sales and purchase performance by date.</p>
        </div>

        <div className="analytics-filter-controls">
          <select
            className="form-select"
            value={datePreset}
            onChange={(e) => setDatePreset(e.target.value)}
          >
            <option value="TODAY">Today</option>
            <option value="LAST_7_DAYS">Last 7 Days</option>
            <option value="THIS_MONTH">This Month</option>
            <option value="LAST_MONTH">Last Month</option>
            <option value="THIS_YEAR">This Year</option>
            <option value="CUSTOM">Custom Range</option>
          </select>

          {datePreset === "CUSTOM" && (
            <>
              <input
                type="date"
                className="form-control"
                value={customStartDate}
                max={customEndDate || undefined}
                onChange={(e) => setCustomStartDate(e.target.value)}
              />

              <input
                type="date"
                className="form-control"
                value={customEndDate}
                min={customStartDate || undefined}
                onChange={(e) => setCustomEndDate(e.target.value)}
              />
            </>
          )}
        </div>
      </section>

      {/* SALES ANALYTICS */}

      <SalesAnalytics dateRange={dateRange} />

      {/* PURCHASE ANALYTICS */}

      <PurchaseAnalytics dateRange={dateRange} />
      {/* EXPENSE ANALYTICS */}
      <ExpenseAnalytics dateRange={dateRange} />
      <FinancialPerformance dateRange={dateRange} />
    </div>
  );
}

export default DashboardPage;
