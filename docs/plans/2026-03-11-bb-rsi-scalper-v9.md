# BB+RSI Scalper v9 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** counter-trade8.html을 복사하여 counter-trade9.html 생성. 트레일링 스탑, 추세 필터, 분봉 적응형 파라미터, MDD 기반 스코어링, 데이터 분할 검증을 추가하여 수익률 개선.

**Architecture:** 단일 HTML 파일(counter-trade9.html) 내 JavaScript 수정. 기존 v8 구조(데이터수집→백테스트→최적화→실시간모니터링)를 유지하면서, 매도 로직 개편(트레일링 스탑), 매수 필터 보강(추세/RSI 반등 강도), 최적화 엔진 개선(MDD 스코어링, 적응형 그리드, 데이터 분할 검증)을 적용.

**Tech Stack:** Vanilla HTML/CSS/JavaScript, Bithumb REST API

**Source file:** `src/main/resources/static/html/counter-trade8.html` (2,644줄)
**Target file:** `src/main/resources/static/html/counter-trade9.html`

---

## Chunk 1: 파일 생성 + 헤더/UI 변경 + 트레일링 스탑

### Task 1: 파일 복사 및 헤더/네비 수정

**Files:**
- Create: `src/main/resources/static/html/counter-trade9.html` (counter-trade8.html 복사 후 수정)

- [ ] **Step 1: counter-trade8.html을 counter-trade9.html로 복사**

```bash
cp src/main/resources/static/html/counter-trade8.html src/main/resources/static/html/counter-trade9.html
```

- [ ] **Step 2: 타이틀/헤더 텍스트 변경**

`counter-trade9.html`에서 다음을 변경:

```
<title>BB+RSI Scalper</title>
→
<title>BB+RSI Scalper v2</title>
```

```
<h1>BB+RSI Scalper</h1><span>볼린저밴드 + RSI 복합 스캘핑 백테스트</span>
→
<h1>BB+RSI Scalper v2</h1><span>트레일링 스탑 + 추세필터 + MDD 최적화</span>
```

```
<div class="logo-icon">BS</div>
→
<div class="logo-icon">B2</div>
```

- [ ] **Step 3: 네비게이션 링크 수정**

현재 nav-links 영역:
```html
<a href="counter-trade5.html">Arena</a>
<a href="counter-trade6.html">AI Dip</a>
<a href="counter-trade7.html">RSI Crash</a>
<a href="counter-trade8.html" class="active">BB+RSI</a>
```

변경:
```html
<a href="counter-trade5.html">Arena</a>
<a href="counter-trade6.html">AI Dip</a>
<a href="counter-trade7.html">RSI Crash</a>
<a href="counter-trade8.html">BB+RSI</a>
<a href="counter-trade9.html" class="active">BB+RSI v2</a>
```

- [ ] **Step 4: 쿠키 접두어 변경**

모든 `ct8_`를 `ct9_`로 일괄 치환 (쿠키/localStorage 키 충돌 방지):
- `ct8_tf` → `ct9_tf`
- `ct8_days` → `ct9_days`
- `ct8_positions` → `ct9_positions`
- `ct8_tradeHistory` → `ct9_tradeHistory`
- `ct8_cache` → `ct9_cache`
- 기타 모든 `ct8_` 접두어

- [ ] **Step 5: 전략 요약 칩 수정**

현재:
```html
<span class="strategy-chip buy-rule">BUY: RSI&le;35 + BB하단 근접/이탈 후 복귀</span>
<span class="strategy-chip sell-rule">SELL: RSI&ge;50 or BB중심선 or 목표/손절/시간</span>
<span class="strategy-chip vol-rule">VOL: 반등시 거래량 &ge;1.5배 (선택)</span>
<span style="font-size:10px;color:var(--muted);font-family:'IBM Plex Mono',monospace">BB근접 0.3% | 되돌림 5봉</span>
```

변경:
```html
<span class="strategy-chip buy-rule">BUY: RSI&le;35 + BB하단 근접 + 추세필터</span>
<span class="strategy-chip sell-rule">SELL: 트레일링스탑 or RSI&ge;65(수익시) or BB중심(수익시) or 손절/시간</span>
<span class="strategy-chip vol-rule">VOL: 반등시 거래량 &ge;1.5배 (선택)</span>
<span style="font-size:10px;color:var(--muted);font-family:'IBM Plex Mono',monospace">트레일링 0.4% | 활성화 0.3%</span>
```

- [ ] **Step 6: placeholder 텍스트 수정**

현재 placeholder의 설명 부분:
```html
<div style="font-size:14px;font-weight:700">BB+RSI Scalper Backtest</div>
<div style="font-size:12px;margin-top:8px">볼린저밴드 하단 이탈 후 복귀 + RSI 과매도 V자 반등 복합 신호</div>
<div style="font-size:11px;margin-top:12px;line-height:1.8">
  <span class="strategy-chip buy-rule">매수</span> RSI &le; 35 &amp; BB하단 근접(0.3%)/이탈 후 복귀 (5봉내)<br>
  <span class="strategy-chip sell-rule">매도</span> RSI &ge; 50 or BB중심선 도달 or 목표/손절/시간<br>
  <span class="strategy-chip vol-rule">확인</span> 반등 거래량 &ge; 평균 &times; 1.5
</div>
```

