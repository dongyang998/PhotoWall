package com.example.dyang.galleryphotolist;

import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.AbsListView.OnScrollListener;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.AbsListView;
import android.os.AsyncTask;

import java.util.HashSet;

/**
 * Created by DYang on 4/26/2015
 */
public class PhotoWallAdapter2 extends CursorAdapter implements OnScrollListener {

    /**
     * 图片缓存技术的核心类，用于缓存所有下载好的图片，在程序内存达到设定值时会将最少最近使用的图片移除掉。
     */
    private LruCache<String, Bitmap> mImagesCache;

    /**
     * GridView的实例
     */
    private GridView mPhotoWall;

    /**
     * 第一张可见图片的下标
     */
    private int mFirstVisibleItem;

    /**
     * 一屏有多少张图片可见
     */
    private int mVisibleItemCount;

    private Context mContext;
    //TODO: dymanic assign this value depend on the screen size
    private int mCatchSize = 200;
    private HashSet<AsyncLoadThumbnail> mLoadThumbnailTasks = new HashSet<>();


    public PhotoWallAdapter2(Context context, Cursor cursor, GridView photoWall) {
        super(context, cursor, 0);  //flag = 0 1:FLAG_AUTO_REQUERY 2:FLAG_REGISTER_CONTENT_OBSERVER

        mContext = context;
        mPhotoWall = photoWall;

        mImagesCache = new LruCache<>(mCatchSize);

        Log.i("cache size", String.valueOf(mImagesCache.size()));
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        if (cursor != null) {
            View photo;
            photo = LayoutInflater.from(mContext).inflate(R.layout.photo_layout, null);
            return photo;
        } else {
            return null;
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view == null || cursor == null) {
            return;
        }

        int imageID = this.getCursor().getInt(0);

        ImageView photo = (ImageView)view.findViewById(R.id.photo);

        Bitmap bitmap = mImagesCache.get(String.valueOf(imageID));
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
        } else {
            //photo.setImageResource(R.drawable.empty_photo);
        }

        //set a tag, in order to find out this photo on a asynchronous task
        photo.setTag(imageID);
        Log.i("bind view","bind view, imageID:" + String.valueOf(imageID));
    }

    /*
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        this.getCursor().moveToPosition(position);
        int imageID = this.getCursor().getInt(0);

        View photoLayout;
        if (convertView == null) {
            photoLayout = LayoutInflater.from(mContext).inflate(R.layout.photo_layout, null);
        } else {
            photoLayout = convertView;
        }

        ImageView photo = (ImageView)photoLayout.findViewById(R.id.photo);

        Bitmap bitmap = mImagesCache.get(String.valueOf(imageID));
        if (bitmap != null) {
            photo.setImageBitmap(bitmap);
        } else {
            photo.setImageResource(R.drawable.empty_photo);
        }

        //set a tag, in order to find out this photo on a asynchronous task
        photo.setTag(String.valueOf(imageID));

        return photoLayout;
    }
    */


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // 仅当GridView静止时才去下载图片，GridView滑动时取消所有正在下载的任务
        if (scrollState == SCROLL_STATE_IDLE) {
            loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
        } else {
            cancelLoadThumbnailTasks();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;

        Log.i("onScroll","on scroll");

        // 下载的任务应该由onScrollStateChanged里调用，但首次进入程序时onScrollStateChanged并不会调用，
        // 因此在这里为首次进入程序开启下载任务。
        if (visibleItemCount > 0 && mImagesCache.size() == 0) {
            loadBitmaps(mFirstVisibleItem, mVisibleItemCount);
            Log.i("onScroll","onScroll msg");
        }

    }

    /**
     * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
     * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
     *
     * @param firstVisibleItem
     *            第一个可见的ImageView的下标
     * @param visibleItemCount
     *            屏幕中总共可见的元素数
     */
    private void loadBitmaps(int firstVisibleItem, int visibleItemCount) {
        if (this.getCursor() == null) {
            return;
        }

        Log.i("load bitmap","load bitmap");
        int imageID;
        try {
            for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
                this.getCursor().moveToPosition(i);
                imageID = this.getCursor().getInt(0);

                Bitmap bitmap = mImagesCache.get(String.valueOf(imageID));
                if (bitmap != null) {
                    ImageView photo = (ImageView) mPhotoWall.findViewWithTag(String.valueOf(imageID));
                    if (photo != null) {
                        photo.setImageBitmap(bitmap);
                    }

                } else {
                    AsyncLoadThumbnail task = new AsyncLoadThumbnail();
                    mLoadThumbnailTasks.add(task);
                    task.execute(imageID);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateImagesCache(String key, Bitmap bitmap) {
        if (mImagesCache.get(key) == null) {
            mImagesCache.put(key, bitmap);
        }
    }

    /**
     * cancel all the loading thumbnail tasks
     */
    public void cancelLoadThumbnailTasks() {
        if (mLoadThumbnailTasks != null) {
            for (AsyncLoadThumbnail task : mLoadThumbnailTasks) {
                task.cancel(false);
            }
        }
    }

    //asynchronous task load thumbnail
    private class AsyncLoadThumbnail extends AsyncTask <Integer, Void, Bitmap> {

        Integer mImageID;

        @Override
        protected Bitmap doInBackground(Integer... imageIDs) {
            mImageID = imageIDs[0];
            return MediaStore.Images.Thumbnails.getThumbnail(mContext.getContentResolver(),
                    mImageID,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    null);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.i("async task", "ImageID:" + String.valueOf(mImageID));
            if (bitmap != null) {
                ImageView photo = (ImageView)mPhotoWall.findViewWithTag(mImageID);
                Log.i("find view","find view, imageID:" + String.valueOf(mImageID));
                if (photo != null) {
                    photo.setImageBitmap(bitmap);
                } else {
                    Log.i("async task", "Can't find the photo.");
                }
                //add to cache
                PhotoWallAdapter2.this.updateImagesCache(String.valueOf(mImageID), bitmap);
            } else {
                Log.i("async task", "thumbnail is null");
            }

            mLoadThumbnailTasks.remove(this);
        }

    }

}
