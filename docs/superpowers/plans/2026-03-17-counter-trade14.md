# Counter-Trade 14 구현 계획

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 코인별 백테스트 기반 최적 전략 자동 배정 + 마틴게일 물타기 자동매매 시스템 구현

**Architecture:** 단일 HTML 파일(counter-trade14.html)에 CSS + JavaScript를 인라인으로 작성. counter-trade13.html의 API/유틸리티/IndexedDB 코드를 재사용하되, 전략 시스템과 매매 엔진을 새로 설계. 스크리너→코인선택→백테스트→전략배정→마틴게일 매매의 파이프라인 구조.

**Tech Stack:** Vanilla HTML/CSS/JS, Upbit REST API, IndexedDB, Web Worker (bgSleep)

**Spec:** `docs/superpowers/specs/2026-03-17-counter-trade14-design.md`

**Base file:** `src/main/resources/static/html/counter-trade14.html` (신규 생성)

**Reference:** `src/main/resources/static/html/counter-trade13.html` (API/유틸리티 코드 복사 원본)

---

## Chunk 1: 기반 구조 + UI 셸

### Task 1: HTML/CSS 기본 구조 + 헤더/컨트롤바

**Files:**
- Create: `src/main/resources/static/html/counter-trade14.html`

counter-trade13.html에서 CSS와 HTML 구조를 복사하되 다음을 변경:

- [ ] **Step 1: HTML 파일 생성 — DOCTYPE부터 `</style>` 태그까지**

CSS 변수, 기본 스타일은 counter-trade13.html과 동일하게 복사. 추가 CSS:
```css
/* Coin Selection Checkbox */
.coin-check { width: 16px; height: 16px; accent-color: var(--up); cursor: pointer; }
.coin-row.disabled { opacity: 0.4; }
.coin-row.stale { opacity: 0.7; font-style: italic; }

/* Strategy Assignment Cards */
.assign-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.assign-card {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: 10px; padding: 14px; transition: all 0.2s;
}
.assign-card.active { border-color: var(--up); box-shadow: 0 0 12px rgba(0,212,170,0.15); }
.assign-card .card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }

/* DCA Stage Progress */
.stage-bar { display: flex; gap: 2px; margin: 6px 0; }
.stage-dot {
  flex: 1; height: 6px; border-radius: 3px; background: var(--surface2);
}
.stage-dot.filled { background: var(--accent3); }
.stage-dot.current { background: var(--gold); box-shadow: 0 0 4px var(--gold); }

/* TP Progress Bar */
.tp-progress { height: 4px; background: var(--surface2); border-radius: 2px; margin-top: 6px; }
.tp-fill { height: 100%; border-radius: 2px; transition: width 0.3s; }
.tp-fill.positive { background: linear-gradient(90deg, var(--accent3), var(--up)); }
.tp-fill.negative { background: linear-gradient(90deg, var(--down), var(--accent2)); }
```

- [ ] **Step 2: 헤더 HTML**

```html
<header>
  <div class="logo">
    <div class="logo-icon">SD</div>
    <div>
      <h1>SMART DCA SCALPER</h1>
      <span>Auto Strategy · Martingale DCA · Per-Coin Backtest</span>
    </div>
  </div>
  <div style="display:flex;align-items:center;gap:16px;">
    <span id="systemStatus" class="mono status-off" style="font-size:12px">STOPPED</span>
    <span id="totalAsset" class="mono" style="font-size:13px;font-weight:700">-</span>
    <button class="ctrl-btn" onclick="toggleScreensaver()" style="padding:4px 10px;font-size:11px;">LOCK</button>
  </div>
</header>
```

- [ ] **Step 3: 컨트롤 바 HTML**

counter-trade13 기반이나 다음 필드 변경:
- 매수금액 입력 (기존과 동일 id="tradeAmountInput")
- **익절% 입력 추가**: `<input id="tpPctInput" type="number" value="1.5" min="0.5" max="10" step="0.1">` + `%` 라벨
- API Key 버튼, 잔고 표시

```html
<div class="control-bar">
  <button id="startBtn" class="action-btn" onclick="startSystem()">▶ 시작</button>
  <button id="stopBtn" class="stop-btn" onclick="stopSystem()" disabled>■ 정지</button>
  <span class="sep">|</span>
  <div class="toggle-wrap">
    <span class="toggle-label">자동매매</span>
    <label class="toggle"><input type="checkbox" id="autoTradeToggle" onchange="onAutoTradeToggle()"><span class="toggle-slider"></span></label>
  </div>
  <span class="sep">|</span>
  <div class="toggle-wrap">
    <span class="toggle-label">매수금액</span>
    <input id="tradeAmountInput" type="number" value="5000" min="5000" step="1000"
      style="width:80px;background:var(--surface2);color:var(--text);border:1px solid var(--border);border-radius:4px;padding:2px 6px;font-size:11px;font-family:'IBM Plex Mono',monospace;text-align:right;"
      onchange="onTradeAmountChange()">
    <span class="toggle-label">원</span>
  </div>
  <span class="sep">|</span>
  <div class="toggle-wrap">
    <span class="toggle-label">익절</span>
    <input id="tpPctInput" type="number" value="1.5" min="0.5" max="10" step="0.1"
      style="width:50px;background:var(--surface2);color:var(--text);border:1px solid var(--border);border-radius:4px;padding:2px 6px;font-size:11px;font-family:'IBM Plex Mono',monospace;text-align:right;"
      onchange="onTpPctChange()">
    <span class="toggle-label">%</span>
  </div>
  <span class="sep">|</span>
  <button class="ctrl-btn" onclick="resetAllData()" style="color:var(--down)">초기화</button>
  <span class="sep">|</span>
  <button id="apiKeyBtn" class="ctrl-btn" onclick="openApiModal()">API Key</button>
  <span id="balanceDisplay" class="mono" style="font-size:11px;color:var(--muted)"></span>
  <span style="flex:1"></span>
  <span id="uptimeDisplay" class="mono" style="font-size:11px;color:var(--muted)"></span>
</div>
```

- [ ] **Step 4: Trade Readiness 패널 + 프로그레스 바 + Stats 바 HTML**

Trade Readiness: counter-trade13과 동일한 구조.

Stats 바:
```html
<div class="stats-bar">
  <div class="stat-card"><div class="stat-label">일일 손익</div><div id="dailyPnl" class="stat-value mono">-</div></div>
  <div class="stat-card"><div class="stat-label">실매매</div><div id="liveTradeCount" class="stat-value mono">0</div></div>
  <div class="stat-card"><div class="stat-label">활성 포지션</div><div id="posCount" class="stat-value mono">0</div></div>
  <div class="stat-card"><div class="stat-label">선택 코인</div><div id="selectedCoinCount" class="stat-value mono">0</div></div>
  <div class="stat-card"><div class="stat-label">총 투자</div><div id="totalInvested" class="stat-value mono">0</div></div>
</div>
```

