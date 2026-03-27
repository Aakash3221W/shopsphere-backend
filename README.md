# ShopSphere 🛒
<<<<<<< HEAD
ShopSphere is a high-performance, scalable e-commerce microservices platform built on Spring Boot 3 and Spring Cloud. It features an event-driven architecture, distributed tracing, centralized logging, and a robust observability stack.

## 🏗️ Architecture Overview

ShopSphere follows a modern microservices architecture designed for scalability, resilience, and maintainability. The system combines **synchronous communication (REST via Feign)** and **asynchronous event-driven communication (Kafka)** to balance consistency and performance.

### 🧩 High-Level Architecture Map

```mermaid
flowchart TD
    %% Custom Styles
    classDef client fill:#f8fafc,stroke:#cbd5e1,stroke-width:2px,color:#0f172a
    classDef gateway fill:#0284c7,stroke:#0369a1,stroke-width:2px,color:#fff
    classDef infra fill:#64748b,stroke:#475569,stroke-width:2px,color:#fff
    classDef service fill:#10b981,stroke:#059669,stroke-width:2px,color:#fff
    classDef data fill:#3b82f6,stroke:#2563eb,stroke-width:2px,color:#fff
    classDef messaging fill:#8b5cf6,stroke:#7c3aed,stroke-width:2px,color:#fff
    classDef monitor fill:#f59e0b,stroke:#d97706,stroke-width:2px,color:#fff

    Clients([External Clients / Web / Postman]):::client

    subgraph Entry [Edge & Discovery Layer]
        Gateway[API Gateway\n:8080]:::gateway
        Eureka[Eureka Registry\n:8761]:::infra
        Config[Config Server\n:8888]:::infra
    end

    subgraph Services [Core Business Services]
        Auth[Auth Service\n:8081]:::service
        Catalog[Catalog Service\n:8082]:::service
        Order[Order Service\n:8083]:::service
        Admin[Admin Service\n:8084]:::service
    end

    subgraph DataMesh [Data & Event Mesh]
        DB[(PostgreSQL\nDatabases)]:::data
        Redis[(Redis\nCache)]:::data
        Kafka{Apache Kafka\nSagas}:::messaging
    end

    subgraph Obs [Observability Stack]
        Zipkin[Zipkin\nTraces]:::monitor
        Prometheus[Prometheus\nMetrics]:::monitor
        Loki[Loki\nLogs]:::monitor
    end

    %% Routing Flow
    Clients -->|JWT / REST| Gateway
    Gateway -->|Route| Auth
    Gateway -->|Route| Catalog
    Gateway -->|Route| Order
    Gateway -->|Route| Admin

    %% Infra Links
    Gateway -.->|Discover| Eureka
    Services -.->|Register| Eureka
    Services -.->|Fetch| Config

    %% Data Flow
    Auth --> DB
    Catalog --> DB
    Order --> DB
    Admin --> DB

    Catalog -.->|Cache| Redis
    Order -.->|Cart State| Redis

    %% Async Kafka Flow
    Order ===>|order.placed / failed| Kafka
    Kafka ===>|Consume Event| Catalog

    %% Observability flow (Simplified to avoid clutter)
    Services -.-> Obs
  ```

=======

ShopSphere is a high-performance, scalable e-commerce microservices platform built on Spring Boot 3 and Spring Cloud. It features an event-driven architecture, distributed tracing, centralized logging, and a robust observability stack.

## 🏗️ Detailed Architecture Overview

ShopSphere is built on a distributed microservices model, emphasizing asynchronous event-driven patterns for consistency and synchronous Feign clients for management.

