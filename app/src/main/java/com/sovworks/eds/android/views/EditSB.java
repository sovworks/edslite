package com.sovworks.eds.android.views;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.util.AttributeSet;

import com.sovworks.eds.crypto.EditableSecureBuffer;
import com.sovworks.eds.crypto.SecureBuffer;

import java.nio.CharBuffer;

public class EditSB extends AppCompatEditText
{
    public EditSB(Context context)
    {
        this(context, null);
    }

    public EditSB(Context context, AttributeSet attrs)
    {
        this(context, attrs, android.R.attr.editTextStyle);
    }

    public EditSB(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setSaveEnabled(false);
    }

    @Override
    public void setText(CharSequence text, BufferType type)
    {
        Editable et = getEditableText();
        if(et != null)
            et.clear();
        super.setText(text, type);
    }

    /*@Override
    public Parcelable onSaveInstanceState()
    {
        Parcelable p = super.onSaveInstanceState();
        EditableSecureBuffer e = getEditableSB();
        SecureBuffer sb = e == null ? null : e.getSecureBuffer();
        return new State(p, sb);

    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        //State s = (State)state;
        super.onRestoreInstanceState(s._parent);
        //setSecureBuffer(s._buf);

    }

    private static class State implements Parcelable
    {
        State(Parcelable parent, SecureBuffer cur)
        {
            _parent = parent;
            _buf = cur;
        }

        protected State(Parcel in)
        {
            _parent = in.readParcelable(ClassLoader.getSystemClassLoader());
            _buf = in.readParcelable(ClassLoader.getSystemClassLoader());
        }

        public static final Creator<State> CREATOR = new Creator<State>()
        {
            @Override
            public State createFromParcel(Parcel in)
            {
                return new State(in);
            }

            @Override
            public State[] newArray(int size)
            {
                return new State[size];
            }
        };

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeParcelable(_parent, 0);
            dest.writeParcelable(_buf, 0);
        }

        private Parcelable _parent;
        private SecureBuffer _buf;
    }
*/
    public void setSecureBuffer(final SecureBuffer sb)
    {
        setEditableFactory(new Editable.Factory()
        {
            @Override
            public Editable newEditable(CharSequence source)
            {
                if(sb != null)
                {
                    sb.adoptData(CharBuffer.wrap(source));
                    return new EditableSecureBuffer(sb);
                }
                return super.newEditable(source);
            }
        });
    }


    public EditableSecureBuffer getEditableSB()
    {
        Editable et = getEditableText();
        return et instanceof EditableSecureBuffer ? ((EditableSecureBuffer)et) : null;
    }
}
