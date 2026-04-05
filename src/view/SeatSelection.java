package view;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import java.sql.*;

public class SeatSelection extends javax.swing.JFrame {
    private JPanel panelSeats;
    private JButton[] seats = new JButton[41]; 
  private int scheduleId;
    private int userId;
    private int actualbusId;      // This will fix the 'busId' red line
    private String travelDate;
    private String route;
    private double busFare;
private String sourceCity;
private String destCity;
private int selectedSeatsCount = 0; // Tracks seats clicked in this session
private final int MAX_SEATS = 6;    // The limit
private JButton btnFinish;

    public SeatSelection(int scheduleId, int userId, String travelDate,int busId, String route) {
    this.scheduleId = scheduleId;
    this.userId = userId;
    this.travelDate = travelDate;
    this.actualbusId = busId;
    this.route = route;
        
        fetchScheduleDetails(); // New method to get info from the JOINed tables
        createUI();
        generateSeats();
        loadBookedSeats();
        
        this.add(panelSeats); 
        this.pack();
        setLocationRelativeTo(null);
    }

   private void fetchScheduleDetails() {
    try {
        Connection con = dao.ConnectionProvider.getCon();
        // Look up BOTH bus details AND the real schedule_id in one query
        String query = "SELECT s.schedule_id, s.fare, b.bus_id, b.source, b.destination " +
                       "FROM schedules s JOIN buses b ON s.bus_id = b.bus_id " +
                       "WHERE b.bus_id = ? AND s.travel_date = ?";
        PreparedStatement ps = con.prepareStatement(query);
        ps.setInt(1, this.actualbusId);
        ps.setString(2, this.travelDate);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            this.scheduleId = rs.getInt("schedule_id"); // Get REAL schedule_id
            this.busFare = rs.getDouble("fare");
            this.actualbusId = rs.getInt("bus_id");
            this.sourceCity = rs.getString("source");
            this.destCity = rs.getString("destination");
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error fetching schedule: " + e.getMessage());
    }
}

   private void createUI() {
    panelSeats = new JPanel();
    // Change layout to BorderLayout to support the bottom button
    this.setLayout(new java.awt.BorderLayout()); 
    
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setTitle("Select Seat - " + sourceCity + " to " + destCity + " (" + travelDate + ")");
    
    panelSeats.setLayout(new GridLayout(10, 5, 10, 10)); 
    
    // Create the Finish Button
    btnFinish = new JButton("Finish & Go to Dashboard");
    btnFinish.setBackground(new Color(0, 102, 204)); // Professional Blue
    btnFinish.setForeground(Color.WHITE);
    btnFinish.setFont(new java.awt.Font("Tahoma", 1, 14));

 // Inside SeatSelection.java -> createUI() method
    btnFinish.addActionListener(e -> {
    this.dispose(); // Close SeatSelection
    
    // THE ROLE-BASED LOGIC
    if (this.userId == 1) { 
        // If Admin (ID 1), go to the Admin Dashboard
        new Dashboard(this.userId).setVisible(true); 
    } else {
        // If anyone else, go to the User Dashboard
        new UserDashboard(this.userId).setVisible(true);
    }
});
    // Add components to the Frame
    this.add(panelSeats, java.awt.BorderLayout.CENTER);
    this.add(btnFinish, java.awt.BorderLayout.SOUTH);
}

    private void generateSeats() {
        int seatCounter = 1;
        for (int i = 0; i < 50; i++) { 
            int column = i % 5;
            if (column == 2) { 
                panelSeats.add(new javax.swing.JLabel("AISLE", javax.swing.SwingConstants.CENTER));
            } else {
                if (seatCounter <= 40) {
                    String label = String.valueOf(seatCounter);
                    if (column == 0 || column == 4) label += " (W)";

                    seats[seatCounter] = new JButton(label);
                    seats[seatCounter].setBackground(Color.GREEN);
                    
                    int seatNum = seatCounter;
                    seats[seatCounter].addActionListener(e -> handleSeatClick(seatNum));
                    
                    panelSeats.add(seats[seatCounter]);
                    seatCounter++;
                } else {
                    panelSeats.add(new javax.swing.JLabel("")); 
                }
            }
        }
    }

