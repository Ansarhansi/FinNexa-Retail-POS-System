# FinNexa — Retail POS & Financial Management System

**A Full-Stack Retail Point of Sale and Financial Management Application**

FinNexa is a full-stack retail management application designed to simplify and centralise everyday business operations. It brings product management, sales, purchases, inventory tracking, customer and supplier management, expense tracking, user administration, and financial reporting into one integrated platform.

Built with **Java, Spring Boot, React, and PostgreSQL**, FinNexa follows a frontend–backend architecture with RESTful API communication and a structured backend service layer.

---

## 1. Project Overview

Retail businesses need an organised way to manage daily transactions, inventory, customers, suppliers, and financial activities.

FinNexa addresses these operational requirements through a unified application that helps businesses:

* Manage products and maintain product records.
* Handle customer and supplier information.
* Process and manage sales transactions.
* Record purchases and supplier transactions.
* Monitor inventory and stock adjustments.
* Track operational expenses.
* View financial and operational reports.
* Manage application users and access roles.
* Maintain audit records of important application activities.

The application consists of two independently runnable modules maintained within one GitHub repository.

| Module   | Technology        | Responsibility                                              |
| -------- | ----------------- | ----------------------------------------------------------- |
| Backend  | Java, Spring Boot | Business logic, REST APIs, security and database operations |
| Frontend | React, Vite       | User interface, navigation, dashboards and API integration  |
| Database | PostgreSQL        | Persistent application data                                 |

---

## 2. Key Features

| Module               | Functionality                                                   |
| -------------------- | --------------------------------------------------------------- |
| Dashboard            | Centralised overview of business activities and key information |
| Product Management   | Manage product information and catalogue records                |
| Customer Management  | Maintain customer details                                       |
| Supplier Management  | Maintain supplier information                                   |
| Sales Management     | Create and manage sales transactions                            |
| Purchase Management  | Record and manage purchases                                     |
| Inventory Management | Monitor inventory and stock adjustments                         |
| Expense Management   | Record and manage business expenses                             |
| Financial Reports    | View financial summaries and business reports                   |
| User Management      | Manage users, roles and account status                          |
| Audit Logs           | Track important application activities                          |
| User Profile         | Update profile information and password                         |

### Business Management Capabilities

* Centralised retail operations.
* Role-based access control.
* REST API-based communication.
* Database-backed data management.
* Audit logging for important operations.
* Dashboard-based business visibility.
* Responsive frontend interface.

---

## 3. Technology Stack

### Backend Technologies

| Technology        | Purpose                                    |
| ----------------- | ------------------------------------------ |
| Java 21           | Core programming language                  |
| Spring Boot 3.5.6 | Backend application framework              |
| Spring Web        | REST API development                       |
| Spring Data JPA   | Database interaction                       |
| Spring Security   | Authentication and authorisation           |
| Hibernate         | ORM and persistence                        |
| PostgreSQL        | Relational database                        |
| Maven             | Dependency management and build automation |

### Frontend Technologies

| Technology        | Purpose                                |
| ----------------- | -------------------------------------- |
| React.js          | Component-based user interface         |
| Vite              | Frontend development and build tooling |
| JavaScript (ES6+) | Application logic                      |
| JSX               | React component structure              |
| React Router      | Client-side navigation                 |
| Axios             | HTTP requests and API integration      |
| Bootstrap         | UI styling and responsive layout       |
| CSS3              | Custom styling                         |
| Recharts          | Charts and data visualisation          |

### Development Tools

* Visual Studio Code
* Eclipse / IntelliJ IDEA
* Git and GitHub
* Postman
* PostgreSQL
* Node.js and npm

---

## 4. System Architecture

FinNexa uses a three-layer application architecture consisting of a React frontend, Spring Boot backend, and PostgreSQL database.

```text
                 FINNEXA APPLICATION
                         |
          +--------------+--------------+
          |                             |
          v                             |
   REACT FRONTEND                       |
   Port: 5173                           |
                                        |
   +---------------------------+        |
   | Pages                     |        |
   | Components                |        |
   | Services                  |        |
   | Utilities                 |        |
   +---------------------------+        |
          |                             |
          | HTTP / REST API             |
          v                             |
   SPRING BOOT BACKEND                  |
   Port: 8081                           |
                                        |
   +---------------------------+        |
   | Controllers               |        |
   | Services                  |        |
   | Repositories              |        |
   | Entities and DTOs          |        |
   | Security Configuration   |        |
   +---------------------------+        |
          |                             |
          | Spring Data JPA             |
          v                             |
   POSTGRESQL DATABASE                  |
   Database: finnexa_db                 |
          |                             |
          +-----------------------------+
```

