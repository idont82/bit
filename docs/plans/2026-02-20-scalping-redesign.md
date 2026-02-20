# counter-trade.html 스캘핑 재설계 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** counter-trade.html의 역추세(BB+RSI) 전략을 거래량 브레이크아웃 스캘핑 전략으로 전면 교체한다.

**Architecture:** 단일 HTML 파일 내 JS로 구현. 스캔 타이머(30초)와 포지션 타이머(10초)를 분리하여 매수 감지와 매도 감시를 독립적으로 운영한다. 기존 코드 구조(상수→유틸→지표→신호감지→컨트롤→렌더링→API→매매→초기화)를 유지하되 지표/신호 로직만 교체한다.

**Tech Stack:** Vanilla JS, Bithumb v1 REST API, Web Crypto API (JWT), localStorage/Cookie

---

## Task 1: 상수 및 변수 교체

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html:460-478`

**Step 1: 기존 역추세 상수 제거, 스캘핑 상수로 교체**

삭제할 상수:
```javascript
const BB_LEN = 20, BB_STD = 2;
const RSI_LEN = 7, RSI_OS = 20, RSI_OB = 80;
const CANDLE_COUNT = 50;
```

새 상수:
```javascript
const CANDLE_COUNT = 20; // 거래량 평균 산출용
const VOL_MULT_DEFAULT = 2.0; // 거래량 배수 기준
const STOPLOSS_MIN_DEFAULT = 5; // 손절 시간(분)
const MAX_POS_DEFAULT = 3; // 최대 동시 포지션
const COOLDOWN_MS = 180000; // 재진입 쿨다운 3분
const POS_CHECK_INTERVAL = 10000; // 포지션 체크 10초
```

삭제할 변수:
```javascript
let signalMode = '1';
```

새 변수:
```javascript
let posTimer = null; // 포지션 체크 타이머
let cooldownMap = {}; // { symbol: 매도시각(ms) } 재진입 쿨다운
```

tradeConfig 객체에 새 필드 추가:
```javascript
var tradeConfig = {
  accessKey: '', secretKey: '',
  autoBuy: false,
  buyAmount: 10000,
  profitTarget: 0.5,
  volMult: 2.0,       // 추가
  stopLossMin: 5,     // 추가
  maxPositions: 3     // 추가
};
```

**Step 2: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "refactor: replace BB/RSI constants with scalping constants"
```

---

## Task 2: 지표 함수 교체

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html:506-556` (지표 섹션)

**Step 1: 기존 지표 함수 삭제**

삭제 대상:
- `calcBB(candles)` (511-528)
- `calcRSI7(candles)` (530-540)
- `calcPrevRSI7(candles)` (542-546)

`calcVolSpike(candles)`는 유지하되 수정.

**Step 2: 새 지표 함수 작성**

```javascript
// 거래량 브레이크아웃 체크
function calcVolBreakout(candles) {
  if (!candles || candles.length < 2) return { breakout: false, ratio: 1 };
  var latest = candles[0];
  var sum = 0;
  for (var i = 1; i < candles.length; i++) sum += candles[i].candle_acc_trade_volume;
  var avg = sum / (candles.length - 1);
  var ratio = avg > 0 ? latest.candle_acc_trade_volume / avg : 0;
  return { breakout: ratio >= tradeConfig.volMult, ratio: ratio };
}

// 양봉 + 전봉 대비 상승 체크
function checkBullish(candles) {
  if (!candles || candles.length < 2) return false;
  var c0 = candles[0], c1 = candles[1];
  return c0.trade_price > c0.opening_price && c0.trade_price > c1.trade_price;
}

// 모멘텀 체크: 3캔들 우상향 또는 최신 캔들 상승률 > 0.1%
function checkMomentum(candles) {
  if (!candles || candles.length < 3) return false;
  var c0 = candles[0], c1 = candles[1], c2 = candles[2];
  // 3캔들 우상향
  if (c0.trade_price > c1.trade_price && c1.trade_price > c2.trade_price) return true;
  // 또는 최신 캔들 상승률 > 0.1%
  var chgRate = c0.opening_price > 0 ? ((c0.trade_price - c0.opening_price) / c0.opening_price) * 100 : 0;
  return chgRate > 0.1;
}
```

**Step 3: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "refactor: replace BB/RSI indicators with volume breakout and momentum"
```

