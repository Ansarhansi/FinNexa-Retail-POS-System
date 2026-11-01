import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  logout,
  updateProfile,
  changePassword,
} from "../../services/authService";
import "./ProfilePage.css";

function ProfilePage() {
  const navigate = useNavigate();

  // User information
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(sessionStorage.getItem("finnexa_user") || "{}");
    } catch {
      return {};
    }
  });

  // Modal states
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);

  // Loading and notification states
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState(null);

  // Profile form
  const [profileForm, setProfileForm] = useState({
    fullName: user.fullName || "",
    email: user.email || "",
  });

  // Password form
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  // Password visibility
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false,
  });

  // Notification
  const showNotification = (type, message) => {
    setNotification({ type, message });

    setTimeout(() => {
      setNotification(null);
    }, 4000);
  };

  // Generate initials
  const getInitials = (name) =>
    (name || "U")
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join("");

  // Format role
  const formatRole = (role) =>
    (role || "USER")
      .replace("ROLE_", "")
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, (char) => char.toUpperCase());

  // Profile form changes
  const handleProfileChange = (event) => {
    const { name, value } = event.target;

    setProfileForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  // Password form changes
  const handlePasswordChange = (event) => {
    const { name, value } = event.target;

    setPasswordForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  // Update profile
  const handleProfileSubmit = async (event) => {
    event.preventDefault();

    if (!profileForm.fullName.trim() || !profileForm.email.trim()) {
      showNotification("error", "Please fill in all profile fields.");
      return;
    }

    setLoading(true);

    try {
      const updatedUser = await updateProfile({
        fullName: profileForm.fullName.trim(),
        email: profileForm.email.trim(),
      });

      setUser(updatedUser);

      setProfileForm({
        fullName: updatedUser.fullName || "",
        email: updatedUser.email || "",
      });

      setShowProfileModal(false);

      showNotification("success", "Profile updated successfully.");
    } catch (error) {
      showNotification(
        "error",
        error.response?.data?.message || "Unable to update profile.",
      );
    } finally {
      setLoading(false);
    }
  };

  // Change password
  const handlePasswordSubmit = async (event) => {
    event.preventDefault();

    if (!passwordForm.currentPassword) {
      showNotification("error", "Please enter your current password.");
      return;
    }

    if (!passwordForm.newPassword || !passwordForm.confirmPassword) {
      showNotification("error", "Please fill in all password fields.");
      return;
    }

    if (passwordForm.newPassword.length < 8) {
      showNotification("error", "Password must contain at least 8 characters.");
      return;
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      showNotification(
        "error",
        "New password and confirm password do not match.",
      );
      return;
    }

    if (passwordForm.currentPassword === passwordForm.newPassword) {
      showNotification(
        "error",
        "New password must be different from your current password.",
      );
      return;
    }

    setLoading(true);

    try {
      await changePassword({
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });

      setPasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });

      setShowPasswordModal(false);

      showNotification("success", "Password changed successfully.");
    } catch (error) {
      showNotification(
        "error",
        error.response?.data?.message ||
          "Unable to change password. Please check your current password.",
      );
    } finally {
      setLoading(false);
    }
  };

  // Logout
  const handleLogout = () => {
    if (!window.confirm("Are you sure you want to logout?")) {
      return;
    }

    logout();
    navigate("/login", { replace: true });
  };

  // Toggle password visibility
  const togglePassword = (field) => {
    setShowPasswords((previous) => ({
      ...previous,
      [field]: !previous[field],
    }));
  };

  return (
    <div className="profile-page">
      {/* Notification */}
      {notification && (
        <div className={`profile-notification ${notification.type}`}>
          <i
            className={`bi ${
              notification.type === "success"
                ? "bi-check-circle-fill"
                : "bi-exclamation-circle-fill"
            }`}
          />
          {notification.message}
        </div>
      )}

      {/* Page Header */}
      <div className="profile-page-header">
        <div>
          <div className="profile-breadcrumb">
            <span>FinNexa</span>
            <span>/</span>
            <span>My Profile</span>
          </div>

          <h1>My Profile</h1>

          <p>Manage your personal information and account security.</p>
        </div>
      </div>

      {/* Profile Overview */}
      <section className="profile-overview-card">
        <div className="profile-avatar">{getInitials(user.fullName)}</div>

        <div className="profile-overview-info">
          <h2>{user.fullName || "User"}</h2>

          <p>{user.email || "Email unavailable"}</p>

          <span className="profile-role-badge">
            <i className="bi bi-shield-check" />
            {formatRole(user.role)}
          </span>
        </div>

        <div className="profile-active-badge">
          <span />
          Logged In
        </div>
      </section>

      {/* Personal Information */}
      <section className="profile-details-card">
        <div className="profile-section-heading">
          <div>
            <h2>Personal Information</h2>
            <p>Your registered account information.</p>
          </div>

          <button
            type="button"
            className="profile-edit-button"
            onClick={() => {
              setProfileForm({
                fullName: user.fullName || "",
                email: user.email || "",
              });

              setShowProfileModal(true);
            }}
          >
            <i className="bi bi-pencil-square" />
            Edit Profile
          </button>
        </div>

        <div className="profile-details-grid">
          <div className="profile-detail-item">
            <label>Full Name</label>

            <div>
              <i className="bi bi-person" />
              <span>{user.fullName || "Not available"}</span>
            </div>
          </div>

          <div className="profile-detail-item">
            <label>Email Address</label>

            <div>
              <i className="bi bi-envelope" />
              <span>{user.email || "Not available"}</span>
            </div>
          </div>

          <div className="profile-detail-item">
            <label>User ID</label>

            <div>
              <i className="bi bi-hash" />
              <span>{user.userId ?? "Not available"}</span>
            </div>
          </div>

          <div className="profile-detail-item">
            <label>Account Role</label>

            <div>
              <i className="bi bi-shield-lock" />
              <span>{formatRole(user.role)}</span>
            </div>
          </div>
        </div>
      </section>

      {/* Account Security */}
      <section className="profile-security-card">
        <div className="profile-section-heading">
          <div>
            <h2>Account Security</h2>
            <p>Manage your password and active session.</p>
          </div>

          <i className="bi bi-shield-check" />
        </div>

        <div className="profile-security-content">
          <div className="profile-security-info">
            <span className="profile-security-icon">
              <i className="bi bi-shield-lock-fill" />
            </span>

            <div>
              <h3>Password & Security</h3>
              <p>Keep your account protected with a strong password.</p>
            </div>
          </div>

          <button
            type="button"
            className="profile-change-password-button"
            onClick={() => {
              setPasswordForm({
                currentPassword: "",
                newPassword: "",
                confirmPassword: "",
              });

              setShowPasswordModal(true);
            }}
          >
            Change Password
          </button>
        </div>

        {/* Logout */}
        <div className="profile-logout-section">
          <div>
            <h3>Sign Out</h3>
            <p>End your current session.</p>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="profile-logout-button"
          >
            <i className="bi bi-box-arrow-right" />
            Logout
          </button>
        </div>
      </section>

      {/* Edit Profile Modal */}
      {showProfileModal && (
        <div
          className="profile-modal-overlay"
          onClick={() => {
            if (!loading) setShowProfileModal(false);
          }}
        >
          <div
            className="profile-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="profile-modal-header">
              <div>
                <h2>Edit Profile</h2>
                <p>Update your personal information.</p>
              </div>

              <button
                type="button"
                disabled={loading}
                onClick={() => setShowProfileModal(false)}
              >
                <i className="bi bi-x-lg" />
              </button>
            </div>

            <form onSubmit={handleProfileSubmit}>
              <label>Full Name</label>

              <input
                name="fullName"
                type="text"
                maxLength="100"
                value={profileForm.fullName}
                onChange={handleProfileChange}
                placeholder="Enter your full name"
                required
              />

              <label>Email Address</label>

              <input
                name="email"
                type="email"
                maxLength="150"
                value={profileForm.email}
                onChange={handleProfileChange}
                placeholder="Enter your email address"
                required
              />

              <div className="profile-modal-actions">
                <button
                  type="button"
                  className="profile-cancel-button"
                  disabled={loading}
                  onClick={() => setShowProfileModal(false)}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="profile-save-button"
                  disabled={loading}
                >
                  {loading ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Change Password Modal */}
      {showPasswordModal && (
        <div
          className="profile-modal-overlay"
          onClick={() => {
            if (!loading) setShowPasswordModal(false);
          }}
        >
          <div
            className="profile-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="profile-modal-header">
              <div>
                <h2>Change Password</h2>
                <p>Choose a new password for your account.</p>
              </div>

              <button
                type="button"
                disabled={loading}
                onClick={() => setShowPasswordModal(false)}
              >
                <i className="bi bi-x-lg" />
              </button>
            </div>

            <form onSubmit={handlePasswordSubmit}>
              {[
                {
                  name: "currentPassword",
                  label: "Current Password",
                  key: "current",
                },
                {
                  name: "newPassword",
                  label: "New Password",
                  key: "new",
                },
                {
                  name: "confirmPassword",
                  label: "Confirm New Password",
                  key: "confirm",
                },
              ].map((field) => (
                <div className="profile-password-field" key={field.name}>
                  <label>{field.label}</label>

                  <div>
                    <input
                      type={showPasswords[field.key] ? "text" : "password"}
                      name={field.name}
                      value={passwordForm[field.name]}
                      onChange={handlePasswordChange}
                      required
                      autoComplete="off"
                      minLength={field.key === "current" ? undefined : 8}
                      maxLength="100"
                    />

                    <button
                      type="button"
                      onClick={() => togglePassword(field.key)}
                      aria-label={
                        showPasswords[field.key]
                          ? "Hide password"
                          : "Show password"
                      }
                    >
                      <i
                        className={`bi ${
                          showPasswords[field.key] ? "bi-eye-slash" : "bi-eye"
                        }`}
                      />
                    </button>
                  </div>
                </div>
              ))}

              <p className="profile-password-hint">
                New password must contain at least 8 characters.
              </p>

              <div className="profile-modal-actions">
                <button
                  type="button"
                  className="profile-cancel-button"
                  disabled={loading}
                  onClick={() => setShowPasswordModal(false)}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="profile-save-button"
                  disabled={loading}
                >
                  {loading ? "Updating..." : "Update Password"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="profile-footer">
        <i className="bi bi-info-circle" />

        <span>
          Your profile and password changes are saved through the FinNexa
          backend.
        </span>
      </div>
    </div>
  );
}

export default ProfilePage;
