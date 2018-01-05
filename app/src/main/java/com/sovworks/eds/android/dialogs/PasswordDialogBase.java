package com.sovworks.eds.android.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.settings.activities.OpeningOptionsActivity;
import com.sovworks.eds.android.views.EditSB;
import com.sovworks.eds.crypto.SecureBuffer;
import com.sovworks.eds.locations.LocationsManager;
import com.sovworks.eds.locations.Openable;
import com.trello.rxlifecycle2.components.RxDialogFragment;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public abstract class PasswordDialogBase extends RxDialogFragment
{
    public static final String TAG = "com.sovworks.eds.android.dialogs.PasswordDialog";

    public static final String ARG_LABEL = "com.sovworks.eds.android.LABEL";
    public static final String ARG_VERIFY_PASSWORD = "com.sovworks.eds.android.VERIFY_PASSWORD";
    public static final String ARG_HAS_PASSWORD = "com.sovworks.eds.android.HAS_PASSWORD";
    public static final String ARG_RECEIVER_FRAGMENT_TAG = "com.sovworks.eds.android.RECEIVER_FRAGMENT_TAG";

    public interface PasswordReceiver
    {
        void onPasswordEntered(PasswordDialog dlg);
        void onPasswordNotEntered(PasswordDialog dlg);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Util.setDialogStyle(this);
        _location = (Openable) LocationsManager.
                getLocationsManager(getActivity()).
                getFromBundle(getArguments(), null);
        _options = savedInstanceState == null ? getArguments() : savedInstanceState;
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.password_dialog, container);
        _labelTextView = v.findViewById(R.id.label);
        if (_labelTextView != null)
        {
            String label = loadLabel();
            if (label != null)
            {
                _labelTextView.setText(label);
                _labelTextView.setVisibility(View.VISIBLE);
            } else
                _labelTextView.setVisibility(View.GONE);
        }
        _passwordEditText = v.findViewById(R.id.password_et);
        _repeatPasswordEditText = v.findViewById(R.id.repeat_password_et);

        if(_passwordEditText != null)
        {
            if (hasPassword())
            {
                _passwordResult = SecureBuffer.reserveChars(50);
                _passwordEditText.setVisibility(View.VISIBLE);
                _passwordEditText.setSecureBuffer(_passwordResult);
            }
            else
            {
                _passwordResult = null;
                _passwordEditText.setVisibility(View.GONE);
            }
        }
        else
            _passwordResult = null;

        if (_repeatPasswordEditText != null)
        {
            if(hasPassword() && isPasswordVerificationRequired())
            {
                _repeatPasswordSB = SecureBuffer.reserveChars(50);
                _repeatPasswordEditText.setVisibility(View.VISIBLE);
                _repeatPasswordEditText.setSecureBuffer(_repeatPasswordSB);
            }
            else
            {
                _repeatPasswordSB = null;
                _repeatPasswordEditText.setVisibility(View.GONE);
            }
        }
        else
            _repeatPasswordSB = null;

        View passwordLayout = v.findViewById(R.id.password_layout);
        if(passwordLayout!=null)
        {
            if(hasPassword())
            {
                passwordLayout.setVisibility(View.VISIBLE);
                _passwordEditText.requestFocus();
                /*lifecycle().
                        filter(event -> event == FragmentEvent.RESUME).
                        subscribe(event -> {
                            final InputMethodManager imm = (InputMethodManager) _passwordEditText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            if(imm != null)
                                imm.showSoftInput(_passwordEditText, InputMethodManager.SHOW_FORCED);
                            //_passwordEditText.requestFocus();
                        });*/
            }
            else
                passwordLayout.setVisibility(hasPassword() ? View.VISIBLE : View.GONE);
        }

        Button b = v.findViewById(android.R.id.button1);
        if(b!=null)
            b.setOnClickListener(view -> confirm());

        ImageButton ib = v.findViewById(R.id.toggle_show_pass);
        if(ib!=null)
        {
            ib.setOnClickListener(v12 -> toggleShowPassword((ImageButton) v12));
        }

        ib = v.findViewById(R.id.settings);
        if(ib!=null)
        {
            if(_location == null)
                ib.setVisibility(View.GONE);
            ib.setOnClickListener(v1 -> openOptions());
        }
        return v;
    }



    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if(_passwordResult != null)
        {
            _passwordResult.close();
            _passwordResult = null;
        }
        if(_repeatPasswordSB != null)
        {
            _repeatPasswordSB.close();
            _repeatPasswordSB = null;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setWidthHeight();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch(requestCode)
        {
            case REQUEST_OPTIONS:
                if (resultCode == Activity.RESULT_OK)
                    _options = data.getExtras();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public char[] getPassword()
    {
        if(hasPassword() && _passwordEditText!=null)
        {
            Editable pwd = _passwordEditText.getText();
            char[] res = new char[pwd.length()];
            pwd.getChars(0, res.length, res, 0);
            return res;
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if(_options!=null)
            outState.putAll(_options);
    }

    public boolean hasPassword()
    {
        Bundle args = getArguments();
        return args!=null && args.getBoolean(ARG_HAS_PASSWORD, _location!=null && _location.hasPassword());
    }

    public Bundle getOptions()
    {
        return _options;
    }

    protected static final int REQUEST_OPTIONS = 1;
    protected TextView _labelTextView;
    protected EditSB _passwordEditText,_repeatPasswordEditText;
    protected Openable _location;
    protected Bundle _options;

    protected SecureBuffer _passwordResult, _repeatPasswordSB;

    protected void setWidthHeight()
    {
        Window w = getDialog().getWindow();
        if(w!=null)
            w.setLayout(calcWidth(), calcHeight());
    }

    protected int calcWidth()
    {
        return getResources().getDimensionPixelSize(R.dimen.password_dialog_width);
    }

    protected int calcHeight()
    {
        return WRAP_CONTENT;
        /*int height = getResources().getDimensionPixelSize(R.dimen.password_dialog_height);
        if(isPasswordVerificationRequired())
            height += 80;
        return height;*/
    }

    protected String loadLabel()
    {
        Bundle args = getArguments();
        return args != null ? args.getString(ARG_LABEL) : null;
    }

    protected boolean isPasswordVerificationRequired()
    {
        Bundle args = getArguments();
        return args!=null && args.getBoolean(ARG_VERIFY_PASSWORD, false);
    }

    protected void toggleShowPassword(ImageButton b)
    {
        int inputType = _passwordEditText.getInputType();
        if ((inputType & EditorInfo.TYPE_MASK_VARIATION) == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        {
            _passwordEditText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            if (_repeatPasswordEditText != null)
                _repeatPasswordEditText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
            b.setImageResource(R.drawable.ic_show_pass);
        } else
        {
            _passwordEditText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            if (_repeatPasswordEditText != null)
                _repeatPasswordEditText.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            b.setImageResource(R.drawable.ic_hide_pass);
        }
    }

    protected void openOptions()
    {
        Intent i = new Intent(getActivity(), OpeningOptionsActivity.class);
        if(_location!=null)
            LocationsManager.storePathsInIntent(i, _location, null);
        i.putExtras(_options);
        startActivityForResult(i, REQUEST_OPTIONS);
    }

    protected PasswordReceiver getResultReceiver()
    {
        Bundle args = getArguments();
        String recTag = args!=null ? args.getString(ARG_RECEIVER_FRAGMENT_TAG) : null;
        return recTag != null ? (PasswordReceiver) getFragmentManager().findFragmentByTag(recTag) : null;
    }

    protected boolean checkInput()
    {
        if(hasPassword() && isPasswordVerificationRequired())
        {
            if(!checkPasswordsMatch())
            {
                Toast.makeText(getActivity(), R.string.password_does_not_match, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    protected void confirm()
    {
        if(!checkInput())
            return;

        onPasswordEntered();
        dismiss();
    }

    protected boolean checkPasswordsMatch()
    {
        return _passwordEditText.getText().equals(_repeatPasswordEditText.getText());
    }

    protected void onPasswordEntered()
    {
        PasswordReceiver r = getResultReceiver();
        if(r!=null)
            r.onPasswordEntered((PasswordDialog) this);
        else
        {
            Activity act = getActivity();
            if(act instanceof PasswordReceiver)
                ((PasswordReceiver)act).onPasswordEntered((PasswordDialog) this);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog)
    {
        onPasswordNotEntered();
    }

    protected void onPasswordNotEntered()
    {
        PasswordReceiver r = getResultReceiver();
        if(r!=null)
            r.onPasswordNotEntered((PasswordDialog) this);
        else
        {
            Activity act = getActivity();
            if(act instanceof PasswordReceiver)
                ((PasswordReceiver)act).onPasswordNotEntered((PasswordDialog) this);
        }
    }
}
