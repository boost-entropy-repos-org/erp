package lsfusion.erp.region.by.machinery.cashregister.fiscalvmk;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import org.apache.log4j.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static lsfusion.base.BaseUtils.trimToEmpty;

public class FiscalVMK {

    static Logger logger;
    static {
        try {
            logger = Logger.getLogger("cashRegisterLog");
            logger.setLevel(Level.INFO);
            FileAppender fileAppender = new FileAppender(new EnhancedPatternLayout("%d{DATE} %5p %c{1} - %m%n%throwable{1000}"),
                    "logs/cashregister.log");   
            logger.removeAllAppenders();
            logger.addAppender(fileAppender);
            
        } catch (Exception ignored) {
        }
    }
    
    public interface vmkDLL extends Library {

        vmkDLL vmk = (vmkDLL) Native.loadLibrary("vmk", vmkDLL.class);

        Integer vmk_lasterror();

        void vmk_errorstring(Integer error, byte[] buffer, Integer length);

        Boolean vmk_open(String comport, Integer baudrate);

        void vmk_close();

        Boolean vmk_opensmn();

        Boolean vmk_opencheck(Integer type);

        Boolean vmk_cancel();

        Boolean vmk_sale(byte[] coddigit, byte[] codname, Integer codcena, Integer ot, Double quantity,
                         Integer sum);

        Boolean vmk_discount(byte[] name, Integer value, int flag);

        Boolean vmk_discountpi(byte[] name, Integer value, int flag);

        Boolean vmk_subtotal();

        Boolean vmk_prnch(byte[] message);

        Boolean vmk_repeat();

        Boolean vmk_oplat(Integer type, Integer sum, Integer flagByte);

        Boolean vmk_xotch();

        Boolean vmk_zotch();

        Boolean vmk_feed(int type, int cnt_string, int cnt_dot_line);

        Boolean vmk_vnes(long sum);

        Boolean vmk_vyd(long sum);

        Boolean vmk_opendrawer(int cnt_msek);

        Boolean vmk_indik(byte[] firstLine, byte[] secondLine);

        Boolean vmk_indik2(byte[] firstLine);

        Boolean vmk_ksastat(ByReference rej, ByReference stat);

        Boolean vmk_ksainfo(byte[] buffer, int buflen);
    }