  private void handleSeatClick(int num) {
    if (seats[num].getBackground() == Color.RED) {
        JOptionPane.showMessageDialog(this, "Seat " + num + " is already booked!");
        return; 
    }

    // UPDATED GATEKEEPER 2: Check database + current session
    int totalBookedByThisUser = getAlreadyBookedCount();
    
    if (totalBookedByThisUser >= MAX_SEATS) {
        JOptionPane.showMessageDialog(this, "You have already booked the maximum of " + MAX_SEATS + " seats for this bus trip.");
        return;
    }

    if (seats[num].getBackground() == Color.GREEN) {
        seats[num].setBackground(Color.YELLOW);
        int choice = JOptionPane.showConfirmDialog(this, "Book Seat " + num + " for ₹" + busFare + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            bookSeatInDB(num); 
        } else {
            seats[num].setBackground(Color.GREEN);
        }
    }
}

   private void bookSeatInDB(int seatNum) {
try {
        String pName = JOptionPane.showInputDialog(this, "Enter Passenger Name:");
        if (pName == null || pName.trim().isEmpty()) {
            seats[seatNum].setBackground(Color.GREEN);
            return;
        }

        Connection con = dao.ConnectionProvider.getCon();
        
        // 1. Double check the seat isn't already taken
        String checkQuery = "SELECT COUNT(*) FROM bookings WHERE schedule_id = ? AND seats_booked = ?";
        PreparedStatement checkPs = con.prepareStatement(checkQuery);
        checkPs.setInt(1, this.scheduleId); // Using scheduleId
        checkPs.setInt(2, seatNum);
        ResultSet checkRs = checkPs.executeQuery();
        
        if (checkRs.next() && checkRs.getInt(1) > 0) {
            JOptionPane.showMessageDialog(this, "This seat was just taken!");
            loadBookedSeats(); 
            return;
        }

        // 2. INSERT INTO BOOKINGS - FIXED PARAMETERS
        String sql = "INSERT INTO bookings (user_id, bus_id, passenger_name, seats_booked, total_amount, travel_date, route, schedule_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        
        ps.setInt(1, this.userId);           
        ps.setInt(2, this.actualbusId); // Correct: Physical Bus ID
        ps.setString(3, pName);              
        ps.setInt(4, seatNum);               
        ps.setDouble(5, this.busFare);            
        ps.setString(6, this.travelDate);    
        ps.setString(7, this.route);         
        ps.setInt(8, this.scheduleId);  // FIXED: Changed from actualbusId to scheduleId
        
        ps.executeUpdate();

        // 3. Update the available seats count in the schedules table
        String updateSql = "UPDATE schedules SET available_seats = available_seats - 1 WHERE schedule_id = ?";
        PreparedStatement psUpdate = con.prepareStatement(updateSql);
        psUpdate.setInt(1, this.scheduleId);
        psUpdate.executeUpdate();

        selectedSeatsCount++; 
        JOptionPane.showMessageDialog(null, "Seat " + seatNum + " Booked Successfully!");
        
        loadBookedSeats(); // Refresh view so seat turns RED

    } catch (Exception e) {
        // This will now show the EXACT SQL error if it happens again
        JOptionPane.showMessageDialog(this, "Booking Error: " + e.getMessage());
        e.printStackTrace(); 
    }
}
   
    public void loadBookedSeats() {
        try {
            Connection con = dao.ConnectionProvider.getCon();
            // Only load seats that match THIS specific trip/schedule
            String query = "SELECT seats_booked FROM bookings WHERE schedule_id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, this.scheduleId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int seatNum = rs.getInt("seats_booked");
                if(seatNum >= 1 && seatNum <= 40) {
                    seats[seatNum].setBackground(Color.RED);
                   seats[seatNum].setForeground(Color.WHITE);
                    seats[seatNum].setText("X");
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading seats: " + e.getMessage());
        }
    }
    private int getAlreadyBookedCount() {
    int count = 0;
    try {
        Connection con = dao.ConnectionProvider.getCon();
        // This query counts EVERY seat this user has booked for this specific schedule in the DB
        String query = "SELECT COUNT(*) FROM bookings WHERE user_id = ? AND schedule_id = ?";
        PreparedStatement ps = con.prepareStatement(query);
        ps.setInt(1, this.userId);
        ps.setInt(2, this.scheduleId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            count = rs.getInt(1);
        }
    } catch (Exception e) {
        System.out.println("Error checking DB limit: " + e.getMessage());
    }
    return count;
}
}