### Backend Request Lifecycle

1. The React frontend sends an HTTP request.
2. The Spring Boot controller receives the request.
3. The service layer processes the business logic.
4. The repository layer communicates with PostgreSQL.
5. The backend returns the response.
6. The frontend updates the user interface.

---

## 5. Complete Repository Structure

The project uses a monorepo structure, keeping the frontend and backend inside one GitHub repository.

```text
FinNexa-Retail-POS-Financial-Management/
│
├── finnexa/                              # Spring Boot Backend
│   │
│   ├── src/
│   │   │
│   │   ├── main/
│   │   │   │
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── finnexa/
│   │   │   │           │
│   │   │   │           ├── config/
│   │   │   │           │   ├── CorsConfig.java
│   │   │   │           │   └── SecurityConfig.java
│   │   │   │           │
│   │   │   │           ├── controller/
│   │   │   │           │   ├── AuthController.java
│   │   │   │           │   ├── ProductController.java
│   │   │   │           │   ├── CustomerController.java
│   │   │   │           │   ├── SupplierController.java
│   │   │   │           │   ├── SaleController.java
│   │   │   │           │   ├── PurchaseController.java
│   │   │   │           │   ├── InventoryController.java
│   │   │   │           │   ├── ExpenseController.java
│   │   │   │           │   ├── FinancialReportController.java
│   │   │   │           │   ├── AuditLogController.java
│   │   │   │           │   ├── UserManagementController.java
│   │   │   │           │   └── HealthController.java
│   │   │   │           │
│   │   │   │           ├── dto/
│   │   │   │           │   └── Request and response DTOs
│   │   │   │           │
│   │   │   │           ├── entity/
│   │   │   │           │   ├── User.java
│   │   │   │           │   ├── Role.java
│   │   │   │           │   ├── Product.java
│   │   │   │           │   ├── Customer.java
│   │   │   │           │   ├── Supplier.java
│   │   │   │           │   ├── Sale.java
│   │   │   │           │   ├── SaleItem.java
│   │   │   │           │   ├── Purchase.java
│   │   │   │           │   ├── PurchaseItem.java
│   │   │   │           │   ├── Expense.java
│   │   │   │           │   ├── AuditLog.java
│   │   │   │           │   └── InventoryAdjustment.java
│   │   │   │           │
│   │   │   │           ├── exception/
│   │   │   │           │   ├── BadRequestException.java
│   │   │   │           │   ├── ResourceNotFoundException.java
│   │   │   │           │   └── GlobalExceptionHandler.java
│   │   │   │           │
│   │   │   │           ├── repository/
│   │   │   │           │   └── JPA Repository Interfaces
│   │   │   │           │
│   │   │   │           ├── service/
│   │   │   │           │   ├── AuthService.java
│   │   │   │           │   ├── ProductService.java
│   │   │   │           │   ├── CustomerService.java
│   │   │   │           │   ├── SupplierService.java
│   │   │   │           │   ├── SaleService.java
│   │   │   │           │   ├── PurchaseService.java
│   │   │   │           │   ├── InventoryService.java
│   │   │   │           │   ├── ExpenseService.java
│   │   │   │           │   ├── ReportService.java
│   │   │   │           │   ├── FinancialReportService.java
│   │   │   │           │   ├── AuditLogService.java
│   │   │   │           │   └── UserManagementService.java
│   │   │   │           │
│   │   │   │           └── FinNexaApplication.java
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── application-local.properties
│   │   │
│   │   └── test/
│   │       └── java/
│   │           └── com/finnexa/
│   │               ├── controller/
│   │               │   └── Controller Test Classes
│   │               └── service/
│   │                   └── Service Test Classes
│   │
│   └── pom.xml
│
│
├── finnexa-frontend/                     # React Frontend
│   │
│   ├── public/
│   │
│   ├── src/
│   │   │
│   │   ├── assets/                       # Images and static assets
│   │   │
│   │   ├── components/                   # Reusable UI components
│   │   │
│   │   ├── pages/                        # Application pages
│   │   │
│   │   ├── services/                     # API integration and services
│   │   │
│   │   ├── utils/                        # Shared utility functions
│   │   │
│   │   ├── App.jsx                       # Root application component
│   │   ├── App.css                       # Application styles
│   │   ├── index.css                     # Global styles
│   │   └── main.jsx                      # Application entry point
│   │
│   ├── index.html
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.js
│   └── eslint.config.js
│
│
├── .gitignore                            # Root Git ignore configuration
└── README.md                             # Project documentation
```

