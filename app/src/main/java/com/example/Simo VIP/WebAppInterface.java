package com.example.skintool;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import rikka.shizuku.Shizuku;

public class WebAppInterface {
    private final Context mContext;
    private final String TARGET_PATH = "/sdcard/Android/data/com.dts.freefireth/files/";
    private final String FILE_NAME = "active_file.bin";

    public WebAppInterface(Context c) {
        mContext = c;
    }

    /** استخراج الملف من assets إلى ذاكرة التطبيق الداخلية عند الحاجة */
    private String extractAssetToFile() {
        File outFile = new File(mContext.getFilesDir(), FILE_NAME);
        try (InputStream in = mContext.getAssets().open(FILE_NAME);
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return outFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @JavascriptInterface
    public void enableGlitches() {
        // 1. استخراج الملف المدمج أولاً
        String extractedPath = extractAssetToFile();
        if (extractedPath == null) {
            showToast("حدث خطأ أثناء قراءة ملف التفعيل من التطبيق!");
            return;
        }

        // 2. أمر إنشاء المجلد المستهدف إن لم يكن موجوداً ثم نسخ الملف
        String command = "mkdir -p " + TARGET_PATH + " && cp -r " + extractedPath + " " + TARGET_PATH;
        runShizukuCommand(command, "تم تفعيل الملف بنجاح!");
    }

    @JavascriptInterface
    public void disableGlitches() {
        // أمر حذف ملف التفعيل لاستعادة الوضع الأصل
        String command = "rm -f " + TARGET_PATH + FILE_NAME;
        runShizukuCommand(command, "تم التعطيل وإزالة الملف بنجاح!");
    }

    private void runShizukuCommand(String command, String successMsg) {
        if (!Shizuku.pingBinder()) {
            showToast("تطبيق Shizuku غير متصل! يرجى تشغيله أولاً.");
            return;
        }

        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0);
            showToast("يرجى منح صلاحية Shizuku للتطبيق.");
            return;
        }

        try {
            Process process = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                showToast(successMsg);
            } else {
                showToast("فشلت العملية، رمز الخطأ: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("حدث خطأ: " + e.getMessage());
        }
    }

    private void showToast(String message) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
        );
    }
}