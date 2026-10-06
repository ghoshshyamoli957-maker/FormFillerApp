package com.formfiller.app;

import android.util.Xml;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.zip.*;
import org.xmlpull.v1.XmlPullParser;

public class XlsxReader {

    public static List<String[]> read(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        byte[] b = bo.toByteArray();
        List<String[]> rows;
        if (b.length > 3 && b[0] == 'P' && b[1] == 'K') rows = readXlsx(b);
        else rows = readCsv(new String(b, "UTF-8"));
        List<String[]> out = new ArrayList<>();
        for (String[] r : rows) {
            boolean empty = true;
            for (String c : r) if (c != null && c.trim().length() > 0) empty = false;
            if (!empty) out.add(r);
        }
        return out;
    }

    private static List<String[]> readXlsx(byte[] b) throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(b));
        ZipEntry e;
        while ((e = z.getNextEntry()) != null) {
            String nm = e.getName();
            if (nm.equals("xl/sharedStrings.xml") || nm.startsWith("xl/worksheets/sheet")) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = z.read(buf)) > 0) bo.write(buf, 0, n);
                files.put(nm, bo.toByteArray());
            }
        }
        List<String> shared = new ArrayList<>();
        if (files.containsKey("xl/sharedStrings.xml")) {
            XmlPullParser p = Xml.newPullParser();
            p.setInput(new ByteArrayInputStream(files.get("xl/sharedStrings.xml")), null);
            StringBuilder sb = new StringBuilder();
            boolean inT = false;
            int ev = p.getEventType();
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    if (p.getName().equals("si")) sb.setLength(0);
                    else if (p.getName().equals("t")) inT = true;
                } else if (ev == XmlPullParser.TEXT) {
                    if (inT) sb.append(p.getText());
                } else if (ev == XmlPullParser.END_TAG) {
                    if (p.getName().equals("t")) inT = false;
                    else if (p.getName().equals("si")) shared.add(sb.toString());
                }
                ev = p.next();
            }
        }
        String sheet = "xl/worksheets/sheet1.xml";
        if (!files.containsKey(sheet)) {
            for (String k : files.keySet()) if (k.endsWith(".xml") && !k.contains("shared")) { sheet = k; break; }
        }
        byte[] sd = files.get(sheet);
        if (sd == null) return new ArrayList<>();
        List<String[]> rows = new ArrayList<>();
        XmlPullParser p = Xml.newPullParser();
        p.setInput(new ByteArrayInputStream(sd), null);
        List<String> cur = null;
        String type = null;
        int col = 0;
        StringBuilder val = new StringBuilder();
        boolean inV = false, inT = false;
        int ev = p.getEventType();
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                String nm = p.getName();
                if (nm.equals("row")) cur = new ArrayList<>();
                else if (nm.equals("c")) {
                    type = p.getAttributeValue(null, "t");
                    String ref = p.getAttributeValue(null, "r");
                    col = 0;
                    if (ref != null) {
                        for (int i = 0; i < ref.length(); i++) {
                            char ch = ref.charAt(i);
                            if (ch >= 'A' && ch <= 'Z') col = col * 26 + (ch - 'A' + 1);
                            else break;
                        }
                        col = col - 1;
                    }
                    val.setLength(0);
                } else if (nm.equals("v")) inV = true;
                else if (nm.equals("t")) inT = true;
            } else if (ev == XmlPullParser.TEXT) {
                if (inV || inT) val.append(p.getText());
            } else if (ev == XmlPullParser.END_TAG) {
                String nm = p.getName();
                if (nm.equals("v")) inV = false;
                else if (nm.equals("t")) inT = false;
                else if (nm.equals("c") && cur != null) {
                    String v = val.toString();
                    if ("s".equals(type)) {
                        try { v = shared.get(Integer.parseInt(v.trim())); } catch (Exception x) { }
                    } else if (type == null || "n".equals(type)) {
                        try { v = new BigDecimal(v.trim()).stripTrailingZeros().toPlainString(); } catch (Exception x) { }
                    }
                    while (cur.size() < col) cur.add("");
                    cur.add(v);
                } else if (nm.equals("row") && cur != null) {
                    rows.add(cur.toArray(new String[0]));
                    cur = null;
                }
            }
            ev = p.next();
        }
        return rows;
    }

    private static List<String[]> readCsv(String s) {
        if (s.startsWith("\uFEFF")) s = s.substring(1);
        List<String[]> rows = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (q) {
                if (c == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') { f.append('"'); i++; }
                    else q = false;
                } else f.append(c);
            } else if (c == '"') q = true;
            else if (c == ',') { cur.add(f.toString()); f.setLength(0); }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') i++;
                cur.add(f.toString()); f.setLength(0);
                rows.add(cur.toArray(new String[0]));
                cur = new ArrayList<>();
            } else f.append(c);
        }
        if (f.length() > 0 || !cur.isEmpty()) {
            cur.add(f.toString());
            rows.add(cur.toArray(new String[0]));
        }
        return rows;
    }
          }
