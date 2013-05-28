package platform.client;

import platform.base.SystemUtils;
import platform.client.remote.proxy.RemoteBusinessLogicProxy;
import platform.interop.RemoteLogicsLoaderInterface;
import platform.interop.RemoteLogicsInterface;
import platform.interop.exceptions.LoginException;
import platform.interop.exceptions.RemoteInternalException;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnknownHostException;
import java.util.concurrent.CancellationException;

import static platform.client.ClientResourceBundle.getString;
import static platform.client.StartupProperties.*;

public final class LoginAction {
    private static class LoginActionHolder {
        private static LoginAction instance = new LoginAction();
    }

    public static LoginAction getInstance() {
        return LoginActionHolder.instance;
    }

    //login statuses
    final static int OK = 0;
    final static int HOST_NAME_ERROR = 1;
    final static int CONNECT_ERROR = 2;
    final static int SERVER_ERROR = 3;
    final static int PENDING_RESTART_WARNING = 4;
    final static int ERROR = 5;
    final static int CANCELED = 6;
    final static int LOGIN_ERROR = 7;

    private boolean autoLogin;
    public LoginInfo loginInfo;
    private LoginDialog loginDialog;

    private RemoteLogicsInterface remoteLogics;
    private int computerId;
    private RemoteNavigatorInterface remoteNavigator;

    private LoginAction() {
        autoLogin = Boolean.getBoolean(PLATFORM_CLIENT_AUTOLOGIN);
        loginInfo = new LoginInfo(System.getProperty(PLATFORM_CLIENT_HOSTNAME), System.getProperty(PLATFORM_CLIENT_HOSTPORT),
                System.getProperty(PLATFORM_CLIENT_EXPORTNAME), System.getProperty(PLATFORM_CLIENT_USER),
                System.getProperty(PLATFORM_CLIENT_PASSWORD));

        loginDialog = new LoginDialog(loginInfo);
    }

    public boolean login() throws MalformedURLException, NotBoundException, RemoteException {
        boolean needData = loginInfo.getServerHost() == null || loginInfo.getServerPort() == null || loginInfo.getServerDB() == null || loginInfo.getUserName() == null || loginInfo.getPassword() == null;
        if (!autoLogin || needData) {
            loginDialog.setAutoLogin(autoLogin);
            loginInfo = loginDialog.login();
        }

        if (loginInfo == null) {
            return false;
        }

        int status = connect();

        while (!(status == OK)) {
            switch (status) {
                case HOST_NAME_ERROR:
                    loginDialog.setWarningMsg(getString("errors.check.server.address"));
                    break;
                case CONNECT_ERROR:
                    loginDialog.setWarningMsg(getString("errors.error.connecting.to.the.server"));
                    break;
                case SERVER_ERROR:
                    loginDialog.setWarningMsg(getString("errors.internal.server.error"));
                    break;
                case PENDING_RESTART_WARNING:
                    loginDialog.setWarningMsg(getString("errors.server.reboots"));
                    break;
                case ERROR:
                    loginDialog.setWarningMsg(getString("errors.error.connecting"));
                    break;
                case CANCELED:
                    loginDialog.setWarningMsg(getString("errors.error.cancel"));
                    break;
                case LOGIN_ERROR:
                    loginDialog.setWarningMsg(getString("errors.check.login.and.password"));
                    break;
            }
            loginDialog.setAutoLogin(false);
            loginInfo = loginDialog.login();
            if (loginInfo == null) {
                return false;
            }
            status = connect();
        }

        return true;
    }

    private int connect() {
        RemoteLogicsLoaderInterface remoteLoader;
        RemoteLogicsInterface remoteLogics;
        int computerId;
        RemoteNavigatorInterface remoteNavigator;

        try {
            //Нужно сразу инициализировать Main.remoteLoader, т.к. используется для загрузки классов в ClientRMIClassLoaderSpi
            Main.remoteLoader = remoteLoader = new ReconnectWorker(loginInfo.getServerHost(), loginInfo.getServerPort(), loginInfo.getServerDB()).connect();
            if (remoteLoader == null) {
                return CANCELED;
            }
            RemoteLogicsInterface remote = remoteLoader.getLogics();

            remoteLogics = new RemoteBusinessLogicProxy(remote);
            computerId = remoteLogics.getComputer(SystemUtils.getLocalHostName());

            remoteNavigator = remoteLogics.createNavigator(Main.module.isFull(), loginInfo.getUserName(), loginInfo.getPassword(), computerId, SystemUtils.getLocalHostIP(), false);
            if (remoteNavigator == null) {
                Main.remoteLoader = null;
                return PENDING_RESTART_WARNING;
            }
        } catch (CancellationException ce) {
            return CANCELED;
        } catch (UnknownHostException e) {
            System.out.println(e.getCause());
            return HOST_NAME_ERROR;
        } catch (RemoteInternalException e) {
            e.printStackTrace();
            return SERVER_ERROR;
        } catch (LoginException e) {
            e.printStackTrace();
            return LOGIN_ERROR;
        } catch (Throwable e) {
            e.printStackTrace();
            return ERROR;
        }

        this.remoteLogics = remoteLogics;
        this.remoteNavigator = remoteNavigator;
        this.computerId = computerId;

        return OK;
    }

    public RemoteLogicsInterface getRemoteLogics() {
        return remoteLogics;
    }

    public int getComputerId() {
        return computerId;
    }

    public RemoteNavigatorInterface getRemoteNavigator() {
        return remoteNavigator;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }
}
