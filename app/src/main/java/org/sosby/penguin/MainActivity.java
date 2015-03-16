package org.sosby.penguin;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandDeviceInfo;
import com.microsoft.band.BandException;
import com.microsoft.band.tiles.BandIcon;
import com.microsoft.band.tiles.BandTile;
import com.microsoft.band.tiles.BandTileManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.UUID;

public class MainActivity extends ActionBarActivity implements IMarketDataProvider.Events {

    private static final String TAG = MainActivity.class.getCanonicalName();

    private TextView mLastTradePrice;
    private ListView mBands;
    private Bitmap mBitcoinIcon;

    private IMarketDataProvider mProvider;
    private ArrayAdapter<BandDeviceInfo> mBandsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLastTradePrice = (TextView) findViewById(R.id.last_trade_price);
        mBands = (ListView) findViewById(R.id.paired_bands);

        mProvider = new CoinbaseRest(60000);
        mProvider.addListener(this);

        mBandsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBands.setAdapter(mBandsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mProvider.resume();
        updateBands();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mProvider.resume();

        try {
            for (BandDeviceInfo bandInfo : BandClientManager.getInstance().getPairedBands()) {
                BandClient band = BandClientManager.getInstance().create(this, bandInfo);
                if (band.isConnected()) {
                    band.disconnect().await();
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Shit: " + ex);
        }
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

    private void updateBands() {
        BandClientManager bandManager = BandClientManager.getInstance();
        BandDeviceInfo[] paired = bandManager.getPairedBands();
        mBandsAdapter.clear();
        mBandsAdapter.addAll(paired);

        for (BandDeviceInfo bandInfo : paired) {
            BandClient band = bandManager.create(this, bandInfo);

            if (!band.isConnected()) {
                ConnectDeviceTask connectTask = new ConnectDeviceTask();
                connectTask.execute(band);
            } else {
                updateBand(band);
            }
        }
    }

    private void updateBand(BandClient band) {
        try {
            //Remove old tiles
            BandTileManager tiles = band.getTileManager();
            if (tiles.getTiles().await().size() > 0) {
                Log.i(TAG, "Band Tile Installed");
                //TODO: Should update the tile somehow?
                return;
            }

            if (tiles.getRemainingTileCapacity().await() > 0) {
                BandTile tile = new BandTile.Builder(UUID.randomUUID(),
                        getString(R.string.band_tile_name),
                        BandIcon.toBandIcon(getBandIcon()))
                        .build();

                tiles.addTile(this, tile);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
            Toast.makeText(this, "Band Error: " + ex, Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getBandIcon() {
        if (mBitcoinIcon == null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            mBitcoinIcon = BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_bitcoin_46x46,
                    opts);
        }

        return mBitcoinIcon;
    }

    private class ConnectDeviceTask extends AsyncTask<BandClient, Void, Boolean> {

        private BandClient[] mBands;

        @Override
        protected Boolean doInBackground(BandClient... params) {
            boolean success = true;
            mBands = params;

            for (BandClient band : mBands) {
                try {
                    band.connect().await();
                    success &= true;
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                    success = false;
                }
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                for (BandClient band : mBands) {
                    if (band.isConnected()) {
                        updateBand(band);
                    }
                }
            }
        }
    }
}
