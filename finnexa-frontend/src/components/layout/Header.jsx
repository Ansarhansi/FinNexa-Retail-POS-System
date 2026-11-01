import { useLocation } from "react-router-dom";

const pageTitles = {
  "/": "Dashboard",
  "/products": "Products",
  "/customers": "Customers",
  "/suppliers": "Suppliers",
  "/sales": "Sales",
  "/purchases": "Purchases",
  "/inventory": "Inventory",
  "/expenses": "Expenses",
  "/reports": "Financial Reports",
  "/users": "User Management",
};

function Header({ onMenuClick }) {
  const location = useLocation();
  const pageTitle = pageTitles[location.pathname] || "FinNexa";

  return (
    <header className="app-header">
      <div className="header-left">
        <button
          type="button"
          className="mobile-menu-button"
          onClick={onMenuClick}
          aria-label="Open navigation"
        >
          <i className="bi bi-list" />
        </button>

        <div>
          <h4>{pageTitle}</h4>
          <p>Manage your business efficiently</p>
        </div>
      </div>

      <div className="header-right">
        <div className="header-status">
          <span className="status-dot" />
          System Online
        </div>

        <div className="header-divider" />

        <div className="user-profile">
          <div className="user-avatar">A</div>
          <div className="user-info">
            <strong>Administrator</strong>
            <span>ADMIN</span>
          </div>
        </div>
      </div>
    </header>
  );
}

export default Header;
