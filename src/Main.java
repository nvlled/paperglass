package com.github.nvlled.paperglass;

import java.io.*;
import java.lang.reflect.*;

public class Main {
    // This is actually a good chance to try kotlin.... (or maybe later)

    static void usage(String[] args) {
        System.out.println("Usage: paperglass <class or interface>");
    }

    static String removePackageName(String fqn) {
        return fqn.replaceAll("(\\w+\\.)+", "");
    }

    interface Strfn<T> { String apply(T x); }

    static <T> String join(T[] ts, CharSequence sep, Strfn<T> fn) {
        String[] strs = new String[ts.length];
        for (int i = 0; i < strs.length; i++)
            strs[i] = fn.apply(ts[i]);
        return String.join(sep, strs);
    }

    static String joinTypes(Type[] params) {
        String str = join(params, ", ", new Strfn<Type>() {
            public String apply(Type t) { return t.getTypeName(); }
        });
        return str;
    }

    static String toTypeParamString(Type[] params) {
        String str = joinTypes(params);
        if (str.length() > 0)
            return "<"+str+">";
        return "";
    }

    static String getClassType(Class c) {
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

    static String printModifiers(Member t) { // excluding public
        return Modifier.toString(t.getModifiers())
                       .replace("public", "");
    }

    public static void main(String[] args) {
        boolean showProtected;

        if (args.length == 0) {
            usage(args);
            return;
        }

        PrintStream out = System.out;
        String className = args[0];

        Class<?> c;
        try {
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {
            out.println("not found: " + className);
            out.println("(check your classpath or your spelling)");
            return;
        }


        out.println(getClassType(c) + " " + 
                    c.getSimpleName() + 
                    toTypeParamString(c.getTypeParameters()));

        if (c.getGenericSuperclass() != null)
            out.println("  extends " + removePackageName(c.getGenericSuperclass().toString()));
        
        out.print("  implements " + joinTypes(c.getGenericInterfaces()));
        out.println("\n");

        out.println("constructors: ");
        for (Constructor t: c.getConstructors()) {
            String params = join(t.getParameterTypes(), ", ", new Strfn<Class>() {
                public String apply(Class c) {
                    return c.getSimpleName();
                }
            });
            out.print("  "+c.getSimpleName() + "("+params+")");
            out.println(" " + printModifiers(t));
        }
        out.println();

        out.println("methods: ");
        for (Method t: c.getMethods()) {
            Type ret = t.getGenericReturnType();
            out.print("    ");
            out.print(t.getName() + "(" + joinTypes(t.getGenericParameterTypes()) + ")");
            out.print(": " + removePackageName(ret.getTypeName()));

            String modStr = printModifiers(t);
            if (modStr.length() > 0)
                out.print(" | " + modStr);
            out.println();
        }
    }
}
