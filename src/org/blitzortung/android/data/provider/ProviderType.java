package org.blitzortung.android.data.provider;

public enum ProviderType {

		HTTP { public DataProvider getProvider() {return new BlitzortungHttpProvider();}},
		RPC { public DataProvider getProvider() {return new JsonRpcProvider();}};
		
		abstract public DataProvider getProvider();

}