package com.fox2code.foxcompat.internal;

import android.app.Activity;

import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.fox2code.foxcompat.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Alert to warn about various actions that developers might miss.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public enum FoxAlert {
    ;

    private final int message;

    FoxAlert(@StringRes int message) {
        this.message = message;
    }

    public void show(Activity activity) {
        if (activity == null) return;
        AlertDialog.Builder builder;
        if (FoxCompat.googleMaterial) {
            builder = new MaterialAlertDialogBuilder(activity);
        } else {
            builder = new AlertDialog.Builder(activity);
        }
        builder.setTitle(R.string.fox_compat_alert_title).setMessage(this.message)
                .setCancelable(true).setPositiveButton(android.R.string.ok, (d, w) -> {}).show();
    }
}
