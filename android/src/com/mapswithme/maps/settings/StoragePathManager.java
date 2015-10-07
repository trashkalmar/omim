package com.mapswithme.maps.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import com.mapswithme.maps.*;
import com.mapswithme.util.Constants;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.concurrency.ThreadPool;
import com.mapswithme.util.concurrency.UiThread;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StoragePathManager
{
  static final String[] MOVABLE_EXTS = Framework.nativeGetMovableFilesExts();
  static final FilenameFilter MOVABLE_FILES_FILTER = new FilenameFilter()
  {
    @Override
    public boolean accept(File dir, String filename)
    {
      for (String ext : MOVABLE_EXTS)
        if (filename.endsWith(ext))
          return true;

      return false;
    }
  };

  public interface MoveFilesListener
  {
    void moveFilesFinished(String newPath);

    void moveFilesFailed(int errorCode);
  }

  public static final int NO_ERROR = 0;
  public static final int UNKNOWN_LITE_PRO_ERROR = 1;
  public static final int IOEXCEPTION_ERROR = 2;
  public static final int NULL_ERROR = 4;
  public static final int NOT_A_DIR_ERROR = 5;
  public static final int UNKNOWN_KITKAT_ERROR = 6;

  static final String TAG = StoragePathManager.class.getName();

  private static final String IS_KITKAT_MIGRATION_COMPLETED = "KitKatMigrationCompleted";

  private BroadcastReceiver mExternalReceiver;
  private BroadcastReceiver mInternalReceiver;
  private Activity mActivity;
  private ArrayList<StorageItem> mItems;
  private int mCurrentStorageIndex = -1;
  private MoveFilesListener mStorageListener;

  /**
   * Observes status of connected media and retrieves list of available external storages.
   */
  public void startExternalStorageWatching(Activity activity, @Nullable BroadcastReceiver receiver, @Nullable MoveFilesListener listener)
  {
    mActivity = activity;
    mStorageListener = listener;
    mExternalReceiver = receiver;
    mInternalReceiver = new BroadcastReceiver()
    {
      @Override
      public void onReceive(Context context, Intent intent)
      {
        if (mExternalReceiver != null)
          mExternalReceiver.onReceive(context, intent);

        updateExternalStorages();
      }
    };

    mActivity.registerReceiver(mInternalReceiver, getMediaChangesIntentFilter());
    updateExternalStorages();
  }

  public static IntentFilter getMediaChangesIntentFilter()
  {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    filter.addAction(Intent.ACTION_MEDIA_REMOVED);
    filter.addAction(Intent.ACTION_MEDIA_EJECT);
    filter.addAction(Intent.ACTION_MEDIA_SHARED);
    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
    filter.addAction(Intent.ACTION_MEDIA_CHECKING);
    filter.addAction(Intent.ACTION_MEDIA_NOFS);
    filter.addDataScheme(ContentResolver.SCHEME_FILE);

    return filter;
  }

  public void stopExternalStorageWatching()
  {
    if (mInternalReceiver != null)
    {
      mActivity.unregisterReceiver(mInternalReceiver);
      mInternalReceiver = null;
      mExternalReceiver = null;
    }
  }

  public boolean hasMoreThanOneStorage()
  {
    return mItems.size() > 1;
  }

  public ArrayList<StorageItem> getStorageItems()
  {
    return mItems;
  }

  public int getCurrentStorageIndex()
  {
    return mCurrentStorageIndex;
  }

  public void updateExternalStorages()
  {
    List<String> pathsFromConfig = new ArrayList<>();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
      StorageUtils.parseKitkatStorages(pathsFromConfig);
    else
      StorageUtils.parseStorages(pathsFromConfig);

    mItems = new ArrayList<>();

    final StorageItem currentStorage = buildStorageItem(StorageUtils.getWritableDirRoot());
    addStorageItem(currentStorage);
    addStorageItem(buildStorageItem(Environment.getExternalStorageDirectory().getAbsolutePath()));
    for (String path : pathsFromConfig)
      addStorageItem(buildStorageItem(path));

    mCurrentStorageIndex = mItems.indexOf(currentStorage);

    if (mCurrentStorageIndex == -1)
    {
      Log.w(TAG, "Unrecognized current path : " + currentStorage);
      Log.w(TAG, "Parsed paths : ");
      for (StorageItem item : mItems)
        Log.w(TAG, item.toString());
    }
  }

  private void addStorageItem(StorageItem item)
  {
    if (item != null && !mItems.contains(item))
      mItems.add(item);
  }

  private static StorageItem buildStorageItem(String path)
  {
    try
    {
      final File f = new File(path + "/");
      if (f.exists() && f.isDirectory() && f.canWrite() && StorageUtils.isDirWritable(path))
      {
        final long freeSize = StorageUtils.getFreeBytesAtPath(path);
        if (freeSize > 0)
        {
          Log.i(TAG, "Storage found : " + path + ", size : " + freeSize);
          return new StorageItem(path, freeSize);
        }
      }
    } catch (final IllegalArgumentException ex)
    {
      Log.i(TAG, "Can't build storage for path : " + path);
    }

    return null;
  }

  protected void changeStorage(int newIndex)
  {
    final StorageItem oldItem = (mCurrentStorageIndex != -1) ? mItems.get(mCurrentStorageIndex) : null;
    final StorageItem item = mItems.get(newIndex);
    final String path = item.getFullPath();

    final File f = new File(path);
    if (!f.exists() && !f.mkdirs())
    {
      Log.e(TAG, "Can't create directory: " + path);
      return;
    }

    new AlertDialog.Builder(mActivity)
        .setCancelable(false)
        .setTitle(R.string.move_maps)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dlg, int which)
          {
            setStoragePath(mActivity, new MoveFilesListener()
            {
              @Override
              public void moveFilesFinished(String newPath)
              {
                updateExternalStorages();
                if (mStorageListener != null)
                  mStorageListener.moveFilesFinished(newPath);
              }

              @Override
              public void moveFilesFailed(int errorCode)
              {
                updateExternalStorages();
                if (mStorageListener != null)
                  mStorageListener.moveFilesFailed(errorCode);
              }
            }, item, oldItem, R.string.wait_several_minutes);

            dlg.dismiss();
          }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dlg, int which)
          {
            dlg.dismiss();
          }
        }).create().show();
  }

  /**
   * Checks whether current directory is actually writable on Kitkat devices. On earlier versions of android ( < 4.4 ) the root of external
   * storages was writable, but on Kitkat it isn't, so we should move our maps to other directory.
   * http://www.doubleencore.com/2014/03/android-external-storage/ check that link for explanations
   */
  private void checkExternalStoragePathOnKitkat(Context context, MoveFilesListener listener)
  {
    final String settingsDir = Framework.nativeGetSettingsDir();
    final String writableDir = Framework.nativeGetWritableDir();

    if (settingsDir.equals(writableDir) || StorageUtils.isDirWritable(writableDir))
      return;

    final long size = StorageUtils.getWritableDirSize();
    updateExternalStorages();
    for (StorageItem item : mItems)
    {
      if (item.mFreeSize > size)
      {
        setStoragePath(context, listener, item, new StorageItem(StorageUtils.getWritableDirRoot(), 0),
                       R.string.kitkat_optimization_in_progress);
        return;
      }
    }

    listener.moveFilesFailed(UNKNOWN_KITKAT_ERROR);
  }

  /**
   * Checks if data (mwms, routing, indeces etc) is located on external storages.
   * <p/>
   * Data should be placed in private app directory on Kitkat+ devices, hence root of sdcard isn't writable anymore there.
   */
  public void checkKitkatMigration(final Activity activity)
  {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
      return;

    if (MwmApplication.get().nativeGetBoolean(IS_KITKAT_MIGRATION_COMPLETED, false))
      return;

    checkExternalStoragePathOnKitkat(activity, new MoveFilesListener()
                                     {
                                       @Override
                                       public void moveFilesFinished(String newPath)
                                       {
                                         MwmApplication.get().nativeSetBoolean(IS_KITKAT_MIGRATION_COMPLETED, true);
                                         UiUtils.showAlertDialog(activity, R.string.kitkat_migrate_ok);
                                       }

                                       @Override
                                       public void moveFilesFailed(int errorCode)
                                       {
                                         UiUtils.showAlertDialog(activity, R.string.kitkat_migrate_failed);
                                       }
                                     }
                                    );
  }

  private void setStoragePath(final Context context, final MoveFilesListener listener, final StorageItem newStorage,
                              final StorageItem oldStorage, final int messageId)
  {
    final ProgressDialog dialog = new ProgressDialog(context);
    dialog.setMessage(context.getString(messageId));
    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    dialog.setIndeterminate(true);
    dialog.setCancelable(false);
    dialog.show();

    ThreadPool.getStorage().execute(new Runnable()
    {
      @Override
      public void run()
      {
        final int result = changeStorage(newStorage, oldStorage);

        UiThread.run(new Runnable()
        {
          @Override
          public void run()
          {
            if (dialog.isShowing())
              dialog.dismiss();

            if (result == NO_ERROR)
              listener.moveFilesFinished(newStorage.mPath);
            else
              listener.moveFilesFailed(result);

            updateExternalStorages();
          }
        });
      }
    });
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static int changeStorage(StorageItem newStorage, StorageItem oldStorage)
  {
    final String fullNewPath = newStorage.getFullPath();

    // According to changeStorage code above, oldStorage can be null.
    if (oldStorage == null)
    {
      Log.w(TAG, "Old storage path is null. New path is: " + fullNewPath);
      return NULL_ERROR;
    }

    final File oldDir = new File(oldStorage.getFullPath());
    final File newDir = new File(fullNewPath);
    if (!newDir.exists())
      newDir.mkdir();

    if (BuildConfig.DEBUG)
    {
      if (!newDir.isDirectory())
        throw new IllegalStateException("Cannot move maps. New path is not a directory. New path : " + newDir);
      if (!oldDir.isDirectory())
        throw new IllegalStateException("Cannot move maps. Old path is not a directory. Old path : " + oldDir);
      if (!StorageUtils.isDirWritable(fullNewPath))
        throw new IllegalStateException("Cannot move maps. New path is not writable. New path : " + fullNewPath);
    }

    List<String> relPaths = new ArrayList<>();
    StorageUtils.listFilesRecursively(oldDir, "", MOVABLE_FILES_FILTER, relPaths);

    File[] oldFiles = new File[relPaths.size()];
    File[] newFiles = new File[relPaths.size()];
    for (int i = 0; i < relPaths.size(); ++i)
    {
      oldFiles[i] = new File(oldDir.getAbsolutePath() + File.separator + relPaths.get(i));
      newFiles[i] = new File(newDir.getAbsolutePath() + File.separator + relPaths.get(i));
    }

    try
    {
      for (int i = 0; i < oldFiles.length; ++i)
      {
        File parent = newFiles[i].getParentFile();
        if (parent != null)
          parent.mkdirs();
        StorageUtils.copyFile(oldFiles[i], newFiles[i]);
      }
    } catch (IOException e)
    {
      e.printStackTrace();
      // In the case of failure delete all new files.  Old files will
      // be lost if new files were just moved from old locations.
      StorageUtils.removeFilesInDirectory(newDir, newFiles);
      return IOEXCEPTION_ERROR;
    }

    UiThread.run(new Runnable()
    {
      @Override
      public void run()
      {
        Framework.nativeSetWritableDir(fullNewPath);
        MwmApplication.prefs().edit()
                      .putString(Constants.STORAGE_PATH_PREF, fullNewPath)
                      .apply();
      }
    });

    // Delete old files because new files were successfully created.
    StorageUtils.removeFilesInDirectory(oldDir, oldFiles);
    return NO_ERROR;
  }
}
