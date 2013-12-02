package lsfusion.erp.integration.universal;


import java.math.BigDecimal;
import java.sql.Date;

public class SaleOrderDetail {
    public Boolean isPosted;
    public String numberOrder;
    public Date dateOrder;
    public String idOrderDetail;
    public String idBarcodeSku;
    public String idBatch;
    public Integer dataIndex;
    public String idItem;
    public String idManufacturer;
    public String idCustomer;
    public String idCustomerStock;
    public BigDecimal quantity;
    public BigDecimal price;
    public BigDecimal sum;
    public BigDecimal valueVAT;
    public BigDecimal sumVAT;
    public BigDecimal invoiceSum;
    public BigDecimal manufacturingPrice;

    public SaleOrderDetail(Boolean isPosted, String numberOrder, Date dateOrder, String idOrderDetail, String idBarcodeSku, String idBatch, 
                           Integer dataIndex, String idItem, String idManufacturer, String idCustomer, String idCustomerStock, 
                           BigDecimal quantity, BigDecimal price, BigDecimal sum, BigDecimal valueVAT, BigDecimal sumVAT, 
                           BigDecimal invoiceSum, BigDecimal manufacturingPrice) {
        this.isPosted = isPosted;
        this.numberOrder = numberOrder;
        this.dateOrder = dateOrder;
        this.idOrderDetail = idOrderDetail;
        this.idBarcodeSku = idBarcodeSku;
        this.idBatch = idBatch;
        this.dataIndex = dataIndex;
        this.idItem = idItem;
        this.idManufacturer = idManufacturer;
        this.idCustomer = idCustomer;
        this.idCustomerStock = idCustomerStock;
        this.quantity = quantity;
        this.price = price;
        this.sum = sum;
        this.valueVAT = valueVAT;
        this.sumVAT = sumVAT;
        this.invoiceSum = invoiceSum;
        this.manufacturingPrice = manufacturingPrice;
    }
}
