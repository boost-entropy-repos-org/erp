package equ.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class MachineryHandler<T extends TransactionInfo, M extends MachineryInfo, S extends SalesBatch>/* extends Serializable*/ {

    public EquipmentServerInterface remote;
    
    public abstract String getGroupId(T transactionInfo) throws IOException;

    public abstract Map<Long, SendTransactionBatch> sendTransaction(List<T> transactionInfoList) throws IOException;

    public abstract void sendSoftCheck(SoftCheckInfo softCheckInfo) throws IOException;

    public void setRemoteObject(EquipmentServerInterface remote) {
        this.remote = remote;
    }

}
