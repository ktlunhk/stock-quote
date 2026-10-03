package com.stockquote;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.stockquote.analysis.ReturnStats;
import com.stockquote.export.FileExporter;
import com.stockquote.export.ShareHelper;
import com.stockquote.model.QuoteResult;
import com.stockquote.model.SearchHit;
import com.stockquote.model.StockData;
import com.stockquote.net.QuoteParser;
import com.stockquote.net.YahooChartClient;
import com.stockquote.search.SymbolSearchService;
import com.stockquote.task.FetchQuoteTask;
import com.stockquote.task.SearchSymbolTask;
import com.stockquote.ui.AnalyzeDialog;
import com.stockquote.ui.DateRangeController;
import com.stockquote.ui.GraphDialog;
import com.stockquote.ui.ResultPanel;
import com.stockquote.ui.SearchResultsDialog;
import com.stockquote.ui.WaitDialog;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Stock quotes for AIDE (no AndroidX, no lambdas).
 * Search by company name to ticker, historical data, save CSV, share WhatsApp.
 *
 * This class only wires the screen together. The work lives in:
 *   search/   name to ticker lookup (Yahoo, East Money, Sina)
 *   net/      price download and parsing
 *   task/     AsyncTasks that call search/ and net/
 *   model/    StockData, SearchHit, QuoteResult
 *   analysis/ indicators and return statistics
 *   chart/    chart drawing
 *   ui/       dialogs and screen helpers
 *   export/   save and share
 */
public class MainActivity extends Activity {

    private EditText etSearchName;
    private EditText etTicker;
    private Spinner spFrequency;
    private Button btnSearch;
    private Button btnGetData;

    private ResultPanel resultPanel;
    private DateRangeController dates;
    private final WaitDialog waitDialog = new WaitDialog();

    private final StockData stockData = new StockData();
    private final SymbolSearchService searchService = new SymbolSearchService();
    private final YahooChartClient chartClient = new YahooChartClient();

