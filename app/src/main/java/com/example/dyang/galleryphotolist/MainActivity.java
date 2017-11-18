package com.example.dyang.galleryphotolist;

import android.content.CursorLoader;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView.RecyclerListener;
import android.widget.GridView;
import android.content.Loader;
import android.app.LoaderManager;
import android.database.Cursor;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends ActionBarActivity {

    //adapter for GridView
    private PhotoWallAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView photoWall;
        photoWall = (GridView)this.findViewById(R.id.photo_wall);

        photoWall.setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                ImageView imageView = (ImageView)view.findViewById(R.id.photo);
                imageView.setImageBitmap(null);
            }
        });

        mAdapter = new PhotoWallAdapter(this);
        photoWall.setAdapter(mAdapter);

        //trigger to load photos
        CameraPhotosLoader photosLoader = new CameraPhotosLoader();
        this.getLoaderManager().initLoader(1, null, photosLoader);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.swapCursor(null);
        //cancel all the tasks
        mAdapter.cancelLoadThumbnailTasks();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.i("onTrimMemory", "onTrimMemory() with level=" + level);

        // Memory we can release here will help overall system performance, and
        // make us a smaller target as the system looks for memory

        if (level >= TRIM_MEMORY_MODERATE) { // 60
            // Nearing middle of list of cached background apps; evict our
            // entire thumbnail cache
            Log.i("onTrimMemory", "evicting entire thumbnail cache");
            mAdapter.evilAllCache();

        } else if (level >= TRIM_MEMORY_BACKGROUND) { // 40
            // Entering list of cached background apps; evict oldest half of our
            // thumbnail cache
            Log.i("onTrimMemory", "evicting oldest half of thumbnail cache");
            mAdapter.trimCache();
        }
    }

    /**
     * implement the loader callback interface
     * load camera photos
     */
    private class CameraPhotosLoader implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

            /*
            Date bDate;
            try {
                bDate = new SimpleDateFormat("yyyy-MM-dd").parse("2015-06-14");
            } catch (Exception e) {
                return null;
            }

            Log.i("Begin Date", bDate.toString());
            */

            try {
                return new CursorLoader(MainActivity.this,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA },
                        null, ///*MediaStore.Images.Media.DATA + "like ",//*/MediaStore.Images.Media.DATE_TAKEN + ">?",
                        null, ///*new String[] { "*\\Camera\\*" },//*/new String[] { String.valueOf(bDate.getTime()) },
                        MediaStore.Images.Media.DATE_TAKEN + " DESC");
            } catch (Exception e) {
                Log.i("error", e.getMessage());
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.i("Total number of photos:", String.valueOf(cursor.getCount()));
            mAdapter.swapCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // TODO Auto-generated method stub
            mAdapter.swapCursor(null);
        }
    }
}
