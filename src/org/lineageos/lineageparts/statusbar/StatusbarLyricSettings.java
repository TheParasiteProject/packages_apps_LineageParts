/*
 * SPDX-FileCopyrightText: 2022 Project Kaleidoscope
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.statusbar;

import android.os.Bundle;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class StatusbarLyricSettings extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.status_bar_lyric_settings);
        getActivity().setTitle(R.string.status_bar_lyric_title);
    }
}