```mermaid
flowchart TD
    subgraph Clients ["🌐 External Access"]
        P[Postman / Browser]
    end

    subgraph Gateway ["🚪 API Gateway (8080)"]
        G[Spring Cloud Gateway]
        SEC[JWT Validation]
        RT[Dynamic Routing]
        G --- SEC --- RT
    end

    subgraph Auth ["🔐 Auth Service (8081)"]
        AS[Auth Controller]
        AD[(Auth DB)]
        AS --- AD
    end

    subgraph Catalog ["📦 Catalog Service (8082)"]
        CS[Catalog Controller]
        INV[Inventory Manager]
        CD[(Catalog DB)]
        CS --- INV --- CD
    end

    subgraph Order ["🛒 Order Service (8083)"]
        OS[Order Controller]
        CRT[Cart Manager]
        CHK[Checkout Flow]
        OD[(Order DB)]
        OS --- CRT --- CHK --- OD
    end

    subgraph Admin ["⚙️ Admin Service (8084)"]
        ADC[Admin Dashboard]
        ADD[(Admin DB)]
        ADC --- ADD
    end

    subgraph Messaging ["🐝 Event Bus (Kafka)"]
        K1[Topic: order.placed]
        K2[Topic: order.failed]
        K3[Topic: order.cancelled]
    end

    subgraph Infra ["🏗️ Infrastructure & Discovery"]
        E[Eureka Registry :8761]
        CFG[Config Server :8888]
        R[(Redis Cache)]
    end

    subgraph Observability ["📊 Monitoring & Tracing"]
        Z[Zipkin Traces]
        L[Loki Logs]
        PR[Prometheus Metrics]
        GF[Grafana Dashboards]
    end

    %% Client to Gateway
    P -->|HTTPS / JWT| G

    %% Gateway to Services
    RT -->|/auth/**| AS
    RT -->|/catalog/**| CS
    RT -->|/orders/**| OS
    RT -->|/admin/**| ADC

    %% Synchronous Inter-Service (Feign)
    ADC -.->|Feign| AS
    ADC -.->|Feign| CS
    ADC -.->|Feign| OS
    OS -.->|Feign: Price Lookup| CS

    %% Asynchronous Events (Kafka)
    CHK -- "1. OrderPlaced" --> K1
    K1 -- "2. Deduct Stock" --> INV
    INV -- "3. If Fail: OrderFailed" --> K2
    K2 -- "4. Rollback Status" --> OS
    OS -- "5. OrderCancelled" --> K3
    K3 -- "6. Restore Stock" --> INV

    %% Infra Discovery & Config
    AS & CS & OS & ADC <-->|Register & Discover| E
    AS & CS & OS & ADC <-->|Fetch Config| CFG
    OS & AS <-->|Session / Cart| R

    %% Observability Flow
    AS & CS & OS & ADC & G -->|Telemetry| Z & L & PR
    Z & L & PR --> GF
```
>>>>>>> 0a1129c (Complete Project)

### Key Architectural Pillars
1. **API Gateway (Spring Cloud Gateway)**: Acts as the single entry point. It handles JWT authentication, strips path prefixes, and routes requests dynamically via Eureka discovery.
2. **Database per Service**: Each microservice manages its own PostgreSQL database, ensuring loose coupling and data encapsulation.
3. **Event-Driven Consistency**: Uses Kafka to handle the "Order-Inventory" saga. When an order is placed, a message is sent to the `order.placed` topic, which the Catalog service consumes to reduce stock.
4. **Service Autonomy**: Services like `order-service` can function (e.g., adding to cart) even if others are temporarily slow, using Redis for high-speed cart storage.
5. **Observability**: A full "LGTM" stack (Loki, Grafana, Tracing via Zipkin, Metrics via Micrometer/Prometheus) provides deep visibility into the distributed system.
- **Service Discovery (Eureka)**: Registry for dynamic service registration and discovery.
- **Configuration Management (Config Server)**: Centralized configuration using a Git-based `config-repo`.
- **Auth Service**: Identity & Access Management using JWT.
- **Catalog Service**: Manages products, categories, search, and inventory levels.
- **Order Service**: Handles shopping carts, multi-step checkout, and order lifecycle.
- **Admin Service**: High-level administrative operations and user management.
- **Infrastructure**: Event-driven communication with Kafka, caching with Redis, and PostgreSQL for persistent storage.
<<<<<<< HEAD
## 🛠️ Tech Stack
=======

