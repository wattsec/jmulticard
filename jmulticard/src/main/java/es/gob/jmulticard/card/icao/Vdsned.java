package es.gob.jmulticard.card.icao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import es.gob.jmulticard.HexUtils;
import es.gob.jmulticard.asn1.Tlv;
import es.gob.jmulticard.asn1.TlvException;

/** <i>Visible Digital Seals for Non-Electronic Documents</i> de ICAO.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class Vdsned {

	private static final Logger LOGGER = Logger.getLogger("es.gob.jmulticard"); //$NON-NLS-1$

	private static final byte MAGIC = (byte) 0xdc;

	private final byte[] encoded;

	private final int version;
	private final String issuingCountry;
	private final byte[] caCr;
	private final Date documentIssueDate;
	private final Date signatureCreationDate;
	private final int documentFeatureDefinitionReference;
	private final int documentTypeCategory;

	private String mrzB = null;
	private int nEntries = 0;
	private int durationOfStay = 0;
	private String passportNumber = null;

	/** Construye un <i>Visible Digital Seals for Non-Electronic Documents</i> de ICAO.
	 * @param enc Codificaci&oacute;n binaria del <i>Visible Digital Seals for Non-Electronic Documents</i>.
	 * @throws IOException Si hay problemas durante el an&aacute;lisis de la codificaci&oacute;n
	 *                     proporcionada.
	 * @throws TlvException */
	public Vdsned(final byte[] enc) throws IOException, TlvException {

		if (enc == null || enc.length < 1) {
			throw new IllegalArgumentException(
				"La codificacion binaria del VDSNED no puede ser nula ni vacia" //$NON-NLS-1$
			);
		}
		this.encoded = enc.clone();
		int offset = 0;

		// Magic
		if (this.encoded[offset++] != MAGIC) {
			throw new IllegalArgumentException(
				"La codificacion binaria proporcionada no corresponde con un VDSNED" //$NON-NLS-1$
			);
		}

		// Version
		this.version = this.encoded[offset++] + 1;
		if (this.version != 3 && this.version != 4) {
			throw new IllegalArgumentException(
				"Solo se soportan VDSNED v3 o v4, y se ha proporcionado un v" + this.version //$NON-NLS-1$
			);
		}

		// Pais emisor
		this.issuingCountry = C40Decoder.decode(new byte[] { this.encoded[offset++], this.encoded[offset++] });

		// CA-CR
		//TODO: Sacar el formato de CA-CR
		this.caCr = new byte[] {
			this.encoded[offset++], this.encoded[offset++], this.encoded[offset++],
			this.encoded[offset++], this.encoded[offset++], this.encoded[offset++]
		};

		// Fecha de emision del documento
		byte[] tmpDateBytes = {
			0x00, this.encoded[offset++], this.encoded[offset++], this.encoded[offset++]
		};
		String tmpDate = Integer.toString(ByteBuffer.wrap(tmpDateBytes).getInt());
		try {
			this.documentIssueDate = new SimpleDateFormat(
				tmpDate.length() == 7 ? "Mddyyyy" : "MMddyyyy" //$NON-NLS-1$ //$NON-NLS-2$
			).parse(tmpDate);
		}
		catch (final ParseException e) {
			throw new IllegalArgumentException(
				"La fecha de emision del documento es invalida (" + HexUtils.hexify(tmpDateBytes, false) + ", " + tmpDate + "): " + e, e //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			);
		}

		// Fecha de creación de la firma
		tmpDateBytes = new byte[] {
			0x00, this.encoded[offset++], this.encoded[offset++], this.encoded[offset++]
		};
		tmpDate = Integer.toString(ByteBuffer.wrap(tmpDateBytes).getInt());
		try {
			this.signatureCreationDate = new SimpleDateFormat(
				tmpDate.length() == 7 ? "Mddyyyy" : "MMddyyyy" //$NON-NLS-1$ //$NON-NLS-2$
			).parse(tmpDate);
		}
		catch (final ParseException e) {
			throw new IllegalArgumentException(
				"La fecha de creacion de la firma es invalida (" + HexUtils.hexify(tmpDateBytes, false) + ", " + tmpDate + "): " + e, e //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			);
		}

		// Referencia
		this.documentFeatureDefinitionReference = this.encoded[offset++];

		// Categoria
		this.documentTypeCategory = this.encoded[offset++];
		if ( (this.documentTypeCategory & 1) == 0 ) {
			throw new IllegalArgumentException(
				"La categoria debe ser un numero impar, pero se ha encontrado " +  this.documentTypeCategory //$NON-NLS-1$
			);
		}

		while(offset < this.encoded.length) {

			final byte[] data = new byte[this.encoded.length - offset];
			System.arraycopy(this.encoded, offset, data, 0, data.length);

			final Tlv tlv = new Tlv(data);

			switch(tlv.getTag()) {
				case 0x02:
					this.mrzB = C40Decoder.decode(tlv.getValue());
					break;
				case 0x03:
					this.nEntries = HexUtils.getUnsignedInt(tlv.getValue(), 0);
					break;
				case 0x04:
					final byte[] durBytes = tlv.getValue();
					// Dos o una posiciones
					if (durBytes.length < 3) {
						this.durationOfStay = HexUtils.getUnsignedInt(durBytes, 0);
					}
					// Tres posiciones
					else if (durBytes.length == 3) {
						this.durationOfStay = ByteBuffer.wrap(
							new byte[] {
								0x00, durBytes[2], durBytes[1], durBytes[0]
							}
						).getInt();
					}
					// Cuatro o mas posiciones
					else {
						this.durationOfStay = ByteBuffer.wrap(tmpDateBytes).getInt();
					}
					break;
				case 0x05:
					this.passportNumber = C40Decoder.decode(tlv.getValue());
					break;
				default:
					LOGGER.warning("Encontrado campo de datos desconocido: " + tlv); //$NON-NLS-1$
			}

			offset = offset + tlv.getBytes().length;

			System.out.println(offset + " : " + this.encoded.length); //$NON-NLS-1$

		}

	}

	@Override
	public String toString() {
		final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy"); //$NON-NLS-1$
		return "Visible Digital Seal for Non-Electronic Documents\n" + //$NON-NLS-1$
			" Version: " + this.version + '\n' + //$NON-NLS-1$
			" Pais emisor: " + CountryCodes.getCountryName(this.issuingCountry) + '\n' + //$NON-NLS-1$
			" Fecha de emision del documento: " + sdf.format(this.documentIssueDate) + '\n' + //$NON-NLS-1$
			" Fecha de creacion de la firma: " + sdf.format(this.signatureCreationDate) + '\n' + //$NON-NLS-1$
			" Referencia: " + this.documentFeatureDefinitionReference + '\n' + //$NON-NLS-1$
			" Categoria: " + this.documentTypeCategory + '\n' + //$NON-NLS-1$
			" MRZ-B: " + this.mrzB + '\n' + //$NON-NLS-1$
			" Numero de entradas: " + this.nEntries + '\n' + //$NON-NLS-1$
			" Duracion de la estancia: " + this.durationOfStay + '\n' + //$NON-NLS-1$
			" Numero de pasaporte: " + this.passportNumber + '\n' //$NON-NLS-1$
		;

	}

}
