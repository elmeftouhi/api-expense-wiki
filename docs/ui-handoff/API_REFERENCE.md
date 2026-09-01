# 🔌 Expense Management API - Quick Reference

## Configuration API

### Get Tenant Configuration
```http
GET /v1/config
Authorization: Bearer {token}

Response: 200 OK
{
  "id": 1,
  "tenantId": 1,
  "currency": "USD",
  "decimalPlaces": 2,
  "language": "EN",
  "timezone": "UTC",
  "dateFormat": "yyyy-MM-dd",
  "timeFormat": "24H",
  "weekStartDay": "MONDAY",
  "budgetAlertThreshold": 80,
  "notificationChannels": "[\"IN_APP\"]",
  "autoPaymentReminderDays": "[3,7]",
  "expenseRoundingMethod": "NONE",
  "reportingPeriod": "CALENDAR_MONTH",
  "createdAt": "2026-09-01T10:00:00Z",
  "updatedAt": "2026-09-01T10:00:00Z",
  "createdBy": "admin",
  "updatedBy": "admin"
}
```

### Update Configuration
```http
PUT /v1/config
Authorization: Bearer {token}
Content-Type: application/json

{
  "currency": "EUR",
  "decimalPlaces": 2,
  "language": "FR",
  "timezone": "Europe/Paris",
  "dateFormat": "dd/MM/yyyy",
  "timeFormat": "24H",
  "weekStartDay": "MONDAY",
  "budgetAlertThreshold": 75,
  "notificationChannels": "[\"IN_APP\", \"EMAIL\"]",
  "autoPaymentReminderDays": "[1,3,7]",
  "expenseRoundingMethod": "NONE",
  "reportingPeriod": "CALENDAR_MONTH"
}

Response: 200 OK
{...updated configuration...}
```

---

## Expense API

### Create Expense
```http
POST /v1/expenses
Authorization: Bearer {token}
Content-Type: application/json

{
  "categoryId": 1,
  "description": "Monthly subscription",
  "amount": 99.99,
  "dueDate": "2026-09-30",
  "type": "SUBSCRIPTION",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Netflix subscription"
}

Response: 201 CREATED
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Entertainment",
  "userId": 1,
  "description": "Monthly subscription",
  "amount": 99.99,
  "currency": "USD",
  "type": "SUBSCRIPTION",
  "status": "PENDING",
  "dueDate": "2026-09-30",
  "paidDate": null,
  "paymentMethod": "CREDIT_CARD",
  "notes": "Netflix subscription",
  "hasSubscription": false,
  "tenantId": 1,
  "createdAt": "2026-09-01T10:00:00Z",
  "updatedAt": "2026-09-01T10:00:00Z"
}
```

### Get All Expenses
```http
GET /v1/expenses
Authorization: Bearer {token}

Response: 200 OK
[
  {...expense 1...},
  {...expense 2...}
]
```

### Get Specific Expense
```http
GET /v1/expenses/1
Authorization: Bearer {token}

Response: 200 OK
{...expense details...}
```

### Update Expense
```http
PUT /v1/expenses/1
Authorization: Bearer {token}
Content-Type: application/json

{
  "categoryId": 2,
  "description": "Updated description",
  "amount": 109.99,
  "dueDate": "2026-10-05",
  "type": "NORMAL",
  "paymentMethod": "BANK_TRANSFER",
  "notes": "Updated notes"
}

Response: 200 OK
{...updated expense...}
```

### Delete Expense (Soft Delete)
```http
DELETE /v1/expenses/1
Authorization: Bearer {token}

Response: 204 NO CONTENT
```

### Change Expense Status
```http
PATCH /v1/expenses/1/status
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "PAID",
  "paidDate": "2026-09-01"
}

Response: 200 OK
{...updated expense with status PAID...}
```

### Mark Expense as Paid
```http
PATCH /v1/expenses/1/mark-as-paid?paidDate=2026-09-01
Authorization: Bearer {token}

Response: 200 OK
{...expense with status PAID and paidDate set...}
```

### Get Overdue Expenses
```http
GET /v1/expenses/overdue
Authorization: Bearer {token}

Response: 200 OK
[
  {...overdue expense 1...},
  {...overdue expense 2...}
]
```

### Get Statistics
```http
GET /v1/expenses/statistics?startDate=2026-09-01&endDate=2026-09-30
Authorization: Bearer {token}

Response: 200 OK
{
  "totalExpenses": 500.00,
  "totalPaid": 300.00,
  "totalPending": 200.00,
  "totalOverdue": 0.00,
  "overdueCount": 0,
  "pendingCount": 3,
  "byCategory": [
    {
      "categoryId": 1,
      "categoryName": "Entertainment",
      "total": 200.00,
      "budget": 250.00,
      "percentage": 80
    },
    {
      "categoryId": 2,
      "categoryName": "Utilities",
      "total": 300.00,
      "budget": 500.00,
      "percentage": 60
    }
  ]
}
```

---

## Subscription API

### Create Subscription
```http
POST /v1/expenses/1/subscriptions
Authorization: Bearer {token}
Content-Type: application/json

{
  "frequency": "MONTHLY",
  "endDate": "2027-09-01"
}

Response: 201 CREATED
{
  "id": 1,
  "expenseId": 1,
  "frequency": "MONTHLY",
  "customIntervalDays": null,
  "nextOccurrenceDate": "2026-10-01",
  "endDate": "2027-09-01",
  "occurrenceCount": 0,
  "active": true,
  "tenantId": 1,
  "createdAt": "2026-09-01T10:00:00Z",
  "updatedAt": "2026-09-01T10:00:00Z"
}
```

