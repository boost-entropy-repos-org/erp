package roman;

import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MCol;
import platform.base.col.interfaces.mutable.MList;
import platform.base.col.interfaces.mutable.MOrderExclSet;
import platform.interop.*;
import platform.interop.action.AudioClientAction;
import platform.interop.action.MessageClientAction;
import platform.interop.form.ServerResponse;
import platform.interop.form.layout.ContainerType;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.Settings;
import platform.server.classes.*;
import platform.server.daemons.ScannerDaemonTask;
import platform.server.daemons.WeightDaemonTask;
import platform.server.data.Time;
import platform.server.data.Union;
import platform.server.data.type.Type;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.navigator.NavigatorElement;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.form.window.PanelNavigatorWindow;
import platform.server.logics.*;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.Event;
import platform.server.logics.property.actions.ChangeBarcodeActionProperty;
import platform.server.logics.property.actions.FormActionProperty;
import platform.server.logics.property.actions.PrevGroupChangeActionProperty;
import platform.server.logics.property.actions.UserActionProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.logics.property.group.AbstractGroup;
import roman.actions.TNVEDImportUAActionProperty;
import roman.actions.TranslateActionProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static platform.base.BaseUtils.add;
import static platform.base.BaseUtils.consecutiveInts;
import static platform.server.logics.PropertyUtils.*;
import static platform.server.logics.property.derived.DerivedProperty.*;

/**
 * Created by IntelliJ IDEA.
 * User: DAle
 * Date: 24.05.11
 * Time: 15:14
 */


public class RomanLogicsModule extends LogicsModule {
    private final RomanBusinessLogics BL;
    private LCP equalsColorItemSupplier;
    private LCP equalsSizeItemSupplier;
    //private LCP equalsSeasonSupplierArticleSupplier;
    private LCP equalsThemeItemSupplier;
    private LCP boxInvoiceAddFA;
    private LCP simpleInvoiceAddFA;
    private LAP orderEditFA;
    private LAP boxInvoiceEditFA;
    private LAP simpleInvoiceEditFA;
    private LAP freightCompleteFA;
    private LAP addNEColorSupplierSIDSupplier;
    private LAP addNEColorSupplierSIDInvoice;
    private LAP executeAddColorDocument;
    private LAP seekColorSIDSupplier;
    private LAP seekColorSIDInvoice;
    public LCP itemArticleCompositeColorSize;
    private ConcreteCustomClass sizeGroupSupplier;
    private LCP supplierSizeGroup;
    private LCP groupSizeSupplier;
    private LCP orderSizeSupplier;
    private LCP equalsGroupSizeSupplier;
    private LCP sizeGroupSupplierArticle;
    private LCP nameSizeGroupSupplierArticle;
    private LCP nameSupplierSizeGroup;
    private LCP nameGroupSizeSupplier;
    private LAP freightChangedFA;
    private LAP freightPricedFA;
    private LAP skuEditFA;
    private LAP cloneItem;
    private LAP addItemArticleCompositeColorSizeBarcode;
    private LAP addItemSIDArticleSupplierColorSizeBarcode;
    private LCP quantitySimpleInvoiceSimpleShipmentStockSku;
    private LCP priceProxyImporterFreightSku;
    //public LCP contractInProxyImporterStockSku, sidContractInProxyImporterStockSku, dateContractInProxyImporterStockSku;
    private LCP priceDirectImporterFreightSku;
    private LCP RRPDirectImporterFreightSku;
    private LCP RRPProxyImporterFreightSku;
    private LCP RRPImporterFreightSku;
    private LCP RRPImporterFreightArticle;
    private LCP RRPFreightArticle;
    private LCP RRPFreightSku;
    private LCP currencyRRPImporterFreightSku;
    private LCP currencyRRPDirectImporterFreightSku;
    private LCP currencyRRPProxyImporterFreightSku;
    private LCP currencyRRPInImporterFreightSku;
    private LCP currencyRRPImporterFreightArticle;
    private LCP nameCurrencyRRPImporterFreightArticle;
    private LCP nameCurrencyRRPImporterFreightSku;
    private LCP currencyRRPFreightArticle;
    private LCP currencyRRPFreightSku;
    private LCP companyInvoice;
    private LCP nameCompanyInvoice;
    private LCP languageInvoice;
    private LCP nameLanguageInvoice;

    public LCP dumb1;

    public RomanLogicsModule(BaseLogicsModule<RomanBusinessLogics> baseLM, RomanBusinessLogics BL) {
        super("RomanLogicsModule");
        setBaseLogicsModule(baseLM);
        this.BL = BL;
    }

    public static StringClass COMPOSITION_CLASS = StringClass.get(200);

    private AbstractCustomClass article;
    ConcreteCustomClass articleComposite;
    public ConcreteCustomClass articleSingle;
    ConcreteCustomClass item;
    public AbstractCustomClass barcodeObject;
    protected AbstractCustomClass sku;
    private ConcreteCustomClass pallet;
    LCP sidArticle;
    LCP articleCompositeItem;
    public LCP itemSupplierArticleSIDColorSIDSizeSID;
    public LCP round2;
    public LCP sumNumeric2;
    public LCP divideNumeric2;
    public LCP divideNumeric3;
    public LCP percentNumeric2;
    private LCP articleSku;
    public ConcreteCustomClass order;
    private ConcreteCustomClass typeInvoice;
    private AbstractCustomClass invoice;
    protected AbstractCustomClass shipment;
    private ConcreteCustomClass boxShipment;
    private ConcreteCustomClass simpleShipment;
    private ConcreteCustomClass freight;
    private ConcreteCustomClass route;
    private ConcreteCustomClass season;
    private ConcreteCustomClass typeContainer;
    private ConcreteCustomClass typeTransit;
    private AbstractCustomClass seller;
    //private AbstractCustomClass buyer;
    private AbstractCustomClass supplier;
    private ConcreteCustomClass contract;
    private AbstractCustomClass document;
    private AbstractCustomClass priceDocument;
    private AbstractCustomClass subject;
    private ConcreteCustomClass multiLanguageNamed;
    LCP addressOriginSubject;
    LCP addressSubject;
    LCP supplierDocument;
    private LCP nameSupplierDocument;
    LCP sidDocument;
    public LCP dateFromOrder;
    public LCP dateToOrder;
    LCP documentSIDSupplier;
    private LCP sumSimpleInvoice;
    private LCP sumInvoicedDocument;
    private LCP sumDocument;
    ConcreteCustomClass commonSize;
    ConcreteCustomClass colorSupplier;
    ConcreteCustomClass sizeSupplier;
    ConcreteCustomClass gender;
    ConcreteCustomClass genderSupplier;
    ConcreteCustomClass seasonSupplier;
    public ConcreteCustomClass brandSupplier;
    public ConcreteCustomClass themeSupplier;
    public ConcreteCustomClass collectionSupplier;
    public ConcreteCustomClass categorySupplier;
    public ConcreteCustomClass subCategorySupplier;
    public ConcreteCustomClass seasonYear;
    public ConcreteCustomClass typeLabel;
    ConcreteCustomClass countrySupplier;
    public  LCP yearSeasonYear;
    public  LCP seasonSeasonYear;
    public  LCP nameSeasonSeasonYear;
    public  LCP nameSeasonYear;
    LCP supplierArticle;
    private LCP nameSupplierArticle;
    private LCP dateFromDataOrderArticle;
    private LCP dateToDataOrderArticle;
    private LCP dateFromOrderOrderArticle;
    private LCP dateToOrderOrderArticle;
    public LCP dateFromOrderArticle;
    public LCP dateToOrderArticle;
    private LCP dateFromOrderArticleSku;
    private LCP dateToOrderArticleSku;
    LCP priceDocumentArticle;
    LCP RRPDocumentArticle;
    LCP RRPDocumentSku;
    LCP currencyRRPDocumentSku;
    LCP RRPRateDocumentSku;
    private LCP sumDocumentArticle;
    private LCP sumSimpleInvoiceArticle;
    LCP colorSupplierItem;
    private LCP nameColorSupplierItem;
    private LCP inListArticleColorSupplier;
    LCP sizeSupplierItem;
    public LCP sidSizeSupplierItem;
    public LCP commonSizeItem;
    public LCP nameCommonSizeItem;
    LCP supplierColorSupplier;
    LCP genderSupplierArticle;
    LCP sidGenderSupplierArticle;
    private LCP quantityCreationStamp;
    private LCP seriesOfCreationStamp;
    private LCP dateOfCreationStamp;
    private LCP nameSupplierColorSupplier;
    private LCP nameSupplierThemeSupplier;
    private LCP nameSupplierCollectionSupplier;
    private LCP nameSupplierSubCategorySupplier;
    private LCP nameSupplierGenderSupplier;
    LCP supplierSizeSupplier;
    private LCP nameSupplierSizeSupplier;
    LCP supplierGenderSupplier;
    public LCP sidBrandSupplier;
    public LCP customsSIDBrandSupplier;
    public LCP customsSIDSupplier;
    public LCP customsSIDArticle;
    public LCP supplierCustomsSID;
    public LCP sidTypeDuty;
    public LCP nameTypeDuty;
    public LCP sidToTypeDuty;
    public LCP typeDutyDuty;
    private LCP sidTypeDutyDuty;
    private LCP nameTypeDutyDuty;
    private LCP typeDutyNDS;
    private LCP sidTypeDutyNDS;
    private LCP nameTypeDutyNDS;
    private LCP typeDutyRegistration;
    private LCP sidTypeDutyRegistration;
    private LCP nameTypeDutyRegistration;
    public LCP typeDutyDutyCustomsZone;
    private LCP sidTypeDutyDutyCustomsZone;
    private LCP nameTypeDutyDutyCustomsZone;
    private LCP typeDutyNDSCustomsZone;
    private LCP sidTypeDutyNDSCustomsZone;
    private LCP nameTypeDutyNDSCustomsZone;
    private LCP typeDutyRegistrationCustomsZone;
    private LCP sidTypeDutyRegistrationCustomsZone;
    private LCP nameTypeDutyRegistrationCustomsZone;

    public LCP supplierBrandSupplier;
    private LCP nameSupplierBrandSupplier;
    private LCP brandSupplierSupplier;
    private LCP nameBrandSupplierSupplier;
    public LCP brandSupplierArticle;
    private LCP sidBrandSupplierArticle;
    public LCP nameBrandSupplierArticle;
    private LCP supplierBrandSupplierArticle;
    private LCP countryBrandSupplier;
    private LCP nameCountryBrandSupplier;
    private LCP brandSupplierDataArticle;
    private LCP brandSupplierSupplierArticle;
    private LCP brandSupplierArticleSku;
    private LCP sidBrandSupplierArticleSku;
    public LCP nameBrandSupplierArticleSku;
    private LCP nameBrandSupplierArticleSkuShipmentDetail;
    public LCP addressSupplier;
    private LCP supplierArticleSku;
    private LCP nameSupplierArticleSku;
    private LCP addressSupplierArticleSku;
    public LCP supplierThemeSupplier;
    public LCP supplierSeasonSupplier;
    public LCP supplierCollectionSupplier;
    public LCP supplierCategorySupplier;
    public LCP supplierSubCategorySupplier;
    public LCP categorySupplierSubCategorySupplier;
    public LCP nameCategorySupplierSubCategorySupplier;
    public LCP sidDestinationSupplier;
    public LCP seasonSupplierArticle;
    public LCP sidSeasonSupplierArticle;
    public LCP seasonYearSeasonSupplierArticle;
    public LCP seasonYearDataArticle;
    public LCP seasonYearArticle;
    public LCP nameSeasonYearArticle;
    public LCP seasonYearArticleSku;
    public LCP nameSeasonYearArticleSku;

    public LCP themeSupplierArticle;
    public LCP nameThemeSupplierArticle;
    public LCP themeSupplierArticleSku;
    public LCP sidThemeSupplierArticleSku;
    public LCP nameThemeSupplierArticleSku;

    public LCP collectionSupplierArticle;
    public LCP nameCollectionSupplierArticle;
    public LCP collectionSupplierArticleSku;
    public LCP sidCollectionSupplierArticleSku;
    public LCP nameCollectionSupplierArticleSku;

    public LCP subCategorySupplierArticle;
    public LCP nameSubCategorySupplierArticle;
    public LCP subCategorySupplierArticleSku;
    public LCP sidSubCategorySupplierArticleSku;
    public LCP nameSubCategorySupplierArticleSku;

    public LCP categorySupplierArticle;
    public LCP nameCategorySupplierArticle;

    public LCP categorySupplierArticleSku;
    public LCP nameCategorySupplierArticleSku;

    public ConcreteCustomClass store;
    private ConcreteCustomClass unitOfMeasure;

    public LCP legalEntityCustomStore;

    public LCP relationStoreSupplier;
    private LCP typeExchangeSTX;
    private LCP nameTypeExchangeSTX;
    public LCP typeExchangeCustom;
    public LCP nameTypeExchangeCustom;
    public LCP typeExchangePayCustom;
    public LCP nameTypeExchangePayCustom;
    public LCP typeExchangePayManagerial;
    public LCP nameTypeExchangePayManagerial;
    public LCP typeExchangePayCustomCustomsZone;
    public LCP nameTypeExchangePayCustomCustomsZone;
    public LCP typeExchangePayManagerialCustomsZone;
    public LCP nameTypeExchangePayManagerialCustomsZone;

//    public LCP typeExchangeRetail;
//    public LCP nameTypeExchangeRetail;
    private LCP currencyPayFreights;
    private LCP nameCurrencyPayFreights;
    private LCP currencyCustom;
    private LCP nameCurrencyCustom;
    public LCP currencyPayCustom;
    //public LCP nameCurrencyPayCustom;
    private LCP NDSPercentCustom;
    private LCP tariffVolumeFreights;
    private LCP percentCostFreights;
    public LCP sidImporterFreightTypeInvoice;
    public LCP sidImporterFreight;
    public LCP sidDestination;
    private LCP destinationSID;
    private LCP unitOfMeasureCategory;
    private LCP nameUnitOfMeasureCategory;
    private LCP unitOfMeasureArticle;
    private LCP nameOriginUnitOfMeasureArticle;
    LCP nameUnitOfMeasureArticle;
    private LCP unitOfMeasureArticleSku;
    private LCP nameUnitOfMeasureArticleSku;
    LCP supplierCountrySupplier;
    private LCP nameSupplierCountrySupplier;
    private LCP countryCountrySupplier;
    private LCP nameCountryCountrySupplier;
    private LCP currencySupplier;
    private LCP nameCurrencySupplier;
    private LCP currencyDocument;
    private LCP nameCurrencyDocument;
    private LCP currencyRRPDocument;
    private LCP nameCurrencyRRPDocument;
    private LCP destinationDestinationDocument;
    private LCP nameDestinationDestinationDocument;
    private LCP sidDestinationDestinationDocument;
    private LCP dateDepartureShipment;
    private LCP dateArrivalShipment;
    private LCP quantityPalletShipment;
    private LCP grossWeightPallet;
    private LCP grossWeightCurrentPalletRoute;
    private LCP grossWeightFreight;
    private LCP sumGrossWeightFreightSku;
    private LCP grossWeightFreightSkuAggr;
    private LCP netWeightShipment;
    private LCP grossWeightShipment;
    LCP sidColorSupplier;
    public LCP sidColorSupplierItem;
    private LCP sidThemeSupplierArticle;
    private LCP quantityDocumentSku;
    private LCP quantityDocumentBrandSupplier;
    private LCP quantityAllDocumentsBrandSupplier;
    private LCP quantitySimpleInvoiceArticle;
    private LCP quantitySimpleInvoice;
    private LCP quantityDocumentArticle;
    private LCP quantityInvoicedDocumentArticle;
    private LCP quantityListArticleCompositeColor;
    private LCP quantityListArticleCompositeSize;
    private LCP quantityListArticleCompositeColorSize;
    private LCP quantityDocument;
    private LCP netWeightDocumentArticle;
    private LCP netWeightDocument;
    LCP originalNameArticle;
    LCP translateNameArticle;
    LCP translateNameArticleSku;
    LCP translateNameColorSupplier;
    LCP translateNameColorSupplierItem;
    LCP translateNameSkuLanguage;
    LAP translationNameSku;
    LCP translateNameSkuInvoice;
    LAP translationNameSkuInvoice;
    LAP translationNameSkuFreight;
    LCP mainCompositionSkuInvoice;
    LAP translationMainCompositionSkuInvoice;
    LCP additionalCompositionSkuInvoice;
    LAP translationAdditionalCompositionSkuInvoice;
    LAP translationNameSkuLanguage;
    private LCP nameArticle;
    private LCP netWeightArticleSku;
    LCP netWeightDataSku;
    private LCP netWeightSku;
    private LCP sumNetWeightFreightSku;
    LCP netWeightArticle;
    private LCP netWeightArticleSize;
    private LCP netWeightArticleSizeSku;
    private LCP netWeightSkuShipmentDetail;
    private LCP mainCompositionOriginDataSku;
    private LCP additionalCompositionOriginDataSku;
    private LCP mainCompositionOriginSku;
    private LCP additionalCompositionOriginSku;
    private LCP mainCompositionOriginSkuLanguage;
    private LCP additionalCompositionOriginSkuLanguage;
    AbstractCustomClass secondNameClass;
    private LCP nameOrigin;
    private ConcreteCustomClass category;
    public ConcreteCustomClass customCategory4;
    public ConcreteCustomClass customCategory6;
    public ConcreteCustomClass customCategory9;
    public ConcreteCustomClass customCategory10;
    ConcreteCustomClass customCategoryOrigin;
    ConcreteCustomClass subCategory;
    ConcreteCustomClass typeDuty;
    ConcreteCustomClass customStore;
    ConcreteCustomClass customsZone;
    public LCP customsZoneCustomCategory9;
    LCP nameCustomsZoneCustomCategory9;
    public LCP customsZoneCustomCategory10;
    LCP nameCustomsZoneCustomCategory10;
    LCP customsZoneCountry;
    LCP nameCustomsZoneCountry;

    private LCP typeInvoiceCategory;
    private LCP nameTypeInvoiceCategory;
    private LCP categoryArticle;
    private LCP warrantyCategory;
    private LCP warrantyCategoryArticleSku;
    private LCP warrantyDataSku;
    private LCP warrantySku;
        private LCP sidUnitOfMeasure;
    private LCP unitOfMeasureDataArticle;
    private LCP unitOfMeasureCategoryArticle;
    private LCP nameOriginUnitOfMeasureArticleSku;
    private LCP nameOriginCategoryArticle;
    private LCP nameCategoryArticle;
    private LCP categoryArticleSku;
    public LCP nameCategoryArticleSku;
    public LCP nameCategoryArticleSkuLanguage;
    public LCP nameCategoryArticleLanguage;
    private LCP nameOriginCategoryArticleSku;
    private LCP typeInvoiceCategoryArticle;
    private LCP typeInvoiceCategoryArticleSku;
    public LCP sidCustomCategory4;
    public LCP sidCustomCategory6;
    public LCP sidCustomCategory9;
    public LCP<PropertyInterface> sidCustomCategory10;
    LCP certificatedCustomCategory10;
    LCP specUnitOfMeasureCustomCategory10;
    LCP nameSpecUnitOfMeasureCustomCategory10;
    LCP additionalUnitOfMeasureCustomCategory10;
    LCP nameAdditionalUnitOfMeasureCustomCategory10;
    LCP sidCustomCategoryOrigin;
    public LCP numberIdCustomCategory10;
    LCP numberIdCustomCategoryOrigin;
    public LCP sidToCustomCategory4;
    public LCP sidToCustomCategory6;
    public LCP sidToCustomCategory9;
    public LCP sidToCustomCategory10;
    LCP sidToCustomCategoryOrigin;
    private LAP importBelTnved;
    private LAP importEuTnved;
    public LAP importUATnved;
    LAP importTnvedCountryMinPrices;
    LAP importTnvedDuty;
    public LCP customCategory4CustomCategory6;
    public LCP customCategory6CustomCategory9;
    public LCP customCategory9CustomCategory10;
    LCP customCategory6CustomCategory10;
    LCP customCategory4CustomCategory10;
    LCP customCategory6CustomCategoryOrigin;
    LCP customCategory4CustomCategoryOrigin;
    LCP customCategory10CustomCategoryOrigin;
    LCP sidCustomCategory10CustomCategoryOrigin;
    LCP typeFabricCustomCategory6;
    LCP nameTypeFabricCustomCategory6;
    LCP typeFabricCustomCategoryOrigin;
    LCP nameSubCategory;
    LCP nameToSubCategory;
    LCP relationCustomCategory10SubCategory;
    LCP subCategoryCustomCategory10;
    LCP countRelationCustomCategory10;
    LCP diffCountRelationCustomCategory10Sku;
    LCP diffCountRelationCustomCategory10FreightSku;
    LCP minPriceCustomCategory10SubCategory;
    LCP minPriceCustomCategory10SubCategoryCountry;
    public LCP dutyPercentCustomCategory10TypeDuty;
    public LCP dutySumCustomCategory10TypeDuty;
    LCP customsZoneTypeDuty;
    LCP nameCustomsZoneTypeDuty;
    LCP customsZoneSubCategory;
    LCP nameCustomsZoneSubCategory;
    private LCP sidCustomCategory4CustomCategory6;
    private LCP sidCustomCategory6CustomCategory9;
    private LCP sidCustomCategory9CustomCategory10;
    public LCP sidCustomCategory6CustomCategoryOrigin;
    LCP sidCustomCategory4CustomCategoryOrigin;
    private LCP nameCustomCategory4CustomCategory6;
    LCP sidCustomCategory6CustomCategory10;
    LCP sidCustomCategory4CustomCategory10;
    private LCP nameCustomCategory6CustomCategory9;
    private LCP nameCustomCategory9CustomCategory10;
    private LCP nameCustomCategory6CustomCategory10;
    private LCP nameCustomCategory4CustomCategory10;
    private LCP nameCustomCategory6CustomCategoryOrigin;
    private LCP nameCustomCategory4CustomCategoryOrigin;
    public LCP relationCustomCategory10CustomCategoryOrigin;
    private LCP customCategory10DataSku;
    private LCP customCategory10Sku;
    private LCP customCategory9Sku;
    private LCP customCategory6FreightSku;
    private LCP sidCustomCategory10Sku;
    private LCP customCategory10DataSkuCustomsZone;
    private LCP customCategory10SkuCustomsZone;
    private LCP customCategory10SkuFreight;
    private LCP sidCustomCategory10SkuFreight;
    private LCP subCategoryDataSku;
    private LCP subCategoryCustomCategory10Sku;
    private LCP subCategorySku;
    private LCP nameSubCategorySku;
    private LCP nameSubCategoryDataSku;

    private LCP subCategoryDataSkuCustomsZone;
    private LCP subCategoryCustomCategory10SkuCustomsZone;
    private LCP subCategorySkuCustomsZone;
    private LCP nameSubCategorySkuCustomsZone;
    private LCP subCategorySkuFreight;
    private LCP nameSubCategorySkuFreight;

    LCP customCategory10CustomCategoryOriginCustomsZone;
    LCP sidCustomCategory10CustomCategoryOriginCustomsZone;
    LCP nameCustomCategory10CustomCategoryOriginCustomsZone;

    private LCP customCategory10CustomCategoryOriginArticle;
    private LCP customCategory10CustomCategoryOriginArticleSku;
    private LCP mainCompositionArticle;
    private LCP additionalCompositionArticle;
    LCP mainCompositionOriginArticle;
    private LCP additionalCompositionOriginArticle;
    private LCP mainCompositionOriginArticleColor;
    private LCP additionalCompositionOriginArticleColor;
    private LCP mainCompositionOriginArticleSku;
    private LCP additionalCompositionOriginArticleSku;
    private LCP mainCompositionOriginArticleColorSku;
    private LCP additionalCompositionOriginArticleColorSku;
    private LCP mainCompositionSku;
    private LCP additionalCompositionSku;
    public LCP mainCompositionSkuLanguage;
    public LCP additionalCompositionSkuLanguage;
    LCP countrySupplierOfOriginArticle;
    private LCP countryOfOriginArticle;
    private LCP nameCountryOfOriginArticle;
    private LCP countryOfOriginArticleColor;
    private LCP countryOfOriginArticleColorSku;
    private LCP countryOfOriginArticleSku;
    private LCP nameCountryArticleColor;

    private LCP genderGenderSupplier;
    private LCP sidGenderGenderSupplier;
    private LCP genderSupplierArticleSku;
    private LCP sidGenderSupplierArticleSku;
    private LCP seasonYearSeasonSupplier;
    private LCP nameSeasonYearSeasonSupplier;

    private LCP countryOfOriginDataSku;
    private LCP countryOfOriginSku;
    private LCP nameCountrySkuLanguage;
    private LCP nameCountryOfOriginSku;
    private LCP nameCountrySupplierOfOriginArticle;
    private LCP nameCountryOfOriginArticleSku;

    private LCP genderBrandSupplier;
    private LCP sidGenderBrandSupplier;
    private LCP genderBrandSupplierArticle;
    private LCP genderOriginArticle;
    private LCP genderDataArticle;
    private LCP genderArticle;
    private LCP sidGenderArticle;
    private LCP genderArticleSku;
    public LCP sidGenderArticleSku;
    public LCP sidGenderArticleSkuLanguage;
    private LCP quantitySizeSupplierGenderCategory;
    private LCP commonSizeCategorySku;
    private LCP commonSizeTypeFabricSku;
    private LCP commonSizeDataSku;
    private LCP commonSizeSku;
    public LCP nameCommonSizeSku;
    public LCP typeFabricArticle;
    public LCP typeFabricCustomCategoryOriginArticle;
    public LCP overTypeFabricArticle;
    public LCP typeFabricArticleSku;
    public LCP nameTypeFabricArticle;
    public LCP nameTypeFabricArticleSku;
    public LCP nameTypeFabricArticleSkuLanguage;
    LCP articleSIDSupplier;
    public LCP articleCustomsSIDSupplier;
    private LAP seekArticleSIDSupplier;
    private LAP seekArticleSIDInvoice;
    LCP numberListArticle;
    LCP notZeroListArticle;
    private LCP articleSIDList;
    private LAP incrementNumberListSID;
    private LAP addArticleSingleSIDSupplier;
    private LAP addNEArticleSingleSIDSupplier;
     private LAP addNEArticleSingleSIDInvoice;
    private LAP addArticleCompositeSIDSupplier;
    private LAP addNEArticleCompositeSIDSupplier;
    private LAP addNEArticleCompositeSIDInvoice;
    private LCP numberListSIDArticle;
    private LCP inOrderInvoice;
    private LCP quantityOrderInvoiceSku;
    private LCP orderedOrderInvoiceSku;
    private LCP orderedInvoiceSku;
    private LCP invoicedOrderSku;
    private LCP invoicedOrderArticle;
    private LCP numberListSku;
    private LCP quantityListSku;
    private LCP orderedOrderInvoiceArticle;
    private LCP orderedInvoiceArticle;
    private LCP priceOrderInvoiceArticle;
    private LCP priceOrderedInvoiceArticle;
    ConcreteCustomClass supplierBox;
    ConcreteCustomClass boxInvoice;
    public ConcreteCustomClass simpleInvoice;
    LCP sidSupplierBox;
    private AbstractCustomClass list;
    LCP quantityDataListSku;
    LCP quantityDataList;
    LCP boxInvoiceSupplierBox;
    private LCP sidBoxInvoiceSupplierBox;
    private LCP supplierSupplierBox;
    private LCP supplierList;
    private LCP orderedSupplierBoxSku;
    private LCP quantityListArticle;
    private LCP orderedSimpleInvoiceSku;
    LCP priceDataDocumentItem;
    private LCP priceArticleDocumentSku;
    private LCP minPriceCustomCategoryFreightSku;
    private LCP minPriceCustomCategoryCountryFreightSku;
    private LCP minPriceRateCustomCategoryFreightSku;
    private LCP minPriceRateFreightSku;
    private LCP minPriceRateImporterFreightSku;
    private LCP minPriceRateImporterFreightArticle;
    private LCP minPriceRateWeightImporterFreightSku;
    private LCP diffPriceMinPriceImporterFreightArticle;
    private LCP diffPriceMinPriceImporterFreightSku;
    private LCP greaterPriceMinPriceImporterFreightArticle;
    private LCP greaterPriceMinPriceImporterFreightSku;
    private LCP dutyNetWeightFreightSku;
    private LCP dutyNetWeightImporterFreightSku;
    private LCP dutyPercentImporterFreightSku;
    private LCP dutyImporterFreightSku;
    private LCP sumDutyImporterFreightSku;
    private LCP sumDutyImporterFreight;
    private LCP priceDutyImporterFreightSku;
    private LCP NDSPercentOriginFreightSku;
    private LCP NDSPercentCustomFreightSku;
    private LCP NDSPercentFreightSku;
    private LCP NDSImporterFreightSku;
    private LCP priceFullDutyNDSImporterFreightSku;
    private LCP priceFullDutyNDSFreightSku;
    private LCP sumNDSImporterFreightSku;
    private LCP sumNDSImporterFreight;
    private LCP sumRegistrationFreightSku;
    private LCP sumRegistrationImporterFreightSku;
    private LCP sumRegistrationImporterFreight;
    private LCP sumCustomImporterFreight;
    private LCP minPriceRateCustomCategoryCountryFreightSku;
    private LCP priceImporterFreightSku;
    private LCP priceDocumentSku;
    private LCP priceRateDocumentArticle;
    private LCP priceRateOriginDocumentSku;
    private LCP priceRateDocumentSku;
    private LCP RRPRateDocumentArticle;
    private LCP sumDocumentSku;
    private LCP inOrderShipment;
    private LCP inInvoiceShipment;
    private LCP descriptionFreight;
    private LCP tonnageFreight;
    private LCP tonnageDataFreight;
    private LCP palletCountFreight;
    private LCP palletCountDataFreight;
    private LCP volumeFreight;
    private LCP volumeDataFreight;
    private LCP currencyFreight;
    private LCP nameCurrencyFreight;
    private LCP symbolCurrencyFreight;
    private LCP sumFreightFreight;
    private LCP sumRateFreightFreight;
    private LCP insuranceFreight;
    private LCP insuranceFreightBrandSupplier;
    private LCP insuranceFreightBrandSupplierArticle;
    private LCP insuranceFreightBrandSupplierSku;
    private LCP routeFreight;
    private LCP nameRouteFreight;
    private LCP dateShipmentFreight;
    private LCP dateArrivalFreight;
    private LCP exporterFreight;
    private LCP supplierFreight;
    private LCP nameSupplierFreight;
    private LCP countryFreight;
    private LCP nameCountryFreight;
    private LCP languageFreight;
    private LCP nameLanguageFreight;
    private LCP currencyCountryFreight;
    private LCP nameCurrencyCountryFreight;
    LCP customsZoneFreight;
    LCP dictionaryFreight;
    LCP nameOriginExporterFreight;
    LCP nameExporterFreight;
    LCP addressOriginExporterFreight;
    LCP addressExporterFreight;

    private LCP inInvoiceFreight;
    private LCP netWeightInvoicedFreight;
    public LCP contractImporterFreight;
    //public LCP nameContractImporterFreight;
    public LCP sidContractImporterFreight;
    public LCP dateContractImporterFreight;
    private LCP conditionShipmentContractImporterFreight;
    private LCP conditionPaymentContractImporterFreight;
    public LCP dateImporterFreightTypeInvoice;
    public LCP dateImporterFreight;
    private LCP dateShipmentImporterFreightTypeInvoice;
    private ConcreteCustomClass stock;
    private ConcreteCustomClass warehouse;
    private ConcreteCustomClass freightBox;
    private ConcreteCustomClass typeFabric;
    private AbstractCustomClass transaction;
    public LCP sidArticleSku;
    public LCP originalNameArticleSku;
    public LCP originalNameArticleSkuLanguage;
    public LCP coefficientArticle;
    public LCP coefficientArticleSku;
    public LCP sidTypeLabel;
    public LCP typeLabelArticle;
    public LCP nameTypeLabelArticle;
    public LCP typeLabelArticleSku;
    public LCP sidTypeLabelArticleSku;
    public LCP nameTypeLabelArticleSku;

    private LCP inSupplierBoxShipment;
    private LCP quantityArticle;
    private LCP quantityShipSku;
    private LCP quantitySupplierBoxBoxShipmentStockSku;
    private LCP quantitySupplierBoxBoxShipmentSku;
    private LCP quantitySimpleShipmentStockSku;
    private LCP quantitySimpleShipmentStockItem;
    private LCP barcodeAction4;
    LCP supplierBoxSIDSupplier;
    private LAP seekSupplierBoxSIDSupplier;
    private LCP between;
    private LCP betweenDates;
    private LCP quantityPalletShipmentBetweenDate;
    private LCP quantityPalletFreightBetweenDate;
    private LCP quantityShipmentStockSku;
    private LCP quantityShipmentRouteSku;
    private LCP quantityShipDimensionShipmentSku;
    private LCP diffListShipSku;
    private LCP supplierPriceDocument;
    private LCP percentShipmentRoute;
    private LCP percentShipmentRouteSku;
    private LCP invoicedShipmentSku;
    private LCP invoicedBetweenDateSku;
    private LCP invoicedBetweenDateBrandSupplier;
    private LCP quantityShipmentedBetweenDateSku;
    private LCP quantityShipmentedBetweenDateBrandSupplier;
    private LCP quantityShipmentedSku;
    private LCP emptyBarcodeShipment;
    private LCP priceShipmentSku;
    private LCP invoicedShipment;
    private LCP invoicedShipmentRouteSku;
    private LCP zeroInvoicedShipmentRouteSku;
    private LCP zeroQuantityShipmentRouteSku;
    private LCP diffShipmentRouteSku;
    private LCP sumShipmentRouteSku;
    private LCP sumShipmentRoute;
    private LCP sumShipmentSku;
    private LCP sumShipmentArticleColor;
    private LCP sumShipment;
    private LCP invoicedShipmentRoute;

    private LCP documentList;
    private LCP numberDocumentArticle;
    private ConcreteCustomClass shipDimension;
    private LCP quantityShipDimensionShipmentStockSku;
    private LCP quantityShipmentSku;
    private LCP orderedOrderShipmentSku;
    private LCP quantityOrderShipmentSku;
    private LCP shipmentedOrderSku;
    private LCP shipmentedAtTermOrderSku;
    private LCP quantityShipment;
    private LCP barcodeCurrentPalletRoute;
    private LCP barcodeCurrentFreightBoxRoute;
    private LCP equalsPalletFreight;
    private LCP equalsPalletFreightBox;
    private ConcreteCustomClass freightType;
    private ConcreteCustomClass creationSku;
    private ConcreteCustomClass creationFreightBox;
    private ConcreteCustomClass creationPallet;
    private LCP quantityCreationSku;
    private LCP quantityCreationPallet;
    private LCP routeCreationPallet;
    private LCP nameRouteCreationPallet;
    private LCP creationSkuSku;
    private LCP creationPalletPallet;
    private LCP routeCreationPalletPallet;
    private LCP nameRouteCreationPalletPallet;
    private LCP quantityCreationFreightBox;
    private LCP routeCreationFreightBox;
    private LCP nameRouteCreationFreightBox;
    private LCP creationFreightBoxFreightBox;
    private LCP routeCreationFreightBoxFreightBox;
    private LCP nameRouteCreationFreightBoxFreightBox;
    private LCP tonnageFreightType;
    private LCP palletCountFreightType;
    private LCP volumeFreightType;
    private LCP freightTypeFreight;
    private LCP nameFreightTypeFreight;
    private LCP palletNumberFreight;
    private LCP palletNumberProxyFreight;
    private LCP diffPalletFreight;
    private LCP barcodePalletFreightBox;
    private LCP freightBoxNumberPallet;
    private LCP freightBoxNumberFreight;
    private LCP notFilledShipmentRouteSku;
    private LCP routeToFillShipmentSku;
    private LCP seekRouteToFillShipmentBarcode;
    private LCP quantityShipmentArticle;
    private LCP oneShipmentArticle;
    private LCP oneShipmentArticleSku;
    private LCP oneShipmentArticleColorSku;
    private LCP oneShipmentArticleSizeSku;
    private LCP quantityShipmentArticleSize;
    private LCP quantityShipmentArticleColor;
    private LCP quantityShipmentArticleColorSize;
    private LCP quantityShipmentSize;
    private LCP oneShipmentArticleSize;
    private LCP oneShipmentArticleColor;

    private LCP oneShipmentSku;
    private LCP quantityBoxShipment;
    private LCP quantityShipmentStock;
    private LCP quantityShipmentPallet;
    private LCP quantityShipmentFreight;
    private LCP quantityShipmentFreightSku;
    private LCP quantityShipmentedFreightSku;
    private LCP quantityShipmentedAllFreightSku;
    private LCP quantityShipmentedFreightArticle;
    private LCP quantityShipmentedFreightBrandSupplier;
    private LCP importerShipmentFreight;
    private LCP nameImporterShipmentFreight;
    private LCP balanceStockSku;
    private LCP quantityStockSku;
    private LCP quantityFreightUnitSku;
    private LCP quantityFreightUnitArticle;
    private LCP quantityImporterDirectSupplierBoxSku;
    private LCP quantityStock;
    private LCP quantityFreightDestination;
    private LCP quantityStockArticle;
    private LCP quantityStockBrandSupplier;
    private LCP quantityFreightUnitBrandSupplier;
    private LCP stockNumberFreightBrandSupplier;
    private LCP quantityPalletSku;
    private LCP quantityPalletBrandSupplier;
    private LCP quantityAllPalletsBrandSupplier;
    private LCP quantityBrandSupplier;
    private LCP quantityRouteSku;
    private AbstractCustomClass destinationDocument;
    private LCP destinationInvoiceSupplierBox;
    private LCP destinationSupplierBox;
    private LCP destinationFreightBox;
    private LCP destinationFreightUnit;
    private LCP nameDestinationFreightBox;
    private LCP nameDestinationInvoiceSupplierBox;
    private LCP nameDestinationSupplierBox;
    private LCP nameDestinationFreightUnit;
    public LCP destinationDataSupplierBox;
    public LCP sidDestinationDataSupplierBox;
    public LCP nameDestinationDataSupplierBox;
    private LCP destinationCurrentFreightBoxRoute;
    private LCP nameDestinationCurrentFreightBoxRoute;
    private LCP quantityShipDimensionStock;
    private LCP isStoreFreightBoxSupplierBox;
    private LAP barcodeActionSetStore;
    public AbstractCustomClass destination;
    private AbstractCustomClass shipmentDetail;
    private ConcreteCustomClass boxShipmentDetail;
    private ConcreteCustomClass simpleShipmentDetail;
    private LCP skuShipmentDetail;
    private LCP boxShipmentBoxShipmentDetail;
    private LCP simpleShipmentSimpleShipmentDetail;
    private LCP quantityShipmentDetail;
    private LCP stockShipmentDetail;
    private LCP supplierBoxShipmentDetail;
    private LCP barcodeSkuShipmentDetail;
    private LCP shipmentShipmentDetail;
    private LAP addBoxShipmentDetailBoxShipmentSupplierBoxStockSku;
    private LAP addSimpleShipmentDetailSimpleShipmentStockSku;
    private LAP addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode;
    private LAP addBoxShipmentDetailBoxShipmentSupplierBoxRouteSku;
    private LAP addSimpleShipmentDetailSimpleShipmentRouteSku;
    private LAP addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode;
    private LCP articleShipmentDetail;
    private LCP sidArticleShipmentDetail;
    private LCP barcodeStockShipmentDetail;
    private LCP barcodeSupplierBoxShipmentDetail;
    private LCP sidSupplierBoxShipmentDetail;
    private LCP routeFreightBoxShipmentDetail;
    private LCP nameRouteFreightBoxShipmentDetail;
    private LAP addSimpleShipmentSimpleShipmentDetailStockBarcode;
    private LAP addSimpleShipmentDetailSimpleShipmentRouteBarcode;
    private AbstractGroup skuAttributeGroup;
    private AbstractGroup supplierAttributeGroup;
    private AbstractGroup intraAttributeGroup;
    private LCP colorSupplierItemShipmentDetail;
    private LCP sidColorSupplierItemShipmentDetail;
    private LCP nameColorSupplierItemShipmentDetail;
    private LCP sizeSupplierItemShipmentDetail;
    private LCP sidSizeSupplierItemShipmentDetail;
    private AbstractGroup itemAttributeGroup;
    private LCP originalNameArticleSkuShipmentDetail;
    private LCP categoryArticleSkuShipmentDetail;
    private LCP nameOriginCategoryArticleSkuShipmentDetail;
    private LCP nameCategoryArticleSkuShipmentDetail;
    private LCP coefficientArticleSkuShipmentDetail;
    private LCP customCategoryOriginArticleSkuShipmentDetail;
    private LCP sidCustomCategoryOriginArticleSkuShipmentDetail;
    private LCP netWeightArticleSkuShipmentDetail;
    private LCP countryOfOriginArticleSkuShipmentDetail;
    private LCP nameCountryOfOriginArticleSkuShipmentDetail;
    private LCP countryOfOriginSkuShipmentDetail;
    private LCP nameCountryOfOriginSkuShipmentDetail;
    private LCP genderArticleSkuShipmentDetail;
    private LCP sidGenderArticleSkuShipmentDetail;
    private LCP typeFabricArticleSkuShipmentDetail;
    private LCP nameTypeFabricArticleSkuShipmentDetail;
    private LCP mainCompositionOriginArticleSkuShipmentDetail;
    private LCP mainCompositionOriginSkuShipmentDetail;
    private LCP additionalCompositionOriginSkuShipmentDetail;
    private LCP unitOfMeasureArticleSkuShipmentDetail;
    private LCP nameOriginUnitOfMeasureArticleSkuShipmentDetail;
    private LCP nameUnitOfMeasureArticleSkuShipmentDetail;
    private LCP userShipmentDetail;
    private LCP nameUserShipmentDetail;
    private LCP timeShipmentDetail;
    private LCP barcodePrefix;
    private LAP createSku;
    private LAP createFreightBox;
    private LAP createPallet;
    private ConcreteCustomClass transfer;
    private LCP stockFromTransfer;
    private LCP barcodeStockFromTransfer;
    private LCP stockToTransfer;
    private LCP barcodeStockToTransfer;
    private LCP balanceStockFromTransferSku;
    private LCP balanceStockToTransferSku;
    private LCP quantityTransferSku;
    private LCP outcomeTransferStockSku;
    private LCP incomeTransferStockSku;
    private LCP incomeStockSku;
    private LCP outcomeStockSku;
    private AbstractCustomClass customCategory;
    public LCP nameCustomCategory;
    LCP customCategoryOriginArticle;
    LCP customCategory6Article;
    private LCP sidCustomCategory6Article;
    private LCP customCategoryOriginArticleSku;
    private LCP sidCustomCategoryOriginArticle;
    private LCP sidCustomCategoryOriginArticleSku;
    private LCP quantityBoxInvoiceBoxShipmentStockSku;
    private LCP quantityInvoiceShipmentStockSku;
    private LCP invoicedSimpleInvoiceSimpleShipmentStockSku;
    private LCP invoicedSimpleInvoiceSimpleShipmentStockArticleComposite;
    private LCP invoicedSimpleInvoiceSimpleShipmentStockItem;
    private LCP quantityDataSimpleInvoiceSimpleShipmentStockSku;
    private LCP quantitySimpleInvoiceSimpleShipmentStockItem;
    private LCP quantitySkuSimpleInvoiceSimpleShipmentStockSku;
    private LCP quantityArticleSimpleInvoiceSimpleShipmentStockItem;
    private LCP quantityInvoiceStockSku;
    private LCP quantityInvoiceSku;
    private LCP quantityInvoice;
    private LCP diffDocumentInvoiceSku;
    private LCP quantitySupplierBoxSku;
    private LCP quantityDirectSupplierBoxSku;
    private LCP quantityDirectListSku;
    private LCP quantityDirectSimpleInvoiceSku;
    private LCP quantitySupplierBox;
    private LCP zeroQuantityListSku;
    private LCP zeroQuantityShipDimensionShipmentSku;
    private LCP diffListSupplierBoxSku;
    private LCP diffListSupplierBox;
    private LCP zeroQuantityShipmentSku;
    private LCP zeroInvoicedShipmentSku;
    private LCP diffShipmentSku;
    private ConcreteCustomClass importer;
    private ConcreteCustomClass exporter;
    private LCP sidContract;
    private LCP dateContract;
    private LCP conditionShipmentContract;
    private LCP conditionPaymentContract;

    private LCP currencyContract;
    private LCP nameCurrencyContract;
    private LCP contractImporter;
    public LCP sidImporter;
    private LCP exporterInvoice, exporterProxyInvoice;
    private LCP nameExporterInvoice;
    private LCP importerDirectInvoice;
    private LCP nameImporterDirectInvoice;
    //private LCP contractInvoice;
    //private LCP sidContractInvoice;
    private LCP freightFreightBox;
    private LCP importerSupplierBox;
    private LCP routeFreightFreightBox;
    private LCP importerShipmentRoute;
    private LCP nameImporterShipmentRoute;
    private LCP importerShipmentFreightBox;
    public LCP quantityImporterStockSku;
    private LCP quantityDirectImporterFreightUnitSku;
    private LCP quantityImporterStock;
    private LCP quantityImporterStockArticle;
    private LCP quantityImporterStockTypeInvoice;
    private LCP quantityProxyImporterFreightSku;
    private LCP quantityDirectImporterFreightSku;
    private LCP quantityImporterFreightSku;

    private LCP netWeightStockSku;
    private LCP netWeightStockArticle;
    private LCP netWeightStock;
    private LCP netWeightImporterFreightUnitSku;
    private LCP netWeightImporterFreightUnitArticle;
    private LCP netWeightImporterFreightUnitTypeInvoice;
    private LCP netWeightImporterFreightUnit;
    private LCP grossWeightImporterFreightUnitSku;
    private LCP grossWeightImporterFreightUnitArticle;
    private LCP grossWeightImporterFreightUnitTypeInvoice;
    private LCP grossWeightImporterFreightUnit;
    private LCP priceInInvoiceStockSku;
    private LCP RRPInInvoiceStockSku;
    private LCP currencyRRPInInvoiceStockSku;
    //private LCP contractInInvoiceStockSku;
    private LCP priceInInvoiceShipmentStockSku;
    private LCP RRPInInvoiceShipmentStockSku;
    private LCP currencyRRPInInvoiceShipmentStockSku;
    //private LCP contractInInvoiceShipmentStockSku;
    private LCP priceInShipmentStockSku;
    private LCP RRPInShipmentStockSku;
    private LCP currencyRRPInShipmentStockSku;
    private LCP priceInShipmentDetail;
    //private LCP contractInShipmentStockSku;
    public LCP priceInImporterFreightSku;
    public LCP priceInImporterFreightArticle;
    public LCP priceInFreightSku;
    public LCP priceInFreightArticle;
    public LCP RRPInImporterFreightSku;
    private LCP netWeightImporterFreightSku;
    private LCP netWeightImporterFreightArticle;
    private LCP netWeightProxyImporterFreightSku;
    private LCP netWeightDirectImporterFreightSku;
    private LCP typeInvoiceDataFreightArticle;
    private LCP typeInvoiceCategoryFreightArticle;
    private LCP typeInvoiceFreightArticle;
    private LCP typeInvoiceFreightSku;
    private LCP nameTypeInvoiceFreightArticleSku;
    private LCP netWeightImporterFreightTypeInvoice;
    private LCP grossWeightImporterFreightSku;
    private LCP grossWeightProxyImporterFreightSku;
    private LCP grossWeightDirectImporterFreightSku;
    private LCP grossWeightImporterFreightTypeInvoice;
    private LCP netWeightImporterFreightCustomCategory6;
    private LCP netWeightImporterFreightCustomCategory6Category;
    private LCP netWeightImporterFreightSupplierCustomCategory6;
    private LCP netWeightImporterFreightSupplierCustomCategory6Category;
    private LCP grossWeightImporterFreightCustomCategory6;
    private LCP grossWeightImporterFreightCustomCategory6Category;
    private LCP grossWeightImporterFreightSupplierCustomCategory6;
    private LCP grossWeightImporterFreightSupplierCustomCategory6Category;
    private LCP netWeightImporterFreight;
    private LCP netWeightImporterFreightSupplier;
    private LCP grossWeightImporterFreightSupplier;
    private LCP grossWeightImporterFreight;
    private LCP priceFreightImporterFreightSku;
    private LCP priceInsuranceImporterFreightSku;
    private LCP priceFreightInsuranceImporterFreightSku;
    private LCP priceFreightInsuranceFreightSku;
    private LCP priceFullImporterFreightSku;
    private LCP priceInFullImporterFreightSku;
    private LCP priceInFullFreightSku;
    private LCP priceFullKgImporterFreightSku;
    private LCP sumFullImporterFreightArticle;
    private LCP priceFullKgImporterFreightArticle;
    private LCP priceFullDutyImporterFreightSku;
    private LCP priceInFullDutyImporterFreightSku;
    private LCP priceInFullDutyNDSFreightSku;
    private LCP oneArticleSkuShipmentDetail;
    private LCP oneArticleColorShipmentDetail;
    private LCP oneArticleSizeShipmentDetail;
    private LCP oneSkuShipmentDetail;
    private LCP quantityImporterFreight;
    private LCP quantityFreight;
    private LCP quantityProxyImporterFreight;
    private LCP quantityImporterFreightTypeInvoice;
    private LCP quantityImporterFreightSupplier;
    private LCP quantityImporterFreightArticle;
    private LCP quantityImporterFreightBrandSupplier;
    private LCP markupPercentImporterFreightBrandSupplier;
    private LCP markupPercentImporterFreightBrandSupplierArticle;
    private LCP markupPercentImporterFreightDataArticle;
    private LCP markupPercentImporterFreightArticle;
    private LCP markupPercentImporterFreightArticleSku;
    private LCP markupPercentImporterFreightDataSku;
    private LCP markupPercentImporterFreightBrandSupplierSku;
    private LCP markupInImporterFreightSku;
    private LCP priceExpenseImporterFreightSku;
    private LCP markupPercentImporterFreightSku;
    private LCP sumPercentImporterFreightBrandSupplier;
    private LCP averagePercentImporterFreightBrandSupplier;
    private LCP markupImporterFreightSku;
    private LCP priceMarkupInImporterFreightSku;
    private LCP priceOutImporterFreightSku;
    private LCP priceInOutImporterFreightSku;
    public LCP sumInImporterStockSku, sumInImporterFreightSku;
    private LCP sumInProxyImporterFreightSku;
    private LCP sumMarkupImporterFreightSku;
    private LCP sumOutImporterFreightSku;
    private LCP sumInImporterFreight;
    private LCP sumMarkupImporterFreight;
    private LCP sumMarkupInImporterFreight;
    private LCP sumMarkupInImporterFreightSku;
    private LCP sumInOutImporterFreightSku;
    private LCP sumInOutProxyImporterFreightSku;
    private LCP sumInOutDirectImporterFreightSku;
    private LCP sumInOutImporterFreightTypeInvoice;
    private LCP sumImporterFreightUnitSku;
    private LCP sumImporterFreightUnitArticle;
    private LCP sumMarkupInFreight;
    private LCP sumImporterFreight;
    private LCP sumImporterFreightTypeInvoice;
    private LCP sumSbivkaImporterFreight;
    private LCP sumInImporterFreightArticle;
    private LCP sumInImporterFreightBrandSupplier;
    private LCP sumInOutImporterFreightArticle;
    private LCP sumInOutImporterFreightBrandSupplier;
    private LCP sumImporterFreightSupplier;
    private LCP sumInFreightArticle;
    private LCP sumInFreightBrandSupplier;
    private LCP sumInOutFreightArticle;
    private LCP sumInOutFreightBrandSupplier;
    private LCP sumInOutFreightBrandSupplierArticle;
    private LCP sumInOutFreightBrandSupplierSku;
    private LCP sumOutImporterFreight;
    private LCP sumInOutImporterFreight;
    private LCP sumInOutFreight;
    private LCP sumInFreight;
    private LCP sumMarkupFreight;
    private LCP sumOutFreight;
    private LCP sumFreightImporterFreightSku;
    private LCP insuranceImporterFreightSku;
    private LCP quantityProxyImporterFreightCustomCategory6Category;
    private LCP quantityProxyImporterFreightCustomCategory6;
    private LCP quantityDirectImporterFreightSupplierCustomCategory6;
    private LCP quantityDirectImporterFreightSupplierCustomCategory6Category;
    private LCP quantityFreightArticle;
    private LCP quantityDirectFreightSku;
    private LCP quantityDirectImporterFreightSupplier;
    private LCP quantityFreightBrandSupplier;
    //private LCP quantityFreightSupplier;
    private LCP quantityFreightSku;
    private LCP quantityFreightedBetweenDateSku;
    private LCP quantityFreightedSku;
    private LCP balanceSku;
    private LCP quantityFreightCategory;
    private LCP sumImporterFreightSku;
    private LCP sumProxyImporterFreightSku;
    private LCP sumDirectImporterFreightSku;
    private LCP sumImporterFreightCustomCategory6;
    private LCP sumImporterFreightCustomCategory6Category;
    private LCP sumImporterFreightSupplierCustomCategory6;
    private LCP sumImporterFreightSupplierCustomCategory6Category;

    private LCP quantityFreightCategoryGenderCompositionTypeFabric;
    private LCP customCategory10CategoryGenderCompositionTypeFabric;
    public LCP customCategory10CategoryGenderCompositionTypeFabricCustomsZone;
    private LCP sidCustomCategory10CategoryGenderCompositionTypeFabricCustomsZone;
    private LCP customCategory10CategoryGenderCompositionTypeFabricFreight;
    private LCP sidCustomCategory10CategoryGenderCompositionTypeFabricFreight;
    private LCP customCategory10CategoryGenderCompositionTypeFabricSkuCustomsZone;
    private LCP sidCustomCategory10CategoryGenderCompositionTypeFabric;
    private LCP customCategory10CategoryGenderCompositionTypeFabricSku;

    LCP quantityImporterFreightArticleCompositionCountryCategory;
    LCP compositionFreightArticleCompositionCountryCategory;
    LCP netWeightImporterFreightArticleCompositionCountryCategory;
    LCP grossWeightImporterFreightArticleCompositionCountryCategory;
    LCP priceImporterFreightArticleCompositionCountryCategory;
    public LCP priceInvoiceImporterFreightSku;
    LCP markupInOutImporterFreightSku;
    public LCP sumInvoiceImporterStockSku;
    LCP sumImporterFreightArticleCompositionCountryCategory;
    LCP sumProxyInvoiceImporterFreightSku;
    private ConcreteCustomClass freightComplete;
    private ConcreteCustomClass freightPriced;
    private ConcreteCustomClass freightChanged;
    private ConcreteCustomClass freightShipped;
    private ConcreteCustomClass freightArrived;
    private LCP dictionaryComposition;
    private LCP nameDictionaryComposition;
    private LCP dictionaryName;
    private LCP nameDictionaryName;
    private LAP translationMainCompositionSku;
    private LAP translationAdditionalCompositionSku;
    private LAP translationMainCompositionSkuLanguage;
    private LAP translationAdditionalCompositionSkuLanguage;
    private LCP mainCompositionSkuFreight;
    private LAP translationMainCompositionSkuFreight;
    private LCP additionalCompositionSkuFreight;
    private LAP translationAdditionalCompositionSkuFreight;
    private LAP translationAllMainComposition;
    private LAP translationInvoiceMainComposition;
    private LAP translationInvoiceAdditionalComposition;
    private LAP translationInvoiceName;
    private LAP translationInvoiceLanguageName;
    private LAP translationMainCompositionFreightSku;
    private LAP translationMainCompositionLanguageFreightSku;
    private LAP translationAdditionalCompositionLanguageFreightSku;
    LCP mainCompositionLanguageFreightSku;
    LCP additionalCompositionLanguageFreightSku;

    private LAP translationAdditionalCompositionFreightSku;
    private LCP sidShipmentShipmentDetail;
    private LCP commonSizeSizeSupplier;
    private LCP nameCommonSizeSizeSupplier;
    private LCP commonSizeSizeSupplierGenderCategory;
    private LCP nameCommonSizeSizeSupplierGenderCategory;
    private LCP commonSizeSizeSupplierGenderCategoryTypeFabric;
    private LCP nameCommonSizeSizeSupplierGenderCategoryTypeFabric;
    LCP colorSIDSupplier;
    LCP sidSizeSupplier;
    LCP sidThemeSupplier;
    LCP sidCollectionSupplier;
    LCP sidSubCategorySupplier;
    LCP sidGender;
    LCP sidGenderSupplier;
    LCP sidSeasonSupplier;
    LCP sizeSIDSupplier;
    LCP themeSIDSupplier;
    LCP collectionSIDSupplier;
    LCP subCategorySIDSupplier;
    LCP genderSIDSupplier;
    LCP destinationSIDSupplier;
    LCP brandSIDSupplier;
    LCP countryNameSupplier;
    LCP numberDataListSku;
    private LCP numberArticleListSku;
    private LCP grossWeightFreightSku;
    private LCP netWeightFreightSku;
    private LCP customCategory10FreightSku;
    private LCP sidCustomCategory10FreightSku;
    private LCP subCategoryFreightSku;
    private LCP nameSubCategoryFreightSku;
    private LCP customCategoryOriginFreightSku;
    private LCP sidCustomCategoryOriginFreightSku;
    private LCP mainCompositionOriginFreightSku;
    public LCP mainCompositionFreightSku;
    public LCP userNumberFreightSku;
    private LCP additionalCompositionOriginFreightSku;
    private LCP additionalCompositionFreightSku;
    private LCP countryOfOriginFreightSku;
    private LCP sidCountryOfOriginFreightSku;
    public LCP nameCountryOfOriginFreightSku;
    private LCP equalsItemArticleComposite;
    private LAP executeArticleCompositeItemSIDSupplier;
    private LAP executeChangeFreightClass;
    private LAP executeChangeFreightClassApply;
    private LAP executeChangeFreightChangedClass;
    private CreateItemFormEntity createItemForm;
    private EditItemFormEntity editItemForm;
    private FindItemFormEntity findItemFormBox, findItemFormBoxBarcode;
    private FindItemFormEntity findItemFormSimple, findItemFormSimpleBarcode;
    private LogFormEntity logFreightForm;
    private LAP formLogFreight;

    private LAP addItemBarcode;
    private LAP barcodeActionSeekFreightBox;
    private LCP currentPalletFreightBox;
    private LAP barcodeActionCheckFreightBox;
    private LAP packingListFormFreightBox;
    private LAP packingListFormRoute;
    LCP quantitySupplierBoxBoxShipmentRouteSku;
    LCP quantitySimpleShipmentRouteSku;
    LCP routePallet, freightPallet, nameRoutePallet, palletFreightBox;
    private LCP currentPalletRouteUser;
    LCP currentPalletRoute;
    private LCP currentFreightBoxRouteUser;
    LCP currentFreightBoxRoute;
    LCP isCurrentFreightBox, isCurrentPalletFreightBox;
    LCP isCurrentPallet;
    private LCP changePallet;
    LAP seekRouteShipmentSkuRoute;
    LAP barcodeActionSeekPallet;
    LAP barcodeActionSetPallet;
    LAP barcodeActionSetPalletFreightBox;
    LAP barcodeActionSetFreight;
    LCP barcodeAction3;
    private LAP invoiceOriginFormImporterFreight;
    private LAP invoiceFormImporterFreight;
    private LAP invoiceExportFormImporterFreight;
    private LAP proformOriginFormImporterFreight;
    private LAP proformFormImporterFreight;
    private LAP annexInvoiceOriginFormImporterFreight;
    private LAP annexInvoiceFormImporterFreight;
    private LAP packingListOriginFormImporterFreight;
    private LAP packingListFormImporterFreight;
    private LAP sbivkaFormImporterFreight;
    private LAP sbivkaFormImporterFreightSupplier;
    private LAP listFreightUnitFormImporterFreight;

    private LCP countrySupplierOfOriginArticleSku;
    private LCP nameCountrySupplierOfOriginArticleSku;
    private AbstractCustomClass innerInvoice;
    private AbstractCustomClass directInvoice;
    private ConcreteCustomClass directBoxInvoice;
    private ConcreteCustomClass directSimpleInvoice;
    private LCP freightDirectInvoice;
    private LCP equalsDirectInvoiceFreight;
    private LCP grossWeightDirectInvoice;
    private LCP palletNumberDirectInvoice;
    private LCP sid3Country;
    public  LCP sidOrigin2ToCountry;
    private LCP nameCountrySku;
    public LCP countryBrandSupplierSku;
    public LCP nameCountryBrandSupplierSku;
    public LCP nameCountryBrandSupplierSkuLanguage;
    private LCP sumInCurrentYear;
    private LCP sumInOutCurrentYear;
    private LCP balanceSumCurrentYear;
    private AbstractCustomClass freightUnit;
    private LCP quantityInvoiceFreightUnitSku;
    private LCP freightSupplierBox;
    private LCP freightSimpleInvoice;
    private LCP freightFreightUnit;
    private LCP priceRateSupplierBoxSku;
    private LCP RRPRateSupplierBoxSku;
    private LCP priceInInvoiceFreightUnitSku;
    ConcreteCustomClass boxSupplier;
    ConcreteCustomClass simpleSupplier;
    ConcreteCustomClass jennyferSupplier;
    ConcreteCustomClass teddySupplier;
    ConcreteCustomClass dieselSupplier;
    ConcreteCustomClass steilmannSupplier;
    ConcreteCustomClass tallyWeijlSupplier;
    ConcreteCustomClass hugoBossSupplier;
    ConcreteCustomClass gerryWeberSupplier;
    ConcreteCustomClass topazSupplier;
    ConcreteCustomClass aprioriSupplier;
    ConcreteCustomClass mexxSupplier;
    ConcreteCustomClass bestsellerSupplier;
    ConcreteCustomClass sOliverSupplier;
    ConcreteCustomClass womenSecretSupplier;
    ConcreteCustomClass babyPhatSupplier;
    private LAP steilmannImportInvoice;
    private LAP dieselImportInvoice;
    private LAP jennyferImportInvoice;
    private LAP teddyImportInvoice;
    private LCP jennyferImportArticleWeightInvoice;
    private LAP tallyWeijlImportInvoice;
    private LAP hugoBossImportInvoice;
    private LAP gerryWeberImportInvoice;
    public LAP<?> mexxImportInvoice;
    public LAP<?> mexxImportPricesInvoice;
    public LAP<?> mexxImportArticleInfoInvoice;
    public LAP<?> mexxImportColorInvoice;
    private LAP mexxImportDelivery;
    private LAP bestsellerImportInvoice;
    private LAP hugoBossImportPricat;
    private LAP gerryWeberImportPricat;
    private LAP sOliverImportInvoice;
    private LAP womenSecretImportInvoice;
    private LAP topazImportInvoice;
    private LAP aprioriImportInvoice;
    public LAP mexxImportOrder;
    public LAP dieselImportOrder;

    private AbstractGroup importInvoiceActionGroup;
    private AbstractGroup importOrderActionGroup;
    private LAP skuPrintFA;
    private LAP printCreateSkuForm;
    private LAP printCreatePalletForm;
    private LAP printCreateFreightBoxForm;
    private LCP priceSupplierBoxSku;
    private LCP sumSupplierBoxSku;
    private LCP nameArticleSku;
    private LCP freightShippedFreightBox;
    private LCP freightShippedDirectInvoice;
    private LCP quantityDirectInvoicedSku;
    private LCP quantityStockedSku;
    private LCP quantitySku;
    private LCP quantityAllSku;
    private LCP sumInInvoiceStockSku;
    private LCP sumStockedSku;
    private LCP sumDirectInvoicedSku;
    private LCP sumSku;
    private LCP netWeightDocumentSku;
    private LCP barcode10;
    private LCP steilmannSupplierArticle;
    private LCP skuJennyferBarcode10;
    private LCP jennyferSupplierArticle;
    private LCP jennyferSupplierArticleSku;
    private LCP substring10;
    private LCP skuJennyferBarcode;
    private LCP substring10s13;
    private LCP skuBarcodeObject;

    private LCP typeSupplier;
    private LCP noBarcodeSupplier;
    private LCP nameClassFreight;
    private LCP logFreight;

    ConcreteCustomClass pricat;
    LCP<PropertyInterface> barcodePricat;
    LCP articleNumberPricat;
    LCP customCategoryOriginalPricat;
    LCP themeCodePricat;
    LCP themeNamePricat;
    LCP brandNamePricat;
    LCP colorCodePricat;
    LCP colorNamePricat;
    LCP subCategoryCodePricat;
    LCP subCategoryNamePricat;
    LCP sizePricat;
    LCP seasonPricat;
    LCP genderPricat;
    LCP originalNamePricat;
    LCP countryPricat;
    LCP netWeightPricat;
    LCP compositionPricat;
    LCP pricePricat;
    LCP rrpPricat;
    LCP supplierPricat;
    LCP barcodeToPricat;
    LAP importPricatSupplier;
    LCP destinationPricat;

    private ConcreteCustomClass stamp;
    private ConcreteCustomClass creationStamp;
    LCP sidStamp;
    LCP dateOfStamp;
    LCP seriesOfStamp;
    LCP stampShipmentDetail;
    LCP sidStampShipmentDetail;
    LCP seriesOfStampShipmentDetail;
    LCP hideSidStampShipmentDetail;
    LCP hideSeriesOfStampShipmentDetail;
    LCP necessaryStampCategory;
    LCP necessaryStampSkuShipmentDetail;
    LCP shipmentDetailStamp;
    LCP firstNumberCreationStamp;
    LCP lastNumberCreationStamp;
    LCP dateOfStampCreationStamp;
    LCP seriesOfStampCreationStamp;
    LAP createStamp;
    LCP creationStampStamp;
    private ConcreteCustomClass transitDocument;
    private LCP sidTransitDocument;
    private LCP dateRepaymentTransitDocument;
    private LCP dateClosingTransitDocument;
    private LCP sellerTransitDocument;
    private LCP nameSellerTransitDocument;
    private LCP importerTransitDocument;
    private LCP freightTransitDocument;
    private LCP typeTransitTransitDocument;
    private LCP nameTypeTransitTransitDocument;
    
    LCP<?> date;
    LCP<PropertyInterface> barcode;
    LCP<PropertyInterface> barcodeToObject;
    LCP barcodeObjectName;
    LCP equalsObjectBarcode;
    LAP seekBarcodeAction;
    LCP reverseBarcode;

    LCP scalesSpeed;
    LCP scalesComPort;
    LCP scannerComPort;
    LCP scannerSingleRead;

    private LAP declarationExport;
    private LAP invoiceExportDbf;

    public AnnexInvoiceFormEntity invoiceFromFormEntity;
    public InvoiceExportFormEntity invoiceExportForm;

    @Override
    public void initModuleDependencies() {
        setRequiredModules(Arrays.asList("System", "Utils", "I18n", "Hierarchy", "Historizable", "Numerator", "Stock", "Document", "Country"));
    }

    @Override
    public void initModule() {
    }

    AbstractCustomClass named;

    @Override
    public void initClasses() {
        initBaseClassAliases();

        named = addAbstractClass("Named", "Объект с именем", baseClass);

        multiLanguageNamed = (ConcreteCustomClass) BL.I18n.getClassByName("MultiLanguageNamed");
        
        destination = addAbstractClass("Destination", "Пункт назначения", baseClass);

        store = addConcreteClass("Store", "Магазин", destination, (CustomClass) BL.Store.getClassByName("Store"));//  named

        barcodeObject = addAbstractClass("BarcodeObject", "Штрихкод", baseClass);
        sku = addAbstractClass("Sku", "SKU", barcodeObject, (CustomClass) BL.Stock.getClassByName("Sku"));

        article = addAbstractClass("Article", "Артикул", baseClass);
        articleComposite = addConcreteClass("ArticleComposite", "Артикул (составной)", article);

        articleSingle = addConcreteClass("ArticleSingle", "Артикул (простой)", sku, article);

        pricat = addConcreteClass("Pricat", "Прикат", baseClass);

        item = addConcreteClass("Item", "Товар", sku);

        transaction = addAbstractClass("Transaction", "Транзакция", baseClass);
        document = addAbstractClass("Document", "Документ", transaction);
        list = addAbstractClass("List", "Список", baseClass);

        contract = addConcreteClass("Contract", "Договор", transaction, (CustomClass) BL.StockContract.getClassByName("ContractSku"));

        priceDocument = addAbstractClass("PriceDocument", "Документ с ценами", document);
        destinationDocument = addAbstractClass("DestinationDocument", "Документ в пункт назначения", document);

        order = addConcreteClass("Order", "Заказ", priceDocument, destinationDocument, list);

        typeInvoice = addConcreteClass("TypeInvoice", "Тип инвойса", named);

        stock = addConcreteClass("Stock", "Место хранения", barcodeObject);  //, (CustomClass) BL.Stock.getClassByName("stock")

        freightUnit = addAbstractClass("FreightUnit", "Машиноместо", baseClass);

        invoice = addAbstractClass("Invoice", "Инвойс", priceDocument, destinationDocument);//, (CustomClass) BL.ContractLedger.getClassByName("contractLedger"), (CustomClass) BL.ContractLedger.getClassByName("contractALedger"), (CustomClass) BL.ContractLedger.getClassByName("inContractLedger"));
        boxInvoice = addConcreteClass("BoxInvoice", "Инвойс по коробам", invoice);

        innerInvoice = addAbstractClass("InnerInvoice", "Внутренний инвойс", baseClass);

        directInvoice = addAbstractClass("DirectInvoice", "Инвойс (напрямую)", invoice, innerInvoice);
        directBoxInvoice = addConcreteClass("DirectBoxInvoice", "Инвойс по коробам (напрямую)", boxInvoice, directInvoice);

        simpleInvoice = addConcreteClass("SimpleInvoice", "Инвойс без коробов", invoice, list);
        directSimpleInvoice = addConcreteClass("DirectSimpleInvoice", "Инвойс без коробов (напрямую)", simpleInvoice, directInvoice, freightUnit);

        shipDimension = addConcreteClass("ShipDimension", "Разрез поставки", baseClass);

        supplierBox = addConcreteClass("SupplierBox", "Короб поставщика", list, shipDimension, barcodeObject, freightUnit);

        shipment = addAbstractClass("Shipment", "Поставка", document);
        boxShipment = addConcreteClass("BoxShipment", "Поставка по коробам", shipment);
        simpleShipment = addConcreteClass("SimpleShipment", "Поставка без коробов", shipment, shipDimension);

        shipmentDetail = addAbstractClass("ShipmentDetail", "Строка поставки", baseClass);
        boxShipmentDetail = addConcreteClass("BoxShipmentDetail", "Строка поставки по коробам", shipmentDetail);
        simpleShipmentDetail = addConcreteClass("SimpleShipmentDetail", "Строка поставки без коробов", shipmentDetail);

        seller = addAbstractClass("Seller", "Продавец", baseClass);
        //buyer = addAbstractClass("Buyer", "Покупатель", named);


        supplier = addAbstractClass("Supplier", "Поставщик", named, seller, (CustomClass) BL.Supplier.getClassByName("Supplier"));

        boxSupplier = addConcreteClass("BoxSupplier", "Поставщик по коробам", supplier);
        simpleSupplier = addConcreteClass("SimpleSupplier", "Поставщик без коробов", supplier);

        jennyferSupplier = addConcreteClass("JennyferSupplier", "Jennyfer", boxSupplier);
        teddySupplier = addConcreteClass("TeddySupplier", "Teddy", boxSupplier);
        dieselSupplier = addConcreteClass("DieselSupplier", "Diesel", boxSupplier);
        steilmannSupplier = addConcreteClass("SteilmannSupplier", "Steilmann", boxSupplier);
        tallyWeijlSupplier = addConcreteClass("TallyWeijlSupplier", "Tally Weijl", boxSupplier);
        hugoBossSupplier = addConcreteClass("HugoBossSupplier", "Hugo Boss", simpleSupplier);
        mexxSupplier = addConcreteClass("MexxSupplier", "Mexx", boxSupplier);
        bestsellerSupplier = addConcreteClass("BestsellerSupplier", "Bestseller", boxSupplier);
        sOliverSupplier = addConcreteClass("SOliverSupplier", "s.Oliver", simpleSupplier);
        womenSecretSupplier = addConcreteClass("WomenSecretSupplier", "Women'Secret", boxSupplier);
        babyPhatSupplier = addConcreteClass("BabyPhatSupplier", "Baby Phat", simpleSupplier);
        gerryWeberSupplier = addConcreteClass("GerryWeberSupplier", "Gerry Weber", simpleSupplier);
        topazSupplier = addConcreteClass("TopazSupplier", "Topaz", simpleSupplier);
        aprioriSupplier = addConcreteClass("AprioriSupplier", "Apriori", simpleSupplier);

        secondNameClass = addAbstractClass("SecondNameClass", "Класс со вторым именем", baseClass);

        subject = addAbstractClass("Subject", "Субъект", named, secondNameClass);
        importer = addConcreteClass("Importer", "Импортер", subject, (CustomClass) BL.Company.getClassByName("Company"));
        exporter = addConcreteClass("Exporter", "Экспортер", subject, seller, (CustomClass) BL.Company.getClassByName("Company"), multiLanguageNamed);

        commonSize = addConcreteClass("CommonSize", "Размер", named);

        colorSupplier = addConcreteClass("ColorSupplier", "Цвет поставщика", named, secondNameClass, multiLanguageNamed);
        sizeSupplier = addConcreteClass("SizeSupplier", "Размер поставщика", baseClass);
        gender = addConcreteClass("Gender", "Пол", named, multiLanguageNamed);
        genderSupplier = addConcreteClass("GenderSupplier", "Пол поставщика", baseClass);
        sizeGroupSupplier = addConcreteClass("SizeGroupSupplier", "Размерная сетка", named);
        seasonSupplier = addConcreteClass("SeasonSupplier", "Сезон поставщика", baseClass);

        typeFabric = addConcreteClass("TypeFabric", "Тип одежды", named, multiLanguageNamed);

        freightBox = addConcreteClass("FreightBox", "Короб для транспортировки", stock, freightUnit);

        freight = addConcreteClass("Freight", "Фрахт", named, transaction, (CustomClass) BL.Numerator.getClassByName("NumeratedObject"));
        freightComplete = addConcreteClass("FreightComplete", "Скомплектованный фрахт", freight);
        freightChanged = addConcreteClass("FreightChanged", "Обработанный фрахт", freightComplete);
        freightPriced = addConcreteClass("FreightPriced", "Расцененный фрахт", freightChanged);
        freightShipped = addConcreteClass("FreightShipped", "Отгруженный фрахт", freightPriced);
        freightArrived = addConcreteClass("FreightArrived", "Прибывший фрахт", freightShipped);

        freightType = addConcreteClass("FreightType", "Тип машины", named);

        pallet = addConcreteClass("Pallet", "Паллета", barcodeObject);

        category = addConcreteClass("Category", "Номенклатурная группа", secondNameClass, named, multiLanguageNamed,
                                                                         (CustomClass) BL.Stock.getClassByName("SkuGroup"));

        customCategory = addAbstractClass("CustomCategory", "Уровень ТН ВЭД", baseClass);

        customCategory4 = addConcreteClass("CustomCategory4", "Первый уровень", customCategory);
        customCategory6 = addConcreteClass("CustomCategory6", "Второй уровень", customCategory);
        customCategory9 = addConcreteClass("CustomCategory9", "Третий уровень", customCategory);
        customCategory10 = addConcreteClass("CustomCategory10", "Четвёртый уровень", customCategory);

        customCategoryOrigin = addConcreteClass("CustomCategoryOrigin", "ЕС уровень", customCategory);

        subCategory = addConcreteClass("SubCategory", "Дополнительное деление", baseClass);

        typeDuty = addConcreteClass("TypeDuty", "Тип пошлины", baseClass);

        customStore = addConcreteClass("CustomStore", "Склад временного хранения", named, (CustomClass) BL.Stock.getClassByName("Stock")); //, (CustomClass) BL.LegalEntity.getClassByName("legalEntity")

        customsZone = addConcreteClass("CustomsZone", "Таможенная зона", named);

        creationSku = addConcreteClass("CreationSku", "Операция создания товаров", transaction);
        creationFreightBox = addConcreteClass("CreationFreightBox", "Операция создания коробов", transaction);
        creationPallet = addConcreteClass("CreationPallet", "Операция создания паллет", transaction);
        creationStamp = addConcreteClass("CreationStamp", "Операция создания марок", transaction);

        transfer = addConcreteClass("Transfer", "Внутреннее перемещение", baseClass);

        unitOfMeasure = addConcreteClass("UnitOfMeasure", "Единица измерения", secondNameClass, named, (CustomClass) BL.Stock.getClassByName("UOM"));

        brandSupplier = addConcreteClass("BrandSupplier", "Бренд поставщика", (CustomClass) BL.RetailCRM.getClassByName("DiscountSkuGroup"), (CustomClass) BL.PriceRound.getClassByName("RoundGroup"), named);  //named,

        themeSupplier = addConcreteClass("ThemeSupplier", "Тема поставщика", named);

        countrySupplier = addConcreteClass("CountrySupplier", "Страна поставщика", named);

        season = addConcreteClass("Season", "Сезон", new String[]{"winter", "summer"}, new String[]{"Зима", "Лето"}, named);

        seasonYear = addConcreteClass("SeasonYear", "Сезон", baseClass);

        collectionSupplier = addConcreteClass("CollectionSupplier", "Коллекция", named);

        categorySupplier = addConcreteClass("CategorySupplier", "Группа", named);

        subCategorySupplier = addConcreteClass("SubCategorySupplier", "Подгруппа", named);

        route = addConcreteClass("Route", "Маршрут", new String[]{"rb", "rf"}, new String[]{"РБ", "РФ"}, named);

        typeTransit = addConcreteClass("TypeTransit", "Тип транзита", new String[]{"ex", "t1"}, new String[]{"EX", "T1"}, named);

        stamp = addConcreteClass("Stamp", "Контрольная марка", baseClass);

        transitDocument = addConcreteClass("TransitDocument", "Транзитный документ", baseClass);

        typeLabel = addConcreteClass("TypeLabel", "Тип этикетки", named);

        typeContainer = addConcreteClass("TypeContainer", "Вид грузового места", new String[]{"palleta", "box"}, new String[]{"Поддон", "Короб"}, named);
    }

    @Override
    public void initTables() {
        ConcreteCustomClass country = getCountryClass();

        addTable("named", named);
        addTable("transaction", transaction);
        addTable("customCategory4", customCategory4);
        addTable("customCategory6", customCategory6);
        addTable("customCategory9", customCategory9);
        addTable("customCategory10", customCategory10);
        addTable("customCategoryOrigin", customCategoryOrigin);
        addTable("customCategory10Origin", customCategory10, customCategoryOrigin);
        addTable("customCategory", customCategory);
        addTable("customCategory10STypeDuty", customCategory10, typeDuty);
        addTable("customCategory10SubCategory", customCategory10, subCategory);
        addTable("customCategory10SubCategoryCountry", customCategory10, subCategory, country);
        addTable("customCategoryOriginCustomsZone", customCategoryOrigin, customsZone);
        addTable("skuCustomsZone", sku, customsZone);

        addTable("colorSupplier", colorSupplier);
        addTable("sizeSupplier", sizeSupplier);
        addTable("country", country);
        addTable("article", article);
        addTable("sku", sku);
        addTable("documentArticle", document, article);
        addTable("documentSku", document, sku);
        addTable("shipmentSku", shipment, sku);
        addTable("listSku", list, sku);
        addTable("listArticle", list, article);

        addTable("importerFreightUnitSku", importer, freightUnit, sku);
        addTable("importerFreightSku", importer, freight, sku);

        addTable("freightUnitBrandSupplier", freightUnit, brandSupplier);
        addTable("freightUnitArticle", freightUnit, article);

        addTable("articleColorSupplier", article, colorSupplier);
        addTable("articleSizeSupplier", article, sizeSupplier);
        addTable("listArticleColorSupplier", list, article, colorSupplier);

        addTable("shipmentRoute", shipment, route);

        addTable("stockSku", stock, sku);
        addTable("stockArticle", stock, article);
        addTable("importerStockSku", importer, stock, sku);
        addTable("importerStockArticle", importer, stock, article);
        addTable("importerFreightUnitArticle", importer, freightUnit, article);
        addTable("importerFreightBrandSupplier", importer, freight, brandSupplier);
        addTable("importerFreightArticle", importer, freight, article);
        addTable("importerFreightCustomCategory6", importer, freight, customCategory6);
        addTable("freightBrandSupplier", freight, brandSupplier);
        addTable("freightArticle", freight, article);
        addTable("freightCategory", freight, category);
        addTable("shipmentFreight", shipment, freight);

        addTable("orderInvoiceSku", order, invoice, sku);
        addTable("invoiceShipment", invoice, shipment);
        addTable("orderShipmentSku", order, shipment, sku);
        addTable("shipmentStockSku", shipment, stock, sku);
        addTable("invoiceStockSku", invoice, stock, sku);
        addTable("importerFreightSupplier", importer, freight, supplier);
        addTable("importerFreightTypeInvoice", importer, freight, typeInvoice);
        addTable("importerFreightSupplierCustomCategory6", importer, freight, supplier, customCategory6);

        addTable("supplierBoxBoxShipmentStockSku", supplierBox, boxShipment, stock, sku);
        addTable("boxInvoiceBoxShipmentStockSku", boxInvoice, boxShipment, stock, sku);

        addTable("palletSku", pallet, sku);
        addTable("palletBrandSupplier", pallet, brandSupplier);
        addTable("stockBrandSupplier", stock, brandSupplier);
        addTable("documentBrandSupplier", document, brandSupplier);

        addTable("freightSku", freight, sku);
        addTable("shipmentDetail", shipmentDetail);
        addTable("pallet", pallet);
        addTable("freight", freight);
        addTable("freightUnit", freightUnit);
        addTable("barcodeObject", barcodeObject);

        addTable("categoryGenderCompositionTypeFabric", category, gender, COMPOSITION_CLASS, typeFabric);
        addTable("categoryGenderCompositionTypeFabricCustomsZone", category, gender, COMPOSITION_CLASS, typeFabric, customsZone);
        addTable("freightCategoryGenderCompositionTypeFabric", freight, category, gender, COMPOSITION_CLASS, typeFabric);
        addTable("importerFreightArticleCompositionCountryCustomCategory10", importer, freight, article, COMPOSITION_CLASS, country, customCategory10);
        addTable("sizeSupplierGenderCategory", sizeSupplier, gender, category);

        addTable("pricat", pricat);
        addTable("strings", StringClass.get(10));

        addTable("subCategory", subCategory);
        addTable("stamp", stamp);
        addTable("secondNameClass", secondNameClass);

        addTable("importerFreight", importer, freight);

        addTable("simpleInvoiceSimpleShipmentStockSku", simpleInvoice, simpleShipment, stock, sku);
    }

    protected AbstractGroup idGroup;
    protected AbstractGroup actionGroup;

    @Override
    public void initGroups() {
        initBaseGroupAliases();
        Settings.get().setDisableSumGroupNotZero(true);

        idGroup = addAbstractGroup("idGroup", "Идентификаторы", publicGroup, false);
        actionGroup = addAbstractGroup("actionGroup", "Действия", publicGroup, false);

        skuAttributeGroup = addAbstractGroup("skuAttributeGroup", "Атрибуты SKU", baseGroup);
        itemAttributeGroup = addAbstractGroup("itemAttributeGroup", "Атрибуты товара", baseGroup);
        supplierAttributeGroup = addAbstractGroup ("supplierAttributeGroup", "Атрибуты поставщика", publicGroup);
        intraAttributeGroup = addAbstractGroup("intraAttributeGroup", "Внутренние атрибуты", publicGroup);
        importInvoiceActionGroup = addAbstractGroup("importInvoiceActionGroup","Импорт инвойсов", actionGroup, false);
        importOrderActionGroup = addAbstractGroup("importOrderActionGroup","Импорт заказов", actionGroup, false);
    }

    public LCP name;

    protected LCP dumb(int interfaces) {
        ValueClass params[] = new ValueClass[interfaces];
        for (int i = 0; i < interfaces; ++i) {
            params[i] = baseLM.baseClass;
        }
        return addCProp("dumb" + interfaces, "dumbProperty" + interfaces, StringClass.get(1), "", params);
    }

    public LCP addCProp(StaticClass valueClass, Object value, ValueClass... params) {
        return addCProp(genSID(), "sys", valueClass, value, params);
    }
    public LCP addCProp(String name, String caption, StaticClass valueClass, Object value, ValueClass... params) {
        CalcPropertyRevImplement implement = DerivedProperty.createCProp(name, caption, valueClass, value, ListFact.toList(params).toIndexedMap());
        return addProperty(privateGroup, false, new LCP<PropertyInterface>(implement.property, ListFact.fromIndexedMap(implement.mapping.reverse())));
    }

    public LAP delete;

    @Override
    public void initProperties() {

        name = addDProp(recognizeGroup, "name", "Имя", StringClass.geti(110), named);

        dumb1 = dumb(1);

        ConcreteCustomClass country = getCountryClass();

        delete = addAProp(baseClass.unknown.getChangeClassAction());
        setDeleteActionOptions(delete);

        date = addDProp(baseGroup, "date", "Дата", DateClass.instance, transaction);
        date.setEventChange(BL.timeLM.currentDate, is(transaction), 1);

        barcode = addDProp(recognizeGroup, "barcode", "Штрихкод", StringClass.get(Settings.get().getBarcodeLength()), barcodeObject);
        barcode.setFixedCharWidth(13);
        barcodeToObject = addAGProp("barcodeToObject", "Объект", barcode);
//        barcodeObjectName = addJProp(baseGroup, "barcodeObjectName", "Объект", name, barcodeToObject, 1);
        equalsObjectBarcode = addJProp(baseLM.equals2, barcode, 1, 2);
        seekBarcodeAction = addJoinAProp("Поиск штрихкода", addSAProp(), barcodeToObject, 1);
        reverseBarcode = addSDProp("reverseBarcode", "Реверс", LogicalClass.instance);

        between = addJProp("between", "Между", baseLM.and1, baseLM.groeq2, 1, 2, baseLM.groeq2, 3, 1);
        betweenDates = addJProp("Между датами", between, object(DateClass.instance), 1, object(DateClass.instance), 2, object(DateClass.instance), 3);

        idGroup.add(baseLM.objectValue);
        delete.property.askConfirm = true;

        round2 = addSFProp("round2", "round(CAST((prm1) as numeric), 2)", NumericClass.get(14, 2), 1);

        sumNumeric2 = addSFProp("sumNumeric2", "((prm1)+(prm2))", NumericClass.get(14, 2), 2);

        divideNumeric2 = addSFProp("divideNumeric2", "round(CAST((CAST((prm1) as numeric)/(prm2)) as numeric),2)", NumericClass.get(14, 2), 2);
        divideNumeric3 = addSFProp("divideNumeric3", "round(CAST((CAST((prm1) as numeric)/(prm2)) as numeric),3)", NumericClass.get(14, 3), 2);

        percentNumeric2 = addSFProp("round(CAST(((prm1)*(prm2)/100) as numeric), 2)", NumericClass.get(14, 2), 2);

        typeSupplier = is(simpleSupplier);
        //typeSupplier = addCUProp("typeSupplier", addCProp(LogicalClass.instance, true, hugoBossSupplier), addCProp(LogicalClass.instance, true, gerryWeberSupplier), addCProp(LogicalClass.instance, true, sOliverSupplier), addCProp(LogicalClass.instance, true, babyPhatSupplier));
        noBarcodeSupplier = addCUProp("noBarcodeSupplier", addCProp(LogicalClass.instance, true, babyPhatSupplier));

        nameClassFreight = addJProp(baseGroup, "nameClassFreight", "Класс фрахта", baseLM.and1, baseLM.objectClassName, 1, is(freight), 1);
        logFreight = addLProp(BL.systemEventsLM, nameClassFreight);

        seasonSeasonYear = addDProp("seasonSeasonYear", "Сезон (ИД)", season, seasonYear);
        seasonSeasonYear.setAutoset(true);

        nameSeasonSeasonYear = addJProp("nameSeasonSeasonYear", "Сезон", name, seasonSeasonYear, 1);

        yearSeasonYear = addDProp("yearSeasonYear", "Год", StringClass.get(4), seasonYear);

        nameSeasonYear = addJProp("nameSeasonYear", "Наименование", baseLM.istring2SP, nameSeasonSeasonYear, 1, yearSeasonYear, 1);

        legalEntityCustomStore = addDProp(idGroup, "legalEntityCustomStore", "Юрлицо", BL.LegalEntity.getClassByName("LegalEntity"), customStore);

        // rate
        typeExchangeSTX = addDProp(idGroup, "typeExchangeSTX", "Тип обмена валют для STX (ИД)", getTypeExchangeClass());
        nameTypeExchangeSTX = addJProp(baseGroup, "nameTypeExchangeSTX", "Тип обмена валют для STX", BL.Currency.getLCPByName("nameTypeExchange"), typeExchangeSTX);
        typeExchangeCustom = addDProp(idGroup, "typeExchangeCustom", "Тип обмена валют для мин.цен (ИД)", getTypeExchangeClass());
        nameTypeExchangeCustom = addJProp(baseGroup, "nameTypeExchangeCustom", "Тип обмена валют для мин.цен", BL.Currency.getLCPByName("nameTypeExchange"), typeExchangeCustom);
        typeExchangePayCustom = addDProp(idGroup, "typeExchangePayCustom", "Тип обмена валют для платежей (ИД)", getTypeExchangeClass());
        nameTypeExchangePayCustom = addJProp(baseGroup, "nameTypeExchangePayCustom", "Тип обмена валют для платежей (БУ)", BL.Currency.getLCPByName("nameTypeExchange"), typeExchangePayCustom);
        typeExchangePayManagerial = addDProp(idGroup, "typeExchangePayManagerial", "Тип обмена валют для платежей (ИД)", getTypeExchangeClass());
        nameTypeExchangePayManagerial = addJProp(baseGroup, "nameTypeExchangePayManagerial", "Тип обмена валют для платежей (УУ)", BL.Currency.getLCPByName("nameTypeExchange"), typeExchangePayManagerial);

        typeExchangePayCustomCustomsZone = addDProp(idGroup, "typeExchangePayCustomCustomsZone", "Тип обмена для платежей (ИД)", getTypeExchangeClass(), customsZone);
        nameTypeExchangePayCustomCustomsZone = addJProp(baseGroup, "nameTypeExchangePayCustomCustomsZone", "Тип обмена для платежей (БУ)", BL.Currency.getLCPByName("nameTypeExchange"), typeExchangePayCustomCustomsZone, 1);

        typeExchangePayManagerialCustomsZone = addDProp(idGroup, "typeExchangePayManagerialCustomsZone", "Тип обмена для платежей (ИД)", getTypeExchangeClass(), customsZone);
        nameTypeExchangePayManagerialCustomsZone = addJProp(baseGroup, "nameTypeExchangePayManagerialCustomsZone", "Тип обмена для платежей (УУ)", BL.Currency.getLCPByName("nameTypeExchange"), typeExchangePayManagerialCustomsZone, 1);

        currencyPayFreights = addDProp(idGroup, "currencyPayFreights", "Валюта транспорта (ИД)", getCurrencyClass());
        nameCurrencyPayFreights = addJProp(baseGroup, "nameCurrencyPayFreights", "Валюта платежей за транспорт (РФ)", BL.Currency.getLCPByName("nameCurrency"), currencyPayFreights);

        currencyCustom = addDProp(idGroup, "currencyCustom", "Валюта мин.цен (ИД)", getCurrencyClass());
        nameCurrencyCustom = addJProp(baseGroup, "nameCurrencyCustom", "Валюта мин.цен", BL.Currency.getLCPByName("nameCurrency"), currencyCustom);
        currencyPayCustom = addDProp(idGroup, "currencyPayCustom", "Валюта для платежей (ИД)", getCurrencyClass());
        //nameCurrencyPayCustom = addJProp(baseGroup, "nameCurrencyPayCustom", "Валюта для платежей", name, currencyPayCustom);
//        typeExchangeRetail = addDProp(idGroup, "typeExchangeRetail", "Тип обмена для розницы", baseLM.typeExchange);
//        nameTypeExchangeRetail = addJProp(baseGroup, "nameTypeExchangeRetail", "Тип обмена для розницы", name, typeExchangeRetail);

        NDSPercentCustom = addDProp(baseGroup, "NDSPercentCustom", "НДС", NumericClass.get(14, 2));
        percentCostFreights = addDProp(baseGroup, "percentCostFreights", "Процент расходов за оформление", NumericClass.get(14, 2));
        tariffVolumeFreights = addDProp(baseGroup, "tariffVolumeFreights", "Тариф для перевозок (м3)", NumericClass.get(14, 2));

        // GENERAL
        nameOrigin = addDProp(baseGroup, "nameOrigin", "Наименование (ориг.)", StringClass.geti(50), secondNameClass);
//        nameOriginCountry = addDProp(baseGroup, "nameOriginCountry", "Наименование (ориг.)", StringClass.geti(50), baseLM.country);
//
//        sidOrigin2Country = addDProp(baseGroup, "sidOrigin2Country", "Код 2 знака (ориг.)", StringClass.get(2), baseLM.country);
//        sidOrigin3Country = addDProp(baseGroup, "sidOrigin3Country", "Код 3 знака (ориг.)", StringClass.get(3), baseLM.country);
        sid3Country = addDProp(baseGroup, "sid3Country", "Код 3 знака", StringClass.get(3), country);

        sidOrigin2ToCountry = addAGProp("sidOrigin2ToCountry", "Страна", getSidOrigin2Country());

        dictionaryComposition = addDProp(idGroup, "dictionaryComposition", "Словарь для составов (ИД)", BL.I18n.getClassByName("Dictionary"));
        nameDictionaryComposition = addJProp(baseGroup, "nameDictionaryComposition", "Словарь для составов", BL.I18n.getLCPByName("nameDictionary"), dictionaryComposition);

        dictionaryName = addDProp(idGroup, "dictionaryName", "Словарь для названий (ИД)", BL.I18n.getClassByName("Dictionary"));
        nameDictionaryName = addJProp(baseGroup, "nameDictionaryName", "Словарь для названий", BL.I18n.getLCPByName("nameDictionary"), dictionaryName);

        sidImporterFreightTypeInvoice = addDProp(baseGroup, "sidImporterFreightTypeInvoice", "Номер инвойса", StringClass.get(50), importer, freight, typeInvoice);
        sidImporterFreight = addMGProp(baseGroup, "sidImporterFreight", "Номер инвойса", sidImporterFreightTypeInvoice, 1, 2);

        sidDestination = addDProp(baseGroup, "sidDestination", "Код", StringClass.get(50), destination);

        destinationSID = addAGProp(idGroup, "destinationSID", "Магазин (ИД)", sidDestination);

        relationStoreSupplier = addDProp(baseGroup, "relationStoreSupplier", "Связь магазинов и поставщиков", LogicalClass.instance, store, supplier);

        sidBrandSupplier = addDProp(baseGroup, "sidBrandSupplier", "Код", StringClass.get(50), brandSupplier);  //???

        sidTransitDocument = addDProp(baseGroup, "sidTransitDocument", "Номер документа", StringClass.get(50), transitDocument);
        dateRepaymentTransitDocument = addDProp(baseGroup, "dateRepaymentTransitDocument", "Срок погашения", DateClass.instance, transitDocument);
        dateClosingTransitDocument = addDProp(baseGroup, "dateClosingTransitDocument", "Дата закрытия", DateClass.instance, transitDocument);

        sellerTransitDocument = addDProp(idGroup, "sellerTransitDocument", "Отправитель документа (ИД)", seller, transitDocument);
        nameSellerTransitDocument = addJProp(baseGroup, "nameSellerTransitDocument", "Отправитель документа", name, sellerTransitDocument, 1);
        nameSellerTransitDocument.property.preferredCharWidth = 15;

        importerTransitDocument = addDProp(baseGroup, "importerTransitDocument", "Получатель документа", importer, transitDocument);

        freightTransitDocument =  addDProp(baseGroup, "freightTransitDocument", "Фрахт документа", freight, transitDocument);
        typeTransitTransitDocument = addDProp(idGroup, "typeTransitTransitDocument", "Тип транзита (ИД)", typeTransit, transitDocument);
        nameTypeTransitTransitDocument = addJProp(baseGroup, "nameTypeTransitTransitDocument", "Тип транзита", name, typeTransitTransitDocument, 1);

        // Duty
        sidTypeDuty = addDProp(baseGroup, "sidTypeDuty", "Код", StringClass.get(10), typeDuty);
        sidToTypeDuty = addAGProp("sidToTypeDuty", "Тип пошлины", sidTypeDuty);

        nameTypeDuty = addDProp(baseGroup, "nameTypeDuty", "Наименование", StringClass.get(50), typeDuty);

        typeDutyDuty = addDProp(idGroup, "typeDutyDuty", "Для пошлин (ИД)", typeDuty);
        sidTypeDutyDuty = addJProp(baseGroup, "sidTypeDutyDuty", "Для пошлин (код)", sidTypeDuty, typeDutyDuty);
        nameTypeDutyDuty = addJProp(baseGroup, "nameTypeDutyDuty", "Для пошлин", nameTypeDuty, typeDutyDuty);

        typeDutyNDS = addDProp(idGroup, "typeDutyNDS", "Для НДС (ИД)", typeDuty);
        sidTypeDutyNDS = addJProp(baseGroup, "sidTypeDutyNDS", "Для НДС (код)", sidTypeDuty, typeDutyNDS);
        nameTypeDutyNDS = addJProp(baseGroup, "nameTypeDutyNDS", "Для НДС", nameTypeDuty, typeDutyNDS);

        typeDutyRegistration = addDProp(idGroup, "typeDutyRegistration", "Для оформления (ИД)", typeDuty);
        sidTypeDutyRegistration = addJProp(baseGroup, "sidTypeDutyRegistration", "Для оформления (код)", sidTypeDuty, typeDutyRegistration);
        nameTypeDutyRegistration = addJProp(baseGroup, "nameTypeDutyRegistration", "Для оформления", nameTypeDuty, typeDutyRegistration);

        typeDutyDutyCustomsZone = addDProp(idGroup, "typeDutyDutyCustomsZone", "Для пошлин (ИД)", typeDuty, customsZone);
        sidTypeDutyDutyCustomsZone = addJProp(baseGroup, "sidTypeDutyDutyCustomsZone", "Для пошлин (код)", sidTypeDuty, typeDutyDutyCustomsZone, 1);
        nameTypeDutyDutyCustomsZone = addJProp(baseGroup, "nameTypeDutyDutyCustomsZone", "Для пошлин", nameTypeDuty, typeDutyDutyCustomsZone, 1);

        typeDutyNDSCustomsZone = addDProp(idGroup, "typeDutyNDSCustomsZone", "Для НДС (ИД)", typeDuty, customsZone);
        sidTypeDutyNDSCustomsZone = addJProp(baseGroup, "sidTypeDutyNDSCustomsZone", "Для НДС (код)", sidTypeDuty, typeDutyNDSCustomsZone, 1);
        nameTypeDutyNDSCustomsZone = addJProp(baseGroup, "nameTypeDutyNDSCustomsZone", "Для НДС", nameTypeDuty, typeDutyNDSCustomsZone, 1);

        typeDutyRegistrationCustomsZone = addDProp(idGroup, "typeDutyRegistrationCustomsZone", "Для оформления (ИД)", typeDuty, customsZone);
        sidTypeDutyRegistrationCustomsZone = addJProp(baseGroup, "sidTypeDutyRegistrationCustomsZone", "Для оформления (код)", sidTypeDuty, typeDutyRegistrationCustomsZone, 1);
        nameTypeDutyRegistrationCustomsZone = addJProp(baseGroup, "nameTypeDutyRegistrationCustomsZone", "Для оформления", nameTypeDuty, typeDutyRegistrationCustomsZone, 1);

        // Contract
        sidContract = addDProp(baseGroup, "sidContract", "Номер договора", StringClass.get(50), contract);
        dateContract = addDProp(baseGroup, "dateContract", "Дата договора", DateClass.instance, contract);
        conditionShipmentContract = addDProp(baseGroup, "conditionShipmentContract", "Условие поставки", StringClass.get(200), contract);
        conditionPaymentContract = addDProp(baseGroup, "conditionPaymentContract", "Условие оплаты", StringClass.get(200), contract);

        //buyerContract = addDProp(idGroup, "buyerContract", "Покупатель (ИД)", buyer, contract);
        //nameBuyerContract = addJProp(baseGroup, "nameBuyerContract", "Покупатель", name, buyerContract, 1);

        currencyContract = addDProp(idGroup, "currencyContract", "Валюта (ИД)", getCurrencyClass(), contract);
        nameCurrencyContract = addJProp(baseGroup, "nameCurrencyContract", "Валюта", BL.Currency.getLCPByName("nameCurrency"), currencyContract, 1);

        // Subject
        addressOriginSubject = addDProp(baseGroup, "addressOriginSubject", "Address", StringClass.get(200), subject);
        addressSubject = addDProp(baseGroup, "addressSubject", "Адрес", StringClass.get(200), subject);

        contractImporter = addDProp(baseGroup, "contractImporter", "Номер договора", StringClass.get(50), importer);
        sidImporter = addDProp(baseGroup, "sidImporter", "Номер клиента", StringClass.get(50), importer);

        // CustomCategory
        customsZoneCustomCategory9 = addDProp(idGroup, "customsZoneCustomCategory9", "Зона категории (ИД)", customsZone, customCategory9);
        nameCustomsZoneCustomCategory9 = addJProp(baseGroup, "nameCustomsZoneCustomCategory9", "Таможенная зона 9", name, customsZoneCustomCategory9, 1);

        customsZoneCustomCategory10 = addDProp(idGroup, "customsZoneCustomCategory10", "Зона категории (ИД)", customsZone, customCategory10);
        customsZoneCustomCategory10.setAutoset(true);
        nameCustomsZoneCustomCategory10 = addJProp(baseGroup, "nameCustomsZoneCustomCategory10", "Таможенная зона", name, customsZoneCustomCategory10, 1);
        nameCustomsZoneCustomCategory10.property.preferredCharWidth = 30;
        nameCustomsZoneCustomCategory10.property.minimumCharWidth = 20;

        customsZoneCountry = addDProp(idGroup, "customsZoneCountry", "Зона Страны (ИД)", customsZone, country);
        customsZoneCountry.setAutoset(true);
        nameCustomsZoneCountry= addJProp(baseGroup, "nameCustomsZoneCountry", "Таможенная зона", name, customsZoneCountry, 1);

        sidCustomCategory4 = addDProp(baseGroup, "sidCustomCategory4", "Код(4)", StringClass.get(4), customCategory4);
        sidCustomCategory4.setFixedCharWidth(4);

        sidCustomCategory6 = addDProp(baseGroup, "sidCustomCategory6", "Код(6)", StringClass.get(6), customCategory6);
        sidCustomCategory6.setFixedCharWidth(6);

        sidCustomCategory9 = addDProp(baseGroup, "sidCustomCategory9", "Код(9)", StringClass.get(9), customCategory9);
        sidCustomCategory9.setFixedCharWidth(9);

        numberIdCustomCategory10 = addDProp(baseGroup, "numberIdCustomCategory10", "Номер", IntegerClass.instance, customCategory10);
        numberIdCustomCategoryOrigin = addDProp(baseGroup, "numberIdCustomCategoryOrigin", "Номер", IntegerClass.instance, customCategoryOrigin);
                
        sidCustomCategory10 = addDProp(baseGroup, "sidCustomCategory10", "Код(10)", StringClass.get(10), customCategory10);
        sidCustomCategory10.setFixedCharWidth(10);

        certificatedCustomCategory10 = addDProp(baseGroup, "certificatedCustomCategory10", "Необходимость сертификации", LogicalClass.instance, customCategory10);

        specUnitOfMeasureCustomCategory10 = addDProp(idGroup, "specUnitOfMeasureCustomCategory10", "Специальная ед. изм. (ИД)", unitOfMeasure, customCategory10);
        nameSpecUnitOfMeasureCustomCategory10 = addJProp(baseGroup, "nameSpecUnitOfMeasureCustomCategory10", "Специальная ед. изм.", name, specUnitOfMeasureCustomCategory10, 1);

        additionalUnitOfMeasureCustomCategory10 = addDProp(idGroup, "additionalUnitOfMeasureCustomCategory10", "Дополнительная ед. изм. (ИД)", unitOfMeasure, customCategory10);
        nameAdditionalUnitOfMeasureCustomCategory10 = addJProp(baseGroup, "nameAdditionalUnitOfMeasureCustomCategory10", "Дополнительная ед. изм.", name, additionalUnitOfMeasureCustomCategory10, 1);

        sidCustomCategoryOrigin = addDProp(baseGroup, "sidCustomCategoryOrigin", "Код ЕС(10)", StringClass.get(10), customCategoryOrigin);
        sidCustomCategoryOrigin.setFixedCharWidth(10);

        nameCustomCategory = addDProp(baseGroup, "nameCustomCategory", "Наименование", StringClass.get(500), customCategory);
        nameCustomCategory.property.preferredCharWidth = 50;
        nameCustomCategory.property.minimumCharWidth = 20;

        sidToCustomCategory4 = addAGProp("sidToCustomCategory4", "Код(4)", sidCustomCategory4);
        sidToCustomCategory6 = addAGProp("sidToCustomCategory6", "Код(6)", sidCustomCategory6);
        sidToCustomCategory9 = addAGProp("sidToCustomCategory9", "Код(9)", sidCustomCategory9, customsZoneCustomCategory9);
        sidToCustomCategory10 = addAGProp("sidToCustomCategory10", "Код(10)", sidCustomCategory10, customsZoneCustomCategory10);
        sidToCustomCategoryOrigin = addAGProp("sidToCustomCategoryOrigin", "Код ЕС (10)", sidCustomCategoryOrigin);

        importBelTnved = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать (РБ)", this, TNVEDImportActionProperty.CLASSIFIER_IMPORT, "belarusian"));
        importEuTnved = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать (ЕС)", this, TNVEDImportActionProperty.CLASSIFIER_IMPORT, "origin"));
        importUATnved = addAProp(new TNVEDImportUAActionProperty("importUATnved", "Импортировать ТНВЭД(Украина)", this));
        importTnvedCountryMinPrices = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать мин. цены", this, TNVEDImportActionProperty.MIN_PRICES_IMPORT));
        importTnvedDuty = addAProp(new TNVEDImportActionProperty(genSID(), "Импортировать платежи", this, TNVEDImportActionProperty.DUTIES_IMPORT));
        dieselImportInvoice = addAProp(importInvoiceActionGroup, new DieselImportInvoiceActionProperty(this));
        jennyferImportInvoice = addAProp(importInvoiceActionGroup, new JennyferImportInvoiceActionProperty(this));
        teddyImportInvoice = addAProp(importInvoiceActionGroup, new TeddyImportInvoiceActionProperty(this));
        steilmannImportInvoice = addAProp(importInvoiceActionGroup, new SteilmannImportInvoiceActionProperty(this));
        tallyWeijlImportInvoice = addAProp(importInvoiceActionGroup, new TallyWeijlImportInvoiceActionProperty(this));
        hugoBossImportInvoice = addAProp(importInvoiceActionGroup, new HugoBossImportInvoiceActionProperty(this));
        gerryWeberImportInvoice = addAProp(importInvoiceActionGroup, new GerryWeberImportInvoiceActionProperty(this));
        mexxImportInvoice = addAProp(new MexxImportInvoiceActionProperty(this));
        mexxImportPricesInvoice = addAProp(new MexxImportPricesInvoiceActionProperty(this));
        mexxImportArticleInfoInvoice = addAProp(new MexxImportArticleInfoInvoiceActionProperty(this));
        mexxImportColorInvoice = addAProp(new MexxImportColorInvoiceActionProperty(this));
        mexxImportDelivery = addAProp(importInvoiceActionGroup, new MexxImportDeliveryActionProperty(this));
        bestsellerImportInvoice = addAProp(importInvoiceActionGroup, new BestsellerImportInvoiceActionProperty(this));
        sOliverImportInvoice = addAProp(importInvoiceActionGroup, new SOliverImportInvoiceActionProperty(this));
        womenSecretImportInvoice = addAProp(importInvoiceActionGroup, new WomenSecretImportInvoiceActionProperty(this));
        topazImportInvoice = addAProp(importInvoiceActionGroup, new TopazImportInvoiceActionProperty(this));
        aprioriImportInvoice = addAProp(importInvoiceActionGroup, new AprioriImportInvoiceActionProperty(this));

        mexxImportOrder = addAProp(importOrderActionGroup, new MexxImportOrderActionProperty(this));
        dieselImportOrder = addAProp(importOrderActionGroup, new DieselImportOrderActionProperty(this));

        customCategory4CustomCategory6 = addDProp(idGroup, "customCategory4CustomCategory6", "Код(4)", customCategory4, customCategory6);
        customCategory4CustomCategory6.setAutoset(true);

        customCategory6CustomCategory9 = addDProp(idGroup, "customCategory6CustomCategory9", "Код(6)", customCategory6, customCategory9);
        customCategory6CustomCategory9.setAutoset(true);

        customCategory9CustomCategory10 = addDProp(idGroup, "customCategory9CustomCategory10", "Код(9)", customCategory9, customCategory10);
        customCategory9CustomCategory10.setAutoset(true);

        customCategory6CustomCategory10 = addJProp(idGroup, "customCategory6CustomCategory10", "Код(6)", customCategory6CustomCategory9, customCategory9CustomCategory10, 1);
        customCategory4CustomCategory10 = addJProp(idGroup, "customCategory4CustomCategory10", "Код(4)", customCategory4CustomCategory6, customCategory6CustomCategory10, 1);

        nameSubCategory = addDProp(baseGroup, "nameSubCategory", "Наименование", StringClass.get(200), subCategory);
        nameSubCategory.property.preferredCharWidth = 30;
        nameSubCategory.property.minimumCharWidth = 10;
        nameToSubCategory = addAGProp("nameToSubCategory", "Наименование", nameSubCategory);

        relationCustomCategory10SubCategory = addDProp(baseGroup, "relationCustomCategory10SubCategory", "Связь ТН ВЭД", LogicalClass.instance, customCategory10, subCategory);

        customsZoneSubCategory = addDProp(idGroup, "customsZoneSubCategory", "Зона ИД", customsZone, subCategory);
        customsZoneSubCategory.setAutoset(true);
        nameCustomsZoneSubCategory = addJProp(baseGroup, "nameCustomsZoneSubCategory", "Таможенная зона", name, customsZoneSubCategory, 1);

        subCategoryCustomCategory10 = addMGProp(baseGroup, "subCategoryCustomCategory10", "По умолчанию", addJProp(baseLM.and1, 2, relationCustomCategory10SubCategory, 1, 2), 1);

        countRelationCustomCategory10 = addSGProp("countRelationCustomCategory10", true, "Кол-во групп", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), relationCustomCategory10SubCategory, 1, 2), 1);

        minPriceCustomCategory10SubCategory = addDProp(baseGroup, "minPriceCustomCategory10SubCategory", "Минимальная цена ($)", NumericClass.get(14, 2), customCategory10, subCategory);
        minPriceCustomCategory10SubCategoryCountry = addDProp("minPriceCustomCategory10SubCategoryCountry", "Минимальная цена для страны ($)", NumericClass.get(14, 2), customCategory10, subCategory, country);
        dutyPercentCustomCategory10TypeDuty = addDProp("dutyPercentCustomCategory10TypeDuty", "в %", NumericClass.get(14, 2), customCategory10, typeDuty);
        dutySumCustomCategory10TypeDuty = addDProp("dutySumCustomCategory10TypeDuty", "в евро", NumericClass.get(14, 2), customCategory10, typeDuty);

        customsZoneTypeDuty = addDProp(idGroup, "customsZoneTypeDuty", "Зона ИД", customsZone, typeDuty);
        customsZoneTypeDuty.setAutoset(true);
        nameCustomsZoneTypeDuty = addJProp(baseGroup, "nameCustomsZoneTypeDuty", "Таможенная зона", name, customsZoneTypeDuty, 1);

        customCategory6CustomCategoryOrigin = addDProp(idGroup, "customCategory6CustomCategoryOrigin", "Код(6)", customCategory6, customCategoryOrigin);
        customCategory4CustomCategoryOrigin = addJProp(idGroup, "customCategory4CustomCategoryOrigin", "Код(4)", customCategory4CustomCategory6, customCategory6CustomCategoryOrigin, 1);

        typeFabricCustomCategory6 = addDProp(idGroup, "typeFabricCustomCategory6", "Тип одежды (ИД)", typeFabric, customCategory6);
        nameTypeFabricCustomCategory6 = addJProp(baseGroup, "nameTypeFabricCustomCategory6", "Тип одежды", name, typeFabricCustomCategory6, 1);
        typeFabricCustomCategoryOrigin = addJProp(idGroup, "typeFabricCustomCategoryOrigin", "Тип одежды (ИД)", typeFabricCustomCategory6, customCategory6CustomCategoryOrigin, 1);

        customCategory10CustomCategoryOrigin = addDProp(idGroup, "customCategory10CustomCategoryOrigin", "Код по умолчанию(ИД)", customCategory10, customCategoryOrigin);
        sidCustomCategory10CustomCategoryOrigin = addJProp(baseGroup, "sidCustomCategory10CustomCategoryOrigin", "Код по умолчанию", sidCustomCategory10, customCategory10CustomCategoryOrigin, 1);
        sidCustomCategory10CustomCategoryOrigin.property.preferredCharWidth = 10;
        sidCustomCategory10CustomCategoryOrigin.property.minimumCharWidth = 10;

        customCategory10CustomCategoryOriginCustomsZone = addDProp(idGroup, "customCategory10CustomCategoryOriginCustomsZone", "Четвертый уровень(ИД)", customCategory10, customCategoryOrigin, customsZone);
        sidCustomCategory10CustomCategoryOriginCustomsZone = addJProp(baseGroup, "sidCustomCategory10CustomCategoryOriginCustomsZone", "Четвертый уровень (код)", sidCustomCategory10, customCategory10CustomCategoryOriginCustomsZone, 1, 2);
        sidCustomCategory10CustomCategoryOriginCustomsZone.property.preferredCharWidth = 30;
        sidCustomCategory10CustomCategoryOriginCustomsZone.property.minimumCharWidth = 30;
        nameCustomCategory10CustomCategoryOriginCustomsZone = addJProp(baseGroup, "nameCustomCategory10CustomCategoryOriginCustomsZone", "Четвертый уровень (наименование)", nameCustomCategory, customCategory10CustomCategoryOriginCustomsZone, 1, 2);
        nameCustomCategory10CustomCategoryOriginCustomsZone.property.preferredCharWidth = 50;
        nameCustomCategory10CustomCategoryOriginCustomsZone.property.minimumCharWidth = 30;

        sidCustomCategory4CustomCategory6 = addJProp(baseGroup, "sidCustomCategory4CustomCategory6", "Код(4)", sidCustomCategory4, customCategory4CustomCategory6, 1);
        sidCustomCategory6CustomCategory9 = addJProp(baseGroup, "sidCustomCategory6CustomCategory9", "Код(6)", sidCustomCategory6, customCategory6CustomCategory9, 1);
        sidCustomCategory9CustomCategory10 = addJProp(idGroup, "sidCustomCategory9CustomCategory10", "Код(9)", sidCustomCategory9, customCategory9CustomCategory10, 1);
        sidCustomCategory6CustomCategoryOrigin = addJProp(idGroup, "sidCustomCategory6CustomCategoryOrigin", "Код(6)", sidCustomCategory6, customCategory6CustomCategoryOrigin, 1);
        sidCustomCategory4CustomCategoryOrigin = addJProp(baseGroup, "sidCustomCategory4CustomCategoryOrigin", "Код(4)", sidCustomCategory4, customCategory4CustomCategoryOrigin, 1);

        sidCustomCategory6CustomCategory10 = addJProp("sidCustomCategory6CustomCategory10", "Код(6)", sidCustomCategory6, customCategory6CustomCategory10, 1);
        sidCustomCategory4CustomCategory10 = addJProp(baseGroup, "sidCustomCategory4CustomCategory10", "Код(4)", sidCustomCategory4, customCategory4CustomCategory10, 1);

        nameCustomCategory4CustomCategory6 = addJProp(baseGroup, "nameCustomCategory4CustomCategory6", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory6, 1);
        nameCustomCategory6CustomCategory9 = addJProp(baseGroup, "nameCustomCategory6CustomCategory9", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory9, 1);
        nameCustomCategory9CustomCategory10 = addJProp("nameCustomCategory9CustomCategory10", "Наименование(9)", nameCustomCategory, customCategory9CustomCategory10, 1);
        nameCustomCategory6CustomCategory10 = addJProp("nameCustomCategory6CustomCategory10", "Наименование(6)", nameCustomCategory, customCategory6CustomCategory10, 1);
        nameCustomCategory4CustomCategory10 = addJProp(baseGroup, "nameCustomCategory4CustomCategory10", "Наименование(4)", nameCustomCategory, customCategory4CustomCategory10, 1);

        nameCustomCategory6CustomCategoryOrigin = addJProp(baseGroup, "nameCustomCategory6CustomCategoryOrigin", "Наименование(6)", nameCustomCategory, customCategory6CustomCategoryOrigin, 1);
        nameCustomCategory4CustomCategoryOrigin = addJProp(baseGroup, "nameCustomCategory4CustomCategoryOrigin", "Наименование(4)", nameCustomCategory, customCategory4CustomCategoryOrigin, 1);

//        relationCustomCategory10CustomCategoryOrigin = addDProp(baseGroup, "relationCustomCategory10CustomCategoryOrigin", "Связь ТН ВЭД", LogicalClass.instance, customCategory10, customCategoryOrigin);

//         addConstraint(addJProp("По умолчанию должен быть среди связанных", and(true, false),
//                addCProp(LogicalClass.instance, true, customCategoryOrigin), 1,
//                addJProp(relationCustomCategory10CustomCategoryOrigin, customCategory10CustomCategoryOrigin, 1, 1), 1,
//                addJProp(is(customCategory10), customCategory10CustomCategoryOrigin, 1), 1), true);

        // Supplier
        currencySupplier = addDProp(idGroup, "currencySupplier", "Валюта (ИД)", getCurrencyClass(), supplier);
        nameCurrencySupplier = addJProp(baseGroup, "nameCurrencySupplier", "Валюта", BL.Currency.getLCPByName("nameCurrency"), currencySupplier, 1);

        sidColorSupplier = addDProp(baseGroup, "sidColorSupplier", "Код", StringClass.get(50), colorSupplier);
        sidColorSupplier.setMinimumCharWidth(5);

        supplierColorSupplier = addDProp(idGroup, "supplierColorSupplier", "Поставщик (ИД)", supplier, colorSupplier);
        nameSupplierColorSupplier = addJProp(baseGroup, "nameSupplierColorSupplier", "Поставщик", name, supplierColorSupplier, 1);

        colorSIDSupplier = addAGProp(idGroup, "colorSIDSupplier", "Цвет поставщика (ИД)", sidColorSupplier, supplierColorSupplier);

        sidSizeSupplier = addDProp(baseGroup, "sidSizeSupplier", "Код", StringClass.get(50), sizeSupplier);
        sidThemeSupplier = addDProp(baseGroup, "sidThemeSupplier", "Код", StringClass.get(50), themeSupplier);
        sidCollectionSupplier = addDProp(baseGroup, "sidCollectionSupplier", "Код", StringClass.get(50), collectionSupplier);
        sidSubCategorySupplier = addDProp(baseGroup, "sidSubCategorySupplier", "Код", StringClass.get(50), subCategorySupplier);
        sidGender = addDProp(baseGroup, "sidGender", "Код", StringClass.get(50), gender);
        sidGender.setPreferredCharWidth(2);
        sidGenderSupplier = addDProp(baseGroup, "sidGenderSupplier", "Код", StringClass.get(10), genderSupplier);
        sidSeasonSupplier = addDProp(baseGroup, "sidSeasonSupplier", "Код", StringClass.get(10), seasonSupplier);

        commonSizeSizeSupplierGenderCategory = addDProp(idGroup, "commonSizeSizeSupplierGenderCategory", "Унифицированный размер (ИД)", commonSize, sizeSupplier, gender, category);
        nameCommonSizeSizeSupplierGenderCategory = addJProp(baseGroup, "nameCommonSizeSizeSupplierGenderCategory", "Унифицированный размер", name, commonSizeSizeSupplierGenderCategory, 1, 2, 3);

        commonSizeSizeSupplierGenderCategoryTypeFabric = addDProp(idGroup, "commonSizeSizeSupplierGenderCategoryTypeFabric", "Унифицированный размер (ИД)", commonSize, sizeSupplier, gender, category, typeFabric);
        nameCommonSizeSizeSupplierGenderCategoryTypeFabric = addJProp(baseGroup, "nameCommonSizeSizeSupplierGenderCategoryTypeFabric", "Унифицированный размер", name, commonSizeSizeSupplierGenderCategoryTypeFabric, 1, 2, 3, 4);

        commonSizeSizeSupplier = addDProp(idGroup, "commonSizeSizeSupplier", "Унифицированный размер (ИД)", commonSize, sizeSupplier);
        nameCommonSizeSizeSupplier = addJProp(baseGroup, "nameCommonSizeSizeSupplier", "Унифицированный размер", name, commonSizeSizeSupplier, 1);

        supplierSizeSupplier = addDProp(idGroup, "supplierSizeSupplier", "Поставщик (ИД)", supplier, sizeSupplier);
        nameSupplierSizeSupplier = addJProp(baseGroup, "nameSupplierSizeSupplier", "Поставщик", name, supplierSizeSupplier, 1);

        supplierGenderSupplier = addDProp(idGroup, "supplierGenderSupplier", "Поставщик (ИД)", supplier, genderSupplier);
        nameSupplierGenderSupplier = addJProp(baseGroup, "nameSupplierGenderSupplier", "Поставщик", name, supplierGenderSupplier, 1);

        supplierSizeGroup = addDProp(idGroup, "supplierSizeGroup", "Поставщик (ИД)", supplier, sizeGroupSupplier);
        nameSupplierSizeGroup = addJProp(baseGroup, "nameSupplierSizeGroup", "Поставщик", name, supplierSizeGroup, 1);
        groupSizeSupplier = addDProp(idGroup, "groupSizeSupplier", "Размерная сетка (ИД)", sizeGroupSupplier, sizeSupplier);
        nameGroupSizeSupplier = addJProp(baseGroup, "nameGroupSizeSupplier", "Размерная сетка", name, groupSizeSupplier, 1);
        orderSizeSupplier = addDProp(baseGroup, "orderSizeSupplier", "Порядок", IntegerClass.instance, sizeSupplier);

        equalsGroupSizeSupplier = addJProp("equalsGroupSizeSupplier", "Вкл", baseLM.equals2, groupSizeSupplier, 1, 2);

        LCP supplierGroupSizeSupplier = addJProp(supplierSizeGroup, groupSizeSupplier, 1);
        addConstraint(addJProp("Поставщик размерной сетки должен соответствовать поставщику размера", baseLM.diff2,
                supplierSizeSupplier, 1,
                supplierGroupSizeSupplier, 1), true);

        supplierSeasonSupplier = addDProp(idGroup, "supplierSeasonSupplier", "Поставщик (ИД)", supplier, seasonSupplier);

        supplierThemeSupplier = addDProp(idGroup, "supplierThemeSupplier", "Поставщик (ИД)", supplier, themeSupplier);
        nameSupplierThemeSupplier = addJProp(baseGroup, "nameSupplierThemeSupplier", "Поставщик", name, supplierThemeSupplier, 1);

        supplierCollectionSupplier = addDProp(idGroup, "supplierCollectionSupplier", "Поставщик (ИД)", supplier, collectionSupplier);
        nameSupplierCollectionSupplier = addJProp(baseGroup, "nameSupplierCollectionSupplier", "Поставщик", name, supplierCollectionSupplier, 1);

        sidDestinationSupplier = addDProp(idGroup, "sidDestinationSupplier", "Идентификатор магазина у поставщика", StringClass.geti(50),  destination, supplier);

        supplierCategorySupplier = addDProp(idGroup, "supplierCategorySupplier", "Поставщик (ИД)", supplier, categorySupplier);
        categorySupplierSubCategorySupplier = addDProp(idGroup, "categorySupplierSubCategorySupplier", "Группа (ИД)", categorySupplier, subCategorySupplier);
        nameCategorySupplierSubCategorySupplier = addJProp(baseGroup, "nameCategorySupplierSubCategorySupplier", "Группа", name, categorySupplierSubCategorySupplier, 1);

        //supplierSubCategorySupplier = addJProp("supplierSubCategorySupplier", "Поставщик (ИД)", supplierCategorySupplier, categorySupplierSubCategorySupplier, 1);
        supplierSubCategorySupplier = addDProp(idGroup, "supplierSubCategorySupplier", "Поставщик (ИД)", supplier, subCategorySupplier);
        nameSupplierSubCategorySupplier = addJProp(baseGroup, "nameSupplierSubCategorySupplier", "Поставщик", name, supplierSubCategorySupplier, 1);

        sizeSIDSupplier = addAGProp(idGroup, "sizeSIDSupplier", "Размер поставщика (ИД)", sidSizeSupplier, supplierSizeSupplier);
        themeSIDSupplier = addAGProp(idGroup, "themeSIDSupplier", "Тема поставщика (ИД)", sidThemeSupplier, supplierThemeSupplier);
        collectionSIDSupplier = addAGProp(idGroup, "collectionSIDSupplier", "Коллекция поставщика (ИД)", sidCollectionSupplier, supplierCollectionSupplier);
        subCategorySIDSupplier = addAGProp(idGroup, "subCategorySIDSupplier", "Категория поставщика (ИД)", sidSubCategorySupplier, supplierSubCategorySupplier);

        genderSIDSupplier = addAGProp(idGroup, "genderSIDSupplier", "Пол поставщика (ИД)", sidGenderSupplier, supplierGenderSupplier);
        destinationSIDSupplier = addAGProp(idGroup, "destinationSIDSupplier", "Магазин поставщика (ИД)",
                addJProp(baseLM.and1, is(destination), 1, is(supplier), 2), 1, sidDestinationSupplier, 1, 2);

        // Country
        supplierCountrySupplier = addDProp(idGroup, "supplierCountrySupplier", "Поставщик (ИД)", supplier, countrySupplier);
        nameSupplierCountrySupplier = addJProp(baseGroup, "nameSupplierCountrySupplier", "Поставщик", name, supplierCountrySupplier, 1);

        countryCountrySupplier = addDProp(idGroup, "countryCountrySupplier", "Страна (ИД)", country, countrySupplier);
        nameCountryCountrySupplier = addJProp(baseGroup, "nameCountryCountrySupplier", "Страна", BL.Country.getLCPByName("nameCountry"), countryCountrySupplier, 1);

        countryNameSupplier = addAGProp(idGroup, "countryNameSupplier", "Страна поставщика", name, supplierCountrySupplier);

        // Brand
        supplierBrandSupplier = addDProp(idGroup, "supplierBrandSupplier", "Поставщик (ИД)", supplier, brandSupplier);
        nameSupplierBrandSupplier = addJProp(baseGroup, "nameSupplierBrandSupplier", "Поставщик", name, supplierBrandSupplier, 1);

        brandSIDSupplier = addAGProp(idGroup, "brandSIDSupplier", "Бренд поставщика (ИД)", sidBrandSupplier, supplierBrandSupplier);

        brandSupplierSupplier = addDProp(idGroup, "brandSupplierSupplier", "Бренд (ИД)", brandSupplier, supplier);
        nameBrandSupplierSupplier = addJProp(baseGroup, "nameBrandSupplierSupplier", "Бренд по умолчанию", name, brandSupplierSupplier, 1);

        addConstraint(addJProp("Бренд по умолчанию для поставщика должен соответствовать брендам поставщика", baseLM.diff2, 1, addJProp(supplierBrandSupplier, brandSupplierSupplier, 1), 1), true);

        countryBrandSupplier = addDProp(idGroup, "countryBrandSupplier", "Страна бренда (ИД)", country, brandSupplier);
        nameCountryBrandSupplier = addJProp(baseGroup, "nameCountryBrandSupplier", "Страна бренда", BL.Country.getLCPByName("nameCountry"), countryBrandSupplier, 1);

        customsSIDBrandSupplier = addDProp(baseGroup, "customsSIDBrandSupplier", "Таможенный код", StringClass.get(50), brandSupplier);
        customsSIDSupplier = addJProp(baseGroup, "customsSIDSupplier", "Поставщик (ИД)", customsSIDBrandSupplier, brandSupplierSupplier, 1);
        supplierCustomsSID  = addAGProp(idGroup, "supplierCustomsSID", "Поставщик (ИД)", customsSIDSupplier);

        // Document
        supplierDocument = addDProp(idGroup, "supplierDocument", "Поставщик (ИД)", supplier, document);
        supplierPriceDocument = addJProp(idGroup, "supplierPricedDocument", "Поставщик(ИД)", baseLM.and1, supplierDocument, 1, is(priceDocument), 1);
        nameSupplierDocument = addJProp(baseGroup, "nameSupplierDocument", "Поставщик", name, supplierDocument, 1);

        addConstraint(addJProp("Для инвойса по коробам поставщик должен быть с коробами", baseLM.and1, is(boxInvoice), 1, addJProp(typeSupplier, supplierDocument, 1), 1), true);
        addConstraint(addJProp("Для инвойса без коробов поставщик должен быть без коробов", baseLM.andNot1, is(simpleInvoice), 1, addJProp(typeSupplier, supplierDocument, 1), 1), true);

        currencyDocument = addDCProp(idGroup, "currencyDocument", "Валюта (ИД)", currencySupplier, supplierPriceDocument, 1);
        nameCurrencyDocument = addJProp(baseGroup, "nameCurrencyDocument", "Валюта", BL.Currency.getLCPByName("nameCurrency"), currencyDocument, 1);
        nameCurrencyDocument.property.preferredCharWidth = 50;
        nameCurrencyDocument.property.minimumCharWidth = 10;

        currencyRRPDocument = addDProp(idGroup, "currencyRRPDocument", "Валюта RRP (ИД)", getCurrencyClass(), priceDocument);
        nameCurrencyRRPDocument = addJProp("nameCurrencyRRPDocument", "Валюта RRP", BL.Currency.getLCPByName("nameCurrency"), currencyRRPDocument, 1);
        nameCurrencyRRPDocument.property.preferredCharWidth = 50;
        nameCurrencyRRPDocument.property.minimumCharWidth = 10;

        addConstraint(addJProp("Для инвойса должна быть задана валюта", baseLM.andNot1, is(invoice), 1, currencyDocument, 1), false);

        destinationDestinationDocument = addDProp(idGroup, "destinationDestinationDocument", "Пункт назначения (ИД)", destination, destinationDocument);
        nameDestinationDestinationDocument = addJProp(baseGroup, "nameDestinationDestinationDocument", "Пункт назначения (наим.)", BL.Store.getLCPByName("nameStore"), destinationDestinationDocument, 1);
        nameDestinationDestinationDocument.property.preferredCharWidth = 50;
        nameDestinationDestinationDocument.property.minimumCharWidth = 30;
        sidDestinationDestinationDocument = addJProp(baseGroup, "sidDestinationDestinationDocument", "Пункт назначения", sidDestination, destinationDestinationDocument, 1);
        setNotNull(destinationDestinationDocument);

        addConstraint(addJProp("Магазин для документа должен быть связан с поставщиком документа", and(false, false, true), addCProp(LogicalClass.instance, true, destinationDocument), 1,
                   destinationDestinationDocument, 1,
                   supplierDocument, 1,
                   addJProp(relationStoreSupplier, destinationDestinationDocument, 1, supplierDocument, 1), 1), true);

        // Invoice
        exporterInvoice = addDProp(idGroup, "exporterInvoice", "Экспортер (ИД)", exporter, invoice);

        exporterProxyInvoice = addJProp(idGroup, "exporterProxyInvoice", "Экспортер (ИД)", baseLM.andNot1, exporterInvoice, 1, is(directInvoice), 1);

        addConstraint(addJProp("Для инвойса должен быть задан экспортёр", and(true, true), is(invoice), 1, exporterInvoice, 1, is(directInvoice), 1), false);

        importerDirectInvoice = addDProp(idGroup, "importerDirectInvoice", "Импортер (ИД)", importer, directInvoice);
        nameImporterDirectInvoice = addJProp(baseGroup, "nameImporterDirectInvoice", "Импортер", name, importerDirectInvoice, 1);
        nameImporterDirectInvoice.property.preferredCharWidth = 50;
        nameImporterDirectInvoice.property.minimumCharWidth = 30;

        addConstraint(addJProp("Для инвойса должен быть задан импортёр", baseLM.andNot1, is(directInvoice), 1, importerDirectInvoice, 1), false);

        companyInvoice = addSUProp(Union.OVERRIDE, exporterProxyInvoice, importerDirectInvoice);
        nameCompanyInvoice = addJProp("nameCompanyInvoice", "Компания", name, companyInvoice, 1);
        nameCompanyInvoice.property.preferredCharWidth = 50;

        languageInvoice = addJProp("languageInvoice", "Язык инвойса (ИД)", BL.Store.getLCPByName("languageStore"), destinationDestinationDocument, 1);
        nameLanguageInvoice = addJProp("nameLanguageInvoice", "Язык инвойса", BL.I18n.getLCPByName("nameLanguage"), languageInvoice, 1);

        //contractInvoice = addDProp(idGroup, "contractInvoice", "Договор (ИД)", contract, invoice);
        //sidContractInvoice = addJProp(baseGroup, "sidContractInvoice", "Договор", sidContract, contractInvoice, 1);

        //addConstraint(addJProp("Экспортер договора должен соответствовать экспортеру инвойса", baseLM.diff2,
        //        exporterProxyInvoice, 1, addJProp(subjectContract, contractInvoice, 1), 1), true);

        //addConstraint(addJProp("Импортер договора должен соответствовать импортеру инвойса", baseLM.diff2,
        //        importerDirectInvoice, 1, addJProp(subjectContract, contractInvoice, 1), 1), true);

        //addConstraint(addJProp("Поставщик договора должен соответствовать поставщику инвойса", baseLM.diff2,
        //        supplierDocument, 1, addJProp(sellerContract, contractInvoice, 1), 1), true);

        // Shipment
        dateDepartureShipment = addDProp(baseGroup, "dateDepartureShipment", "Дата отгрузки", DateClass.instance, shipment);
        dateArrivalShipment = addDProp(baseGroup, "dateArrivalShipment", "Дата прихода на STX", DateClass.instance, shipment);
        quantityPalletShipment = addDProp(baseGroup, "quantityPalletShipment", "Кол-во паллет", IntegerClass.instance, shipment);
        netWeightShipment = addDProp(baseGroup, "netWeightShipment", "Вес нетто", NumericClass.get(14, 3), shipment);
        grossWeightShipment = addDProp(baseGroup, "grossWeightShipment", "Вес брутто", NumericClass.get(14, 3), shipment);

        grossWeightPallet = addDProp(baseGroup, "grossWeightPallet", "Вес брутто", NumericClass.get(14, 3), pallet);
        quantityBoxShipment = addDProp(baseGroup, "quantityBoxShipment", "Кол-во коробов", IntegerClass.instance, shipment);

        // Item
        articleCompositeItem = addDProp(idGroup, "articleCompositeItem", "Артикул (ИД)", articleComposite, item);
        equalsItemArticleComposite = addJProp(baseGroup, "equalsItemArticleComposite", "Вкл.", baseLM.equals2, articleCompositeItem, 1, 2);

        articleSku = addCUProp(idGroup, "articleSku", true, "Артикул (ИД)", object(articleSingle), articleCompositeItem);
//        setNotNull(articleSku);
//        addConstraint(addJProp("Для товара должен быть задан артикул", baseLM.andNot1, is(sku), 1, articleSku, 1), false);

        addItemBarcode = addAAProp("Ввод товара по штрих-коду", item, barcode);

        // Article
        sidArticle = addDProp(baseGroup, "sidArticle", "Артикул", StringClass.get(50), article);
        sidArticle.setMinimumCharWidth(15);
        sidArticleSku = addJProp(supplierAttributeGroup, "sidArticleSku", "Артикул", sidArticle, articleSku, 1);

        originalNameArticle = addDProp(supplierAttributeGroup, "originalNameArticle", "Наименование (ориг.)", StringClass.geti(50), article);
        originalNameArticleSku = addJProp(supplierAttributeGroup, "originalNameArticleSku", "Наименование (ориг.)", originalNameArticle, articleSku, 1);

        translateNameArticle = addDProp(supplierAttributeGroup, "translateNameArticle", "Наименование", StringClass.geti(50), article);
        translateNameArticleSku = addJProp(supplierAttributeGroup, true, "translateNameArticleSku", "Наименование", translateNameArticle, articleSku, 1);

        originalNameArticleSkuLanguage = addJProp(baseLM.and1, originalNameArticleSku, 1, is((CustomClass) BL.I18n.getClassByName("Language")), 2);
        translateNameSkuLanguage = addDProp(supplierAttributeGroup, "translateNameSkuLanguage", "Наименование", StringClass.geti(50), sku, (CustomClass) BL.I18n.getClassByName("Language"));

        translationNameSku = addJoinAProp(actionGroup, "translationNameSku", "Перевести", addTAProp(originalNameArticleSku, translateNameArticleSku), dictionaryName, 1);
        translationNameSku.addToContextMenuFor(translateNameArticleSku);

        translationNameSkuLanguage = addJoinAProp(actionGroup, "translationNameSkuLanguage", "Перевод наименования", addTAProp(originalNameArticleSkuLanguage, translateNameSkuLanguage), BL.RomanI18n.getLCPByName("dictionaryNameLanguage"), 2, 1, 2);
        //translationNameSkuInvoice = addJoinAProp(actionGroup, "translationNameSkuInvoice", "Перевод наименования", addTAProp(originalNameArticleSkuLanguage, translateNameSkuLanguage), dictionaryName, 1, languageInvoice, 2);

        translateNameSkuInvoice = addJProp("translateNameSkuInvoice", "Наименование (иностр.)", translateNameSkuLanguage, 1, languageInvoice, 2);

        translationNameSkuInvoice = addJoinAProp("translationNameSkuInvoice", "Перевести", translationNameSkuLanguage, 1, languageInvoice, 2);
        translationNameSkuInvoice.addToContextMenuFor(translateNameSkuInvoice);

        translateNameColorSupplier = addDProp(supplierAttributeGroup, "translateNameColorSupplier", "Наименование", StringClass.geti(50), colorSupplier);

        coefficientArticle = addDProp(intraAttributeGroup, "coefficientArticle", "Кол-во в комплекте", IntegerClass.instance, article);
        coefficientArticleSku = addJProp(intraAttributeGroup, true, "coefficientArticleSku", "Кол-во в комплекте", coefficientArticle, articleSku, 1);

        sidTypeLabel = addDProp(baseGroup, "sidTypeLabel", "Код", StringClass.geti(50), typeLabel);

        typeLabelArticle = addDProp(idGroup, "typeLabelArticle", "Тип этикетки (ИД)", typeLabel, article);
        nameTypeLabelArticle = addJProp("nameTypeLabelArticle", "Тип этикетки", name, typeLabelArticle, 1);

        typeLabelArticleSku = addJProp("typeLabelArticleSku", "Тип этикетки (ИД)", typeLabelArticle, articleSku, 1);
        sidTypeLabelArticleSku = addJProp("sidTypeLabelArticleSku", "Код", sidTypeLabel, typeLabelArticleSku, 1);
        nameTypeLabelArticleSku = addJProp("nameTypeLabelArticleSku", "Тип этикетки", name, typeLabelArticleSku, 1);

        //Category
        typeInvoiceCategory = addDProp(idGroup, "typeInvoiceCategory", "Тип инвойса номенклатурной группы (ИД)", typeInvoice, category);
        nameTypeInvoiceCategory = addJProp(baseGroup, "nameTypeInvoiceCategory", "Тип инвойса номенклатурной группы", name, typeInvoiceCategory, 1);
        setNotNull(typeInvoiceCategory);
        
        warrantyCategory = addDProp(baseGroup, "warrantyCategory", "Гарантийный срок", NumericClass.get(14, 3), category);

        categoryArticle = addDProp(idGroup, "categoryArticle", "Номенклатурная группа товара (ИД)", category, article);
        nameOriginCategoryArticle = addJProp(intraAttributeGroup, "nameOriginCategoryArticle", "Номенклатурная группа товара (ориг.)", nameOrigin, categoryArticle, 1);
        nameCategoryArticle = addJProp(intraAttributeGroup, "nameCategoryArticle", "Номенклатурная группа товара", name, categoryArticle, 1);
        nameCategoryArticle.property.preferredCharWidth = 30;
        nameCategoryArticle.property.minimumCharWidth = 15;
        categoryArticleSku = addJProp(idGroup, true, "categoryArticleSku", true, "Номенклатурная группа товара (ИД)", categoryArticle, articleSku, 1);
        nameCategoryArticleSku = addJProp(intraAttributeGroup, "nameCategoryArticleSku", "Номенклатурная группа товара", name, categoryArticleSku, 1);
        nameCategoryArticleSku.property.preferredCharWidth = 50;
        nameCategoryArticleSku.property.minimumCharWidth = 15;
        nameOriginCategoryArticleSku = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSku", "Номенклатурная группа товара", nameOrigin, categoryArticleSku, 1);

        nameCategoryArticleLanguage = addJProp("nameCategoryArticleLanguage", "Номенклатурная группа", BL.I18n.getLCPByName("languageName"), categoryArticle, 1, 2);
        nameCategoryArticleSkuLanguage = addJProp("nameCategoryArticleSkuLanguage", "Номенклатурная группа", BL.I18n.getLCPByName("languageName"), categoryArticleSku, 1, 2);

        warrantyCategoryArticleSku = addJProp("warrantyCategoryArticleSku", "Гарантийный срок", warrantyCategory, categoryArticleSku, 1);
        warrantyDataSku = addDProp("warrantyDataSku", "Гарантийный срок", NumericClass.get(14, 3), sku);

        warrantySku = addSUProp("warrantySku", "Гарантийный срок", Union.OVERRIDE, warrantyCategoryArticleSku, warrantyDataSku);

        typeInvoiceCategoryArticle = addJProp(idGroup, "typeInvoiceCategoryArticle", "Тип инвойса артикула (ИД)", typeInvoiceCategory, categoryArticle, 1);
        typeInvoiceCategoryArticleSku = addJProp(idGroup, "typeInvoiceCategoryArticleSku", "Тип инвойса товара (ИД)", typeInvoiceCategory, categoryArticleSku, 1);

        typeInvoiceDataFreightArticle = addDProp(idGroup, "typeInvoiceDataFreightArticle", "Тип инвойса (ИД)", typeInvoice, freight, article);
        typeInvoiceCategoryFreightArticle = addJProp(idGroup, "typeInvoiceCategoryFreightArticle", "Тип инвойса (ИД)", baseLM.and1, typeInvoiceCategoryArticle, 2, is(freight), 1);
        typeInvoiceFreightArticle = addSUProp(idGroup, "typeInvoiceFreightArticle", "Тип инвойса (ИД)", Union.OVERRIDE, typeInvoiceCategoryFreightArticle, typeInvoiceDataFreightArticle);
        typeInvoiceFreightSku = addJProp(idGroup, true, "typeInvoiceFreightSku", "Тип инвойса (ИД)", typeInvoiceFreightArticle, 1, articleSku, 2);
        nameTypeInvoiceFreightArticleSku = addJProp(baseGroup, "nameTypeInvoiceFreightArticleSku", "Тип инвойса", name, typeInvoiceFreightSku, 1, 2);
        nameTypeInvoiceFreightArticleSku.property.preferredCharWidth = 50;
        nameTypeInvoiceFreightArticleSku.property.minimumCharWidth = 15;

        nameArticle = addSUProp(baseGroup, "nameArticle", "Наименование", Union.OVERRIDE, originalNameArticle, nameOriginCategoryArticle);
        nameArticleSku = addJProp(intraAttributeGroup, "nameArticleSku", "Наименование", nameArticle, articleSku, 1);

        colorSupplierItem = addDProp(idGroup, "colorSupplierItem", "Цвет поставщика (ИД)", colorSupplier, item);
        sidColorSupplierItem = addJProp(itemAttributeGroup, "sidColorSupplierItem", "Код цвета", sidColorSupplier, colorSupplierItem, 1);
        nameColorSupplierItem = addJProp(itemAttributeGroup, "nameColorSupplierItem", "Цвет поставщика", name, colorSupplierItem, 1);

        translateNameColorSupplierItem = addJProp("translateNameColorSupplierItem", "Цвет (на этикетке)", translateNameColorSupplier, colorSupplierItem, 1);

        inListArticleColorSupplier = addDProp(baseGroup, "inListArticleColorSupplier", "Наличие", LogicalClass.instance, list, article, colorSupplier);

       /*addConstraint(addJProp("Выбранный должен быть среди связанных кодов", andNot1, addCProp(LogicalClass.instance, true, article), 1,
                   addJProp(relationCustomCategory10CustomCategoryOrigin, customCategory10Article, 1, customCategoryOriginArticle, 1), 1), true);*/

        // unitOfMeasure
        sidUnitOfMeasure = addDProp(baseGroup, "sidUnitOfMeasure", "Код единицы измерения", StringClass.get(5), unitOfMeasure);

        unitOfMeasureCategory = addDProp(idGroup, "unitOfMeasureCategory", "Единица измерения (ИД)", unitOfMeasure, category);
        nameUnitOfMeasureCategory = addJProp(baseGroup, "nameUnitOfMeasureCategory", "Единица измерения", name, unitOfMeasureCategory, 1);
        unitOfMeasureCategoryArticle = addJProp(idGroup, "unitOfMeasureCategoryArticle", "Единица измерения (ИД)", unitOfMeasureCategory, categoryArticle, 1);
        unitOfMeasureDataArticle = addDProp(idGroup, "unitOfMeasureDataArticle", "Единица измерения (ИД)", unitOfMeasure, article);
        unitOfMeasureArticle = addSUProp(idGroup, "unitOfMeasureArticle", "Единица измерения", Union.OVERRIDE, unitOfMeasureCategoryArticle, unitOfMeasureDataArticle);

        nameOriginUnitOfMeasureArticle = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticle", "Единица измерения (ориг.)", nameOrigin, unitOfMeasureArticle, 1);
        nameUnitOfMeasureArticle = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticle", "Единица измерения", name, unitOfMeasureArticle, 1);
        unitOfMeasureArticleSku = addJProp(idGroup, true, "unitOfMeasureArticleSku", "Ед. изм. товара (ИД)", unitOfMeasureArticle, articleSku, 1);
        nameUnitOfMeasureArticleSku = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSku", "Ед. изм. товара", name, unitOfMeasureArticleSku, 1);
        nameOriginUnitOfMeasureArticleSku = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSku", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSku, 1);

        // Supplier
        supplierArticle = addDProp(idGroup, "supplierArticle", "Поставщик (ИД)", supplier, article);
        setNotNull(supplierArticle);
        nameSupplierArticle = addJProp(baseGroup, "nameSupplierArticle", "Поставщик", name, supplierArticle, 1);

        customsSIDArticle = addJProp(baseGroup, "customsSIDArticle", "Артикул (ИД)", customsSIDSupplier, supplierArticle, 1);
        jennyferSupplierArticle = addJProp("jennyferSupplierArticle", "Поставщик Jennyfer (ИД)", baseLM.and1, supplierArticle, 1, addJProp(is(jennyferSupplier), supplierArticle, 1), 1);
        steilmannSupplierArticle = addJProp("steilmannSupplierArticle", "Поставщик Steilmann (ИД)", baseLM.and1, supplierArticle, 1, addJProp(is(steilmannSupplier), supplierArticle, 1), 1);

        sizeGroupSupplierArticle = addDProp(idGroup, "sizeGroupSupplierArticle", "Размерная сетка (ИД)", sizeGroupSupplier, article);
        nameSizeGroupSupplierArticle = addJProp(baseGroup, "nameSizeGroupSupplierArticle", "Размерная сетка", name, sizeGroupSupplierArticle, 1);
        nameSizeGroupSupplierArticle.setMinimumCharWidth(6); nameSizeGroupSupplierArticle.setPreferredCharWidth(10);
        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику размерной сетки", baseLM.diff2,
                supplierArticle, 1,
                addJProp(supplierSizeGroup, sizeGroupSupplierArticle, 1), 1), true);

        brandSupplierDataArticle = addDProp(idGroup, "brandSupplierDataArticle", "Бренд (ИД)", brandSupplier, article);
        brandSupplierSupplierArticle = addJProp(idGroup, "brandSupplierSupplierArticle", "Бренд (ИД)", brandSupplierSupplier, supplierArticle, 1);
        brandSupplierArticle = addSUProp(idGroup, "brandSupplierArticle", "Бренд (ИД)", Union.OVERRIDE, brandSupplierSupplierArticle, brandSupplierDataArticle);
        nameBrandSupplierArticle = addJProp(baseGroup, "nameBrandSupplierArticle", "Бренд", name, brandSupplierArticle, 1);
        nameBrandSupplierArticle.property.preferredCharWidth = 30;
        nameBrandSupplierArticle.property.minimumCharWidth = 15;
        sidBrandSupplierArticle = addJProp(supplierAttributeGroup, "sidBrandSupplierArticle", "Бренд (ИД)", sidBrandSupplier, brandSupplierArticle, 1);
        sidBrandSupplierArticle.property.preferredCharWidth = 20;
        sidBrandSupplierArticle.property.minimumCharWidth = 10;

        supplierBrandSupplierArticle = addJProp(idGroup, "supplierBrandSupplierArticle", "Поставщик", supplierBrandSupplier, brandSupplierArticle, 1);
        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику бренда артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierBrandSupplier, brandSupplierArticle, 1), 1), true);

        brandSupplierArticleSku = addJProp(idGroup, true, "brandSupplierArticleSku", "Бренд (ИД)", brandSupplierArticle, articleSku, 1);
        nameBrandSupplierArticleSku = addJProp(supplierAttributeGroup, "nameBrandSupplierArticleSku", "Бренд", name, brandSupplierArticleSku, 1);
        nameBrandSupplierArticleSku.property.preferredCharWidth = 30;
        nameBrandSupplierArticleSku.property.minimumCharWidth = 15;
        sidBrandSupplierArticleSku = addJProp(supplierAttributeGroup, "sidBrandSupplierArticleSku", "Бренд(ИД)", sidBrandSupplier, brandSupplierArticleSku, 1);
        sidBrandSupplierArticleSku.property.preferredCharWidth = 20;
        sidBrandSupplierArticleSku.property.minimumCharWidth = 10;

        themeSupplierArticle = addDProp(idGroup, "themeSupplierArticle", "Тема поставщика(ИД)", themeSupplier, article);
        nameThemeSupplierArticle = addJProp(supplierAttributeGroup, "nameThemeSupplierArticle", "Тема", name, themeSupplierArticle, 1);
        nameThemeSupplierArticle.property.preferredCharWidth = 30;
        nameThemeSupplierArticle.property.minimumCharWidth = 15;

        collectionSupplierArticle = addDProp(idGroup, "collectionSupplierArticle", "Коллекция (ИД)", collectionSupplier, article);
        nameCollectionSupplierArticle = addJProp(supplierAttributeGroup, "nameCollectionSupplierArticle", "Коллекция", name, collectionSupplierArticle, 1);
        nameCollectionSupplierArticle.property.preferredCharWidth = 30;
        nameCollectionSupplierArticle.property.minimumCharWidth = 15;

        subCategorySupplierArticle = addDProp(idGroup, "subCategorySupplierArticle", "Подгруппа (ИД)", subCategorySupplier, article);
        nameSubCategorySupplierArticle = addJProp(supplierAttributeGroup, "nameSubCategorySupplierArticle", "Подгруппа", name, subCategorySupplierArticle, 1);
        nameSubCategorySupplierArticle.property.preferredCharWidth = 30;
        nameSubCategorySupplierArticle.property.minimumCharWidth = 15;

        categorySupplierArticle = addJProp("categorySupplierArticle", "Группа (ИД)", categorySupplierSubCategorySupplier, subCategorySupplierArticle, 1);
        nameCategorySupplierArticle = addJProp(supplierAttributeGroup, "nameCategorySupplierArticle", "Группа", name, categorySupplierArticle, 1);

        categorySupplierArticleSku = addJProp(idGroup, true, "categorySupplierArticleSku", "Группа (ИД)", categorySupplierArticle, articleSku, 1);
        nameCategorySupplierArticleSku = addJProp(supplierAttributeGroup, "nameCategorySupplierArticleSku", "Группа", name, categorySupplierArticleSku, 1);
        nameCategorySupplierArticleSku.property.preferredCharWidth = 30;
        nameCategorySupplierArticleSku.property.minimumCharWidth = 15;


        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику темы артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierThemeSupplier, themeSupplierArticle, 1), 1), true);

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику коллекции артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierCollectionSupplier, collectionSupplierArticle, 1), 1), true);

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику группы артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierSubCategorySupplier, subCategorySupplierArticle, 1), 1), true);

        themeSupplierArticleSku = addJProp(idGroup, "themeSupplierArticleSku", "Тема (ИД)", themeSupplierArticle, articleSku, 1);
        nameThemeSupplierArticleSku = addJProp(supplierAttributeGroup, "nameThemeSupplierArticleSku", "Тема", name, themeSupplierArticleSku, 1);
        nameThemeSupplierArticleSku.property.preferredCharWidth = 30;
        nameThemeSupplierArticleSku.property.minimumCharWidth = 15;
        sidThemeSupplierArticleSku = addJProp(baseGroup, "sidThemeSupplierArticleSku", "Код темы", sidThemeSupplier, themeSupplierArticleSku, 1);

        subCategorySupplierArticleSku = addJProp(idGroup, "subCategorySupplierArticleSku", "Подгруппа (ИД)", subCategorySupplierArticle, articleSku, 1);
        nameSubCategorySupplierArticleSku = addJProp(supplierAttributeGroup, "nameSubCategorySupplierArticleSku", "Подгруппа", name, subCategorySupplierArticleSku, 1);
        nameSubCategorySupplierArticleSku.property.preferredCharWidth = 30;
        nameSubCategorySupplierArticleSku.property.minimumCharWidth = 15;
        sidSubCategorySupplierArticleSku = addJProp(baseGroup, "sidSubCategorySupplierArticleSku", "Категория", sidSubCategorySupplier, subCategorySupplierArticleSku, 1);

        collectionSupplierArticleSku = addJProp(idGroup, "collectionSupplierArticleSku", "Коллекция (ИД)", collectionSupplierArticle, articleSku, 1);
        nameCollectionSupplierArticleSku = addJProp(supplierAttributeGroup, "nameCollectionSupplierArticleSku", "Коллекция", name, collectionSupplierArticleSku, 1);
        nameCollectionSupplierArticleSku.property.preferredCharWidth = 30;
        nameCollectionSupplierArticleSku.property.minimumCharWidth = 15;
        sidCollectionSupplierArticleSku = addJProp(baseGroup, "sidCollectionSupplierArticleSku", "Коллекция", sidCollectionSupplier, collectionSupplierArticleSku, 1);

        seasonYearSeasonSupplier = addDProp(idGroup, "seasonYearSeasonSupplier", "Сезон (ИД)", seasonYear, seasonSupplier);
        nameSeasonYearSeasonSupplier = addJProp(baseGroup, "nameSeasonYearSeasonSupplier", "Сезон", nameSeasonYear, seasonYearSeasonSupplier, 1);

        seasonSupplierArticle = addDProp(idGroup, "seasonSupplierArticle", "Сезон поставщика (ИД)", seasonSupplier, article);
        sidSeasonSupplierArticle = addJProp("sidSeasonSupplierArticle", "Сезон поставщика", sidSeasonSupplier, seasonSupplierArticle, 1);

        seasonYearSeasonSupplierArticle = addJProp(idGroup, "seasonYearSeasonSupplierArticle", "Сезон (ИД)", seasonYearSeasonSupplier, seasonSupplierArticle, 1);

        seasonYearDataArticle = addDProp(itemAttributeGroup, "seasonYearDataArticle", "Сезон (ИД)", seasonYear, article);
        seasonYearArticle = addSUProp(idGroup, "seasonYearArticle", "Сезон (ИД)", Union.OVERRIDE, seasonYearSeasonSupplierArticle, seasonYearDataArticle);

        nameSeasonYearArticle = addJProp(itemAttributeGroup, "nameSeasonYearArticle", "Сезон", nameSeasonYear, seasonYearArticle, 1);
        nameSeasonYearArticle.property.preferredCharWidth = 30;
        nameSeasonYearArticle.property.minimumCharWidth = 15;
        seasonYearArticleSku = addJProp(idGroup, "seasonYearArticleSku", "Сезон", seasonYearArticle, articleSku, 1);
        nameSeasonYearArticleSku = addJProp(baseGroup, "nameSeasonYearArticleSku", "Сезон", nameSeasonYear, seasonYearArticleSku, 1);

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику сезона артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierSeasonSupplier, seasonSupplierArticle, 1), 1), true);


        articleSIDSupplier = addAGProp(idGroup, "articleSIDSupplier", "Артикул (ИД)", sidArticle, supplierArticle);
        articleCustomsSIDSupplier = addAGProp(idGroup, "articleCustomsSIDSupplier", "Артикул (ИД)", sidArticle, customsSIDArticle);

        seekArticleSIDSupplier = addJoinAProp("Поиск артикула", addSAProp(), articleSIDSupplier, 1, 2);
        seekArticleSIDInvoice = addJoinAProp("Поиск артикула", seekArticleSIDSupplier, 1, supplierDocument, 2);

        addNEColorSupplierSIDSupplier = addIfAProp("Ввод цвета (НС)", true, colorSIDSupplier, 1, 2, addAAProp(colorSupplier, sidColorSupplier, supplierColorSupplier), 1, 2);
        addNEColorSupplierSIDInvoice = addJoinAProp("Ввод цвета (НС)", addNEColorSupplierSIDSupplier, 1, supplierDocument, 2);

        executeAddColorDocument = addSetPropertyAProp("Наличие цвета", inListArticleColorSupplier, 1, 2, 3, baseLM.vtrue);

        seekColorSIDSupplier = addJoinAProp("Поиск цвета", addSAProp(), colorSIDSupplier, 1, 2);
        seekColorSIDInvoice = addJoinAProp("Поиск цвета", seekColorSIDSupplier, 1, supplierDocument, 2);

        executeArticleCompositeItemSIDSupplier = addSetPropertyAProp("Замена артикула", articleCompositeItem, 1, articleSIDSupplier, 2, 3);

        executeChangeFreightClass = addIfAProp("Изменить класс фрахта", is(freight), 1, addChangeClassAProp(), 1, 2);

        executeChangeFreightClassApply = addListAProp(executeChangeFreightClass, 1, 2, baseLM.apply, baseLM.cancel);

        executeChangeFreightChangedClass = addIfAProp("Пометить как обработанный", addJProp(baseLM.andNot1, is(freightComplete), 1, is(freightChanged), 1), 1,
                addJoinAProp(executeChangeFreightClassApply, 1, addCProp(baseClass.objectClass, "freightChanged")), 1);
        executeChangeFreightChangedClass.property.askConfirm = true;
        executeChangeFreightChangedClass.setImage("sign_tick.png");

        addressSupplier = BL.LegalEntity.getLCPByName("addressLegalEntity");

        supplierArticleSku = addJProp(idGroup, "supplierArticleSku", "Поставщик (ИД)", supplierArticle, articleSku, 1);
        nameSupplierArticleSku = addJProp(baseGroup, "nameSupplierArticleSku", "Поставщик", name, supplierArticleSku, 1);
        addressSupplierArticleSku = addJProp(baseGroup, "addressSupplierArticleSku", "Адрес", addressSupplier, supplierArticleSku, 1);

        jennyferSupplierArticleSku = addJProp("jennyferSupplierArticleSku", "Поставщик Jennyfer (ИД)", jennyferSupplierArticle, articleSku, 1);

        sizeSupplierItem = addDProp(itemAttributeGroup, "sizeSupplierItem", "Размер поставщика (ИД)", sizeSupplier, item);
        sidSizeSupplierItem = addJProp(itemAttributeGroup, "sidSizeSupplierItem", "Размер поставщика", sidSizeSupplier, sizeSupplierItem, 1);

        commonSizeItem = addJProp(idGroup, "commonSizeItem", "Размер (ИД)", commonSizeSizeSupplier, sizeSupplierItem, 1);
        nameCommonSizeItem = addJProp("nameCommonSizeItem", "Размер", name, commonSizeItem, 1);

        genderGenderSupplier = addDProp(idGroup, "genderGenderSupplier", "Унифицированный пол", gender, genderSupplier);
        sidGenderGenderSupplier = addJProp(baseGroup, "sidGenderGenderSupplier", "Пол", sidGender, genderGenderSupplier, 1);

        genderSupplierArticle = addDProp(itemAttributeGroup, "genderSupplierArticle", "Пол поставщика (ИД)", genderSupplier, article);
        sidGenderSupplierArticle = addJProp(itemAttributeGroup, "sidGenderSupplierArticle", "Пол поставщика", sidGenderSupplier, genderSupplierArticle, 1);

        genderOriginArticle = addJProp(itemAttributeGroup, "genderOriginArticle", "Пол (ИД)", genderGenderSupplier, genderSupplierArticle, 1);


        LCP supplierColorItem = addJProp(supplierColorSupplier, colorSupplierItem, 1);
        addConstraint(addJProp("Поставщик товара должен соответствовать цвету поставщика", baseLM.diff2,
                supplierArticleSku, 1,
                supplierColorItem, 1), true);

        LCP supplierSizeItem = addJProp(supplierSizeSupplier, sizeSupplierItem, 1);
        addConstraint(addJProp("Поставщик товара должен соответствовать размеру поставщика", baseLM.diff2,
                supplierArticleSku, 1,
                supplierSizeItem, 1), true);

        equalsColorItemSupplier = addJProp(baseLM.equals2, supplierColorItem, 1, 2); // временное решение
        equalsSizeItemSupplier = addJProp(baseLM.equals2, supplierSizeItem, 1, 2); // временное решение

        //LCP supplierSeasonSupplierArticle = addJProp(supplierSeasonSupplier, seasonSupplierArticle, 1);
        //addConstraint(addJProp("Поставщик товара должен соответствовать сезону поставщика", baseLM.diff2,
        //        supplierArticle, 1,
        //        supplierSeasonSupplierArticle, 1), true);

        LCP supplierThemeArticle = addJProp(supplierThemeSupplier, themeSupplierArticle, 1);
        addConstraint(addJProp("Поставщик товара должен соответствовать теме поставщика", baseLM.diff2,
                supplierArticle, 1,
                supplierThemeArticle, 1), true);

        //equalsSeasonSupplierArticleSupplier = addJProp(baseLM.equals2, supplierSeasonSupplierArticle, 1, 2); // временное решение
        equalsThemeItemSupplier = addJProp(baseLM.equals2, supplierThemeArticle, 1, 2); // временное решение

        addItemArticleCompositeColorSizeBarcode = addJoinAProp("Ввод товара", addAAProp(item, articleCompositeItem, colorSupplierItem, sizeSupplierItem, barcode), 1, 2, 3, 4);
        addItemSIDArticleSupplierColorSizeBarcode = addJoinAProp("Ввод товара", addItemArticleCompositeColorSizeBarcode, articleSIDSupplier, 1, 2, 3, 4, 5);

        // Weight
        netWeightArticle = addDProp(supplierAttributeGroup, "netWeightArticle", "Вес нетто (ориг.)", NumericClass.get(14, 6), article);
        netWeightArticleSku = addJProp(intraAttributeGroup, "netWeightArticleSku", "Вес нетто (ориг.)", netWeightArticle, articleSku, 1);
        netWeightArticleSize = addDProp(intraAttributeGroup, "netWeightArticleSize", "Вес нетто размера", NumericClass.get(14, 6), article, sizeSupplier);

        netWeightDataSku = addDProp(intraAttributeGroup, "netWeightDataSku", "Вес нетто", NumericClass.get(14, 6), sku);
        netWeightArticleSizeSku = addJProp(intraAttributeGroup, true, "netWeightArticleSizeSku", "Вес нетто", netWeightArticleSize, articleSku, 1, sizeSupplierItem, 1);
        netWeightSku = addSUProp(intraAttributeGroup, "netWeightSku", "Вес нетто (ед.)", Union.OVERRIDE, netWeightArticleSku, netWeightArticleSizeSku);

        // Gender
        genderSupplierArticleSku = addJProp(idGroup, "genderSupplierArticleSku", "Пол (ИД)", genderSupplierArticle, articleSku, 1);
        sidGenderSupplierArticleSku = addJProp(baseGroup, "sidGenderSupplierArticleSku", "Пол", sidGenderSupplier, genderSupplierArticleSku, 1);

        genderBrandSupplier = addDProp(idGroup, "genderBrandSupplier", "Пол (ИД)", gender, brandSupplier);
        sidGenderBrandSupplier = addJProp(baseGroup, "sidGenderBrandSupplier", "Пол", sidGender, genderBrandSupplier, 1);

        genderBrandSupplierArticle = addJProp(idGroup, "genderBrandSupplierArticle", "Пол (ИД)", genderBrandSupplier, brandSupplierArticle, 1);

        genderDataArticle = addDProp(idGroup, "genderDataArticle", "Пол (ИД)", gender, article);
        genderArticle = addSUProp(idGroup, "genderArticle", "Пол (ИД)", Union.OVERRIDE, genderBrandSupplierArticle, genderOriginArticle, genderDataArticle);
        sidGenderArticle = addJProp(baseGroup, "sidGenderArticle", "Пол", sidGender, genderArticle, 1);
        sidGenderArticle.property.preferredCharWidth = 5;
        sidGenderArticle.property.minimumCharWidth = 3;

        //nameGenderArticle = addJProp(baseGroup, "nameGenderArticle", "Пол", name, genderArticle, 1);
        //nameGenderArticle.property.preferredCharWidth = 5;
        //nameGenderArticle.property.minimumCharWidth = 3;

        genderArticleSku = addJProp(idGroup, true, "genderArticleSku", true, "Пол (ИД)", genderArticle, articleSku, 1);
        sidGenderArticleSku = addJProp(baseGroup, "sidGenderArticleSku", "Пол", sidGender, genderArticleSku, 1);
        sidGenderArticleSku.property.preferredCharWidth = 5;
        sidGenderArticleSku.property.minimumCharWidth = 3;

        sidGenderArticleSkuLanguage = addJProp("sidGenderArticleSkuLanguage", "Пол", BL.I18n.getLCPByName("languageName"), genderArticleSku, 1, 2);

        // Country
        countrySupplierOfOriginArticle = addDProp(idGroup, "countrySupplierOfOriginArticle", "Страна происхождения (ИД)", countrySupplier, article);
        nameCountrySupplierOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticle", "Страна происхождения (ориг.)", name, countrySupplierOfOriginArticle, 1);
        nameCountrySupplierOfOriginArticle.property.preferredCharWidth = 30;
        nameCountrySupplierOfOriginArticle.property.minimumCharWidth = 15;

        countrySupplierOfOriginArticleSku = addJProp(idGroup, "countrySupplierOfOriginArticleSku", "Страна происхождения (ИД)", countrySupplierOfOriginArticle, articleSku, 1);
        nameCountrySupplierOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountrySupplierOfOriginArticleSku", "Страна происхождения (ориг.)", name, countrySupplierOfOriginArticleSku, 1);
        nameCountrySupplierOfOriginArticleSku.property.preferredCharWidth = 30;
        nameCountrySupplierOfOriginArticleSku.property.minimumCharWidth = 15;

        LCP nameOriginCountry = BL.getModule("Country").getLCPByName("nameOriginCountry");

        countryOfOriginArticle = addJProp(idGroup, "countryOfOriginArticle", "Страна происхождения (ИД)", countryCountrySupplier, countrySupplierOfOriginArticle, 1);
        nameCountryOfOriginArticle = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticle", "Страна происхождения", nameOriginCountry, countryOfOriginArticle, 1);

        countryOfOriginArticleSku = addJProp(idGroup, "countryOfOriginArticleSku", "Страна происхождения (ИД)", countryOfOriginArticle, articleSku, 1);
        nameCountryOfOriginArticleSku = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSku", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSku, 1);

        countryOfOriginArticleColor = addDProp(idGroup, "countryOfOriginArticleColor", "Страна происхождения (ИД)", country, article, colorSupplier);
        countryOfOriginArticleColorSku = addJProp(idGroup, true, "countryOfOriginArticleColorSku", "Страна происхождения (ИД)", countryOfOriginArticleColor, articleSku, 1, colorSupplierItem, 1);
        nameCountryArticleColor = addJProp(baseGroup, "nameCountryArticleColor", "Страна происхождения", BL.Country.getLCPByName("nameCountry"), countryOfOriginArticleColor, 1, 2);
        nameCountryArticleColor.property.preferredCharWidth = 50;
        nameCountryArticleColor.property.minimumCharWidth = 15;

        countryOfOriginDataSku = addDProp(idGroup, "countryOfOriginDataSku", "Страна происхождения (ИД) (первичное)", country, sku);

        countryOfOriginSku = addSUProp(idGroup, "countryOfOriginSku", true, "Страна происхождения (ИД)", Union.OVERRIDE, countryOfOriginArticleSku, countryOfOriginArticleColorSku);
        nameCountryOfOriginSku = addJProp(intraAttributeGroup, "nameCountryOfOriginSku", "Страна происхождения", nameOriginCountry, countryOfOriginSku, 1);
        nameCountrySku = addJProp(intraAttributeGroup, "nameCountrySku", "Страна происхождения", BL.Country.getLCPByName("nameCountry"), countryOfOriginSku, 1);
        nameCountrySku.property.preferredCharWidth = 50;
        nameCountrySku.property.minimumCharWidth = 15;

        nameCountrySkuLanguage = addJProp("nameCountrySkuLanguage", "Страна", BL.I18n.getLCPByName("languageName"), countryOfOriginSku, 1,  2);

        addConstraint(addJProp("Поставщик артикула должен соответствовать поставщику страны артикула", baseLM.diff2,
                supplierArticle, 1, addJProp(supplierCountrySupplier, countrySupplierOfOriginArticle, 1), 1), true);

        countryBrandSupplierSku = addJProp(idGroup, "countryBrandSupplierSku", "Страна поставки (ИД)", countryBrandSupplier, brandSupplierArticleSku, 1);
        nameCountryBrandSupplierSku = addJProp(baseGroup, "nameCountryBrandSupplierSku", "Страна поставки", BL.Country.getLCPByName("nameCountry"), countryBrandSupplierSku, 1);

        nameCountryBrandSupplierSkuLanguage = addJProp("nameCountryBrandSupplierSkuLanguage", "Страна поставки", BL.I18n.getLCPByName("languageName"), countryBrandSupplierSku, 1, 2);

        // Composition
        mainCompositionOriginArticle = addDProp(supplierAttributeGroup, "mainCompositionOriginArticle", "Состав", COMPOSITION_CLASS, article);
        mainCompositionOriginArticle.property.preferredCharWidth = 70;
        mainCompositionOriginArticle.property.minimumCharWidth = 35;
        additionalCompositionOriginArticle = addDProp(supplierAttributeGroup, "additionalCompositionOriginArticle", "Доп. состав", COMPOSITION_CLASS, article);
        additionalCompositionOriginArticle.property.preferredCharWidth = 40;
        additionalCompositionOriginArticle.property.minimumCharWidth = 20;

        mainCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "mainCompositionOriginArticleSku", "Состав", mainCompositionOriginArticle, articleSku, 1);
        additionalCompositionOriginArticleSku = addJProp(supplierAttributeGroup, "additionalCompositionOriginArticleSku", "Доп. состав", additionalCompositionOriginArticle, articleSku, 1);

        mainCompositionOriginArticleColor = addDProp(supplierAttributeGroup, "mainCompositionOriginArticleColor", "Состав", COMPOSITION_CLASS, article, colorSupplier);
        additionalCompositionOriginArticleColor = addDProp(supplierAttributeGroup, "additionalCompositionOriginArticleColor", "Доп. состав", COMPOSITION_CLASS, article, colorSupplier);

        mainCompositionOriginArticleColorSku = addJProp(supplierAttributeGroup, true, "mainCompositionOriginArticleColorSku", "Состав", mainCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);
        additionalCompositionOriginArticleColorSku = addJProp(supplierAttributeGroup, true, "additionalCompositionOriginArticleColorSku", "Доп. состав", additionalCompositionOriginArticleColor, articleSku, 1, colorSupplierItem, 1);

        mainCompositionOriginDataSku = addDProp(intraAttributeGroup, "mainCompositionOriginDataSku", "Состав", COMPOSITION_CLASS, sku);
        additionalCompositionOriginDataSku = addDProp(intraAttributeGroup, "additionalCompositionOriginDataSku", "Доп. состав", COMPOSITION_CLASS, sku);

        mainCompositionOriginSku = addSUProp(intraAttributeGroup, "mainCompositionOriginSku", true, "Состав", Union. OVERRIDE, mainCompositionOriginArticleSku, mainCompositionOriginArticleColorSku);
        mainCompositionOriginSku.setPreferredCharWidth(80);

        additionalCompositionOriginSku = addSUProp(intraAttributeGroup, "additionalCompositionOriginSku", "Доп. состав", Union.OVERRIDE, additionalCompositionOriginArticleSku, additionalCompositionOriginArticleColorSku);
        additionalCompositionOriginSku.property.preferredCharWidth = 40;
        additionalCompositionOriginSku.property.minimumCharWidth = 20;

        mainCompositionArticle = addDProp(intraAttributeGroup, "mainCompositionArticle", "Состав (рус.)", COMPOSITION_CLASS, article);
        additionalCompositionArticle = addDProp(intraAttributeGroup, "additionalCompositionArticle", "Доп. состав (рус.)", COMPOSITION_CLASS, article);

        mainCompositionSku = addDProp(intraAttributeGroup, "mainCompositionSku", "Состав (рус.)", COMPOSITION_CLASS, sku);
        mainCompositionOriginSku.setPreferredCharWidth(80);

        additionalCompositionSku = addDProp(intraAttributeGroup, "additionalCompositionSku", "Доп. состав (рус.)", COMPOSITION_CLASS, sku);
        additionalCompositionSku.property.preferredCharWidth = 40;
        additionalCompositionSku.property.minimumCharWidth = 20;

        mainCompositionSkuLanguage = addDProp("mainCompositionSkuLanguage", "Состав (укр.)", COMPOSITION_CLASS, sku, (CustomClass) BL.I18n.getClassByName("Language"));
        additionalCompositionSkuLanguage = addDProp ("additionalCompositionSkuLanguage" , "Доп. состав (укр.)", COMPOSITION_CLASS, sku, (CustomClass) BL.I18n.getClassByName("Language"));

        mainCompositionOriginSkuLanguage = addJProp(baseLM.and1, mainCompositionOriginSku, 1, is((CustomClass) BL.I18n.getClassByName("Language")), 2);
        additionalCompositionOriginSkuLanguage = addJProp(baseLM.and1, additionalCompositionOriginSku, 1, is((CustomClass) BL.I18n.getClassByName("Language")), 2);

        translationMainCompositionSkuLanguage = addJoinAProp(actionGroup, "translationMainCompositionSkuLanguage", "Перевод состава", addTAProp(mainCompositionOriginSkuLanguage, mainCompositionSkuLanguage), BL.RomanI18n.getLCPByName("dictionaryCompositionLanguage"), 2, 1, 2);
        translationAdditionalCompositionSkuLanguage = addJoinAProp(actionGroup, "translationAdditionalCompositionSkuLanguage", "Перевод доп. состава", addTAProp(additionalCompositionOriginSkuLanguage, additionalCompositionSkuLanguage), BL.RomanI18n.getLCPByName("dictionaryCompositionLanguage"), 2, 1, 2);

        mainCompositionSkuInvoice = addJProp("mainCompositionSkuInvoice", "Состав (укр.)", mainCompositionSkuLanguage, 1, languageInvoice, 2);

        translationMainCompositionSkuInvoice = addJoinAProp("translationMainCompositionSkuInvoice", "Перевести", translationMainCompositionSkuLanguage, 1, languageInvoice, 2);
        translationMainCompositionSkuInvoice.addToContextMenuFor(mainCompositionSkuInvoice);

        additionalCompositionSkuInvoice = addJProp("additionalCompositionSkuInvoice", "Доп. состав (укр.)", additionalCompositionSkuLanguage, 1, languageInvoice, 2);

        translationAdditionalCompositionSkuInvoice = addJoinAProp("translationAdditionalCompositionSkuInvoice", "Перевести", translationAdditionalCompositionSkuLanguage, 1, languageInvoice, 2);
        translationAdditionalCompositionSkuInvoice.addToContextMenuFor(additionalCompositionSkuInvoice);

        //translationNameSkuFreight = addJoinAProp("translationNameSkuFreight", "Перевод", translationNameSkuLanguage, 1, languageFreight, 2);

        // CustomCategory
        customCategoryOriginArticle = addDProp(idGroup, "customCategoryOriginArticle", "ТН ВЭД (ориг.) (ИД)", customCategoryOrigin, article);
        sidCustomCategoryOriginArticle = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticle", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategory6Article = addDProp(idGroup, "customCategory6Article", "ТН ВЭД (6) (ИД)", customCategory6, article);
        sidCustomCategory6Article = addJProp(supplierAttributeGroup, "sidCustomCategory6Article", "Код ТН ВЭД (6)", sidCustomCategory6, customCategory6Article, 1);
        customCategoryOriginArticleSku = addJProp(idGroup, true, "customCategoryOriginArticleSku", "ТН ВЭД (ориг.) (ИД)", customCategoryOriginArticle, articleSku, 1);
        sidCustomCategoryOriginArticleSku = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSku", "Код ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginArticleSku, 1);

        // Type fabric
        typeFabricArticle = addDProp(idGroup, "typeFabricArticle", "Тип одежды (ИД)", typeFabric, article);
        typeFabricCustomCategoryOriginArticle = addJProp(idGroup, "typeFabricCustomCategoryOriginArticle", "Тип одежды (ИД)", typeFabricCustomCategoryOrigin, customCategoryOriginArticle, 1);
        overTypeFabricArticle = addSUProp(Union.OVERRIDE, typeFabricCustomCategoryOriginArticle, typeFabricArticle);

        nameTypeFabricArticle = addJProp(baseGroup, "nameTypeFabricArticle", "Тип одежды", name, overTypeFabricArticle, 1);
        nameTypeFabricArticle.property.preferredCharWidth = 10;
        nameTypeFabricArticle.property.minimumCharWidth = 5;
        typeFabricArticleSku = addJProp(idGroup, true, "typeFabricArticleSku", true, "Тип одежды (ИД)", overTypeFabricArticle, articleSku, 1);
        nameTypeFabricArticleSku = addJProp(baseGroup, "nameTypeFabricArticleSku", "Тип одежды", name, typeFabricArticleSku, 1);
        nameTypeFabricArticleSku.property.preferredCharWidth = 10;
        nameTypeFabricArticleSku.property.minimumCharWidth = 5;
        nameTypeFabricArticleSkuLanguage = addJProp(baseGroup, "nameTypeFabricArticleSkuLanguage", "Тип одежды", BL.I18n.getLCPByName("languageName"), typeFabricArticleSku, 1, 2);

        // commonSize
        commonSizeDataSku = addDProp("commonSizeDataSku", "Унифицированный размер (ИД)", commonSize, sku);
        commonSizeCategorySku = addJProp(idGroup, "commonSizeCategorySku", "Унифицированный размер (ИД)", commonSizeSizeSupplierGenderCategory, sizeSupplierItem, 1, genderArticleSku, 1, categoryArticleSku, 1);
        commonSizeTypeFabricSku = addJProp(idGroup, "commonSizeTypeFabricSku", "Унифицированный размер (ИД)", commonSizeSizeSupplierGenderCategoryTypeFabric, sizeSupplierItem, 1, genderArticleSku, 1, categoryArticleSku, 1, typeFabricArticleSku, 1);

        commonSizeSku = addSUProp("commonSizeSku", "Унифицированный размер (ИД)", Union.OVERRIDE, commonSizeCategorySku, commonSizeDataSku);
        nameCommonSizeSku = addJProp(baseGroup, "nameCommonSizeSku", "Унифицированный размер", name, commonSizeSku, 1);


        customCategory10CategoryGenderCompositionTypeFabric = addDProp(idGroup, "customCategory10FreightCategoryGenderCompositionTypeFabric", "ТН ВЭД (ИД)", customCategory10, category, gender, COMPOSITION_CLASS, typeFabric);
        sidCustomCategory10CategoryGenderCompositionTypeFabric = addJProp(baseGroup, "sidCustomCategory10CategoryGenderCompositionTypeFabric", "ТН ВЭД", sidCustomCategory10, customCategory10CategoryGenderCompositionTypeFabric, 1, 2, 3, 4);

        customCategory10CategoryGenderCompositionTypeFabricSku = addJProp(idGroup, "customCategory10CategoryGenderCompositionTypeFabricSku", "ТН ВЭД (ИД)", customCategory10CategoryGenderCompositionTypeFabric,
                                categoryArticleSku, 1, genderArticleSku, 1, mainCompositionOriginSku, 1, typeFabricArticleSku, 1);

        customCategory10CategoryGenderCompositionTypeFabricCustomsZone = addDProp(idGroup, "customCategory10CategoryGenderCompositionTypeFabricCustomsZone", "ТН ВЭД (ИД)", customCategory10, category, gender, COMPOSITION_CLASS, typeFabric, customsZone);
        sidCustomCategory10CategoryGenderCompositionTypeFabricCustomsZone = addJProp(baseGroup, "sidCustomCategory10CategoryGenderCompositionTypeFabricCustomsZone", "ТН ВЭД", sidCustomCategory10, customCategory10CategoryGenderCompositionTypeFabricCustomsZone, 1, 2, 3, 4, 5);
        customCategory10CategoryGenderCompositionTypeFabricSkuCustomsZone = addJProp(idGroup, "customCategory10CategoryGenderCompositionTypeFabricSkuCustomsZone", "ТН ВЭД (ИД)", customCategory10CategoryGenderCompositionTypeFabricCustomsZone,
                                categoryArticleSku, 1, genderArticleSku, 1, mainCompositionOriginSku, 1, typeFabricArticleSku, 1, 2);

        customCategory10DataSku = addDProp(idGroup, "customCategory10DataSku", "ТН ВЭД (ИД)", customCategory10, sku);
        customCategory10CustomCategoryOriginArticle = addJProp(idGroup, "customCategory10CustomCategoryOriginArticle", "ТН ВЭД (ИД)", customCategory10CustomCategoryOrigin, customCategoryOriginArticle, 1);
        customCategory10CustomCategoryOriginArticleSku = addJProp(idGroup, "customCategory10CustomCategoryOriginArticleSku", "ТН ВЭД (ИД)", customCategory10CustomCategoryOriginArticle, articleSku, 1);
        customCategory10Sku = addSUProp(idGroup, "customCategory10Sku", true, "ТН ВЭД (ИД)", Union.OVERRIDE, customCategory10CustomCategoryOriginArticleSku, customCategory10CategoryGenderCompositionTypeFabricSku, customCategory10DataSku);
        customCategory9Sku = addJProp(baseGroup, "customCategory9Sku", "ТН ВЭД", customCategory9CustomCategory10, customCategory10Sku, 1);
        sidCustomCategory10Sku = addJProp(baseGroup, "sidCustomCategory10Sku", "ТН ВЭД", sidCustomCategory10, customCategory10Sku, 1);

        diffCountRelationCustomCategory10Sku = addJProp("diffCountRelationCustomCategory10Sku", baseLM.greater2, addJProp(countRelationCustomCategory10, customCategory10Sku, 1), 1, addCProp(IntegerClass.instance, 1));
        diffCountRelationCustomCategory10FreightSku = addJProp("diffCountRelationCustomCategory10FreightSku", baseLM.and1, diffCountRelationCustomCategory10Sku, 2, is(freight), 1);

        subCategoryDataSku = addDProp(idGroup, "subCategoryDataSku", "Дополнительное деление (ИД)", subCategory, sku);
        nameSubCategoryDataSku = addJProp(baseGroup, "nameSubCategoryDataSku", "Дополнительное деление", nameSubCategory, subCategoryDataSku, 1);
        subCategoryCustomCategory10Sku = addJProp(idGroup, "subCategoryCustomCategory10Sku", "Дополнительное деление (ИД)", subCategoryCustomCategory10, customCategory10Sku, 1);

        subCategorySku = addSUProp(idGroup, "subCategorySku", "Дополнительное деление (ИД)", Union.OVERRIDE, subCategoryCustomCategory10Sku, subCategoryDataSku);
        nameSubCategorySku = addJProp(baseGroup, "nameSubCategorySku", "Дополнительное деление", nameSubCategory, subCategorySku, 1);

        addConstraint(addJProp("Выбранный для товара ТН ВЭД должен иметь верхний элемент", baseLM.andNot1, customCategory10Sku, 1, customCategory9Sku, 1), false);

        addConstraint(addJProp("Категория минимальных предельных цен запрещена для выбранного кода ТНВЭД", and(false, false, true), addCProp(LogicalClass.instance, true, sku), 1,
                   customCategory10Sku, 1,
                   subCategoryDataSku, 1,
                   addJProp(relationCustomCategory10SubCategory, customCategory10Sku, 1, subCategorySku, 1), 1), true);

        // barcode Jennyfer
        substring10 = addSFProp("substring10", "substring(prm1,1,10)", StringClass.get(10), 1);
        substring10s13 = addJProp(baseLM.and1, substring10, 1, is(StringClass.get(13)), 1);

        barcode10 = addJProp("barcode10", "Штрих-код(10)", substring10, barcode, 1);
        skuJennyferBarcode10 = addMGProp("skuJennyferBarcode10", "Товар (ИД)", addJProp(baseLM.and1, object(sku), 1, addJProp(is(jennyferSupplier), supplierArticleSku, 1), 1),
                barcode10, 1);
        skuJennyferBarcode = addJProp("skuJennyferBarcode", "Товар (ИД)", skuJennyferBarcode10, substring10s13, 1);

        skuBarcodeObject = addSUProp(Union.OVERRIDE, barcodeToObject, skuJennyferBarcode);

        sidDocument = addDProp(baseGroup, "sidDocument", "Код документа", StringClass.get(50), document);
        documentSIDSupplier = addAGProp(idGroup, "documentSIDSupplier", "Документ поставщика (ИД)", sidDocument, supplierDocument);

        // коробки
        sidSupplierBox = addDProp(baseGroup, "sidSupplierBox", "Номер короба", StringClass.get(50), supplierBox);
        sidSupplierBox.property.preferredCharWidth = 50;
        sidSupplierBox.property.minimumCharWidth = 20;

        boxInvoiceSupplierBox = addDProp(idGroup, "boxInvoiceSupplierBox", "Документ по коробам (ИД)", boxInvoice, supplierBox);
        setNotNull(boxInvoiceSupplierBox, PropertyFollows.RESOLVE_FALSE);

        sidBoxInvoiceSupplierBox = addJProp(baseGroup, "sidBoxInvoiceSupplierBox", "Документ по коробам", sidDocument, boxInvoiceSupplierBox, 1);

        destinationInvoiceSupplierBox = addJProp(idGroup, "destinationInvoiceSupplierBox", "Пункт назначения (ИД)", destinationDestinationDocument, boxInvoiceSupplierBox, 1);
        nameDestinationInvoiceSupplierBox = addJProp(baseGroup, "nameDestinationInvoiceSupplierBox", "Пункт назначения", name, destinationInvoiceSupplierBox, 1);
        nameDestinationInvoiceSupplierBox.property.preferredCharWidth = 50;
        nameDestinationInvoiceSupplierBox.property.minimumCharWidth = 20;

        supplierSupplierBox = addJProp(idGroup, "supplierSupplierBox", "Поставщик (ИД)", supplierDocument, boxInvoiceSupplierBox, 1);

        supplierBoxSIDSupplier = addAGProp(idGroup, "supplierBoxSIDSupplier", "Короб поставщика (ИД)", sidSupplierBox, supplierSupplierBox);

        seekSupplierBoxSIDSupplier = addJoinAProp("Поиск короба поставщика", addSAProp(), supplierBoxSIDSupplier, 1, 2);

        // заказ по артикулам
        documentList = addCUProp(idGroup, "documentList", "Документ (ИД)", object(order), object(simpleInvoice), boxInvoiceSupplierBox);
        supplierList = addJProp(idGroup, "supplierList", "Поставщик (ИД)", supplierDocument, documentList, 1);

        articleSIDList = addJProp(idGroup, "articleSIDList", "Артикул (ИД)", articleSIDSupplier, 1, supplierList, 2);

        numberListArticle = addDProp(baseGroup, "numberListArticle", "Номер", IntegerClass.instance, list, article);
        numberListSIDArticle = addJProp(numberListArticle, 1, articleSIDList, 2, 1);

        //notZeroListArticle = addJProp(baseLM.andNot1, );

        numberDataListSku = addDProp(baseGroup, "numberDataListSku", "Номер", IntegerClass.instance, list, sku);
        numberArticleListSku = addJProp(baseGroup, "numberArticleListSku", "Номер (артикула)", numberListArticle, 1, articleSku, 2);

        numberListSku = addSUProp("numberListSku", "Номер", Union.OVERRIDE, numberArticleListSku, numberDataListSku);

        numberDocumentArticle = addSGProp(baseGroup, "inDocumentArticle", numberListArticle, documentList, 1, 2);

        incrementNumberListSID = addIfAProp("Добавить строку", true, numberListSIDArticle, 1, 2,
                addJoinAProp(addIAProp(numberListArticle, 1),
                        1, articleSIDList, 2, 1), 1, 2); // если еще не было добавлено такой строки

        //price and catalog (pricat)
        barcodePricat = addDProp(baseGroup, "barcodePricat", "Штрих-код", StringClass.get(13), pricat);
        articleNumberPricat = addDProp(baseGroup, "articleNumberPricat", "Артикул", StringClass.get(20), pricat);
        customCategoryOriginalPricat = addDProp(baseGroup, "customCategoryOriginalPricat", "Код ЕС (10)", StringClass.get(10), pricat);
        colorCodePricat = addDProp(baseGroup, "colorCodePricat", "Код цвета", StringClass.get(20), pricat);
        colorNamePricat = addDProp(baseGroup, "colorNamePricat", "Цвет", StringClass.get(50), pricat);
        themeCodePricat = addDProp(baseGroup, "themeCodePricat", "Код темы", StringClass.get(20), pricat);
        themeNamePricat = addDProp(baseGroup, "themeNamePricat", "Тема", StringClass.get(50), pricat);
        subCategoryCodePricat = addDProp(baseGroup, "subCategoryCodePricat", "Код подгруппы", StringClass.get(20), pricat);
        subCategoryNamePricat = addDProp(baseGroup, "subCategoryNamePricat", "Подгруппа", StringClass.get(50), pricat);
        sizePricat = addDProp(baseGroup, "sizePricat", "Размер", StringClass.get(5), pricat);
        seasonPricat = addDProp(baseGroup, "seasonPricat", "Сезон", StringClass.get(10), pricat);
        genderPricat = addDProp(baseGroup, "genderPricat", "Пол", StringClass.get(10), pricat);
        brandNamePricat = addDProp(baseGroup, "brandNamePricat", "Бренд", StringClass.get(50), pricat);
        originalNamePricat = addDProp(baseGroup, "originalNamePricat", "Наименование (ориг.)", StringClass.get(50), pricat);
        countryPricat = addDProp(baseGroup, "countryPricat", "Страна происхождения", StringClass.get(20), pricat);
        netWeightPricat = addDProp(baseGroup, "netWeightPricat", "Вес нетто", NumericClass.get(14, 3), pricat);
        compositionPricat = addDProp(baseGroup, "compositionPricat", "Состав", StringClass.get(50), pricat);
        pricePricat = addDProp(baseGroup, "pricePricat", "Цена", NumericClass.get(14, 4), pricat);
        rrpPricat = addDProp(baseGroup, "RRP", "Рекомендованная цена", NumericClass.get(14, 4), pricat);
        destinationPricat = addDProp("destinationPricat", "Пункт назначения", destination, pricat);
        supplierPricat = addDProp("supplierPricat", "Поставщик", supplier, pricat);
        barcodeToPricat = addAGProp("barcodeToPricat", "штрих-код", barcodePricat);
        importPricatSupplier = addProperty(null, new LAP(new PricatEDIImportActionProperty(genSID(), this, supplier)));
        hugoBossImportPricat = addProperty(null, new LAP(new HugoBossPricatCSVImportActionProperty(genSID(), this, hugoBossSupplier)));
        gerryWeberImportPricat = addProperty(null, new LAP(new GerryWeberPricatCSVImportActionProperty(genSID(), this, gerryWeberSupplier)));

        // кол-во заказа
        quantityDataListSku = addDProp("quantityDataListSku", "Кол-во (первичное)", NumericClass.get(14, 2), list, sku);
        quantityListSku = quantityDataListSku; //addJProp(baseGroup, "quantityListSku", true, "Кол-во", baseLM.and1, quantityDataListSku, 1, 2, numberListSku, 1, 2);
        quantityDataList = addSGProp(baseGroup, "quantityDataList", "Кол-во", quantityDataListSku, 1);

        quantitySimpleInvoiceArticle = addDProp(baseGroup, "quantitySimpleInvoiceArticle", "Кол-во", NumericClass.get(14, 4), simpleInvoice, articleComposite);
        quantitySimpleInvoice = addSGProp(baseGroup, "quantitySimpleInvoice", true, true, "Кол-во в документе", quantityListSku, documentList, 1, 2);

        quantityDocumentSku = addSGProp(baseGroup, "quantityDocumentSku", true, true, "Кол-во в документе", quantityListSku, documentList, 1, 2);
        quantityInvoicedDocumentArticle = addSGProp(baseGroup, "quantityInvoicedDocumentArticle", "Кол-во артикула в документе", quantityDocumentSku, 1, articleSku, 2);
        quantityDocumentArticle = addSUProp(baseGroup, "quantityDocumentArticle", "Кол-во артикула в документе", Union.SUM, quantityInvoicedDocumentArticle, quantitySimpleInvoiceArticle);
        quantityDocument = addSGProp(baseGroup, "quantityDocument", "Общее кол-во в документе", quantityDocumentArticle, 1);

        quantitySizeSupplierGenderCategory = addSGProp(baseGroup, "quantitySizeSupplierGenderCategory", true, "Кол-во", addCProp(IntegerClass.instance, 1, sku), sizeSupplierItem, 1, genderArticleSku, 1, categoryArticleSku, 1);

        // связь инвойсов и заказов
        inOrderInvoice = addDProp(baseGroup, "inOrderInvoice", "Вкл", LogicalClass.instance, order, invoice);

        addConstraint(addJProp("Магазин инвойса должен совпадать с магазином заказа", baseLM.and1,
                addJProp(baseLM.diff2, destinationDestinationDocument, 1, destinationDestinationDocument, 2), 1, 2,
                inOrderInvoice, 1, 2), true);

        orderedOrderInvoiceSku = addJProp(baseLM.and1, quantityDocumentSku, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceSku = addSGProp(baseGroup, "orderedInvoiceSku", "Кол-во заказано", orderedOrderInvoiceSku, 2, 3);

        orderedSimpleInvoiceSku = addJProp(baseGroup, "orderedSimpleInvoiceSku", "Кол-во заказано", baseLM.and1, orderedInvoiceSku, 1, 2, is(simpleInvoice), 1);
        // здесь на самом деле есть ограничение, что supplierBox ссылается именно на invoice
        orderedSupplierBoxSku = addJProp("orderedSupplierBoxSku", "Кол-во заказано", orderedInvoiceSku, boxInvoiceSupplierBox, 1, 2);

        // todo : переделать на PGProp, здесь надо derive'ить, иначе могут быть проблемы с расписыванием
        // если включаешь, то начинает тормозить изменение количества в заказах
        quantityOrderInvoiceSku = addPGProp(baseGroup, "quantityOrderInvoiceSku", true, 0, true, "Кол-во по заказу/инвойсу (расч.)",
                orderedOrderInvoiceSku,
                quantityDocumentSku, 2, 3);

        invoicedOrderSku = addSGProp(baseGroup, "invoicedOrderSku", "Выставлено инвойсов", quantityOrderInvoiceSku, 1, 3);

        // todo : не работает на инвойсе/простом товаре
        quantityListArticle = addDGProp(baseGroup, "quantityListArticle", "Кол-во (итог)",
                1, false, // кол-во объектов для порядка и ascending/descending
                quantityListSku, 1, articleSku, 2,
                addCUProp(addCProp(NumericClass.get(14, 2), 9999999.0, list, articleSingle),
                        addCProp(NumericClass.get(14, 2), 9999999.0, order, item),
                        addJProp(baseLM.and1, orderedSimpleInvoiceSku, 1, 2, is(item), 2), // если не артикул (простой), то пропорционально заказано
                        addJProp(baseLM.and1, orderedSupplierBoxSku, 1, 2, is(item), 2)), 1, 2, // ограничение (максимально-возможное число)
                2);

        quantityListArticleCompositeColor = addSGProp(baseGroup, "quantityListArticleCompositeColor", "Кол-во", quantityListSku, 1, articleCompositeItem, 2, colorSupplierItem, 2);
        quantityListArticleCompositeSize = addSGProp(baseGroup, "quantityListArticleCompositeSize", "Кол-во", quantityListSku, 1, articleCompositeItem, 2, sizeSupplierItem, 2);

        quantityListArticleCompositeColorSize = addDGProp(baseGroup, "quantityListArticleCompositeColorSize", "Кол-во",
                1, false,
                quantityListSku, 1, articleCompositeItem, 2, colorSupplierItem, 2, sizeSupplierItem, 2,
                addCProp(NumericClass.get(14, 2), 9999999.0, list, sku), 1, 2,
                2);
        quantityListArticleCompositeColorSize.property.setFixedCharWidth(3);
        quantityListArticleCompositeColorSize.setEditAction(ServerResponse.CHANGE, new LAP(new ChangeQuantityListArticleCompositeColorSize()));

        itemArticleCompositeColorSize = addAGProp("itemArticleCompositeColorSize", "Item", true, articleCompositeItem, colorSupplierItem, sizeSupplierItem);

        orderedOrderInvoiceArticle = addJProp(baseLM.and1, quantityListArticle, 1, 3, inOrderInvoice, 1, 2);

        orderedInvoiceArticle = addSGProp(baseGroup, "orderedInvoiceArticle", "Кол-во заказано", orderedOrderInvoiceArticle, 2, 3);
        // todo : сделать, чтобы работало автоматическое проставление
//        quantityListArticle.setDerivedForcedChange(orderedInvoiceArticle, 1, 2, numberListArticle, 1, 2);

        invoicedOrderArticle = addSGProp(baseGroup, "invoicedOrderArticle", "Выставлено инвойсов", invoicedOrderSku, 1, articleSku, 2);

        itemSupplierArticleSIDColorSIDSizeSID = addJProp("itemSupplierArticleSIDColorSIDSizeSID", "item", itemArticleCompositeColorSize, articleSIDSupplier, 2, 1, colorSIDSupplier, 3, 1, sizeSIDSupplier, 4, 1);

        // сроки заказа
        dateFromOrder = addDProp(baseGroup, "dateFromOrder", "Дата с", DateClass.instance, order);
        dateToOrder = addDProp(baseGroup, "dateToOrder", "Дата по", DateClass.instance, order);

        dateFromDataOrderArticle = addDProp(baseGroup, "dateFromDataOrderArticle", "Дата с", DateClass.instance, order, article);
        dateToDataOrderArticle = addDProp(baseGroup, "dateToDataOrderArticle", "Дата по", DateClass.instance, order, article);

        dateFromOrderOrderArticle = addJProp(baseGroup, "dateFromOrderOrderArticle", "Дата с", baseLM.and1, dateFromOrder, 1, is(article), 2);
        dateToOrderOrderArticle = addJProp(baseGroup, "dateToOrderOrderArticle", "Дата по", baseLM.and1, dateToOrder, 1, is(article), 2);

        dateFromOrderArticle = addSUProp(baseGroup, "dateFromOrderArticle", "Дата с", Union.OVERRIDE, dateFromOrderOrderArticle, dateFromDataOrderArticle);
        dateToOrderArticle = addSUProp(baseGroup, "dateToOrderArticle", "Дата по", Union.OVERRIDE, dateToOrderOrderArticle, dateToDataOrderArticle);

        dateFromOrderArticleSku = addJProp(baseGroup, "dateFromOrderArticleSku", "Дата с", dateFromOrderArticle, 1, articleSku, 2);
        dateToOrderArticleSku = addJProp(baseGroup, "dateToOrderArticleSku", "Дата по", dateToOrderArticle, 1, articleSku, 2);

        // цены
        priceDocumentArticle = addDProp(baseGroup, "priceDocumentArticle", "Цена", NumericClass.get(14, 4), priceDocument, article);
        //priceRateDocumentArticle = addJProp(baseGroup, "priceRateDocumentArticle", true, "Цена (конверт.)", round2, addJProp(baseLM.multiply, priceDocumentArticle, 1, 2, addJProp(nearestRateExchange, typeExchangeSTX, currencyDocument, 1, 1), 1), 1, 2);

        priceDataDocumentItem = addDProp(baseGroup, "priceDataDocumentItem", "Цена по товару", NumericClass.get(14, 4), priceDocument, item);
        priceArticleDocumentSku = addJProp(baseGroup, "priceArticleDocumentItem", "Цена по артикулу", priceDocumentArticle, 1, articleSku, 2);
        priceDocumentSku = addSUProp(baseGroup, "priceDocumentSku", true, "Цена", Union.OVERRIDE, priceArticleDocumentSku, priceDataDocumentItem);

        //priceRateDocumentSku = addJProp(baseGroup, "priceRateDocumentSku", true, "Цена (конверт.)", round2, addJProp(baseLM.multiply, priceDocumentSku, 1, 2, addJProp(baseLM.nearestRateExchange, typeExchangeSTX, currencyDocument, 1, 1), 1), 1, 2);
        priceRateOriginDocumentSku = addJProp(baseGroup, "priceRateOriginDocumentSku", true, "Цена (конверт.)", round2, addJProp(baseLM.multiply, priceDocumentSku, 1, 2, addJProp(BL.getModule("Currency").getLCPByName("rateTypeExchangeCurrencyDate"), typeExchangeSTX, currencyDocument, 1, date, 1), 1), 1, 2);
        priceRateDocumentSku = addDProp("priceRateDocumentSku", "Цена (конверт.)", NumericClass.get(14, 4), priceDocument, sku);

        RRPDocumentArticle = addDProp(baseGroup, "RRPDocumentArticle", "Рекомендованная цена", NumericClass.get(14, 4), priceDocument, article);
        RRPDocumentSku = addJProp(baseGroup, "RRPDocumentSku", "Рекомендованная цена", RRPDocumentArticle, 1, articleSku, 2);
        currencyRRPDocumentSku = addJProp(idGroup, "currencyRRPDocumentSku", baseLM.and1, currencyRRPDocument, 1, quantityDocumentSku, 1, 2);

        RRPRateDocumentArticle = addJProp(baseGroup, "RRPRateDocumentArticle", true, "Рекомендованная цена (конверт.)", round2, addJProp(baseLM.multiply, RRPDocumentArticle, 1, 2, addJProp(BL.getModule("Currency").getLCPByName("rateTypeExchangeCurrencyDate"), typeExchangeSTX, currencyDocument, 1, date, 1), 1), 1, 2);
        RRPRateDocumentSku = addJProp(baseGroup, "RRPRateDocumentSku", "Рекомендованная цена (конверт.)", RRPRateDocumentArticle, 1, articleSku, 2);

        priceSupplierBoxSku = addJProp(baseGroup, "priceSupplierBoxSku", "Цена", priceDocumentSku, boxInvoiceSupplierBox, 1, 2);

        priceOrderInvoiceArticle = addJProp(baseLM.and1, priceDocumentArticle, 1, 3, inOrderInvoice, 1, 2);
        priceOrderedInvoiceArticle = addMGProp(baseGroup, "priceOrderedInvoiceArticle", "Цена в заказе", priceOrderInvoiceArticle, 2, 3);
        // todo : не работает
        priceDocumentArticle.setEventChangePrevSet(priceOrderedInvoiceArticle, 1, 2, numberDocumentArticle, 1, 2);

        sumSupplierBoxSku = addJProp(baseGroup, "sumSupplierBoxSku", "Сумма", baseLM.multiply, quantityListSku, 1, 2, priceSupplierBoxSku, 1, 2);
        sumDocumentSku = addJProp(baseGroup, "sumDocumentSku", "Сумма", baseLM.multiply, quantityDocumentSku, 1, 2, priceDocumentSku, 1, 2);

        netWeightDocumentArticle = addJProp(baseGroup, "netWeightDocumentArticle", "Общий вес по артикулу", baseLM.multiply, quantityDocumentArticle, 1, 2, netWeightArticle, 2);
        netWeightDocumentSku = addJProp(baseGroup, "netWeightDocumentSku", "Общий вес по sku", baseLM.multiply, quantityDocumentSku, 1, 2, netWeightSku, 2);
        netWeightDocument = addSGProp(baseGroup, "netWeightDocument", "Общий вес", netWeightDocumentSku, 1);

        sumDocumentArticle = addSGProp(baseGroup, "sumDocumentArticle", "Сумма", sumDocumentSku, 1, articleSku, 2);
        sumSimpleInvoiceArticle = addJProp(baseGroup, "sumSimpleInvoiceArticle", "Сумма по артикулу", baseLM.multiply, priceDocumentArticle, 1, 2, quantitySimpleInvoiceArticle, 1, 2);

        sumSimpleInvoice = addSGProp(baseGroup, "sumSimpleInvoice", "Сумма", sumSimpleInvoiceArticle, 1);
        sumInvoicedDocument = addSGProp(baseGroup, "sumInvoicedDocument", "Сумма", sumDocumentSku, 1);

        sumDocument = addSUProp(baseGroup, "sumDocument", "Сумма документа", Union.SUM, sumInvoicedDocument, sumSimpleInvoice);

        // route
        percentShipmentRoute = addDProp(baseGroup, "percentShipmentRoute", "Процент", NumericClass.get(14, 2), shipment, route);

        percentShipmentRouteSku = addJProp(baseGroup, "percentShipmentRouteSku", "Процент", baseLM.and1, percentShipmentRoute, 1, 2, is(sku), 3);

        // creation
        quantityCreationSku = addDProp(baseGroup, "quantityCreationSku", "Количество", IntegerClass.instance, creationSku);

        creationSkuSku = addDProp(idGroup, "creationSkuSku", "Операция (ИД)", creationSku, sku);

        quantityCreationFreightBox = addDProp(baseGroup, "quantityCreationFreightBox", "Количество", IntegerClass.instance, creationFreightBox);
        routeCreationFreightBox = addDProp(idGroup, "routeCreationFreightBox", "Маршрут (ИД)", route, creationFreightBox);
        nameRouteCreationFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBox", "Маршрут", name, routeCreationFreightBox, 1);

        quantityCreationPallet = addDProp(baseGroup, "quantityCreationPallet", "Количество", IntegerClass.instance, creationPallet);
        routeCreationPallet = addDProp(idGroup, "routeCreationPallet", "Маршрут (ИД)", route, creationPallet);
        nameRouteCreationPallet = addJProp(baseGroup, "nameRouteCreationPallet", "Маршрут", name, routeCreationPallet, 1);

        // паллеты
        creationPalletPallet = addDProp(idGroup, "creationPalletPallet", "Операция (ИД)", creationPallet, pallet);
        routeCreationPalletPallet = addJProp(idGroup, "routeCreationPalletPallet", true, "Маршрут (ИД)", routeCreationPallet, creationPalletPallet, 1);
        nameRouteCreationPalletPallet = addJProp(baseGroup, "nameRouteCreationPalletPallet", "Маршрут", name, routeCreationPalletPallet, 1);

        freightPallet = addDProp(baseGroup, "freightPallet", "Фрахт (ИД)", freight, pallet);
        equalsPalletFreight = addJProp(baseGroup, "equalsPalletFreight", "Вкл.", baseLM.equals2, freightPallet, 1, 2);

        // инвойсы напрямую во фрахты
        freightDirectInvoice = addDProp(baseGroup, "freightDirectInvoice", "Фрахт (ИД)", freight, directInvoice);
        equalsDirectInvoiceFreight = addJProp(baseGroup, "equalsDirectInvoiceFreight", "Вкл.", baseLM.equals2, freightDirectInvoice, 1, 2);

        grossWeightDirectInvoice = addDProp(baseGroup, "grossWeightDirectInvoice", "Вес брутто", NumericClass.get(14, 3), directInvoice);
        palletNumberDirectInvoice = addDProp(baseGroup, "palletNumberDirectInvoice", "Кол-во паллет", IntegerClass.instance, directInvoice);

        addConstraint(addJProp("Для инвойса должен быть задан вес брутто", baseLM.andNot1, freightDirectInvoice, 1, grossWeightDirectInvoice, 1), false);

        freightShippedDirectInvoice = addJProp(baseGroup, "freightShippedDirectInvoice", "Инвойс отгружен", is(freightShipped), freightDirectInvoice, 1);

        sumDirectInvoicedSku = addSGProp(baseGroup, "sumDirectInvoicedSku", true, "Сумма по инвойсам напрямую", addJProp(and(false, true), sumDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDirectInvoicedSku = addSGProp(baseGroup, "quantityDirectInvoicedSku", true, "Кол-во по инвойсам напрямую", addJProp(and(false, true), quantityDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 2);
        quantityDocumentBrandSupplier = addSGProp(baseGroup, "quantityDocumentBrandSupplier", true, "Кол-во по бренду в документе", addJProp(and(false, true), quantityDocumentSku, 1, 2, is(directInvoice), 1, freightShippedDirectInvoice, 1), 1, brandSupplierArticleSku, 2);
        quantityAllDocumentsBrandSupplier = addSGProp(baseGroup, "quantityAllDocumentsBrandSupplier", true, "Кол-во по бренду в документах", quantityDocumentBrandSupplier, 2);

        // freight box
        creationFreightBoxFreightBox = addDProp(idGroup, "creationFreightBoxFreightBox", "Операция (ИД)", creationFreightBox, freightBox);

        palletFreightBox = addDProp(idGroup, "palletFreightBox", "Паллета (ИД)", pallet, freightBox);
        equalsPalletFreightBox = addJProp(baseGroup, "equalsPalletFreightBox", "Вкл.", baseLM.equals2, palletFreightBox, 1, 2);
        barcodePalletFreightBox = addJProp(baseGroup, "barcodePalletFreightBox", "Паллета (штрих-код)", barcode, palletFreightBox, 1);

        routeCreationFreightBoxFreightBox = addJProp(idGroup, "routeCreationFreightBoxFreightBox", true, "Маршрут (ИД)", routeCreationFreightBox, creationFreightBoxFreightBox, 1);
        nameRouteCreationFreightBoxFreightBox = addJProp(baseGroup, "nameRouteCreationFreightBoxFreightBox", "Маршрут", name, routeCreationFreightBoxFreightBox, 1);

        freightFreightBox = addJProp(idGroup, "freightFreightBox", "Фрахт короба транспортировки", freightPallet, palletFreightBox, 1);

        destinationFreightBox = addDProp(idGroup, "destinationFreightBox", "Пункт назначения (ИД)", destination, freightBox);
        nameDestinationFreightBox = addJProp(baseGroup, "nameDestinationFreightBox", "Пункт назначения", name, destinationFreightBox, 1);

        destinationDataSupplierBox = addDProp(idGroup, "destinationDataSupplierBox", "Пункт назначения (ИД)", destination, supplierBox);
        sidDestinationDataSupplierBox = addJProp(baseGroup, "sidDestinationDataSupplierBox", "Пункт назначения (код)", sidDestination, destinationDataSupplierBox, 1);
        nameDestinationDataSupplierBox = addJProp(baseGroup, "nameDestinationDataSupplierBox", "Пункт назначения", name, destinationDataSupplierBox, 1);

        destinationSupplierBox = addSUProp(idGroup, "destinationSupplierBox", "Пункт назначения (ИД)", Union.OVERRIDE, destinationInvoiceSupplierBox, destinationDataSupplierBox);
        nameDestinationSupplierBox = addJProp(baseGroup, "nameDestinationSupplierBox", "Пункт назначения", name, destinationSupplierBox, 1);

        destinationFreightUnit = addCUProp(idGroup, "destinationFreightUnit", "Пункт назначения (ИД)", destinationSupplierBox, destinationFreightBox);
        nameDestinationFreightUnit = addJProp(baseGroup, "nameDestinationFreightUnit", "Пункт назначения", BL.Store.getLCPByName("nameStore"), destinationFreightUnit, 1);

        // поставка на склад
        inOrderShipment = addDProp(baseGroup, "inOrderShipment", "Вкл", LogicalClass.instance, order, shipment);

        inInvoiceShipment = addDProp(baseGroup, "inInvoiceShipment", "Вкл", LogicalClass.instance, invoice, shipment);

        inSupplierBoxShipment = addJProp(baseGroup, "inSupplierBoxShipment", "Вкл", inInvoiceShipment, boxInvoiceSupplierBox, 1, 2);

        invoicedShipmentSku = addSGProp(baseGroup, "invoicedShipmentSku", true, true, "Ожид. (пост.)",
                addJProp(baseLM.and1, quantityDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        invoicedBetweenDateSku = addSGProp(baseGroup, "invoicedBetweenDateSku", "Заявленное кол-во на период", addJProp(baseLM.and1, invoicedShipmentSku, 1, 2, addJProp(betweenDates, dateArrivalShipment, 1, object(DateClass.instance), 3, object(DateClass.instance), 4), 1, 2, 3, 4), 2, 3, 4);
        invoicedBetweenDateBrandSupplier = addSGProp(baseGroup, "invoicedBetweenDateBrandSupplier", "Заявленное кол-во на период", invoicedBetweenDateSku, brandSupplierArticleSku, 1, 2, 3);

        emptyBarcodeShipment = addSGProp(privateGroup, "emptyBarcodeShipment", true, true, "Кол-во позиций без штрих-кода",
                addJProp(and(false, true), addCProp(IntegerClass.instance, 1, shipment), 1, invoicedShipmentSku, 1, 2, barcode, 2),
                1);

        priceShipmentSku = addMGProp(baseGroup, "priceShipmentSku", true, "Цена (пост.)",
                addJProp(baseLM.and1, priceDocumentSku, 1, 2, inInvoiceShipment, 1, 3), 3, 2);

        invoicedShipment = addSGProp(baseGroup, "invoicedShipment", true, "Всего ожидается (пост.)", invoicedShipmentSku, 1);

        //sku shipment detail
        skuShipmentDetail = addDProp(idGroup, "skuShipmentDetail", "SKU (ИД)", sku, shipmentDetail);

        barcodeSkuShipmentDetail = addJProp(baseGroup, "barcodeSkuShipmentDetail", "Штрих-код SKU", barcode, skuShipmentDetail, 1);

        articleShipmentDetail = addJProp(idGroup, "articleShipmentDetail", "Артикул (ИД)", articleSku, skuShipmentDetail, 1);
        sidArticleShipmentDetail = addJProp(baseGroup, "sidArticleShipmentDetail", "Артикул", sidArticle, articleShipmentDetail, 1);
        
        colorSupplierItemShipmentDetail = addJProp(idGroup, "colorSupplierItemShipmentDetail", "Цвет поставщика (ИД)", colorSupplierItem, skuShipmentDetail, 1);
        sidColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidColorSupplierItemShipmentDetail", "Код цвета", sidColorSupplier, colorSupplierItemShipmentDetail, 1);
        nameColorSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "nameColorSupplierItemShipmentDetail", "Цвет поставщика", name, colorSupplierItemShipmentDetail, 1);

        sizeSupplierItemShipmentDetail = addJProp(idGroup, "sizeSupplierItemShipmentDetail", "Размер поставщика (ИД)", sizeSupplierItem, skuShipmentDetail, 1);
        sidSizeSupplierItemShipmentDetail = addJProp(itemAttributeGroup, "sidSizeSupplierItemShipmentDetail", "Размер поставщика", sidSizeSupplier, sizeSupplierItemShipmentDetail, 1);

        nameBrandSupplierArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "nameBrandSupplierArticleSkuShipmentDetail", "Бренд", nameBrandSupplierArticleSku, skuShipmentDetail, 1);
        originalNameArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, true, "originalNameArticleSkuShipmentDetail", "Наименование (ориг.)", originalNameArticleSku, skuShipmentDetail, 1);

        genderArticleSkuShipmentDetail = addJProp(idGroup, true, "genderArticleSkuShipmentDetail", "Пол товара (ИД)", genderArticleSku, skuShipmentDetail, 1);
        sidGenderArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "sidGenderArticleSkuShipmentDetail", "Пол товара", sidGender, genderArticleSkuShipmentDetail, 1);
        sidGenderArticleSkuShipmentDetail.property.preferredCharWidth = 10;
        sidGenderArticleSkuShipmentDetail.property.minimumCharWidth = 5;

        typeFabricArticleSkuShipmentDetail = addJProp(idGroup, true, "typeFabricArticleSkuShipmentDetail", "Тип одежды (ИД)", typeFabricArticleSku, skuShipmentDetail, 1);
        nameTypeFabricArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameTypeFabricArticleSkuShipmentDetail", "Тип одежды", name, typeFabricArticleSkuShipmentDetail, 1);
        nameTypeFabricArticleSkuShipmentDetail.property.preferredCharWidth = 10;
        nameTypeFabricArticleSkuShipmentDetail.property.minimumCharWidth = 5;

        categoryArticleSkuShipmentDetail = addJProp(idGroup, true, "categoryArticleSkuShipmentDetail", "Номенклатурная группа товара (ИД)", categoryArticleSku, skuShipmentDetail, 1);
        nameOriginCategoryArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameOriginCategoryArticleSkuShipmentDetail", "Номенклатурная группа товара", nameOrigin, categoryArticleSkuShipmentDetail, 1);
        nameCategoryArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameCategoryArticleSkuShipmentDetail", "Номенклатурная группа товара", name, categoryArticleSkuShipmentDetail, 1);

        coefficientArticleSkuShipmentDetail = addJProp(intraAttributeGroup, true, "coefficientArticleSkuShipmentDetail", "Кол-во в комплекте", coefficientArticleSku, skuShipmentDetail, 1);

        customCategoryOriginArticleSkuShipmentDetail = addJProp(idGroup, true, "customCategoryOriginArticleSkuShipmentDetail", "ТН ВЭД (ИД)", customCategoryOriginArticleSku, skuShipmentDetail, 1);
        sidCustomCategoryOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "sidCustomCategoryOriginArticleSkuShipmentDetail", "Код ТН ВЭД", sidCustomCategoryOrigin, customCategoryOriginArticleSkuShipmentDetail, 1);

        netWeightArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "netWeightArticleSkuShipmentDetail", "Вес нетто (ориг.)", netWeightArticleSku, skuShipmentDetail, 1);
        netWeightSkuShipmentDetail = addJProp(intraAttributeGroup, true, "netWeightSkuShipmentDetail", "Вес нетто (ед.)", netWeightSku, skuShipmentDetail, 1);

        countryOfOriginArticleSkuShipmentDetail = addJProp(idGroup, "countryOfOriginArticleSkuShipmentDetail", "Страна происхождения (ориг.) (ИД)", countryOfOriginArticleSku, skuShipmentDetail, 1);
        nameCountryOfOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "nameCountryOfOriginArticleSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginArticleSkuShipmentDetail, 1);

        countryOfOriginSkuShipmentDetail = addJProp(idGroup, true, "countryOfOriginSkuShipmentDetail", "Страна происхождения (ИД)", countryOfOriginSku, skuShipmentDetail, 1);
        nameCountryOfOriginSkuShipmentDetail = addJProp(intraAttributeGroup, "nameCountryOfOriginSkuShipmentDetail", "Страна происхождения", nameOriginCountry, countryOfOriginSkuShipmentDetail, 1);

        mainCompositionOriginArticleSkuShipmentDetail = addJProp(supplierAttributeGroup, "mainCompositionOriginArticleSkuShipmentDetail", "Состав", mainCompositionOriginArticleSku, skuShipmentDetail, 1);
        mainCompositionOriginSkuShipmentDetail = addJProp(intraAttributeGroup, true, "mainCompositionOriginSkuShipmentDetail", "Состав", mainCompositionOriginSku, skuShipmentDetail, 1);

        additionalCompositionOriginSkuShipmentDetail = addJProp(intraAttributeGroup, true, "additionalCompositionOriginSkuShipmentDetail", "Дополнительный состав", additionalCompositionOriginSku, skuShipmentDetail, 1);

        unitOfMeasureArticleSkuShipmentDetail = addJProp(idGroup, true, "unitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара (ИД)", unitOfMeasureArticleSku, skuShipmentDetail, 1);
        nameOriginUnitOfMeasureArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameOriginUnitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара", nameOrigin, unitOfMeasureArticleSkuShipmentDetail, 1);
        nameUnitOfMeasureArticleSkuShipmentDetail = addJProp(intraAttributeGroup, "nameUnitOfMeasureArticleSkuShipmentDetail", "Ед. изм. товара", name, unitOfMeasureArticleSkuShipmentDetail, 1);

        // stock shipment detail
        stockShipmentDetail = addDProp(idGroup, "stockShipmentDetail", "Место хранения (ИД)", stock, shipmentDetail);
        barcodeStockShipmentDetail = addJProp(baseGroup, "barcodeStockShipmentDetail", "Штрих-код короба для транспортировки", barcode, stockShipmentDetail, 1);

        routeFreightBoxShipmentDetail = addJProp(idGroup, "routeFreightBoxShipmentDetail", "Маршрут (ИД)", routeCreationFreightBoxFreightBox, stockShipmentDetail, 1);
        nameRouteFreightBoxShipmentDetail = addJProp(baseGroup, "nameRouteFreightBoxShipmentDetail", "Маршрут", name, routeFreightBoxShipmentDetail, 1);
        nameRouteFreightBoxShipmentDetail.setFixedCharWidth(3);

        boxShipmentBoxShipmentDetail = addDProp(idGroup, "boxShipmentBoxShipmentDetail", "Поставка (ИД)", boxShipment, boxShipmentDetail);
        simpleShipmentSimpleShipmentDetail = addDProp(idGroup, "simpleShipmentSimpleShipmentDetail", "Поставка (ИД)", simpleShipment, simpleShipmentDetail);
        shipmentShipmentDetail = addCUProp(idGroup, "shipmentShipmentDetail", "Поставка (ИД)", boxShipmentBoxShipmentDetail, simpleShipmentSimpleShipmentDetail);
        sidShipmentShipmentDetail = addJProp(baseGroup, "sidShipmentShipmentDetail", "Поставка", sidDocument, shipmentShipmentDetail, 1);

        addConstraint(addJProp("Поставщик товара должен совпадать с поставщиком строки", baseLM.diff2,
            addJProp(supplierArticleSku, skuShipmentDetail, 1), 1, addJProp(supplierDocument, shipmentShipmentDetail, 1), 1), true);

        // supplier box shipmentDetail
        supplierBoxShipmentDetail = addDProp(idGroup, "supplierBoxShipmentDetail", "Короб поставщика (ИД)", supplierBox, boxShipmentDetail);
        setNotNull(supplierBoxShipmentDetail);
        sidSupplierBoxShipmentDetail = addJProp(baseGroup, "sidSupplierBoxShipmentDetail", "Номер короба поставщика", sidSupplierBox, supplierBoxShipmentDetail, 1);
        barcodeSupplierBoxShipmentDetail = addJProp(baseGroup, "barcodeSupplierBoxShipmentDetail", "Штрих-код короба поставщика", barcode, supplierBoxShipmentDetail, 1);

        quantityShipmentDetail = addDProp(baseGroup, "quantityShipmentDetail", "Кол-во", NumericClass.get(14, 2), shipmentDetail);

        userShipmentDetail = addDCProp(idGroup, "userShipmentDetail", "Пользователь (ИД)", BL.authenticationLM.currentUser, true, is(shipmentDetail), 1);
        nameUserShipmentDetail = addJProp(baseGroup, "nameUserShipmentDetail", "Пользователь", BL.contactLM.nameContact, userShipmentDetail, 1);

        timeShipmentDetail = addTCProp(Time.DATETIME, "timeShipmentDetail", true, "Время ввода", quantityShipmentDetail);

        addBoxShipmentDetailBoxShipmentSupplierBoxStockSku = addJoinAProp("Добавить строку поставки",
                addAAProp(boxShipmentDetail, boxShipmentBoxShipmentDetail, supplierBoxShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, 3, 4, addCProp(NumericClass.get(14, 2), 1.0));

        addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode = addJoinAProp("Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockSku,
                1, 2, 3, skuBarcodeObject, 4);

        addSimpleShipmentDetailSimpleShipmentStockSku = addJoinAProp("Добавить строку поставки",
                addAAProp(simpleShipmentDetail, simpleShipmentSimpleShipmentDetail, stockShipmentDetail, skuShipmentDetail, quantityShipmentDetail),
                1, 2, 3, addCProp(NumericClass.get(14, 2), 1.0));

        addSimpleShipmentSimpleShipmentDetailStockBarcode = addJoinAProp("Добавить строку поставки",
                addSimpleShipmentDetailSimpleShipmentStockSku,
                1, 2, skuBarcodeObject, 3);

        quantityArticle = addSGProp(baseGroup, "quantityArticle", true, "Оприходовано", quantityShipmentDetail, articleShipmentDetail, 1);
        quantityShipSku = addSGProp(baseGroup, "quantityShipSku", true, "Оприходовано", quantityShipmentDetail, skuShipmentDetail, 1);

        addConstraint(addJProp("Для артикула должна быть задана номенклатурная группа", baseLM.andNot1, quantityArticle, 1, categoryArticle, 1), false);
        addConstraint(addJProp("Для артикула должен быть задан пол", baseLM.andNot1, quantityArticle, 1, genderArticle, 1), false);
        addConstraint(addJProp("Для артикула должен быть задан тип одежды", baseLM.andNot1, quantityArticle, 1, typeFabricArticle, 1), false);

        addConstraint(addJProp("Для товара должен быть задан вес", baseLM.andNot1, quantityShipSku, 1, netWeightSku, 1), false);
        addConstraint(addJProp("Для товара должен быть задан страна", baseLM.andNot1, quantityShipSku, 1, countryOfOriginSku, 1), false);
        addConstraint(addJProp("Для товара должен быть задан состав", baseLM.andNot1, quantityShipSku, 1, mainCompositionOriginSku, 1), false);

        quantitySupplierBoxBoxShipmentStockSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentStockSku", true, "Кол-во оприход.", quantityShipmentDetail,
                supplierBoxShipmentDetail, 1, boxShipmentBoxShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantitySupplierBoxBoxShipmentSku = addSGProp(baseGroup, "quantitySupplierBoxBoxShipmentSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku,
                1, 2, 4);

        quantitySupplierBoxSku = addSGProp(baseGroup, "quantitySupplierBoxSku", "Кол-во оприход.", quantitySupplierBoxBoxShipmentStockSku, 1, 4);

        quantityDirectSupplierBoxSku = addJProp(baseGroup, "quantityDirectSupplierBoxSku", "Кол-во", baseLM.and1, quantityListSku, 1, 2, addJProp(is(directInvoice), boxInvoiceSupplierBox, 1), 1);
        quantityDirectSimpleInvoiceSku = addJProp("quantityDirectSimpleInvoiceSku", "Кол-во", baseLM.and1, quantityDocumentSku, 1, 2, is(directSimpleInvoice), 1);

        quantitySupplierBox = addSGProp(baseGroup, "quantitySupplierBox", "Кол-во оприход.(короб)", quantitySupplierBoxSku, 1);

        diffListSupplierBoxSku = addJProp(baseLM.equals2, quantityDataListSku, 1, 2, quantitySupplierBoxSku, 1, 2);

        diffListSupplierBox = addJProp(baseLM.less2, quantityDataList, 1, quantitySupplierBox, 1);

        quantitySimpleShipmentStockSku = addSGProp(baseGroup, "quantitySimpleShipmentStockSku", true, "Кол-во оприход.", quantityShipmentDetail,
                simpleShipmentSimpleShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        quantityShipDimensionShipmentStockSku = addCUProp(baseGroup, "quantityShipDimensionShipmentStockSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                addJProp(baseLM.and1, quantitySimpleShipmentStockSku, 2, 3, 4, baseLM.equals2, 1, 2));

        quantityBoxInvoiceBoxShipmentStockSku = addSGProp(baseGroup, "quantityBoxInvoiceBoxShipmentStockSku", true, "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku,
                boxInvoiceSupplierBox, 1, 2, 3, 4);

        invoicedSimpleInvoiceSimpleShipmentStockSku = addJProp("invoicedSimpleInvoiceSimpleShipmentStockSku", "Кол-во оприх.", and(false, false, false, false), quantityDocumentSku, 1, 4, inInvoiceShipment, 1, 2, is(simpleInvoice), 1, is(simpleShipment), 2, is(stock), 3);
        invoicedSimpleInvoiceSimpleShipmentStockArticleComposite = addJProp("invoicedSimpleInvoiceSimpleShipmentStockArticleComposite", "Кол-во оприх.", and(false, false, false), quantitySimpleInvoiceArticle, 1, 4, inInvoiceShipment, 1, 2, is(simpleShipment), 2, is(stock), 3);
        invoicedSimpleInvoiceSimpleShipmentStockItem = addJProp("invoicedSimpleInvoiceSimpleShipmentStockItem", "Кол-во оприх.", invoicedSimpleInvoiceSimpleShipmentStockArticleComposite, 1, 2, 3, articleCompositeItem, 4);

        quantitySkuSimpleInvoiceSimpleShipmentStockSku = addPGProp(baseGroup, "quantitySkuSimpleInvoiceSimpleShipmentStockSku", true, 0, true, "Кол-во оприход.",
                invoicedSimpleInvoiceSimpleShipmentStockSku,
                quantitySimpleShipmentStockSku, 2, 3, 4);

        quantitySimpleShipmentStockItem = addJProp("quantitySimpleShipmentStockItem", baseLM.and1, quantitySimpleShipmentStockSku, 1, 2, 3, is(item), 3);

        quantitySimpleInvoiceSimpleShipmentStockItem = addPGProp(baseGroup, "quantitySimpleInvoiceSimpleShipmentStockItem", true, 0, true, "Кол-во оприход.",
                invoicedSimpleInvoiceSimpleShipmentStockItem,
                quantitySimpleShipmentStockItem, 2, 3, 4);

        quantitySimpleInvoiceSimpleShipmentStockSku = addSUProp(baseGroup, "quantitySimpleInvoiceSimpleShipmentStockSku", "Кол-во оприход.", Union.SUM, quantitySimpleInvoiceSimpleShipmentStockItem, quantitySkuSimpleInvoiceSimpleShipmentStockSku);

        quantityInvoiceShipmentStockSku = addCUProp(baseGroup, "quantityInvoiceShipmentStockSku", "Кол-во оприход.",
                quantityBoxInvoiceBoxShipmentStockSku, quantitySimpleInvoiceSimpleShipmentStockSku);

        quantityInvoiceStockSku = addSGProp(baseGroup, "quantityInvoiceStockSku", true, "Кол-во оприход.", quantityInvoiceShipmentStockSku, 1, 3, 4);

        quantityInvoiceSku = addSGProp(baseGroup, "quantityInvoiceSku", true, true, "Кол-во оприход.", quantityInvoiceStockSku, 1, 3);

        quantityInvoice = addSGProp(baseGroup, "quantityInvoice", true, true, "Кол-во оприход.", quantityInvoiceSku, 1);

        diffDocumentInvoiceSku = addJProp(baseLM.equals2, quantityDocumentSku, 1, 2, quantityInvoiceSku, 1, 2);

        priceInInvoiceStockSku = addJProp(baseGroup, "priceInInvoiceStockSku", false, "Цена входная", baseLM.and1,
                priceRateDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);
        RRPInInvoiceStockSku = addJProp(baseGroup, "RRPInInvoiceStockSku", false, "Цена рекомендованная", baseLM.and1,
                RRPDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);
        currencyRRPInInvoiceStockSku = addJProp(baseGroup, "currencyRRPInInvoiceStockSku", false, "Цена рекомендованная", baseLM.and1,
                currencyRRPDocumentSku, 1, 3, quantityInvoiceStockSku, 1, 2, 3);

        //contractInInvoiceStockSku = addJProp(baseGroup, "contractInInvoiceStockSku", false, "Контракт (ИД)", baseLM.and1,
        //        contractInvoice, 1, quantityInvoiceStockSku, 1, 2, 3);

        priceInInvoiceShipmentStockSku = addJProp("priceInInvoiceShipmentStockSku", "Цена входная", baseLM.and1, priceInInvoiceStockSku, 1, 3, 4, inInvoiceShipment, 1, 2);
        RRPInInvoiceShipmentStockSku = addJProp("RRPInInvoiceShipmentStockSku", "Цена рекомендованная", baseLM.and1, RRPInInvoiceStockSku, 1, 3, 4, inInvoiceShipment, 1, 2);
        currencyRRPInInvoiceShipmentStockSku = addJProp("currencyRRPInInvoiceShipmentStockSku", "Валюта (ИД)", baseLM.and1, currencyRRPInInvoiceStockSku, 1, 3, 4, inInvoiceShipment, 1, 2);
        //contractInInvoiceShipmentStockSku = addJProp("contractInInvoiceShipmentStockSku", "Контракт (ИД)", baseLM.and1, contractInInvoiceStockSku, 1, 3, 4, inInvoiceShipment, 1, 2);

        priceInShipmentStockSku = addMGProp(baseGroup, "priceShipmentStockSku", true, "Цена входная", priceInInvoiceShipmentStockSku, 2, 3, 4);
        RRPInShipmentStockSku = addMGProp(baseGroup, "RRPShipmentStockSku", true, "Цена рекомендованная", RRPInInvoiceShipmentStockSku, 2, 3, 4);
        currencyRRPInShipmentStockSku = addMGProp(baseGroup, "currencyRRPShipmentStockSku", true, "Валюта (ИД)", currencyRRPInInvoiceShipmentStockSku, 2, 3, 4);

        priceInShipmentDetail = addJProp(baseGroup, "priceInShipmentDetail", "Цена входная", priceInShipmentStockSku, shipmentShipmentDetail, 1, stockShipmentDetail, 1, skuShipmentDetail, 1);

        //contractInShipmentStockSku = addMGProp(baseGroup, "contractInShipmentStockSku", true, "Контракт (ИД)", contractInInvoiceShipmentStockSku, 2, 3, 4);

        quantityShipDimensionStock = addSGProp(baseGroup, "quantityShipDimensionStock", "Всего оприход.", quantityShipDimensionShipmentStockSku, 1, 3);

        quantityShipDimensionShipmentSku = addSGProp(baseGroup, "quantityShipDimensionShipmentSku", "Оприход. (короб)", quantityShipDimensionShipmentStockSku, 1, 2, 4);

        zeroQuantityListSku = addSUProp(baseGroup, "zeroQuantityListSku", "кол-во", Union.OVERRIDE, addCProp(NumericClass.get(14, 2), 0.0, list, sku), quantityListSku);
        zeroQuantityShipDimensionShipmentSku = addSUProp(baseGroup, "zeroQuantityShipDimensionShipmentSku", "кол-во", Union.OVERRIDE, addCProp(NumericClass.get(14, 2), 0.0, shipDimension, shipment, sku), quantityShipDimensionShipmentSku);

        diffListShipSku = addJProp(baseLM.diff2, zeroQuantityListSku, 1, 3, zeroQuantityShipDimensionShipmentSku, 1, 2, 3);

        quantityShipmentStockSku = addSGProp(baseGroup, "quantityShipmentStockSku", true, "Кол-во оприход.", quantityShipDimensionShipmentStockSku, 2, 3, 4);

        quantityShipmentStock = addSGProp(baseGroup, "quantityShipmentStock", "Всего оприход.", quantityShipmentStockSku, 1, 2);

        quantityShipmentSku = addSGProp(baseGroup, "quantityShipmentSku", "Оприход. (пост.)", quantityShipmentStockSku, 1, 3);

        quantityShipmentedBetweenDateSku = addSGProp(baseGroup, "quantityShipmentedBetweenDateSku", "Фактический приход за период", addJProp(baseLM.and1, quantityShipmentSku, 1, 2, addJProp(betweenDates, dateArrivalShipment, 1, object(DateClass.instance), 3, object(DateClass.instance), 4), 1, 2, 3, 4), 2, 3, 4);
        quantityShipmentedBetweenDateBrandSupplier = addSGProp(baseGroup, "quantityShipmentedBetweenDateBrandSupplier", "Фактический приход за период", quantityShipmentedBetweenDateSku, brandSupplierArticleSku, 1, 2, 3);

        quantityShipmentedSku = addSGProp(baseGroup, "quantityShipmentedSku", "Фактический приход", quantityShipmentSku, 2);

        // выполнение заказа
        orderedOrderShipmentSku = addJProp(baseLM.and1, quantityDocumentSku, 1, 3, inOrderShipment, 1, 2);

        quantityOrderShipmentSku = addPGProp(baseGroup, "quantityOrderShipmentSku", true, 0, true, "Кол-во по заказу/поставке (расч.)",
                orderedOrderShipmentSku,
                quantityShipmentSku, 2, 3);

        shipmentedOrderSku = addSGProp(baseGroup, "shipmentedOrderSku", "Прислано по заказу", quantityOrderShipmentSku, 1, 3);

        shipmentedAtTermOrderSku = addSGProp(baseGroup, "shipmentedAtTermOrderSku", "Прислано в срок", addJProp(baseLM.and1, quantityOrderShipmentSku, 1, 2, 3, addJProp(betweenDates, dateArrivalShipment, 2, dateFromOrderArticleSku, 1, 3, dateToOrderArticleSku, 1, 3), 1, 2, 3), 1, 3);

        quantityShipment = addSGProp(baseGroup, "quantityShipment", true, "Оприходовано", quantityShipmentSku, 1);

        zeroQuantityShipmentSku = addSUProp(baseGroup, "zeroQuantityShipmentSku", "кол-во", Union.OVERRIDE, addCProp(NumericClass.get(14, 2), 0.0, shipment, sku), quantityShipmentSku);
        zeroInvoicedShipmentSku = addSUProp(baseGroup, "zeroInvoicedShipmentSku", "кол-во", Union.OVERRIDE, addCProp(NumericClass.get(14, 2), 0.0, shipment, sku), invoicedShipmentSku);
        diffShipmentSku = addJProp(baseLM.diff2, zeroQuantityShipmentSku, 1, 2, zeroInvoicedShipmentSku, 1, 2);

        quantityStockSku = addSGProp(baseGroup, "quantityStockSku", true, true, "Оприход. в короб для транспортировки", quantityShipmentStockSku, 2, 3);

        quantityDirectListSku = addCUProp("quantityDirectListSku", "Кол-во в коробе", quantityDirectSupplierBoxSku, quantityDirectSimpleInvoiceSku);
        quantityFreightUnitSku = addCUProp(baseGroup, "quantityFreightUnitSku", "Кол-во в коробе", quantityDirectListSku, addJProp(baseLM.and1, quantityStockSku, 1, 2, is(freightBox), 1));

        quantityFreightUnitArticle = addSGProp(baseGroup, "quantityFreightUnitArticle", true, true, "Кол-во в коробе", quantityFreightUnitSku, 1, articleSku, 2);

        quantityImporterDirectSupplierBoxSku = addJProp("quantityImporterDirectSupplierBoxSku", "Кол-во в коробе", baseLM.and1, quantityDirectListSku, 2, 3, is(importer), 1);

        //quantityImporterFreightUnitSku = addCUProp(baseGroup, "quantityImporterFreightUnitSku", "Кол-во", quantityImporterStockSku, quantityImporterDirectSupplierBoxSku);

        quantityStock = addSGProp(baseGroup, "quantityStock", "Кол-во оприход.", quantityStockSku, 1);

        quantityStockArticle = addSGProp(baseGroup, "quantityStockArticle", true, "Кол-во по артикулу", quantityStockSku, 1, articleSku, 2);

        quantityFreightDestination = addSGProp(baseGroup, "quantityFreightDestination", "Кол-во в магазин", quantityStock, freightFreightBox, 1, destinationFreightBox, 1);

        freightShippedFreightBox = addJProp(baseGroup, "freightShippedFreightBox", "Отгружен", is(freightShipped), freightFreightBox, 1);

        sumInInvoiceStockSku = addJProp(baseGroup, "sumInInvoiceStockSku", "Сумма в коробе", baseLM.multiply, addJProp(baseLM.andNot1, quantityInvoiceStockSku, 1, 2, 3, freightShippedFreightBox, 2), 1, 2, 3, priceInInvoiceStockSku, 1, 2, 3);

        sumStockedSku = addSGProp(baseGroup, "sumStockedSku", true, "Сумма на приемке", sumInInvoiceStockSku, 3);
        quantityAllSku = addSGProp(baseGroup, "quantityAllSku", true, "Кол-во на приемке", quantityStockSku, 2);
        quantityStockedSku = addSGProp(baseGroup, "quantityStockedSku", true, "Кол-во на приемке", addJProp(baseLM.andNot1, quantityStockSku, 1, 2, freightShippedFreightBox, 1), 2);

        quantitySku = addSUProp(baseGroup, "quantitySku", "Кол-во", Union.SUM, quantityStockedSku, quantityDirectInvoicedSku);
        sumSku = addSUProp(baseGroup, "sumSku", "Сумма", Union.SUM, sumStockedSku, sumDirectInvoicedSku);

        quantityStockBrandSupplier = addSGProp(baseGroup, "quantityStockBrandSupplier", true, "Кол-во по бренду",
                addJProp(baseLM.andNot1, quantityStockArticle, 1, 2, freightShippedFreightBox, 1), 1, brandSupplierArticle, 2);

        quantityFreightUnitBrandSupplier = addSGProp(baseGroup, "quantityFreightUnitBrandSupplier", true, true, "Кол-во по бренду", quantityFreightUnitSku, 1, brandSupplierArticleSku, 2);

        quantityPalletSku = addSGProp(baseGroup, "quantityPalletSku", true, "Оприход. (пал.)", quantityStockSku, palletFreightBox, 1, 2);

        quantityPalletBrandSupplier = addSGProp(baseGroup, "quantityPalletBrandSupplier", true, "Кол-во по бренду", quantityStockBrandSupplier, palletFreightBox, 1, 2);
        quantityAllPalletsBrandSupplier = addSGProp(baseGroup, "quantityAllPalletBrandSupplier", true, "Кол-во по бренду", quantityPalletBrandSupplier, 2);

        quantityBrandSupplier = addSUProp(baseGroup, "quantityBrandSupplier", "Кол-во по бренду", Union.SUM, quantityAllDocumentsBrandSupplier, quantityAllPalletsBrandSupplier);

        quantityShipmentPallet = addSGProp(baseGroup, "quantityShipmentPallet", "Всего оприход. (паллета)", quantityShipmentStock, 1, palletFreightBox, 2);

        quantityShipmentFreight = addSGProp(baseGroup, "quantityShipmentFreight", true, true, "Всего оприход. (фрахт)", quantityShipmentPallet, 1, freightPallet, 2);

        quantityShipmentFreightSku = addJProp(baseGroup, "quantityShipmentFreightSku", "Кол-во по данным поставщика", baseLM.and1, invoicedShipmentSku, 1, 3, quantityShipmentFreight, 1, 2);

        quantityShipmentedFreightSku = addSGProp(baseGroup, "quantityShipmentedFreightSku", "Кол-во", quantityShipmentFreightSku, 2, 3);

        quantityShipmentArticle = addSGProp(baseGroup, "quantityShipmentArticle", "Всего оприход. (артикул)", quantityShipmentSku, 1, articleSku, 2);
        quantityShipmentArticleSize = addSGProp(baseGroup, "quantityShipmentArticleSize", "Всего оприход. (артикул-размер)", quantityShipmentSku, 1, articleSku, 2, sizeSupplierItem, 2);
        quantityShipmentArticleColor = addSGProp(baseGroup, "quantityShipmentArticleColor", "Всего оприход. (артикул-цвет)", quantityShipmentSku, 1, articleSku, 2, colorSupplierItem, 2);
        quantityShipmentArticleColorSize = addSGProp(baseGroup, "quantityShipmentArticleColorSize", "Всего оприход. (артикул-цвет-размер)", quantityShipmentSku, 1, articleSku, 2, colorSupplierItem, 2, sizeSupplierItem, 2);
        quantityShipmentSize = addSGProp(baseGroup, "quantityShipmentSize", "Всего оприход. (размер)", quantityShipmentArticleSize, 1, 3);

        oneShipmentArticle = addJProp(baseGroup, "oneShipmentArticle", "Первый артикул", baseLM.equals2, quantityShipmentArticle, 1, 2, addCProp(NumericClass.get(14, 2), 1.0));
        oneShipmentArticleColor = addJProp(baseGroup, "oneShipmentArticleColor", "Первый артикул-цвет", baseLM.equals2, quantityShipmentArticleColor, 1, 2, 3, addCProp(NumericClass.get(14, 2), 1.0));
        oneShipmentArticleSize = addJProp(baseGroup, "oneShipmentArticleSize", "Первый артикул-размер", baseLM.equals2, quantityShipmentArticleSize, 1, 2, 3, addCProp(NumericClass.get(14, 2), 1.0));

        oneShipmentArticleSku = addJProp(baseGroup, "oneShipmentArticleSku", "Первый артикул", oneShipmentArticle, 1, articleSku, 2);
        oneShipmentArticleColorSku = addJProp(baseGroup, "oneShipmentArticleColorSku", "Первый артикул-цвет", oneShipmentArticleColor, 1, articleSku, 2, colorSupplierItem, 2);
        oneShipmentArticleSizeSku = addJProp(baseGroup, "oneShipmentArticleSizeSku", "Первый артикул-размер", oneShipmentArticleSize, 1, articleSku, 2, sizeSupplierItem, 2);

        oneShipmentSku = addJProp(baseGroup, "oneShipmentSku", "Первый SKU", baseLM.equals2, quantityShipmentSku, 1, 2, addCProp(NumericClass.get(14, 2), 1.0));

        oneArticleSkuShipmentDetail = addJProp(baseGroup, "oneArticleSkuShipmentDetail", "Первый артикул", oneShipmentArticleSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneArticleColorShipmentDetail = addJProp(baseGroup, "oneArticleColorShipmentDetail", "Первый артикул-цвет", oneShipmentArticleColorSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneArticleSizeShipmentDetail = addJProp(baseGroup, "oneArticleSizeShipmentDetail", "Первый артикул-размер", oneShipmentArticleSizeSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);
        oneSkuShipmentDetail = addJProp(baseGroup, "oneSkuShipmentDetail", "Первый SKU", oneShipmentSku, shipmentShipmentDetail, 1, skuShipmentDetail, 1);

        // Stamp
        quantityCreationStamp = addDProp(baseGroup, "quantityCreationStamp", "Количество", IntegerClass.instance, creationStamp);
        seriesOfCreationStamp = addDProp(baseGroup, "seriesOfCreationStamp", "Серия марки", StringClass.get(2), creationStamp);
        firstNumberCreationStamp = addDProp(baseGroup, "firstNumberCreationStamp", "Номер с", StringClass.get(8), creationStamp);
        lastNumberCreationStamp = addDProp(baseGroup, "lastNumberCreationStamp", "Номер по", StringClass.get(8), creationStamp);
        dateOfCreationStamp = addDProp(baseGroup, "dateOfCreationStamp", "Дата", DateClass.instance, creationStamp);

        creationStampStamp = addDProp(idGroup, "creationStampStamp", "Операция (ИД)", creationStamp, stamp);
        sidStamp = addDProp(baseGroup, "sidStamp", "Контрольная марка", StringClass.get(100), stamp);
        seriesOfStamp = addJProp(baseGroup, "seriesOfStamp", "Серия марки", seriesOfCreationStamp, creationStampStamp, 1);
        dateOfStamp = addJProp(baseGroup, "dateOfStamp", "Дата марки", dateOfCreationStamp, creationStampStamp, 1);

        stampShipmentDetail = addDProp("stampSkuShipmentDetail", "Контрольная марка", stamp, shipmentDetail);
        necessaryStampCategory = addDProp(baseGroup, "necessaryStampCategory", "Нужна марка", LogicalClass.instance, category);
        necessaryStampSkuShipmentDetail = addJProp("necessaryStampSkuShipmentDetail", necessaryStampCategory, categoryArticleSkuShipmentDetail, 1);
        sidStampShipmentDetail = addJProp(intraAttributeGroup, "sidStampShipmentDetail", "Контрольная марка",  sidStamp, stampShipmentDetail, 1);
        seriesOfStampShipmentDetail = addJProp(intraAttributeGroup, "seriesOfStampShipmentDetail", "Серия контрольной марки",  seriesOfStamp, stampShipmentDetail, 1);
        hideSidStampShipmentDetail = addHideCaptionProp(privateGroup, "Контрольная марка", necessaryStampSkuShipmentDetail);
        hideSeriesOfStampShipmentDetail = addHideCaptionProp(privateGroup, "Серия контрольной марки", necessaryStampSkuShipmentDetail);
        shipmentDetailStamp = addAGProp(idGroup, "shipmentDetailStamp", "Контрольная марка (ИД)", shipmentDetail, stampShipmentDetail);

        // Transfer
        stockFromTransfer = addDProp(idGroup, "stockFromTransfer", "Место хранения (с) (ИД)", stock, transfer);
        barcodeStockFromTransfer = addJProp(baseGroup, "barcodeStockFromTransfer", "Штрих-код МХ (с)", barcode, stockFromTransfer, 1);

        stockToTransfer = addDProp(idGroup, "stockToTransfer", "Место хранения (на) (ИД)", stock, transfer);
        barcodeStockToTransfer = addJProp(baseGroup, "barcodeStockToTransfer", "Штрих-код МХ (на)", barcode, stockToTransfer, 1);

        quantityTransferSku = addDProp(baseGroup, "quantityTransferStockSku", "Кол-во перемещения", NumericClass.get(14, 2), transfer, sku);

        outcomeTransferStockSku = addSGProp(baseGroup, "outcomeTransferStockSku", "Расход по ВП", quantityTransferSku, stockFromTransfer, 1, 2);
        incomeTransferStockSku = addSGProp(baseGroup, "incomeTransferStockSku", "Приход по ВП", quantityTransferSku, stockToTransfer, 1, 2);

        incomeStockSku = addSUProp(baseGroup, "incomeStockSku", "Приход", Union.SUM, quantityStockSku, incomeTransferStockSku);
        outcomeStockSku = outcomeTransferStockSku;

        balanceStockSku = addDUProp(baseGroup, "balanceStockSku", "Тек. остаток", incomeStockSku, outcomeStockSku);

        balanceStockFromTransferSku = addJProp(baseGroup, "balanceStockFromTransferSku", "Тек. остаток на МХ (с)", balanceStockSku, stockFromTransfer, 1, 2);
        balanceStockToTransferSku = addJProp(baseGroup, "balanceStockToTransferSku", "Тек. остаток на МХ (на)", balanceStockSku, stockToTransfer, 1, 2);

        // Расписывание по route'ам количеств в инвойсе
        quantityShipmentRouteSku = addSGProp(baseGroup, "quantityShipmentRouteSku", "Кол-во оприход.", quantityShipmentStockSku, 1, routeCreationFreightBoxFreightBox, 2, 3);
        invoicedShipmentRouteSku = addPGProp(baseGroup, "invoicedShipmentRouteSku", false, 0, true, "Кол-во ожид.",
                percentShipmentRouteSku,
                invoicedShipmentSku, 1, 3);

        zeroQuantityShipmentRouteSku = addSUProp(baseGroup, "zeroQuantityShipmentRouteSku", "кол-во", Union.OVERRIDE, addCProp(NumericClass.get(14, 2), 0.0, shipment, route, sku), quantityShipmentRouteSku);
        zeroInvoicedShipmentRouteSku = addSUProp(baseGroup, "zeroInvoicedShipmentRouteSku", "кол-во", Union.OVERRIDE, addCProp(NumericClass.get(14, 2), 0.0, shipment, route, sku), invoicedShipmentRouteSku);

        diffShipmentRouteSku = addJProp(baseLM.greater2, zeroQuantityShipmentRouteSku, 1, 2, 3, zeroInvoicedShipmentRouteSku, 1, 2, 3);

        sumShipmentRouteSku = addJProp("sumShipmentRouteSku", "Сумма", baseLM.multiply, invoicedShipmentRouteSku, 1, 2, 3, priceShipmentSku, 1, 3);
        sumShipmentRoute = addSGProp("sumShipmentRoute", "Сумма (ожид.)", sumShipmentRouteSku, 1, 2);

        sumShipmentSku = addJProp("sumShipmentSku", "Сумма (оприход.)", baseLM.multiply, priceShipmentSku, 1, 2, quantityShipmentSku, 1, 2);

        sumShipmentArticleColor = addSGProp("sumShipmentArticleColor", "Сумма (оприход.)", sumShipmentSku, 1, articleSku, 2, colorSupplierItem, 2);
        sumShipment = addSGProp("sumShipment", "Сумма (ожид.)", sumShipmentRoute, 1);

        invoicedShipmentRoute = addSGProp(baseGroup, "invoicedShipmentRoute", "Кол-во", invoicedShipmentRouteSku, 1, 2);

//        notFilledShipmentRouteSku = addJProp(baseGroup, "notFilledShipmentRouteSku", "Не заполнен", greater2, invoicedShipmentRouteSku, 1, 2, 3,
//                addSUProp(Union.OVERRIDE, addCProp(DoubleClass.instance, 0, shipment, route, sku), quantityShipmentRouteSku), 1, 2, 3);
//
//        routeToFillShipmentSku = addMGProp(idGroup, "routeToFillShipmentSku", "Маршрут (ИД)",
//                addJProp(baseLM.and1, object(route), 2, notFilledShipmentRouteSku, 1, 2, 3), 1, 3);
//
//        LCP routeToFillShipmentBarcode = addJProp(routeToFillShipmentSku, 1, barcodeToObject, 2);
//        seekRouteToFillShipmentBarcode = addJProp(actionGroup, true, "seekRouteToFillShipmentSku", "Поиск маршрута", addSAProp(null),
//                routeToFillShipmentBarcode, 1, 2);

        addConstraint(addJProp("Магазин короба для транспортировки должен совпадать с магазином короба поставщика", baseLM.and1,
                addJProp(baseLM.diff2, destinationSupplierBox, 1, destinationFreightBox, 2), 1, 2,
                quantityShipDimensionStock, 1, 2), true);

        // Freight
        tonnageFreightType = addDProp(baseGroup, "tonnageFreightType", "Тоннаж (кг)", NumericClass.get(14, 3), freightType);
        palletCountFreightType = addDProp(baseGroup, "palletCountFreightType", "Кол-во паллет", IntegerClass.instance, freightType);
        volumeFreightType = addDProp(baseGroup, "volumeFreightType", "Объем", NumericClass.get(14, 3), freightType);

        descriptionFreight = addDProp(baseGroup, "descriptionFreight", "Описание", StringClass.get(50), freight);

        freightTypeFreight = addDProp(idGroup, "freightTypeFreight", "Тип машины (ИД)", freightType, freight);
        nameFreightTypeFreight = addJProp(baseGroup, "nameFreightTypeFreight", "Тип машины", name, freightTypeFreight, 1);

        tonnageFreight = addJProp(baseGroup, "tonnageFreight", "Тоннаж (кг)", tonnageFreightType, freightTypeFreight, 1);
        tonnageDataFreight = addDProp(baseGroup, "tonnageDataFreight", "Тоннаж (кг)", NumericClass.get(14, 3), freight);
        palletCountFreight = addJProp(baseGroup, "palletCountFreight", "Кол-во паллет", palletCountFreightType, freightTypeFreight, 1);
        palletCountDataFreight = addDProp(baseGroup, "palletCountDataFreight", "Кол-во паллет", IntegerClass.instance, freight);
        volumeFreight = addJProp(baseGroup, "volumeFreight", "Объём", volumeFreightType, freightTypeFreight, 1);
        volumeDataFreight = addDProp(baseGroup, "volumeDataFreight", "Объем груза", NumericClass.get(14, 3), freight);

        currencyFreight = addDProp(idGroup, "currencyFreight", "Валюта (ИД)", getCurrencyClass(), freight);
        nameCurrencyFreight = addJProp(baseGroup, "nameCurrencyFreight", "Валюта", BL.Currency.getLCPByName("nameCurrency"), currencyFreight, 1);
        nameCurrencyFreight.setFixedCharWidth(10);
        symbolCurrencyFreight = addJProp(baseGroup, "symbolCurrencyFreight", "Валюта", BL.getModule("Currency").getLCPByName("symbolCurrency"), currencyFreight, 1);

        sumFreightFreight = addDProp(baseGroup, "sumFreightFreight", "Стоимость", NumericClass.get(14, 2), freight);

        sumRateFreightFreight = addJProp(baseGroup, "sumRateFreightFreight", true, "Стоимость (конверт.)", round2, addJProp(baseLM.multiply, sumFreightFreight, 1, addJProp(BL.getModule("Currency").getLCPByName("rateTypeExchangeCurrencyDate"), typeExchangeSTX, currencyFreight, 1, date, 1), 1), 1);

        insuranceFreight = addDProp(baseGroup, "insuranceFreight", "Страховка", NumericClass.get(14, 2), freight);
        insuranceFreightBrandSupplier = addDProp(baseGroup, "insuranceFreightBrandSupplier", "Страховка за бренд", NumericClass.get(14, 2), freight, brandSupplier);
        insuranceFreightBrandSupplierArticle = addJProp(baseGroup, "insuranceFreightBrandSupplierArticle", "Страховка за бренд", insuranceFreightBrandSupplier, 1, brandSupplierArticle, 2);
        insuranceFreightBrandSupplierSku = addJProp(baseGroup, "insuranceFreightBrandSupplierSku", "Страховка за бренд", insuranceFreightBrandSupplier, 1, brandSupplierArticleSku, 2);

        routeFreight = addDProp(idGroup, "routeFreight", "Маршрут (ИД)", route, freight);
        nameRouteFreight = addJProp(baseGroup, "nameRouteFreight", "Маршрут", name, routeFreight, 1);
        nameRouteFreight.setFixedCharWidth(8);

        exporterFreight = addDProp(idGroup, "exporterFreight", "Экспортер (ИД)", exporter, freight);
        setNotNull(exporterFreight);
        nameOriginExporterFreight = addJProp(baseGroup, "nameOriginExporterFreight", "Экспортер", nameOrigin, exporterFreight, 1);
        nameExporterFreight = addJProp(baseGroup, "nameExporterFreight", "Экспортер", name, exporterFreight, 1);
        addressOriginExporterFreight = addJProp(baseGroup, "addressOriginExporterFreight", "Адрес", addressOriginSubject, exporterFreight, 1);
        addressExporterFreight = addJProp(baseGroup, "addressExporterFreight", "Адрес", addressSubject, exporterFreight, 1);

        supplierFreight = addDProp(idGroup, "supplierFreight", "Поставщик (ИД)", supplier, freight);
        nameSupplierFreight = addJProp("nameSupplierFreight", "Поставщик", name, supplierFreight, 1);

        addConstraint(addJProp("Поставщик инвойса должен соответствовать поставщику фрахта", baseLM.diff2,
                supplierDocument, 1, addJProp(supplierFreight, freightDirectInvoice, 1), 1), true);

        inInvoiceFreight = addDProp(baseGroup, "inInvoiceFreight", "Вкл.", LogicalClass.instance, invoice, freight);
        netWeightInvoicedFreight = addSGProp(baseGroup, "netWeightInvoicedFreight", "Вес инвойсов", addJProp(baseLM.and1, netWeightDocument, 1, inInvoiceFreight, 1, 2), 2);

        dateShipmentFreight = addDProp(baseGroup, "dateShipmentFreight", "Дата отгрузки", DateClass.instance, freight);
        dateArrivalFreight = addDProp(baseGroup, "dateArrivalFreight", "Дата поступления на склад", DateClass.instance, freight);

        countryFreight = addDProp("countryFreight", "Страна назначения (ИД)", country, freight);
        nameCountryFreight = addJProp("nameCountryFreight", "Страна назначения", BL.Country.getLCPByName("nameCountry"), countryFreight, 1);

        languageFreight = addJProp("languageFreight", "Язык фрахта (ИД)", BL.Country.getLCPByName("languageCountry"), countryFreight, 1);
        nameLanguageFreight = addJProp("nameLanguageFreight", "Язык фрахта", BL.I18n.getLCPByName("nameLanguage"), languageFreight, 1);

        currencyCountryFreight = addJProp("currencyCountryFreight", "Валюта страны назначения (ИД)", BL.getModule("Country").getLCPByName("currencyCountry"), countryFreight, 1);
        nameCurrencyCountryFreight = addJProp("nameCurrencyCountryFreight", "Валюта страны назначения", BL.Currency.getLCPByName("nameCurrency"), languageFreight, 1);

        customsZoneFreight = addJProp("customsZoneFreight", "", customsZoneCountry, countryFreight, 1);

        mainCompositionLanguageFreightSku  = addDProp("mainCompositionLanguageFreightSku", "Состав (укр.)", StringClass.get(200), freight, sku);
        additionalCompositionLanguageFreightSku = addDProp("additionalCompositionLanguageFreightSku", "Доп. состав (укр.)", StringClass.get(200), freight, sku);

        mainCompositionSkuFreight = addJProp("mainCompositionSkuFreight", "Состав (укр.)", mainCompositionSkuLanguage, 1, languageFreight, 2);

        translationMainCompositionSkuFreight = addJoinAProp("translationMainCompositionSkuFreight", "Перевести", translationMainCompositionSkuLanguage, 1, languageFreight, 2);
        translationMainCompositionSkuFreight.addToContextMenuFor(mainCompositionSkuFreight);

        additionalCompositionSkuFreight = addJProp("additionalCompositionSkuFreight", "Доп. состав (укр.)", additionalCompositionSkuLanguage, 1, languageFreight, 2);

        translationAdditionalCompositionSkuFreight = addJoinAProp("translationAdditionalCompositionSkuFreight", "Перевести", translationAdditionalCompositionSkuLanguage, 1, languageFreight, 2);
        translationAdditionalCompositionSkuFreight.addToContextMenuFor(additionalCompositionSkuFreight);

        customCategory10CategoryGenderCompositionTypeFabricFreight = addJProp(true, "customCategory10CategoryGenderCompositionTypeFabricFreight", "ТН ВЭД (ИД)", customCategory10CategoryGenderCompositionTypeFabricCustomsZone, 1, 2, 3, 4, customsZoneFreight, 5);
        sidCustomCategory10CategoryGenderCompositionTypeFabricFreight = addJProp("sidCustomCategory10CategoryGenderCompositionTypeFabricFreight", "ТН ВЭД", sidCustomCategory10, customCategory10CategoryGenderCompositionTypeFabricFreight, 1, 2, 3, 4, 5);

        dictionaryFreight = addJProp("dictionaryFreight", "Словарь", BL.RomanI18n.getLCPByName("dictionaryCompositionLanguage"), languageFreight, 1);

        dateImporterFreightTypeInvoice = addDProp(baseGroup, "dateImporterFreightTypeInvoice", "Дата инвойса", DateClass.instance, importer, freight, typeInvoice);
        dateImporterFreight = addMGProp(baseGroup, "dateImporterFreight", "Дата инвойса", dateImporterFreightTypeInvoice, 1, 2);

        dateShipmentImporterFreightTypeInvoice = addDProp(baseGroup, "dateShipmentImporterFreightTypeInvoice", "Дата поставки", DateClass.instance, importer, freight, typeInvoice);

        contractImporterFreight = addDProp(idGroup, "contractImporterFreight", "Договор (ИД)", contract, importer, freight);
        //nameContractImporterFreight = addJProp(baseGroup, "nameContractImporterFreight", "Договор", name, contractImporterFreight, 1, 2);

        sidContractImporterFreight = addJProp(baseGroup, "sidContractImporterFreight", "Договор", sidContract, contractImporterFreight, 1, 2);
        dateContractImporterFreight = addJProp(baseGroup, "dateContractImporterFreight", "Дата договора", dateContract, contractImporterFreight, 1, 2);
        conditionShipmentContractImporterFreight = addJProp(baseGroup, "conditionShipmentContractImporterFreight", "Условие поставки", conditionShipmentContract, contractImporterFreight, 1, 2);
        conditionPaymentContractImporterFreight = addJProp(baseGroup, "conditionPaymentContractImporterFreight", "Условие оплаты", conditionPaymentContract, contractImporterFreight, 1, 2);

        quantityPalletShipmentBetweenDate = addSGProp(baseGroup, "quantityPalletShipmentBetweenDate", "Кол-во паллет по поставкам за интервал",
                addJProp(baseLM.and1, quantityPalletShipment, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);
        quantityPalletFreightBetweenDate = addSGProp(baseGroup, "quantityPalletFreightBetweenDate", "Кол-во паллет по фрахтам за интервал",
                addJProp(baseLM.and1, palletCountFreight, 1, addJProp(between, date, 1, object(DateClass.instance), 2, object(DateClass.instance), 3), 1, 2, 3), 2, 3);

        freightBoxNumberPallet = addSGProp(baseGroup, "freightBoxNumberPallet", "Кол-во коробов", addCProp(IntegerClass.instance, 1, freightBox), palletFreightBox, 1);

        addConstraint(addJProp("Для паллеты должен быть задан вес бруто", baseLM.andNot1, freightBoxNumberPallet, 1, grossWeightPallet, 1), false);

        addConstraint(addJProp("Маршрут паллеты должен совпадать с маршрутом фрахта", baseLM.diff2,
                routeCreationPalletPallet, 1, addJProp(routeFreight, freightPallet, 1), 1), true);

        //addConstraint(addJProp("Маршрут короба должен совпадать с маршрутом паллеты", baseLM.diff2,
        //        routeCreationFreightBoxFreightBox, 1, addJProp(routeCreationPalletPallet, palletFreightBox, 1), 1), true);

        palletNumberProxyFreight = addSGProp("palletNumberProxyFreight", "Кол-во присоединённых паллет", addCProp(IntegerClass.instance, 1, pallet), freightPallet, 1);

        palletNumberFreight = addSUProp(baseGroup, "palletNumberFreight", true, "Кол-во присоединённых паллет", Union.SUM,
                palletNumberProxyFreight,
                addSGProp(palletNumberDirectInvoice, freightDirectInvoice, 1));

        freightBoxNumberFreight = addSGProp(baseGroup, "freightBoxNumberFreight", "Кол-во присоединённых коробов", freightBoxNumberPallet, freightPallet, 1);

        diffPalletFreight = addJProp(baseLM.greater2, palletNumberFreight, 1, palletCountDataFreight, 1);

        freightSupplierBox = addJProp(baseGroup, "freightSupplierBox", "Фрахт (ИД)", freightDirectInvoice, boxInvoiceSupplierBox, 1);
        freightSimpleInvoice = addJProp("freightSimpleInvoice", "Фрахт (ИД)", baseLM.and1, freightDirectInvoice, 1, is(directSimpleInvoice), 1);

        freightFreightUnit = addCUProp(idGroup, "freightFreightUnit", true, "Фрахт (ИД)", freightFreightBox, freightSupplierBox, freightSimpleInvoice);

        stockNumberFreightBrandSupplier = addSGProp(baseGroup, "stockNumberFreightBrandSupplier", "Кол-во коробов по бренду", addJProp(baseLM.and1, addCProp(IntegerClass.instance, 1), quantityFreightUnitBrandSupplier, 1, 2), freightFreightUnit, 1, 2);

        //importerSupplierBox = addJProp(baseGroup, "importerSupplierBox", "Импортер (ИД)", importerDirectInvoice, boxInvoiceSupplierBox, 1);

        importerSupplierBox = addCUProp(baseGroup, "importerSupplierBox", "Импортер (ИД)", importerDirectInvoice, addJProp(baseLM.and1, addJProp(importerDirectInvoice, boxInvoiceSupplierBox, 1), 1, is(supplierBox), 1));

        // Кол-во для импортеров
        // здесь не соблюдается policy, что входы совпадают с именем
        quantityInvoiceFreightUnitSku = addCUProp(baseGroup, "quantityInvoiceFreightUnitSku", "Кол-во",
                quantityInvoiceStockSku,
                addJProp(baseLM.and1, quantityListSku, 2, 3, addJProp(baseLM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        priceRateSupplierBoxSku = addJProp("priceRateSupplierBoxSku", "Цена входная", priceRateDocumentSku, boxInvoiceSupplierBox, 1, 2);
        RRPRateSupplierBoxSku = addJProp("RRPRateSupplierBoxSku", "Цена рекомендованная", RRPRateDocumentSku, boxInvoiceSupplierBox, 1, 2);

        priceInInvoiceFreightUnitSku = addCUProp(baseGroup, "priceInInvoiceFreightUnitSku", "Цена входная",
                priceInInvoiceStockSku,
                addJProp(baseLM.and1, priceRateDocumentSku, 1, 3, addJProp(baseLM.equals2, 1, boxInvoiceSupplierBox, 2), 1, 2));

        routeFreightFreightBox = addJProp(idGroup, "routeFreightFreightBox", "Маршрут (ИД)", routeFreight, freightFreightBox, 1);
        importerShipmentRoute = addDProp("importerShipmentRoute", "Импортер (ИД)", importer, shipment, route);

        addConstraint(addJProp("Для маршрута поставки должен быть задан импортер", baseLM.andNot1, percentShipmentRoute, 1, 2, importerShipmentRoute, 1, 2), false);

        nameImporterShipmentRoute = addJProp("nameImporterShipmentRoute", "Импортер", name, importerShipmentRoute, 1, 2);
        importerShipmentFreightBox = addJProp("importerShipmentFreightBox", "Импортер (ИД)", importerShipmentRoute, 1, routeFreightFreightBox, 2);

        importerShipmentFreight = addJProp(idGroup, true, "importerShipmentFreight", "Импортёр (ИД)", importerShipmentRoute, 1, routeFreight, 2);
        nameImporterShipmentFreight = addJProp(baseGroup, "nameImporterShipmentFreight", "Импортёр", name, importerShipmentFreight, 1, 2);

        quantityImporterStockSku = addSGProp(baseGroup, "quantityImporterStockSku", true, "Кол-во", quantityShipmentStockSku, importerShipmentFreightBox, 1, 2, 2, 3);
        // quantityImporterStockSku = addSGProp(baseGroup, "quantityImporterStockSku", true, "Кол-во", quantityInvoiceStockSku, importerInvoice, 1, 2, 3);
        quantityImporterStockArticle = addSGProp(baseGroup, "quantityImporterStockArticle", true, true, "Кол-во", quantityImporterStockSku, 1, 2, articleSku, 3);

        quantityImporterStockTypeInvoice = addSGProp(baseGroup, "quantityImporterStockTypeInvoice", "Кол-во", quantityImporterStockArticle, 1, 2, addJProp(typeInvoiceFreightArticle, freightFreightUnit, 2, 3), 1, 2, 3);
        quantityImporterStock = addSGProp(baseGroup, "quantityImporterStock", "Кол-во", quantityImporterStockSku, 1, 2);

        quantityProxyImporterFreightSku = addSGProp(baseGroup, "quantityProxyImporterFreightSku", true, true, "Кол-во (из приёмки)", quantityImporterStockSku, 1, freightFreightUnit, 2, 3);
        quantityDirectImporterFreightSku = addSGProp(baseGroup, "quantityDirectImporterFreightSku", true, true, "Кол-во (напрямую)", quantityListSku, importerSupplierBox, 1, freightFreightUnit, 1, 2);
        quantityImporterFreightSku = addSUProp(baseGroup, "quantityImporterFreightSku", true, "Кол-во", Union.SUM, quantityProxyImporterFreightSku, quantityDirectImporterFreightSku);

        quantityFreightArticle = addSGProp(baseGroup, "quantityFreightArticle", true, true, "Кол-во отгруженное с STX", quantityImporterFreightSku, 2, articleSku, 3);
        quantityFreightBrandSupplier = addSGProp(baseGroup, "quantityFreightBrandSupplier", true, true, "Кол-во отгруженное с STX", quantityImporterFreightSku, 2, brandSupplierArticleSku, 3);
        //quantityFreightSupplier = addSGProp(baseGroup, "quantityFreightSupplier", true, "Кол-во", quantityFreightBrandSupplier, 1, supplierBrandSupplier, 2);


        quantityFreightSku = addSGProp(baseGroup, "quantityFreightSku", true, true, "Кол-во", quantityImporterFreightSku, 2, 3);
        quantityDirectFreightSku = addSGProp(baseGroup, "quantityDirectFreightSku", true, true, "Кол-во (напрямую)", quantityDirectImporterFreightSku, 2, 3);
        quantityDirectImporterFreightSupplier = addSGProp(baseGroup, "quantityDirectImporterFreightSupplier", true, true, "Кол-во (напрямую)", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        quantityShipmentedAllFreightSku = addSUProp(Union.SUM, quantityShipmentedFreightSku, quantityDirectFreightSku);
        quantityShipmentedFreightArticle = addSGProp(baseGroup, "quantityShipmentedFreightArticle", true, true, "Кол-во по данным поставщика", quantityShipmentedAllFreightSku, 1, articleSku, 2);
        quantityShipmentedFreightBrandSupplier = addSGProp(baseGroup, "quantityShipmentedFreightBrandSupplier", true, true, "Кол-во по данным поставщика", quantityShipmentedFreightArticle, 1, brandSupplierArticle, 2);

        quantityFreightedBetweenDateSku = addSGProp(baseGroup, "quantityFreightedBetweenDateSku", "Кол-во отгруженное за период", addJProp(and(false, false), quantityFreightSku, 1, 2, is(freightShipped), 1, addJProp(betweenDates, date, 1, object(DateClass.instance), 3, object(DateClass.instance), 4), 1, 2, 3, 4), 2, 3, 4);
        quantityFreightedSku = addSGProp(baseGroup, "quantityFreightedSku", "Кол-во отгруженное", quantityFreightSku, 2);

        balanceSku = addDUProp(baseGroup, "balanceSku", "Остатки на складе", quantityShipmentedSku, quantityFreightedSku);

        quantityFreightCategory = addSGProp(baseGroup, "quantityFreightCategory", true, true, "Кол-во", quantityFreightSku, 1, categoryArticleSku, 2);

        quantityFreightCategoryGenderCompositionTypeFabric = addSGProp(baseGroup, "quantityFreightCategoryGenderCompositionTypeFabric", "Кол-во", quantityFreightSku, 1, categoryArticleSku, 2, genderArticleSku, 2, mainCompositionOriginSku, 2, typeFabricArticleSku, 2);

        customCategory10DataSkuCustomsZone = addDProp("customCategory10DataSkuCustomsZone", "ТН ВЭД (ИД)", customCategory10, sku, customsZone);
        customCategory10SkuCustomsZone = addSUProp("customCategory10SkuCustomsZone", "ТН ВЭД (ИД)", Union.OVERRIDE, customCategory10CategoryGenderCompositionTypeFabricSkuCustomsZone, customCategory10DataSkuCustomsZone);

        customCategory10SkuFreight = addJProp(true, "customCategory10SkuFreight", "ТН ВЭД (ИД)", customCategory10SkuCustomsZone, 1, customsZoneFreight, 2);
        sidCustomCategory10SkuFreight = addJProp("sidCustomCategory10SkuFreight", "ТН ВЭД", sidCustomCategory10, customCategory10SkuFreight, 1, 2);

        subCategoryDataSkuCustomsZone = addDProp("subCategoryDataSkuCustomsZone", "Дополнительное деление (ИД)", subCategory, sku, customsZone);
        subCategoryCustomCategory10SkuCustomsZone = addJProp(idGroup, "subCategoryCustomCategory10SkuCustomsZone", "Дополнительное деление (ИД)", subCategoryCustomCategory10, customCategory10SkuCustomsZone, 1, 2);
        subCategorySkuCustomsZone = addSUProp(Union.OVERRIDE, subCategoryCustomCategory10SkuCustomsZone, subCategoryDataSkuCustomsZone);

        subCategorySkuFreight = addJProp(true, "subCategorySkuFreight", "Дополнительное деление (ИД)", subCategoryDataSkuCustomsZone, 1, customsZoneFreight, 2);
        nameSubCategorySkuFreight = addJProp("nameSubCategorySkuFreight", "Дополнительное деление", nameSubCategory, subCategorySkuFreight, 1, 2);

        customCategory10FreightSku = addDProp(idGroup, "customCategory10FreightSku", "ТН ВЭД (ИД)", customCategory10, freight, sku);
        customCategory10FreightSku.setEventChangeNewSet(addJProp(baseLM.and1, customCategory10SkuFreight, 2, 1, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        sidCustomCategory10FreightSku = addJProp(baseGroup, "sidCustomCategory10FreightSku", "ТН ВЭД", sidCustomCategory10, customCategory10FreightSku, 1, 2);
        addConstraint(addJProp("Для SKU должен быть задан ТН ВЭД", and(true, false), is(freightChanged), 1, customCategory10FreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        subCategoryFreightSku = addDProp(idGroup, "subCategoryFreightSku", "Дополнительное деление (ИД)", subCategory, freight, sku);
        subCategoryFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, subCategorySkuFreight, 2, 1, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        nameSubCategoryFreightSku = addJProp(baseGroup, "nameSubCategoryFreightSku", "Дополнительное деление", nameSubCategory, subCategoryFreightSku, 1, 2);
        addConstraint(addJProp("Для SKU должно быть задано дополнительное деление", and(true, false, false), is(freightChanged), 1, subCategoryFreightSku, 1, 2, diffCountRelationCustomCategory10FreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        customCategory6FreightSku = addJProp(idGroup, "customCategory6FreightSku", "ТН ВЭД", customCategory6CustomCategory10, customCategory10FreightSku, 1, 2);

        customCategoryOriginFreightSku = addDProp(idGroup, "customCategoryOriginFreightSku", "ТН ВЭД (ИД)", customCategoryOrigin, freight, sku);
        customCategoryOriginFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, customCategoryOriginArticleSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        sidCustomCategoryOriginFreightSku = addJProp(baseGroup, "sidCustomCategoryOriginFreightSku", "ТН ВЭД (ориг.)", sidCustomCategoryOrigin, customCategoryOriginFreightSku, 1, 2);
        addConstraint(addJProp("Зона ТН ВЭД должна совпадать с зоной фрахта", baseLM.diff2,
                customsZoneFreight, 1, addJProp(customsZoneCustomCategory10, customCategory10FreightSku, 1, 2), 1, 2), true);

        addConstraint(addJProp("Зона ТН ВЭД должна совпадать с зоной фрахта", baseLM.diff2,
                customsZoneFreight, 1, addJProp(customsZoneCustomCategory10, customCategory10SkuFreight, 2, 1), 1, 2), true);

        quantityProxyImporterFreightCustomCategory6Category = addSGProp(baseGroup, "quantityProxyImporterFreightCustomCategory6Category", "Кол-во", quantityProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        quantityProxyImporterFreightCustomCategory6 = addSGProp(baseGroup, "quantityProxyImporterFreightCustomCategory6", "Кол-во", quantityProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        quantityDirectImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "quantityDirectImporterFreightSupplierCustomCategory6Category", "Кол-во", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        quantityDirectImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "quantityDirectImporterFreightSupplierCustomCategory6", "Кол-во", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        mainCompositionOriginFreightSku = addDProp(baseGroup, "mainCompositionOriginFreightSku", "Состав", COMPOSITION_CLASS, freight, sku);
        mainCompositionOriginFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, mainCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        additionalCompositionOriginFreightSku = addDProp(baseGroup, "additionalCompositionOriginFreightSku", "Доп. состав", COMPOSITION_CLASS, freight, sku);
        additionalCompositionOriginFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, additionalCompositionOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        additionalCompositionOriginFreightSku.property.preferredCharWidth = 40;
        additionalCompositionOriginFreightSku.property.minimumCharWidth = 20;

        translationMainCompositionSku = addJoinAProp(actionGroup, "translationMainCompositionSku", "Перевести", addTAProp(mainCompositionOriginSku, mainCompositionSku), dictionaryComposition, 1);
        translationMainCompositionSku.addToContextMenuFor(mainCompositionSku);
        translationAdditionalCompositionSku = addJoinAProp(actionGroup, "translationAdditionalCompositionSku", "Перевести", addTAProp(additionalCompositionOriginSku, additionalCompositionSku), dictionaryComposition, 1);
        translationAdditionalCompositionSku.addToContextMenuFor(additionalCompositionSku);

        mainCompositionFreightSku = addDProp(baseGroup, "mainCompositionFreightSku", "Состав (рус.)", COMPOSITION_CLASS, freight, sku);
        mainCompositionFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, mainCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        userNumberFreightSku = addDProp(baseGroup, "userNumberFreightSku", "Пользовательский номер", IntegerClass.instance, freight, sku);

        addConstraint(addJProp("Для SKU должен быть задан состав", and(true, false), is(freightChanged), 1, mainCompositionFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        additionalCompositionFreightSku = addDProp(baseGroup, "additionalCompositionFreightSku", "Доп. состав (рус.)", COMPOSITION_CLASS, freight, sku);
        additionalCompositionFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, additionalCompositionSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);
        additionalCompositionFreightSku.property.preferredCharWidth = 40;
        additionalCompositionFreightSku.property.minimumCharWidth = 20;

        translationMainCompositionFreightSku = addJoinAProp(actionGroup, "translationMainCompositionFreightSku", "Перевести", addTAProp(mainCompositionOriginFreightSku, mainCompositionFreightSku), dictionaryComposition, 1, 2);
        translationMainCompositionFreightSku.addToContextMenuFor(mainCompositionFreightSku);

        translationAdditionalCompositionFreightSku = addJoinAProp(actionGroup, "translationAdditionalCompositionFreightSku", "Перевести", addTAProp(additionalCompositionOriginFreightSku, additionalCompositionFreightSku), dictionaryComposition, 1, 2);
        translationAdditionalCompositionFreightSku.addToContextMenuFor(additionalCompositionFreightSku);

        translationMainCompositionLanguageFreightSku = addJoinAProp(actionGroup, "translationMainCompositionLanguageFreightSku", "Перевести", addTAProp(mainCompositionOriginFreightSku, mainCompositionLanguageFreightSku), dictionaryFreight, 1, 1, 2);
        translationMainCompositionLanguageFreightSku.addToContextMenuFor(mainCompositionLanguageFreightSku);

        translationAdditionalCompositionLanguageFreightSku = addJoinAProp(actionGroup, "translationAdditionalCompositionLanguageFreightSku", "Перевести", addTAProp(additionalCompositionOriginFreightSku, additionalCompositionLanguageFreightSku), dictionaryFreight, 1, 1, 2);
        translationAdditionalCompositionLanguageFreightSku.addToContextMenuFor(additionalCompositionLanguageFreightSku);

        countryOfOriginFreightSku = addDProp(idGroup, "countryOfOriginFreightSku", "Страна (ИД)", country, freight, sku);
        countryOfOriginFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, countryOfOriginSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должна быть задана страна", and(true, false), is(freightChanged), 1, countryOfOriginFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        sidCountryOfOriginFreightSku = addJProp(baseGroup, "sidCountryOfOriginFreightSku", "Код страны", BL.getModule("Country").getLCPByName("sidCountry"), countryOfOriginFreightSku, 1, 2);
        nameCountryOfOriginFreightSku = addJProp(baseGroup, "nameCountryOfOriginFreightSku", "Страна", BL.Country.getLCPByName("nameCountry"), countryOfOriginFreightSku, 1, 2);
        nameCountryOfOriginFreightSku.property.preferredCharWidth = 50;
        nameCountryOfOriginFreightSku.property.minimumCharWidth = 15;

        quantityImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "quantityImporterFreightArticleCompositionCountryCategory", "Кол-во",
                quantityProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        compositionFreightArticleCompositionCountryCategory = addMGProp(baseGroup, "compositionFreightArticleCompositionCountryCategory", "Состав",
                mainCompositionFreightSku, 1, articleSku, 2, mainCompositionOriginFreightSku, 1, 2, countryOfOriginFreightSku, 1, 2, customCategory10FreightSku, 1, 2);

        netWeightStockSku = addJProp(baseGroup, "netWeightStockSku", "Вес нетто", baseLM.multiply, quantityStockSku, 1, 2, netWeightSku, 2);
        netWeightStockArticle = addSGProp(baseGroup, "netWeightStockArticle", "Вес нетто", netWeightStockSku, 1, articleSku, 2);
        netWeightStock = addSGProp(baseGroup, "netWeightStock", "Вес нетто короба", netWeightStockSku, 1);

        netWeightFreightSku = addDProp(baseGroup, "netWeightFreightSku", "Вес нетто (ед.)", NumericClass.get(14, 3), freight, sku);
        netWeightFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, netWeightSku, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должен быть задан вес нетто", and(true, false), is(freightChanged), 1, netWeightFreightSku, 1, 2, quantityFreightSku, 1, 2), false);

        netWeightImporterFreightUnitSku = addJProp(baseGroup, "netWeightImporterFreightUnitSku", "Вес нетто", baseLM.multiply, quantityImporterStockSku, 1, 2, 3, addJProp(netWeightFreightSku, freightFreightUnit, 1, 2), 2, 3);
        netWeightImporterFreightUnitArticle = addSGProp(baseGroup, "netWeightImporterFreightUnitArticle", true, "Вес нетто", netWeightImporterFreightUnitSku, 1, 2, articleSku, 3);

        netWeightImporterFreightUnitTypeInvoice = addSGProp(baseGroup, "netWeightImporterFreightUnitTypeInvoice", "Вес нетто", netWeightImporterFreightUnitArticle, 1, 2, addJProp(typeInvoiceFreightArticle, freightFreightUnit, 2, 3), 1, 2, 3);

        netWeightImporterFreightUnit = addSGProp(baseGroup, "netWeightImporterFreightUnit", "Вес нетто", netWeightImporterFreightUnitSku, 1, 2);

        netWeightImporterFreightSku = addJProp(baseGroup, "netWeightImporterFreightSku", "Вес нетто", baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);
        netWeightProxyImporterFreightSku = addJProp(baseGroup, "netWeightProxyImporterFreightSku", "Вес нетто", baseLM.multiply, quantityProxyImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);
        netWeightDirectImporterFreightSku = addJProp(baseGroup, "netWeightDirectImporterFreightSku", "Вес нетто", baseLM.multiply, quantityDirectImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);

        netWeightImporterFreightArticle = addSGProp(baseGroup, "netWeightImporterFreightArticle", true, "Вес нетто", netWeightImporterFreightSku, 1, 2, articleSku, 3);

        netWeightImporterFreightTypeInvoice = addSGProp(baseGroup, "netWeightImporterFreightTypeInvoice", "Вес нетто", netWeightProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);
        netWeightImporterFreightCustomCategory6 = addSGProp(baseGroup, "netWeightImporterFreightCustomCategory6", "Вес нетто", netWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        netWeightImporterFreightCustomCategory6Category = addSGProp(baseGroup, "netWeightImporterFreightCustomCategory6Category", "Вес нетто", netWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        netWeightImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "netWeightImporterFreightSupplierCustomCategory6Category", "Вес нетто", netWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        netWeightImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "netWeightImporterFreightSupplierCustomCategory6", "Вес нетто", netWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        netWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "netWeightImporterFreightArticleCompositionCountryCategory", "Вес нетто",
                netWeightProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        netWeightImporterFreight = addSGProp(baseGroup, "netWeightImporterFreight", true, "Вес нетто", netWeightProxyImporterFreightSku, 1, 2);
        netWeightImporterFreightSupplier = addSGProp(baseGroup, "netWeightImporterFreightSupplier", true, "Вес нетто", netWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        quantityImporterFreightArticle = addSGProp(baseGroup, "quantityImporterFreightArticle", true, "Кол-во (импортер)", quantityImporterFreightSku, 1, 2, articleSku, 3);
        quantityImporterFreightBrandSupplier = addSGProp(baseGroup, "quantityImporterFreightBrandSupplier", true, "Кол-во (импортер)", quantityImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        quantityImporterFreight = addSGProp(baseGroup, "quantityImporterFreight", true, "Кол-во позиций", quantityImporterFreightSku, 1, 2);
        quantityProxyImporterFreight = addSGProp(baseGroup, "quantityProxyImporterFreight", true, "Кол-во позиций", quantityProxyImporterFreightSku, 1, 2);
        quantityImporterFreightTypeInvoice = addSGProp(baseGroup, "quantityImporterFreightTypeInvoice", true, "Кол-во позиций", quantityProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);
        quantityImporterFreightSupplier = addSGProp(baseGroup, "quantityImporterFreightSupplier", true, "Кол-во позиций", quantityDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        quantityFreight = addSGProp(baseGroup, "quantityFreight", true, "Кол-во единиц", quantityImporterFreight, 2);

        // Текущие палеты/коробки для приема
        currentPalletRouteUser = addDProp("currentPalletRouteUser", "Тек. паллета (ИД)", pallet, route, BL.authenticationLM.user);

        currentPalletRoute = addJProp(true, "currentPalletRoute", "Тек. паллета (ИД)", currentPalletRouteUser, 1, BL.authenticationLM.currentUser);
        barcodeCurrentPalletRoute = addJProp("barcodeCurrentPalletRoute", "Тек. паллета (штрих-код)", barcode, currentPalletRoute, 1);

        sumNetWeightFreightSku = addJProp(baseGroup, "sumNetWeightFreightSku", "Вес нетто (всего)", baseLM.multiply, quantityFreightSku, 1, 2, netWeightSku, 2);

        grossWeightCurrentPalletRoute = addJProp(true, "grossWeightCurrentPalletRoute", "Вес брутто", grossWeightPallet, currentPalletRoute, 1);
        grossWeightFreight = addSUProp(baseGroup, "grossWeightFreight", true, "Вес брутто (фрахт)", Union.SUM,
                addSGProp(grossWeightPallet, freightPallet, 1),
                addSGProp(grossWeightDirectInvoice, freightDirectInvoice, 1));

        sumGrossWeightFreightSku = addPGProp(baseGroup, "sumGrossWeightFreightSku", false, 10, false, "Вес брутто",
                sumNetWeightFreightSku,
                grossWeightFreight, 1);

        grossWeightFreightSkuAggr = addJProp(baseGroup, "grossWeightFreightSkuAggr", "Вес брутто", divideNumeric3, sumGrossWeightFreightSku, 1, 2, quantityFreightSku, 1, 2);
        grossWeightFreightSku = addDProp(baseGroup, "grossWeightFreightSku", "Вес брутто", NumericClass.get(14, 3), freight, sku);
        grossWeightFreightSku.setEventChangeNewSet(addJProp(baseLM.and1, grossWeightFreightSkuAggr, 1, 2, quantityFreightSku, 1, 2), 1, 2, is(freightChanged), 1);

        addConstraint(addJProp("Для SKU должен быть задан вес брутто", and(true, false), is(freightChanged), 1, grossWeightFreightSku, 1, 2, quantityFreightSku, 1, 2), false);
        addConstraint(addJProp("Для SKU вес брутто должен быть больше веса нетто", and(true, false), is(freightChanged), 1, addJProp(baseLM.greater2, grossWeightFreightSku, 1, 2, netWeightFreightSku, 1, 2), 1, 2, quantityFreightSku, 1, 2), false);

        grossWeightImporterFreightSku = addJProp(baseGroup, "grossWeightImporterFreightSku", "Вес брутто", baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);
        grossWeightProxyImporterFreightSku = addJProp(baseGroup, "grossWeightProxyImporterFreightSku", "Вес брутто", baseLM.multiply, quantityProxyImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);
        grossWeightDirectImporterFreightSku = addJProp(baseGroup, "grossWeightDirectImporterFreightSku", "Вес брутто", baseLM.multiply, quantityDirectImporterFreightSku, 1, 2, 3, grossWeightFreightSku, 2, 3);

        grossWeightImporterFreightTypeInvoice = addSGProp(baseGroup, "grossWeightImporterFreightTypeInvoice", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);
        grossWeightImporterFreightCustomCategory6 = addSGProp(baseGroup, "grossWeightImporterFreightCustomCategory6", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        grossWeightImporterFreightCustomCategory6Category = addSGProp(baseGroup, "grossWeightImporterFreightCustomCategory6Category", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        grossWeightImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "grossWeightImporterFreightSupplierCustomCategory6Category", "Вес брутто", grossWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        grossWeightImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "grossWeightImporterFreightSupplierCustomCategory6", "Вес брутто", grossWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        grossWeightImporterFreightSupplier = addSGProp(baseGroup, "grossWeightImporterFreightSupplier", true, "Вес брутто", grossWeightDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        grossWeightImporterFreight = addSGProp(baseGroup, "grossWeightImporterFreight", "Вес брутто", grossWeightProxyImporterFreightSku, 1, 2);
        grossWeightImporterFreightUnitSku = addJProp(baseGroup, "grossWeightImporterFreightUnitSku", "Вес брутто", baseLM.multiply, quantityImporterStockSku, 1, 2, 3, addJProp(grossWeightFreightSku, freightFreightUnit, 2, 3), 1, 2, 3);
        grossWeightImporterFreightUnitArticle = addSGProp(baseGroup, "grossWeightImporterFreightUnitArticle", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2, articleSku, 3);

        grossWeightImporterFreightUnitTypeInvoice = addSGProp(baseGroup, "grossWeightImporterFreightUnitTypeInvoice", "Вес брутто", grossWeightImporterFreightUnitArticle, 1, 2, addJProp(typeInvoiceFreightArticle, freightFreightUnit, 2, 3), 1, 2, 3);
        grossWeightImporterFreightUnit = addSGProp(baseGroup, "grossWeightImporterFreightUnit", "Вес брутто", grossWeightImporterFreightUnitSku, 1, 2);

        grossWeightImporterFreightArticleCompositionCountryCategory = addSGProp(baseGroup, "grossWeightImporterFreightArticleCompositionCountryCategory", "Вес брутто",
                grossWeightProxyImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        // цены, надбавки, пошлины, налоги
        priceImporterFreightSku = addDProp(baseGroup, "priceImporterFreightSku", "Цена входная", NumericClass.get(14, 2), importer, freight, sku);
//        priceMaxImporterFreightSku = addMGProp(baseGroup, "priceMaxImporterFreightSku", true, "Цена входная", priceInInvoiceFreightUnitSku, importerDirectInvoice, 1, freightFreightUnit, 2, 3);
        priceProxyImporterFreightSku = addMGProp(baseGroup, "priceProxyImporterFreightSku", true, "Цена входная", priceInShipmentStockSku, importerShipmentFreightBox, 1, 2, freightFreightUnit, 2, 3);
        priceDirectImporterFreightSku = addMGProp(baseGroup, "priceDirectImporterFreightSku", true, "Цена входная", priceRateDocumentSku, importerDirectInvoice, 1, freightDirectInvoice, 1, 2);
        priceInImporterFreightSku = addSUProp(baseGroup, "priceInImporterFreightSku", "Цена входная", Union.OVERRIDE, priceDirectImporterFreightSku, priceProxyImporterFreightSku, priceImporterFreightSku);

        priceInFreightSku = addMGProp(baseGroup, "priceInFreightSku", true, "Цена входная", priceInImporterFreightSku, 2, 3);
        priceInImporterFreightArticle = addMGProp(baseGroup, "priceInImporterFreightArticle", true, "Цена входная", priceInImporterFreightSku, 1, 2, articleSku, 3);

        RRPImporterFreightSku = addDProp(baseGroup, "RRPImporterFreightSku", "Цена рекомендованная", NumericClass.get(14, 2), importer, freight, sku);
        RRPProxyImporterFreightSku = addMGProp(baseGroup, "RRPProxyImporterFreightSku", true, "Цена рекомендованная", RRPInShipmentStockSku, importerShipmentFreightBox, 1, 2, freightFreightUnit, 2, 3);
        RRPDirectImporterFreightSku = addMGProp(baseGroup, "RRPDirectImporterFreightSku", true, "Цена рекомендованная", RRPDocumentSku, importerDirectInvoice, 1, freightDirectInvoice, 1, 2);
        RRPInImporterFreightSku = addSUProp(baseGroup, "RRPInImporterFreightSku", "Цена рекомендованная", Union.OVERRIDE, RRPDirectImporterFreightSku, RRPProxyImporterFreightSku, RRPImporterFreightSku);

        currencyRRPImporterFreightSku = addDProp(idGroup, "currencyRRPImporterFreightSku", "Валюта RRP (ИД)", getCurrencyClass(), importer, freight, sku);
        currencyRRPProxyImporterFreightSku = addMGProp(idGroup, "currencyRRPProxyImporterFreightSku", true, "Валюта RRP (ИД)", currencyRRPInShipmentStockSku, importerShipmentFreightBox, 1, 2, freightFreightUnit, 2, 3);
        currencyRRPDirectImporterFreightSku = addMGProp(idGroup, "currencyRRPDirectImporterFreightSku", true, "Валюта RRP (ИД)", currencyRRPDocumentSku, importerDirectInvoice, 1, freightDirectInvoice, 1, 2);
        currencyRRPInImporterFreightSku = addSUProp(idGroup, "currencyRRPInImporterFreightSku", "Валюта RRP (ИД)", Union.OVERRIDE, currencyRRPDirectImporterFreightSku, currencyRRPProxyImporterFreightSku, currencyRRPImporterFreightSku);
        nameCurrencyRRPImporterFreightSku = addJProp("nameCurrencyRRPImporterFreightSku", "Валюта RRP", BL.Currency.getLCPByName("nameCurrency"), currencyRRPInImporterFreightSku, 1, 2, 3);

        RRPFreightSku = addMGProp(baseGroup, "RRPFreightSku", true, "Цена рекомендованная", RRPInImporterFreightSku, 2, 3);
        RRPFreightArticle = addMGProp(baseGroup, "RRPFreightArticle", true, "Цена рекомендованная", RRPInImporterFreightSku, 2, articleSku, 3);
        RRPImporterFreightArticle = addMGProp("RRPImporterFreightArticle", "Цена рекомендованная", RRPInImporterFreightSku, 1, 2, articleSku, 3);

        currencyRRPImporterFreightArticle = addMGProp(idGroup, "currencyRRPImporterFreightArticle", "Валюта RRP (ИД)", currencyRRPInImporterFreightSku, 1, 2, articleSku, 3);
        nameCurrencyRRPImporterFreightArticle = addJProp(baseGroup, "nameCurrencyRRPImporterFreightArticle", "Валюта RRP", BL.Currency.getLCPByName("nameCurrency"), currencyRRPImporterFreightArticle, 1, 2, 3);
        //currencyRRPFreightArticle = addMGProp(baseGroup, "currencyRRPFreightArticle", true, "Валюта RRP (ИД)", currencyRRPImporterFreightSku, 2, articleSku, 3);

        currencyRRPFreightSku = addMGProp(baseGroup, "currencyRRPFreightSku", true, "Валюта RRP (ИД)", currencyRRPImporterFreightSku, 2, 3);
        currencyRRPFreightArticle = addMGProp(baseGroup, "currencyRRPFreightArticle", true, "Валюта RRP (ИД)", currencyRRPImporterFreightSku, 2, articleSku, 3);

        addConstraint(addJProp("Для SKU должна быть задана входная цена", and(true, false), is(freightPriced), 2, priceInImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), false);

        sumInImporterStockSku = addJProp(baseGroup, "sumInImporterStockSku", "Сумма входная", baseLM.multiply, quantityImporterStockSku, 1, 2, 3, addJProp(priceInImporterFreightSku, 1, freightFreightBox, 2, 3), 1, 2, 3);
        sumInImporterFreightSku = addJProp(baseGroup, "sumInImporterFreightSku", "Сумма входная", baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, priceInImporterFreightSku, 1, 2, 3);

        //contractInProxyImporterStockSku = addMGProp(baseGroup, "contractInProxyImporterStockSku", true, "Договор (ИД)", contractInShipmentStockSku, importerShipmentFreightBox, 1, 2, 2, 3);
        //sidContractInProxyImporterStockSku = addJProp(baseGroup, "sidContractInProxyImporterStockSku", "Номер договора", sidContract, contractInProxyImporterStockSku, 1, 2, 3);
        //dateContractInProxyImporterStockSku = addJProp(baseGroup, "dateContractInProxyImporterStockSku", "Дата договора", dateContract, contractInProxyImporterStockSku, 1, 2, 3);

        sumFreightImporterFreightSku = addPGProp(baseGroup, "sumFreightImporterFreightSku", false, 10, false, "Сумма фрахта",
                grossWeightImporterFreightSku,
                sumFreightFreight, 2);

        priceFreightImporterFreightSku = addJProp(baseGroup, "priceFreightImporterFreightSku", true, "Цена за фрахт", divideNumeric2, sumFreightImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        //priceExpenseImporterFreightSku = addJProp(baseGroup, "priceExpenseImporterFreightSku", "Цена затр.", baseLM.sumDouble2, priceInImporterFreightSku, 1, 2, 3, priceFreightImporterFreightSku, 1, 2, 3);

        markupPercentImporterFreightBrandSupplier = addDProp(baseGroup, "markupPercentImporterFreightBrandSupplier", "Надбавка (%)", NumericClass.get(14, 2), importer, freight, brandSupplier);

        markupPercentImporterFreightBrandSupplierArticle = addJProp(baseGroup, "markupPercentImporterFreightBrandSupplierArticle", "Надбавка (%)", baseLM.and1, addJProp(markupPercentImporterFreightBrandSupplier, 1, 2, brandSupplierArticle, 3), 1, 2, 3, quantityImporterFreightArticle, 1, 2, 3);
        markupPercentImporterFreightDataArticle = addDProp(baseGroup, "markupPercentImporterFreightDataArticle", "Надбавка (%)", NumericClass.get(14, 2), importer, freight, article);
        markupPercentImporterFreightArticle = addSUProp(baseGroup, "markupPercentImporterFreightArticle", true, "Надбавка (%)", Union.OVERRIDE, markupPercentImporterFreightBrandSupplierArticle, markupPercentImporterFreightDataArticle);
        markupPercentImporterFreightArticleSku = addJProp(baseGroup, "markupPercentImporterFreightArticleSku", "Надбавка (%)", markupPercentImporterFreightArticle, 1, 2, articleSku, 3);

        markupPercentImporterFreightDataSku = addDProp(baseGroup, "markupPercentImporterFreightDataSku", "Надбавка (%)", NumericClass.get(14, 2), importer, freight, sku);
        markupPercentImporterFreightBrandSupplierSku = addJProp(baseGroup, "markupPercentImporterFreightBrandSupplierSku", true, "Надбавка (%)", baseLM.and1, addJProp(markupPercentImporterFreightBrandSupplier, 1, 2, brandSupplierArticleSku, 3), 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        markupPercentImporterFreightSku = addSUProp(baseGroup, "markupPercentImporterFreightSku", true, "Надбавка (%)", Union.OVERRIDE, markupPercentImporterFreightArticleSku, markupPercentImporterFreightDataSku);

        LCP round0 = addJProp(BL.getModule("Utils").getLCPByName("round"), 1, addCProp(IntegerClass.instance, 0));

        sumPercentImporterFreightBrandSupplier = addSGProp(baseGroup, "sumPercentImporterFreightBrandSupplier", true, "Сумма процентов надбавок", addJProp(baseLM.multiply, markupPercentImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), 1, 2, brandSupplierArticleSku, 3);
        averagePercentImporterFreightBrandSupplier = addJProp(baseGroup, "averagePercentImporterFreightBrandSupplier", "Средний процент надбавки", round0, addJProp(divideNumeric2, sumPercentImporterFreightBrandSupplier, 1, 2, 3, quantityImporterFreightBrandSupplier, 1, 2, 3), 1, 2, 3);

        minPriceCustomCategoryFreightSku = addJProp(baseGroup, "minPriceCustomCategoryFreightSku", "Минимальная цена ($)", minPriceCustomCategory10SubCategory, customCategory10FreightSku, 1, 2, subCategoryFreightSku, 1, 2);
        minPriceCustomCategoryCountryFreightSku = addJProp(baseGroup, "minPriceCustomCategoryCountryFreightSku", "Минимальная цена для страны ($)", minPriceCustomCategory10SubCategoryCountry, customCategory10FreightSku, 1, 2, subCategoryFreightSku, 1, 2, countryOfOriginFreightSku, 1, 2);

        minPriceRateCustomCategoryFreightSku = addJProp(baseGroup, "minPriceRateCustomCategoryFreightSku", true, "Минимальная цена (евро)", round2, addJProp(baseLM.multiply, minPriceCustomCategoryFreightSku, 1, 2, addJProp(BL.getModule("Currency").getLCPByName("rateTypeExchangeCurrencyDate"), typeExchangeCustom, currencyCustom, date, 1), 1), 1, 2);
        minPriceRateCustomCategoryCountryFreightSku = addJProp(baseGroup, "minPriceRateCustomCategoryCountryFreightSku", true, "Минимальная цена (евро)", round2, addJProp(baseLM.multiply, minPriceCustomCategoryCountryFreightSku, 1, 2, addJProp(BL.getModule("Currency").getLCPByName("rateTypeExchangeCurrencyDate"), typeExchangeCustom, currencyCustom, date, 1), 1), 1, 2);

        minPriceRateFreightSku = addSUProp(baseGroup, "minPriceRateFreightSku", "Минимальная цена (евро)", Union.OVERRIDE, minPriceRateCustomCategoryFreightSku, minPriceRateCustomCategoryCountryFreightSku);
        minPriceRateImporterFreightSku = addJProp(baseGroup, "minPriceImporterFreightSku", "Минимальная цена (евро)", baseLM.and1, minPriceRateFreightSku, 2, 3, is(importer), 1);
        minPriceRateImporterFreightArticle = addMGProp(baseGroup, "minPriceRateImporterFreightArticle", true, "Минимальная для артикула за кг", minPriceRateImporterFreightSku, 1, 2, articleSku, 3);

        minPriceRateWeightImporterFreightSku = addJProp(baseGroup, "minPriceRateWeightImporterFreightSku", "Минимальная для веса", round2, addJProp(baseLM.multiply, minPriceRateImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3), 1, 2, 3);

        // sku
        markupInImporterFreightSku = addJProp(baseGroup, "markupInImporterFreightSku", "Надбавка", percentNumeric2, priceInImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);
        priceMarkupInImporterFreightSku = addJProp(baseGroup, "priceMarkupInImporterFreightSku", "Цена выходная", sumNumeric2, priceInImporterFreightSku, 1, 2, 3, markupInImporterFreightSku, 1, 2, 3);

        priceInOutImporterFreightSku = addDProp(baseGroup, "priceInOutImporterFreightSku", "Цена выходная", NumericClass.get(14, 2), importer, freightPriced, sku);
        priceInOutImporterFreightSku.setEventChangeNew(addJProp(baseLM.and1, priceMarkupInImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3), 1, 2, 3, is(freightPriced), 2, markupPercentImporterFreightSku, 1, 2, 3);

        priceImporterFreightArticleCompositionCountryCategory = addMGProp(baseGroup, "priceImporterFreightArticleCompositionCountryCategory", true, "Цена", true,
            priceInOutImporterFreightSku, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        priceInvoiceImporterFreightSku = addJProp(baseGroup, "priceInvoiceImporterFreightSku", true, "Цена в инвойсе",
                priceImporterFreightArticleCompositionCountryCategory, 1, 2, articleSku, 3, mainCompositionOriginFreightSku, 2, 3, countryOfOriginFreightSku, 2, 3, customCategory10FreightSku, 2, 3);

        markupInOutImporterFreightSku = addDUProp(baseGroup, "markupInOutImporterFreightSku", "Надбавка", priceInvoiceImporterFreightSku, priceInImporterFreightSku);

        sumInvoiceImporterStockSku = addJProp(baseGroup, "sumInvoiceImporterStockSku", "Сумма", baseLM.multiply, quantityImporterStockSku, 1, 2, 3, addJProp(priceInvoiceImporterFreightSku, 1, freightFreightBox, 2, 3), 1, 2, 3);

        sumImporterFreightSku = addJProp(baseGroup, "sumImporterFreightSku", "Сумма", baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumProxyImporterFreightSku = addJProp(baseGroup, "sumProxyImporterFreightSku", "Сумма", baseLM.multiply, quantityProxyImporterFreightSku, 1, 2, 3, priceInvoiceImporterFreightSku, 1, 2, 3);
        sumDirectImporterFreightSku = addJProp(baseGroup, "sumDirectImporterFreightSku", "Сумма", baseLM.multiply, quantityDirectImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumImporterFreightCustomCategory6 = addSGProp(baseGroup, "sumImporterFreightCustomCategory6", true, "Сумма", sumProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3);
        sumImporterFreightCustomCategory6Category = addSGProp(baseGroup, "sumImporterFreightCustomCategory6Category", "Сумма", sumProxyImporterFreightSku, 1, 2, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        sumImporterFreightSupplierCustomCategory6Category = addSGProp(baseGroup, "sumImporterFreightSupplierCustomCategory6Category", "Сумма", sumDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3, categoryArticleSku, 3);
        sumImporterFreightSupplierCustomCategory6 = addSGProp(baseGroup, "sumImporterFreightSupplierCustomCategory6", true, "Сумма", sumDirectImporterFreightSku, 1, 2, supplierArticleSku, 3, customCategory6FreightSku, 2, 3);

        sumImporterFreightUnitSku = addJProp(baseGroup, "sumImporterFreightUnitSku", "Сумма", baseLM.multiply, quantityImporterStockSku, 1, 2, 3, addJProp(priceInOutImporterFreightSku, 1, freightFreightUnit, 2, 3), 1, 2, 3);

        sumImporterFreightUnitArticle = addSGProp(baseGroup, "sumImporterFreightUnitArticle", "Сумма", sumImporterFreightUnitSku, 1, 2, articleSku, 3);

        sumImporterFreightArticleCompositionCountryCategory = addJProp(baseGroup, "sumImporterFreightArticleCompositionCountryCategory", "Сумма", baseLM.multiply,
                quantityImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6,
                priceImporterFreightArticleCompositionCountryCategory, 1, 2, 3, 4, 5, 6);

        sumProxyInvoiceImporterFreightSku = addJProp(baseGroup, "sumProxyInvoiceImporterFreightSku", "Сумма в инвойсе", baseLM.multiply,
                quantityProxyImporterFreightSku, 1, 2, 3,
                priceInvoiceImporterFreightSku, 1, 2, 3);

        ////sumImporterFreight = addSGProp(baseGroup, "sumImporterFreight", true, "Сумма выходная", sumImporterFreightArticleCompositionCountryCategory, 1, 2);
        sumImporterFreight = addSGProp(baseGroup, "sumImporterFreight", true, "Сумма выходная", sumImporterFreightSku, 1, 2);

        sumImporterFreightTypeInvoice = addSGProp(baseGroup, "sumImporterFreightTypeInvoice", "Сумма выходная", sumProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);

        sumSbivkaImporterFreight = addSGProp(baseGroup, "sumSbivkaImporterFreight", "Сумма выходная", sumProxyImporterFreightSku, 1, 2);
        sumImporterFreightSupplier = addSGProp(baseGroup, "sumImporterFreightSupplier", "Сумма выходная", sumDirectImporterFreightSku, 1, 2, supplierArticleSku, 3);

        sumMarkupInImporterFreightSku = addJProp(baseGroup, "sumMarkupInImporterFreightSku", "Сумма надбавки", baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, markupInOutImporterFreightSku, 1, 2, 3);
        sumInOutProxyImporterFreightSku = addJProp(baseGroup, "sumInOutProxyImporterFreightSku", "Сумма выходная", baseLM.multiply, quantityProxyImporterFreightSku, 1, 2, 3, priceInvoiceImporterFreightSku, 1, 2, 3);
        sumInOutDirectImporterFreightSku = addJProp(baseGroup, "sumInOutDirectImporterFreightSku", "Сумма выходная", baseLM.multiply, quantityDirectImporterFreightSku, 1, 2, 3, priceInOutImporterFreightSku, 1, 2, 3);
        sumInOutImporterFreightSku = addSUProp(baseGroup, "sumInOutImporterFreightSku", true, "Сумма выходная", Union.SUM, sumInOutProxyImporterFreightSku, sumInOutDirectImporterFreightSku);

        sumInImporterFreightArticle = addSGProp(baseGroup, "sumInImporterFreightArticle", true, "Итого по ценам поставщика (импортер)", sumInImporterFreightSku, 1, 2, articleSku, 3);

        sumInImporterFreightBrandSupplier = addSGProp(baseGroup, "sumInImporterFreightBrandSupplier", true, "Итого по ценам поставщика (импортер)", sumInImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        sumInOutImporterFreightArticle = addSGProp(baseGroup, "sumInOutImporterFreightArticle", true, "Итого по отгрузочным ценам (импортер)", sumInOutImporterFreightSku, 1, 2, articleSku, 3);
        sumInOutImporterFreightBrandSupplier = addSGProp(baseGroup, "sumInOutImporterFreightBrandSupplier", true, "Итого по отгрузочным ценам (импортер)", sumInOutImporterFreightSku, 1, 2, brandSupplierArticleSku, 3);

        sumInFreightArticle = addSGProp(baseGroup, "sumInFreightArticle", true, "Итого по ценам поставщика", sumInImporterFreightSku, 2, articleSku, 3);
        sumInFreightBrandSupplier = addSGProp(baseGroup, "sumInFreightBrandSupplier", true, "Итого по ценам поставщика", sumInImporterFreightSku, 2, brandSupplierArticleSku, 3);

        sumInOutFreightArticle = addSGProp(baseGroup, "sumInOutFreightArticle", true, "Итого по отгрузочным ценам", sumInOutImporterFreightSku, 2, articleSku, 3);
        sumInOutFreightBrandSupplier = addSGProp(baseGroup, "sumInOutFreightBrandSupplier", true, "Итого по отгрузочным ценам", sumInOutImporterFreightSku, 2, brandSupplierArticleSku, 3);
        sumInOutFreightBrandSupplierArticle = addJProp(baseGroup, "sumInOutFreightBrandSupplierArticle", "Сумма по бренду", sumInOutFreightBrandSupplier, 1, brandSupplierArticle, 2);
        sumInOutFreightBrandSupplierSku = addJProp(baseGroup, "sumInOutFreightBrandSupplierSku", "Сумма по бренду", sumInOutFreightBrandSupplier, 1, brandSupplierArticleSku, 2);

        sumInOutImporterFreight = addSGProp(baseGroup, "sumInOutImporterFreight", true, "Сумма выходная", sumInOutImporterFreightSku, 1, 2);
        sumInOutFreight = addSGProp(baseGroup, "sumInOutFreight", true, true, "Сумма выходная", sumInOutImporterFreight, 2);
        // временно так пока система сама не научится либо обнаруживать равные свойства, либо решать проблему с инкрементностью по другому
        insuranceImporterFreightSku = addJProp("insuranceImporterFreightSku", true, "Сумма за страховку", addSFProp("ROUND(CAST((prm1*prm2/prm3) as NUMERIC(15,3))," + 10 + ")", NumericClass.get(14, 2), 3), sumInOutImporterFreightSku, 1, 2, 3, insuranceFreightBrandSupplierSku, 2, 3, sumInOutFreightBrandSupplierSku, 2, 3);
        //addPGProp(baseGroup, "insuranceImporterFreightSku", false, 2, false, "Сумма страховки",
        //        sumInOutImporterFreightSku,
        //        insuranceFreight, 2);

        priceInsuranceImporterFreightSku = addJProp(baseGroup, "priceInsuranceImporterFreightSku", "Цена за страховку", divideNumeric2, insuranceImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);

        priceFreightInsuranceImporterFreightSku = addSUProp(baseGroup, "priceFreightInsuranceImporterFreightSku", "Расходы", Union.SUM, priceFreightImporterFreightSku, priceInsuranceImporterFreightSku);

        priceFullImporterFreightSku = addJProp(baseGroup, "priceFullImporterFreightSku", true, "Цена с расходами", baseLM.and1, addSUProp(Union.SUM, priceInvoiceImporterFreightSku, priceFreightInsuranceImporterFreightSku), 1, 2, 3, is(freightPriced), 2);

        priceFreightInsuranceFreightSku = addMGProp(baseGroup, "priceFreightInsuranceFreightSku", "Расходы", priceFreightInsuranceImporterFreightSku, 2, 3);

        priceInFullImporterFreightSku = addJProp(baseGroup, "priceInFullImporterFreightSku", true, "Цена поставщика с расходами", baseLM.and1, addSUProp(Union.SUM, priceInImporterFreightSku, priceFreightInsuranceImporterFreightSku), 1, 2, 3, is(freightPriced), 2);

        priceInFullFreightSku = addMGProp(baseGroup, "priceInFullFreightSku", true, "Цена поставщика с расходами", priceInFullImporterFreightSku, 2, 3);

        priceFullKgImporterFreightSku = addJProp(baseGroup, "priceFullKgImporterFreightSku", "Цена за кг", divideNumeric2, priceFullImporterFreightSku, 1, 2, 3, netWeightFreightSku, 2, 3);

        sumFullImporterFreightArticle = addSGProp(baseGroup, "sumFullImporterFreightArticle", true, "Сумма", addJProp(baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, priceFullImporterFreightSku, 1, 2, 3), 1, 2, articleSku, 3);

        priceFullKgImporterFreightArticle = addJProp(baseGroup, "priceFullKgImporterFreightArticle", "Цена за кг", divideNumeric2, sumFullImporterFreightArticle, 1, 2, 3, netWeightImporterFreightArticle, 1, 2, 3);

        diffPriceMinPriceImporterFreightArticle = addDUProp(baseGroup, "diffPriceMinPriceImporterFreightArticle", "Разница цен", minPriceRateImporterFreightArticle, priceFullKgImporterFreightArticle);
        greaterPriceMinPriceImporterFreightArticle = addJProp(baseGroup, "greaterPriceMinPriceImporterFreightArticle", "Недостаточность цены", baseLM.greater2, diffPriceMinPriceImporterFreightArticle, 1, 2, 3, baseLM.vzero);

        diffPriceMinPriceImporterFreightSku = addDUProp(baseGroup, "diffPriceMinPriceImporterFreightSku", "Разница цен", minPriceRateImporterFreightSku, priceFullKgImporterFreightSku);
        greaterPriceMinPriceImporterFreightSku = addJProp(baseGroup, "greaterPriceMinPriceImporterFreightSku", "Недостаточность цены", baseLM.greater2, diffPriceMinPriceImporterFreightSku, 1, 2, 3, baseLM.vzero);

        dutyNetWeightFreightSku = addJProp(baseGroup, "dutyNetWeightFreightSku", "Пошлина по весу нетто", baseLM.multiply, netWeightFreightSku, 1, 2, addJProp(dutySumCustomCategory10TypeDuty, customCategory10FreightSku, 1, 2, typeDutyDuty), 1, 2);
        dutyNetWeightImporterFreightSku = addJProp(baseGroup, "dutyNetWeightImporterFreightSku", "Пошлина по весу нетто", baseLM.and1, dutyNetWeightFreightSku, 2, 3, is(importer), 1);

        dutyPercentImporterFreightSku = addJProp(baseGroup, "dutyPercentImporterFreightSku", "Пошлина по цене", percentNumeric2, priceFullImporterFreightSku, 1, 2, 3, addJProp(dutyPercentCustomCategory10TypeDuty, customCategory10FreightSku, 2, 3, typeDutyDuty), 1, 2, 3);

        dutyImporterFreightSku = addJProp(baseGroup, "dutyImporterFreightSku", true, "Пошлина", and(false, false), addSUProp(Union.MAX, dutyNetWeightImporterFreightSku, dutyPercentImporterFreightSku), 1, 2, 3, is(freightPriced), 2, quantityImporterFreightSku, 1, 2, 3);
        priceDutyImporterFreightSku = addJProp(baseGroup, "priceDutyImporterFreightSku", "Сумма пошлины", sumNumeric2, dutyImporterFreightSku, 1, 2, 3, priceInvoiceImporterFreightSku, 1, 2, 3);

        priceFullDutyImporterFreightSku = addSUProp(baseGroup, "priceFullDutyImporterFreightSku", "Цена с пошлиной", Union.SUM, priceFullImporterFreightSku, dutyImporterFreightSku);
        priceInFullDutyImporterFreightSku = addSUProp(baseGroup, "priceInFullDutyImporterFreightSku", "Цена с пошлиной (УУ)", Union.SUM, priceInFullImporterFreightSku, dutyImporterFreightSku);

        sumDutyImporterFreightSku = addJProp(baseGroup, "sumDutyImporterFreightSku", "Сумма пошлины", baseLM.multiply, dutyImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        sumDutyImporterFreight = addSGProp(baseGroup, "sumDutyImporterFreight", true, "Сумма пошлины", sumDutyImporterFreightSku, 1, 2);

        NDSPercentOriginFreightSku = addJProp(baseGroup, "NDSPercentOriginFreightSku", true, "НДС (%)", dutyPercentCustomCategory10TypeDuty, customCategory10FreightSku, 1, 2, typeDutyNDS);
        NDSPercentCustomFreightSku = addJProp(baseGroup, "NDSPercentCustomFreightSku", "НДС (%)", and(false, false), NDSPercentCustom, is(freight), 1, is(sku), 2);
        NDSPercentFreightSku = addSUProp(baseGroup, "NDSPercentFreightSku", "НДС (%)", Union.OVERRIDE, NDSPercentCustomFreightSku, NDSPercentOriginFreightSku);
        NDSImporterFreightSku = addJProp(baseGroup, "NDSImporterFreightSku", true, "НДС (евро)", percentNumeric2, priceFullDutyImporterFreightSku, 1, 2, 3, NDSPercentFreightSku, 2, 3);

        priceFullDutyNDSImporterFreightSku = addSUProp(baseGroup, "priceFullDutyNDSImporterFreightSku", "Цена с расходами", Union.SUM, priceInFullDutyImporterFreightSku, NDSImporterFreightSku);
        priceFullDutyNDSFreightSku = addMGProp(baseGroup, "priceFullDutyNDSFreightSku", true, "Цена с расходами", priceFullDutyNDSImporterFreightSku, 2, 3);

        sumNDSImporterFreightSku = addJProp(baseGroup, "sumNDSImporterFreightSku", "Сумма НДС", baseLM.multiply, NDSImporterFreightSku, 1, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        sumNDSImporterFreight = addSGProp(baseGroup, "sumNDSImporterFreight", true, "Сумма НДС", sumNDSImporterFreightSku, 1, 2);

        sumRegistrationFreightSku = addJProp(baseGroup, "sumRegistrationFreightSku", "Таможенный сбор", dutySumCustomCategory10TypeDuty, customCategory10FreightSku, 1, 2, typeDutyRegistration);
        sumRegistrationImporterFreightSku = addJProp(baseGroup, "sumRegistrationImporterFreightSku", "Таможенный сбор", baseLM.and1, sumRegistrationFreightSku, 2, 3, quantityImporterFreightSku, 1, 2, 3);
        sumRegistrationImporterFreight = addMGProp(baseGroup, "sumRegistrationImporterFreight", true, "Таможенный сбор", sumRegistrationImporterFreightSku, 1, 2);

        sumCustomImporterFreight = addSUProp(baseGroup, "sumCustomImporterFreight", "Итого по таможенным платежам", Union.SUM, sumDutyImporterFreight, sumNDSImporterFreight, sumRegistrationImporterFreight);
        sumCustomImporterFreight.property.preferredCharWidth = 35;

        sumMarkupInImporterFreight = addSGProp(baseGroup, "sumMarkupInImporterFreight", true, "Сумма надбавки", sumMarkupInImporterFreightSku, 1, 2);

        sumInOutImporterFreightTypeInvoice = addSGProp(baseGroup, "sumInOutImporterFreightTypeInvoice", true, "Сумма выходная", sumInOutProxyImporterFreightSku, 1, 2, typeInvoiceFreightSku, 2, 3);

        sumMarkupInFreight = addSGProp(baseGroup, "sumMarkupInFreight", true, "Сумма надбавки", sumMarkupInImporterFreight, 2);

        //markupImporterFreightSku = addJProp(baseGroup, "markupImporterFreightSku", "Надбавка", baseLM.percent2, priceExpenseImporterFreightSku, 1, 2, 3, markupPercentImporterFreightSku, 1, 2, 3);
        //sumMarkupImporterFreightSku = addJProp(baseGroup, "sumMarkupImporterFreightSku", "Сумма надбавки", baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, markupInOutImporterFreightSku, 1, 2, 3);

        //priceOutImporterFreightSku = addJProp(baseGroup, "priceOutImporterFreightSku", "Цена выходная", baseLM.sum, priceExpenseImporterFreightSku, 1, 2, 3, markupImporterFreightSku, 1, 2, 3);
        //sumOutImporterFreightSku = addJProp(baseGroup, "sumOutImporterFreightSku", "Сумма выходная", baseLM.multiply, quantityImporterFreightSku, 1, 2, 3, priceOutImporterFreightSku, 1, 2, 3);

        sumInImporterFreight = addSGProp(baseGroup, "sumInImporterFreight", true, "Сумма входная", sumInImporterFreightSku, 1, 2);
        //sumMarkupImporterFreight = addSGProp(baseGroup, "sumMarkupImporterFreight", true, "Сумма надбавки", sumMarkupImporterFreightSku, 1, 2);
        //sumOutImporterFreight = addSGProp(baseGroup, "sumOutImporterFreight", true, "Сумма выходная", sumOutImporterFreightSku, 1, 2);

        sumInFreight = addSGProp(baseGroup, "sumInFreight", true, "Сумма входная", sumInImporterFreight, 2);
        //sumMarkupFreight = addSGProp(baseGroup, "sumMarkupFreight", true, "Сумма надбавки", sumMarkupImporterFreight, 2);
        //sumOutFreight = addSGProp(baseGroup, "sumOutFreight", true, "Сумма выходная", sumOutImporterFreight, 2);

        // итоги с начала года
        sumInCurrentYear = addSGProp(baseGroup, "sumInCurrentYear", "Итого вход", addJProp(baseLM.and1, sumInFreight, 1, addJProp(baseLM.equals2, addJProp(BL.timeLM.yearInDate, BL.timeLM.currentDate), addJProp(BL.timeLM.yearInDate, date, 1), 1), 1));
        sumInOutCurrentYear = addSGProp(baseGroup, "sumInOutCurrentYear", "Итого выход", addJProp(baseLM.and1, sumInOutFreight, 1, addJProp(baseLM.equals2, addJProp(BL.timeLM.yearInDate, BL.timeLM.currentDate), addJProp(BL.timeLM.yearInDate, date, 1), 1), 1));
        balanceSumCurrentYear = addDUProp(baseGroup, "balanceSumCurrentYear", "Сальдо", sumInOutCurrentYear, sumInCurrentYear);

        currentFreightBoxRouteUser = addDProp("currentFreightBoxRouteUser", "Тек. короб (ИД)", freightBox, route, BL.authenticationLM.user);

        currentFreightBoxRoute = addJProp(true, "currentFreightBoxRoute", "Тек. короб (ИД)", currentFreightBoxRouteUser, 1, BL.authenticationLM.currentUser);
        barcodeCurrentFreightBoxRoute = addJProp("barcodeCurrentFreightBoxRoute", "Тек. короб (штрих-код)", barcode, currentFreightBoxRoute, 1);

        destinationCurrentFreightBoxRoute = addJProp(true, "destinationCurrentFreightBoxRoute", "Пункт назначения тек. короба (ИД)", destinationFreightBox, currentFreightBoxRoute, 1);
        nameDestinationCurrentFreightBoxRoute = addJProp("nameDestinationCurrentFreightBoxRoute", "Пункт назначения тек. короба", name, destinationCurrentFreightBoxRoute, 1);

        isCurrentFreightBox = addJProp(baseLM.equals2, addJProp(true, currentFreightBoxRoute, routeCreationFreightBoxFreightBox, 1), 1, 1);
        isCurrentPallet = addJProp(baseLM.equals2, addJProp(true, currentPalletRoute, routeCreationPalletPallet, 1), 1, 1);
        currentPalletFreightBox = addJProp(currentPalletRoute, routeCreationFreightBoxFreightBox, 1);
        isCurrentPalletFreightBox = addJProp(true, baseLM.and1, addJProp(baseLM.equals2, palletFreightBox, 1, currentPalletFreightBox, 1), 1, currentPalletFreightBox, 1);
        isStoreFreightBoxSupplierBox = addJProp(baseLM.equals2, destinationFreightBox, 1, destinationSupplierBox, 2);

        seekRouteShipmentSkuRoute = addAProp(new SeekRouteActionProperty());

        barcodeActionSeekPallet = addSetPropertyAProp("Найти палету", addJProp(true, isCurrentPallet, barcodeToObject, 1), 1, baseLM.vtrue);
        barcodeActionSeekFreightBox = addSetPropertyAProp("Найти короб для транспортировки", addJProp(true, isCurrentFreightBox, barcodeToObject, 1), 1, baseLM.vtrue);
        barcodeActionSetPallet = addSetPropertyAProp("Установить паллету", addJProp(true, isCurrentPalletFreightBox, barcodeToObject, 1), 1, baseLM.vtrue);
        barcodeActionSetStore = addSetPropertyAProp("Установить магазин", addJProp(true, isStoreFreightBoxSupplierBox, barcodeToObject, 1, 2), 1, 2, baseLM.vtrue);

        changePallet = addJProp(true, "Изменить паллету", isCurrentPalletFreightBox, currentFreightBoxRoute, 1);

        barcodeActionSetPalletFreightBox = addSetPropertyAProp("Установить паллету", addJProp(true, equalsPalletFreightBox, barcodeToObject, 1, 2), 1, 2, baseLM.vtrue);

        barcodeActionSetFreight = addSetPropertyAProp("Установить фрахт", addJProp(true, equalsPalletFreight, barcodeToObject, 1, 2), 1, 2, baseLM.vtrue);

        addBoxShipmentDetailBoxShipmentSupplierBoxRouteSku = addJoinAProp("Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockSku, 1, 2, currentFreightBoxRoute, 3, 4);

        addSimpleShipmentDetailSimpleShipmentRouteSku = addJoinAProp("Добавить строку поставки",
                addSimpleShipmentDetailSimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode = addJoinAProp("Добавить строку поставки",
                addBoxShipmentDetailBoxShipmentSupplierBoxStockBarcode, 1, 2, currentFreightBoxRoute, 3, 4);

        addSimpleShipmentDetailSimpleShipmentRouteBarcode = addJoinAProp("Добавить строку поставки",
                addSimpleShipmentSimpleShipmentDetailStockBarcode, 1, currentFreightBoxRoute, 2, 3);

        quantityRouteSku = addJProp(baseGroup, "quantityRouteSku", "Оприход. в короб для транспортировки", quantityStockSku, currentFreightBoxRoute, 1, 2);

        quantitySupplierBoxBoxShipmentRouteSku = addJProp(baseGroup, true, "quantitySupplierBoxBoxShipmentRouteSku", "Кол-во оприход.",
                quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4);
        quantitySimpleShipmentRouteSku = addJProp(baseGroup, true, "quantitySimpleShipmentRouteSku", "Кол-во оприход.",
                quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3);

        barcodePrefix = addDProp(baseGroup, "barcodePrefix", "Префикс штрих-кодов", StringClass.get(13));

        createSku = addGroupGen("Сгенерировать товары", articleSingle, creationSkuSku, quantityCreationSku);
        createFreightBox = addGroupGen("Сгенерировать короба", freightBox, creationFreightBoxFreightBox, quantityCreationFreightBox);
        createPallet = addGroupGen("Сгенерировать паллеты", pallet, creationPalletPallet, quantityCreationPallet);
        createStamp = addAProp(actionGroup, new CreateStampActionProperty());

        LCP isSkuBarcode = addJProp(is(sku), barcodeToObject, 1);
        barcodeActionCheckFreightBox = addListAProp(addIfAProp("Проверка короба для транспортировки", addJProp(baseLM.andNot1, isSkuBarcode, 2, currentFreightBoxRoute, 1), 1, 2,
                addListAProp(addMAProp("Для выбранного маршрута не задан короб для транспортировки", "Поиск по штрих-коду"), baseLM.flowReturn)), 1, 2,
                addIfAProp("Проверка короба для транспортировки (скомплектован)", addJProp(baseLM.and1, isSkuBarcode, 2, addJProp(freightFreightBox, currentFreightBoxRoute, 1), 1), 1, 2,
                        addListAProp(addMAProp("Текущей короб находится в скомплектованном фрахте", "Поиск по штрих-коду"), baseLM.flowReturn)), 1, 2);

        cloneItem = addAProp(new CloneItemActionProperty());

        barcodeAction4 = addJProp(true, "Ввод штрих-кода 4",
                addCUProp(
                        addSCProp(addJProp(true, quantitySupplierBoxBoxShipmentStockSku, 1, 2, currentFreightBoxRoute, 3, 4))
                ), 1, 2, 3, barcodeToObject, 4);
        barcodeAction3 = addJProp(true, "Ввод штрих-кода 3",
                addCUProp(
                        addSCProp(addJProp(true, quantitySimpleShipmentStockSku, 1, currentFreightBoxRoute, 2, 3))
                ), 1, 2, barcodeToObject, 3);
        declarationExport = addDEAProp("declarationExport");
        invoiceExportDbf = addProperty(null, new LAP(new InvoiceExportDbfActionProperty("invoiceExportDbf", "Экспорт в dbf", importer, freight, typeInvoice, this, baseLM)));
        scalesComPort = addDProp(baseGroup, "scalesComPort", "COM-порт весов", IntegerClass.instance, BL.authenticationLM.computer);
        scalesSpeed = addDProp(baseGroup, "scalesSpeed", "Скорость весов", IntegerClass.instance, BL.authenticationLM.computer);
        scannerComPort = addDProp(baseGroup, "scannerComPort", "COM-порт сканера", IntegerClass.instance, BL.authenticationLM.computer);
        scannerSingleRead = addDProp(baseGroup, "scannerSingleRead", "Одно событие на весь штрих-код", LogicalClass.instance, BL.authenticationLM.computer);

        initNavigators();
    }
    
    public LAP addGroupGen(String caption, ConcreteCustomClass customClass, LCP setProp, LCP countProp) {
        return addJoinAProp(caption, addForAProp(false, 2, 2, 3, BL.Utils.getLCPByName("count"), 1, 2, addAAProp(barcode, customClass, barcodePrefix, setProp), 3), countProp, 1, 1);
    }

    public LAP addDEAProp(String sID) {
        return addProperty(null, new LAP(new DeclarationExportActionProperty(sID, "Экспорт декларанта", importer, freight, this, baseLM)));
    }

    @Override
    public void initIndexes() {
        addIndex(barcode);
    }

    private void initNavigators() {

        PanelNavigatorWindow generateToolbar = addWindow(new PanelNavigatorWindow(SwingConstants.HORIZONTAL, "generateToolbar", "Генерация"));
        generateToolbar.titleShown = false;
        generateToolbar.drawRoot = true;
        generateToolbar.drawScrollBars = false;

        generateToolbar.setDockPosition(20, 6, 80, 4);
        baseLM.windows.forms.setDockPosition(20, 10, 80, 89);

        ArticleCompositeEditFormEntity articleCompositeEditForm = new ArticleCompositeEditFormEntity(null, "articleCompositeEditForm", "Артикул (составной)");
        articleComposite.setEditForm(articleCompositeEditForm, articleCompositeEditForm.objArticleComposite);

        ArticleCompositeFormEntity articleCompositeForm = new ArticleCompositeFormEntity(null, "articleCompositeForm", "Артикул (составной)");
        articleComposite.setDialogForm(articleCompositeForm, articleCompositeForm.objArticleComposite);

        ColorSupplierFormEntity colorSupplierForm = new ColorSupplierFormEntity(null, "colorSupplierForm", "Цвет поставщика");
        colorSupplier.setDialogForm(colorSupplierForm, colorSupplierForm.objColor);

        SizeSupplierFormEntity sizeSupplierForm = new SizeSupplierFormEntity(null, "sizeSupplierForm", "Размер поставщика");
        sizeSupplier.setDialogForm(sizeSupplierForm, sizeSupplierForm.objSize);

        ThemeSupplierFormEntity themeSupplierForm = new ThemeSupplierFormEntity(null, "themeSupplierForm", "Тема поставщика");
        themeSupplier.setDialogForm(themeSupplierForm, themeSupplierForm.objTheme);

        GenderSupplierFormEntity genderSupplierForm = new GenderSupplierFormEntity(null, "genderSuppliers", "Пол поставщика");
        genderSupplier.setDialogForm(genderSupplierForm, genderSupplierForm.objGender);

        CommonSizeFormEntity commonSizeForm = addFormEntity(new CommonSizeFormEntity(null, "commonSizeForm", "Унифицированный размер"));
        commonSize.setDialogForm(commonSizeForm, commonSizeForm.objCommonSize);

        createItemForm = addFormEntity(new CreateItemFormEntity(null, "createItemForm", "Ввод товара"));
        editItemForm = addFormEntity(new EditItemFormEntity(null, "editItemForm", "Редактирование товара"));
        findItemFormBox = addFormEntity(new FindItemFormEntity(null, "findItemFormBox", "Поиск товара (с коробами)", true, false));
        findItemFormSimple = addFormEntity(new FindItemFormEntity(null, "findItemFormSimple", "Поиск товара", false, false));
        findItemFormBoxBarcode = addFormEntity(new FindItemFormEntity(null, "findItemFormBoxBarcode", "Поиск товара (с коробами и выбором штрих-кода)", true, true));
        findItemFormSimpleBarcode = addFormEntity(new FindItemFormEntity(null, "findItemFormSimpleBarcode", "Поиск товара (с выбором штрих-кода)", false, true));

        addFormEntity(new AnnexInvoiceFormEntity(null, "annexInvoiceForm", "Приложение к инвойсу", false));
        invoiceFromFormEntity = addFormEntity(new AnnexInvoiceFormEntity(null, "annexInvoiceForm2", "Приложение к инвойсу (перевод)", true));
        addFormEntity(new InvoiceFromFormEntity(null, "invoiceFromForm", "Исходящие инвойсы", false));
        addFormEntity(new InvoiceFromFormEntity(null, "invoiceFromForm2", "Исходящие инвойсы (перевод)", true));
        addFormEntity(new DeclarantFormEntity(null, "declarantForm", "Экспорт в деларант"));
        addFormEntity(new ProformInvoiceFormEntity(null, "proformInvoiceForm", "Исходящие инвойсы-проформы", false));
        addFormEntity(new ProformInvoiceFormEntity(null, "proformInvoiceForm2", "Исходящие инвойсы-проформы (перевод)", true));
        addFormEntity(new SbivkaFormEntity(null, "sbivkaForm", "Сбивка товаров"));
        addFormEntity(new SbivkaSupplierFormEntity(null, "sbivkaSupplierForm", "Сбивка товаров поставщика"));
        addFormEntity(new PackingListFormEntity(null, "packingListForm", "Исходящие упаковочные листы", false));
        addFormEntity(new PackingListFormEntity(null, "packingListForm2", "Исходящие упаковочные листы (перевод)", true));
        addFormEntity(new PackingListBoxFormEntity(null, "packingListBoxForm", "Упаковочные листы коробов"));
        addFormEntity(new ListFreightUnitFreightFormEntity(null, "listFreightUnitFreightForm", "Список коробов"));
        addFormEntity(new PrintSkuFormEntity(null, "printSkuForm", "Товар"));

        FormEntity createSkuForm = addFormEntity(new CreateSkuFormEntity(null, "createSkuForm", "Штрих-коды товаров", FormType.PRINT));
        FormEntity createPalletForm = addFormEntity(new CreatePalletFormEntity(null, "createPalletForm", "Штрих-коды паллет", FormType.PRINT));
        FormEntity createFreightBoxForm = addFormEntity(new CreateFreightBoxFormEntity(null, "createFreightBoxForm", "Штрих-коды коробов", FormType.PRINT));

        invoiceExportForm = addFormEntity(new InvoiceExportFormEntity(null, "invoiceExportForm", "Экспорт инвойсов"));

        addFormEntity(new ColorSizeSupplierFormEntity(null, "colorSizeSupplierForm", "Поставщики"));

        NavigatorElement orders = addNavigatorElement((NavigatorElement)null, "orders", "Заказы");
        addFormEntity(new PricatFormEntity(orders, "pricatForm", "Прикаты"));

        addFormEntity(new OrderEditFormEntity(orders, "orderAddForm", "Создать заказ", false)).modalityType = ModalityType.FULLSCREEN_MODAL;

        addFormEntity(new OrderEditFormEntity(null, "orderEditForm", "Редактировать заказ", true));
        addFormEntity(new OrderFormEntity(orders, "orderForm", "Заказы"));

        NavigatorElement invoices = addNavigatorElement((NavigatorElement)null, "invoices", "Инвойсы");

        addFormEntity(new InvoiceEditFormEntity(null, "boxInvoiceEditForm", "Редактировать инвойс по коробам", true, true));
        addFormEntity(new InvoiceEditFormEntity(null, "simpleInvoiceEditForm", "Редактировать инвойс без коробов", false, true));
        addFormEntity(new InvoiceFormEntity(invoices, "boxInvoiceForm", "Инвойсы по коробам", true));
        addFormEntity(new InvoiceFormEntity(invoices, "simpleInvoiceForm", "Инвойсы без коробов", false));
        addFormEntity(new InvoiceShipmentFormEntity(invoices, "invoiceShipmentForm", "Сравнение по инвойсам"));

        NavigatorElement shipments = addNavigatorElement((NavigatorElement)null, "shipments", "Поставки");
        addFormEntity(new ShipmentListFormEntity(shipments, "boxShipmentListForm", "Поставки по коробам", true));
        addFormEntity(new ShipmentListFormEntity(shipments, "simpleShipmentListForm", "Поставки без коробов", false));
        addFormEntity(new ShipmentExportFormEntity(shipments, "shipmentExportForm", "Экспорт поставки"));

        addFormEntity(new SkuFormEntity(null, "skus", "Товары"));

        NavigatorElement distribution = addNavigatorElement(baseLM.root, "distribution", "Sintitex");
        distribution.window = baseLM.windows.toolbar;
        distribution.setImage("/images/germany2.png");

        NavigatorElement distributionClassifier = addNavigatorElement(distribution, "distributionClassifier", "Справочники");
        distributionClassifier.add(exporter.getListForm(baseLM).form);

        NavigatorElement distributionDocument = addNavigatorElement(distribution, "distributionDocument", "Документы");

        NavigatorElement preparation = addNavigatorElement(distributionDocument, "preparation", "Подготовка к приемке");

        NavigatorElement generationSintitex = addNavigatorElement(preparation, "generationSintitex", "Генерация");
        generationSintitex.window = generateToolbar;

        FormEntity createPalletFormCreate = addFormEntity(new CreatePalletFormEntity(generationSintitex, "createPalletFormAdd", "Сгенерировать паллеты", FormType.ADD));
        createPalletFormCreate.modalityType = ModalityType.MODAL;
        FormEntity createFreightBoxFormAdd = addFormEntity(new CreateFreightBoxFormEntity(generationSintitex, "createFreightBoxFormAdd", "Сгенерировать короба", FormType.ADD));
        createFreightBoxFormAdd.modalityType = ModalityType.MODAL;
        FormEntity createSkuFormAdd = addFormEntity(new CreateSkuFormEntity(generationSintitex, "createSkuFormAdd", "Сгенерировать товары", FormType.ADD));
        createSkuFormAdd.modalityType = ModalityType.MODAL;
        FormEntity createStampFormAdd = addFormEntity(new CreateStampFormEntity(generationSintitex, "createStampFormAdd", "Сгенерировать марки", FormType.ADD));
        createStampFormAdd.modalityType = ModalityType.MODAL;

        addFormEntity(new CreatePalletFormEntity(preparation, "createPalletFormList", "Паллеты", FormType.LIST));
        addFormEntity(new CreateFreightBoxFormEntity(preparation, "createFreightBoxFormList", "Короба", FormType.LIST));
        addFormEntity(new CreateSkuFormEntity(preparation, "createSkuFormList", "Товары", FormType.LIST));
        addFormEntity(new CreateStampFormEntity(preparation, "createStampFormList", "Марки", FormType.LIST));

        NavigatorElement acceptance = addNavigatorElement(distributionDocument, "acceptance", "Приемка");
        addFormEntity(new ShipmentSpecFormEntity(acceptance, "boxShipmentSpecForm", "Прием товара по коробам", true));
        addFormEntity(new ShipmentSpecFormEntity(acceptance, "simpleShipmentSpecForm", "Прием товара без коробов", false));

        NavigatorElement placing = addNavigatorElement(distributionDocument, "placing", "Распределение");
        addFormEntity(new BoxPalletStoreFormEntity(placing, "boxPalletStoreForm", "Распределение коробов по паллетам"));
        addFormEntity(new FreightShipmentStoreFormEntity(placing, "freightShipmentStoreForm", "Распределение паллет по фрахтам"));

        NavigatorElement distributionReport = addNavigatorElement(distribution, "distributionReport", "Сводная информация");
        addFormEntity(new BalanceBrandWarehouseFormEntity(distributionReport, "balanceBrandWarehouseForm", "Остатки на складе (по брендам)"));
        addFormEntity(new BalanceWarehouseFormEntity(distributionReport, "balanceWarehouseForm", "Остатки на складе"));
        addFormEntity(new BalanceWarehousePeriodFormEntity(distributionReport, "balanceWarehousePeriodForm", "Движение товара за период"));

        logFreightForm = new LogFormEntity("logFreightForm", "История фрахта", nameClassFreight, logFreight, BL.systemEventsLM, false);
        formLogFreight = addMFAProp("История фрахта", logFreightForm, logFreightForm.params);
        formLogFreight.setImage("history.png");

        //NavigatorElement actionFreight = addNavigatorElement(shipmentDocument, "actionFreight", "Действия");
        addFormEntity(new FreightShipmentFormEntity(null, "freightShipmentForm", "Комплектация фрахта"));
        addFormEntity(new FreightChangeFormEntity(null, "freightChangeForm", "Обработка фрахта"));
        addFormEntity(new FreightInvoiceFormEntity(null, "freightInvoiceForm", "Расценка фрахта"));

        addFormEntity(new FreightListFormEntity(null, "freightListForm", "Фрахты"));

        addFormEntity(new FreightReportFormEntity(null, "freightReportForm", "Отчёт по фрахту"));
        addFormEntity(new FreightBoxContentFormEntity(null, "freightBoxContentForm", "Содержимое короба"));

        NavigatorElement customs = addNavigatorElement(baseLM.root, "customs", "Таможенное оформление");
        customs.window = baseLM.windows.toolbar;
        customs.setImage("/images/cop.png");

        NavigatorElement customClassifier = addNavigatorElement(customs, "customClassifier", "Справочники");

        NavigatorElement logistics = addNavigatorElement(baseLM.root, "logistics", "Логистика");
        logistics.window = baseLM.windows.toolbar;
        logistics.setImage("/images/arrows.png");

        addFormEntity(new CommonSizeEditFormEntity(null, "commonEditSizeForm", "Белорусские размеры"));
        addFormEntity(new CommonSizeImportFormEntity(null, "commonImportSizeForm", "Белорусские размеры (таблицей)"));

        baseLM.root.add(baseLM.administration);
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    public ConcreteCustomClass getCountryClass() {
        return (ConcreteCustomClass) BL.getModule("Country").getClassByName("Country");
    }

    public ConcreteCustomClass getCurrencyClass() {
        return (ConcreteCustomClass) BL.getModule("Currency").getClassByName("Currency");
    }

    public ConcreteCustomClass getTypeExchangeClass() {
        return (ConcreteCustomClass) BL.getModule("Currency").getClassByName("TypeExchange");
    }

    public LCP getSidOrigin2Country() {
        return BL.getModule("Country").getLCPByName("sidOrigin2Country");
    }

    private class
            BarcodeFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objBarcode;

        protected Font getDefaultFont() {
            return null;
        }

        private BarcodeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

//            objBarcode.resetOnApply = true;

            getPropertyDraw(baseLM.objectValue, objBarcode).eventID = ScannerDaemonTask.SCANNER_SID;

            addPropertyDraw(reverseBarcode);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {

            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (getDefaultFont() != null)
                design.setFont(getDefaultFont());

            PropertyDrawView barcodeView = design.get(getPropertyDraw(baseLM.objectValue, objBarcode));

            design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(getPropertyDraw(reverseBarcode)));
//            design.getPanelContainer(design.get(objBarcode.groupTo)).constraints.maxVariables = 0;

//            design.setBackground(barcodeObjectName, new Color(240, 240, 240));

            design.setEditKey(barcodeView, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
            design.setEditKey(reverseBarcode, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));

            design.setFocusable(reverseBarcode, false);
            design.setFocusable(false, objBarcode.groupTo);

            return design;
        }
    }

    private class CategoryFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCategory;

        private CategoryFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCategory = addSingleGroupObject("c", category, "Номенклатурная группа", baseGroup);
            setEditType(PropertyEditType.READONLY);

            addFormActions(this, objCategory);

            addDefaultOrder(name, true);
        }
    }

    private class ArticleCompositeEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objArticleComposite;

        public ArticleCompositeEditFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objArticleComposite = addSingleGroupObject(articleComposite, "Артикул", sidArticle, nameSupplierArticle, nameBrandSupplierArticle);
            objArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            addDefaultOrder(sidArticle, true);
        }
    }


    private class ArticleCompositeFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objArticleComposite;

        public ArticleCompositeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objArticleComposite = addSingleGroupObject(articleComposite, "Артикул", sidArticle, nameSupplierArticle, nameBrandSupplierArticle, nameCategoryArticle, mainCompositionOriginArticle);
            setEditType(PropertyEditType.READONLY);

            addFormActions(this, objArticleComposite);

            addDefaultOrder(sidArticle, true);
        }
    }


    private class ColorSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objColor;

        public ColorSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objColor = addSingleGroupObject(colorSupplier, "Цвет поставщика", sidColorSupplier, name, nameSupplierColorSupplier);
            setEditType(PropertyEditType.READONLY);

            addFormActions(this, objColor);

            addDefaultOrder(sidColorSupplier, true);
        }
    }

    private class GenderSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objGender;

        public GenderSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objGender = addSingleGroupObject('g', genderSupplier, "Пол поставщика", sidGenderSupplier, nameSupplierGenderSupplier/*, nameGenderSupplierSku*/);
            setEditType(PropertyEditType.READONLY);

            addFormActions(this, objGender);

            addDefaultOrder(sidGenderSupplier, true);
        }
    }

     private class ThemeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objTheme;

        public ThemeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objTheme = addSingleGroupObject(themeSupplier, "Тема поставщика", sidThemeSupplier, name, nameSupplierThemeSupplier);
            setEditType(PropertyEditType.READONLY);

            addFormActions(this, objTheme);

            addDefaultOrder(sidThemeSupplier, true);
        }
    }

    private class SizeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSize;

        public SizeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSize = addSingleGroupObject(sizeSupplier, "Размер поставщика", sidSizeSupplier, nameSupplierSizeSupplier, orderSizeSupplier);
            setEditType(PropertyEditType.READONLY);

            addFormActions(this, objSize);

            addDefaultOrder(orderSizeSupplier, true);
        }
    }

    private class CommonSizeFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCommonSize;

        private CommonSizeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCommonSize = addSingleGroupObject(commonSize, "Унифицированный размер", name);
            objCommonSize.groupTo.initClassView = ClassViewType.GRID;
            addObjectActions(this, objCommonSize);

            addDefaultOrder(name, true);
        }
    }

    private class PackingListBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objBox;
        private ObjectEntity objArticle;

        private PackingListBoxFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objBox = addSingleGroupObject(1, "box", freightBox, "Короб", nameDestinationFreightBox, barcode, netWeightStock, quantityStock);
            objBox.groupTo.initClassView = ClassViewType.PANEL;

            objArticle = addSingleGroupObject(2, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle, nameCategoryArticle);

            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            addPropertyDraw(netWeightStockArticle, objBox, objArticle);
            objArticle.groupTo.initClassView = ClassViewType.GRID;

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockArticle, objBox, objArticle)));

            packingListFormFreightBox = addFAProp("Упаковочный лист", this, objBox);
            packingListFormRoute = addJoinAProp("packingListFormRoute", "Упаковочный лист", packingListFormFreightBox, currentFreightBoxRoute, 1);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objBox.groupTo).grid.constraints.fillVertical = 2;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            return design;
        }
    }


    private class OrderEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean edit;

        private ObjectEntity objOrder;
        private ObjectEntity objSIDArticleComposite;
        private ObjectEntity objSIDColorSupplier;
        private ObjectEntity objGroupSizeSupplier;
        private ObjectEntity objArticle;
        private ObjectEntity objItem;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;
        //private ObjectEntity objSku;

        private OrderEditFormEntity(NavigatorElement parent, String sID, String caption, boolean edit) {
            super(parent, sID, caption);

            this.edit = edit;

            objOrder = addSingleGroupObject(order, "Заказ");
            if (!edit) {
                addPropertyDraw(nameSupplierDocument, objOrder);
                setAddOnEvent(objOrder, RomanLogicsModule.this, FormEventType.INIT);
            }

            addPropertyDraw(objOrder, date, sidDocument, nameDestinationDestinationDocument, dateFromOrder, dateToOrder, nameCurrencyDocument, sumDocument, quantityDocument, netWeightDocument);
            objOrder.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", baseLM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticle = addSingleGroupObject(articleComposite, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, objOrder, objArticle);
            addPropertyDraw(objArticle, nameSizeGroupSupplierArticle, sidArticle, sidSeasonSupplierArticle, nameSeasonYearArticle,
                    nameBrandSupplierArticle, nameCollectionSupplierArticle, nameSubCategorySupplierArticle, nameThemeSupplierArticle,
                    nameCategoryArticle, originalNameArticle, sidCustomCategoryOriginArticle, nameTypeFabricArticle, sidGenderArticle,
                    nameCountrySupplierOfOriginArticle, netWeightArticle, mainCompositionOriginArticle, barcode);
            addPropertyDraw(quantityListArticle, objOrder, objArticle);
            addPropertyDraw(dateFromOrderArticle, objOrder, objArticle);
            addPropertyDraw(dateToOrderArticle, objOrder, objArticle);
            addPropertyDraw(priceDocumentArticle, objOrder, objArticle);
            addPropertyDraw(RRPDocumentArticle, objOrder, objArticle);
            addPropertyDraw(sumDocumentArticle, objOrder, objArticle);
            addPropertyDraw(invoicedOrderArticle, objOrder, objArticle);
            //addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);

            objSIDColorSupplier = addSingleGroupObject(StringClass.get(50), "Ввод цвета", baseLM.objectValue);
            objSIDColorSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(addNEColorSupplierSIDInvoice, objSIDColorSupplier, objOrder));
            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(seekColorSIDInvoice, objSIDColorSupplier, objOrder));

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер"); // baseLM.selection, sidSizeSupplier
            addPropertyDraw(orderSizeSupplier, objSizeSupplier).forceViewType = ClassViewType.HIDE;

            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, name);
            setEditType(sidColorSupplier, PropertyEditType.READONLY, objColorSupplier.groupTo);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(executeAddColorDocument, objOrder, objArticle, objColorSupplier));

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityListArticleCompositeColorSize, objOrder, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.addColumnGroupObject(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            addPropertyDraw(addGCAProp(actionGroup, "nullOrderListArticleCompositeColor" + (edit ? "Edit" : ""), "Сбросить", objSizeSupplier.groupTo, addSetPropertyAProp(quantityListArticleCompositeColorSize, 1, 2, 3, 4, baseLM.vzero), 1, 2, 3, 4, 4),
                    objOrder, objArticle, objColorSupplier, objSizeSupplier);

            addPropertyDraw(quantityListSku, objOrder, objItem);
            addPropertyDraw(priceDocumentSku, objOrder, objItem);
            addPropertyDraw(priceRateDocumentSku, objOrder, objItem);
            addPropertyDraw(invoicedOrderSku, objOrder, objItem);
            addPropertyDraw(quantityListArticleCompositeColor, objOrder, objArticle, objColorSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, addPropertyObject(supplierDocument, objOrder)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objOrder)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objOrder)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleCompositeItem, objItem), Compare.EQUALS, objArticle));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListArticle, objOrder, objArticle)));


//            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeGroup, objGroupSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(groupSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(sizeGroupSupplierArticle, objArticle)));

            RegularFilterGroupEntity filterGroupColor = new RegularFilterGroupEntity(genID());
            filterGroupColor.addFilter(new RegularFilterEntity(genID(), new OrFilterEntity(
                    new NotNullFilterEntity(addPropertyObject(quantityListArticleCompositeColor, objOrder, objArticle, objColorSupplier)),
                    new CompareFilterEntity(addPropertyObject(inListArticleColorSupplier, objOrder, objArticle, objColorSupplier), Compare.EQUALS, true)),
                    "В заказе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterGroupColor);

            RegularFilterGroupEntity filterItemOrder = new RegularFilterGroupEntity(genID());
            filterItemOrder.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListSku, objOrder, objItem)),
                    "В заказе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterItemOrder);

            if(edit) {
               orderEditFA = addMFAProp(actionGroup, "Редактировать заказ", this, new ObjectEntity[] {objOrder}, true);
               orderEditFA.setImage("edit.png");
            }

            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addAProp(new AddNewArticleActionProperty(objArticle, true)), objSIDArticleComposite, objOrder));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, objOrder, objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDInvoice, objSIDArticleComposite, objOrder));

            addDefaultOrder(numberListArticle, true);
            addDefaultOrder(orderSizeSupplier, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(date, objOrder)).caption = "Дата заказа";

            design.get(objOrder.groupTo).grid.constraints.fillVertical = 0.2;

            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(baseLM.objectValue, objSIDColorSupplier)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);

            design.addIntersection(design.getGroupObjectContainer(objColorSupplier.groupTo),
                    design.getGroupObjectContainer(objItem.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objColorSupplier.groupTo).grid.constraints.fillHorizontal = 3;

//            design.get(getPropertyDraw(cloneItem, objItem)).drawToToolbar = true;
            return design;
        }
    }

    // params - по каким входам группировать
    protected LAP addIAProp(LCP dataProperty, Integer... params) {
        return addAProp(new IncrementActionProperty(genSID(), "sys", dataProperty,
                addMGProp(dataProperty, params),
                params));
    }

    public static class IncrementActionProperty extends UserActionProperty {

        LCP dataProperty;
        LCP maxProperty;
        java.util.List<Integer> params;

        IncrementActionProperty(String sID, String caption, LCP dataProperty, LCP maxProperty, Integer[] params) {
            super(sID, caption, dataProperty.getInterfaceClasses());

            this.dataProperty = dataProperty;
            this.maxProperty = maxProperty;
            this.params = Arrays.asList(params);
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

            // здесь опять учитываем, что порядок тот же
            int i = 0;
            DataObject[] dataPropertyInput = new DataObject[context.getKeyCount()];
            java.util.List<DataObject> maxPropertyInput = new ArrayList<DataObject>();

            for (ClassPropertyInterface classInterface : interfaces) {
                dataPropertyInput[i] = context.getDataKeyValue(classInterface);
                if (params.contains(i + 1)) {
                    maxPropertyInput.add(dataPropertyInput[i]);
                }
                i++;
            }

            Integer maxValue = (Integer) maxProperty.read(context, maxPropertyInput.toArray(new DataObject[0]));
            if (maxValue == null)
                maxValue = 0;
            maxValue += 1;

            dataProperty.change(maxValue, context, dataPropertyInput);
        }
    }

    private class ChangeQuantityListArticleCompositeColorSize extends UserActionProperty {

        private ChangeQuantityListArticleCompositeColorSize() {
            super("CHANGE_" + quantityListArticleCompositeColorSize.property.getSID(), quantityListArticleCompositeColorSize.getInterfaceClasses());
        }

        protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            
            ImOrderSet<DataObject> listKeys = getOrderInterfaces().mapOrder(context.getDataKeys());
            DataObject[] keys = listKeys.toArray(new DataObject[listKeys.size()]);
            
            ObjectValue value = context.requestUserData((DataClass) quantityListArticleCompositeColorSize.property.getValueClass(), quantityListArticleCompositeColorSize.read(context, keys));
            
            if(value instanceof DataObject) {
                ((CalcProperty)itemArticleCompositeColorSize.property).setNotNull(
                        itemArticleCompositeColorSize.listInterfaces.mapSet(listKeys.subOrder(1, 4)),
                        context.getEnv(), true, true);
            }

            quantityListArticleCompositeColorSize.change(value.getValue(), context, keys);
        }

        @Override
        public Type getSimpleRequestInputType() {
            return (DataClass) quantityListArticleCompositeColorSize.property.getValueClass();
        }
    }

    private class OrderFormEntity extends FormEntity<RomanBusinessLogics> {
        private ObjectEntity objSupplier;
        private ObjectEntity objOrder;
        private ObjectEntity objInvoice;
        private ObjectEntity objSku;

        private OrderFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameCurrencySupplier, importOrderActionGroup, true);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objOrder = addSingleGroupObject(order, "Заказ", date, sidDocument, dateFromOrder, dateToOrder, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            addObjectActions(this, objOrder);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));
            addPropertyDraw(orderEditFA, objOrder).forceViewType = ClassViewType.GRID;

            objInvoice = addSingleGroupObject(invoice, "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objInvoice, date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objSku = addSingleGroupObject(sku, "SKU");
            addPropertyDraw(new LP[]{barcode, sidArticleSku, nameSeasonYearArticleSku, sidGenderSupplierArticleSku,
                    nameCollectionSupplierArticle, nameSubCategorySupplierArticle, sidThemeSupplierArticleSku,
                    nameThemeSupplierArticleSku, nameSeasonYearArticleSku, nameSubCategorySupplierArticleSku, nameCollectionSupplierArticleSku,
                    nameCategoryArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    sidBrandSupplierArticleSku, nameBrandSupplierArticleSku, originalNameArticleSku,
                    nameCountrySupplierOfOriginArticleSku, nameCountryOfOriginSku, netWeightSku,
                    mainCompositionOriginSku, additionalCompositionOriginSku, delete}, objSku);

            setEditType(sidArticleSku, PropertyEditType.READONLY, objSku.groupTo);

            addPropertyDraw(priceDocumentSku, objOrder, objSku);
            addPropertyDraw(quantityDocumentSku, objOrder, objSku);
            addPropertyDraw(invoicedOrderSku, objOrder, objSku);
            addPropertyDraw(shipmentedOrderSku, objOrder, objSku);
            addPropertyDraw(shipmentedAtTermOrderSku, objOrder, objSku);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objOrder, objSku)));

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            CompareFilterEntity invoiceSupplierFilter = new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier);
            addFixedFilter(invoiceSupplierFilter);

            setEditType(objSupplier, PropertyEditType.SELECTOR);
            setEditType(importOrderActionGroup, PropertyEditType.EDITABLE, objSupplier.groupTo);

            //addDefaultOrder(numberListArticle, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(date, objOrder)).caption = "Дата заказа";

            ContainerView detContainer = design.createContainer();
            design.getMainContainer().addAfter(detContainer, design.getGroupObjectContainer(objOrder.groupTo));
            detContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            detContainer.add(design.getGroupObjectContainer(objInvoice.groupTo));
            detContainer.setType(ContainerType.TABBED_PANE);

            return design;
        }
    }


    private class InvoiceEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean box;
        private boolean edit;

        private ObjectEntity objInvoice;
        private ObjectEntity objSupplierBox;
        private ObjectEntity objSIDArticleComposite;
        private ObjectEntity objSIDArticleSingle;
        private ObjectEntity objSIDColorSupplier;
        private ObjectEntity objGroupSizeSupplier;
        private ObjectEntity objArticle;
        private ObjectEntity objItem;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;
        private PropertyDrawEntity nullArticleColor;
        private PropertyDrawEntity nullArticle;
        //private ObjectEntity objSku;

        private InvoiceEditFormEntity(NavigatorElement parent, String sID, String caption, boolean box, boolean edit) {
            super(parent, sID, caption);

            this.box = box;

            objInvoice = addSingleGroupObject("invoice", (box ? boxInvoice : simpleInvoice), "Инвойс");
           // if (!edit) {
                addPropertyDraw(nameSupplierDocument, objInvoice);
//                setAddOnEvent(objInvoice, RomanLogicsModule.this, FormEventType.INIT);
           //}

            addPropertyDraw(objInvoice, date, baseLM.objectClassName, sidDocument, nameCurrencyDocument, nameCurrencyRRPDocument,
                    sumDocument, quantityDocument, netWeightDocument, nameCompanyInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument);
            objInvoice.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objList;
            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, barcode, nameDestinationDataSupplierBox);
                objSupplierBox.groupTo.initClassView = ClassViewType.GRID;
                addObjectActions(this, objSupplierBox);
                objList = objSupplierBox;
            } else
                objList = objInvoice;

            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Ввод составного артикула", baseLM.objectValue);
            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSIDArticleSingle = addSingleGroupObject(StringClass.get(50), "Ввод простого артикула", baseLM.objectValue);
            objSIDArticleSingle.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticle = addSingleGroupObject(article, "Артикул");
            objArticle.groupTo.setSingleClassView(ClassViewType.GRID);

            addPropertyDraw(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(objArticle, nameSizeGroupSupplierArticle, sidArticle, sidSeasonSupplierArticle, nameSeasonYearArticle, nameBrandSupplierArticle,
                    nameCollectionSupplierArticle, nameSubCategorySupplierArticle, nameThemeSupplierArticle, nameCategoryArticle,
                    originalNameArticle, sidCustomCategoryOriginArticle, nameTypeFabricArticle, sidGenderArticle, nameTypeLabelArticle,
                    nameCountrySupplierOfOriginArticle, netWeightArticle, mainCompositionOriginArticle);
            addPropertyDraw(barcode, objArticle).setDrawToToolbar(true);
            addPropertyDraw(quantityListArticle, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(priceDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(RRPDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(sumDocumentArticle, objInvoice, objArticle);
            addPropertyDraw(orderedInvoiceArticle, objInvoice, objArticle);
            addPropertyDraw(priceOrderedInvoiceArticle, objInvoice, objArticle);

//            getPropertyDraw(sizeGroupSupplierArticle).forceViewType = ClassViewType.PANEL;

            setEditType(quantityListArticle, PropertyEditType.READONLY, objArticle.groupTo);
            setEditType(sumDocumentArticle, PropertyEditType.READONLY);

            objItem = addSingleGroupObject(item, "Товар", barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            objSIDColorSupplier = addSingleGroupObject(StringClass.get(50), "Ввод цвета", baseLM.objectValue);
            objSIDColorSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(addNEColorSupplierSIDInvoice, objSIDColorSupplier, objInvoice));
            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(seekColorSIDInvoice, objSIDColorSupplier, objInvoice));

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер"); // baseLM.selection, sidSizeSupplier
            objSizeSupplier.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(orderSizeSupplier, objSizeSupplier).forceViewType = ClassViewType.HIDE;

            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, name);
            setEditType(sidColorSupplier, PropertyEditType.READONLY, objColorSupplier.groupTo);

            addActionsOnObjectChange(objSIDColorSupplier, addPropertyObject(executeAddColorDocument, objList, objArticle, objColorSupplier));

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityListArticleCompositeColorSize, objList, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.addColumnGroupObject(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            String formPostfix = (box ? "Box" : "") + (edit ? "Edit" : "");

            nullArticle = addPropertyDraw(addListAProp("nullListArticle" + formPostfix, "Сбросить",
                    addSetPropertyAProp(quantityListArticle, 1, 2, baseLM.vnull), 1, 2,
                    addSetPropertyAProp(numberListArticle, 1, 2, baseLM.vnull), 1, 2),
                    objList, objArticle);
            nullArticleColor = addPropertyDraw(addListAProp("nullInvoiceListArticleCompositeColor" + formPostfix, "Сбросить",
                    addGCAProp(actionGroup, "nullGCAInvoiceListArticleCompositeColor" + formPostfix, "Сбросить (количество)", objSizeSupplier.groupTo, addSetPropertyAProp(quantityListArticleCompositeColorSize, 1, 2, 3, 4, baseLM.vnull), 1, 2, 3, 4, 4), 1, 2, 3, 4,
                    addSetPropertyAProp(inListArticleColorSupplier, 1, 2, 3, baseLM.vnull), 1, 2, 3),
                                                objList, objArticle, objColorSupplier, objSizeSupplier);

            addPropertyDraw(quantityListSku, (box ? objSupplierBox : objInvoice), objItem);
            addPropertyDraw(quantityListSku, (box ? objSupplierBox : objInvoice), objArticle);
            addPropertyDraw(priceDocumentSku, objInvoice, objItem);
            addPropertyDraw(priceRateDocumentSku, objInvoice, objItem);
            addPropertyDraw(orderedInvoiceSku, objInvoice, objItem);
            addPropertyDraw(quantityListArticleCompositeColor, objList, objArticle, objColorSupplier);
//            addPropertyDraw(quantityListArticleCompositeSize, objInvoice, objArticle, objSizeSupplier);

            if (box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(boxInvoiceSupplierBox, objSupplierBox), Compare.EQUALS, objInvoice));
                addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
                addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
                addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
                addFixedFilter(new CompareFilterEntity(addPropertyObject(articleCompositeItem, objItem), Compare.EQUALS, objArticle));
                //addFixedFilter(new NotNullFilterEntity(addPropertyObject(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle)));
                addFixedFilter(new CompareFilterEntity(addPropertyObject(numberListArticle, (box ? objSupplierBox : objInvoice), objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

    //            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeGroup, objGroupSizeSupplier), Compare.EQUALS, addPropertyObject(supplierDocument, objInvoice)));
                addFixedFilter(new CompareFilterEntity(addPropertyObject(groupSizeSupplier, objSizeSupplier), Compare.EQUALS, addPropertyObject(sizeGroupSupplierArticle, objArticle)));

/*            RegularFilterGroupEntity filterGroupSize = new RegularFilterGroupEntity(genID());
            filterGroupSize.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListArticleCompositeSize, objInvoice, objArticle, objSizeSupplier)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupSize);*/

            //addPropertyObject(sidColorSupplier, objColorSupplier), Compare.EQUALS, objSIDColorSupplier)

            RegularFilterGroupEntity filterGroupColor = new RegularFilterGroupEntity(genID());
            filterGroupColor.addFilter(new RegularFilterEntity(genID(), new OrFilterEntity(
                    new NotNullFilterEntity(addPropertyObject(quantityListArticleCompositeColor, objList, objArticle, objColorSupplier)),
                    new CompareFilterEntity(addPropertyObject(inListArticleColorSupplier, objList, objArticle, objColorSupplier), Compare.EQUALS, true)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterGroupColor);

            RegularFilterGroupEntity filterItemInvoice = new RegularFilterGroupEntity(genID());
            filterItemInvoice.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListSku, objList, objItem)),
                    "В инвойсе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), true);
            addRegularFilterGroup(filterItemInvoice);

            if(edit){
               if(box) {
                  boxInvoiceEditFA = addDMFAProp(actionGroup, "Редактировать инвойс", this, new ObjectEntity[] {objInvoice}, true);
                  boxInvoiceEditFA.setDrawToToolbar(true);
                  boxInvoiceEditFA.setImage("edit.png");
               }
               else {
                  simpleInvoiceEditFA = addDMFAProp(actionGroup, "Редактировать инвойс", this, new ObjectEntity[] {objInvoice}, true);
                  simpleInvoiceEditFA.setDrawToToolbar(true);
                  simpleInvoiceEditFA.setImage("edit.png");
               }
            }

//            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addNEArticleCompositeSIDInvoice, objSIDArticleComposite, objInvoice));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(addAProp(new AddNewArticleActionProperty(objArticle, true)), objSIDArticleComposite, objInvoice));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleComposite));
            addActionsOnObjectChange(objSIDArticleComposite, addPropertyObject(seekArticleSIDInvoice, objSIDArticleComposite, objInvoice));

            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(addAProp(new AddNewArticleActionProperty(objArticle, false)), objSIDArticleSingle, objInvoice));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(incrementNumberListSID, (box ? objSupplierBox : objInvoice), objSIDArticleSingle));
            addActionsOnObjectChange(objSIDArticleSingle, addPropertyObject(seekArticleSIDInvoice, objSIDArticleSingle, objInvoice));

            addDefaultOrder(numberListArticle, true);
            addDefaultOrder(orderSizeSupplier, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(date, objInvoice)).caption = "Дата инвойса";

            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 0.2;

            design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                    design.getGroupObjectContainer(objSIDArticleSingle.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            if (box) {
                design.addIntersection(design.getGroupObjectContainer(objInvoice.groupTo),
                    design.getGroupObjectContainer(objSupplierBox.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                    design.getGroupObjectContainer(objSupplierBox.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.addIntersection(design.getGroupObjectContainer(objSIDArticleSingle.groupTo),
                        design.getGroupObjectContainer(objSupplierBox.groupTo),
                        DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
                design.get(objSupplierBox.groupTo).grid.constraints.fillVertical= 0.4;
            }

            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleComposite)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0);
            design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0);
            design.get(getPropertyDraw(baseLM.objectValue, objSIDColorSupplier)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
            /*design.get(getPropertyDraw(baseLM.objectValue, objSIDArticleSingle)).editKey = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);

            design.addIntersection(design.getGroupObjectContainer(objSIDArticleComposite.groupTo),
                    design.getGroupObjectContainer(objSIDArticleSingle.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);*/

            //design.addIntersection(design.getGroupObjectContainer(objColorSupplier.groupTo),
            //        design.getGroupObjectContainer(objItem.groupTo),
            //        DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objArticle.groupTo),
                    design.getGroupObjectContainer(objItem.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objItem.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objArticle.groupTo).grid.constraints.fillHorizontal = 2;

            design.get(nullArticle).design.setIconPath("delete.png");
            design.get(nullArticleColor).design.setIconPath("delete.png");
//            design.get(getPropertyDraw(cloneItem, objItem)).drawToToolbar = true;
            return design;
        }
    }

    private class InvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean box;

        private ObjectEntity objSupplier;
        private ObjectEntity objOrder;
        private ObjectEntity objInvoice;
        private ObjectEntity objSku;

        private InvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            this.box = box;
            objSupplier = addSingleGroupObject("supplier", supplier, "Поставщик", name, importInvoiceActionGroup, true);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objInvoice = addSingleGroupObject("invoice", (box ? boxInvoice : simpleInvoice), "Инвойс", date, baseLM.objectClassName,
                    sidDocument, nameCurrencyDocument, nameCurrencyRRPDocument, sumDocument, quantityDocument, netWeightDocument,
                    nameCompanyInvoice, sidDestinationDestinationDocument, nameDestinationDestinationDocument, delete);
            //addObjectActions(this, objInvoice);
            setEditType(PropertyEditType.READONLY, objInvoice.groupTo);
            setEditType(baseLM.objectClassName, PropertyEditType.EDITABLE, objInvoice.groupTo);
            setEditType(nameCurrencyDocument, PropertyEditType.EDITABLE, objInvoice.groupTo);
            setEditType(nameCurrencyRRPDocument, PropertyEditType.EDITABLE, objInvoice.groupTo);
            setEditType(nameCompanyInvoice, PropertyEditType.EDITABLE, objInvoice.groupTo);
            setEditType(nameDestinationDestinationDocument, PropertyEditType.EDITABLE, objInvoice.groupTo);
            setEditType(delete, PropertyEditType.EDITABLE, objInvoice.groupTo);

            for (PropertyDrawEntity propertyDraw : getProperties(importInvoiceActionGroup)) {
                propertyDraw.toDraw = objInvoice.groupTo;
                propertyDraw.setDrawToToolbar(true);
            }

            if (box) {
                addPropertyDraw(boxInvoiceEditFA, objInvoice).forceViewType = ClassViewType.PANEL;
            }
            else {
                addPropertyDraw(simpleInvoiceEditFA, objInvoice).forceViewType = ClassViewType.PANEL;
            }

            objOrder = addSingleGroupObject("order", order, "Заказ");
            objOrder.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(inOrderInvoice, objOrder, objInvoice);
            addPropertyDraw(objOrder, date, sidDocument, nameCurrencyDocument, sumDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objSku = addSingleGroupObject("sku", sku, "SKU");
            addPropertyDraw(new LP[]{barcode, sidArticleSku, sidSeasonSupplierArticle, nameSeasonYearArticleSku, sidGenderSupplierArticleSku,
                    nameThemeSupplierArticleSku, nameSeasonYearArticleSku, nameSubCategorySupplierArticleSku, nameCollectionSupplierArticleSku, nameCategoryArticleSku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, nameCountrySupplierOfOriginArticleSku, nameCountryOfOriginSku, netWeightSku}, objSku);

            setEditType(PropertyEditType.READONLY, objSku.groupTo);

            if (box) {
                addGCAProp(actionGroup, "translationInvoiceMainComposition", "Перевести все", objSku.groupTo, translationMainCompositionSku);
                addGCAProp(actionGroup, "translationInvoiceAdditionalComposition", "Перевести все", objSku.groupTo, translationAdditionalCompositionSku);
                addGCAProp(actionGroup, "translationInvoiceName", "Перевести все", objSku.groupTo, translationNameSku);
                addGCAProp(actionGroup, "translationInvoiceLanguageMainComposition", "Перевести все", objSku.groupTo, translationMainCompositionSkuInvoice, 1, 2, 1);
                addGCAProp(actionGroup, "translationInvoiceLanguageAdditionalComposition", "Перевести все", objSku.groupTo, translationAdditionalCompositionSkuInvoice, 1, 2, 1);
                addGCAProp(actionGroup, "translationInvoiceLanguageName", "Перевести все", objSku.groupTo, translationNameSkuInvoice, 1, 2, 1);
            } else {
                addGCAProp(actionGroup, "translationInvoiceMainCompositionSimple", "Перевести все", objSku.groupTo, translationMainCompositionSku);
                addGCAProp(actionGroup, "translationInvoiceAdditionalCompositionSimple", "Перевести все", objSku.groupTo, translationAdditionalCompositionSku);
                addGCAProp(actionGroup, "translationInvoiceNameSimple", "Перевести все", objSku.groupTo, translationNameSku);
                addGCAProp(actionGroup, "translationInvoiceLanguageMainCompositionSimple", "Перевести все", objSku.groupTo, translationMainCompositionSkuInvoice, 1, 2, 1);
                addGCAProp(actionGroup, "translationInvoiceLanguageAdditionalCompositionSimple", "Перевести все", objSku.groupTo, translationAdditionalCompositionSkuInvoice, 1, 2, 1);
                addGCAProp(actionGroup, "translationInvoiceLanguageNameSimple", "Перевести все", objSku.groupTo, translationNameSkuInvoice, 1, 2, 1);
            }

            addPropertyDraw(priceDocumentSku, objInvoice, objSku);
            addPropertyDraw(quantityDocumentSku, objInvoice, objSku);
            setEditType(priceDocumentSku, PropertyEditType.READONLY);
            setEditType(quantityDocumentSku, PropertyEditType.READONLY);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objInvoice, objSku)));

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            if (!box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue)));

            if (box)
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));

            CompareFilterEntity orderSupplierFilter = new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier);
            addFixedFilter(orderSupplierFilter);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));

            /*addFixedFilter(new CompareFilterEntity(addPropertyObject(inOrderInvoice, objOrder, objInvoice), Compare.EQUALS, true));
            addPropertyDraw(
                    addSelectFromListAction(null, "Выбрать заказы", objOrder, new FilterEntity[]{orderSupplierFilter}, inOrderInvoice, true, order, invoice),
                    objOrder.groupTo,
                    objInvoice
            ).forceViewType = ClassViewType.PANEL;*/

            setEditType(objSupplier, PropertyEditType.SELECTOR);
            setEditType(importInvoiceActionGroup, PropertyEditType.EDITABLE, objSupplier.groupTo);

            //addDefaultOrder(numberListArticle, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(date, objInvoice)).caption = "Дата инвойса";
            design.get(getPropertyDraw(sidDocument, objOrder)).caption = "Номер заказа";
            design.get(getPropertyDraw(date, objOrder)).caption = "Дата заказа";

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 3;
            design.get(objOrder.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;

            ContainerView detContainer = design.createContainer(null, null, "invoiceDetail");
            design.getMainContainer().addAfter(detContainer, design.getGroupObjectContainer(objInvoice.groupTo));
            //detContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            //detContainer.add(design.getGroupObjectContainer(objOrder.groupTo));
            detContainer.setType(ContainerType.TABBED_PANE);

            return design;
        }
    }

    private class ShipmentListFormEntity extends FormEntity {
        private boolean box;

        private ObjectEntity objSupplier;
        private ObjectEntity objShipment;
        private ObjectEntity objInvoice;
        private ObjectEntity objOrder;
        private ObjectEntity objRoute;

        private ShipmentListFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            this.box = box;

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", date, sidDocument, dateDepartureShipment, dateArrivalShipment, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);//, invoicedShipment, sumShipment
            addObjectActions(this, objShipment);

            objOrder = addSingleGroupObject(order, "Заказ", sidDocument, dateFromOrder, dateToOrder);
            addPropertyDraw(inOrderShipment, objOrder, objShipment);

            objInvoice = addSingleGroupObject((box ? boxInvoice : simpleInvoice), "Инвойс");
            objInvoice.groupTo.setSingleClassView(ClassViewType.GRID);
            setEditType(objInvoice, PropertyEditType.READONLY);

            addPropertyDraw(inInvoiceShipment, objInvoice, objShipment);
            addPropertyDraw(objInvoice, date, sidDocument, sidDestinationDestinationDocument, nameDestinationDestinationDocument);

            objRoute = addSingleGroupObject(route, "Маршрут", name);
            addPropertyDraw(nameImporterShipmentRoute, objShipment, objRoute);
            addPropertyDraw(percentShipmentRoute, objShipment, objRoute);
            addPropertyDraw(invoicedShipmentRoute, objShipment, objRoute);
            addPropertyDraw(sumShipmentRoute, objShipment, objRoute);

            if (!box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue)));

            if (box) {
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(is(directInvoice), objInvoice), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));
            }

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objOrder), Compare.EQUALS, objSupplier));

            setEditType(objSupplier, PropertyEditType.SELECTOR);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(date, objShipment)).caption = "Дата поставки";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(date, objInvoice)).caption = "Дата инвойса";

            design.get(objRoute.groupTo).grid.constraints.fillHorizontal = 0.6;

            design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                    design.getGroupObjectContainer(objOrder.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objInvoice.groupTo),
                    design.getGroupObjectContainer(objRoute.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class PrintSkuFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSku;

        private PrintSkuFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            objSku = addSingleGroupObject(1, "sku", sku, "Товар", barcode);
            objSku.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objSku, PropertyEditType.READONLY);

            skuPrintFA = addFAProp("Печать штрих-кода", this, objSku);
        }
    }

    private class ShipmentSpecFormEntity extends BarcodeFormEntity {
        private boolean box;

        private ObjectEntity objSIDSupplierBox;
        private ObjectEntity objSupplier;
        private ObjectEntity objShipment;
        private ObjectEntity objSupplierBox;
        private ObjectEntity objSku;
        private ObjectEntity objRoute;
        private ObjectEntity objShipmentDetail;

        private PropertyDrawEntity findItemBox;
        private PropertyDrawEntity findItemSimple;

        private PropertyDrawEntity nameRoute;

        private ShipmentSpecFormEntity(NavigatorElement parent, String sID, String caption, boolean box) {
            super(parent, sID, caption);

            isSynchronizedApply = true;

            this.box = box;

            if (box) {
                objSIDSupplierBox = addSingleGroupObject(StringClass.get(50), "Номер короба", baseLM.objectValue);
                objSIDSupplierBox.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objShipment = addSingleGroupObject((box ? boxShipment : simpleShipment), "Поставка", date, sidDocument, invoicedShipment, quantityShipment);
            objShipment.groupTo.initClassView = ClassViewType.PANEL;

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб поставщика", sidSupplierBox, barcode, nameDestinationSupplierBox, quantityDataList, quantitySupplierBox);
                objSupplierBox.groupTo.initClassView = ClassViewType.PANEL;
            }

            objRoute = addSingleGroupObject(route, "Маршрут", name, barcodeCurrentPalletRoute, grossWeightCurrentPalletRoute, barcodeCurrentFreightBoxRoute, nameDestinationCurrentFreightBoxRoute);
            objRoute.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(packingListFormRoute, objRoute);
            addPropertyDraw(changePallet, objRoute);

            nameRoute = addPropertyDraw(name, objRoute);
            nameRoute.forceViewType = ClassViewType.PANEL;

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameBrandSupplierArticleSku, originalNameArticleSku, nameCategoryArticleSku, nameUnitOfMeasureArticleSku,
                    netWeightArticleSku, sidCustomCategoryOriginArticleSku, nameCountryOfOriginArticleSku, mainCompositionOriginArticleSku,
                    netWeightSku, nameCountryOfOriginSku, mainCompositionOriginSku, additionalCompositionOriginSku);

            objSku.groupTo.setSingleClassView(ClassViewType.GRID);
            addPropertyDraw(skuEditFA, objSku).forceViewType = ClassViewType.GRID;
            addPropertyDraw(skuPrintFA, objSku).forceViewType = ClassViewType.GRID;

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objSku.groupTo);
            //getPropertyDraw(nameOriginCategoryArticleSku).forceViewType = ClassViewType.GRID;
            //getPropertyDraw(netWeightArticleSku).forceViewType = ClassViewType.GRID;            

            setEditType(supplierAttributeGroup, PropertyEditType.READONLY, objSku.groupTo);

            addPropertyDraw(invoicedShipmentSku, objShipment, objSku);
            addPropertyDraw(quantityShipmentSku, objShipment, objSku);

//            addPropertyDraw(oneShipmentArticleSku, objShipment, objSku);
//            addPropertyDraw(oneShipmentSku, objShipment, objSku);
//
//            getPropertyDraw(oneShipmentArticleSku).forceViewType = ClassViewType.PANEL;
//            getPropertyDraw(oneShipmentSku).forceViewType = ClassViewType.PANEL;

            PropertyDrawEntity quantityColumn;
            LCP highlightColor = addCProp(ColorClass.instance, new Color(255, 128, 128));
            if (box) {
                addPropertyDraw(quantityListSku, objSupplierBox, objSku);
                addPropertyDraw(quantityShipDimensionShipmentSku, objSupplierBox, objShipment, objSku);
                quantityColumn = addPropertyDraw(quantitySupplierBoxBoxShipmentRouteSku, objSupplierBox, objShipment, objRoute, objSku);

                CalcPropertyObjectEntity diffListSupplierBoxProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, diffListSupplierBox, 1), objSupplierBox);
                getPropertyDraw(quantityDataList).setPropertyBackground(diffListSupplierBoxProperty);
                getPropertyDraw(quantitySupplierBox).setPropertyBackground(diffListSupplierBoxProperty);

                CalcPropertyObjectEntity diffListShipSkuProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, diffListShipSku, 1, 2, 3), objSupplierBox, objShipment, objSku);
                getPropertyDraw(quantityDataListSku).setPropertyBackground(diffListShipSkuProperty);
                getPropertyDraw(quantityShipDimensionShipmentSku).setPropertyBackground(diffListShipSkuProperty);

            } else {
                quantityColumn = addPropertyDraw(quantitySimpleShipmentRouteSku, objShipment, objRoute, objSku);
            }

            quantityColumn.addColumnGroupObject(objRoute.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(name, objRoute);

            addPropertyDraw(quantityRouteSku, objRoute, objSku);

            addPropertyDraw(percentShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(invoicedShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);
            addPropertyDraw(quantityShipmentRouteSku, objShipment, objRoute, objSku).setToDraw(objRoute.groupTo);

            CalcPropertyObjectEntity diffShipmentRouteSkuProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, diffShipmentRouteSku, 1, 2, 3), objShipment, objRoute, objSku);
            getPropertyDraw(invoicedShipmentRouteSku).setPropertyBackground(diffShipmentRouteSkuProperty);
            getPropertyDraw(quantityShipmentRouteSku).setPropertyBackground(diffShipmentRouteSkuProperty);

            objShipmentDetail = addSingleGroupObject((box ? boxShipmentDetail : simpleShipmentDetail),
                    baseLM.selection, barcodeSkuShipmentDetail, nameBrandSupplierArticleSkuShipmentDetail, sidArticleShipmentDetail, sidColorSupplierItemShipmentDetail, nameColorSupplierItemShipmentDetail, sidSizeSupplierItemShipmentDetail,
                    nameBrandSupplierArticleSkuShipmentDetail, sidCustomCategoryOriginArticleSkuShipmentDetail, originalNameArticleSkuShipmentDetail,
                    nameCategoryArticleSkuShipmentDetail, nameUnitOfMeasureArticleSkuShipmentDetail, sidGenderArticleSkuShipmentDetail, nameTypeFabricArticleSkuShipmentDetail,
                    coefficientArticleSkuShipmentDetail,  netWeightArticleSkuShipmentDetail,
                    nameCountryOfOriginArticleSkuShipmentDetail, mainCompositionOriginArticleSkuShipmentDetail,
                    netWeightSkuShipmentDetail, nameCountryOfOriginSkuShipmentDetail,
                    mainCompositionOriginSkuShipmentDetail, additionalCompositionOriginSkuShipmentDetail,
                    sidShipmentShipmentDetail, priceInShipmentDetail,
                    sidSupplierBoxShipmentDetail, barcodeSupplierBoxShipmentDetail,
                    barcodeStockShipmentDetail, nameRouteFreightBoxShipmentDetail,
                    quantityShipmentDetail, nameUserShipmentDetail, sidStampShipmentDetail, seriesOfStampShipmentDetail, timeShipmentDetail, delete);

            objShipmentDetail.groupTo.setSingleClassView(ClassViewType.GRID);

            LAP skuShipmentDetailEditFA = addJoinAProp("Редактировать", skuEditFA, skuShipmentDetail, 1);
            skuShipmentDetailEditFA.setImage("edit.png");
            addPropertyDraw(skuShipmentDetailEditFA, objShipmentDetail);

            getPropertyDraw(sidStampShipmentDetail).propertyCaption = addPropertyObject(hideSidStampShipmentDetail, objShipmentDetail);
            getPropertyDraw(seriesOfStampShipmentDetail).propertyCaption = addPropertyObject(hideSeriesOfStampShipmentDetail, objShipmentDetail);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objShipmentDetail.groupTo);
            setForceViewType(supplierAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);
            setForceViewType(intraAttributeGroup, ClassViewType.PANEL, objShipmentDetail.groupTo);

            getPropertyDraw(nameBrandSupplierArticleSkuShipmentDetail, 0).forceViewType = ClassViewType.GRID;

            //getPropertyDraw(nameOriginCategoryArticleSkuShipmentDetail).forceViewType = ClassViewType.GRID;
            //getPropertyDraw(netWeightArticleSkuShipmentDetail).forceViewType = ClassViewType.GRID;

            CalcPropertyObjectEntity oneArticleProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, oneArticleSkuShipmentDetail, 1), objShipmentDetail);
            CalcPropertyObjectEntity oneSkuProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, oneSkuShipmentDetail, 1), objShipmentDetail);
            CalcPropertyObjectEntity oneArticleColorProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, oneArticleColorShipmentDetail, 1), objShipmentDetail);
            CalcPropertyObjectEntity oneArticleSizeProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, oneArticleSizeShipmentDetail, 1), objShipmentDetail);

            getPropertyDraw(nameCategoryArticleSkuShipmentDetail).setPropertyBackground(oneArticleProperty);
            getPropertyDraw(nameUnitOfMeasureArticleSkuShipmentDetail).setPropertyBackground(oneArticleProperty);
            getPropertyDraw(netWeightSkuShipmentDetail).setPropertyBackground(oneArticleSizeProperty);
            getPropertyDraw(netWeightSkuShipmentDetail).eventID = WeightDaemonTask.SCALES_SID;
            getPropertyDraw(nameCountryOfOriginSkuShipmentDetail).setPropertyBackground(oneArticleColorProperty);
            getPropertyDraw(mainCompositionOriginSkuShipmentDetail).setPropertyBackground(oneArticleColorProperty);
            getPropertyDraw(additionalCompositionOriginSkuShipmentDetail).setPropertyBackground(oneArticleColorProperty);

            if (!box)
                addFixedFilter(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue)));

            if (box)
                addFixedFilter(new NotFilterEntity(new CompareFilterEntity(addPropertyObject(typeSupplier, objSupplier), Compare.EQUALS, addPropertyObject(baseLM.vtrue))));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));

            if (box)
                addFixedFilter(new NotNullFilterEntity(addPropertyObject(inSupplierBoxShipment, objSupplierBox, objShipment)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            if (box) {
                FilterEntity inSupplierBox = new NotNullFilterEntity(addPropertyObject(quantityListSku, objSupplierBox, objSku));
                FilterEntity inSupplierBoxShipmentStock = new NotNullFilterEntity(addPropertyObject(quantitySupplierBoxBoxShipmentRouteSku, objSupplierBox, objShipment, objRoute, objSku));
                FilterEntity inSupplierBoxShipmentSku = new NotNullFilterEntity(addPropertyObject(quantitySupplierBoxBoxShipmentSku, objSupplierBox, objShipment, objSku));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new OrFilterEntity(inSupplierBox, inSupplierBoxShipmentSku),
                        "В коробе поставщика или оприходовано",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        inSupplierBox,
                        "В коробе поставщика"));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        inSupplierBoxShipmentStock,
                        "Оприходовано в тек. короб",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku)),
                        "Ожидается в поставке"));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku)),
                                           new NotNullFilterEntity(addPropertyObject(quantityShipmentSku, objShipment, objSku))),
                        "Ожидается или оприходовано в поставке"));


            } else {
                FilterEntity inInvoice = new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku));
                FilterEntity inSimpleShipment = new NotNullFilterEntity(addPropertyObject(quantityShipmentSku, objShipment, objSku));
                FilterEntity inInvoiceShipmentStock = new NotNullFilterEntity(addPropertyObject(quantitySimpleShipmentRouteSku, objShipment, objRoute, objSku));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new OrFilterEntity(inInvoice, inInvoiceShipmentStock),
                        "В инвойсах или оприходовано в короб",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        inInvoiceShipmentStock,
                        "Оприходовано в тек. короб",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        inInvoice,
                        "Ожидается в поставке"));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        new OrFilterEntity(inInvoice, inSimpleShipment),
                        "Ожидается или оприходовано в поставке"));
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                        inSimpleShipment,
                        "Оприходовано в поставке"));
            }
            filterGroup.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroup);

            RegularFilterGroupEntity filterGroup2 = new RegularFilterGroupEntity(genID());
            filterGroup2.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(shipmentShipmentDetail, objShipmentDetail), Compare.EQUALS, objShipment),
                    "В поставке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroup2.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroup2);

            RegularFilterGroupEntity filterGroup4 = new RegularFilterGroupEntity(genID());
            filterGroup4.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(userShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(BL.authenticationLM.currentUser)),
                    "Пользователя",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroup4);

            RegularFilterGroupEntity filterGroup5 = new RegularFilterGroupEntity(genID());
            filterGroup5.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(stockShipmentDetail, objShipmentDetail), Compare.EQUALS, addPropertyObject(currentFreightBoxRoute, objRoute)),
                    "В коробе для трансп.",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup5);

            RegularFilterGroupEntity filterGroupDiffShipment = new RegularFilterGroupEntity(genID());
            filterGroupDiffShipment.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(diffShipmentSku, objShipment, objSku)),
                    "Отличающиеся в поставке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            addRegularFilterGroup(filterGroupDiffShipment);

            if (box) {
                RegularFilterGroupEntity filterGroup3 = new RegularFilterGroupEntity(genID());
                filterGroup3.addFilter(new RegularFilterEntity(genID(),
                        new CompareFilterEntity(addPropertyObject(supplierBoxShipmentDetail, objShipmentDetail), Compare.EQUALS, objSupplierBox),
                        "В коробе поставщика",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
                addRegularFilterGroup(filterGroup3);

                RegularFilterGroupEntity filterGroupDiffBox = new RegularFilterGroupEntity(genID());
                filterGroupDiffBox.addFilter(new RegularFilterEntity(genID(),
                        new NotNullFilterEntity(addPropertyObject(diffListShipSku, objSupplierBox, objShipment, objSku)),
                        "Отличающиеся в коробе",
                        KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0)));
                addRegularFilterGroup(filterGroupDiffBox);
            }

            addActionsOnObjectChange(objBarcode, addPropertyObject(addListAProp(baseLM.apply, addIfAProp(baseLM.canceled, baseLM.flowBreak))));

            if (box)
                addActionsOnObjectChange(objBarcode, addPropertyObject(
                        addIfAProp(addJProp(baseLM.andNot1, emptyBarcodeShipment, 1, skuBarcodeObject, 2), 1, 2,
                                addMFAProp(null, "Поиск по артикулу",
                                        findItemFormBoxBarcode,
                                        new ObjectEntity[]{findItemFormBoxBarcode.objShipment, findItemFormBoxBarcode.objBarcode, findItemFormBoxBarcode.objSupplierBox},
                                        false), 1, 2, 3),
                                objShipment, objBarcode, objSupplierBox));
            else
                addActionsOnObjectChange(objBarcode, addPropertyObject(
                        addIfAProp(addJProp(baseLM.andNot1, emptyBarcodeShipment, 1, skuBarcodeObject, 2), 1, 2,
                                addMFAProp(null, "Поиск по артикулу",
                                        findItemFormSimpleBarcode,
                                        new ObjectEntity[]{findItemFormSimpleBarcode.objShipment, findItemFormSimpleBarcode.objBarcode},
                                        false), 1, 2),
                                objShipment, objBarcode));

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSeekPallet, objBarcode));
            //addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionCheckPallet, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSeekFreightBox, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetPallet, objBarcode));

            if (box)
                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetStore, objBarcode, objSupplierBox));

//            addActionsOnObjectChange(objBarcode, addPropertyObject(seekRouteToFillShipmentBarcode, objShipment, objBarcode));
//            if (box)
//                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction4, objSupplierBox, objShipment, objRoute, objBarcode));
//            else
//                addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeAction3, objShipment, objRoute, objBarcode));

//
//            addActionsOnObjectChange(objBarcode, addPropertyObject(
//                    addJProp(true, baseLM.and1, addJProp(true, baseLM.equalsObjectBarcode, skuShipmentDetail, 1, 2), 1, 2, noBarcodeSupplier, 3),
//                    objShipmentDetail, objBarcode, objSupplier
//            ));

            addActionsOnObjectChange(objBarcode, addPropertyObject(
                    addJoinAProp(addSAProp(), skuBarcodeObject, 1),
                    objBarcode));

            addActionsOnObjectChange(objBarcode,
                    addPropertyObject(
                            addIfAProp(true, skuBarcodeObject, 2,
                                    addMFAProp(
                                            null,
                                            "Ввод нового товара",
                                            createItemForm,
                                            new ObjectEntity[]{createItemForm.objSupplier, createItemForm.objBarcode},
                                            false),
                                    1, 2),
                            objSupplier, objBarcode));

            addActionsOnObjectChange(objBarcode, addPropertyObject(
                    addJoinAProp(seekRouteShipmentSkuRoute,
                            1, skuBarcodeObject, 2, 3),
                    objShipment, objBarcode, objRoute));

            if (box) {
                addActionsOnObjectChange(objBarcode, addPropertyObject(addListAProp(barcodeActionCheckFreightBox, 3, 4,
                        addBoxShipmentDetailBoxShipmentSupplierBoxRouteBarcode, 1, 2, 3, 4), objShipment, objSupplierBox, objRoute, objBarcode));
            } else {
                addActionsOnObjectChange(objBarcode, addPropertyObject(addListAProp(barcodeActionCheckFreightBox, 2, 3,
                        addSimpleShipmentDetailSimpleShipmentRouteBarcode, 1, 2, 3), objShipment, objRoute, objBarcode));
            }

//            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeNotFoundMessage, objBarcode));
            if (box)
                addActionsOnObjectChange(objSIDSupplierBox, addPropertyObject(seekSupplierBoxSIDSupplier, objSIDSupplierBox, objSupplier));

            setEditType(name, PropertyEditType.READONLY, objRoute.groupTo);
            setEditType(percentShipmentRouteSku, PropertyEditType.READONLY, objRoute.groupTo);
            setEditType(itemAttributeGroup, PropertyEditType.READONLY, objSku.groupTo);
            setEditType(sidArticleSku, PropertyEditType.READONLY, objSku.groupTo);

            setEditType(baseGroup, PropertyEditType.READONLY, objShipmentDetail.groupTo);
            setEditType(supplierAttributeGroup, PropertyEditType.READONLY, objShipmentDetail.groupTo);
            setEditType(barcodeSkuShipmentDetail, PropertyEditType.EDITABLE, objShipmentDetail.groupTo);
            setEditType(sidSupplierBoxShipmentDetail, PropertyEditType.EDITABLE, objShipmentDetail.groupTo);
            setEditType(barcodeSupplierBoxShipmentDetail, PropertyEditType.EDITABLE, objShipmentDetail.groupTo);
            setEditType(barcodeStockShipmentDetail, PropertyEditType.EDITABLE, objShipmentDetail.groupTo);

            setEditType(objSupplier, PropertyEditType.SELECTOR);
            setEditType(objShipment, PropertyEditType.SELECTOR);

            if (box) {
                setEditType(objSupplierBox, PropertyEditType.SELECTOR);

                findItemBox = addPropertyDraw(addMFAProp(null, "Поиск по артикулу",
                            findItemFormBox,
                                                 new ObjectEntity[]{findItemFormBox.objShipment, findItemFormBox.objSupplierBox, findItemFormBox.objSku},
                                                 false),
                                                 objShipment, objSupplierBox, objSku);
                ((FormActionProperty)findItemBox.propertyObject.property).seekOnOk.add(findItemFormBox.objSku);
                ((FormActionProperty)findItemBox.propertyObject.property).seekOnOk.add(findItemFormBox.objShipmentDetail);
                findItemBox.forceViewType = ClassViewType.PANEL;
            } else {
                findItemSimple = addPropertyDraw(addMFAProp(null, "Поиск по артикулу",
                            findItemFormSimple,
                                                 new ObjectEntity[]{findItemFormSimple.objShipment, findItemFormSimple.objSku},
                                                 false),
                                                 objShipment, objSku);
                ((FormActionProperty)findItemSimple.propertyObject.property).seekOnOk.add(findItemFormSimple.objSku);
                ((FormActionProperty)findItemSimple.propertyObject.property).seekOnOk.add(findItemFormSimple.objShipmentDetail);
                findItemSimple.forceViewType = ClassViewType.PANEL;
            }

            addHintsIncrementTable(priceInShipmentStockSku, priceInInvoiceShipmentStockSku, priceInInvoiceStockSku, quantityInvoiceStockSku, quantityInvoiceShipmentStockSku);
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objShipment)).caption = "Номер поставки";
            design.get(getPropertyDraw(date, objShipment)).caption = "Дата поставки";

            if (box)
                design.setEditKey(design.get(getPropertyDraw(baseLM.objectValue, objSIDSupplierBox)), KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));

            design.get(objRoute.groupTo).toolbar.visible = false;
            design.get(objRoute.groupTo).setTableRowsCount(0);

            if (box)
                design.get(getPropertyDraw(quantityListSku, objSku)).caption = "Ожид. (короб)";

            if (box)
                design.addIntersection(design.getGroupObjectContainer(objBarcode.groupTo),
                        design.getGroupObjectContainer(objSIDSupplierBox.groupTo),
                        DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                    design.getGroupObjectContainer(objShipment.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            if (box)
                design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                        design.getGroupObjectContainer(objSupplierBox.groupTo),
                        DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objShipmentDetail.groupTo));
            specContainer.add(design.getGroupObjectContainer(objShipmentDetail.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            specContainer.setType(ContainerType.TABBED_PANE);

            design.get(nameRoute).setMinimumCharWidth(4);
            design.get(nameRoute).panelLabelAbove = true;
            design.get(nameRoute).design.font = new Font("Tahoma", Font.BOLD, 48);
            design.getGroupObjectContainer(objRoute.groupTo).constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;

            ContainerView supplierRow1 = design.createContainer();
            supplierRow1.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            supplierRow1.add(design.get(getPropertyDraw(originalNameArticleSkuShipmentDetail)));
            supplierRow1.add(design.get(getPropertyDraw(sidCustomCategoryOriginArticleSkuShipmentDetail)));
            supplierRow1.add(design.get(getPropertyDraw(nameCountryOfOriginArticleSkuShipmentDetail)));

            ContainerView supplierRow2 = design.createContainer();
            supplierRow2.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            supplierRow2.add(design.get(getPropertyDraw(nameBrandSupplierArticleSkuShipmentDetail, 1)));
            supplierRow2.add(design.get(getPropertyDraw(netWeightArticleSkuShipmentDetail)));
            supplierRow2.add(design.get(getPropertyDraw(mainCompositionOriginArticleSkuShipmentDetail)));

            ContainerView supplierContainer = design.getGroupPropertyContainer(objShipmentDetail.groupTo, supplierAttributeGroup);
            supplierContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            supplierContainer.add(supplierRow1);
            supplierContainer.add(supplierRow2);

            ContainerView intraRow1 = design.createContainer();
            intraRow1.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            intraRow1.add(design.get(getPropertyDraw(sidGenderArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameCategoryArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameTypeFabricArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameUnitOfMeasureArticleSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(netWeightSkuShipmentDetail)));
            intraRow1.add(design.get(getPropertyDraw(nameCountryOfOriginSkuShipmentDetail)));

            ContainerView intraRow2 = design.createContainer();
            intraRow2.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            intraRow2.add(design.get(getPropertyDraw(mainCompositionOriginSkuShipmentDetail)));
            intraRow2.add(design.get(getPropertyDraw(additionalCompositionOriginSkuShipmentDetail)));
            intraRow2.add(design.get(getPropertyDraw(coefficientArticleSkuShipmentDetail)));

            ContainerView intraRow3 = design.createContainer();
            intraRow3.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_RIGHT;
            intraRow3.add(design.get(getPropertyDraw(sidStampShipmentDetail)));
            intraRow3.add(design.get(getPropertyDraw(seriesOfStampShipmentDetail)));

            ContainerView intraContainer = design.getGroupPropertyContainer(objShipmentDetail.groupTo, intraAttributeGroup);
            intraContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            intraContainer.add(intraRow1);
            intraContainer.add(intraRow2);
            intraContainer.add(intraRow3);

            if (box)
                design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(this.findItemBox));
            else
                design.getPanelContainer(design.get(objBarcode.groupTo)).add(design.get(this.findItemSimple));

            return design;
        }
    }

    /************/

    private class ShipmentExportFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objShipment;
        private ObjectEntity objArticle;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objColorSupplier;

        private ShipmentExportFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);

            objShipment = addSingleGroupObject(shipment, "Поставка", date, sidDocument);

            //objArticle = addSingleGroupObject(article, "Артикул", sidArticle);

            //objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет", baseLM.selection, sidColorSupplier, name);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер", sidSizeSupplier);

            GroupObjectEntity gobjArticleColor= new GroupObjectEntity(genID());
            objArticle = new ObjectEntity(genID(), articleComposite, "Артикул");
            objColorSupplier = new ObjectEntity(genID(), colorSupplier, "Цвет");
            gobjArticleColor.add(objArticle);
            gobjArticleColor.add(objColorSupplier);
            addGroupObject(gobjArticleColor);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objColorSupplier, sidColorSupplier, name);
            addPropertyDraw(objArticle, objColorSupplier, mainCompositionOriginArticleColor, additionalCompositionOriginArticleColor);
            addPropertyDraw(objShipment, objArticle, objColorSupplier, sumShipmentArticleColor);

            PropertyDrawEntity quantityColumn = addPropertyDraw(quantityShipmentArticleColorSize, objShipment, objArticle, objColorSupplier, objSizeSupplier);
            quantityColumn.addColumnGroupObject(objSizeSupplier.groupTo);
            quantityColumn.propertyCaption = addPropertyObject(sidSizeSupplier, objSizeSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objShipment), Compare.EQUALS, objSupplier));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentArticleColor, objShipment, objArticle, objColorSupplier)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentSize, objShipment, objSizeSupplier)));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            //design.get(getPropertyDraw(sidDocument, objDirectInvoice)).caption = "Номер инвойса";
            //design.get(getPropertyDraw(date, objDirectInvoice)).caption = "Дата инвойса";

            //design.get(objShipment.groupTo).grid.constraints.fillVertical = 1;
            //design.get(objSku.groupTo).grid.constraints.fillVertical = 4;

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                    design.getGroupObjectContainer(objShipment.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }


    private class FreightShipmentStoreFormEntity extends BarcodeFormEntity {

        private ObjectEntity objFreight;
        private ObjectEntity objPallet;
        private ObjectEntity objFreightBox;

        private FreightShipmentStoreFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", baseLM.objectValue, date, baseLM.objectClassName, nameRouteFreight,
                                    nameExporterFreight, descriptionFreight, grossWeightFreight, volumeDataFreight, palletCountDataFreight,
                                    palletNumberFreight, freightBoxNumberFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objFreight, PropertyEditType.SELECTOR);

            LCP highlightColor = addCProp(ColorClass.instance, new Color(128, 255, 128));
            CalcPropertyObjectEntity diffPalletFreightProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, diffPalletFreight, 1), objFreight);
            getPropertyDraw(palletCountDataFreight).setPropertyBackground(diffPalletFreightProperty);
            getPropertyDraw(palletNumberFreight).setPropertyBackground(diffPalletFreightProperty);

            objPallet = addSingleGroupObject(pallet, "Паллета", barcode, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);
            setEditType(objPallet, PropertyEditType.READONLY);
            setEditType(grossWeightPallet, PropertyEditType.EDITABLE);

            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetFreight, objBarcode, objFreight));

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightPallet, objPallet), Compare.EQUALS, objFreight));

            objFreightBox = addSingleGroupObject(freightBox, "Короб", barcode, netWeightStock);
            objFreightBox.groupTo.setSingleClassView(ClassViewType.GRID);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(palletFreightBox, objFreightBox), Compare.EQUALS, objPallet));

        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 1;
            design.get(objFreightBox.groupTo).grid.constraints.fillVertical = 2;

            return design;
        }
    }

    private class BoxPalletStoreFormEntity extends BarcodeFormEntity {

        private ObjectEntity objBox;
        private ObjectEntity objPallet;
        private ObjectEntity objShipment;
        private ObjectEntity objSku;

        private BoxPalletStoreFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objPallet = addSingleGroupObject(pallet, "Паллета", barcode, nameRouteCreationPalletPallet, grossWeightPallet, freightBoxNumberPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objPallet, PropertyEditType.SELECTOR);
            setEditType(grossWeightPallet, PropertyEditType.EDITABLE);

            objBox = addSingleGroupObject(freightBox, "Короб для транспортировки", barcode, nameRouteCreationFreightBoxFreightBox, quantityStock, netWeightStock);
            objBox.groupTo.setSingleClassView(ClassViewType.GRID);
            setEditType(objBox, PropertyEditType.READONLY);

            addPropertyDraw(equalsPalletFreightBox, objBox, objPallet);

            GroupObjectEntity gobjShipmentSku = new GroupObjectEntity(genID());
            objShipment = new ObjectEntity(genID(), shipment, "Поставка");
            objSku = new ObjectEntity(genID(), sku, "SKU");
            gobjShipmentSku.add(objShipment);
            gobjShipmentSku.add(objSku);
            addGroupObject(gobjShipmentSku);

            addPropertyDraw(objShipment, date, sidDocument);
            addPropertyDraw(objSku, sidArticleSku, nameArticleSku, nameBrandSupplierArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(quantityShipmentStockSku, objShipment, objBox, objSku);
            setEditType(objSku, PropertyEditType.READONLY);

            addActionsOnObjectChange(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));
            addActionsOnObjectChange(objBarcode, addPropertyObject(barcodeActionSetPalletFreightBox, objBarcode, objPallet));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new OrFilterEntity(new CompareFilterEntity(addPropertyObject(palletFreightBox, objBox), Compare.EQUALS, objPallet),
                            new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(palletFreightBox, objBox)))),
                    "Не расписанные короба или в текущей паллете",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(palletFreightBox, objBox), Compare.EQUALS, objPallet),
                    "В текущей паллете",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroup);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStock, objBox)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentStockSku, objShipment, objBox, objSku)));
        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            return design;
        }
    }

    private class FreightShipmentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objPallet;
        private ObjectEntity objShipment;
        private ObjectEntity objDirectInvoice;
        private ObjectEntity objSku;

        private FreightShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject(freight, "Фрахт", date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight,
                    descriptionFreight, volumeDataFreight, grossWeightFreight,
                    volumeDataFreight, palletCountDataFreight, palletNumberFreight, freightBoxNumberFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objFreight, PropertyEditType.READONLY);

            CalcPropertyObjectEntity diffPalletFreightProperty = addPropertyObject(addJProp(baseLM.and1, addCProp(ColorClass.instance, new Color(128, 255, 128)), diffPalletFreight, 1), objFreight);
            getPropertyDraw(palletCountDataFreight).setPropertyBackground(diffPalletFreightProperty);
            getPropertyDraw(palletNumberFreight).setPropertyBackground(diffPalletFreightProperty);

            objShipment = addSingleGroupObject(shipment, "Поставка", date, sidDocument, nameSupplierDocument);

            addPropertyDraw(objShipment, objFreight, nameImporterShipmentFreight);

            objPallet = addSingleGroupObject(pallet, "Паллета", barcode, grossWeightPallet, freightBoxNumberPallet, nameRouteCreationPalletPallet);
            objPallet.groupTo.setSingleClassView(ClassViewType.GRID);
            setEditType(objPallet, PropertyEditType.READONLY);

            addPropertyDraw(equalsPalletFreight, objPallet, objFreight);

            objSku = addSingleGroupObject(sku, "SKU", sidArticleSku, nameArticleSku, nameBrandSupplierArticleSku, sidColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            addPropertyDraw(quantityPalletSku, objPallet, objSku);
            setEditType(objSku, PropertyEditType.READONLY);

            objDirectInvoice = addSingleGroupObject(directInvoice, "Инвойс напрямую", date, sidDocument, nameImporterDirectInvoice,
                    nameDestinationDestinationDocument, netWeightDocument, grossWeightDirectInvoice, palletNumberDirectInvoice);
            setEditType(objDirectInvoice, PropertyEditType.READONLY);
            setEditType(grossWeightDirectInvoice, PropertyEditType.EDITABLE);
            setEditType(palletNumberDirectInvoice, PropertyEditType.EDITABLE);

            addPropertyDraw(equalsDirectInvoiceFreight, objDirectInvoice, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(routeCreationPalletPallet, objPallet), Compare.EQUALS, addPropertyObject(routeFreight, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(freightBoxNumberPallet, objPallet)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityShipmentFreight, objShipment, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityPalletSku, objPallet, objSku)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new OrFilterEntity(new CompareFilterEntity(addPropertyObject(freightPallet, objPallet), Compare.EQUALS, objFreight),
                            new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(freightPallet, objPallet)))),
                    "Не расписанные паллеты или в текущем фрахте",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(freightPallet, objPallet), Compare.EQUALS, objFreight),
                    "В текущем фрахте",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroup);

            RegularFilterGroupEntity filterGroupInvoice = new RegularFilterGroupEntity(genID());
            filterGroupInvoice.addFilter(new RegularFilterEntity(genID(),
                    new OrFilterEntity(new CompareFilterEntity(addPropertyObject(freightDirectInvoice, objDirectInvoice), Compare.EQUALS, objFreight),
                            new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(freightDirectInvoice, objDirectInvoice)))),
                    "Не расписанные инвойсы или в текущем фрахте",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroupInvoice.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(freightDirectInvoice, objDirectInvoice), Compare.EQUALS, objFreight),
                    "В текущем фрахте",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroupInvoice.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupInvoice);

            freightCompleteFA = addDMFAProp(actionGroup, "Скомплектовать", this, new ObjectEntity[] {objFreight}, true,
                    addPropertyObject(addJoinAProp(executeChangeFreightClass, 1, addCProp(baseClass.objectClass, "freightComplete")), objFreight));
            freightCompleteFA.setImage("arrow_right.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidDocument, objDirectInvoice)).caption = "Номер инвойса";
            design.get(getPropertyDraw(date, objDirectInvoice)).caption = "Дата инвойса";
            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objShipment.groupTo).grid.constraints.fillVertical = 1;
            design.get(objPallet.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 2;

            design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objPallet.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSku.groupTo),
                    design.getGroupObjectContainer(objDirectInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class CreateFreightBoxFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objFreightBox;

        private CreateFreightBoxFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(1, "creationFreightBox", creationFreightBox, "Документ генерации коробов");

            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, nameRouteCreationFreightBox, quantityCreationFreightBox);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createFreightBox, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreateFreightBoxForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                setAddOnTransaction(objCreate, RomanLogicsModule.this);

            objFreightBox = addSingleGroupObject(2, "freightBox", freightBox, "Короба для транспортировки", barcode);
            setEditType(objFreightBox, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationFreightBoxFreightBox, objFreightBox), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreateFreightBoxForm = addFAProp("Печать штрих-кодов", this, objCreate);
        }
    }

    private class CreatePalletFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objPallet;

        private CreatePalletFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(1, "creationPallet", creationPallet, "Документ генерации паллет");
            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, nameRouteCreationPallet, quantityCreationPallet);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createPallet, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreatePalletForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                setAddOnTransaction(objCreate, RomanLogicsModule.this);

            objPallet = addSingleGroupObject(2, "pallet", pallet, "Паллеты для транспортировки", barcode);
            setEditType(objPallet, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationPalletPallet, objPallet), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreatePalletForm = addFAProp("Печать штрих-кодов", this, objCreate);
        }
    }


    private class CreateSkuFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCreate;
        private ObjectEntity objSku;

        private CreateSkuFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(creationSku, "Документ генерации товаров");
            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, quantityCreationSku);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createSku, objCreate);

            if (!type.equals(FormType.PRINT))
                addPropertyDraw(objCreate, printCreateSkuForm);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.ADD))
                setAddOnTransaction(objCreate, RomanLogicsModule.this);

            objSku = addSingleGroupObject(sku, "Товары", barcode);
            setEditType(objSku, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationSkuSku, objSku), Compare.EQUALS, objCreate));

            if (type.equals(FormType.PRINT))
                printCreateSkuForm = addFAProp("Печать штрих-кодов", this, objCreate);
        }
    }

    private class CreateStampFormEntity extends FormEntity<RomanBusinessLogics> {
        private ObjectEntity objCreate;
        private ObjectEntity objStamp;

        private CreateStampFormEntity(NavigatorElement parent, String sID, String caption, FormType type) {
            super(parent, sID, caption, type.equals(FormType.PRINT));

            objCreate = addSingleGroupObject(creationStamp, "Документ генерации марок");
            if (!type.equals(FormType.ADD))
                addPropertyDraw(objCreate, baseLM.objectValue);

            addPropertyDraw(objCreate, seriesOfCreationStamp, firstNumberCreationStamp, lastNumberCreationStamp, dateOfCreationStamp);

            if (type.equals(FormType.ADD))
                addPropertyDraw(createStamp, objCreate);

            if (!type.equals(FormType.LIST))
                objCreate.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (type.equals(FormType.LIST))
                addPropertyDraw(objCreate, delete);

            if (type.equals(FormType.ADD))
                setAddOnTransaction(objCreate, RomanLogicsModule.this);

            objStamp = addSingleGroupObject(stamp, "Таможенные марки", sidStamp, delete);
            setEditType(objStamp, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(creationStampStamp, objStamp), Compare.EQUALS, objCreate));

        }
    }

    private class BalanceWarehouseFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSku;

        private BalanceWarehouseFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, barcode, nameSupplierArticleSku,
                    nameBrandSupplierArticleSku, nameThemeSupplierArticleSku, nameSeasonYearArticleSku,
                    nameSubCategorySupplierArticleSku, nameCollectionSupplierArticleSku, nameSeasonYearArticleSku,
                    nameCategoryArticleSku, sidArticleSku, nameArticleSku, sidCustomCategory10Sku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameCountrySku, netWeightSku,
                    mainCompositionSku, additionalCompositionSku, quantityDirectInvoicedSku, quantityStockedSku, quantitySku, sumSku);
            addObjectActions(this, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setEditType(objSku, PropertyEditType.READONLY);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(quantitySku, objSku), Compare.GREATER, addPropertyObject(baseLM.vzero)),
                    "Ненулевые остатки",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(quantitySku, objSku), Compare.EQUALS,addPropertyObject( baseLM.vzero)),
                    "Нулевые остатки",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            return design;
        }
    }


    private class BalanceWarehousePeriodFormEntity extends DateIntervalFormEntity<RomanBusinessLogics> {

        private ObjectEntity objBrand;
        private ObjectEntity objSku;

        private BalanceWarehousePeriodFormEntity(NavigatorElement parent, String sID, String caption) {
            super(baseLM, parent, sID, caption);

            objBrand = addSingleGroupObject(4, brandSupplier, "Бренд", name);

            addPropertyDraw(invoicedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo);
            addPropertyDraw(quantityShipmentedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo);

            objSku = addSingleGroupObject(5, sku, "SKU", baseLM.selection, barcode, nameCategoryArticleSku, sidArticleSku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            //addObjectActions(this, objSku);

            addPropertyDraw(invoicedBetweenDateSku, objSku, objDateFrom, objDateTo);
            addPropertyDraw(quantityShipmentedBetweenDateSku, objSku, objDateFrom, objDateTo);
            addPropertyDraw(quantityFreightedBetweenDateSku, objSku, objDateFrom, objDateTo);

            addPropertyDraw(balanceSku, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
            setEditType(objSku, PropertyEditType.READONLY);

            addFixedFilter(new OrFilterEntity(
                               new CompareFilterEntity(addPropertyObject(invoicedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero)),
                               new CompareFilterEntity(addPropertyObject(quantityShipmentedBetweenDateBrandSupplier, objBrand, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero))));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticleSku, objSku), Compare.EQUALS, objBrand));

            addFixedFilter(new OrFilterEntity(
                               new CompareFilterEntity(addPropertyObject(invoicedBetweenDateSku, objSku, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero)),
                               new CompareFilterEntity(addPropertyObject(quantityShipmentedBetweenDateSku, objSku, objDateFrom, objDateTo), Compare.GREATER, addPropertyObject(baseLM.vzero))));

            /*RegularFilterGroupEntity filterGroupBrand = new RegularFilterGroupEntity(genID());
            filterGroupBrand.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(brandSupplierArticleSku, objSku), Compare.EQUALS, objBrand),
                    "Текущего бренда",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroupBrand.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupBrand);*/

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objBrand.groupTo),
                    design.getGroupObjectContainer(objSku.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objSku.groupTo).grid.constraints.fillHorizontal = 3;

            return design;
        }
    }


    private class SkuFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objCategory;
        private ObjectEntity objArticle;
        private ObjectEntity objSku;

        private SkuFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", name);

            objArticle = addSingleGroupObject("article", article, "Артикул", sidArticle, nameSupplierArticle, nameSeasonYearArticle,
                            nameBrandSupplierArticle, nameCollectionSupplierArticle, nameSubCategorySupplierArticle,
                            nameThemeSupplierArticle, sidGenderArticle, nameCategoryArticle, nameTypeFabricArticle, nameTypeLabelArticle, nameArticle);

            setEditType(nameSupplierArticle, PropertyEditType.READONLY);

            objSku = addSingleGroupObject(sku, "SKU", baseLM.selection, barcode, nameSupplierArticleSku, nameBrandSupplierArticleSku,
                    nameThemeSupplierArticleSku, nameSeasonYearArticleSku, nameSubCategorySupplierArticleSku, nameCollectionSupplierArticleSku,
                    sidGenderArticleSku, nameSeasonYearArticleSku, nameCategoryArticleSku,
                    nameTypeFabricArticleSku, sidArticleSku, nameArticleSku, sidColorSupplierItem, nameColorSupplierItem,
                    sidSizeSupplierItem, nameCommonSizeSku, nameCountrySku, netWeightSku,
                    mainCompositionSku, additionalCompositionSku, warrantySku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            setEditType(PropertyEditType.READONLY, objSupplier.groupTo);
            setEditType(PropertyEditType.READONLY, objCategory.groupTo);
            setEditType(PropertyEditType.READONLY, objSku.groupTo);
            setEditType(sidColorSupplierItem, PropertyEditType.EDITABLE);
            setEditType(sidSizeSupplierItem, PropertyEditType.EDITABLE);
            setEditType(netWeightSku, PropertyEditType.EDITABLE);
            setEditType(nameCountrySku, PropertyEditType.EDITABLE);
            setEditType(mainCompositionSku, PropertyEditType.EDITABLE);
            setEditType(additionalCompositionSku, PropertyEditType.EDITABLE);

            addObjectActions(this, objArticle);
            addObjectActions(this, objSku);

            RegularFilterGroupEntity filterGroupSupplierSku = new RegularFilterGroupEntity(genID());
            filterGroupSupplierSku.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(supplierArticleSku, objSku), Compare.EQUALS, objSupplier),
                    "Текущего поставщика",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroupSupplierSku.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupSupplierSku);

            RegularFilterGroupEntity filterGroupCategorySku = new RegularFilterGroupEntity(genID());
            filterGroupCategorySku.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(categoryArticleSku, objSku), Compare.EQUALS, objCategory),
                    "Текущей номенклатурной группы",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroupCategorySku.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupCategorySku);

            RegularFilterGroupEntity filterGroupSupplierArticle = new RegularFilterGroupEntity(genID());
            filterGroupSupplierArticle.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(supplierArticle, objArticle), Compare.EQUALS, objSupplier),
                    "Текущего поставщика",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            filterGroupSupplierArticle.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupSupplierArticle);

            RegularFilterGroupEntity filterGroupCategoryArticle = new RegularFilterGroupEntity(genID());
            filterGroupCategoryArticle.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(categoryArticle, objArticle), Compare.EQUALS, objCategory),
                    "Текущей номенклатурной группы",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            filterGroupCategoryArticle.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupCategoryArticle);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            specContainer.setType(ContainerType.TABBED_PANE);

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                    design.getGroupObjectContainer(objCategory.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class CommonSizeEditFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objCategory;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objGender;
        private ObjectEntity objTypeFabric;
        private ObjectEntity objCommonSize;

        private GroupObjectEntity gobjGenderSizeSupplier;

        private CommonSizeEditFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", name);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);

            gobjGenderSizeSupplier = new GroupObjectEntity(genID());
            objGender = new ObjectEntity(genID(), gender, "Пол");
            objSizeSupplier = new ObjectEntity(genID(), sizeSupplier, "Размер");
            gobjGenderSizeSupplier.add(objGender);
            gobjGenderSizeSupplier.add(objSizeSupplier);
            addGroupObject(gobjGenderSizeSupplier);

            addPropertyDraw(objGender, sidGender);
            addPropertyDraw(objSizeSupplier, sidSizeSupplier, nameSupplierSizeSupplier);
            addPropertyDraw(objSizeSupplier, objGender, objCategory, nameCommonSizeSizeSupplierGenderCategory);

            objTypeFabric = addSingleGroupObject(typeFabric, "Номенклатурная группа", name);
            addPropertyDraw(objSizeSupplier, objGender, objCategory, objTypeFabric, nameCommonSizeSizeSupplierGenderCategoryTypeFabric);

            objCommonSize = addSingleGroupObject(commonSize, "Унифицированный размер", name);
            addObjectActions(this, objCommonSize);

            RegularFilterGroupEntity filterGroupSupplierSize = new RegularFilterGroupEntity(genID());
            filterGroupSupplierSize.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier),
                    "Текущего поставщика",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroupSupplierSize.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupSupplierSize);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(sidGender, objGender)).caption = "Пол";
            design.get(getPropertyDraw(sidSizeSupplier, objSizeSupplier)).caption = "Размер";

            design.get(gobjGenderSizeSupplier).grid.constraints.fillVertical = 2;
            design.get(gobjGenderSizeSupplier).grid.constraints.fillHorizontal = 4;
            design.get(objTypeFabric.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objCommonSize.groupTo).grid.constraints.fillHorizontal = 3;

            design.addIntersection(design.getGroupObjectContainer(objCategory.groupTo),
                    design.getGroupObjectContainer(objSupplier.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(gobjGenderSizeSupplier),
                    design.getGroupObjectContainer(objTypeFabric.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objTypeFabric.groupTo),
                    design.getGroupObjectContainer(objCommonSize.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            return design;
        }
    }

    private class CommonSizeImportFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objCategory;
        private ObjectEntity objSizeSupplier;
        private ObjectEntity objGender;

        private GroupObjectEntity gobjCategoryGenderSizeSupplier;

        private CommonSizeImportFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            gobjCategoryGenderSizeSupplier = new GroupObjectEntity(genID());
            objCategory = new ObjectEntity(genID(), category, "Номенклатурная группа");
            objGender = new ObjectEntity(genID(), gender, "Пол");
            objSizeSupplier = new ObjectEntity(genID(), sizeSupplier, "Размер поставщика");
            gobjCategoryGenderSizeSupplier.add(objCategory);
            gobjCategoryGenderSizeSupplier.add(objGender);
            gobjCategoryGenderSizeSupplier.add(objSizeSupplier);
            addGroupObject(gobjCategoryGenderSizeSupplier);

            addPropertyDraw(objCategory, name);
            addPropertyDraw(objGender, sidGender);
            addPropertyDraw(objSizeSupplier, sidSizeSupplier, nameSupplierSizeSupplier);
            addPropertyDraw(objSizeSupplier, objGender, objCategory, nameCommonSizeSizeSupplierGenderCategory);
            addPropertyDraw(objSizeSupplier, objGender, objCategory, quantitySizeSupplierGenderCategory);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantitySizeSupplierGenderCategory, objSizeSupplier, objGender, objCategory), Compare.GREATER, addPropertyObject(baseLM.vzero)));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            return design;
        }
    }


    private class BalanceBrandWarehouseFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objBrand;
        private ObjectEntity objPallet;
        private ObjectEntity objInvoice;
        private ObjectEntity objBox;
        private ObjectEntity objArticle;
        private ObjectEntity objArticle2;
        private ObjectEntity objSku;
        private ObjectEntity objSku2;

        private TreeGroupEntity treeSupplierBrand;
        private TreeGroupEntity treePalletBoxArticleSku;
        private TreeGroupEntity treeInvoiceArticleSku;

        private BalanceBrandWarehouseFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, dumb1, dumb1, dumb1);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", name, quantityBrandSupplier);

            treeSupplierBrand = addTreeGroupObject(objSupplier.groupTo, objBrand.groupTo);

            objPallet = addSingleGroupObject(pallet, "Паллета", barcode);
            addPropertyDraw(quantityPalletBrandSupplier, objPallet, objBrand);

            objInvoice = addSingleGroupObject(directInvoice, "Инвойс (напрямую)", sidDocument);
            addPropertyDraw(quantityDocumentBrandSupplier, objInvoice, objBrand);
            addPropertyDraw(objInvoice, date);

            objBox = addSingleGroupObject(freightBox, "Короб", barcode);
            addPropertyDraw(quantityStockBrandSupplier, objBox, objBrand);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle);
            addPropertyDraw(quantityStockArticle, objBox, objArticle);
            addPropertyDraw(objArticle, nameArticleSku, nameThemeSupplierArticle, nameCategoryArticleSku);

            objArticle2 = addSingleGroupObject(article, "Артикул", sidArticle);
            addPropertyDraw(quantityDocumentArticle, objInvoice, objArticle2);
            addPropertyDraw(objArticle2, dumb1, nameArticleSku, nameThemeSupplierArticle, nameCategoryArticleSku);

            objSku = addSingleGroupObject(sku, "SKU", barcode);
            addPropertyDraw(quantityStockSku, objBox, objSku);
            addPropertyDraw(objSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameCountrySku, netWeightSku, mainCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            objSku2 = addSingleGroupObject(sku, "SKU", barcode);
            addPropertyDraw(quantityDocumentSku, objInvoice, objSku2);
            addPropertyDraw(objSku2, dumb1, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameCountrySku, netWeightSku, mainCompositionSku, additionalCompositionSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku2.groupTo);

            setEditType(objSupplier, PropertyEditType.READONLY);
            setEditType(objBrand, PropertyEditType.READONLY);
            setEditType(objInvoice, PropertyEditType.READONLY);
            setEditType(objPallet, PropertyEditType.READONLY);
            setEditType(objBox, PropertyEditType.READONLY);
            setEditType(objArticle, PropertyEditType.READONLY);
            setEditType(objArticle2, PropertyEditType.READONLY);
            setEditType(objSku, PropertyEditType.READONLY);
            setEditType(objSku2, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplier, objBrand), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(palletFreightBox, objBox), Compare.EQUALS, objPallet));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticle, objArticle), Compare.EQUALS, objBrand));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticle, objArticle2), Compare.EQUALS, objBrand));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSku, objSku), Compare.EQUALS, objArticle));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(articleSku, objSku2), Compare.EQUALS, objArticle2));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentBrandSupplier, objInvoice, objBrand)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityPalletBrandSupplier, objPallet, objBrand)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockBrandSupplier, objBox, objBrand)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockArticle, objBox, objArticle)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentArticle, objInvoice, objArticle2)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityStockSku, objBox, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objInvoice, objSku2)));

            treePalletBoxArticleSku = addTreeGroupObject(objPallet.groupTo, objBox.groupTo, objArticle.groupTo, objSku.groupTo);
            treeInvoiceArticleSku = addTreeGroupObject(objInvoice.groupTo, objArticle2.groupTo, objSku2.groupTo);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.getTreeContainer(treeSupplierBrand).setTitle("Поставщики и их бренды");
            design.getTreeContainer(treePalletBoxArticleSku).setTitle("В паллетах и коробах");
            design.getTreeContainer(treeInvoiceArticleSku).setTitle("В инвойсах напрямую");

            design.get(treeSupplierBrand).constraints.fillVertical = 2;
            design.get(treePalletBoxArticleSku).constraints.fillVertical = 5;
            design.get(treeInvoiceArticleSku).constraints.fillVertical = 5;

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.get(treePalletBoxArticleSku));
            specContainer.add(design.getTreeContainer(treePalletBoxArticleSku));
            specContainer.add(design.getTreeContainer(treeInvoiceArticleSku));
            specContainer.setType(ContainerType.TABBED_PANE);

            return design;
        }
    }

    private class InvoiceShipmentFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objInvoice;
        private ObjectEntity objBox;
        private ObjectEntity objSku;
        private ObjectEntity objSku2;

        private InvoiceShipmentFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);

            objInvoice = addSingleGroupObject(invoice, "Инвойс", date, sidDocument, baseLM.objectClassName, quantityDocument, quantityInvoice);

            objBox = addSingleGroupObject(supplierBox, "Короб из инвойса", sidSupplierBox);

            objSku = addSingleGroupObject(sku, "Товар в инвойсе", barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            addPropertyDraw(quantityDocumentSku, objInvoice, objSku);
            addPropertyDraw(quantityInvoiceSku, objInvoice, objSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            objSku2 = addSingleGroupObject(sku, "Товар в коробе", barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku,
                    sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);

            addPropertyDraw(quantityListSku, objBox, objSku2);
            addPropertyDraw(quantitySupplierBoxSku, objBox, objSku2);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku2.groupTo);

            setEditType(objSupplier, PropertyEditType.READONLY);
            setEditType(objInvoice, PropertyEditType.READONLY);
            setEditType(objBox, PropertyEditType.READONLY);
            setEditType(objSku, PropertyEditType.READONLY);
            setEditType(objSku2, PropertyEditType.READONLY);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierDocument, objInvoice), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(boxInvoiceSupplierBox, objBox), Compare.EQUALS, objInvoice));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(quantityDocumentSku, objInvoice, objSku)),
                    new NotNullFilterEntity(addPropertyObject(quantityInvoiceSku, objInvoice, objSku))));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(quantityListSku, objBox, objSku2)),
                    new NotNullFilterEntity(addPropertyObject(quantitySupplierBoxSku, objBox, objSku2))));

            RegularFilterGroupEntity filterGroupDiffInvoice = new RegularFilterGroupEntity(genID());
            filterGroupDiffInvoice.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(diffDocumentInvoiceSku, objInvoice, objSku))),
                    "По отличающимся",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupDiffInvoice);

            RegularFilterGroupEntity filterGroupDiffBox = new RegularFilterGroupEntity(genID());
            filterGroupDiffBox.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(diffListSupplierBoxSku, objBox, objSku2))),
                    "По отличающимся",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupDiffBox);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(baseLM.objectClassName, objInvoice)).caption = "Тип инвойса";
            design.get(getPropertyDraw(sidDocument, objInvoice)).caption = "Номер инвойса";

            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objInvoice.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSupplier.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objInvoice.groupTo).grid.constraints.fillHorizontal = 5;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;
            design.get(objBox.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku2.groupTo).grid.constraints.fillVertical = 4;
            design.get(objBox.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objSku2.groupTo).grid.constraints.fillHorizontal = 5;

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                    design.getGroupObjectContainer(objInvoice.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView boxContainer = design.createContainer("По коробам");
            boxContainer.add(design.getGroupObjectContainer(objBox.groupTo));
            boxContainer.add(design.getGroupObjectContainer(objSku2.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objSku.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            specContainer.add(boxContainer);
            specContainer.setType(ContainerType.TABBED_PANE);

            return design;
        }
    }

    private class FreightChangeFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objArticle;
        private ObjectEntity objCategory;
        private ObjectEntity objCategory2;
        private ObjectEntity objGender;
        private ObjectEntity objComposition;
        private ObjectEntity objTypeFabric;
        private ObjectEntity objSku;
        private ObjectEntity objSkuFreight;
        GroupObjectEntity gobjCategoryGenderCompositionTypeFabric;

        private RegularFilterGroupEntity filterGroupCategory;
        private RegularFilterGroupEntity filterGroupCustomCategory10;
        private RegularFilterGroupEntity filterGroupCountry;

        private FreightChangeFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject("freight", freightComplete, "Фрахт", date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, grossWeightFreight, nameCurrencyFreight, sumFreightFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objFreight, PropertyEditType.READONLY);

            objImporter = addSingleGroupObject(importer, "Импортёр", name, addressSubject);
            setEditType(objImporter, PropertyEditType.READONLY);
            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            setEditType(quantityImporterFreight, PropertyEditType.READONLY);

            objArticle = addSingleGroupObject(article, "Артикул", baseLM.selection, sidArticle, nameBrandSupplierArticle, originalNameArticle, nameCategoryArticle, nameArticle,
                    sidCustomCategoryOriginArticle, nameCountryOfOriginArticle, mainCompositionOriginArticle, additionalCompositionOriginArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(quantityFreightArticle, objFreight, objArticle);

            objCategory2 = addSingleGroupObject(category, "Номенклатурная группа", name);

            gobjCategoryGenderCompositionTypeFabric = new GroupObjectEntity(genID(), "Группировка");
            objGender = new ObjectEntity(genID(), gender, "Пол");
            objComposition = new ObjectEntity(genID(), COMPOSITION_CLASS, "Состав");
            objTypeFabric = new ObjectEntity(genID(), typeFabric, "Тип одежды");

            gobjCategoryGenderCompositionTypeFabric.add(objGender);
            gobjCategoryGenderCompositionTypeFabric.add(objComposition);
            gobjCategoryGenderCompositionTypeFabric.add(objTypeFabric);
            addGroupObject(gobjCategoryGenderCompositionTypeFabric);

            addPropertyDraw(objGender, sidGender);
            addPropertyDraw(objComposition, baseLM.objectValue);
            addPropertyDraw(objTypeFabric, name);
            setEditType(objCategory2, PropertyEditType.READONLY);
            setEditType(objGender, PropertyEditType.READONLY);
            setEditType(objTypeFabric, PropertyEditType.READONLY);

            addPropertyDraw(sidCustomCategory10CategoryGenderCompositionTypeFabricFreight, objCategory2, objGender, objComposition, objTypeFabric, objFreight);
            addPropertyDraw(quantityFreightCategoryGenderCompositionTypeFabric, objFreight, objCategory2, objGender, objComposition, objTypeFabric);

            objCategory = addSingleGroupObject(category, "Номенклатурная группа", name);
            setEditType(objCategory, PropertyEditType.READONLY);

            objSku = addSingleGroupObject("sku", sku, "SKU", barcode, sidArticleSku,
                     nameBrandSupplierArticleSku, nameCategoryArticleSku, sidGenderArticleSku, nameTypeFabricArticleSku,
                     sidCustomCategoryOriginArticleSku, nameCountrySku, netWeightSku);

            CalcPropertyObjectEntity diffCountRelationCustomCategory10SkuProperty = addPropertyObject(addJProp(baseLM.and1, addCProp(ColorClass.instance, new Color(128, 255, 255)), diffCountRelationCustomCategory10Sku, 1), objSku);
            //getPropertyDraw(nameSubCategorySku).setPropertyBackground(diffCountRelationCustomCategory10SkuProperty);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            addGCAProp(actionGroup, "translationAllMainComposition", "Перевести все", objSku.groupTo, translationMainCompositionSku);
            addGCAProp(actionGroup, "translationAllAdditionalComposition", "Перевести все", objSku.groupTo, translationAdditionalCompositionSku);
            addGCAProp(actionGroup, "translationAllMainCompositionLanguage", "Перевести все", objSku.groupTo, translationMainCompositionSkuFreight, 1, 2, 1);
            addGCAProp(actionGroup, "translationAllAdditionalCompositionLanguage", "Перевести все", objSku.groupTo, translationAdditionalCompositionSkuFreight, 1, 2, 1);

            setEditType(PropertyEditType.READONLY, objSku.groupTo);
            //setEditType(sidCustomCategory10Sku, PropertyEditType.EDITABLE, objSku.groupTo);
            //setEditType(nameSubCategorySku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(nameCountrySku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(netWeightSku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(mainCompositionOriginSku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(additionalCompositionOriginSku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(mainCompositionSku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(additionalCompositionSku, PropertyEditType.EDITABLE, objSku.groupTo);

            objSkuFreight = addSingleGroupObject("skuFreight", sku, "Позиции фрахта", baseLM.selection, barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem,
                    sidSizeSupplierItem, nameBrandSupplierArticleSku, nameArticleSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSkuFreight.groupTo);

            addPropertyDraw(quantityFreightSku, objFreight, objSku);
            addPropertyDraw(quantityDirectFreightSku, objFreight, objSku);
            addPropertyDraw(sidCustomCategoryOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(sidCustomCategory10FreightSku, objFreight, objSkuFreight);
            addPropertyDraw(nameSubCategoryFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(nameCountryOfOriginFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(netWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(grossWeightFreightSku, objFreight, objSkuFreight);
            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(quantityProxyImporterFreightSku, objImporter, objFreight, objSkuFreight);
            addPropertyDraw(quantityDirectImporterFreightSku, objImporter, objFreight, objSkuFreight);

            addGCAProp(actionGroup, "translationAllFreightMainComposition", "Перевести все", objSkuFreight.groupTo, translationMainCompositionFreightSku, 1, 2, 2);
            addGCAProp(actionGroup, "translationAllFreightAdditionalComposition", "Перевести все", objSkuFreight.groupTo, translationAdditionalCompositionFreightSku, 1, 2, 2);
            addGCAProp(actionGroup, "translationAllLanguageMainComposition", "Перевести все", objSkuFreight.groupTo, translationMainCompositionLanguageFreightSku, 1, 2, 2);
            addGCAProp(actionGroup, "translationAllLanguageAdditionalComposition", "Перевести все", objSkuFreight.groupTo, translationAdditionalCompositionLanguageFreightSku, 1, 2, 2);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightCategoryGenderCompositionTypeFabric, objFreight, objCategory2, objGender, objComposition, objTypeFabric)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightCategory, objFreight, objCategory)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightCategory, objFreight, objCategory2)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightSku, objFreight, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSkuFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightArticle, objFreight, objArticle)));

            filterGroupCategory = new RegularFilterGroupEntity(genID());
            filterGroupCategory.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(categoryArticleSku, objSku), Compare.EQUALS, objCategory),
                    "По ном. группе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0)));
            filterGroupCategory.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroupCategory);

            filterGroupCustomCategory10 = new RegularFilterGroupEntity(genID());
            filterGroupCustomCategory10.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(customCategory10SkuFreight, objSku, objFreight))),
                    "Без ТН ВЭД",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroupCustomCategory10);

            filterGroupCountry = new RegularFilterGroupEntity(genID());
            filterGroupCountry.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(countryOfOriginSku, objSku))),
                    "Без страны",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterGroupCountry);

            RegularFilterGroupEntity filterGroupWeight = new RegularFilterGroupEntity(genID());
            filterGroupWeight.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(netWeightSku, objSku))),
                    "Без веса",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0)));
            addRegularFilterGroup(filterGroupWeight);

            RegularFilterGroupEntity filterGroupComposition = new RegularFilterGroupEntity(genID());
            filterGroupComposition.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(mainCompositionOriginSku, objSku))),
                    "Без состава",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));
            addRegularFilterGroup(filterGroupComposition);

            setPageSize(0);

            freightChangedFA = addDMFAProp(actionGroup, "Обработать", this,
                    new ObjectEntity[]{objFreight}, true);
            freightChangedFA.setImage("arrow_right.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.mainContainer.preferredSize = new Dimension(1280, 1024);

            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";
            design.get(getPropertyDraw(name, objGender)).caption = "Пол";
            design.get(getPropertyDraw(name, objComposition)).caption = "Состав";
            design.get(getPropertyDraw(name, objTypeFabric)).caption = "Тип одежды";

            design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
            design.get(gobjCategoryGenderCompositionTypeFabric).grid.constraints.fillVertical = 4;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 4;
            design.get(objCategory.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objSku.groupTo).grid.constraints.fillHorizontal = 4;
            design.get(objCategory.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 4;
            design.get(objSkuFreight.groupTo).grid.constraints.fillVertical = 4;

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo),
                    design.getGroupObjectContainer(objImporter.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objCategory.groupTo),
                    design.getGroupObjectContainer(objSku.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView skuContainer = design.createContainer("SKU");
            skuContainer.add(design.getGroupObjectContainer(objCategory.groupTo));
            skuContainer.add(design.getGroupObjectContainer(objSku.groupTo));

            ContainerView categoryContainer = design.createContainer("Группы");
            categoryContainer.add(design.getGroupObjectContainer(objCategory2.groupTo));
            categoryContainer.add(design.getGroupObjectContainer(gobjCategoryGenderCompositionTypeFabric));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            specContainer.add(categoryContainer);
            specContainer.add(skuContainer);
            specContainer.add(design.getGroupObjectContainer(objSkuFreight.groupTo));
            specContainer.setType(ContainerType.TABBED_PANE);

            //design.get(filterGroupCategory).drawToToolbar = false;
            //design.get(filterGroupCustomCategory10).drawToToolbar = false;
            //design.get(filterGroupCountry).drawToToolbar = false;

            return design;
        }
    }

    public class AnnexInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private AnnexInvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            this.translate = translate;

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
            addGroupObject(gobjFreightImporter);

            addPropertyDraw(objFreight, date, baseLM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, name);
                addPropertyDraw(objImporter, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight);
                addPropertyDraw(objFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin);
                addPropertyDraw(objImporter, addressOriginSubject);
            }

            addPropertyDraw(objImporter, contractImporter);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumImporterFreightTypeInvoice);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", BL.getModule("Country").getClassByName("Country"), "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroupObject(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objArticle, nameBrandSupplierArticle);

            if (translate) {
                addPropertyDraw(objArticle, nameCategoryArticle);
                addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);
            }

            if (!translate) {
                addPropertyDraw(objArticle, nameArticle);
                addPropertyDraw(objComposition, baseLM.objectValue);
            }

            addPropertyDraw(objCountry, BL.getModule("Country").getLCPByName("sidCountry"));
            if (!translate)
                addPropertyDraw(objCountry, BL.getModule("Country").getLCPByName("nameOriginCountry"));

            if (translate)
                addPropertyDraw(objCountry, name);

            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            if (!translate)
                addPropertyDraw(objArticle, nameOriginUnitOfMeasureArticle);

            if (translate)
                addPropertyDraw(objArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(netWeightImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(grossWeightImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(sidImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                annexInvoiceOriginFormImporterFreight = addFAProp("Приложение", this, objImporter, objFreight, objTypeInvoice);

            if (translate)
                annexInvoiceFormImporterFreight = addFAProp("Приложение (перевод)", this, objImporter, objFreight, objTypeInvoice);
        }
    }

    public class InvoiceExportFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
        public ObjectEntity objFreightBox;
        public ObjectEntity objSku;

        private GroupObjectEntity gobjFreightImporterTypeInvoice;
        private GroupObjectEntity gobjFreightBoxSku;

        private InvoiceExportFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporterTypeInvoice = new GroupObjectEntity(1, "freightImporterTypeInvoice");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporterTypeInvoice.add(objFreight);
            gobjFreightImporterTypeInvoice.add(objImporter);
            gobjFreightImporterTypeInvoice.add(objTypeInvoice);
            addGroupObject(gobjFreightImporterTypeInvoice);

            addPropertyDraw(objFreight, date, baseLM.objectClassName);
            addPropertyDraw(objImporter, name);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice);
            addPropertyDraw(objTypeInvoice, name);

            gobjFreightImporterTypeInvoice.initClassView = ClassViewType.PANEL;

            gobjFreightBoxSku = new GroupObjectEntity(4, "freightBoxSku");
            objFreightBox = new ObjectEntity(5, "freightBox", freightBox, "Короб");
            objSku = new ObjectEntity(6, "sku", sku, "SKU");

            gobjFreightBoxSku.add(objFreightBox);
            gobjFreightBoxSku.add(objSku);
            addGroupObject(gobjFreightBoxSku);

            addPropertyDraw(objFreightBox, barcode);
            addPropertyDraw(objSku, sidArticleSku, originalNameArticleSku, sidColorSupplierItem, sidSizeSupplierItem, nameCommonSizeSku);
            addPropertyDraw(objFreight, objSku, mainCompositionFreightSku, nameCountryOfOriginFreightSku);
            addPropertyDraw(objSku, barcode, nameBrandSupplierArticleSku, nameCountryBrandSupplierSku, sidGenderArticleSku, nameThemeSupplierArticleSku, nameSeasonYearArticleSku, nameCategoryArticleSku, nameSupplierArticleSku);
            //setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);
//            getPropertyDraw(sidImporterFreightTypeInvoice).toDraw = objSku.groupTo;
//            getPropertyDraw(dateImporterFreightTypeInvoice).toDraw = objSku.groupTo;
            //addPropertyDraw(sidContractInProxyImporterStockSku, objImporter, objFreightBox, objSku);
            //addPropertyDraw(dateContractInProxyImporterStockSku, objImporter, objFreightBox, objSku);

            addPropertyDraw(quantityImporterStockSku, objImporter, objFreightBox, objSku);
            addPropertyDraw(priceInvoiceImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(sumInvoiceImporterStockSku, objImporter, objFreightBox, objSku);
            addPropertyDraw(priceInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(RRPInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(sumInImporterStockSku, objImporter, objFreightBox, objSku);

            gobjFreightBoxSku.initClassView = ClassViewType.GRID;

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightSku, objFreight, objSku), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockSku, objImporter, objFreightBox, objSku)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));

            invoiceExportFormImporterFreight = addFAProp("Экспорт", this, objImporter, objFreight, objTypeInvoice);

            setEditType(PropertyEditType.READONLY);
        }
    }

    public class InvoiceFromFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private InvoiceFromFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            this.translate = translate;

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
            addGroupObject(gobjFreightImporter);

            addPropertyDraw(objFreight, date, baseLM.objectClassName, nameCurrencyFreight, symbolCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, name);
                addPropertyDraw(objImporter, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight);
                addPropertyDraw(objFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin);
                addPropertyDraw(objImporter, addressOriginSubject);
            }

            addPropertyDraw(objImporter, sidImporter);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight, conditionShipmentContractImporterFreight, conditionPaymentContractImporterFreight);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice, quantityImporterFreightTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumImporterFreightTypeInvoice);

            addPropertyDraw(objTypeInvoice, name);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", BL.getModule("Country").getClassByName("Country"), "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroupObject(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objArticle, nameBrandSupplierArticle);

            if (translate) {
                addPropertyDraw(objArticle, nameCategoryArticle);
                addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);
            }

            if (!translate) {
                addPropertyDraw(objArticle, nameArticle, nameOriginCategoryArticle);
                addPropertyDraw(objComposition, baseLM.objectValue);
            }

            addPropertyDraw(objArticle, coefficientArticle);

            addPropertyDraw(objCountry, BL.getModule("Country").getLPByName("sidCountry"));
            if (!translate)
                addPropertyDraw(objCountry, BL.getModule("Country").getLPByName("nameOriginCountry"));

            if (translate)
                addPropertyDraw(objCountry, name);

            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            if (!translate)
                addPropertyDraw(objArticle, nameOriginUnitOfMeasureArticle);

            if (translate)
                addPropertyDraw(objArticle, nameUnitOfMeasureArticle, languageFreight);

            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                invoiceOriginFormImporterFreight = addFAProp("Инвойс", this, objImporter, objFreight, objTypeInvoice);

            if (translate)
                invoiceFormImporterFreight = addFAProp("Инвойс (перевод)", this, objImporter, objFreight, objTypeInvoice);
        }
    }


    public class DeclarantFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objTypeInvoice;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private DeclarantFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(9, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
            addGroupObject(gobjFreightImporter);

            addPropertyDraw(objFreight, date, baseLM.objectClassName, nameCurrencyFreight, symbolCurrencyFreight);

            addPropertyDraw(objImporter, sidImporter);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, conditionShipmentContractImporterFreight, conditionPaymentContractImporterFreight);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice, quantityImporterFreightTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumImporterFreightTypeInvoice);

            addPropertyDraw(objTypeInvoice, name);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", BL.getModule("Country").getClassByName("Country"), "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroupObject(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objArticle, nameBrandSupplierArticle);

            addPropertyDraw(objArticle, nameCategoryArticle);
            addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(objCountry, name);
            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(objArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

        }
    }


    public class ProformInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objArticle;
        public ObjectEntity objCategory;
        public ObjectEntity objComposition;
        public ObjectEntity objCountry;

        private GroupObjectEntity gobjFreightImporter;
        private GroupObjectEntity gobjArticleCompositionCountryCategory;

        private ProformInvoiceFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            this.translate = translate;

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroupObject(gobjFreightImporter);

            addPropertyDraw(objFreight, date, baseLM.objectClassName, nameCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight);
                addPropertyDraw(objFreight, addressExporterFreight);
                addPropertyDraw(objImporter, name);
                addPropertyDraw(objImporter, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight);
                addPropertyDraw(objFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin);
                addPropertyDraw(objImporter, addressOriginSubject);
            }

            addPropertyDraw(objImporter, contractImporter);
            addPropertyDraw(objImporter, objFreight, netWeightImporterFreight, grossWeightImporterFreight, sumImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            gobjArticleCompositionCountryCategory = new GroupObjectEntity(4, "gobjArticleCompositionCountryCategory");
            objArticle = new ObjectEntity(5, "article", article, "Артикул");
            objComposition = new ObjectEntity(6, "composition", COMPOSITION_CLASS, "Состав");
            objCountry = new ObjectEntity(7, "country", BL.getModule("Country").getClassByName("Country"), "Страна");
            objCategory = new ObjectEntity(8, "category", customCategory10, "ТН ВЭД");

            gobjArticleCompositionCountryCategory.add(objArticle);
            gobjArticleCompositionCountryCategory.add(objComposition);
            gobjArticleCompositionCountryCategory.add(objCountry);
            gobjArticleCompositionCountryCategory.add(objCategory);
            addGroupObject(gobjArticleCompositionCountryCategory);

            addPropertyDraw(objArticle, sidArticle);
            addPropertyDraw(objArticle, nameBrandSupplierArticle);

            if (translate) {
                addPropertyDraw(objArticle, nameCategoryArticle);
                addPropertyDraw(compositionFreightArticleCompositionCountryCategory, objFreight, objArticle, objComposition, objCountry, objCategory);
            }

            if (!translate) {
                addPropertyDraw(objArticle, nameArticle);
                addPropertyDraw(objComposition, baseLM.objectValue);
            }

            addPropertyDraw(objCountry, BL.getModule("Country").getLCPByName("sidCountry"));
            if (!translate)
                addPropertyDraw(objCountry, BL.getModule("Country").getLCPByName("nameOriginCountry"));

            if (translate)
                addPropertyDraw(objCountry, name);

            addPropertyDraw(objCategory, sidCustomCategory10);
            addPropertyDraw(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            if (!translate)
                addPropertyDraw(objArticle, nameOriginUnitOfMeasureArticle);

            if (translate)
                addPropertyDraw(objArticle, nameUnitOfMeasureArticle);

            addPropertyDraw(priceImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);
            addPropertyDraw(sumImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(declarationExport, objImporter, objFreight);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticleCompositionCountryCategory, objImporter, objFreight, objArticle, objComposition, objCountry, objCategory)));

            if (!translate)
                proformOriginFormImporterFreight = addFAProp("Инвойс-проформа", this, objImporter, objFreight);

            if (translate)
                proformFormImporterFreight = addFAProp("Инвойс-проформа (перевод)", this, objImporter, objFreight);
        }
    }



    private class ListFreightUnitFreightFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objFreightUnit;
        private ObjectEntity objBrand;

        private PropertyDrawEntity nameBrand;

        private GroupObjectEntity gobjFreightImporter;

        private ListFreightUnitFreightFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroupObject(gobjFreightImporter);

            addPropertyDraw(objFreight, date);
            addPropertyDraw(objImporter, name);
            addPropertyDraw(objImporter, objFreight, dateImporterFreight, sidImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objBrand = addSingleGroupObject(4, "brand", brandSupplier, "Бренд");
            nameBrand = addPropertyDraw(name, objBrand);

            objFreightUnit = addSingleGroupObject(5, "freightUnit", freightUnit, "Короб", barcode, nameDestinationFreightUnit);

            addPropertyDraw(quantityFreightUnitBrandSupplier, objFreightUnit, objBrand);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightUnit, objFreightUnit), Compare.EQUALS, objFreight));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightUnitBrandSupplier, objFreightUnit, objBrand)));

            listFreightUnitFormImporterFreight = addFAProp("Короба фрахта", this, objImporter, objFreight);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            return design;
        }
    }

    private class PackingListFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean translate;

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objFreightBox;
        private ObjectEntity objArticle;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objSku;

        private GroupObjectEntity gobjFreightImporter;

        private PackingListFormEntity(NavigatorElement parent, String sID, String caption, boolean translate) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objTypeInvoice = new ObjectEntity(6, "typeInvoice", typeInvoice, "Тип инвойса");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            gobjFreightImporter.add(objTypeInvoice);
            addGroupObject(gobjFreightImporter);

            addPropertyDraw(objFreight, date, baseLM.objectClassName, nameCurrencyFreight, symbolCurrencyFreight);

            if (translate) {
                addPropertyDraw(objFreight, nameExporterFreight, addressExporterFreight);
                addPropertyDraw(objImporter, name, addressSubject);
            }

            if (!translate) {
                addPropertyDraw(objFreight, nameOriginExporterFreight, addressOriginExporterFreight);
                addPropertyDraw(objImporter, nameOrigin, addressOriginSubject);
            }

            addPropertyDraw(objImporter, sidImporter);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, quantityImporterFreightTypeInvoice, netWeightImporterFreightTypeInvoice, grossWeightImporterFreightTypeInvoice, sumInOutImporterFreightTypeInvoice);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight, conditionShipmentContractImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objFreightBox = addSingleGroupObject(4, "freightBox", freightBox, "Короб", barcode);
            addPropertyDraw(objImporter, objFreightBox, objTypeInvoice, netWeightImporterFreightUnitTypeInvoice, grossWeightImporterFreightUnitTypeInvoice, quantityImporterStockTypeInvoice);

            objArticle = addSingleGroupObject(5, "article", article, "Артикул", sidArticle, nameBrandSupplierArticle);

            if (translate)
                addPropertyDraw(objArticle, nameCategoryArticle);               

            if (!translate)
                addPropertyDraw(objArticle, nameArticle);

            addPropertyDraw(quantityImporterStockArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(netWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(grossWeightImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);
            addPropertyDraw(sumImporterFreightUnitArticle, objImporter, objFreightBox, objArticle);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(typeInvoiceFreightArticle, objFreight, objArticle), Compare.EQUALS, objTypeInvoice));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockTypeInvoice, objImporter, objFreightBox, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterStockArticle, objImporter, objFreightBox, objArticle)));

            if (!translate)
                packingListOriginFormImporterFreight = addFAProp("Упаковочный лист", this, objImporter, objFreight, objTypeInvoice);

            if (translate)
                packingListFormImporterFreight = addFAProp("Упаковочный лист (перевод)", this, objImporter, objFreight, objTypeInvoice);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objFreightBox.groupTo).grid.constraints.fillVertical = 2;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            return design;
        }
    }

    public class SbivkaFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objImporter;
        public ObjectEntity objCustomCategory6;
        public ObjectEntity objCategory;

        private GroupObjectEntity gobjFreightImporter;

        private SbivkaFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporter = new GroupObjectEntity(1, "freightImporter");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");

            gobjFreightImporter.add(objFreight);
            gobjFreightImporter.add(objImporter);
            addGroupObject(gobjFreightImporter);

            addPropertyDraw(objFreight, date, nameExporterFreight, addressExporterFreight, nameCurrencyFreight);
            addPropertyDraw(objImporter, name, addressSubject, contractImporter);
            addPropertyDraw(objImporter, objFreight, quantityProxyImporterFreight, netWeightImporterFreight, grossWeightImporterFreight, sumSbivkaImporterFreight, sidImporterFreight, dateImporterFreight);

            gobjFreightImporter.initClassView = ClassViewType.PANEL;

            objCustomCategory6 = addSingleGroupObject(4, "customCategory6", customCategory6, "ТН ВЭД");

            addPropertyDraw(objCustomCategory6, sidCustomCategory6);
            addPropertyDraw(objImporter, objFreight, objCustomCategory6, quantityProxyImporterFreightCustomCategory6, netWeightImporterFreightCustomCategory6, grossWeightImporterFreightCustomCategory6, sumImporterFreightCustomCategory6);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityProxyImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityProxyImporterFreightCustomCategory6, objImporter, objFreight, objCustomCategory6)));

            objCategory = addSingleGroupObject(5, "category", category, "Номенклатурная группа", name, nameUnitOfMeasureCategory);
            addPropertyDraw(objImporter, objFreight, objCustomCategory6, objCategory, quantityProxyImporterFreightCustomCategory6Category, netWeightImporterFreightCustomCategory6Category, grossWeightImporterFreightCustomCategory6Category, sumImporterFreightCustomCategory6Category);
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityProxyImporterFreightCustomCategory6Category, objImporter, objFreight, objCustomCategory6, objCategory)));

            sbivkaFormImporterFreight = addFAProp("Сбивка", this, objImporter, objFreight);
        }
    }

    public class SbivkaSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        public ObjectEntity objFreight;
        public ObjectEntity objSupplier;
        public ObjectEntity objImporter;
        public ObjectEntity objCustomCategory6;
        public ObjectEntity objCategory;

        private GroupObjectEntity gobjFreightImporterSupplier;

        private SbivkaSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption, true);

            gobjFreightImporterSupplier = new GroupObjectEntity(1, "freightImporterSupplier");

            objFreight = new ObjectEntity(2, "freight", freightPriced, "Фрахт");
            objImporter = new ObjectEntity(3, "importer", importer, "Импортер");
            objSupplier = new ObjectEntity(4, "supplier", supplier, "Поставщик");

            gobjFreightImporterSupplier.add(objFreight);
            gobjFreightImporterSupplier.add(objImporter);
            gobjFreightImporterSupplier.add(objSupplier);
            addGroupObject(gobjFreightImporterSupplier);

            addPropertyDraw(objFreight, date, nameCurrencyFreight);
            addPropertyDraw(objImporter, name, addressSubject, contractImporter);
            addPropertyDraw(objSupplier, name);

            gobjFreightImporterSupplier.initClassView = ClassViewType.PANEL;

            objCustomCategory6 = addSingleGroupObject(5, "customCategory6", customCategory6, "ТН ВЭД");

            addPropertyDraw(objCustomCategory6, sidCustomCategory6, nameCustomCategory);
            addPropertyDraw(objImporter, objFreight, objSupplier, objCustomCategory6, quantityDirectImporterFreightSupplierCustomCategory6, netWeightImporterFreightSupplierCustomCategory6, grossWeightImporterFreightSupplierCustomCategory6, sumImporterFreightSupplierCustomCategory6);
            addPropertyDraw(objImporter, objFreight, objSupplier, quantityImporterFreightSupplier, netWeightImporterFreightSupplier, grossWeightImporterFreightSupplier, sumImporterFreightSupplier);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSupplier, objImporter, objFreight, objSupplier)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDirectImporterFreightSupplierCustomCategory6, objImporter, objFreight, objSupplier, objCustomCategory6)));

            objCategory = addSingleGroupObject(6, "category", category, "Номенклатурная группа", name);
            addPropertyDraw(quantityDirectImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);
            addPropertyDraw(netWeightImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);
            addPropertyDraw(grossWeightImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);
            addPropertyDraw(sumImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityDirectImporterFreightSupplierCustomCategory6Category, objImporter, objFreight, objSupplier, objCustomCategory6, objCategory)));

            sbivkaFormImporterFreightSupplier = addFAProp("Сбивка по поставщику", this, objImporter, objFreight, objSupplier);
        }
    }

    private class ColorSizeSupplierFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objSupplier;
        private ObjectEntity objColor;
        private ObjectEntity objSize;
        private ObjectEntity objBrand;
        private ObjectEntity objCountry;
        private ObjectEntity objTheme;
        private ObjectEntity objCollection;
        private ObjectEntity objCategory;
        private ObjectEntity objSubCategory;

        private ObjectEntity objSeason;
        private ObjectEntity objGroupSize;
        private ObjectEntity objGenderSupplier;

        private ColorSizeSupplierFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name, nameBrandSupplierSupplier, nameCurrencySupplier, BL.LegalEntity.getLPByName("addressLegalEntity"), BL.LegalEntity.getLPByName("dialogAddressLegalEntity"));
            addObjectActions(this, objSupplier);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд", sidBrandSupplier, customsSIDBrandSupplier, name, nameCountryBrandSupplier, sidGenderBrandSupplier);
            addObjectActions(this, objBrand);

            objColor = addSingleGroupObject(colorSupplier, "Цвет", sidColorSupplier, name, translateNameColorSupplier);
            addObjectActions(this, objColor);

            objGroupSize = addSingleGroupObject(sizeGroupSupplier, "Размерная сетка", name);
            addObjectActions(this, objGroupSize);

            objSize = addSingleGroupObject(sizeSupplier, "Размер", sidSizeSupplier, nameGroupSizeSupplier, orderSizeSupplier);
            addObjectActions(this, objSize);

            addPropertyDraw(equalsGroupSizeSupplier, objSize, objGroupSize);

            objTheme = addSingleGroupObject(themeSupplier, "Тема", name);
            addObjectActions(this, objTheme);

            objSeason = addSingleGroupObject(seasonSupplier, "Сезон", name, sidSeasonSupplier, nameSeasonYearSeasonSupplier);
            addObjectActions(this, objSeason);

            objCollection = addSingleGroupObject(collectionSupplier, "Коллекция", name);
            addObjectActions(this, objCollection);

            objCategory = addSingleGroupObject(categorySupplier, "Группа", name);
            addObjectActions(this, objCategory);

            objSubCategory = addSingleGroupObject(subCategorySupplier, "Подгруппа", name, nameCategorySupplierSubCategorySupplier);
            addObjectActions(this, objSubCategory);

            objCountry = addSingleGroupObject(countrySupplier, "Страна", name, nameCountryCountrySupplier);
            addObjectActions(this, objCountry);

            objGenderSupplier = addSingleGroupObject(genderSupplier, "Пол", name, sidGenderSupplier, sidGenderGenderSupplier);
            addObjectActions(this, objGenderSupplier);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColor), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSize), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierBrandSupplier, objBrand), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierThemeSupplier, objTheme), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSeasonSupplier, objSeason), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierCollectionSupplier, objCollection), Compare.EQUALS, objSupplier));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierCategorySupplier, objCategory), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSubCategorySupplier, objSubCategory), Compare.EQUALS, objSupplier));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierCountrySupplier, objCountry), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeGroup, objGroupSize), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierGenderSupplier, objGenderSupplier), Compare.EQUALS, objSupplier));

            RegularFilterGroupEntity filterCategory = new RegularFilterGroupEntity(genID());
            filterCategory.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(categorySupplierSubCategorySupplier, objSubCategory), Compare.EQUALS, objCategory),
                    "В группе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)), true);
            addRegularFilterGroup(filterCategory);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(groupSizeSupplier, objSize), Compare.EQUALS, objGroupSize),
                    "В размерной сетке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)), false);
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.addIntersection(design.getGroupObjectContainer(objSupplier.groupTo),
                                   design.getGroupObjectContainer(objBrand.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
//
//            design.addIntersection(design.getGroupObjectContainer(objColor.groupTo),
//                    design.getGroupObjectContainer(objSize.groupTo),
//                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
//
//            design.addIntersection(design.getGroupObjectContainer(objSize.groupTo),
//                                   design.getGroupObjectContainer(objTheme.groupTo),
//                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
//
//            design.addIntersection(design.getGroupObjectContainer(objTheme.groupTo),
//                    design.getGroupObjectContainer(objCollection.groupTo),
//                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
//
            design.addIntersection(design.getGroupObjectContainer(objGroupSize.groupTo),
                    design.getGroupObjectContainer(objSize.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objCategory.groupTo),
                    design.getGroupObjectContainer(objSubCategory.groupTo),
                    DoNotIntersectSimplexConstraint.TOTHE_RIGHT);


            ContainerView groupContainer = design.createContainer("Группы");
            groupContainer.add(design.getGroupObjectContainer(objCategory.groupTo));
            groupContainer.add(design.getGroupObjectContainer(objSubCategory.groupTo));

            ContainerView sizeContainer = design.createContainer("Размеры");
            sizeContainer.add(design.getGroupObjectContainer(objGroupSize.groupTo));
            sizeContainer.add(design.getGroupObjectContainer(objSize.groupTo));

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objColor.groupTo));
            specContainer.add(design.getGroupObjectContainer(objColor.groupTo));
            specContainer.add(sizeContainer);
            specContainer.add(design.getGroupObjectContainer(objCollection.groupTo));
            specContainer.add(groupContainer);
            specContainer.add(design.getGroupObjectContainer(objTheme.groupTo));
            specContainer.add(design.getGroupObjectContainer(objSeason.groupTo));
            specContainer.add(design.getGroupObjectContainer(objCountry.groupTo));
            specContainer.add(design.getGroupObjectContainer(objGenderSupplier.groupTo));
            specContainer.setType(ContainerType.TABBED_PANE);

            design.get(objSupplier.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objSupplier.groupTo).grid.constraints.fillVertical = 1;
            design.get(objBrand.groupTo).grid.constraints.fillVertical = 1;

            design.get(objGroupSize.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objSize.groupTo).grid.constraints.fillHorizontal = 3;

            design.get(objCategory.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objSubCategory.groupTo).grid.constraints.fillHorizontal = 3;

            design.get(objColor.groupTo).grid.constraints.fillVertical = 2;
            design.get(objTheme.groupTo).grid.constraints.fillVertical = 2;
            design.get(objCollection.groupTo).grid.constraints.fillVertical = 2;
            sizeContainer.constraints.fillVertical = 2;
            groupContainer.constraints.fillVertical = 2;
            design.get(objSeason.groupTo).grid.constraints.fillVertical = 2;
            design.get(objCountry.groupTo).grid.constraints.fillVertical = 2;
            design.get(objGenderSupplier.groupTo).grid.constraints.fillVertical = 2;

            return design;
        }
    }

    private class StockTransferFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objTransfer;
        private ObjectEntity objSku;

        private StockTransferFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objTransfer = addSingleGroupObject(transfer, "Внутреннее перемещение", baseLM.objectValue, barcodeStockFromTransfer, barcodeStockToTransfer);
            addObjectActions(this, objTransfer);

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem,
                    nameCategoryArticleSku, sidCustomCategoryOriginArticleSku,
                    nameCountryOfOriginSku, netWeightSku, mainCompositionOriginSku,
                    additionalCompositionOriginSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            addPropertyDraw(balanceStockFromTransferSku, objTransfer, objSku);
            addPropertyDraw(quantityTransferSku, objTransfer, objSku);
            addPropertyDraw(balanceStockToTransferSku, objTransfer, objSku);

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(balanceStockFromTransferSku, objTransfer, objSku)),
                    "Есть на остатке",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityTransferSku, objTransfer, objSku)),
                    "В документе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            filterGroup.defaultFilterIndex = 0;
            addRegularFilterGroup(filterGroup);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(objTransfer.groupTo).grid.constraints.fillVertical = 0.4;

            return design;
        }
    }

    private class FreightInvoiceFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objBrandSupplier;
        private ObjectEntity objSku;
        private ObjectEntity objArticle;

        private FreightInvoiceFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject("freight", freightChanged, "Фрахт", date, baseLM.objectClassName, nameRouteFreight, nameFreightTypeFreight, nameCurrencyFreight, sumFreightFreight, sumInFreight, sumMarkupInFreight, sumInOutFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objFreight, PropertyEditType.READONLY);
            setEditType(nameCurrencyFreight, PropertyEditType.EDITABLE);
            setEditType(sumFreightFreight, PropertyEditType.EDITABLE);

            addPropertyDraw(sumInCurrentYear);
            addPropertyDraw(sumInOutCurrentYear);
            addPropertyDraw(balanceSumCurrentYear);
            setEditType(sumInCurrentYear, PropertyEditType.READONLY);
            setEditType(sumInOutCurrentYear, PropertyEditType.READONLY);
            setEditType(balanceSumCurrentYear, PropertyEditType.READONLY);

            objImporter = addSingleGroupObject(importer, "Импортер", name);
            //setEditType(PropertyEditType.READONLY, objImporter.groupTo);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumMarkupInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInOutImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumDutyImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumNDSImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumRegistrationImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumCustomImporterFreight, objImporter, objFreight);

            setEditType(PropertyEditType.READONLY, objImporter.groupTo);

            objBrandSupplier = addSingleGroupObject(brandSupplier, "Бренд", name, nameSupplierBrandSupplier);
            setEditType(PropertyEditType.READONLY, objBrandSupplier.groupTo);

            addPropertyDraw(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(markupPercentImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(averagePercentImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier);
            addPropertyDraw(insuranceFreightBrandSupplier, objFreight, objBrandSupplier);
            setEditType(quantityImporterFreightBrandSupplier, PropertyEditType.READONLY);
            setEditType(averagePercentImporterFreightBrandSupplier, PropertyEditType.READONLY);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameBrandSupplierArticle, nameCategoryArticle);

            addPropertyDraw(objImporter, objFreight, objArticle, quantityImporterFreightArticle, nameCurrencyRRPImporterFreightArticle, markupPercentImporterFreightArticle,
                                priceFullKgImporterFreightArticle, minPriceRateImporterFreightArticle);

            LCP highlightColor = addCProp(ColorClass.instance, new Color(255, 128, 128));
            CalcPropertyObjectEntity greaterPriceMinPriceImporterFreightArticleProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, greaterPriceMinPriceImporterFreightArticle, 1, 2, 3), objImporter, objFreight, objArticle);
            getPropertyDraw(minPriceRateImporterFreightArticle).setPropertyBackground(greaterPriceMinPriceImporterFreightArticleProperty);
            getPropertyDraw(priceFullKgImporterFreightArticle).setPropertyBackground(greaterPriceMinPriceImporterFreightArticleProperty);

            setEditType(PropertyEditType.READONLY, objArticle.groupTo);
            setEditType(markupPercentImporterFreightArticle, PropertyEditType.EDITABLE);

            objSku = addSingleGroupObject("sku", sku, "SKU", barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku);

            setForceViewType(itemAttributeGroup, ClassViewType.GRID, objSku.groupTo);

            setEditType(baseGroup, PropertyEditType.READONLY, objSku.groupTo);
            setEditType(publicGroup, PropertyEditType.READONLY, objSku.groupTo);

            addPropertyDraw(nameCountryOfOriginFreightSku, objFreight, objSku);
            addPropertyDraw(netWeightFreightSku, objFreight, objSku);
            addPropertyDraw(sidCustomCategory10FreightSku, objFreight, objSku);
            //addPropertyDraw(nameSubCategoryFreightSku, objFreight, objSku);
            //addPropertyDraw(nameTypeInvoiceFreightArticleSku, objFreight, objSku);
            addPropertyDraw(quantityImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(RRPInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(nameCurrencyRRPImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(markupPercentImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInOutImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInvoiceImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(markupInOutImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceFreightImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceInsuranceImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceFullImporterFreightSku, objImporter, objFreight, objSku);
            //addPropertyDraw(minPriceRateWeightImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(priceFullKgImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(minPriceRateFreightSku, objFreight, objSku);
            addPropertyDraw(dutyImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(NDSPercentFreightSku, objFreight, objSku);
            addPropertyDraw(NDSImporterFreightSku, objImporter, objFreight, objSku);
            addPropertyDraw(sumRegistrationImporterFreightSku, objImporter, objFreight, objSku);

            setEditType(PropertyEditType.READONLY, objSku.groupTo);
            setEditType(priceInImporterFreightSku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(RRPInImporterFreightSku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(nameCurrencyRRPImporterFreightSku, PropertyEditType.EDITABLE, objSku.groupTo);
            setEditType(markupPercentImporterFreightSku, PropertyEditType.EDITABLE, objSku.groupTo);

            CalcPropertyObjectEntity greaterPriceMinPriceImporterFreightSkuProperty = addPropertyObject(addJProp(baseLM.and1, highlightColor, greaterPriceMinPriceImporterFreightSku, 1, 2, 3), objImporter, objFreight, objSku);
            getPropertyDraw(minPriceRateFreightSku).setPropertyBackground(greaterPriceMinPriceImporterFreightSkuProperty);
            getPropertyDraw(priceFullKgImporterFreightSku).setPropertyBackground(greaterPriceMinPriceImporterFreightSkuProperty);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightBrandSupplier, objImporter, objFreight, objBrandSupplier)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightArticle, objImporter, objFreight, objArticle)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));

            RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
            filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(priceInImporterFreightSku, objImporter, objFreight, objSku))),
                    "Без цены",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
            addRegularFilterGroup(filterGroup);

            RegularFilterGroupEntity filterPriceGroup = new RegularFilterGroupEntity(genID());
            filterPriceGroup.addFilter(new RegularFilterEntity(genID(),
                    new CompareFilterEntity(addPropertyObject(priceInImporterFreightSku, objImporter, objFreight, objSku), Compare.EQUALS, addPropertyObject(baseLM.vzero)),
                    "С нулевой ценой",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
            addRegularFilterGroup(filterPriceGroup);

            addHintsIncrementTable(priceFullKgImporterFreightArticle, minPriceRateImporterFreightArticle,
                    sumInOutImporterFreightArticle, sumInOutImporterFreightBrandSupplier, sumPercentImporterFreightBrandSupplier, sumInOutFreightArticle,
                    markupPercentImporterFreightSku, priceInOutImporterFreightSku, priceInvoiceImporterFreightSku, priceInsuranceImporterFreightSku, priceFullImporterFreightSku,
                    sumInOutFreightBrandSupplier, sumInOutImporterFreightSku, dutyImporterFreightSku, priceMarkupInImporterFreightSku, sumMarkupInImporterFreight,
                    sumInImporterFreight, sumInOutImporterFreight,
                    sumInFreight, sumMarkupInFreight, sumInOutFreight);
//            addHintsNoUpdate(objImporter.groupTo);
//            addHintsNoUpdate(dutyImporterFreightSku);
//            addHintsNoUpdate(NDSImporterFreightSku);
            //addHintsNoUpdate(sumInImporterFreight);

            setPageSize(0);

            freightPricedFA = addDMFAProp(actionGroup, "Расценить", this, new ObjectEntity[] {objFreight}, true,
                    addPropertyObject(addJoinAProp(executeChangeFreightClass, 1, addCProp(baseClass.objectClass, "freightPriced")), objFreight));
            freightPricedFA.setImage("arrow_right.png");
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objBrandSupplier.groupTo).grid.constraints.fillHorizontal = 1;

            ContainerView specContainer = design.createContainer("Итоги по текущему году");
            specContainer.add(design.get(getPropertyDraw(sumInCurrentYear)));
            specContainer.add(design.get(getPropertyDraw(sumInOutCurrentYear)));
            specContainer.add(design.get(getPropertyDraw(balanceSumCurrentYear)));
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objFreight.groupTo));

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo), specContainer, DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objImporter.groupTo), design.getGroupObjectContainer(objBrandSupplier.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView priceContainer = design.createContainer();
            design.getMainContainer().addAfter(priceContainer, design.getGroupObjectContainer(objSku.groupTo));
            priceContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            priceContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            priceContainer.setType(ContainerType.TABBED_PANE);

            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 3;

            return design;
        }
    }

//    private class FreightContentFormEntity extends FormEntity<RomanBusinessLogics> {
//
//        private ObjectEntity objFreight;
//        private ObjectEntity objImporter;
//        private ObjectEntity objBrand;
//        private ObjectEntity objArticle;
//        private ObjectEntity objFreightBox;
//
//        private PropertyDrawEntity nameBrand;
//
//        private FreightContentFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
//            super(parent, sID, caption);
//
//            objFreight = addSingleGroupObject(freight, "Фрахт", date, baseLM.objectClassName, nameRouteFreight, nameExporterFreight, nameFreightTypeFreight, nameCurrencyFreight, sumFreightFreight);
//            objFreight.groupTo.setSingleClassView(ClassViewType.GRID);
//
//            objImporter = addSingleGroupObject(importer, "Импортер", name);
//
//            objBrand = addSingleGroupObject(brandSupplier, "Бренд");
//
//            nameBrand = addPropertyDraw(name, objBrand);
//
//            addPropertyDraw(objFreight, objBrand, stockNumberFreightBrandSupplier, quantityShipmentedFreightBrandSupplier, quantityFreightBrandSupplier, sumInFreightBrandSupplier, sumInOutFreightBrandSupplier);
//            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightBrandSupplier, objFreight, objBrand), Compare.GREATER, addPropertyObject(baseLM.vzero)));
//
//            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameCategoryArticle);
//
//            addPropertyDraw(objFreight, objArticle, quantityFreightArticle, sumInFreightArticle, sumInOutFreightArticle);
//            //addPropertyDraw(objImporter, objFreight, objArticle, quantityImporterFreightArticle, sumInImporterFreightArticle, sumInOutImporterFreightArticle);
//
//            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticle, objArticle), Compare.EQUALS, objBrand));
//            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightArticle, objFreight, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));
//
//            objFreightBox = addSingleGroupObject(freightBox, "Короб", barcode);
//
//            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
//            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityStockArticle, objFreightBox, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));
//
//        }
//
//        @Override
//        public FormView createDefaultRichDesign() {
//            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
//
//            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
//            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";
//
//            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo), design.getGroupObjectContainer(objImporter.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
//
//            design.get(nameBrand).setPreferredCharWidth(15);
//            design.get(getPropertyDraw(nameCategoryArticle, objArticle)).setPreferredCharWidth(15);
//            design.get(getPropertyDraw(sidArticle, objArticle)).setPreferredCharWidth(15);
//
//            design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 2;
//            design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 1;
//
//            design.addIntersection(design.getGroupObjectContainer(objArticle.groupTo), design.getGroupObjectContainer(objFreightBox.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
//            design.get(objArticle.groupTo).grid.constraints.fillHorizontal = 3;
//            design.get(objFreightBox.groupTo).grid.constraints.fillHorizontal = 1;
//
//            design.get(objFreight.groupTo).grid.constraints.fillVertical = 1;
//            design.get(objBrand.groupTo).grid.constraints.fillVertical = 1;
//            design.get(objArticle.groupTo).grid.constraints.fillVertical = 2;
//
//            return design;
//        }
//    }

    private class FreightReportFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objTransitDocument;
        private ObjectEntity objBrand;
        private ObjectEntity objDestination;
        private ObjectEntity objFreightBox;

        private GroupObjectEntity gobjFreightImporterTypeInvoice;

        private PropertyDrawEntity nameBrand;
        private PropertyDrawEntity nameDestination;

        private FreightReportFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            gobjFreightImporterTypeInvoice = new GroupObjectEntity(genID(), "Группировка");
            objFreight = new ObjectEntity(genID(), freight, "Фрахт");
            objImporter = new ObjectEntity(genID(), importer, "Импортёр");
            objTypeInvoice = new ObjectEntity(genID(), typeInvoice, "Тип инвойса");

            gobjFreightImporterTypeInvoice.add(objFreight);
            gobjFreightImporterTypeInvoice.add(objImporter);
            gobjFreightImporterTypeInvoice.add(objTypeInvoice);
            addGroupObject(gobjFreightImporterTypeInvoice);

            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice);

            addPropertyDraw(objFreight, date, dateArrivalFreight, nameRouteFreight);
            addPropertyDraw(objImporter, name);
            addPropertyDraw(objFreight, nameCurrencyFreight, sumFreightFreight, freightBoxNumberFreight, quantityFreight, sumInFreight, sumInOutFreight);
            setForceViewType(sumInOutFreight, ClassViewType.GRID);

            addPropertyDraw(objImporter, objFreight, sumCustomImporterFreight);

            setEditType(PropertyEditType.READONLY, gobjFreightImporterTypeInvoice);

            objDestination = addSingleGroupObject(destination, "Магазин");
            nameDestination = addPropertyDraw(name, objDestination);

            objBrand = addSingleGroupObject(brandSupplier, "Бренд");
            nameBrand = addPropertyDraw(name, objBrand);

            objTransitDocument = addSingleGroupObject(transitDocument, "Транзитный документ", sidTransitDocument, nameSellerTransitDocument, nameTypeTransitTransitDocument, dateRepaymentTransitDocument, dateClosingTransitDocument);

            objFreightBox = addSingleGroupObject(freightBox, "Короб", barcode, quantityStock);

            addPropertyDraw(objFreight, objBrand, stockNumberFreightBrandSupplier, quantityFreightBrandSupplier, sumInFreightBrandSupplier, sumInOutFreightBrandSupplier);
            addPropertyDraw(objImporter, objFreight, objBrand, averagePercentImporterFreightBrandSupplier);

            addPropertyDraw(objFreight, objDestination, quantityFreightDestination);

            setEditType(PropertyEditType.READONLY, objDestination.groupTo);
            setEditType(PropertyEditType.READONLY, objBrand.groupTo);
            setEditType(PropertyEditType.READONLY, objTransitDocument.groupTo);
            setEditType(PropertyEditType.READONLY, objFreightBox.groupTo);

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(sidImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityFreightDestination, objFreight, objDestination)));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(importerTransitDocument, objTransitDocument), Compare.EQUALS, objImporter));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightTransitDocument, objTransitDocument), Compare.EQUALS, objFreight));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight), Compare.GREATER, addPropertyObject(baseLM.vzero)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightBrandSupplier, objFreight, objBrand), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(destinationFreightBox, objFreightBox), Compare.EQUALS, objDestination));

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(name, objImporter)).caption = "Импортёр";
            design.get(getPropertyDraw(sumInFreight, objFreight)).caption = "Итого в ценах поставщика";
            design.get(getPropertyDraw(sumInOutFreight, objFreight)).caption = "Итого в отгрузочных ценах";

            design.get(getPropertyDraw(sidImporterFreightTypeInvoice)).setPreferredCharWidth(15);
            design.get(getPropertyDraw(dateArrivalFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(freightBoxNumberFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(sumInFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(sumInOutFreight, objFreight)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(name, objImporter)).setPreferredCharWidth(20);
            design.get(getPropertyDraw(name, objBrand)).setPreferredCharWidth(20);

            //design.addIntersection(design.getGroupObjectContainer(objTransitDocument.groupTo), design.getGroupObjectContainer(objBrand.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objTransitDocument.groupTo), design.getGroupObjectContainer(objFreightBox.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objBrand.groupTo), design.getGroupObjectContainer(objFreightBox.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objDestination.groupTo), design.getGroupObjectContainer(objFreightBox.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(gobjFreightImporterTypeInvoice).grid.constraints.fillVertical = 1;
            design.get(objTransitDocument.groupTo).grid.constraints.fillVertical = 1;
            design.get(objBrand.groupTo).grid.constraints.fillVertical = 1;

            design.get(objTransitDocument.groupTo).grid.constraints.fillHorizontal= 2;
            design.get(objBrand.groupTo).grid.constraints.fillHorizontal = 3;

            return design;
        }
    }

    private class FreightBoxContentFormEntity extends BarcodeFormEntity {

        private ObjectEntity objFreightBox;
        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objArticle;

        private GroupObjectEntity gobjFreightImporterTypeInvoice;
        private PropertyDrawEntity importerName;
        private PropertyDrawEntity typeInvoiceName;

        private FreightBoxContentFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreightBox = addSingleGroupObject(freightBox, "Короб", barcode, netWeightStock, nameDestinationFreightBox);
            objFreightBox.groupTo.setSingleClassView(ClassViewType.GRID);
            setEditType(PropertyEditType.READONLY, objFreightBox.groupTo);

            addActionsOnObjectChange(objBarcode, addPropertyObject(seekBarcodeAction, objBarcode));

            gobjFreightImporterTypeInvoice = new GroupObjectEntity(genID(), "Группировка");
            objFreight = new ObjectEntity(genID(), freight, "Фрахт");
            objImporter = new ObjectEntity(genID(), importer, "Импортёр");
            objTypeInvoice = new ObjectEntity(genID(), typeInvoice, "Тип инвойса");

            gobjFreightImporterTypeInvoice.add(objFreight);
            gobjFreightImporterTypeInvoice.add(objImporter);
            gobjFreightImporterTypeInvoice.add(objTypeInvoice);
            addGroupObject(gobjFreightImporterTypeInvoice);

            addPropertyDraw(objFreight, date, dateArrivalFreight, nameExporterFreight);
            importerName = addPropertyDraw(name, objImporter);
            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight);

            typeInvoiceName = addPropertyDraw(name, objTypeInvoice);
            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityImporterStock, objImporter, objFreightBox), Compare.GREATER, addPropertyObject(baseLM.vzero)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityImporterStockTypeInvoice, objImporter, objFreightBox, objTypeInvoice), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(freightFreightBox, objFreightBox)));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightBox, objFreightBox), Compare.EQUALS, objFreight));

            setEditType(PropertyEditType.READONLY, gobjFreightImporterTypeInvoice);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameCategoryArticle);

            addPropertyDraw(objFreightBox, objArticle, quantityStockArticle);
            setEditType(PropertyEditType.READONLY, objArticle.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityStockArticle, objFreightBox, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

        }

        @Override
        public DefaultFormView createDefaultRichDesign() {
            DefaultFormView design = super.createDefaultRichDesign();

            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
            design.get(importerName).caption = "Импортер";
            design.get(typeInvoiceName).caption = "Тип";

            //design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            design.addIntersection(design.getGroupObjectContainer(objFreight.groupTo), design.getGroupObjectContainer(objImporter.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            design.addIntersection(design.getGroupObjectContainer(objImporter.groupTo), design.getGroupObjectContainer(objTypeInvoice.groupTo), DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            //design.get(objFreight.groupTo).grid.constraints.fillHorizontal = 2;
            //design.get(objImporter.groupTo).grid.constraints.fillHorizontal = 1;
            //design.get(objTypeInvoice.groupTo).grid.constraints.fillHorizontal = 2;            /
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 2;

            return design;
        }
    }

    private class FreightListFormEntity extends FormEntity<RomanBusinessLogics> {

        private ObjectEntity objFreight;
        private ObjectEntity objImporter;
        private ObjectEntity objTypeInvoice;
        private ObjectEntity objSeller;
        private ObjectEntity objTransitDocument;
        private ObjectEntity objSku;
        private ObjectEntity objBrand;
        private ObjectEntity objArticle;
        private ObjectEntity objFreightUnit;

        private FreightListFormEntity(NavigatorElement<RomanBusinessLogics> parent, String sID, String caption) {
            super(parent, sID, caption);

            objFreight = addSingleGroupObject("freight", freight, "Фрахт", date, baseLM.objectClassName,
                                              nameRouteFreight, netWeightInvoicedFreight, grossWeightFreight,
                                              palletNumberFreight, freightBoxNumberFreight, formLogFreight);
            objFreight.groupTo.setSingleClassView(ClassViewType.GRID);
            setEditType(objFreight, PropertyEditType.READONLY);
            setEditType(formLogFreight, PropertyEditType.EDITABLE);
            addPropertyDraw(freightCompleteFA, objFreight).forceViewType = ClassViewType.GRID;
            addPropertyDraw(freightChangedFA, objFreight).forceViewType = ClassViewType.GRID;

            addPropertyDraw(executeChangeFreightChangedClass, objFreight).forceViewType = ClassViewType.GRID;
            addPropertyDraw(freightPricedFA, objFreight).forceViewType = ClassViewType.GRID;

//            GroupObjectEntity gobjDates = new GroupObjectEntity(genID());
//            objDateFrom = new ObjectEntity(genID(), DateClass.instance, "Дата (с)");
//            objDateTo = new ObjectEntity(genID(), DateClass.instance, "Дата (по)");
//            gobjDates.add(objDateFrom);
//            gobjDates.add(objDateTo);
//
//            addGroupObject(gobjDates);
//            gobjDates.setSingleClassView(ClassViewType.PANEL);

            objImporter = addSingleGroupObject(importer, "Импортер", name, sidImporter);

            objTypeInvoice = addSingleGroupObject(typeInvoice, "Тип инвойса", name);

            setEditType(PropertyEditType.READONLY, objImporter.groupTo);
            setEditType(PropertyEditType.READONLY, objTypeInvoice.groupTo);

            addPropertyDraw(objImporter, objFreight, sidContractImporterFreight, dateContractImporterFreight, sidImporterFreight, dateImporterFreight); //, conditionShipmentContractImporterFreight, conditionPaymentContractImporterFreight);
            setEditType(sidContractImporterFreight, PropertyEditType.READONLY);

            addPropertyDraw(quantityImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumMarkupInImporterFreight, objImporter, objFreight);
            addPropertyDraw(sumInOutImporterFreight, objImporter, objFreight);

            setEditType(PropertyEditType.READONLY, objImporter.groupTo);
            setEditType(sidContractImporterFreight, PropertyEditType.EDITABLE);

            addPropertyDraw(objImporter, objFreight, objTypeInvoice, sidImporterFreightTypeInvoice, dateImporterFreightTypeInvoice, dateShipmentImporterFreightTypeInvoice);

            objSeller = addSingleGroupObject(seller, "Поставщик", name);
            setEditType(PropertyEditType.READONLY, objSeller.groupTo);

            addPropertyDraw(invoiceOriginFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(invoiceFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(sbivkaFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(listFreightUnitFormImporterFreight, objImporter, objFreight);
            addPropertyDraw(packingListOriginFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(packingListFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(sbivkaFormImporterFreightSupplier, objImporter, objFreight, objSeller);
            addPropertyDraw(invoiceExportFormImporterFreight, objImporter, objFreight, objTypeInvoice);
            addPropertyDraw(invoiceExportDbf, objImporter, objFreight, objTypeInvoice);

            objTransitDocument = addSingleGroupObject(transitDocument, "Транзитный документ", sidTransitDocument, nameTypeTransitTransitDocument, dateRepaymentTransitDocument, dateClosingTransitDocument);
            addObjectActions(this, objTransitDocument);

            objSku = addSingleGroupObject(sku, "SKU", barcode, sidArticleSku, nameBrandSupplierArticleSku, nameCategoryArticleSku, sidGenderArticleSku, nameUnitOfMeasureArticleSku,
                      sidSizeSupplierItem, nameCommonSizeSku, sidColorSupplierItem, nameColorSupplierItem);

            setForceViewType(baseGroup, ClassViewType.GRID, objSku.groupTo);

            //setEditType(baseGroup, PropertyEditType.READONLY, objSku.groupTo);
            //setEditType(publicGroup, PropertyEditType.READONLY, objSku.groupTo);

            addPropertyDraw(objFreight, objSku, sidCustomCategory10FreightSku, nameCountryOfOriginFreightSku, mainCompositionFreightSku, additionalCompositionFreightSku);
            addPropertyDraw(objImporter, objFreight, objSku, netWeightImporterFreightSku, grossWeightImporterFreightSku);

            addPropertyDraw(objImporter, objFreight, objSku, quantityImporterFreightSku, priceInOutImporterFreightSku, sumInOutImporterFreightSku);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightTransitDocument, objTransitDocument), Compare.EQUALS, objFreight));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(sellerTransitDocument, objTransitDocument), Compare.EQUALS, objSeller));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(importerTransitDocument, objTransitDocument), Compare.EQUALS, objImporter));

            addFixedFilter(new OrFilterEntity(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSupplier, objImporter, objFreight, objSeller)),
                                              new CompareFilterEntity(addPropertyObject(exporterFreight, objFreight), Compare.EQUALS, objSeller)));

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreight, objImporter, objFreight)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightTypeInvoice, objImporter, objFreight, objTypeInvoice)));
            addFixedFilter(new NotNullFilterEntity(addPropertyObject(quantityImporterFreightSku, objImporter, objFreight, objSku)));

            setEditType(publicGroup, PropertyEditType.READONLY, objSku.groupTo);
            // содержимое фрахта
            objBrand = addSingleGroupObject(brandSupplier, "Бренд", name);

            addPropertyDraw(objFreight, objBrand, stockNumberFreightBrandSupplier, quantityShipmentedFreightBrandSupplier, quantityFreightBrandSupplier, sumInFreightBrandSupplier, sumInOutFreightBrandSupplier);
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightBrandSupplier, objFreight, objBrand), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            setEditType(PropertyEditType.READONLY, objBrand.groupTo);

            objArticle = addSingleGroupObject(article, "Артикул", sidArticle, nameCategoryArticle);

            addPropertyDraw(objFreight, objArticle, quantityFreightArticle, sumInFreightArticle, sumInOutFreightArticle);
            setEditType(PropertyEditType.READONLY, objArticle.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(brandSupplierArticle, objArticle), Compare.EQUALS, objBrand));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightArticle, objFreight, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            objFreightUnit = addSingleGroupObject(freightUnit, "Короб", barcode);
            setEditType(PropertyEditType.READONLY, objFreightUnit.groupTo);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(freightFreightUnit, objFreightUnit), Compare.EQUALS, objFreight));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(quantityFreightUnitArticle, objFreightUnit, objArticle), Compare.GREATER, addPropertyObject(baseLM.vzero)));

            //addPropertyDraw(objDateFrom, baseLM.objectValue);
            //addPropertyDraw(objDateTo, baseLM.objectValue);
            //addPropertyDraw(quantityPalletShipmentBetweenDate, objDateFrom, objDateTo);
            //addPropertyDraw(quantityPalletFreightBetweenDate, objDateFrom, objDateTo);

            //objShipment = addSingleGroupObject(shipment, "Поставка", date, nameSupplierDocument, sidDocument, sumDocument, nameCurrencyDocument, netWeightShipment, grossWeightShipment, quantityPalletShipment, quantityBoxShipment);
            //setEditType(objShipment, PropertyEditType.READONLY);
            //addPropertyDraw(quantityShipmentFreight, objShipment, objFreight);

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            design.get(getPropertyDraw(date, objFreight)).caption = "Дата отгрузки";
            design.get(getPropertyDraw(baseLM.objectClassName, objFreight)).caption = "Статус фрахта";

            ContainerView printContainer = design.createContainer("Печать документов");
            printContainer.constraints.childConstraints = DoNotIntersectSimplexConstraint.TOTHE_BOTTOM;
            printContainer.add(design.getGroupObjectContainer(objImporter.groupTo));
            printContainer.add(design.getGroupObjectContainer(objTypeInvoice.groupTo));
            printContainer.add(design.getGroupObjectContainer(objSeller.groupTo));
            printContainer.add(design.getGroupObjectContainer(objTransitDocument.groupTo));
            printContainer.add(design.getGroupObjectContainer(objSku.groupTo));
            printContainer.preferredSize = new Dimension(-1, 200);

            design.addIntersection(design.getGroupObjectContainer(objTypeInvoice.groupTo),
                                   design.getGroupObjectContainer(objSeller.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.addIntersection(design.getGroupObjectContainer(objSeller.groupTo),
                                   design.getGroupObjectContainer(objTransitDocument.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            design.get(objFreight.groupTo).grid.constraints.fillVertical = 3;

            design.get(objImporter.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objTypeInvoice.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objSeller.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objTransitDocument.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 2;

            design.get(objTypeInvoice.groupTo).grid.constraints.fillHorizontal = 3;
            design.get(objSeller.groupTo).grid.constraints.fillHorizontal = 1;
            design.get(objTransitDocument.groupTo).grid.constraints.fillHorizontal = 2;

            design.addIntersection(design.getGroupObjectContainer(objArticle.groupTo),
                                   design.getGroupObjectContainer(objFreightUnit.groupTo),
                                   DoNotIntersectSimplexConstraint.TOTHE_RIGHT);

            ContainerView containContainer = design.createContainer("Содержимое фрахта");
            containContainer.add(design.getGroupObjectContainer(objBrand.groupTo));
            containContainer.add(design.getGroupObjectContainer(objArticle.groupTo));
            containContainer.add(design.getGroupObjectContainer(objFreightUnit.groupTo));
            containContainer.preferredSize = new Dimension(-1, 200);

            ContainerView specContainer = design.createContainer();
            design.getMainContainer().addAfter(specContainer, design.getGroupObjectContainer(objFreight.groupTo));
            specContainer.add(printContainer);
            specContainer.add(containContainer);
            specContainer.setType(ContainerType.TABBED_PANE);


            design.get(objBrand.groupTo).grid.constraints.fillVertical = 1.5;
            design.get(objArticle.groupTo).grid.constraints.fillVertical = 3.5;
            design.get(objFreightUnit.groupTo).grid.constraints.fillVertical = 3.5;

            design.get(objArticle.groupTo).grid.constraints.fillHorizontal = 2;
            design.get(objFreightUnit.groupTo).grid.constraints.fillHorizontal = 1;

            return design;
        }
    }

    private class CreateItemFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objSupplier;
        ObjectEntity objBarcode;
        ObjectEntity objArticleComposite;
        ObjectEntity objSIDArticleComposite;
        ObjectEntity objColorSupplier;
        ObjectEntity objSizeSupplier;
        //ObjectEntity objSeasonSupplier;
        ObjectEntity objThemeSupplier;
        ObjectEntity objGenderSupplier;

        public CreateItemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSupplier = addSingleGroupObject(supplier, "Поставщик", name);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue);
            objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);

            objArticleComposite = addSingleGroupObject(articleComposite, "Артикул", sidArticle);
            objArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objArticleComposite, PropertyEditType.SELECTOR);

//            objSIDArticleComposite = addSingleGroupObject(StringClass.get(50), "Артикул (новый)", baseLM.objectValue);
//            objSIDArticleComposite.groupTo.setSingleClassView(ClassViewType.PANEL);

            objColorSupplier = addSingleGroupObject(colorSupplier, "Цвет поставщика", sidColorSupplier, name);
            objColorSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objColorSupplier, PropertyEditType.SELECTOR);

            objSizeSupplier = addSingleGroupObject(sizeSupplier, "Размер поставщика", sidSizeSupplier);
            objSizeSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objSizeSupplier, PropertyEditType.SELECTOR);

            //objSeasonSupplier = addSingleGroupObject(seasonSupplier, "Сезон поставщика", sidSeasonSupplier, name);
            //objSeasonSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            //setEditType(objSeasonSupplier, PropertyEditType.SELECTOR);

            objThemeSupplier = addSingleGroupObject(themeSupplier, "Тема поставщика", sidThemeSupplier, name);
            objThemeSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objThemeSupplier, PropertyEditType.SELECTOR);

            objGenderSupplier = addSingleGroupObject(genderSupplier, "Пол поставщика", sidGenderSupplier, name);
            objGenderSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objGenderSupplier, PropertyEditType.SELECTOR);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierArticle, objArticleComposite), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierColorSupplier, objColorSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSizeSupplier, objSizeSupplier), Compare.EQUALS, objSupplier));
            //addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierSeasonSupplier, objSeasonSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierThemeSupplier, objThemeSupplier), Compare.EQUALS, objSupplier));
            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierGenderSupplier, objGenderSupplier), Compare.EQUALS, objSupplier));

//            addActionsOnOk(addPropertyObject(addNEArticleCompositeSIDSupplier, objSIDArticleComposite, objSupplier));
//            addActionsOnOk(addPropertyObject(addItemSIDArticleSupplierColorSizeBarcode, objSIDArticleComposite, objSupplier, objColorSupplier, objSizeSupplier, objBarcode));

            addActionsOnEvent(FormEventType.OK, addPropertyObject(addItemArticleCompositeColorSizeBarcode, objArticleComposite, objColorSupplier, objSizeSupplier, objBarcode));
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setEnabled(objSupplier, false);
            design.setEnabled(objBarcode, false);
            return design;
        }
    }

    private class EditItemFormEntity extends FormEntity<RomanBusinessLogics> {

        ObjectEntity objSku;

        public EditItemFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);

            objSku = addSingleGroupObject(sku, "Товар", nameSupplierArticleSku, barcode, sidArticleSku, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            objSku.groupTo.setSingleClassView(ClassViewType.PANEL);
            setEditType(objSku, PropertyEditType.READONLY);
            setEditType(barcode, PropertyEditType.EDITABLE, objSku.groupTo);

            skuEditFA = addMFAProp(actionGroup, "Редактировать товар", this, new ObjectEntity[] {objSku}, true);
            skuEditFA.setImage("edit.png");

        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();
            design.setEnabled(nameSupplierArticleSku, false);

            return design;
        }
    }


    private class FindItemFormEntity extends FormEntity<RomanBusinessLogics> {

        private boolean box;
        private boolean isBarcode;

        ObjectEntity objBarcode;
        ObjectEntity objShipment;
        ObjectEntity objSupplierBox;
        ObjectEntity objRoute;
        ObjectEntity objSku;
        ObjectEntity objShipmentDetail;

        public FindItemFormEntity(NavigatorElement parent, String sID, String caption, boolean box, boolean isBarcode) {
            super(parent, sID, caption);

            this.box = box;
            this.isBarcode = isBarcode;

            if (isBarcode) {
                objBarcode = addSingleGroupObject(StringClass.get(13), "Штрих-код", baseLM.objectValue);
                objBarcode.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            if (box)
                objShipment = addSingleGroupObject(boxShipment, "Поставка", baseLM.objectValue, sidDocument, date);
            else
                objShipment = addSingleGroupObject(simpleShipment, "Поставка", baseLM.objectValue, sidDocument, date);

            objShipment.groupTo.setSingleClassView(ClassViewType.PANEL);

            if (box) {
                objSupplierBox = addSingleGroupObject(supplierBox, "Короб", sidSupplierBox, barcode);
                objSupplierBox.groupTo.setSingleClassView(ClassViewType.PANEL);
            }

            objRoute = addSingleGroupObject(route, "Маршрут", name);
            objRoute.groupTo.setSingleClassView(ClassViewType.PANEL);

            objSku = addSingleGroupObject(sku, "Товар", sidArticleSku, barcode, sidColorSupplierItem, nameColorSupplierItem, sidSizeSupplierItem);
            setForceViewType(baseGroup, ClassViewType.GRID, objSku.groupTo);

            addPropertyDraw(invoicedShipmentSku, objShipment, objSku);
            if (box)
                addPropertyDraw(quantityListSku, objSupplierBox, objSku);
            setEditType(objSku, PropertyEditType.READONLY);

            objShipmentDetail = addSingleGroupObject(box ? boxShipmentDetail : simpleShipmentDetail, "Строка поставки");

            addFixedFilter(new NotNullFilterEntity(addPropertyObject(invoicedShipmentSku, objShipment, objSku)));
            if (isBarcode) {
                RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotFilterEntity(new NotNullFilterEntity(addPropertyObject(barcode, objSku))),
                    "Без штрих-кода",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0)));
                filterGroup.defaultFilterIndex = 0;
                addRegularFilterGroup(filterGroup);
            }

            if (box) {
                RegularFilterGroupEntity filterGroup = new RegularFilterGroupEntity(genID());
                filterGroup.addFilter(new RegularFilterEntity(genID(),
                    new NotNullFilterEntity(addPropertyObject(quantityListSku, objSupplierBox, objSku)),
                    "В текущем коробе",
                    KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0)));
//                filterGroup.defaultFilterIndex = 0;
                addRegularFilterGroup(filterGroup);
            }

            if (isBarcode) {
                addActionsOnEvent(FormEventType.OK, addPropertyObject(addSetPropertyAProp(equalsObjectBarcode, 1, 2, baseLM.vtrue), objSku, objBarcode));
            } else {
                addActionsOnEvent(FormEventType.OK, addPropertyObject(seekRouteShipmentSkuRoute, objShipment, objSku, objRoute));
                if (box)
                    addActionsOnEvent(FormEventType.OK, addPropertyObject(addBoxShipmentDetailBoxShipmentSupplierBoxRouteSku, objShipment, objSupplierBox, objRoute, objSku));
                else
                    addActionsOnEvent(FormEventType.OK, addPropertyObject(addSimpleShipmentDetailSimpleShipmentRouteSku, objShipment, objRoute, objSku));
            }

            setEditType(PropertyEditType.READONLY);

            addDefaultOrder(sidArticleSku, true);
        }

        @Override
        public FormView createDefaultRichDesign() {
            DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

            if (box) {
                design.addIntersection(design.getGroupObjectContainer(objShipment.groupTo),
                                       design.getGroupObjectContainer(objSupplierBox.groupTo),
                                       DoNotIntersectSimplexConstraint.TOTHE_RIGHT);
            }

            design.get(objShipment.groupTo).grid.constraints.fillVertical = 1;
            design.get(objSku.groupTo).grid.constraints.fillVertical = 3;

            if (isBarcode)
                design.setEnabled(objBarcode, false);

            design.setEnabled(objShipment, false);
            if (box)
                design.setEnabled(objSupplierBox, false);
            design.setEnabled(objRoute, false);
            return design;
        }
    }

    public class PricatFormEntity extends FormEntity {
        ObjectEntity objSupplier;

        public PricatFormEntity(NavigatorElement parent, String sID, String caption) {
            super(parent, sID, caption);
            objSupplier = addSingleGroupObject(supplier, name, importPricatSupplier, gerryWeberImportPricat, hugoBossImportPricat);
            objSupplier.groupTo.setSingleClassView(ClassViewType.PANEL);

            ObjectEntity objPricat = addSingleGroupObject(pricat);
            addPropertyDraw(objPricat, baseGroup);
            addObjectActions(this, objPricat);

            addFixedFilter(new CompareFilterEntity(addPropertyObject(supplierPricat, objPricat), Compare.EQUALS, objSupplier));
            setEditType(objSupplier, PropertyEditType.SELECTOR);
            setEditType(importPricatSupplier, PropertyEditType.EDITABLE);
            setEditType(hugoBossImportPricat, PropertyEditType.EDITABLE);
            setEditType(gerryWeberImportPricat, PropertyEditType.EDITABLE);
        }
    }

    public class AddNewArticleActionProperty extends UserActionProperty {

        ObjectEntity objArticle;
        private final ClassPropertyInterface sidInterface;
        private final ClassPropertyInterface docInterface;

        boolean composite;

        public AddNewArticleActionProperty(ObjectEntity objArticle, boolean composite) {
            super(genSID(), StringClass.get(50), document);

            this.objArticle = objArticle;
            this.composite = composite;

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            sidInterface = i.next();
            docInterface = i.next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            context.emitExceptionIfNotInFormSession();

            DataObject sID = context.getDataKeyValue(sidInterface);
            DataObject document = context.getDataKeyValue(docInterface);

            ObjectValue supplier = supplierDocument.readClasses(context, document);
            if (supplier.isNull()) {
                context.delayUserInterfaction(new MessageClientAction("Не выбран поставщик", "Ввод товара"));
                return;
            }

            ObjectValue articlePrev = articleSIDSupplier.readClasses(context, sID, (DataObject)supplier);
            if (articlePrev.isNull()) {
                ObjectValue oldArticle = context.getObjectInstance(objArticle).getObjectValue();
                DataObject article = context.addObject(composite ? articleComposite : articleSingle);
                sidArticle.change(sID.getValue(), context, article);
                supplierArticle.change(supplier.getValue(), context, article);
                if (composite && !oldArticle.isNull())
                    sizeGroupSupplierArticle.change(sizeGroupSupplierArticle.read(context, (DataObject) oldArticle), context, article);
            }
        }
    }

    public class CreateStampActionProperty extends UserActionProperty {
        private ClassPropertyInterface createStampInterface;

        public CreateStampActionProperty() {
            super(genSID(), "Сгенерировать марки", new ValueClass[]{creationStamp});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            createStampInterface = i.next();
        }

        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject objCreateStamp = context.getDataKeyValue(createStampInterface);
            if ((firstNumberCreationStamp.read(context, objCreateStamp) == null) || (lastNumberCreationStamp.read(context, objCreateStamp) == null)) {
                context.delayUserInterfaction(new MessageClientAction("Необходимо задать диапазон", "Ошибка"));
                return;
            }

            String stringStart = (String) firstNumberCreationStamp.read(context, objCreateStamp);
            String stringFinish = (String) lastNumberCreationStamp.read(context, objCreateStamp);

            if (stringStart.length() != stringFinish.length()) {
                context.delayUserInterfaction(new MessageClientAction("Количество символов у границ диапазонов должно совпадать", "Ошибка"));
                return;
            }

            Integer start = Integer.parseInt(stringStart);
            Integer finish = Integer.parseInt(stringFinish);

            if ((finish - start) > 3000) {
                context.delayUserInterfaction(new MessageClientAction("Слишком большой диапазон (больше 3000)", "Ошибка"));
                return;
            }

            for (int i = start; i <= finish; i++) {
                DataObject stampObject = context.addObject(stamp);
                creationStampStamp.change(objCreateStamp.getValue(), context, stampObject);
                sidStamp.change(BaseUtils.padl(((Integer) i).toString(), stringStart.length(), '0'), context, stampObject);
            }
        }
    }

    public class CloneItemActionProperty extends UserActionProperty {
        private ClassPropertyInterface itemInterface;

        public CloneItemActionProperty() {
            super(genSID(), "Копировать", new ValueClass[]{item});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            itemInterface = i.next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            DataObject cloneObject = context.getDataKeyValue(itemInterface);
            DataObject newObject = context.addObject(item);

            for(LCP lp : new LCP[]{colorSupplierItem, sizeSupplierItem})
                lp.change(lp.read(context, cloneObject), context, newObject);
        }
    }

    public class SeekRouteActionProperty extends UserActionProperty {

        private ClassPropertyInterface shipmentInterface;
        private ClassPropertyInterface skuInterface;
        private ClassPropertyInterface routeInterface;

        // route в интерфейсе нужен только, чтобы найти нужный ObjectInstance (не хочется бегать и искать его по массиву ObjectInstance)
        public SeekRouteActionProperty() {
            super(genSID(), "Поиск маршрута", new ValueClass[]{shipment, sku, route});

            Iterator<ClassPropertyInterface> i = interfaces.iterator();
            shipmentInterface = i.next();
            skuInterface = i.next();
            routeInterface = i.next();
        }

        @Override
        public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
            context.emitExceptionIfNotInFormSession();

            DataObject objShipment = context.getDataKeyValue(shipmentInterface);
            DataObject objSku = context.getDataKeyValue(skuInterface);

            DataObject objRouteRB = route.getDataObject("rb");
            DataObject objRouteRF = route.getDataObject("rf");

            Double invoiced = (Double) invoicedShipmentSku.read(context, objShipment, objSku);

            DataObject objRouteResult;
            if (invoiced == null) {
                Double percentRF = (Double) percentShipmentRouteSku.read(context, objShipment, objRouteRF, objSku);
                objRouteResult = (percentRF != null && percentRF > 1E-9) ? objRouteRF : objRouteRB;
            } else {

                Double invoicedRB = (Double) BaseUtils.nvl(invoicedShipmentRouteSku.read(context, objShipment, objRouteRB, objSku), 0.0);
                Double quantityRB = (Double) BaseUtils.nvl(quantityShipmentRouteSku.read(context, objShipment, objRouteRB, objSku), 0.0);

                Double invoicedRF = (Double) BaseUtils.nvl(invoicedShipmentRouteSku.read(context, objShipment, objRouteRF, objSku), 0.0);
                Double quantityRF = (Double) BaseUtils.nvl(quantityShipmentRouteSku.read(context, objShipment, objRouteRF, objSku), 0.0);

                if (quantityRB + 1E-9 < invoicedRB) {
                    objRouteResult = objRouteRB;
                } else if (quantityRF + 1E-9 < invoicedRF) {
                    objRouteResult = objRouteRF;
                } else {
                    Double percentRB = (Double) percentShipmentRouteSku.read(context, objShipment, objRouteRB, objSku);
                    objRouteResult = (percentRB != null && percentRB > 1E-9) ? objRouteRB : objRouteRF;
                }
            }

            ObjectInstance objectInstance = (ObjectInstance) context.getObjectInstance(routeInterface);
            if (!objRouteResult.equals(objectInstance.getObjectValue())) {
                try {
                    context.delayUserInterfaction(new AudioClientAction(getClass().getResourceAsStream(
                            objRouteResult.equals(objRouteRB) ? "/audio/rb.wav" : "/audio/rf.wav"
                    )));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            context.getFormInstance().seekObject(objectInstance, objRouteResult);
        }
    }

    protected LAP addTAProp(LCP sourceProperty, LCP targetProperty) {
        return addProperty(null, new LAP(new TranslateActionProperty(genSID(), "translate", BL.I18n.getLCPByName("translationDictionaryEntryDictionaryTerm"), BL.I18n.getLCPByName("insensitiveTranslationDictionaryEntryDictionaryTerm"), BL.I18n.getLCPByName("insensitiveDictionary"), sourceProperty, targetProperty, BL.I18n.getClassByName("Dictionary"))));
    }

    protected LCP addDProp(AbstractGroup group, String name, String caption, ValueClass value, boolean stored, ValueClass... params) {
        if(stored)
            return addDProp(group, name, caption, value, params);
        else
            return addSDProp(group, name, caption, value, params);
    }

    protected <D extends PropertyInterface> LCP addDCProp(AbstractGroup group, String name, String caption, LCP<D> derivedProp, Object... params) {
        return addDCProp(group, name, caption, 0, derivedProp, params);
    }

    protected LAP addFAProp(String caption, FormEntity form, ObjectEntity... params) {
        return addFAProp(null, caption, form, params, null, false, false);
    }

    protected LAP addDMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession) {
        return addDMFAProp(group, genSID(), caption, form, objectsToSet, newSession);
    }

    protected LAP addDMFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, boolean newSession, ActionPropertyObjectEntity startAction) {
        return addDMFAProp(group, genSID(), caption, form, objectsToSet, startAction, newSession);
    }

    protected LAP addFAProp(AbstractGroup group, String caption, FormEntity form, ObjectEntity[] objectsToSet, ActionPropertyObjectEntity startAction, boolean newSession, boolean isModal) {
        return addFAProp(group, genSID(), caption, form, objectsToSet, startAction, newSession, isModal, false);
    }

    protected <P extends PropertyInterface> LCP addTCProp(Time time, String name, boolean isStored, String caption, LCP<P> changeProp, ValueClass... classes) {
        return addTCProp(null, time, name, isStored, caption, changeProp, classes);
    }

    protected <P extends PropertyInterface, T extends PropertyInterface> LCP addTCProp(AbstractGroup group, Time time, String name, boolean isStored, String caption, LCP<P> changeProp, ValueClass... classes) {
        LCP timeProperty = addDProp(group, name, caption, time.getConcreteValueClass(), isStored, overrideClasses(changeProp.getInterfaceClasses(), classes));
        LCP curTime = addTProp(time);
        LAP setCurTime = addSetPropertyAProp(BaseUtils.add(directLI(timeProperty), curTime));
        setCurTime.setEventAction(this, Event.SESSION, changeProp, getIntParams(changeProp));
/*        LAP editAction = changeProp.getEditAction(ServerResponse.CHANGE);
        LAP<?> overrideAction = addListAProp(BaseUtils.add(directLI(editAction), directLI(setCurTime)));
        changeProp.setEditAction(ServerResponse.CHANGE, overrideAction);*/
        return timeProperty;
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(Object... params) {
        return addSetPropertyAProp("sys", params);
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(String caption, Object... params) {
        return addSetPropertyAProp(genSID(), caption, params);
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(String name, String caption, Object... params) {
        return addSetPropertyAProp(null, name, caption, params);
    }

    protected <C extends PropertyInterface, W extends PropertyInterface> LAP addSetPropertyAProp(AbstractGroup group, String name, String caption, Object... params) {
        int resInterfaces = getIntNum(params);
        return addSetPropertyAProp(group, name, caption, resInterfaces, false, BaseUtils.add(ListFact.consecutiveList(resInterfaces).toArray(new Integer[resInterfaces]), params));
    }

    protected LAP addChangeClassAProp() {
        return addAProp(baseClass.getChangeClassValueAction());
    }

    protected LAP addIfAProp(Object... params) {
        return addIfAProp(false, params);
    }

    protected LAP addIfAProp(boolean not, Object... params) {
        return addIfAProp("if", not, params);
    }

    protected LAP addIfAProp(String caption, Object... params) {
        return addIfAProp(caption, false, params);
    }

    protected LAP addIfAProp(String caption, boolean not, Object... params) {
        return addIfAProp(null, genSID(), caption, not, params);
    }

    protected LAP addForAProp(boolean hasElse, int resInterfaces, Object... params) {
        return addForAProp(false, false, false, hasElse, resInterfaces, params);
    }

    protected LAP addForAProp(boolean ascending, boolean ordersNotNull, boolean recursive, boolean hasElse, int resInterfaces, Object... params) {
        return addForAProp(null, genSID(), "sys", ascending, ordersNotNull, recursive, hasElse, resInterfaces, null, true, 0, false, params);
    }

    protected <P extends PropertyInterface> LCP addSCProp(LCP<P> lp) {
        return addSCProp(baseLM.privateGroup, "sys", lp);
    }

    protected <P extends PropertyInterface> LCP addSCProp(AbstractGroup group, String caption, LCP<P> lp) {
        return addProperty(group, new LCP<ShiftChangeProperty.Interface<P>>(new ShiftChangeProperty<P, PropertyInterface>(genSID(), caption,
                ((CalcProperty) lp.property), new CalcPropertyMapImplement<PropertyInterface, P>((CalcProperty<PropertyInterface>) getLCPByName("reverseBarcode").property))));
    }

    protected LCP addTProp(Time time) {
        return addTProp(genSID(), time.toString(), time);
    }

    protected LCP addJProp(boolean implementChange, String caption, LCP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, caption, mainProp, params);
    }

    protected LCP addJProp(AbstractGroup group, boolean implementChange, String caption, LCP mainProp, Object... params) {
        return addJProp(group, implementChange, genSID(), caption, mainProp, params);
    }

    protected LCP addJProp(boolean implementChange, LCP mainProp, Object... params) {
        return addJProp(baseLM.privateGroup, implementChange, genSID(), "sys", mainProp, params);
    }

    protected LAP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LAP mainProperty) {
        int mainInts[] = consecutiveInts(mainProperty.listInterfaces.size());

        return addGCAProp(group, name, caption, groupObject, mainProperty, mainInts, mainInts);
    }

    /**
     * Создаёт свойство для группового изменения
     * Пример:
     * <pre>
     *   LCP ценаПоставкиТовара = Свойство(Товар)
     *   LCP ценаПродажиТовара = Свойство(Магазин, Товар)
     *
     *   Тогда, чтобы установить цену для всех товаров в магазине, равной цене поставки товара, создаём свойство
     *
     *   addGCAProp(...,
     *      ценаПродажиТовара,  // изменяемое свойство
     *      1, 2,               // номера интерфейсов результирующего свойства на входах изменяемого свойтства
     *      ценаПоставкиТовара, // изменяющее свойство
     *      2,                  // номера интерфейсов результирующего свойства на входах изменяющего свойтства
     *      2                   // номера интерфейсов для группировки
     *      )
     * </pre>
     *
     * Если groupObject != null, то результирующее свойство будет принимать на входы в том числе и группирующие интерфейсы.
     * Это нужно для того, чтобы можно было создать фильтр по ключам этого groupObject'а.
     * Если groupObject == null, то группирующие интерфейсы включены не будут.
     *
     * @param groupObject используется для получения фильтров на набор, для которого будут происходить изменения.
     *
     * @param params      сначала идут номера интерфейсов для входов главного свойства,
     *                      затем getterProperty,
     *                      затем мэппинг интерфейсов getterProperty,
     *                      затем номера групирующих интерфейсов
     */
    protected LAP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LAP mainProperty, Object... params) {
        assert params.length > 0;

        int mainIntCnt = mainProperty.listInterfaces.size();

        int groupIntCnt = params.length - mainIntCnt;

        int mainInts[] = new int[mainIntCnt];
        int groupInts[] = new int[groupIntCnt];

        for (int i = 0; i < mainIntCnt; ++i) {
            mainInts[i] = (Integer)params[i] - 1;
        }

        for (int i = 0; i < groupIntCnt; ++i) {
            groupInts[i] = (Integer)params[mainIntCnt] - 1;
        }

        return addGCAProp(group, name, caption, groupObject, mainProperty, mainInts, groupInts);
    }

    private LAP addGCAProp(AbstractGroup group, String name, String caption, GroupObjectEntity groupObject, LAP mainProperty, int[] mainInts, int[] groupInts) {
        return addProperty(group, new LAP(
                new PrevGroupChangeActionProperty(name, caption, groupObject,
                        mainProperty, mainInts, groupInts)));
    }

    protected LCP addSGProp(LCP groupProp, Object... params) {
        return addSGProp(baseLM.privateGroup, "sys", groupProp, params);
    }

    protected LCP addSGProp(AbstractGroup group, String caption, LCP groupProp, Object... params) {
        return addSGProp(group, genSID(), caption, groupProp, params);
    }

    protected LCP addSGProp(String name, String caption, LCP groupProp, Object... params) {
        return addSGProp(name, false, caption, groupProp, params);
    }

    protected LCP addSGProp(String name, boolean persistent, String caption, LCP groupProp, Object... params) {
        return addSGProp(null, name, persistent, caption, groupProp, params);
    }

    protected LCP addSGProp(AbstractGroup group, String name, String caption, LCP groupProp, Object... params) {
        return addSGProp(group, name, false, caption, groupProp, params);
    }

    protected LCP addSGProp(AbstractGroup group, String name, boolean persistent, String caption, LCP groupProp, Object... params) {
        return addSGProp(group, name, persistent, false, caption, groupProp, params);
    }

    protected <T extends PropertyInterface> LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, LCP<T> groupProp, Object... params) {
        return addSGProp(group, name, persistent, notZero, caption, groupProp, readCalcImplements(groupProp.listInterfaces, params));
    }

    private <T extends PropertyInterface> LCP addSGProp(AbstractGroup group, String name, boolean persistent, boolean notZero, String caption, LCP<T> groupProp, ImList<CalcPropertyInterfaceImplement<T>> listImplements) {
        return addSGProp(group, name, persistent, notZero, caption, groupProp.listInterfaces, ListFact.add(((CalcProperty<T>) groupProp.property).getImplement(), listImplements));
    }

    protected LCP addMGProp(String name, String caption, LCP groupProp, Object... params) {
        return addMGProp(null, name, caption, groupProp, params);
    }

    protected LCP addAGProp(String name, String caption, LCP... props) {
        return addAGProp(null, name, caption, props);
    }

    protected LCP addAGProp(String name, String caption, boolean noConstraint, LCP... props) {
        return addAGProp(null, name, caption, noConstraint, props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, LCP... props) {
        return addAGProp(group, name, caption, false, props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, boolean noConstraint, LCP... props) {
        LCP andProp = addJProp(and(new boolean[props.length - 1]), getUParams(props));
        return addAGProp(group, name, caption, noConstraint, (CustomClass) andProp.getInterfaceClasses()[0], props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, CustomClass customClass, LCP... props) {
        return addAGProp(group, name, caption, false, customClass, props);
    }

    protected LCP addAGProp(AbstractGroup group, String name, String caption, boolean noConstraint, CustomClass customClass, LCP... props) {
        return addAGProp(group, false, name, false, caption, noConstraint, customClass, props);
    }

    protected LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, CustomClass customClass, LCP... props) {
        if(props.length==1)
            ((CalcProperty)props[0].property).aggProp = true;
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, is(customClass), add(1, getUParams(props)));
    }

    protected <T extends PropertyInterface> LCP addAGProp(AbstractGroup group, String name, String caption, LCP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, false, name, false, caption, lp, aggrInterface, props);
    }

    protected <T extends PropertyInterface<T>> LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, LCP<T> lp, int aggrInterface, Object... props) {
        return addAGProp(group, checkChange, name, persistent, caption, false, lp, add(aggrInterface, props));
    }

    protected <T extends PropertyInterface<T>> LCP addAGProp(AbstractGroup group, boolean checkChange, String name, boolean persistent, String caption, boolean noConstraint, LCP<T> lp, Object... props) {
        ImList<CalcPropertyInterfaceImplement<T>> readImplements = readCalcImplements(lp.listInterfaces, props);
        T aggrInterface = (T) readImplements.get(0);
        ImList<CalcPropertyInterfaceImplement<T>> groupImplements = readImplements.subList(1, readImplements.size());
        ImList<CalcPropertyInterfaceImplement<T>> fullInterfaces = groupImplements.addList(lp.listInterfaces.removeOrderIncl(aggrInterface));
        return addAGProp(group, checkChange, name, persistent, caption, noConstraint, lp.listInterfaces, SetFact.toOrderExclSet(aggrInterface, lp.property.getImplement()).addList(fullInterfaces));
    }

    protected <T extends PropertyInterface, P extends PropertyInterface> LCP addDGProp(AbstractGroup group, String name, String caption, int orders, boolean ascending, LCP<T> groupProp, Object... params) {
        return addDGProp(group, name, false, caption, orders, ascending, groupProp, params);
    }

    protected <T extends PropertyInterface> LCP addDGProp(AbstractGroup group, String name, boolean persistent, String caption, int orders, boolean ascending, LCP<T> groupProp, Object... params) {
        return addDGProp(group, name, persistent, caption, orders, ascending, false, groupProp, params);
    }

    protected <T extends PropertyInterface> LCP addDGProp(AbstractGroup group, String name, boolean persistent, String caption, int orders, boolean ascending, boolean over, LCP<T> groupProp, Object... params) {
        ImList<CalcPropertyInterfaceImplement<T>> listImplements = readCalcImplements(groupProp.listInterfaces, params);
        int intNum = listImplements.size();
        LCP result = addSGProp(group, name, persistent, false, caption, groupProp, listImplements.subList(0, intNum - orders - 1));
        result.setDG(ascending, over, listImplements.subList(intNum - orders - 1, intNum));
        return result;
    }

    protected LCP addCUProp(LCP... props) {
        return addCUProp(baseLM.privateGroup, "sys", props);
    }

    protected LCP addCUProp(String caption, LCP... props) {
        return addCUProp((AbstractGroup) null, caption, props);
    }

    protected LCP addCUProp(AbstractGroup group, String caption, LCP... props) {
        return addCUProp(group, genSID(), caption, props);
    }

    protected LCP addCUProp(String name, String caption, LCP... props) {
        return addCUProp(name, false, caption, props);
    }

    protected LCP addCUProp(String name, boolean persistent, String caption, LCP... props) {
        return addCUProp(null, name, persistent, caption, props);
    }

    protected LCP addCUProp(AbstractGroup group, String name, String caption, LCP... props) {
        return addCUProp(group, name, false, caption, props);
    }

    protected LCP addCUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP... props) {
        return addXSUProp(group, name, persistent, caption, props);
    }

    protected LCP addJProp(boolean implementChange, String name, String caption, LCP mainProp, Object... params) {
        return addJProp((AbstractGroup) null, implementChange, name, caption, mainProp, params);
    }

    protected LCP addDUProp(AbstractGroup group, String name, String caption, LCP prop1, LCP prop2) {
        return addDUProp(group, name, false, caption, prop1, prop2);
    }

    protected LCP addDUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP prop1, LCP prop2) {
        return addUProp(group, name, persistent, caption, Union.SUM, null, new int[]{1, -1}, getUParams(new LCP[]{prop1, prop2}));
    }

    public LCP addSUProp(Union unionType, LCP... props) {
        return addSUProp(baseLM.privateGroup, "sys", unionType, props);
    }

    protected LCP addSUProp(AbstractGroup group, String caption, Union unionType, LCP... props) {
        return addSUProp(group, genSID(), caption, unionType, props);
    }

    protected LCP addSUProp(String name, String caption, Union unionType, LCP... props) {
        return addSUProp(name, false, caption, unionType, props);
    }

    protected LCP addXSUProp(AbstractGroup group, String name, boolean persistent, String caption, LCP... props) {
        return addUProp(group, name, persistent, caption, Union.EXCLUSIVE, null, null, getUParams(props));
    }

    protected LAP addMAProp(String message, String title) {
        return addMAProp(null, message, title);
    }

    protected LAP addMAProp(AbstractGroup group, String message, String title) {
        return addMAProp(group, "", title, addCProp(StringClass.get(message.length()), message));
    }

    protected LAP addAAProp(ConcreteCustomClass customClass, LCP... properties) {
        return addAAProp(null, customClass, null, properties);
    }

    protected LAP addAAProp(String caption, ConcreteCustomClass customClass, LCP... properties) {
        return addAAProp(caption, null, customClass, null, properties);
    }

    protected LAP addAAProp(LCP barcode, ConcreteCustomClass customClass, LCP barcodePrefix, LCP... properties) {
        return addAAProp(ServerResourceBundle.getString("logics.add"), barcode, customClass, barcodePrefix, properties);
    }

    protected LAP addAAProp(String caption, LCP<?> barcode, ConcreteCustomClass customClass, LCP<?> barcodePrefix, LCP... properties) {
        PropertyInterface addedInterface = new PropertyInterface();

        MList<ActionPropertyMapImplement<?, PropertyInterface>> mList = ListFact.mList();

        // генерация штрихкода
        if(barcode!=null)
            mList.add(new ChangeBarcodeActionProperty(barcode.property, barcodePrefix != null ? barcodePrefix.property : null).getImplement(SetFact.singletonOrder(addedInterface)));

        MCol<CalcPropertyMapImplement<?, PropertyInterface>> mCheckClasses = ListFact.mCol();
        MOrderExclSet<PropertyInterface> mInnerInterfaces = SetFact.mOrderExclSet(properties.length);
        for(LCP<?> lp : properties) {
            PropertyInterface genInterface = new PropertyInterface();
            ImSet<PropertyInterface> setInterfaces = SetFact.toExclSet(genInterface, addedInterface);
            mList.add(createSetAction(setInterfaces, lp.getImplement(addedInterface), genInterface));
            mInnerInterfaces.exclAdd(genInterface);

            ValueClass valueClass = lp.property.getValueClass();
            if(valueClass instanceof CustomClass) {
                mCheckClasses.add(((CustomClass)valueClass).getProperty().getImplement(SetFact.singletonOrder(genInterface)));
            }
        }
        ImOrderSet<PropertyInterface> innerInterfaces = mInnerInterfaces.immutableOrder();
        ImCol<CalcPropertyMapImplement<?, PropertyInterface>> checkClasses = mCheckClasses.immutableCol();

        mList.add(addSAProp().getImplement(addedInterface));

        ImSet<PropertyInterface> addedInnerInterfaces = innerInterfaces.getSet().addExcl(addedInterface);
        ActionPropertyMapImplement<?, PropertyInterface> result = createForAction(addedInnerInterfaces, innerInterfaces.getSet(), createListAction(addedInnerInterfaces, mList.immutableList()), addedInterface, customClass, false);
        if(checkClasses.size() > 0)
            result = createIfAction(innerInterfaces.getSet(), createAnd(checkClasses), result, null);
        return mapLAProp(null, result, innerInterfaces);
    }

    private <P extends PropertyInterface, L extends PropertyInterface> LAP mapLAProp(AbstractGroup group, ActionPropertyMapImplement<L, P> implement, ImOrderSet<P> listInterfaces) {
        return addProperty(group, new LAP<L>(implement.property, listInterfaces.mapOrder(implement.mapping.reverse())));
    }

    protected <R extends PropertyInterface, L extends PropertyInterface> LCP addPGProp(AbstractGroup group, String name, boolean persistent, int roundlen, boolean roundfirst, String caption, LCP<R> proportion, LCP<L> ungroup, Object... params) {
        return addPGProp(group, name, persistent, roundlen, roundfirst, caption, proportion.listInterfaces.size(), true, Settings.get().isDefaultOrdersNotNull(), ungroup, add(add(directLI(proportion), params), getParams(proportion)));
    }

    protected LCP addMGProp(LCP groupProp, Object... params) {
        return addMGProp("sys", groupProp, params);
    }

    protected LCP addMGProp(String caption, LCP groupProp, Object... params) {
        return addMGProp(baseLM.privateGroup, genSID(), caption, groupProp, params);
    }

    protected LCP addMGProp(AbstractGroup group, String name, String caption, LCP groupProp, Object... params) {
        return addMGProp(group, name, false, caption, groupProp, params);
    }

    protected LCP addMGProp(AbstractGroup group, String name, boolean persist, String caption, LCP groupProp, Object... params) {
        return addMGProp(group, name, persist, caption, false, groupProp, params);
    }

    protected LCP addMGProp(AbstractGroup group, String name, boolean persist, String caption, boolean min, LCP groupProp, Object... params) {
        return addMGProp(group, name, persist, caption, min, groupProp.listInterfaces.size(), add(directLI(groupProp), params));
    }

    protected LCP addSUProp(AbstractGroup group, String name, String caption, Union unionType, LCP... props) {
        return addSUProp(group, name, false, caption, unionType, props);
    }

}
