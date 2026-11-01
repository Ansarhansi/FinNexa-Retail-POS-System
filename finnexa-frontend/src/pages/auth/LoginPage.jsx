
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../../services/authService';
import './LoginPage.css';

function LoginPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();

    setError('');
    setLoading(true);

    try {
      await login(email.trim(), password);
      navigate('/', { replace: true });
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        setError('Invalid credentials or insufficient permissions.');
      } else {
        setError(
          err.response?.data?.message ||
          'Unable to connect to the server. Please try again.'
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-brand-panel">
        <div className="login-brand-content">
          <div className="login-brand-logo">
            <span className="login-logo-icon">
              <i className="bi bi-bar-chart-line-fill"></i>
            </span>
            <span>FinNexa</span>
          </div>

          <div className="login-brand-message">
            <span className="login-eyebrow">SMARTER BUSINESS MANAGEMENT</span>
            <h1>Your business.<br />One clear picture.</h1>
            <p>
              Manage sales, inventory, expenses and financial
              performance from one simple workspace.
            </p>

            <div className="login-feature-list">
              <div>
                <i className="bi bi-check-circle-fill"></i>
                Real-time business insights
              </div>
              <div>
                <i className="bi bi-check-circle-fill"></i>
                Inventory and sales management
              </div>
              <div>
                <i className="bi bi-check-circle-fill"></i>
                Centralized financial reporting
              </div>
            </div>
          </div>

          <div className="login-brand-footer">
            Retail POS &amp; Financial Management System
          </div>
        </div>
      </section>

      <section className="login-form-panel">
        <div className="login-form-container">
          <div className="login-mobile-logo">
            <span className="login-logo-icon">
              <i className="bi bi-bar-chart-line-fill"></i>
            </span>
            <span>FinNexa</span>
          </div>

          <div className="login-heading">
            <span className="login-eyebrow">WELCOME BACK</span>
            <h2>Sign in to your account</h2>
            <p>Enter your credentials to access your workspace.</p>
          </div>

          {error && (
            <div className="login-error" role="alert">
              <i className="bi bi-exclamation-circle-fill"></i>
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="login-field">
              <label htmlFor="email">Email address</label>
              <div className="login-input-wrapper">
                <i className="bi bi-envelope"></i>
                <input
                  id="email"
                  type="email"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  autoComplete="username"
                  required
                />
              </div>
            </div>

            <div className="login-field">
              <label htmlFor="password">Password</label>
              <div className="login-input-wrapper">
                <i className="bi bi-lock"></i>
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Enter your password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  autoComplete="current-password"
                  required
                />
                <button
                  className="password-toggle"
                  type="button"
                  onClick={() => setShowPassword((previous) => !previous)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  <i className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`}></i>
                </button>
              </div>
            </div>

            <button
              className="login-submit-button"
              type="submit"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2"></span>
                  Signing in...
                </>
              ) : (
                <>
                  Sign in
                  <i className="bi bi-arrow-right"></i>
                </>
              )}
            </button>
          </form>

          <div className="login-security-note">
            <i className="bi bi-shield-check"></i>
            <span>Your access is protected by secure authentication.</span>
          </div>

          <p className="login-copyright">
            © {new Date().getFullYear()} FinNexa. All rights reserved.
          </p>
        </div>
      </section>
    </main>
  );
}

export default LoginPage;

