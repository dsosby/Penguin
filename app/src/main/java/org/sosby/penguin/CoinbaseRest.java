package org.sosby.penguin;

import android.net.Uri;
import android.os.AsyncTask;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

public class CoinbaseRest extends BaseMarketDataProvider {

    private static URI uri;
    static {
        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority("api.exchange.coinbase.com")
                    .appendPath("products")
                    .appendPath("btc-usd")
                    .appendPath("trades")
                    .appendQueryParameter("limit", "2");
            uri = new URI(builder.build().toString());
        } catch (URISyntaxException ex) {
        }
    }

    private int mPollPeriodMs;

    public CoinbaseRest(int pollPeriodMs) {
        mPollPeriodMs = pollPeriodMs;
    }

    @Override
    public void pause() {
        stopSyncTimer();
    }

    @Override
    public void resume() {
        startSyncTimer();
        sync();
    }

    @Override
    public void sync() {
        SyncTask syncTask = new SyncTask();
        syncTask.execute();
    }

    private void stopSyncTimer() {
    }

    private void startSyncTimer() {
    }

    private class SyncResult {
        public BigDecimal Price;
        public Side Side;
        public String Error;
    }

    private class SyncTask extends AsyncTask<Void, Void, SyncResult> {

        @Override
        protected SyncResult doInBackground(Void... params) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet(uri);
            ResponseHandler<String> handler = new BasicResponseHandler();
            SyncResult result = new SyncResult();

            try {
                String httpResult = httpClient.execute(request, handler);
                JSONArray trades = new JSONArray(httpResult);
                if (trades.length() > 0) {
                    JSONObject lastTrade = trades.getJSONObject(0);
                    result.Price = new BigDecimal(lastTrade.getDouble("price"));
                    result.Side = Side.valueOf(lastTrade.getString("side"));
                }
            } catch (Exception ex) {
                result.Error = ex.toString();
            } finally {
                httpClient.getConnectionManager().shutdown();
            }

            return result;
        }

        @Override
        protected void onPostExecute(SyncResult syncResult) {
            if (syncResult.Error == null)
                onTrade(syncResult.Price, syncResult.Side);
            else
                onError(syncResult.Error);
        }

        @Override
        protected void onCancelled() {
            onError("Cancelled");
        }
    }
}
