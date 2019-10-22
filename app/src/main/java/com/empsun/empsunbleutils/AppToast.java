package com.empsun.empsunbleutils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by chen on 2018/1/15.
 */

public class AppToast {

    public static void creat(Context context, String msg){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

}
