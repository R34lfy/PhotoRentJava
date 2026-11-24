#!/usr/bin/env bash

HOST="${HOST:-http://localhost:8080}"
SFX=$(LC_ALL=C tr -dc 'a-z0-9' </dev/urandom 2>/dev/null | head -c 5)
TMP=$(mktemp)
OK=0; FAIL=0

# даты считаем от сегодняшнего дня, чтобы скрипт не устаревал (macOS: date -v, Linux: date -d)
day() { date -v"$1"d +%Y-%m-%d 2>/dev/null || date -d "$1 days" +%Y-%m-%d; }
D_PAST=$(day -1)
D_START=$(day +10); D_END=$(day +14)
D_OVERLAP_START=$(day +12); D_OVERLAP_END=$(day +17)
D_EXTEND=$(day +17)
D_CHECK=$(day +40)
D_FUTURE_START=$(day +60); D_FUTURE_END=$(day +62)

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }

req() {
  local name="$1" expected="$2" method="$3" path="$4" body="$5" code
  if [ -n "$body" ]; then
    code=$(curl -s -o "$TMP" -w '%{http_code}' -X "$method" "$HOST$path" \
           -H 'Content-Type: application/json' -d "$body")
  else
    code=$(curl -s -o "$TMP" -w '%{http_code}' -X "$method" "$HOST$path")
  fi
  echo
  echo "== $name"
  echo "   $method $path"
  [ -n "$body" ] && echo "   тело: $body"
  if [ "$code" = "$expected" ]; then
    echo "   $(green "PASS") (HTTP $code)"; OK=$((OK+1))
  else
    echo "   $(red "FAIL") (получили $code, ожидали $expected)"; FAIL=$((FAIL+1))
  fi
  if [ -s "$TMP" ]; then
    if command -v python3 >/dev/null 2>&1; then
      python3 -m json.tool < "$TMP" 2>/dev/null | head -n 25 || head -c 400 "$TMP"
    else
      head -c 400 "$TMP"; echo
    fi
  fi
  LAST_ID=$(grep -o '"id":[0-9]*' "$TMP" | head -1 | cut -d: -f2)
  if [ -n "$PAUSE" ]; then read -r -p "   [Enter - дальше] " _; fi
}

echo "PhotoRent demo, сервер: $HOST, суффикс данных: $SFX"

# CREATE
req "1.0 Категории" 200 GET /api/categories
req "1.1 Админ" 201 POST /api/users "{\"username\":\"admin_$SFX\",\"email\":\"admin_$SFX@demo.com\",\"password\":\"secret\",\"role\":\"ADMIN\"}"; ADMIN=$LAST_ID
req "1.2 Менеджер" 201 POST /api/users "{\"username\":\"manager_$SFX\",\"email\":\"manager_$SFX@demo.com\",\"password\":\"secret\",\"role\":\"MANAGER\"}"; MANAGER=$LAST_ID
req "1.3 Клиент" 201 POST /api/users "{\"username\":\"john_$SFX\",\"email\":\"john_$SFX@demo.com\",\"password\":\"secret\",\"role\":\"CUSTOMER\"}"; CUSTOMER=$LAST_ID
req "1.4 Дубликат логина -> 409" 409 POST /api/users "{\"username\":\"john_$SFX\",\"email\":\"other_$SFX@demo.com\",\"password\":\"secret\",\"role\":\"CUSTOMER\"}"
req "1.5 Невалидный email -> 400" 400 POST /api/users '{"username":"x","email":"not-an-email","password":"secret","role":"CUSTOMER"}'
req "2.0 Тариф с лишними знаками после запятой -> 400" 400 POST /api/models "{\"categoryPrefix\":\"CAM\",\"brand\":\"Sony\",\"name\":\"Bad-$SFX\",\"dailyRate\":10.123}"
req "2.1 Модель техники" 201 POST /api/models "{\"categoryPrefix\":\"CAM\",\"brand\":\"Sony\",\"name\":\"A7-$SFX\",\"description\":\"Full-frame\",\"dailyRate\":50}"; MODEL=$LAST_ID
req "2.2 Экземпляр 1 (NEW)" 201 POST /api/equipment "{\"modelId\":$MODEL,\"condition\":\"NEW\"}"; EQ1=$LAST_ID
req "2.3 Экземпляр 2 (WORN)" 201 POST /api/equipment "{\"modelId\":$MODEL,\"condition\":\"WORN\"}"; EQ2=$LAST_ID
req "2.4 Экземпляр 3 (GOOD)" 201 POST /api/equipment "{\"modelId\":$MODEL,\"condition\":\"GOOD\"}"; EQ3=$LAST_ID

