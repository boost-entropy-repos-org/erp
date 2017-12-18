package equ.clt.handler.digi;

import equ.api.MachineryInfo;
import equ.api.SendTransactionBatch;
import equ.api.scales.ScalesInfo;
import equ.api.scales.ScalesItemInfo;
import equ.api.scales.TransactionScalesInfo;
import equ.clt.EquipmentServer;
import org.apache.log4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import javax.naming.CommunicationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static equ.clt.handler.HandlerUtils.safeMultiply;

public class DigiSM120Handler extends DigiHandler {

    private final static Logger processTransactionLogger = Logger.getLogger("TransactionLogger");

    private static String separator = ",";

    public DigiSM120Handler(FileSystemXmlApplicationContext springContext) {
        super(springContext);
    }

    protected String getLogPrefix() {
        return "Digi SM120: ";
    }

    @Override
    public Map<Long, SendTransactionBatch> sendTransaction(List<TransactionScalesInfo> transactionList) throws IOException {

        Map<Long, SendTransactionBatch> sendTransactionBatchMap = new HashMap<>();

        Map<String, String> brokenPortsMap = new HashMap<>();
        if(transactionList.isEmpty()) {
            processTransactionLogger.error(getLogPrefix() + "Empty transaction list!");
        }
        for(TransactionScalesInfo transaction : transactionList) {
            processTransactionLogger.info(getLogPrefix() + "Send Transaction # " + transaction.id);

            DigiSM120Settings digiSettings = springContext.containsBean("digiSM120Settings") ? (DigiSM120Settings) springContext.getBean("digiSM120Settings") : null;
            Integer nameLineFont = digiSettings != null ? digiSettings.getNameLineFont() : null;
            if (nameLineFont == null)
                nameLineFont = 9;
            Integer nameLineLength = digiSettings != null ? digiSettings.getNameLineLength() : null;
            if (nameLineLength == null)
                nameLineLength = 22;
            Integer descriptionLineFont = digiSettings != null ? digiSettings.getDescriptionLineFont() : null;
            if (descriptionLineFont == null)
                descriptionLineFont = 2;
            Integer descriptionLineLength = digiSettings != null ? digiSettings.getDescriptionLineLength() : null;
            if (descriptionLineLength == null)
                descriptionLineLength = 56;

            List<MachineryInfo> succeededScalesList = new ArrayList<>();
            List<MachineryInfo> clearedScalesList = new ArrayList<>();
            Exception exception = null;
            try {

                if (!transaction.machineryInfoList.isEmpty()) {

                    List<ScalesInfo> enabledScalesList = getEnabledScalesList(transaction, succeededScalesList);
                    Map<String, List<String>> errors = new HashMap<>();
                    Set<String> ips = new HashSet<>();

                    processTransactionLogger.info(getLogPrefix() + "Starting sending to " + enabledScalesList.size() + " scales...");
                    Collection<Callable<SendTransactionResult>> taskList = new LinkedList<>();
                    for (ScalesInfo scales : enabledScalesList) {
                        if (scales.port != null) {
                            String brokenPortError = brokenPortsMap.get(scales.port);
                            if(brokenPortError != null) {
                                errors.put(scales.port, Collections.singletonList(String.format("Broken ip: %s, error: %s", scales.port, brokenPortError)));
                            } else {
                                ips.add(scales.port);
                                taskList.add(new SendTransactionTask(transaction, scales, nameLineFont, nameLineLength, descriptionLineFont, descriptionLineLength));
                            }
                        }
                    }

                    if(!taskList.isEmpty()) {
                        ExecutorService singleTransactionExecutor = EquipmentServer.getFixedThreadPool(taskList.size(), "DigiSendTransaction");
                        List<Future<SendTransactionResult>> threadResults = singleTransactionExecutor.invokeAll(taskList);
                        for (Future<SendTransactionResult> threadResult : threadResults) {
                            if(threadResult.get().localErrors.isEmpty())
                                succeededScalesList.add(threadResult.get().scalesInfo);
                            else {
                                brokenPortsMap.put(threadResult.get().scalesInfo.port, threadResult.get().localErrors.get(0));
                                errors.put(threadResult.get().scalesInfo.port, threadResult.get().localErrors);
                            }
                            if(threadResult.get().cleared)
                                clearedScalesList.add(threadResult.get().scalesInfo);
                        }
                        singleTransactionExecutor.shutdown();
                    }
                    if(!enabledScalesList.isEmpty())
                        errorMessages(errors, ips, brokenPortsMap);

                }
            } catch (Exception e) {
                exception = e;
            }
            sendTransactionBatchMap.put(transaction.id, new SendTransactionBatch(clearedScalesList, succeededScalesList, exception));
        }
        return sendTransactionBatchMap;
    }