---

## Task 3: detectSignal 재작성

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html:560-686` (detectSignal 함수)

**Step 1: 기존 detectSignal 전체 교체**

```javascript
function detectSignal(candles) {
  if (!candles || candles.length < 3) return { signal: 'none', detail: '', badges: [] };

  var vol = calcVolBreakout(candles);
  var bullish = checkBullish(candles);
  var momentum = checkMomentum(candles);
  var c0 = candles[0];
  var price = c0.trade_price;

  var result = {
    signal: 'none',
    volRatio: vol.ratio,
    price: price,
    detail: '',
    badges: []
  };

  // 거래량 배지 항상 표시
  result.badges.push({
    text: 'VOL x' + vol.ratio.toFixed(1),
    cls: vol.breakout ? 'badge-up' : 'badge-info'
  });

  if (vol.breakout && bullish && momentum) {
    // ★ 매수 신호: 거래량 브레이크아웃 + 양봉상승 + 모멘텀 ★
    result.signal = 'buy';
    result.detail = 'VOL x' + vol.ratio.toFixed(1) + '+양봉+모멘텀';
    result.badges.push({ text: '양봉상승', cls: 'badge-up' });
    result.badges.push({ text: '모멘텀↑', cls: 'badge-up' });
  } else if (vol.breakout && bullish) {
    // 거래량+양봉은 있지만 모멘텀 부족 → 대기
    result.signal = 'wait';
    result.detail = 'VOL x' + vol.ratio.toFixed(1) + '+양봉 (모멘텀 부족)';
    result.badges.push({ text: '양봉상승', cls: 'badge-warn' });
    result.badges.push({ text: '모멘텀 부족', cls: 'badge-info' });
  } else if (vol.breakout) {
    // 거래량만 급등 → 대기
    result.signal = 'wait';
    result.detail = 'VOL x' + vol.ratio.toFixed(1) + ' (방향성 미확인)';
    result.badges.push({ text: '방향성 대기', cls: 'badge-warn' });
  }

  return result;
}
```

**Step 2: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "feat: implement volume breakout scalping signal detection"
```

---

## Task 4: UI 컨트롤 수정

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (HTML 컨트롤 영역 + JS 컨트롤 함수)

**Step 1: 봉 기준 버튼에서 10분/30분/1시간 제거**

기존:
```html
<button class="tf-btn" data-tf="10" onclick="setTf('10',this)">10분</button>
<button class="tf-btn" data-tf="30" onclick="setTf('30',this)">30분</button>
<button class="tf-btn" data-tf="60" onclick="setTf('60',this)">1시간</button>
```
→ 삭제

**Step 2: 매수 조건 모드 선택 버튼 제거**

기존:
```html
<span class="control-label">매수 조건</span>
<button class="tf-btn active sig-btn" ...>(재진입 OR RSI돌파) + 거래량</button>
<button class="tf-btn sig-btn" ...>3캔들내 재진입 + RSI돌파 + 거래량</button>
```
→ 전체 `control-row` 삭제

**Step 3: setSignalMode 함수 삭제**

`function setSignalMode(mode, btn) { ... }` 전체 삭제.

