package org.zywx.wbpalmstar.plugin.uexvideo.lib.camera;

import org.zywx.wbpalmstar.plugin.uexvideo.lib.CLog;

public class PrepareCameraException extends Exception {

	private static final String	LOG_PREFIX			= "Unable to unlock camera - ";
	private static final String	MESSAGE				= "Unable to use camera for recording";

	private static final long	serialVersionUID	= 6305923762266448674L;

	@Override
	public String getMessage() {
		CLog.e(CLog.EXCEPTION, LOG_PREFIX + MESSAGE);
		return MESSAGE;
	}
}
