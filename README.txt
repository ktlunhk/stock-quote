Stock Quotes - AIDE Android Project
=========================================

This is a classic AIDE-compatible Android project (no AndroidX, no Java 8
lambdas / "->") that implements the same function as the Excel VBA macros
GetData / getYahooFinanceData.

What it does
------------
- Reads ticker, start date, end date, frequency (d/w/m)
- Converts dates to Unix timestamps (same as VBA)
- Maps d -> 1d, w -> 1wk, m -> 1mo
- Calls stock data chart API:
  https://query1.finance.yahoo.com/v8/finance/chart/{TICKER}
    ?period1=...&period2=...&interval=...&events=history&includeAdjustedClose=true
- Parses JSON and shows Date, Open, High, Low, Close, Adj Close, Volume

How to open in AIDE
-------------------
1. Copy the whole YahooStockQuotesAIDE folder to your device
   (or open the zip and extract it).
2. In AIDE: Menu -> Open existing project -> select the folder
   that contains AndroidManifest.xml.
3. Build & Run (AIDE will generate R.java automatically).

Requirements
------------
- AIDE (or any Ant-based Android build with API 14+)
- Internet permission (already declared)
- No external libraries required (uses built-in org.json and HttpURLConnection)

Notes
-----
- Default sample values: 1398.HK, 2024-08-01 to 2024-08-10, frequency d
- Date format must be yyyy-MM-dd
- Network runs on background thread (AsyncTask)
- Compatible with older Android (minSdk 14)
- No androidx.*, no support-v4, no lambdas

Source layout
-------------
src/com/yahoo/stockquotes/MainActivity.java   - all logic
res/layout/main.xml                           - UI
res/values/strings.xml
res/drawable/ic_launcher.png
AndroidManifest.xml
project.properties

Updated UI features
-------------------
- Date picker on Start / End date fields (tap the field)
- Time picker on Start / End time fields (tap the field)
- "Current Date" button: sets Start=today 00:00, End=today 23:59
- Frequency dropdown: Daily (d) / Weekly (w) / Monthly (m)
- Clear button: clears the result table and status


OpenCC4j (Traditional-Simplified Chinese)
-----------------------------------------
JARs in libs/ (required):
  opencc4j-1.14.0.jar
  heaven-0.13.0.jar
  nlp-common-0.0.5.jar

AIDE: place project so libs/ is on the classpath (default for AIDE projects).
Conversion is offline via ZhConverterUtil.toSimple(...).


UI refresh
----------
- New colour palette in res/values/colors.xml
- Gradient app bar, card-style input panel, rounded input fields
- Primary (blue) / secondary (grey) button drawables:
  btn_primary.xml, btn_rounded.xml (still used by Graph dialog buttons)
- Result table: smaller monospace font, header row tinted, horizontal
  scroll so columns stay aligned on narrow screens
- All view IDs unchanged; no Java changes required

Dialog refresh
--------------
- All popups (Select code, Analyze, Graph, date picker, Please wait) now use
  showStyledDialog(): rounded card, gradient title bar, rounded buttons
- Analyze shows stat tiles; Select code shows symbol + description rows
- Graph toggles (BB/MA/RSI/MACD) turn blue when on
- Result table: header strip has rounded top corners (table_header_top_bg.xml)
  and is hidden while the table is empty
