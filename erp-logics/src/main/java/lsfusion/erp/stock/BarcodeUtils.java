package lsfusion.erp.stock;

public class BarcodeUtils {

    public static String appendCheckDigitToBarcode(String barcode) {
        try {
            if (barcode != null && barcode.length() == 12) {     //EAN-13
                int checkSum = 0;
                for (int i = 0; i <= 10; i = i + 2) {
                    checkSum += Integer.valueOf(String.valueOf(barcode.charAt(i)));
                    checkSum += Integer.valueOf(String.valueOf(barcode.charAt(i + 1))) * 3;
                }
                checkSum %= 10;
                if (checkSum != 0)
                    checkSum = 10 - checkSum;
                return barcode.concat(String.valueOf(checkSum));
            } else if (barcode != null && barcode.length() == 7) {  //EAN-8
                int checkSum = 0;
                for (int i = 0; i <= 6; i = i + 2) {
                    checkSum += Integer.valueOf(String.valueOf(barcode.charAt(i))) * 3;
                    checkSum += i == 6 ? 0 : Integer.valueOf(String.valueOf(barcode.charAt(i + 1)));
                }
                checkSum %= 10;
                if (checkSum != 0)
                    checkSum = 10 - checkSum;
                return barcode.concat(String.valueOf(checkSum));
            } else
                return barcode;
        } catch (Exception e) {
            return barcode;
        }
    }
}