package platform.client.remote.proxy;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import platform.base.EProvider;
import platform.client.form.BusyDisplayer;
import platform.interop.remote.MethodInvocation;
import platform.interop.remote.PendingRemoteInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RemoteObjectProxy<T extends PendingRemoteInterface> implements PendingRemoteInterface {
    private static Logger logger = Logger.getLogger(RemoteFormProxy.class);

    protected final T target;

    private final Map<Object, Object> properties = new HashMap<Object, Object>();

    private long startCall = 0;

    public RemoteObjectProxy(T target) {
        this.target = target;
    }

    @Override
    public Object[] createAndExecute(final MethodInvocation creator, final MethodInvocation[] invocations) throws RemoteException {
        logRemoteMethodStartCall("createAndExecute");

        if (logger.isDebugEnabled()) {
            logger.debug("  Creator in createAndExecute: " + creator.toString());
            for (MethodInvocation invocation : invocations) {
                logger.debug("  Invocation in createAndExecute: " + invocation.toString());
            }
        }

        BusyDisplayer busyDisplayer = new BusyDisplayer(new EProvider<String>() {
            @Override
            public String getExceptionally() throws RemoteException {
                return target.getRemoteActionMessage();
            }
        });
        busyDisplayer.start();

        Object[] result;
        try {
            result = target.createAndExecute(creator, invocations);
            logRemoteMethodEndCall("createAndExecute", result);
            return result;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        } finally {
            busyDisplayer.stop();
        }
    }

    @Override
    public String getRemoteActionMessage() throws RemoteException {
        return target.getRemoteActionMessage();
    }

    public Object getProperty(Object key) {
        return properties.get(key);
    }

    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    public boolean hasProperty(Object key) {
        return properties.containsKey(key);
    }

    protected void logRemoteMethodStartCall(String methodName) {
        if (logger.isInfoEnabled()) {
            startCall = System.currentTimeMillis();
            logger.info("Calling remote method: " + this.getClass().getSimpleName() + "." + methodName);
        }
    }

    protected void logRemoteMethodEndCall(String methodName, Object result) {
        if (logger.isInfoEnabled()) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            try {
                new ObjectOutputStream(outStream).writeObject(result);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info(
                    String.format("Remote method called (time: %1$d ms.; result size: %2$d): %3$s.%4$s",
                            System.currentTimeMillis() - startCall, outStream.size(), this.getClass().getSimpleName(), methodName));
        }
    }

    protected void logRemoteMethodStartVoidCall(String methodName) {
        logRemoteMethodStartCall(methodName);
    }

    protected void logRemoteMethodEndVoidCall(String methodName) {
        logRemoteMethodEndCall(methodName, null);
    }

    protected static List<MethodInvocation> getImmutableMethodInvocations(Class clazz) {
        List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(ImmutableMethod.class) == null) {
                continue;
            }

            // естественно, разрешены только функи без параметров
            assert method.getParameterTypes().length == 0;

            MethodInvocation invocation = new MethodInvocation(method.getName(), new Class[0], new Object[0], method.getReturnType());
            invocations.add(invocation);
        }

        return invocations;
    }
}
