package com.skobbler.sdkdemo.activity;

/**
 * Created by marcinsendera on 19.09.16.
 */
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;

public class DialogMessage {

    private AlertDialog.Builder alertDialogBuilder;
    private Context context;

    public DialogMessage(Context context) {
        this.context = context;
        this.alertDialogBuilder = new AlertDialog.Builder(context);
    }

    public void setMessage(String message, int seq1,  DialogInterface.OnClickListener listener1){
        this.alertDialogBuilder.setMessage(message);
        this.alertDialogBuilder.setPositiveButton(seq1, listener1);
    }

    public void setMessage(String message, int seq1,  DialogInterface.OnClickListener listener1, int seq2,  DialogInterface.OnClickListener listener2){
        this.alertDialogBuilder.setMessage(message);
        this.alertDialogBuilder.setPositiveButton(seq1, listener1);
        this.alertDialogBuilder.setNegativeButton(seq2, listener2);
    }

    public void setMessage(String message, int seq1,  DialogInterface.OnClickListener listener1, int seq2,  DialogInterface.OnClickListener listener2, int seq3,  DialogInterface.OnClickListener listener3){
        this.alertDialogBuilder.setMessage(message);
        this.alertDialogBuilder.setPositiveButton(seq1, listener1);
        this.alertDialogBuilder.setNeutralButton(seq2, listener2);
        this.alertDialogBuilder.setNegativeButton(seq3, listener3);
    }

    public void show() {
        AlertDialog alertDialog = this.alertDialogBuilder.create();
        alertDialog.show();
    }

    public void showWithTimeout(int timeout) {
        final AlertDialog alertDialog = this.alertDialogBuilder.create();
        alertDialog.show();

        new Handler().postDelayed(new Runnable() {

            public void run() {
                alertDialog.dismiss();
            }
        }, timeout);
    }

    public void cancel() {
        AlertDialog alertDialog = this.alertDialogBuilder.create();
        alertDialog.cancel();
    }
}