- [ ] **Step 5: 섹션 HTML 셸 (빈 컨테이너)**

```html
<!-- COIN SCREENER + SELECTION -->
<div class="section">
  <div class="section-title">◆ COIN SCREENER
    <button class="ctrl-btn" onclick="selectAllCoins()" style="margin-left:12px;font-size:10px;padding:2px 8px;">전체선택</button>
    <button class="ctrl-btn" onclick="deselectAllCoins()" style="font-size:10px;padding:2px 8px;">전체해제</button>
    <span id="selectedCount" style="margin-left:8px;font-size:11px;color:var(--muted)"></span>
  </div>
  <div class="table-wrapper">
    <table><thead><tr><th>선택</th><th>#</th><th>코인</th><th>현재가</th><th>거래량(24h)</th><th>ATR%</th><th>배정전략</th><th>BT점수</th></tr></thead>
    <tbody id="screenerBody"></tbody></table>
  </div>
</div>

<!-- STRATEGY ASSIGNMENT -->
<div class="section">
  <div class="section-title">◆ 전략 배정 현황
    <span id="nextReeval" style="margin-left:12px;font-size:11px;color:var(--muted)"></span>
  </div>
  <div id="assignBoard" class="assign-grid"></div>
</div>

<!-- ACTIVE POSITIONS (DCA Detail) -->
<div class="section">
  <div class="section-title">◆ ACTIVE POSITIONS (DCA)</div>
  <div class="table-wrapper">
    <table><thead><tr><th>코인</th><th>전략</th><th>단계</th><th>평단가</th><th>현재가</th><th>손익</th><th>목표가</th><th>보유시간</th><th>진행</th></tr></thead>
    <tbody id="positionsBody"></tbody></table>
  </div>
</div>

<!-- TRADE HISTORY -->
<div class="section">
  <div class="section-title">◆ TRADE HISTORY</div>
  <div id="tradeHistSummary" style="font-size:12px;color:var(--muted);margin-bottom:8px;"></div>
  <div class="table-wrapper" style="max-height:250px;">
    <table><thead><tr><th>시간</th><th>코인</th><th>전략</th><th>단계</th><th>투자</th><th>손익</th><th>사유</th></tr></thead>
    <tbody id="historyBody"></tbody></table>
  </div>
</div>

<!-- LOG -->
<div class="section">
  <div class="section-title">◆ SYSTEM LOG</div>
  <div id="logPanel" class="table-wrapper" style="max-height:200px;padding:8px;font-size:11px;font-family:'IBM Plex Mono',monospace;"></div>
</div>
```

- [ ] **Step 6: API Modal + Screensaver + Toast HTML**

counter-trade13에서 그대로 복사 (apiModal, screensaver, toast div).

- [ ] **Step 7: `<script>` 태그 시작 — 상수 + 전역변수 선언**

```javascript
var API = 'https://api.upbit.com/v1';
var EXCLUDE_COINS = ['USDT','USDC','DAI','TUSD','BUSD','USDD','XAUT','TRX'];

// === 시스템 상수 ===
var MAX_ACTIVE_COINS = 2;
var MARTINGALE_TRIGGER = -0.02;
var MARTINGALE_MULTIPLIER = 2;
var TP_PCT = 0.015;
var SL_PCT = -0.05;
var MAX_HOLD_BASE = 60;
var MAX_HOLD_PER_STAGE = 30;
var MAX_HOLD_PROFIT_EXT = 30;
var BACKTEST_HOURS = 6;
var BACKTEST_WARMUP = 60;
var HARD_STOP_CAPITAL_PCT = -0.05;
var REEVAL_INTERVAL = 30 * 60000;
var SCREENER_INTERVAL = 10 * 60000;
var MAIN_CYCLE_MS = 60000;
var FEE_RATE = 0.0004;
var FEE_ROUND = 0.0008;
var DAILY_MDD = -0.03;
var CAPITAL_FLOOR = -0.10;
var MAX_DAILY_TRADES = 60;
var TRADE_AMOUNT = 5000;

// === 전역 상태 ===
var accessKey = '', secretKey = '';
var krwBalance = 0, initialCapital = 0, dailyStartCapital = 0;
var screenedCoins = [];
var selectedCoins = {};  // { 'KRW-BTC': true, ... } localStorage 저장
var coinAssignments = {}; // { 'KRW-BTC': { strategyIdx: 0, score: 0.85, pnlPct: 0.02, winRate: 0.6, trades: 5, assignedAt: timestamp } }
var livePositions = {};   // { 'KRW-BTC': { market, symbol, strategyName, stages:[], avgPrice, totalQty, totalInvested, currentStage, maxStage, entryTime } }
var systemRunning = false, autoTradeOn = false;
var dailyLivePnl = 0, dailyLiveTrades = 0, lastLiveTradeTime = 0;
var totalLiveTrades = 0;
var lastDailyDate = new Date().toDateString();
var systemStartTime = 0;
var logEntries = [];
var mainLoopTimer = null, screenerTimer = null, reevalTimer = null, uptimeTimer = null;
var lastReevalTime = 0;
```

- [ ] **Step 8: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): HTML/CSS 기본 구조 + 헤더/컨트롤바/섹션 셸"
```

---

### Task 2: API/유틸리티/IndexedDB 코드 복사

- [ ] **Step 1: counter-trade13에서 다음 함수들을 그대로 복사**

복사 대상 (변경 없이):
- `apiFetch`, `base64url`, `uuidv4`, `sha512`, `generateJWT`, `apiAuth`
- `sleep`, `getBgTimerWorker`, `bgSleep`
- `calcRSI`, `calcBB`, `calcEMA`, `calcATR`
- `showToast`, `formatPct`, `formatKRW`
- `fetchHistoricalCandles`

- [ ] **Step 2: MACD 계산 함수 추가 (신규)**

```javascript
function calcMACD(closes, fast, slow, signal) {
  var emaFast = calcEMA(closes, fast);
  var emaSlow = calcEMA(closes, slow);
  var macdLine = [], offset = slow - fast;
  for (var i = 0; i < emaFast.length; i++) {
    if (i >= offset) macdLine.push(emaFast[i] - emaSlow[i - offset]);
  }
  var signalLine = calcEMA(macdLine, signal);
  var histogram = [];
  var sigOffset = macdLine.length - signalLine.length;
  for (var i = 0; i < signalLine.length; i++) {
    histogram.push(macdLine[i + sigOffset] - signalLine[i]);
  }
  return { macd: macdLine, signal: signalLine, histogram: histogram };
}
```

- [ ] **Step 3: IndexedDB (DB_NAME 변경)**

counter-trade13의 IndexedDB 코드를 복사하되:
- `DB_NAME = 'ct14_trades'` (ct13과 충돌 방지)
- store 구조: `live` (매매이력), `state` (상태 저장)
- `paper` store 제거 (ct14에는 페이퍼 트레이딩 없음)

```javascript
var DB_NAME = 'ct14_trades';
var DB_VERSION = 1;
```

openDB의 onupgradeneeded에서 `paper` store 생성 제거.

- [ ] **Step 4: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): API/유틸리티/IndexedDB 코드 복사 + calcMACD 추가"
```

