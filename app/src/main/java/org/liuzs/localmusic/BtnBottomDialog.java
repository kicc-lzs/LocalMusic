package org.liuzs.localmusic;

import android.app.Dialog;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BtnBottomDialog extends BottomSheetDialogFragment{

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null) return super.onCreateDialog(savedInstanceState);

        BottomSheetDialog dialog = new BottomSheetDialog(getActivity(), R.style.Theme_MaterialComponents_BottomSheetDialog);

        dialog.setContentView(R.layout.dialog_fragment_layout);

        return dialog;
    }

}