# READ + фильтры
req "3.1 Фильтр: категория + статус" 200 GET "/api/equipment?categoryPrefix=CAM&status=AVAILABLE"
req "3.2 Фильтр: название + состояние" 200 GET "/api/equipment?name=A7-$SFX&condition=NEW"
req "3.3 Лучший свободный экземпляр (NEW)" 200 GET "/api/equipment/best?name=A7-$SFX"
req "3.4 Пользователи CUSTOMER" 200 GET "/api/users?role=CUSTOMER"
req "3.5 Модели категории CAM" 200 GET "/api/models?categoryPrefix=CAM"
req "3.6 Невалидный фильтр -> 400" 400 GET "/api/equipment?status=BROKEN"
req "3.7 Несуществующий id -> 404" 404 GET /api/equipment/999999

# UPDATE
req "4.1 Изменить модель" 200 PUT "/api/models/$MODEL" '{"description":"Full-frame mirrorless","dailyRate":60}'
req "4.2 Изменить пользователя" 200 PUT "/api/users/$CUSTOMER" "{\"username\":\"john_doe_$SFX\",\"email\":\"john_$SFX@demo.com\"}"
req "4.3 Сменить состояние (создаёт заметку)" 200 PATCH "/api/equipment/$EQ2/condition" "{\"condition\":\"GOOD\",\"staffId\":$MANAGER,\"reason\":\"Cleaned and tested\"}"
req "4.4 Журнал заметок" 200 GET "/api/equipment/$EQ2/notes"
req "4.5 Клиент меняет состояние -> 409" 409 PATCH "/api/equipment/$EQ2/condition" "{\"condition\":\"WORN\",\"staffId\":$CUSTOMER,\"reason\":\"I am a customer\"}"

# Аренда
req "5.1 Бронь" 201 POST /api/rentals "{\"equipmentId\":$EQ1,\"customerId\":$CUSTOMER,\"startDate\":\"$D_START\",\"endDate\":\"$D_END\"}"; RENTAL=$LAST_ID
req "5.2 Пересечение дат -> 409" 409 POST /api/rentals "{\"equipmentId\":$EQ1,\"customerId\":$CUSTOMER,\"startDate\":\"$D_OVERLAP_START\",\"endDate\":\"$D_OVERLAP_END\"}"
req "5.2a Начало брони в прошлом -> 400" 400 POST /api/rentals "{\"equipmentId\":$EQ1,\"customerId\":$CUSTOMER,\"startDate\":\"$D_PAST\",\"endDate\":\"$D_START\"}"
req "5.3 Выдача" 200 POST "/api/rentals/$RENTAL/issue"
req "5.4 Продление" 200 PATCH "/api/rentals/$RENTAL/extend" "{\"newEndDate\":\"$D_EXTEND\"}"
req "5.5 Списание с открытой арендой -> 409" 409 PATCH "/api/equipment/$EQ1/condition" "{\"condition\":\"DECOMMISSIONED\",\"staffId\":$ADMIN,\"reason\":\"Broken\"}"
req "5.6 Фильтр аренд" 200 GET "/api/rentals?customerId=$CUSTOMER&status=ACTIVE"
req "5.7 Проверка просрочек (многопоточная)" 200 POST "/api/rentals/overdue-check?today=$D_CHECK"
req "5.8 Статус аренды (OVERDUE)" 200 GET "/api/rentals/$RENTAL"
req "5.9 Возврат" 200 POST "/api/rentals/$RENTAL/return"

# DELETE
req "6.1 Списание (теперь можно)" 200 PATCH "/api/equipment/$EQ1/condition" "{\"condition\":\"DECOMMISSIONED\",\"staffId\":$ADMIN,\"reason\":\"Broken\"}"
req "6.2 Бронь на будущее" 201 POST /api/rentals "{\"equipmentId\":$EQ2,\"customerId\":$CUSTOMER,\"startDate\":\"$D_FUTURE_START\",\"endDate\":\"$D_FUTURE_END\"}"; RENTAL2=$LAST_ID
req "6.3 Отмена брони -> 204" 204 DELETE "/api/rentals/$RENTAL2"
req "6.4 Удаление экземпляра без связей -> 204" 204 DELETE "/api/equipment/$EQ3"
req "6.5 Повторное удаление -> 404" 404 DELETE "/api/equipment/$EQ3"
req "6.6 Удаление экземпляра со ссылками -> 409 (FK)" 409 DELETE "/api/equipment/$EQ2"

echo
echo "================ ИТОГ: $(green "$OK passed"), $( [ "$FAIL" -eq 0 ] && echo "0 failed" || red "$FAIL failed") ================"
rm -f "$TMP"