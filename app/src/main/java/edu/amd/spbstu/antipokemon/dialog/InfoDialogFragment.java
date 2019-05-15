package edu.amd.spbstu.antipokemon.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import edu.amd.spbstu.antipokemon.R;

public class InfoDialogFragment extends DialogFragment {
    private static final String TITLE_TAG = "title";
    private static final String INFO_TAG = "info";
    private static final String IMAGE_TAG = "info_img";
    private static final String OK_BUTTON_TAG = "ok_button";
    private static final String LISTENER_TAG = "listener";

    private TextView titleView;
    private ImageView imageView;
    private TextView infoView;
    private Button okButton;

    private ListenerOK mListener;

    private boolean isTextDialog;

    public static void showDialog(FragmentActivity activity, @StringRes int titleID, @StringRes int infoID,
                                  @DrawableRes int imageID, @StringRes int buttonID, ListenerOK okListener) {
        InfoDialogFragment dialogFragment = InfoDialogFragment.newInstance(activity, titleID, infoID, imageID, buttonID, okListener);
        dialogFragment.show(activity.getSupportFragmentManager(), "dialog");
    }

    private static InfoDialogFragment newInstance(FragmentActivity activity, @StringRes int titleID, @StringRes int infoID,
                                                   @DrawableRes int imageID, @StringRes int buttonID, ListenerOK okListener) {
        InfoDialogFragment infoDialogFragment = new InfoDialogFragment();
        Bundle args = new Bundle();
        args.putInt(TITLE_TAG, titleID);
        args.putInt(INFO_TAG, infoID);
        args.putInt(OK_BUTTON_TAG, buttonID);
        args.putInt(IMAGE_TAG, imageID);
        args.putParcelable(LISTENER_TAG, okListener);
        infoDialogFragment.setArguments(args);
        infoDialogFragment.setCancelable(true);
        return infoDialogFragment;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mListener.onOKButtonPressed();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            if (isTextDialog) {
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = metrics.widthPixels;
                int height = metrics.heightPixels;
                if (width < height)
                    dialog.getWindow().setLayout((int)(0.9 * width), ViewGroup.LayoutParams.WRAP_CONTENT);
                else
                    dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(0.9 * height));

            } else {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private View createViewImage(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.target_dialog, container);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        mListener = getArguments().getParcelable(LISTENER_TAG);

        titleView = (TextView)view.findViewById(R.id.dialog_title);
        imageView = (ImageView)view.findViewById(R.id.dialog_image_view);
        infoView = (TextView)view.findViewById(R.id.dialog_info);
        okButton = (Button)view.findViewById(R.id.dialog_ok_button);

        titleView.setText(getArguments().getInt(TITLE_TAG));
        infoView.setText(getArguments().getInt(INFO_TAG));
        okButton.setText(getArguments().getInt(OK_BUTTON_TAG));
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        imageView.setImageResource(getArguments().getInt(IMAGE_TAG, 0));
        return view;
    }

    private View createViewText(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.text_dialog, container);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        mListener = getArguments().getParcelable(LISTENER_TAG);

        infoView = (TextView)view.findViewById(R.id.dialog_info);
        okButton = (Button)view.findViewById(R.id.dialog_ok_button);

        infoView.setText(getArguments().getInt(INFO_TAG));
        okButton.setText(getArguments().getInt(OK_BUTTON_TAG));
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        return view;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int imageID = getArguments().getInt(IMAGE_TAG, 0);
        isTextDialog = (imageID == 0);
        if (imageID != 0)
            return createViewImage(inflater, container, savedInstanceState);
        else
            return createViewText(inflater, container, savedInstanceState);
    }
}
