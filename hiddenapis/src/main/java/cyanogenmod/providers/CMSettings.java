package cyanogenmod.providers;

import android.content.ContentResolver;
import android.provider.Settings;

public class CMSettings {
    public static final String AUTHORITY = "cmsettings";
    public static final String ACTION_DATA_USAGE = "cyanogenmod.settings.ACTION_DATA_USAGE";

    public static final class Secure extends Settings.NameValueTable {

        public static String getString(ContentResolver resolver, String name) {
            return Settings.System.getString(resolver, name);
        }

        public static final String DEV_FORCE_SHOW_NAVBAR = "dev_force_show_navbar";
    }
}
