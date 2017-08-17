package com.sovworks.eds.fs.encfs;

public class B64
{

    public static int B256ToB64Bytes(int numB256Bytes)
    {
        return (numB256Bytes * 8 + 5) / 6;  // round up
    }

    public static int B256ToB32Bytes(int numB256Bytes)
    {
        return (numB256Bytes * 8 + 4) / 5;  // round up
    }

    public static int B64ToB256Bytes(int numB64Bytes)
    {
        return (numB64Bytes * 6) / 8;  // round down
    }

    public static int B32ToB256Bytes(int numB32Bytes)
    {
        return (numB32Bytes * 5) / 8;  // round down
    }

    public static void changeBase2Inline(byte[] src, int offset, int srcLen, int src2Pow, int dst2Pow,
                           boolean outputPartialLastByte)
    {
        changeBase2Inline(src, offset, srcLen, src2Pow, dst2Pow, outputPartialLastByte, 0, 0, null, 0);
    }

    public static void changeBase2Inline(
            byte[] src,
            int offset,
            int srcLen,
            int src2Pow,
            int dst2Pow,
            boolean outputPartialLastByte,
            long work,
            int workBits,
            byte[] outLoc,
            int outOffset)
    {
        int mask = (1 << dst2Pow) - 1;
        if (outLoc == null)
        {
            outLoc = src;
            outOffset = offset;
        }

        // copy the new bits onto the high bits of the stream.
        // The bits that fall off the low end are the output bits.
        while (srcLen > 0 && workBits < dst2Pow)
        {
            work |= (src[offset++] & 0xFFL) << workBits;
            workBits += src2Pow;
            --srcLen;
        }

        // we have at least one value that can be output
        byte outVal = (byte)(work & mask);
        work >>= dst2Pow;
        workBits -= dst2Pow;

        if (srcLen > 0)
        {
            // more input left, so recurse
            changeBase2Inline(
                    src, offset, srcLen, src2Pow, dst2Pow, outputPartialLastByte,
                    work, workBits, outLoc, outOffset + 1);
            outLoc[outOffset] = outVal;
        }
        else
        {
            // no input left, we can write remaining values directly
            outLoc[outOffset++] = outVal;
            // we could have a partial value left in the work buffer..
            if (outputPartialLastByte)
            {
                while (workBits > 0)
                {
                    outLoc[outOffset++] = (byte)(work & mask);
                    work >>= dst2Pow;
                    workBits -= dst2Pow;
                }
            }
        }
    }

    // character set for ascii b64:
// ",-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
// a standard base64 (eg a64l doesn't use ',-' but uses './'.  We don't
// do that because '/' is a reserved character, and it is useful not to have
// '.' included in the encrypted names, so that it can be reserved for files
// with special meaning.
    private static final char[] B642AsciiTable = ",-0123456789".toCharArray();
    public static String B64ToString(byte[] in, int offset, int count)
    {
        StringBuilder sb = new StringBuilder();
        for (int cnt = 0; cnt < count; ++cnt)
        {
            int ch = in[offset + cnt];
            if (ch > 11)
            {
                if (ch > 37)
                    ch += 'a' - 38;
                else
                    ch += 'A' - 12;
            }
            else
                ch = B642AsciiTable[ch];
            sb.append((char)ch);
        }
        return sb.toString();
    }

    public static String B32ToString(byte[] buf, int offset, int count)
    {
        StringBuilder sb = new StringBuilder();
        for (int cnt = 0; cnt < count; ++cnt)
        {
            int ch = buf[offset + cnt];
            if (ch >= 0 && ch < 26)
                ch += 'A';
            else
                ch += '2' - 26;

            sb.append((char)ch);
        }
        return sb.toString();
    }

    public static byte[] StringToB32(String s)
    {
        byte[] res = new byte[s.length()];
        int i = 0;
        for(char ch: s.toCharArray())
        {
            int lch = Character.toUpperCase(ch);
            if (lch >= 'A')
                lch -= 'A';
            else
                lch += 26 - '2';
            res[i++] = (byte)(lch & 0xFF);
        }
        return res;
    }

    private static final char Ascii2B64Table[] =
        "                                            01  23456789:;       ".toCharArray();
    public static byte[] StringToB64(String s)
    {
        byte[] res = new byte[s.length()];
        int i = 0;
        for(char ch: s.toCharArray())
        {
            if (ch >= 'A')
            {
                if (ch >= 'a')
                    ch += 38 - 'a';
                else
                    ch += 12 - 'A';
            }
            else
                ch = (char)(Ascii2B64Table[ch] - '0');
            res[i++] = (byte)(ch & 0xFF);
        }
        return res;
    }
}
