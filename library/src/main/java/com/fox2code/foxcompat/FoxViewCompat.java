package com.fox2code.foxcompat;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

public class FoxViewCompat {
    public static Rect getMargin(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams =
                    (ViewGroup.MarginLayoutParams) layoutParams;
            return new Rect(marginLayoutParams.leftMargin, marginLayoutParams.topMargin,
                    marginLayoutParams.rightMargin, marginLayoutParams.bottomMargin);
        }
        return new Rect(0, 0, 0, 0);
    }

    public static void setMargin(View view, Rect margin) {
        setMargin(view, margin.left, margin.top, margin.right, margin.bottom);
    }

    public static void setMargin(View view, int leftMargin, int topMargin,
                                 int rightMargin, int bottomMargin) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = new ViewGroup.MarginLayoutParams(-1, -1);
        } else if (layoutParams.getClass().getName().equals(
                ViewGroup.LayoutParams.class.getName())) {
            layoutParams = new ViewGroup.MarginLayoutParams(layoutParams);
        }
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams =
                    (ViewGroup.MarginLayoutParams) layoutParams;
            marginLayoutParams.leftMargin = leftMargin;
            marginLayoutParams.topMargin = topMargin;
            marginLayoutParams.rightMargin = rightMargin;
            marginLayoutParams.bottomMargin = bottomMargin;
            view.setLayoutParams(marginLayoutParams);
        }
    }
}
