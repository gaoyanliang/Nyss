package com.nsyy.nsyy.server.controller;

import com.nsyy.Nsyy.R;
import com.nsyy.nsyy.message.FileHelper;
import com.nsyy.nsyy.server.api.ReturnData;
import com.yanzhenjie.andserver.annotation.CrossOrigin;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestMethod;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.StreamBody;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.multipart.MultipartFile;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.FileInputStream;
import java.net.URLEncoder;

@RestController
public class FileController {

    @CrossOrigin(methods = {RequestMethod.POST})
    @PostMapping(path = "/upload")
    public ReturnData uploadFile(@RequestParam(name = "data") MultipartFile data) {
        ReturnData returnData = new ReturnData();
        if (data != null && !data.isEmpty()) {
            try {
                FileHelper.getInstance().uploadFile(data);
                returnData.setSuccess(true);
                returnData.setCode(200);
                returnData.setData("文件上传成功");
                return returnData;
            } catch (Exception e) {
                e.printStackTrace();
                returnData.setSuccess(false);
                returnData.setCode(500);
                returnData.setData("文件上传失败" + e.getMessage());
                return returnData;
            }
        } else {
            // 文件为空，发送错误响应
            returnData.setSuccess(false);
            returnData.setCode(500);
            returnData.setData("文件上传失败, 上传文件异常，请重试");
            return returnData;
        }
    }


    @CrossOrigin(methods = {RequestMethod.GET})
    @GetMapping(path = "/download")
    public StreamBody downloadFile(@RequestParam("filename") String filename, HttpResponse response) {

        try {
            FileInputStream fileInputStream = FileHelper.getInstance().downloadFile(filename);

            // 创建文件流响应体
            StreamBody body = new StreamBody(fileInputStream, MediaType.APPLICATION_OCTET_STREAM);
            // 设置下载时的文件名
            String encodedFilename = URLEncoder.encode(filename, "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
            return body;

//            return new StreamBody(fileInputStream, MediaType.APPLICATION_OCTET_STREAM);
        } catch (Exception e) {
            e.printStackTrace();
            // 返回文件未找到响应
            return null;
        }
    }

}
