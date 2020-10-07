/*
 * SPDX-FileCopyrightText: 2017-2021 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.statusbar;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.preference.Preference;

import lineageos.preference.LineageSecureSettingListPreference;
import lineageos.preference.LineageSecureSettingMainSwitchPreference;
import lineageos.preference.LineageSecureSettingSeekBarPreference;
import lineageos.preference.LineageSecureSettingSwitchPreference;
import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class NetworkTrafficSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "NetworkTrafficSettings";

    private LineageSecureSettingMainSwitchPreference mNetTrafficEnabled;
    private LineageSecureSettingListPreference mNetTrafficMode;
    private LineageSecureSettingSwitchPreference mNetTrafficAutohide;
    private LineageSecureSettingSeekBarPreference mNetTrafficAutohideThreshold;
    private LineageSecureSettingSeekBarPreference mNetTrafficRefreshInterval;
    private LineageSecureSettingListPreference mNetTrafficUnits;
    private LineageSecureSettingSwitchPreference mNetTrafficHideArrow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.network_traffic_settings);
        final ContentResolver resolver = getActivity().getContentResolver();

        mNetTrafficEnabled =
                (LineageSecureSettingMainSwitchPreference)
                        findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_ENABLED);
        mNetTrafficEnabled.setOnPreferenceChangeListener(this);
        mNetTrafficMode =
                (LineageSecureSettingListPreference)
                        findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_MODE);
        mNetTrafficAutohide =
                (LineageSecureSettingSwitchPreference)
                        findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE);
        mNetTrafficAutohideThreshold =
                (LineageSecureSettingSeekBarPreference)
                        findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);
        mNetTrafficRefreshInterval =
                (LineageSecureSettingSeekBarPreference)
                        findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_REFRESH_INTERVAL);
        mNetTrafficUnits =
                (LineageSecureSettingListPreference)
                        findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_UNITS);
        mNetTrafficHideArrow =
                (LineageSecureSettingSwitchPreference)
                        findPreference(LineageSettings.Secure.NETWORK_TRAFFIC_HIDEARROW);

        boolean enabled =
                LineageSettings.Secure.getIntForUser(
                                resolver,
                                LineageSettings.Secure.NETWORK_TRAFFIC_ENABLED,
                                0,
                                UserHandle.USER_CURRENT)
                        != 0;
        updateEnabledStates(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mNetTrafficEnabled) {
            updateEnabledStates((Boolean) newValue);
            return true;
        }
        return false;
    }

    private void updateEnabledStates(boolean enabled) {
        mNetTrafficMode.setEnabled(enabled);
        mNetTrafficAutohide.setEnabled(enabled);
        mNetTrafficAutohideThreshold.setEnabled(enabled);
        mNetTrafficHideArrow.setEnabled(enabled);
        mNetTrafficRefreshInterval.setEnabled(enabled);
        mNetTrafficUnits.setEnabled(enabled);
    }
}
