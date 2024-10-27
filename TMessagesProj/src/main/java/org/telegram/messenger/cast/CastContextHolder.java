package org.telegram.messenger.cast;

import android.annotation.SuppressLint;
import android.content.Context;

import com.google.android.gms.cast.framework.CastContext;

public class CastContextHolder {

    @SuppressLint("StaticFieldLeak")
    public static CastContext context;

    public static void init(Context context) {
        if (CastContextHolder.context != null) return;
        CastContextHolder.context = CastContext.getSharedInstance(context);
    }
}
