package com.stockquote.net;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Minimal blocking HTTP GET used by every data source. */
public final class HttpUtil {

    public static final String USER_AGENT_CHROME =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static final String USER_AGENT_SHORT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private HttpUtil() {
    }

    /**
     * GET a URL and return the body as one string (line breaks removed).
     * Error responses (non 2xx) return the error body, like the original code.
     *
     * @param headerPairs alternating header name / value, e.g. "Accept", "application/json"
     */
    public static String get(String urlStr, int timeoutMs, String charset,
                             String... headerPairs) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            for (int i = 0; i + 1 < headerPairs.length; i += 2) {
                conn.setRequestProperty(headerPairs[i], headerPairs[i + 1]);
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
