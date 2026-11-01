import {
  BrowserRouter,
  Navigate,
  Outlet,
  Route,
  Routes,
} from "react-router-dom";
import ProfilePage from "./pages/profile/ProfilePage";
import AppLayout from "./components/layout/AppLayout";
import DashboardPage from "./pages/dashboard/DashboardPage";
import LoginPage from "./pages/auth/LoginPage";
import AuditLogsPage from "./pages/audit-logs/AuditLogsPage";
import ProductsPage from "./pages/products/ProductsPage";
import CustomersPage from "./pages/customers/CustomersPage";
import SuppliersPage from "./pages/suppliers/SuppliersPage";
import SalesPage from "./pages/Sales/SalesPage";
import PurchasesPage from "./pages/purchases/PurchasesPage";
import InventoryPage from "./pages/inventory/InventoryPage";
import ExpensesPage from "./pages/expenses/ExpensesPage";
import ReportsPage from "./pages/reports/ReportsPage";
import UsersPage from "./pages/users/UsersPage";

// Protect application routes
function ProtectedLayout() {
  const credentials = sessionStorage.getItem("finnexa_credentials");

  if (!credentials) {
    return <Navigate to="/login" replace />;
  }

  // Render nested routes instead of rendering AppLayout again
  return <Outlet />;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public route */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected application routes */}
        <Route element={<ProtectedLayout />}>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />

            <Route path="products" element={<ProductsPage />} />
            <Route path="customers" element={<CustomersPage />} />
            <Route path="suppliers" element={<SuppliersPage />} />
            <Route path="sales" element={<SalesPage />} />
            <Route path="purchases" element={<PurchasesPage />} />
            <Route path="inventory" element={<InventoryPage />} />
            <Route path="expenses" element={<ExpensesPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="/audit-logs" element={<AuditLogsPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Route>
        </Route>

        {/* Redirect unknown routes */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
