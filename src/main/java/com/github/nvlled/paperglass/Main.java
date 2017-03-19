package com.github.nvlled.paperglass;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.jar.*;
import com.beust.jcommander.*;

public class Main {
    public static final int MATCH_LIMIT = 2048;

    // This is actually a good chance to try kotlin.... (or maybe later)
    interface Strfn<T> { String apply(T x); }

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

    static <T> String join(T[] ts, CharSequence sep, Strfn<T> fn) {
        String[] strs = new String[ts.length];
        for (int i = 0; i < strs.length; i++)
            strs[i] = fn.apply(ts[i]);
        return String.join(sep, strs);
    }

    static String joinTypes(Type[] params) {
        String str = join(params, ", ", new Strfn<Type>() {
            public String apply(Type t) { return ClassUtil.removePackageName(t.getTypeName()); }
        });
        return str;
    }

    static String toTypeParamString(Type[] params) {
        String str = joinTypes(params);
        if (str.length() > 0)
            return "<"+str+">";
        return "";
    }

    public static String joinPath(String path1, String path2) {
        int len = path1.length();
        if (len == 0)
            return path2;
        if (path1.charAt(len-1) != '/')
            path1 = path1 + "/";
        return path1 + path2;
    }

    public static List<String> findStdClass(String packageName) {
        String sub = packageName.replace(".", "/");
        Path basePath = Paths.get(getStdDir(), sub);
        List<String> matches = new LimitedList<String>(MATCH_LIMIT);

        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                    List<String> lines = new ArrayList<String>();
                    try {
                        lines = Files.readAllLines(p, java.nio.charset.Charset.defaultCharset());
                    } catch (IOException e) { }

                    String prefix = packageName + p.getParent()
                                        .toString()
                                        .replace(basePath+"", "").replace("/", ".");

                    for (String line : lines) {
                        String className = prefix+"."+line;
                        if (packagePat.matcher(className).matches()) {
                            matches.add(className);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("tree walk failed: " + e);
        }
        return matches;
    }

    public static List<String> findLoadableClasses(String classDir, String sub) {
        File dirFile = new File(classDir + "/" + sub);
        List<String> matches = new LimitedList<String>(MATCH_LIMIT);

        if (dirFile.isDirectory()) {
            for (String filename: dirFile.list()) {
                String className = joinPath(sub, filename).replace("/", ".");
                className = className.replace(".class", "");
                if (ClassUtil.isLoadable(className)) {
                    if (packagePat.matcher(className).matches()) {
                        matches.add(className);
                    }
                } else {
                    List<String> subMatches = findLoadableClasses(classDir, joinPath(sub, filename));
                    matches.addAll(subMatches);
                }
            }
        }
        return matches;
    }

    public static List<String> getClassNamesFromPackage(String packageName) throws IOException{
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;

        String packageDir = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageDir);

        if (packageURL == null)
            return new ArrayList<String>();

        if (packageURL.getProtocol() == "file") {
            String path = packageURL.getPath();
            String classDir = path.replace(packageDir, "");
            return findLoadableClasses(classDir, packageDir);
        } 

        List<String> matches = new LimitedList<String>(MATCH_LIMIT);
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
                        if (ClassUtil.isLoadable(className) && packagePat.matcher(className).matches()) {
                            matches.add(className);
                        }
                    }
                }
            }
        }

        return matches;
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

    static String getStdDir() {
        String distDir = ".";
        try {
            distDir =
                new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                .getParent();
        } catch (URISyntaxException e) { }

        return distDir + "/std";
    }

    static class Options {
        @com.beust.jcommander.Parameter
        List<String> parameters = new ArrayList<String>();

        @com.beust.jcommander.Parameter(names="-std", description="source zip file of jdk")
        String jdkSource = "";

        @com.beust.jcommander.Parameter(names={"-i", "-index"}, description="index from matches")
        int matchIndex = -1;

        @com.beust.jcommander.Parameter(names={"-k", "-packages"}, description="show only packages")
        boolean showOnlyPackages = false;

        @com.beust.jcommander.Parameter(names={"-f", "-full"}, description="Show full package names")
        boolean showFullName = false;
    }

    static void tryBuildIndex(String stdFilename) {
        SourceReader r = null;
        try {
            r = new SourceReader(stdFilename);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return;
        }

        Indexer indexer = new Indexer(getStdDir());
        if (indexer.indexExists())
            return;

        indexer.build(r);
    }

    public static void main(String[] args) throws Exception {
        boolean showProtected;

        Options opts = new Options();
        try {
            new com.beust.jcommander.JCommander(opts, args);
        } catch (ParameterException e) {
            System.err.println("**" + e);
            return;
        }

        if (opts.jdkSource.length() > 0)
            tryBuildIndex(opts.jdkSource);

        args = opts.parameters.toArray(new String[]{});

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
            List<String> matches = getClassNamesFromPackage(packageName);

            if (matches.size() == 0)
                matches = findStdClass(packageName);

            int index = opts.matchIndex;

            if (matches.size() == 1)
                index = 0;

            boolean validIndex = index >= 0 && index < matches.size();

            if (validIndex) {
                c = Class.forName(matches.get(index));
            } else {
                if (matches.size() == 0) {
                    out.print("not found: " + className);
                    out.println(" (check your classpath or your spelling)");
                } else {
                    int i = 0;
                    for (String m: matches) {
                        System.out.printf("%4d] %s\n", i, m);
                        i++;
                    }
                }
                return;
            }
        }

        out.println(ClassUtil.getClassType(c) + " " +
                c.getSimpleName() + 
                toTypeParamString(c.getTypeParameters()));

        if (c.getGenericSuperclass() != null)
            out.println("  extends " +
                    ClassUtil.removePackageName(c.getGenericSuperclass().toString()));

        Type[] interfaces = c.getGenericInterfaces();
        if (interfaces.length > 0) {
            out.print("  implements " + joinTypes(interfaces));
        }
        out.println("\n");

        String indent = "   ";

        out.println("constructors: ");
        if (c.getConstructors().length == 0)
            out.println(indent + "(none)");

        for (Constructor t: c.getConstructors()) {
            String params = join(t.getParameterTypes(), ", ", new Strfn<Class>() {
                public String apply(Class c) {
                    return c.getSimpleName();
                }
            });
            out.print(indent + c.getSimpleName() + "("+params+")");
            out.println(indent + ClassUtil.printModifiers(t));
        }
        out.println();

        out.println("methods: ");
        if (c.getMethods().length == 0)
            out.println(indent + "(none)");

        for (Method t: c.getMethods()) {
            if ( ! methodPat.matcher(t.getName()).matches())
                continue;

            Type ret = t.getGenericReturnType();
            out.print(indent);
            out.print(t.getName() + "(" + joinTypes(t.getGenericParameterTypes()) + ")");
            out.print(": " + ClassUtil.removePackageName(ret.getTypeName()));

            String modStr = ClassUtil.printModifiers(t);
            if (modStr.length() > 0)
                out.print(" | " + modStr);
            out.println();
        }
    }
}
