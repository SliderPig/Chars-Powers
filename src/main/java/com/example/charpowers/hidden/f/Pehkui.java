package com.example.charpowers.hidden.f;

import com.example.charpowers.CharPowers;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;

// Talks to the Pehkui mod (changes how big things are) without needing it to build:
// ScaleTypes.BASE.getScaleData(entity).setTargetScale(x). If Pehkui isn't there, scale() just says false.
final class Pehkui {
    private static boolean tried;
    private static Object base;
    private static Method getScaleData, setTargetScale;

    static boolean scale(Entity e, float target) {
        if (!tried) {
            tried = true;
            try {
                Class<?> types = Class.forName("virtuoel.pehkui.api.ScaleTypes");
                base = types.getField("BASE").get(null);
                getScaleData = base.getClass().getMethod("getScaleData", Entity.class);
                Class<?> data = Class.forName("virtuoel.pehkui.api.ScaleData");
                setTargetScale = data.getMethod("setTargetScale", float.class);
            } catch (ReflectiveOperationException | LinkageError ex) {
                CharPowers.LOG.info("Pehkui not found, Mini Me / Tall Tale will do nothing");
                base = null;
            }
        }
        if (base == null) return false;
        try {
            setTargetScale.invoke(getScaleData.invoke(base, e), target);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private Pehkui() {}
}
