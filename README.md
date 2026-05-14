# Banking System

## Описание проекта

Учебная банковская система на Java и Spring Boot, собранная как multi-module Maven project.

- `core-service` отвечает за счета, переводы, историю операций и начисление процентов.
- `currencies-service` отвечает за курсы валют, конвертацию и кэширование курсов в Redis.
- `common` содержит общие DTO, enums, events и exceptions.
- PostgreSQL используется для данных сервисов и Casdoor.
- Redis используется для кэша курсов валют.
- Kafka используется для событий по операциям и начислениям.
- Casdoor используется как Identity Provider / SSO.

## Архитектура

`core-service` хранит банковскую доменную модель: пользователей, счета, транзакции и журналы начисления процентов. Он проверяет bearer token через Spring Security OAuth2 Resource Server, обращается к `currencies-service` через OpenFeign и публикует события в Kafka.

`currencies-service` хранит курсы валют в PostgreSQL, получает данные из внешнего API и кэширует результаты в Redis.

PostgreSQL databases:

- `core_db` для `core-service`;
- `currencies_db` для `currencies-service`;
- `casdoor_db` для Casdoor.

Redis используется только как cache layer для `currencies-service`.

Kafka используется для событий банковских операций и начисления процентов.

Casdoor выпускает JWT и выступает внешним IdP/SSO для обычного режима работы.

`dev-auth` profile существует только для локальной разработки и быстрых ручных проверок без полноценного Casdoor flow.

## Аутентификация и авторизация

Default auth использует Casdoor OIDC/JWT и Spring Security OAuth2 Resource Server. Casdoor выпускает JWT, а backend сам JWT не создаёт и не подписывает.

`core-service` проверяет JWT через `issuer-uri` и `jwk-set-uri`:

```properties
OIDC_ISSUER_URI=http://localhost:8000
OIDC_JWK_SET_URI=http://localhost:8000/.well-known/jwks
OIDC_CLIENT_ID=banking-core
OIDC_AUTHORIZATION_URL=http://localhost:8000/login/oauth/authorize
OIDC_TOKEN_URL=http://localhost:8000/api/login/oauth/access_token
```

Роли берутся из Casdoor claim `roles[].name`:

- `USER` -> `ROLE_USER`;
- `ADMIN` -> `ROLE_ADMIN`.

`dev-auth` profile существует только для локальной разработки. Он не является production security. В обычном режиме protected endpoints без bearer token возвращают `401`.

Проверенная модель доступа:

- `dev-user` имеет доступ к пользовательским endpoints, например `/api/accounts/my`;
- `dev-user` не имеет доступа к admin endpoints;
- `admin-user` имеет доступ к admin endpoints.

## Почему это не session-auth

`spring-session-data-redis` не используется. Redis не хранит auth sessions, и `SecurityContext` не сохраняется в Redis.

Access token и refresh token не хранятся в нашей БД. `core-service` хранит только связь Casdoor user -> local `UserEntity` через `externalAuthId` и email.

Обращение к БД нужно для связи внешнего пользователя Casdoor с доменной моделью банка. Это не восстановление server-side session.

## Локальный запуск инфраструктуры

```powershell
docker compose up -d
docker compose ps
```

Ожидаемые сервисы:

- `postgres-core`;
- `postgres-currencies`;
- `postgres-casdoor`;
- `redis`;
- `kafka`;
- `casdoor`.

## Локальный запуск сервисов через Maven

`currencies-service`:

```powershell
.\mvnw.cmd -pl currencies-service spring-boot:run
```

`core-service`, обычный режим:

```powershell
.\mvnw.cmd -pl core-service spring-boot:run
```

`core-service`, local `dev-auth` режим:

```powershell
.\mvnw.cmd -pl core-service spring-boot:run -Dspring-boot.run.profiles=dev-auth
```

## Swagger / OpenAPI

- `currencies-service`: http://localhost:8081/swagger-ui.html
- `core-service`: http://localhost:8080/swagger-ui.html
- Casdoor redirect URL для Swagger OAuth: http://localhost:8080/swagger-ui/oauth2-redirect.html

## Проверки

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package -DskipTests
```

## Переменные окружения

`.env` не коммитится. Файл `.env.example` содержит только примерные значения без секретов и без client secret.

## Безопасность

- `jjwt` не используется.
- Backend не выпускает JWT.
- Production authentication filter не пишется вручную.
- Используется Spring Security OAuth2 Resource Server.
- `dev-auth` используется только для локальной разработки.
