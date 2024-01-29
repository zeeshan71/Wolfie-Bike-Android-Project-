package com.example.mcc_project;

public class Users {
    String name, sbuID, phone, email;

    public Users() {
    }

    public Users(String name, String sbuID, String phone, String email) {
        this.name = name;
        this.sbuID = sbuID;
        this.phone = phone;
        this.email = email;
    }

    //GETTERS
    public String getName() {
        return name;
    }
    public String getSbuID() {
        return sbuID;
    }
    public String getPhone() {
        return phone;
    }
    public String getEmail() { return email; }

    //SETTERS
    public void setName(String name) {
        this.name = name;
    }
    public void setSbuID(String sbuID) {
        this.sbuID = sbuID;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) { this.email = email; }
}