**Step 4: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "refactor: remove BB/RSI mode controls, keep 1/3/5min timeframes"
```

---

## Task 5: 매매 설정 패널에 새 필드 추가

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (HTML 설정 패널 + saveTradeSettings/loadTradeSettings)

**Step 1: 설정 패널 HTML에 새 입력 필드 추가**

기존 `매도 수익률` 줄 아래에 추가:
```html
<div class="trade-row">
  <span class="trade-lbl">거래량 배수</span>
  <input type="text" class="trade-input" id="inVolMult" placeholder="2.0" value="2.0" style="width:70px" onchange="saveTradeSettings()">
  <span style="font-size:11px;color:var(--muted)">x 이상</span>
  <span class="trade-lbl" style="margin-left:16px">손절 시간</span>
  <button class="tf-btn sl-btn active" data-sl="3" onclick="setStopLoss(3,this)">3분</button>
  <button class="tf-btn sl-btn" data-sl="5" onclick="setStopLoss(5,this)">5분</button>
  <button class="tf-btn sl-btn" data-sl="10" onclick="setStopLoss(10,this)">10분</button>
  <button class="tf-btn sl-btn" data-sl="15" onclick="setStopLoss(15,this)">15분</button>
  <span class="trade-lbl" style="margin-left:16px">최대 포지션</span>
  <input type="text" class="trade-input" id="inMaxPos" placeholder="3" value="3" style="width:50px" onchange="saveTradeSettings()">
  <span style="font-size:11px;color:var(--muted)">개</span>
</div>
```

**Step 2: setStopLoss 함수 추가**

```javascript
function setStopLoss(min, btn) {
  tradeConfig.stopLossMin = min;
  setCookie('ct_slmin', String(min));
  document.querySelectorAll('.sl-btn').forEach(function(b) { b.classList.remove('active'); });
  btn.classList.add('active');
}
```

**Step 3: saveTradeSettings에 새 필드 저장 추가**

```javascript
tradeConfig.volMult = parseFloat(document.getElementById('inVolMult').value) || 2.0;
tradeConfig.maxPositions = parseInt(document.getElementById('inMaxPos').value) || 3;
setCookie('ct_volmult', String(tradeConfig.volMult));
setCookie('ct_maxpos', String(tradeConfig.maxPositions));
```

**Step 4: loadTradeSettings에 새 필드 로드 추가**

```javascript
var vm = getCookie('ct_volmult');
if (vm !== '') tradeConfig.volMult = parseFloat(vm) || 2.0;
var sl = getCookie('ct_slmin');
if (sl !== '') tradeConfig.stopLossMin = parseInt(sl) || 5;
var mp = getCookie('ct_maxpos');
if (mp !== '') tradeConfig.maxPositions = parseInt(mp) || 3;

document.getElementById('inVolMult').value = tradeConfig.volMult;
document.getElementById('inMaxPos').value = tradeConfig.maxPositions;
document.querySelectorAll('.sl-btn').forEach(function(b) {
  b.classList.toggle('active', parseInt(b.dataset.sl) === tradeConfig.stopLossMin);
});
```

**Step 5: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "feat: add volume multiplier, stop-loss time, max positions settings"
```

---

## Task 6: 포지션 타이머 분리 + 시간 기반 손절

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (checkPositionsForSell, fetchData, init)

**Step 1: checkPositionsForSell에 시간 기반 손절 추가**

`grossPct >= requiredPct` 조건 분기 아래에 시간 손절 분기 추가:
```javascript
// 시간 초과 손절
var elapsed = Date.now() - p.buyTime;
var limitMs = tradeConfig.stopLossMin * 60 * 1000;
if (elapsed >= limitMs && !p.sellOrderId) {
  // 시간 초과 → 시장가 청산
  try {
    var vol = parseFloat(p.volume);
    var sellResult = await apiAuth('POST', '/orders', {
      market: p.market, side: 'ask', ord_type: 'market', volume: vol.toFixed(8)
    });
    p.sellOrderId = sellResult.uuid;
    p.sellTime = Date.now();
    p.sellReason = 'timeout';
    // 체결 확인 (최대 10초)
    for (var sr = 0; sr < 5; sr++) {
      await sleep(2000);
      var sellOrder = await apiAuth('GET', '/order', { uuid: sellResult.uuid });
      if (sellOrder.state === 'done' || sellOrder.state === 'cancel') {
        if (sellOrder.trades && sellOrder.trades.length > 0) {
          var soldVol = 0, soldCost = 0;
          sellOrder.trades.forEach(function(st) { soldVol += parseFloat(st.volume); soldCost += parseFloat(st.price) * parseFloat(st.volume); });
          p.sellPrice = soldVol > 0 ? soldCost / soldVol : curPrice;
        } else { p.sellPrice = curPrice; }
        break;
      }
    }
    if (!p.sellPrice) p.sellPrice = curPrice;
    p.status = 'sold';
    p.grossPct = ((p.sellPrice - p.buyPrice) / p.buyPrice) * 100;
    p.netPct = p.grossPct - (FEE_ROUND * 100);
    // 쿨다운 등록
    cooldownMap[p.symbol] = Date.now();
  } catch (e) {
    console.error('시간손절 실패:', p.symbol, e.message);
  }
  await sleep(500);
}
```

