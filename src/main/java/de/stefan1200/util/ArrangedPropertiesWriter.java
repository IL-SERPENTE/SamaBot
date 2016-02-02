/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ArrangedPropertiesWriter {
    private final String SEPARATOR = "***";
    private final String LINE_SEPARATOR = System.getProperty("line.separator");
    private HashMap<String, String> hmHelp = new HashMap<>();
    private HashMap<String, String> hmValue = new HashMap<>();
    private HashMap<String, Boolean> hmSave = new HashMap<>();
    private Vector<String> vKeys = new Vector<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public boolean addKey(String key, String helpText) {
        return this.addKey(key, helpText, null, true);
    }

    public boolean addKey(String key, String helpText, String defaultValue) {
        return this.addKey(key, helpText, defaultValue, true);
    }

    public boolean addKey(String key, String helpText, boolean saveToFile) {
        return this.addKey(key, helpText, null, saveToFile);
    }

    public boolean addKey(String key, String helpText, String defaultValue, boolean saveToFile) {
        if (!key.equals(SEPARATOR) && key.length() > 0 && this.vKeys.indexOf(key) == -1) {
            this.vKeys.addElement(key);
            this.hmValue.put(key, defaultValue);
            this.hmHelp.put(key, helpText);
            this.hmSave.put(key, saveToFile);
            return true;
        }
        return false;
    }

    public boolean insertKey(String key, int pos, String helpText) {
        if (!key.equals(SEPARATOR) && key.length() > 0 && this.vKeys.indexOf(key) == -1) {
            this.vKeys.insertElementAt(key, pos);
            this.hmHelp.put(key, helpText);
            return true;
        }
        return false;
    }

    public boolean canSaveToFile(String key) {
        return this.hmSave.get(key);
    }

    public Vector<String> getKeys() {
        Vector<String> retKeys = new Vector<String>();
        retKeys.addAll(this.vKeys);
        while (retKeys.removeElement(SEPARATOR)) {
        }
        return retKeys;
    }

    public String getValue(String key) {
        return this.hmValue.get(key);
    }

    public String getValue(String key, String defValue) {
        if (this.hmValue.get(key) == null) {
            return defValue;
        }
        return this.hmValue.get(key);
    }

    public boolean setValue(String key, String value) {
        if (!key.equals(SEPARATOR) && key.length() > 0 && this.vKeys.indexOf(key) != -1) {
            this.hmValue.put(key, value);
            return true;
        }
        return false;
    }

    public boolean setValue(String key, long value) {
        return this.setValue(key, Long.toString(value));
    }

    public boolean setValue(String key, double value) {
        return this.setValue(key, Double.toString(value));
    }

    public boolean setValue(String key, boolean value) {
        return this.setValue(key, Boolean.toString(value));
    }

    public void removeAllValues() {
        this.hmValue.clear();
    }

    public void addSeparator() {
        this.vKeys.addElement(SEPARATOR);
    }

    public void insertSeparator(int pos) {
        this.vKeys.insertElementAt(SEPARATOR, pos);
    }

    public void removeAllSeparators() {
        while (this.vKeys.removeElement(SEPARATOR)) {
        }
    }

    public int getKeyCount() {
        return this.vKeys.size();
    }

    public String getHelpText(String key) {
        return this.hmHelp.get(key);
    }

    public boolean removeKey(String key) {
        if (!key.equals(SEPARATOR) && key.length() > 0 && this.vKeys.indexOf(key) != -1) {
            this.vKeys.removeElement(key);
            this.hmHelp.remove(key);
            this.hmValue.remove(key);
            this.hmSave.remove(key);
            return true;
        }
        return false;
    }

    public boolean loadValues(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(file));
            for (String key : this.vKeys) {
                String temp = prop.getProperty(key);
                if (temp == null) continue;
                this.hmValue.put(key, temp);
            }
        }
        catch (Exception e) {
            prop = null;
            return false;
        }
        prop = null;
        return true;
    }

    public boolean loadValues(String filename) {
        if (filename == null) {
            return false;
        }
        return this.loadValues(new File(filename));
    }

    public boolean save(String filename, String header) {
        PrintStream ps;
        if (filename == null) {
            return false;
        }
        try {
            ps = new PrintStream(filename, "ISO-8859-1");
        }
        catch (Exception e) {
            return false;
        }
        if (header != null && header.length() > 0) {
            ps.println(this.convertString(header));
        }
        ps.println("# File created at " + this.sdf.format(new Date(System.currentTimeMillis())));
        ps.println();
        for (String key : this.vKeys) {
            if (key.equals(SEPARATOR)) {
                ps.println();
                continue;
            }
            if (!this.hmSave.get(key).booleanValue()) continue;
            if (this.hmHelp.get(key) != null) {
                ps.println(this.convertString(this.hmHelp.get(key)));
            }
            ps.print(key);
            ps.print(" = ");
            ps.println(this.hmValue.get(key) == null ? "" : this.hmValue.get(key));
        }
        ps.close();
        return true;
    }

    private String convertString(String text) {
        String retValue = "# " + text;
        retValue = retValue.replace("\\", "$[mkbackslashsave]");
        retValue = retValue.replace("\n", String.valueOf(this.LINE_SEPARATOR) + "# ");
        retValue = retValue.replace("$[mkbackslashsave]", "\\");
        return retValue;
    }
}

