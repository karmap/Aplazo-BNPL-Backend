# Aplazo BNPL Backend API

A Spring Boot application implementing a Buy Now Pay Later (BNPL) API that allows customer registration and loan management with credit line assignment and payment schemes.

## Features

- **Customer Management**: Register customers with age-based credit line assignment
- **Loan Management**: Create loans with payment schemes and installment calculations
- **JWT Authentication**: Secure API endpoints with JWT tokens
- **Payment Schemes**: Two different payment schemes with different interest rates
- **Installment Management**: Automatic generation of 5 biweekly installments
- **Credit Line Control**: Real-time credit line validation and updates

## Technology Stack

- **Java 17**
- **Spring Boot 3.5.3**
- **Spring Data JPA**
- **PostgreSQL** (with H2 for development)
- **Spring Security** with JWT
- **Maven**
- **Docker & Docker Compose**
- **Testcontainers** for integration testing
- **JaCoCo** for code coverage

## Business Rules

### Credit Line Assignment
- **$3,000** for customers aged **18 to 25** years
- **$5,000** for customers aged **26 to 30** years  
- **$8,000** for customers aged **31 to 65** years
- Customers under 18 or over 65 are not accepted

### Payment Schemes
- **Scheme 1**: 13% interest rate (for customers with first name starting with C, L, or H)
- **Scheme 2**: 16% interest rate (default for other customers)
- Both schemes have 5 biweekly installments

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose

### Using Docker Compose (Recommended)

1. **Clone and navigate to the project**:
   ```bash
   cd aplazo-backend
   ```

2. **Start the application**:
   ```bash
   docker-compose up -d
   ```

3. **Access the application**:
   - **API Base URL**: `http://localhost:8080/v1`
   - **Swagger UI**: `http://localhost:8080/v1/swagger-ui.html`
   - **Health Check**: `http://localhost:8080/v1/actuator/health`
   - **pgAdmin (Database GUI)**: `http://localhost:5050`
     - **Email**: `admin@aplazo.com`
     - **Password**: `admin123`

4. **Database Connection (for pgAdmin)**:
   - **Host**: `postgres`
   - **Port**: `5432`
   - **Database**: `aplazo_db`
   - **Username**: `aplazo_user`
   - **Password**: `aplazo_password`

### Manual Setup

1. **Install dependencies**:
   ```bash
   mvn clean install
   ```

2. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

## API Testing

### Postman Collection
Complete Postman collections are included for easy API testing:

#### Basic Collection
1. **Import the collection**: `Aplazo-BNPL-API.postman_collection.json`
2. **Import the environment**: `Aplazo-BNPL-API.postman_environment.json`

#### Enhanced Collection (Recommended)
1. **Import the enhanced collection**: `Aplazo-BNPL-API-Enhanced.postman_collection.json`
2. **Import the environment**: `Aplazo-BNPL-API.postman_environment.json`
3. **Select the environment**: "Aplazo BNPL API - Local"

#### Enhanced Features
- ✅ **Sample responses** for all endpoints
- ✅ **Multiple customer examples** (different age groups)
- ✅ **Multiple loan examples** (different amounts)
- ✅ **Automated variable management** (customer_id, loan_id, jwt_token)
- ✅ **Pre-configured requests** with realistic test data
- ✅ **Authentication handling** with JWT tokens
- ✅ **Error handling** and validation
- ✅ **Health check** and monitoring endpoints
- ✅ **Visual indicators** (emojis) for better organization

### Testing Workflow
1. **Create Customer** → Customer ID is automatically saved
2. **Generate JWT Token** → Token is automatically saved
3. **Create Loan** → Loan ID is automatically saved
4. **Test other endpoints** using the saved variables

### Collection Features
- ✅ **Automated variable management** (customer_id, loan_id, jwt_token)
- ✅ **Pre-configured requests** with sample data
- ✅ **Authentication handling** with JWT tokens
- ✅ **Error handling** and validation
- ✅ **Health check** and monitoring endpoints

## API Endpoints

### Customer Management

#### Create Customer
```http
POST /v1/customers
Content-Type: application/json

{
  "firstName": "Juan",
  "lastName": "López",
  "secondLastName": "Pérez",
  "dateOfBirth": "1998-07-21"
}
```

**Response**:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "createdAt": "2024-01-15T10:30:00",
  "creditLineAmount": 5000.00,
  "availableCreditLineAmount": 5000.00
}
```

**Headers**:
- `Location`: `/v1/customers/{customerId}`
- `X-Auth-Token`: JWT token for authentication

#### Get Customer
```http
GET /v1/customers/{customerId}
Authorization: Bearer {jwt-token}
```

### Loan Management

#### Create Loan
```http
POST /v1/loans
Authorization: Bearer {jwt-token}
Content-Type: application/json

{
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "amount": 1000.00
}
```

**Response**:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
  "customerId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
  "amount": 1000.00,
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00",
  "paymentPlan": {
    "commissionAmount": 130.00,
    "installments": [
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2024-01-29",
        "status": "NEXT"
      },
      {
        "amount": 226.00,
        "scheduledPaymentDate": "2024-02-12",
        "status": "PENDING"
      }
    ]
  }
}
```

