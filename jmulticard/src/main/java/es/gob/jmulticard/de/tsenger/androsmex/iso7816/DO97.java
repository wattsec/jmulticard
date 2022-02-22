/**
 *  Copyright 2011, Tobias Senger
 *
 *  This file is part of animamea.
 *
 *  Animamea is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Animamea is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with animamea.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.gob.jmulticard.de.tsenger.androsmex.iso7816;

import java.io.IOException;

import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERTaggedObject;

/** <i>Payload</i> de respuesta.
 * <code>| 0x97 | L | Longitud (L octetos) |</code>
 * @author Tobias Senger (tobias@t-senger.de). */
final class DO97 {

    private byte[] data = null;
    private DERTaggedObject to = null;

	DO97(final int le) {
		this.data = new byte[1];
		this.data[0] = (byte) le;
		this.to = new DERTaggedObject(false, 0x17, new DEROctetString(this.data));
	}

	byte[] getEncoded() throws SecureMessagingException {
    	try {
			return this.to.getEncoded();
		}
    	catch (final IOException e) {
			throw new SecureMessagingException(e);
		}
    }

}
