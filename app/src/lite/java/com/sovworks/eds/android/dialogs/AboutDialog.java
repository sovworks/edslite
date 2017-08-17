package com.sovworks.eds.android.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.settings.GlobalConfig;

public class AboutDialog extends AboutDialogBase
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if(v == null)
            return null;
        v.findViewById(R.id.donation_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                openDonationsPage();
            }
        });

        v.findViewById(R.id.check_source_code_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                openSourceCodePage();
            }
        });

        v.findViewById(R.id.check_full_version_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                openFullVersionPage();
            }
        });
        return v;
    }

    private void openDonationsPage()
    {
        try
        {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GlobalConfig.DONATIONS_URL)));
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    private void openFullVersionPage()
    {
        try
        {
            startActivity(Intent.createChooser(
                    new Intent(Intent.ACTION_VIEW, Uri.parse(GlobalConfig.FULL_VERSION_URL)),
                    "Select application")
            );
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

    private void openSourceCodePage()
    {
        try
        {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GlobalConfig.SOURCE_CODE_URL)));
        }
        catch (Exception e)
        {
            Logger.showAndLog(getActivity(), e);
        }
    }

}
