package org.blitzortung.android.data.provider;

import org.blitzortung.android.data.provider.blitzortung.BlitzortungHttpDataProvider;
import org.blitzortung.android.data.provider.standard.JsonRpcDataProvider;

public class DataProviderFactory {
    public DataProvider getDataProviderForType(DataProviderType providerType) {
        switch (providerType) {
            case RPC:
                return new JsonRpcDataProvider();

            case HTTP:
                return new BlitzortungHttpDataProvider();

            default:
                throw new IllegalStateException(String.format("unhandled data provider type '%s'", providerType));
        }
    }
}