변경:
```html
<div style="font-size:14px;font-weight:700">BB+RSI Scalper v2 Backtest</div>
<div style="font-size:12px;margin-top:8px">트레일링 스탑 + 추세 필터 + MDD 최적화 강화 버전</div>
<div style="font-size:11px;margin-top:12px;line-height:1.8">
  <span class="strategy-chip buy-rule">매수</span> RSI &le; 35 &amp; BB하단 근접 + 추세필터 + RSI반등강도<br>
  <span class="strategy-chip sell-rule">매도</span> 트레일링스탑 or RSI &ge; 65(수익시) or BB중심(수익시) or 손절/시간<br>
  <span class="strategy-chip vol-rule">확인</span> 반등 거래량 &ge; 평균 &times; 1.5
</div>
```

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: BB+RSI Scalper v9 파일 생성 - 헤더/네비/쿠키 분리"
```

---

### Task 2: 컨트롤바에 트레일링 스탑 & 분봉 적응형 UI 추가

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: 컨트롤바 Row 2에 트레일링 파라미터 추가**

현재 `손절` 입력 뒤에 `시간제한` 입력이 있는 영역 이후, `최소거래대금` 앞에 트레일링 필드 추가.

`시간제한` 입력 뒤 (`<span style="font-size:11px;color:var(--muted)">분</span>`) 다음에 삽입:

```html
<span class="sep">|</span>
<span class="ctrl-label">트레일링</span>
<input type="text" id="trailPct" value="0.4" class="ctrl-input" style="width:45px" title="고점 대비 하락시 청산%">
<span style="font-size:11px;color:var(--muted)">%</span>
<span class="ctrl-label">활성화</span>
<input type="text" id="trailActivate" value="0.3" class="ctrl-input" style="width:45px" title="트레일링 스탑 활성화 수익%">
<span style="font-size:11px;color:var(--muted)">%</span>
```

- [ ] **Step 2: RSI매도 기본값을 50→65로 변경**

```
<input type="text" id="rsiSell" value="50"
→
<input type="text" id="rsiSell" value="65"
```

title도 변경:
```
title="RSI 이상에서 매도"
→
title="수익시 RSI 이상에서 매도"
```

- [ ] **Step 3: INPUT_COOKIES에 트레일링 파라미터 추가**

`INPUT_COOKIES` 배열에 추가:
```js
{ id: 'trailPct', key: 'ct9_trailPct' },
{ id: 'trailActivate', key: 'ct9_trailActivate' },
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 트레일링 스탑 UI 컨트롤 추가"
```

---

### Task 3: 분봉 적응형 기본값 함수 추가

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: 분봉별 적응형 기본값 함수 추가**

`getTickPct` 함수 뒤 (line ~644 근방)에 추가:

```js
// ========================================
// Timeframe-adaptive defaults
// ========================================
function getTfDefaults(tf) {
  tf = parseInt(tf) || 1;
  if (tf <= 1)  return { stopLoss: 0.5, timeLimit: 20, profitTarget: 0.7 };
  if (tf <= 3)  return { stopLoss: 0.7, timeLimit: 40, profitTarget: 1.0 };
  if (tf <= 5)  return { stopLoss: 1.0, timeLimit: 60, profitTarget: 1.5 };
  if (tf <= 10) return { stopLoss: 1.2, timeLimit: 90, profitTarget: 1.8 };
  return              { stopLoss: 1.5, timeLimit: 120, profitTarget: 2.0 }; // 15분+
}
```

- [ ] **Step 2: 분봉 변경 시 기본값 자동 업데이트**

`setTf` 함수를 수정. 현재:
```js
function setTf(btn) {
  document.querySelectorAll('.ctrl-btn[data-tf]').forEach(function(b) { b.classList.remove('active'); });
  btn.classList.add('active'); btTf = btn.dataset.tf;
  setCookie('ct9_tf', btTf);
}
```

변경:
```js
function setTf(btn) {
  document.querySelectorAll('.ctrl-btn[data-tf]').forEach(function(b) { b.classList.remove('active'); });
  btn.classList.add('active'); btTf = btn.dataset.tf;
  setCookie('ct9_tf', btTf);
  // 분봉별 적응형 기본값 제안
  var defs = getTfDefaults(btTf);
  var slEl = document.getElementById('stopLoss');
  var tlEl = document.getElementById('timeLimit');
  var ptEl = document.getElementById('profitTarget');
  if (!getCookie('ct9_stopLoss'))   slEl.value = defs.stopLoss;
  if (!getCookie('ct9_timeLimit'))   tlEl.value = defs.timeLimit;
  if (!getCookie('ct9_profitTarget')) ptEl.value = defs.profitTarget;
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 분봉 적응형 기본값 함수 추가"
```

---

### Task 4: backtestCoin 매수 로직 보강

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: 매수 조건에 추세 필터 추가**

`backtestCoin` 함수 내부, 현재 `// 6. 연속하락 필터` 블록 바로 뒤, `// 7. Volume confirmation` 앞에 추가:

```js
    // 6.5. 추세 필터: BB중심선 기울기 확인 (급락 추세 회피)
    if (bbIdx >= 3) {
      var bbMidNow = bb.mid[bbIdx];
      var bbMid3ago = bb.mid[bbIdx - 3];
      var bbSlope = ((bbMidNow - bbMid3ago) / bbMid3ago) * 100;
      if (bbSlope < -0.5) continue; // BB중심선이 3봉간 0.5%이상 하락 = 강한 하락추세
    }
```

- [ ] **Step 2: RSI 반등 강도 조건 강화**

현재 (backtestCoin 내):
```js
    // 2. RSI 반등 확인: 직전봉 RSI보다 현재 RSI가 높아야 함 (V자 반등)
    var prevRsiIdx = (i - 1) - params.rsiPeriod;
    if (prevRsiIdx >= 0 && prevRsiIdx < rsiArr.length) {
      if (curRsi <= rsiArr[prevRsiIdx]) continue; // RSI가 계속 하락중이면 스킵
    }
```

변경:
```js
    // 2. RSI 반등 확인: lookback 내 최저 RSI 대비 2 이상 반등해야 함
    var rsiMin = curRsi;
    for (var ri = 1; ri <= Math.min(params.lookback, 5) && (rsiIdx - ri) >= 0; ri++) {
      if (rsiArr[rsiIdx - ri] < rsiMin) rsiMin = rsiArr[rsiIdx - ri];
    }
    if (curRsi - rsiMin < 2) continue; // RSI 반등 강도 부족
```

- [ ] **Step 3: 연속하락 필터 완화 (3봉→4봉)**

현재:
```js
    // 6. 연속하락 필터: 직전 3봉 연속 음봉이면 스킵 (낙하중 진입 방지)
    if (i >= 3) {
      var consecutiveDown = 0;
      for (var cd = 1; cd <= 3 && (i - cd) >= 0; cd++) {
```
```js
      if (consecutiveDown >= 3) continue;
```

변경:
```js
    // 6. 연속하락 필터: 직전 4봉 연속 음봉이면 스킵 (낙하중 진입 방지, 3→4 완화)
    if (i >= 4) {
      var consecutiveDown = 0;
      for (var cd = 1; cd <= 4 && (i - cd) >= 0; cd++) {
```
```js
      if (consecutiveDown >= 4) continue;
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 매수 로직 보강 - 추세필터, RSI반등강도, 연속하락완화"
```

---

### Task 5: backtestCoin 매도 로직 개편 (트레일링 스탑)

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: params 파싱에 트레일링 파라미터 추가**

`runStrategy()` 함수 내 params 객체에 추가:

```js
    trailPct: parseFloat(document.getElementById('trailPct').value) || 0.4,
    trailActivate: parseFloat(document.getElementById('trailActivate').value) || 0.3,
```

- [ ] **Step 2: backtestCoin 매도 로직 전면 교체**

현재 `backtestCoin` 함수의 `// Position management` 블록 (약 line 884~916)을 전면 교체.

현재:
```js
    // Position management
    if (inPos && entry) {
      var gp = ((curClose - entry.price) / entry.price) * 100;
      var gpHigh = ((curHigh - entry.price) / entry.price) * 100;
      var gpLow = ((curLow - entry.price) / entry.price) * 100;
      var elapsed = curTimeMs - entry.time;
      if (gpHigh > entry.peakPct) entry.peakPct = gpHigh;

      var reason = null;
      var sellPrice = curClose * (1 - SLIPPAGE); // 매도 슬리피지
      // Stop loss (check low price)
      if (gpLow <= stopPct) { reason = 'stoploss'; sellPrice = entry.price * (1 + stopPct / 100); }
      // Profit target (check high price)
      if (!reason && gpHigh >= reqPct) { reason = 'profit'; sellPrice = entry.price * (1 + reqPct / 100); }
      // RSI sell signal: RSI >= rsiSell (최소 1봉 보유)
      if (!reason && elapsed >= tfMs && curRsi >= params.rsiSell) { reason = 'rsi_exit'; sellPrice = curClose * (1 - SLIPPAGE); }
      // BB middle touch (최소 1봉 보유 - 즉시 탈출 방지)
      if (!reason && elapsed >= tfMs && curHigh >= curBBMid) { reason = 'bb_mid'; sellPrice = Math.min(curClose, curBBMid) * (1 - SLIPPAGE); }
      // Timeout
      if (!reason && elapsed >= stopMs) { reason = 'timeout'; sellPrice = curClose * (1 - SLIPPAGE); }

      if (reason) {
        var netPct = ((sellPrice - entry.price) / entry.price) * 100 - (FEE_ROUND * 100);
        trades.push({
          buyTime: entry.time, buyPrice: entry.price, buyRsi: entry.rsi,
          buyBBLower: entry.bbLower, buyBBMid: entry.bbMid,
          sellTime: curTimeMs, sellPrice: sellPrice, sellRsi: curRsi,
          netPct: netPct, reason: reason, elapsed: elapsed,
          volSurge: entry.volSurge
        });
        inPos = false; entry = null;
      }
      continue;
    }
```

변경:
```js
    // Position management
    if (inPos && entry) {
      var gp = ((curClose - entry.price) / entry.price) * 100;
      var gpHigh = ((curHigh - entry.price) / entry.price) * 100;
      var gpLow = ((curLow - entry.price) / entry.price) * 100;
      var elapsed = curTimeMs - entry.time;
      if (gpHigh > entry.peakPct) entry.peakPct = gpHigh;

      var reason = null;
      var sellPrice = curClose * (1 - SLIPPAGE);

      // 1. Stop loss (항상 최우선)
      if (gpLow <= stopPct) {
        reason = 'stoploss'; sellPrice = entry.price * (1 + stopPct / 100);
      }

      // 2. 트레일링 스탑: 고점 대비 trailPct% 하락 시 청산 (활성화 조건 충족 후)
      if (!reason && entry.peakPct >= params.trailActivate) {
        var dropFromPeak = entry.peakPct - gpHigh;
        if (dropFromPeak >= params.trailPct) {
          reason = 'trail';
          // 트레일링 매도가 = 고점에서 trailPct% 빠진 가격
          var trailPrice = entry.price * (1 + (entry.peakPct - params.trailPct) / 100);
          sellPrice = Math.min(curClose, trailPrice) * (1 - SLIPPAGE);
        }
      }

      // 3. RSI 매도: 수익 구간에서만 + RSI >= rsiSell (최소 1봉 보유)
      if (!reason && elapsed >= tfMs && gp > 0 && curRsi >= params.rsiSell) {
        reason = 'rsi_exit'; sellPrice = curClose * (1 - SLIPPAGE);
      }

      // 4. BB중심선: 수익 구간에서만 (최소 1봉 보유)
      if (!reason && elapsed >= tfMs && gp > 0 && curHigh >= curBBMid) {
        reason = 'bb_mid'; sellPrice = Math.min(curClose, curBBMid) * (1 - SLIPPAGE);
      }

      // 5. Timeout
      if (!reason && elapsed >= stopMs) {
        reason = 'timeout'; sellPrice = curClose * (1 - SLIPPAGE);
      }

      if (reason) {
        var netPct = ((sellPrice - entry.price) / entry.price) * 100 - (FEE_ROUND * 100);
        trades.push({
          buyTime: entry.time, buyPrice: entry.price, buyRsi: entry.rsi,
          buyBBLower: entry.bbLower, buyBBMid: entry.bbMid,
          sellTime: curTimeMs, sellPrice: sellPrice, sellRsi: curRsi,
          netPct: netPct, reason: reason, elapsed: elapsed,
          volSurge: entry.volSurge
        });
        inPos = false; entry = null;
      }
      continue;
    }
```

- [ ] **Step 3: reasonMap에 'trail' 추가**

`renderRankingPanel`, `renderTradesPanel`, `showCoinTrades` 함수 내 `reasonMap` 객체에 추가:
```js
var reasonMap = { profit: '익절', trail: '트레일링', stoploss: '손절', timeout: '시간초과', rsi_exit: 'RSI매도', bb_mid: 'BB중심', open: '미결' };
var reasonColors = { profit: 'var(--up)', trail: 'var(--gold)', stoploss: 'var(--down)', timeout: 'var(--muted)', rsi_exit: 'var(--accent3)', bb_mid: 'var(--purple)', open: 'var(--gold)' };
```

이 수정은 3곳에 적용:
1. `renderRankingPanel` 함수 내 reasonMap/reasonColors
2. `renderTradesPanel` 함수 내 reasonMap/reasonColors
3. `showCoinTrades` 함수 내 reasonMap/reasonColors

또한 `renderRankingPanel`의 랭킹 테이블 헤더:
```
<th>익절/손절/RSI/BB/시간</th>
```
변경:
```
<th>트레일/익절/손절/RSI/BB/시간</th>
```

해당 행 데이터도:
```js
+ '<td class="mono" style="font-size:10px">' + (r.profit || 0) + '/' + (r.stoploss || 0) + '/' + (r.rsi_exit || 0) + '/' + (r.bb_mid || 0) + '/' + (r.timeout || 0) + '</td>'
```
변경:
```js
+ '<td class="mono" style="font-size:10px">' + (r.trail || 0) + '/' + (r.profit || 0) + '/' + (r.stoploss || 0) + '/' + (r.rsi_exit || 0) + '/' + (r.bb_mid || 0) + '/' + (r.timeout || 0) + '</td>'
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 매도 로직 개편 - 트레일링 스탑, 조건부 RSI/BB 청산"
```

---

## Chunk 2: fastBacktest 개편 + 최적화 엔진 개선

### Task 6: fastBacktest에 트레일링 스탑 + MDD 계산 추가

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: fastBacktest 함수 전면 교체**

현재 `fastBacktest` 함수 (약 line 1644~1751)를 전면 교체:

```js
function fastBacktest(ind, params) {
  var startIdx = Math.max(ind.rsiPeriod, ind.bbPeriod - 1) + 1;
  var reqPct = params.profitTarget + (FEE_ROUND * 100);
  var stopPct = -(params.stopLoss + FEE_ROUND * 100);
  var stopMs = params.timeLimit * 60000;
  var bbProxR = params.bbProx / 100;
  var lookbackN = params.lookback;
  var hourStart = params.hourStart;
  var hourEnd = params.hourEnd;
  var trailPct = params.trailPct || 0.4;
  var trailActivate = params.trailActivate || 0.3;

  var tfMs = (params.tf || 5) * 60000;
  var trades = 0, wins = 0, totalPct = 0;
  var inPos = false, entryPrice = 0, entryTime = 0, peakPct = 0;

  // MDD tracking
  var cumPct = 0, cumPeak = 0, maxDD = 0;

  for (var i = startIdx; i < ind.closes.length; i++) {
    var rsiIdx = i - ind.rsiPeriod;
    var bbIdx = i - (ind.bbPeriod - 1);
    if (rsiIdx < 0 || rsiIdx >= ind.rsiArr.length) continue;
    if (bbIdx < 0 || bbIdx >= ind.bb.mid.length) continue;

    var curRsi = ind.rsiArr[rsiIdx];
    var curClose = ind.closes[i];
    var curHigh = ind.highs[i];
    var curLow = ind.lows[i];
    var curOpen = ind.opens[i];
    var bbLow = ind.bb.lower[bbIdx];
    var bbMid = ind.bb.mid[bbIdx];
    var curTimeMs = ind.times[i].getTime();

    if (inPos) {
      var gpHigh = ((curHigh - entryPrice) / entryPrice) * 100;
      var gpLow = ((curLow - entryPrice) / entryPrice) * 100;
      var gp = ((curClose - entryPrice) / entryPrice) * 100;
      var elapsed = curTimeMs - entryTime;
      if (gpHigh > peakPct) peakPct = gpHigh;

      var sellPrice = curClose * (1 - SLIPPAGE);
      var sold = false;

      // 1. Stop loss
      if (gpLow <= stopPct) { sellPrice = entryPrice * (1 + stopPct / 100); sold = true; }
      // 2. Trailing stop
      else if (peakPct >= trailActivate) {
        var dropFromPeak = peakPct - gpHigh;
        if (dropFromPeak >= trailPct) {
          var trailPrice = entryPrice * (1 + (peakPct - trailPct) / 100);
          sellPrice = Math.min(curClose, trailPrice) * (1 - SLIPPAGE);
          sold = true;
        }
      }
      // 3. RSI exit (수익시만)
      if (!sold && elapsed >= tfMs && gp > 0 && curRsi >= params.rsiSell) { sellPrice = curClose * (1 - SLIPPAGE); sold = true; }
      // 4. BB mid (수익시만)
      if (!sold && elapsed >= tfMs && gp > 0 && curHigh >= bbMid) { sellPrice = Math.min(curClose, bbMid) * (1 - SLIPPAGE); sold = true; }
      // 5. Timeout
      if (!sold && elapsed >= stopMs) { sold = true; }

      if (sold) {
        var netPct = ((sellPrice - entryPrice) / entryPrice) * 100 - (FEE_ROUND * 100);
        trades++; totalPct += netPct;
        if (netPct > 0) wins++;
        // MDD update
        cumPct += netPct;
        if (cumPct > cumPeak) cumPeak = cumPct;
        var dd = cumPeak - cumPct;
        if (dd > maxDD) maxDD = dd;
        inPos = false;
      }
      continue;
    }

    // Hour filter
    var curHour = ind.times[i].getHours();
    if (hourStart <= hourEnd) { if (curHour < hourStart || curHour >= hourEnd) continue; }
    else { if (curHour < hourStart && curHour >= hourEnd) continue; }

    // RSI check
    if (curRsi > params.rsiBuy) continue;

    // RSI 반등 강도: lookback 내 최저 RSI 대비 2 이상 반등
    var rsiMin = curRsi;
    for (var ri = 1; ri <= Math.min(lookbackN, 5) && (rsiIdx - ri) >= 0; ri++) {
      if (ind.rsiArr[rsiIdx - ri] < rsiMin) rsiMin = ind.rsiArr[rsiIdx - ri];
    }
    if (curRsi - rsiMin < 2) continue;

    // BB proximity lookback
    var touched = false;
    for (var lb = 0; lb <= lookbackN && (i - lb) >= 0; lb++) {
      var ci2 = i - lb;
      var bi2 = ci2 - (ind.bbPeriod - 1);
      if (bi2 < 0 || bi2 >= ind.bb.lower.length) continue;
      if (ind.lows[ci2] <= ind.bb.lower[bi2] * (1 + bbProxR)) { touched = true; break; }
    }
    if (!touched) continue;

    // Re-entry check
    if (curClose < bbLow) continue;

    // 양봉 확인
    if (curClose <= curOpen) continue;

    // 연속하락 필터 (4봉)
    if (i >= 4) {
      var cDown = 0;
      for (var cd = 1; cd <= 4 && (i - cd) >= 0; cd++) {
        if (ind.closes[i - cd] < ind.opens[i - cd]) cDown++; else break;
      }
      if (cDown >= 4) continue;
    }

    // 추세 필터: BB중심선 기울기
    if (bbIdx >= 3) {
      var bbMidNow = ind.bb.mid[bbIdx];
      var bbMid3ago = ind.bb.mid[bbIdx - 3];
      if (((bbMidNow - bbMid3ago) / bbMid3ago) * 100 < -0.5) continue;
    }

    // Volume filter
    if (params.volFilter && i >= 20 && ind.volAvg[i] > 0) {
      if (ind.volumes[i] < ind.volAvg[i] * 1.5) continue;
    }

    inPos = true;
    entryPrice = curClose * (1 + SLIPPAGE);
    entryTime = curTimeMs;
    peakPct = 0;
  }

  // Close open position
  if (inPos) {
    var lp = ind.closes[ind.closes.length - 1] * (1 - SLIPPAGE);
    var netPct = ((lp - entryPrice) / entryPrice) * 100 - (FEE_ROUND * 100);
    trades++; totalPct += netPct;
    if (netPct > 0) wins++;
    cumPct += netPct;
    if (cumPct > cumPeak) cumPeak = cumPct;
    var dd = cumPeak - cumPct;
    if (dd > maxDD) maxDD = dd;
  }

  return { trades: trades, wins: wins, totalPct: totalPct, maxDD: maxDD };
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 fastBacktest 개편 - 트레일링스탑, MDD계산, 매수필터 동기화"
```

---

### Task 7: 최적화 그리드 확장 + 스코어링 개편

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: 최적화 그리드 교체**

`runOptimize` 함수 내 `var grid = {` 블록을 교체.

현재:
```js
  var grid = {
    rsiBuy:       [25, 30, 35, 40],
    rsiSell:      [45, 50, 55, 60],
    bbProx:       [0.2, 0.5, 1.0],
    lookback:     [3, 5, 8],
    profitTarget: [0.3, 0.5, 0.7, 1.0],
    stopLoss:     [0.3, 0.5, 0.8],
    timeLimit:    [10, 20, 30],
    volFilter:    [true, false]
  };
```

변경:
```js
  // 분봉 적응형 그리드
  var tfN = parseInt(btTf) || 1;
  var tlGrid, slGrid, ptGrid;
  if (tfN <= 1)       { tlGrid = [15, 25, 40];     slGrid = [0.4, 0.6, 0.8];       ptGrid = [0.5, 0.7, 1.0, 1.5]; }
  else if (tfN <= 3)  { tlGrid = [20, 40, 60];     slGrid = [0.5, 0.8, 1.0];       ptGrid = [0.7, 1.0, 1.5, 2.0]; }
  else if (tfN <= 5)  { tlGrid = [30, 60, 90];     slGrid = [0.7, 1.0, 1.5];       ptGrid = [1.0, 1.5, 2.0, 2.5]; }
  else if (tfN <= 10) { tlGrid = [45, 90, 120];    slGrid = [1.0, 1.5, 2.0];       ptGrid = [1.5, 2.0, 2.5, 3.0]; }
  else                { tlGrid = [60, 120, 180];   slGrid = [1.2, 1.5, 2.0];       ptGrid = [2.0, 2.5, 3.0, 4.0]; }

  var grid = {
    rsiBuy:        [25, 30, 35, 40],
    rsiSell:       [55, 60, 65, 70],
    bbProx:        [0.2, 0.5, 1.0],
    lookback:      [3, 5, 8],
    profitTarget:  ptGrid,
    stopLoss:      slGrid,
    timeLimit:     tlGrid,
    trailPct:      [0.3, 0.5, 0.7],
    trailActivate: [0.2, 0.4, 0.6],
    volFilter:     [true, false]
  };
```

- [ ] **Step 2: 조합 생성 루프에 트레일링 추가**

현재 combos 생성 루프를 교체:

```js
  var combos = [];
  for (var a = 0; a < grid.rsiBuy.length; a++)
  for (var b = 0; b < grid.rsiSell.length; b++)
  for (var c = 0; c < grid.bbProx.length; c++)
  for (var d = 0; d < grid.lookback.length; d++)
  for (var e = 0; e < grid.profitTarget.length; e++)
  for (var f = 0; f < grid.stopLoss.length; f++)
  for (var g = 0; g < grid.timeLimit.length; g++)
  for (var h = 0; h < grid.trailPct.length; h++)
  for (var j = 0; j < grid.trailActivate.length; j++)
  for (var k = 0; k < grid.volFilter.length; k++) {
    if (grid.rsiBuy[a] >= grid.rsiSell[b]) continue;
    // trailActivate는 profitTarget 이하만 의미 있음
    if (grid.trailActivate[j] > grid.profitTarget[e]) continue;
    combos.push({
      rsiBuy: grid.rsiBuy[a], rsiSell: grid.rsiSell[b],
      bbProx: grid.bbProx[c], lookback: grid.lookback[d],
      profitTarget: grid.profitTarget[e], stopLoss: grid.stopLoss[f],
      timeLimit: grid.timeLimit[g],
      trailPct: grid.trailPct[h], trailActivate: grid.trailActivate[j],
      volFilter: grid.volFilter[k],
      hourStart: hourStart, hourEnd: hourEnd,
      tf: parseInt(btTf) || 5
    });
  }
```

- [ ] **Step 3: 스코어링 함수 개편**

현재 스코어 계산 블록:
```js
    if (totTrades >= 5) {
      var wr = totWins / totTrades * 100;
      var avgPct = totPct / totTrades;
      // Score: 수익률 * 승률 보정 * 거래량 보정
      var tradeScore = Math.min(1, totTrades / 20);
      var score = totPct * (wr / 100) * tradeScore;
      optResults.push({
        params: p, trades: totTrades, wins: totWins,
        winRate: wr, totalPct: totPct, avgPct: avgPct, score: score
      });
    }
```

변경:
```js
    if (totTrades >= 5) {
      var wr = totWins / totTrades * 100;
      var avgPct = totPct / totTrades;
      var totMDD = 0;
      // MDD 합산
      for (var pi = 0; pi < preList.length; pi++) {
        if (p.profitTarget < preList[pi].tickPct * 2) continue;
        var r2 = fastBacktest(preList[pi].ind, p);
        totMDD = Math.max(totMDD, r2.maxDD);
      }
      // Score: 수익률 / (1+MDD) * 승률보정 * 매매수보정 * 건당평균보너스
      var tradeScore = Math.min(1, totTrades / 15);
      var avgBonus = avgPct > 0 ? 1.2 : 0.8;
      var score = (totPct / (1 + totMDD)) * (wr / 100) * tradeScore * avgBonus;
      optResults.push({
        params: p, trades: totTrades, wins: totWins,
        winRate: wr, totalPct: totPct, avgPct: avgPct, maxDD: totMDD, score: score
      });
    }
```

**주의**: 이렇게 하면 fastBacktest가 2번 호출됩니다(1번은 집계, 1번은 MDD). 성능 개선을 위해 첫 번째 루프에서 MDD도 동시에 수집하도록 최적화:

실제로는 첫 번째 루프를 수정하여 MDD도 같이 수집:
```js
    var totTrades = 0, totWins = 0, totPct = 0, totMDD = 0;

    for (var pi = 0; pi < preList.length; pi++) {
      if (p.profitTarget < preList[pi].tickPct * 2) continue;
      var r = fastBacktest(preList[pi].ind, p);
      totTrades += r.trades;
      totWins += r.wins;
      totPct += r.totalPct;
      if (r.maxDD > totMDD) totMDD = r.maxDD;
    }

    if (totTrades >= 5) {
      var wr = totWins / totTrades * 100;
      var avgPct = totPct / totTrades;
      var tradeScore = Math.min(1, totTrades / 15);
      var avgBonus = avgPct > 0 ? 1.2 : 0.8;
      var score = (totPct / (1 + totMDD)) * (wr / 100) * tradeScore * avgBonus;
      optResults.push({
        params: p, trades: totTrades, wins: totWins,
        winRate: wr, totalPct: totPct, avgPct: avgPct, maxDD: totMDD, score: score
      });
    }
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 최적화 그리드 확장 - 분봉적응형, 트레일링, MDD 스코어링"
```

---

### Task 8: 데이터 분할 검증 (과적합 방어)

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: runOptimize에 데이터 분할 검증 추가**

`runOptimize` 함수에서, 최종 `optResults.sort(...)` 직전에 검증 로직 삽입.

현재:
```js
  // Sort by score
  optResults.sort(function(a, b) { return b.score - a.score; });
```

변경 (해당 줄 직전에 삽입):
```js
  // ===== 데이터 분할 검증 (과적합 방어) =====
  progText.textContent = '데이터 분할 검증 중...';
  await sleep(10);

  // 캐시 데이터의 캔들을 70% 학습 / 30% 검증으로 분할
  var valPreList = [];
  for (var vi = 0; vi < cachedData.coins.length; vi++) {
    var coin = cachedData.coins[vi];
    if (coin.candles.length < 50) continue;
    // candles는 newest first → 앞 30%가 최신(검증), 뒤 70%가 과거(학습)
    var splitIdx = Math.floor(coin.candles.length * 0.3);
    var valCandles = coin.candles.slice(0, splitIdx);  // 최신 30%
    if (valCandles.length < 30) continue;
    var tickPct = getTickPct(valCandles[0].trade_price);
    valPreList.push({ ind: precompute(valCandles, rsiPeriod, bbPeriod, bbMult), symbol: coin.symbol, tickPct: tickPct });
  }

  // 상위 50개에 대해 검증 데이터로 재평가
  var top50 = optResults.sort(function(a, b) { return b.score - a.score; }).slice(0, 50);
  for (var ti = 0; ti < top50.length; ti++) {
    var p = top50[ti].params;
    var vTrades = 0, vWins = 0, vPct = 0, vMDD = 0;
    for (var vpi = 0; vpi < valPreList.length; vpi++) {
      if (p.profitTarget < valPreList[vpi].tickPct * 2) continue;
      var vr = fastBacktest(valPreList[vpi].ind, p);
      vTrades += vr.trades;
      vWins += vr.wins;
      vPct += vr.totalPct;
      if (vr.maxDD > vMDD) vMDD = vr.maxDD;
    }
    top50[ti].valTrades = vTrades;
    top50[ti].valWinRate = vTrades > 0 ? vWins / vTrades * 100 : 0;
    top50[ti].valPct = vPct;
    top50[ti].valMDD = vMDD;
    // 복합 스코어: 학습 40% + 검증 60%
    var valScore = vTrades >= 3 ? (vPct / (1 + vMDD)) * (top50[ti].valWinRate / 100) : 0;
    top50[ti].combinedScore = top50[ti].score * 0.4 + valScore * 0.6;
  }

  // 복합 스코어로 재정렬
  optResults = top50;
  optResults.sort(function(a, b) { return (b.combinedScore || b.score) - (a.combinedScore || a.score); });
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 데이터 분할 검증 - 학습70%/검증30% 과적합 방어"
```

---

### Task 9: 최적화 결과 UI 업데이트

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: renderOptimizePanel 함수 업데이트**

`renderOptimizePanel` 함수의 BEST 파라미터 표시에 트레일링 추가:

현재 BEST 파라미터 영역에 추가:
```js
    + '<span class="mono" style="font-size:11px">VOL=' + (bp.volFilter ? 'ON' : 'OFF') + '</span>'
```
뒤에:
```js
    + '<span class="mono" style="font-size:11px">트레일=' + bp.trailPct + '%</span>'
    + '<span class="mono" style="font-size:11px">활성화=' + bp.trailActivate + '%</span>'
```

테이블 헤더에 MDD, 검증수익률 컬럼 추가. 현재:
```js
    + '<th>순위</th><th>Score</th><th>수익률</th><th>승률</th><th>매매수</th><th>건당평균</th>'
    + '<th>RSI매수</th><th>RSI매도</th><th>BB근접</th><th>되돌림</th><th>목표</th><th>손절</th><th>제한</th><th>VOL</th><th></th>'
```

변경:
```js
    + '<th>순위</th><th>Score</th><th>수익률</th><th>검증</th><th>MDD</th><th>승률</th><th>매매수</th><th>건당평균</th>'
    + '<th>RSI매수</th><th>RSI매도</th><th>BB근접</th><th>되돌림</th><th>목표</th><th>손절</th><th>제한</th><th>트레일</th><th>활성화</th><th>VOL</th><th></th>'
```

테이블 행 렌더링에서, 현재 `avgPct` 뒤에 추가하고 트레일링 컬럼 추가:

현재 행 데이터:
```js
      + '<td class="mono" style="color:' + (r.avgPct >= 0 ? 'var(--up)' : 'var(--down)') + '">' + (r.avgPct >= 0 ? '+' : '') + r.avgPct.toFixed(3) + '%</td>'
```
뒤에 (VOL 앞에) 추가 삽입하여 전체 행:

```js
      + '<td class="mono" style="color:' + (r.avgPct >= 0 ? 'var(--up)' : 'var(--down)') + '">' + (r.avgPct >= 0 ? '+' : '') + r.avgPct.toFixed(3) + '%</td>'
```

이 줄 다음을 교체 (기존 RSI매수부터 끝까지):
```js
      + '<td style="color:' + ((r.valPct || 0) >= 0 ? 'var(--up)' : 'var(--down)') + ';font-size:10px">' + ((r.valPct || 0) >= 0 ? '+' : '') + (r.valPct || 0).toFixed(2) + '%</td>'
      + '<td style="color:var(--down);font-size:10px">' + (r.maxDD || 0).toFixed(2) + '%</td>'
      + '<td style="color:' + (r.winRate >= 50 ? 'var(--up)' : 'var(--down)') + '">' + r.winRate.toFixed(1) + '%</td>'
      + '<td>' + r.trades + '</td>'
      + '<td class="mono" style="color:' + (r.avgPct >= 0 ? 'var(--up)' : 'var(--down)') + '">' + (r.avgPct >= 0 ? '+' : '') + r.avgPct.toFixed(3) + '%</td>'
      + '<td class="mono">' + p.rsiBuy + '</td>'
      + '<td class="mono">' + p.rsiSell + '</td>'
      + '<td class="mono">' + p.bbProx + '</td>'
      + '<td class="mono">' + p.lookback + '</td>'
      + '<td class="mono">' + p.profitTarget + '%</td>'
      + '<td class="mono">' + p.stopLoss + '%</td>'
      + '<td class="mono">' + p.timeLimit + '분</td>'
      + '<td class="mono">' + p.trailPct + '%</td>'
      + '<td class="mono">' + p.trailActivate + '%</td>'
      + '<td>' + (p.volFilter ? '<span style="color:var(--purple)">ON</span>' : '<span style="color:var(--muted)">OFF</span>') + '</td>'
      + '<td><button onclick="applyParams(' + idx + ')" class="ctrl-btn" style="font-size:9px;padding:3px 8px">적용</button></td>'
```

- [ ] **Step 2: applyParams에 트레일링 파라미터 적용 추가**

`applyParams` 함수에서, 현재 `volFilter` 적용 후 트레일링 추가:

```js
  document.getElementById('trailPct').value = p.trailPct;
  document.getElementById('trailActivate').value = p.trailActivate;
  setCookie('ct9_trailPct', p.trailPct);
  setCookie('ct9_trailActivate', p.trailActivate);
```

- [ ] **Step 3: BEST 요약 카드에 MDD 추가**

stats-bar에 MDD 카드 추가 (BEST 매매수 뒤에):
```js
    + '<div class="stat-card"><div class="stat-label">BEST MDD</div><div class="stat-value" style="color:var(--down)">' + (best.maxDD || 0).toFixed(2) + '%</div></div>'
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 최적화 결과 UI - MDD, 검증수익률, 트레일링 파라미터 표시"
```

---

## Chunk 3: 실시간 모니터링 동기화 + 마무리

### Task 10: 실시간 모니터링 매수/매도 로직 v9 동기화

**Files:**
- Modify: `src/main/resources/static/html/counter-trade9.html`

- [ ] **Step 1: monitorCycle 매도 로직에 트레일링 스탑 추가**

`monitorCycle` 함수 내 `// ===== SELL CHECK =====` 블록을 교체.

현재 매도 로직:
```js
        var reason = null;
        var idealSellPrice = curPrice;
        // 손절: 저가 기준으로 조기 감지
        if (gpLow <= stopPct) { reason = 'stoploss'; idealSellPrice = pos.price * (1 + stopPct / 100); }
        // 익절: 고가 기준으로 감지
        if (!reason && gpHigh >= reqPct) { reason = 'profit'; idealSellPrice = pos.price * (1 + reqPct / 100); }
        // RSI매도: 최소 1봉 보유 후
        if (!reason && elapsed >= tfMs && curRsi >= params.rsiSell) { reason = 'rsi_exit'; idealSellPrice = curPrice; }
        // BB중심: 최소 1봉 보유 후 (즉시 탈출 방지)
        if (!reason && elapsed >= tfMs && curHigh >= curBBMid) { reason = 'bb_mid'; idealSellPrice = Math.min(curPrice, curBBMid); }
        // 시간초과
        if (!reason && elapsed >= stopMs) { reason = 'timeout'; idealSellPrice = curPrice; }
```

변경:
```js
        // peakPct 업데이트
        if (!pos.peakPct) pos.peakPct = 0;
        if (gpHigh > pos.peakPct) { pos.peakPct = gpHigh; savePositions(); }

        var trailPctVal = parseFloat(document.getElementById('trailPct').value) || 0.4;
        var trailActivateVal = parseFloat(document.getElementById('trailActivate').value) || 0.3;

        var reason = null;
        var idealSellPrice = curPrice;
        // 1. 손절
        if (gpLow <= stopPct) { reason = 'stoploss'; idealSellPrice = pos.price * (1 + stopPct / 100); }
        // 2. 트레일링 스탑
        if (!reason && pos.peakPct >= trailActivateVal) {
          var dropFromPeak = pos.peakPct - gpHigh;
          if (dropFromPeak >= trailPctVal) {
            reason = 'trail';
            var trailPrice = pos.price * (1 + (pos.peakPct - trailPctVal) / 100);
            idealSellPrice = Math.min(curPrice, trailPrice);
          }
        }
        // 3. RSI매도 (수익시만, 최소 1봉)
        if (!reason && elapsed >= tfMs && gp > 0 && curRsi >= params.rsiSell) { reason = 'rsi_exit'; idealSellPrice = curPrice; }
        // 4. BB중심 (수익시만, 최소 1봉)
        if (!reason && elapsed >= tfMs && gp > 0 && curHigh >= curBBMid) { reason = 'bb_mid'; idealSellPrice = Math.min(curPrice, curBBMid); }
        // 5. 시간초과
        if (!reason && elapsed >= stopMs) { reason = 'timeout'; idealSellPrice = curPrice; }
```

- [ ] **Step 2: monitorCycle 매수 로직에 추세 필터 + RSI 반등 강도 추가**

현재 매수 신호 `// 2. RSI 반등 확인` 블록을 교체:

현재:
```js
      // 2. RSI 반등 확인: 직전봉 RSI보다 현재 RSI가 높아야 함
      var prevRsiIdx2 = (curIdx - 1) - params.rsiPeriod;
      if (prevRsiIdx2 >= 0 && prevRsiIdx2 < rsiArr.length) {
        if (curRsi <= rsiArr[prevRsiIdx2]) {
          if (statusEl) statusEl.innerHTML = '<span style="color:var(--accent2)">RSI ' + curRsi.toFixed(1) + ' (하락중)</span>';
          continue;
        }
      }
```

변경:
```js
      // 2. RSI 반등 강도: lookback 내 최저 RSI 대비 2 이상 반등
      var rsiMinLive = curRsi;
      for (var rli = 1; rli <= Math.min(params.lookback, 5) && (rsiIdx - rli) >= 0; rli++) {
        if (rsiArr[rsiIdx - rli] < rsiMinLive) rsiMinLive = rsiArr[rsiIdx - rli];
      }
      if (curRsi - rsiMinLive < 2) {
        if (statusEl) statusEl.innerHTML = '<span style="color:var(--accent2)">RSI ' + curRsi.toFixed(1) + ' (반등부족)</span>';
        continue;
      }
```

현재 `// 6. 연속하락 필터` 블록의 `consDown >= 3`을 `consDown >= 4`로 변경하고 루프도 `<= 4`로:

```js
      // 6. 연속하락 필터: 직전 4봉 연속 음봉이면 스킵
      if (curIdx >= 4) {
        var consDown = 0;
        for (var cd = 1; cd <= 4 && (curIdx - cd) >= 0; cd++) {
          var cdClose = closes[curIdx - cd];
          var cdOpen = asc[curIdx - cd] ? asc[curIdx - cd].opening_price || cdClose : cdClose;
          if (cdClose < cdOpen) consDown++; else break;
        }
        if (consDown >= 4) {
          if (statusEl) statusEl.innerHTML = '<span style="color:var(--accent2)">RSI ' + curRsi.toFixed(1) + ' (연속하락)</span>';
          continue;
        }
      }
```

BB 근접 체크 (`// 3. BB lower proximity`) 뒤, `// 4. Re-entry` 앞에 추세 필터 추가:

```js
      // 3.5. 추세 필터: BB중심선 기울기
      if (bbIdx >= 3) {
        var bbMidNowLive = bb.mid[bbIdx];
        var bbMid3agoLive = bb.mid[bbIdx - 3];
        if (((bbMidNowLive - bbMid3agoLive) / bbMid3agoLive) * 100 < -0.5) {
          if (statusEl) statusEl.innerHTML = '<span style="color:var(--accent2)">RSI ' + curRsi.toFixed(1) + ' (하락추세)</span>';
          continue;
        }
      }
```

- [ ] **Step 3: monitorCycle reasonMap에 trail 추가**

현재:
```js
          var reasonMap = { profit: '익절', stoploss: '손절', timeout: '시간초과', rsi_exit: 'RSI매도', bb_mid: 'BB중심' };
```

변경:
```js
          var reasonMap = { profit: '익절', trail: '트레일링', stoploss: '손절', timeout: '시간초과', rsi_exit: 'RSI매도', bb_mid: 'BB중심' };
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v9 실시간 모니터링 동기화 - 트레일링, 추세필터, RSI반등강도"
```

---

### Task 11: v8 네비에 v9 링크 추가

**Files:**
- Modify: `src/main/resources/static/html/counter-trade8.html`

- [ ] **Step 1: counter-trade8.html 네비에 v9 링크 추가**

현재:
```html
<a href="counter-trade8.html" class="active">BB+RSI</a>
```

변경:
```html
<a href="counter-trade8.html" class="active">BB+RSI</a>
<a href="counter-trade9.html">BB+RSI v2</a>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/html/counter-trade8.html src/main/resources/static/html/counter-trade9.html
git commit -m "feat: v8↔v9 네비게이션 연결"
```

---

### Task 12: 최종 검증

- [ ] **Step 1: 브라우저에서 counter-trade9.html 열기**

확인 사항:
1. 헤더가 "BB+RSI Scalper v2"로 표시
2. 네비에 "BB+RSI v2" 링크가 active
3. 트레일링%, 활성화% 입력 필드 존재
4. RSI매도 기본값이 65
5. 전략 요약 칩이 v9 내용 반영

- [ ] **Step 2: 데이터 수집 → 전략 실행 테스트**

1. "데이터 수집" 클릭 → 코인 데이터 정상 수집
2. "전략 실행" 클릭 → 백테스트 결과에 "트레일링" 매도 사유 표시 확인
3. 코인 랭킹 테이블에 "트레일/익절/손절/RSI/BB/시간" 컬럼 확인

- [ ] **Step 3: 자동 최적화 테스트**

1. "자동 최적화" 클릭 → 최적화 진행
2. 결과 테이블에 MDD, 검증수익률, 트레일, 활성화 컬럼 확인
3. "적용" 버튼 → 트레일링 파라미터도 자동 적용 확인

- [ ] **Step 4: 모니터링 테스트**

1. 전략 실행 후 "모니터링 시작" 클릭
2. 보유 포지션에서 수익률, 경과 시간 실시간 업데이트 확인
3. 로그에 "하락추세", "반등부족" 등 v9 필터 메시지 확인

- [ ] **Step 5: Final Commit**

```bash
git add -A
git commit -m "feat: BB+RSI Scalper v9 완성 - 트레일링스탑, 추세필터, MDD최적화, 분할검증"
```
