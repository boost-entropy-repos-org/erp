package lsfusion.erp.region.by.machinery.cashregister.fiscalabsolut;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static lsfusion.base.BaseUtils.trimToEmpty;

public class FiscalAbsolut {

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
    
    public interface absolutDLL extends Library {

        absolutDLL absolut = (absolutDLL) Native.loadLibrary("absolut", absolutDLL.class);

        Boolean Open(String port, int baud, int oper, int pwd);

        void Close();

        int LastError();

        void ErrorString(int error, byte[] buffer, int buflen);

        Boolean SmenBegin();

        Boolean BegChk();

        Boolean BegReturn();

        Boolean EndChk();

        Boolean CopyChk();

        Boolean InOut(int id, double sum);

        Boolean OutTone(int duration, int freq);

        Boolean OutScr(int row, byte[] scr);

        Boolean TextComment(byte[] comment);

        Boolean PrintComment(byte[] comment);

        Boolean FullProd(String plu, double price, double quant,
                         int dep, int group, int tax, byte[] naim);

        Boolean Prod(String plu, double price, double quant, int dep,
                     int group);

        Boolean NacSkd(int id, double sum, double prc);

        Boolean Oplata(int id, double sum, long code);

        Boolean Subtotal();

        Boolean VoidChk();

        Boolean VoidLast();

        Boolean VoidProd(String plu);

        Boolean PrintReport(int id);

        Boolean OpenComment();

        Boolean CloseComment();

        Boolean PrintComment(String comment);

        void GetFactoryNumber(byte[] buffer, int buflen);

        Boolean PrintBarCode(int typ, byte[] cod, int width, int height, int feed);
    }

    public static String getError(boolean closePort) {
        logAction("LastError");
        Integer lastError = absolutDLL.absolut.LastError();
        int length = 255;
        byte[] lastErrorText = new byte[length];
        logAction("ErrorString");
        absolutDLL.absolut.ErrorString(lastError, lastErrorText, length);
        if (closePort)
            closePort();
        return Native.toString(lastErrorText, "cp1251");
    }

    public static void openPort(int comPort, int baudRate) {
        logAction("Open", "COM" + comPort, baudRate, 1, 1111);
        if (!absolutDLL.absolut.Open("COM" + comPort, baudRate, 1, 1111))
            checkErrors(true);
    }

    public static void closePort() {
        logAction("Close");
        absolutDLL.absolut.Close();
    }

    static boolean openReceipt(boolean sale) {    //0 - продажа, 1 - возврат {
        logAction("BegChk");
        boolean result = absolutDLL.absolut.BegChk();
        if (result && !sale)
            return absolutDLL.absolut.BegReturn();
        else return result;
    }

    public static boolean closeReceipt() {
        logAction("EndChk");
        return absolutDLL.absolut.EndChk();
    }

    public static boolean cancelReceipt() {
        logAction("VoidChk");
        return absolutDLL.absolut.VoidChk();
    }

    static void simpleLogAction(String msg) {
        logger.info(msg);
    }

    public static boolean printMultilineFiscalText(String msg) {
        if (msg != null && !msg.isEmpty()) {
            int start = 0;
            while (start < msg.length()) {
                int end = Math.min(start + 30, msg.length());
                if (!printFiscalText(msg.substring(start, end)))
                    return false;
                start = end;
            }
        }
        return true;
    }

