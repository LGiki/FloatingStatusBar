package net.lgiki.floatingstatusbar.preferences;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;

import net.lgiki.floatingstatusbar.R;

public class FontWeightListPreference extends ListPreference {
    public FontWeightListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FontWeightListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FontWeightListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FontWeightListPreference(@NonNull Context context) {
        super(context);
    }

    private TextAppearanceSpan createFontWeightSpan(String weight) {
        int styleIndex;
        switch (weight) {
            case "100":
                styleIndex = R.style.TextAppearance_Weight100;
                break;
            case "200":
                styleIndex = R.style.TextAppearance_Weight200;
                break;
            case "300":
                styleIndex = R.style.TextAppearance_Weight300;
                break;
            case "500":
                styleIndex = R.style.TextAppearance_Weight500;
                break;
            case "600":
                styleIndex = R.style.TextAppearance_Weight600;
                break;
            case "700":
                styleIndex = R.style.TextAppearance_Weight700;
                break;
            case "800":
                styleIndex = R.style.TextAppearance_Weight800;
                break;
            case "900":
                styleIndex = R.style.TextAppearance_Weight900;
                break;
            case "950":
                styleIndex = R.style.TextAppearance_Weight950;
                break;
            case "400":
            default:
                styleIndex = R.style.TextAppearance_Weight400;
                break;
        }
        return new TextAppearanceSpan(getContext(), styleIndex);
    }

    @Override
    protected void onClick() {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();

        if (entries == null || entryValues == null) {
            super.onClick();
            return;
        }

        SpannableString[] typefaceItems = new SpannableString[entries.length];
        for (int i = 0; i < entries.length; i++) {
            SpannableString spannableString = new SpannableString(entries[i]);
            TextAppearanceSpan textAppearanceSpan = createFontWeightSpan(entryValues[i].toString());

            spannableString.setSpan(
                    textAppearanceSpan,
                    0,
                    spannableString.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            typefaceItems[i] = spannableString;
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this.getContext(), R.layout.custom_list_preference_entry, typefaceItems);

        new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setSingleChoiceItems(adapter, findIndexOfValue(getValue()), (dialog, index) -> {
                    if (index >= 0) {
                        String value = entryValues[index].toString();
                        if (callChangeListener(value)) {
                            setValue(value);
                        }
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
