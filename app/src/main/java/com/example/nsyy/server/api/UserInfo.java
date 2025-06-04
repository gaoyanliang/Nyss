package com.example.nsyy.server.api;

public class UserInfo {

    public boolean hasValue;

    public String username;

    public String password;
    public Integer pers_id;

    public boolean isHasValue() {
        return hasValue;
    }

    public void setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
    }

    public String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public UserInfo() {
    }

    public UserInfo(boolean hasValue, String username, String password, String version, Integer pers_id) {
        this.hasValue = hasValue;
        this.username = username;
        this.password = password;
        this.version = version;
        this.pers_id = pers_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPers_id() {
        return pers_id;
    }

    public void setPers_id(Integer pers_id) {
        this.pers_id = pers_id;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "hasValue=" + hasValue +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", pers_id=" + pers_id +
                ", version='" + version + '\'' +
                '}';
    }
}
