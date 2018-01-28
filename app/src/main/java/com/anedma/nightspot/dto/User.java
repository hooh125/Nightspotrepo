package com.anedma.nightspot.dto;

/**
 * Class created by Andrés Mata (andreseduardomp@gmail.com) on 26/01/2018.
 */

public class User {

    private static User instance = null;
    private static String name;
    private static String lastName;
    private static String email;

    public static User getInstance() {
        if(instance == null) {
            instance = new User(name, lastName, email);
        }
        return instance;
    }

    private User(String name, String lastName, String email) {
        User.name = name;
        User.lastName = lastName;
        User.email = email;
    }

    public void setName(String name) {
        User.name = name;
    }

    public void setLastName(String lastName) {
        User.lastName = lastName;
    }

    public void setEmail(String email) {
        User.email = email;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}
