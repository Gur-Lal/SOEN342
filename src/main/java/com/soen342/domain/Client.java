package com.soen342.domain;

public class Client {
    private static int counter = 0;
    private String name;
    private int age;
    private String id;
     private int dbId;

    public Client(String name, int age) {
        counter++;
        this.name = name;
        this.age = age;
        this.id = "CLI" + String.format("%04d", counter); 
    }

    public Client(int dbId, String name, int age, String id) {
        this.dbId = dbId;
        this.name = name;
        this.age = age;
        this.id = id;
    }

      public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    //  convenience alias for DB compatibility
    public String getIdentifier() {
        return id;
    }

    // debugging / logging helper
    @Override
    public String toString() {
        return "Client{" +
                "dbId=" + dbId +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", id='" + id + '\'' +
                '}';
    }
    
}
