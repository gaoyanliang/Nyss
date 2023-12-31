/*
 * Copyright (C) 2020 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.example.nsyy.service;

/**
 * 服务器配置
 *
 */
public final class ServerConfig {

    private ServerConfig() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    // 服务器端口
    public static final int SERVER_PORT = 8081;

    // 服务器响应超时时间(秒）
    public static final int SERVER_TIMEOUT = 10;

}
