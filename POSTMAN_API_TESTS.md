# Wild Beyond — REST API Test Guide (Postman)

Base URL: `http://localhost:8080`

---

## Prerequisites

1. Application is running (`App: Run` task is active).
2. Database is running (`DB: Start` task is active).
3. Postman is open.

---

## Authentication

Write endpoints (`POST`, `PUT`, `DELETE`) require a **SELLER** or **ADMIN** account.  
The app supports **HTTP Basic Auth** — no session cookie or token needed.

Use these credentials in every authenticated request:

| Field    | Value               |
|----------|---------------------|
| Username | `seller@test.com`   |
| Password | `seller123`         |
| User ID  | `5`                 |

**How to add Basic Auth in Postman:**

1. Open the request.
2. Click the **Authorization** tab.
3. Set **Type** → `Basic Auth`.
4. Enter `seller@test.com` in **Username**.
5. Enter `seller123` in **Password**.

Read-only endpoints (`GET`) are **public** — no auth needed.

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

Expected: `500` (no global exception handler yet — `RuntimeException: Product not found`).

**Database verification:**

```sql
SELECT * FROM products;
```

The table should be empty (or missing the deleted row).

---

## Full Test Sequence Summary

Run in this order to avoid dependency issues:

| Order | Test    | Method   | URL                          | Auth  | Expected |
|-------|---------|----------|------------------------------|-------|----------|
| 1     | TEST-2  | `GET`    | `/api/products`              | None  | 200 `[]` |
| 2     | TEST-3  | `POST`   | `/api/products`              | Basic | 201 + JSON |
| 3     | TEST-2  | `GET`    | `/api/products`              | None  | 200 `[{...}]` |
| 4     | TEST-4  | `GET`    | `/api/products/{id}`         | None  | 200 + JSON |
| 5     | TEST-5  | `PUT`    | `/api/products/{id}`         | Basic | 200 + updated JSON |
| 6     | TEST-6  | `DELETE` | `/api/products/{id}`         | Basic | 204 |
| 7     | TEST-4  | `GET`    | `/api/products/{id}`         | None  | 500 (deleted) |

---

## Security Check

| Scenario                            | Expected Status | Meaning                        |
|-------------------------------------|-----------------|--------------------------------|
| POST without Authorization header  | `401`           | Security is working correctly  |
| POST with wrong password            | `401`           | Security is working correctly  |
| POST with BUYER account credentials | `403`           | Role enforcement working       |
| GET without auth                    | `200`           | Public endpoint working        |
