/*
 * $Id: Properties.java 554 2014-02-16 19:23:03Z vollbsve $
 */
package unihost.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class Properties {

  private static ResourceBundle bundle;

  public static Boolean getBoolean(String key) {
    return get(key, (Boolean) null);
  }

  public static Boolean get(String key, Boolean defaultValue) {
    String value = get(key, defaultValue != null ? defaultValue.toString() : null);
    return value != null ? Boolean.valueOf(value) : null;
  }

  public static Integer getInteger(String key) {
    return get(key, (Integer) null);
  }

  public static Integer get(String key, Integer defaultValue) {
    String value = get(key, defaultValue != null ? defaultValue.toString() : null);
    return value != null ? Integer.valueOf(value) : null;
  }

  public static String get(String key) {
    return get(key, (String) null);
  }

  public static String get(String key, String defaultValue) {
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(System.getProperty("unihost.resource.bundle", "unihost"));
    }
    if (bundle.containsKey(key)) {
      return bundle.getString(key);
    } else {
      return defaultValue;
    }
  }

  public static List<String> getList(String key) {
    return Arrays.asList(get(key, "").split("\\s*,\\s*"));
  }

  public static Map<String, String> getAll() {
    return getAll(null);
  }

  public static Map<String, String> getAll(String regex) {
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(System.getProperty("unihost.resource.bundle", "unihost"));
    }
    Map<String, String> map = new HashMap<>();
    Enumeration<String> keys = bundle.getKeys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      if (regex == null || key.matches(regex)) {
        map.put(key, bundle.getString(key));
      }
    }
    return map;
  }

  public static java.util.Properties asProperties() {
    return asProperties(null);
  }

  public static java.util.Properties asProperties(String regex) {
    java.util.Properties properties = new java.util.Properties();

    for (String key : getAll(regex).keySet()) {
      properties.put(key, get(key));
    }

    return properties;
  }
}
