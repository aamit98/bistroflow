/**
 * BistroFlow UI Components
 * 
 * Reusable building blocks for consistent UI across the app.
 * Use these instead of raw HTML to maintain the design system.
 */
import React from 'react';
import './ui.css';

// ─────────────────────────────────────────────────────────────────────────────
// Loading States
// ─────────────────────────────────────────────────────────────────────────────

export const Spinner: React.FC<{ size?: 'sm' | 'md' | 'lg' }> = ({ size = 'md' }) => {
  const sizes = { sm: 20, md: 32, lg: 48 };
  return (
    <div 
      className="bf-spinner" 
      style={{ width: sizes[size], height: sizes[size] }}
    />
  );
};

export const PageLoader: React.FC<{ message?: string }> = ({ message = 'Loading...' }) => (
  <div className="bf-page-loader">
    <Spinner size="lg" />
    <p>{message}</p>
  </div>
);

export const Skeleton: React.FC<{ 
  width?: string | number; 
  height?: string | number;
  borderRadius?: string;
}> = ({ width = '100%', height = 16, borderRadius = '8px' }) => (
  <div 
    className="bf-skeleton" 
    style={{ width, height, borderRadius }}
  />
);

export const SkeletonCard: React.FC = () => (
  <div className="bf-card" style={{ padding: 24 }}>
    <Skeleton height={24} width="60%" />
    <div style={{ height: 12 }} />
    <Skeleton height={16} width="40%" />
    <div style={{ height: 20 }} />
    <Skeleton height={80} />
  </div>
);

// ─────────────────────────────────────────────────────────────────────────────
// Status Badges
// ─────────────────────────────────────────────────────────────────────────────

type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral';

export const Badge: React.FC<{
  variant?: BadgeVariant;
  children: React.ReactNode;
  icon?: string;
}> = ({ variant = 'neutral', children, icon }) => (
  <span className={`bf-badge bf-badge-${variant}`}>
    {icon && <span>{icon}</span>}
    {children}
  </span>
);

export const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const config: Record<string, { variant: BadgeVariant; icon: string; label: string }> = {
    active: { variant: 'success', icon: '●', label: 'Active' },
    inactive: { variant: 'neutral', icon: '○', label: 'Inactive' },
    pending: { variant: 'warning', icon: '◐', label: 'Pending' },
    approved: { variant: 'success', icon: '✓', label: 'Approved' },
    rejected: { variant: 'danger', icon: '✕', label: 'Rejected' },
    draft: { variant: 'info', icon: '◌', label: 'Draft' },
    published: { variant: 'success', icon: '✓', label: 'Published' },
  };
  
  const { variant, icon, label } = config[status.toLowerCase()] || config.pending;
  return <Badge variant={variant} icon={icon}>{label}</Badge>;
};

// ─────────────────────────────────────────────────────────────────────────────
// Empty States
// ─────────────────────────────────────────────────────────────────────────────

export const EmptyState: React.FC<{
  icon?: string;
  title: string;
  description?: string;
  action?: React.ReactNode;
}> = ({ icon = '📭', title, description, action }) => (
  <div className="bf-empty-state">
    <div className="bf-empty-icon">{icon}</div>
    <h3>{title}</h3>
    {description && <p>{description}</p>}
    {action}
  </div>
);

// ─────────────────────────────────────────────────────────────────────────────
// Avatar
// ─────────────────────────────────────────────────────────────────────────────

export const Avatar: React.FC<{
  name: string;
  size?: 'sm' | 'md' | 'lg';
  src?: string;
}> = ({ name, size = 'md', src }) => {
  const sizes = { sm: 32, md: 40, lg: 56 };
  const fontSizes = { sm: 12, md: 14, lg: 20 };
  const initials = name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
  
  return (
    <div 
      className="bf-avatar"
      style={{ 
        width: sizes[size], 
        height: sizes[size],
        fontSize: fontSizes[size]
      }}
    >
      {src ? (
        <img src={src} alt={name} />
      ) : (
        initials
      )}
    </div>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Stat Card
// ─────────────────────────────────────────────────────────────────────────────

export const StatCard: React.FC<{
  icon: string;
  label: string;
  value: string | number;
  change?: { value: string; direction: 'up' | 'down' };
  color?: string;
}> = ({ icon, label, value, change, color = 'var(--bf-primary)' }) => (
  <div className="bf-stat-card" style={{ '--stat-color': color } as React.CSSProperties}>
    <div className="bf-stat-icon" style={{ background: `${color}20` }}>{icon}</div>
    <p className="bf-stat-value">{value}</p>
    <p className="bf-stat-label">{label}</p>
    {change && (
      <div className={`bf-stat-change ${change.direction}`}>
        {change.direction === 'up' ? '↑' : '↓'} {change.value}
      </div>
    )}
  </div>
);

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip (simple CSS-only)
// ─────────────────────────────────────────────────────────────────────────────

export const Tooltip: React.FC<{
  text: string;
  children: React.ReactNode;
  position?: 'top' | 'bottom';
}> = ({ text, children, position = 'top' }) => (
  <div className="bf-tooltip-wrapper">
    {children}
    <span className={`bf-tooltip bf-tooltip-${position}`}>{text}</span>
  </div>
);

// ─────────────────────────────────────────────────────────────────────────────
// Animated Counter
// ─────────────────────────────────────────────────────────────────────────────

export const AnimatedNumber: React.FC<{ value: number; duration?: number }> = ({ 
  value, 
  duration = 500 
}) => {
  const [displayValue, setDisplayValue] = React.useState(0);
  
  React.useEffect(() => {
    const startTime = Date.now();
    const startValue = displayValue;
    
    const animate = () => {
      const elapsed = Date.now() - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3); // easeOutCubic
      
      setDisplayValue(Math.round(startValue + (value - startValue) * eased));
      
      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };
    
    requestAnimationFrame(animate);
  }, [value]);
  
  return <span className="bf-animated-number">{displayValue}</span>;
};

// ─────────────────────────────────────────────────────────────────────────────
// Page Header
// ─────────────────────────────────────────────────────────────────────────────

export const PageHeader: React.FC<{
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
  breadcrumb?: React.ReactNode;
}> = ({ title, subtitle, actions, breadcrumb }) => (
  <div className="bf-page-header">
    {breadcrumb && <div className="bf-breadcrumb">{breadcrumb}</div>}
    <div className="bf-page-header-row">
      <div>
        <h1 className="bf-page-title">{title}</h1>
        {subtitle && <p className="bf-page-subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="bf-page-actions">{actions}</div>}
    </div>
  </div>
);

// ─────────────────────────────────────────────────────────────────────────────
// Card
// ─────────────────────────────────────────────────────────────────────────────

export const Card: React.FC<{
  title?: string;
  badge?: string;
  actions?: React.ReactNode;
  footer?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}> = ({ title, badge, actions, footer, children, className = '' }) => (
  <div className={`bf-card ${className}`}>
    {(title || actions) && (
      <div className="bf-card-header">
        <div className="bf-card-title">
          {title && <h2>{title}</h2>}
          {badge && <span className="bf-card-badge">{badge}</span>}
        </div>
        {actions}
      </div>
    )}
    <div className="bf-card-body">{children}</div>
    {footer && <div className="bf-card-footer">{footer}</div>}
  </div>
);
