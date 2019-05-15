package edu.amd.spbstu.antipokemon.error;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.Toast;

public class DebugExceptionHandler implements  ExceptionHandler {
    private Context context;

    public DebugExceptionHandler(@NonNull Context context) {
        this.context = context;
    }

    @Override
    public void process(ResourcedException e) {
        Toast.makeText(context, e.getMsgId(), Toast.LENGTH_SHORT).show();
    }
}
