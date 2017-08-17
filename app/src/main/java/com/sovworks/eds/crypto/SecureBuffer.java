package com.sovworks.eds.crypto;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

public class SecureBuffer implements Parcelable, CharSequence
{
    public static void eraseData(byte[] data)
    {
        if(data!=null)
            eraseData(data, 0, data.length);
    }

    public static void eraseData(byte[] data, int offset, int count)
    {
        if(data!=null)
        {
            /*byte[] randomBytes = new byte[count];
            _secureRandom.nextBytes(randomBytes);
            System.arraycopy(randomBytes, 0, data, offset, count);*/
            Arrays.fill(data, (byte)0);
        }
    }

    public static void eraseData(char[] data)
    {
        if(data!=null)
        {
            //for(int i=0, l=data.length;i<l;i++)
            //    data[i] = (char) _secureRandom.nextInt();
            Arrays.fill(data, (char)0);
        }
    }

    public static synchronized void closeAll()
    {
        for(int i = 0, l = _data.size(); i<l; i++)
        {
            Buffer b = _data.valueAt(i);
            b.erase();
        }
        _data.clear();
    }

    public static SecureBuffer reserveBytes(int capacity)
    {
        return new SecureBuffer(new byte[capacity], 0, 0);
    }

    public static SecureBuffer reserveChars(int capacity)
    {
        SecureBuffer sb = new SecureBuffer();
        sb.adoptData(new char[capacity], 0, 0);
        return sb;
    }

    public SecureBuffer()
    {
        this((byte[])null, 0, 0);
    }

    public SecureBuffer(byte[] data)
    {
        this(data!=null ? data : null, 0, data!=null ? data.length : 0);
    }

    public SecureBuffer(byte[] data, int offset, int count)
    {
        _id = reserveNewId();
        if(data!=null)
            adoptData(data, offset, count);
    }

    public SecureBuffer(char[] data)
    {
        this(data!=null ? data : null, 0, data!=null ? data.length : 0);
    }

    public SecureBuffer(char[] data, int offset, int count)
    {
        _id = reserveNewId();
        if(data!=null)
            adoptData(data, offset, count);
    }

    @Override
    public int length()
    {
        CharBuffer cb = getCharBuffer();
        return cb != null ? cb.length() : 0;
    }

    @Override
    public char charAt(int index)
    {
        return getCharBuffer().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        return getCharBuffer().subSequence(start, end);
    }

