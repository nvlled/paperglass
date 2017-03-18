package com.github.nvlled.paperglass;

import java.util.*;
import java.lang.reflect.*;

public class ClassUtil {
    public static String getClassType(Class c) {
        if (c.isEnum())
            return "enum";
        else if (c.isInterface())
            return "interface";
        else if (c.isArray())
            return "array";
        else if (c.isPrimitive())
            return "primitive";
        else
            return "class";
    }

    public static String printModifiers(Member t) { // excluding public
        return Modifier.toString(t.getModifiers())
                .replace("public", "");
    }

    public static boolean isLoadable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Error e) {
        } catch (Exception e) { }
        return false;
    }

    public static String removePackageName(String fqn) {
        return fqn.replaceAll("(\\w+\\.)+", "");
    }
}