    private byte[] makePLURecord(ScalesItemInfo item, Integer plu, Integer nameLineFont, Integer nameLineLength) throws IOException {
        String pluNumber = fillLeadingZeroes(plu, 6);
        int flagForDelete = 0; //No data/0: Add or Change, 1: Delete
        //временно весовой товар определяется как в старых Digi
        //int isWeight = item.splitItem ? 0 : 1; //0: Weighed item   1: Non-weighed item
        int isWeight = item.shortNameUOM != null && item.shortNameUOM.toUpperCase().startsWith("ШТ") ? 1 : 0;
        String price = getPrice(item.price); //max 9999.99
        String labelFormat1 = "017";
        String labelFormat2 = "0";
        String barcodeFormat = "1"; //F1F2 CCCCC XCD XXXX CD
        String barcodeFlagOfEANData = "22"; //weight prefix

        String itemCodeOfEANData = pluNumber + "0000"; //6-digit Item code + 4-digit Expanded item code
        String extendItemCodeOfEANData = "";
        String barcodeTypeOfEANData = "0"; //0: EAN 9: ITF
        String rightSideDataOfEANData = "1"; //0: Price 1: Weight 2: QTY 3: Original price 4: Weight/QTY 5: U.P. 6: U.P. after discount

        String cellByDate = fillLeadingZeroes(item.daysExpiry == null ? 0 : item.daysExpiry, 3); //days
        String cellByTime = fillLeadingZeroes(item.hoursExpiry == null ? 0 : item.hoursExpiry, 2) + "00";//HHmm

        String quantity = "0001";
        String quantitySymbol = "00"; //0 No print, 1 PCS, 2 FOR, 3 kg, 4 lb, 5 g, 6 oz

        String stepDiscountStartDate = "000000"; //YY MM DD
        String stepDiscountStartTime = "0000"; //HH MM
        String stepDiscountEndDate = "000000"; //YY MM DD
        String stepDiscountEndTime = "0000"; //HH MM

        String stepDiscountPoint1 = "000000"; //Weight or Quantity
        String stepDiscountValue1 = getPrice(item.price); //Price value or Percent value
        String stepDiscountPoint2 = "99999"; //Weight or Quantity
        String stepDiscountValue2 = getPrice(item.price); //Price value or Percent value
        String stepDiscountType = "02"; //"0: No step discount, 1: Free item, 2: Unit price discount, 3: Unit price % discount, 4: Total price discount, 5: Total price % discount, 6: Fixed price discount, 11: U.P./PCS - U.P./kg
        String typeOfMarkdown = "0"; //"0: No markdown, 1: Unit price markdown, 2: Price markdown, 3: Unit price and price markdown

        byte[] dataBytes = getBytes(pluNumber + separator + flagForDelete + separator + isWeight + separator + "0" + separator +
                "1" + separator + "1" + separator + "1" + separator + "1" + separator + "1" + separator  + "0" + separator +
                "0" + separator + "0" + separator + "0" + separator + "0" + separator + "0" + separator + "0" + separator +
                "0" + separator + "0" + separator + price + separator + labelFormat1 + separator + labelFormat2 + separator +
                barcodeFormat + separator + barcodeFlagOfEANData + separator + itemCodeOfEANData + separator + extendItemCodeOfEANData + separator +
                barcodeTypeOfEANData + separator + rightSideDataOfEANData + separator + "000000" + separator + "000000" + separator +
                cellByDate + separator + cellByTime + separator + cellByDate + separator + "000" + separator + "0000" + separator +
                "000000" + separator + "0000" + separator + quantity + separator + quantitySymbol + separator + "0" + separator +
                "0000" + separator + "00" + separator + "00" + separator + "00" + separator + "00" + separator + "00" + separator +
                "00" + separator + "00" + separator + "00" + separator + "00" + separator + "00" + separator + pluNumber + separator +
                pluNumber + separator + "000000" + separator + "000000" + separator + "000000" + separator + "000000" + separator +
                "000000" + separator + "000000" + separator + "000000" + separator + "000000" + separator + "000000" + separator +
                "000000" + separator + "000000" + separator + "000000" + separator + "000000" + separator + "000000" + separator +
                "000000" + separator + "000000" + separator + "000000" + separator + "000000" + separator + "000000" + separator +
                "000000" + separator + "000000" + separator + "000000" + separator + stepDiscountStartDate + separator + stepDiscountStartTime + separator +
                stepDiscountEndDate + separator + stepDiscountEndTime + separator + stepDiscountPoint1 + separator + stepDiscountValue1 + separator +
                stepDiscountPoint2 + separator + stepDiscountValue2 + separator + stepDiscountType + separator + typeOfMarkdown + separator +
                "0" + separator + "0" + separator + "0" + separator + "0" + separator + "0" + separator + "0" + separator +
                "0" + separator + "0" + separator + "000000" + separator + "000000" + separator + "000000" + separator +
                "000000" + separator + "000000" + separator + "00000000" + separator + "00000000" + separator + "00000" + separator +
                "000000" + separator + getNameLines(item.name, nameLineFont, nameLineLength) + separator + "000000" + separator + "000000" + separator + "0" + separator + "000000" + separator +
                "000000" + separator + "00" + separator + "00");

        int totalSize = dataBytes.length + 8;
        ByteBuffer bytes = ByteBuffer.allocate(totalSize);
        bytes.putInt(plu); //4 bytes
        bytes.putShort((short) totalSize); //2 bytes
        bytes.put(dataBytes);
        bytes.put(new byte[] {0x0d, 0x0a});

        return bytes.array();
    }

