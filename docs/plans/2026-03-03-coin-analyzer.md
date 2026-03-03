# Coin Analyzer (counter-trade6.html) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 거래량/변동률 상위 5코인을 선정하고, 5개 새 전략(Volume Spike, BB Squeeze, Double Bottom, OBV Divergence, ATR Channel) 백테스트를 통해 코인별 최적 전략을 추천하는 독립 페이지.

**Architecture:** 단일 HTML 파일(counter-trade6.html). counter-trade5의 다크테마 CSS, 기본 지표 함수(calcRSI, calcBB, calcEMA), btSimulate 엔진을 복사 후 독립 구성. 새 지표 함수(calcOBV, calcATR) 추가. Lightweight Charts(CDN)로 캔들차트. 3-탭 구조.

**Tech Stack:** HTML/CSS/JS (vanilla), Bithumb v1 REST API, Lightweight Charts 4.x (CDN)

**5개 새 전략:**
1. **Volume Spike** — 거래량 3배 급증 + RSI ≤ 40 → 매수
2. **BB Squeeze** — BB 폭 수축 후 상방 브레이크아웃 → 매수
3. **Double Bottom** — 비슷한 가격대 두 번 지지 후 반등 → 매수
4. **OBV Divergence** — 가격 신저가 but OBV 상승 다이버전스 → 매수
5. **ATR Channel** — ATR 하단 터치 + 양봉 확인 → 매수

---

### Task 1: 기본 골격 — HTML/CSS/탭 구조 + 유틸 함수

**Files:**
- Create: `src/main/resources/static/html/counter-trade6.html`

**내용:**

1. `<head>`: 폰트(IBM Plex Mono, Noto Sans KR), Lightweight Charts CDN, CSS 변수
2. `<header>`: 로고 "Coin Analyzer", nav 링크
3. 탭 버튼 3개: `[코인 선정]` `[심층 분석]` `[설정]`
4. CSS: counter-trade5 다크테마 재사용

Lightweight Charts CDN:
```html
<script src="https://unpkg.com/lightweight-charts@4.1.3/dist/lightweight-charts.standalone.production.js"></script>
```

counter-trade5에서 복사할 함수들:
- `setCookie`, `getCookie`, `apiFetch`, `sleep`, `fmtPrice`, `fmt`, `fmtDt`
- `calcEMA`, `calcRSI`, `calcBollingerBands`, `calcMACD`, `calcStochRSI`, `calcCandleVolatility`
- `fetchHistoricalCandles`, `isInTradeHours`
- `btSimulate` (고가/저가 정밀, 손절 1.2배, 트레일링 목표도달 후)

Config:
```javascript
var API = 'https://api.bithumb.com/v1';
var FEE_RATE = 0.0004;
var FEE_ROUND = 0.0008;
var EXCLUDE_COINS = ['USDT','USDC','DAI','TUSD','BUSD','USDD','XAUT','TRX'];
var TOP_N = 5;
var selectedCoins = [];
var analysisData = {};
```

**커밋:** `feat: counter-trade6 기본 골격 (3탭 + 유틸 함수)`

---

### Task 2: 새 지표 함수 (calcOBV, calcATR)

**Files:**
- Modify: `src/main/resources/static/html/counter-trade6.html`

**calcOBV — On-Balance Volume:**
```javascript
function calcOBV(candles) {
  // candles: 최신순 [0]=최신, [N]=가장 오래된
  if (!candles || candles.length < 2) return [];
  var obv = [0];
  // 오래된→최신 순으로 누적
  for (var i = candles.length - 2; i >= 0; i--) {
    var prev = obv[obv.length - 1];
    var vol = candles[i].candle_acc_trade_volume || 0;
    if (candles[i].trade_price > candles[i + 1].trade_price) obv.push(prev + vol);
    else if (candles[i].trade_price < candles[i + 1].trade_price) obv.push(prev - vol);
    else obv.push(prev);
  }
  obv.reverse(); // 다시 최신순으로
  return obv;
}
```

