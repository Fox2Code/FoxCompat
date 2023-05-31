package com.fox2code.foxcompat.app.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.HardwareRenderer;
import android.os.Build;
import android.os.SystemProperties;
import android.util.TypedValue;
import android.view.ThreadedRenderer;
import android.view.View;
import android.widget.EdgeEffect;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.TintTypedArray;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.fox2code.foxcompat.R;
import com.fox2code.foxcompat.os.FoxLineage;
import com.google.android.material.color.MaterialColors;
import com.kieronquinn.monetcompat.core.MonetCompat;
import com.kieronquinn.monetcompat.extensions.views.ViewExtensions_RecyclerViewKt;
import com.kieronquinn.monetcompat.extensions.views.ViewExtensions_ScrollViewKt;
import com.kieronquinn.monetcompat.widget.StretchEdgeEffect;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import me.weishu.reflection.Reflection;
import rikka.core.res.ResourcesCompatLayoutInflaterListener;
import rikka.layoutinflater.view.LayoutInflaterFactory;

/**
 * List all libraries compatible with FoxCompat
 */
@SuppressLint("DiscouragedPrivateApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class FoxCompat {
    public static final boolean samsungStatusBarManager;
    public static final boolean cyanogenModSettings;
    public static final boolean lineageOsSettingsBB;
    public static final boolean lineageOsSettings;
    public static final boolean lineageOsStyles;
    public static final boolean googleMaterial;
    public static final boolean rikkaXCore;
    public static final boolean monetCompat;
    public static final boolean cardView;
    public static final LayoutInflaterFactory.OnViewCreatedListener
            TOOLBAR_ALIGNMENT_FIX = (view, parent, name, context, attrs) -> {
        if (view instanceof Toolbar) {
            int insetStartWithNavigation = context.getResources().getDimensionPixelSize(
                    androidx.appcompat.R.dimen.abc_action_bar_content_inset_with_nav);
            ((Toolbar) view).setContentInsetStartWithNavigation(insetStartWithNavigation);
        }
    };
    @SuppressLint("RestrictedApi")
    public static final LayoutInflaterFactory.OnViewCreatedListener
            CARD_VIEW_COLOR_FIX = (view, parent, name, context, attrs) -> {
        if (view instanceof CardView) {
            TintTypedArray a = TintTypedArray.obtainStyledAttributes(
                    context, attrs, FoxCompatR.styleable.CardView);
            if (a.hasValue(FoxCompatR.styleable.CardView_cardBackgroundColor)) {
                ColorStateList colorStateList = a.getColorStateList(
                        FoxCompatR.styleable.CardView_cardBackgroundColor);
                if (colorStateList != null)
                    ((CardView) view).setCardBackgroundColor(colorStateList);
            }
            a.recycle();
        }
    };
    public static final LayoutInflaterFactory.OnViewCreatedListener
            SWITCH_COMPAT_CRASH_FIX = (view, parent, name, context, attrs) -> {
        // Fix a crash where a null CharSequence is passed to "android.text.StaticLayout"
        if (view instanceof SwitchCompat switchCompat) {
            if (switchCompat.getTextOn() == null) {
                switchCompat.setTextOn("");
            }
            if (switchCompat.getTextOff() == null) {
                switchCompat.setTextOff("");
            }
        }
    };
    private static boolean hiddenApiBypass;
    private static boolean freeReflection;
    private static boolean hiddenApis;
    // LayoutInflaterFactory.OnViewCreatedListener factory
    private static Field mEdgeGlowTop, mEdgeGlowBottom;
    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("SoonBlockedPrivateApi")
    public static final LayoutInflaterFactory.OnViewCreatedListener
            STRETCH_OVERSCROLL = (view, parent, name, context, attrs) -> {
        if (view instanceof RecyclerView) {
            ViewExtensions_RecyclerViewKt.enableStretchOverscroll(
                    (RecyclerView) view, null);
        } else if (view instanceof NestedScrollView) {
            ViewExtensions_ScrollViewKt.enableStretchOverscroll(
                    (NestedScrollView) view, null);
        } else if (checkReflection(context, Build.VERSION_CODES.P)) {
            if (view instanceof ScrollView) {
                if (mEdgeGlowBottom == null) {
                    try {
                        mEdgeGlowTop = ScrollView.class.getDeclaredField("mEdgeGlowTop");
                        mEdgeGlowTop.setAccessible(true);
                        mEdgeGlowBottom = ScrollView.class.getDeclaredField("mEdgeGlowBottom");
                        mEdgeGlowBottom.setAccessible(true);
                    } catch (Exception e) {
                        return;
                    }
                }
                // Top need bottom direction and bottom need top direction.
                setEdgeEffect(view, mEdgeGlowTop, new StretchEdgeEffect(
                        context, view, StretchEdgeEffect.Direction.BOTTOM));
                setEdgeEffect(view, mEdgeGlowBottom, new StretchEdgeEffect(
                        context, view, StretchEdgeEffect.Direction.TOP));
            } else if (view instanceof HorizontalScrollView) {
                view.setOverScrollMode(View.OVER_SCROLL_NEVER);
            }
        }
    };
    @SuppressWarnings("ALL")
    private static Boolean dynamicAccent = (Boolean)
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                    (Object) FoxCompat.googleMaterial :
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                            (Object) !(isAndroidSDK() || isOxygenOS()) : (Object) null));

    static {
        boolean samsungStatusBarManagerTmp, cyanogenModSettingsTmp,
                lineageOsSettingsTmp, lineageOsSettingsBBTmp, lineageOsStylesTmp,
                hiddenApiBypassTmp, freeReflectionTmp, rikkaXCoreTmp,
                googleMaterialTmp, monetCompatTmp, cardViewTmp;
        try {
            Class.forName("android.app.SemStatusBarManager");
            samsungStatusBarManagerTmp = true;
        } catch (Throwable throwable) {
            samsungStatusBarManagerTmp = false;
        }
        try {
            Class.forName("cyanogenmod.providers.CMSettings");
            cyanogenModSettingsTmp = true;
        } catch (Throwable throwable) {
            cyanogenModSettingsTmp = false;
        }
        try {
            Class<?> cls = Class.forName("lineageos.providers.LineageSettings$System");
            lineageOsSettingsTmp = true;
            try {
                cls.getDeclaredField("BERRY_BLACK_THEME");
                lineageOsSettingsBBTmp = true;
            } catch (Throwable throwable) {
                lineageOsSettingsBBTmp = false;
            }
        } catch (Throwable throwable) {
            lineageOsSettingsTmp = false;
            lineageOsSettingsBBTmp = false;
        }
        try {
            Class.forName("lineageos.style.StyleInterface");
            lineageOsStylesTmp = true;
        } catch (Throwable throwable) {
            lineageOsStylesTmp = false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.getDeclaredMethod(ThreadedRenderer.class,
                        "setContextPriority", int.class);
                hiddenApiBypassTmp = true;
            } catch (Throwable ignored) {
                hiddenApiBypassTmp = false;
            }
        } else hiddenApiBypassTmp = false;
        try {
            Reflection.class.getDeclaredMethod(
                    "unseal", Context.class);
            freeReflectionTmp = true;
        } catch (Throwable throwable) {
            freeReflectionTmp = false;
        }
        try {
            rikkaXCoreTmp = ResourcesCompatLayoutInflaterListener.getInstance() != null;
        } catch (Throwable ignored) {
            rikkaXCoreTmp = false;
        }
        try {
            googleMaterialTmp =
                    MaterialColors.isColorLight(Color.WHITE);
        } catch (Throwable ignored) {
            googleMaterialTmp = false;
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            MonetCompat.getUseSystemColorsOnAndroid12();
            monetCompatTmp = true;
        } catch (Throwable ignored) {
            monetCompatTmp = false;
        }
        try {
            Class.forName("androidx.cardview.widget.CardView");
            cardViewTmp = true;
        } catch (Throwable ignored) {
            cardViewTmp = false;
        }
        samsungStatusBarManager = samsungStatusBarManagerTmp;
        cyanogenModSettings = cyanogenModSettingsTmp;
        lineageOsSettingsBB = lineageOsSettingsBBTmp;
        lineageOsSettings = lineageOsSettingsTmp;
        lineageOsStyles = lineageOsStylesTmp;
        hiddenApiBypass = hiddenApiBypassTmp;
        freeReflection = freeReflectionTmp;
        rikkaXCore = rikkaXCoreTmp;
        googleMaterial = googleMaterialTmp;
        monetCompat = monetCompatTmp;
        cardView = cardViewTmp;
        hiddenApis = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || hasHiddenAPI0()) {
            hiddenApis = true;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try { // Try unlocking with meta reflection.
                Method forName = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethod = Class.class.getDeclaredMethod(
                        "getDeclaredMethod", String.class, Class[].class);

                Class<?> vmRuntimeClass = (Class<?>) forName.invoke(
                        null, "dalvik.system.VMRuntime");
                Method getRuntime = (Method) getDeclaredMethod.invoke(
                        vmRuntimeClass, "getRuntime", null);
                Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(
                        vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
                Object vmRuntime = Objects.requireNonNull(getRuntime).invoke(null);

                Objects.requireNonNull(setHiddenApiExemptions).invoke(
                        vmRuntime, new Object[]{new String[]{"L"}});
                hiddenApis = true;
            } catch (Exception ignored) {
            }
        }
    }

    // Hidden API block
    public static boolean checkReflection(Context context, int maxTargetSdk) {
        int effectiveTargetSdk = Math.min(Build.VERSION.SDK_INT,
                context.getApplicationInfo().targetSdkVersion);
        return effectiveTargetSdk <= maxTargetSdk || getHiddenApiStatus(context);
    }

    public static void tryUnlockHiddenApisInternal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                !hiddenApis && hiddenApiBypass) {
            hiddenApiBypass = false;
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    HiddenApiBypass.setHiddenApiExemptions("L")) || hasHiddenAPI0()) {
                hiddenApis = true;
            }
        }
    }

    public static boolean getHiddenApiStatus(Context context) {
        Objects.requireNonNull(context);
        if (hiddenApis) return true;
        if (hiddenApiBypass) {
            hiddenApiBypass = false;
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    HiddenApiBypass.setHiddenApiExemptions("L")) || hasHiddenAPI0()) {
                hiddenApis = true;
                return true;
            }
        }
        if (freeReflection) {
            freeReflection = false;
            try {
                Reflection.unseal(context);
            } catch (Throwable ignored) {
            }
            return hiddenApis = hasHiddenAPI0();
        }
        return false;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static boolean hasHiddenAPI0() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { // Check this for Android 10+ just in case.
                HardwareRenderer.class.getMethod("setContextPriority", int.class);
                return true;
            } catch (Throwable ignored) {
            }
        }
        try {
            ThreadedRenderer.class.getMethod("setContextPriority", int.class);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void setEdgeEffect(Object o, Field field, EdgeEffect edgeEffect) {
        try {
            field.set(o, edgeEffect);
        } catch (IllegalAccessException ignored) {
        }
    }

    public static boolean isOxygenOS() {
        if (Build.VERSION.SDK_INT >= 31) return false;
        String roRomVersion = SystemProperties.get("ro.rom.version", "").trim();
        String roOxygenVersion = SystemProperties.get("ro.oxygen.version", "").trim();
        return roRomVersion.contains("Oxygen OS") || roRomVersion.contains("O2_BETA") ||
                roOxygenVersion.length() >= 1;
    }

    public static boolean isAndroidSDK() {
        return Build.BRAND.equals("google") && Build.MODEL.contains("sdk");
    }

    // May be wrong is come cases, but is mostly right.
    public static boolean isDynamicAccentSupported(Context context) {
        if (dynamicAccent != null) return dynamicAccent;
        TypedValue typedValue = new TypedValue();
        context.getResources().getValue(R.color.system_accent_light, typedValue, true);
        int lightType = typedValue.type;
        context.getResources().getValue(R.color.system_accent_dark, typedValue, true);
        int darkType = typedValue.type;
        return dynamicAccent = FoxLineage.getFoxLineage(context).hasLineageStyles() &&
                (lightType >= TypedValue.TYPE_FIRST_COLOR_INT &&
                        lightType <= TypedValue.TYPE_LAST_COLOR_INT) &&
                (darkType >= TypedValue.TYPE_FIRST_COLOR_INT &&
                        darkType <= TypedValue.TYPE_LAST_COLOR_INT);
    }
}
