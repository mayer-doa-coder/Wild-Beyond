# Wild Beyond — REST API Test Guide (Postman)

Base URL: `http://localhost:8080`

---

## Prerequisites

1. Application is running (`App: Run` task is active).
2. Database is running (`DB: Start` task is active).
3. Postman is open.

---

## Authentication

The app supports **HTTP Basic Auth** — no session cookie or token needed.
Set it in every authenticated request via the **Authorization** tab → **Basic Auth**.

### Test Accounts

Credentials depend on your seeded users and environment configuration.
If your local data differs, query the DB and use the actual account/password values.

| Role   | Username           | Password    | User ID | Can do                                      |
|--------|--------------------|-------------|---------|---------------------------------------------|
| BUYER  | `buyer@test.com`   | `<configured-password>` | `6`     | Place orders, view own orders               |
| SELLER | `seller@test.com`  | `<configured-password>` | `5`     | Add products, update products               |
| ADMIN  | `admin@test.com`   | `<configured-password>` | `7`     | View all orders, delete orders/products     |

**How to add Basic Auth in Postman:**

1. Open the request.
2. Click the **Authorization** tab.
3. Set **Type** → `Basic Auth`.
4. Enter the **Username** and **Password** from the table above for the relevant role.

> Read-only product endpoints (`GET /api/products`) are **public** — no auth needed.

---

## TEST-2 · GET All Products

**Purpose:** Verify the endpoint is reachable and returns a JSON list.

| Field  | Value                              |
|--------|------------------------------------|
| Method | `GET`                              |
| URL    | `http://localhost:8080/api/products` |
| Auth   | None                               |
| Body   | None                               |

**Steps:**

1. Create a new request in Postman.
2. Set method to `GET`.
3. Enter URL `http://localhost:8080/api/products`.
4. Click **Send**.

**Expected result:**

```
Status: 200 OK
Body:   []
```

> The list is empty until you create a product in TEST-3.

**Failure diagnosis:**

| Status | Cause                              | Fix                                      |
|--------|------------------------------------|------------------------------------------|
| 401    | Security blocking public GET       | Check `/error` is in `permitAll` in `SecurityConfig` |
| 404    | Wrong URL or app not started       | Verify app is running on port 8080       |
| 500    | Service/DB error on startup        | Check app logs for stack trace           |

---

## TEST-3 · POST Create Product

**Purpose:** Create a product and confirm it is persisted to the database.

| Field          | Value                                |
|----------------|--------------------------------------|
| Method         | `POST`                               |
| URL            | `http://localhost:8080/api/products` |
| Auth           | Basic Auth (see above)               |
| Content-Type   | `application/json`                   |

**Steps:**

1. Create a new request. Set method to `POST`.
2. Enter URL `http://localhost:8080/api/products`.
3. Go to **Authorization** tab → `Basic Auth` → enter credentials.
4. Go to **Headers** tab → add:
   - Key: `Content-Type`
   - Value: `application/json`
5. Go to **Body** tab → select **raw** → select **JSON** from the dropdown.
6. Paste the following body:

```json
{
  "name": "Wild Lens",
  "description": "Premium wildlife photography lens",
  "price": 500,
  "stock": 10,
  "sellerId": 5
}
```

7. Click **Send**.

**Expected result:**

```
Status: 201 Created
Body:
{
  "id": 1,
  "name": "Wild Lens",
  "description": "Premium wildlife photography lens",
  "price": 500.00,
  "stock": 10
}
```

> Note the `id` value in the response — you will need it for TEST-4, TEST-5, and TEST-6.
> The `seller` field is intentionally absent (`@JsonIgnore` prevents circular serialization).

**Failure diagnosis:**

| Status | Body                        | Cause                          | Fix                                        |
|--------|-----------------------------|--------------------------------|--------------------------------------------|
| 401    | —                           | Missing or wrong credentials   | Re-check Basic Auth username/password      |
| 403    | —                           | User is not SELLER or ADMIN    | Use `seller@test.com` (id=5, role=SELLER)  |
| 400    | `"name": "must not be blank"` | `@Valid` rejected the DTO    | Supply all required fields                 |
| 500    | `"Seller not found"`        | `sellerId` doesn't exist in DB | Use `sellerId: 5` (confirmed in users table) |

**Database verification:**

```sql
SELECT * FROM products;
```

You should see one row with the product you just created.

---

## TEST-4 · GET Product by ID

**Purpose:** Fetch a single product by the `id` returned from TEST-3.

