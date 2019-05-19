package com.example.vasu.redcarpet;

public class User {
    public int count;
    public String number;
    public String pictureURL;
    public String uniqueID;

    public User() {

    }

    public User(int count, String number, String pictureURL,String uniqueID) {
        this.count = count;
        this.number = number;
        this.pictureURL = pictureURL;
        this.uniqueID = uniqueID;
    }
}
