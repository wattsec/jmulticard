module es.gob.jmulticard.jse {
	exports es.gob.jmulticard.jse;
	exports es.gob.jmulticard.jse.provider.ceres;
	exports es.gob.jmulticard.jse.provider.rsacipher;
	exports es.gob.jmulticard.jse.provider;
	exports es.gob.jmulticard.jse.provider.gide;

	requires java.desktop;
	requires java.logging;
	requires java.smartcardio;
	requires es.gob.jmulticard;
	requires core;
	requires bcpkix.jdk15on;
	requires prov;
}