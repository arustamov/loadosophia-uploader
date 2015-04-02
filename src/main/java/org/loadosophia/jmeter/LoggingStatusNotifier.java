package org.loadosophia.jmeter;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * Created by Anton Rustamov (arustamov) on 4/1/2015.
 */
public class LoggingStatusNotifier implements StatusNotifierCallback {

    private static final Logger log = LoggingManager.getLoggerForClass();

    @Override
    public void notifyAbout(String s) {
        log.info(s);
    }
}
