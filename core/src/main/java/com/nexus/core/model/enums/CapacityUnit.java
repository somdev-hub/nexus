package com.nexus.core.model.enums;

/**
 * Unit for a logistics routing-capacity quantity.
 *
 * Mass/volume units (KG, LBS, TONNES, LITRES, GALLONS, CUBIC_METERS,
 * CUBIC_FEET) describe bulk capacity directly. Unitized values (PALLETS,
 * SHIPPING_CONTAINER, FREIGHT_CONTAINER) count load units and therefore
 * require per-unit specifications (length/width/height/total volume) so
 * suppliers can judge whether their freight fits.
 */
public enum CapacityUnit {
    KG,
    LBS,
    TONNES,
    LITRES,
    GALLONS,
    CUBIC_METERS,
    CUBIC_FEET,
    PALLETS,
    SHIPPING_CONTAINER,
    FREIGHT_CONTAINER;

    public boolean isUnitized() {
        return this == PALLETS || this == SHIPPING_CONTAINER || this == FREIGHT_CONTAINER;
    }
}
