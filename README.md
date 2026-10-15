# Solicitações de Atendimento API

REST API for service requests: a client fills a request in three steps (with save-and-resume),
submits it, and an analyst reviews it, but only for the states (UF) they cover. Java 21, Spring
Boot 3.5, PostgreSQL, Elasticsearch, JWT.

## Running

```bash
docker compose up --build
```

- API: http://localhost:8080. Swagger UI: http://localhost:8080/swagger-ui.html (use **Authorize** with the token from `/auth/login`).
- The first ADMIN is created at startup: `admin@sea.local` / `admin12345` (`ADMIN_EMAIL`, `ADMIN_PASSWORD` in `docker-compose.yml`). With no `ADMIN_PASSWORD`, no admin is created.
- Metrics (ADMIN token): `/actuator/metrics`, including `audit.actions` (per action) and `http.server.requests`.

To run the app from the IDE instead, start only the dependencies with `docker compose up postgres elasticsearch` and run
`ChallengeSeaApplication` with `ADMIN_PASSWORD` set.

## Test frontend

A small Vite + React app in `frontend/` to click through the flow: client wizard, analyst search
and review, admin users. Its dev server proxies the API paths to `localhost:8080`, so the backend
needs no CORS.

```bash
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## A walk through the flow

```bash
API=http://localhost:8080
curl -s -XPOST $API/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Ana","email":"ana@mail.com","password":"ana-pass-123"}'
CLIENT=$(curl -s -XPOST $API/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ana@mail.com","password":"ana-pass-123"}' | jq -r .accessToken)

ID=$(curl -s -XPOST $API/solicitations -H "Authorization: Bearer $CLIENT" | jq .id)
curl -s -XPUT $API/solicitations/$ID/steps/basic-data -H "Authorization: Bearer $CLIENT" -H 'Content-Type: application/json' \
  -d '{"serviceType":"INSTALLATION","title":"Instalar ar-condicionado","description":"Dois aparelhos split na sala"}'
curl -s -XPUT $API/solicitations/$ID/steps/address -H "Authorization: Bearer $CLIENT" -H 'Content-Type: application/json' \
  -d '{"cep":"01001-000","number":"12A"}'
curl -s -XPUT $API/solicitations/$ID/steps/confirmation -H "Authorization: Bearer $CLIENT" -H 'Content-Type: application/json' \
  -d '{"priority":"HIGH","preferredDate":"2030-01-10","estimatedValue":250,"termsAccepted":true}'
curl -s -XPOST $API/solicitations/$ID/submit -H "Authorization: Bearer $CLIENT"

ADMIN=$(curl -s -XPOST $API/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@sea.local","password":"admin12345"}' | jq -r .accessToken)
curl -s -XPOST $API/admin/users -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"name":"Bia","email":"bia@sea.local","password":"bia-pass-123","role":"ANALYST","coveredStates":["SP"]}'
ANALYST=$(curl -s -XPOST $API/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"bia@sea.local","password":"bia-pass-123"}' | jq -r .accessToken)

curl -s "$API/analyst/solicitations/search?q=ar-condicionado&status=SUBMITTED" -H "Authorization: Bearer $ANALYST"
curl -s -XPOST $API/analyst/solicitations/$ID/start -H "Authorization: Bearer $ANALYST"
curl -s -XPOST $API/analyst/solicitations/$ID/decide -H "Authorization: Bearer $ANALYST" -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVE","comment":"Endereço e valores conferidos"}'
```

## Endpoints

| Endpoint | Who | What |
|---|---|---|
| `POST /auth/register` | anyone | Creates a CLIENT |
| `POST /auth/login` | anyone | Bearer JWT for any role |
| `POST /admin/users` | ADMIN | Creates an ANALYST (with `coveredStates`) or ADMIN |
| `PUT /admin/users/{id}/covered-states` | ADMIN | Replaces an analyst's states |
| `POST /solicitations` | CLIENT | Starts an empty draft |
| `GET /solicitations`, `GET /solicitations/{id}` | CLIENT, owner | Own solicitations; drafts include `pendingFields` |
| `PUT /solicitations/{id}/steps/basic-data` · `/address` · `/confirmation` | CLIENT, owner, DRAFT | Saves one step, partially if needed |
| `POST /solicitations/{id}/submit` | CLIENT, owner, DRAFT | DRAFT → SUBMITTED; 422 lists every pending field by step |
| `GET /analyst/solicitations/search` | ANALYST, ADMIN | `q`, `status`, `serviceType`, `priority`, `state`, `dateFrom`, `dateTo`, `page`, `size`, `sort` → `{items, page, size, total}` |
| `GET /analyst/solicitations/{id}` | ANALYST (state covered), ADMIN | |
| `POST /analyst/solicitations/{id}/start` | ANALYST (state covered), ADMIN | SUBMITTED → IN_REVIEW (optional) |
| `POST /analyst/solicitations/{id}/decide` | ANALYST (state covered), ADMIN | SUBMITTED or IN_REVIEW → APPROVED / REJECTED, comment of 10 to 1000 chars |

## How it works

- **Steps and drafts.** A step can be saved partially; fields left out keep their value. What a
  step needs to be *done* is written once, as Bean Validation groups on the `Solicitation` entity
  (`SolicitationStep`). Every step save and the submit run the same rules. `currentStep` is the first step
  that is not done, and a client cannot save a later step before it. The request DTOs only check
  format (lengths, 8-digit CEP, no past date) and answer 400.
- **CEP.** A new CEP is looked up on ViaCEP, which fills street, neighborhood, city and state. An
  unknown CEP and a ViaCEP failure both leave step 2 incomplete (`cepValidated` stays pending).
- **Access.** The URL rules check the role. Ownership and state coverage depend on the data, so
  the services check them. Someone else's solicitation answers 404, so its id is not confirmed. A state
  outside an analyst's coverage answers 403. Coverage is read from the database on each request,
  so an admin's change applies at once.
- **Search.** Every write publishes an event, and the document is indexed in Elasticsearch after
  the transaction commits. For an ANALYST the search always adds a filter on their states, and a
  `state` parameter can only narrow it. Drafts are indexed too, but the search always leaves them out.
- **Audit.** `@Audit` on a service method makes `AuditAspect` write an `audit_logs` row (action, user, resource id)
  and count it in the `audit.actions` metric.
- **Errors.** Every error is an RFC 9457 `ProblemDetail`. Validation errors add `errors: [{field, message}]`;
  submit adds `errors: [{step, field, message}]`. Messages are in pt-BR.

## Tests

```bash
./mvnw verify   # unit tests + the integration test (needs Docker; skipped without it)
```

`SolicitationFlowIntegrationTest` runs the whole flow against PostgreSQL and Elasticsearch in
Testcontainers, with only ViaCEP faked. The unit tests cover the step rules, submit, the review
transitions, coverage and the search query.

Against a running API (`docker compose up`), `scripts/api-smoke.sh` calls every endpoint with curl,
success and error paths alike (72 checks: validation, ownership, state coverage, status
transitions, search filters, the real ViaCEP). It needs `jq`.

```bash
scripts/api-smoke.sh   # http://localhost:8080; pass another base URL as the first argument to test elsewhere
```

## Code quality

The same tooling as Base-API and CensusAPIv2. None of it is bound to the build, so `./mvnw verify` stays build-and-test.

```bash
./mvnw spotless:apply                          # format (palantir-java-format)
./mvnw spotless:check                          # check formatting without changing files
./mvnw pmd:check                               # lint, rules in pmd-ruleset.xml; every rule is an error
./mvnw test-compile dependency:analyze-only    # unused and test-only dependencies
./mvnw cyclonedx:makeBom && osv-scanner scan source --config=osv-scanner.toml -L target/bom.json   # known vulnerabilities
git config core.hooksPath .githooks            # once per clone: format staged files on commit
```

`.github/workflows/ci.yml` runs all of them on every PR.