    /** The running Search / Get Data task, so Stop can cancel it. */
    private AsyncTask<?, ?, ?> currentTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        bindViews();
        setupFrequencySpinner();
        setupDateButtons();
        setupActionButtons();
    }

    // ==================== Setup ====================

    private void bindViews() {
        etSearchName = (EditText) findViewById(R.id.etSearchName);
        etTicker = (EditText) findViewById(R.id.etTicker);
        spFrequency = (Spinner) findViewById(R.id.spFrequency);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnGetData = (Button) findViewById(R.id.btnGetData);

        resultPanel = new ResultPanel(
                (TextView) findViewById(R.id.tvStatus),
                (TextView) findViewById(R.id.tvHeader),
                (TextView) findViewById(R.id.tvHeaderLine),
                (TextView) findViewById(R.id.tvResult));

        dates = new DateRangeController(this,
                (EditText) findViewById(R.id.etStartDate),
                (EditText) findViewById(R.id.etEndDate));

        // Blank share code at startup; start/end = today
        etTicker.setText("");
        dates.resetToToday();
    }

    private void setupFrequencySpinner() {
        ArrayAdapter<CharSequence> freqAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.frequency_options,
                R.layout.spinner_item);
        freqAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spFrequency.setAdapter(freqAdapter);
        spFrequency.setSelection(0);
    }

    private void setupDateButtons() {
        bindClick(R.id.etStartDate, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dates.showPicker(true);
            }
        });
        bindClick(R.id.etEndDate, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dates.showPicker(false);
            }
        });
        bindClick(R.id.btnCurrentDate, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dates.setToday();
            }
        });
        bindClick(R.id.btnPreset1W, new PresetClick(1, 0));
        bindClick(R.id.btnPreset1M, new PresetClick(0, 1));
        bindClick(R.id.btnPreset3M, new PresetClick(0, 3));
        bindClick(R.id.btnPreset1Y, new PresetClick(0, 12));
    }

    private void setupActionButtons() {
        bindClick(R.id.btnSearch, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSymbolSearch();
            }
        });
        bindClick(R.id.btnGetData, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGetData();
            }
        });
        bindClick(R.id.btnClear, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearData();
            }
        });
        bindClick(R.id.btnSave, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCsvFile();
            }
        });
        bindClick(R.id.btnShare, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareQuotes();
            }
        });
        bindClick(R.id.btnAnalyze, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzeData();
            }
        });
        bindClick(R.id.btnGraph, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGraph();
            }
        });
    }

    /** Set a click listener if the view exists in the layout. */
    private void bindClick(int viewId, View.OnClickListener listener) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(listener);
        }
    }

    /** End date = today; start date = today minus N weeks or N months. */
    private class PresetClick implements View.OnClickListener {
        private final int weeks;
        private final int months;

        PresetClick(int weeks, int months) {
            this.weeks = weeks;
            this.months = months;
        }

        @Override
        public void onClick(View v) {
            if (weeks > 0) {
                dates.setPresetWeeks(weeks);
            } else {
                dates.setPresetMonths(months);
            }
        }
    }

    // ==================== Search by name ====================

    private void startSymbolSearch() {
        String q = etSearchName.getText().toString().trim();
        if (q.length() == 0) {
            Toast.makeText(this, "Enter a company name to search", Toast.LENGTH_SHORT).show();
            return;
        }
        resultPanel.setStatus(R.string.status_searching);
        btnSearch.setEnabled(false);
        new SearchSymbolTask(searchService, searchListener).execute(q);
    }

    private final SearchSymbolTask.Listener searchListener = new SearchSymbolTask.Listener() {
        @Override
        public void onSearchStarted(AsyncTask<?, ?, ?> task) {
            currentTask = task;
            showWaitDialog("Searching codes...");
        }

        @Override
        public void onSearchCancelled() {
            currentTask = null;
            waitDialog.dismiss();
            btnSearch.setEnabled(true);
            resultPanel.setStatus("Stopped");
        }

        @Override
        public void onSearchFinished(ArrayList<SearchHit> hits, String errorMessage) {
            currentTask = null;
            waitDialog.dismiss();
            btnSearch.setEnabled(true);

            if (errorMessage != null && (hits == null || hits.size() == 0)) {
                resultPanel.setStatus(errorMessage);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                return;
            }

            if (hits == null || hits.size() == 0) {
                resultPanel.setStatus("No codes found");
                Toast.makeText(MainActivity.this, "No codes found", Toast.LENGTH_SHORT).show();
                return;
            }

            resultPanel.setStatus("Found " + hits.size() + " code(s). Pick one.");
            SearchResultsDialog.show(MainActivity.this, hits,
                    new SearchResultsDialog.OnHitSelectedListener() {
                        @Override
                        public void onHitSelected(SearchHit hit) {
                            etTicker.setText(hit.symbol);
                            resultPanel.setStatus("Selected: " + hit.symbol);
                            Toast.makeText(MainActivity.this,
                                    "Share code set to " + hit.symbol, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    };

    // ==================== Get Data ====================

    private void startGetData() {
        String ticker = etTicker.getText().toString().trim().toUpperCase(Locale.US);

        if (ticker.length() == 0) {
            Toast.makeText(this, "Please enter a ticker symbol", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!dates.readFromFields()) {
            return;
        }

        long period1 = dates.getStartMillis() / 1000L;
        // include the whole end day
        long period2 = dates.getEndMillis() / 1000L + 86399L;

        int pos = spFrequency.getSelectedItemPosition();
        String interval;
        if (pos == 1) {
            interval = "1wk";
        } else if (pos == 2) {
            interval = "1mo";
        } else {
            interval = "1d";
        }

        resultPanel.clearTable();
        resultPanel.setStatus(R.string.status_loading);
        btnGetData.setEnabled(false);

        new FetchQuoteTask(chartClient, fetchListener).execute(
                ticker, String.valueOf(period1), String.valueOf(period2), interval);
    }

    private final FetchQuoteTask.Listener fetchListener = new FetchQuoteTask.Listener() {
        @Override
        public void onFetchStarted(AsyncTask<?, ?, ?> task) {
            currentTask = task;
            showWaitDialog("Downloading stock data...");
        }

        @Override
        public void onFetchCancelled() {
            currentTask = null;
            waitDialog.dismiss();
            btnGetData.setEnabled(true);
            resultPanel.setStatus("Stopped");
        }

        @Override
        public void onFetchFinished(QuoteResult result) {
            currentTask = null;
            waitDialog.dismiss();
            btnGetData.setEnabled(true);

            if (result != null && result.errorMessage != null) {
                resultPanel.setStatus("Failed");
                resultPanel.showMessage(result.errorMessage);
                stockData.clearQuoteText();
                Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
            } else if (result != null && result.table != null) {
                String status = "Success! " + result.rowCount + " rows downloaded";
                resultPanel.setStatus(status);
                resultPanel.showTable(QuoteParser.HEADER_TEXT, QuoteParser.HEADER_LINE,
                        result.table);
                // Include header in shared/saved text for convenience
                stockData.setQuote(
                        QuoteParser.HEADER_TEXT + "\n" + QuoteParser.HEADER_LINE + "\n" + result.table,
                        result.csv, result.ticker);
                Toast.makeText(MainActivity.this, status, Toast.LENGTH_SHORT).show();
            } else {
                resultPanel.setStatus("");
                resultPanel.showMessage("No data returned");
                stockData.clearQuoteText();
            }
        }
    };

    // ==================== Wait dialog / Stop ====================

    private void showWaitDialog(String message) {
        waitDialog.show(this, message, new Runnable() {
            @Override
            public void run() {
                stopCurrentTask();
            }
        });
    }

    /** Cancel running Search / Get Data task and close wait dialog. */
    private void stopCurrentTask() {
        try {
            if (currentTask != null) {
                currentTask.cancel(true);
            }
        } catch (Exception e) {
            // ignore
        }
        currentTask = null;
        waitDialog.dismiss();
        btnSearch.setEnabled(true);
        btnGetData.setEnabled(true);
        resultPanel.setStatus("Stopped");
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
    }

    // ==================== Buttons on the loaded data ====================

    private void clearData() {
        resultPanel.clearAll();
        stockData.clearAll();
        Toast.makeText(this, "Data cleared", Toast.LENGTH_SHORT).show();
    }

    private void saveCsvFile() {
        if (!stockData.hasCsv()) {
            Toast.makeText(this, "No data to save. Get Data first.", Toast.LENGTH_SHORT).show();
            return;
        }
        String path = FileExporter.saveCsv(this, stockData.getCsvContent(), stockData.getTicker());
        if (path != null) {
            resultPanel.setStatus("Saved: " + path);
        }
    }

    private void shareQuotes() {
        if (!stockData.hasDisplayText()) {
            Toast.makeText(this, "No data to share. Get Data first.", Toast.LENGTH_SHORT).show();
            return;
        }
        ShareHelper.shareText(this, stockData.getTicker(), stockData.getDisplayText());
    }

    private void analyzeData() {
        ArrayList<Double> closes = stockData.getCloseList();
        if (closes.size() < 2) {
            // try parse from CSV
            stockData.parseSeriesFromCsv(stockData.getCsvContent());
            closes = stockData.getCloseList();
        }
        if (closes.size() < 2) {
            Toast.makeText(this, "Please Get Data first (need at least 2 rows)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ReturnStats stats = ReturnStats.compute(closes);
        if (stats == null) {
            Toast.makeText(this, "Cannot compute returns", Toast.LENGTH_SHORT).show();
            return;
        }

        String ticker = stockData.getTicker();
        if (ticker == null || ticker.length() == 0) {
            ticker = etTicker.getText().toString().trim();
        }
        AnalyzeDialog.show(this, ticker, stats);
    }

    private void showGraph() {
        new GraphDialog(this, stockData, etTicker.getText().toString().trim()).show();
    }
}
