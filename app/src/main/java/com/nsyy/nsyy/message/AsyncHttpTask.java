package com.nsyy.nsyy.message;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class AsyncHttpTask extends AsyncTask<String, Void, String> {

    public String fileName;
    public String dir;

    public AsyncHttpTask(String fileName, String dir) {
        this.fileName = fileName;
        this.dir = dir;
    }

    @Override
    protected String doInBackground(String... params) {
        String url = params[0];
        String postData = params[1];

        try {
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();

            // 设置请求方法为 POST
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");

            // 写入 POST 数据
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = postData.getBytes("utf-8");
                outputStream.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // 获取响应结果
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line.trim());
                }

                // Close the connection
                connection.disconnect();

                return response.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @Override
    protected void onPostExecute(String result) {
        // 在这里处理异步请求的结果
        // result 包含了从服务器返回的数据
        System.out.println("Response: " + result);
        // Convert JSON string to JSON object
        try {
            JSONObject jsonObject = new JSONObject(result);
            int code = jsonObject.getInt("code");
            // 查询成功，将查询到的消息写入文件
            if (code == 20000) {
                JSONArray msgs = jsonObject.getJSONArray("data");
                FileHelper.getInstance().writeLinesToFile(FileHelper.getInstance().jsonArrayToList(msgs), fileName, dir, true);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
}
