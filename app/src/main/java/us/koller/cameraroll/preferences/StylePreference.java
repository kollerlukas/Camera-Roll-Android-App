package us.koller.cameraroll.preferences;

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.Settings;

public class StylePreference extends DialogPreference {

    private int style;
    private int mDialogLayoutResId = R.layout.pref_dialog_style;

    @SuppressWarnings("unused")
    public StylePreference(Context context) {
        this(context, null);
    }

    @SuppressWarnings("WeakerAccess")
    public StylePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.dialogPreferenceStyle);
    }

    @SuppressWarnings("WeakerAccess")
    public StylePreference(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        this(context, attrs, defStyleAttr, defStyleAttr);
    }

    @SuppressWarnings("WeakerAccess")
    public StylePreference(Context context, AttributeSet attrs,
                           int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setDialogLayoutResource(mDialogLayoutResId);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        style = getPersistedInt(getContext()
                .getResources().getInteger(R.integer.STYLE_PARALLAX_VALUE));
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;

        // Save to Shared Preferences
        persistInt(style);

        //update summary
        String style_name = Settings.Utils.getStyleName(getContext(), style);
        setSummary(style_name);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defaultValue) {
        // Read the value. Use the default value if it is not possible.
        setStyle(restorePersistedValue ?
                getPersistedInt(style) : (int) defaultValue);
    }

    @Override
    public int getDialogLayoutResource() {
        return mDialogLayoutResId;
    }
}