---

## Chunk 2: 전략 엔진 + 스크리너

### Task 3: 6개 전략 함수 정의

- [ ] **Step 1: 전략 배열 + 공통 구조**

```javascript
var STRATEGIES = [
  { name: 'RSI반전',     fn: stratRSIReversal },
  { name: 'BB브레이크',   fn: stratBBBreakout },
  { name: 'EMA추세',     fn: stratEMACross },
  { name: '거래량급등',   fn: stratVolSurge },
  { name: '핀바반전',     fn: stratPinBar },
  { name: 'MACD괴리',    fn: stratMACDDiv }
];
```

각 전략 함수는 다음 인터페이스를 따름:
```javascript
// 입력: closes, opens, highs, lows, volumes, idx (확인 완료된 봉 인덱스)
// 출력: { signal: boolean, reason: string } 또는 null
function stratXxx(closes, opens, highs, lows, volumes, idx) { ... }
```

- [ ] **Step 2: RSI 반전 전략**

counter-trade13의 stratRSIReversal 로직을 간소화 (매수 신호만 반환):
```javascript
function stratRSIReversal(closes, opens, highs, lows, volumes, idx) {
  var rsi = calcRSI(closes, 7);
  var ri = idx - (closes.length - rsi.length);
  if (ri < 1) return null;
  if (rsi[ri] <= 25 && rsi[ri] > rsi[ri - 1]) {
    return { signal: true, reason: 'RSI(' + rsi[ri].toFixed(0) + ')반전' };
  }
  return null;
}
```

- [ ] **Step 3: BB 브레이크아웃 전략**

```javascript
function stratBBBreakout(closes, opens, highs, lows, volumes, idx) {
  var bb = calcBB(closes, 20, 2);
  var rsi = calcRSI(closes, 14);
  var bi = idx - (closes.length - bb.mid.length);
  var ri = idx - (closes.length - rsi.length);
  if (bi < 50 || ri < 0) return null;
  var bws = [];
  for (var j = bi - 49; j <= bi; j++) {
    if (j >= 0) bws.push((bb.upper[j] - bb.lower[j]) / bb.mid[j]);
  }
  bws.sort(function(a,b){return a-b;});
  var th = bws[Math.floor(bws.length * 0.2)];
  var curBW = (bb.upper[bi] - bb.lower[bi]) / bb.mid[bi];
  if (curBW <= th && closes[idx] > bb.upper[bi] && rsi[ri] < 65) {
    return { signal: true, reason: 'BB스퀴즈돌파' };
  }
  return null;
}
```

- [ ] **Step 4: EMA 추세추종 전략**

```javascript
function stratEMACross(closes, opens, highs, lows, volumes, idx) {
  var ema5 = calcEMA(closes, 5);
  var ema13 = calcEMA(closes, 13);
  var ema55 = calcEMA(closes, 55);
  var rsi = calcRSI(closes, 14);
  var i5 = idx - (closes.length - ema5.length);
  var i13 = idx - (closes.length - ema13.length);
  var i55 = idx - (closes.length - ema55.length);
  var ri = idx - (closes.length - rsi.length);
  if (i5 < 1 || i13 < 2 || i55 < 0 || ri < 0) return null;
  var avgVol = 0;
  for (var v = idx - 19; v <= idx; v++) { if (v >= 0) avgVol += volumes[v]; }
  avgVol /= 20;
  if (ema5[i5] > ema13[i13] && ema5[i5 - 1] <= ema13[i13 - 1]
    && rsi[ri] >= 45 && rsi[ri] <= 62
    && closes[idx] > ema55[i55]
    && ema13[i13] > ema55[i55]
    && ema13[i13] > ema13[i13 - 1] && ema13[i13 - 1] > ema13[i13 - 2]
    && volumes[idx] > avgVol) {
    return { signal: true, reason: 'EMA골든크로스' };
  }
  return null;
}
```

- [ ] **Step 5: 거래량 급등 전략**

```javascript
function stratVolSurge(closes, opens, highs, lows, volumes, idx) {
  if (idx < 20) return null;
  var avgVol = 0;
  for (var v = idx - 19; v <= idx; v++) avgVol += volumes[v];
  avgVol /= 20;
  var rsi = calcRSI(closes, 14);
  var ri = idx - (closes.length - rsi.length);
  if (ri < 0) return null;
  var maxPrice = 0;
  for (var p = idx - 15; p < idx; p++) { if (p >= 0 && closes[p] > maxPrice) maxPrice = closes[p]; }
  if (volumes[idx] >= avgVol * 5 && closes[idx] > opens[idx]
    && closes[idx] > maxPrice && rsi[ri] < 70) {
    return { signal: true, reason: '거래량' + (volumes[idx]/avgVol).toFixed(0) + 'x급등' };
  }
  return null;
}
```

- [ ] **Step 6: 핀바 반전 전략**

```javascript
function stratPinBar(closes, opens, highs, lows, volumes, idx) {
  if (idx < 20) return null;
  var totalLen = highs[idx] - lows[idx];
  if (totalLen <= 0) return null;
  var bodyTop = Math.max(opens[idx], closes[idx]);
  var bodyBot = Math.min(opens[idx], closes[idx]);
  var bodyLen = bodyTop - bodyBot;
  var lowerWick = bodyBot - lows[idx];
  if (lowerWick / totalLen < 0.66 || bodyLen / totalLen > 0.20) return null;
  if (!(closes[idx - 3] > closes[idx - 2] && closes[idx - 2] > closes[idx - 1])) return null;
  var avgVol = 0;
  for (var v = idx - 19; v <= idx; v++) avgVol += volumes[v];
  avgVol /= 20;
  if (volumes[idx] >= avgVol * 1.5) {
    return { signal: true, reason: '핀바(꼬리' + (lowerWick/totalLen*100).toFixed(0) + '%)' };
  }
  return null;
}
```

- [ ] **Step 7: MACD 다이버전스 전략 (신규)**

