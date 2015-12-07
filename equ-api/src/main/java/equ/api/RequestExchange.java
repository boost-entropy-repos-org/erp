package equ.api;

import java.io.Serializable;
import java.sql.Date;
import java.util.Map;
import java.util.Set;

public class RequestExchange implements Serializable {
    public Integer requestExchange;
    public Set<Integer> cashRegisterSet;
    public String idStock;
    public Map<String, Set<String>> directoryStockMap;
    public Date dateFrom;
    public Date dateTo;
    public Date startDate;
    public String idDiscountCardFrom;
    public String idDiscountCardTo;
    private String typeRequestExchange;

    public RequestExchange(Integer requestExchange, Set<Integer> cashRegisterSet, String idStock, Map<String, Set<String>> directoryStockMap,
                           Date dateFrom, Date dateTo, Date startDate, String idDiscountCardFrom, String idDiscountCardTo, String typeRequestExchange) {
        this.requestExchange = requestExchange;
        this.cashRegisterSet = cashRegisterSet;
        this.idStock = idStock;
        this.directoryStockMap = directoryStockMap;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.startDate = startDate;
        this.idDiscountCardFrom = idDiscountCardFrom;
        this.idDiscountCardTo = idDiscountCardTo;
        this.typeRequestExchange = typeRequestExchange;
    }

    public boolean isTerminalOrderExchange() {
        return typeRequestExchange != null && typeRequestExchange.contains("terminalOrder");
    }

    public boolean isSalesInfoExchange() {
        return typeRequestExchange != null && typeRequestExchange.contains("salesInfo");
    }

    public boolean isCheckZReportExchange() {
        return typeRequestExchange != null && typeRequestExchange.contains("checkZReport");
    }

    public boolean isDiscountCard() {
        return typeRequestExchange != null && typeRequestExchange.contains("discountCard");
    }
    
    public boolean isPromotion() {
        return typeRequestExchange != null && typeRequestExchange.contains("promotion");
    }

    public boolean isCashier() {
        return typeRequestExchange != null && typeRequestExchange.contains("cashierInfo");
    }
    
}
