package com.example.dyang.galleryphotolist;

import android.database.Cursor;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.Log;
import android.graphics.Bitmap;
import android.widget.CursorAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.os.AsyncTask;

import java.util.HashSet;

public class PhotoWallAdapter extends CursorAdapter {

    //use LruCache to cache the thumbnails
    private ImagesCache mImagesCache;

    private Context mContext;

    private HashSet<AsyncLoadThumbnail> mLoadThumbnailTasks = new HashSet<>();

    public PhotoWallAdapter(Context context) {
        super(context, null, 0);  //flag = 0 1:FLAG_AUTO_REQUERY 2:FLAG_REGISTER_CONTENT_OBSERVER

        mContext = context;
        mImagesCache = new ImagesCache(mContext);

        Log.i("cache size", String.valueOf(mImagesCache.size()));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(mContext).inflate(R.layout.photo_layout, null);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ImageView photo = (ImageView)view.findViewById(R.id.photo);

        //if has a older task, cancel it.
        AsyncLoadThumbnail olderTask = null;
        if (photo.getTag() != null) {
            try {
                olderTask = (AsyncLoadThumbnail)photo.getTag();
            }
            catch (Exception ex) {
                olderTask = null;
            }
        }

        if (olderTask != null) {
            olderTask.cancel(false);
        }

        long imageID = this.getCursor().getLong(0);
        Bitmap bitmap = mImagesCache.get(String.valueOf(imageID));
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
        } else {

            photo.setImageBitmap(null);

            //load thumbnail
            AsyncLoadThumbnail task = new AsyncLoadThumbnail(photo);
            photo.setTag(task);
            mLoadThumbnailTasks.add(task);
            task.execute(imageID);
        }
    }

    public void updateImagesCache(String key, Bitmap bitmap) {
        if (mImagesCache.get(key) == null) {
            mImagesCache.put(key, bitmap);
        }
    }


    /**
     * Cancel all the tasks
     */
    public void cancelLoadThumbnailTasks() {
        if (mLoadThumbnailTasks != null) {
            for (AsyncLoadThumbnail task : mLoadThumbnailTasks) {
                task.cancel(false);
            }
        }
    }

    // evict all cache
    public void evilAllCache() {
        mImagesCache.evictAll();
    }

    //trim to half memory size
    public void trimCache() {
        //mImagesCache.trimToSize(mImagesCache.size() / 2);
    }

    // asynchronous task load thumbnail
    private class AsyncLoadThumbnail extends AsyncTask <Long, Void, Bitmap> {

        Long mImageID;
        ImageView mPhoto;

        public AsyncLoadThumbnail(ImageView photo) {
            super();
            mPhoto = photo;
        }

        @Override
         protected Bitmap doInBackground(Long... imageIDs) {
            mImageID = imageIDs[0];
            Bitmap thumbnail = Thumbnails.getThumbnail(mContext.getContentResolver(),
                                                       mImageID,
                                                       Thumbnails.MICRO_KIND,
                                                       null);
            return thumbnail;
         }

         @Override
         protected void onPostExecute(Bitmap bitmap) {
             if (bitmap == null) {
                 Log.i("async task", "Can't find the thumbnail. ImageID:" + String.valueOf(mImageID));
                 return;
             }

             //the GridView is scrolling
             if (mPhoto.getTag() != this) {
                 return;
             }

             mPhoto.setImageBitmap(bitmap);
             //reset tag to null, identify the task is done
             mPhoto.setTag(null);
             //add to cache
             PhotoWallAdapter.this.updateImagesCache(String.valueOf(mImageID), bitmap);

             mLoadThumbnailTasks.remove(this);
         }

    }

}
