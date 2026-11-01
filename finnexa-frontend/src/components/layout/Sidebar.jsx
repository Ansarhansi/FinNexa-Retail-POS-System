import { NavLink } from "react-router-dom";

const navigation = [
  { label: "Dashboard", path: "/", icon: "bi-grid-1x2" },
  { label: "Products", path: "/products", icon: "bi-box-seam" },
  { label: "Customers", path: "/customers", icon: "bi-people" },
  { label: "Suppliers", path: "/suppliers", icon: "bi-truck" },
  { label: "Sales", path: "/sales", icon: "bi-cart3" },
  { label: "Purchases", path: "/purchases", icon: "bi-bag-check" },
  { label: "Inventory", path: "/inventory", icon: "bi-boxes" },
  { label: "Expenses", path: "/expenses", icon: "bi-wallet2" },
  { label: "Reports", path: "/reports", icon: "bi-bar-chart" },
  { label: "Users", path: "/users", icon: "bi-person-gear" },
  {
    label: "Audit Logs",
    path: "/audit-logs",
    icon: "bi bi-journal-text",
  },
  {
    label: "My Profile",
    path: "/profile",
    icon: "bi bi-person-circle",
  },
];

function Sidebar({ open, onClose }) {
  return (
    <>
      {open && <div className="sidebar-backdrop" onClick={onClose} />}

      <aside className={`app-sidebar ${open ? "sidebar-open" : ""}`}>
        <div className="sidebar-brand">
          <div className="brand-logo">F</div>
          <div>
            <h5>FinNexa</h5>
            <span>Retail Management</span>
          </div>
        </div>

        <div className="sidebar-section-label">WORKSPACE</div>

        <nav className="sidebar-navigation">
          {navigation.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === "/"}
              onClick={onClose}
              className={({ isActive }) =>
                `sidebar-link ${isActive ? "active" : ""}`
              }
            >
              <i className={`bi ${item.icon}`} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-footer-icon">
            <i className="bi bi-shield-check" />
          </div>
          <div>
            <strong>FinNexa</strong>
            <span>Financial Management</span>
          </div>
        </div>
      </aside>
    </>
  );
}

export default Sidebar;
