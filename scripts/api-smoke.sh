#!/usr/bin/env bash
# Exercises every endpoint against a running API, success and error paths. Needs curl and jq,
# and the bootstrap admin admin@sea.local / admin12345 (docker-compose.yml). Each run uses fresh
# random emails, so it can run repeatedly; the data it creates stays in the database.
# Usage: scripts/api-smoke.sh [base-url]
B=${1:-http://localhost:8080}
RUN=$RANDOM
PASS=0; FAIL=0
BODY=$(mktemp)

# req METHOD PATH TOKEN [JSON] -> sets $CODE, body in $BODY
req() {
  local auth=() data=()
  [ -n "$3" ] && auth=(-H "Authorization: Bearer $3")
  [ -n "$4" ] && data=(-H 'Content-Type: application/json' -d "$4")
  CODE=$(curl -s -o "$BODY" -w '%{http_code}' -X "$1" "$B$2" "${auth[@]}" "${data[@]}")
}
# check NAME EXPECTED_CODE [jq-expression that must be true]
check() {
  local ok=1
  [ "$CODE" = "$2" ] || ok=0
  if [ $ok = 1 ] && [ -n "$3" ]; then jq -e "$3" "$BODY" >/dev/null 2>&1 || ok=0; fi
  if [ $ok = 1 ]; then PASS=$((PASS+1)); printf '  \e[32m✓\e[0m %-62s %s\n' "$1" "$CODE"
  else FAIL=$((FAIL+1)); printf '  \e[31m✗\e[0m %-62s got %s, want %s %s\n      %s\n' "$1" "$CODE" "$2" "$3" "$(head -c 300 "$BODY")"; fi
}
login() { req POST /auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}"; jq -r .accessToken "$BODY"; }
if [ "$(curl -s -m 5 -o /dev/null -w '%{http_code}' "$B/actuator/health")" != 200 ]; then
  echo "API not reachable at $B (GET /actuator/health did not answer 200). Is it running?" >&2
  exit 1
fi
FUTURE=$(date -d '+10 days' +%F); PAST=$(date -d '-1 day' +%F); TODAY=$(date +%F)

echo "== Public, docs and auth"
req GET /actuator/health;                          check "health is public" 200 '.status=="UP"'
req GET /v3/api-docs;                              check "OpenAPI spec is public" 200 '.paths["/auth/token"]'
req GET /solicitations;                            check "no token -> 401 ProblemDetail" 401 '.status==401'
req GET /solicitations "not.a.jwt";                check "garbage token -> 401" 401

C1="client1.$RUN@t.local"; C2="client2.$RUN@t.local"
req POST /auth/register "" "{\"name\":\"Cliente Um\",\"email\":\"$C1\",\"password\":\"pass12345\"}"
check "register client" 201 '.role=="CLIENT"'
req POST /auth/register "" "{\"name\":\"Cliente Um\",\"email\":\"$(echo $C1 | tr a-z A-Z)\",\"password\":\"pass12345\"}"
check "register same email, other case -> 409" 409
req POST /auth/register "" '{"name":"","email":"nope","password":"123"}'
check "register invalid -> 400 with field errors" 400 '(.errors|map(.field)|sort)==["email","name","password"]'
req POST /auth/register "" "{\"name\":\"Cliente Dois\",\"email\":\"$C2\",\"password\":\"pass12345\"}"
check "register second client" 201
req POST /auth/login "" "{\"email\":\"$C1\",\"password\":\"wrong-pass\"}"
check "login wrong password -> 401" 401
CODE=$(curl -s -o "$BODY" -w '%{http_code}' -X POST "$B/auth/token" -d "username=$C1&password=pass12345")
check "OAuth2 form login (/auth/token)" 200 '.access_token and .token_type=="Bearer"'

CT1=$(login "$C1" pass12345); CT2=$(login "$C2" pass12345); AT=$(login admin@sea.local admin12345)
[ -z "$AT" ] || [ "$AT" = "null" ] && { echo "admin login failed: is ADMIN_PASSWORD=admin12345 set?" >&2; exit 1; }

echo "== Options"
for kind in service-types priorities statuses decisions steps roles; do
  req GET /options/$kind "$CT1"; check "GET /options/$kind" 200 'length>0 and (.[0]|has("option") and has("name") and has("description"))'
done
req GET /options/roles "$CT1";                     check "roles are the assignable ones" 200 'map(.option)==["ANALYST","ADMIN"]'

echo "== Admin"
ASP="analyst.sp.$RUN@t.local"; ARJ="analyst.rj.$RUN@t.local"
req POST /admin/users "$CT1" '{}';                 check "client on /admin -> 403" 403
req POST /admin/users "$AT" "{\"name\":\"Analista SP\",\"email\":\"$ASP\",\"password\":\"pass12345\",\"role\":\"ANALYST\",\"coveredStates\":[\"SP\"]}"
check "create analyst SP" 201 '.coveredStates==["SP"]'
req POST /admin/users "$AT" "{\"name\":\"Analista RJ\",\"email\":\"$ARJ\",\"password\":\"pass12345\",\"role\":\"ANALYST\",\"coveredStates\":[\"MG\"]}"
check "create analyst MG (changed to RJ below)" 201; ARJ_ID=$(jq .id "$BODY")
req POST /admin/users "$AT" "{\"name\":\"X\",\"email\":\"x.$RUN@t.local\",\"password\":\"pass12345\",\"role\":\"CLIENT\"}"
check "create CLIENT via admin -> 400" 400
req POST /admin/users "$AT" "{\"name\":\"X\",\"email\":\"y.$RUN@t.local\",\"password\":\"pass12345\",\"role\":\"ADMIN\",\"coveredStates\":[\"SP\"]}"
check "admin with states -> 400" 400
req POST /admin/users "$AT" "{\"name\":\"X\",\"email\":\"z.$RUN@t.local\",\"password\":\"pass12345\",\"role\":\"ANALYST\",\"coveredStates\":[\"sp1\"]}"
check "invalid UF -> 400" 400
req PUT /admin/users/$ARJ_ID/covered-states "$AT" '{"states":["RJ"]}'
check "set analyst states to RJ" 200 '.coveredStates==["RJ"]'
req PUT /admin/users/999999/covered-states "$AT" '{"states":["RJ"]}'
check "states of missing user -> 404" 404
ST=$(login "$ASP" pass12345); RT=$(login "$ARJ" pass12345)

echo "== Client: draft and steps"
req POST /solicitations "$AT";                     check "admin cannot create (CLIENT only) -> 403" 403
req POST /solicitations "$CT1";                    check "create draft" 201 '.status=="DRAFT" and .currentStep==1 and (.pendingFields|length)>0'
ID=$(jq .id "$BODY"); S=/solicitations/$ID
req GET $S "$CT1";                                 check "get own" 200 ".id==$ID"
req GET $S "$CT2";                                 check "other client's -> 404" 404
req GET "/solicitations?page=0&size=5" "$CT1";     check "list own" 200 ".total>=1 and .size==5 and (.items|map(.id)|index($ID))!=null"
req POST $S/submit "$CT1";                         check "submit empty draft -> 422, errors by step" 422 '[.errors[].step]|unique==["ADDRESS","BASIC_DATA","CONFIRMATION"]'
req PUT $S/steps/address "$CT1" '{"cep":"01001000","number":"1"}'
check "step 2 before step 1 -> 409" 409
req PUT $S/steps/basic-data "$CT1" '{"title":"ab","description":"curta"}'
check "step 1 bad lengths -> 400" 400 '(.errors|map(.field)|sort)==["description","title"]'
req PUT $S/steps/basic-data "$CT1" '{"serviceType":"WRONG"}'
check "step 1 unknown enum -> 400" 400
req PUT $S/steps/basic-data "$CT1" '{"title":"  Instalar ar-condicionado  "}'
check "step 1 partial save stays on step 1" 200 '.currentStep==1 and .title=="Instalar ar-condicionado"'
req PUT $S/steps/basic-data "$CT2" '{"title":"Hack"}'
check "other client edits -> 404" 404
req PUT $S/steps/basic-data "$CT1" '{"serviceType":"INSTALLATION","description":"Dois aparelhos split na sala e no quarto"}'
check "step 1 complete -> currentStep 2" 200 '.currentStep==2'
req PUT $S/steps/address "$CT1" '{"cep":"123","number":"1"}'
check "step 2 malformed CEP -> 400" 400
req PUT $S/steps/address "$CT1" '{"cep":"00000-000","number":"12A"}'
check "step 2 unknown CEP -> saved, not validated" 200 '.cepValidated==false and .currentStep==2 and (.pendingFields|map(.field)|index("cepValidated"))!=null'
req PUT $S/steps/address "$CT1" '{"cep":"01001-000","number":"12A","complement":"Apto 3"}'
check "step 2 real CEP via ViaCEP -> SP, step 3" 200 '.cepValidated and .state=="SP" and .city=="São Paulo" and .currentStep==3'
req PUT $S/steps/confirmation "$CT1" "{\"preferredDate\":\"$PAST\"}"
check "step 3 past date -> 400" 400
req PUT $S/steps/confirmation "$CT1" '{"estimatedValue":-1}'
check "step 3 negative value -> 400" 400
req PUT $S/steps/confirmation "$CT1" "{\"priority\":\"HIGH\",\"preferredDate\":\"$FUTURE\",\"estimatedValue\":50,\"termsAccepted\":false}"
check "step 3 HIGH<100 and terms off -> both pending" 200 '(.pendingFields|map(.field)|sort)==["estimatedValueAllowedForPriority","termsAccepted"]'
req POST $S/submit "$CT1";                         check "submit with pending -> 422" 422 '(.errors|length)==2'
req PUT $S/steps/confirmation "$CT1" '{"estimatedValue":250.5,"termsAccepted":true}'
check "step 3 fixed -> nothing pending" 200 '.pendingFields==[]'
req POST $S/submit "$CT2";                         check "other client submits -> 404" 404
req POST $S/submit "$CT1";                         check "submit" 200 '.status=="SUBMITTED" and .submittedAt!=null'
req POST $S/submit "$CT1";                         check "submit twice -> 409" 409
req PUT $S/steps/basic-data "$CT1" '{"title":"Outro título"}'
check "edit after submit -> 409" 409

req POST /solicitations "$CT2";                    DRAFT_ID=$(jq .id "$BODY")

echo "== Analyst: access and search"
sleep 1.2   # Elasticsearch refreshes every second
req GET /analyst/solicitations/$ID "$CT1";         check "client on /analyst -> 403" 403
req GET /analyst/solicitations/$ID "$RT";          check "RJ analyst, SP solicitation -> 403" 403
req GET /analyst/solicitations/$ID "$ST";          check "SP analyst -> 200" 200 '.state=="SP"'
req GET /analyst/solicitations/$DRAFT_ID "$AT";    check "draft hidden from review -> 404" 404
req GET /analyst/solicitations/999999 "$AT";       check "missing -> 404" 404
req GET "/analyst/solicitations/search?q=ar-condicionado" "$ST"
check "search full text (SP analyst)" 200 "(.items|map(.id)|index($ID))!=null and has(\"page\") and has(\"size\") and has(\"total\")"
req GET "/analyst/solicitations/search?q=ar-condicionado&state=SP" "$RT"
check "RJ analyst asking state=SP -> nothing" 200 '.total==0'
req GET "/analyst/solicitations/search?status=SUBMITTED&serviceType=INSTALLATION&priority=HIGH&state=SP&dateFrom=$TODAY&dateTo=$TODAY" "$ST"
check "search with every filter" 200 "(.items|map(.id)|index($ID))!=null"
req GET "/analyst/solicitations/search?priority=LOW&q=ar-condicionado" "$ST"
check "filter that excludes it" 200 "(.items|map(.id)|index($ID))==null"
req GET "/analyst/solicitations/search?status=DRAFT" "$AT"
check "drafts never in search" 200 '.total==0'
req GET "/analyst/solicitations/search?sort=estimatedValue,desc&size=2&page=0" "$AT"
check "sort + page size" 200 '.size==2 and (.items|length)<=2'
req GET "/analyst/solicitations/search?sort=title" "$AT"
check "sort on unsupported field -> 400" 400
req GET "/analyst/solicitations/search?dateFrom=2030-01-02&dateTo=2030-01-01" "$AT"
check "dateFrom after dateTo -> 400" 400

echo "== Analyst: review"
req POST /analyst/solicitations/$ID/start "$RT";   check "start outside coverage -> 403" 403
req POST /analyst/solicitations/$ID/decide "$ST" '{"decision":"APPROVE","comment":"curto"}'
check "decide with short comment -> 400" 400
req POST /analyst/solicitations/$ID/start "$ST";   check "start review" 200 '.status=="IN_REVIEW" and .analystId!=null'
req POST /analyst/solicitations/$ID/start "$ST";   check "start twice -> 409" 409
req POST /analyst/solicitations/$ID/decide "$ST" '{"decision":"REJECT","comment":"Endereço fora da área de atendimento"}'
check "decide REJECT" 200 '.status=="REJECTED" and .decidedAt!=null'
req POST /analyst/solicitations/$ID/decide "$AT" '{"decision":"APPROVE","comment":"Revendo a decisão anterior"}'
check "decide again -> 409" 409
sleep 1.2
req GET "/analyst/solicitations/search?status=REJECTED&q=ar-condicionado" "$ST"
check "index updated after decision" 200 "(.items|map(.id)|index($ID))!=null"
req GET $S "$CT1";                                 check "client sees the decision" 200 '.status=="REJECTED" and .decisionComment!=null'

echo "== Metrics"
req GET /actuator/metrics/audit.actions "$AT";     check "audit.actions metric (admin)" 200 '.measurements[0].value>0'
req GET /actuator/metrics "$CT1";                  check "metrics as client -> 403" 403

echo; echo "Passed: $PASS   Failed: $FAIL"
rm -f "$BODY"
[ $FAIL = 0 ]
