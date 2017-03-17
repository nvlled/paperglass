package com.github.nvlled.paperglass;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.jar.*;

public class Main {
    // This is actually a good chance to try kotlin.... (or maybe later)

    static Pattern methodPat = Pattern.compile(".*");

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
            public String apply(Type t) { return removePackageName(t.getTypeName()); }
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

    static boolean isLoadable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ExceptionInInitializerError e) {
        } catch (Exception e) { }
        return false;
    }

    public static String joinPath(String path1, String path2) {
        int len = path1.length();
        if (len == 0)
            return path2;
        if (path1.charAt(len-1) != '/')
            path1 = path1 + "/";
        return path1 + path2;
    }

    public static void findLoadableClasses(String classDir, String sub) {
        File dirFile = new File(classDir + "/" + sub);
        if (dirFile.isDirectory()) {
            for (String filename: dirFile.list()) {
                String className = joinPath(sub, filename).replace("/", ".");
                className = className.replace(".class", "");
                if (isLoadable(className)) {
                    if (methodPat.matcher(className).matches())
                        System.out.println(className);
                } else {
                    findLoadableClasses(classDir, joinPath(sub, filename));
                }
            }
        }
    }

    public static boolean getClassNamesFromPackage(String packageName) throws IOException{
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;

        String packageDir = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageDir);

        if (packageURL == null)
            return false;

        if (packageURL.getProtocol() == "file") {
            String path = packageURL.getPath();
            String classDir = path.replace(packageDir, "");
            findLoadableClasses(classDir, packageDir);

        } else if(packageURL.getProtocol().equals("jar")){
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            while(jarEntries.hasMoreElements()){
                entryName = jarEntries.nextElement().getName();
                if(entryName.startsWith(packageDir) && entryName.length()>packageDir.length()+5){
                    int i = entryName.lastIndexOf('.');
                    if (i >= 0) {
                        entryName = entryName.substring(packageDir.length(), i);
                        String className = (packageDir + entryName).replace("/", ".");
                        if (isLoadable(className) && methodPat.matcher(className).matches())
                            System.out.println(className);
                    }
                }
            }
        }
        return true;
    }

    static String findClassdir(String packageName) {
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

        for(int i=0; i< urls.length; i++)
        {
            String classDir = urls[i].getFile();
            String dir = packageName.replace(".", "/");
            File dirFile = new File(classDir + "/" + dir);
            if (dirFile.isDirectory()) {
                return classDir;
            }
        } 
        return "";
    }

    public static void main(String[] args) throws Exception {
        boolean showProtected;

        if (args.length == 0) {
            usage(args);
            return;
        }

        PrintStream out = System.out;
        String className = args[0];

        if (args.length > 1) {
            String pat = ".*" + args[1] + ".*";
            methodPat = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
        }

        Class<?> c;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {

            String packageName = className;
            boolean found = getClassNamesFromPackage(packageName);
            if (!found) {
                out.print("not found: " + className);
                out.println(" (check your classpath or your spelling)");
            }

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
            if ( ! methodPat.matcher(t.getName()).matches())
                continue;

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



