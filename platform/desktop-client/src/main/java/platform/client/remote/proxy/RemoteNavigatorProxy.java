package platform.client.remote.proxy;

import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ServerResponse;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.ClientCallBackInterface;
import platform.interop.remote.MethodInvocation;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface>
        extends RemoteObjectProxy<T>
        implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target) {
        super(target);
    }

    public RemoteFormInterface createForm(String formSID, Map<String, String> initialObjects, boolean isModal, boolean interactive) throws RemoteException {
        List<MethodInvocation> invocations = getImmutableMethodInvocations(RemoteFormProxy.class);

        boolean hasCachedRichDesignByteArray = false;
        if (RemoteFormProxy.cachedRichDesign.get(formSID) != null) {
            hasCachedRichDesignByteArray = true;
            for (int i = 0; i < invocations.size(); ++i) {
                if (invocations.get(i).name.equals("getRichDesignByteArray")) {
                    invocations.remove(i);
                    break;
                }
            }
        }

        RemoteFormProxy proxy = createForm(invocations, MethodInvocation.create(this.getClass(), "createForm", formSID, initialObjects, isModal, interactive));

        if (hasCachedRichDesignByteArray) {
            proxy.setProperty("getRichDesignByteArray", RemoteFormProxy.cachedRichDesign.get(formSID));
        } else {
            RemoteFormProxy.cachedRichDesign.put(formSID, (byte[]) proxy.getProperty("getRichDesignByteArray"));
        }

        return proxy;
    }

    public RemoteFormInterface createPreviewForm(byte[] formState) throws RemoteException {
        return createForm(getImmutableMethodInvocations(RemoteFormProxy.class),
                          MethodInvocation.create(this.getClass(), "createPreviewForm", new Object[]{formState}));
    }

    private RemoteFormProxy createForm(List<MethodInvocation> immutableMethods, MethodInvocation creator) throws RemoteException {

        Object[] result = createAndExecute(creator, immutableMethods.toArray(new MethodInvocation[immutableMethods.size()]));

        RemoteFormInterface remoteForm = (RemoteFormInterface) result[0];
        if (remoteForm == null) {
            return null;
        }

        RemoteFormProxy proxy = new RemoteFormProxy(remoteForm);
        for (int i = 0; i < immutableMethods.size(); ++i) {
            proxy.setProperty(immutableMethods.get(i).name, result[i + 1]);
        }

        return proxy;
    }

    public byte[] getRichDesignByteArray(String formSID) throws RemoteException {
        return target.getRichDesignByteArray(formSID);
    }

    public byte[] getFormEntityByteArray(String formSID) throws RemoteException {
        return target.getFormEntityByteArray(formSID);
    }

    public byte[] getCurrentUserInfoByteArray() throws RemoteException {
        logRemoteMethodStartCall("getCurrentUserInfoByteArray");
        byte[] result = target.getCurrentUserInfoByteArray();
        logRemoteMethodEndCall("getCurrentUserInfoByteArray", result);
        return result;
    }

    public byte[] getElementsByteArray(String groupSID) throws RemoteException {
        logRemoteMethodStartCall("getElementsByteArray");
        byte[] result = target.getElementsByteArray(groupSID);
        logRemoteMethodEndCall("getElementsByteArray", result);
        return result;
    }

    public void logClientException(String info, String client, String message, String type, String erTrace) throws RemoteException {
        target.logClientException(info, client, message, type, erTrace);
    }

    public void close() throws RemoteException {
        target.close();
    }

    public ClientCallBackInterface getClientCallBack() throws RemoteException {
        return target.getClientCallBack();
    }

    public void setUpdateTime(int updateTime) throws RemoteException {
        target.setUpdateTime(updateTime);
    }

    @ImmutableMethod
    public boolean showDefaultForms() throws RemoteException {
        return target.showDefaultForms();
    }

    @ImmutableMethod
    public List<String> getDefaultForms() throws RemoteException {
        return target.getDefaultForms();
    }

    @ImmutableMethod
    public byte[] getNavigatorTree() throws RemoteException {
        return target.getNavigatorTree();
    }

    @ImmutableMethod
    public byte[] getCommonWindows() throws RemoteException {
        return target.getCommonWindows();
    }

    @Override
    public String getCurrentFormSID() throws RemoteException {
        return target.getCurrentFormSID();
    }

    @Override
    public boolean isConfigurationAccessAllowed() throws RemoteException {
        return target.isConfigurationAccessAllowed();
    }

    @Override
    public ServerResponse executeNavigatorAction(String navigatorActionSID) throws RemoteException {
        return target.executeNavigatorAction(navigatorActionSID);
    }

    @Override
    public ServerResponse continueNavigatorAction(Object[] actionResults) throws RemoteException {
        return target.continueNavigatorAction(actionResults);
    }

    @Override
    public ServerResponse throwInNavigatorAction(Exception clientException) throws RemoteException {
        return target.throwInNavigatorAction(clientException);
    }
}
