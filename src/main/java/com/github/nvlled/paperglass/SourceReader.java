package com.github.nvlled.paperglass;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class SourceReader implements Iterable<String> {
    private String zipFilename;

    public SourceReader(String zipFilename) throws IOException, ZipException {
        this.zipFilename = zipFilename;
        new ZipFile(zipFilename);
    }

    public Iterator<String> iterator() {
        return new SourceFileIterator();
    }

    class SourceFileIterator implements Iterator<String> {
        ZipFile zipFile;
        Enumeration<? extends ZipEntry> enumeration;
        public SourceFileIterator() {
            try {
                zipFile = new ZipFile(new File(zipFilename));
                enumeration = zipFile.entries();
            } catch (IOException e) { }
        }

        public boolean hasNext() {
            if (enumeration == null)
                return false;

            boolean more = enumeration.hasMoreElements();
            if (!more) {
                try {
                    enumeration = null;
                    zipFile.close();
                } catch (IOException e) { };
            }
            return more;
        }

        public String next() {
            if (enumeration == null)
                return "";

            while (hasNext()) {
                ZipEntry entry = enumeration.nextElement();
                String name = entry.getName().replace("/", ".").replace(".java", "");
                if (ClassUtil.isLoadable(name))
                    return name;
            }
            return "";
        }

        public void remove() {/* do nothing */}
    }
}