## 🛠️ Tech Stack

>>>>>>> 0a1129c (Complete Project)
- **Framework**: Spring Boot 3.2.5, Spring Cloud 2023.0.1
- **Language**: Java 17
- **Database**: PostgreSQL 16
- **Messaging**: Apache Kafka 3.7.0 (KRaft mode)
- **Caching**: Redis 7
- **Security**: Spring Security, JJWT (JSON Web Token)
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Resilience**: Resilience4j (Circuit Breaker, Retry, Rate Limiter)
- **Monitoring & Observability**:
  - **Tracing**: Micrometer Tracing with Zipkin
  - **Logging**: Grafana Loki
  - **Metrics**: Prometheus & Grafana
- **Testing & Quality**: JUnit 5, Mockito, JaCoCo, SonarQube
- **DevOps**: Docker & Docker Compose
<<<<<<< HEAD
## 🚀 Getting Started
### Prerequisites
- **Java 17** or higher
- **Maven 3.8+**
- **Docker & Docker Compose**
### Quick Start with Docker
The entire ecosystem can be launched with a single command:
```bash
docker-compose up -d
```
This starts all infrastructure (Postgres, Kafka, Redis, Zipkin, Loki, Grafana, Prometheus) and all microservices.
### Manual Setup (Development)
=======

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Docker & Docker Compose**

### Quick Start with Docker

The entire ecosystem can be launched with a single command:

```bash
docker-compose up -d
```

This starts all infrastructure (Postgres, Kafka, Redis, Zipkin, Loki, Grafana, Prometheus) and all microservices.

### Manual Setup (Development)

>>>>>>> 0a1129c (Complete Project)
1. **Build the project**:
   ```bash
   mvn clean install
   ```
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
2. **Start Infrastructure**:
   Ensure PostgreSQL, Kafka, and Redis are running. You can use the `docker-compose.yml` to start only the infra:
   ```bash
   docker-compose up -d postgres kafka redis zipkin loki grafana prometheus
   ```
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
3. **Start Services in order**:
   1. `eureka-server` (8761)
   2. `config-server` (8888)
   3. `auth-service` (8081)
   4. `catalog-service` (8082)
   5. `order-service` (8083)
   6. `admin-service` (8084)
   7. `api-gateway` (8080)
<<<<<<< HEAD
## 📡 API Endpoints
### API Gateway (Port: 8080)
=======

## 📡 API Endpoints

### API Gateway (Port: 8080)