    static boolean printBarcode(String barcode) {
        try {
            if(barcode != null)
                FiscalAbsolut.absolutDLL.absolut.PrintBarCode(1, FiscalAbsolut.getBytes(barcode), 2, 40, 5);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    static boolean printFiscalText(String msg) {
        try {
            if (msg != null && !msg.isEmpty()) {
                for (String line : msg.split("\n")) {
                    boolean result = printComment(line, false);
                    if (!result) return false;
                }
            }
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    private static boolean printComment(String comment, boolean saveCommentOnFiscalTape) throws UnsupportedEncodingException {
        logAction(saveCommentOnFiscalTape ? "TextComment" : "PrintComment", comment);
        return saveCommentOnFiscalTape ?
                absolutDLL.absolut.TextComment(getBytes(comment)) : absolutDLL.absolut.PrintComment(getBytes(comment));
    }

    static boolean repeatReceipt() {
        logAction("Copychk");
        return absolutDLL.absolut.CopyChk();
    }

    static boolean totalCash(BigDecimal sum) {
        if (sum == null)
            return true;
        double sumValue = formatAbsPrice(sum);
        logAction("Oplata", 0, sumValue, 0);
        return absolutDLL.absolut.Oplata(0, sumValue, 0);
    }

    static boolean totalCard(BigDecimal sum) {
        if (sum == null)
            return true;
        double sumValue = formatAbsPrice(sum);
        logAction("Oplata", 3, sumValue, 0);
        return absolutDLL.absolut.Oplata(3, sumValue, 0);
    }

    static boolean totalGiftCard(BigDecimal sum) {
        if (sum == null)
            return true;
        int sumValue = formatAbsPrice(sum);
        logAction("Oplata", 1, sumValue, 0);
        return absolutDLL.absolut.Oplata(1, (double) sumValue, 0);
    }

    public static boolean total(BigDecimal sumPayment, Integer typePayment) {
        int sumPaymentValue = formatAbsPrice(sumPayment);
        logAction("Oplata", typePayment, sumPaymentValue, 0);
        if (!absolutDLL.absolut.Oplata(typePayment, sumPaymentValue, 0))
            return false;

        return true;
    }

    public static void xReport() {
        logAction("PrintReport", 10);
        absolutDLL.absolut.PrintReport(10);
    }

    public static void zReport(int type) {
        logAction("PrintReport", type);
        absolutDLL.absolut.PrintReport(type);
    }

    public static boolean inOut(BigDecimal sum) {
        double sumValue = formatPrice(sum);
        if (sumValue > 0) {
            logAction("InOut", sumValue);
            if (!absolutDLL.absolut.InOut(0, sumValue))
                checkErrors(true);
        } else {
            logAction("InOut", -sumValue);
            if (!absolutDLL.absolut.InOut(0, sumValue))
                return false;
        }
        return true;
    }

    static void displayText(ReceiptItem item) {
        try {
            String firstLine = " " + toStr(item.quantity) + "x" + toStr(item.price);
            int length = 16 - Math.min(16, firstLine.length());
            firstLine = item.name.substring(0, Math.min(length, item.name.length())) + firstLine;
            String secondLine = String.valueOf(item.sumPos);
            while (secondLine.length() < 11)
                secondLine = " " + secondLine;
            secondLine = "ИТОГ:" + secondLine;
            logAction("Outscr", firstLine, secondLine);
            if (!absolutDLL.absolut.OutScr(0, getBytes(firstLine)))
                checkErrors(true);
            if (!absolutDLL.absolut.OutScr(1, getBytes(secondLine)))
                checkErrors(true);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static boolean registerAndDiscountItem(BigDecimal sum, BigDecimal discSum) {
        try {
            logAction("FullProd", "11110", sum, 1, 1, 0, 0, "");
            if (absolutDLL.absolut.FullProd("11110", formatPrice(sum), 1, 1, 1, 0, getBytes(""))) {
                double discountSum = formatPrice(discSum);
                if (discountSum != 0.0) {
                    boolean discount = discountSum < 0;
                    logAction("NacSkd", discount ? 0 : 1, Math.abs(discountSum), 0);
                    return absolutDLL.absolut.NacSkd(discount ? 0 : 1, Math.abs(discountSum), 0);
                } else return true;
            } else return false;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    static boolean registerItem(ReceiptItem item, boolean saveCommentOnFiscalTape, boolean groupPaymentsByVAT) {
        try {
            double price = formatAbsPrice(item.price);
            if(item.barcode != null)
                printComment(item.barcode, saveCommentOnFiscalTape);
            for(String line : splitName(item.name))
                printComment(line, saveCommentOnFiscalTape);
            int tax = groupPaymentsByVAT ? (item.valueVAT == 20.0 ? 1 : item.valueVAT == 10.0 ? 2 : item.valueVAT == 0.0 ? 3 : 0) : 0;
            String plu = groupPaymentsByVAT ? (item.valueVAT == 20.0 ? "11111" : item.valueVAT == 10.0 ? "11112" : item.valueVAT == 0.0 ? "11113" : "11110") : "11110";
            logAction("FullProd", plu, price, item.quantity, item.isGiftCard ? 2 : 1, 0, tax, "");
            return absolutDLL.absolut.FullProd(plu, price, item.quantity, item.isGiftCard ? 2 : 1, 1, tax, getBytes(""));
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    static boolean registerItemPayment(BigDecimal sumPayment, boolean saveCommentOnFiscalTape) {
        try {
            double sum = formatPrice(sumPayment);
            printComment("ОПЛАТА", saveCommentOnFiscalTape);
            logAction("Prod", "", sum, 1.0, 1, 1);
            return absolutDLL.absolut.Prod("", sum, 1.0, 1, 1);
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }
    
    static boolean discountItem(ReceiptItem item, String numberDiscountCard) {
        double discSum = formatPrice(item.articleDiscSum - item.bonusPaid); //articleDiscSum is negative, bonusPaid is positive
        if (discSum == 0)
            return true;
        boolean discount = discSum < 0;
        logAction("NacSkd", discount ? 0 : 1, Math.abs(discSum), 0, "discountCard: " + numberDiscountCard);
        return absolutDLL.absolut.NacSkd(discount ? 0 : 1, Math.abs(discSum), 0);
    }

    static boolean discountReceipt(ReceiptInstance receipt) {
        if (receipt.sumDisc == null)
            return true;
        boolean discount = receipt.sumDisc.compareTo(BigDecimal.ZERO) < 0;
        double sumDisc = formatAbsPrice(receipt.sumDisc);
        logAction("NacSkd", discount ? "Скидка" : "Наценка", sumDisc, discount ? 3 : 1, "discountCard: " + receipt.numberDiscountCard);
        return absolutDLL.absolut.NacSkd(discount ? 4 :5, sumDisc, 0);
    }
    
    static boolean subtotal() {
        logAction("Subtotal");
        if (!absolutDLL.absolut.Subtotal())
            return false;
        return true;
    }

    static void smenBegin() {
        logAction("SmenBegin");
        if (!absolutDLL.absolut.SmenBegin())
            checkErrors(true);
    }

    static boolean zeroReceipt() {
        try {
            smenBegin();
            if(!openReceipt(true))
                return false;
            logAction("FullProd", "11110", 0, 1, 1, 0, 0, "");
            if(!absolutDLL.absolut.FullProd("11110", 0, 1, 1, 1, 0, getBytes("")))
                return false;
            if (!subtotal())
                return false;
            if(!totalCash(BigDecimal.ZERO))
                return false;
            return closeReceipt();
        }catch (Exception e){
            FiscalAbsolut.cancelReceipt();
            return false;
        }
    }

    private static String toStr(double value) {
        boolean isInt = (value - (int) value) == 0;
        return isInt ? String.valueOf((int) value) : String.valueOf(value);
    }

    public static String toStr(BigDecimal value) {
        String result = null;
        if (value != null) {
            value = value.setScale(2, BigDecimal.ROUND_HALF_UP);
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(2);
            df.setMinimumFractionDigits(2);
            df.setGroupingUsed(false);
            result = df.format(value).replace(",", ".");
        }
        return result;
    }

    public static int checkErrors(Boolean throwException) {
        logAction("LastError");
        Integer lastError = absolutDLL.absolut.LastError();
        if (lastError != 0) {
            if (throwException)
                throw new RuntimeException("Absolut Exception: " + lastError);
        }
        return lastError;
    }

    public static int getReceiptNumber(Boolean throwException) {
        byte[] buffer = new byte[50];
        String result = Native.toString(buffer, "cp1251");
        return Integer.parseInt(result.split(",")[0]);
    }

    public static void logReceipt(ReceiptInstance receipt, Integer numberReceipt) {
        OutputStreamWriter sw = null;
        try {

            sw = new OutputStreamWriter(new FileOutputStream(new File("logs/absolut.txt"), true), "UTF-8");
            String dateTime = new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
            for(ReceiptItem item : receipt.receiptSaleList) {
                sw.write(String.format("%s|%s|1|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s\r\n", dateTime, numberReceipt,
                        trimToEmpty(item.barcode), item.name, toStr(item.price), item.quantity, item.sumPos, item.articleDiscSum,
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
            logger.error("FiscalAbsolut Error: ", e);
        } finally {
            if (sw != null) {
                try {
                    sw.flush();
                    sw.close();
                } catch (IOException e) {
                    logger.error("FiscalAbsolut Error: ", e);
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

    private static byte[] getBytes(String value) throws UnsupportedEncodingException {
        return (value + "\0").getBytes("cp1251");
    }

    private static int formatAbsPrice(BigDecimal value) {
        return value == null ? 0 : (value.abs().multiply(new BigDecimal(100)).intValue());
    }

    private static int formatPrice(BigDecimal value) {
        return value == null ? 0 : value.multiply(new BigDecimal(100)).intValue();
    }

    private static double formatPrice(double value) {
        return value * 100;
    }

    private static List<String> splitName(String value) {
        List<String> result = new ArrayList<>();
        if(value != null) {
            while (value.length() > 30) {
                result.add(value.substring(0, 30));
                value = value.substring(30);
            }
            result.add(value);
        }
        return result;
    }
}

