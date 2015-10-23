package equ.api.cashregister;

import equ.api.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class  CashRegisterHandler<S extends SalesBatch> extends MachineryHandler<TransactionCashRegisterInfo, CashRegisterInfo, S> {

    public abstract void sendStopListInfo(StopListInfo stopListInfo, Set<String> directorySet) throws IOException;

    public abstract void sendDiscountCardList(List<DiscountCard> discountCardList, Date startDate, Set<String> directory) throws IOException;

    public abstract void sendCashierInfoList(List<CashierInfo> cashierInfoList, Set<String> directorySet, Set<String> stockSet) throws IOException;

    public abstract List<CashierTime> requestCashierTime(List<MachineryInfo> cashRegisterInfoList) throws IOException, ClassNotFoundException, SQLException;

    public abstract void sendPromotionInfo(PromotionInfo promotionInfo, Set<String> directory) throws IOException;
    
    public abstract SalesBatch readSalesInfo(String directory, List<CashRegisterInfo> cashRegisterInfoList) throws IOException, ParseException, ClassNotFoundException;

    public abstract void requestSalesInfo(List<RequestExchange> requestExchangeList, Set<String> directorySet,
                                            Set<Integer> succeededRequests, Map<Integer, String> failedRequests, Map<Integer, String> ignoredRequests) throws IOException, ParseException;

    public abstract void finishReadingSalesInfo(S salesBatch);

    public abstract CashDocumentBatch readCashDocumentInfo(List<CashRegisterInfo> cashRegisterInfoList, Set<String> cashDocumentSet) throws ClassNotFoundException;

    public abstract void finishReadingCashDocumentInfo(CashDocumentBatch cashDocumentBatch);
   
    public abstract Map<String, Timestamp> requestSucceededSoftCheckInfo(Set<String> directorySet) throws ClassNotFoundException, SQLException;
    
    public abstract List<List<Object>> checkZReportSum(Map<String, List<Object>> zReportSumMap, List<List<Object>> cashRegisterList) throws ClassNotFoundException, SQLException;

    public abstract Map<String, List<Object>> readExtraCheckZReport(List<CashRegisterInfo> cashRegisterInfoList) throws ClassNotFoundException, SQLException;

    public abstract ExtraCheckZReportBatch compareExtraCheckZReport(Map<String, List<Object>> handlerZReportSumMap, Map<String, BigDecimal> baseZReportSumMap) throws ClassNotFoundException, SQLException;
}
