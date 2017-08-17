package com.sovworks.eds.android.settings.container;

import android.view.View;
import android.view.ViewGroup;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragment;
import com.sovworks.eds.android.locations.fragments.CreateEDSLocationFragmentBase;
import com.sovworks.eds.android.settings.PropertyEditorBase;

public class ExistingContainerPropertyEditor extends PropertyEditorBase
{
    public ExistingContainerPropertyEditor(CreateEDSLocationFragmentBase createEDSLocationFragment)
    {
        super(
                createEDSLocationFragment,
                R.layout.settings_create_new_or_existing_container,
                R.string.create_new_container_or_add_existing_container,
                0);
    }

    @Override
    protected View createView(ViewGroup parent)
    {
        View view = super.createView(parent);
        view.findViewById(R.id.create_new_container_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                getHostFragment().showCreateNewLocationProperties();
                getHostFragment().getPropertiesView().loadProperties();
            }
        });
        view.findViewById(R.id.add_existing_container_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                getHostFragment().showAddExistingLocationProperties();
                getHost().getPropertiesView().loadProperties();
            }
        });
        return view;
    }

    protected CreateEDSLocationFragment getHostFragment()
    {
        return (CreateEDSLocationFragment) getHost();
    }
}
