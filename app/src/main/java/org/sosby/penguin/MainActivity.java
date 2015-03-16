package org.sosby.penguin;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.text.NumberFormat;


public class MainActivity extends ActionBarActivity implements IMarketDataProvider.Events {

    private static final String TAG = MainActivity.class.getCanonicalName();

    private TextView mLastTradePrice;
    private IMarketDataProvider mProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLastTradePrice = (TextView) findViewById(R.id.last_trade_price);
        mProvider = new CoinbaseRest(60000);
        mProvider.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mProvider.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mProvider.resume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sync) {
            mProvider.sync();
            return true;
        } else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onError(String error) {
        Toast.makeText(this, getString(R.string.generic_error, error), Toast.LENGTH_SHORT).show();
        mLastTradePrice.setText(getString(R.string.unknown_price));
    }

    @Override
    public void onTrade(BigDecimal price, IMarketDataProvider.Side side) {
        mLastTradePrice.setText(NumberFormat.getCurrencyInstance().format(price));
    }
}
