import { useEffect, useMemo, useState } from "react";

import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  LineChart,
  Line,
} from "recharts";

import { getSales } from "../../services/saleService";
import { getPurchases } from "../../services/purchaseService";
import { getExpenses } from "../../services/expenseService";

import "./Analytics.css";

function formatCurrency(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(Number(value) || 0);
}

function FinancialPerformance({ dateRange }) {
  const [sales, setSales] = useState([]);
  const [purchases, setPurchases] = useState([]);
  const [expenses, setExpenses] = useState([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Load financial data

  useEffect(() => {
    let active = true;

    async function loadFinancialData() {
      try {
        const [salesResult, purchasesResult, expensesResult] =
          await Promise.all([getSales(), getPurchases(), getExpenses()]);

        if (!active) return;

        setSales(Array.isArray(salesResult) ? salesResult : []);
        setPurchases(Array.isArray(purchasesResult) ? purchasesResult : []);
        setExpenses(Array.isArray(expensesResult) ? expensesResult : []);

        setError("");
      } catch (err) {
        if (active) {
          setError("Unable to load financial performance data.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadFinancialData();

    return () => {
      active = false;
    };
  }, []);

  // Filter sales

  const filteredSales = useMemo(() => {
    if (!dateRange) return [];

    return sales.filter((sale) => {
      if (sale.status !== "COMPLETED" || !sale.saleDate) {
        return false;
      }

      const date = sale.saleDate.substring(0, 10);

      return date >= dateRange.startDate && date <= dateRange.endDate;
    });
  }, [sales, dateRange]);

  // Filter received purchases

  const filteredPurchases = useMemo(() => {
    if (!dateRange) return [];

    return purchases.filter((purchase) => {
      if (purchase.status !== "RECEIVED" || !purchase.purchaseDate) {
        return false;
      }

      const date = purchase.purchaseDate.substring(0, 10);

      return date >= dateRange.startDate && date <= dateRange.endDate;
    });
  }, [purchases, dateRange]);

  // Filter expenses

  const filteredExpenses = useMemo(() => {
    if (!dateRange) return [];

    return expenses.filter((expense) => {
      if (!expense.expenseDate) return false;

      const date = expense.expenseDate.substring(0, 10);

      return date >= dateRange.startDate && date <= dateRange.endDate;
    });
  }, [expenses, dateRange]);

  // Financial calculations

  const totalRevenue = useMemo(
    () =>
      filteredSales.reduce(
        (total, sale) => total + Number(sale.totalAmount || 0),
        0,
      ),
    [filteredSales],
  );

  const totalPurchases = useMemo(
    () =>
      filteredPurchases.reduce(
        (total, purchase) => total + Number(purchase.totalAmount || 0),
        0,
      ),
    [filteredPurchases],
  );

  const totalExpenses = useMemo(
    () =>
      filteredExpenses.reduce(
        (total, expense) => total + Number(expense.amount || 0),
        0,
      ),
    [filteredExpenses],
  );

  const netCashMovement = totalRevenue - totalPurchases - totalExpenses;

  // Monthly financial performance

  const monthlyPerformance = useMemo(() => {
    const monthlyData = {};

    function addAmount(dateValue, key, amount) {
      if (!dateValue) return;

      const date = new Date(dateValue);

      if (Number.isNaN(date.getTime())) return;

      const monthKey = `${date.getFullYear()}-${String(
        date.getMonth() + 1,
      ).padStart(2, "0")}`;

      if (!monthlyData[monthKey]) {
        monthlyData[monthKey] = {
          month: date.toLocaleDateString("en-IN", {
            month: "short",
            year: "numeric",
          }),
          revenue: 0,
          purchases: 0,
          expenses: 0,
        };
      }

      monthlyData[monthKey][key] += Number(amount || 0);
    }

    filteredSales.forEach((sale) => {
      addAmount(sale.saleDate, "revenue", sale.totalAmount);
    });

    filteredPurchases.forEach((purchase) => {
      addAmount(purchase.purchaseDate, "purchases", purchase.totalAmount);
    });

    filteredExpenses.forEach((expense) => {
      addAmount(expense.expenseDate, "expenses", expense.amount);
    });

    return Object.entries(monthlyData)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([, value]) => ({
        ...value,
        revenue: Number(value.revenue.toFixed(2)),
        purchases: Number(value.purchases.toFixed(2)),
        expenses: Number(value.expenses.toFixed(2)),
      }));
  }, [filteredSales, filteredPurchases, filteredExpenses]);

  if (loading) {
    return (
      <div className="analytics-loading">Loading financial performance...</div>
    );
  }

  if (error) {
    return <div className="alert alert-warning">{error}</div>;
  }

  return (
    <section className="analytics-section financial-performance">
      {/* HEADING */}

      <div className="analytics-section-heading">
        <div>
          <span className="analytics-eyebrow">FINANCIAL OVERVIEW</span>

          <h3>Financial Performance</h3>

          <p>
            Consolidated overview of business revenue, purchases and expenses.
          </p>
        </div>

        <span className="analytics-live-badge">
          <i className="bi bi-activity me-2" />
          Live Data
        </span>
      </div>

      {/* FINANCIAL SUMMARY */}

      <div className="analytics-summary-grid">
        <article className="analytics-summary-card">
          <div className="analytics-summary-icon blue">
            <i className="bi bi-graph-up-arrow" />
          </div>

          <span>Total Sales Revenue</span>

          <h4>{formatCurrency(totalRevenue)}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon purple">
            <i className="bi bi-cart-check" />
          </div>

          <span>Received Purchases</span>

          <h4>{formatCurrency(totalPurchases)}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon orange">
            <i className="bi bi-wallet2" />
          </div>

          <span>Total Expenses</span>

          <h4>{formatCurrency(totalExpenses)}</h4>
        </article>

        <article className="analytics-summary-card">
          <div className="analytics-summary-icon green">
            <i className="bi bi-calculator" />
          </div>

          <span>Net Cash Movement</span>

          <h4 className={netCashMovement >= 0 ? "text-success" : "text-danger"}>
            {formatCurrency(netCashMovement)}
          </h4>
        </article>
      </div>

      {/* FINANCIAL CHARTS */}

      <div className="analytics-charts-grid">
        {/* REVENUE VS EXPENSES */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Revenue vs Purchases vs Expenses</h5>

              <p>Monthly comparison of financial activities.</p>
            </div>
          </div>

          {monthlyPerformance.length === 0 ? (
            <div className="analytics-empty">
              No financial data available for this period.
            </div>
          ) : (
            <div className="analytics-chart">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthlyPerformance}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />

                  <XAxis dataKey="month" />

                  <YAxis
                    tickFormatter={(value) =>
                      `₹${Number(value).toLocaleString("en-IN")}`
                    }
                  />

                  <Tooltip formatter={(value) => formatCurrency(value)} />

                  <Legend />

                  <Bar
                    dataKey="revenue"
                    name="Sales Revenue"
                    fill="#2563eb"
                    radius={[4, 4, 0, 0]}
                  />

                  <Bar
                    dataKey="purchases"
                    name="Received Purchases"
                    fill="#9333ea"
                    radius={[4, 4, 0, 0]}
                  />

                  <Bar
                    dataKey="expenses"
                    name="Expenses"
                    fill="#ea580c"
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </article>

        {/* FINANCIAL TREND */}

        <article className="analytics-chart-card">
          <div className="analytics-chart-heading">
            <div>
              <h5>Revenue Trend</h5>

              <p>Monthly sales revenue performance.</p>
            </div>
          </div>

          {monthlyPerformance.length === 0 ? (
            <div className="analytics-empty">No revenue trend available.</div>
          ) : (
            <div className="analytics-chart">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={monthlyPerformance}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />

                  <XAxis dataKey="month" />

                  <YAxis
                    tickFormatter={(value) =>
                      `₹${Number(value).toLocaleString("en-IN")}`
                    }
                  />

                  <Tooltip formatter={(value) => formatCurrency(value)} />

                  <Line
                    type="monotone"
                    dataKey="revenue"
                    name="Sales Revenue"
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
      </div>

      {/* FINANCIAL NOTE */}

      <div className="alert alert-info mt-3">
        <i className="bi bi-info-circle me-2" />
        Net Cash Movement = Completed Sales Revenue − Received Purchase Value −
        Recorded Expenses. This is an indicative operational measure, not
        accounting net profit.
      </div>
    </section>
  );
}

export default FinancialPerformance;
