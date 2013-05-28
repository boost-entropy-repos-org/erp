package platform.client.remote;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.client.remote.proxy.RemoteObjectProxy;

@Aspect
public class ImmutableProxyMethodsAspect {
    private static Logger logger = Logger.getLogger(ImmutableProxyMethodsAspect.class);

    @Around("execution(" +
            "@platform.client.remote.proxy.ImmutableMethod" +
            " * platform.client.remote.proxy.RemoteObjectProxy+.*(..))" +
            " && target(object)")
    public synchronized Object executeImmutableMethod(ProceedingJoinPoint thisJoinPoint, Object object) throws Throwable {
        if (isRestarting) {
            return null;
        }

        String name = thisJoinPoint.getSignature().getName();
        RemoteObjectProxy remoteObject = (RemoteObjectProxy) object;
        logger.debug("Running immutable method: " + name);
        if (remoteObject.hasProperty(name)) {
            logger.debug("  Returning cached value: " + remoteObject.getProperty(name));
            return remoteObject.getProperty(name);
        } else {
            logger.debug("  Directly call immutable method:");
            return thisJoinPoint.proceed();
        }
    }

    private boolean isRestarting = false;
    public synchronized void startRestarting() {
        isRestarting = true;
    }

    public synchronized void stopRestarting() {
        isRestarting = false;
    }
}
