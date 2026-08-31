# JWT + Multi-Tenant REST API

This project is a Spring Boot REST API with:
- JWT authentication
- tenant isolation (`tenant_id` on tenant-owned tables)
- tenant switching for users assigned to multiple tenants

## Architecture (packages)

- `auth`: authentication controllers/services and DTOs
- `security`: JWT service/filter + Spring Security config
- `multitenancy`: `TenantContext`, tenant filter, tenant entity listener
- `tenant`: tenant entity/repository/DTOs
- `user`: user, role, user-tenant link, profile endpoints/services
- `customer`: tenant-owned customer CRUD
- `customer.category`: tenant-owned customer category CRUD
- `provider`: tenant-owned provider CRUD
- `invoice`: sample tenant-owned CRUD resource
- `shared.exception`: API exception handling

## Profiles

- `local`: PostgreSQL + local JWT secret in `application-local.yml`
- `prod`: PostgreSQL + JWT settings from environment variables
- `test`: H2 in-memory DB + JWT test secret in `application-test.yml`

Base config: `application.yml` sets `spring.profiles.default=local`.

## Required prod environment variables

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (must be at least 32 chars)
- `JWT_EXPIRATION_MINUTES` (optional, default `60`)

## Security and tenant behavior

- Public endpoints:
  - `POST /v1/auth/register`
  - `POST /v1/auth/login`
- Auth required for all other endpoints.
- Login and tenant-join endpoints are rate-limited (in-memory, per app instance).
- JWT contains:
  - `uid` (user id)
  - `sub` (email)
  - `tenantId` (selected tenant)
  - `tenants` (allowed tenant IDs)
- `TenantContext` is filled from JWT on each request and cleared after request.
- Tenant-owned writes auto-assign `tenant_id` using JPA listener.
- Tenant-owned reads/updates/deletes always query with current tenant id.
- Tenant join requires an invite code (not tenant id).
- Invite codes are one-time and expire automatically.

## Endpoints

### Auth

- `POST /v1/auth/register`
- `POST /v1/auth/login`
- `POST /v1/auth/switch-tenant`
- `POST /v1/auth/tenants` (create tenant and auto-join)
- `POST /v1/auth/tenants/invites` (create tenant invite code, owner/admin only)
- `POST /v1/auth/tenants/join` (join with invite code)
- `DELETE /v1/auth/tenants/{tenantId}/members/{memberUserId}` (owner/admin only)
- `POST /v1/auth/logout`

### User

- `GET /v1/me`
- `GET /v1/me/tenants`

### Tenant-owned sample resource (invoices)

- `POST /v1/invoices`
- `GET /v1/invoices`
- `GET /v1/invoices/{id}`
- `PUT /v1/invoices/{id}`
- `DELETE /v1/invoices/{id}`

### Tenant-owned customers

- `POST /v1/customers`
- `GET /v1/customers`
- `GET /v1/customers/{id}`
- `PUT /v1/customers/{id}`
- `DELETE /v1/customers/{id}`

`PUT /v1/customers/{id}` accepts partial payloads. Missing fields keep their current values.

### Tenant-owned customer categories

- `POST /v1/customer-categories`
- `GET /v1/customer-categories`
- `GET /v1/customer-categories/{id}`
- `PUT /v1/customer-categories/{id}`
- `PUT /v1/customer-categories/{id}/default`
- `DELETE /v1/customer-categories/{id}/default`
- `DELETE /v1/customer-categories/{id}`

Default customer category cannot be deleted directly.
Customer categories linked to existing customers cannot be deleted.

`PUT /v1/customer-categories/{id}` accepts partial payloads. Missing fields keep their current values.
Use `PUT /v1/customer-categories/{id}/default` to mark a category as default, and `DELETE /v1/customer-categories/{id}/default` to unset it.

### Tenant-owned providers

- `POST /v1/providers`
- `GET /v1/providers`
- `GET /v1/providers/{id}`
- `PUT /v1/providers/{id}`
- `DELETE /v1/providers/{id}`

