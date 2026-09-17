package org.medicmobile.webapp.mobile.p2p;

import static org.medicmobile.webapp.mobile.MedicLog.log;
import static org.medicmobile.webapp.mobile.MedicLog.warn;

/**
	* Manages the WiFi hotspot lifecycle for P2P sync.
	*
	* Wraps a {@link HotspotProvider} with:
	* - Idle timeout detection, so an unused hotspot shuts itself down
	* - Start/stop timing
	* - State tracking to prevent double-start
	*
	* The idle timeout is passed in rather than read from a config object: this class needs one
	* number, and the caller owns where it comes from.
	*/
public class WifiHotspotManager {


	private final HotspotProvider provider;
	private final long idleTimeoutMs;

	private long startedAt;
	private volatile long lastActivityAt;
	private String activeSsid;
	private String activePassword;
	private String activeIpAddress;

	public WifiHotspotManager(HotspotProvider provider, int idleTimeoutSec) {
		if (provider == null) {
			throw new IllegalArgumentException("provider must not be null");
		}
		if (idleTimeoutSec <= 0) {
			throw new IllegalArgumentException("idleTimeoutSec must be positive");
		}
		this.provider = provider;
		this.idleTimeoutMs = (long) idleTimeoutSec * 1000;
	}

	/**
		* Start the hotspot. Wraps the provider callback with lifecycle tracking.
		*
		* @param callback receives hotspot credentials on success, or failure reason
		*/
	public void startHotspot(HotspotProvider.HotspotCallback callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback must not be null");
		}

		if (provider.isRunning()) {
			warn(this, "Hotspot already active, returning existing credentials");
			callback.onStarted(activeSsid, activePassword, activeIpAddress);
			return;
		}

		provider.start(new HotspotProvider.HotspotCallback() {
			@Override
			public void onStarted(String ssid, String password, String ipAddress) {
				long now = System.currentTimeMillis();
				startedAt = now;
				lastActivityAt = now;
				activeSsid = ssid;
				activePassword = password;
				activeIpAddress = ipAddress;

				log(this, "Hotspot started: SSID=" + ssid + ", IP=" + ipAddress);
				callback.onStarted(ssid, password, ipAddress);
			}

			@Override
			public void onFailed(String reason) {
				warn(this, "Hotspot failed to start: " + reason);
				callback.onFailed(reason);
			}
		});
	}

	/**
		* Stop the hotspot and clear all state.
		*/
	public void stopHotspot() {
		if (provider.isRunning()) {
			provider.stop();
			long duration = System.currentTimeMillis() - startedAt;
			log(this, "Hotspot stopped after " + (duration / 1000) + "s");
		}
		activeSsid = null;
		activePassword = null;
		activeIpAddress = null;
		startedAt = 0;
		lastActivityAt = 0;
	}

	/**
		* Check if the hotspot is currently active.
		*/
	public boolean isActive() {
		return provider.isRunning();
	}

	/**
		* Record activity to reset the idle timeout clock.
		* Call this whenever a sync operation occurs (doc transfer, auth, etc.).
		*/
	public void recordActivity() {
		lastActivityAt = System.currentTimeMillis();
	}

	/**
		* Check if the hotspot has exceeded its idle timeout.
		* Used to auto-shutdown the hotspot when no sync activity occurs.
		*
		* @return true if idle time exceeds the configured idle timeout
		*/
	public boolean isIdleTimedOut() {
		if (!provider.isRunning() || lastActivityAt == 0) {
			return false;
		}
		long idleMs = System.currentTimeMillis() - lastActivityAt;
		return idleMs > idleTimeoutMs;
	}



	// --- Getters for active credentials ---

	public String getActiveSsid() {
		return activeSsid;
	}

	public String getActivePassword() {
		return activePassword;
	}

	public String getActiveIpAddress() {
		return activeIpAddress;
	}

}
