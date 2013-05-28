package paas.terminal;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.BusinessLogicsBootstrap;
import platform.server.logics.RMIManager;

public class ApplicationTerminalImpl extends LifecycleAdapter implements ApplicationTerminal, InitializingBean {

    private static final Logger logger = Logger.getLogger(ApplicationTerminalImpl.class);

    public static final String EXPORT_NAME = "AppTerminal";

    private RMIManager rmiManager;
    private boolean started = false;

    public void setRmiManager(RMIManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(rmiManager, "rmiManager must be specified");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Binding Application Terminal.");
        try {
            rmiManager.bindAndExport(EXPORT_NAME, this);
            started = true;
        } catch (Exception e) {
            throw new RuntimeException("Error exporting Application Terminal: ", e);
        }
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        if (started) {
            logger.info("Stopping Application Terminal.");
            try {
                rmiManager.unbindAndUnexport(EXPORT_NAME, this);
            } catch (Exception e) {
                throw new RuntimeException("Error stopping Application Terminal: ", e);
            }
        }
    }

    @Override
    public void stop() {
        //в отдельном потоке, чтобы вернуть управление в точку вызова,
        //чтобы удалённый клиент продолжил выполнение
        new Thread() {
            @Override
            public void run() {
                BusinessLogicsBootstrap.stop();
            }
        }.start();
    }
}