```javascript
function stratMACDDiv(closes, opens, highs, lows, volumes, idx) {
  var macd = calcMACD(closes, 12, 26, 9);
  var hi = idx - (closes.length - macd.histogram.length);
  if (hi < 40) return null;
  // 최근 40봉에서 히스토그램 음수 구간의 저점 2개를 찾음
  var troughs = []; // { idx: global_idx, histVal: ..., priceVal: ... }
  var inNeg = false, trough = null;
  for (var j = hi - 39; j <= hi; j++) {
    if (macd.histogram[j] < 0) {
      if (!inNeg) { inNeg = true; trough = { hIdx: j, histVal: macd.histogram[j], priceIdx: j + (closes.length - macd.histogram.length) }; }
      else if (macd.histogram[j] < trough.histVal) { trough.histVal = macd.histogram[j]; trough.hIdx = j; trough.priceIdx = j + (closes.length - macd.histogram.length); }
    } else {
      if (inNeg && trough) { trough.priceVal = closes[trough.priceIdx]; troughs.push(trough); }
      inNeg = false; trough = null;
    }
  }
  if (troughs.length < 2) return null;
  var prev = troughs[troughs.length - 2], curr = troughs[troughs.length - 1];
  // 가격 저점 갱신 BUT MACD 히스토그램 저점 미갱신 + 현재 양전환
  if (curr.priceVal < prev.priceVal && curr.histVal > prev.histVal && macd.histogram[hi] > 0 && macd.histogram[hi - 1] <= 0) {
    return { signal: true, reason: 'MACD상승괴리' };
  }
  return null;
}
```

- [ ] **Step 8: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): 6개 전략 함수 정의 (RSI/BB/EMA/Vol/PinBar/MACD)"
```

---

### Task 4: 스크리너 + 코인 선택 UI

- [ ] **Step 1: 스크리너 함수**

counter-trade13의 `runScreener()`를 복사하되:
- `renderScreenerPanel()` 호출을 `renderScreener()` 로 변경
- 선별 후 체크박스 상태를 localStorage에서 복원

```javascript
async function runScreener() {
  // ... (counter-trade13과 동일한 로직)
  // 마지막에:
  screenedCoins = scored.slice(0, 30);
  loadCoinSelection();  // localStorage에서 선택 상태 복원
  renderScreener();
}
```

- [ ] **Step 2: 코인 선택 관리 함수**

```javascript
function loadCoinSelection() {
  try {
    var saved = JSON.parse(localStorage.getItem('ct14_selected_coins') || '{}');
    selectedCoins = saved;
  } catch(e) { selectedCoins = {}; }
}
function saveCoinSelection() {
  localStorage.setItem('ct14_selected_coins', JSON.stringify(selectedCoins));
}
function toggleCoin(market) {
  selectedCoins[market] = !selectedCoins[market];
  saveCoinSelection();
  renderScreener();
  updateStatsBar();
}
function selectAllCoins() {
  screenedCoins.forEach(function(c) { selectedCoins[c.market] = true; });
  saveCoinSelection(); renderScreener(); updateStatsBar();
}
function deselectAllCoins() {
  // 포지션 있는 코인은 유지
  screenedCoins.forEach(function(c) { if (!livePositions[c.market]) selectedCoins[c.market] = false; });
  saveCoinSelection(); renderScreener(); updateStatsBar();
}
function getSelectedCoins() {
  return screenedCoins.filter(function(c) { return selectedCoins[c.market]; });
}
```

- [ ] **Step 3: 스크리너 렌더링 (체크박스 + 배정전략 표시)**

```javascript
function renderScreener() {
  var body = document.getElementById('screenerBody');
  if (!body) return;
  body.innerHTML = '';
  screenedCoins.forEach(function(c, i) {
    var checked = selectedCoins[c.market] ? 'checked' : '';
    var hasPos = livePositions[c.market] ? true : false;
    var assign = coinAssignments[c.market];
    var stratName = assign ? assign.strategyName : '-';
    var btScore = assign ? assign.score.toFixed(2) : '-';
    var staleClass = '';
    // 스크리너에서 빠졌지만 선택된 코인은 회색
    var tr = document.createElement('tr');
    tr.className = staleClass;
    tr.innerHTML = '<td><input type="checkbox" class="coin-check" ' + checked + ' onchange="toggleCoin(\'' + c.market + '\')" ' + (hasPos ? 'disabled' : '') + '></td>'
      + '<td>' + (i+1) + '</td>'
      + '<td><strong>' + c.symbol + '</strong> <span style="color:var(--muted);font-size:10px">' + c.name + '</span></td>'
      + '<td class="mono">' + formatKRW(c.price) + '</td>'
      + '<td class="mono">' + (c.vol24h / 1e8).toFixed(0) + '억</td>'
      + '<td class="mono">' + (c.atrPct * 100).toFixed(2) + '%</td>'
      + '<td><span class="mode-badge ' + (assign ? 'live' : 'paper') + '">' + stratName + '</span></td>'
      + '<td class="mono">' + btScore + '</td>';
    body.appendChild(tr);
  });
  document.getElementById('selectedCount').textContent = getSelectedCoins().length + '/' + screenedCoins.length + ' 선택';
}
```

- [ ] **Step 4: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): 스크리너 + 코인 체크박스 선택 UI"
```

---

## Chunk 3: 백테스트 + 전략 배정

### Task 5: 백테스트 엔진

- [ ] **Step 1: 단일 코인/단일 전략 백테스트 함수**

```javascript
function backtestStrategy(stratIdx, closes, opens, highs, lows, volumes) {
  var strat = STRATEGIES[stratIdx];
  var trades = [];
  var inPos = false, entryPrice = 0, entryIdx = 0;

  for (var i = BACKTEST_WARMUP; i < closes.length - 1; i++) {
    if (!inPos) {
      var sig = strat.fn(closes, opens, highs, lows, volumes, i);
      if (sig && sig.signal) {
        entryPrice = closes[i] * (1 + FEE_RATE); // 슬리피지+수수료
        entryIdx = i;
        inPos = true;
      }
    } else {
      var pct = (closes[i] - entryPrice) / entryPrice;
      var holdBars = i - entryIdx;
      // 익절
      if (pct >= TP_PCT) {
        trades.push({ pnl: TP_PCT - FEE_ROUND, bars: holdBars, reason: 'TP' });
        inPos = false; continue;
      }
      // 손절 (DCA 없이 단순 백테스트에서는 -2%를 SL로 사용)
      if (pct <= -0.02) {
        trades.push({ pnl: pct - FEE_ROUND, bars: holdBars, reason: 'SL' });
        inPos = false; continue;
      }
      // 타임아웃 (60봉)
      if (holdBars >= 60) {
        trades.push({ pnl: pct - FEE_ROUND, bars: holdBars, reason: 'TIMEOUT' });
        inPos = false; continue;
      }
    }
  }
  // 미청산 포지션 무시
  return trades;
}
```

- [ ] **Step 2: 코인별 6개 전략 백테스트 + 점수 산출**

