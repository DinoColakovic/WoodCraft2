package unze.ptf.woodcraft.woodcraft.util;

import unze.ptf.woodcraft.woodcraft.model.UnitSystem;

public final class UnitConverter {
    private static final double CM_PER_INCH = 2.54;

    private UnitConverter() {
    }

    public static double toCm(double value, UnitSystem unit) {
        if (unit == UnitSystem.IN) {
            return value * CM_PER_INCH;
        }
        return value;
    }

    public static double fromCm(double cmValue, UnitSystem unit) {
        if (unit == UnitSystem.IN) {
            return cmValue / CM_PER_INCH;
        }
        return cmValue;
    }
}
