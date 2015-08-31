package equ.api.cashregister;

import equ.api.ItemInfo;
import java.math.BigDecimal;
import java.sql.Date;

public class CashRegisterItemInfo extends ItemInfo {
    public Integer itemGroupObject;
    public String description;
    public String idBrand;
    public String nameBrand;
    public String idSeason;
    public String nameSeason;
    public String idDepartmentStore;
    public String section;
    public BigDecimal minPrice;
    public String extIdItemGroup;

    public CashRegisterItemInfo(String idItem, String idBarcode, String name, BigDecimal price, boolean splitItem, Integer daysExpiry, 
                                Date expiryDate, boolean passScales, BigDecimal vat, Integer pluNumber, Integer flags,
                                String idItemGroup, String nameItemGroup, String idUOM, String shortNameUOM,
                                Integer itemGroupObject, String description, String idBrand, String nameBrand, String idSeason, String nameSeason,
                                String idDepartmentStore, String section, BigDecimal minPrice, String extIdItemGroup) {
        super(idItem, idBarcode, name, price, splitItem, daysExpiry, expiryDate, passScales, vat, pluNumber, flags, idItemGroup, nameItemGroup,
                idUOM, shortNameUOM);
        this.itemGroupObject = itemGroupObject;
        this.description = description;
        this.idBrand = idBrand;
        this.nameBrand = nameBrand;
        this.idSeason = idSeason;
        this.nameSeason = nameSeason;
        this.idDepartmentStore = idDepartmentStore;
        this.section = section;
        this.minPrice = minPrice;
        this.extIdItemGroup = extIdItemGroup;
    }
}