**calcATR — Average True Range:**
```javascript
function calcATR(candles, period) {
  // candles: 최신순
  if (!candles || candles.length < period + 1) return [];
  var trs = [];
  for (var i = 0; i < candles.length - 1; i++) {
    var h = candles[i].high_price;
    var l = candles[i].low_price;
    var pc = candles[i + 1].trade_price; // 이전 봉 종가
    var tr = Math.max(h - l, Math.abs(h - pc), Math.abs(l - pc));
    trs.push(tr);
  }
  // EMA 방식 ATR
  if (trs.length < period) return [];
  var result = [];
  var sum = 0;
  for (var i = trs.length - 1; i >= trs.length - period; i--) sum += trs[i];
  var atr = sum / period;
  result.push(atr);
  for (var i = trs.length - period - 1; i >= 0; i--) {
    atr = (atr * (period - 1) + trs[i]) / period;
    result.push(atr);
  }
  return result; // 최신순
}
```

**커밋:** `feat: calcOBV, calcATR 지표 함수 추가`

---

### Task 3: 5개 새 전략 detect 함수

**Files:**
- Modify: `src/main/resources/static/html/counter-trade6.html`

**STRATEGIES + DETECT_FN:**
```javascript
var STRATEGIES = {
  volspike: '거래량폭발',
  bbsqueeze: 'BB스퀴즈',
  dblbottom: '이중바닥',
  obvdiv: 'OBV다이버전스',
  atrchannel: 'ATR채널'
};
var DETECT_FN = {
  volspike: detectVolSpike,
  bbsqueeze: detectBBSqueeze,
  dblbottom: detectDoubleBottom,
  obvdiv: detectOBVDivergence,
  atrchannel: detectATRChannel
};
```

**1) detectVolSpike — 거래량 폭발 + RSI:**
```javascript
function detectVolSpike(candles) {
  // 최근 거래량 vs 10봉 평균 비교
  if (candles.length < 15) return { signal: 'none', detail: '' };
  var recentVol = candles[0].candle_acc_trade_volume || 0;
  var avgVol = 0;
  for (var i = 1; i <= 10; i++) avgVol += (candles[i].candle_acc_trade_volume || 0);
  avgVol /= 10;
  var volRatio = avgVol > 0 ? recentVol / avgVol : 0;
  var rsi = calcRSI(candles, 14);
  var r = rsi.length > 0 ? rsi[0] : 50;

  // 거래량 3배 이상 + RSI 40 이하 + 양봉
  if (volRatio >= 3 && r <= 40 && candles[0].trade_price > candles[0].opening_price) {
    return { signal: 'buy', detail: 'Vol ' + volRatio.toFixed(1) + 'x RSI' + r.toFixed(0), sellCheck: function(sl) {
      var rs = calcRSI(sl, 14); return rs.length > 0 && rs[0] >= 65;
    }};
  }
  if (volRatio >= 2 && r <= 40) return { signal: 'wait', detail: 'Vol ' + volRatio.toFixed(1) + 'x RSI' + r.toFixed(0) };
  return { signal: 'none', detail: '' };
}
```

**2) detectBBSqueeze — BB 스퀴즈 브레이크아웃:**
```javascript
function detectBBSqueeze(candles) {
  if (candles.length < 30) return { signal: 'none', detail: '' };
  // 현재 BB 폭 vs 20봉 전 BB 폭 비교
  var bbNow = calcBollingerBands(candles.slice(0, 20), 20, 2);
  var bbPrev = calcBollingerBands(candles.slice(10, 30), 20, 2);
  if (!bbNow || !bbPrev) return { signal: 'none', detail: '' };

  var widthNow = (bbNow.upper - bbNow.lower) / bbNow.middle * 100;
  var widthPrev = (bbPrev.upper - bbPrev.lower) / bbPrev.middle * 100;
  var price = candles[0].trade_price;
  var squeeze = widthNow < widthPrev * 0.7; // 폭이 30% 이상 줄었으면 스퀴즈
  var breakout = price > bbNow.upper * 0.998; // 상단 근접/돌파

  if (squeeze && breakout) {
    return { signal: 'buy', detail: 'BB스퀴즈(' + widthNow.toFixed(1) + '→돌파)', sellCheck: function(sl) {
      var b = calcBollingerBands(sl.slice(0, 20), 20, 2);
      return b && sl[0].trade_price < b.middle;
    }};
  }
  if (squeeze) return { signal: 'wait', detail: 'BB스퀴즈 대기(' + widthNow.toFixed(1) + '%)' };
  return { signal: 'none', detail: '' };
}
```

