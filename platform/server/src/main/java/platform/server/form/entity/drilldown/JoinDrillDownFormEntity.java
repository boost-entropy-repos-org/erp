package platform.server.form.entity.drilldown;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MMap;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.server.classes.ValueClass;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.filter.CompareFilterEntity;
import platform.server.form.view.ContainerView;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.FormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.*;

import java.util.ArrayList;
import java.util.List;

import static platform.base.BaseUtils.isRedundantString;
import static platform.server.logics.ServerResourceBundle.getString;

public class JoinDrillDownFormEntity<I extends PropertyInterface> extends DrillDownFormEntity<JoinProperty.Interface, JoinProperty<I>> {

    private List<PropertyDrawEntity> detailsProperties;
    private PropertyDrawEntity implPropertyDraw;

    public JoinDrillDownFormEntity(String sID, String caption, JoinProperty<I> property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }

    @Override
    protected void setupDrillDownForm() {
        detailsProperties = new ArrayList<PropertyDrawEntity>();

        CalcProperty<I> implProperty = property.implement.property;
        ImMap<I, CalcPropertyInterfaceImplement<JoinProperty.Interface>> implMapping = property.implement.mapping;
        ImMap<I, ValueClass> implClasses = implProperty.getInterfaceClasses(ClassType.ASSERTFULL);

        MMap<I, ObjectEntity> mImplObjects = MapFact.mMap(MapFact.<I, ObjectEntity>override());

        for (int i = 0; i < implMapping.size(); ++i) {
            I iFace = implMapping.getKey(i);
            CalcPropertyInterfaceImplement<JoinProperty.Interface> intImpl = implMapping.getValue(i);
            if (intImpl instanceof CalcPropertyMapImplement) {
                //добавляем дополнительный объект, если на входе - свойство
                ObjectEntity innerObject  = addSingleGroupObject(implClasses.get(iFace));
                innerObject.groupTo.setSingleClassView(ClassViewType.PANEL);
                mImplObjects.add(iFace, innerObject);

                //добавляем фильтр для этого объекта и соотв. свойства
                CalcPropertyMapImplement<PropertyInterface, JoinProperty.Interface> mapImplement = (CalcPropertyMapImplement<PropertyInterface, JoinProperty.Interface>) intImpl;
                ImMap<PropertyInterface, ObjectEntity> mapImplMapping = mapImplement.mapImplement(interfaceObjects).mapping;
                addFixedFilter(new CompareFilterEntity(addPropertyObject(mapImplement.property, mapImplMapping), Compare.EQUALS, innerObject));

                //и добавляем само свойство на форму, если оно ещё не было добавлено при создании ObjectEntity
                if (mapImplMapping.size() != 1 || !BL.LM.recognizeGroup.hasChild(mapImplement.property)) {
                    if (mapImplement.property.isFull()) {
                        detailsProperties.add(
                                addPropertyDraw(mapImplement.property, mapImplMapping)
                        );
                    }
                }
            } else {
                JoinProperty.Interface intImplement = (JoinProperty.Interface) intImpl;
                mImplObjects.add(iFace, interfaceObjects.get(intImplement));
            }
        }

        ImMap<I, ObjectEntity> implObjects = mImplObjects.immutable();
        implPropertyDraw = addPropertyDraw(implProperty, implObjects);
    }

    @Override
    public FormView createDefaultRichDesign() {
        DefaultFormView design = (DefaultFormView) super.createDefaultRichDesign();

        ContainerView extraParamsContainer = design.createContainer(getString("logics.property.drilldown.form.inner.params"));
        design.mainContainer.addAfter(extraParamsContainer, valueContainer);
        for (PropertyDrawEntity detailProperty : detailsProperties) {
            PropertyDrawView detailPropertyView = design.get(detailProperty);
            if (isRedundantString(detailPropertyView.getCaption())) {
                detailPropertyView.caption = detailProperty.propertyObject.property.getSID();
            }
            detailsContainer.add(detailPropertyView);
        }

        valueContainer.add(design.get(implPropertyDraw));

        return design;
    }
}
