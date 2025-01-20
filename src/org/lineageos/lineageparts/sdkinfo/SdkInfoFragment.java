/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-FileCopyrightText: 2025 TheParasiteProject
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.sdkinfo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.search.BaseSearchIndexProvider;
import org.lineageos.lineageparts.search.Searchable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class SdkInfoFragment extends SettingsPreferenceFragment implements Searchable {

    public static final String TAG = "SdkInfoFragment";

    private static final String LINEAGE_VERSION = "lineage_version";
    private static final String LINEAGE_VENDOR_SECURITY_PATCH = "lineage_vendor_security_patch";
    private static final String LINEAGE_BUILD_DATE = "lineage_build_date";
    private static final String LINEAGE_LICENSE = "lineage_license";

    private static final String KEY_LINEAGE_VERSION_PROP = "ro.lineage.build.version";
    private static final String KEY_AOSP_VENDOR_SECURITY_PATCH = "ro.vendor.build.security_patch";
    private static final String KEY_LINEAGE_VENDOR_SECURITY_PATCH =
            "ro.lineage.build.vendor_security_patch";
    private static final String KEY_BUILD_DATE_PROP = "ro.build.date";
    private static final String KEY_LINEAGE_LICENSE_URL = "ro.lineagelegal.url";

    private static final int DELAY_TIMER_MILLIS = 500;
    private static final int ACTIVITY_TRIGGER_COUNT = 3;

    private static final String PLATLOGO_PACKAGE_NAME = "org.lineageos.lineageparts";
    private static final String PLATLOGO_ACTIVITY_CLASS =
            PLATLOGO_PACKAGE_NAME + ".logo.PlatLogoActivity";

    private Preference mLineageVersion;
    private Preference mLineageVendorSecurityPatch;
    private Preference mLineageBuildDate;

    private UserManager mUserManager;
    private long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];

    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lineage_sdk_info);

        mUserManager = (UserManager) requireContext().getSystemService(Context.USER_SERVICE);
        initializeAdminPermissions();

        mLineageVersion = findPreference(LINEAGE_VERSION);
        mLineageVersion.setSummary(
                SystemProperties.get(KEY_LINEAGE_VERSION_PROP, getString(R.string.unknown)));

        mLineageVendorSecurityPatch = findPreference(LINEAGE_VENDOR_SECURITY_PATCH);
        mLineageVendorSecurityPatch.setSummary(getVendorSecurityPatchSummary());

        mLineageBuildDate = findPreference(LINEAGE_BUILD_DATE);
        mLineageBuildDate.setSummary(
                SystemProperties.get(KEY_BUILD_DATE_PROP, getString(R.string.unknown)));

        mLineageBuildDate = findPreference(LINEAGE_LICENSE);
        mLineageBuildDate.setOnPreferenceClickListener(
                p -> {
                    startActivity(getLicenseIntent());
                    return true;
                });
        mLineageBuildDate.setVisible(
                getLicenseIntent().resolveActivity(requireContext().getPackageManager()) != null);
    }

    private CharSequence getVendorSecurityPatchSummary() {
        String patchLevel = SystemProperties.get(KEY_AOSP_VENDOR_SECURITY_PATCH);

        if (patchLevel.isEmpty()) {
            patchLevel = SystemProperties.get(KEY_LINEAGE_VENDOR_SECURITY_PATCH);
        }

        if (!patchLevel.isEmpty()) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchLevelDate = template.parse(patchLevel);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                patchLevel = DateFormat.format(format, patchLevelDate).toString();
            } catch (ParseException e) {
                // parsing failed, use raw string
            }
        } else {
            patchLevel = getString(R.string.unknown);
        }

        return patchLevel;
    }

    private Intent getLicenseIntent() {
        return new Intent(
                Intent.ACTION_VIEW, Uri.parse(SystemProperties.get(KEY_LINEAGE_LICENSE_URL)));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference != mLineageVersion) {
            return false;
        }
        arrayCopy();
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            if (mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                if (mFunDisallowedAdmin != null && !mFunDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                            requireContext(), mFunDisallowedAdmin);
                }
                Log.d(TAG, "Sorry, no fun for you!");
                return true;
            }

            final Intent intent =
                    new Intent(Intent.ACTION_MAIN)
                            .setClassName(PLATLOGO_PACKAGE_NAME, PLATLOGO_ACTIVITY_CLASS);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Unable to start activity " + intent.toString());
            }
        }
        return true;
    }

    private void arrayCopy() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
    }

    private void initializeAdminPermissions() {
        mFunDisallowedAdmin =
                RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                        requireContext(), UserManager.DISALLOW_FUN, UserHandle.myUserId());
        mFunDisallowedBySystem =
                RestrictedLockUtilsInternal.hasBaseUserRestriction(
                        requireContext(), UserManager.DISALLOW_FUN, UserHandle.myUserId());
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
