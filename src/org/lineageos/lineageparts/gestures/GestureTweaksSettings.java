/*
 * Copyright (C) 2020 abcduwhatever
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.lineageparts.gestures;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import lineageos.preference.SystemSettingMainSwitchPreference;

import org.lineageos.internal.util.DeviceKeysConstants;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;
import org.lineageos.lineageparts.utils.DeviceUtils;

import java.util.Set;

public class GestureTweaksSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, Searchable {

    private ListPreference mLeftSwipeActions;
    private ListPreference mRightSwipeActions;
    private Preference mLeftSwipeAppSelection;
    private Preference mRightSwipeAppSelection;
    private ListPreference mLeftVerticalSwipeActions;
    private ListPreference mRightVerticalSwipeActions;
    private Preference mLeftVerticalSwipeAppSelection;
    private Preference mRightVerticalSwipeAppSelection;
    private SystemSettingMainSwitchPreference mExtendedSwipe;
    private final int LAST_ACTION = DeviceKeysConstants.Action.values().length;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.gesture_nav_tweaks);

        final ContentResolver resolver = getActivity().getContentResolver();

        mExtendedSwipe = (SystemSettingMainSwitchPreference) findPreference("back_swipe_extended");
        boolean extendedSwipe =
                Settings.System.getIntForUser(
                                resolver,
                                Settings.System.BACK_SWIPE_EXTENDED,
                                0,
                                UserHandle.USER_CURRENT)
                        != 0;
        mExtendedSwipe.setChecked(extendedSwipe);
        mExtendedSwipe.setOnPreferenceChangeListener(this);

        int leftSwipeActions =
                Settings.System.getIntForUser(
                        resolver,
                        Settings.System.LEFT_LONG_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);
        mLeftSwipeActions = (ListPreference) findPreference("left_swipe_actions");
        mLeftSwipeActions.setValue(Integer.toString(leftSwipeActions));
        mLeftSwipeActions.setSummary(mLeftSwipeActions.getEntry());
        mLeftSwipeActions.setEnabled(extendedSwipe);
        mLeftSwipeActions.setOnPreferenceChangeListener(this);

        int rightSwipeActions =
                Settings.System.getIntForUser(
                        resolver,
                        Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);
        mRightSwipeActions = (ListPreference) findPreference("right_swipe_actions");
        mRightSwipeActions.setValue(Integer.toString(rightSwipeActions));
        mRightSwipeActions.setSummary(mRightSwipeActions.getEntry());
        mRightSwipeActions.setEnabled(extendedSwipe);
        mRightSwipeActions.setOnPreferenceChangeListener(this);

        mLeftSwipeAppSelection = (Preference) findPreference("left_swipe_app_action");
        boolean isAppSelection =
                Settings.System.getIntForUser(
                                resolver,
                                Settings.System.LEFT_LONG_BACK_SWIPE_ACTION,
                                0,
                                UserHandle.USER_CURRENT)
                        == LAST_ACTION /*action_app_action*/;
        mLeftSwipeAppSelection.setVisible(isAppSelection);

        mRightSwipeAppSelection = (Preference) findPreference("right_swipe_app_action");
        isAppSelection =
                Settings.System.getIntForUser(
                                resolver,
                                Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION,
                                0,
                                UserHandle.USER_CURRENT)
                        == LAST_ACTION /*action_app_action*/;
        mRightSwipeAppSelection.setVisible(isAppSelection);

        int leftVerticalSwipeActions =
                Settings.System.getIntForUser(
                        resolver,
                        Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);
        mLeftVerticalSwipeActions = (ListPreference) findPreference("left_vertical_swipe_actions");
        mLeftVerticalSwipeActions.setValue(Integer.toString(leftVerticalSwipeActions));
        mLeftVerticalSwipeActions.setSummary(mLeftVerticalSwipeActions.getEntry());
        mLeftVerticalSwipeActions.setEnabled(extendedSwipe);
        mLeftVerticalSwipeActions.setOnPreferenceChangeListener(this);

        int rightVerticalSwipeActions =
                Settings.System.getIntForUser(
                        resolver,
                        Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);
        mRightVerticalSwipeActions =
                (ListPreference) findPreference("right_vertical_swipe_actions");
        mRightVerticalSwipeActions.setValue(Integer.toString(rightVerticalSwipeActions));
        mRightVerticalSwipeActions.setSummary(mRightVerticalSwipeActions.getEntry());
        mRightVerticalSwipeActions.setEnabled(extendedSwipe);
        mRightVerticalSwipeActions.setOnPreferenceChangeListener(this);

        mLeftVerticalSwipeAppSelection =
                (Preference) findPreference("left_vertical_swipe_app_action");
        isAppSelection =
                Settings.System.getIntForUser(
                                resolver,
                                Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION,
                                0,
                                UserHandle.USER_CURRENT)
                        == LAST_ACTION /*action_app_action*/;
        mLeftVerticalSwipeAppSelection.setVisible(extendedSwipe && isAppSelection);

        mRightVerticalSwipeAppSelection =
                (Preference) findPreference("right_vertical_swipe_app_action");
        isAppSelection =
                Settings.System.getIntForUser(
                                resolver,
                                Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION,
                                0,
                                UserHandle.USER_CURRENT)
                        == LAST_ACTION /*action_app_action*/;
        mRightVerticalSwipeAppSelection.setVisible(extendedSwipe && isAppSelection);

        customAppCheck();
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLeftSwipeActions) {
            int leftSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(
                    getContentResolver(),
                    Settings.System.LEFT_LONG_BACK_SWIPE_ACTION,
                    leftSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mLeftSwipeActions.findIndexOfValue((String) newValue);
            mLeftSwipeActions.setSummary(mLeftSwipeActions.getEntries()[index]);
            mLeftSwipeAppSelection.setVisible(
                    mExtendedSwipe.isChecked() && leftSwipeActions == LAST_ACTION);
            actionPreferenceReload();
            customAppCheck();
            return true;
        } else if (preference == mRightSwipeActions) {
            int rightSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(
                    getContentResolver(),
                    Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION,
                    rightSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mRightSwipeActions.findIndexOfValue((String) newValue);
            mRightSwipeActions.setSummary(mRightSwipeActions.getEntries()[index]);
            mRightSwipeAppSelection.setVisible(
                    mExtendedSwipe.isChecked() && rightSwipeActions == LAST_ACTION);
            actionPreferenceReload();
            customAppCheck();
            return true;
        } else if (preference == mLeftVerticalSwipeActions) {
            int leftVerticalSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(
                    getContentResolver(),
                    Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION,
                    leftVerticalSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mLeftVerticalSwipeActions.findIndexOfValue((String) newValue);
            mLeftVerticalSwipeActions.setSummary(mLeftVerticalSwipeActions.getEntries()[index]);
            mLeftVerticalSwipeAppSelection.setVisible(
                    mExtendedSwipe.isChecked() && leftVerticalSwipeActions == LAST_ACTION);
            customAppCheck();
            return true;
        } else if (preference == mRightVerticalSwipeActions) {
            int rightVerticalSwipeActions = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(
                    getContentResolver(),
                    Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION,
                    rightVerticalSwipeActions,
                    UserHandle.USER_CURRENT);
            int index = mRightVerticalSwipeActions.findIndexOfValue((String) newValue);
            mRightVerticalSwipeActions.setSummary(mRightVerticalSwipeActions.getEntries()[index]);
            mRightVerticalSwipeAppSelection.setVisible(
                    mExtendedSwipe.isChecked() && rightVerticalSwipeActions == LAST_ACTION);
            customAppCheck();
            return true;
        } else if (preference == mExtendedSwipe) {
            boolean enabled = ((Boolean) newValue).booleanValue();
            mExtendedSwipe.setChecked(enabled);
            mLeftSwipeActions.setEnabled(enabled);
            mRightSwipeActions.setEnabled(enabled);
            mLeftVerticalSwipeActions.setEnabled(enabled);
            mRightVerticalSwipeActions.setEnabled(enabled);
            mLeftVerticalSwipeAppSelection.setVisible(
                    enabled && mLeftVerticalSwipeActions.getValue().equals("" + LAST_ACTION));
            mRightVerticalSwipeAppSelection.setVisible(
                    enabled && mRightVerticalSwipeActions.getValue().equals("" + LAST_ACTION));
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ensure preferences sensible to change get updated
        actionPreferenceReload();
        customAppCheck();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Ensure preferences sensible to change gets updated
        actionPreferenceReload();
        customAppCheck();
    }

    /* Helper for reloading both short and long gesture as they might change on
    package uninstallation */
    private void actionPreferenceReload() {
        int leftSwipeActions =
                Settings.System.getIntForUser(
                        getContentResolver(),
                        Settings.System.LEFT_LONG_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);

        int rightSwipeActions =
                Settings.System.getIntForUser(
                        getContentResolver(),
                        Settings.System.RIGHT_LONG_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);

        // Reload the action preferences
        mLeftSwipeActions.setValue(Integer.toString(leftSwipeActions));
        mLeftSwipeActions.setSummary(mLeftSwipeActions.getEntry());

        mRightSwipeActions.setValue(Integer.toString(rightSwipeActions));
        mRightSwipeActions.setSummary(mRightSwipeActions.getEntry());

        mLeftSwipeAppSelection.setVisible(
                mLeftSwipeActions.getEntryValues()[leftSwipeActions].equals("" + LAST_ACTION));
        mRightSwipeAppSelection.setVisible(
                mRightSwipeActions.getEntryValues()[rightSwipeActions].equals("" + LAST_ACTION));

        int leftVerticalSwipeActions =
                Settings.System.getIntForUser(
                        getContentResolver(),
                        Settings.System.LEFT_VERTICAL_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);

        int rightVerticalSwipeActions =
                Settings.System.getIntForUser(
                        getContentResolver(),
                        Settings.System.RIGHT_VERTICAL_BACK_SWIPE_ACTION,
                        0,
                        UserHandle.USER_CURRENT);

        // Reload the action preferences
        mLeftVerticalSwipeActions.setValue(Integer.toString(leftVerticalSwipeActions));
        mLeftVerticalSwipeActions.setSummary(mLeftVerticalSwipeActions.getEntry());

        mRightVerticalSwipeActions.setValue(Integer.toString(rightVerticalSwipeActions));
        mRightVerticalSwipeActions.setSummary(mRightVerticalSwipeActions.getEntry());

        mLeftVerticalSwipeAppSelection.setVisible(
                mExtendedSwipe.isChecked()
                        && mLeftVerticalSwipeActions.getEntryValues()[leftVerticalSwipeActions]
                                .equals("" + LAST_ACTION));
        mRightVerticalSwipeAppSelection.setVisible(
                mExtendedSwipe.isChecked()
                        && mRightVerticalSwipeActions.getEntryValues()[rightVerticalSwipeActions]
                                .equals("" + LAST_ACTION));
    }

    private void customAppCheck() {
        mLeftSwipeAppSelection.setSummary(
                Settings.System.getStringForUser(
                        getContentResolver(),
                        String.valueOf(Settings.System.LEFT_LONG_BACK_SWIPE_APP_FR_ACTION),
                        UserHandle.USER_CURRENT));
        mRightSwipeAppSelection.setSummary(
                Settings.System.getStringForUser(
                        getContentResolver(),
                        String.valueOf(Settings.System.RIGHT_LONG_BACK_SWIPE_APP_FR_ACTION),
                        UserHandle.USER_CURRENT));

        mLeftVerticalSwipeAppSelection.setSummary(
                Settings.System.getStringForUser(
                        getContentResolver(),
                        String.valueOf(Settings.System.LEFT_VERTICAL_BACK_SWIPE_APP_FR_ACTION),
                        UserHandle.USER_CURRENT));
        mRightVerticalSwipeAppSelection.setSummary(
                Settings.System.getStringForUser(
                        getContentResolver(),
                        String.valueOf(Settings.System.RIGHT_VERTICAL_BACK_SWIPE_APP_FR_ACTION),
                        UserHandle.USER_CURRENT));
    }

    public static final Searchable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public Set<String> getNonIndexableKeys(Context context) {
                    final Set<String> result = new ArraySet<>();

                    if (!DeviceUtils.isEdgeToEdgeEnabled(context)) {
                        result.add("back_swipe_extended");
                        result.add("left_swipe_actions");
                        result.add("right_swipe_actions");
                        result.add("left_vertical_swipe_actions");
                        result.add("right_vertical_swipe_actions");
                    } else {
                        result.remove("back_swipe_extended");
                        result.remove("left_swipe_actions");
                        result.remove("right_swipe_actions");
                        result.remove("left_vertical_swipe_actions");
                        result.remove("right_vertical_swipe_actions");
                    }
                    return result;
                }
            };
}
