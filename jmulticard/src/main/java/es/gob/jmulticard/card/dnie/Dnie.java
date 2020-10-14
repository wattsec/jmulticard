/*
 * Controlador Java de la Secretaria de Estado de Administraciones Publicas
 * para el DNI electronico.
 *
 * El Controlador Java para el DNI electronico es un proveedor de seguridad de JCA/JCE
 * que permite el acceso y uso del DNI electronico en aplicaciones Java de terceros
 * para la realizacion de procesos de autenticacion, firma electronica y validacion
 * de firma. Para ello, se implementan las funcionalidades KeyStore y Signature para
 * el acceso a los certificados y claves del DNI electronico, asi como la realizacion
 * de operaciones criptograficas de firma con el DNI electronico. El Controlador ha
 * sido disenado para su funcionamiento independiente del sistema operativo final.
 *
 * Copyright (C) 2012 Direccion General de Modernizacion Administrativa, Procedimientos
 * e Impulso de la Administracion Electronica
 *
 * Este programa es software libre y utiliza un licenciamiento dual (LGPL 2.1+
 * o EUPL 1.1+), lo cual significa que los usuarios podran elegir bajo cual de las
 * licencias desean utilizar el codigo fuente. Su eleccion debera reflejarse
 * en las aplicaciones que integren o distribuyan el Controlador, ya que determinara
 * su compatibilidad con otros componentes.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los terminos de la
 * Lesser GNU General Public License publicada por la Free Software Foundation,
 * tanto en la version 2.1 de la Licencia, o en una version posterior.
 *
 * El Controlador puede ser redistribuido y/o modificado bajo los terminos de la
 * European Union Public License publicada por la Comision Europea,
 * tanto en la version 1.1 de la Licencia, o en una version posterior.
 *
 * Deberia recibir una copia de la GNU Lesser General Public License, si aplica, junto
 * con este programa. Si no, consultelo en <http://www.gnu.org/licenses/>.
 *
 * Deberia recibir una copia de la European Union Public License, si aplica, junto
 * con este programa. Si no, consultelo en <http://joinup.ec.europa.eu/software/page/eupl>.
 *
 * Este programa es distribuido con la esperanza de que sea util, pero
 * SIN NINGUNA GARANTIA; incluso sin la garantia implicita de comercializacion
 * o idoneidad para un proposito particular.
 */
package es.gob.jmulticard.card.dnie;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import es.gob.jmulticard.CancelledOperationException;
import es.gob.jmulticard.CryptoHelper;
import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.apdu.CommandApdu;
import es.gob.jmulticard.apdu.ResponseApdu;
import es.gob.jmulticard.apdu.connection.ApduConnection;
import es.gob.jmulticard.apdu.connection.ApduConnectionException;
import es.gob.jmulticard.apdu.connection.LostChannelException;
import es.gob.jmulticard.apdu.connection.cwa14890.Cwa14890Connection;
import es.gob.jmulticard.apdu.connection.cwa14890.Cwa14890OneV1Connection;
import es.gob.jmulticard.apdu.connection.cwa14890.SecureChannelException;
import es.gob.jmulticard.apdu.dnie.ChangePINApduCommand;
import es.gob.jmulticard.apdu.dnie.GetChipInfoApduCommand;
import es.gob.jmulticard.apdu.dnie.RetriesLeftApduCommand;
import es.gob.jmulticard.apdu.dnie.VerifyApduCommand;
import es.gob.jmulticard.apdu.iso7816eight.PsoSignHashApduCommand;
import es.gob.jmulticard.apdu.iso7816four.ExternalAuthenticateApduCommand;
import es.gob.jmulticard.apdu.iso7816four.InternalAuthenticateApduCommand;
import es.gob.jmulticard.apdu.iso7816four.MseSetAuthenticationKeyApduCommand;
import es.gob.jmulticard.apdu.iso7816four.MseSetComputationApduCommand;
import es.gob.jmulticard.asn1.der.pkcs1.DigestInfo;
import es.gob.jmulticard.asn1.der.pkcs15.Cdf;
import es.gob.jmulticard.asn1.der.pkcs15.PrKdf;
import es.gob.jmulticard.callback.CustomAuthorizeCallback;
import es.gob.jmulticard.card.AuthenticationModeLockedException;
import es.gob.jmulticard.card.BadPinException;
import es.gob.jmulticard.card.CardMessages;
import es.gob.jmulticard.card.CompressionUtils;
import es.gob.jmulticard.card.CryptoCardException;
import es.gob.jmulticard.card.Location;
import es.gob.jmulticard.card.PasswordCallbackNotFoundException;
import es.gob.jmulticard.card.PinException;
import es.gob.jmulticard.card.PrivateKeyReference;
import es.gob.jmulticard.card.cwa14890.Cwa14890Card;
import es.gob.jmulticard.card.cwa14890.Cwa14890PrivateConstants;
import es.gob.jmulticard.card.cwa14890.Cwa14890PublicConstants;
import es.gob.jmulticard.card.iso7816eight.Iso7816EightCard;
import es.gob.jmulticard.card.iso7816four.FileNotFoundException;
import es.gob.jmulticard.card.iso7816four.Iso7816FourCardException;
import es.gob.jmulticard.card.iso7816four.Iso7816fourErrorCodes;
import es.gob.jmulticard.card.pace.PaceConnection;