    private byte[] makeIngredientRecord(Integer plu, String lineData, Integer lineNumber, int flagForDelete, int fontSize) throws IOException {

        byte[] dataBytes = getBytes(plu + separator + lineNumber + separator + flagForDelete + separator + fontSize + separator + lineData);

        int totalSize = dataBytes.length + 8;
        ByteBuffer bytes = ByteBuffer.allocate(totalSize);

        //bytes.put(plu >>> 24); //first byte
        bytes.put((byte) (plu >>> 16)); //second byte
        bytes.put((byte) (plu >>> 8)); //third byte
        bytes.put(plu.byteValue()); //fourth byte
        bytes.put(lineNumber.byteValue()); // line number
        bytes.putShort((short) totalSize); //2 bytes
        bytes.put(dataBytes);
        bytes.put(new byte[]{0x0d, 0x0a});

        return bytes.array();
    }

    private byte[] makeKeyAssignmentRecord(Integer plu) throws IOException {
        String pluNumber = fillLeadingZeroes(plu, 6);
        int flagForDelete = 0; //No data/0: Add or Change, 1: Delete

        //хотя по документации можно до 256 товаров на каждой из 3 страниц, но кнопок на табло только 56
        if(plu > 0 && plu <= 56) {
            Integer pageNumber = 0;
            byte[] dataBytes = getBytes(pageNumber + separator + pluNumber + separator + flagForDelete + separator + pluNumber + separator +
                    "0" + separator + pluNumber);

            int totalSize = dataBytes.length + 8;
            ByteBuffer bytes = ByteBuffer.allocate(totalSize);
            bytes.putInt(plu); //4 bytes
            bytes.putShort((short) totalSize); //2 bytes
            bytes.put(dataBytes);
            bytes.put(new byte[]{0x0d, 0x0a});

            return bytes.array();
        } else return null;
    }