**3) detectDoubleBottom — 이중 바닥:**
```javascript
function detectDoubleBottom(candles) {
  if (candles.length < 30) return { signal: 'none', detail: '' };
  // 최근 30봉에서 저점 2개 찾기
  var lows = [];
  for (var i = 2; i < Math.min(28, candles.length - 2); i++) {
    if (candles[i].low_price <= candles[i-1].low_price &&
        candles[i].low_price <= candles[i-2].low_price &&
        candles[i].low_price <= candles[i+1].low_price &&
        candles[i].low_price <= candles[i+2].low_price) {
      lows.push({ idx: i, price: candles[i].low_price });
    }
  }
  if (lows.length < 2) return { signal: 'none', detail: '' };

  // 첫번째 저점(더 오래된)과 두번째 저점(최근) 비교
  var bottom1 = lows[lows.length - 1]; // 가장 오래된 저점
  var bottom2 = lows[0]; // 가장 최근 저점

  // 두 저점이 5봉 이상 떨어져 있고, 가격 차이 1% 이내
  var dist = bottom1.idx - bottom2.idx;
  var priceDiff = Math.abs(bottom1.price - bottom2.price) / bottom1.price * 100;
  var curPrice = candles[0].trade_price;
  var neckline = 0;
  // 두 저점 사이 최고점 = 넥라인
  for (var i = bottom2.idx; i <= bottom1.idx; i++) {
    if (candles[i].high_price > neckline) neckline = candles[i].high_price;
  }

  if (dist >= 5 && priceDiff < 1.5 && bottom2.idx <= 5 && curPrice > neckline * 0.998) {
    return { signal: 'buy', detail: 'W바닥(' + priceDiff.toFixed(1) + '%차)', bbMiddle: neckline, sellCheck: function(sl) {
      // 넥라인 위 일정 비율 도달 시 매도
      return sl[0].trade_price < neckline * 0.99;
    }};
  }
  if (dist >= 5 && priceDiff < 1.5 && bottom2.idx <= 5) {
    return { signal: 'wait', detail: 'W바닥형성(' + priceDiff.toFixed(1) + '%차) 넥라인대기' };
  }
  return { signal: 'none', detail: '' };
}
```

**4) detectOBVDivergence — OBV 상승 다이버전스:**
```javascript
function detectOBVDivergence(candles) {
  if (candles.length < 20) return { signal: 'none', detail: '' };
  var obv = calcOBV(candles);
  if (obv.length < 15) return { signal: 'none', detail: '' };

  // 최근 저점 vs 10봉 전 저점 비교
  // 가격: 최근 저점 < 이전 저점 (신저가)
  // OBV: 최근 저점 > 이전 저점 (상승 다이버전스)
  var recentLow = candles[0].low_price;
  var recentOBV = obv[0];
  var prevLow = candles[0].low_price;
  var prevOBV = obv[0];

  // 최근 5봉 최저점
  for (var i = 0; i < 5; i++) {
    if (candles[i].low_price < recentLow) { recentLow = candles[i].low_price; recentOBV = obv[i]; }
  }
  // 5~15봉 전 최저점
  for (var i = 5; i < Math.min(15, candles.length); i++) {
    if (candles[i].low_price < prevLow) { prevLow = candles[i].low_price; prevOBV = obv[i]; }
  }

  var priceLower = recentLow < prevLow * 1.005; // 가격이 이전 저점 이하
  var obvHigher = recentOBV > prevOBV; // OBV는 이전보다 높음
  var isRising = candles[0].trade_price > candles[1].trade_price; // 현재 상승 중

  if (priceLower && obvHigher && isRising) {
    return { signal: 'buy', detail: 'OBV다이버전스(가격↓OBV↑)', sellCheck: function(sl) {
      var o = calcOBV(sl);
      return o.length > 1 && o[0] < o[1] && sl[0].trade_price < sl[1].trade_price;
    }};
  }
  if (priceLower && obvHigher) return { signal: 'wait', detail: 'OBV다이버전스 반등대기' };
  return { signal: 'none', detail: '' };
}
```

**5) detectATRChannel — ATR 채널 반등:**
```javascript
function detectATRChannel(candles) {
  if (candles.length < 20) return { signal: 'none', detail: '' };
  var atr = calcATR(candles, 14);
  var ema = calcEMA(candles, 20);
  if (atr.length === 0 || ema.length === 0) return { signal: 'none', detail: '' };

  var mid = ema[0];
  var lower = mid - atr[0] * 2; // ATR 채널 하단
  var upper = mid + atr[0] * 2;
  var price = candles[0].trade_price;
  var prevPrice = candles[1].trade_price;
  var isGreen = price > candles[0].opening_price; // 양봉

  // ATR 하단 터치 + 양봉 (반등 시작)
  if (price <= lower * 1.005 && isGreen) {
    return { signal: 'buy', detail: 'ATR하단(' + atr[0].toFixed(1) + ')', bbMiddle: mid, sellCheck: function(sl) {
      var e = calcEMA(sl, 20); return e.length > 0 && sl[0].trade_price >= e[0];
    }};
  }
  var lowerDist = mid > lower ? (price - lower) / (mid - lower) * 100 : 50;
  if (lowerDist <= 20) return { signal: 'wait', detail: 'ATR하단근접 ' + lowerDist.toFixed(0) + '%' };
  return { signal: 'none', detail: '' };
}
```