| Field  | Value                                      |
|--------|--------------------------------------------|
| Method | `GET`                                      |
| URL    | `http://localhost:8080/api/products/{id}`  |
| Auth   | None                                       |
| Body   | None                                       |

Replace `{id}` with the actual `id` from TEST-3 (e.g. `1`).

**Steps:**

1. Create a new request. Set method to `GET`.
2. Enter URL `http://localhost:8080/api/products/1` (replace `1` with your id).
3. Click **Send**.

**Expected result:**

```
Status: 200 OK
Body:
{
  "id": 1,
  "name": "Wild Lens",
  "description": "Premium wildlife photography lens",
  "price": 500.00,
  "stock": 10
}
```

**Failure diagnosis:**

| Status | Cause                              | Fix                                              |
|--------|------------------------------------|--------------------------------------------------|
| 500    | Product with that id doesn't exist | Run TEST-3 first, use the id from its response   |
| 404    | Wrong URL path                     | Confirm path is `/api/products/{id}` not `/products/{id}` |

---

## TEST-5 · PUT Update Product

**Purpose:** Update an existing product's name, price, description, and stock.

| Field          | Value                                        |
|----------------|----------------------------------------------|
| Method         | `PUT`                                        |
| URL            | `http://localhost:8080/api/products/{id}`    |
| Auth           | Basic Auth (see above)                       |
| Content-Type   | `application/json`                           |

Replace `{id}` with the same id from TEST-3.

**Steps:**

1. Create a new request. Set method to `PUT`.
2. Enter URL `http://localhost:8080/api/products/1`.
3. Add **Authorization** → `Basic Auth` → enter credentials.
4. Add **Header**: `Content-Type: application/json`.
5. Set **Body** → raw → JSON:

```json
{
  "name": "Updated Lens",
  "description": "Updated description for the lens",
  "price": 600,
  "stock": 8,
  "sellerId": 5
}
```

6. Click **Send**.

**Expected result:**

```
Status: 200 OK
Body:
{
  "id": 1,
  "name": "Updated Lens",
  "description": "Updated description for the lens",
  "price": 600.00,
  "stock": 8
}
```

**Database verification:**

```sql
SELECT * FROM products WHERE id = 1;
```

The `name`, `price`, `description`, and `stock` columns should reflect the updated values.

---

## TEST-6 · DELETE Product

**Purpose:** Delete a product and confirm it no longer exists.

| Field  | Value                                        |
|--------|----------------------------------------------|
| Method | `DELETE`                                     |
| URL    | `http://localhost:8080/api/products/{id}`    |
| Auth   | Basic Auth (see above)                       |
| Body   | None                                         |

**Steps:**

1. Create a new request. Set method to `DELETE`.
2. Enter URL `http://localhost:8080/api/products/1`.
3. Add **Authorization** → `Basic Auth` → enter credentials.
4. Click **Send**.

**Expected result:**

```
Status: 204 No Content
Body:   (empty)
```

**Confirm deletion — repeat TEST-4:**

Send `GET http://localhost:8080/api/products/1` again.

Expected: `404 Not Found` (GlobalExceptionHandler catches `ResourceNotFoundException`).

```json
{ "error": "Product not found with id: 1" }
```

**Database verification:**

```sql
SELECT * FROM products;
```

The table should be empty (or missing the deleted row).

---

---

## Order Tests

> **Dependency:** Run TEST-3 first to create a product. You will need the returned `productId` to place an order.

---

## TEST-7 · POST Create Order (BUYER places an order)

**Purpose:** Place a new order as a buyer. The buyer identity is taken from the auth principal, not the request body.

| Field        | Value                              |
|--------------|------------------------------------|
| Method       | `POST`                             |
| URL          | `http://localhost:8080/api/orders` |
| Auth         | Basic Auth — **BUYER** account     |
| Content-Type | `application/json`                 |

**Steps:**

1. Create a new request. Set method to `POST`.
2. Enter URL `http://localhost:8080/api/orders`.
3. Go to **Authorization** tab → `Basic Auth`:
   - Username: `buyer@test.com`
  - Password: `<configured-password>`
4. Go to **Headers** tab → add `Content-Type: application/json`.
5. Go to **Body** tab → raw → JSON:

