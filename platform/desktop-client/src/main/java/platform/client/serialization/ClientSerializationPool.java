package platform.client.serialization;

import platform.base.context.ApplicationContext;
import platform.base.serialization.SerializationPool;
import platform.client.descriptor.*;
import platform.client.descriptor.filter.*;
import platform.client.descriptor.property.AbstractGroupDescriptor;
import platform.client.descriptor.property.ActionPropertyDescriptor;
import platform.client.descriptor.property.CalcPropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;
import platform.client.logics.*;


public class ClientSerializationPool extends SerializationPool<ClientForm> {


    public ClientSerializationPool() {
        this(null, null);
    }

    public ClientSerializationPool(ApplicationContext appContext) {
        this(null, appContext);
    }

    public ClientSerializationPool(ClientForm context, ApplicationContext appContext) {
        super(context, appContext);
        //порядок добавления должен соответствовать порядку в ServerSerializationPool

        addMapping(FormDescriptor.class);
        addMapping(GroupObjectDescriptor.class);
        addMapping(TreeGroupDescriptor.class);
        addMapping(PropertyDrawDescriptor.class);
        addMapping(CalcPropertyDescriptor.class);
        addMapping(ActionPropertyDescriptor.class);
        addMapping(AbstractGroupDescriptor.class);

        addMapping(PropertyFilterDescriptor.class);
        addMapping(CompareFilterDescriptor.class);
        addMapping(IsClassFilterDescriptor.class);
        addMapping(NotNullFilterDescriptor.class);

        addMapping(OrFilterDescriptor.class);
        addMapping(NotFilterDescriptor.class);

        addMapping(RegularFilterDescriptor.class);
        addMapping(RegularFilterGroupDescriptor.class);

        addMapping(PropertyInterfaceDescriptor.class);

        addMapping(DataObjectDescriptor.class);
        addMapping(ObjectDescriptor.class);
        addMapping(CalcPropertyObjectDescriptor.class);
        addMapping(ActionPropertyObjectDescriptor.class);

        addMapping(ClientForm.class);
        addMapping(ClientComponent.class);
        addMapping(ClientContainer.class);
        addMapping(ClientGroupObject.class);
        addMapping(ClientTreeGroup.class);
        addMapping(ClientShowType.class);
        addMapping(ClientGrid.class);
        addMapping(ClientToolbar.class);
        addMapping(ClientFilter.class);
        addMapping(ClientClassChooser.class);
        addMapping(ClientObject.class);
        addMapping(ClientPropertyDraw.class);
        addMapping(ClientRegularFilter.class);
        addMapping(ClientRegularFilterGroup.class);
    }
}
