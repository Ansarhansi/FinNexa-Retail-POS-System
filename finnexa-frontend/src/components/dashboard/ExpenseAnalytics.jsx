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

import { getExpenses } from "../../services/expenseService";
import "./Analytics.css";

const CHART_COLORS = [
  "#2563eb",
  "#16a34a",
  "#f59e0b",
  "#9333ea",
  "#ec4899",
  "#0891b2",
  "#dc2626",
  "#64748b",
];

function formatCurrency(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);
}

function ExpenseAnalytics({ dateRange }) {
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function loadExpenses() {
      try {
        const data = await getExpenses();

        if (active) {
          setExpenses(Array.isArray(data) ? data : []);
          setError("");
        }
      } catch (err) {
        if (active) {
          setError("Unable to load expense analytics.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadExpenses();

    return () => {
      active = false;
    };
  }, []);

  // Filter expenses by selected date range

  const filteredExpenses = useMemo(() => {
    if (!dateRange) return [];

    return expenses.filter((expense) => {
      if (!expense.expenseDate) return false;

      const expenseDate = expense.expenseDate.substring(0, 10);

      return (
        expenseDate >= dateRange.startDate && expenseDate <= dateRange.endDate
      );
    });
  }, [expenses, dateRange]);

  // Expense summary

  const totalExpenses = useMemo(
    () =>
      filteredExpenses.reduce(
        (total, expense) => total + Number(expense.amount || 0),
        0,
      ),
    [filteredExpenses],
  );

  const expenseCount = filteredExpenses.length;

  const averageExpense = expenseCount > 0 ? totalExpenses / expenseCount : 0;

  // Monthly expense chart

  const monthlyExpenses = useMemo(() => {
    const monthlyData = {};

    filteredExpenses.forEach((expense) => {
      if (!expense.expenseDate) return;

      const date = new Date(expense.expenseDate);

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
          amount: 0,
        };
      }

      monthlyData[key].amount += Number(expense.amount || 0);
    });

    return Object.entries(monthlyData)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, value]) => ({
        ...value,
        amount: Number(value.amount.toFixed(2)),
      }));
  }, [filteredExpenses]);

  // Expense category distribution

  const categoryData = useMemo(() => {
    const categories = {};

    filteredExpenses.forEach((expense) => {
      const category = expense.category || "UNKNOWN";

      categories[category] =
        (categories[category] || 0) + Number(expense.amount || 0);
    });

    return Object.entries(categories).map(([name, value]) => ({
      name: name.replaceAll("_", " "),
      value,
    }));
  }, [filteredExpenses]);

  // Payment method distribution

  const paymentData = useMemo(() => {
    const methods = {};

    filteredExpenses.forEach((expense) => {
      const method = expense.paymentMethod || "UNKNOWN";

      methods[method] = (methods[method] || 0) + Number(expense.amount || 0);
    });

    return Object.entries(methods).map(([name, value]) => ({
      name: name.replaceAll("_", " "),
      value,
    }));
  }, [filteredExpenses]);

  if (loading) {
    return (
      <div className="analytics-loading">Loading expense analytics...</div>
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
          <span className="analytics-eyebrow">EXPENSE ANALYSIS</span>

          <h3>Expense Analytics</h3>

          <p>Monitor business expenses and spending patterns.</p>
        </div>

        <span className="analytics-live-badge">
          <i className="bi bi-activity me-2" />
          Live Data
        </span>
      </div>

      {/* EXPENSE SUMMARY */}

      <div className="analytics-summary-grid">
        <article className="analytics-summary-card">
          <div className="analytics-summary-icon blue">
            <i className="bi bi-wallet2" />
          </div>

          <span>Total Expenses</span>

          <h4>{formatCurrency(totalExpenses)}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon green">
            <i className="bi bi-receipt" />
          </div>

          <span>Expense Records</span>

          <h4>{expenseCount}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon purple">
            <i className="bi bi-calculator" />
          </div>

          <span>Average Expense</span>

          <h4>{formatCurrency(averageExpense)}</h4>
        </article>
      </div>

      {/* EXPENSE CHARTS */}

      <div className="analytics-charts-grid">
        {/* MONTHLY EXPENSES */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Monthly Expenses</h5>

              <p>Business expenses grouped by month.</p>
            </div>
          </div>

          {monthlyExpenses.length === 0 ? (
            <div className="analytics-empty">
              No expense data available for this period.
            </div>
          ) : (
            <div className="analytics-chart">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthlyExpenses}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />

                  <XAxis dataKey="month" />

                  <YAxis
                    tickFormatter={(value) =>
                      `₹${Number(value).toLocaleString("en-IN")}`
                    }
                  />

                  <Tooltip
                    formatter={(value) => [formatCurrency(value), "Expenses"]}
                  />

                  <Bar dataKey="amount" fill="#ea580c" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </article>

        {/* CATEGORY DISTRIBUTION */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Expense Categories</h5>

              <p>Spending distribution by category.</p>
            </div>
          </div>

          {categoryData.length === 0 ? (
            <div className="analytics-empty">
              No expense categories available.
            </div>
          ) : (
            <div className="analytics-chart pie-chart">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="45%"
                    outerRadius={90}
                    paddingAngle={3}
                    label
                  >
                    {categoryData.map((entry, index) => (
                      <Cell
                        key={entry.name}
                        fill={CHART_COLORS[index % CHART_COLORS.length]}
                      />
                    ))}
                  </Pie>

                  <Tooltip formatter={(value) => formatCurrency(value)} />

                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </article>

        {/* PAYMENT METHODS */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Expense Payment Methods</h5>

              <p>Spending grouped by payment method.</p>
            </div>
          </div>

          {paymentData.length === 0 ? (
            <div className="analytics-empty">No payment data available.</div>
          ) : (
            <div className="analytics-chart pie-chart">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={paymentData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="45%"
                    outerRadius={90}
                    paddingAngle={3}
                    label
                  >
                    {paymentData.map((entry, index) => (
                      <Cell
                        key={entry.name}
                        fill={CHART_COLORS[index % CHART_COLORS.length]}
                      />
                    ))}
                  </Pie>

                  <Tooltip formatter={(value) => formatCurrency(value)} />

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

export default ExpenseAnalytics;
