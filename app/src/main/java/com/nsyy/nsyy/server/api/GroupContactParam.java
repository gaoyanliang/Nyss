package com.nsyy.nsyy.server.api;

public class GroupContactParam {
    int group_id;
    int cur_user_id;
    String group_name;

    public GroupContactParam() {
    }

    public GroupContactParam(int group_id, int cur_user_id, String group_name) {
        this.group_id = group_id;
        this.group_name = group_name;
        this.cur_user_id = cur_user_id;
    }

    public int getGroup_id() {
        return group_id;
    }

    public void setGroup_id(int group_id) {
        this.group_id = group_id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public int getCur_user_id() {
        return cur_user_id;
    }

    public void setCur_user_id(int cur_user_id) {
        this.cur_user_id = cur_user_id;
    }

    @Override
    public String toString() {
        return "GroupContactParam{" +
                "group_id=" + group_id +
                ", group_name='" + group_name + '\'' +
                ", cur_user_id='" + cur_user_id + '\'' +
                '}';
    }
}
