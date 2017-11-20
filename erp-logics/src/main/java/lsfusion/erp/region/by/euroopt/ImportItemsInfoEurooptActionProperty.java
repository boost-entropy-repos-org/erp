package lsfusion.erp.region.by.euroopt;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.ServerLoggers;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.integration.*;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.silvertunnel_ng.netlib.api.NetLayer;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class ImportItemsInfoEurooptActionProperty extends EurooptActionProperty {

    public ImportItemsInfoEurooptActionProperty(ScriptingLogicsModule LM) throws ScriptingErrorLog.SemanticErrorException {
        super(LM);
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            boolean useTor = findProperty("ImportEuroopt.useTor[]").read(context) != null;
            boolean skipKeys = findProperty("skipKeys[]").read(context) != null;

            List<List<Object>> data = getItemsInfo(context, useTor, skipKeys);
            if(!data.isEmpty()) {
                importItems(context, data, skipKeys);
                context.delayUserInteraction(new MessageClientAction("Импорт успешно завершён", "Импорт товаров Евроопт"));
            } else {
                context.delayUserInteraction(new MessageClientAction("Не выбрано ни одного существующего товара!", "Ошибка"));
            }

        } catch (IOException | ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }

    }

    private void importItems(ExecutionContext context, List<List<Object>> data, boolean skipKeys) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {

        List<ImportProperty<?>> props = new ArrayList<>();
        List<ImportField> fields = new ArrayList<>();
        List<ImportKey<?>> keys = new ArrayList<>();

        ImportField idBarcodeSkuField = new ImportField(findProperty("idBarcode[Sku]"));
        ImportKey<?> itemKey = new ImportKey((CustomClass) findClass("Item"),
                findProperty("skuBarcode[STRING[15]]").getMapping(idBarcodeSkuField));
        itemKey.skipKey = skipKeys;
        keys.add(itemKey);
        fields.add(idBarcodeSkuField);

        ImportKey<?> barcodeKey = new ImportKey((CustomClass) findClass("Barcode"),
                findProperty("extBarcode[VARSTRING[100]]").getMapping(idBarcodeSkuField));
        barcodeKey.skipKey = skipKeys;
        keys.add(barcodeKey);
        props.add(new ImportProperty(idBarcodeSkuField, findProperty("sku[Barcode]").getMapping(barcodeKey),
                object(findClass("Item")).getMapping(itemKey)));
        props.add(new ImportProperty(idBarcodeSkuField, findProperty("extId[Barcode]").getMapping(barcodeKey), true));
        props.add(new ImportProperty(idBarcodeSkuField, findProperty("id[Barcode]").getMapping(barcodeKey), true));
        props.add(new ImportProperty(idBarcodeSkuField, findProperty("id[Item]").getMapping(itemKey), true));

        ImportField idItemGroupField = new ImportField(findProperty("id[ItemGroup]"));
        ImportKey<?> itemGroupKey = new ImportKey((CustomClass) findClass("ItemGroup"),
                findProperty("itemGroup[VARSTRING[100]]").getMapping(idItemGroupField));
        itemGroupKey.skipKey = skipKeys;
        keys.add(itemGroupKey);
        props.add(new ImportProperty(idItemGroupField, findProperty("id[ItemGroup]").getMapping(itemGroupKey), true));
        props.add(new ImportProperty(idItemGroupField, findProperty("name[ItemGroup]").getMapping(itemGroupKey), true));
        props.add(new ImportProperty(idItemGroupField, findProperty("itemGroup[Item]").getMapping(itemKey),
                LM.object(findClass("ItemGroup")).getMapping(itemGroupKey), true));
        fields.add(idItemGroupField);

        ImportField captionItemField = new ImportField(findProperty("caption[Item]"));
        props.add(new ImportProperty(captionItemField, findProperty("caption[Item]").getMapping(itemKey), true));
        fields.add(captionItemField);

        ImportField netWeightItemField = new ImportField(findProperty("netWeight[Item]"));
        props.add(new ImportProperty(netWeightItemField, findProperty("netWeight[Item]").getMapping(itemKey), true));
        fields.add(netWeightItemField);

        ImportField descriptionItemField = new ImportField(findProperty("description[Item]"));
        props.add(new ImportProperty(descriptionItemField, findProperty("description[Item]").getMapping(itemKey), true));
        fields.add(descriptionItemField);

        ImportField compositionItemField = new ImportField(findProperty("composition[Item]"));
        props.add(new ImportProperty(compositionItemField, findProperty("composition[Item]").getMapping(itemKey), true));
        fields.add(compositionItemField);

        ImportField proteinsItemField = new ImportField(findProperty("proteins[Item]"));
        props.add(new ImportProperty(proteinsItemField, findProperty("proteins[Item]").getMapping(itemKey), true));
        fields.add(proteinsItemField);

        ImportField fatsItemField = new ImportField(findProperty("fats[Item]"));
        props.add(new ImportProperty(fatsItemField, findProperty("fats[Item]").getMapping(itemKey), true));
        fields.add(fatsItemField);

        ImportField carbohydratesItemField = new ImportField(findProperty("carbohydrates[Item]"));
        props.add(new ImportProperty(carbohydratesItemField, findProperty("carbohydrates[Item]").getMapping(itemKey), true));
        fields.add(carbohydratesItemField);

        ImportField energyItemField = new ImportField(findProperty("energy[Item]"));
        props.add(new ImportProperty(energyItemField, findProperty("energy[Item]").getMapping(itemKey), true));
        fields.add(energyItemField);

        ImportField idManufacturerField = new ImportField(findProperty("id[Manufacturer]"));
        ImportKey<?> manufacturerKey = new ImportKey((CustomClass) findClass("Manufacturer"),
                findProperty("manufacturer[VARSTRING[100]]").getMapping(idManufacturerField));
        manufacturerKey.skipKey = skipKeys;
        keys.add(manufacturerKey);
        props.add(new ImportProperty(idManufacturerField, findProperty("id[Manufacturer]").getMapping(manufacturerKey), true));
        props.add(new ImportProperty(idManufacturerField, findProperty("name[Manufacturer]").getMapping(manufacturerKey), true));
        props.add(new ImportProperty(idManufacturerField, findProperty("manufacturer[Item]").getMapping(itemKey),
                LM.object(findClass("Manufacturer")).getMapping(manufacturerKey), true));
        fields.add(idManufacturerField);

        ImportField idUOMField = new ImportField(findProperty("id[UOM]"));
        ImportKey<?> UOMKey = new ImportKey((CustomClass) findClass("UOM"),
                findProperty("UOM[VARSTRING[100]]").getMapping(idUOMField));
        UOMKey.skipKey = true;
        keys.add(UOMKey);
        props.add(new ImportProperty(idUOMField, findProperty("UOM[Item]").getMapping(itemKey),
                object(findClass("UOM")).getMapping(UOMKey), true));
        props.add(new ImportProperty(idUOMField, findProperty("UOM[Barcode]").getMapping(barcodeKey),
                object(findClass("UOM")).getMapping(UOMKey), true));
        fields.add(idUOMField);

        ImportField idBrandField = new ImportField(findProperty("id[Brand]"));
        ImportKey<?> brandKey = new ImportKey((CustomClass) findClass("Brand"),
                findProperty("brand[VARSTRING[100]]").getMapping(idBrandField));
        brandKey.skipKey = true;
        keys.add(brandKey);
        props.add(new ImportProperty(idBrandField, findProperty("id[Brand]").getMapping(brandKey), true));
        props.add(new ImportProperty(idBrandField, findProperty("name[Brand]").getMapping(brandKey), true));
        props.add(new ImportProperty(idBrandField, findProperty("brand[Item]").getMapping(itemKey),
                object(findClass("Brand")).getMapping(brandKey), true));
        fields.add(idBrandField);

        ImportField urlEurooptItemField = new ImportField(findProperty("url[EurooptItem]"));
        ImportKey<?> eurooptItemKey = new ImportKey((CustomClass) findClass("EurooptItem"),
                findProperty("eurooptItem[STRING[255]]").getMapping(urlEurooptItemField));
        eurooptItemKey.skipKey = true;
        keys.add(eurooptItemKey);
        props.add(new ImportProperty(idBarcodeSkuField, findProperty("item[EurooptItem]").getMapping(eurooptItemKey),
                object(findClass("Item")).getMapping(itemKey)));
        fields.add(urlEurooptItemField);

        ImportTable table = new ImportTable(fields, data);

        try (DataSession session = context.createSession()) {
            session.pushVolatileStats("IE_IT");
            IntegrationService service = new IntegrationService(session, table, keys, props);
            service.synchronize(true, false);
            session.apply(context);
            session.popVolatileStats();
        }
    }

    private List<List<Object>> getItemsInfo(ExecutionContext context, boolean useTor, boolean skipKeys) throws ScriptingErrorLog.SemanticErrorException, SQLHandledException, SQLException, IOException {
        List<List<Object>> itemsList = new ArrayList<>();
        Map<String, String> barcodeSet = getBarcodeSet(context);
        List<String> itemURLs = getItemURLs(context);
        if(!itemURLs.isEmpty()) {
            ServerLoggers.importLogger.info(String.format(logPrefix + "import %s item(s)", itemURLs.size()));
            int skipped = 0;
            NetLayer lowerNetLayer = useTor ? getNetLayer() : null;

            int i = 1;
            for (String itemURL : itemURLs) {
                ServerLoggers.importLogger.info(String.format(logPrefix + "parsing item page #%s of %s: %s", i, itemURLs.size(), (useTor ? mainPage : "") + itemURL));
                Document doc = getDocument(lowerNetLayer, itemURL);
                if (doc != null) {
                    String title = doc.getElementsByTag("title").text().replace(" - Каталог товаров", "");
                    Elements descriptionElement = doc.getElementsByClass("description");
                    List<Node> descriptionAttributes = descriptionElement.size() == 0 ? new ArrayList<Node>() : descriptionElement.get(0).childNodes();
                    String idBarcode = null;
                    String captionItem = doc.getElementsByTag("h1").text();
                    String idItemGroup = null;
                    String brandItem = null;
                    BigDecimal netWeight = null;
                    String UOMItem = null;
                    for (Node attribute : descriptionAttributes) {
                        if (attribute instanceof Element && ((Element) attribute).children().size() == 2) {
                            String type = parseChild((Element) attribute, 0);
                            String value = parseChild((Element) attribute, 1);
                            switch (type) {
                                case "Штрих-код:":
                                    idBarcode = value;
                                    idItemGroup = barcodeSet.containsKey(idBarcode) ? barcodeSet.get(idBarcode) : "ВСЕ";
                                    break;
                                case "Торговая марка:":
                                    brandItem = value;
                                    break;
                                case "Масса:":
                                    String[] split = value.split(" ");
                                    netWeight = new BigDecimal(split[0]);
                                    UOMItem = split.length >= 2 ? split[1] : null;
                                    break;
                            }
                        }
                    }
                    Elements propertyAttributes = doc.getElementsByClass("property_group");
                    String descriptionItem = null;
                    String compositionItem = null;
                    BigDecimal proteinsItem = null;
                    BigDecimal fatsItem = null;
                    BigDecimal carbohydratesItem = null;
                    BigDecimal energyItem = null;
                    String manufacturerItem = null;
                    for (Element propertyAttribute : propertyAttributes) {
                        Elements propertyRows = propertyAttribute.select("tr");
                        for (Element propertyRow : propertyRows) {
                            String type = parseChild(propertyRow, 0);
                            String value = parseChild(propertyRow, 1);
                            switch (type) {
                                case "Краткое описание":
                                    descriptionItem = value;
                                    break;
                                case "Состав":
                                    compositionItem = value;
                                    break;
                                case "Белки":
                                    proteinsItem = parseBigDecimalWeight(value);
                                    break;
                                case "Жиры":
                                    fatsItem = parseBigDecimalWeight(value);
                                    break;
                                case "Углеводы":
                                    carbohydratesItem = parseBigDecimalWeight(value);
                                    break;
                                case "Энергетическая ценность на 100 г":
                                    energyItem = parseBigDecimalWeight(value.split("\\s(ккал|калл)")[0]);
                                    break;
                                case "Производитель":
                                    manufacturerItem = value;
                                    break;
                            }
                        }
                    }
                    if (idBarcode != null && (!skipKeys || barcodeSet.containsKey(idBarcode))) {

                        itemsList.add(Arrays.asList((Object) idBarcode, idItemGroup, captionItem, netWeight, descriptionItem, compositionItem, proteinsItem,
                                fatsItem, carbohydratesItem, energyItem, manufacturerItem, UOMItem, brandItem, itemURL));
                        //to avoid duplicates
                        barcodeSet.remove(idBarcode);
                        ServerLoggers.importLogger.info(String.format(logPrefix + "parsed item page #%s of %s: %s", i, itemURLs.size(), title));
                    } else {
                        ServerLoggers.importLogger.info(logPrefix + (idBarcode == null ? "no barcode, item skipped" : "not in base, item skipped") + " (" + title + ")");
                        skipped++;
                    }
                }
                i++;
            }
            ServerLoggers.importLogger.info(String.format(logPrefix + "read finished. %s items, %s items without barcode skipped", itemsList.size(), skipped));
        }
        return itemsList;
    }

    private String parseChild(Element element, int child) {
        return element.children().size() > child ? Jsoup.parse(element.childNode(child).outerHtml()).text() : "";
    }

    private Map<String, String> getBarcodeSet(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        Map<String, String> barcodeSet = new HashMap<>();
        KeyExpr barcodeExpr = new KeyExpr("barcode");
        ImRevMap<Object, KeyExpr> keys = MapFact.singletonRev((Object) "barcode", barcodeExpr);
        QueryBuilder<Object, Object> query = new QueryBuilder<>(keys);
        query.addProperty("idBarcode", findProperty("id[Barcode]").getExpr(context.getModifier(), barcodeExpr));
        query.addProperty("idItemGroupBarcode", findProperty("idItemGroup[Barcode]").getExpr(context.getModifier(), barcodeExpr));
        query.and(findProperty("id[Barcode]").getExpr(barcodeExpr).getWhere());
        ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> itemResult = query.executeClasses(context);
        for (ImMap<Object, ObjectValue> entry : itemResult.values()) {
            String idBarcode = trim((String) entry.get("idBarcode").getValue());
            String idItemGroupBarcode = trim((String) entry.get("idItemGroupBarcode").getValue());
            barcodeSet.put(idBarcode, idItemGroupBarcode);
        }
        return barcodeSet;
    }

    private List<String> getItemURLs(ExecutionContext context) throws IOException, ScriptingErrorLog.SemanticErrorException, SQLException, SQLHandledException {
        List<String> itemURLs = new ArrayList<>();
        KeyExpr itemExpr = new KeyExpr("eurooptItem");
        ImRevMap<Object, KeyExpr> itemKeys = MapFact.singletonRev((Object) "eurooptItem", itemExpr);

        QueryBuilder<Object, Object> itemQuery = new QueryBuilder<>(itemKeys);
        itemQuery.addProperty("url", findProperty("url[EurooptItem]").getExpr(context.getModifier(), itemExpr));
        itemQuery.and(findProperty("in[EurooptItem]").getExpr(context.getModifier(), itemExpr).getWhere());
        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> itemResult = itemQuery.execute(context);
        for (ImMap<Object, Object> entry : itemResult.values()) {
            itemURLs.add(trim((String) entry.get("url")));
        }
        return itemURLs;
    }

    private BigDecimal parseBigDecimalWeight(String input) {
        try {
            input = input.replace("г", "").replace("Г", "").trim();
            return input.isEmpty() ? null : new BigDecimal(input);
        } catch (Exception e) {
            return null;
        }
    }
}
