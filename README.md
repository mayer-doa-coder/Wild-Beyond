# Wild Beyond

## Overview

Wild Beyond is a full-stack Spring Boot application that combines wildlife-focused content discovery with a role-based e-commerce workflow. The platform supports public users plus authenticated BUYER, SELLER, and ADMIN roles.

It delivers:

- Public content experience (Home, Blog, Explore, Wildlife Photography).
- Product marketplace (create/list/view/update/delete).
- Shopping flow (cart, checkout, orders).
- Role-based dashboards and administration.

## Features

### Feature Screenshots

#### Home and Public Experience

- Home hero section

![Home Hero](src/main/resources/static/images/Screenshot%202026-04-05%20230545.png)

- Latest stories and feature highlights

![Latest Stories](src/main/resources/static/images/Screenshot%202026-04-05%20230606.png)

- Wildlife categories (Explore)

![Explore Categories](src/main/resources/static/images/Screenshot%202026-04-05%20230554.png)

- Visual story blocks

![Visual Story Blocks](src/main/resources/static/images/Screenshot%202026-04-05%20230521.png)

#### Product Form UI

- Product creation form detail

![Product Form](src/main/resources/static/images/Screenshot%202026-04-05%20230330.png)

#### Marketplace and Product Management

- Product listing and in-page add-product form

![Marketplace Product Management](src/main/resources/static/images/Screenshot%202026-04-05%20230444.png)

#### Orders and Dashboards

- Seller dashboard

![Seller Dashboard](src/main/resources/static/images/Screenshot%202026-04-05%20230238.png)

- Orders page (buying vs seller product orders)

![Orders View](src/main/resources/static/images/Screenshot%202026-04-05%20230309.png)

### Public Experience Features

- Home page with dynamic featured stories and products.
- Blog page with internal content + external feed enrichment.
- Explore module by categories (animals, birds, ecosystems).
- Explore detail pages with dynamic image/content handling.
- Wildlife photography gallery view.
- About page.

### Marketplace Features

- Public product browsing and details.
- Product photo serving endpoint.
- Seller product creation with optional image upload.
- Seller/Admin product edit and delete.
- Validation and error feedback through flash messages.

### Buyer Workflow Features

- Add item to cart with quantity checks.
- Remove item from cart.
- Checkout from cart.
- Stock validation during checkout.
- Redirect to orders on successful checkout.
- Error handling for empty cart and insufficient stock.

### Order and Dashboard Features

- Buyer order history and order details.
- Seller selling-order visibility.
- Admin full order visibility.
- Role-based dashboard routing:
	- Admin dashboard
	- Seller dashboard
	- Buyer dashboard

### Security and Access Features

- Spring Security filter-chain based access control.
- Role-based route restrictions (BUYER/SELLER/ADMIN).
- Form login with email as principal.
- HTTP Basic support for API clients.
- CSRF protection (API route exclusions for client usability).

## Core Features (With Subfeatures)

### 1. Authentication and Authorization

- Register with role selection (BUYER/SELLER).
- Login/logout flows.
- BCrypt password hashing.
- Email-based user loading.
- URL-level + method-level authorization.

### 2. Product Management

- Create product.
- View all products.
- View single product details.
- Update product.
- Delete product.
- Seller ownership check + admin override.
- Product image handling.

### 3. Cart and Checkout

- Session-based cart storage.
- Add/remove cart lines.
- Checkout conversion from cart to order.
- Per-item stock check.
- Stock reduction on successful checkout.
- Cart clear on success.

### 4. Order Management

- Place order through API/MVC flow.
- Buyer own-order retrieval.
- Seller product-order retrieval.
- Admin all-orders retrieval.
- Order detail retrieval.
- Order deletion (admin restricted in REST).

### 5. Content and Explore Services

- Internal blog post listing/detail.
- External blog feed merge.
- Explore content by category + slug.
- Wildlife gallery generation.
- External media resolution with fallbacks.

### 6. Admin and User Management

- Admin user listing view.
- Role-based dashboard metrics.
- Startup role and admin seeding.

## Technology Stack

### Backend

