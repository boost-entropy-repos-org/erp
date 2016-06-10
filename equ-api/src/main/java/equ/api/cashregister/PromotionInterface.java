package equ.api.cashregister;

import lsfusion.interop.remote.RmiServerInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface PromotionInterface extends RmiServerInterface {
    
    PromotionInfo readPromotionInfo() throws RemoteException, SQLException;
    
}
