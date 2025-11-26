# 🍽️ BistroFlow - Smart Restaurant Scheduling System

> A comprehensive scheduling and workforce management system for restaurants with real-time notifications, employee availability tracking, and HR analytics.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev)
[![Node.js](https://img.shields.io/badge/Node.js-18+-green.svg)](https://nodejs.org)

## 📋 Features

### Current (Phase 1)
- ✅ **Employee & HR Authentication** - JWT-based login with role-based access control
- ✅ **Availability Submission** - Employees submit when they can work each week
- ✅ **Time-off Requests** - Employees request time off, HR approves/rejects with notifications
- ✅ **Real-time Notifications** - WebSocket/STOMP for instant updates to HR and employees
- ✅ **Schedule Management** - Basic shift assignment and viewing
- ✅ **Notification Panel** - Persistent notification management with clear history

### Coming Soon (Phase 2)
- 🟡 **Smart Scheduling with Constraints** - Auto-assign employees respecting role requirements
- 🟡 **Force-Assignment** - Force unavailable employees with automatic notifications
- 🟡 **HR Dashboard Analytics** - Real-time staffing metrics and coverage analysis
- 🟡 **Inventory Integration** - Link scheduling to inventory demand forecasting

## 🛠️ Tech Stack

### Backend
- **Framework:** Spring Boot 4
- **Language:** Java 17
- **Database:** H2 (can upgrade to PostgreSQL)
- **Authentication:** JWT (io.jsonwebtoken)
- **Real-time:** Spring WebSocket + STOMP
- **ORM:** Spring Data JPA

### Frontend
- **Framework:** Vite + React 18
- **Language:** TypeScript
- **HTTP Client:** axios
- **Real-time:** @stomp/stompjs + sockjs-client
- **Styling:** CSS custom

## 🚀 Quick Start

### Prerequisites
- **Java 17+** - [Download](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- **Node.js 18+** - [Download](https://nodejs.org)
- **Git**

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/bistroflow.git
cd bistroflow
```

#### 2. Backend Setup
```bash
cd backend/adss-backend

# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

Backend will start on `http://localhost:8080`

#### 3. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Development server (with hot reload)
npm run dev
```

Frontend will start on `http://localhost:5173`

### Verify Installation
- Open `http://localhost:5173` in your browser
- You should see the login page
- Debug Panel (bottom-right) shows auth state

## 📚 Documentation

- **[START_HERE.md](START_HERE.md)** - Complete overview and getting started guide
- **[QUICK_START.md](QUICK_START.md)** - Detailed setup instructions
- **[ROADMAP.md](ROADMAP.md)** - Feature roadmap and Phase 2 details
- **[PROBLEMS_AND_SOLUTIONS.md](PROBLEMS_AND_SOLUTIONS.md)** - Troubleshooting guide
- **[ARCHITECTURAL_OVERVIEW.md](docs/ARCHITECTURE.md)** - System design (coming soon)

## 📊 Architecture

```
Frontend (Vite + React)
    ↓ (axios + JWT)
Backend (Spring Boot)
    ↓
Database (H2/PostgreSQL)

Real-time Layer: WebSocket/STOMP
├─ Employee notifications
├─ HR updates
└─ Schedule changes
```

## 🔐 Authentication

- **JWT-based** with claims: employeeId, hrManager, branchId, roles
- **Role-based access control:** Employee vs HR Manager
- **Token persistence:** localStorage (bistroflow-auth)
- **Auto-rehydration:** Token automatically restored on page refresh

## 🗄️ Database Schema

Key tables:
- `employees` - Employee accounts
- `availability` - Weekly availability submissions
- `time_off_requests` - Time-off request tracking
- `shift_assignments` - Shift assignments
- `notifications` - Notification history
- `shift_constraints` - Role requirements (Phase 2)
- `forced_assignments` - Force-assignment tracking (Phase 2)

## 🧪 Testing

### Backend Tests
```bash
cd backend/adss-backend
./mvnw test
```

### Frontend Tests (Coming Soon)
```bash
cd frontend
npm run test
```

## 🐛 Known Issues

- HR endpoints return 403 if JWT token isn't properly attached (see [PROBLEMS_AND_SOLUTIONS.md](PROBLEMS_AND_SOLUTIONS.md))
- Debug mode can be verbose (see Debug Panel to disable/filter)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## 👥 Authors

- **Asher** - Initial implementation

## 🙏 Acknowledgments

- Spring Boot team for excellent framework
- React team for amazing UI library
- Community contributors and testers

## 📞 Support

- 📖 Read the [documentation](START_HERE.md)
- 🐛 Check [known issues](PROBLEMS_AND_SOLUTIONS.md)
- 💬 Open a GitHub issue for bugs or feature requests

## 🗺️ Roadmap

### Phase 1 (Current)
- ✅ Core authentication and basic scheduling

### Phase 2 (Next)
- 🟡 Smart scheduling with constraints
- 🟡 Force-assignment with notifications
- 🟡 HR analytics dashboard
- 🟡 Inventory integration

### Phase 3 (Future)
- 🔵 Mobile app
- 🔵 Advanced reporting
- 🔵 Integration with payroll system
- 🔵 ML-based scheduling optimization

---

**Made with ❤️ for restaurant managers and employees everywhere.** 🍽️