---

## 6. Prerequisites

Install the following software before running the application.

| Software   | Requirement                       |
| ---------- | --------------------------------- |
| Java JDK   | 21                                |
| Maven      | Compatible with Spring Boot 3.5.6 |
| Node.js    | LTS version                       |
| npm        | Included with Node.js             |
| PostgreSQL | Database server                   |
| Git        | Version control                   |

Verify the installations:

```bash
java -version
mvn -version
node -v
npm -v
psql --version
git --version
```

---

## 7. Installation and Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/Ansarhansi/FinNexa-Retail-POS-Financial-Management.git
```

Navigate to the project:

```bash
cd FinNexa-Retail-POS-Financial-Management
```

### Step 2: Create the PostgreSQL Database

Open PostgreSQL and execute:

```sql
CREATE DATABASE finnexa_db;
```

Ensure that your PostgreSQL server is running.

### Step 3: Configure the Backend

Navigate to the backend:

```bash
cd finnexa
```

Create:

```text
src/main/resources/application-local.properties
```

Configure your local database credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finnexa_db
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

server.port=8081
```

Replace the placeholders with your actual local PostgreSQL credentials.

The tracked `application.properties` should contain:

```properties
spring.config.import=optional:classpath:application-local.properties
```

**Security:** Never commit `application-local.properties` or publish database passwords.

### Step 4: Start the Backend

From the `finnexa` directory:

```bash
mvn spring-boot:run
```

Backend base URL:

```text
http://localhost:8081
```

### Step 5: Start the Frontend

Open another terminal at the repository root.

Navigate to:

```bash
cd finnexa-frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

---

## 8. Application URLs

| Service             | URL                       |
| ------------------- | ------------------------- |
| Frontend            | http://localhost:5173     |
| Backend             | http://localhost:8081     |
| REST API Base       | http://localhost:8081/api |
| PostgreSQL Database | localhost:5432            |
| Database Name       | finnexa_db                |

---

## 9. Useful Development Commands

### Backend Commands

Run from `finnexa/`:

```bash
# Run application
mvn spring-boot:run

# Execute tests
mvn test

# Clean and build
mvn clean package

# Run packaged application
java -jar target/finnexa-1.0.0.jar
```

### Frontend Commands

Run from `finnexa-frontend/`:

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build production version
npm run build

# Preview production build
npm run preview

# Run lint checks
npm run lint
```

---

## 10. Security and Configuration

FinNexa follows a separation between application code and local configuration.

* Database credentials are stored in a local configuration file.
* Spring Security manages backend access.
* Role-based authorisation is used for protected application operations.
* Audit logging records important application activities.
* Local environment files and generated build files are excluded from Git.
* Sensitive credentials must never be committed to the repository.

For production deployment, use environment-specific configuration and a secure secrets-management approach.

---

## 11. Future Enhancements

Potential future improvements include:

* Automated CI/CD pipelines.
* Production deployment and hosting.
* Enhanced reporting and analytics.
* Expanded integration and end-to-end testing.
* Advanced inventory forecasting.
* Improved monitoring and application observability.

---

## 12. Project Information

| Detail               | Information                         |
| -------------------- | ----------------------------------- |
| Project Name         | FinNexa                             |
| Project Type         | Full-Stack Web Application          |
| Domain               | Retail POS and Financial Management |
| Backend              | Java 21, Spring Boot                |
| Frontend             | React.js, Vite                      |
| Database             | PostgreSQL                          |
| API Architecture     | RESTful APIs                        |
| Build Tool           | Maven / npm                         |
| Version Control      | Git and GitHub                      |
| Repository Structure | Monorepo                            |

---

**FinNexa — Simplifying Retail Operations. Connecting Sales, Inventory and Financial Management.**