```javascript
async function evaluateStrategiesForCoin(coin) {
  var candles = await fetchHistoricalCandles(coin.market, 1, BACKTEST_HOURS / 24 + 0.1);
  if (!candles || candles.length < BACKTEST_WARMUP + 60) return null;
  candles.reverse();
  var closes = candles.map(function(c){return c.trade_price;});
  var opens = candles.map(function(c){return c.opening_price;});
  var highs = candles.map(function(c){return c.high_price;});
  var lows = candles.map(function(c){return c.low_price;});
  var volumes = candles.map(function(c){return c.candle_acc_trade_volume;});

  var results = [];
  for (var si = 0; si < STRATEGIES.length; si++) {
    var trades = backtestStrategy(si, closes, opens, highs, lows, volumes);
    if (trades.length === 0) {
      results.push({ stratIdx: si, pnlPct: 0, winRate: 0, maxLossPct: 0, trades: 0, score: -999 });
      continue;
    }
    var totalPnl = 0, wins = 0, maxLoss = 0;
    trades.forEach(function(t) {
      totalPnl += t.pnl;
      if (t.pnl >= 0) wins++;
      if (t.pnl < maxLoss) maxLoss = t.pnl;
    });
    results.push({
      stratIdx: si, pnlPct: totalPnl, winRate: wins / trades.length,
      maxLossPct: maxLoss, trades: trades.length, score: 0
    });
  }

  // 정규화 (min-max scaling)
  var validResults = results.filter(function(r){return r.trades > 0;});
  if (validResults.length === 0) return null;

  var minPnl = Infinity, maxPnl = -Infinity, minLoss = Infinity, maxLoss = -Infinity;
  validResults.forEach(function(r) {
    if (r.pnlPct < minPnl) minPnl = r.pnlPct;
    if (r.pnlPct > maxPnl) maxPnl = r.pnlPct;
    var absLoss = Math.abs(r.maxLossPct);
    if (absLoss < minLoss) minLoss = absLoss;
    if (absLoss > maxLoss) maxLoss = absLoss;
  });

  var pnlRange = maxPnl - minPnl || 1;
  var lossRange = maxLoss - minLoss || 1;
  results.forEach(function(r) {
    if (r.trades === 0) return;
    var normPnl = (r.pnlPct - minPnl) / pnlRange;
    var normWR = r.winRate;
    var normSafe = 1 - (Math.abs(r.maxLossPct) - minLoss) / lossRange;
    r.score = normPnl * 0.5 + normWR * 0.3 + normSafe * 0.2;
  });

  // 최고 점수 전략 선택
  results.sort(function(a,b){return b.score - a.score;});
  var best = results[0];
  if (best.score <= 0 || best.trades === 0) return null;
  return {
    strategyIdx: best.stratIdx,
    strategyName: STRATEGIES[best.stratIdx].name,
    score: best.score,
    pnlPct: best.pnlPct,
    winRate: best.winRate,
    trades: best.trades,
    allResults: results
  };
}
```

- [ ] **Step 3: 전체 선택 코인 백테스트 + 배정 실행**

```javascript
async function runStrategyAssignment() {
  var selected = getSelectedCoins();
  if (selected.length === 0) { addLog('warn', '선택된 코인 없음'); return; }
  addLog('info', '전략 배정 백테스트 시작 (' + selected.length + '개 코인)');
  var progFill = document.getElementById('progressFill');
  var progText = document.getElementById('progressText');

  for (var i = 0; i < selected.length; i++) {
    var coin = selected[i];
    progText.textContent = '백테스트 ' + coin.symbol + ' (' + (i+1) + '/' + selected.length + ')';
    progFill.style.width = ((i+1)/selected.length*100) + '%';
    try {
      var result = await evaluateStrategiesForCoin(coin);
      if (result) {
        coinAssignments[coin.market] = {
          strategyIdx: result.strategyIdx,
          strategyName: result.strategyName,
          score: result.score,
          pnlPct: result.pnlPct,
          winRate: result.winRate,
          trades: result.trades,
          assignedAt: Date.now()
        };
        addLog('success', coin.symbol + ' → ' + result.strategyName + ' (점수:' + result.score.toFixed(2) + ' 승률:' + (result.winRate*100).toFixed(0) + '%)');
      } else {
        delete coinAssignments[coin.market];
        addLog('warn', coin.symbol + ' — 적합 전략 없음');
      }
    } catch(e) {
      addLog('error', coin.symbol + ' 백테스트 오류: ' + e.message);
    }
    await bgSleep(200);
  }
  lastReevalTime = Date.now();
  progText.textContent = '전략 배정 완료';
  renderScreener();
  renderAssignBoard();
}
```

- [ ] **Step 4: 전략 배정 카드 UI 렌더링**

```javascript
function renderAssignBoard() {
  var el = document.getElementById('assignBoard');
  if (!el) return;
  el.innerHTML = '';
  var selected = getSelectedCoins();
  selected.forEach(function(coin) {
    var assign = coinAssignments[coin.market];
    if (!assign) return;
    var hasPos = livePositions[coin.market] ? true : false;
    var card = document.createElement('div');
    card.className = 'assign-card' + (hasPos ? ' active' : '');
    var timeSince = assign.assignedAt ? Math.floor((Date.now() - assign.assignedAt)/60000) : 0;
    var nextReeval = Math.max(0, Math.floor((REEVAL_INTERVAL - (Date.now() - lastReevalTime))/60000));
    card.innerHTML = '<div class="card-header">'
      + '<span class="strat-name">' + coin.symbol + '</span>'
      + '<span class="strat-status live">' + assign.strategyName + '</span></div>'
      + '<div class="card-stats">'
      + '<div><span class="stat-label">BT점수</span><span class="stat-value">' + assign.score.toFixed(2) + '</span></div>'
      + '<div><span class="stat-label">승률</span><span class="stat-value">' + (assign.winRate*100).toFixed(0) + '%</span></div>'
      + '<div><span class="stat-label">수익률</span><span class="stat-value ' + (assign.pnlPct>=0?'up':'down') + '">' + formatPct(assign.pnlPct) + '</span></div>'
      + '<div><span class="stat-label">배정</span><span class="stat-value">' + timeSince + 'm전</span></div>'
      + '</div>';
    el.appendChild(card);
  });
  // 다음 재평가 시간 표시
  var nextReeval = Math.max(0, Math.floor((REEVAL_INTERVAL - (Date.now() - lastReevalTime))/60000));
  document.getElementById('nextReeval').textContent = '재평가: ' + nextReeval + '분 후';
}
```

