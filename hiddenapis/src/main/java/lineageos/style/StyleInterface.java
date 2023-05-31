package lineageos.style;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;

@SuppressWarnings("unused")
public class StyleInterface {
    public static final int STYLE_GLOBAL_AUTO_WALLPAPER = 0;
    public static final int STYLE_GLOBAL_AUTO_DAYTIME = 1;
    public static final int STYLE_GLOBAL_LIGHT = 2;
    public static final int STYLE_GLOBAL_DARK = 3;
    public static final String ACCENT_DEFAULT = "lineageos";
    public static final String OVERLAY_DARK_DEFAULT = "org.lineageos.overlay.dark";
    public static final String OVERLAY_DARK_BLACK = "org.lineageos.overlay.black";

    @SuppressLint("StaticFieldLeak")
    private static StyleInterface sInstance;
    private final Context mContext;

    private StyleInterface(Context context) {
        this.mContext = context;
    }

    public static StyleInterface getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new StyleInterface(context);
        }
        return sInstance;
    }

    public boolean isDarkNow() {
        return (this.mContext.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public String getDarkOverlay() {
        return OVERLAY_DARK_DEFAULT;
    }
}
