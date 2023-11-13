package com.example.nsyy.server.api;

public class UserInfo {

    public boolean hasValue;

    public String username;

    public String password;

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

    public UserInfo(boolean hasValue, String username, String password, String version) {
        this.hasValue = hasValue;
        this.username = username;
        this.password = password;
        this.version = version;
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

    @Override
    public String toString() {
        return "UserInfo{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", hasValue='" + hasValue + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
