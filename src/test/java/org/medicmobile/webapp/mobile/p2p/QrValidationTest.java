package org.medicmobile.webapp.mobile.p2p;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QrValidationTest {

	@Test
	public void accept_isAcceptedAndCarriesNoReason() {
		QrValidation validation = QrValidation.accept();

		assertTrue(validation.isAccepted());
		assertNull(validation.getReason());
	}

	@Test
	public void reject_isNotAcceptedAndKeepsTheReason() {
		QrValidation validation = QrValidation.reject("missing required field: ssid");

		assertFalse(validation.isAccepted());
		assertEquals("missing required field: ssid", validation.getReason());
	}

	@Test
	public void toString_saysWhichOutcomeItIs() {
		assertTrue(QrValidation.accept().toString().contains("accepted"));
		assertTrue(QrValidation.reject("expired").toString().contains("expired"));
	}
}