- Java 17
- Spring Boot 4
- Spring Security
- Spring Data JPA (Hibernate)
- Spring Validation
- Spring Web MVC
- PostgreSQL driver
- Lombok

### Frontend

- Thymeleaf templates
- HTML/CSS with utility-class based styling
- Template fragments for reusable UI sections

### External APIs

- Guardian API
- NewsAPI
- Wikipedia Summary API

## Project Structure

```text
src/main/java/com/wildbeyond
	config/        -> security + app bootstrapping configuration
	controller/    -> MVC presentation layer
	controller/rest/ -> REST presentation layer
	service/       -> business logic layer
	repository/    -> persistence abstraction layer
	model/         -> domain entities
	dto/           -> request/response/view data contracts
	exception/     -> centralized error handling

src/main/resources
	templates/     -> Thymeleaf views
	static/        -> static assets
	application.yml -> environment-driven runtime config

src/test/java/com/wildbeyond
	controller/    -> controller tests
	service/       -> service tests
	exception/     -> exception handler tests
	repository/    -> repository tests
```

## Architecture and Design Diagrams

### Architecture Diagram

- ![Architecture Detailed](docs/diagrams/architectural-diagram-detailed.png)
- ![Architecture Simplified](docs/diagrams/architectural-diagram-simplified.png)

### DFD Diagram

- ![DFD Level 0](docs/diagrams/dfd-level-0-context.png)
- ![DFD Level 1](docs/diagrams/dfd-level-1.png)
- ![DFD Level 2 Checkout](docs/diagrams/dfd-level-2-checkout.png)
- ![DFD Level 2 Product Management](docs/diagrams/dfd-level-2-project-management.png)

### ER Diagram

- ![ER Diagram](docs/diagrams/er-diagram.png)

### Activity Diagram (Buyer + Seller)

- ![Buyer Activity Diagram](docs/diagrams/activity-diagram-buyer-flow.png)
- ![Seller Activity Diagram](docs/diagrams/activity-diagram-seller-flow.png)

## API Endpoints

### Auth Controller

- GET /auth/login
- GET /auth/register
- POST /auth/register
- POST /auth/logout (handled by Spring Security)

### Home / Content Controller

- GET /
- GET /home
- GET /index
- GET /blog
- GET /blog/{id}
- GET /explore
- GET /explore/{category}/{slug}
- GET /explore/wildlife-photography
- GET /about

### Product Controller (MVC)

- GET /products
- GET /products/{id}
- GET /products/{id}/photo
- GET /products/new
- GET /products/edit/{id}
- POST /products
- POST /products/edit/{id}
- GET /products/delete/{id}
- POST /products/{id}/delete
- POST /products/{id}/edit

### Cart Controller (MVC)

- GET /buyer/cart
- GET /seller/cart
- POST /buyer/cart/add/{productId}
- POST /seller/cart/add/{productId}
- POST /buyer/cart/remove/{productId}
- POST /seller/cart/remove/{productId}
- POST /buyer/cart/checkout
- POST /seller/cart/checkout

Compatibility redirects:

