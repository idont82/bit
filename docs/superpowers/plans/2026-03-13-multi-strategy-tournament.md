# Multi-Strategy Tournament Scalper Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a multi-strategy tournament scalper that auto-selects coins, runs 5 strategies in parallel via paper trading, and promotes top performers to live trading.

**Architecture:** Single HTML file (`counter-trade13.html`) following existing counter-trade9.html patterns. Client-side JavaScript with Bithumb API v1 (Upbit-compatible). IndexedDB for trade persistence. Web Worker for background timers.

**Tech Stack:** Vanilla JavaScript, Bithumb REST API, IndexedDB, Web Workers, HTML5/CSS3

**Spec:** `docs/superpowers/specs/2026-03-13-multi-strategy-tournament-design.md`

---

## Chunk 1: Foundation (HTML Shell + Utilities + Coin Screener)

### Task 1: HTML Shell & CSS

**Files:**
- Create: `src/main/resources/static/html/counter-trade13.html`

Create the base HTML file with all CSS styles and the UI skeleton. Reuse counter-trade9.html's design system (CSS variables, fonts, dark theme).

- [ ] **Step 1: Create HTML file with head, CSS, and empty body structure**

```html
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Multi-Strategy Tournament Scalper</title>
<!-- Same fonts as ct9 -->
<link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;600;700&family=Noto+Sans+KR:wght@300;400;700&display=swap" rel="stylesheet">
<style>
  /* Reuse ct9 CSS variables and base styles */
  :root {
    --bg: #070b12; --surface: #0d1420; --surface2: #131c2e;
    --border: #1e2d47; --accent: #00d4aa; --accent2: #ff6b35;
    --accent3: #4f8ef7; --text: #e8f0fe; --muted: #5a7090;
    --up: #00d4aa; --down: #ff4757; --gold: #ffd700;
    --purple: #a855f7;
  }
  /* ... (full styles from spec UI layout) ... */
</style>
</head>
```

Body sections (top to bottom per spec):
1. Header: 총 자산, 일일 손익, 가동 시간
2. Control bar: 시작/정지, 설정
3. Stats bar: 핵심 지표 카드
4. Coin screener panel
5. Strategy tournament board (5 strategy cards)
6. Active positions panel
7. Divergence chart area
8. Trade log table

- [ ] **Step 2: Open in browser and verify layout renders**

Verify: All 8 sections visible with placeholder content, dark theme applied, responsive layout.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/html/counter-trade13.html
git commit -m "feat: counter-trade13 HTML shell + CSS foundation"
```

---

### Task 2: Core Utilities (API, Sleep, IndexedDB, Indicators)

**Files:**
- Modify: `src/main/resources/static/html/counter-trade13.html`

Add the `<script>` section with all utility functions reused from ct9 + new IndexedDB layer.

- [ ] **Step 1: Add global constants and variables**

```javascript
// ========== CONSTANTS ==========
var API = 'https://api.bithumb.com/v1';
var FEE_RATE = 0.0004;        // 편도 0.04%
var FEE_ROUND = 0.0008;       // 왕복 0.08%
var BASE_SLIPPAGE = 0.0005;   // 페이퍼 슬리피지 0.05%
var EXCLUDE_COINS = ['USDT','USDC','DAI','TUSD','BUSD','USDD','XAUT','TRX'];

var STRATEGY_NAMES = ['RSI반전', 'BB스퀴즈', '거래량급등', 'EMA크로스', '핀바'];
var MAX_POSITIONS = 2;         // 전체 최대 동시 포지션
var POSITION_PCT = 0.25;       // 전략당 자금의 25%
var STOP_LOSS = -0.005;        // -0.5%
var MAX_HOLD_MIN = 10;         // 최대 보유 10분
var TRAIL_ACTIVATE = 0.003;    // 트레일링 활성화 +0.3%
var TRAIL_PCT = 0.002;         // 트레일링 폭 -0.2%
var COOLDOWN_MS = 3 * 60000;   // 손절 후 3분 쿨다운
var DAILY_MDD = -0.03;         // 일일 MDD -3%
var CAPITAL_FLOOR = -0.10;     // 절대 자본 하한 -10%
var PROMOTE_MIN_TRADES = 10;   // 승격 최소 매매 건수
var DEMOTE_CONSEC_LOSS = 2;    // 강등 연속 손절 횟수
var SCREENER_INTERVAL = 10 * 60000; // 10분
var EVAL_INTERVAL = 30 * 60000;     // 30분
var STARVATION_MS = 4 * 3600000;    // 4시간

// ========== STATE ==========
var accessKey = '', secretKey = '';
var krwBalance = 0;
var initialCapital = 0;
var dailyStartCapital = 0;
var screenedCoins = [];       // 스크리너 결과
var strategies = [];          // 5개 전략 상태
var paperPositions = {};      // {stratIdx_market: position}
var livePositions = {};       // {stratIdx_market: position}
var paperHistory = [];        // 페이퍼 매매 기록
var liveHistory = [];         // 실매매 기록
var backtestBaseline = {};    // 전략별 백테스트 기준값
var systemRunning = false;
var autoTradeOn = false;
var lastPromotionTime = 0;
```

- [ ] **Step 2: Add API, sleep, Web Worker functions (from ct9)**

Copy and adapt from ct9:
- `apiFetch(path)` — public API GET
- `apiAuth(method, path, body)` — authenticated API (JWT signing)
- `sleep(ms)` — MessageChannel fallback
- `getBgTimerWorker()` / `bgSleep(ms)` — Web Worker timer
- `fetchHistoricalCandles(market, tf, count)` — candle fetcher with pagination

- [ ] **Step 3: Add indicator functions (from ct9)**

Copy from ct9:
- `calcRSI(closes, period)` — Wilder's RSI
- `calcBB(closes, period, mult)` — Bollinger Bands (SMA + std)

Add new:
- `calcEMA(closes, period)` — Exponential Moving Average
- `calcATR(highs, lows, closes, period)` — Average True Range

```javascript
function calcEMA(closes, period) {
  if (closes.length < period) return [];
  var k = 2 / (period + 1);
  var ema = [];
  var sum = 0;
  for (var i = 0; i < period; i++) sum += closes[i];
  ema.push(sum / period);
  for (var i = period; i < closes.length; i++) {
    ema.push(closes[i] * k + ema[ema.length - 1] * (1 - k));
  }
  return ema; // length = closes.length - period + 1
}

function calcATR(highs, lows, closes, period) {
  if (closes.length < period + 1) return [];
  var trs = [];
  for (var i = 1; i < closes.length; i++) {
    var tr = Math.max(highs[i] - lows[i], Math.abs(highs[i] - closes[i-1]), Math.abs(lows[i] - closes[i-1]));
    trs.push(tr);
  }
  var atr = [];
  var sum = 0;
  for (var i = 0; i < period; i++) sum += trs[i];
  atr.push(sum / period);
  for (var i = period; i < trs.length; i++) {
    atr.push((atr[atr.length - 1] * (period - 1) + trs[i]) / period);
  }
  return atr; // length = closes.length - period
}
```

- [ ] **Step 4: Add IndexedDB helper**

```javascript
var DB_NAME = 'ct13_trades';
var DB_VERSION = 1;
var _db = null;

function openDB() {
  return new Promise(function(resolve, reject) {
    if (_db) { resolve(_db); return; }
    var req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = function(e) {
      var db = e.target.result;
      if (!db.objectStoreNames.contains('paper')) {
        var s = db.createObjectStore('paper', { keyPath: 'id', autoIncrement: true });
        s.createIndex('time', 'exitTime');
        s.createIndex('strategy', 'stratIdx');
      }
      if (!db.objectStoreNames.contains('live')) {
        var s = db.createObjectStore('live', { keyPath: 'id', autoIncrement: true });
        s.createIndex('time', 'exitTime');
        s.createIndex('strategy', 'stratIdx');
      }
    };
    req.onsuccess = function(e) { _db = e.target.result; resolve(_db); };
    req.onerror = function(e) { reject(e.target.error); };
  });
}

