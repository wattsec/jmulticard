package es.gob.jmulticard.card.icao;

/** MRZ err&oacute;neo.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class MalformedMrzException extends IcaoException {

	/** Identificador de versi&oacute;n para la serializaci&oacute;n. */
	private static final long serialVersionUID = 437348879325857134L;

	/** Crea la excepci&oacute;n de MRZ err&oacute;neo introducido.
	 * @param description Descripci&oacute;n del error. */
	MalformedMrzException(final String description) {
		super(description);
	}

	/** Excepci&oacute;n de MRZ err&oacute;neo introducido.
	 * @param description Descripci&oacute;n del error.
	 * @param cause Causa inicial del error. */
	MalformedMrzException(final String description, final Exception cause) {
		super(description, cause);
	}
}