    private String getPrice(BigDecimal price) {
        return fillLeadingZeroes(safeMultiply(price, 100).intValue(), 6);
    }

    private String getNameLines(String name, Integer lineFont, Integer lineLength) {
        String font = fillLeadingZeroes(lineFont, 2);
        String first = getNameLine(font, name.substring(0, Math.min(name.length(), lineLength)));
        String second = getNameLine(font, name.substring(Math.min(name.length(), lineLength), Math.min(name.length(), lineLength * 2)));
        String third = getNameLine(font, name.substring(Math.min(name.length(), lineLength * 2), Math.min(name.length(), lineLength * 3)));
        String fourth = getNameLine(font, name.substring(Math.min(name.length(), lineLength * 3), Math.min(name.length(), lineLength * 4)));
        return first + separator + second + separator + third + separator + fourth;
    }

    private String getNameLine(String font, String line) {
        return font + separator + "\"" + encodeText(line) + "\"";
    }

    private String encodeText(String line) {
        return line.replace("\"", "\"\"");
    }

    private Integer getPlu(ScalesItemInfo item) {
        return item.pluNumber == null ? Integer.parseInt(item.idBarcode) : item.pluNumber;
    }

    class SendTransactionTask implements Callable<SendTransactionResult> {
        TransactionScalesInfo transaction;
        ScalesInfo scales;
        Integer nameLineFont;
        Integer nameLineLength;
        Integer descriptionLineFont;
        Integer descriptionLineLength;

        public SendTransactionTask(TransactionScalesInfo transaction, ScalesInfo scales, Integer nameLineFont, Integer nameLineLength, Integer descriptionLineFont, Integer descriptionLineLength) {
            this.transaction = transaction;
            this.scales = scales;
            this.nameLineFont = nameLineFont;
            this.nameLineLength = nameLineLength;
            this.descriptionLineFont = descriptionLineFont;
            this.descriptionLineLength = descriptionLineLength;
        }

        @Override
        public SendTransactionResult call() throws Exception {
            List<String> localErrors = new ArrayList<>();
            boolean cleared = false;
            DataSocket socket = new DataSocket(scales.port);
            try {
                socket.open();
                int globalError = 0;
                boolean needToClear = !transaction.itemsList.isEmpty() && transaction.snapshot && !scales.cleared;
                if (needToClear)
                    cleared = clearFile(socket, localErrors, scales.port, filePLU)
                            && clearFile(socket, localErrors, scales.port, fileIngredient)
                            && clearFile(socket, localErrors, scales.port, fileKeyAssignment);
                if (cleared || !needToClear) {
                    processTransactionLogger.info(getLogPrefix() + "Sending items..." + scales.port);
                    if (localErrors.isEmpty()) {
                        int count = 0;
                        for (ScalesItemInfo item : transaction.itemsList) {
                            count++;
                            if (!Thread.currentThread().isInterrupted() && globalError < 3) {
                                processTransactionLogger.info(String.format(getLogPrefix() + "IP %s, Transaction #%s, sending item #%s (barcode %s) of %s", scales.port, transaction.id, count, item.idBarcode, transaction.itemsList.size()));
                                Integer pluNumber = getPlu(item);
                                if(item.idBarcode.length() <= 5) {
                                    if (!sendPLU(socket, localErrors, item, pluNumber)
                                            || !sendIngredient(socket, localErrors, item, pluNumber, descriptionLineFont, descriptionLineLength)
                                            || !sendKeyAssignment(socket, localErrors, pluNumber))
                                        globalError++;
                                } else {
                                    processTransactionLogger.info(String.format(getLogPrefix() + "Sending item %s to scales %s failed: incorrect barcode %s", pluNumber, scales.port, item.idBarcode));
                                }
                            } else break;
                        }
                    }
                    socket.close();
                }

            } catch (Exception e) {
                logError(localErrors, String.format(getLogPrefix() + "IP %s error, transaction %s;", scales.port, transaction.id), e);
            } finally {
                processTransactionLogger.info(getLogPrefix() + "Finally disconnecting..." + scales.port);
                try {
                    socket.close();
                } catch (CommunicationException e) {
                    logError(localErrors, String.format(getLogPrefix() + "IP %s close port error ", scales.port), e);
                }
            }
            processTransactionLogger.info(getLogPrefix() + "Completed ip: " + scales.port);
            return new SendTransactionResult(scales, localErrors, cleared);
        }

