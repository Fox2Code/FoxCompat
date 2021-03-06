package com.fox2code.foxcompat;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FoxViewCompat {
    public static final ColorDrawable NULL_DRAWABLE = new ColorDrawable(Color.TRANSPARENT) {
        @Override public void setColor(int color) {
            super.setColor(Color.TRANSPARENT);
        }
    };

    public static ViewGroup.LayoutParams getLayoutParams(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof WrappedLayoutParams) {
            ViewParent viewParent = view.getParent();
            if (viewParent instanceof FrameLayout) {
                return ((FrameLayout) viewParent).getLayoutParams();
            }
        }
        return layoutParams;
    }

    public static void setLayoutParams(View view, ViewGroup.LayoutParams layoutParams) {
        ViewGroup.LayoutParams oldLayoutParams = view.getLayoutParams();
        if (oldLayoutParams instanceof WrappedLayoutParams) {
            ViewParent viewParent = view.getParent();
            if (viewParent instanceof FrameLayout) {
                ((FrameLayout) viewParent).setLayoutParams(layoutParams);
                oldLayoutParams.width = layoutParams.width;
                oldLayoutParams.height = layoutParams.height;
                view.setLayoutParams(oldLayoutParams);
                return;
            }
        }
        view.setLayoutParams(layoutParams);
    }

    public static Rect getMargin(View view) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams(view);
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
        ViewGroup.LayoutParams layoutParams = getLayoutParams(view);
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
            setLayoutParams(view, marginLayoutParams);
        }
    }

    @Nullable
    public static FrameLayout getBackgroundView(@NonNull View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        ViewParent viewParent = view.getParent();
        if (layoutParams instanceof WrappedLayoutParams &&
                viewParent instanceof FrameLayout) {
            return (FrameLayout) viewParent;
        }
        return null;
    }

    @Nullable
    public static ViewParent getViewParent(@NonNull View view) {
        FrameLayout frameLayout = getBackgroundView(view);
        return (frameLayout != null ? frameLayout : view).getParent();
    }

    @Nullable
    public static Drawable getBackground(@NonNull View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        ViewParent viewParent = view.getParent();
        if (layoutParams instanceof WrappedLayoutParams &&
                viewParent instanceof FrameLayout) {
            return nullable(((WrappedLayoutParams) layoutParams).background);
        }
        return nullable(view.getBackground());
    }

    public static void setBackgroundView(@NonNull View view, @Nullable FrameLayout newParent) {
        if (newParent != null && newParent.getParent() != null)
            throw new IllegalArgumentException("Parent need to be unbound");
        if (newParent != null && newParent.getChildCount() != 0)
            throw new IllegalArgumentException("Parent need to be empty");
        // Step 1 - Get original state (Or current if no background view)
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        Drawable background = view.getBackground();
        ViewParent viewParent = view.getParent();
        if (!(viewParent instanceof ViewGroup))
            throw new IllegalStateException("Can't redefine view at root of activity");
        ViewGroup viewGroup = (ViewGroup) viewParent;
        if (layoutParams instanceof WrappedLayoutParams &&
                viewParent instanceof FrameLayout) {
            background = ((WrappedLayoutParams) layoutParams).background;
            layoutParams = getLayoutParams(view);
            viewParent = viewParent.getParent();
        } else if (newParent == null) {
            return; // Removing something that doesn't exists return null
        }
        ViewGroup viewRoot = (ViewGroup) viewParent;
        // Step 2 - Unbind old view
        int index;
        if (viewGroup == viewRoot) {
            index = viewRoot.indexOfChild(view);
            viewRoot.removeViewAt(index);
        } else {
            index = viewRoot.indexOfChild(viewGroup);
            viewRoot.removeViewAt(index);
            viewGroup.removeAllViewsInLayout();
        }
        // Step 3 - Bind new view
        if (newParent == null) {
            view.setBackground(background);
            view.setLayoutParams(layoutParams);
            viewRoot.addView(view, index);
        } else {
            view.setLayoutParams(new WrappedLayoutParams(layoutParams, background));
            view.setBackground(NULL_DRAWABLE);
            newParent.setLayoutParams(layoutParams);
            newParent.addView(view);
            viewRoot.addView(newParent, index);
        }
    }

    private static final class WrappedLayoutParams extends FrameLayout.LayoutParams {
        public final Drawable background;

        public WrappedLayoutParams(ViewGroup.LayoutParams layoutParams, Drawable background) {
            super(layoutParams.width, layoutParams.height);
            this.background = background;
        }
    }

    @NonNull
    public static Drawable nonNull(@Nullable Drawable drawable) {
        return drawable == null ? NULL_DRAWABLE : drawable;
    }

    @Nullable
    public static Drawable nullable(@Nullable Drawable drawable) {
        return drawable == NULL_DRAWABLE ? null : drawable;
    }

    public static boolean isNull(Drawable drawable) {
        return drawable == null || drawable == NULL_DRAWABLE;
    }
}