**Step 2: 익절 매도에도 쿨다운 등록 추가**

익절 성공 후:
```javascript
cooldownMap[p.symbol] = Date.now();
```

**Step 3: 독립 포지션 타이머 추가**

```javascript
function startPosTimer() {
  if (posTimer) return;
  posTimer = setInterval(function() {
    var positions = loadPositions();
    var hasHold = positions.some(function(p) { return p.status === 'hold'; });
    if (hasHold) {
      checkPositionsForSell();
    } else {
      clearInterval(posTimer);
      posTimer = null;
    }
  }, POS_CHECK_INTERVAL);
}
```

**Step 4: fetchData에서 기존 checkPositionsForSell() 직접 호출 제거, startPosTimer 호출로 교체**

기존:
```javascript
checkPositionsForSell();
```
→ 교체:
```javascript
startPosTimer();
```

**Step 5: executeAutoBuys 끝에 포지션 타이머 시작 추가**

매수 성공 후:
```javascript
startPosTimer();
```

**Step 6: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "feat: add independent position timer with time-based stop-loss"
```

---

## Task 7: executeAutoBuys에 포지션 한도 + 쿨다운 체크 추가

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (executeAutoBuys)

**Step 1: 함수 상단에 한도/쿨다운 체크 추가**

```javascript
async function executeAutoBuys(buys) {
  if (!tradeConfig.accessKey || !tradeConfig.secretKey) return;
  var amount = tradeConfig.buyAmount;
  if (amount < 5000) return;

  var positions = loadPositions();
  var holdCount = positions.filter(function(p) { return p.status === 'hold'; }).length;

  for (var i = 0; i < buys.length; i++) {
    // 포지션 한도 체크
    if (holdCount >= tradeConfig.maxPositions) break;

    var b = buys[i];
    var market = 'KRW-' + b.symbol;

    // 동일 코인 보유 중 체크
    var already = positions.some(function(p) { return p.market === market && p.status === 'hold'; });
    if (already) continue;

    // 쿨다운 체크
    var lastSold = cooldownMap[b.symbol] || 0;
    if (Date.now() - lastSold < COOLDOWN_MS) continue;

    // ... 기존 매수 로직 ...
    holdCount++;
  }
}
```

**Step 2: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "feat: add max position limit and cooldown check to auto-buy"
```

---

## Task 8: renderTable 수정 (BB/RSI 컬럼 제거, 거래량 배수 표시)

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (renderTable)

**Step 1: 테이블 헤더에서 BB 위치 제거, 거래량 배수 유지**

coinData에서 `bbPos` 필드 제거. `volRatio` 유지. 테이블 컬럼:
코인명 | 현재가 | 등락률 | 거래량배수 | 신호 | 상세

**Step 2: 신호 표시를 스캘핑 맞게 수정**

- `buy` → 녹색 `매수` 배지
- `wait` → 노란색 `대기` 배지
- 그 외 → 회색 `-` 표시

**Step 3: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "refactor: update table columns for scalping strategy"
```

---

## Task 9: renderPositions 수정 (경과시간, 남은시간 추가)

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (renderPositions)

**Step 1: 포지션 테이블에 경과시간/남은시간 컬럼 추가**

기존 컬럼 + 추가:
```
코인 | 매수가 | 현재가 | 수익률 | 투자금 | 경과/남은시간 | 상태 | 동작
```

경과시간/남은시간 셀:
```javascript
var elapsed = Date.now() - p.buyTime;
var limitMs = tradeConfig.stopLossMin * 60 * 1000;
var remain = Math.max(0, limitMs - elapsed);
var elapsedSec = Math.floor(elapsed / 1000);
var remainSec = Math.floor(remain / 1000);
var elapsedStr = Math.floor(elapsedSec / 60) + ':' + ('0' + (elapsedSec % 60)).slice(-2);
var remainStr = Math.floor(remainSec / 60) + ':' + ('0' + (remainSec % 60)).slice(-2);
var urgent = remain < 30000;
var progress = Math.min((elapsed / limitMs) * 100, 100);

