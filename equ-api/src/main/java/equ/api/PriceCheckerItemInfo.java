package equ.api;

import java.math.BigDecimal;
import java.sql.Date;

public class PriceCheckerItemInfo extends ItemInfo {
    
    public PriceCheckerItemInfo(String idItem, String idBarcode, String name, BigDecimal price, boolean splitItem, Integer daysExpiry, 
                                Date expiryDate, boolean passScales, BigDecimal vat, Integer pluNumber, Integer flags,
                                String idItemGroup, String canonicalNameSkuGroup, String info) {
        super(null, idItem, idBarcode, name, price, splitItem, daysExpiry, expiryDate, passScales, vat, pluNumber, flags,
                idItemGroup, canonicalNameSkuGroup, null, null, info);
    }
}
