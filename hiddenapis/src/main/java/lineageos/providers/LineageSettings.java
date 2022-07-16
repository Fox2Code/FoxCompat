package lineageos.providers;

import android.content.ContentResolver;
import android.provider.Settings;

public class LineageSettings {
    public static final String AUTHORITY = "lineagesettings";
    public static final String ACTION_DATA_USAGE = "lineageos.settings.ACTION_DATA_USAGE";

    public static final class System extends Settings.NameValueTable {

        public static String getString(ContentResolver resolver, String name) {
            return Settings.System.getString(resolver, name);
        }

        public static final String BERRY_BLACK_THEME = "berry_black_theme";

        public static final String FORCE_SHOW_NAVBAR = "force_show_navbar";
    }
}
