# Splitwise Clone - Backend API

A production-grade expense sharing backend system built with Spring Boot.

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.3**
- **Spring Data JPA**
- **PostgreSQL** (production) / **H2** (development)
- **Gradle**
- **Lombok**
- **MapStruct**
- **JUnit 5**

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Controllers                             │
│  (UserController, GroupController, ExpenseController)        │
├─────────────────────────────────────────────────────────────┤
│                       Services                               │
│  (UserService, GroupService, ExpenseService, SettlementService) │
├─────────────────────────────────────────────────────────────┤
│                      Repositories                            │
│  (JPA Repositories with custom queries)                      │
├─────────────────────────────────────────────────────────────┤
│                       Entities                               │
│  (User, Group, GroupMember, Expense, ExpenseSplit)           │
└─────────────────────────────────────────────────────────────┘
```

## Project Structure

```
src/main/java/com/split/splitwise/
├── controller/          # REST API endpoints
├── service/             # Business logic
├── repository/          # Data access layer
├── entity/              # JPA entities
├── dto/
│   ├── request/         # Input DTOs with validation
│   └── response/        # Output DTOs
├── mapper/              # MapStruct mappers
├── exception/           # Custom exceptions & global handler
└── SplitwiseApplication.java
```

## Quick Start

### Prerequisites

- JDK 17+
- PostgreSQL (for production) or use H2 (default for dev)

### Run with Development Profile (H2 Database)

```bash
./gradlew bootRun
```

The application starts at `http://localhost:8080`

H2 Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:splitwise`
- Username: `sa`
- Password: (empty)

### Run with PostgreSQL

1. Create database:
```sql
CREATE DATABASE splitwise;
```

2. Set environment variables:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=splitwise
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

3. Run with production profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Run Tests

```bash
./gradlew test
```

## API Endpoints

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/users` | Create a new user |
| GET | `/api/v1/users/{id}` | Get user by ID |
| GET | `/api/v1/users` | List all users |

### Groups

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups` | Create a new group |
| POST | `/api/v1/groups/{id}/members` | Add member to group |
| GET | `/api/v1/groups/{id}` | Get group with members |

### Expenses

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/groups/{id}/expenses` | Create expense |
| GET | `/api/v1/groups/{id}/balances` | Get group balances |
| GET | `/api/v1/groups/{id}/settlements` | Get optimized settlements |

## API Examples

### Create User

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com"}'
```

### Create Group

```bash
curl -X POST http://localhost:8080/api/v1/groups \
  -H "Content-Type: application/json" \
  -d '{"name": "Trip to Paris", "createdBy": "<user-uuid>"}'
```

### Add Member to Group

```bash
curl -X POST http://localhost:8080/api/v1/groups/<group-uuid>/members \
  -H "Content-Type: application/json" \
  -d '{"userId": "<user-uuid>"}'
```

### Create Expense (Equal Split)

```bash
curl -X POST http://localhost:8080/api/v1/groups/<group-uuid>/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Dinner",
    "totalAmount": 90.00,
    "paidBy": "<user-uuid>",
    "splitType": "EQUAL"
  }'
```

### Create Expense (Exact Split)

```bash
curl -X POST http://localhost:8080/api/v1/groups/<group-uuid>/expenses \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Hotel",
    "totalAmount": 100.00,
    "paidBy": "<user-uuid>",
    "splitType": "EXACT",
    "splits": [
      {"userId": "<user1-uuid>", "amount": 50.00},
      {"userId": "<user2-uuid>", "amount": 30.00},
      {"userId": "<user3-uuid>", "amount": 20.00}
    ]
  }'
```

### Get Balances

```bash
curl http://localhost:8080/api/v1/groups/<group-uuid>/balances
```

### Get Optimized Settlements

```bash
curl http://localhost:8080/api/v1/groups/<group-uuid>/settlements
```

## Key Features

### 1. Expense Splitting

- **EQUAL**: Automatically divides among all group members with proper rounding
- **EXACT**: Custom amounts per person with validation

### 2. Balance Calculation

For each user:
- **Positive balance** = should receive money
- **Negative balance** = owes money

### 3. Debt Simplification Algorithm

Minimizes the number of transactions using a greedy algorithm with two priority queues:

```
Time Complexity: O(n log n)
Space Complexity: O(n)
```

Example: 5 people with complex debts → simplified to 4 transactions (maximum n-1)

### 4. Rounding Handling

- All monetary calculations use `BigDecimal` with scale 2
- Equal splits: last person gets the remainder to ensure exact totals
- Settlements: uses epsilon (0.01) to handle floating-point edge cases

## Error Handling

All errors return consistent format:

```json
{
  "timestamp": "2024-03-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "User not found with id: xxx",
  "path": "/api/v1/users/xxx"
}
```

Error codes:
- `RESOURCE_NOT_FOUND` (404)
- `VALIDATION_ERROR` (400)
- `BUSINESS_RULE_VIOLATION` (409)
- `DUPLICATE_RESOURCE` (409)

## Database Schema

```
users
├── id (UUID, PK)
├── name
├── email (unique)
└── created_at

groups
├── id (UUID, PK)
├── name
├── created_by (FK → users)
└── created_at

group_members
├── id (UUID, PK)
├── group_id (FK → groups)
├── user_id (FK → users)
├── joined_at
└── UNIQUE(group_id, user_id)

expenses
├── id (UUID, PK)
├── description
├── total_amount
├── paid_by (FK → users)
├── group_id (FK → groups)
├── split_type (EQUAL/EXACT)
└── created_at

expense_splits
├── id (UUID, PK)
├── expense_id (FK → expenses)
├── user_id (FK → users)
└── amount_owed
```

## Performance Considerations

1. **Indexes** on frequently queried columns (group_id, user_id)
2. **Fetch joins** in repositories to prevent N+1 queries
3. **`@Transactional(readOnly=true)`** for read operations
4. **BigDecimal** for all monetary calculations (no floating-point errors)

## Testing

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "SettlementServiceTest"

# Run with coverage report
./gradlew test jacocoTestReport
```

Test coverage includes:
- Unit tests for split logic
- Unit tests for debt simplification algorithm
- Integration tests for REST endpoints
- Edge cases: rounding, empty groups, large groups (100+ users)
