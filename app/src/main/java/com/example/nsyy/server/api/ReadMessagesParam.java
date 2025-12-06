package com.example.nsyy.server.api;

public class ReadMessagesParam {

    public String url;
    public Integer read_type;
    public Integer cur_user_id;
    public Integer chat_user_id;
    public Integer start;
    public Integer count;
    public String keyword;
    public String start_time_str;
    public String end_time_str;


    public ReadMessagesParam() {

    }

    public ReadMessagesParam(String url, Integer cur_user_id, Integer chat_user_id,
                             Integer read_type, Integer start, Integer count, String keyword,
                             String start_time_str, String end_time_str) {
        this.url = url;
        this.cur_user_id = cur_user_id;
        this.chat_user_id = chat_user_id;
        this.read_type = read_type;
        this.start = start;
        this.count = count;
        this.keyword = keyword;
        this.start_time_str = start_time_str;
        this.end_time_str = end_time_str;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getCur_user_id() {
        return cur_user_id;
    }

    public void setCur_user_id(int cur_user_id) {
        this.cur_user_id = cur_user_id;
    }

    public Integer getChat_user_id() {
        return chat_user_id;
    }

    public void setChat_user_id(int chat_user_id) {
        this.chat_user_id = chat_user_id;
    }

    public Integer getRead_type() {
        return read_type;
    }

    public void setRead_type(int read_type) {
        this.read_type = read_type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStart_time_str() {
        return start_time_str;
    }

    public void setStart_time_str(String start_time_str) {
        this.start_time_str = start_time_str;
    }

    public String getEnd_time_str() {
        return end_time_str;
    }

    public void setEnd_time_str(String end_time_str) {
        this.end_time_str = end_time_str;
    }

    @Override
    public String toString() {
        return "ReadMessagesParam{" +
                "url='" + url + '\'' +
                ", read_type=" + read_type +
                ", cur_user_id=" + cur_user_id +
                ", chat_user_id=" + chat_user_id +
                ", start=" + start +
                ", count=" + count +
                ", keyword='" + keyword + '\'' +
                ", start_time_str='" + start_time_str + '\'' +
                ", end_time_str='" + end_time_str + '\'' +
                '}';
    }
}