## Example requests and responses

### Register

`POST /v1/auth/register`

```json
{
  "email": "alice@example.com",
  "password": "StrongPass123!",
  "tenantName": "Acme"
}
```

Response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInMinutes": 120,
  "selectedTenantId": 1,
  "allowedTenantIds": [1]
}
```

### Login

`POST /v1/auth/login`

```json
{
  "email": "alice@example.com",
  "password": "StrongPass123!"
}
```

### Switch tenant

`POST /v1/auth/switch-tenant`

```json
{
  "tenantId": 2
}
```

Response: same shape as login/register but with updated `selectedTenantId` and new token.

### Logout (authenticated)

`POST /v1/auth/logout`

No body. Send `Authorization: Bearer <access_token>`.

Response: `204 No Content`.

### Create invoice

`POST /v1/invoices`

```json
{
  "reference": "INV-2026-001",
  "description": "Consulting services",
  "amount": 1500.00,
  "templateChoice": "MODERN"
}
```

`tenant_id` is not accepted from client and is assigned from current tenant context.
If `templateChoice` is omitted, the invoice uses the tenant/company default template, or `CLASSIC` when no default is configured.

### Create customer

`POST /v1/customers`

```json
{
  "name": "Acme Morocco",
  "email": "contact@acme.ma",
  "phone": "+212600000000",
  "address": "Casablanca",
  "taxId": "ICE-123456",
  "categoryId": 1
}
```

`categoryId` is optional. If omitted, the tenant default category is used. When provided, it must reference a category in the current tenant.

### Create customer category

`POST /v1/customer-categories`

```json
{
  "name": "Wholesale",
  "description": "B2B customers with volume contracts",
  "isDefault": true
}
```

`isDefault` is optional. If no default exists yet for the tenant, the created category becomes default automatically.

### Create provider

`POST /v1/providers`

```json
{
  "name": "Office Supplies SARL",
  "email": "sales@supplies.ma",
  "phone": "+212611111111",
  "address": "Rabat",
  "taxId": "ICE-654321"
}
```

### Create another tenant (authenticated)

`POST /v1/auth/tenants`

```json
{
  "tenantName": "Beta LLC"
}
```

Response: same shape as login/register with the new tenant selected.

### Join existing tenant (authenticated)

`POST /v1/auth/tenants/invites`

```json
{
  "tenantId": 2,
  "expiresInHours": 24
}
```

Notes:
- Only the tenant owner (user whose `defaultTenantId` is this tenant) or a global `ADMIN` can create invites.
- Invite max lifetime is 168 hours.

Example response:

```json
{
  "inviteCode": "9XK7P2QW4M",
  "tenantId": 2,
  "expiresAt": "2026-06-20T14:30:00Z"
}
```

`POST /v1/auth/tenants/join`

```json
{
  "inviteCode": "9XK7P2QW4M"
}
```

Response: same shape as login/register with the joined tenant selected.

Notes:
- Invite code is single-use and consumed atomically.
- Invalid/expired/used codes return an error and do not join the tenant.

### Remove tenant member (authenticated)

`DELETE /v1/auth/tenants/{tenantId}/members/{memberUserId}`

Notes:
- Only tenant owner or global `ADMIN` can remove members.
- Self-removal is blocked on this endpoint.
- Tenant owner cannot be removed from their own tenant (transfer ownership first).
- Response: `204 No Content`.

## Run

```powershell
Set-Location "C:\Users\Windows 11\Desktop\Projects\api.facturesimple.ma"
cmd /c ".\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local"
```

## Test

```powershell
Set-Location "C:\Users\Windows 11\Desktop\Projects\api.facturesimple.ma"
cmd /c ".\mvnw.cmd test -Dspring.profiles.active=test"
```

> Note: this repository is a Spring Boot/Maven project, so `npm run test` will fail because there is no `package.json`.

