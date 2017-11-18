package com.example.dyang.galleryphotolist;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Created by DYang on 7/7/2015.
 */
public class ImagesCache extends LruCache<String, Bitmap> {

    public ImagesCache(Context context) {
        //use half of available memory
        super(((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() * 1024 * 1024 / 2);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount();
    }

}
