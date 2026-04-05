package dao;

import java.sql.*;
import javax.swing.JOptionPane;

public class ConnectionProvider {
    public static Connection getCon() {
        try {
            // Loading the MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Connection String: change 'YOUR_PASSWORD' to your MySQL root password
            Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/SmartBusReservation", "root", "Kcdr@123");
            
            return con;
        } catch (Exception e) {
            // This will show a popup if connection fails
            JOptionPane.showMessageDialog(null, "Connection Failed: " + e.getMessage());
            return null;
        }
    }
}