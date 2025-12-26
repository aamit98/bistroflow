import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  getSystemStatsApi,
  getAllHrManagersApi,
  getAllRestaurantsApi,
  getRestaurantBranchesApi,
  updateHrManagerRestaurantApi,
  createHrManagerApi,
  type SystemStats,
  type HrManager,
  type Restaurant,
  type BranchSummary,
} from "../api/AdminApi";
import {
  IconBuilding,
  IconBranch,
  IconManager,
  IconUsers,
  IconAlert,
  IconCheck,
  IconPlus,
  IconArrowRight,
  IconClose,
  IconChart,
} from "../components/Icons";
import "./admin/Admin.css";

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<SystemStats | null>(null);
  const [hrManagers, setHrManagers] = useState<HrManager[]>([]);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [branchesByRestaurant, setBranchesByRestaurant] = useState<Record<number, BranchSummary[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [expandedRestaurants, setExpandedRestaurants] = useState<Set<number>>(new Set());

  // Modal state for creating new HR manager
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newManager, setNewManager] = useState({
    id: "",
    name: "",
    password: "",
    restaurantId: "",
  });
  const [creating, setCreating] = useState(false);

  // State for assigning restaurant
  const [assigningManagerId, setAssigningManagerId] = useState<number | null>(null);
  const [selectedRestaurantId, setSelectedRestaurantId] = useState<number | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    try {
      setLoading(true);
      const [statsRes, managersRes, restaurantsRes] = await Promise.all([
        getSystemStatsApi(),
        getAllHrManagersApi(),
        getAllRestaurantsApi(),
      ]);
      setStats(statsRes.data);
      setHrManagers(managersRes.data);
      setRestaurants(restaurantsRes.data);

      // Load branches for all restaurants
      const branchesMap: Record<number, BranchSummary[]> = {};
      await Promise.all(
        restaurantsRes.data.map(async (restaurant: Restaurant) => {
          try {
            const branchRes = await getRestaurantBranchesApi(restaurant.id);
            branchesMap[restaurant.id] = branchRes.data;
          } catch (err) {
            console.error(`Failed to load branches for restaurant ${restaurant.id}`, err);
            branchesMap[restaurant.id] = [];
          }
        })
      );
      setBranchesByRestaurant(branchesMap);
    } catch (err) {
      setError("Failed to load dashboard data");
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  const toggleRestaurant = (restaurantId: number) => {
    setExpandedRestaurants(prev => {
      const next = new Set(prev);
      if (next.has(restaurantId)) {
        next.delete(restaurantId);
      } else {
        next.add(restaurantId);
      }
      return next;
    });
  };

  async function handleCreateManager(e: React.FormEvent) {
    e.preventDefault();
    if (!newManager.id || !newManager.name || !newManager.password) {
      setError("ID, Name, and Password are required");
      return;
    }

    try {
      setCreating(true);
      await createHrManagerApi({
        id: parseInt(newManager.id),
        name: newManager.name,
        password: newManager.password,
        restaurantId: newManager.restaurantId ? parseInt(newManager.restaurantId) : undefined,
      });
      setShowCreateModal(false);
      setNewManager({
        id: "",
        name: "",
        password: "",
        restaurantId: "",
      });
      await loadData();
    } catch (err) {
      setError("Failed to create HR manager");
      console.error(err);
    } finally {
      setCreating(false);
    }
  }

  async function handleAssignRestaurant(managerId: number) {
    if (selectedRestaurantId === null) return;

    try {
      await updateHrManagerRestaurantApi(managerId, selectedRestaurantId);
      setAssigningManagerId(null);
      setSelectedRestaurantId(null);
      await loadData();
    } catch (err) {
      setError("Failed to assign restaurant");
      console.error(err);
    }
  }

  function getRestaurantName(restaurantId: number | null | undefined): string {
    if (!restaurantId) return "Not Assigned";
    const restaurant = restaurants.find((r) => r.id === restaurantId);
    return restaurant?.name || "Unknown";
  }

  // Compute health metrics
  const unassignedManagers = hrManagers.filter((m) => !m.restaurantId).length;
  const restaurantsWithoutManager = restaurants.filter(
    (r) => !hrManagers.some((m) => m.restaurantId === r.id)
  ).length;
  const systemHealthy = unassignedManagers === 0 && restaurantsWithoutManager === 0;

  if (loading) {
    return (
      <div className="bf-loading-container">
        <div className="bf-spinner"></div>
        <p className="bf-loading-text">Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className="bf-page">
      {/* Header */}
      <div className="bf-page-header">
        <div>
          <h1 className="bf-page-title">System Overview</h1>
          <p className="bf-page-subtitle">Manage restaurants, branches, and HR personnel across your organization</p>
        </div>
        <div className="bf-header-actions">
          <button onClick={() => setShowCreateModal(true)} className="bf-btn bf-btn-primary">
            <IconPlus size={18} /> Add HR Manager
          </button>
        </div>
      </div>

      {error && (
        <div className="bf-alert bf-alert-error">
          <div className="bf-alert-content">
            <IconAlert size={20} />
            <span>{error}</span>
          </div>
          <button onClick={() => setError("")} className="bf-alert-close">
            <IconClose size={18} />
          </button>
        </div>
      )}

      {/* System Health Banner */}
      <div className={`bf-health-banner ${systemHealthy ? "bf-health-good" : "bf-health-warning"}`}>
        <div className="bf-health-content">
          <div className="bf-health-icon">
            {systemHealthy ? <IconCheck size={24} /> : <IconAlert size={24} />}
          </div>
          <div className="bf-health-text">
            <p className="bf-health-title">
              {systemHealthy ? "System Status: All Good" : "Action Required"}
            </p>
            <p className="bf-health-subtitle">
              {systemHealthy 
                ? "All HR managers are assigned and all restaurants have coverage"
                : `${unassignedManagers > 0 ? `${unassignedManagers} unassigned HR manager(s)` : ""} ${unassignedManagers > 0 && restaurantsWithoutManager > 0 ? "• " : ""}${restaurantsWithoutManager > 0 ? `${restaurantsWithoutManager} restaurant(s) need HR manager` : ""}`
              }
            </p>
          </div>
        </div>
        <Link to="/admin/restaurants" className="bf-btn bf-btn-outline">
          Manage Restaurant Chains <IconArrowRight size={16} />
        </Link>
      </div>

      {/* Stats Cards */}
      <div className="bf-stats-grid">
        <div className="bf-stat-card bf-stat-primary">
          <div className="bf-stat-icon">
            <IconBuilding size={28} />
          </div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats?.totalRestaurants ?? 0}</p>
            <p className="bf-stat-label">Restaurant Chains</p>
          </div>
          <Link to="/admin/restaurants" className="bf-stat-link">
            View all <IconArrowRight size={14} />
          </Link>
        </div>
        
        <div className="bf-stat-card bf-stat-secondary">
          <div className="bf-stat-icon">
            <IconBranch size={28} />
          </div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats?.totalBranches ?? 0}</p>
            <p className="bf-stat-label">Branches</p>
          </div>
          <Link to="/admin/branches" className="bf-stat-link">
            View all <IconArrowRight size={14} />
          </Link>
        </div>
        
        <div className="bf-stat-card bf-stat-accent">
          <div className="bf-stat-icon">
            <IconManager size={28} />
          </div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats?.totalHrManagers ?? 0}</p>
            <p className="bf-stat-label">HR Managers</p>
          </div>
          <button onClick={() => setShowCreateModal(true)} className="bf-stat-link">
            <IconPlus size={14} /> Add new
          </button>
        </div>
        
        <div className="bf-stat-card bf-stat-success">
          <div className="bf-stat-icon">
            <IconUsers size={28} />
          </div>
          <div className="bf-stat-content">
            <p className="bf-stat-value">{stats?.totalEmployees ?? 0}</p>
            <p className="bf-stat-label">Total Employees</p>
          </div>
          <span className="bf-stat-subtext">Across all branches</span>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bf-quick-actions">
        <Link to="/admin/restaurants" className="bf-action-card">
          <div className="bf-action-icon bf-action-primary">
            <IconBuilding size={24} />
          </div>
          <div className="bf-action-text">
            <h3>Restaurant Chains</h3>
            <p>Create and configure restaurant chains</p>
          </div>
          <IconArrowRight size={20} className="bf-action-arrow" />
        </Link>
        
        <Link to="/admin/branches" className="bf-action-card">
          <div className="bf-action-icon bf-action-secondary">
            <IconBranch size={24} />
          </div>
          <div className="bf-action-text">
            <h3>Manage Branches</h3>
            <p>Add branch locations to chains</p>
          </div>
          <IconArrowRight size={20} className="bf-action-arrow" />
        </Link>
        
        <button onClick={() => setShowCreateModal(true)} className="bf-action-card">
          <div className="bf-action-icon bf-action-accent">
            <IconPlus size={24} />
          </div>
          <div className="bf-action-text">
            <h3>Add HR Manager</h3>
            <p>Create new HR personnel account</p>
          </div>
          <IconArrowRight size={20} className="bf-action-arrow" />
        </button>
        
        <Link to="/admin/analytics" className="bf-action-card">
          <div className="bf-action-icon bf-action-success">
            <IconChart size={24} />
          </div>
          <div className="bf-action-text">
            <h3>View Analytics</h3>
            <p>System-wide performance metrics</p>
          </div>
          <IconArrowRight size={20} className="bf-action-arrow" />
        </Link>
      </div>

      {/* HR Managers Section */}
      <div className="bf-card bf-card-table">
        <div className="bf-card-header">
          <div className="bf-card-title-group">
            <h2 className="bf-card-title">HR Managers</h2>
            <span className="bf-badge bf-badge-info">{hrManagers.length} total</span>
          </div>
          <button
            onClick={() => setShowCreateModal(true)}
            className="bf-btn bf-btn-primary"
          >
            <IconPlus size={16} /> Add HR Manager
          </button>
        </div>

        <div className="bf-table-wrapper">
          <table className="bf-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Restaurant</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {hrManagers.map((manager) => (
                <tr key={manager.id}>
                  <td className="bf-table-mono">{manager.id}</td>
                  <td>
                    <div className="bf-table-user">
                      <div className="bf-avatar bf-avatar-accent">
                        {manager.name?.charAt(0).toUpperCase() || "?"}
                      </div>
                      <span className="bf-table-name">{manager.name}</span>
                    </div>
                  </td>
                  <td className="bf-table-muted">{manager.email || "-"}</td>
                  <td>
                    {assigningManagerId === manager.id ? (
                      <div className="bf-inline-edit">
                        <select
                          value={selectedRestaurantId ?? ""}
                          onChange={(e) => setSelectedRestaurantId(e.target.value ? parseInt(e.target.value) : null)}
                          className="bf-select bf-select-sm"
                        >
                          <option value="">Select Restaurant</option>
                          {restaurants.map((r) => (
                            <option key={r.id} value={r.id}>
                              {r.name}
                            </option>
                          ))}
                        </select>
                        <button
                          onClick={() => handleAssignRestaurant(manager.id)}
                          className="bf-btn bf-btn-icon bf-btn-success"
                        >
                          <IconCheck size={16} />
                        </button>
                        <button
                          onClick={() => {
                            setAssigningManagerId(null);
                            setSelectedRestaurantId(null);
                          }}
                          className="bf-btn bf-btn-icon bf-btn-ghost"
                        >
                          <IconClose size={16} />
                        </button>
                      </div>
                    ) : (
                      <span className={`bf-badge ${manager.restaurantId ? "bf-badge-success" : "bf-badge-warning"}`}>
                        <span className="bf-badge-dot"></span>
                        {getRestaurantName(manager.restaurantId)}
                      </span>
                    )}
                  </td>
                  <td>
                    <button
                      onClick={() => {
                        setAssigningManagerId(manager.id);
                        setSelectedRestaurantId(manager.restaurantId ?? null);
                      }}
                      className="bf-btn bf-btn-sm bf-btn-outline"
                    >
                      {manager.restaurantId ? "Change" : "Assign"}
                    </button>
                  </td>
                </tr>
              ))}
              {hrManagers.length === 0 && (
                <tr>
                  <td colSpan={5}>
                    <div className="bf-empty-state">
                      <div className="bf-empty-icon">
                        <IconManager size={48} />
                      </div>
                      <h3>No HR managers found</h3>
                      <p>Create your first HR manager to get started</p>
                      <button
                        onClick={() => setShowCreateModal(true)}
                        className="bf-btn bf-btn-primary"
                      >
                        <IconPlus size={16} /> Create HR Manager
                      </button>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Restaurants Overview */}
      <div className="bf-card bf-card-list">
        <div className="bf-card-header">
          <div className="bf-card-title-group">
            <h2 className="bf-card-title">Restaurant Chains & Branches</h2>
            <span className="bf-badge bf-badge-info">{restaurants.length} chains</span>
          </div>
          <Link to="/admin/restaurants" className="bf-btn bf-btn-outline">
            Manage Chains <IconArrowRight size={16} />
          </Link>
        </div>

        <div className="bf-accordion-list">
          {restaurants.map((restaurant) => {
            const manager = hrManagers.find((m) => m.restaurantId === restaurant.id);
            const branches = branchesByRestaurant[restaurant.id] || [];
            const isExpanded = expandedRestaurants.has(restaurant.id);
            
            return (
              <div key={restaurant.id} className="bf-accordion-item">
                {/* Restaurant Header */}
                <div 
                  className="bf-accordion-header"
                  onClick={() => toggleRestaurant(restaurant.id)}
                >
                  <div className="bf-accordion-left">
                    <div className="bf-accordion-icon bf-icon-primary">
                      <IconBuilding size={24} />
                    </div>
                    <div className="bf-accordion-info">
                      <div className="bf-accordion-title-row">
                        <h3>{restaurant.name}</h3>
                        <span className={`bf-badge bf-badge-sm ${restaurant.active ? "bf-badge-success" : "bf-badge-danger"}`}>
                          {restaurant.active ? "Active" : "Inactive"}
                        </span>
                      </div>
                      <div className="bf-accordion-meta">
                        <span><IconBranch size={14} /> {branches.length} branch{branches.length !== 1 ? "es" : ""}</span>
                        <span><IconManager size={14} /> {manager?.name || "No HR Manager"}</span>
                      </div>
                    </div>
                  </div>
                  <div className="bf-accordion-right">
                    {!manager && (
                      <span className="bf-badge bf-badge-warning bf-badge-sm">Needs HR Manager</span>
                    )}
                    <span className={`bf-accordion-chevron ${isExpanded ? "expanded" : ""}`}>▼</span>
                  </div>
                </div>

                {/* Branches List (Expandable) */}
                {isExpanded && (
                  <div className="bf-accordion-content">
                    {branches.length > 0 ? (
                      <div className="bf-branch-list">
                        {branches.map((branch) => (
                          <div key={branch.id} className="bf-branch-item">
                            <div className="bf-branch-left">
                              <IconBranch size={18} className="bf-branch-icon" />
                              <div>
                                <p className="bf-branch-name">{branch.name}</p>
                                <p className="bf-branch-meta">
                                  {branch.city} • {branch.employeeCount} employees
                                </p>
                              </div>
                            </div>
                            <span className={`bf-badge bf-badge-sm ${branch.active ? "bf-badge-success" : "bf-badge-danger"}`}>
                              {branch.active ? "Active" : "Inactive"}
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="bf-branch-empty">
                        <p>No branches yet</p>
                        <Link to="/admin/branches">+ Add branch</Link>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
          {restaurants.length === 0 && (
            <div className="bf-empty-state">
              <div className="bf-empty-icon">
                <IconBuilding size={48} />
              </div>
              <h3>No restaurant chains found</h3>
              <p>Create your first restaurant chain to get started</p>
              <Link to="/admin/restaurants" className="bf-btn bf-btn-primary">
                <IconPlus size={16} /> Create Restaurant Chain
              </Link>
            </div>
          )}
        </div>
      </div>

      {/* Create HR Manager Modal */}
      {showCreateModal && (
        <div className="bf-modal-overlay">
          <div className="bf-modal">
            <div className="bf-modal-header">
              <div className="bf-modal-title-group">
                <div className="bf-modal-icon">
                  <IconManager size={24} />
                </div>
                <div>
                  <h2>Add HR Manager</h2>
                  <p>Create a new HR manager account</p>
                </div>
              </div>
              <button className="bf-modal-close" onClick={() => setShowCreateModal(false)}>
                <IconClose size={20} />
              </button>
            </div>
            
            <form onSubmit={handleCreateManager}>
              <div className="bf-modal-body">
                <div className="bf-form-group">
                  <label className="bf-label">
                    Employee ID <span className="bf-required">*</span>
                  </label>
                  <input
                    type="number"
                    value={newManager.id}
                    onChange={(e) => setNewManager({ ...newManager, id: e.target.value })}
                    className="bf-input"
                    placeholder="Enter employee ID"
                    required
                  />
                </div>
                <div className="bf-form-group">
                  <label className="bf-label">
                    Full Name <span className="bf-required">*</span>
                  </label>
                  <input
                    type="text"
                    value={newManager.name}
                    onChange={(e) => setNewManager({ ...newManager, name: e.target.value })}
                    className="bf-input"
                    placeholder="Enter full name"
                    required
                  />
                </div>
                <div className="bf-form-group">
                  <label className="bf-label">
                    Password <span className="bf-required">*</span>
                  </label>
                  <input
                    type="password"
                    value={newManager.password}
                    onChange={(e) => setNewManager({ ...newManager, password: e.target.value })}
                    className="bf-input"
                    placeholder="Create a password"
                    required
                  />
                </div>
                <div className="bf-form-group">
                  <label className="bf-label">Assign to Restaurant</label>
                  <select
                    value={newManager.restaurantId}
                    onChange={(e) => setNewManager({ ...newManager, restaurantId: e.target.value })}
                    className="bf-select"
                  >
                    <option value="">-- Select Later --</option>
                    {restaurants.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.name}
                      </option>
                    ))}
                  </select>
                  <p className="bf-form-hint">Optional - can be assigned after creation</p>
                </div>
              </div>
              
              <div className="bf-modal-footer">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="bf-btn bf-btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="bf-btn bf-btn-primary"
                >
                  {creating ? (
                    <>
                      <span className="bf-spinner bf-spinner-sm"></span>
                      Creating...
                    </>
                  ) : (
                    "Create HR Manager"
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
