package com.hideaki.kk_reminder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import static android.app.Activity.RESULT_OK;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_URI_SOUND;
import static com.hideaki.kk_reminder.UtilClass.DEFAULT_VIBRATION_PATTERN;
import static com.hideaki.kk_reminder.UtilClass.REQUEST_CODE_RINGTONE_PICKER;
import static com.hideaki.kk_reminder.UtilClass.getPxFromDp;
import static com.hideaki.kk_reminder.UtilClass.getRegularizedVibrationStr;
import static com.hideaki.kk_reminder.UtilClass.getVibrationPattern;
import static com.hideaki.kk_reminder.UtilClass.setCursorDrawableColor;
import static java.util.Objects.requireNonNull;

public class SetAllFragment extends BasePreferenceFragmentCompat {

  private MainActivity activity;
  private Preference pickAlarm;
  private PreferenceScreen label;
  static String vibrationStr;
  static Uri uriSound;

  public static SetAllFragment newInstance() {

    return new SetAllFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {

    super.onAttach(context);
    activity = (MainActivity)context;
  }

  @Override
  public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {

    addPreferencesFromResource(R.xml.pick_alarm);
    addPreferencesFromResource(R.xml.vibration);
    setHasOptionsMenu(true);

    uriSound = DEFAULT_URI_SOUND;
    pickAlarm = findPreference("pick_alarm");
    pickAlarm.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pick_alarm));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, uriSound);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uriSound);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE_PICKER);
        return true;
      }
    });

    vibrationStr = DEFAULT_VIBRATION_PATTERN;
    label = (PreferenceScreen)findPreference("label");
    label.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {

        // ダイアログに表示するEditTextの設定
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText editText = new EditText(activity);
        setCursorDrawableColor(activity, editText);
        editText.getBackground().mutate().setColorFilter(new PorterDuffColorFilter(
          activity.accentColor,
          PorterDuff.Mode.SRC_IN
        ));
        editText.setText(vibrationStr);
        editText.setSelection(editText.getText().length());
        editText.setHint(R.string.vibration_hint);
        editText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        linearLayout.addView(editText);
        int paddingPx = getPxFromDp(activity, 20);
        linearLayout.setPadding(paddingPx, 0, paddingPx, 0);

        // バイブレーションのパターンを設定するダイアログを表示
        final AlertDialog dialog = new AlertDialog.Builder(activity)
          .setTitle(R.string.default_vibration)
          .setView(linearLayout)
          .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

              vibrationStr = editText.getText().toString();
              vibrationStr = getRegularizedVibrationStr(vibrationStr);
              label.setTitle(vibrationStr);
              if(vibrationStr.equals(DEFAULT_VIBRATION_PATTERN)) {
                label.setSummary(R.string.default_vibration_hint);
              }
              else {
                label.setSummary(R.string.custom_vibration_hint);
              }
              long[] vibrationPattern = getVibrationPattern(vibrationStr);
              Vibrator vibrator = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);
              requireNonNull(vibrator);
              if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect =
                  VibrationEffect.createWaveform(vibrationPattern, -1);
                vibrator.vibrate(effect);
              }
              else {
                vibrator.vibrate(vibrationPattern, -1);
              }

              SetAllProgressBarDialogFragment setAllProgressBarDialogFragment =
                new SetAllProgressBarDialogFragment(false);
              setAllProgressBarDialogFragment.show(
                activity.getSupportFragmentManager(), "set_all_vibration_progress_bar"
              );
            }
          })
          .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
          })
          .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
          @Override
          public void onShow(DialogInterface dialogInterface) {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(activity.accentColor);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(activity.accentColor);
          }
        });

        dialog.show();

        // ダイアログ表示時にソフトキーボードを自動で表示
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
          @Override
          public void onFocusChange(View v, boolean hasFocus) {

            if(hasFocus) {
              Window dialogWindow = dialog.getWindow();
              requireNonNull(dialogWindow);

              dialogWindow.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
              );
            }
          }
        });
        editText.requestFocus();

        return true;
      }
    });
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState
  ) {

    View view = super.onCreateView(inflater, container, savedInstanceState);
    requireNonNull(view);

    if(activity.isDarkMode) {
      view.setBackgroundColor(activity.backgroundMaterialDarkColor);
    }
    else {
      view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.background_light));
    }

    Toolbar toolbar = activity.findViewById(R.id.toolbar_layout);
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    requireNonNull(actionBar);

    activity.drawerToggle.setDrawerIndicatorEnabled(false);
    actionBar.setHomeAsUpIndicator(activity.upArrow);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle(R.string.set_all);

    // ラベルの初期化
    Ringtone ringtone = RingtoneManager.getRingtone(activity, uriSound);
    pickAlarm.setSummary(ringtone.getTitle(activity));

    label.setTitle(vibrationStr);
    if(vibrationStr.equals(DEFAULT_VIBRATION_PATTERN)) {
      label.setSummary(R.string.default_vibration_hint);
    }
    else {
      label.setSummary(R.string.custom_vibration_hint);
    }

    return view;
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {

    FragmentManager manager = getFragmentManager();
    requireNonNull(manager);
    manager.popBackStack();
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    if(requestCode == REQUEST_CODE_RINGTONE_PICKER) {
      if(resultCode == RESULT_OK) {

        uriSound = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if(uriSound == null) {
          uriSound = DEFAULT_URI_SOUND;
        }
        Ringtone ringtone = RingtoneManager.getRingtone(activity, uriSound);
        pickAlarm.setSummary(ringtone.getTitle(activity));

        SetAllProgressBarDialogFragment setAllProgressBarDialogFragment =
          new SetAllProgressBarDialogFragment(true);
        setAllProgressBarDialogFragment.show(
          activity.getSupportFragmentManager(), "set_all_sound_progress_bar"
        );
      }
    }
  }
}