async function saveTrade(store, trade) {
  var db = await openDB();
  return new Promise(function(resolve, reject) {
    var tx = db.transaction(store, 'readwrite');
    tx.objectStore(store).add(trade);
    tx.oncomplete = resolve;
    tx.onerror = function(e) { reject(e.target.error); };
  });
}

async function getRecentTrades(store, since) {
  var db = await openDB();
  return new Promise(function(resolve, reject) {
    var tx = db.transaction(store, 'readonly');
    var idx = tx.objectStore(store).index('time');
    var range = IDBKeyRange.lowerBound(since);
    var results = [];
    idx.openCursor(range).onsuccess = function(e) {
      var cursor = e.target.result;
      if (cursor) { results.push(cursor.value); cursor.continue(); }
      else resolve(results);
    };
    tx.onerror = function(e) { reject(e.target.error); };
  });
}

async function purgeOldTrades(store, olderThan) {
  var db = await openDB();
  return new Promise(function(resolve, reject) {
    var tx = db.transaction(store, 'readwrite');
    var idx = tx.objectStore(store).index('time');
    var range = IDBKeyRange.upperBound(olderThan);
    idx.openCursor(range).onsuccess = function(e) {
      var cursor = e.target.result;
      if (cursor) { cursor.delete(); cursor.continue(); }
      else resolve();
    };
    tx.onerror = function(e) { reject(e.target.error); };
  });
}
```

- [ ] **Step 5: Add helper utilities**

```javascript
function getTickSize(price) {
  if (price >= 2000000) return 1000;
  if (price >= 1000000) return 500;
  if (price >= 500000) return 100;
  if (price >= 100000) return 50;
  if (price >= 10000) return 10;
  if (price >= 1000) return 5;
  if (price >= 100) return 1;
  if (price >= 10) return 0.1;
  return 0.01;
}

function getTickPct(price) {
  return getTickSize(price) / price;
}

function showToast(msg, type) {
  var el = document.getElementById('toast');
  el.textContent = msg;
  el.className = 'toast show ' + (type || '');
  clearTimeout(el._tid);
  el._tid = setTimeout(function() { el.className = 'toast'; }, 3000);
}

function addLog(type, msg) {
  var now = new Date();
  var ts = now.toLocaleTimeString('ko-KR');
  var row = { time: ts, type: type, msg: msg };
  logEntries.unshift(row);
  if (logEntries.length > 200) logEntries.length = 200;
  renderLog();
}

function formatPct(v) { return (v >= 0 ? '+' : '') + (v * 100).toFixed(2) + '%'; }
function formatKRW(v) { return Math.round(v).toLocaleString() + '원'; }
```

- [ ] **Step 6: Verify in browser console — test `calcEMA`, `calcATR`, `openDB`**

Open browser, run:
```javascript
console.log(calcEMA([1,2,3,4,5,6,7,8,9,10], 3)); // should return 8 values
console.log(calcATR([5,6,7,8],[3,4,5,6],[4,5,6,7], 2)); // should return 2 values
openDB().then(function(db) { console.log('DB opened:', db.name); });
```

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/html/counter-trade13.html
git commit -m "feat: core utilities - API, indicators, IndexedDB, Web Worker"
```

---

### Task 3: Coin Screener

**Files:**
- Modify: `src/main/resources/static/html/counter-trade13.html`

- [ ] **Step 1: Implement screener function**

```javascript
async function runScreener() {
  addLog('info', '코인 스크리닝 시작...');

  // 1. 전체 마켓 조회
  var mk = await apiFetch('/market/all?isDetails=true');
  var markets = mk.filter(function(m) { return m.market.startsWith('KRW-'); });
  var nameMap = {};
  markets.forEach(function(m) { nameMap[m.market] = m.korean_name; });

  // 2. 티커 조회 (배치)
  var codes = markets.map(function(m) { return m.market; });
  var allTickers = [];
  for (var i = 0; i < codes.length; i += 100) {
    var batch = codes.slice(i, i + 100);
    var tickers = await apiFetch('/ticker?markets=' + batch.join(','));
    allTickers = allTickers.concat(tickers);
    if (i + 100 < codes.length) await bgSleep(200);
  }

  // 3. 기본 필터: 스테이블코인 제외, 최소가격, 거래량 상위 30%
  var filtered = allTickers.filter(function(t) {
    var sym = t.market.replace('KRW-', '');
    if (EXCLUDE_COINS.indexOf(sym) >= 0) return false;
    if ((t.trade_price || 0) < 10) return false;
    return true;
  });
  filtered.sort(function(a, b) { return (b.acc_trade_price_24h || 0) - (a.acc_trade_price_24h || 0); });
  var top30pct = Math.ceil(filtered.length * 0.3);
  var volFiltered = filtered.slice(0, top30pct);

  // 4. ATR 기반 변동성 계산 (상위 코인에 대해 1분봉 50개 조회)
  var scored = [];
  for (var i = 0; i < volFiltered.length; i++) {
    var t = volFiltered[i];
    try {
      var candles = await apiFetch('/candles/minutes/1?market=' + t.market + '&count=60');
      if (!candles || candles.length < 30) continue;
      candles.reverse(); // oldest first
      var highs = candles.map(function(c) { return c.high_price; });
      var lows = candles.map(function(c) { return c.low_price; });
      var closes = candles.map(function(c) { return c.trade_price; });
      var atr = calcATR(highs, lows, closes, 14);
      if (atr.length === 0) continue;
      var lastATR = atr[atr.length - 1];
      var atrPct = lastATR / closes[closes.length - 1]; // ATR as % of price
      var vol24h = t.acc_trade_price_24h || 0;
      scored.push({
        market: t.market,
        symbol: t.market.replace('KRW-', ''),
        name: nameMap[t.market] || t.market,
        price: t.trade_price,
        vol24h: vol24h,
        atrPct: atrPct,
        score: vol24h * atrPct // 거래량 × 변동성
      });
      await bgSleep(150);
    } catch (e) { /* skip */ }
  }

  // 5. 변동성 상위 50% 필터 + 점수순 정렬
  scored.sort(function(a, b) { return b.atrPct - a.atrPct; });
  var top50pct = Math.ceil(scored.length * 0.5);
  var volAtile = scored.slice(0, top50pct);
  volAtile.sort(function(a, b) { return b.score - a.score; });

  // 6. 상위 10개 선별
  screenedCoins = volAtile.slice(0, 10);

  addLog('info', '스크리닝 완료: ' + screenedCoins.length + '개 코인 선별');
  renderScreenerPanel();
  return screenedCoins;
}
```

- [ ] **Step 2: Implement screener UI panel rendering**

```javascript
function renderScreenerPanel() {
  var el = document.getElementById('screenerBody');
  if (!el) return;
  el.innerHTML = '';
  screenedCoins.forEach(function(c, i) {
    var tr = document.createElement('tr');
    tr.innerHTML = '<td>' + (i + 1) + '</td>'
      + '<td><strong>' + c.symbol + '</strong><br><span style="color:var(--muted);font-size:10px">' + c.name + '</span></td>'
      + '<td class="mono">' + c.price.toLocaleString() + '</td>'
      + '<td class="mono">' + (c.vol24h / 1e8).toFixed(0) + '억</td>'
      + '<td class="mono">' + (c.atrPct * 100).toFixed(2) + '%</td>'
      + '<td class="mono">' + (c.score / 1e8).toFixed(1) + '</td>';
    el.appendChild(tr);
  });
}
```