// 시간 프로그레스바 (녹색→빨간색)
var barColor = urgent ? 'var(--down)' : 'var(--accent)';
html += '<td>'
  + '<span style="font-size:10px">' + elapsedStr + '</span>'
  + '<span style="font-size:10px;color:' + (urgent ? 'var(--down)' : 'var(--muted)') + '"> / ' + remainStr + '</span>'
  + '<div style="width:60px;height:3px;background:var(--border);border-radius:2px;margin-top:2px">'
  + '<div style="width:' + progress.toFixed(0) + '%;height:100%;background:' + barColor + ';border-radius:2px"></div></div>'
  + '</td>';
```

**Step 2: 매도 사유 표시**

`p.sellReason === 'timeout'`이면 `시간초과` 빨간색, 아니면 `익절` 녹색.

**Step 3: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "feat: add elapsed/remaining time display with progress bar to positions"
```

---

## Task 10: 페이지 타이틀 및 init 정리

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (title, header, init)

**Step 1: 타이틀/헤더 변경**

```html
<title>스캘핑 매매</title>
```
헤더의 `역추세 매매 알림` → `스캘핑 매매`

**Step 2: init에서 signalMode 쿠키 로드 제거**

삭제:
```javascript
var sm = getCookie('ct_sigmode');
if (sm && (sm === '1' || sm === '2')) { ... }
```

**Step 3: init에서 손절 시간 쿠키 로드 추가**

```javascript
var sl = getCookie('ct_slmin');
if (sl !== '') {
  tradeConfig.stopLossMin = parseInt(sl) || 5;
  document.querySelectorAll('.sl-btn').forEach(function(b) {
    b.classList.toggle('active', parseInt(b.dataset.sl) === tradeConfig.stopLossMin);
  });
}
```

**Step 4: init에서 보유 포지션 있으면 포지션 타이머 시작**

```javascript
var existingPos = loadPositions().filter(function(p) { return p.status === 'hold'; });
if (existingPos.length > 0) startPosTimer();
```

**Step 5: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "refactor: update title, clean init for scalping strategy"
```

---

## Task 11: fetchData에서 coinData 구조 수정

**Files:**
- Modify: `src/main/resources/static/html/counter-trade.html` (fetchData 내 coinData)

**Step 1: coinData에서 BB/RSI 필드 제거**

기존:
```javascript
sig: 'none', rsi: null, bbPos: 0.5, volRatio: 1, badges: [], detail: ''
```
→ 교체:
```javascript
sig: 'none', volRatio: 1, badges: [], detail: ''
```

**Step 2: detectSignal 결과 매핑 수정**

기존:
```javascript
coinData.rsi = result.rsi;
coinData.bbPos = result.bbPos;
```
→ 삭제

유지/수정:
```javascript
coinData.sig = result.signal;
coinData.volRatio = result.volRatio;
coinData.badges = result.badges;
coinData.detail = result.detail;
coinData.price = result.price || candles[0].trade_price;
```

**Step 3: 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "refactor: simplify coinData structure for scalping"
```

---

## Task 12: 전체 동작 검증 및 최종 커밋

**Step 1: 브라우저에서 페이지 열고 확인**
- 봉 기준 1분/3분/5분만 표시되는지
- 매수 조건 모드 버튼이 사라졌는지
- 거래량 배수/손절 시간/최대 포지션 설정이 표시되는지
- 스캔 시 거래량 브레이크아웃 감지가 작동하는지
- 포지션 테이블에 경과시간/남은시간이 표시되는지

**Step 2: 최종 커밋**
```bash
git add src/main/resources/static/html/counter-trade.html
git commit -m "feat: complete scalping strategy redesign for counter-trade"
```