- [ ] **Step 5: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): 백테스트 엔진 + 정규화 점수 + 전략 자동 배정"
```

---

## Chunk 4: 마틴게일 매매 엔진

### Task 6: 마틴게일 포지션 관리

- [ ] **Step 1: 가용잔고 계산**

```javascript
function getAvailableBalance() {
  var reserved = 0;
  for (var market in livePositions) {
    var pos = livePositions[market];
    // 남은 물타기 단계의 예약 금액 합산
    for (var s = pos.currentStage + 1; s <= pos.maxStage; s++) {
      reserved += TRADE_AMOUNT * Math.pow(MARTINGALE_MULTIPLIER, s - 1);
    }
  }
  return Math.max(0, krwBalance - reserved);
}

function calcMaxStage(availBalance) {
  var stage = 1, cumulative = TRADE_AMOUNT;
  while (true) {
    var nextAmt = TRADE_AMOUNT * Math.pow(MARTINGALE_MULTIPLIER, stage);
    if (cumulative + nextAmt > availBalance) break;
    stage++;
    cumulative += nextAmt;
  }
  return stage;
}
```

- [ ] **Step 2: 1차 진입 (매수 신호 발생 시)**

```javascript
async function openPosition(coin, strategyName) {
  var available = getAvailableBalance();
  if (TRADE_AMOUNT > available) { addLog('warn', coin.symbol + ' 가용잔고 부족'); return false; }
  if (TRADE_AMOUNT > krwBalance) { addLog('warn', coin.symbol + ' 잔고 부족'); return false; }

  var result = await executeBuy(coin.market, coin.symbol, TRADE_AMOUNT);
  if (!result || result.volume <= 0) return false;

  livePositions[coin.market] = {
    market: coin.market,
    symbol: coin.symbol,
    strategyName: strategyName,
    stages: [{ price: result.price, qty: result.volume, amount: TRADE_AMOUNT, time: Date.now() }],
    avgPrice: result.price,
    totalQty: result.volume,
    totalInvested: TRADE_AMOUNT,
    currentStage: 1,
    maxStage: calcMaxStage(getAvailableBalance()),
    entryTime: Date.now()
  };
  addLog('success', '[매수][' + strategyName + '] ' + coin.symbol + ' 1단계 ' + formatKRW(TRADE_AMOUNT));
  return true;
}
```

- [ ] **Step 3: 물타기 (N차 추가 매수)**

```javascript
async function addMartingaleStage(market) {
  var pos = livePositions[market];
  if (!pos) return false;
  var nextStageAmt = TRADE_AMOUNT * Math.pow(MARTINGALE_MULTIPLIER, pos.currentStage);
  if (nextStageAmt < 5000) nextStageAmt = 5000;
  if (nextStageAmt > krwBalance) { addLog('warn', pos.symbol + ' 물타기 잔고부족'); return false; }

  var result = await executeBuy(pos.market, pos.symbol, nextStageAmt);
  if (!result || result.volume <= 0) return false;

  pos.stages.push({ price: result.price, qty: result.volume, amount: nextStageAmt, time: Date.now() });
  pos.totalQty += result.volume;
  pos.totalInvested += nextStageAmt;
  pos.avgPrice = pos.totalInvested / pos.totalQty;
  pos.currentStage++;
  pos.maxStage = calcMaxStage(getAvailableBalance());

  addLog('info', '[물타기][' + pos.strategyName + '] ' + pos.symbol + ' ' + pos.currentStage + '단계 ' + formatKRW(nextStageAmt) + ' 평단:' + formatKRW(pos.avgPrice));
  return true;
}
```

- [ ] **Step 4: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): 마틴게일 포지션 관리 (진입/물타기/가용잔고)"
```

---

### Task 7: 매도 엔진 (익절/손절/타임아웃/하드스탑)

- [ ] **Step 1: executeBuy, executeSell 함수**

counter-trade13의 `executeBuy`, `executeSell`, `checkTradeKey` 를 그대로 복사.
`dailyLiveTrades++; lastLiveTradeTime = Date.now();` 부분도 포함.

- [ ] **Step 2: 포지션 매도 체크 함수**

```javascript
async function checkPositionExit(market) {
  var pos = livePositions[market];
  if (!pos) return;

  var candles = await apiFetch('/candles/minutes/1?market=' + market + '&count=5');
  if (!candles || candles.length === 0) return;
  var currentPrice = candles[0].trade_price;
  var pct = (currentPrice - pos.avgPrice) / pos.avgPrice;
  var holdMin = (Date.now() - pos.entryTime) / 60000;
  var maxHold = MAX_HOLD_BASE + (pos.currentStage - 1) * MAX_HOLD_PER_STAGE;
  if (pct > 0) maxHold += MAX_HOLD_PROFIT_EXT;
  var unrealizedLoss = (currentPrice - pos.avgPrice) * pos.totalQty;
  var capitalPct = initialCapital > 0 ? unrealizedLoss / initialCapital : 0;

  // 1) 익절
  if (pct >= TP_PCT) {
    await closePosition(market, currentPrice, 'TP(' + formatPct(TP_PCT) + ')');
    return;
  }

  // 2) 하드 스탑 (총자본 대비)
  if (capitalPct <= HARD_STOP_CAPITAL_PCT) {
    await closePosition(market, currentPrice, 'HARD_STOP(자본' + formatPct(capitalPct) + ')');
    return;
  }

  // 3) 물타기 트리거 체크
  if (pct <= MARTINGALE_TRIGGER && pos.currentStage < pos.maxStage) {
    var canAdd = await addMartingaleStage(market);
    if (canAdd) return; // 물타기 성공 — 매도 안 함
  }

  // 4) 최종 단계 후 손절
  if (pct <= SL_PCT && pos.currentStage >= pos.maxStage) {
    await closePosition(market, currentPrice, 'SL(' + formatPct(pct) + ')');
    return;
  }

  // 5) 타임아웃
  if (holdMin >= maxHold) {
    await closePosition(market, currentPrice, 'TIMEOUT(' + holdMin.toFixed(0) + 'm)');
    return;
  }

  addLog('info', '[체크] ' + pos.symbol + ' 손익=' + formatPct(pct) + ' 단계=' + pos.currentStage + '/' + pos.maxStage + ' 보유=' + holdMin.toFixed(0) + 'm/' + maxHold + 'm');
}
```

- [ ] **Step 3: 포지션 청산 함수**

