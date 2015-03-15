package org.sosby.penguin;

import java.math.BigDecimal;
import java.util.ArrayList;

public abstract class BaseMarketDataProvider implements IMarketDataProvider {
    private ArrayList<Events> mListeners = new ArrayList<Events>();

    private BigDecimal mLastPrice = new BigDecimal(-1.0);
    private Side mLastSide = Side.buy;

    @Override
    public final void addListener(Events listener) {
        mListeners.add(listener);
    }

    @Override
    public final void removeListener(Events listener) {
        mListeners.remove(listener);
    }

    @Override
    public final BigDecimal getLastTradePrice() {
        return mLastPrice;
    }

    @Override
    public final Side getLastTradeSide() {
        return mLastSide;
    }

    protected final void onError(String error) {
        for (Events listener : mListeners) {
            listener.onError(error);
        }
    }

    protected final void onTrade(BigDecimal price, Side side) {
        mLastPrice = price;
        mLastSide = side;

        for (Events listener : mListeners) {
            listener.onTrade(price, side);
        }
    }
}