```json
{
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

> Replace `productId: 1` with the actual id returned from TEST-3.

6. Click **Send**.

**Expected result:**

```
Status: 201 Created
Body:
{
  "id": 1,
  "orderDate": "2026-03-04T...",
  "totalPrice": 1000.00,
  "status": "PENDING",
  "items": [
    {
      "id": 1,
      "quantity": 2,
      "unitPrice": 500.00,
      "product": { "id": 1, "name": "Wild Lens", ... }
    }
  ]
}
```

> `totalPrice` = quantity × unitPrice (price is snapshotted at order time).
> `buyer` field is absent (`@JsonIgnore` prevents circular serialization).
> Note the order `id` — you will need it for TEST-9 and TEST-11.

**Failure diagnosis:**

| Status | Cause                                       | Fix                                                   |
|--------|---------------------------------------------|-------------------------------------------------------|
| 401    | Missing or wrong credentials                | Re-check Basic Auth — use `buyer@test.com`            |
| 403    | Using a SELLER account to place order       | Only BUYER and ADMIN can place orders                 |
| 400    | `"items": "Order must contain at least one item"` | Add at least one item to the `items` array      |
| 404    | `"Product not found with id: ..."`          | Run TEST-3 first; use the `id` from its response      |

**Database verification:**

```sql
SELECT o.id, o.status, o.total_price, o.buyer_id FROM orders o ORDER BY o.id;
SELECT * FROM order_items;
```

You should see one order row and one order_item row.

---

## TEST-8 · GET My Orders (BUYER views own orders)

**Purpose:** A buyer retrieves only their own orders.

| Field  | Value                                 |
|--------|---------------------------------------|
| Method | `GET`                                 |
| URL    | `http://localhost:8080/api/orders/my` |
| Auth   | Basic Auth — **BUYER** account        |
| Body   | None                                  |

**Steps:**

1. Create a new request. Set method to `GET`.
2. Enter URL `http://localhost:8080/api/orders/my`.
3. Add **Authorization** → Basic Auth → `buyer@test.com` / `<configured-password>`.
4. Click **Send**.

**Expected result:**

```
Status: 200 OK
Body:   [ { "id": 1, "status": "PENDING", "totalPrice": 1000.00, "items": [...] } ]
```

**Failure diagnosis:**

| Status | Cause                   | Fix                                     |
|--------|-------------------------|-----------------------------------------|
| 401    | Missing auth            | Add Basic Auth credentials              |
| 200    | Empty array `[]`        | Run TEST-7 first with this buyer account |

---

## TEST-9 · GET Order by ID

**Purpose:** Fetch a single order by id. Any authenticated user can call this.

| Field  | Value                                       |
|--------|---------------------------------------------|
| Method | `GET`                                       |
| URL    | `http://localhost:8080/api/orders/{id}`     |
| Auth   | Basic Auth — any account                    |
| Body   | None                                        |

**Steps:**

1. Create a new request. Set method to `GET`.
2. Enter URL `http://localhost:8080/api/orders/1` (replace `1` with id from TEST-7).
3. Add **Authorization** → Basic Auth → any valid account.
4. Click **Send**.

**Expected result:**

```
Status: 200 OK
Body:   { "id": 1, "status": "PENDING", "totalPrice": 1000.00, "items": [...] }
```

**Failure diagnosis:**

| Status | Cause                                 | Fix                                        |
|--------|---------------------------------------|--------------------------------------------|
| 404    | `"Order not found with id: ..."`      | Use the id returned by TEST-7              |
| 401    | No credentials                        | Add Basic Auth                             |

---

## TEST-10 · GET All Orders (ADMIN only)

**Purpose:** Admin retrieves a full platform-wide view of all orders.

| Field  | Value                                 |
|--------|---------------------------------------|
| Method | `GET`                                 |
| URL    | `http://localhost:8080/api/orders`    |
| Auth   | Basic Auth — **ADMIN** account        |
| Body   | None                                  |

**Steps:**

1. Create a new request. Set method to `GET`.
2. Enter URL `http://localhost:8080/api/orders`.
3. Add **Authorization** → Basic Auth:
   - Username: `admin@test.com`
  - Password: `<configured-password>`
4. Click **Send**.

**Expected result:**

```
Status: 200 OK
Body:   [ { "id": 1, "status": "PENDING", ... } ]
```

**Failure diagnosis:**

| Status | Cause                            | Fix                                              |
|--------|----------------------------------|--------------------------------------------------|
| 403    | Using BUYER or SELLER account    | Only ADMIN can view all orders                   |
| 401    | Missing credentials              | Add Basic Auth with admin@test.com               |

---

## TEST-11 · DELETE Order (ADMIN cancels an order)

