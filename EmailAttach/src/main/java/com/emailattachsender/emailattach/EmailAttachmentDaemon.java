package com.emailattachsender.emailattach;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

public class EmailAttachmentDaemon implements Daemon {

    private Thread thread;

    @Override
    public void init(DaemonContext context) throws DaemonInitException, Exception {
    }

    @Override
    public void start() throws Exception {
        thread = new Thread(() -> {
            try {
                EmailAttachmentSaverConfig.runService();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    @Override
    public void stop() throws Exception {
        if (thread != null) {
            thread.interrupt();
            thread.join();
        }
    }

    @Override
    public void destroy() {
    }

    public static void main(String[] args) {
        EmailAttachmentDaemon daemon = new EmailAttachmentDaemon();
        try {
            if (args.length > 0 && args[0].equals("service")) {
                // Iniciar no modo serviço
                daemon.start();
            } else {
                // Iniciar no modo normal
                EmailAttachmentSaverConfig.main(args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
