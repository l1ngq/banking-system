# Banking System

Учебная банковская система на Java и Spring Boot, собранная как multi-module Maven project.

## Modules

- `core-service`: счета, переводы, история операций и начисление процентов.
- `currencies-service`: курсы валют, конвертация и Redis cache для курсов.
- `common`: общие DTO, enums, events и exceptions.

## Infrastructure

PostgreSQL:

- `core_db` для `core-service`;
- `currencies_db` для `currencies-service`.

Redis используется в двух местах:

- currencies cache в `currencies-service`;
- Spring Session для `core-service`, namespace `banking:core:sessions`.

Kafka используется для событий банковских операций и начисления процентов.

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
- `CACHE_TYPE`
- `BOOTSTRAP_SERVERS`
- `KAFKA_LISTENER_AUTO_STARTUP`
- `CURRENCIES_SERVICE_URL`
- `CORS_ALLOWED_ORIGINS`
- `SCHEDULING_ENABLED`
