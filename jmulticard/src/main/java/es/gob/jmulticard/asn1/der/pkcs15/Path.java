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
package es.gob.jmulticard.asn1.der.pkcs15;

import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.asn1.OptionalDecoderObjectElement;
import es.gob.jmulticard.asn1.der.DerInteger;
import es.gob.jmulticard.asn1.der.OctectString;
import es.gob.jmulticard.asn1.der.Sequence;
/** Tipo ASN&#46;1 PKCS#15 <i>Path</i>.
 * <pre>
 *  Path ::= SEQUENCE {
 *   path OCTET STRING,
 *   index INTEGER (0..pkcs15-ub-index) OPTIONAL,
 *   length [0] INTEGER (0..pkcs15-ub-index) OPTIONAL
 *  }
 * </pre>
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class Path extends Sequence {

	/** Crea un objeto ASN&#46;1 PKCS#15 <i>Path</i>. */
	public Path() {
		super(
			new OptionalDecoderObjectElement(
				OctectString.class,
				false
			),
			new OptionalDecoderObjectElement(
				DerInteger.class,
				true
			),
			new OptionalDecoderObjectElement(
				PathLength.class,
				true
			)
		);
	}

	/** Obtiene la ruta del certificado como texto.
	 * @return Ruta del certificado como texto */
	String getPathString() {
		return HexUtils.hexify(((OctectString)getElementAt(0)).getOctectStringByteValue(), false);
	}

	/** Obtiene la ruta en forma de array de octetos.
	 * @return Ruta en forma de array de octetos. */
	public byte[] getPathBytes() {
		return ((OctectString)getElementAt(0)).getOctectStringByteValue();
	}

	@Override
	public String toString() {
		return getPathString();
	}
}