#### Get Loan
```http
GET /v1/loans/{loanId}
Authorization: Bearer {jwt-token}
```

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn test -Dspring.profiles.active=test
```

### Code Coverage Report
```bash
mvn clean test jacoco:report
```
Coverage report will be generated in `target/site/jacoco/index.html`

### Test Coverage Requirements
- **Minimum**: 60% line coverage
- **Desired**: 80% line coverage

## Development

### Project Structure
```
src/
├── main/
│   ├── java/com/aplazo/backend/
│   │   ├── controller/          # REST controllers
│   │   ├── dto/                 # Data Transfer Objects
│   │   ├── entity/              # JPA entities
│   │   ├── repository/          # Data access layer
│   │   ├── security/            # JWT authentication
│   │   ├── service/             # Business logic
│   │   └── AplazoBackendApplication.java
│   └── resources/
│       └── application.properties
└── test/
    ├── java/com/aplazo/backend/
    │   ├── integration/         # Integration tests
    │   ├── service/             # Unit tests
    │   └── AplazoBackendApplicationTests.java
    └── resources/
        └── application-test.properties
```

### Database Schema
The application uses JPA with Hibernate to automatically generate the database schema. Key tables:
- `customers`: Customer information and credit lines
- `loans`: Loan details and payment schemes
- `installments`: Individual payment installments

### Configuration
Key configuration properties in `application.properties`:
- Database connection settings
- JWT secret and expiration
- Logging levels
- OpenAPI/Swagger settings

## Error Handling

The API returns standardized error responses:

```json
{
  "code": "APZ000002",
  "error": "INVALID_CUSTOMER_REQUEST",
  "timestamp": 1739397485,
  "message": "Customer age must be between 18 and 65 years",
  "path": "/v1/customers"
}
```

### Error Codes
- `APZ000001`: Internal Server Error
- `APZ000002`: Invalid Customer Request
- `APZ000003`: Rate Limit Error
- `APZ000004`: Invalid Request
- `APZ000005`: Customer Not Found
- `APZ000006`: Invalid Loan Request
- `APZ000007`: Unauthorized
- `APZ000008`: Loan Not Found

## Security

- JWT-based authentication for protected endpoints
- Customer creation endpoint is public
- All other endpoints require valid JWT token
- Tokens include customer ID and role information

## Monitoring

- Health check endpoint: `/v1/actuator/health`
- Metrics endpoint: `/actuator/metrics`
- Application info: `/actuator/info`

## Deployment

### Docker
```bash
# Build image
docker build -t aplazo-backend .

# Run container
docker run -p 8080:8080 aplazo-backend
```

### Docker Compose
```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f aplazo-backend

# View specific service logs
docker-compose logs -f postgres
docker-compose logs -f pgadmin

# Check service status
docker-compose ps

# Restart a specific service
docker-compose restart aplazo-backend

# Rebuild and start (after code changes)
docker-compose up -d --build
```

### Environment Variables
The application uses the following environment variables (configured in `docker-compose.yml`):

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `SPRING_DATASOURCE_URL` | Database connection URL | `jdbc:postgresql://postgres:5432/aplazo_db` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `aplazo_user` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `aplazo_password` |
| `JWT_SECRET` | JWT signing secret | `aplazo-secret-key-for-jwt-token-generation-and-validation-2024` |
| `JWT_EXPIRATION` | JWT token expiration (ms) | `86400000` (24 hours) |
| `POSTGRES_DB` | PostgreSQL database name | `aplazo_db` |
| `POSTGRES_USER` | PostgreSQL username | `aplazo_user` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `aplazo_password` |
| `PGADMIN_DEFAULT_EMAIL` | pgAdmin login email | `admin@aplazo.com` |
| `PGADMIN_DEFAULT_PASSWORD` | pgAdmin login password | `admin123` |

## Troubleshooting

### Common Issues

#### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Kill the process or use different ports
docker-compose down
docker-compose up -d
```

#### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

#### Application Won't Start
```bash
# Check application logs
docker-compose logs aplazo-backend

# Rebuild the application
docker-compose down
docker-compose up -d --build
```

#### pgAdmin Connection Issues
1. **Verify pgAdmin is running**: `docker-compose ps pgadmin`
2. **Check pgAdmin logs**: `docker-compose logs pgadmin`
3. **Database connection settings**:
   - Host: `postgres` (not localhost)
   - Port: `5432`
   - Database: `aplazo_db`
   - Username: `aplazo_user`
   - Password: `aplazo_password`

#### Data Persistence
```bash
# Check if volumes exist
docker volume ls | grep aplazo

# Backup database
docker-compose exec postgres pg_dump -U aplazo_user -d aplazo_db > backup.sql

# Restore database
docker-compose exec postgres psql -U aplazo_user -d aplazo_db < backup.sql
```

#### Apple Silicon (M1/M2) Issues
The Dockerfile uses Eclipse Temurin images which have native ARM64 support. If you encounter issues:
```bash
# Rebuild with platform specification
docker build --platform linux/amd64 -t aplazo-backend .
```

### Health Checks
```bash
# Check all services health
docker-compose ps

# Check application health
curl http://localhost:8080/v1/actuator/health

# Check database health
docker-compose exec postgres pg_isready -U aplazo_user -d aplazo_db
```

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature-name`
2. Make your changes
3. Add tests for new functionality
4. Ensure all tests pass: `mvn clean test`
5. Commit your changes: `git commit -m "Add your feature"`
6. Push to the branch: `git push origin feature/your-feature-name`

## License

This project is part of the Aplazo technical test. 

## OpenAPI Documentation

### OpenAPI JSON Specification
```
http://localhost:8080/v1/api-docs
``` 