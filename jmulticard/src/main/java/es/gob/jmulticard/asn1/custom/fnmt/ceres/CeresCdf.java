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
package es.gob.jmulticard.asn1.custom.fnmt.ceres;

import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.asn1.OptionalDecoderObjectElement;
import es.gob.jmulticard.asn1.der.Record;
import es.gob.jmulticard.asn1.der.pkcs15.CertificateObject;
import es.gob.jmulticard.asn1.der.pkcs15.Pkcs15Cdf;

/** Objeto PKCS#15 CDF (<i>Certificate Description File</i>) ASN&#46;1
 * (<i>EF.CD</i> en ISO 7816-15) espec&iacute;fico para ciertas tarjetas FNMT CERES.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class CeresCdf extends Record implements Pkcs15Cdf {

    private static final int BUFFER_SIZE = 150;

    /** Construye un objeto PKCS#15 CDF (<i>Certificate Description File</i>) ASN&#46;1
     * espec&iacute;fico para ciertas tarjetas FNMT CERES. */
	public CeresCdf() {
        super(
			// Maximo 10 certificados por tarjeta
			// Estructura antigua, que incumple PKCS#15
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true),
			new OptionalDecoderObjectElement(CeresCertificateObject.class, true)
		);
    }

    @Override
	public int getCertificateCount() {
        return getElementCount();
    }

    @Override
	public byte[] getCertificateId(final int index) {
    	final CertificateObject tmpCo = (CertificateObject) getElementAt(index);
    	if (tmpCo != null) {
    		return tmpCo.getIdentifier();
    	}
    	return null;
    }

	@Override
	public String getCertificateAlias(final int index) {
		final byte[] id = getCertificateId(index);
		if (id != null) {
			return HexUtils.hexify(id, false);
		}
		return null;
	}

    @Override
	public String getCertificatePath(final int index) {
    	final CertificateObject tmpCo = (CertificateObject) getElementAt(index);
    	if (tmpCo != null) {
    		return tmpCo.getPath();
    	}
    	return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(BUFFER_SIZE);
        sb.append("Fichero de Descripcion de Certificados CERES:\n"); //$NON-NLS-1$
        for (int index = 0; index < getCertificateCount(); index++) {
            sb.append(" Certificado "); //$NON-NLS-1$
            sb.append(Integer.toString(index));
            sb.append("\n  Ruta PKCS#15: "); //$NON-NLS-1$
            sb.append(getCertificatePath(index));
            sb.append("\n  Identificador del certificado: "); //$NON-NLS-1$
            sb.append(HexUtils.hexify(getCertificateId(index), false));
			if (index != getCertificateCount() -1) {
				sb.append('\n');
			}
        }
        return sb.toString();
    }
}
