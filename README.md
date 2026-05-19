# Banking System

Учебная банковская система на Java и Spring Boot, собранная как multi-module Maven project.

## Modules

- `core-service`: счета, переводы, история операций и начисление процентов.
- `currencies-service`: курсы валют и конвертация через PostgreSQL.
- `common`: общие DTO, enums, events и exceptions.

## Infrastructure

PostgreSQL:

- `core_db` для `core-service`;
- `currencies_db` для `currencies-service`.

Redis используется только в `core-service`:

- Spring Session для `core-service`, namespace `banking:core:sessions`.

`currencies-service` хранит и читает курсы валют из PostgreSQL. Внешний Frankfurter API не используется: курсы задаются вручную через endpoint сервиса.

Kafka используется просто, через Spring Boot auto-configuration, только для внутренних бизнес-событий `core-service`:

- `TransactionEventProducer` отправляет события банковских операций в `transaction-events`;
- `InterestEventProducer` отправляет события начисления процентов в `interest-events`;
- `NotificationConsumer` читает события через `@KafkaListener`.

Kafka broker запускается через `docker-compose`.

## Currencies

`currencies-service` работает как простой сервис курсов валют на PostgreSQL.

Задать или обновить курс вручную:

```http
PUT http://localhost:8081/api/currencies/rates
Content-Type: application/json

{
  "baseCurrency": "USD",
  "targetCurrency": "RUB",
  "rate": 90.00
}
```

После этого можно читать курс и выполнять конвертацию:

```http
GET http://localhost:8081/api/currencies/rate?from=USD&to=RUB
GET http://localhost:8081/api/currencies/convert?from=USD&to=RUB&amount=10
```

В учебном проекте endpoint `PUT /api/currencies/rates` открыт. В production такой endpoint должен быть admin-only.

## Auth

`core-service` использует session auth через Spring Security:

- backend не выпускает JWT;
- внешний SSO/IdP не используется;
- логин обрабатывается стандартным `formLogin`;
- сессия хранится в Redis через Spring Session;
- браузер или Postman хранит `JSESSIONID`;
- CSRF token выдаётся cookie `XSRF-TOKEN`, отправлять его нужно header `X-XSRF-TOKEN`.

Endpoints:

- `GET /api/auth/me`: выдаёт `XSRF-TOKEN` и возвращает состояние текущей сессии;
- `POST /api/auth/registration`: регистрирует пользователя;
- `POST /api/auth/login`: стандартный Spring Security login, body `x-www-form-urlencoded`;
- `POST /api/auth/logout`: завершает сессию.

Для login через Postman:

```text
Content-Type: application/x-www-form-urlencoded

email=user@example.com
password=password123
```

Перед `POST`, `PUT` и `DELETE`:

1. Выполнить `GET http://localhost:8080/api/auth/me`.
2. Взять cookie `XSRF-TOKEN`.
3. Отправить header `X-XSRF-TOKEN: <token>` вместе с cookie `JSESSIONID`, если endpoint требует сессию.

### Postman session smoke flow

Primary HTTP smoke testing is done through Postman. Swagger/OpenAPI is kept as API documentation only.

Keep Postman cookies enabled. For every `POST` below send the current `XSRF-TOKEN`
cookie value as `X-XSRF-TOKEN`.

1. Get CSRF cookie:

```http
GET http://localhost:8080/api/auth/me
```

2. Register:

```http
POST http://localhost:8080/api/auth/registration
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

3. Login:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/x-www-form-urlencoded

email=user@example.com
password=password123
```

4. Create an account:

```http
POST http://localhost:8080/api/accounts
Content-Type: application/json

{
  "currency": "USD",
  "type": "CHECKING"
}
```

5. Deposit:

```http
POST http://localhost:8080/api/accounts/{id}/deposit
Content-Type: application/json

{
  "amount": 100.00
}
```

6. Withdraw:

```http
POST http://localhost:8080/api/accounts/{id}/withdraw
Content-Type: application/json

{
  "amount": 50.00
}
```

7. Transfer:

```http
POST http://localhost:8080/api/transfers
Content-Type: application/json

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 10.00,
  "currency": "USD"
}
```

8. History:

```http
GET http://localhost:8080/api/transfers/history?accountId={id}
```

## Docker

```powershell
docker compose down
docker compose up -d --build
docker compose ps
```

Ожидаемые сервисы:

- `postgres-core`;
- `postgres-currencies`;
- `redis`;
- `kafka`;
- `currencies-service`;
- `core-service`.

URLs:

- `core-service`: http://localhost:8080/swagger-ui.html
- `currencies-service`: http://localhost:8081/swagger-ui.html
- Kafka: `localhost:9092`
- Redis: `localhost:6379`
- Postgres core: `localhost:5433`
- Postgres currencies: `localhost:5434`

## Local Maven Run

`currencies-service`:

```powershell
.\mvnw.cmd -pl currencies-service spring-boot:run
```

`core-service`:

```powershell
.\mvnw.cmd -pl core-service spring-boot:run
```

## Checks

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package -DskipTests
```

## Environment

`.env` is local only and must not be committed. Use `.env.example` as a safe template without secrets.

Required variables:

- `CORE_DATABASE_URL`
- `CURRENCIES_DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `APP_DATABASE_SCHEMA`
- `REDIS_HOST`
- `REDIS_PORT`
- `BOOTSTRAP_SERVERS`
- `KAFKA_LISTENER_AUTO_STARTUP`
- `CURRENCIES_SERVICE_URL`
- `CORS_ALLOWED_ORIGINS`
- `SCHEDULING_ENABLED` для `core-service` interest accrual scheduler