- [ ] **Step 3: Test screener in browser**

Click "스크리닝" button, verify:
- 5~10 coins appear in table
- Sorted by score (vol × ATR)
- No stablecoins in results
- Takes ~30-60 seconds (API calls)

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/html/counter-trade13.html
git commit -m "feat: coin screener - auto-select top coins by volume×volatility"
```

---

## Chunk 2: Strategy Engine (5 Strategies + Common Exit Logic)

### Task 4: Common Position Management

**Files:**
- Modify: `src/main/resources/static/html/counter-trade13.html`

- [ ] **Step 1: Define position structure and common exit checker**

```javascript
// Position structure:
// { stratIdx, market, symbol, entryPrice, entryTime, qty, peakPct, mode:'paper'|'live' }

function checkExit(pos, candle, currentTime) {
  // candle: { open, high, low, close, volume, time }
  // Returns: null (hold) or { exitPrice, exitReason }
  var pct = (candle.close - pos.entryPrice) / pos.entryPrice;
  var highPct = (candle.high - pos.entryPrice) / pos.entryPrice;
  var lowPct = (candle.low - pos.entryPrice) / pos.entryPrice;

  // Update peak
  if (highPct > (pos.peakPct || 0)) pos.peakPct = highPct;

  // 1. Stop loss
  if (lowPct <= STOP_LOSS) {
    var exitP = pos.entryPrice * (1 + STOP_LOSS);
    return { exitPrice: exitP, exitReason: 'STOP_LOSS' };
  }

  // 2. Time limit
  var holdMin = (currentTime - pos.entryTime) / 60000;
  if (holdMin >= MAX_HOLD_MIN) {
    return { exitPrice: candle.close, exitReason: 'TIMEOUT' };
  }

  // 3. Trailing stop
  if (pos.peakPct >= TRAIL_ACTIVATE) {
    var trailLevel = pos.peakPct - TRAIL_PCT;
    if (pct <= trailLevel) {
      var exitP = pos.entryPrice * (1 + trailLevel);
      return { exitPrice: exitP, exitReason: 'TRAIL_STOP' };
    }
  }

  // 4. Strategy-specific exit (profit target) — handled by each strategy
  return null;
}
```

- [ ] **Step 2: Define strategy interface**

```javascript
// Each strategy implements:
// checkBuySignal(candles, idx) => { signal: true/false, reason: string }
// checkSellSignal(pos, candles, idx) => { exit: true/false, price, reason } (optional strategy-specific exit)
// getIndicators(candles) => { ... } (pre-calculated indicators for the candle array)