- /cart/** -> canonical role-scoped cart route

### Order Controller (MVC)

- GET /buyer/orders
- GET /seller/orders
- GET /admin/orders
- GET /buyer/orders/{id}
- GET /seller/orders/{id}
- GET /admin/orders/{id}
- POST /buyer/orders
- POST /seller/orders
- POST /admin/orders

Compatibility redirects:

- /orders/** -> canonical role-scoped orders route

### Dashboard Controller (MVC)

- GET /dashboard
- GET /admin
- GET /seller/dashboard
- GET /buyer/dashboard

### User Controller (MVC)

- GET /admin/users
- GET /admin/users/{id}
- POST /admin/users/{id}/edit
- POST /admin/users/{id}/delete

Compatibility redirects:

- /users/** -> /admin/users/**

### Product REST API

- GET /api/products
- GET /api/products/{id}
- POST /api/products
- PUT /api/products/{id}
- DELETE /api/products/{id}

### Order REST API

- POST /api/orders
- GET /api/orders/my
- GET /api/orders/{id}
- GET /api/orders
- DELETE /api/orders/{id}

### API Access Notes By Module

- Product write API: SELLER for create, SELLER/ADMIN for update/delete.
- Order create API: BUYER + SELLER (seller cannot order own products).
- Order admin list/delete APIs: ADMIN.

## Run Instructions

### Local Run (Maven)

Prerequisites:

- Java 17
- PostgreSQL instance

Run:

```bash
./mvnw.cmd spring-boot:run
```

App URL:

- http://localhost:8080

### Docker Run

1. Create env file from template:

```bash
cp .env.example .env
```

2. Start application + database:

```bash
docker compose up --build
```

3. Access:

- App: http://localhost:8180
- DB: localhost:5432

Stop:

```bash
docker compose down
```

Reset with volume removal:

```bash
docker compose down -v
```

### Render Run / Deployment

1. Create Render web service from repository (Docker deployment).
2. Attach PostgreSQL service.
3. Configure environment variables:
	 - DB_HOST
	 - DB_PORT
	 - DB_NAME
	 - DB_USERNAME
	 - DB_PASSWORD
	 - GUARDIAN_API_KEY (optional)
	 - NEWSAPI_KEY (optional)
4. Create deploy hook in Render.
5. Add GitHub secret RENDER_DEPLOY_HOOK.
6. Push to main branch for deployment trigger.

## CI/CD Explanation

Workflow file: .github/workflows/ci.yml

### CI Triggers

- Push: main, dev, feature/**
- Pull request: main, dev

### CI Build Pipeline

1. Checkout source
2. Setup Java 17 (Temurin)
3. Build (`./mvnw.cmd clean install -DskipTests`)
4. Test (`./mvnw.cmd test`)
5. Docker image build
6. Upload surefire reports on failure

### CD Deployment Pipeline

Deploy job runs only when:

- Event is push
- Branch is main
- Build job is successful

Deployment method:

- POST request to Render deploy hook from GitHub Action.

## Database Schema

Primary tables and purpose:

### users

- Stores account identity, credentials, status, and audit timestamps.

### roles

- Stores role names (ADMIN, SELLER, BUYER).

### user_roles

- Join table between users and roles (many-to-many mapping).

### products

- Product catalog with seller owner, pricing, stock, description, and optional image blob/content type.

### orders

- Parent order records with buyer, status, order date, and total price.

### order_items

- Child order lines linking order to products with quantity and unit price snapshot.

### blog_posts

- Internal editorial content with publish flag and created timestamp.

## Testing

This project includes controller, service, repository, exception, and context tests.

### Test Files Present

- src/test/java/com/wildbeyond/WildBeyondApplicationTests.java
- src/test/java/com/wildbeyond/controller/HomeControllerTest.java
- src/test/java/com/wildbeyond/controller/ProductControllerTest.java
- src/test/java/com/wildbeyond/controller/ProductRestControllerTest.java
- src/test/java/com/wildbeyond/controller/OrderControllerTest.java
- src/test/java/com/wildbeyond/controller/OrderRestControllerTest.java
- src/test/java/com/wildbeyond/controller/DashboardControllerTest.java
- src/test/java/com/wildbeyond/service/UserServiceTest.java
- src/test/java/com/wildbeyond/service/ProductServiceTest.java
- src/test/java/com/wildbeyond/service/OrderServiceTest.java
- src/test/java/com/wildbeyond/service/HomepageServiceTest.java
- src/test/java/com/wildbeyond/repository/HomepageRepositoryTest.java
- src/test/java/com/wildbeyond/exception/GlobalExceptionHandlerTest.java
- src/test/java/com/wildbeyond/exception/GlobalExceptionHandlerMvcTest.java

### How to Run Tests

```bash
./mvnw.cmd test
```

In CI, tests are executed automatically in GitHub Actions before deployment.

## CI/CD Pipeline

### Branch Strategy

- main: production-ready branch with deployment enabled.
- dev: integration branch.
- feature/**: isolated feature development.

### CI Triggers

- Push to main, dev, feature/**
- Pull requests targeting main and dev

### Build and Test Flow

1. Checkout repository.
2. Setup Java 17 + Maven cache.
3. Build project.
4. Run test suite.
5. Build Docker image.

### Conditional Deployment Rules

Deploy runs only when:

1. Event is push.
2. Branch is main.
3. Build job passes.

### Render Deployment

- Deployment is triggered by Render deploy hook.
- Required GitHub secret: RENDER_DEPLOY_HOOK.
- Deploy hook is not hardcoded in repository files.

### Validation Checklist

1. Push to feature branch: CI runs, deploy does not run.
2. PR to dev/main: CI runs, deploy does not run.
3. Merge to main: CI runs, then deploy runs.
4. Verify new revision in Render dashboard.

## Postman API Testing Information

### Base URL

- http://localhost:8080

### Authentication Method

- HTTP Basic Auth for protected endpoints.
- Public endpoint: GET /api/products (no auth required).

### Test Accounts Used

- BUYER: buyer@test.com / seller123
- SELLER: seller@test.com / seller123
- ADMIN: admin@test.com / seller123

### Prerequisites

1. Application is running.
2. Database is running.
3. At least one product exists before starting order tests.

### Full Test Cases (All)

#### Product API Tests

1. TEST-1
	- Method/URL: GET /api/products
	- Auth: None
	- Expected: 200

2. TEST-2
	- Method/URL: GET /api/products
	- Auth: None
	- Expected: 200

3. TEST-3
	- Method/URL: POST /api/products
	- Auth: SELLER
	- Expected: 201

4. TEST-4
	- Method/URL: GET /api/products/{id}
	- Auth: None
	- Expected: 200

5. TEST-5
	- Method/URL: PUT /api/products/{id}
	- Auth: SELLER
	- Expected: 200

6. TEST-6
	- Method/URL: DELETE /api/products/{id}
	- Auth: ADMIN
	- Expected: 204

#### Order API Tests

7. TEST-7
	- Method/URL: POST /api/orders
	- Auth: BUYER
	- Expected: 201

8. TEST-8
	- Method/URL: GET /api/orders/my
	- Auth: BUYER
	- Expected: 200

9. TEST-9
	- Method/URL: GET /api/orders/{id}
	- Auth: Authenticated user
	- Expected: 200

10. TEST-10
	- Method/URL: GET /api/orders
	- Auth: ADMIN
	- Expected: 200

11. TEST-11
	- Method/URL: DELETE /api/orders/{id}
	- Auth: ADMIN
	- Expected: 204

### Full Execution Sequence Used

#### Product Sequence

1. TEST-2 -> 200
2. TEST-2 -> 200
3. TEST-3 -> 201
4. TEST-2 -> 200 (contains created product)
5. TEST-4 -> 200
6. TEST-5 -> 200
7. TEST-6 -> 204
8. TEST-4 -> 404 (deleted item)

#### Order Sequence

1. Re-run TEST-3 to create a product dependency
2. TEST-7 -> 201
3. TEST-8 -> 200
4. TEST-9 -> 200
5. TEST-10 -> 200
6. TEST-11 -> 204
7. TEST-9 -> 404 (deleted order)

### Role Permission Summary (Postman-Verified)

- Browse products: public + all roles
- Create product: SELLER
- Update/delete product: SELLER/ADMIN
- Place order: BUYER + SELLER (seller own-product purchase blocked)
- View own orders: authenticated users
- View all orders: ADMIN
- Delete order: ADMIN

### Security Checks Performed

- POST /api/products without auth -> 401
- POST /api/products wrong credentials -> 401
- POST /api/products with BUYER role -> 403
- POST /api/orders with SELLER role (other seller product) -> 201
- POST /api/orders with SELLER role (own product) -> 403
- POST /api/orders without auth -> 401
- GET /api/orders with BUYER/SELLER role -> 403
- GET /api/products without auth -> 200
- GET /api/orders/my without auth -> 401
- Missing product/order id -> 404 with structured error response

## Project Links

- Repository: https://github.com/mayer-doa-coder/Wild-Beyond
- Live: https://wild-beyond.onrender.com
