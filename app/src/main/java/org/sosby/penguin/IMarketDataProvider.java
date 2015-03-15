package org.sosby.penguin;

import java.math.BigDecimal;

public interface IMarketDataProvider {
    void pause();
    void resume();
    void sync();

    void addListener(Events listener);
    void removeListener(Events listener);

    BigDecimal getLastTradePrice();
    Side getLastTradeSide();

    interface Events {
        void onError(String error);
        void onTrade(BigDecimal price, Side side);
    }

    enum Side {
        buy,
        sell
    }
}
