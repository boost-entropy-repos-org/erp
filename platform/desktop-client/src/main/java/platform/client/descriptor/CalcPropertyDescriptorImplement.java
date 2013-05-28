package platform.client.descriptor;

import platform.client.descriptor.property.CalcPropertyDescriptor;
import platform.client.descriptor.property.PropertyInterfaceDescriptor;

import java.util.Map;

public class CalcPropertyDescriptorImplement<T> extends PropertyDescriptorImplement {
    public CalcPropertyDescriptorImplement(CalcPropertyDescriptor property, Map<PropertyInterfaceDescriptor, T> mapping) {
        super(property, mapping);
    }

    public CalcPropertyDescriptorImplement(CalcPropertyDescriptorImplement<T> implement) {
        super(implement);
    }
}
