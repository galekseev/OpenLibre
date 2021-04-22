package com.camomile.openlibre.ui;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.camomile.openlibre.R;

/**
 * A {@link androidx.fragment.app.FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter extends SmartFragmentStatePagerAdapter {

    SectionsPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager, context);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
//        if (position == mContext.getResources().getInteger(R.integer.viewpager_page_show_scan))
//            return DataPlotFragment.newInstance();
//        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_fragment_log))
//            return LogFragment.newInstance();
//        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_profile))
//            return UserFragment.newInstance();
        return null;
    }

    @Override
    public int getCount() {
        // Show 2 total pages.
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == mContext.getResources().getInteger(R.integer.viewpager_page_show_scan))
            return mContext.getResources().getString(R.string.fragment_title_scan);
        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_fragment_log))
            return mContext.getResources().getString(R.string.fragment_title_log);
        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_profile))
            return mContext.getResources().getString(R.string.fragment_title_profile);
        return null;
    }
}
