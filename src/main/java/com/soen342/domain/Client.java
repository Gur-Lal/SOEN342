package com.soen342.domain;

public class Client {
    private static int counter = 0;
    private int dbId;          // database primary key
    private String firstName;
    private String lastName;
    private int age;
    private String id;         // internal generated code like CLI0001

    //  Constructor for new clients (auto-generate ID)
    public Client(String firstName, String lastName, int age) {
        counter++;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.id = "CLI" + String.format("%04d", counter);
    }

    //  Constructor for clients loaded from DB
    public Client(int dbId, String firstName, String lastName, int age, String id) {
        this.dbId = dbId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.id = id;
    }

    // Getters & Setters 
    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Client{" +
                "dbId=" + dbId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", age=" + age +
                ", id='" + id + '\'' +
                '}';
    }
}
