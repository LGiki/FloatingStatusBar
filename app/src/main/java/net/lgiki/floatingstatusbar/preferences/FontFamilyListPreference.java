package net.lgiki.floatingstatusbar.preferences;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;

import net.lgiki.floatingstatusbar.R;

public class FontFamilyListPreference extends ListPreference {

    public FontFamilyListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FontFamilyListPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FontFamilyListPreference(@NonNull Context context) {
        super(context);
    }

    public FontFamilyListPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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
            TypefaceSpan typeface;

            switch (entryValues[i].toString()) {
                case "monospace":
                    typeface = new TypefaceSpan("monospace");
                    break;
                case "serif":
                    typeface = new TypefaceSpan("serif");
                    break;
                case "cursive":
                    typeface = new TypefaceSpan("cursive");
                    break;
                case "sans_serif":
                default:
                    typeface = new TypefaceSpan("sans-serif");
                    break;
            }

            spannableString.setSpan(
                    typeface,
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