>>>>>>> 0a1129c (Complete Project)
| Service | Path Prefix | Port | Documentation |
|---------|-------------|------|---------------|
| **Auth** | `/auth/**` | 8081 | [Swagger](http://localhost:8081/swagger-ui.html) |
| **Catalog** | `/catalog/**` | 8082 | [Swagger](http://localhost:8082/swagger-ui.html) |
| **Order** | `/orders/**` | 8083 | [Swagger](http://localhost:8083/swagger-ui.html) |
| **Admin** | `/admin/**` | 8084 | [Swagger](http://localhost:8084/swagger-ui.html) |
<<<<<<< HEAD
### Key User Flows
=======

### Key User Flows

>>>>>>> 0a1129c (Complete Project)
- **Authentication**: `POST /auth/signup`, `POST /auth/login`
- **Browsing**: `GET /catalog/products`, `GET /catalog/products/{id}`
- **Cart**: `GET /orders/cart`, `POST /orders/cart/items`
- **Checkout**:
  1. `POST /orders/checkout/start`
  2. `POST /orders/checkout/address`
  3. `POST /orders/checkout/delivery`
  4. `POST /orders/place` (Finalizes payment & reduces stock)
<<<<<<< HEAD
## 📊 Observability & Monitoring
ShopSphere is pre-configured with a full observability stack:
=======

## 📊 Observability & Monitoring

ShopSphere is pre-configured with a full observability stack:

>>>>>>> 0a1129c (Complete Project)
- **Distributed Tracing**: [Zipkin UI](http://localhost:9411) - Track requests across services.
- **Centralized Logging**: [Grafana Loki](http://localhost:3000) - View aggregated logs from all containers.
- **Metrics**: [Prometheus](http://localhost:9090) & [Grafana Dashboard](http://localhost:3000).
- **Service Registry**: [Eureka Dashboard](http://localhost:8761) - Monitor service health and instances.
<<<<<<< HEAD
## 🛡️ Security & Microservices Communication
=======

## 🛡️ Security & Microservices Communication

>>>>>>> 0a1129c (Complete Project)
- **External Security**: Handled by `api-gateway` and `auth-service` using JWT.
- **Internal Security**: The gateway validates JWTs and propagates user context via custom headers:
  - `X-User-Id`, `X-User-Role`, `X-User-Email`.
- **Inter-Service Communication**: Done via **OpenFeign** for synchronous calls and **Kafka** for asynchronous events.
- **Fault Tolerance**: Resilience4j is used to prevent cascading failures.
<<<<<<< HEAD
## 🧪 Testing & Quality
- **Unit/Integration Tests**: `mvn test`
- **Code Coverage**: JaCoCo report generated during `mvn verify`.
- **Static Analysis**: Configured for SonarQube at `http://localhost:9000`.
---
## 🚀 API Testing Guide (Postman)
Use `http://localhost:8080` as the base URL and test everything through the gateway.
=======

## 🧪 Testing & Quality

- **Unit/Integration Tests**: `mvn test`
- **Code Coverage**: JaCoCo report generated during `mvn verify`.
- **Static Analysis**: Configured for SonarQube at `http://localhost:9000`.

---

## 🚀 API Testing Guide (Postman)

Use `http://localhost:8080` as the base URL and test everything through the gateway.

>>>>>>> 0a1129c (Complete Project)
### Postman Environment Setup
Create a Postman environment with the following variables:
- `baseUrl` = `http://localhost:8080`
- `adminEmail`, `adminPassword`, `customerEmail`, `customerPassword`
- `adminToken`, `customerToken`, `adminRefreshToken`
- `categoryId`, `productId`, `directProductId`, `cartItemId`, `order1Id`, `order2Id`
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
### Automatic Token Saving
For login requests, add this to the **Tests** tab in Postman:
```javascript
const res = pm.response.json();
if (res?.data?.accessToken) {
  // If it's an admin login, save to adminToken, else save to customerToken
  const body = JSON.parse(pm.request.body.raw);
  if (body.email.includes("admin")) {
    pm.environment.set("adminToken", res.data.accessToken);
    pm.environment.set("adminRefreshToken", res.data.refreshToken);
  } else {
    pm.environment.set("customerToken", res.data.accessToken);
  }
}
```
<<<<<<< HEAD
### Essential API Flow
=======

### Essential API Flow

>>>>>>> 0a1129c (Complete Project)
#### 1. Authentication
- **Signup Admin**: `POST {{baseUrl}}/gateway/auth/signup`
- **Signup Customer**: `POST {{baseUrl}}/gateway/auth/signup`
- **Login**: `POST {{baseUrl}}/gateway/auth/login` (Admin/Customer)
- **Me**: `GET {{baseUrl}}/gateway/auth/me` (Header: `Authorization: Bearer {{token}}`)
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
#### 2. Catalog Management
- **Create Category**: `POST {{baseUrl}}/gateway/catalog/admin/categories` (Admin Token)
- **Create Product**: `POST {{baseUrl}}/gateway/admin/products` (Admin Token)
- **Browse (Public)**: `GET {{baseUrl}}/gateway/catalog/products`
<<<<<<< HEAD
#### 3. Shopping Cart
- **Add to Cart**: `POST {{baseUrl}}/gateway/orders/cart/items` (Customer Token)
- **Get Cart**: `GET {{baseUrl}}/gateway/orders/cart`
=======

#### 3. Shopping Cart
- **Add to Cart**: `POST {{baseUrl}}/gateway/orders/cart/items` (Customer Token)
- **Get Cart**: `GET {{baseUrl}}/gateway/orders/cart`

>>>>>>> 0a1129c (Complete Project)
#### 4. Checkout Flow
1. **Start Checkout**: `POST {{baseUrl}}/gateway/orders/checkout/start`
2. **Set Address**: `POST {{baseUrl}}/gateway/orders/checkout/address?orderId={{orderId}}&address=...`
3. **Select Delivery**: `POST {{baseUrl}}/gateway/orders/checkout/delivery?orderId={{orderId}}`
4. **Place Order**: `POST {{baseUrl}}/gateway/orders/place?orderId={{orderId}}`
<<<<<<< HEAD
### Full Detailed Postman Flow
<details>
<summary>Click to expand full endpoint list</summary>
#### 1. Health
- **Check Health**: `GET {{baseUrl}}/actuator/health` (Expected: `status = UP`)
=======

### Full Detailed Postman Flow

<details>
<summary>Click to expand full endpoint list</summary>

#### 1. Health
- **Check Health**: `GET {{baseUrl}}/actuator/health` (Expected: `status = UP`)

>>>>>>> 0a1129c (Complete Project)
#### 2. Auth Endpoints
- **Signup Admin**: `POST {{baseUrl}}/gateway/auth/signup`
- **Signup Customer**: `POST {{baseUrl}}/gateway/auth/signup`
- **Login Admin**: `POST {{baseUrl}}/gateway/auth/login` (Saves `adminToken` & `adminRefreshToken`)
- **Login Customer**: `POST {{baseUrl}}/gateway/auth/login` (Saves `customerToken`)
- **Refresh Token**: `POST {{baseUrl}}/gateway/auth/refresh?refreshToken={{adminRefreshToken}}`
- **Current User**: `GET {{baseUrl}}/gateway/auth/me` (Bearer Token)
- **Change Password**: `PUT {{baseUrl}}/gateway/auth/changepassword`
- **Logout**: `POST {{baseUrl}}/gateway/auth/logout`
- **Admin Users List**: `GET {{baseUrl}}/gateway/auth/users` (Admin Only)
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
#### 3. Catalog Public Endpoints
- **Product List**: `GET {{baseUrl}}/gateway/catalog/products`
- **Product Detail**: `GET {{baseUrl}}/gateway/catalog/products/{{productId}}`
- **Featured Products**: `GET {{baseUrl}}/gateway/catalog/featured`
- **Categories List**: `GET {{baseUrl}}/gateway/catalog/categories`
- **Products by Category**: `GET {{baseUrl}}/gateway/catalog/categories/{{categoryId}}/products`
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
#### 4. Catalog Admin Endpoints
- **Create Category**: `POST {{baseUrl}}/gateway/catalog/admin/categories` (Admin Token)
- **Update Category**: `PUT {{baseUrl}}/gateway/catalog/admin/categories/{{categoryId}}`
- **Direct Product Create**: `POST {{baseUrl}}/gateway/catalog/admin/products`
- **Direct Product Update**: `PUT {{baseUrl}}/gateway/catalog/admin/products/{{directProductId}}`
- **Update Stock**: `PUT {{baseUrl}}/gateway/catalog/admin/products/{{directProductId}}/stock?quantity=18`
- **Delete Product**: `DELETE {{baseUrl}}/gateway/catalog/admin/products/{{directProductId}}`
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
#### 5. Admin-Service Product Endpoints
- **Create Product via Admin**: `POST {{baseUrl}}/gateway/admin/products`
- **Update Product via Admin**: `PUT {{baseUrl}}/gateway/admin/products/{{productId}}`
- **Update Stock via Admin**: `PUT {{baseUrl}}/gateway/admin/products/{{productId}}/stock?quantity=30`
- **Delete Product via Admin**: `DELETE {{baseUrl}}/gateway/admin/products/{{productId}}`
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
#### 6. Cart Endpoints (Customer Token)
- **Get Cart**: `GET {{baseUrl}}/gateway/orders/cart`
- **Add Item**: `POST {{baseUrl}}/gateway/orders/cart/items`
- **Update Item**: `PUT {{baseUrl}}/gateway/orders/cart/items/{{cartItemId}}`
- **Delete Item**: `DELETE {{baseUrl}}/gateway/orders/cart/items/{{cartItemId}}`
- **Clear Cart**: `DELETE {{baseUrl}}/gateway/orders/cart`
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
#### 7. Checkout & Order Flow
- **Start Checkout**: `POST {{baseUrl}}/gateway/orders/checkout/start` (Saves `order1Id`)
- **Set Address**: `POST {{baseUrl}}/gateway/orders/checkout/address?orderId={{order1Id}}&address=123 Main St`
- **Select Delivery**: `POST {{baseUrl}}/gateway/orders/checkout/delivery?orderId={{order1Id}}`
- **Place Order**: `POST {{baseUrl}}/gateway/orders/place?orderId={{order1Id}}`
<<<<<<< HEAD
- **My Orders**: `GET {{baseUrl}}/gateway/orders/my`## 🏗️ Architecture Overview

ShopSphere follows a **modern microservices architecture** designed for scalability, resilience, and maintainability. The system combines **synchronous communication (REST via Feign)** and **asynchronous event-driven communication (Kafka)** to balance consistency and performance.

---

### 🧩 High-Level Architecture

- **Single Entry Point** → API Gateway  
- **Service Registry** → Eureka (Service Discovery)  
- **Centralized Config** → Spring Cloud Config Server (Git-based)  
- **Communication**:
  - Sync → OpenFeign (REST)
  - Async → Kafka (Event-driven Saga)
- **Caching Layer** → Redis  
- **Database Pattern** → Database per Service  
- **Observability Stack** → Zipkin + Prometheus + Loki + Grafana  

---

### 🔁 Request Flow (End-to-End)

1. Client sends request → API Gateway  
2. Gateway:
   - Validates JWT
   - Routes request using Eureka
3. Request hits target microservice  
4. Service may:
   - Call other services (Feign)
   - Publish events (Kafka)
5. Observability tools capture logs, traces, and metrics  

---

### ⚙️ Core Components

#### 🚪 API Gateway
- Built with **Spring Cloud Gateway**
- Responsibilities:
  - Authentication (JWT validation)
  - Routing via service discovery
  - Rate limiting & filtering
- Acts as the **single entry point** for all clients

---

#### 🔐 Auth Service
- Handles **authentication & authorization**
- Generates and validates JWT tokens
- Stores user credentials securely

---

#### 📦 Catalog Service
- Manages:
  - Products
  - Categories
  - Inventory
- Listens to Kafka events to update stock

---

#### 🛒 Order Service
- Handles:
  - Cart management (via Redis)
  - Checkout workflow
  - Order lifecycle
- Initiates **event-driven saga** for order processing

---

#### ⚙️ Admin Service
- Provides:
  - Product management
  - Order monitoring
  - User administration
- Uses Feign for orchestration

---

### 🔄 Event-Driven Architecture (Saga Pattern)

ShopSphere uses a **Kafka-based Saga pattern** for distributed transactions:

1. `OrderPlaced` → emitted by Order Service  
2. Catalog Service consumes → deducts inventory  
3. If failure:
   - Emits `OrderFailed`
   - Order Service rolls back  
4. If cancelled:
   - Emits `OrderCancelled`
   - Inventory restored  

👉 Ensures **eventual consistency without tight coupling**

---

### 🧠 Key Design Principles

#### 1. Database per Service
- Each service owns its data
- No shared database
- Ensures **loose coupling**

---

#### 2. Service Autonomy
- Services can function independently
- Example:
  - Cart works even if Catalog is slow (Redis caching)

---

#### 3. Hybrid Communication Model
- **Feign (Sync)** → Immediate responses
- **Kafka (Async)** → Reliable background processing

---

#### 4. Scalability
- Services scale independently
- Kafka decouples load spikes

---

#### 5. Fault Tolerance
- Resilience4j:
  - Circuit Breaker
  - Retry
  - Rate Limiter

---

### 📊 Observability & Monitoring

| Component | Purpose |
|----------|--------|
| Zipkin | Distributed tracing |
| Loki | Centralized logging |
| Prometheus | Metrics collection |
| Grafana | Visualization |

👉 Provides **full system visibility (LGTM stack)**

---

### 🏗️ Infrastructure Layer

- **Eureka** → Service discovery  
- **Config Server** → Centralized config (GitHub repo)  
- **Redis** → Caching (cart/session)  
- **Kafka** → Event streaming  

---

### 🔐 Security Model

- **External Security**:
  - JWT via API Gateway
- **Internal Communication**:
  - User context propagated via headers:
    - `X-User-Id`
    - `X-User-Role`
    - `X-User-Email`

---

### 🧱 Why This Architecture?

✔ Scalable → Microservices + Kafka  
✔ Resilient → Circuit breakers + async fallback  
✔ Maintainable → Clear separation of concerns  
✔ Production-ready → Observability + Config + Discovery  

---

- **Order Detail**: `GET {{baseUrl}}/gateway/orders/{{order1Id}}`
- **Cancel Order**: `POST {{baseUrl}}/gateway/orders/{{order1Id}}/cancel`
=======
- **My Orders**: `GET {{baseUrl}}/gateway/orders/my`
- **Order Detail**: `GET {{baseUrl}}/gateway/orders/{{order1Id}}`
- **Cancel Order**: `POST {{baseUrl}}/gateway/orders/{{order1Id}}/cancel`

>>>>>>> 0a1129c (Complete Project)
#### 8. Order Admin Endpoints (Admin Token)
- **Order List**: `GET {{baseUrl}}/gateway/orders/admin/orders`
- **Order Detail**: `GET {{baseUrl}}/gateway/orders/admin/orders/{{order2Id}}`
- **Update Status**: `PUT {{baseUrl}}/gateway/orders/admin/orders/{{order2Id}}/status?status=PACKED`
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
#### 9. Admin Service Endpoints (Admin Token)
- **Dashboard**: `GET {{baseUrl}}/gateway/admin/dashboard`
- **Admin Orders List**: `GET {{baseUrl}}/gateway/admin/orders`
- **Admin Order Detail**: `GET {{baseUrl}}/gateway/admin/orders/{{order2Id}}`
- **Admin Update Status**: `PUT {{baseUrl}}/gateway/admin/orders/{{order2Id}}/status?status=SHIPPED`
- **Users List**: `GET {{baseUrl}}/gateway/admin/users`
<<<<<<< HEAD
</details>
=======

</details>

>>>>>>> 0a1129c (Complete Project)
### How To Validate Quickly
For each request, ensure:
1. **HTTP Status** is `200 OK` or `201 Created`.
2. **Success** field in response is `true`.
3. **Data** contains the expected object or ID.
4. **Auth Rules**: Admin-only endpoints reject customer tokens. Public endpoints work without tokens.
<<<<<<< HEAD
=======

>>>>>>> 0a1129c (Complete Project)
---
Developed by **Aakash** | ShopSphere E-commerce Platform