### Create Subscription with Custom Interval
```http
POST /v1/expenses/2/subscriptions
Authorization: Bearer {token}
Content-Type: application/json

{
  "frequency": "CUSTOM",
  "customIntervalDays": 14,
  "endDate": "2027-12-31"
}

Response: 201 CREATED
{...subscription with 14-day interval...}
```

### Get Subscription
```http
GET /v1/expenses/1/subscriptions
Authorization: Bearer {token}

Response: 200 OK
{...subscription details...}
```

### Update Subscription
```http
PUT /v1/expenses/1/subscriptions
Authorization: Bearer {token}
Content-Type: application/json

{
  "frequency": "BIWEEKLY",
  "endDate": "2027-12-31"
}

Response: 200 OK
{...updated subscription...}
```

### Toggle Subscription Status
```http
PATCH /v1/expenses/1/subscriptions/status?active=false
Authorization: Bearer {token}

Response: 200 OK
{...subscription with active=false...}
```

---

## Notification API

### Get All Notifications
```http
GET /v1/notifications
Authorization: Bearer {token}

Response: 200 OK
[
  {
    "id": 1,
    "userId": 1,
    "type": "EXPENSE_CREATED",
    "status": "SENT",
    "title": "Expense Created",
    "message": "You created a new expense: Netflix subscription",
    "subjectType": "EXPENSE",
    "subjectId": 5,
    "data": null,
    "tenantId": 1,
    "createdAt": "2026-09-01T10:00:00Z",
    "readAt": null,
    "archivedAt": null
  }
]
```

### Get Unread Notifications
```http
GET /v1/notifications/unread
Authorization: Bearer {token}

Response: 200 OK
[
  {...unread notification 1...},
  {...unread notification 2...}
]
```

### Get Unread Count
```http
GET /v1/notifications/unread/count
Authorization: Bearer {token}

Response: 200 OK
3
```

### Mark Notification as Read
```http
PATCH /v1/notifications/1/mark-as-read
Authorization: Bearer {token}

Response: 200 OK
{...notification with status=READ and readAt set...}
```

### Mark Notification as Archived
```http
PATCH /v1/notifications/1/mark-as-archived
Authorization: Bearer {token}

Response: 200 OK
{...notification with status=ARCHIVED and archivedAt set...}
```

### Mark All Notifications as Read
```http
POST /v1/notifications/mark-all-as-read
Authorization: Bearer {token}

Response: 204 NO CONTENT
```

---

## Enum Reference

### Currency
```
USD, EUR, GBP, CAD, AUD, JPY, CNY, INR, SAR, AED
```

### Language
```
EN, FR, ES, DE, AR, ZH, JA, PT, RU, IT
```

### TimeFormat
```
FORMAT_12H, FORMAT_24H
```

### WeekStartDay
```
MONDAY, SUNDAY
```

### ExpenseType
```
NORMAL, SUBSCRIPTION
```

### ExpenseStatus
```
PENDING, PAID, OVERDUE, CANCELLED
```

### SubscriptionFrequency
```
DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, SEMIANNUAL, YEARLY, CUSTOM
```

### NotificationType
```
EXPENSE_CREATED, EXPENSE_DUE_TODAY, EXPENSE_OVERDUE, EXPENSE_DUE_SOON,
SUBSCRIPTION_NEXT, PAYMENT_REMINDER, BUDGET_EXCEEDED, BUDGET_ALERT
```

### NotificationStatus
```
SENT, READ, ARCHIVED
```

---

## Example Workflows

### Workflow 1: Create and Pay an Expense
```
1. POST /v1/expenses
   → Creates PENDING expense

2. Wait for payment...

3. PATCH /v1/expenses/{id}/mark-as-paid?paidDate=2026-09-15
   → Updates to PAID status

4. GET /v1/notifications/unread
   → See "Expense marked as paid" notification
```

### Workflow 2: Set Up Monthly Subscription
```
1. POST /v1/expenses
   → Create expense with type: "SUBSCRIPTION"

2. POST /v1/expenses/{id}/subscriptions
   → frequency: "MONTHLY", endDate: "2027-09-01"

3. At midnight daily:
   → Scheduler checks for due subscriptions
   → Auto-generates new expense for next month
   → Creates SubscriptionOccurrence record
   → Sends "SUBSCRIPTION_NEXT" notification

4. User receives reminder notifications:
   → 3 days before due date
   → 7 days before due date
   → On due date
   → When overdue
```

### Workflow 3: Track Budget
```
1. Category has monthlyBudget: 500.00

2. User creates expenses totaling 400.00

3. GET /v1/expenses/statistics
   → Shows percentage: 80
   → Triggers BUDGET_ALERT notification (if threshold = 80)

4. User adds more expenses reaching 600.00

5. System sends BUDGET_EXCEEDED notification
```

---

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Successful GET, PUT, PATCH |
| 201 | Created - Successful POST |
| 204 | No Content - Successful DELETE or empty response |
| 400 | Bad Request - Validation error |
| 401 | Unauthorized - Missing/invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 409 | Conflict - Duplicate or constraint violation |
| 500 | Internal Server Error |

---

## Error Response Format

```json
{
  "timestamp": "2026-09-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Category not found",
  "path": "/v1/expenses"
}
```

---

## Authentication

All endpoints require Bearer token in Authorization header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

Current user ID is extracted from token for audit logging.

---

## Rate Limiting

- Default: 100 requests/minute per user
- Notifications: Batch up to 1000/minute
- Schedulers: System-level jobs with independent limits

---

## Notes

- All timestamps are in ISO 8601 format (UTC)
- Amounts use BigDecimal (precise to 2-8 decimal places per config)
- Soft deletes: Deleted records still queryable with filter
- Multi-tenant: All responses scoped to current tenant
- Configuration changes: Cached, invalidated on update
- Notifications: Auto-generated by schedulers or manual API calls


