package com.sovworks.eds.android.settings.container;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.ContainerSettingsFragment;
import com.sovworks.eds.android.locations.fragments.ContainerSettingsFragmentBase;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.container.ContainerFormatInfo;
import com.sovworks.eds.veracrypt.FormatInfo;

import java.util.ArrayList;
import java.util.List;

public class ContainerFormatHintPropertyEditor extends ChoiceDialogPropertyEditor
{
    public ContainerFormatHintPropertyEditor(ContainerSettingsFragmentBase containerSettingsFragment)
    {
        super(containerSettingsFragment, R.string.container_format, R.string.container_format_desc, containerSettingsFragment.getTag());
    }

    @Override
    public ContainerSettingsFragment getHost()
    {
        return (ContainerSettingsFragment) super.getHost();
    }

    @Override
    protected int loadValue()
    {
        ContainerFormatInfo cfi = getHost().getCurrentContainerFormat();
        if (cfi == null)
        {
            showHints(null);
            return 0;
        }
        showHints(cfi);
        List<ContainerFormatInfo> supportedFormats = getHost().getLocation().getSupportedFormats();
        for(int i=0, l=supportedFormats.size();i<l;i++)
            if(cfi.getFormatName().equalsIgnoreCase(supportedFormats.get(i).getFormatName()))
                return l>1 ? i + 1 : i;
        return 0;
    }

    @Override
    protected void saveValue(int value)
    {
        ContainerFormatInfo selectedFormat = getSelectedFormat(value);
        getHost().getLocation().getExternalSettings().setContainerFormatName(
                selectedFormat == null ?
                        null
                        :
                        selectedFormat.getFormatName()
        );
        getHost().saveExternalSettings();
        getHost().getPropertiesView().loadProperties();
    }

    @Override
    protected ArrayList<String> getEntries()
    {
        List<ContainerFormatInfo> supportedFormats = getHost().getLocation().getSupportedFormats();
        ArrayList<String> entries = new ArrayList<>();
        if(supportedFormats.size() > 1)
            entries.add("-");
        for (ContainerFormatInfo cfi : supportedFormats)
            entries.add(cfi.getFormatName());
        return entries;
    }

    private ContainerFormatInfo getSelectedFormat(int pos)
    {
        List<ContainerFormatInfo> supportedFormats = getHost().getLocation().getSupportedFormats();
        if(supportedFormats.size() == 1)
            return supportedFormats.get(0);
        if (pos <= 0 || pos >= supportedFormats.size() + 1)
            return null;
        return supportedFormats.get(pos - 1);
    }

    private void showHints(ContainerFormatInfo selectedFormat)
    {
        if (selectedFormat == null)
            disableAlgHints();
        else
        {
            String cfn = selectedFormat.getFormatName();
            if (cfn.equalsIgnoreCase(com.sovworks.eds.truecrypt.FormatInfo.FORMAT_NAME) || cfn.equalsIgnoreCase(FormatInfo.FORMAT_NAME))
                enableAlgHints();
            else
                disableAlgHints();
        }
    }

    private void disableAlgHints()
    {
        getHost().getPropertiesView().setPropertyState(R.string.encryption_algorithm, false);
        getHost().getPropertiesView().setPropertyState(R.string.hash_algorithm, false);
    }

    private void enableAlgHints()
    {
        getHost().getPropertiesView().setPropertyState(R.string.encryption_algorithm, true);
        getHost().getPropertiesView().setPropertyState(R.string.hash_algorithm, true);
    }

}
