package com.fox2code.foxcompat.os;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.SystemProperties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fox2code.foxcompat.app.internal.FoxCompat;

import org.jetbrains.annotations.NotNull;

import cyanogenmod.providers.CMSettings;
import lineageos.app.LineageContextConstants;
import lineageos.providers.LineageSettings;
import lineageos.style.StyleInterface;

/**
 * This class is contain the LineageOS & CyanogenMod compatibility.
 * It use subclass to avoid class loading issues.
 */
public final class FoxLineage {
    private static FoxLineage sFoxLineage;
    private final Settings mSettings;
    private final Styles mStyles;

    private FoxLineage(Context context) {
        boolean lineageSettings = false;
        if (context.getPackageManager().hasSystemFeature(
                LineageContextConstants.Features.SETTINGS)
                && FoxCompat.lineageOsSettings) {
            mSettings = new Settings(context.getContentResolver());
            lineageSettings = true;
        } else if (FoxCompat.cyanogenModSettings) {
            mSettings = new SettingsCyanogenMod(
                    context.getContentResolver());
        } else mSettings = null;
        if (context.getPackageManager().hasSystemFeature(
                LineageContextConstants.Features.STYLES)
                && FoxCompat.lineageOsStyles) {
            mStyles = new ServiceStyles(StyleInterface.getInstance(context));
        } else if (lineageSettings && FoxCompat.lineageOsSettingsBB) {
            mStyles = new SettingsStyles(context.getContentResolver());
        } else mStyles = null;
    }

    public boolean isForceNavBar() {
        return mSettings != null && mSettings.isForceNavBar();
    }

    public boolean isBlackMode() {
        return mStyles != null && mStyles.isBlackMode();
    }

    public boolean hasLineageStyles() {
        return mStyles != null;
    }

    public boolean isLineageOS() {
        return (!(mSettings instanceof SettingsCyanogenMod)) &&
                mStyles != null || mSettings != null;
    }

    public boolean isCyanogenMod() {
        return mSettings instanceof SettingsCyanogenMod;
    }

    @Nullable
    public String getDataUsageIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return android.provider.Settings.ACTION_DATA_USAGE_SETTINGS;
        } else if (mSettings != null) {
            return mSettings.getDataUsageIntent();
        } else return null;
    }

    public void fixConfiguration(Configuration configuration) {
        if (mStyles != null && mStyles.isDarkNow()) {
            int uiMode = configuration.uiMode;
            uiMode &= ~Configuration.UI_MODE_NIGHT_MASK;
            uiMode |= Configuration.UI_MODE_NIGHT_YES;
            configuration.uiMode = uiMode;
        }
    }

    /**
     * @return Current rom (Aka. Android system) type
     *
     * Note: RomType is not Rom name, Most custom roms might appear
     * as {@link RomType#CYANOGENMOD} or {@link RomType#LINEAGEOS}
     * */
    public RomType getRomType() {
        if (mSettings != null) {
            return mSettings instanceof SettingsCyanogenMod ?
                    RomType.CYANOGENMOD : RomType.LINEAGEOS;
        } else if (mStyles != null) {
            return RomType.LINEAGEOS;
        } else if (FoxCompat.isAndroidSDK()) {
            return RomType.SDK;
        } else if (FoxCompat.isOxygenOS()) {
            return RomType.OXYGENOS;
        } else if (FoxCompat.samsungStatusBarManager) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ?
                    RomType.ONEUI : RomType.SAMSUNG_EXPERIENCE;
        } else {
            return RomType.UNKNOWN;
        }
    }

    @NonNull
    public String getProperty(@NonNull String key) {
        return SystemProperties.get(key, "");
    }

    @NotNull
    public static FoxLineage getFoxLineage(@NonNull Context context) {
        if (sFoxLineage != null) return sFoxLineage;
        Context localContext = context.getApplicationContext();
        if (localContext == null) localContext = context;
        return sFoxLineage = new FoxLineage(localContext);
    }

    public enum RomType {
        UNKNOWN("Android"),
        SDK("Android SDK"),
        LINEAGEOS("LineageOS"),
        CYANOGENMOD("CyanogenMod"),
        OXYGENOS("OxygenOS"),
        SAMSUNG_EXPERIENCE("SamsungExperience"),
        ONEUI("OneUI");

        public final String name;

        RomType(String name) {
            this.name = name;
        }
    }

    private static abstract class Styles {
        abstract boolean isBlackMode();

        boolean isDarkNow() {
            return false;
        }
    }

    private static class ServiceStyles extends Styles {
         final StyleInterface styleInterface;

        private ServiceStyles(StyleInterface styleInterface) {
            this.styleInterface = styleInterface;
        }

        @Override
        boolean isBlackMode() {
            return StyleInterface.OVERLAY_DARK_BLACK.equals(
                    this.styleInterface.getDarkOverlay());
        }

        @Override
        boolean isDarkNow() {
            return this.styleInterface.isDarkNow();
        }
    }

    private static class SettingsStyles extends Styles {
        final ContentResolver contentResolver;

        SettingsStyles(ContentResolver contentResolver) {
            this.contentResolver = contentResolver;
        }

        @Override
        boolean isBlackMode() {
            return "1".equals(LineageSettings.System.getString(
                    this.contentResolver, LineageSettings.System.BERRY_BLACK_THEME));
        }
    }

    private static class Settings {
        final ContentResolver contentResolver;

        Settings(ContentResolver contentResolver) {
            this.contentResolver = contentResolver;
        }

        boolean isForceNavBar() {
            return Boolean.parseBoolean(LineageSettings.System.getString(
                    this.contentResolver, LineageSettings.System.FORCE_SHOW_NAVBAR));
        }

        String getDataUsageIntent() {
            return LineageSettings.ACTION_DATA_USAGE;
        }
    }

    private static class SettingsCyanogenMod extends FoxLineage.Settings {
        SettingsCyanogenMod(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        boolean isForceNavBar() {
            return Boolean.parseBoolean(CMSettings.Secure.getString(
                    this.contentResolver, CMSettings.Secure.DEV_FORCE_SHOW_NAVBAR));
        }

        @Override
        String getDataUsageIntent() {
            return CMSettings.ACTION_DATA_USAGE;
        }
    }
}