        private boolean sendPLU(DataSocket socket, List<String> localErrors, ScalesItemInfo item, Integer plu) throws IOException {
            byte[] record = makePLURecord(item, plu, nameLineFont, nameLineLength);
            processTransactionLogger.info(String.format(getLogPrefix() + "Sending plu file item %s to scales %s", plu, scales.port));
            int reply = sendRecord(socket, cmdWrite, filePLU, record);
            if(reply != 0)
            logError(localErrors, String.format(getLogPrefix() + "Send plu %s to scales %s failed. Error: %s", plu, scales.port, reply));
            return reply == 0;
        }

        private boolean sendIngredient(DataSocket socket, List<String> localErrors, ScalesItemInfo item, Integer plu, Integer descriptionLineFont, Integer descriptionLineLength) throws IOException {
            if(item.description != null) {
                String description = encodeText(item.description);
                int lineNumber = 1;
                int reply = sendIngredientRecord(socket, localErrors, plu, "delete", lineNumber, 2, descriptionLineFont);
                while (!description.isEmpty() && reply == 0) {
                    String lineData = description.substring(0, Math.min(description.length(), descriptionLineLength));
                    description = description.substring(Math.min(description.length(), descriptionLineLength));
                    reply = sendIngredientRecord(socket, localErrors, plu, lineData, lineNumber, 0, descriptionLineFont);
                    lineNumber++;
                }
                return reply == 0;
            } else return true;
        }

        private int sendIngredientRecord(DataSocket socket, List<String> localErrors, Integer plu, String lineData, int lineNumber, int flagForDelete, int fontSize) throws IOException {
            //FlagForDelete: No data/0: Add or Change   1: Delete line   2: Delete record
            byte[] record = makeIngredientRecord(plu, lineData, lineNumber, flagForDelete, fontSize);
            processTransactionLogger.info(String.format(getLogPrefix() + "Sending ingredient file item %s to scales %s", plu, scales.port));
            int reply = sendRecord(socket, cmdWrite, fileIngredient, record);
            if (reply != 0)
                logError(localErrors, String.format(getLogPrefix() + "Send ingredient %s to scales %s failed. Error: %s", plu, scales.port, reply));
            return reply;
        }

        private boolean sendKeyAssignment(DataSocket socket, List<String> localErrors, Integer plu) throws IOException {
            byte[] record = makeKeyAssignmentRecord(plu);
            if (record != null) {
                processTransactionLogger.info(String.format(getLogPrefix() + "Sending keyAssignment file item %s to scales %s", plu, scales.port));
                int reply = sendRecord(socket, cmdWrite, fileKeyAssignment, record);
                if (reply != 0)
                    logError(localErrors, String.format(getLogPrefix() + "Send keyAssignment %s to scales %s failed. Error: %s", plu, scales.port, reply));
                return reply == 0;
            } else return true;
        }
    }
}