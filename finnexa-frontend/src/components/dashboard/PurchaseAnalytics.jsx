import { useEffect, useMemo, useState } from "react";

import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";

import { getPurchases } from "../../services/purchaseService";
import "./Analytics.css";

const STATUS_COLORS = {
  RECEIVED: "#16a34a",
  PENDING: "#f59e0b",
  CANCELLED: "#dc2626",
};

function formatCurrency(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);
}

function PurchaseAnalytics({ dateRange }) {
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Load purchases

  useEffect(() => {
    let active = true;

    async function loadPurchases() {
      try {
        const data = await getPurchases();

        if (active) {
          setPurchases(Array.isArray(data) ? data : []);
          setError("");
        }
      } catch (err) {
        if (active) {
          setError("Unable to load purchase analytics.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadPurchases();

    return () => {
      active = false;
    };
  }, []);

  // Filter purchases by selected date range

  const filteredPurchases = useMemo(() => {
    if (!dateRange) return [];

    return purchases.filter((purchase) => {
      if (!purchase.purchaseDate) return false;

      const purchaseDate = purchase.purchaseDate.substring(0, 10);

      return (
        purchaseDate >= dateRange.startDate && purchaseDate <= dateRange.endDate
      );
    });
  }, [purchases, dateRange]);

  // Total purchase value

  const totalPurchaseValue = useMemo(
    () =>
      filteredPurchases
        .filter((purchase) => purchase.status !== "CANCELLED")
        .reduce(
          (total, purchase) => total + Number(purchase.totalAmount || 0),
          0,
        ),
    [filteredPurchases],
  );

  // Purchase statuses

  const receivedPurchases = filteredPurchases.filter(
    (purchase) => purchase.status === "RECEIVED",
  ).length;

  const pendingPurchases = filteredPurchases.filter(
    (purchase) => purchase.status === "PENDING",
  ).length;

  // Monthly purchases chart

  const monthlyPurchases = useMemo(() => {
    const monthlyData = {};

    filteredPurchases
      .filter((purchase) => purchase.status !== "CANCELLED")
      .forEach((purchase) => {
        if (!purchase.purchaseDate) return;

        const date = new Date(purchase.purchaseDate);

        if (Number.isNaN(date.getTime())) return;

        const key = `${date.getFullYear()}-${String(
          date.getMonth() + 1,
        ).padStart(2, "0")}`;

        if (!monthlyData[key]) {
          monthlyData[key] = {
            month: date.toLocaleDateString("en-IN", {
              month: "short",
              year: "numeric",
            }),
            amount: 0,
          };
        }

        monthlyData[key].amount += Number(purchase.totalAmount || 0);
      });

    return Object.entries(monthlyData)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, value]) => ({
        ...value,
        amount: Number(value.amount.toFixed(2)),
      }));
  }, [filteredPurchases]);

  // Purchase status distribution

  const purchaseStatuses = useMemo(() => {
    const statuses = {};

    filteredPurchases.forEach((purchase) => {
      const status = purchase.status || "UNKNOWN";

      statuses[status] = (statuses[status] || 0) + 1;
    });

    return Object.entries(statuses).map(([name, value]) => ({
      name,
      value,
    }));
  }, [filteredPurchases]);

  if (loading) {
    return (
      <div className="analytics-loading">Loading purchase analytics...</div>
    );
  }

  if (error) {
    return <div className="alert alert-warning">{error}</div>;
  }

  return (
    <section className="analytics-section">
      {/* HEADING */}

      <div className="analytics-section-heading">
        <div>
          <span className="analytics-eyebrow">PROCUREMENT ANALYSIS</span>

          <h3>Purchase Analytics</h3>

          <p>Monitor purchase spending and supplier procurement activity.</p>
        </div>

        <span className="analytics-live-badge">
          <i className="bi bi-activity me-2" />
          Live Data
        </span>
      </div>

      {/* PURCHASE SUMMARY */}

      <div className="analytics-summary-grid">
        <article className="analytics-summary-card">
          <div className="analytics-summary-icon blue">
            <i className="bi bi-cart-check" />
          </div>

          <span>Total Purchase Value</span>

          <h4>{formatCurrency(totalPurchaseValue)}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon green">
            <i className="bi bi-check-circle" />
          </div>

          <span>Received Purchases</span>

          <h4>{receivedPurchases}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon purple">
            <i className="bi bi-hourglass-split" />
          </div>

          <span>Pending Purchases</span>

          <h4>{pendingPurchases}</h4>
        </article>
      </div>

      {/* PURCHASE CHARTS */}

      <div className="analytics-charts-grid">
        {/* MONTHLY PURCHASES */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Monthly Purchase Value</h5>

              <p>
                Purchase value grouped by month, excluding cancelled purchases.
              </p>
            </div>
          </div>

          {monthlyPurchases.length === 0 ? (
            <div className="analytics-empty">
              No purchase data available for this period.
            </div>
          ) : (
            <div className="analytics-chart">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthlyPurchases}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />

                  <XAxis dataKey="month" />

                  <YAxis
                    tickFormatter={(value) =>
                      `₹${Number(value).toLocaleString("en-IN")}`
                    }
                  />

                  <Tooltip
                    formatter={(value) => [
                      formatCurrency(value),
                      "Purchase Value",
                    ]}
                  />

                  <Bar dataKey="amount" fill="#7c3aed" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </article>

        {/* PURCHASE STATUS */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Purchase Status Distribution</h5>

              <p>Overview of purchase order statuses.</p>
            </div>
          </div>

          {purchaseStatuses.length === 0 ? (
            <div className="analytics-empty">
              No purchase status data available for this period.
            </div>
          ) : (
            <div className="analytics-chart pie-chart">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={purchaseStatuses}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="45%"
                    outerRadius={90}
                    paddingAngle={3}
                    label
                  >
                    {purchaseStatuses.map((entry) => (
                      <Cell
                        key={entry.name}
                        fill={STATUS_COLORS[entry.name] || "#94a3b8"}
                      />
                    ))}
                  </Pie>

                  <Tooltip />

                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </article>
      </div>
    </section>
  );
}

export default PurchaseAnalytics;
