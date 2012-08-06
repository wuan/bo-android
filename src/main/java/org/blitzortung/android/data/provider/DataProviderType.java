package org.blitzortung.android.data.provider;

public enum DataProviderType {

	HTTP {
		public DataProvider getProvider() {
			return new BlitzortungHttpDataProvider();
		}
	},
	RPC {
		public DataProvider getProvider() {
			return new JsonRpcDataProvider();
		}
	};

	abstract public DataProvider getProvider();
}