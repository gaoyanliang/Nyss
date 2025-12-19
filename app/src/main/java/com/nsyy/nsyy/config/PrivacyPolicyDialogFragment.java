package com.nsyy.nsyy.config;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.nsyy.Nsyy.R;
import com.nsyy.nsyy.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class PrivacyPolicyDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_privacy_policy, null);

        TextView tvContent = view.findViewById(R.id.tv_privacy_content);

        // 读取本地 HTML 并转为字符串
        String htmlContent = getPrivacyHtml();

        // 用 Html.fromHtml 渲染（支持基本标签）
        tvContent.setText(Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY));

        // 支持点击链接
        tvContent.setMovementMethod(LinkMovementMethod.getInstance());

        // 按钮
        Button btnDisagree = view.findViewById(R.id.btn_disagree);
        Button btnAgree = view.findViewById(R.id.btn_agree);

        btnDisagree.setOnClickListener(v -> {
            requireActivity().finishAffinity();
            System.exit(0);
        });

        btnAgree.setOnClickListener(v -> {
            SharedPreferences sp = requireContext().getSharedPreferences("app_config", Context.MODE_PRIVATE);
            sp.edit().putBoolean("has_agreed_privacy", true).apply();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).fullSetupAfterAgree();
            }
            dismiss();
        });

        return new AlertDialog.Builder(requireActivity())
                .setView(view)
                .setCancelable(false)
                .create();
    }

    private String getPrivacyHtml() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(requireContext().getAssets().open("privacy_policy.html"), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "<h1>加载失败</h1>";
        }
        return sb.toString();
    }
}