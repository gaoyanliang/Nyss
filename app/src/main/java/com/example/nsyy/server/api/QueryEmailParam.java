package com.example.nsyy.server.api;

import java.util.Map;

public class QueryEmailParam {

    public String message_id;
    public String user_account;
    public String mailbox;

    public QueryEmailParam() {
    }

    public QueryEmailParam(String message_id, String user_account, String mailbox) {
        this.message_id = message_id;
        this.user_account = user_account;
        this.mailbox = mailbox;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public String getUser_account() {
        return user_account;
    }

    public void setUser_account(String user_account) {
        this.user_account = user_account;
    }

    public String getMailbox() {
        return mailbox;
    }

    public void setMailbox(String mailbox) {
        this.mailbox = mailbox;
    }

    @Override
    public String toString() {
        return "QueryEmailParam{" +
                "message_id='" + message_id + '\'' +
                ", user_account='" + user_account + '\'' +
                ", mailbox='" + mailbox + '\'' +
                '}';
    }
}
