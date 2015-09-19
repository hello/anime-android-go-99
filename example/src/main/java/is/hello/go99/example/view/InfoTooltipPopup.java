package is.hello.go99.example.view;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import is.hello.go99.example.R;

public class InfoTooltipPopup {
    public static final long VISIBLE_DURATION = 2000;

    private final Activity activity;
    private final Runnable onAutomaticDismiss;

    private final PopupWindow popupWindow;
    private final TextView contents;

    public InfoTooltipPopup(@NonNull Activity activity,
                            @NonNull Runnable onAutomaticDismiss) {
        this.activity = activity;
        this.onAutomaticDismiss = onAutomaticDismiss;

        this.popupWindow = new PopupWindow(activity);
        popupWindow.setAnimationStyle(R.style.AppTheme_TooltipAnimation);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable()); // Required for touch to dismiss
        popupWindow.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        this.contents = new TextView(activity);
        contents.setTextAppearance(activity, android.support.v7.appcompat.R.style.TextAppearance_AppCompat_Body2);
        contents.setBackgroundResource(R.drawable.background_info_tooltip_popup);

        final Resources resources = activity.getResources();
        final int paddingHorizontal = resources.getDimensionPixelSize(R.dimen.popup_info_tooltip_padding_horizontal),
                paddingVertical = resources.getDimensionPixelSize(R.dimen.popup_info_tooltip_padding_vertical);
        contents.setPadding(contents.getPaddingLeft() + paddingHorizontal,
                            contents.getPaddingTop() + paddingVertical,
                            contents.getPaddingRight() + paddingHorizontal,
                            contents.getPaddingBottom() + paddingVertical);

        popupWindow.setContentView(contents);
    }

    private int getNavigationBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Display display = activity.getWindowManager().getDefaultDisplay();

            Point realSize = new Point();
            display.getRealSize(realSize);

            Point visibleArea = new Point();
            display.getSize(visibleArea);

            // Status bar is counted as part of the display height,
            // so the delta just gives us the navigation bar height.
            return realSize.y - visibleArea.y;
        } else {
            return 0;
        }
    }

    public void setText(@NonNull CharSequence text) {
        contents.setText(text);
    }

    public void show(@NonNull View fromView) {
        View parent = (View) fromView.getParent();
        Resources resources = activity.getResources();
        int parentHeight = parent.getMeasuredHeight();
        int overlay = resources.getDimensionPixelSize(R.dimen.popup_info_tooltip_overlay);
        int fromViewTop = fromView.getTop() + overlay;
        int bottomInset = (parentHeight - fromViewTop) + getNavigationBarHeight();
        int leftInset = resources.getDimensionPixelSize(R.dimen.popup_info_tooltip_left_inset);
        popupWindow.showAtLocation(parent, Gravity.BOTTOM | Gravity.START, leftInset, bottomInset);

        contents.postDelayed(new Runnable() {
            @Override
            public void run() {
                onAutomaticDismiss.run();
                dismiss();
            }
        }, VISIBLE_DURATION);
    }

    public void dismiss() {
        popupWindow.dismiss();
    }
}
