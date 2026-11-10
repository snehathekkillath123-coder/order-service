# order-service

Part of the QuickBite online food ordering system. Owns orders. On order creation it calls
restaurant-service over REST to validate menu items and fetch prices.
Spring Boot 3 / Java 17 / PostgreSQL. Port 8082.

## API
| Method | Path | Type |
|---|---|---|
| POST | /api/orders | command |
| PUT | /api/orders/{id}/status?status=CONFIRMED | command |
| DELETE | /api/orders/{id} (cancel) | command |
| GET | /api/orders/{id} | query |
| GET | /api/orders?customerName=... | query |

Config: `RESTAURANT_SERVICE_URL` (default http://localhost:8081), `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Run locally
    docker run -d --name odb -e POSTGRES_DB=orderdb -e POSTGRES_PASSWORD=postgres -p 5433:5432 postgres:16-alpine
    DB_URL=jdbc:postgresql://localhost:5433/orderdb mvn spring-boot:run

## Docker / Minikube
Deploy restaurant-service first (it creates the `foodapp` namespace).

    eval $(minikube docker-env)
    docker build -t order-service:1.0.0 .
    kubectl apply -f k8s/
    minikube service order-service -n foodapp --url