function initStrategies() {
  strategies = [
    { name: 'RSI반전',     fn: strategyRSIReversal,   status: 'paper', score: 0, wins: 0, losses: 0, totalPnl: 0, consecLoss: 0, trades: 0, promoted: false, lastPromoted: 0 },
    { name: 'BB스퀴즈',    fn: strategyBBSqueeze,     status: 'paper', score: 0, wins: 0, losses: 0, totalPnl: 0, consecLoss: 0, trades: 0, promoted: false, lastPromoted: 0 },
    { name: '거래량급등',   fn: strategyVolumeSurge,   status: 'paper', score: 0, wins: 0, losses: 0, totalPnl: 0, consecLoss: 0, trades: 0, promoted: false, lastPromoted: 0 },
    { name: 'EMA크로스',   fn: strategyEMACross,      status: 'paper', score: 0, wins: 0, losses: 0, totalPnl: 0, consecLoss: 0, trades: 0, promoted: false, lastPromoted: 0 },
    { name: '핀바',        fn: strategyPinBar,        status: 'paper', score: 0, wins: 0, losses: 0, totalPnl: 0, consecLoss: 0, trades: 0, promoted: false, lastPromoted: 0 },
  ];
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/html/counter-trade13.html
git commit -m "feat: common position management + strategy interface"
```

---

### Task 5: Strategy 1 — RSI Reversal Scalper

- [ ] **Step 1: Implement RSI reversal strategy**

```javascript
function strategyRSIReversal() {}
strategyRSIReversal.checkBuySignal = function(closes, volumes, idx, indicators) {
  // RSI(7) <= 25 이고 직전 봉 대비 RSI 상승 전환
  var rsi = indicators.rsi7;
  var rsiIdx = idx - (closes.length - rsi.length); // align index
  if (rsiIdx < 1 || rsiIdx >= rsi.length) return null;
  var curRSI = rsi[rsiIdx];
  var prevRSI = rsi[rsiIdx - 1];
  if (curRSI <= 25 && curRSI > prevRSI) {
    return { signal: true, reason: 'RSI(' + curRSI.toFixed(1) + ') ≤25 반전' };
  }
  return null;
};
strategyRSIReversal.checkTakeProfit = function(pos, closes, idx, indicators) {
  // +0.4% 또는 RSI 50 도달
  var pct = (closes[idx] - pos.entryPrice) / pos.entryPrice;
  if (pct >= 0.004) return { exit: true, price: pos.entryPrice * 1.004, reason: 'TP_0.4%' };
  var rsi = indicators.rsi7;
  var rsiIdx = idx - (closes.length - rsi.length);
  if (rsiIdx >= 0 && rsi[rsiIdx] >= 50 && pct > 0) {
    return { exit: true, price: closes[idx], reason: 'RSI≥50' };
  }
  return null;
};
strategyRSIReversal.getIndicators = function(closes) {
  return { rsi7: calcRSI(closes, 7) };
};
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: strategy 1 - RSI reversal scalper"
```

---

### Task 6: Strategy 2 — BB Squeeze Breakout

- [ ] **Step 1: Implement BB squeeze breakout strategy**

```javascript
function strategyBBSqueeze() {}
strategyBBSqueeze.checkBuySignal = function(closes, volumes, idx, indicators) {
  var bb = indicators.bb20;
  var bbIdx = idx - (closes.length - bb.mid.length);
  if (bbIdx < 50 || bbIdx >= bb.mid.length) return null;

  // 밴드폭 계산
  var bw = (bb.upper[bbIdx] - bb.lower[bbIdx]) / bb.mid[bbIdx];
  // 최근 50봉 밴드폭 중 하위 20% 체크
  var bws = [];
  for (var j = bbIdx - 49; j <= bbIdx; j++) {
    if (j >= 0) bws.push((bb.upper[j] - bb.lower[j]) / bb.mid[j]);
  }
  bws.sort(function(a, b) { return a - b; });
  var threshold = bws[Math.floor(bws.length * 0.2)];
  var isSqueeze = bw <= threshold;

  // 스퀴즈 + 상단밴드 돌파
  if (isSqueeze && closes[idx] > bb.upper[bbIdx]) {
    return { signal: true, reason: 'BB스퀴즈 상단돌파(bw=' + (bw * 100).toFixed(2) + '%)' };
  }
  return null;
};
strategyBBSqueeze.checkTakeProfit = function(pos, closes, idx, indicators) {
  var pct = (closes[idx] - pos.entryPrice) / pos.entryPrice;
  if (pct >= 0.005) return { exit: true, price: pos.entryPrice * 1.005, reason: 'TP_0.5%' };
  // 밴드 중심선 복귀 (수익 구간에서만)
  var bb = indicators.bb20;
  var bbIdx = idx - (closes.length - bb.mid.length);
  if (bbIdx >= 0 && pct > 0.001 && closes[idx] <= bb.mid[bbIdx]) {
    return { exit: true, price: closes[idx], reason: 'BB중심선' };
  }
  return null;
};
strategyBBSqueeze.getIndicators = function(closes) {
  return { bb20: calcBB(closes, 20, 2) };
};
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: strategy 2 - BB squeeze breakout"
```

---

### Task 7: Strategy 3 — Volume Surge Momentum

- [ ] **Step 1: Implement volume surge strategy**

```javascript
function strategyVolumeSurge() {}
strategyVolumeSurge.checkBuySignal = function(closes, volumes, idx, indicators) {
  if (idx < 20) return null;

  // 거래량 20봉 평균
  var volSum = 0;
  for (var j = idx - 20; j < idx; j++) volSum += volumes[j];
  var avgVol = volSum / 20;

  // 현재 거래량 >= 3배
  if (volumes[idx] < avgVol * 3) return null;

  // 가격 15봉 최고가 돌파
  var maxHigh = 0;
  for (var j = idx - 15; j < idx; j++) {
    if (closes[j] > maxHigh) maxHigh = closes[j];
  }
  if (closes[idx] <= maxHigh) return null;

  // RSI < 70 필터
  var rsi = indicators.rsi14;
  var rsiIdx = idx - (closes.length - rsi.length);
  if (rsiIdx >= 0 && rsi[rsiIdx] >= 70) return null;

  var volMult = (volumes[idx] / avgVol).toFixed(1);
  return { signal: true, reason: '거래량 ' + volMult + 'x + 15봉고가돌파' };
};
strategyVolumeSurge.checkTakeProfit = function(pos, closes, volumes, idx, indicators) {
  var pct = (closes[idx] - pos.entryPrice) / pos.entryPrice;
  if (pct >= 0.006) return { exit: true, price: pos.entryPrice * 1.006, reason: 'TP_0.6%' };
  // 거래량 평균 이하 하락 시 (수익 구간)
  if (idx >= 20 && pct > 0) {
    var volSum = 0;
    for (var j = idx - 20; j < idx; j++) volSum += volumes[j];
    var avgVol = volSum / 20;
    if (volumes[idx] < avgVol) {
      return { exit: true, price: closes[idx], reason: '거래량감소' };
    }
  }
  return null;
};
strategyVolumeSurge.getIndicators = function(closes) {
  return { rsi14: calcRSI(closes, 14) };
};
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: strategy 3 - volume surge momentum"
```

---

### Task 8: Strategy 4 — EMA Cross Scalper

- [ ] **Step 1: Implement EMA cross strategy**

```javascript
function strategyEMACross() {}
strategyEMACross.checkBuySignal = function(closes, volumes, idx, indicators) {
  var ema5 = indicators.ema5;
  var ema13 = indicators.ema13;
  var ema55 = indicators.ema55;
  var rsi = indicators.rsi14;

  // Align indices
  var e5i = idx - (closes.length - ema5.length);
  var e13i = idx - (closes.length - ema13.length);
  var e55i = idx - (closes.length - ema55.length);
  var ri = idx - (closes.length - rsi.length);

  if (e5i < 1 || e13i < 1 || e55i < 0 || ri < 0) return null;

  // 골든크로스: EMA5 > EMA13 (현재) AND EMA5 <= EMA13 (직전)
  var cross = ema5[e5i] > ema13[e13i] && ema5[e5i - 1] <= ema13[e13i - 1];
  if (!cross) return null;

  // RSI > 40
  if (rsi[ri] <= 40) return null;

  // 현재가 > EMA55
  if (closes[idx] <= ema55[e55i]) return null;

  return { signal: true, reason: 'EMA(5,13) 골든크로스 RSI=' + rsi[ri].toFixed(0) };
};
strategyEMACross.checkTakeProfit = function(pos, closes, idx, indicators) {
  var pct = (closes[idx] - pos.entryPrice) / pos.entryPrice;
  if (pct >= 0.004) return { exit: true, price: pos.entryPrice * 1.004, reason: 'TP_0.4%' };
  // 데드크로스
  var ema5 = indicators.ema5;
  var ema13 = indicators.ema13;
  var e5i = idx - (closes.length - ema5.length);
  var e13i = idx - (closes.length - ema13.length);
  if (e5i >= 0 && e13i >= 0 && pct > 0 && ema5[e5i] < ema13[e13i]) {
    return { exit: true, price: closes[idx], reason: 'EMA데드크로스' };
  }
  return null;
};
strategyEMACross.getIndicators = function(closes) {
  return {
    ema5: calcEMA(closes, 5),
    ema13: calcEMA(closes, 13),
    ema55: calcEMA(closes, 55),
    rsi14: calcRSI(closes, 14)
  };
};
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: strategy 4 - EMA cross scalper"
```

---

### Task 9: Strategy 5 — Pin Bar

- [ ] **Step 1: Implement pin bar strategy**

```javascript
function strategyPinBar() {}
strategyPinBar.checkBuySignal = function(closes, volumes, idx, indicators) {
  if (idx < 4) return null;

  var open = indicators.opens[idx];
  var high = indicators.highs[idx];
  var low = indicators.lows[idx];
  var close = closes[idx];
  var bodyLen = Math.abs(close - open);
  var totalLen = high - low;
  if (totalLen <= 0) return null;

  var lowerWick = Math.min(open, close) - low;

  // 핀바 조건: 아래꼬리 >= 66%, 몸통 <= 20%
  if (lowerWick / totalLen < 0.66) return null;
  if (bodyLen / totalLen > 0.20) return null;

  // 직전 3봉 하락 추세
  var downTrend = closes[idx - 1] < closes[idx - 2] && closes[idx - 2] < closes[idx - 3];
  if (!downTrend) return null;

  // 거래량 >= 평균 1.5배
  if (idx < 20) return null;
  var volSum = 0;
  for (var j = idx - 20; j < idx; j++) volSum += volumes[j];
  var avgVol = volSum / 20;
  if (volumes[idx] < avgVol * 1.5) return null;

  return { signal: true, reason: '핀바(꼬리' + (lowerWick / totalLen * 100).toFixed(0) + '%)' };
};
strategyPinBar.checkTakeProfit = function(pos, closes, idx, indicators) {
  var pct = (closes[idx] - pos.entryPrice) / pos.entryPrice;
  if (pct >= 0.004) return { exit: true, price: pos.entryPrice * 1.004, reason: 'TP_0.4%' };
  // 직전 고가 도달
  if (idx >= 2 && pct > 0) {
    var prevHigh = indicators.highs[idx - 1];
    if (closes[idx] >= prevHigh) {
      return { exit: true, price: prevHigh, reason: '직전고가' };
    }
  }
  return null;
};
strategyPinBar.getIndicators = function(closes, opens, highs, lows) {
  return { opens: opens, highs: highs, lows: lows };
};
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: strategy 5 - pin bar reversal"
```

---

## Chunk 3: Backtest Engine + Paper Trading + Tournament

### Task 10: Backtest Engine

**Files:**
- Modify: `src/main/resources/static/html/counter-trade13.html`

- [ ] **Step 1: Implement backtest function for all strategies**

```javascript
function backtestStrategy(stratIdx, candles) {
  // candles: API format (newest first) -> reverse to oldest first
  var c = candles.slice().reverse();
  var closes = c.map(function(x) { return x.trade_price; });
  var opens = c.map(function(x) { return x.opening_price; });
  var highs = c.map(function(x) { return x.high_price; });
  var lows = c.map(function(x) { return x.low_price; });
  var volumes = c.map(function(x) { return x.candle_acc_trade_volume; });
  var times = c.map(function(x) { return new Date(x.candle_date_time_kst || x.candle_date_time_utc).getTime(); });

  var strat = strategies[stratIdx];
  var indicators = strat.fn.getIndicators(closes, opens, highs, lows);

  var trades = [];
  var pos = null;
  var cooldowns = {}; // market -> cooldown end time

  var startIdx = 60; // enough data for all indicators

  for (var i = startIdx; i < closes.length; i++) {
    var candle = { open: opens[i], high: highs[i], low: lows[i], close: closes[i], volume: volumes[i], time: times[i] };

    // Check exit for pending buy (enter at this bar's open)
    if (pos && pos.pending) {
      pos.entryPrice = opens[i] * (1 + BASE_SLIPPAGE); // 슬리피지 적용
      pos.entryTime = times[i];
      pos.pending = false;
    }

    // Check exit
    if (pos && !pos.pending) {
      var exitResult = checkExit(pos, candle, times[i]);
      var tpResult = strat.fn.checkTakeProfit(pos, closes, i, indicators);

      if (exitResult) {
        var pnl = (exitResult.exitPrice - pos.entryPrice) / pos.entryPrice - FEE_ROUND;
        trades.push({ entryPrice: pos.entryPrice, exitPrice: exitResult.exitPrice, entryTime: pos.entryTime, exitTime: times[i], pnl: pnl, reason: exitResult.exitReason });
        if (pnl < 0) cooldowns[pos.market] = times[i] + COOLDOWN_MS;
        pos = null;
      } else if (tpResult && tpResult.exit) {
        var exitP = tpResult.price * (1 - BASE_SLIPPAGE);
        var pnl = (exitP - pos.entryPrice) / pos.entryPrice - FEE_ROUND;
        trades.push({ entryPrice: pos.entryPrice, exitPrice: exitP, entryTime: pos.entryTime, exitTime: times[i], pnl: pnl, reason: tpResult.reason });
        pos = null;
      }
    }

    // Check buy signal on completed candle (i-1) -> enter at next bar (i+1) open
    if (!pos && i < closes.length - 1) {
      // cooldown check
      if (cooldowns['bt'] && times[i] < cooldowns['bt']) continue;

      var sig = strat.fn.checkBuySignal(closes, volumes, i - 1, indicators);
      if (sig && sig.signal) {
        pos = { stratIdx: stratIdx, market: 'bt', entryPrice: 0, entryTime: 0, peakPct: 0, pending: true };
      }
    }
  }

  // Calculate stats
  var wins = trades.filter(function(t) { return t.pnl > 0; }).length;
  var losses = trades.length - wins;
  var totalPnl = trades.reduce(function(s, t) { return s + t.pnl; }, 0);
  var avgWin = wins > 0 ? trades.filter(function(t) { return t.pnl > 0; }).reduce(function(s, t) { return s + t.pnl; }, 0) / wins : 0;
  var avgLoss = losses > 0 ? trades.filter(function(t) { return t.pnl <= 0; }).reduce(function(s, t) { return s + t.pnl; }, 0) / losses : 0;

  return {
    stratIdx: stratIdx,
    trades: trades,
    count: trades.length,
    wins: wins,
    losses: losses,
    winRate: trades.length > 0 ? wins / trades.length : 0,
    totalPnl: totalPnl,
    avgWin: avgWin,
    avgLoss: avgLoss,
    profitFactor: avgLoss !== 0 ? Math.abs(avgWin / avgLoss) : 0
  };
}

async function runInitialBacktest() {
  addLog('info', '초기 백테스트 시작...');
  backtestBaseline = {};

  for (var si = 0; si < strategies.length; si++) {
    var results = [];
    for (var ci = 0; ci < screenedCoins.length; ci++) {
      var coin = screenedCoins[ci];
      try {
        var candles = await fetchHistoricalCandles(coin.market, 1, 1); // 1분봉 1일
        if (!candles || candles.length < 100) continue;
        var r = backtestStrategy(si, candles);
        if (r.count > 0) results.push(r);
        await bgSleep(150);
      } catch (e) { /* skip */ }
    }

    var totalTrades = results.reduce(function(s, r) { return s + r.count; }, 0);
    var totalPnl = results.reduce(function(s, r) { return s + r.totalPnl; }, 0);
    var totalWins = results.reduce(function(s, r) { return s + r.wins; }, 0);

    backtestBaseline[si] = {
      trades: totalTrades,
      pnl: totalPnl,
      winRate: totalTrades > 0 ? totalWins / totalTrades : 0
    };

    addLog('info', strategies[si].name + ' 백테스트: ' + totalTrades + '건, ' + formatPct(totalPnl));
  }
  addLog('info', '초기 백테스트 완료');
}
```

- [ ] **Step 2: Test backtest in browser with screened coins**

Run screener first, then `runInitialBacktest()`. Verify each strategy produces trade count and PnL.

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: backtest engine - initial strategy validation"
```

---

### Task 11: Paper Trading Engine

- [ ] **Step 1: Implement paper trading cycle**

```javascript
async function paperTradingCycle() {
  // 각 선별 코인에 대해 최근 캔들 조회 → 5개 전략 신호 체크
  for (var ci = 0; ci < screenedCoins.length; ci++) {
    var coin = screenedCoins[ci];
    try {
      var candles = await apiFetch('/candles/minutes/1?market=' + coin.market + '&count=200');
      if (!candles || candles.length < 60) continue;
      candles.reverse(); // oldest first

      var closes = candles.map(function(c) { return c.trade_price; });
      var opens = candles.map(function(c) { return c.opening_price; });
      var highs = candles.map(function(c) { return c.high_price; });
      var lows = candles.map(function(c) { return c.low_price; });
      var volumes = candles.map(function(c) { return c.candle_acc_trade_volume; });
      var times = candles.map(function(c) { return new Date(c.candle_date_time_kst || c.candle_date_time_utc).getTime(); });
      var lastIdx = closes.length - 2; // 완성봉 (마지막은 미완성)
      var now = Date.now();

      for (var si = 0; si < strategies.length; si++) {
        var posKey = si + '_' + coin.market;

        // Check exit for existing paper positions
        if (paperPositions[posKey]) {
          var pos = paperPositions[posKey];
          var candle = { open: opens[closes.length - 1], high: highs[closes.length - 1], low: lows[closes.length - 1], close: closes[closes.length - 1], volume: volumes[closes.length - 1], time: now };
          var indicators = strategies[si].fn.getIndicators(closes, opens, highs, lows);
          var exitResult = checkExit(pos, candle, now);
          var tpResult = strategies[si].fn.checkTakeProfit(pos, closes, closes.length - 1, indicators);

          if (exitResult || (tpResult && tpResult.exit)) {
            var exitPrice, reason;
            if (exitResult) { exitPrice = exitResult.exitPrice; reason = exitResult.exitReason; }
            else { exitPrice = tpResult.price * (1 - BASE_SLIPPAGE); reason = tpResult.reason; }

            var pnl = (exitPrice - pos.entryPrice) / pos.entryPrice - FEE_ROUND;
            var trade = { stratIdx: si, market: coin.market, symbol: coin.symbol, entryPrice: pos.entryPrice, exitPrice: exitPrice, entryTime: pos.entryTime, exitTime: now, pnl: pnl, reason: reason };

            await saveTrade('paper', trade);
            updateStrategyStats(si, pnl);
            addLog(pnl >= 0 ? 'success' : 'error', '[페이퍼][' + strategies[si].name + '] ' + coin.symbol + ' ' + reason + ' ' + formatPct(pnl));
            delete paperPositions[posKey];
          }
          continue;
        }

        // Check buy signal (completed candle)
        if (getPositionCount('paper', si) >= 1) continue; // 전략별 최대 1개
        if (getTotalPositionCount('paper') >= 5) continue; // 페이퍼는 넉넉하게 5개

        var indicators = strategies[si].fn.getIndicators(closes, opens, highs, lows);
        var sig = strategies[si].fn.checkBuySignal(closes, volumes, lastIdx, indicators);
        if (sig && sig.signal) {
          var entryPrice = closes[closes.length - 1] * (1 + BASE_SLIPPAGE); // 다음 봉 시가 근사
          paperPositions[posKey] = {
            stratIdx: si, market: coin.market, symbol: coin.symbol,
            entryPrice: entryPrice, entryTime: now, peakPct: 0, mode: 'paper'
          };
          addLog('info', '[페이퍼][' + strategies[si].name + '] ' + coin.symbol + ' 매수 ' + sig.reason);
        }
      }
      await bgSleep(150);
    } catch (e) { console.error('Paper cycle err:', coin.symbol, e); }
  }
}

function getPositionCount(mode, stratIdx) {
  var positions = mode === 'paper' ? paperPositions : livePositions;
  var count = 0;
  for (var key in positions) {
    if (positions[key].stratIdx === stratIdx) count++;
  }
  return count;
}

function getTotalPositionCount(mode) {
  return Object.keys(mode === 'paper' ? paperPositions : livePositions).length;
}

function updateStrategyStats(stratIdx, pnl) {
  var s = strategies[stratIdx];
  s.trades++;
  s.totalPnl += pnl;
  if (pnl > 0) { s.wins++; s.consecLoss = 0; }
  else { s.losses++; s.consecLoss++; }
  // Score = 40% PnL + 30% WinRate + 20% ProfitFactor + 10% Trades
  var wr = s.trades > 0 ? s.wins / s.trades : 0;
  var avgW = s.wins > 0 ? s.totalPnl / s.wins : 0; // simplified
  s.score = s.totalPnl * 0.4 + wr * 0.3 + (s.trades >= PROMOTE_MIN_TRADES ? 0.1 : 0);
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: paper trading engine - virtual execution with all 5 strategies"
```

---

### Task 12: Tournament Manager

- [ ] **Step 1: Implement tournament evaluation and promotion/demotion**

```javascript
function evaluateTournament() {
  addLog('info', '토너먼트 평가 시작...');

  // 1. Score all strategies
  var ranked = strategies.map(function(s, i) { return { idx: i, score: s.score, trades: s.trades, totalPnl: s.totalPnl, consecLoss: s.consecLoss }; });
  ranked.sort(function(a, b) { return b.score - a.score; });

  // 2. Demote: 연속 2회 손절 시 강등
  for (var i = 0; i < strategies.length; i++) {
    if (strategies[i].status === 'live' && strategies[i].consecLoss >= DEMOTE_CONSEC_LOSS) {
      strategies[i].status = 'paper';
      strategies[i].promoted = false;
      addLog('warn', '[강등] ' + strategies[i].name + ' → 페이퍼 (연속 ' + strategies[i].consecLoss + '회 손절)');
    }
  }

  // 3. Promote: 상위 2개 (조건 충족 시)
  var minTrades = PROMOTE_MIN_TRADES;
  var starvation = (Date.now() - lastPromotionTime) > STARVATION_MS && lastPromotionTime > 0;
  if (starvation) {
    minTrades = 5; // 기아 방지: 기준 완화
    addLog('warn', '4시간 동안 승격 없음 — 기준 완화 (5건)');
  }

  var promoted = 0;
  for (var r = 0; r < ranked.length && promoted < 2; r++) {
    var s = strategies[ranked[r].idx];
    if (ranked[r].trades >= minTrades && ranked[r].totalPnl > 0) {
      if (s.status !== 'live') {
        s.status = 'live';
        s.promoted = true;
        s.lastPromoted = Date.now();
        lastPromotionTime = Date.now();
        addLog('success', '[승격] ' + s.name + ' → 실매매 (점수: ' + ranked[r].score.toFixed(4) + ')');
      }
      promoted++;
    }
  }

  // 4. 나머지 live -> paper (2개 초과 방지)
  var liveCount = 0;
  for (var r = 0; r < ranked.length; r++) {
    var s = strategies[ranked[r].idx];
    if (s.status === 'live') {
      liveCount++;
      if (liveCount > 2) {
        s.status = 'paper';
        s.promoted = false;
        addLog('info', '[강등] ' + s.name + ' → 페이퍼 (3위 이하)');
      }
    }
  }

  renderTournamentBoard();
  addLog('info', '토너먼트 평가 완료');
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: tournament manager - evaluation, promotion, demotion"
```

---

## Chunk 4: Live Trading + Safety + Dashboard + Main Loop

### Task 13: Live Trading Engine

- [ ] **Step 1: Implement live trading cycle (buy/sell via API)**

Reuse ct9's `apiAuth`, `executeBuy`, `executeSell` patterns but adapted for multi-strategy:

```javascript
async function liveTradingCycle() {
  if (!autoTradeOn) return;

  // Safety checks
  if (!await checkTradeKey()) { addLog('error', 'key.json autoTrade=false'); return; }
  if (checkDailyMDD()) { addLog('error', '일일 MDD 초과 — 매매 중단'); autoTradeOn = false; return; }
  if (checkCapitalFloor()) { addLog('error', '절대 자본 하한 도달 — 전체 중단'); autoTradeOn = false; return; }

  await loadBalance();

  for (var ci = 0; ci < screenedCoins.length; ci++) {
    var coin = screenedCoins[ci];
    try {
      var candles = await apiFetch('/candles/minutes/1?market=' + coin.market + '&count=200');
      if (!candles || candles.length < 60) continue;
      candles.reverse();

      var closes = candles.map(function(c) { return c.trade_price; });
      var opens = candles.map(function(c) { return c.opening_price; });
      var highs = candles.map(function(c) { return c.high_price; });
      var lows = candles.map(function(c) { return c.low_price; });
      var volumes = candles.map(function(c) { return c.candle_acc_trade_volume; });
      var lastIdx = closes.length - 2;
      var now = Date.now();

      // Only check strategies with status='live'
      for (var si = 0; si < strategies.length; si++) {
        if (strategies[si].status !== 'live') continue;
        var posKey = si + '_' + coin.market;

        // Exit check
        if (livePositions[posKey]) {
          var pos = livePositions[posKey];
          var candle = { open: opens[closes.length - 1], high: highs[closes.length - 1], low: lows[closes.length - 1], close: closes[closes.length - 1], volume: volumes[closes.length - 1], time: now };
          var indicators = strategies[si].fn.getIndicators(closes, opens, highs, lows);
          var exitResult = checkExit(pos, candle, now);
          var tpResult = strategies[si].fn.checkTakeProfit(pos, closes, closes.length - 1, indicators);

          if (exitResult || (tpResult && tpResult.exit)) {
            var reason = exitResult ? exitResult.exitReason : tpResult.reason;
            await executeSell(coin.market, pos.qty);
            var exitPrice = closes[closes.length - 1]; // approximate
            var pnl = (exitPrice - pos.entryPrice) / pos.entryPrice - FEE_ROUND;
            var trade = { stratIdx: si, market: coin.market, symbol: coin.symbol, entryPrice: pos.entryPrice, exitPrice: exitPrice, entryTime: pos.entryTime, exitTime: now, pnl: pnl, reason: reason };
            await saveTrade('live', trade);
            updateStrategyStats(si, pnl);
            addLog(pnl >= 0 ? 'success' : 'error', '[실매매][' + strategies[si].name + '] ' + coin.symbol + ' ' + reason + ' ' + formatPct(pnl));
            delete livePositions[posKey];
          }
          continue;
        }

        // Buy signal check
        if (getTotalPositionCount('live') >= MAX_POSITIONS) continue;
        if (getPositionCount('live', si) >= 1) continue;

        var indicators = strategies[si].fn.getIndicators(closes, opens, highs, lows);
        var sig = strategies[si].fn.checkBuySignal(closes, volumes, lastIdx, indicators);
        if (sig && sig.signal) {
          // Balance check
          var buyAmt = Math.floor(krwBalance * POSITION_PCT);
          if (buyAmt < 5000) { addLog('warn', '잔고 부족: ' + formatKRW(krwBalance)); continue; }

          var result = await executeBuy(coin.market, buyAmt);
          if (result && result.qty) {
            livePositions[posKey] = {
              stratIdx: si, market: coin.market, symbol: coin.symbol,
              entryPrice: result.avgPrice, entryTime: now, qty: result.qty, peakPct: 0, mode: 'live'
            };
            addLog('success', '[실매매][' + strategies[si].name + '] ' + coin.symbol + ' 매수 ' + sig.reason + ' ' + formatKRW(buyAmt));
          }
        }
      }
      await bgSleep(150);
    } catch (e) { console.error('Live cycle err:', coin.symbol, e); }
  }
}
```

- [ ] **Step 2: Implement safety checks**

```javascript
function checkDailyMDD() {
  if (dailyStartCapital <= 0) return false;
  var currentPnl = (krwBalance - dailyStartCapital) / dailyStartCapital;
  return currentPnl <= DAILY_MDD;
}

function checkCapitalFloor() {
  if (initialCapital <= 0) return false;
  var totalPnl = (krwBalance - initialCapital) / initialCapital;
  return totalPnl <= CAPITAL_FLOOR;
}

var totalConsecLoss = 0;
function checkConsecutiveLoss() {
  return totalConsecLoss >= 5;
}

async function checkOrphanedPositions() {
  try {
    var accounts = await apiAuth('GET', '/accounts');
    var unexpected = accounts.filter(function(a) {
      return a.currency !== 'KRW' && parseFloat(a.balance) > 0;
    });
    var knownMarkets = {};
    for (var key in livePositions) knownMarkets[livePositions[key].market] = true;

    unexpected.forEach(function(a) {
      var market = 'KRW-' + a.currency;
      if (!knownMarkets[market]) {
        addLog('warn', '⚠ 고아 포지션 감지: ' + a.currency + ' (' + a.balance + ')');
      }
    });
  } catch (e) { /* ignore */ }
}
```

- [ ] **Step 3: Implement executeBuy/executeSell (from ct9 pattern)**

```javascript
async function executeBuy(market, krwAmount) {
  try {
    var ok = await checkTradeKey();
    if (!ok) return null;
    var body = { market: market, side: 'bid', ord_type: 'price', price: String(krwAmount) };
    var result = await apiAuth('POST', '/orders', body);
    await bgSleep(2000);
    // confirm
    var trades = await apiAuth('GET', '/orders/' + result.uuid);
    var qty = parseFloat(trades.executed_volume || 0);
    var avgPrice = qty > 0 ? krwAmount / qty : 0;
    return { qty: qty, avgPrice: avgPrice };
  } catch (e) {
    addLog('error', '매수 실패: ' + e.message);
    return null;
  }
}

async function executeSell(market, volume) {
  try {
    var ok = await checkTradeKey();
    if (!ok) return null;
    var body = { market: market, side: 'ask', ord_type: 'market', volume: String(volume) };
    var result = await apiAuth('POST', '/orders', body);
    await bgSleep(2000);
    return result;
  } catch (e) {
    addLog('error', '매도 실패: ' + e.message);
    return null;
  }
}
```

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: live trading engine + safety checks + order execution"
```

---

### Task 14: Main Loop & System Control

- [ ] **Step 1: Implement main loop orchestrator**

```javascript
var mainLoopTimer = null;
var screenerTimer = null;
var evalTimer = null;

async function startSystem() {
  if (systemRunning) return;
  systemRunning = true;
  addLog('info', '=== 시스템 시작 ===');

  // 1. Load keys
  await loadKeys();

  // 2. Init DB
  await openDB();
  await purgeOldTrades('paper', Date.now() - 7 * 86400000);
  await purgeOldTrades('live', Date.now() - 7 * 86400000);

  // 3. Init strategies
  initStrategies();

  // 4. Load balance
  if (accessKey) {
    await loadBalance();
    initialCapital = krwBalance;
    dailyStartCapital = krwBalance;
    await checkOrphanedPositions();
  }

  // 5. Run screener
  await runScreener();
  if (screenedCoins.length === 0) {
    addLog('error', '선별된 코인이 없습니다');
    systemRunning = false;
    return;
  }

  // 6. Initial backtest
  await runInitialBacktest();

  // 7. Start loops
  addLog('info', '메인 루프 시작 (60초 주기)');
  mainLoop();
  screenerTimer = setInterval(runScreener, SCREENER_INTERVAL);
  evalTimer = setInterval(evaluateTournament, EVAL_INTERVAL);

  updateSystemUI();
}

async function mainLoop() {
  if (!systemRunning) return;
  try {
    // Paper trading (always)
    await paperTradingCycle();

    // Live trading (if enabled and strategies promoted)
    if (autoTradeOn) {
      if (!checkConsecutiveLoss()) {
        await liveTradingCycle();
      } else {
        addLog('warn', '연속 5회 손절 — 실매매 일시 중지');
      }
    }

    // Update UI
    renderTournamentBoard();
    renderPositions();
    renderDivergenceChart();

  } catch (e) {
    addLog('error', '메인 루프 오류: ' + e.message);
  }

  // Schedule next cycle (60 seconds)
  if (systemRunning) {
    mainLoopTimer = setTimeout(function() { mainLoop(); }, 60000);
  }
}

function stopSystem() {
  systemRunning = false;
  clearTimeout(mainLoopTimer);
  clearInterval(screenerTimer);
  clearInterval(evalTimer);
  addLog('info', '=== 시스템 정지 ===');
  updateSystemUI();
}

async function loadKeys() {
  try {
    var r = await fetch('key.json?t=' + Date.now());
    if (r.ok) {
      var keyData = await r.json();
      if (keyData.accessKey) accessKey = keyData.accessKey;
      if (keyData.secretKey) secretKey = keyData.secretKey;
    }
  } catch (e) {}
  if (!accessKey) accessKey = localStorage.getItem('bit_access_key') || '';
  if (!secretKey) secretKey = localStorage.getItem('bit_secret_key') || '';
}

async function loadBalance() {
  try {
    var accounts = await apiAuth('GET', '/accounts');
    var krw = accounts.find(function(a) { return a.currency === 'KRW'; });
    krwBalance = krw ? parseFloat(krw.balance) : 0;
  } catch (e) { /* keep previous */ }
}
```

- [ ] **Step 2: Wire up start/stop buttons and autoTrade toggle**

```javascript
document.getElementById('startBtn').onclick = startSystem;
document.getElementById('stopBtn').onclick = stopSystem;
document.getElementById('autoTradeToggle').onchange = function() {
  var checked = this.checked;
  if (checked && (!accessKey || !secretKey)) {
    this.checked = false;
    showToast('API Key를 먼저 설정하세요', 'error');
    return;
  }
  autoTradeOn = checked;
  addLog('info', autoTradeOn ? '*** 자동매매 ON ***' : '자동매매 OFF');
};
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: main loop orchestrator - system start/stop, 60s cycle"
```

---

### Task 15: UI Rendering (Tournament Board, Positions, Log, Dashboard)

- [ ] **Step 1: Implement tournament board rendering**

```javascript
function renderTournamentBoard() {
  var el = document.getElementById('tournamentBoard');
  if (!el) return;

  var ranked = strategies.map(function(s, i) { return { idx: i, s: s }; });
  ranked.sort(function(a, b) { return b.s.score - a.s.score; });

  el.innerHTML = '';
  ranked.forEach(function(r, rank) {
    var s = r.s;
    var wr = s.trades > 0 ? (s.wins / s.trades * 100).toFixed(0) : '-';
    var statusClass = s.status === 'live' ? 'live' : 'paper';
    var rankBadge = rank === 0 ? 'gold' : rank === 1 ? 'silver' : rank === 2 ? 'bronze' : 'normal';

    var card = document.createElement('div');
    card.className = 'strategy-card ' + statusClass;
    card.innerHTML =
      '<div class="card-header">' +
        '<span class="rank-badge ' + rankBadge + '">' + (rank + 1) + '</span>' +
        '<span class="strat-name">' + s.name + '</span>' +
        '<span class="strat-status ' + statusClass + '">' + (s.status === 'live' ? '실매매' : '페이퍼') + '</span>' +
      '</div>' +
      '<div class="card-stats">' +
        '<div><span class="stat-label">수익률</span><span class="stat-value ' + (s.totalPnl >= 0 ? 'up' : 'down') + '">' + formatPct(s.totalPnl) + '</span></div>' +
        '<div><span class="stat-label">승률</span><span class="stat-value">' + wr + '%</span></div>' +
        '<div><span class="stat-label">매매</span><span class="stat-value">' + s.trades + '건</span></div>' +
        '<div><span class="stat-label">연속손절</span><span class="stat-value">' + s.consecLoss + '</span></div>' +
      '</div>';
    el.appendChild(card);
  });
}
```

- [ ] **Step 2: Implement positions, log, and header rendering**

```javascript
function renderPositions() {
  var el = document.getElementById('positionsBody');
  if (!el) return;
  el.innerHTML = '';

  var allPos = [];
  for (var key in paperPositions) allPos.push(Object.assign({ key: key, mode: 'paper' }, paperPositions[key]));
  for (var key in livePositions) allPos.push(Object.assign({ key: key, mode: 'live' }, livePositions[key]));

  allPos.forEach(function(p) {
    var coin = screenedCoins.find(function(c) { return c.market === p.market; });
    var currentPrice = coin ? coin.price : p.entryPrice;
    var pnl = (currentPrice - p.entryPrice) / p.entryPrice;
    var holdSec = Math.floor((Date.now() - p.entryTime) / 1000);
    var holdStr = Math.floor(holdSec / 60) + ':' + ('0' + (holdSec % 60)).slice(-2);

    var tr = document.createElement('tr');
    tr.innerHTML =
      '<td><span class="mode-badge ' + p.mode + '">' + (p.mode === 'live' ? '실' : '페') + '</span></td>' +
      '<td>' + strategies[p.stratIdx].name + '</td>' +
      '<td>' + p.symbol + '</td>' +
      '<td class="mono">' + p.entryPrice.toLocaleString() + '</td>' +
      '<td class="mono ' + (pnl >= 0 ? 'up' : 'down') + '">' + formatPct(pnl) + '</td>' +
      '<td class="mono">' + holdStr + '</td>';
    el.appendChild(tr);
  });
}

var logEntries = [];
function renderLog() {
  var el = document.getElementById('logBody');
  if (!el) return;
  el.innerHTML = '';
  logEntries.slice(0, 50).forEach(function(entry) {
    var tr = document.createElement('tr');
    tr.innerHTML = '<td class="mono" style="width:70px">' + entry.time + '</td>'
      + '<td><span class="log-type ' + entry.type + '">' + entry.type + '</span></td>'
      + '<td>' + entry.msg + '</td>';
    el.appendChild(tr);
  });
}

function updateSystemUI() {
  document.getElementById('startBtn').disabled = systemRunning;
  document.getElementById('stopBtn').disabled = !systemRunning;
  document.getElementById('systemStatus').textContent = systemRunning ? 'RUNNING' : 'STOPPED';
  document.getElementById('systemStatus').className = systemRunning ? 'status-on' : 'status-off';
  document.getElementById('totalAsset').textContent = formatKRW(krwBalance);
}
```

- [ ] **Step 3: Implement divergence chart (simple text-based comparison)**

```javascript
function renderDivergenceChart() {
  var el = document.getElementById('divergencePanel');
  if (!el) return;
  el.innerHTML = '';

  strategies.forEach(function(s, si) {
    var bt = backtestBaseline[si] || { pnl: 0, winRate: 0, trades: 0 };
    var paperPnl = s.totalPnl;
    var divergence = bt.pnl !== 0 ? Math.abs((paperPnl - bt.pnl) / Math.abs(bt.pnl)) : 0;
    var isWarning = divergence > 0.2;

    var row = document.createElement('div');
    row.className = 'divergence-row' + (isWarning ? ' warning' : '');
    row.innerHTML =
      '<span class="div-name">' + s.name + '</span>' +
      '<span class="div-bt">BT: ' + formatPct(bt.pnl) + '</span>' +
      '<span class="div-paper">페이퍼: ' + formatPct(paperPnl) + '</span>' +
      '<span class="div-gap ' + (isWarning ? 'warn' : 'ok') + '">괴리: ' + (divergence * 100).toFixed(0) + '%</span>';
    el.appendChild(row);

    if (isWarning && s.status === 'live') {
      s.status = 'paper';
      s.promoted = false;
      addLog('warn', '[괴리경고] ' + s.name + ' 비활성화 (괴리율 ' + (divergence * 100).toFixed(0) + '%)');
    }
  });
}
```

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: UI rendering - tournament board, positions, log, divergence"
```

---

### Task 16: Final Assembly & Integration Test

- [ ] **Step 1: Ensure all HTML body sections are properly wired**

Wire up all rendering calls, event listeners, and initialization. Verify the complete file is self-contained.

- [ ] **Step 2: Add apiAuth function (JWT signing from ct9)**

Copy ct9's JWT-based authenticated API function (uses HMAC-SHA512 for Upbit-compatible auth).

- [ ] **Step 3: Add HomeController route**

Modify `HomeController.java` to add a route for counter-trade13:
```java
@GetMapping("/counter-trade13")
public String counterTrade13() { return "redirect:/html/counter-trade13.html"; }
```

- [ ] **Step 4: Browser integration test — paper-only mode**

1. Open `counter-trade13.html`
2. Click "시작" — screener runs, coins appear
3. Backtest completes, baseline shows
4. Paper trading cycle runs every 60s
5. Tournament evaluation runs every 30min
6. Verify log shows paper trades accumulating

- [ ] **Step 5: Browser integration test — live mode (소액)**

1. Ensure `key.json` has valid keys + `autoTrade: true`
2. Toggle "자동매매" ON
3. Wait for strategy promotion (may need to lower PROMOTE_MIN_TRADES temporarily)
4. Verify live orders appear in Bithumb account
5. Verify safety stops (MDD, consecutive loss) work

- [ ] **Step 6: Commit final version**

```bash
git add src/main/resources/static/html/counter-trade13.html src/main/java/kr/co/scrab/controller/HomeController.java
git commit -m "feat: multi-strategy tournament scalper v1 - complete integration"
```

---

## Summary

| Chunk | Tasks | Description |
|-------|-------|-------------|
| 1 | 1-3 | HTML shell, utilities, coin screener |
| 2 | 4-9 | 5 strategies + common exit logic |
| 3 | 10-12 | Backtest, paper trading, tournament manager |
| 4 | 13-16 | Live trading, safety, dashboard, main loop, integration |

**Total: 16 tasks, ~50 steps**
**Output: single file `counter-trade13.html` (~3000-4000 lines)**