/** DNI Electr&oacute;nico.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public class Dnie extends Iso7816EightCard implements Dni, Cwa14890Card {

	private static final int DEFAULT_KEY_SIZE = 2048;

	@SuppressWarnings("static-method")
	protected Cwa14890PublicConstants getCwa14890PublicConstants() {
		return new DnieCwa14890Constants();
	}

	@SuppressWarnings("static-method")
	protected Cwa14890PrivateConstants getCwa14890PrivateConstants() {
		return new DnieCwa14890Constants();
	}

	protected static final Logger LOGGER = Logger.getLogger("es.gob.jmulticard"); //$NON-NLS-1$

    /** Octeto que identifica una verificaci&oacute;n fallida del PIN. */
    private final static byte ERROR_PIN_SW1 = (byte) 0x63;

    private CallbackHandler callbackHandler;

	private String[] aliases = null;

    private static final boolean PIN_AUTO_RETRY;
    static {
    	PIN_AUTO_RETRY = true;
    }

    /** Identificador del fichero del certificado de componente del DNIe. */
    private static final byte[] CERT_ICC_FILE_ID = new byte[] {
        (byte) 0x60, (byte) 0x1F
    };

    /** Nombre del <i>Master File</i> del DNIe. */
    private static final String MASTER_FILE_NAME = "Master.File"; //$NON-NLS-1$

	/** Alias del certificado de autenticaci&oacute;n del DNIe. */
    public static final String CERT_ALIAS_AUTH = "CertAutenticacion"; //$NON-NLS-1$

    /** Alias del certificado de firma del DNIe. */
    public static final String CERT_ALIAS_SIGN = "CertFirmaDigital"; //$NON-NLS-1$

    protected static final String CERT_ALIAS_SIGNALIAS = "CertFirmaSeudonimo"; //$NON-NLS-1$
    protected static final String CERT_ALIAS_CYPHER = "CertCifrado"; //$NON-NLS-1$
    protected static final String CERT_ALIAS_INTERMEDIATE_CA = "CertCAIntermediaDGP"; //$NON-NLS-1$

    private static final String AUTH_KEY_LABEL = "KprivAutenticacion"; //$NON-NLS-1$
    private static final String SIGN_KEY_LABEL = "KprivFirmaDigital"; //$NON-NLS-1$
    private static final String CYPH_KEY_LABEL = "KprivCifrado"; //$NON-NLS-1$

    protected static final Location CDF_LOCATION = new Location("50156004"); //$NON-NLS-1$
    protected static final Location PRKDF_LOCATION = new Location("50156001"); //$NON-NLS-1$
	protected static final Location IDESP_LOCATION = new Location("3F000006"); //$NON-NLS-1$

    protected X509Certificate authCert;
    protected X509Certificate signCert;
    protected X509Certificate cyphCert;
    protected X509Certificate signAliasCert;
    protected X509Certificate intermediateCaCert;

    private Location authCertPath;
    private Location signCertPath;

    /** Localizaci&oacute;n del certificado de cifrado.
     * Es opcional, ya que solo est&aacute; presente en las TIF, no en los DNIe normales. */
    private Location cyphCertPath = null;

    /** Localizaci&oacute;n del certificado de firma con seud&oacute;nimo.
     * Es opcional, ya que solo est&aacute; presente en las TIF, no en los DNIe normales. */
    private Location signAliasCertPath = null;

    private DniePrivateKeyReference authKeyRef;
    private DniePrivateKeyReference signKeyRef;

    /** Referencia a la clave privada de cifrado.
     * Es opcional, ya que solo est&aacute; presente en las TIF, no en los DNIe normales. */
    private DniePrivateKeyReference cyphKeyRef = null;

    /** Referencia a la clave privada de firma con seud&oacute;nimo.
     * Es opcional, ya que solo est&aacute; presente en las TIF, no en los DNIe normales. */
    private DniePrivateKeyReference signAliasKeyRef = null;

    /** Conexi&oacute;n inicial con la tarjeta, sin ning&uacute;n canal seguro. */
    protected ApduConnection rawConnection;

    /** Manejador de funciones criptogr&aacute;ficas. */
    protected final CryptoHelper cryptoHelper;

    protected CryptoHelper getCryptoHelper() {
    	return this.cryptoHelper;
    }

    private PasswordCallback passwordCallback;

    protected PasswordCallback getPasswordCallback() {
    	return this.passwordCallback;
    }

	@Override
	public String toString() {
		try {
			final Cdf cdf = getCdf();
			return getCardName() + "\n" + new DnieSubjectPrincipalParser(cdf.getCertificateSubjectPrincipal(0)).toString(); //$NON-NLS-1$
		}
		catch (final ApduConnectionException e) {
			LOGGER.warning("No se ha podido leer el CDF del DNIe: " + e); //$NON-NLS-1$
		}
		return getCardName();
	}

    /** Conecta con el lector del sistema que tenga un DNIe insertado.
     * @param conn Conexi&oacute;n hacia el DNIe.
     * @throws ApduConnectionException Si hay problemas de conexi&oacute;n con la tarjeta. */
    public static void connect(final ApduConnection conn) throws ApduConnectionException {
    	if (!conn.isOpen()) {
    		conn.open();
    	}
    }

    /** Construye una clase que representa un DNIe.
     * @param conn Conexi&oacute;n con la tarjeta.
     * @param pwc <i>PasswordCallback</i> para obtener el PIN del DNIe.
     * @param cryptoHelper Funcionalidades criptogr&aacute;ficas de utilidad que
     *                     pueden variar entre m&aacute;quinas virtuales.
     * @param ch Gestor de <i>callbacks</i> para la solicitud de datos al usuario.
     * @throws ApduConnectionException Si la conexi&oacute;n con la tarjeta se proporciona
     *                                 cerrada y no es posible abrirla.*/
    Dnie(final ApduConnection conn,
    	 final PasswordCallback pwc,
    	 final CryptoHelper cryptoHelper,
    	 final CallbackHandler ch) throws ApduConnectionException {
    	this(conn, pwc, cryptoHelper, ch, true);
    }

    /** Construye una clase que representa un DNIe.
     * @param conn Conexi&oacute;n con la tarjeta.
     * @param pwc <i>PasswordCallback</i> para obtener el PIN del DNIe.
     * @param cryptoHelper Funcionalidades criptogr&aacute;ficas de utilidad que
     *                     pueden variar entre m&aacute;quinas virtuales.
     * @param ch Gestor de <i>callbacks</i> para la solicitud de datos al usuario.
     * @param loadCertsAndKeys Si se indica <code>true</code>, se cargan las referencias a
     *                         las claves privadas y a los certificados, mientras que si se
     *                         indica <code>false</code>, no se cargan, permitiendo la
     *                         instanciaci&oacute;n de un DNIe sin capacidades de firma o
     *                         autenticaci&oacute;n con certificados.
     * @throws ApduConnectionException Si la conexi&oacute;n con la tarjeta se proporciona
     *                                 cerrada y no es posible abrirla.*/
    protected Dnie(final ApduConnection conn,
    	           final PasswordCallback pwc,
    	           final CryptoHelper cryptoHelper,
    	           final CallbackHandler ch,
    	           final boolean loadCertsAndKeys) throws ApduConnectionException {
        super((byte) 0x00, conn);
        conn.reset();
        connect(conn);

        this.rawConnection = conn;
        this.callbackHandler = ch;

        try {
			selectMasterFile();
		}
        catch (final Iso7816FourCardException e) {
			LOGGER.warning(
				"No se ha podido seleccionar el directorio raiz antes de leer las estructuras: " + e //$NON-NLS-1$
			);
		}

        this.passwordCallback = pwc;

        if (cryptoHelper == null) {
            throw new IllegalArgumentException("El CryptoHelper no puede ser nulo"); //$NON-NLS-1$
        }
        this.cryptoHelper = cryptoHelper;

        if (loadCertsAndKeys) {

	        // Cargamos la localizacion de los certificados y el certificado
	        // de CA intermedia de los certificados de firma, autenticacion y, si existe, cifrado
	        loadCertificatesPaths();

	        // Cargamos la informacion publica con la referencia a las claves
	        loadKeyReferences();
        }

    }

    /** Carga la informaci&oacute;n p&uacute;blica con la referencia a las claves de firma. */
    protected void loadKeyReferences() {
        final PrKdf prKdf = new PrKdf();
        try {
            prKdf.setDerValue(
        		selectFileByLocationAndRead(PRKDF_LOCATION)
    		);
        }
        catch (final Exception e) {
            throw new IllegalStateException(
        		"No se ha podido cargar el PrKDF de la tarjeta: " + e.toString() //$NON-NLS-1$
    		);
        }

        for (int i = 0; i < prKdf.getKeyCount(); i++) {
            if (AUTH_KEY_LABEL.equals(prKdf.getKeyName(i))) {
                this.authKeyRef = new DniePrivateKeyReference(
            		this,
            		prKdf.getKeyIdentifier(i),
            		new Location(prKdf.getKeyPath(i)),
            		AUTH_KEY_LABEL,
            		prKdf.getKeyReference(i),
            		DEFAULT_KEY_SIZE
        		);
            }
            else if (SIGN_KEY_LABEL.equals(prKdf.getKeyName(i))) {
                this.signKeyRef = new DniePrivateKeyReference(
            		this,
            		prKdf.getKeyIdentifier(i),
            		new Location(prKdf.getKeyPath(i)),
            		SIGN_KEY_LABEL,
            		prKdf.getKeyReference(i),
            		DEFAULT_KEY_SIZE
        		);
            }
            else if (CYPH_KEY_LABEL.equals(prKdf.getKeyName(i))) {
                this.cyphKeyRef = new DniePrivateKeyReference(
            		this,
            		prKdf.getKeyIdentifier(i),
            		new Location(prKdf.getKeyPath(i)),
            		CYPH_KEY_LABEL,
            		prKdf.getKeyReference(i),
            		DEFAULT_KEY_SIZE
        		);
            }
            else {
            	// Certificado de firma con seudonimo
            	this.signAliasKeyRef = new DniePrivateKeyReference(
        			this,
        			prKdf.getKeyIdentifier(i),
        			new Location(prKdf.getKeyPath(i)),
        			prKdf.getKeyName(i),
        			prKdf.getKeyReference(i),
        			DEFAULT_KEY_SIZE
    			);
            }
        }
    }

    /** Recupera el n&uacute;mero de serie de un DNIe.
     * @return Un array de octetos que contiene el n&uacute;mero de serie del DNIe.
     * @throws ApduConnectionException Si la conexi&oacute;n con la tarjeta se proporciona
     *                                 cerrada y no es posible abrirla. */
    @Override
    public byte[] getSerialNumber() throws ApduConnectionException {
        final ResponseApdu response = getConnection().transmit(new GetChipInfoApduCommand());
        if (response.isOk()) {
        	return response.getData();
        }
        throw new ApduConnectionException(
    		"Respuesta invalida en la obtencion del numero de serie con el codigo: " + response.getStatusWord() //$NON-NLS-1$
		);
    }

    /** {@inheritDoc} */
	@Override
    public String getCardName() {
        return "DNIe"; //$NON-NLS-1$
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAliases() {
    	if (this.aliases == null) {
	    	final List<String> aliasesList = new ArrayList<>();
	    	aliasesList.add(CERT_ALIAS_AUTH);
	    	aliasesList.add(CERT_ALIAS_SIGN);
	    	if (this.cyphCertPath != null) {
	    		aliasesList.add(CERT_ALIAS_CYPHER);
	    	}
	    	if (this.signAliasCertPath != null) {
	    		aliasesList.add(CERT_ALIAS_SIGNALIAS);
	    	}
	    	this.aliases = aliasesList.toArray(new String[0]);
    	}
    	return this.aliases;
    }

    /** Obtiene el CDF PKCS#15 del DNIe.
     * @return CDF PKCS#15 del DNIe.
     * @throws ApduConnectionException Si no se puede conectar con el DNIe. */
    public Cdf getCdf() throws ApduConnectionException {
        final Cdf cdf = new Cdf();
        try {
        	selectMasterFile();
        	final byte[] cdfBytes = selectFileByLocationAndRead(CDF_LOCATION);
            cdf.setDerValue(cdfBytes);
        }
        catch (final Exception e) {
            throw new ApduConnectionException (
        		"No se ha podido cargar el CDF de la tarjeta: " + e.toString(), e //$NON-NLS-1$
    		);
        }
        return cdf;
    }

    /** Carga el certificado de la CA intermedia y las localizaciones de los
     * certificados de firma y autenticaci&oacute;n.
     * @throws ApduConnectionException Si hay problemas en la precarga. */
    protected void loadCertificatesPaths() throws ApduConnectionException {

        final Cdf cdf = getCdf();

        for (int i = 0; i < cdf.getCertificateCount(); i++) {
        	final String currentAlias = cdf.getCertificateAlias(i);
            if (CERT_ALIAS_AUTH.equals(currentAlias)) {
                this.authCertPath = new Location(cdf.getCertificatePath(i));
            }
            else if (CERT_ALIAS_SIGN.equals(currentAlias)) {
                this.signCertPath = new Location(cdf.getCertificatePath(i));
            }
            else if (CERT_ALIAS_CYPHER.equals(currentAlias)) {
            	this.cyphCertPath = new Location(cdf.getCertificatePath(i));
            }
            else if (CERT_ALIAS_INTERMEDIATE_CA.equals(currentAlias)) {
            	try {
            		final byte[] intermediateCaCertEncoded = selectFileByLocationAndRead(
						new Location(
							cdf.getCertificatePath(i)
						)
    				);
            		this.intermediateCaCert = CompressionUtils.getCertificateFromCompressedOrNotData(
        				intermediateCaCertEncoded
    				);
            	}
            	catch (final Exception e) {
            		LOGGER.warning(
        				"No se ha podido cargar el certificado de la autoridad intermedia del CNP: " + e //$NON-NLS-1$
    				);
            		this.intermediateCaCert = null;
            	}
            }
            else if (CERT_ALIAS_SIGNALIAS.equals(currentAlias)){
            	this.signAliasCertPath = new Location(cdf.getCertificatePath(i));
            }
            else {
            	LOGGER.warning(
        			"Se ha encontrado un certificado desconocido en la tarjeta con alias: " + currentAlias //$NON-NLS-1$
    			);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public X509Certificate getCertificate(final String alias) throws CryptoCardException, PinException {

        if (this.authCert == null) { // Este certificado esta presente en todas las variantes del DNIe

        	if (this.authCertPath == null) {
        		try {
					loadCertificatesPaths();
				}
        		catch (final ApduConnectionException e) {
					throw new CryptoCardException(
						"Error cargando las rutas hacia los certificados: " + e, e //$NON-NLS-1$
					);
				}
        	}
        	// Abrimos el canal si es necesario
        	openSecureChannelIfNotAlreadyOpened();
            // Cargamos certificados si es necesario
        	loadCertificates();
        }

        if (CERT_ALIAS_AUTH.equals(alias)) {
            return this.authCert;
        }
        if (CERT_ALIAS_SIGN.equals(alias)) {
            return this.signCert;
        }
        if (CERT_ALIAS_INTERMEDIATE_CA.equals(alias)) {
            return this.intermediateCaCert;
        }
        if (CERT_ALIAS_CYPHER.equals(alias)) {
        	return this.cyphCert;
        }
        if (CERT_ALIAS_SIGNALIAS.equals(alias)) {
        	return this.signAliasCert;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void verifyCaIntermediateIcc() {
        // No se comprueba
    }

    /** {@inheritDoc} */
    @Override
    public void verifyIcc() {
        // No se comprueba
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getIccCertEncoded() throws IOException {
        byte[] iccCertEncoded;
        try {
        	selectMasterFile();
            iccCertEncoded = selectFileByIdAndRead(CERT_ICC_FILE_ID);
        }
        catch (final ApduConnectionException e) {
            throw new IOException(
        		"Error en el envio de APDU para la seleccion del certificado de componente de la tarjeta: " + e, e //$NON-NLS-1$
    		);
        }
        catch (final Iso7816FourCardException e) {
            throw new IOException("Error en la seleccion del certificado de componente de la tarjeta: " + e, e); //$NON-NLS-1$
        }
        return iccCertEncoded;
    }

    /** {@inheritDoc} */
    @Override
    public void verifyIfdCertificateChain(final Cwa14890PublicConstants consts) throws ApduConnectionException {

        // Seleccionamos en la tarjeta la clave publica de la CA raiz del controlador
    	// (clave publica de la autoridad certificadora raiz de la jerarquia de certificados
    	// verificable por la tarjeta).
        try {
            setPublicKeyToVerification(consts.getRefCCvCaPublicKey());
        }
        catch (final SecureChannelException e) {
            throw new SecureChannelException(
        		"Error al seleccionar para verificacion la " +//$NON-NLS-1$
                     "clave publica de la CA raiz de los certificados verificables por la tarjeta", e //$NON-NLS-1$
    		);
        }

        // Verificamos la CA intermedia del controlador. La clave publica queda almacenada en memoria
        try {
            verifyCertificate(consts.getCCvCa());
        }
        catch (final SecureChannelException e) {
            throw new SecureChannelException(
        		"Error en la verificacion del certificado de la CA intermedia de Terminal: " + e, e //$NON-NLS-1$
    		);
        }

        // Seleccionamos a traves de su CHR la clave publica del certificado recien cargado en memoria
        // (CA intermedia de Terminal) para su verificacion
        try {
            setPublicKeyToVerification(consts.getChrCCvCa());
        }
        catch (final SecureChannelException e) {
            throw new SecureChannelException(
        		"Error al establecer la clave publica del certificado de CA intermedia de Terminal para su verificacion en tarjeta: " + e, e //$NON-NLS-1$
    		);
        }

        // Enviamos el certificado de Terminal (C_CV_IFD) para su verificacion por la tarjeta
        try {
            verifyCertificate(consts.getCCvIfd());
        }
        catch (final SecureChannelException e) {
            throw new SecureChannelException(
        		"Error en la verificacion del certificado de Terminal: " + e, e //$NON-NLS-1$
    		);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getRefIccPrivateKey(final Cwa14890PublicConstants consts) {
        return consts.getRefIccPrivateKey();
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getChrCCvIfd(final Cwa14890PublicConstants consts) {
        return consts.getChrCCvIfd();
    }

    /** {@inheritDoc} */
    @Override
    public RSAPrivateKey getIfdPrivateKey(final Cwa14890PrivateConstants consts) {
        return consts.getIfdPrivateKey();
    }

    /** {@inheritDoc} */
    @Override
    public void setKeysToAuthentication(final byte[] refPublicKey,
    		                            final byte[] refPrivateKey) throws ApduConnectionException {
        final CommandApdu apdu = new MseSetAuthenticationKeyApduCommand((byte) 0x00, refPublicKey, refPrivateKey);
        final ResponseApdu res = getConnection().transmit(apdu);
        if (!res.isOk()) {
            throw new SecureChannelException(
        		"Error durante el establecimiento de las claves publica y privada " + //$NON-NLS-1$
                     "para atenticacion (error: " + HexUtils.hexify(res.getBytes(), true) + ")" //$NON-NLS-1$ //$NON-NLS-2$
            );
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getInternalAuthenticateMessage(final byte[] randomIfd, final byte[] chrCCvIfd) throws ApduConnectionException {
        final CommandApdu apdu = new InternalAuthenticateApduCommand((byte) 0x00, randomIfd, chrCCvIfd);
        final ResponseApdu res = getConnection().transmit(apdu);
        if (res.isOk()) {
        	return res.getData();
        }
        throw new ApduConnectionException(
    		"Respuesta invalida en la obtencion del mensaje de autenticacion interna con el codigo: " + res.getStatusWord() //$NON-NLS-1$
		);
    }

    /** {@inheritDoc} */
    @Override
    public boolean externalAuthentication(final byte[] extAuthenticationData) throws ApduConnectionException {
        final CommandApdu apdu = new ExternalAuthenticateApduCommand((byte) 0x00, extAuthenticationData);
        return getConnection().transmit(apdu).isOk();
    }

    /** {@inheritDoc} */
    @Override
    public PrivateKeyReference getPrivateKey(final String alias) {
    	if (this.authKeyRef == null) {
    		loadKeyReferences();
    	}
        if (CERT_ALIAS_AUTH.equals(alias)) {
            return this.authKeyRef;
        }
        else if (CERT_ALIAS_SIGN.equals(alias)) {
            return this.signKeyRef;
        }
        else if (CERT_ALIAS_CYPHER.equals(alias)) {
        	return this.cyphKeyRef;
        }
        else if (CERT_ALIAS_SIGNALIAS.equals(alias)){
        	return this.signAliasKeyRef;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] sign(final byte[] data,
    		           final String signAlgorithm,
    		           final PrivateKeyReference privateKeyReference) throws CryptoCardException,
    		                                                                 PinException {
    	final byte[] signBytes = signInternal(
			data,
			signAlgorithm,
			privateKeyReference
		);
    	try {
			getConnection().close();
		}
    	catch (final ApduConnectionException e) {
			LOGGER.severe(
				"No se ha podido cerrar el canal despues de una firma, es posible que fallen operaciones posteriores de firma: " + e //$NON-NLS-1$
			);
		}
    	return signBytes;
    }

    protected byte[] signInternal(final byte[] data,
    		                      final String signAlgorithm,
    		                      final PrivateKeyReference privateKeyReference) throws CryptoCardException,
    		                                                                            PinException {
        if (!(privateKeyReference instanceof DniePrivateKeyReference)) {
            throw new IllegalArgumentException(
        		"La referencia a la clave privada tiene que ser de tipo DniePrivateKeyReference" //$NON-NLS-1$
    		);
        }

        // Si la tarjeta requiere autenticacion, la solicitamos
        if (needAuthorizationToSign()) {
        	if (this.callbackHandler != null) {
        		// Callback para autorizar la firma
        		final Callback cc = new CustomAuthorizeCallback();
        		try {
        			this.callbackHandler.handle(
    					new Callback[] {
							cc
    					}
					);
        		}
        		catch (final UnsupportedCallbackException e) {
        			// Si no se especifica un callback de autorizacion, se omite
        			LOGGER.severe(
    					"No se ha proporcionado un CallbackHandler valido para mostrar el dialogo de confirmacion de firma, se omitira: " + e //$NON-NLS-1$
					);
        		}
        		catch (final Exception e) {
        			throw new AccessControlException(
    					"No ha sido posible pedir la confirmacion de firma al usuario: " + e //$NON-NLS-1$
					);
        		}

        		if (!((CustomAuthorizeCallback)cc).isAuthorized()) {
        			throw new CancelledOperationException(
    					"El usuario ha denegado la operacion de firma" //$NON-NLS-1$
					);
        		}
        	}
        	else {
        		LOGGER.warning(
    				"No se ha proporcionado un CallbackHandler para mostrar el dialogo de confirmacion de firma. Se omitira." //$NON-NLS-1$
				);
        	}
        }

        return signOperation(data, signAlgorithm, privateKeyReference);
    }

    /** Indica si la tarjeta requiere autorizaci&oacute;n del usuario para ejecutar una
     * operaci&oacute;n de firma.
     * @return <code>true</code> si la tarjeta requiere autorizaci&oacute;n del usuario para ejecutar una
     *         operaci&oacute;n de firma, <code>false</code> en caso contrario. */
	@SuppressWarnings("static-method")
	protected boolean needAuthorizationToSign() {
    	return true;
    }

    /** Realiza la operaci&oacute;n de firma.
     * @param data Datos que se desean firmar.
     * @param signAlgorithm Algoritmo de firma (por ejemplo, <code>SHA512withRSA</code>, <code>SHA1withRSA</code>, etc.).
     * @param privateKeyReference Referencia a la clave privada para la firma.
     * @return Firma de los datos.
     * @throws CryptoCardException Cuando se produce un error durante la operaci&oacute;n de firma.
     * @throws PinException Si el PIN proporcionado en la <i>PasswordCallback</i>
     *                      es incorrecto y no estaba habilitado el reintento autom&aacute;tico.
     * @throws es.gob.jmulticard.card.AuthenticationModeLockedException Cuando el DNIe est&aacute; bloqueado. */
    protected byte[] signOperation(final byte[] data,
    		                       final String signAlgorithm,
    		                       final PrivateKeyReference privateKeyReference) throws CryptoCardException,
    		                                                                             PinException {
        openSecureChannelIfNotAlreadyOpened();

        ResponseApdu res;
        try {
            CommandApdu apdu = new MseSetComputationApduCommand(
        		(byte) 0x00, ((DniePrivateKeyReference) privateKeyReference).getKeyPath().getLastFilePath(),
        		null
    		);

            res = getConnection().transmit(apdu);
            if (!res.isOk()) {
                throw new DnieCardException(
            		"Error en el establecimiento de las clave de firma con respuesta: " + //$NON-NLS-1$
        				Iso7816fourErrorCodes.getErrorDescription(res.getStatusWord()),
    				res.getStatusWord()
        		);
            }

            final byte[] digestInfo;
            try {
                digestInfo = DigestInfo.encode(signAlgorithm, data, this.cryptoHelper);
            }
            catch (final IOException e) {
                throw new DnieCardException("Error en el calculo de la huella para firmar: " + e, e); //$NON-NLS-1$
            }

            apdu = new PsoSignHashApduCommand((byte) 0x00, digestInfo);
            res = getConnection().transmit(apdu);
            if (!res.isOk()) {
            	LOGGER.severe(
            		"Recibida APDU inesperada de respuesta al PSOSignHash:\n" + HexUtils.hexify(res.getBytes(), true) //$NON-NLS-1$
        		);
                throw new DnieCardException(
                	"Error durante la operacion de firma con respuesta: " + //$NON-NLS-1$
            			Iso7816fourErrorCodes.getErrorDescription(res.getStatusWord()),
                	res.getStatusWord()
                );
            }
        }
        catch(final LostChannelException e) {
            try {
                getConnection().close();
                if (getConnection() instanceof Cwa14890Connection) {
                    setConnection(((Cwa14890Connection) getConnection()).getSubConnection());
                }
            }
            catch (final Exception ex) {
                throw new DnieCardException("No se pudo recuperar el canal seguro para firmar (" + e + "): " + ex, ex); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return signOperation(data, signAlgorithm, privateKeyReference);
        }
        catch (final ApduConnectionException e) {
            throw new DnieCardException("Error en la transmision de comandos a la tarjeta: " + e, e); //$NON-NLS-1$
        }

        return res.getData();
    }

    /** Establece y abre el canal seguro CWA-14890 si no lo estaba ya.
     * @throws CryptoCardException Si hay problemas en el proceso.
     * @throws PinException Si el PIN usado para la apertura de canal no es v&aacute;lido o no se ha proporcionado
     * 						un PIN para validar. */
    public void openSecureChannelIfNotAlreadyOpened() throws CryptoCardException, PinException {
    	openSecureChannelIfNotAlreadyOpened(true);
    }

    /** Establece y abre el canal seguro CWA-14890 vsi no lo estaba ya.
     * @param doChv <code>true</code> si la apertura de canal seguro debe incluir la verificaci&oacute;n de PIN,
     *              <code>false</code> si debe abrirse canal seguro <b>sin verificar PIN</b>.
     * @throws CryptoCardException Si hay problemas en el proceso.
     * @throws PinException Si el PIN usado para la apertura de canal no es v&aacute;lido o no se ha proporcionado
     * 						un PIN para validar (en el caso de que se opte por verificar el PIN). */
    public void openSecureChannelIfNotAlreadyOpened(final boolean doChv) throws CryptoCardException, PinException {
        // Abrimos el canal seguro si no lo esta ya
        if (!isSecurityChannelOpen()) {
        	// Aunque el canal seguro estuviese cerrado, podria si estar enganchado
            if (!(getConnection() instanceof Cwa14890Connection)) {
            	final ApduConnection secureConnection;
        		secureConnection = new Cwa14890OneV1Connection(
            		this,
            		getConnection(),
            		this.cryptoHelper,
            		getCwa14890PublicConstants(),
            		getCwa14890PrivateConstants()
        		);
                try {
                    setConnection(secureConnection);
                }
                catch (final ApduConnectionException e) {
                    throw new CryptoCardException("Error en el establecimiento del canal seguro: " + e, e); //$NON-NLS-1$
                }
            }
            if (doChv) {
	            try {
	                verifyPin(getInternalPasswordCallback());
	            }
	            catch (final ApduConnectionException e) {
	                throw new CryptoCardException("Error en la apertura del canal seguro: " + e, e); //$NON-NLS-1$
	            }
            }
        }
    }

    private int getPinRetriesLeft() throws PinException {
    	final CommandApdu verifyCommandApdu = new RetriesLeftApduCommand();
    	final ResponseApdu verifyResponse;
		try {
			verifyResponse = getConnection().transmit(
				verifyCommandApdu
			);
		}
		catch (final ApduConnectionException e) {
			throw new PinException(
				"Error obteniendo el PIN del CallbackHandler: " + e  //$NON-NLS-1$
			);
		}
    	return verifyResponse.getStatusWord().getLsb() - (byte) 0xC0;
    }

    protected PasswordCallback getInternalPasswordCallback() throws PinException,
    																PasswordCallbackNotFoundException {
    	return getInternalPasswordCallback(false);
    }

    protected PasswordCallback getInternalPasswordCallback(final boolean reset) throws	PinException,
    																					PasswordCallbackNotFoundException {
    	// Si hay establecido un PasswordCallback, devolvemos ese
    	if (this.passwordCallback != null) {
    		final int retriesLeft = getPinRetriesLeft();
    		if (retriesLeft == 0) {
    			throw new AuthenticationModeLockedException();
    		}
    		return this.passwordCallback;
    	}

    	// Si hay establecido un CallbackHandler, le solicitamos un PasswordCallback
    	if (this.callbackHandler != null) {

    		// Si se ha pedido resetear los valores predefinidos, comprobamos si teniamos un
    		// callbackHandler que cachease los resultados y los reseteamos en tal caso
    		if (reset && this.callbackHandler instanceof CacheElement) {
    			((CacheElement) this.callbackHandler).reset();
    		}

        	final int retriesLeft = getPinRetriesLeft();
        	if (retriesLeft == 0) {
        		throw new AuthenticationModeLockedException();
        	}

        	final PasswordCallback  pwc = new PasswordCallback(
    			getPinMessage(retriesLeft),
				false
			);
			try {
				this.callbackHandler.handle(new Callback[] { pwc });
			}
			catch (final IOException e) {
				throw new PinException(
					"Error obteniendo el PIN del CallbackHandler: " + e, e//$NON-NLS-1$
				);
			}
			catch (final UnsupportedCallbackException e) {
				throw new PasswordCallbackNotFoundException(
					"El CallbackHandler no soporta pedir el PIN al usuario: " + e, e//$NON-NLS-1$
				);
			}
			if (pwc.getPassword() == null || pwc.getPassword().toString().isEmpty()) {
				throw new PinException(
					"El PIN no puede ser nulo ni vacio" //$NON-NLS-1$
				);
			}
			return pwc;
    	}
    	throw new PasswordCallbackNotFoundException("No hay ningun metodo para obtener el PIN"); //$NON-NLS-1$
    }

    /** Devuelve el texto del di&aacute;logo de inserci&oacute;n de PIN.
     * @param retriesLeft Intentos restantes antes de bloquear la tarjeta.
     * @return Mensaje que mostrar en el cuerpo del di&aacute;logo de inserci&oacute;n de PIN. */
    @SuppressWarnings("static-method")
	protected String getPinMessage(final int retriesLeft) {
    	return CardMessages.getString("Dnie.0", Integer.toString(retriesLeft)); //$NON-NLS-1$
    }

    private X509Certificate loadCertificate(final Location location) throws IOException,
                                                                            Iso7816FourCardException,
                                                                            CertificateException {
    	selectMasterFile();
        final byte[] certEncoded = selectFileByLocationAndRead(location);
        return CompressionUtils.getCertificateFromCompressedOrNotData(certEncoded);
    }

    /** Carga los certificados del DNIe.
     * Necesita que est&eacute;n previamente cargadas las rutas hacia los certificados.
     * @throws CryptoCardException En cualquier error durante la carga. */
    protected void loadCertificates() throws CryptoCardException {

        // Cargamos certificados si es necesario
    	if (this.authCert == null ||
    		this.signCert == null ||
    		this.cyphCert == null && this.cyphCertPath != null ||
    		this.signAliasCert == null && this.signAliasCertPath != null) {
		        try {
	        		this.signCert = loadCertificate(this.signCertPath);
	        		this.authCert = loadCertificate(this.authCertPath);
		            if (this.cyphCertPath != null) {
	            		this.cyphCert = loadCertificate(this.cyphCertPath);
	            	}
		            if (this.signAliasCertPath != null) {
		            	this.signAliasCert = loadCertificate(this.signAliasCertPath);
		            }
		        }
		        catch (final CertificateException e) {
		            throw new CryptoCardException(
		        		"Error al cargar los certificados del DNIe, no es posible obtener una factoria de certificados X.509: " + e, e //$NON-NLS-1$
		    		);
		        }
		        catch (final IOException e) {
		            throw new CryptoCardException(
		        		"Error al cargar los certificados del DNIe, error en la descompresion de los datos: " + e, e //$NON-NLS-1$
		    		);
				}
		        catch (final Iso7816FourCardException e) {
		            throw new CryptoCardException(
		        		"Error al cargar los certificados del DNIe: " + e, e //$NON-NLS-1$
		    		);
				}
    	}
    }

	@Override
    protected void selectMasterFile() throws ApduConnectionException, Iso7816FourCardException {
    	selectFileByName(MASTER_FILE_NAME);
    }

    protected boolean isSecurityChannelOpen() {
	    //Devuelve true si el canal actual es de PIN o de usuario
        return getConnection() instanceof Cwa14890Connection &&
        		getConnection().isOpen() &&
        			!(getConnection() instanceof PaceConnection);
    }

    @Override
    public void verifyPin(final PasswordCallback psc) throws ApduConnectionException,
    		                                             PinException {
    	if (psc == null) {
    		throw new IllegalArgumentException(
    			"No se puede verificar el titular con un PasswordCallback nulo" //$NON-NLS-1$
        	);
    	}
    	VerifyApduCommand verifyCommandApdu = new VerifyApduCommand((byte) 0x00, psc);

    	final ResponseApdu verifyResponse = getConnection().transmit(
			verifyCommandApdu
    	);
    	verifyCommandApdu = null;

        // Comprobamos si ocurrio algun error durante la verificacion del PIN para volverlo
        // a pedir si es necesario
        if (!verifyResponse.isOk()) {
            if (verifyResponse.getStatusWord().getMsb() == ERROR_PIN_SW1) {
            	// Si no hay reintento automatico se lanza la excepcion
            	// Incluimos una proteccion en el caso de usar algun "CachePasswordCallback" del
            	// Cliente @firma o un callback personalizado que indicaba que debia almacenarse el PIN,
            	// ya que en caso de reutilizarlos se bloquearia el DNI
            	if (!PIN_AUTO_RETRY || psc.getClass().getName().endsWith("CachePasswordCallback")) { //$NON-NLS-1$
            		throw new BadPinException(verifyResponse.getStatusWord().getLsb() - (byte) 0xC0);
            	}
            	// Si hay reintento automatico volvemos a pedir el PIN con la misma CallBack
           		verifyPin(getInternalPasswordCallback(true));
            }
            else if (verifyResponse.getStatusWord().getMsb() == (byte)0x69 &&
            		 verifyResponse.getStatusWord().getLsb() == (byte)0x83) {
            	throw new AuthenticationModeLockedException();
            }
            else if (verifyResponse.getStatusWord().getMsb() == (byte)0x00 &&
            		 verifyResponse.getStatusWord().getLsb() == (byte)0x00) {
            	throw new ApduConnectionException("Se ha perdido el canal NFC"); //$NON-NLS-1$
            }
            else {
            	throw new ApduConnectionException(
        			new Iso7816FourCardException(
    	        		"Error en la verificacion de PIN (" + verifyResponse.getStatusWord() + ")", //$NON-NLS-1$ //$NON-NLS-2$
    	        		verifyResponse.getStatusWord()
    				)
    			);
            }
        }
    }

	@Override
	public int getIfdKeyLength(final Cwa14890PublicConstants consts) {
		return consts.getIfdKeyLength();
	}

	/** Realiza la operaci&oacute;n de cambio de PIN. Necesita tener un canal administrativo abierto.
	 * @param oldPin PIN actual.
	 * @param newPin PIN nuevo.
	 * @return APDU de respuesta de la operaci&oacute;n.
	 * @throws CryptoCardException Cuando se produce un error en el cambio de PIN.
	 * @throws PinException Si el PIN actual es incorrecto.
	 * @throws AuthenticationModeLockedException Cuando el DNIe est&aacute; bloqueado. */
	public byte[] changePIN(final String oldPin, final String newPin) throws CryptoCardException,
	                                                                         PinException,
	                                                                         AuthenticationModeLockedException {
		openSecureChannelIfNotAlreadyOpened();
		try {
			//Seleccion de directorio maestro
			selectMasterFile();
			//Seleccion de fichero de PIN por Id
			final byte[] pinFile = new byte[] {(byte)0x00, (byte) 0x00};
			selectFileById(pinFile);
			//Envio de APDU de cambio de PIN
			final CommandApdu apdu = new ChangePINApduCommand(oldPin.getBytes(), newPin.getBytes());
			final ResponseApdu res = getConnection().transmit(apdu);
			if (!res.isOk()) {
				throw new DnieCardException(
					"Error en el establecimiento de las variables de entorno para el cambio de PIN", res.getStatusWord() //$NON-NLS-1$
				);
			}
			return res.getData();
		}
		catch(final LostChannelException e) {
			LOGGER.warning("Se ha perdido el canal seguro para cambiar el PIN, se procede a recuperarlo: " + e); //$NON-NLS-1$
			try {
				getConnection().close();
				if (getConnection() instanceof Cwa14890Connection) {
					setConnection(((Cwa14890Connection) getConnection()).getSubConnection());
				}
				// Se vuelve a llamar ya con el canal recuperado.
				// Como no hay control de la recursividad, si hay continuas perdidas de canal
				// se terminara provocando un desbordamiento de pila.
				return changePIN(oldPin, newPin);
			}
			catch (final Exception ex) {
				throw new DnieCardException("No se pudo recuperar el canal seguro para firmar: " + ex, ex); //$NON-NLS-1$
			}
		}
		catch (final ApduConnectionException e) {
			throw new DnieCardException("Error en la transmision de comandos a la tarjeta: " + e, e); //$NON-NLS-1$
		}
		catch (final Iso7816FourCardException e) {
			throw new DnieCardException("No se pudo seleccionar el fichero de PIN de la tarjeta: " + e, e); //$NON-NLS-1$
		}
	}

    /** Asigna un <code>CallbackHandler</code> a la tarjeta.
     * @param handler <code>CallbackHandler</code> a asignar. */
    public void setCallbackHandler(final CallbackHandler handler) {
    	this.callbackHandler = handler;
    }

	/** Asigna un <code>PasswordCallback</code> a la tarjeta.
	 * @param pwc <code>PasswordCallback</code> a asignar. */
	public void setPasswordCallback(final PasswordCallback pwc) {
		this.passwordCallback = pwc;
	}

	/** Obtiene el n&uacute;mero de soporte (IDESP) del DNIe.
	 * @return Obtiene el n&uacute;mero de soporte (IDESP) del DNIe.
	 * @throws Iso7816FourCardException Si hay problemas enviando la APDU.
	 * @throws FileNotFoundException Si no se encuentra el fichero que contiene el IDESP.
	 * @throws IOException Si no se puede conectar con la tarjeta. */
	public String getIdesp() throws Iso7816FourCardException, IOException {
		return new String(selectFileByLocationAndRead(IDESP_LOCATION));
	}
}