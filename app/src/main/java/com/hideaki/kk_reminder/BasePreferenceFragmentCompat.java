package com.hideaki.kk_reminder;

import android.annotation.SuppressLint;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.view.ViewGroup;

import com.takisoft.fix.support.v7.preference.EditTextPreference;
import com.takisoft.fix.support.v7.preference.EditTextPreferenceDialogFragmentCompat;
import com.takisoft.fix.support.v7.preference.PreferenceCategory;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class BasePreferenceFragmentCompat extends PreferenceFragmentCompat {

  private static final String DIALOG_FRAGMENT_TAG =
      BasePreferenceFragmentCompat.class.getSimpleName();

  @SuppressLint("RestrictedApi")
  @Override
  protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {

    return new PreferenceGroupAdapter(preferenceScreen) {
      @Override
      public void onBindViewHolder(PreferenceViewHolder holder, int position) {

        super.onBindViewHolder(holder, position);
        Preference preference = getItem(position);
        if(preference instanceof PreferenceCategory) {
          setZeroPaddingToLayoutChildren(holder.itemView);
        }
        else {
          View iconFrame = holder.itemView.findViewById(R.id.icon_frame);
          if(iconFrame != null) {
            iconFrame.setVisibility(preference.getIcon() == null ? View.GONE : View.VISIBLE);
          }
        }
      }
    };
  }

  private void setZeroPaddingToLayoutChildren(View view) {

    if(!(view instanceof ViewGroup)) {
      return;
    }
    ViewGroup viewGroup = (ViewGroup)view;
    int childCount = viewGroup.getChildCount();
    for(int i = 0; i < childCount; i++) {
      setZeroPaddingToLayoutChildren(viewGroup.getChildAt(i));
      viewGroup.setPaddingRelative(
          0,
          viewGroup.getPaddingTop(),
          viewGroup.getPaddingEnd(),
          viewGroup.getPaddingBottom()
      );
    }
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {

    FragmentManager manager = getFragmentManager();
    checkNotNull(manager);
    if(manager.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
      return;
    }

    final DialogFragment f;
    if(preference instanceof EditTextPreference) {
      f = EditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
      f.setTargetFragment(this, 0);
      f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);

      getFragmentManager().executePendingTransactions();
      final AlertDialog dialog = (AlertDialog)f.getDialog();
      checkNotNull(dialog);

      MainActivity activity = (MainActivity)getActivity();
      checkNotNull(activity);

      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accent_color);
      dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(activity.accent_color);
    }
    else {
      super.onDisplayPreferenceDialog(preference);
    }
  }
}