**커밋:** `feat: 5개 새 전략 (VolSpike, BBSqueeze, DoubleBottom, OBV, ATR)`

---

### Task 4: 탭1 — 코인 선정

**Files:**
- Modify: `src/main/resources/static/html/counter-trade6.html`

코인 선정 로직:
1. Bithumb KRW 마켓 전체 조회
2. 스테이블코인 제외 + 유효숫자 4자리 이상 필터
3. 점수 = `거래대금 × |24h변동률|`, 상위 TOP_N개

**유효숫자 필터:**
```javascript
function countSignificantDigits(price) {
  if (price <= 0) return 0;
  var s = price.toFixed(20);
  s = s.replace(/^0+\.?0*/, '').replace(/\./, '').replace(/0+$/, '');
  return s.length;
}
```

**scanCoins():** 마켓 조회 → 티커 조회 → 필터 → 점수 정렬 → 상위 5개 카드 렌더

**커밋:** `feat: 코인 선정 탭 (거래량×변동률 상위5, 유효숫자 필터)`

---

### Task 5: 탭3 — 설정 + 쿠키 저장

**Files:**
- Modify: `src/main/resources/static/html/counter-trade6.html`

설정 항목:
- 분봉 (3/5/15분)
- 백테스트 기간 (1/3/7일)
- 매매시간 (기본 08~16시 KST, 24시간 체크박스)
- 목표 수익률 (기본 0.5%)
- 손절 시간 (기본 10분)
- 최소 거래대금 (기본 10억)
- 선정 코인 수 (기본 5개)

ct6_ prefix 쿠키로 저장/복원.

**커밋:** `feat: 설정 탭 (분봉/기간/매매시간/목표수익률/손절)`

---

### Task 6: 탭2 — 심층 분석 (지표 대시보드 + 백테스트)

**Files:**
- Modify: `src/main/resources/static/html/counter-trade6.html`

**startAnalysis() 프로세스:**
1. 5코인 각각 캔들 데이터 수집
2. calcAllIndicators() — RSI, BB, MACD, StochRSI, EMA(10/20/60/100), OBV, ATR, 거래량추세, 변동성
3. 5전략 백테스트 (btSimulate 재사용)
4. 코인별 총수익률 1위 전략 = 추천 전략

**UI 구성:**
- 코인 서브탭 (5개, 추천전략 배지)
- 지표 대시보드 (RSI, BB위치, MACD, StochRSI, EMA배열, 거래량, OBV, ATR, 변동성)
- 5전략 백테스트 결과 테이블 (순위/승률/수익률)

**커밋:** `feat: 심층분석 탭 (9개 지표 대시보드 + 5전략 백테스트)`

---

### Task 7: Lightweight Charts 캔들차트

**Files:**
- Modify: `src/main/resources/static/html/counter-trade6.html`

**renderChart():**
- 캔들차트 + 거래량 히스토그램
- BB 오버레이 (상단/중앙/하단 점선)
- EMA 4개 라인 (10=금, 20=주황, 60=민트, 100=파랑)
- ATR 채널 (EMA20 ± ATR×2) 오버레이

다크테마 설정: background #0d1420, grid #1e2d47, 캔들 up=#00d4aa down=#ff4757

**커밋:** `feat: Lightweight Charts 캔들차트 + BB/EMA/ATR 오버레이`

---

### Task 8: 마무리 — nav 링크 + 최종 커밋 + 푸시

**Files:**
- Modify: `src/main/resources/static/html/counter-trade6.html`
- Modify: `src/main/resources/static/html/counter-trade5.html` (nav 링크 추가)

counter-trade5 ↔ counter-trade6 상호 nav 링크.

**최종 커밋:** `feat: Coin Analyzer 완성 (코인선정→심층분석→5전략추천)`

**푸시:** `git push`
