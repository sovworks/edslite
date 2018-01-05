package com.sovworks.eds.android.filemanager.records;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.helpers.ExtendedFileInfoLoader;
import com.sovworks.eds.android.helpers.Util;
import com.sovworks.eds.android.service.FileOpsService;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.fs.Path;
import com.sovworks.eds.fs.util.StringPathUtil;
import com.sovworks.eds.settings.GlobalConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class FileRecord extends FsBrowserRecord
{

    public static class ExtFileInfo implements ExtendedFileInfoLoader.ExtendedFileInfo
    {
        Drawable mainIcon;

        @Override
        public void attach(BrowserRecord record)
        {
            FileRecord fr = (FileRecord) record;
            _records.add(fr);
            fr._mainIcon = mainIcon;
            fr._needLoadExtInfo = false;
            FileRecord.updateRowView(fr.getHostFragment(), fr);
        }

        @Override
        public void detach(BrowserRecord record)
        {
            _records.remove(record);
        }

        @Override
        public void clear()
        {
            for(BrowserRecord r: _records)
            {
                FileRecord fr = (FileRecord)r;
                RowViewInfo rvi = FileRecord.getCurrentRowViewInfo(fr.getHostFragment(), fr);
                if(rvi!=null)
                {
                    ImageView iv = rvi.view.findViewById(android.R.id.icon);
                    iv.setImageDrawable(null);
                    iv.setImageBitmap(null);
                    FileRecord.updateRowView(rvi);
                }
            }

            //if(mainIcon instanceof BitmapDrawable)
            //{
            //   Bitmap b = ((BitmapDrawable) mainIcon).getBitmap();
            //    if(b!=null)
            //        b.recycle();
            //}
            mainIcon = null;
        }

        private final List<BrowserRecord> _records = new ArrayList<>();
    }

	public FileRecord(Context context)
	{
		super(context);
        _loadPreviews = _needLoadExtInfo = UserSettings.getSettings(context).showPreviews();
        DisplayMetrics dm = _context.getResources().getDisplayMetrics();
        _iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GlobalConfig.FB_PREVIEW_WIDTH, dm);
        _iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, GlobalConfig.FB_PREVIEW_HEIGHT, dm);

	}

    @Override
    public void init(Path path) throws IOException
    {
        super.init(path);
        updateFileInfoString();
    }

    @Override
    public boolean allowSelect()
	{
        return _host.allowFileSelect();
	}

    @Override
    public void updateView(View view, final int position)
    {
        super.updateView(view, position);
        TextView tv = view.findViewById(android.R.id.text2);
        if(_infoString != null)
        {
            tv.setVisibility(View.VISIBLE);
            tv.setText(_infoString);
        }
        else
            tv.setVisibility(View.INVISIBLE);

        ImageView iv = view.findViewById(android.R.id.icon);
        if(_mainIcon != null)
        {
            //iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageDrawable(_mainIcon);
            if(_animateIcon)
            {
                iv.startAnimation(AnimationUtils.loadAnimation(_context, R.anim.restore));
                _animateIcon = false;
            }
        }
        //DisplayMetrics dm = _mainActivity.getResources().getDisplayMetrics();
		//_iconWidth = iv.getMeasuredWidth();
        //if(_iconWidth == 0)
        //    _iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Preferences.FB_PREVIEW_WIDTH, dm);
	    //_iconHeight = iv.getMeasuredHeight();
        //if(_iconHeight == 0)
        //    _iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Preferences.FB_PREVIEW_HEIGHT, dm);
    }

    @Override
    public ExtendedFileInfoLoader.ExtendedFileInfo loadExtendedInfo()
    {
        ExtFileInfo res = new ExtFileInfo();
        initExtFileInfo(res);
        return res;
    }

    @Override
    public boolean needLoadExtendedInfo()
    {
        return _needLoadExtInfo;
    }

    protected boolean _needLoadExtInfo;

	@Override
	protected Drawable getDefaultIcon()
	{
		return getFileIcon(_host);
	}

    protected void updateFileInfoString()
    {
        if(_path!=null)
        {
            try
            {
                _infoString = formatInfoString(_context);
            }
            catch(IOException ignored)
            {

            }
        }
        else
            _infoString = null;
    }

    protected String formatInfoString(Context context) throws IOException
	{
        StringBuilder sb = new StringBuilder();
        appendSizeInfo(context, sb);
        appendModDataInfo(context, sb);
        return sb.toString();
	}

    protected void appendSizeInfo(Context context, StringBuilder sb)
    {
        sb.append(String.format("%s: %s",
                context.getText(R.string.size),
                Formatter.formatFileSize(context, getSize())
        ));
    }

    protected void appendModDataInfo(Context context, StringBuilder sb)
    {
        Date md = getModificationDate();
        if(md!=null)
        {
            java.text.DateFormat df = android.text.format.DateFormat.getDateFormat(context);
            java.text.DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
            sb.append(String.format(" %s: %s %s",
                    context.getText(R.string.last_modified),
                    df.format(md),
                    tf.format(md)
            ));
        }
    }

    protected void initExtFileInfo(ExtFileInfo info)
    {
        if(_loadPreviews)
            info.mainIcon = loadMainIcon();
    }

    protected Drawable loadMainIcon()
    {
        String mime = FileOpsService.getMimeTypeFromExtension(_context, new StringPathUtil(getName()).getFileExtension());
        Drawable res = null;
        try
        {
            res = mime.startsWith("image/") ? getImagePreview(_path) : getDefaultAppIcon(mime);
            _animateIcon = true;
        }
        catch (IOException e)
        {
            Logger.log(e);
        }
        return res;
    }

    protected Drawable getImagePreview(Path path) throws IOException
    {
        Bitmap bitmap = Util.loadDownsampledImage(path, _iconWidth, _iconHeight);
        return bitmap!=null ? new BitmapDrawable(_context.getResources(), bitmap) : null;
    }

    private static Drawable _fileIcon;

    private static synchronized Drawable getFileIcon(Context context)
    {
        if(_fileIcon == null && context!=null)
        {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.fileIcon, typedValue, true);
            //noinspection deprecation
            _fileIcon = context.getResources().getDrawable(typedValue.resourceId);
        }
        return _fileIcon;
    }
    private int _iconWidth = 40, _iconHeight = 40;
    private String _infoString;
    private Drawable _mainIcon;

    private boolean _animateIcon, _loadPreviews;

    private Drawable getDefaultAppIcon(String mime)
    {
        if(mime.equals("*/*"))
            return null;
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType(mime);

        PackageManager pacMan = _context.getPackageManager();
        try
        {
            final List<ResolveInfo> matches = pacMan.queryIntentActivities(intent, 0);
            for (ResolveInfo match : matches)
            {
                final Drawable icon = match.loadIcon(pacMan);
                if (icon != null)
                    return icon;//drawableToBitmap(icon);
            }
        }
        catch(NullPointerException ignored)
        {
            //bug?
            //java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String android.net.Uri.getHost()' on a null object reference
	        //at android.os.Parcel.readException(Parcel.java:1552)
        }
        return null;
    }

    /*
    private Bitmap drawableToBitmap(Drawable d)
    {
        if (d instanceof BitmapDrawable)
            return ((BitmapDrawable)d).getBitmap();

        Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        return bitmap;
    }
    */

}