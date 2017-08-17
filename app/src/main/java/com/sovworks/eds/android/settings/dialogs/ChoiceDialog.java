package com.sovworks.eds.android.settings.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.android.settings.PropertyEditor;
import com.sovworks.eds.android.settings.views.PropertiesView;

import java.util.ArrayList;
import java.util.List;

public class ChoiceDialog extends DialogFragment
{
    public static final String ARG_VARIANTS = "com.sovworks.eds.android.VARIANTS";
    public static final String ARG_TITLE = "com.sovworks.eds.android.TITLE";

    public static final String TAG = "ChoiceDialog";
	
	public static void showDialog(FragmentManager fm, int propertyId, String title, List<String> variants, String hostFragmentTag)
	{
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_VARIANTS, new ArrayList<>(variants));
        args.putString(ARG_TITLE, title);
        args.putInt(PropertyEditor.ARG_PROPERTY_ID, propertyId);
        if(hostFragmentTag!=null)
            args.putString(PropertyEditor.ARG_HOST_FRAGMENT_TAG, hostFragmentTag);
		DialogFragment newFragment = new ChoiceDialog();
        newFragment.setArguments(args);
	    newFragment.show(fm, TAG);
	}

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
        PropertyEditor.Host host = PropertiesView.getHost(ChoiceDialog.this);
        final ChoiceDialogPropertyEditor pe = (ChoiceDialogPropertyEditor) host.getPropertiesView().getPropertyById(getArguments().getInt(PropertyEditor.ARG_PROPERTY_ID));
        ArrayList<String> variants = getArguments().getStringArrayList(ARG_VARIANTS);
		final String[] strings = variants == null ? new String[0] : variants.toArray(new String[variants.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle(getArguments().getString(ARG_TITLE))
            .setSingleChoiceItems(strings, pe.getSelectedEntry(),
                    new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int item)
                        {
                            pe.setSelectedEntry(item);
                            dialog.dismiss();
                        }
                    }
            );
        return builder.create();
	}

}