    static void init() {

        try {
            System.loadLibrary("vmk");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }

    public static String getError(boolean closePort) {
        logAction("vmk_lasterror");
        Integer lastError = vmkDLL.vmk.vmk_lasterror();
        int length = 255;
        byte[] lastErrorText = new byte[length];
        logAction("vmk_errorstring");
        vmkDLL.vmk.vmk_errorstring(lastError, lastErrorText, length);
        if (closePort)
            closePort();
        return Native.toString(lastErrorText, "cp1251");
    }

    public static void openPort(String ip, int comPort, int baudRate) {
        logAction("vmk_open", ip != null ? ip : ("COM" + comPort), ip != null ? comPort : baudRate);
        if (!vmkDLL.vmk.vmk_open(ip != null ? ip : ("COM" + comPort), ip != null ? comPort : baudRate))
            checkErrors(true);
    }

    public static void closePort() {
        logAction("vmk_close");
        vmkDLL.vmk.vmk_close();
    }

    public static boolean openReceipt(int type) {    //0 - продажа, 1 - возврат
        logAction("vmk_opencheck", type);
        return vmkDLL.vmk.vmk_opencheck(type);
    }

    public static boolean cancelReceipt() {
        logAction("vmk_cancel");
        return vmkDLL.vmk.vmk_cancel();
    }

    public static boolean getFiscalClosureStatus() {
        IntByReference rej = new IntByReference();
        IntByReference stat = new IntByReference();
        logAction("vmk_ksastat");
        if (!vmkDLL.vmk.vmk_ksastat(rej, stat))
            return false;
        if (BigInteger.valueOf(stat.getValue()).testBit(14))
            if (!cancelReceipt())
                return false;
        return true;
    }

    public static boolean printFiscalText(String msg) {
        try {
        if(msg != null && !msg.isEmpty()) {
            for(String line : msg.split("\n")) {
                logAction("vmk_prnch", line);
                boolean result = vmkDLL.vmk.vmk_prnch((line + "\0").getBytes("cp1251"));
                if(!result) return false;
            }
        }
        return true;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace(); 
            return false;
        }
    }

    public static boolean repeatReceipt() {
        logAction("vmk_repeat");
        return vmkDLL.vmk.vmk_repeat();
    }

    public static boolean totalCash(BigDecimal sum) {
        if (sum == null)
            return true;
        logAction("vmk_oplat", 0, Math.abs(sum.intValue()), 0);
        return vmkDLL.vmk.vmk_oplat(0, Math.abs(sum.intValue()), 0/*"00000000"*/);
    }

    public static boolean totalCard(BigDecimal sum) {
        if (sum == null)
            return true;
        logAction("vmk_oplat", 1, Math.abs(sum.intValue()), 0);
        return vmkDLL.vmk.vmk_oplat(1, Math.abs(sum.intValue()), 0/*"00000000"*/);
    }

    public static boolean totalGiftCard(BigDecimal sum) {
        if (sum == null)
            return true;
        logAction("vmk_oplat", 2, Math.abs(sum.intValue()), 0);
        return vmkDLL.vmk.vmk_oplat(2, Math.abs(sum.intValue()), 0/*"00000000"*/);
    }

    public static boolean total(BigDecimal sumPayment, Integer typePayment) {
        logAction("vmk_oplat", typePayment, Math.abs(sumPayment.intValue()), 0);
        if (!vmkDLL.vmk.vmk_oplat(typePayment, Math.abs(sumPayment.intValue()), 0/*"00000000"*/))
            return false;

        return true;
    }

    public static void xReport() {
        logAction("vmk_xotch");
        if (!vmkDLL.vmk.vmk_xotch())
            checkErrors(true);
    }

    public static void zReport() {
        logAction("vmk_zotch");
        if (!vmkDLL.vmk.vmk_zotch())
            checkErrors(true);
    }

    public static void advancePaper(int lines) {
        logAction("vmk_feed", 1, lines, 1);
        if (!vmkDLL.vmk.vmk_feed(1, lines, 1))
            checkErrors(true);
    }

    public static boolean inOut(Long sum) {

        if (sum > 0) {
            logAction("vmk_vnes", sum);
            if (!vmkDLL.vmk.vmk_vnes(sum))
                checkErrors(true);
        } else {
            logAction("vmk_vyd", -sum);
            if (!vmkDLL.vmk.vmk_vyd(-sum))
                return false;
        }
        return true;
    }

    public static boolean openDrawer() {
        logAction("vmk_opendrawer");
        return vmkDLL.vmk.vmk_opendrawer(0);
    }

    public static void displayText(ReceiptItem item) {
        try {
            String firstLine = " " + toStr(item.quantity) + "x" + String.valueOf(item.price);
            int length = 16 - Math.min(16, firstLine.length());
            firstLine = item.name.substring(0, Math.min(length, item.name.length())) + firstLine;
            String secondLine = String.valueOf(item.sumPos);
            while (secondLine.length() < 11)
                secondLine = " " + secondLine;
            secondLine = "ИТОГ:" + secondLine;
            logAction("vmk_indik", firstLine, secondLine);
            if(!vmkDLL.vmk.vmk_indik2((secondLine + "\0").getBytes("cp1251")))
                checkErrors(true);
            if (!vmkDLL.vmk.vmk_indik((firstLine + "\0").getBytes("cp1251"), (secondLine + "\0").getBytes("cp1251")))
                checkErrors(true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public static boolean registerItem(ReceiptItem item) {
        try {
            logAction("vmk_sale", item.barcode, item.name, (int) Math.abs(item.price), item.isGiftCard ? 2 : 1 /*отдел*/, item.quantity, 0);
            return vmkDLL.vmk.vmk_sale((item.barcode + "\0").getBytes("cp1251"), (item.name + "\0").getBytes("cp1251"), (int) Math.abs(item.price), item.isGiftCard ? 2 : 1 /*отдел*/, item.quantity, (int) (item.sumPos - item.articleDiscSum));
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    public static boolean registerItemPayment(long sumPayment) {
        try {
            logAction("vmk_sale", "", "ОПЛАТА", (int) Math.abs(sumPayment), 1 /*отдел*/, 1, 0);
            return vmkDLL.vmk.vmk_sale("\0".getBytes("cp1251"), ("ОПЛАТА" + "\0").getBytes("cp1251"), (int) Math.abs(sumPayment), 1 /*отдел*/, 1.0, 0);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }
    
    public static boolean discountItem(ReceiptItem item) {
        if (item.articleDiscSum == 0)
            return true;
        boolean discount = item.articleDiscSum < 0;
        try {
            logAction("vmk_discount", discount ? "Скидка" : "Наценка", (int) Math.abs(item.articleDiscSum), discount ? 3 : 1);
            return vmkDLL.vmk.vmk_discount(((discount ? "Скидка" : "Наценка") + "\0").getBytes("cp1251"), (int) Math.abs(item.articleDiscSum), discount ? 3 : 1);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    public static boolean discountReceipt(ReceiptInstance receipt) {
        if (receipt.sumDisc == null)
            return true;
        boolean discount = receipt.sumDisc.compareTo(BigDecimal.ZERO) < 0;
        try {
            logAction("vmk_discountpi", discount ? "Скидка" : "Наценка", (int) Math.abs(receipt.sumDisc.doubleValue()), discount ? 3 : 1);
            return vmkDLL.vmk.vmk_discountpi(((discount ? "Скидка" : "Наценка") + "\0").getBytes("cp1251"), (int) Math.abs(receipt.sumDisc.doubleValue()), discount ? 3 : 1);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }
    
    public static boolean subtotal() {
        logAction("vmk_subtotal");
        if (!vmkDLL.vmk.vmk_subtotal())
            return false;
        return true;
    }

    public static void opensmIfClose() {
        IntByReference rej = new IntByReference();
        IntByReference stat = new IntByReference();
        logAction("vmk_ksastat");
        if (!vmkDLL.vmk.vmk_ksastat(rej, stat))
            checkErrors(true);
        if (!BigInteger.valueOf(stat.getValue()).testBit(15)) {//15 - открыта ли смена 
            logAction("vmk_opensmn");
            if (!vmkDLL.vmk.vmk_opensmn())
                checkErrors(true);
        }
    }

    private static String toStr(double value) {
        boolean isInt = (value - (int) value) == 0;
        return isInt ? String.valueOf((int) value) : String.valueOf(value);
    }

    public static int checkErrors(Boolean throwException) {
        logAction("vmk_lasterror");
        Integer lastError = vmkDLL.vmk.vmk_lasterror();
        if (lastError != 0) {
            if (throwException)
                throw new RuntimeException("VMK Exception: " + lastError);
        }
        return lastError;
    }

    public static int getReceiptNumber(Boolean throwException) {
        byte[] buffer = new byte[50];
        logAction("vmk_ksainfo");
        if(!vmkDLL.vmk.vmk_ksainfo(buffer, 50))
            checkErrors(throwException);
        String result = Native.toString(buffer, "cp1251");
        return Integer.parseInt(result.split(",")[0]);
    }

    public static int getZReportNumber(Boolean throwException) {
        byte[] buffer = new byte[50];
        logAction("vmk_ksainfo");
        if(!vmkDLL.vmk.vmk_ksainfo(buffer, 50))
            checkErrors(throwException);
        String result = Native.toString(buffer, "cp1251");
        return Integer.parseInt(result.split(",")[1]);
    }

    public static long getCashSum(Boolean throwException) {
        byte[] buffer = new byte[50];
        logAction("vmk_ksainfo");
        if(!vmkDLL.vmk.vmk_ksainfo(buffer, 50))
            checkErrors(throwException);
        String result = Native.toString(buffer, "cp1251");
        return Long.parseLong(result.split(",")[2]);
    }

    public static void logReceipt(ReceiptInstance receipt, Integer numberReceipt) {
        OutputStreamWriter sw = null;
        try {
            sw = new OutputStreamWriter(new FileOutputStream(new File("logs/vmk.txt"), true), "UTF-8");
            String dateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
            for(ReceiptItem item : receipt.receiptSaleList) {
                sw.write(String.format("%s|%s|1|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s\r\n", dateTime, numberReceipt,
                        trimToEmpty(item.barcode), item.name, item.price, item.quantity, item.sumPos, item.articleDiscSum,
                        item.isGiftCard ? "1" : "0", trim(receipt.sumDisc), trim(receipt.sumCard), trim(receipt.sumCash),
                        trim(receipt.sumGiftCard), trim(receipt.sumTotal)));
            }
            for(ReceiptItem item : receipt.receiptReturnList) {
                sw.write(String.format("%s|%s|2|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s\r\n", dateTime, numberReceipt,
                        trimToEmpty(item.barcode), item.name, item.price, item.quantity, item.sumPos, item.articleDiscSum,
                        item.isGiftCard ? "1" : "0", trim(receipt.sumDisc), trim(receipt.sumCard), trim(receipt.sumCash),
                        trim(receipt.sumGiftCard), trim(receipt.sumTotal)));
            }
        } catch (IOException e) {
            logger.error("FiscalVMK Error: ", e);
        } finally {
            if (sw != null) {
                try {
                    sw.flush();
                    sw.close();
                } catch (IOException e) {
                    logger.error("FiscalVMK Error: ", e);
                }
            }
        }
    }

    private static void logAction(Object... actionParams) {
        String pattern = "";
        for(Object param : actionParams)
            pattern += "%s;";
        logger.info(String.format(pattern, actionParams));
    }

    private static String trim(BigDecimal value) {
        return value == null ? "" : String.valueOf(value);
    }
}

