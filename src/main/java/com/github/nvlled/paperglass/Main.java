package com.github.nvlled.paperglass;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.jar.*;

public class Main {
    // This is actually a good chance to try kotlin.... (or maybe later)
    interface Strfn<T> { String apply(T x); }

    static class SearchResult {

        public final int matchCount;
        public final String lastMatch;

        public SearchResult(int count, String match) {
            matchCount = count;
            lastMatch = match;
        }
        @Override
        public String toString() {
            return String.format("result{count=%d, lastMatch=%s", matchCount, lastMatch);
        }
    }

    static Pattern packagePat = Pattern.compile(".*");
    static Pattern methodPat  = Pattern.compile(".*");

    static void usage() {
        System.out.println("Usage: paperglass <CLASS or PACKAGE>");
        System.out.println("       paperglass <CLASS> [methodName regex]");
        System.out.println("       paperglass <PACKAGE> [package regex] [methodName regex]");
        System.out.println("");
        System.out.println("CLASS must be fully qualified, e.g., com.example.sub.ClassName");
        System.out.println("PACKAGE can be partial (it is not a regex).");
        System.out.println("  For instance, if PACKAGE is com.example, then all classes under");
        System.out.println("  com.example and com.example.sub and so on are listed.");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("    paperglass com.example.ClassA methodName");
        System.out.println("    paperglass com.example.sub.ClassB getS");
        System.out.println("    paperglass com.example ClassB methodNa");
        System.out.println("    paperglass com.example");
        System.out.println("    paperglass com");
    }

    static String removePackageName(String fqn) {
        return fqn.replaceAll("(\\w+\\.)+", "");
    }


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

    public static SearchResult findLoadableClasses(String classDir, String sub) {
        File dirFile = new File(classDir + "/" + sub);
        int matches = 0;
        String lastMatch = "";
        if (dirFile.isDirectory()) {
            for (String filename: dirFile.list()) {
                String className = joinPath(sub, filename).replace("/", ".");
                className = className.replace(".class", "");
                if (isLoadable(className)) {
                    if (packagePat.matcher(className).matches()) {
                        System.out.println(className);
                        lastMatch = className;
                        matches++;
                    }
                } else {
                    SearchResult subResult = findLoadableClasses(classDir, joinPath(sub, filename));
                    if (subResult.matchCount > 0) {
                        matches = subResult.matchCount;
                        lastMatch = subResult.lastMatch;
                    }
                }
            }
        }
        return new SearchResult(matches, lastMatch);
    }

    public static SearchResult getClassNamesFromPackage(String packageName) throws IOException{
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;

        String packageDir = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageDir);

        if (packageURL == null)
            return new SearchResult(0, "");

        if (packageURL.getProtocol() == "file") {
            String path = packageURL.getPath();
            String classDir = path.replace(packageDir, "");
            return findLoadableClasses(classDir, packageDir);
        } 
        
        int matches = 0;
        String lastMatch = "";
        if(packageURL.getProtocol().equals("jar")) {
            String jarFileName;
            JarFile jf;
            Enumeration<JarEntry> jarEntries;
            String entryName;

            jarFileName = URLDecoder.decode(packageURL.getFile(), "UTF-8");
            jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));
            jf = new JarFile(jarFileName);
            jarEntries = jf.entries();
            while(jarEntries.hasMoreElements()) {
                entryName = jarEntries.nextElement().getName();
                if(entryName.startsWith(packageDir) && entryName.length()>packageDir.length()+5) {
                    int i = entryName.lastIndexOf('.');
                    if (i >= 0) {
                        entryName = entryName.substring(packageDir.length(), i);
                        String className = (packageDir + entryName).replace("/", ".");
                        if (isLoadable(className) && packagePat.matcher(className).matches()) {
                            System.out.println(className);
                            lastMatch = className;
                            matches++;
                        }
                    }
                }
            }
        }

        return new SearchResult(matches, lastMatch);
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
            usage();
            return;
        }

        PrintStream out = System.out;
        String className = args[0];

        if (args.length == 3) {
            packagePat = Pattern.compile(".*" + args[1] + ".*", Pattern.CASE_INSENSITIVE);
            methodPat  = Pattern.compile(".*" + args[2] + ".*", Pattern.CASE_INSENSITIVE);
        } else if (args.length == 2) {
            packagePat  = Pattern.compile(".*" + args[1] + ".*", Pattern.CASE_INSENSITIVE);
        } else if (args.length != 1) {
            usage();
            return;
        }

        Class<?> c = null; // TODO: rename to _class
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            c = Class.forName(className);
        } catch (ClassNotFoundException e) {

            String packageName = className;
            SearchResult result = getClassNamesFromPackage(packageName);

            if (result.matchCount == 1) {
                c = Class.forName(result.lastMatch);
            } else {
                if (result.matchCount == 0) {
                    out.print("not found: " + className);
                    out.println(" (check your classpath or your spelling)");
                }
                return;
            }
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