**Purpose:** Admin removes an order (e.g. fraudulent or problematic). All order items are deleted by cascade.

| Field  | Value                                       |
|--------|---------------------------------------------|
| Method | `DELETE`                                    |
| URL    | `http://localhost:8080/api/orders/{id}`     |
| Auth   | Basic Auth — **ADMIN** account              |
| Body   | None                                        |

**Steps:**

1. Create a new request. Set method to `DELETE`.
2. Enter URL `http://localhost:8080/api/orders/1`.
3. Add **Authorization** → Basic Auth → `admin@test.com` / `<configured-password>`.
4. Click **Send**.

**Expected result:**

```
Status: 204 No Content
Body:   (empty)
```

**Confirm deletion — repeat TEST-9:**

Send `GET http://localhost:8080/api/orders/1` again.

Expected: `404 Not Found`
```json
{ "error": "Order not found with id: 1" }
```

**Database verification:**

```sql
SELECT * FROM orders;
SELECT * FROM order_items;
```

Both tables should be empty (cascade delete removes items with the order).

---

## Full Test Sequence Summary

Run in this order — products must exist before orders can be placed.

### Product Tests

| Step | Test    | Method   | URL                          | Account  | Expected           |
|------|---------|----------|------------------------------|----------|--------------------|
| 1    | TEST-2  | `GET`    | `/api/products`              | None     | 200 `[]`           |
| 2    | TEST-3  | `POST`   | `/api/products`              | SELLER   | 201 + product JSON |
| 3    | TEST-2  | `GET`    | `/api/products`              | None     | 200 `[{...}]`      |
| 4    | TEST-4  | `GET`    | `/api/products/{id}`         | None     | 200 + JSON         |
| 5    | TEST-5  | `PUT`    | `/api/products/{id}`         | SELLER   | 200 + updated JSON |
| 6    | TEST-6  | `DELETE` | `/api/products/{id}`         | ADMIN    | 204                |
| 7    | TEST-4  | `GET`    | `/api/products/{id}`         | None     | 404 (deleted)      |

### Order Tests

> Re-run TEST-3 to have a product available before placing orders.

| Step | Test    | Method   | URL                          | Account  | Expected           |
|------|---------|----------|------------------------------|----------|--------------------|
| 1    | TEST-7  | `POST`   | `/api/orders`                | BUYER    | 201 + order JSON   |
| 2    | TEST-8  | `GET`    | `/api/orders/my`             | BUYER    | 200 `[{...}]`      |
| 3    | TEST-9  | `GET`    | `/api/orders/{id}`           | BUYER    | 200 + JSON         |
| 4    | TEST-10 | `GET`    | `/api/orders`                | ADMIN    | 200 all orders     |
| 5    | TEST-11 | `DELETE` | `/api/orders/{id}`           | ADMIN    | 204                |
| 6    | TEST-9  | `GET`    | `/api/orders/{id}`           | BUYER    | 404 (deleted)      |

---

## Role Permission Summary

| Action                        | BUYER | SELLER | ADMIN |
|-------------------------------|-------|--------|-------|
| Browse products (GET)         | ✅    | ✅     | ✅    |
| Add product (POST)            | ❌    | ✅     | ✅    |
| Update product (PUT)          | ❌    | ✅     | ✅    |
| Delete product (DELETE)       | ❌    | ✅     | ✅    |
| Place order (POST)            | ✅    | ❌     | ✅    |
| View own orders (GET /my)     | ✅    | ✅     | ✅    |
| View any order by ID          | ✅    | ✅     | ✅    |
| View all orders (GET)         | ❌    | ❌     | ✅    |
| Delete order (DELETE)         | ❌    | ❌     | ✅    |

---

## Security Check

| Scenario                                        | Expected | Meaning                           |
|-------------------------------------------------|----------|-----------------------------------|
| POST `/api/products` without auth               | `401`    | Security working correctly        |
| POST `/api/products` with wrong password        | `401`    | Security working correctly        |
| POST `/api/products` with BUYER account         | `403`    | Role enforcement working          |
| POST `/api/orders` with SELLER account          | `403`    | Sellers cannot place orders       |
| POST `/api/orders` without auth                 | `401`    | Security working correctly        |
| GET `/api/orders` with BUYER or SELLER account  | `403`    | Only ADMIN sees all orders        |
| GET `/api/products` without auth                | `200`    | Products are publicly browsable   |
| GET `/api/orders/my` without auth               | `401`    | Order history requires login      |
