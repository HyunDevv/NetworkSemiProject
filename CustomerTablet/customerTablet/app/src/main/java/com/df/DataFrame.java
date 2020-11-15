package com.df;

import java.util.HashMap;

public class DataFrame {
    private static final long serialVersionUID = 1L;
    private HashMap<String,DataFrame> maps;
    private String ip;
    private String sender;
    private String contents;

    public HashMap<String, DataFrame> getMaps() {
        return maps;
    }

    public void setMaps(HashMap<String, DataFrame> maps) {
        this.maps = maps;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public DataFrame() {
    }

    public DataFrame(HashMap<String, DataFrame> maps) {
        this.maps = maps;
    }

    public DataFrame(String ip, String contents) {
        this.ip = ip;
        this.contents = contents;
    }

    public DataFrame(HashMap<String, DataFrame> maps, String ip, String sender, String contents) {
        this.maps = maps;
        this.ip = ip;
        this.sender = sender;
        this.contents = contents;
    }

    public DataFrame(String ip, String sender, String contents) {z
        this.ip = ip;
        this.sender = sender;
        this.contents = contents;
    }
}
