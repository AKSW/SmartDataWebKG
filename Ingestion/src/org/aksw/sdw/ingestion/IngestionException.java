package org.aksw.sdw.ingestion;

import aksw.org.kg.KgException;

public class IngestionException extends KgException {

	/**
	 * generated serial version
	 */
	private static final long serialVersionUID = 1567295659099559318L;
	
	public IngestionException(final String message) {
		super(message);
	}
	
	public IngestionException(final String message, final Throwable throwable) {
		super(message, throwable);
	}
}