    public CharBuffer getCharBuffer()
    {
        return getCharBuffer(_id);
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof SecureBuffer)
        {
            ByteBuffer d1 = getByteBuffer();
            ByteBuffer d2 = ((SecureBuffer)obj).getByteBuffer();
            return d1.equals(d2);
        }
        else
            return super.equals(obj);
    }

    @Override
    public int hashCode()
    {
        ByteBuffer d = getByteBuffer();
        return d != null ? d.hashCode() : 0;
    }

    public void adoptData(CharBuffer cb)
    {
        if(cb.hasArray())
            adoptData(cb.array(), cb.position(), cb.remaining());
        else
        {
            char[] arr = new char[cb.length()];
            cb.get(arr);
            cb.rewind();
            if(!cb.isReadOnly())
            {
                while(cb.hasRemaining())
                    cb.put((char) _secureRandom.nextInt());
            }
            adoptData(arr, 0, arr.length);
        }
    }

    private synchronized static int reserveNewId()
    {
        return _counter++;
    }

    private synchronized static void setData(int id, byte[] data, int offset, int count)
    {
        Buffer b = _data.get(id);
        if(b!=null)
            b.setData(data, offset, count);
        else
        {
            b = new Buffer();
            b.setData(data, offset, count);
            _data.put(id, b);
        }
    }

    private synchronized static void setData(int id, char[] data, int offset, int count)
    {
        Buffer b = _data.get(id);
        if(b!=null)
            b.setData(data, offset, count);
        else
        {
            b = new Buffer();
            b.setData(data, offset, count);
            _data.put(id, b);
        }
    }

    private static synchronized ByteBuffer getByteBuffer(int id)
    {
        Buffer b = _data.get(id);
        return b == null ? null : b.getByteData();
    }

    private static synchronized CharBuffer getCharBuffer(int id)
    {
        Buffer b = _data.get(id);
        return b == null ? null : b.getCharData();
    }

    private synchronized static void closeBuffer(int id)
    {
        Buffer b = _data.get(id);
        if(b != null)
        {
            b.erase();
            _data.remove(id);
        }
    }

    public void close()
    {
        closeBuffer(_id);
    }

    public void adoptData(byte[] data)
    {
        adoptData(data, 0, data!=null ? data.length : 0);
    }

    public void adoptData(char[] data)
    {
        adoptData(data, 0, data!=null ? data.length : 0);
    }

    public void adoptData(byte[] data, int offset, int count)
    {
        setData(_id, data, offset, count);
    }

    public void adoptData(char[] data, int offset, int count)
    {
        setData(_id, data, offset, count);
    }

    public ByteBuffer getByteBuffer()
    {
        return getByteBuffer(_id);
    }

    public byte[] getDataArray()
    {
        ByteBuffer bb = getByteBuffer();
        if(bb == null)
            return null;
        byte[] res = new byte[bb.remaining()];
        bb.get(res);
        return res;
    }

    public static final Creator<SecureBuffer> CREATOR = new Creator<SecureBuffer>()
    {
        @Override
        public SecureBuffer createFromParcel(Parcel in)
        {
            return new SecureBuffer(in);
        }

        @Override
        public SecureBuffer[] newArray(int size)
        {
            return new SecureBuffer[size];
        }
    };

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeInt(_id);
    }

    protected SecureBuffer(Parcel in)
    {
        _id = in.readInt();
    }

    protected static class Buffer
    {
        ByteBuffer getByteData()
        {
            if(_isByteDataValid)
                return _isByteDataRO ? _byteData.asReadOnlyBuffer() : _byteData.duplicate();

            if(_charData == null || !_isCharDataValid)
                return null;
            if(_byteData != null)
                eraseData(_byteData.array());
            _charData.mark();
            _byteData = _charset.encode(_charData);
            _charData.reset();
            _isByteDataRO = true;
            return _byteData.asReadOnlyBuffer();
        }

        CharBuffer getCharData()
        {
            if(_isCharDataValid)
                return _isCharDataRO ? _charData.asReadOnlyBuffer() : _charData.duplicate();

            if(_byteData == null || !_isByteDataValid)
                return null;
            if(_charData != null)
                eraseData(_charData.array());
            _byteData.mark();
            _charData = _charset.decode(_byteData);
            _byteData.reset();
            _isCharDataRO = true;
            return _charData.asReadOnlyBuffer();
        }

        void setData(byte[] newData, int offset, int count)
        {
            _isCharDataValid = false;
            if(_byteData == null)
               _byteData = ByteBuffer.wrap(newData, offset, count);
            else if(_byteData.capacity() <= newData.length)
            {
                eraseData(_byteData.array());
                _byteData = ByteBuffer.wrap(newData, offset, count);
            }
            else
            {
                _byteData.clear().mark();
                _byteData.put(newData, offset, count).reset();
                _byteData.limit(count);
                eraseData(newData);
            }
            _isByteDataValid = true;
        }

        void setData(char[] newData, int offset, int count)
        {
            _isByteDataValid = false;
            if(_charData == null)
                _charData = CharBuffer.wrap(newData, offset, count);
            else if(_charData.capacity() <= newData.length)
            {
                eraseData(_charData.array());
                _charData = CharBuffer.wrap(newData, offset, count);
            }
            else
            {
                _charData.clear().mark();
                _charData.put(newData, offset, count).reset();
                _charData.limit(count);
                eraseData(newData);
            }
            _isCharDataValid = true;
        }

        void erase()
        {
            if(_charData!=null)
            {
                eraseData(_charData.array());
                _charData = null;
            }
            if(_byteData != null)
            {
                eraseData(_byteData.array());
                _byteData = null;
            }
            _isByteDataValid = _isCharDataValid = false;
        }

        private ByteBuffer _byteData;
        private CharBuffer _charData;
        private boolean _isCharDataValid, _isByteDataValid, _isCharDataRO, _isByteDataRO;
    }

    protected static final SecureRandom _secureRandom = new SecureRandom();
    private static final SparseArray<Buffer> _data = new SparseArray<>();
    private static int _counter;
    private static final Charset _charset = Charset.forName("UTF-8");

    private final int _id;
}
