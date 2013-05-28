package jasperapi;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import platform.base.BaseUtils;
import platform.base.ByteArray;
import platform.base.DateConverter;
import platform.base.Pair;
import platform.interop.form.PropertyReadType;
import platform.interop.form.ReportConstants;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ClientReportData implements JRDataSource {
    final List<String> objectNames = new ArrayList<String>();
    private final List<String> propertyNames = new ArrayList<String>();  // todo [dale]: не обеспечивается уникальность имен
    private final Map<String, Integer> objects = new HashMap<String,Integer>();
    private final Map<String, Pair<Integer, Integer>> properties = new HashMap<String, Pair<Integer, Integer>>();

    private final ListIterator<HashMap<Integer, Object>> iterator;
    private final List<HashMap<Integer, Object>> keyRows = new ArrayList<HashMap<Integer, Object>>();
    private HashMap<Integer, Object> currentKeyRow;
    private final Map<Map<Integer, Object>, Map<Pair<Integer, Integer>, Object>> rows =
            new HashMap<Map<Integer, Object>, Map<Pair<Integer, Integer>, Object>>();

    private Map<String, List<Integer>> compositeFieldsObjects;
    private Map<String, Map<List<Object>, Object>> compositeObjectValues;
    private Map<String, List<Integer>> compositeColumnObjects;
    private Map<String, List<List<Object>>> compositeColumnValues;

    public static final String beginMarker = "[";
    public static final String endMarker = "]";

    private final Map<ByteArray, String> files;

    public ClientReportData(DataInputStream inStream, Map<ByteArray, String> files) throws IOException {

        if (!inStream.readBoolean()) {
            int objectCnt = inStream.readInt();
            for (int i = 0; i < objectCnt; i++) {
                String name = inStream.readUTF();
                objectNames.add(name);
                objects.put(name, inStream.readInt());
            }

            int propCnt = inStream.readInt();
            for (int i = 0; i < propCnt; i++) {
                String name = inStream.readUTF();
                int type = inStream.readInt();
                if (type == PropertyReadType.CAPTION) {
                    name += ReportConstants.captionSuffix;
                } else if (type == PropertyReadType.FOOTER) {
                    name += ReportConstants.footerSuffix;
                }
                propertyNames.add(name);
                properties.put(name, new Pair<Integer, Integer>(type, inStream.readInt()));
            }

            int rowCnt = inStream.readInt();
            for (int i = 0; i < rowCnt; i++) {
                HashMap<Integer, Object> objectValues = new HashMap<Integer, Object>();
                for (String objName : objectNames) {
                    Object objValue = BaseUtils.deserializeObject(inStream);
                    objectValues.put(objects.get(objName), objValue);
                }
                Map<Pair<Integer, Integer>, Object> propValues = new HashMap<Pair<Integer, Integer>, Object>();
                for (String propName : propertyNames) {
                    Object propValue = BaseUtils.deserializeObject(inStream);
                    propValues.put(properties.get(propName), propValue);
                }
                keyRows.add(objectValues);
                rows.put(objectValues, propValues);
            }
        }
        iterator = keyRows.listIterator();

        this.files = files;
    }

    public boolean next() throws JRException {
        if(!iterator.hasNext()) return false;
        currentKeyRow = iterator.next();
        return true;
    }

    public void revert() {
        assert iterator.hasPrevious();
        iterator.previous();
    }

    public void setCompositeData(Map<String, List<Integer>> fieldObjects,
                                 Map<String, Map<List<Object>, Object>> objectValues,
                                 Map<String, List<Integer>> columnObjects,
                                 Map<String, List<List<Object>>> columnValues) {
        compositeFieldsObjects = fieldObjects;
        compositeObjectValues = objectValues;
        compositeColumnObjects = columnObjects;
        compositeColumnValues = columnValues; 
    }

    public Object getFieldValue(JRField jrField) throws JRException {

        String fieldName = jrField.getName();
        Object value = null;
        Integer objectID = objects.get(fieldName);
        if (objectID != null) {
            value = currentKeyRow.get(objectID);
        } else {
            Pair<Integer, Integer>  propertyID = properties.get(fieldName);
            if (propertyID != null) {
                value = rows.get(currentKeyRow).get(propertyID);
            } else if (fieldName.endsWith(endMarker)) { 
                Pair<Integer, String> extractData = extractFieldData(fieldName);
                int index = extractData.first;
                String realFieldName = extractData.second;
                if (index != -1) {
                    String dataFieldName = realFieldName;
                    if (realFieldName.endsWith(ReportConstants.captionSuffix)) {
                        dataFieldName = realFieldName.substring(0, realFieldName.length() - ReportConstants.captionSuffix.length());
                    } else if (realFieldName.endsWith(ReportConstants.footerSuffix)) {
                        dataFieldName = realFieldName.substring(0, realFieldName.length() - ReportConstants.footerSuffix.length());
                    }
                    value = getCompositeFieldValue(dataFieldName, realFieldName, index);
                }
            }
        }

        if (Date.class.getName().equals(jrField.getValueClassName()) && value != null) {
            if (value instanceof java.sql.Timestamp)
                value = DateConverter.stampToDate((java.sql.Timestamp) value);
            else
                value = DateConverter.sqlToDate((java.sql.Date) value);
        }

        if (value instanceof String) {
            value = BaseUtils.rtrim((String) value);
        }

        if(jrField.getDescription()!=null && Number.class.isAssignableFrom(jrField.getValueClass()) && jrField.getDescription().contains("@Z") && value == null) {
            value = jrField.getValueClass().cast(0);
        }

        if (value instanceof byte[]) {
            if (files != null) {
                ByteArray file = new ByteArray(((byte[])value));
                String fileName = files.get(file);
                if(fileName==null) {
                    fileName = "File " + (files.size()+1) + ".pdf";
                    files.put(file, fileName);
                }
                value = fileName;
            }
        }

        return value;
    }

    private Object getCompositeFieldValue(String dataFieldName, String realFieldName, int index) {
        List<Object> row = new ArrayList<Object>(Collections.nCopies(compositeFieldsObjects.get(dataFieldName).size(), null));

        Map<Integer, Integer> pos = new HashMap<Integer, Integer>();
        int i = 0;
        for (Integer id : compositeFieldsObjects.get(dataFieldName)) {
            pos.put(id, i);
            ++i;
        }

        for (Map.Entry<Integer, Object> entry : currentKeyRow.entrySet()) {
            if (pos.containsKey(entry.getKey())) {
                row.set(pos.get(entry.getKey()), entry.getValue());
            }
        }
        List<Object> values = compositeColumnValues.get(dataFieldName).get(index);
        int j = 0;
        for (Integer id : compositeColumnObjects.get(dataFieldName)) {
            row.set(pos.get(id), values.get(j));
            ++j;
        }
        return compositeObjectValues.get(realFieldName).get(row);
    }

    public Object getKeyValueByIndex(int index) {
        return currentKeyRow.get(objects.get(objectNames.get(index)));
    }

    private Pair<Integer, String> extractFieldData(String id) {
        int markerPos = id.substring(0, id.length() - endMarker.length()).lastIndexOf(beginMarker);
        if (markerPos == -1) return new Pair<Integer, String>(-1, "");
        String indexString = id.substring(markerPos + beginMarker.length(), id.length() - endMarker.length());
        String realFieldName = id.substring(0, markerPos);
        return new Pair<Integer, String>(Integer.parseInt(indexString), realFieldName);
    }
}


