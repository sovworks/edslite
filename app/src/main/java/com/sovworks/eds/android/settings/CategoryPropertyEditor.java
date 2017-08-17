package com.sovworks.eds.android.settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sovworks.eds.android.R;

public abstract class CategoryPropertyEditor extends PropertyEditorBase
{

	@Override
	public View createView(ViewGroup parent)
	{
		View view = super.createView(parent);
        _indicatorIcon = view.findViewById(android.R.id.icon);
        _indicatorIcon.setRotation(isExpanded() ? 180 : 0);
		return view;
	}

	@Override
	public void onClick()
	{
		rotateIconAndChangeState();
	}

	@Override
	public void save(Bundle b)
	{
		b.putBoolean(getBundleKey(), _isExpanded);
	}

	@Override
	public void save()
	{

	}

	@Override
	public void load(Bundle b)
	{
		if(b.getBoolean(getBundleKey(), false))
			expand();
		else
			collapse();
	}

	public final boolean isExpanded()
	{
		return _isExpanded;
	}

	public final void collapse()
	{
		_isExpanded = false;
		load();
		if(_indicatorIcon!=null)
        	_indicatorIcon.setRotation(0);
	}

	public final void expand()
	{
		_isExpanded = true;
		load();
		if(_indicatorIcon!=null)
        	_indicatorIcon.setRotation(180);
	}

	protected CategoryPropertyEditor(Host host, int titleResId, int descResId)
	{
		super(host, R.layout.settings_category, titleResId, descResId);
	}

	public static boolean IS_ANIMATING = false;
	private ImageView _indicatorIcon;
	private boolean _isExpanded;


    private void rotateIconAndChangeState()
    {
		IS_ANIMATING = true;
        _indicatorIcon.clearAnimation();
        ObjectAnimator anim = ObjectAnimator.ofFloat(_indicatorIcon, View.ROTATION, _isExpanded ? 0 : 180);
        anim.setDuration(200);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            _indicatorIcon.setHasTransientState(true);
        anim.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                if(_isExpanded)
                    collapse();
                else
                    expand();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    _indicatorIcon.setHasTransientState(false);
				IS_ANIMATING = false;

            }
        });
        anim.start();
    }
}
