package com.sovworks.eds.android.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.Util;

import java.util.List;

public abstract class SingleChoiceDialog<T> extends DialogFragment
{
    @Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        Util.setDialogStyle(this);
	}

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(getLayoutId(), container);
        v.findViewById(android.R.id.button1).setOnClickListener(view ->
        {
            onNo();
            getDialog().dismiss();
        });
        _okButton = v.findViewById(android.R.id.button2);
        _okButton.setOnClickListener(view ->
        {
            onYes();
            dismiss();
        });
        _okButton.setEnabled(false);
        _progressBar = v.findViewById(android.R.id.progress);
        _listView = v.findViewById(android.R.id.list);
        ((TextView)v.findViewById(android.R.id.text1)).setText(getTitle());
        if(_progressBar!=null)
        {
            _progressBar.setVisibility(View.VISIBLE);
            _listView.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        onLoadItems();
    }

    protected void onNo()
    {

    }

    protected void onYes()
    {
        int pos = getListView().getCheckedItemPosition();
        //noinspection unchecked
        onItemSelected(pos, (T) getListView().getItemAtPosition(pos));
    }

    protected int getLayoutId()
    {
        return R.layout.single_choice_dialog;
    }

    protected abstract void onItemSelected(int position, T item);
    protected abstract String getTitle();
    protected abstract void onLoadItems();

    protected final void fillList(List<T> items)
    {
        final ArrayAdapter<T> adapter = initAdapter(items);
        getListView().setAdapter(adapter);
        if(adapter.getCount() > 0 && _listView.getCheckedItemPosition()<0)
            getListView().setItemChecked(0, true);
        _okButton.setEnabled(_listView.getCheckedItemPosition()>=0);
        if(_progressBar!=null)
        {
            _progressBar.setVisibility(View.GONE);
            _listView.setVisibility(View.VISIBLE);
        }
    }

    protected ArrayAdapter<T> initAdapter(List<T> items)
    {
        return new ArrayAdapter<T>(getActivity(), android.R.layout.simple_list_item_single_choice, items)
        {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent)
            {
                final CheckedTextView tv = (CheckedTextView) super.getView(position, convertView, parent);
                tv.setOnClickListener(v ->
                {
                    getListView().setItemChecked(position, true);
                    _okButton.setEnabled(true);
                });
                tv.setChecked(getListView().isItemChecked(position));
                return tv;
            }
        };
    }

    protected final ListView getListView()
    {
        return _listView;
    }

    private ListView _listView;
    private ProgressBar _progressBar;
    private Button _okButton;

}