```javascript
async function closePosition(market, currentPrice, reason) {
  var pos = livePositions[market];
  if (!pos) return;

  var sellResult = await executeSell(pos.market, pos.symbol, pos.totalQty);
  var exitPrice = sellResult ? sellResult.price : currentPrice;
  var pnl = (exitPrice - pos.avgPrice) / pos.avgPrice - FEE_ROUND;
  var pnlKRW = (exitPrice - pos.avgPrice) * pos.totalQty;

  await saveTrade('live', {
    market: pos.market, symbol: pos.symbol, strategyName: pos.strategyName,
    stages: pos.currentStage, avgPrice: pos.avgPrice, exitPrice: exitPrice,
    totalInvested: pos.totalInvested, pnl: pnl, pnlKRW: pnlKRW,
    entryTime: pos.entryTime, exitTime: Date.now(), reason: reason
  });

  dailyLivePnl += pnlKRW;
  totalLiveTrades++;
  addLog(pnl >= 0 ? 'success' : 'error',
    '[청산][' + pos.strategyName + '] ' + pos.symbol + ' ' + reason + ' ' + formatPct(pnl) + ' (' + formatKRW(pnlKRW) + ')');
  delete livePositions[market];
  await loadBalance();
  renderPositions();
  renderTradeHistory();
}
```

- [ ] **Step 4: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): 매도 엔진 (익절/손절/물타기/하드스탑/타임아웃)"
```

---

## Chunk 5: 메인 루프 + 리스크 관리 + 영속성

### Task 8: 메인 루프 + 매매 사이클

- [ ] **Step 1: 매매 사이클 함수**

```javascript
async function tradingCycle() {
  // 1) 기존 포지션 매도 체크 (가드와 무관하게 항상 실행)
  var posKeys = Object.keys(livePositions);
  for (var i = 0; i < posKeys.length; i++) {
    await checkPositionExit(posKeys[i]);
    await bgSleep(150);
  }

  // 2) 매수 가드 체크
  if (!autoTradeOn) return;
  if (!await checkTradeKey()) { addLog('error', 'key.json autoTrade=false'); return; }
  if (checkDailyMDD()) { addLog('warn', '일일 MDD 초과 — 매수중단'); return; }
  if (checkCapitalFloor()) { addLog('warn', '절대 자본 하한 — 매수중단'); return; }
  if (dailyLiveTrades >= MAX_DAILY_TRADES) { addLog('warn', '일일 최대매매 초과'); return; }
  if (Object.keys(livePositions).length >= MAX_ACTIVE_COINS) return;

  await loadBalance();

  // 3) 전략 배정된 코인 중 매수 신호 탐색
  var selected = getSelectedCoins();
  for (var ci = 0; ci < selected.length; ci++) {
    var coin = selected[ci];
    if (livePositions[coin.market]) continue; // 이미 포지션 있음
    if (Object.keys(livePositions).length >= MAX_ACTIVE_COINS) break;
    var assign = coinAssignments[coin.market];
    if (!assign) continue; // 전략 미배정

    try {
      var candles = await apiFetch('/candles/minutes/1?market=' + coin.market + '&count=200');
      if (!candles || candles.length < 60) continue;
      candles.reverse();
      var closes = candles.map(function(c){return c.trade_price;});
      var opens = candles.map(function(c){return c.opening_price;});
      var highs = candles.map(function(c){return c.high_price;});
      var lows = candles.map(function(c){return c.low_price;});
      var volumes = candles.map(function(c){return c.candle_acc_trade_volume;});
      var lastIdx = closes.length - 2;

      var sig = STRATEGIES[assign.strategyIdx].fn(closes, opens, highs, lows, volumes, lastIdx);
      if (sig && sig.signal) {
        var success = await openPosition(coin, assign.strategyName);
        if (success) {
          dailyLiveTrades++; lastLiveTradeTime = Date.now();
        }
      }
    } catch(e) { console.error('Trading err:', coin.symbol, e); }
    await bgSleep(150);
  }
}
```

- [ ] **Step 2: 리스크 관리 함수**

counter-trade13에서 복사 + 수정:
```javascript
function checkDailyMDD() {
  if (dailyStartCapital <= 0) return false;
  var tradedToday = lastLiveTradeTime > 0 && new Date(lastLiveTradeTime).toDateString() === new Date().toDateString();
  if (!tradedToday && Object.keys(livePositions).length === 0) {
    dailyStartCapital = krwBalance;
    return false;
  }
  return (krwBalance - dailyStartCapital) / dailyStartCapital <= DAILY_MDD;
}
function checkCapitalFloor() {
  if (initialCapital <= 0) return false;
  return (krwBalance - initialCapital) / initialCapital <= CAPITAL_FLOOR;
}
function checkDailyReset() {
  var today = new Date().toDateString();
  if (lastDailyDate !== today) {
    addLog('info', '자정 경과 — 일일 카운터 리셋');
    dailyLivePnl = 0; dailyLiveTrades = 0;
    dailyStartCapital = krwBalance;
    lastDailyDate = today; lastLiveTradeTime = 0;
  }
}
```

- [ ] **Step 3: Trade Readiness 체커**

counter-trade13의 `checkTradeReadiness()` 함수를 복사하되:
- 승격전략 → 전략배정 코인 수로 변경
- 포지션 기준 MAX_ACTIVE_COINS로 변경

- [ ] **Step 4: 메인 루프 + 시작/정지**

```javascript
async function startSystem() {
  if (systemRunning) return;
  systemRunning = true;
  systemStartTime = Date.now();
  addLog('info', '=== 시스템 시작 ===');
  document.getElementById('startBtn').disabled = true;
  document.getElementById('stopBtn').disabled = false;
  document.getElementById('systemStatus').textContent = 'RUNNING';
  document.getElementById('systemStatus').className = 'mono status-on';

  await loadKeys();
  var restored = await restoreState();

  if (accessKey && secretKey) {
    try {
      await loadBalance();
      if (!restored || initialCapital <= 0) initialCapital = krwBalance;
      if (!restored || dailyStartCapital <= 0 || (dailyLiveTrades === 0 && Object.keys(livePositions).length === 0)) {
        dailyStartCapital = krwBalance;
        addLog('info', '일일 시작자본 설정: ' + formatKRW(krwBalance));
      }
    } catch(e) {}
  }

  await runScreener();
  if (getSelectedCoins().length > 0) {
    await runStrategyAssignment();
  }
  await checkTradeReadiness();

  addLog('info', '메인 루프 시작 (' + (MAIN_CYCLE_MS/1000) + '초 주기)');
  mainLoop();
  screenerTimer = setInterval(function(){ runScreener(); }, SCREENER_INTERVAL);
  reevalTimer = setInterval(function(){ runStrategyAssignment(); }, REEVAL_INTERVAL);
  uptimeTimer = setInterval(function(){
    var sec = Math.floor((Date.now()-systemStartTime)/1000);
    var h = Math.floor(sec/3600), m = Math.floor((sec%3600)/60);
    document.getElementById('uptimeDisplay').textContent = 'Uptime: ' + h + 'h ' + m + 'm';
  }, 10000);
}

async function mainLoop() {
  if (!systemRunning) return;
  try {
    checkDailyReset();
    await tradingCycle();
    renderPositions();
    updateStatsBar();
    await checkTradeReadiness();
    await persistState();
  } catch(e) { addLog('error', '루프 오류: ' + e.message); }
  if (systemRunning) {
    mainLoopTimer = setTimeout(function(){ mainLoop(); }, MAIN_CYCLE_MS);
  }
}

