package edu.amd.spbstu.antipokemon.error;

import android.support.annotation.StringRes;

public abstract class ResourcedException extends Exception {
    private @StringRes int msgId;

    public ResourcedException(@StringRes int msgId) {
        this.msgId = msgId;
    }

    public int getMsgId() {
        return msgId;
    }
}
