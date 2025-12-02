package com.soen342.domain;

public class Client {
    private String firstName;
private String lastName;
    private int age;
    private String id; // This is the license_id

    public Client(String firstName, String lastName, int age, String id) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public int getAge() {
        return age;
    }
    
    public String getId() {
        return id;
    }
}