// Outline-style SVG Icons for BistroFlow
// Based on the brand design system - rounded endpoints, line-based icons

import React from 'react'

interface IconProps {
  size?: number
  color?: string
  className?: string
  strokeWidth?: number
}

const defaultProps: IconProps = {
  size: 24,
  color: 'currentColor',
  strokeWidth: 1.5
}

// Dashboard / Analytics Icon
export const IconDashboard: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M3 3v18h18" />
    <path d="M7 16l4-4 4 4 5-6" />
  </svg>
)

// Users / Employees Icon
export const IconUsers: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="9" cy="7" r="4" />
    <path d="M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2" />
    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    <path d="M21 21v-2a4 4 0 0 0-3-3.85" />
  </svg>
)

// Calendar / Schedule Icon
export const IconCalendar: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <rect x="3" y="4" width="18" height="18" rx="2" />
    <line x1="16" y1="2" x2="16" y2="6" />
    <line x1="8" y1="2" x2="8" y2="6" />
    <line x1="3" y1="10" x2="21" y2="10" />
    <path d="M9 16l2 2 4-4" />
  </svg>
)

// Time-Off / Vacation Icon
export const IconVacation: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    {/* Suitcase */}
    <rect x="3" y="8" width="10" height="12" rx="2" />
    <path d="M6 8V6a2 2 0 0 1 2-2h0a2 2 0 0 1 2 2v2" />
    <line x1="8" y1="12" x2="8" y2="16" />
    {/* Palm tree */}
    <path d="M18 20v-8" />
    <path d="M15 12c0-2 1.5-3 3-3s3 1 3 3" />
    <path d="M14 10c1-1.5 2.5-2 4-2" />
    <path d="M22 10c-1-1.5-2.5-2-4-2" />
  </svg>
)

// Inventory / Box Icon
export const IconInventory: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    {/* 3D boxes */}
    <path d="M12.5 3L3 8l9.5 5L22 8l-9.5-5z" />
    <path d="M3 8v8l9.5 5V13" />
    <path d="M22 8v8l-9.5 5V13" />
    <line x1="12.5" y1="13" x2="12.5" y2="21" />
  </svg>
)

// Settings / Gear Icon
export const IconSettings: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="12" cy="12" r="3" />
    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
  </svg>
)

// Building / Restaurant Icon
export const IconBuilding: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <rect x="4" y="2" width="16" height="20" rx="2" />
    <path d="M9 22v-4h6v4" />
    <line x1="8" y1="6" x2="8" y2="6.01" />
    <line x1="12" y1="6" x2="12" y2="6.01" />
    <line x1="16" y1="6" x2="16" y2="6.01" />
    <line x1="8" y1="10" x2="8" y2="10.01" />
    <line x1="12" y1="10" x2="12" y2="10.01" />
    <line x1="16" y1="10" x2="16" y2="10.01" />
    <line x1="8" y1="14" x2="8" y2="14.01" />
    <line x1="12" y1="14" x2="12" y2="14.01" />
    <line x1="16" y1="14" x2="16" y2="14.01" />
  </svg>
)

// Command Center / Lightning Icon
export const IconCommand: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
  </svg>
)

// HR Manager / Tie Icon
export const IconManager: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="12" cy="7" r="4" />
    <path d="M5 21v-2a7 7 0 0 1 14 0v2" />
    <path d="M12 11l-1.5 3 1.5 7 1.5-7-1.5-3z" />
  </svg>
)

// Clock / Time Icon
export const IconClock: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="12" cy="12" r="10" />
    <polyline points="12 6 12 12 16 14" />
  </svg>
)

// User Profile Icon
export const IconUser: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="12" cy="8" r="5" />
    <path d="M20 21a8 8 0 0 0-16 0" />
  </svg>
)

// Document / Request Icon
export const IconDocument: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
    <polyline points="14 2 14 8 20 8" />
    <line x1="16" y1="13" x2="8" y2="13" />
    <line x1="16" y1="17" x2="8" y2="17" />
    <line x1="10" y1="9" x2="8" y2="9" />
  </svg>
)

// Notification Bell Icon
export const IconBell: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
  </svg>
)

// Activity Logs Icon
export const IconActivity: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
  </svg>
)

// Branch / Store Icon
export const IconBranch: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    <polyline points="9 22 9 12 15 12 15 22" />
  </svg>
)

// Logout Icon
export const IconLogout: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
    <polyline points="16 17 21 12 16 7" />
    <line x1="21" y1="12" x2="9" y2="12" />
  </svg>
)

// Plus Icon
export const IconPlus: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <line x1="12" y1="5" x2="12" y2="19" />
    <line x1="5" y1="12" x2="19" y2="12" />
  </svg>
)

// Check Icon
export const IconCheck: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polyline points="20 6 9 17 4 12" />
  </svg>
)

// X / Close Icon
export const IconClose: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <line x1="18" y1="6" x2="6" y2="18" />
    <line x1="6" y1="6" x2="18" y2="18" />
  </svg>
)

// Alert / Warning Icon
export const IconAlert: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
    <line x1="12" y1="9" x2="12" y2="13" />
    <line x1="12" y1="17" x2="12.01" y2="17" />
  </svg>
)

// Chart / Analytics Icon  
export const IconChart: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <line x1="18" y1="20" x2="18" y2="10" />
    <line x1="12" y1="20" x2="12" y2="4" />
    <line x1="6" y1="20" x2="6" y2="14" />
  </svg>
)

// Arrow Right Icon
export const IconArrowRight: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <line x1="5" y1="12" x2="19" y2="12" />
    <polyline points="12 5 19 12 12 19" />
  </svg>
)

// Refresh Icon
export const IconRefresh: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polyline points="23 4 23 10 17 10" />
    <polyline points="1 20 1 14 7 14" />
    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
  </svg>
)

// Search Icon
export const IconSearch: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <circle cx="11" cy="11" r="8" />
    <line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
)

// Edit / Pencil Icon
export const IconEdit: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
  </svg>
)

// Trash Icon
export const IconTrash: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <polyline points="3 6 5 6 21 6" />
    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    <line x1="10" y1="11" x2="10" y2="17" />
    <line x1="14" y1="11" x2="14" y2="17" />
  </svg>
)

// Eye Icon (View)
export const IconEye: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
    <circle cx="12" cy="12" r="3" />
  </svg>
)

// Eye Off Icon (Hidden)
export const IconEyeOff: React.FC<IconProps> = ({ 
  size = defaultProps.size, 
  color = defaultProps.color,
  strokeWidth = defaultProps.strokeWidth,
  className 
}) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={strokeWidth} strokeLinecap="round" strokeLinejoin="round" className={className}>
    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
    <line x1="1" y1="1" x2="23" y2="23" />
  </svg>
)

export default {
  IconDashboard,
  IconUsers,
  IconCalendar,
  IconVacation,
  IconInventory,
  IconSettings,
  IconBuilding,
  IconCommand,
  IconManager,
  IconClock,
  IconUser,
  IconDocument,
  IconBell,
  IconActivity,
  IconBranch,
  IconLogout,
  IconPlus,
  IconCheck,
  IconClose,
  IconAlert,
  IconChart,
  IconArrowRight,
  IconRefresh,
  IconSearch,
  IconEdit,
  IconTrash,
  IconEye,
  IconEyeOff
}
