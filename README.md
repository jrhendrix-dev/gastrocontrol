# GastroControl

Restaurant Operations Platform for the Hostelería sector.

## 🚀 Overview
GastroControl is a robust backend application designed to streamline restaurant operations. It provides features for managing products, categories, orders, tables, and user authentication, tailored for various roles such as Admin, Manager, and Staff.

## 🛠 Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.4.12
- **Security:** Spring Security with JWT (JSON Web Tokens)
- **Database:** MySQL 8.0
- **Migrations:** Flyway
- **Build Tool:** Maven
- **Containerization:** Docker & Docker Compose
- **Utilities:** Lombok

## 📋 Requirements
- **Java:** JDK 21
- **Maven:** 3.8+ (or use the provided `mvnw`)
- **Docker & Docker Compose:** For running the database and the application in containers.

## ⚙️ Setup & Installation

### 1. Clone the repository
```bash
git clone https://github.com/your-repo/gastrocontrol.git
cd gastrocontrol
```

### 2. Environment Configuration
Set the following environment variables (or provide them via a `.env` file if using Docker):
- `DB_PASSWORD`: Password for the MySQL database.
- `JWT_SECRET`: A long random string for JWT signing.
- `MAILGUN_API_KEY`: (Optional) For email services.
- `MAILGUN_DOMAIN`: (Optional) For email services.

### 3. Run with Docker Compose (Recommended)
This will start both the MySQL database and the API.
```bash
docker-compose up --build
```
The API will be available at `http://localhost:8080`.

### 4. Run Locally (Development)
Start the database first (e.g., using Docker):
```bash
docker-compose up -d db
```
Then run the application:
```bash
./mvnw spring-boot:run
```

## 📜 Scripts
- `./mvnw clean install`: Build the project and install dependencies.
- `./mvnw spring-boot:run`: Run the application.
- `./mvnw test`: Run unit and integration tests.

## 🔐 Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `gastrocontrol` |
| `DB_USER` | Database user | `gastrocontrol` |
| `DB_PASSWORD` | Database password | **Required** |
| `JWT_SECRET` | JWT Signing Key | **Required** |
| `MAIL_ENABLED` | Enable email sending | `false` |
| `MAILGUN_API_KEY` | Mailgun API Key | - |
| `MAILGUN_DOMAIN` | Mailgun Domain | - |
| `MAIL_FROM_EMAIL` | Sender Email | `no-reply@jonathan-hendrix.dev` |

## 🧪 Testing
Run the tests using Maven:
```bash
./mvnw test
```

## 📂 Project Structure
```text
src/main/java/com/gastrocontrol/gastrocontrol/
├── application/     # Application services (Use Cases) and Ports
├── common/          # Shared exceptions, web responses, and utilities
├── config/          # Configuration classes (Security, Mail, etc.)
├── controller/      # REST Controllers (API Endpoints)
├── domain/          # Domain models and Enums
├── dto/             # Data Transfer Objects
├── infrastructure/  # Persistence (Entities/Repositories) and External Adapters (Mail)
├── mapper/          # Object mapping logic
└── security/        # JWT and Spring Security implementation
```

## 📄 License
This project is currently unlicensed (TODO: Update if a specific license is chosen).