function stopSystem() {
  systemRunning = false;
  clearTimeout(mainLoopTimer);
  clearInterval(screenerTimer);
  clearInterval(reevalTimer);
  clearInterval(uptimeTimer);
  document.getElementById('startBtn').disabled = false;
  document.getElementById('stopBtn').disabled = true;
  document.getElementById('systemStatus').textContent = 'STOPPED';
  document.getElementById('systemStatus').className = 'mono status-off';
  addLog('info', '=== 시스템 정지 ===');
  checkTradeReadiness();
}
```

- [ ] **Step 5: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): 메인 루프 + 매매 사이클 + 리스크 관리"
```

---

### Task 9: 영속성 (저장/복원) + UI 이벤트

- [ ] **Step 1: persistState / restoreState**

```javascript
async function persistState() {
  try {
    await saveState('livePositions', livePositions);
    await saveState('coinAssignments', coinAssignments);
    await saveState('counters', {
      totalLiveTrades: totalLiveTrades,
      dailyLivePnl: dailyLivePnl, dailyLiveTrades: dailyLiveTrades,
      initialCapital: initialCapital, dailyStartCapital: dailyStartCapital,
      tradeAmount: TRADE_AMOUNT, tpPct: TP_PCT,
      lastLiveTradeTime: lastLiveTradeTime,
      lastReevalTime: lastReevalTime
    });
    await saveState('logEntries', logEntries.slice(0, 100));
  } catch(e) { console.error('persistState err:', e); }
}

async function restoreState() {
  try {
    var lp = await loadState('livePositions');
    if (lp) livePositions = lp;
    var ca = await loadState('coinAssignments');
    if (ca) coinAssignments = ca;
    var c = await loadState('counters');
    if (c) {
      totalLiveTrades = c.totalLiveTrades || 0;
      initialCapital = c.initialCapital || 0;
      lastReevalTime = c.lastReevalTime || 0;
      if (c.tradeAmount) { TRADE_AMOUNT = c.tradeAmount; document.getElementById('tradeAmountInput').value = TRADE_AMOUNT; }
      if (c.tpPct) { TP_PCT = c.tpPct; document.getElementById('tpPctInput').value = (TP_PCT * 100).toFixed(1); }
      lastLiveTradeTime = c.lastLiveTradeTime || 0;
      // 오늘 매매 여부로 일일 카운터 결정
      var today = new Date().toDateString();
      var tradedToday = lastLiveTradeTime > 0 && new Date(lastLiveTradeTime).toDateString() === today;
      if (tradedToday) {
        dailyLivePnl = c.dailyLivePnl || 0;
        dailyLiveTrades = c.dailyLiveTrades || 0;
        dailyStartCapital = c.dailyStartCapital || 0;
      } else {
        dailyLivePnl = 0; dailyLiveTrades = 0; dailyStartCapital = 0;
        addLog('info', '오늘 매매 없음 — 일일 카운터 리셋');
      }
    }
    var logs = await loadState('logEntries');
    if (logs && logs.length > 0) logEntries = logs;
    return Object.keys(livePositions).length > 0 || (c != null);
  } catch(e) { return false; }
}
```

- [ ] **Step 2: UI 이벤트 핸들러**

```javascript
function onAutoTradeToggle() {
  var checked = document.getElementById('autoTradeToggle').checked;
  if (checked && (!accessKey || !secretKey)) {
    document.getElementById('autoTradeToggle').checked = false;
    showToast('API Key를 먼저 설정하세요', 'error'); return;
  }
  autoTradeOn = checked;
  addLog('info', autoTradeOn ? '*** 자동매매 ON ***' : '자동매매 OFF');
  checkTradeReadiness();
}
function onTradeAmountChange() {
  var val = parseInt(document.getElementById('tradeAmountInput').value) || 5000;
  if (val < 5000) val = 5000;
  TRADE_AMOUNT = val;
  document.getElementById('tradeAmountInput').value = val;
  addLog('info', '매수금액 변경: ' + formatKRW(val));
}
function onTpPctChange() {
  var val = parseFloat(document.getElementById('tpPctInput').value) || 1.5;
  if (val < 0.5) val = 0.5;
  if (val > 10) val = 10;
  TP_PCT = val / 100;
  document.getElementById('tpPctInput').value = val.toFixed(1);
  addLog('info', '익절% 변경: ' + val.toFixed(1) + '%');
}
```

- [ ] **Step 3: 나머지 UI 렌더링 함수들**

```javascript
// 포지션 렌더링 (DCA 상세)
function renderPositions() { ... }
// 매매 이력 렌더링
function renderTradeHistory() { ... }
// 스탯 바 업데이트
function updateStatsBar() { ... }
// 로그 렌더링 + addLog
function addLog(type, msg) { ... }
function renderLog() { ... }
// API 모달/키 관리
// 스크린세이버
// 초기화 함수
```

(이 함수들은 counter-trade13에서 복사 후 변수명만 맞춤)

- [ ] **Step 4: 초기화 코드 (IIFE)**

```javascript
(async function() {
  await loadKeys();
  await openDB();
  var restored = await restoreState();
  if (restored) {
    addLog('success', '=== 이전 세션 상태 복원됨 ===');
    renderPositions();
  }
  loadCoinSelection();
  renderLog();
  updateStatsBar();
  if (accessKey && secretKey) {
    try { await loadBalance(); } catch(e) {}
  }
  checkTradeReadiness();
})();
```

- [ ] **Step 5: 커밋**

```bash
git add src/main/resources/static/html/counter-trade14.html
git commit -m "feat(ct14): 영속성 + UI 이벤트 + 초기화 — counter-trade14 완성"
```

---

### Task 10: 통합 확인 + HomeController 경로 등록

- [ ] **Step 1: HomeController에 경로 추가**

`src/main/java/kr/co/scrab/controller/HomeController.java`에 counter-trade14 경로 추가 (기존 counter-trade13 패턴 참고).

- [ ] **Step 2: 브라우저에서 수동 확인 체크리스트**

1. 페이지 로드 → Trade Readiness 패널 표시 (BLOCKED)
2. API Key 설정 → 잔고 로드
3. 시작 버튼 → 스크리너 실행 → 코인 목록 + 체크박스 표시
4. 코인 선택 → 백테스트 → 전략 배정 카드 표시
5. 자동매매 ON → 매수 신호 대기
6. 정지 → 재시작 → 상태 복원 확인
7. 초기화 → 모든 데이터 클리어

- [ ] **Step 3: 최종 커밋**

```bash
git add -A
git commit -m "feat(ct14): HomeController 경로 등록 — counter-trade14 완성"
```
