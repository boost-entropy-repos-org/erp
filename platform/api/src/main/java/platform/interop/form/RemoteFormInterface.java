package platform.interop.form;

import platform.interop.ClassViewType;
import platform.interop.remote.PendingRemoteInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteFormInterface extends PendingRemoteInterface {

    // структура формы

    byte[] getRichDesignByteArray() throws RemoteException;

    String getSID() throws RemoteException;

    // синхронное общение с сервером

    public ServerResponse getRemoteChanges(long requestIndex) throws RemoteException;

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException;

    public ServerResponse throwInServerInvocation(Exception clientException) throws RemoteException;

    // события формы

    void gainedFocus(long requestIndex) throws RemoteException;

    ServerResponse setTabVisible(long requestIndex, int tabPaneID, int tabIndex) throws RemoteException;

    ServerResponse closedPressed(long requestIndex) throws RemoteException;

    ServerResponse okPressed(long requestIndex) throws RemoteException;

    // события групп объектов

    ServerResponse changeGroupObject(long requestIndex, int groupID, byte[] value) throws RemoteException;

    ServerResponse changePageSize(long requestIndex, int groupID, Integer pageSize) throws RemoteException; // размер страницы

    ServerResponse changeGroupObject(long requestIndex, int groupID, byte changeType) throws RemoteException; // скроллинг

    ServerResponse pasteExternalTable(long requestIndex, List<Integer> propertyIDs, List<byte[]> columnKeys, List<List<byte[]>> values) throws RemoteException; // paste подряд

    ServerResponse pasteMulticellValue(long requestIndex, Map<Integer, List<Map<Integer, Object>>> keys, Map<Integer, byte[]> values) throws RemoteException; // paste выборочно

    ServerResponse changeClassView(long requestIndex, int groupID, ClassViewType classView) throws RemoteException;

    // деревья

    ServerResponse expandGroupObject(long requestIndex, int groupId, byte[] bytes) throws RemoteException;

    ServerResponse collapseGroupObject(long requestIndex, int groupId, byte[] bytes) throws RemoteException;

    ServerResponse moveGroupObject(long requestIndex, int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException;

    // свойства

    ServerResponse executeEditAction(long requestIndex, int propertyID, byte[] columnKey, String actionSID) throws RemoteException;

    // асинхронные вызовы

    ServerResponse changeProperty(long requestIndex, int propertyID, byte[] fullKey, byte[] pushChange, Integer pushAdd) throws RemoteException;

    // фильтры / порядки

    ServerResponse changeGridClass(long requestIndex, int objectID, int idClass) throws RemoteException;

    ServerResponse changePropertyOrder(long requestIndex, int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    ServerResponse clearPropertyOrders(long requestIndex, int groupObjectID) throws RemoteException;
    
    ServerResponse setUserFilters(long requestIndex, byte[][] filters) throws RemoteException;

    ServerResponse setRegularFilter(long requestIndex, int groupID, int filterID) throws RemoteException;

    // отчеты

    ReportGenerationData getReportData(long requestIndex, Integer groupId, boolean toExcel, FormUserPreferences userPreferences) throws RemoteException;

    Map<String, String> getReportPath(long requestIndex, boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException;

    // быстрая информация

    int countRecords(long requestIndex, int groupObjectID) throws RemoteException;

    Object calculateSum(long requestIndex, int propertyID, byte[] columnKeys) throws RemoteException;

    Map<List<Object>, List<Object>> groupData(long requestIndex, Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                              Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException;

    // пользовательские настройки

    void saveUserPreferences(long requestIndex, FormUserPreferences preferences, Boolean forAllUsers) throws RemoteException;

    FormUserPreferences getUserPreferences() throws RemoteException;
}
