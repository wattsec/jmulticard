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
package es.gob.jmulticard.asn1.der;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import es.gob.jmulticard.asn1.Asn1Exception;
import es.gob.jmulticard.asn1.DecoderObject;
import es.gob.jmulticard.asn1.Tlv;
import es.gob.jmulticard.asn1.TlvException;

/** Tipo <i>UTF8String</i> de ASN&#46;1.
 * Incorpora soporte adem&aacute;s para <i>GeneralizedTime</i>.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class GeneralizedTime extends DecoderObject {

	/** Tipo ASN&#46;1 "GeneralizedTime". */
    private static final byte TAG = (byte) 0x18;

    @Override
    protected byte getDefaultTag() {
        return TAG;
    }

    private String strValue = null;

    @Override
    protected void decodeValue() throws Asn1Exception, TlvException {
    	final Tlv tlv = new Tlv(getBytes());
    	checkTag(tlv.getTag());
    	strValue = new String(tlv.getValue(), StandardCharsets.US_ASCII);
    }

    /** Obtiene el valor de la fecha.
     * @return Valor de la fecha.
     * @throws Asn1Exception Si la fecha no est&aacute;n en el formato esperado. */
    public Date getDateValue() throws Asn1Exception {
    	try {
			return new SimpleDateFormat("yyyyMMdd").parse(strValue.substring(0, 8)); //$NON-NLS-1$
		}
    	catch (final ParseException e) {
			throw new Asn1Exception("La fecha no estaba en el formato esperado: " + strValue, e); //$NON-NLS-1$
		}
    }

    @Override
    public String toString() {
    	return strValue;
    }
}
