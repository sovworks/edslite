package com.sovworks.eds.android.settings.program;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import com.sovworks.eds.android.Logger;
import com.sovworks.eds.android.R;
import com.sovworks.eds.android.providers.ContainersDocumentProvider;
import com.sovworks.eds.android.settings.ChoiceDialogPropertyEditor;
import com.sovworks.eds.android.settings.UserSettings;
import com.sovworks.eds.android.settings.fragments.ProgramSettingsFragmentBase;
import com.sovworks.eds.settings.Settings;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static com.sovworks.eds.android.settings.UserSettings.EXTERNAL_FILE_MANAGER;

@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
public class ExtFileManagerPropertyEditor extends ChoiceDialogPropertyEditor
{

    public static void saveExtInfo(UserSettings settings, Settings.ExternalFileManagerInfo info)
    {
        if(info == null)
            settings.getSharedPreferences().edit().remove(EXTERNAL_FILE_MANAGER).commit();
        else
        {
            try
            {
                settings.getSharedPreferences().edit().putString(EXTERNAL_FILE_MANAGER, info.save()).commit();
            }
            catch (JSONException e)
            {
                Logger.log(e);
            }
        }
    }

    public ExtFileManagerPropertyEditor(ProgramSettingsFragmentBase f)
    {
        super(
                f,
                R.string.use_external_file_manager,
                R.string.use_external_file_manager_desc,
                f.getTag()
        );
    }

    @Override
    public ProgramSettingsFragmentBase getHost()
    {
        return (ProgramSettingsFragmentBase) super.getHost();
    }

    @Override
    public void load()
    {
        if(!getHost().getPropertiesView().isPropertyEnabled(getId()))
            return;
        loadExtBrowserInfo();
        loadChoiceStrings();
        super.load();
    }

    @Override
    protected int loadValue()
    {
        return getSelectionFromExtFMInfo(
                getHost().getSettings().getExternalFileManagerInfo()
        );
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void saveValue(int value)
    {
        Settings.ExternalFileManagerInfo info = getExtFMInfoFromSelection(value);
        saveExtInfo(getHost().getSettings(), info);
    }

    @Override
    protected List<String> getEntries()
    {
        return _choiceStrings;
    }

    private static class ExternalBrowserInfo
    {
        ResolveInfo resolveInfo;
        public String action, mime, label;

        @Override
        public String toString()
        {
            return label;
        }

    }

    private final ArrayList<ExternalBrowserInfo> _extBrowserInfo = new ArrayList<>();
    private final ArrayList<String> _choiceStrings = new ArrayList<>();

    private void loadExtBrowserInfo()
    {
        _extBrowserInfo.clear();

        Uri testPath = Uri.fromFile(getHost().getContext().getFilesDir());
        addMatches(_extBrowserInfo, Intent.ACTION_VIEW, testPath, "resource/folder");
        addMatches(_extBrowserInfo, Intent.ACTION_MEDIA_MOUNTED, testPath, null);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            //Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            //i.addCategory(Intent.CATEGORY_OPENABLE);
            //addMatches(_extBrowserInfo, i);
            testPath = DocumentsContract.buildTreeDocumentUri(
                    ContainersDocumentProvider.AUTHORITY,
                    "id"
            );
            addMatches(_extBrowserInfo, Intent.ACTION_VIEW, testPath, DocumentsContract.Document.MIME_TYPE_DIR);
            //addMatches(_extBrowserInfo, Intent.ACTION_VIEW, testPath, "resource/folder");
        }
    }

    private void loadChoiceStrings()
    {
        _choiceStrings.clear();
        _choiceStrings.add(getHost().getString(R.string.builtin_file_manager));
        for(ExternalBrowserInfo i: _extBrowserInfo)
            _choiceStrings.add(i.label);
    }

    private int getSelectionFromExtFMInfo(Settings.ExternalFileManagerInfo info)
    {
        if(info != null)
            for(int i=0;i<_extBrowserInfo.size();i++)
            {
                ExternalBrowserInfo item = _extBrowserInfo.get(i);
                if(info.packageName.equals(item.resolveInfo.activityInfo.packageName) &&
                        info.className.equals(item.resolveInfo.activityInfo.name) &&
                        info.action.equals(item.action) &&
                        info.mimeType.equals(item.mime))
                    return i+1;
            }
        return 0;
    }

    private Settings.ExternalFileManagerInfo getExtFMInfoFromSelection(int selection)
    {
        int idx = selection - 1;
        if(idx<0 || idx>=_extBrowserInfo.size())
            return null;

        ExternalBrowserInfo item = _extBrowserInfo.get(idx);
        Settings.ExternalFileManagerInfo res = new Settings.ExternalFileManagerInfo();
        res.packageName = item.resolveInfo.activityInfo.packageName;
        res.className = item.resolveInfo.activityInfo.name;
        res.action = item.action;
        res.mimeType = item.mime;
        return res;
    }

    private void addMatches(List<ExternalBrowserInfo> matches,String action,Uri data,String mime)
    {
        final Intent intent = new Intent(action);
        if(data!=null && mime!=null)
            intent.setDataAndType(data, mime);
        else if(data!=null)
            intent.setData(data);
        else if(mime!=null)
            intent.setType(mime);
        addMatches(matches, intent);

    }

    private void addMatches(List<ExternalBrowserInfo> matches, Intent intent)
    {
        String ignoredPackage = getHost().getContext().getApplicationContext().getPackageName();
        PackageManager pacMan = getHost().getContext().getPackageManager();
        final List<ResolveInfo> allMatches = pacMan.queryIntentActivities(intent, 0);
        for (ResolveInfo match : allMatches)
        {
            if(match.activityInfo!=null && !match.activityInfo.applicationInfo.packageName.equals(ignoredPackage) && !isFileManagerAdded(matches, match))
            {
                ExternalBrowserInfo eb = new ExternalBrowserInfo();
                eb.resolveInfo = match;
                eb.action = intent.getAction();
                eb.mime = intent.getType() == null ? "" : intent.getType();
                eb.label = match.loadLabel(pacMan).toString();
                if(intent.getData()!=null && ContentResolver.SCHEME_CONTENT.equals(intent.getData().getScheme()))
                    eb.label += " (content provider browser)";
                matches.add(eb);
            }
        }
    }

    private boolean isFileManagerAdded(List<ExternalBrowserInfo> matches,ResolveInfo m)
    {
        for (ExternalBrowserInfo match : matches)
        {
            if(match.resolveInfo.activityInfo.packageName.equals(m.activityInfo.packageName) && match.resolveInfo.activityInfo.name.equals(m.activityInfo.name))
                return true;
        }
        return false;
    }
}
