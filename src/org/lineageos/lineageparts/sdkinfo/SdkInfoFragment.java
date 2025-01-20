/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.sdkinfo;

import android.content.Context;
import android.os.Bundle;
import android.util.ArraySet;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;

import java.util.List;
import java.util.Set;

public class SdkInfoFragment extends SettingsPreferenceFragment implements Searchable  {

    public static final String TAG = "SdkInfoFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lineage_sdk_info);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public Set<String> getNonIndexableKeys(Context context) {
                    final Set<String> result = new ArraySet<>();
                    return result;
                }
            };
}
