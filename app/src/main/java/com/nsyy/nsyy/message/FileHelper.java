package com.nsyy.nsyy.message;

import android.content.Context;

import com.yanzhenjie.andserver.http.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;

public class FileHelper {

    // 附件目录
    public static String ATTACHMENTS_DIR = "ATTACHMENTS";

    public static boolean RUN_IN_BACKGROUND = false;

    private volatile static FileHelper uniqueInstance;
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    //采用Double CheckLock(DCL)实现单例
    public static FileHelper getInstance() {
        if (uniqueInstance == null) {
            synchronized (FileHelper.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new FileHelper();
                }
            }
        }
        return uniqueInstance;
    }


    // ======================== 文件上传和下载 ========================

    public void uploadFile(MultipartFile file) throws Exception {
        String dir = "/" + ATTACHMENTS_DIR + "/";
        String filePath = context.getFilesDir() + dir + file.getFilename();
        // 定义文件保存路径
        File destFile = new File(filePath);

        // 获取文件所在目录
        File directory = destFile.getParentFile();
        // 检查目录是否存在，如果不存在，则创建
        if (!directory.exists()) {
            boolean created = directory.mkdirs();

            if (created) {
                System.out.println("Directory created successfully.");
            } else {
                System.out.println("Failed to create directory.");
            }
        }

        if (destFile.exists()) {
            throw new Exception("文件已存在");
        }

        // 将文件保存到本地
        file.transferTo(destFile);
    }


    public FileInputStream downloadFile(String fileName) throws Exception {
        String dir = "/" + ATTACHMENTS_DIR + "/";
        String filePath = context.getFilesDir() + dir + fileName;
        // 定义文件保存路径
        File destFile = new File(filePath);

        // 检查目录 & 文件 是否存在
        if (!destFile.getParentFile().exists() || !destFile.exists()) {
            throw new Exception("文件已存在");
        }

        // 读取文件内容
        FileInputStream inputStream = new FileInputStream(destFile);
        return inputStream;
    }

}
