package com.example.nsyy.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.nsyy.utils.JsonUtils;
import com.yanzhenjie.andserver.annotation.Converter;
import com.yanzhenjie.andserver.framework.MessageConverter;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.util.IOUtils;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

@Converter
public class NotificationConverter implements MessageConverter {
    /**
     * 服务端 -> 客户端
     *
     * 将服务端返回值 ReturnData 转换为 json 返回给客户端
     *
     * @param output
     * @param mediaType
     * @return
     */
    @Override
    public ResponseBody convert(@Nullable Object output, @Nullable MediaType mediaType) {
        return new JsonBody(JsonUtils.toJsonString(output));
    }

    /**
     * 客户端 -> 服务端
     *
     * 将客户端传过来的 josn 转换为服务端实体
     *
     * @param stream
     * @param mediaType
     * @param type
     * @param <T>
     * @return
     * @throws IOException
     */
    @Nullable
    @Override
    public <T> T convert(@NonNull InputStream stream, @Nullable MediaType mediaType, Type type) throws IOException {
        Charset charset = mediaType == null ? null : mediaType.getCharset();
        if (charset == null) {
            return JsonUtils.parseJson(IOUtils.toString(stream), type);
        }
        return JsonUtils.parseJson(IOUtils.toString(stream, charset), type);
    }
}
