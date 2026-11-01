import { useEffect, useMemo, useState } from "react";

import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";

import { getSales } from "../../services/saleService";
import "./Analytics.css";

const PAYMENT_COLORS = ["#2563eb", "#16a34a", "#f59e0b", "#9333ea"];

function formatCurrency(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);
}

function SalesAnalytics({ dateRange }) {
  const [sales, setSales] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Load sales data

  useEffect(() => {
    let active = true;

    async function loadSales() {
      try {
        const data = await getSales();

        if (active) {
          setSales(Array.isArray(data) ? data : []);
          setError("");
        }
      } catch (err) {
        if (active) {
          setError("Unable to load sales analytics.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadSales();

    return () => {
      active = false;
    };
  }, []);

  // Completed sales

  const completedSales = useMemo(
    () => sales.filter((sale) => sale.status === "COMPLETED"),
    [sales],
  );

  // Filter sales by selected date range

  const filteredSales = useMemo(() => {
    if (!dateRange) return [];

    return completedSales.filter((sale) => {
      if (!sale.saleDate) return false;

      const saleDate = sale.saleDate.substring(0, 10);

      return saleDate >= dateRange.startDate && saleDate <= dateRange.endDate;
    });
  }, [completedSales, dateRange]);

  // Sales summary

  const totalRevenue = useMemo(
    () =>
      filteredSales.reduce(
        (total, sale) => total + Number(sale.totalAmount || 0),
        0,
      ),
    [filteredSales],
  );

  const totalTransactions = filteredSales.length;

  const averageTransaction =
    totalTransactions > 0 ? totalRevenue / totalTransactions : 0;

  // Monthly sales chart

  const monthlySales = useMemo(() => {
    const monthlyData = {};

    filteredSales.forEach((sale) => {
      if (!sale.saleDate) return;

      const date = new Date(sale.saleDate);

      if (Number.isNaN(date.getTime())) return;

      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(
        2,
        "0",
      )}`;

      if (!monthlyData[key]) {
        monthlyData[key] = {
          month: date.toLocaleDateString("en-IN", {
            month: "short",
            year: "numeric",
          }),
          revenue: 0,
        };
      }

      monthlyData[key].revenue += Number(sale.totalAmount || 0);
    });

    return Object.entries(monthlyData)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, value]) => ({
        ...value,
        revenue: Number(value.revenue.toFixed(2)),
      }));
  }, [filteredSales]);

  // Payment method distribution

  const paymentMethods = useMemo(() => {
    const methods = {};

    filteredSales.forEach((sale) => {
      const method = sale.paymentMethod || "UNKNOWN";

      methods[method] = (methods[method] || 0) + 1;
    });

    return Object.entries(methods).map(([name, value]) => ({
      name: name.replaceAll("_", " "),
      value,
    }));
  }, [filteredSales]);

  if (loading) {
    return <div className="analytics-loading">Loading sales analytics...</div>;
  }

  if (error) {
    return <div className="alert alert-warning">{error}</div>;
  }

  return (
    <section className="analytics-section">
      {/* HEADING */}

      <div className="analytics-section-heading">
        <div>
          <span className="analytics-eyebrow">REVENUE ANALYSIS</span>

          <h3>Sales Analytics</h3>

          <p>Monitor your sales performance and payment activity.</p>
        </div>

        <span className="analytics-live-badge">
          <i className="bi bi-activity me-2" />
          Live Data
        </span>
      </div>

      {/* SALES SUMMARY */}

      <div className="analytics-summary-grid">
        <article className="analytics-summary-card">
          <div className="analytics-summary-icon blue">
            <i className="bi bi-currency-rupee" />
          </div>

          <span>Total Sales Revenue</span>

          <h4>{formatCurrency(totalRevenue)}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon green">
            <i className="bi bi-receipt" />
          </div>

          <span>Completed Transactions</span>

          <h4>{totalTransactions}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon purple">
            <i className="bi bi-calculator" />
          </div>

          <span>Average Transaction</span>

          <h4>{formatCurrency(averageTransaction)}</h4>
        </article>
      </div>

      {/* SALES CHARTS */}

      <div className="analytics-charts-grid">
        {/* MONTHLY SALES */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Monthly Sales Revenue</h5>

              <p>Revenue generated from completed sales.</p>
            </div>
          </div>

          {monthlySales.length === 0 ? (
            <div className="analytics-empty">
              No completed sales data available for this period.
            </div>
          ) : (
            <div className="analytics-chart">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={monthlySales}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />

                  <XAxis dataKey="month" />

                  <YAxis
                    tickFormatter={(value) =>
                      `₹${Number(value).toLocaleString("en-IN")}`
                    }
                  />

                  <Tooltip
                    formatter={(value) => [formatCurrency(value), "Revenue"]}
                  />

                  <Line
                    type="monotone"
                    dataKey="revenue"
                    stroke="#2563eb"
                    strokeWidth={3}
                    dot={{ r: 4 }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </article>

        {/* PAYMENT METHODS */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Payment Methods</h5>

              <p>Distribution of completed transactions.</p>
            </div>
          </div>

          {paymentMethods.length === 0 ? (
            <div className="analytics-empty">
              No payment data available for this period.
            </div>
          ) : (
            <div className="analytics-chart pie-chart">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={paymentMethods}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="45%"
                    outerRadius={90}
                    paddingAngle={3}
                    label
                  >
                    {paymentMethods.map((entry, index) => (
                      <Cell
                        key={entry.name}
                        fill={PAYMENT_COLORS[index % PAYMENT_COLORS.length]}
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

export default SalesAnalytics;
