package android.os;

public class SystemProperties {
    public static String get(String key) {
        return System.getProperty(key);
    }

    public static String get(String key, String def) {
        return System.getProperty(key, def);
    }

    public static int getInt(String key, int def) {
        try {
            String value = get(key);
            if (value.isEmpty()) return def;
            return Integer.parseInt(value);
        } catch (Exception e) {
            return def;
        }
    }
}
