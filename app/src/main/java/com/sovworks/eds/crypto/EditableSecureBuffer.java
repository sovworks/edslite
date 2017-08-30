package com.sovworks.eds.crypto;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;

import com.sovworks.eds.android.Logger;

import java.lang.reflect.Array;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.IdentityHashMap;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class EditableSecureBuffer implements Editable
{
    public static final String TAG = "EditableSecureBuffer";
    private static final boolean VERBOSE_LOG = false;

    public EditableSecureBuffer(SecureBuffer sb)
    {
        if(VERBOSE_LOG) Logger.debug(TAG + ": creating");
        _sb = sb;
        CharBuffer cb = sb.getCharBuffer();
        mGapStart = cb.length();
        mGapLength = cb.capacity() - mGapStart;
        mSpanCount = 0;
        mSpanInsertCount = 0;
        mSpans = new Object[0];
        mSpanStarts =
        mSpanEnds =
        mSpanFlags =
        mSpanMax =
        mSpanOrder =
        mPrioSortBuffer =
        mOrderSortBuffer = new int[0];
    }

    @SuppressWarnings("unused")
    public int getTextWatcherDepth()
    {
        return mTextWatcherDepth;
    }

    private static class GrowingArrayUtils
    {
        static <T> T[] append(T[] current, int count, T newElement)
        {
            count++;
            if(count >= current.length)
                current = Arrays.copyOf(current, getNewSize(count - 1 , 1));
            current[count - 1] = newElement;
            return current;
        }

        static int[] append(int[] current, int count, int newElement)
        {
            count++;
            if(count >= current.length)
                current = Arrays.copyOf(current, getNewSize(count - 1 , 1));
            current[count - 1] = newElement;
            return current;
        }

        private static int getNewSize(int current, int adding)
        {
            return Math.max(current*2, current + adding);
        }
    }

    private static class ArrayUtils
    {
        static <T> T[] emptyArray(Class<T> kind)
        {
            //noinspection unchecked
            return (T[])Array.newInstance(kind, 0);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in equals");
        if(obj instanceof EditableSecureBuffer && length() == ((EditableSecureBuffer) obj).length())
        {
            char[] d1 = new char[length()];
            char[] d2 = d1.clone();
            try
            {
                getChars(0, d1.length, d1, 0);
                ((EditableSecureBuffer)obj).getChars(0, d2.length, d2, 0);
                return Arrays.equals(d1, d2);
            }
            finally
            {
                SecureBuffer.eraseData(d1);
                SecureBuffer.eraseData(d2);
            }
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        char[] d = new char[length()];
        getChars(0, d.length, d, 0);
        return Arrays.hashCode(d);
    }

    private final SecureBuffer _sb;

    /**
     * Return the char at the specified offset within the buffer.
     */
    @Override
    public char charAt(int where) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in charAt");
        CharBuffer cb = _sb.getCharBuffer();
        if(cb == null)
            return ' ';
        cb.clear();
        int len = cb.capacity() - mGapLength;
        if (where < 0) {
            throw new IndexOutOfBoundsException("charAt: " + where + " < 0");
        } else if (where >= len) {
            throw new IndexOutOfBoundsException("charAt: " + where + " >= length " + len);
        }

        if (where >= mGapStart)
            return cb.charAt(where + mGapLength);
        else
            return cb.charAt(where);
    }

    /**
     * Return the number of chars in the buffer.
     */
    @Override
    public int length() {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in length");
        CharBuffer cb = _sb.getCharBuffer();
        return cb == null ? 0 : (cb.capacity() - mGapLength);
    }

    private void resizeFor(int size)
    {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in resizeFor");
        CharBuffer cb = _sb.getCharBuffer();
        if(cb == null)
            return;
        cb.clear();
        final int oldLength = cb.capacity();
        if (size + 1 <= oldLength) {
            return;
        }

        char[] newText = new char[size*2];
        cb.get(newText, 0, mGapStart);
        final int newLength = newText.length;
        final int delta = newLength - oldLength;
        final int after = oldLength - (mGapStart + mGapLength);
        cb.position(oldLength - after);
        cb.get(newText, newLength - after, after);
        _sb.adoptData(CharBuffer.wrap(newText));

        mGapLength += delta;
        if (mGapLength < 1)
            new Exception("mGapLength < 1").printStackTrace();

        if (mSpanCount != 0) {
            for (int i = 0; i < mSpanCount; i++) {
                if (mSpanStarts[i] > mGapStart) mSpanStarts[i] += delta;
                if (mSpanEnds[i] > mGapStart) mSpanEnds[i] += delta;
            }
            calcMax(treeRoot());
        }
    }

    private void moveGapTo(int where) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in moveGapTo");
        if (where == mGapStart)
            return;

        boolean atEnd = (where == length());

        CharBuffer cb = _sb.getCharBuffer();
        if(cb == null)
            return;
        cb.clear();
        if (where < mGapStart) {
            int overlap = mGapStart - where;
            CharBuffer sub = cb.subSequence(where, where + overlap);
            cb.position(mGapStart + mGapLength - overlap);
            cb.put(sub);
        } else /* where > mGapStart */ {
            int overlap = where - mGapStart;
            CharBuffer sub = cb.subSequence(where + mGapLength - overlap, where + mGapLength);
            cb.position(mGapStart);
            cb.put(sub);
        }

        if (mSpanCount != 0) {
            for (int i = 0; i < mSpanCount; i++) {
                int start = mSpanStarts[i];
                int end = mSpanEnds[i];

                if (start > mGapStart)
                    start -= mGapLength;
                if (start > where)
                    start += mGapLength;
                else if (start == where) {
                    int flag = (mSpanFlags[i] & START_MASK) >> START_SHIFT;

                    if (flag == POINT || (atEnd && flag == PARAGRAPH))
                        start += mGapLength;
                }

                if (end > mGapStart)
                    end -= mGapLength;
                if (end > where)
                    end += mGapLength;
                else if (end == where) {
                    int flag = (mSpanFlags[i] & END_MASK);

                    if (flag == POINT || (atEnd && flag == PARAGRAPH))
                        end += mGapLength;
                }

                mSpanStarts[i] = start;
                mSpanEnds[i] = end;
            }
            calcMax(treeRoot());
        }

        mGapStart = where;
    }

    // Documentation from interface
    public EditableSecureBuffer insert(int where, CharSequence tb, int start, int end) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in insert");
        return replace(where, where, tb, start, end);
    }

    // Documentation from interface
    public EditableSecureBuffer insert(int where, CharSequence tb) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in insert 2");
        return replace(where, where, tb, 0, tb.length());
    }

    // Documentation from interface
    public EditableSecureBuffer delete(int start, int end) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in delete");
        EditableSecureBuffer ret = replace(start, end, "", 0, 0);

        if (mGapLength > 2 * length())
            resizeFor(length());

        return ret; // == this
    }

    // Documentation from interface
    public void clear() {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in clear");
        replace(0, length(), "", 0, 0);
        mSpanInsertCount = 0;
    }

    // Documentation from interface
    public void clearSpans() {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in clearSpans");
        for (int i = mSpanCount - 1; i >= 0; i--) {
            Object what = mSpans[i];
            int ostart = mSpanStarts[i];
            int oend = mSpanEnds[i];

            if (ostart > mGapStart)
                ostart -= mGapLength;
            if (oend > mGapStart)
                oend -= mGapLength;

            mSpanCount = i;
            mSpans[i] = null;

            sendSpanRemoved(what, ostart, oend);
        }
        if (mIndexOfSpan != null) {
            mIndexOfSpan.clear();
        }
        mSpanInsertCount = 0;
    }

    @NonNull
    @Override
    public String toString()
    {
        char[] cs = new char[length()];
        Arrays.fill(cs, ' ');
        return new String(cs);
    }

    // Documentation from interface
    public EditableSecureBuffer append(CharSequence text) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in append");
        int length = length();
        return replace(length, length, text, 0, text.length());
    }

    /**
     * Appends the character sequence {@code text} and spans {@code what} over the appended part.
     * See {@link Spanned} for an explanation of what the flags mean.
     * @param text the character sequence to append.
     * @param what the object to be spanned over the appended text.
     * @param flags see {@link Spanned}.
     * @return this {@code SpannableStringBuilder}.
     */
    public EditableSecureBuffer append(CharSequence text, Object what, int flags) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in append 2");
        int start = length();
        append(text);
        setSpan(what, start, length(), flags);
        return this;
    }

    // Documentation from interface
    public EditableSecureBuffer append(CharSequence text, int start, int end) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in append 3");
        int length = length();
        return replace(length, length, text, start, end);
    }

    // Documentation from interface
    public EditableSecureBuffer append(char text) {
        return append(String.valueOf(text));
    }

    // Returns true if a node was removed (so we can restart search from root)
    private boolean removeSpansForChange(int start, int end, boolean textIsRemoved, int i) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in removeSpansForChange");
        if ((i & 1) != 0) {
            // internal tree node
            if (resolveGap(mSpanMax[i]) >= start &&
                    removeSpansForChange(start, end, textIsRemoved, leftChild(i))) {
                return true;
            }
        }
        if (i < mSpanCount) {
            if ((mSpanFlags[i] & Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) ==
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE &&
                    mSpanStarts[i] >= start && mSpanStarts[i] < mGapStart + mGapLength &&
                    mSpanEnds[i] >= start && mSpanEnds[i] < mGapStart + mGapLength &&
                    // The following condition indicates that the span would become empty
                    (textIsRemoved || mSpanStarts[i] > start || mSpanEnds[i] < mGapStart)) {
                mIndexOfSpan.remove(mSpans[i]);
                removeSpan(i);
                return true;
            }
            return resolveGap(mSpanStarts[i]) <= end && (i & 1) != 0 &&
                    removeSpansForChange(start, end, textIsRemoved, rightChild(i));
        }
        return false;
    }

    private void change(int start, int end, CharSequence cs, int csStart, int csEnd) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in change");
        // Can be negative
        final int replacedLength = end - start;
        final int replacementLength = csEnd - csStart;
        final int nbNewChars = replacementLength - replacedLength;

        boolean changed = false;
        for (int i = mSpanCount - 1; i >= 0; i--) {
            int spanStart = mSpanStarts[i];
            if (spanStart > mGapStart)
                spanStart -= mGapLength;

            int spanEnd = mSpanEnds[i];
            if (spanEnd > mGapStart)
                spanEnd -= mGapLength;

            if ((mSpanFlags[i] & SPAN_PARAGRAPH) == SPAN_PARAGRAPH) {
                int ost = spanStart;
                int oen = spanEnd;
                int clen = length();

                if (spanStart > start && spanStart <= end) {
                    for (spanStart = end; spanStart < clen; spanStart++)
                        if (spanStart > end && charAt(spanStart - 1) == '\n')
                            break;
                }

                if (spanEnd > start && spanEnd <= end) {
                    for (spanEnd = end; spanEnd < clen; spanEnd++)
                        if (spanEnd > end && charAt(spanEnd - 1) == '\n')
                            break;
                }

                if (spanStart != ost || spanEnd != oen) {
                    setSpan(false, mSpans[i], spanStart, spanEnd, mSpanFlags[i]);
                    changed = true;
                }
            }

            int flags = 0;
            if (spanStart == start) flags |= SPAN_START_AT_START;
            else if (spanStart == end + nbNewChars) flags |= SPAN_START_AT_END;
            if (spanEnd == start) flags |= SPAN_END_AT_START;
            else if (spanEnd == end + nbNewChars) flags |= SPAN_END_AT_END;
            mSpanFlags[i] |= flags;
        }
        if (changed) {
            restoreInvariants();
        }

        moveGapTo(end);

        CharBuffer cb = _sb.getCharBuffer();
        if(cb == null)
            return;

        if (nbNewChars >= mGapLength) {
            resizeFor(cb.capacity() + nbNewChars - mGapLength);
        }

        final boolean textIsRemoved = replacementLength == 0;
        // The removal pass needs to be done before the gap is updated in order to broadcast the
        // correct previous positions to the correct intersecting SpanWatchers
        if (replacedLength > 0) { // no need for span fixup on pure insertion
            //noinspection StatementWithEmptyBody
            while (mSpanCount > 0 &&
                    removeSpansForChange(start, end, textIsRemoved, treeRoot())) {
                // keep deleting spans as needed, and restart from root after every deletion
                // because deletion can invalidate an index.
            }
        }

        mGapStart += nbNewChars;
        mGapLength -= nbNewChars;

        if (mGapLength < 1)
            new Exception("mGapLength < 1").printStackTrace();

        cb = _sb.getCharBuffer();
        if(cb == null)
            return;
        cb.clear();
        cb.position(start);
        cb.put(CharBuffer.wrap(cs, csStart, csEnd));

        if (replacedLength > 0) { // no need for span fixup on pure insertion
            final boolean atEnd = (mGapStart + mGapLength == cb.capacity());

            for (int i = 0; i < mSpanCount; i++) {
                final int startFlag = (mSpanFlags[i] & START_MASK) >> START_SHIFT;
                mSpanStarts[i] = updatedIntervalBound(mSpanStarts[i], start, nbNewChars, startFlag,
                        atEnd, textIsRemoved);

                final int endFlag = (mSpanFlags[i] & END_MASK);
                mSpanEnds[i] = updatedIntervalBound(mSpanEnds[i], start, nbNewChars, endFlag,
                        atEnd, textIsRemoved);
            }
            restoreInvariants();
        }

        if (cs instanceof Spanned) {
            Spanned sp = (Spanned) cs;
            Object[] spans = sp.getSpans(csStart, csEnd, Object.class);

            for (int i = 0; i < spans.length; i++) {
                int st = sp.getSpanStart(spans[i]);
                int en = sp.getSpanEnd(spans[i]);

                if (st < csStart) st = csStart;
                if (en > csEnd) en = csEnd;

                // Add span only if this object is not yet used as a span in this string
                if (getSpanStart(spans[i]) < 0) {
                    int copySpanStart = st - csStart + start;
                    int copySpanEnd = en - csStart + start;
                    int copySpanFlags = sp.getSpanFlags(spans[i]) | SPAN_ADDED;

                    int flagsStart = (copySpanFlags & START_MASK) >> START_SHIFT;
                    int flagsEnd = copySpanFlags & END_MASK;

                    if(!isInvalidParagraphStart(copySpanStart, flagsStart) &&
                            !isInvalidParagraphEnd(copySpanEnd, flagsEnd)) {
                        setSpan(false, spans[i], copySpanStart, copySpanEnd, copySpanFlags);
                    }
                }
            }
            restoreInvariants();
        }
    }

    private int updatedIntervalBound(int offset, int start, int nbNewChars, int flag, boolean atEnd,
                                     boolean textIsRemoved) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in updatedIntervalBound");
        if (offset >= start && offset < mGapStart + mGapLength) {
            if (flag == POINT) {
                // A POINT located inside the replaced range should be moved to the end of the
                // replaced text.
                // The exception is when the point is at the start of the range and we are doing a
                // text replacement (as opposed to a deletion): the point stays there.
                if (textIsRemoved || offset > start) {
                    return mGapStart + mGapLength;
                }
            } else {
                if (flag == PARAGRAPH) {
                    if (atEnd) {
                        return mGapStart + mGapLength;
                    }
                } else { // MARK
                    // MARKs should be moved to the start, with the exception of a mark located at
                    // the end of the range (which will be < mGapStart + mGapLength since mGapLength
                    // is > 0, which should stay 'unchanged' at the end of the replaced text.
                    if (textIsRemoved || offset < mGapStart - nbNewChars) {
                        return start;
                    } else {
                        // Move to the end of replaced text (needed if nbNewChars != 0)
                        return mGapStart;
                    }
                }
            }
        }
        return offset;
    }

    // Note: caller is responsible for removing the mIndexOfSpan entry.
    private void removeSpan(int i) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in removeSpan");
        Object object = mSpans[i];

        int start = mSpanStarts[i];
        int end = mSpanEnds[i];

        if (start > mGapStart) start -= mGapLength;
        if (end > mGapStart) end -= mGapLength;

        int count = mSpanCount - (i + 1);
        System.arraycopy(mSpans, i + 1, mSpans, i, count);
        System.arraycopy(mSpanStarts, i + 1, mSpanStarts, i, count);
        System.arraycopy(mSpanEnds, i + 1, mSpanEnds, i, count);
        System.arraycopy(mSpanFlags, i + 1, mSpanFlags, i, count);
        System.arraycopy(mSpanOrder, i + 1, mSpanOrder, i, count);

        mSpanCount--;

        invalidateIndex(i);
        mSpans[mSpanCount] = null;

        // Invariants must be restored before sending span removed notifications.
        restoreInvariants();

        sendSpanRemoved(object, start, end);
    }

    /**
     * Return externally visible offset given offset into gapped buffer.
     */
    private int resolveGap(int i) {
        return i > mGapStart ? i - mGapLength : i;
    }

    // Documentation from interface
    public EditableSecureBuffer replace(int start, int end, CharSequence tb) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in replace");
        return replace(start, end, tb, 0, tb.length());
    }

    // Documentation from interface
    @SuppressLint("DefaultLocale")
    public EditableSecureBuffer replace(final int start, final int end,
                                        CharSequence tb, int tbstart, int tbend) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in replace 2");
        checkRange("replace", start, end);

        int filtercount = mFilters.length;
        for (int i = 0; i < filtercount; i++) {
            CharSequence repl = mFilters[i].filter(tb, tbstart, tbend, this, start, end);

            if (repl != null) {
                tb = repl;
                tbstart = 0;
                tbend = repl.length();
            }
        }

        final int origLen = end - start;
        final int newLen = tbend - tbstart;

        if (origLen == 0 && newLen == 0 && !hasNonExclusiveExclusiveSpanAt(tb, tbstart)) {
            // This is a no-op iif there are no spans in tb that would be added (with a 0-length)
            // Early exit so that the text watchers do not get notified
            return this;
        }

        TextWatcher[] textWatchers = getSpans(start, start + origLen, TextWatcher.class);
        sendBeforeTextChanged(textWatchers, start, origLen, newLen);

        // Try to keep the cursor / selection at the same relative position during
        // a text replacement. If replaced or replacement text length is zero, this
        // is already taken care of.
        boolean adjustSelection = origLen != 0 && newLen != 0;
        int selectionStart = 0;
        int selectionEnd = 0;
        if (adjustSelection) {
            selectionStart = Selection.getSelectionStart(this);
            selectionEnd = Selection.getSelectionEnd(this);
        }

        change(start, end, tb, tbstart, tbend);

        if (adjustSelection) {
            boolean changed = false;
            if (selectionStart > start && selectionStart < end) {
                final long diff = selectionStart - start;
                final int offset = (int)(diff * newLen / origLen);
                selectionStart = start + offset;

                changed = true;
                setSpan(false, Selection.SELECTION_START, selectionStart, selectionStart,
                        Spanned.SPAN_POINT_POINT);
            }
            if (selectionEnd > start && selectionEnd < end) {
                final long diff = selectionEnd - start;
                final int offset = (int)(diff * newLen / origLen);
                selectionEnd = start + offset;

                changed = true;
                setSpan(false, Selection.SELECTION_END, selectionEnd, selectionEnd,
                        Spanned.SPAN_POINT_POINT);
            }
            if (changed) {
                restoreInvariants();
            }
        }

        if(VERBOSE_LOG) Logger.debug(String.format("before send text changed: start=%d origLen=%d newLen=%d", start, origLen, newLen));
        sendTextChanged(textWatchers, start, origLen, newLen);
        sendAfterTextChanged(textWatchers);

        // Span watchers need to be called after text watchers, which may update the layout
        sendToSpanWatchers(start, end, newLen - origLen);

        return this;
    }

    private static boolean hasNonExclusiveExclusiveSpanAt(CharSequence text, int offset) {
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            Object[] spans = spanned.getSpans(offset, offset, Object.class);
            final int length = spans.length;
            for (int i = 0; i < length; i++) {
                Object span = spans[i];
                int flags = spanned.getSpanFlags(span);
                if (flags != Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) return true;
            }
        }
        return false;
    }

    private void sendToSpanWatchers(int replaceStart, int replaceEnd, int nbNewChars) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in sendToSpanWatchers");
        for (int i = 0; i < mSpanCount; i++) {
            int spanFlags = mSpanFlags[i];

            // This loop handles only modified (not added) spans.
            if ((spanFlags & SPAN_ADDED) != 0) continue;
            int spanStart = mSpanStarts[i];
            int spanEnd = mSpanEnds[i];
            if (spanStart > mGapStart) spanStart -= mGapLength;
            if (spanEnd > mGapStart) spanEnd -= mGapLength;

            int newReplaceEnd = replaceEnd + nbNewChars;
            boolean spanChanged = false;

            int previousSpanStart = spanStart;
            if (spanStart > newReplaceEnd) {
                if (nbNewChars != 0) {
                    previousSpanStart -= nbNewChars;
                    spanChanged = true;
                }
            } else if (spanStart >= replaceStart) {
                // No change if span start was already at replace interval boundaries before replace
                if ((spanStart != replaceStart ||
                        ((spanFlags & SPAN_START_AT_START) != SPAN_START_AT_START)) &&
                        (spanStart != newReplaceEnd ||
                                ((spanFlags & SPAN_START_AT_END) != SPAN_START_AT_END))) {
                    // A correct previousSpanStart cannot be computed at this point.
                    // It would require to save all the previous spans' positions before the replace
                    // Using an invalid -1 value to convey this would break the broacast range
                    spanChanged = true;
                }
            }

            int previousSpanEnd = spanEnd;
            if (spanEnd > newReplaceEnd) {
                if (nbNewChars != 0) {
                    previousSpanEnd -= nbNewChars;
                    spanChanged = true;
                }
            } else if (spanEnd >= replaceStart) {
                // No change if span start was already at replace interval boundaries before replace
                if ((spanEnd != replaceStart ||
                        ((spanFlags & SPAN_END_AT_START) != SPAN_END_AT_START)) &&
                        (spanEnd != newReplaceEnd ||
                                ((spanFlags & SPAN_END_AT_END) != SPAN_END_AT_END))) {
                    // same as above for previousSpanEnd
                    spanChanged = true;
                }
            }

            if (spanChanged) {
                sendSpanChanged(mSpans[i], previousSpanStart, previousSpanEnd, spanStart, spanEnd);
            }
            mSpanFlags[i] &= ~SPAN_START_END_MASK;
        }

        // Handle added spans
        for (int i = 0; i < mSpanCount; i++) {
            int spanFlags = mSpanFlags[i];
            if ((spanFlags & SPAN_ADDED) != 0) {
                mSpanFlags[i] &= ~SPAN_ADDED;
                int spanStart = mSpanStarts[i];
                int spanEnd = mSpanEnds[i];
                if (spanStart > mGapStart) spanStart -= mGapLength;
                if (spanEnd > mGapStart) spanEnd -= mGapLength;
                sendSpanAdded(mSpans[i], spanStart, spanEnd);
            }
        }
    }

    /**
     * Mark the specified range of text with the specified object.
     * The flags determine how the span will behave when text is
     * inserted at the start or end of the span's range.
     */
    public void setSpan(Object what, int start, int end, int flags) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in setSpan");
        setSpan(true, what, start, end, flags);
    }

    // Note: if send is false, then it is the caller's responsibility to restore
    // invariants. If send is false and the span already exists, then this method
    // will not change the index of any spans.
    private void setSpan(boolean send, Object what, int start, int end, int flags) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in setSpan 2");
        checkRange("setSpan", start, end);

        int flagsStart = (flags & START_MASK) >> START_SHIFT;
        if(isInvalidParagraphStart(start, flagsStart)) {
            throw new RuntimeException("PARAGRAPH span must start at paragraph boundary");
        }

        int flagsEnd = flags & END_MASK;
        if(isInvalidParagraphEnd(end, flagsEnd)) {
            throw new RuntimeException("PARAGRAPH span must end at paragraph boundary");
        }

        // 0-length Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        if (flagsStart == POINT && flagsEnd == MARK && start == end) {
            if (send) {
                Log.e(TAG, "SPAN_EXCLUSIVE_EXCLUSIVE spans cannot have a zero length");
            }
            // Silently ignore invalid spans when they are created from this class.
            // This avoids the duplication of the above test code before all the
            // calls to setSpan that are done in this class
            return;
        }

        int nstart = start;
        int nend = end;

        if (start > mGapStart) {
            start += mGapLength;
        } else if (start == mGapStart) {
            if (flagsStart == POINT || (flagsStart == PARAGRAPH && start == length()))
                start += mGapLength;
        }

        if (end > mGapStart) {
            end += mGapLength;
        } else if (end == mGapStart) {
            if (flagsEnd == POINT || (flagsEnd == PARAGRAPH && end == length()))
                end += mGapLength;
        }

        if (mIndexOfSpan != null) {
            Integer index = mIndexOfSpan.get(what);
            if (index != null) {
                int i = index;
                int ostart = mSpanStarts[i];
                int oend = mSpanEnds[i];

                if (ostart > mGapStart)
                    ostart -= mGapLength;
                if (oend > mGapStart)
                    oend -= mGapLength;

                mSpanStarts[i] = start;
                mSpanEnds[i] = end;
                mSpanFlags[i] = flags;

                if (send) {
                    restoreInvariants();
                    sendSpanChanged(what, ostart, oend, nstart, nend);
                }

                return;
            }
        }

        mSpans = GrowingArrayUtils.append(mSpans, mSpanCount, what);
        mSpanStarts = GrowingArrayUtils.append(mSpanStarts, mSpanCount, start);
        mSpanEnds = GrowingArrayUtils.append(mSpanEnds, mSpanCount, end);
        mSpanFlags = GrowingArrayUtils.append(mSpanFlags, mSpanCount, flags);
        mSpanOrder = GrowingArrayUtils.append(mSpanOrder, mSpanCount, mSpanInsertCount);
        invalidateIndex(mSpanCount);
        mSpanCount++;
        mSpanInsertCount++;
        // Make sure there is enough room for empty interior nodes.
        // This magic formula computes the size of the smallest perfect binary
        // tree no smaller than mSpanCount.
        int sizeOfMax = 2 * treeRoot() + 1;
        if (mSpanMax.length < sizeOfMax) {
            mSpanMax = new int[sizeOfMax];
        }

        if (send) {
            restoreInvariants();
            sendSpanAdded(what, nstart, nend);
        }
    }

    private boolean isInvalidParagraphStart(int start, int flagsStart) {
        if (flagsStart == PARAGRAPH) {
            if (start != 0 && start != length()) {
                char c = charAt(start - 1);

                if (c != '\n') return true;
            }
        }
        return false;
    }

    private boolean isInvalidParagraphEnd(int end, int flagsEnd) {
        if (flagsEnd == PARAGRAPH) {
            if (end != 0 && end != length()) {
                char c = charAt(end - 1);

                if (c != '\n') return true;
            }
        }
        return false;
    }

    /**
     * Remove the specified markup object from the buffer.
     */
    public void removeSpan(Object what) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in removeSpan");
        if (mIndexOfSpan == null) return;
        Integer i = mIndexOfSpan.remove(what);
        if (i != null) {
            removeSpan(i.intValue());
        }
    }

    /**
     * Return the buffer offset of the beginning of the specified
     * markup object, or -1 if it is not attached to this buffer.
     */
    public int getSpanStart(Object what) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in getSpanStart");
        if (mIndexOfSpan == null) return -1;
        Integer i = mIndexOfSpan.get(what);
        return i == null ? -1 : resolveGap(mSpanStarts[i]);
    }

    /**
     * Return the buffer offset of the end of the specified
     * markup object, or -1 if it is not attached to this buffer.
     */
    public int getSpanEnd(Object what) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in getSpanEnd");
        if (mIndexOfSpan == null) return -1;
        Integer i = mIndexOfSpan.get(what);
        return i == null ? -1 : resolveGap(mSpanEnds[i]);
    }

    /**
     * Return the flags of the end of the specified
     * markup object, or 0 if it is not attached to this buffer.
     */
    public int getSpanFlags(Object what) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in getSpanFlags");
        if (mIndexOfSpan == null) return 0;
        Integer i = mIndexOfSpan.get(what);
        return i == null ? 0 : mSpanFlags[i];
    }

    /**
     * Return an array of the spans of the specified type that overlap
     * the specified range of the buffer.  The kind may be Object.class to get
     * a list of all the spans regardless of type.
     */
    @SuppressWarnings("unchecked")
    public <T> T[] getSpans(int queryStart, int queryEnd, @Nullable Class<T> kind) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in getSpans");
        return getSpans(queryStart, queryEnd, kind, true);
    }

    /**
     * Return an array of the spans of the specified type that overlap
     * the specified range of the buffer.  The kind may be Object.class to get
     * a list of all the spans regardless of type.
     *
     * @param queryStart Start index.
     * @param queryEnd End index.
     * @param kind Class type to search for.
     * @param sort If true the results are sorted by the insertion order.
     * @param <T> span type
     * @return Array of the spans. Empty array if no results are found.
     *
     */
    private <T> T[] getSpans(int queryStart, int queryEnd, @Nullable Class<T> kind,
                             boolean sort) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in getSpans 2");
        if (kind == null) //noinspection unchecked
            return (T[]) ArrayUtils.emptyArray(Object.class);
        if (mSpanCount == 0) return ArrayUtils.emptyArray(kind);
        int count = countSpans(queryStart, queryEnd, kind, treeRoot());
        if (count == 0) {
            return ArrayUtils.emptyArray(kind);
        }

        // Safe conversion, but requires a suppressWarning
        @SuppressWarnings("unchecked") T[] ret = (T[]) Array.newInstance(kind, count);
        if (sort) {
            mPrioSortBuffer = checkSortBuffer(mPrioSortBuffer, count);
            mOrderSortBuffer = checkSortBuffer(mOrderSortBuffer, count);
        }
        getSpansRec(queryStart, queryEnd, kind, treeRoot(), ret, mPrioSortBuffer,
                mOrderSortBuffer, 0, sort);
        if (sort) sort(ret, mPrioSortBuffer, mOrderSortBuffer);
        return ret;
    }

    private int countSpans(int queryStart, int queryEnd, Class kind, int i) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in countSpans");
        int count = 0;
        if ((i & 1) != 0) {
            // internal tree node
            int left = leftChild(i);
            int spanMax = mSpanMax[left];
            if (spanMax > mGapStart) {
                spanMax -= mGapLength;
            }
            if (spanMax >= queryStart) {
                count = countSpans(queryStart, queryEnd, kind, left);
            }
        }
        if (i < mSpanCount) {
            int spanStart = mSpanStarts[i];
            if (spanStart > mGapStart) {
                spanStart -= mGapLength;
            }
            if (spanStart <= queryEnd) {
                int spanEnd = mSpanEnds[i];
                if (spanEnd > mGapStart) {
                    spanEnd -= mGapLength;
                }
                if (spanEnd >= queryStart &&
                        (spanStart == spanEnd || queryStart == queryEnd ||
                                (spanStart != queryEnd && spanEnd != queryStart)) &&
                        (Object.class == kind || kind.isInstance(mSpans[i]))) {
                    count++;
                }
                if ((i & 1) != 0) {
                    count += countSpans(queryStart, queryEnd, kind, rightChild(i));
                }
            }
        }
        return count;
    }

    /**
     * Fills the result array with the spans found under the current interval tree node.
     *
     * @param queryStart Start index for the interval query.
     * @param queryEnd End index for the interval query.
     * @param kind Class type to search for.
     * @param i Index of the current tree node.
     * @param ret Array to be filled with results.
     * @param priority Buffer to keep record of the priorities of spans found.
     * @param insertionOrder Buffer to keep record of the insertion orders of spans found.
     * @param count The number of found spans.
     * @param sort Flag to fill the priority and insertion order buffers. If false then
     *             the spans with priority flag will be sorted in the result array.
     * @param <T> span type
     * @return The total number of spans found.
     */
    @SuppressWarnings("unchecked")
    private <T> int getSpansRec(int queryStart, int queryEnd, Class<T> kind,
                                int i, T[] ret, int[] priority, int[] insertionOrder, int count, boolean sort) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in getSpansRec");
        if ((i & 1) != 0) {
            // internal tree node
            int left = leftChild(i);
            int spanMax = mSpanMax[left];
            if (spanMax > mGapStart) {
                spanMax -= mGapLength;
            }
            if (spanMax >= queryStart) {
                count = getSpansRec(queryStart, queryEnd, kind, left, ret, priority,
                        insertionOrder, count, sort);
            }
        }
        if (i >= mSpanCount) return count;
        int spanStart = mSpanStarts[i];
        if (spanStart > mGapStart) {
            spanStart -= mGapLength;
        }
        if (spanStart <= queryEnd) {
            int spanEnd = mSpanEnds[i];
            if (spanEnd > mGapStart) {
                spanEnd -= mGapLength;
            }
            if (spanEnd >= queryStart &&
                    (spanStart == spanEnd || queryStart == queryEnd ||
                            (spanStart != queryEnd && spanEnd != queryStart)) &&
                    (Object.class == kind || kind.isInstance(mSpans[i]))) {
                int spanPriority = mSpanFlags[i] & SPAN_PRIORITY;
                int target = count;
                if (sort) {
                    priority[target] = spanPriority;
                    insertionOrder[target] = mSpanOrder[i];
                } else if (spanPriority != 0) {
                    //insertion sort for elements with priority
                    int j = 0;
                    for (; j < count; j++) {
                        int p = getSpanFlags(ret[j]) & SPAN_PRIORITY;
                        if (spanPriority > p) break;
                    }
                    System.arraycopy(ret, j, ret, j + 1, count - j);
                    target = j;
                }
                ret[target] = (T) mSpans[i];
                count++;
            }
            if (count < ret.length && (i & 1) != 0) {
                count = getSpansRec(queryStart, queryEnd, kind, rightChild(i), ret, priority,
                        insertionOrder, count, sort);
            }
        }
        return count;
    }

    /**
     * Check the size of the buffer and grow if required.
     *
     * @param buffer Buffer to be checked.
     * @param size Required size.
     * @return Same buffer instance if the current size is greater than required size. Otherwise a
     * new instance is created and returned.
     */
    private int[] checkSortBuffer(int[] buffer, int size) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in checkSortBuffer");
        if(size > buffer.length) {
            return new int[size + buffer.length /2];
        }
        return buffer;
    }

    /**
     * An iterative heap sort implementation. It will sort the spans using first their priority
     * then insertion order. A span with higher priority will be before a span with lower
     * priority. If priorities are the same, the spans will be sorted with insertion order. A
     * span with a lower insertion order will be before a span with a higher insertion order.
     *
     * @param array Span array to be sorted.
     * @param priority Priorities of the spans
     * @param insertionOrder Insertion orders of the spans
     * @param <T> Span object type.
     */
    private <T> void sort(T[] array, int[] priority, int[] insertionOrder) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in sort");
        int size = array.length;
        for (int i = size / 2 - 1; i >= 0; i--) {
            siftDown(i, array, size, priority, insertionOrder);
        }

        for (int i = size - 1; i > 0; i--) {
            T v = array[0];
            int prio = priority[0];
            int insertOrder = insertionOrder[0];
            array[0] = array[i];
            priority[0] = priority[i];
            insertionOrder[0] = insertionOrder[i];
            siftDown(0, array, i, priority, insertionOrder);
            array[i] = v;
            priority[i] = prio;
            insertionOrder[i] = insertOrder;
        }
    }

    /**
     * Helper function for heap sort.
     *
     * @param index Index of the element to sift down.
     * @param array Span array to be sorted.
     * @param size Current heap size.
     * @param priority Priorities of the spans
     * @param insertionOrder Insertion orders of the spans
     * @param <T> Span object type.
     */
    private <T> void siftDown(int index, T[] array, int size, int[] priority,
                              int[] insertionOrder) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in siftDown");
        T v = array[index];
        int prio = priority[index];
        int insertOrder = insertionOrder[index];

        int left = 2 * index + 1;
        while (left < size) {
            if (left < size - 1 && compareSpans(left, left + 1, priority, insertionOrder) < 0) {
                left++;
            }
            if (compareSpans(index, left, priority, insertionOrder) >= 0) {
                break;
            }
            array[index] = array[left];
            priority[index] = priority[left];
            insertionOrder[index] = insertionOrder[left];
            index = left;
            left = 2 * index + 1;
        }
        array[index] = v;
        priority[index] = prio;
        insertionOrder[index] = insertOrder;
    }

    /**
     * Compare two span elements in an array. Comparison is based first on the priority flag of
     * the span, and then the insertion order of the span.
     *
     * @param left Index of the element to compare.
     * @param right Index of the other element to compare.
     * @param priority Priorities of the spans
     * @param insertionOrder Insertion orders of the spans
     * @return comparison result
     */
    private int compareSpans(int left, int right, int[] priority,
                             int[] insertionOrder) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in compareSpans");
        int priority1 = priority[left];
        int priority2 = priority[right];
        if (priority1 == priority2) {
            int x = insertionOrder[left];
            int y = insertionOrder[right];
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
        // since high priority has to be before a lower priority, the arguments to compare are
        // opposite of the insertion order check.
        return priority2 < priority1 ? -1 : 1;
    }

    /**
     * Return the next offset after <code>start</code> but less than or
     * equal to <code>limit</code> where a span of the specified type
     * begins or ends.
     */
    public int nextSpanTransition(int start, int limit, Class kind) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in nextSpanTransition");
        if (mSpanCount == 0) return limit;
        if (kind == null) {
            kind = Object.class;
        }
        return nextSpanTransitionRec(start, limit, kind, treeRoot());
    }

    private int nextSpanTransitionRec(int start, int limit, Class kind, int i) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in nextSpanTransitionRec");
        if ((i & 1) != 0) {
            // internal tree node
            int left = leftChild(i);
            if (resolveGap(mSpanMax[left]) > start) {
                limit = nextSpanTransitionRec(start, limit, kind, left);
            }
        }
        if (i < mSpanCount) {
            int st = resolveGap(mSpanStarts[i]);
            int en = resolveGap(mSpanEnds[i]);
            if (st > start && st < limit && kind.isInstance(mSpans[i]))
                limit = st;
            if (en > start && en < limit && kind.isInstance(mSpans[i]))
                limit = en;
            if (st < limit && (i & 1) != 0) {
                limit = nextSpanTransitionRec(start, limit, kind, rightChild(i));
            }
        }

        return limit;
    }

    /**
     * Return a new CharSequence containing a copy of the specified
     * range of this buffer, including the overlapping spans.
     */
    public CharSequence subSequence(int start, int end) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in subSequence");
        return new SpannableStringBuilder(this, start, end);
    }

    /**
     * Copy the specified range of chars from this buffer into the
     * specified array, beginning at the specified offset.
     */
    public void getChars(int start, int end, char[] dest, int destoff) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in getChars");
        checkRange("getChars", start, end);

        CharBuffer cb = _sb.getCharBuffer();
        if(cb == null)
            return;
        cb.clear();
        if (end <= mGapStart) {
            cb.position(start);
            cb.get(dest, destoff, end - start);
        } else if (start >= mGapStart) {
            cb.position(start + mGapLength);
            cb.get(dest, destoff, end - start);
        } else {
            cb.position(start);
            cb.get(dest, destoff, mGapStart - start).position(mGapStart + mGapLength);
            cb.get(dest, destoff + (mGapStart - start), end - mGapStart);
        }
    }

    private void sendBeforeTextChanged(TextWatcher[] watchers, int start, int before, int after) {
        int n = watchers.length;

        mTextWatcherDepth++;
        for (int i = 0; i < n; i++) {
            try
            {
                watchers[i].beforeTextChanged(this, start, before, after);
            }
            catch (Throwable e)
            {
                Logger.log(e);
            }
        }
        mTextWatcherDepth--;
    }

    private void sendTextChanged(TextWatcher[] watchers, int start, int before, int after) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in sendTextChanged");
        int n = watchers.length;

        mTextWatcherDepth++;
        for (int i = 0; i < n; i++) {
            try
            {
                watchers[i].onTextChanged(this, start, before, after);
            }
            catch (Throwable e)
            {
                Logger.log(e);
            }
        }
        mTextWatcherDepth--;
    }

    private void sendAfterTextChanged(TextWatcher[] watchers) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in sendAfterTextChanged");
        int n = watchers.length;

        mTextWatcherDepth++;
        for (int i = 0; i < n; i++) {
            try
            {
                watchers[i].afterTextChanged(this);
            }
            catch (Throwable e)
            {
                Logger.log(e);
            }
        }
        mTextWatcherDepth--;
    }

    private void sendSpanAdded(Object what, int start, int end) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in sendSpanAdded");
        SpanWatcher[] recip = getSpans(start, end, SpanWatcher.class);
        int n = recip.length;

        for (int i = 0; i < n; i++) {
            try
            {
                recip[i].onSpanAdded(this, what, start, end);
            }
            catch (Throwable e)
            {
                Logger.log(e);
            }
        }
    }

    private void sendSpanRemoved(Object what, int start, int end) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in sendSpanRemoved");
        SpanWatcher[] recip = getSpans(start, end, SpanWatcher.class);
        int n = recip.length;

        for (int i = 0; i < n; i++) {
            try
            {
                recip[i].onSpanRemoved(this, what, start, end);
            }
            catch (Throwable e)
            {
                Logger.log(e);
            }
        }
    }

    private void sendSpanChanged(Object what, int oldStart, int oldEnd, int start, int end) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in sendSpanChanged");
        // The bounds of a possible SpanWatcher are guaranteed to be set before this method is
        // called, so that the order of the span does not affect this broadcast.
        SpanWatcher[] spanWatchers = getSpans(Math.min(oldStart, start),
                Math.min(Math.max(oldEnd, end), length()), SpanWatcher.class);
        int n = spanWatchers.length;
        for (int i = 0; i < n; i++) {
            try
            {
                spanWatchers[i].onSpanChanged(this, what, oldStart, oldEnd, start, end);
            }
            catch (Throwable e)
            {
                Logger.log(e);
            }
        }
    }

    private static String region(int start, int end) {
        return "(" + start + " ... " + end + ")";
    }

    private void checkRange(final String operation, int start, int end) {
        if (end < start) {
            throw new IndexOutOfBoundsException(operation + " " +
                    region(start, end) + " has end before start");
        }

        int len = length();

        if (start > len || end > len) {
            throw new IndexOutOfBoundsException(operation + " " +
                    region(start, end) + " ends beyond length " + len);
        }

        if (start < 0 || end < 0) {
            throw new IndexOutOfBoundsException(operation + " " +
                    region(start, end) + " starts before 0");
        }
    }

    // Documentation from interface
    public void setFilters(InputFilter[] filters) {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in setFilters");
        if (filters == null) {
            throw new IllegalArgumentException();
        }

        mFilters = filters;
    }

    // Documentation from interface
    public InputFilter[] getFilters() {
        return mFilters;
    }

    // Primitives for treating span list as binary tree

    // The spans (along with start and end offsets and flags) are stored in linear arrays sorted
    // by start offset. For fast searching, there is a binary search structure imposed over these
    // arrays. This structure is inorder traversal of a perfect binary tree, a slightly unusual
    // but advantageous approach.

    // The value-containing nodes are indexed 0 <= i < n (where n = mSpanCount), thus preserving
    // logic that accesses the values as a contiguous array. Other balanced binary tree approaches
    // (such as a complete binary tree) would require some shuffling of node indices.

    // Basic properties of this structure: For a perfect binary tree of height m:
    // The tree has 2^(m+1) - 1 total nodes.
    // The root of the tree has index 2^m - 1.
    // All leaf nodes have even index, all interior nodes odd.
    // The height of a node of index i is the number of trailing ones in i's binary representation.
    // The left child of a node i of height h is i - 2^(h - 1).
    // The right child of a node i of height h is i + 2^(h - 1).

    // Note that for arbitrary n, interior nodes of this tree may be >= n. Thus, the general
    // structure of a recursive traversal of node i is:
    // * traverse left child if i is an interior node
    // * process i if i < n
    // * traverse right child if i is an interior node and i < n

    private int treeRoot() {
        return Integer.highestOneBit(mSpanCount) - 1;
    }

    // (i+1) & ~i is equal to 2^(the number of trailing ones in i)
    private static int leftChild(int i) {
        return i - (((i + 1) & ~i) >> 1);
    }

    private static int rightChild(int i) {
        return i + (((i + 1) & ~i) >> 1);
    }

    // The span arrays are also augmented by an mSpanMax[] array that represents an interval tree
    // over the binary tree structure described above. For each node, the mSpanMax[] array contains
    // the maximum value of mSpanEnds of that node and its descendants. Thus, traversals can
    // easily reject subtrees that contain no spans overlapping the area of interest.

    // Note that mSpanMax[] also has a valid valuefor interior nodes of index >= n, but which have
    // descendants of index < n. In these cases, it simply represents the maximum span end of its
    // descendants. This is a consequence of the perfect binary tree structure.
    private int calcMax(int i) {
        int max = 0;
        if ((i & 1) != 0) {
            // internal tree node
            max = calcMax(leftChild(i));
        }
        if (i < mSpanCount) {
            max = Math.max(max, mSpanEnds[i]);
            if ((i & 1) != 0) {
                max = Math.max(max, calcMax(rightChild(i)));
            }
        }
        mSpanMax[i] = max;
        return max;
    }

    // restores binary interval tree invariants after any mutation of span structure
    private void restoreInvariants() {
        if(VERBOSE_LOG) Logger.debug(TAG + ": in restoreInvariants");
        if (mSpanCount == 0) return;

        // invariant 1: span starts are nondecreasing

        // This is a simple insertion sort because we expect it to be mostly sorted.
        for (int i = 1; i < mSpanCount; i++) {
            if (mSpanStarts[i] < mSpanStarts[i - 1]) {
                Object span = mSpans[i];
                int start = mSpanStarts[i];
                int end = mSpanEnds[i];
                int flags = mSpanFlags[i];
                int insertionOrder = mSpanOrder[i];
                int j = i;
                do {
                    mSpans[j] = mSpans[j - 1];
                    mSpanStarts[j] = mSpanStarts[j - 1];
                    mSpanEnds[j] = mSpanEnds[j - 1];
                    mSpanFlags[j] = mSpanFlags[j - 1];
                    mSpanOrder[j] = mSpanOrder[j - 1];
                    j--;
                } while (j > 0 && start < mSpanStarts[j - 1]);
                mSpans[j] = span;
                mSpanStarts[j] = start;
                mSpanEnds[j] = end;
                mSpanFlags[j] = flags;
                mSpanOrder[j] = insertionOrder;
                invalidateIndex(j);
            }
        }

        // invariant 2: max is max span end for each node and its descendants
        calcMax(treeRoot());

        // invariant 3: mIndexOfSpan maps spans back to indices
        if (mIndexOfSpan == null) {
            mIndexOfSpan = new IdentityHashMap<>();
        }
        for (int i = mLowWaterMark; i < mSpanCount; i++) {
            Integer existing = mIndexOfSpan.get(mSpans[i]);
            if (existing == null || existing != i) {
                mIndexOfSpan.put(mSpans[i], i);
            }
        }
        mLowWaterMark = Integer.MAX_VALUE;
    }

    // Call this on any update to mSpans[], so that mIndexOfSpan can be updated
    private void invalidateIndex(int i) {
        mLowWaterMark = Math.min(i, mLowWaterMark);
    }

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private InputFilter[] mFilters = NO_FILTERS;

    private int mGapStart;
    private int mGapLength;

    private Object[] mSpans;
    private int[] mSpanStarts;
    private int[] mSpanEnds;
    private int[] mSpanMax;  // see calcMax() for an explanation of what this array stores
    private int[] mSpanFlags;
    private int[] mSpanOrder;  // store the order of span insertion
    private int mSpanInsertCount;  // counter for the span insertion
    private int[] mPrioSortBuffer;  // buffer used to sort getSpans result
    private int[] mOrderSortBuffer;  // buffer used to sort getSpans result

    private int mSpanCount;
    private IdentityHashMap<Object, Integer> mIndexOfSpan;
    private int mLowWaterMark;  // indices below this have not been touched

    // TextWatcher callbacks may trigger changes that trigger more callbacks. This keeps track of
    // how deep the callbacks go.
    private int mTextWatcherDepth;

    private static final int MARK = 1;
    private static final int POINT = 2;
    private static final int PARAGRAPH = 3;

    private static final int START_MASK = 0xF0;
    private static final int END_MASK = 0x0F;
    private static final int START_SHIFT = 4;

    // These bits are not (currently) used by SPANNED flags
    private static final int SPAN_ADDED = 0x800;
    private static final int SPAN_START_AT_START = 0x1000;
    private static final int SPAN_START_AT_END = 0x2000;
    private static final int SPAN_END_AT_START = 0x4000;
    private static final int SPAN_END_AT_END = 0x8000;
    private static final int SPAN_START_END_MASK = 0xF000;

}
