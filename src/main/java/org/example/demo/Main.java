package org.example.demo;

import org.example.persistence.ormanager.ORManager;
import org.example.persistence.utilities.Utils;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        String path = "h2.properties";
        ORManager orManager = Utils.withPropertiesFrom(path);

    }
}