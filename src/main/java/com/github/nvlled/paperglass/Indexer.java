package com.github.nvlled.paperglass;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Indexer {
    public static final String INDEX_FILE = "index.txt";
    public static final String INDEX_DONE = "_done";

    private String destDir;

    public Indexer() {
        this(".");
    }

    public Indexer(String destDir) {
        this.destDir = destDir;
    }

    public boolean indexExists() {
        return new File(destDir+"/"+INDEX_DONE).exists();
    }

    public void build(SourceReader reader) {
        System.out.print("Building JDK index, this may take a while");

        PrintStream indexStream = null;
        String currentDir = "";

        for (String className: reader) {

            String filename = className.replace(".", "/");
            File file = new File(destDir + "/" + filename);
            File dir = file.getParentFile();

            //System.out.println("filename: " + filename);
            //System.out.println("dir: " + dir.getPath());

            if (! dir.getPath().equals(currentDir)) {
                System.out.print(".");
                //System.out.println("starting new index..");
                if (indexStream != null) {
                    indexStream.flush();
                    indexStream.close(); 
                }

                dir.mkdirs();
                currentDir = dir.getPath();
                //System.out.println("currentDir: " + currentDir);

                try {
                    indexStream = new PrintStream(
                            new FileOutputStream(currentDir+"/"+INDEX_FILE, true));
                } catch (FileNotFoundException e) { 
                    System.err.println(e+"");
                    break;
                }
            }

            indexStream.println(file.getName());
        }

        // write index file
        try {
            OutputStream out = new FileOutputStream(new File(destDir+"/"+INDEX_DONE));
            out.write(0xbeefbabe);
            out.close();
        } catch (IOException e) {/* meh */}

        System.out.println("done");
    }
}
