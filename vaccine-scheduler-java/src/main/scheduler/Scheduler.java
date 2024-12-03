package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        mainMenu();

            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("> ");
                String response = "";
                try {
                    response = r.readLine();
                } catch (IOException e) {
                    System.out.println("An error occurred. Please try again!");
                    continue;
                }
                // split the user input by spaces
                String[] tokens = response.split(" ");
                // check if input exists
                if (tokens.length == 0) {
                    System.out.println("Invalid input. Please try again!");
                    mainMenu();
                    continue;
                }
                // determine which operation to perform
                String operation = tokens[0];
                if (operation.equals("create_patient")) {
                    createPatient(tokens);
                } else if (operation.equals("create_caregiver")) {
                    createCaregiver(tokens);
                } else if (operation.equals("login_patient")) {
                    loginPatient(tokens);
                } else if (operation.equals("login_caregiver")) {
                    loginCaregiver(tokens);
                } else if (operation.equals("search_caregiver_schedule")) {
                    searchCaregiverSchedule(tokens);
                } else if (operation.equals("reserve")) {
                    reserve(tokens);
                } else if (operation.equals("upload_availability")) {
                    uploadAvailability(tokens);
                } else if (operation.equals("cancel")) {
                    cancel(tokens);
                } else if (operation.equals("add_doses")) {
                    addDoses(tokens);
                } else if (operation.equals("show_appointments")) {
                    showAppointments(tokens);
                } else if (operation.equals("logout")) {
                    logout(tokens);
                } else if (operation.equals("quit")) {
                    System.out.println("Bye!");
                    return;
                } else {
                    System.out.println("Invalid operation name! Please try again.");
                }
                mainMenu();
            }
        }

        private static void mainMenu() {
            System.out.println();
            System.out.println("*** Please enter one of the following commands ***");
            System.out.println("> create_patient <username> <password>");
            System.out.println("> create_caregiver <username> <password>");
            System.out.println("> login_patient <username> <password>");
            System.out.println("> login_caregiver <username> <password>");
            System.out.println("> search_caregiver_schedule <date>");
            System.out.println("> reserve <date> <vaccine>");
            System.out.println("> upload_availability <date>");
            System.out.println("> cancel <appointment_id>"); // TODO: implement cancel (extra credit)
            System.out.println("> add_doses <vaccine> <number>");
            System.out.println("> show_appointments");
            System.out.println("> logout");
            System.out.println("> quit");
            System.out.println();
        }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!isStrongPassword(password)) {
            System.out.println("Password must be at least 8 characters, have uppercase and lowercase letters, " +
                    "numbers, and at least one special character (!, @, #, ?).");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }

        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        if (!isStrongPassword(password)) {
            System.out.println("Password must be at least 8 characters, have uppercase and lowercase letters, " +
                    "numbers, and at least one special character (!, @, #, ?).");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean isStrongPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            }
            if (Character.isLowerCase(c)) {
                hasLowercase = true;
            }
        }
        if (!hasUppercase || !hasLowercase) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasNumber = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
            if (Character.isDigit(c)) {
                hasNumber = true;
            }
        }
        if (!hasLetter || !hasNumber) {
            return false;
        }

        String specialCharacters = "!@#?";
        boolean hasSpecialCharacter = false;
        for (char c : password.toCharArray()) {
            if (specialCharacters.contains(String.valueOf(c))) {
                hasSpecialCharacter = true;
            }
        }
        return hasSpecialCharacter;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        Date dateStr;
        try {
            dateStr = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String caregiverQuery = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
        String vaccineQuery = "SELECT Name, Doses FROM Vaccines";

        try {
            // Retrieve available caregivers
            PreparedStatement caregiverStmt = con.prepareStatement(caregiverQuery);
            caregiverStmt.setDate(1, Date.valueOf(date));
            ResultSet caregiverResults = caregiverStmt.executeQuery();

            while (caregiverResults.next()) {
                System.out.println(caregiverResults.getString("Username"));
            }

            caregiverResults.close();
            caregiverStmt.close();

            // Retrieve vaccines and doses
            PreparedStatement vaccineStmt = con.prepareStatement(vaccineQuery);
            ResultSet vaccineResults = vaccineStmt.executeQuery();

            while (vaccineResults.next()) {
                String vaccineName = vaccineResults.getString("Name");
                int doses = vaccineResults.getInt("Doses");
                System.out.println(vaccineName + " " + doses);
            }

            vaccineResults.close();
            vaccineStmt.close();
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        Date dateStr;
        try {
            dateStr = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
            return;
        }
        String vaccineName = tokens[2];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            con.setAutoCommit(false);

            String vaccineQuery = "SELECT Doses FROM Vaccines WHERE Name = ?";
            try (PreparedStatement vaccineStmt = con.prepareStatement(vaccineQuery)) {
                vaccineStmt.setString(1, vaccineName);
                ResultSet vaccineResult = vaccineStmt.executeQuery();

                if (!vaccineResult.next() || vaccineResult.getInt("Doses") <= 0) {
                    System.out.println("Not enough available doses. Please try again.");
                    return;
                }
            }

            String caregiverQuery =
                    "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
            String caregiverUsername = null;

            try (PreparedStatement caregiverStmt = con.prepareStatement(caregiverQuery)) {
                caregiverStmt.setDate(1, Date.valueOf(date));
                ResultSet caregiverResult = caregiverStmt.executeQuery();

                if (!caregiverResult.next()) {
                    System.out.println("No caregiver is available on the selected date. Please try again.");
                    return;
                }
                caregiverUsername = caregiverResult.getString("Username");
            }

            String maxIdQuery = "SELECT MAX(ID) AS MaxID FROM Appointments";
            int appointmentId = 1;

            try (PreparedStatement maxIdStmt = con.prepareStatement(maxIdQuery);
                 ResultSet maxIdResult = maxIdStmt.executeQuery()) {
                if (maxIdResult.next()) {
                    appointmentId = maxIdResult.getInt("MaxID") + 1;
                }
            }

            String reserveAppointment =
                    "INSERT INTO Appointments (ID, Time, Vaccine, Caregiver, Patient) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement appointmentStmt = con.prepareStatement(reserveAppointment)) {
                appointmentStmt.setInt(1, appointmentId);
                appointmentStmt.setDate(2, Date.valueOf(date));
                appointmentStmt.setString(3, vaccineName);
                appointmentStmt.setString(4, caregiverUsername);
                appointmentStmt.setString(5, currentPatient.getUsername());
                appointmentStmt.executeUpdate();
            }

            String updateCaregiver = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
            try (PreparedStatement updateCaregiverStmt = con.prepareStatement(updateCaregiver)) {
                updateCaregiverStmt.setString(1, caregiverUsername);
                updateCaregiverStmt.setDate(2, Date.valueOf(date));
                updateCaregiverStmt.executeUpdate();
            }

            String updateVaccine = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
            try (PreparedStatement updateVaccineStmt = con.prepareStatement(updateVaccine)) {
                updateVaccineStmt.setString(1, vaccineName);
                updateVaccineStmt.executeUpdate();
            }

            con.commit();

            System.out.println("Appointment ID " + appointmentId + ", Caregiver username " + caregiverUsername);
        } catch (SQLException | IllegalArgumentException e) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            System.out.println("An error occurred while processing your request. Please try again.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            // Determine if the logged-in user is a patient or a caregiver
            if (currentPatient != null) {
                // Query appointments for the logged-in patient
                String patientQuery =
                        "SELECT ID, Vaccine, Time, Caregiver FROM Appointments WHERE Patient = ? ORDER BY ID ASC";
                try (PreparedStatement patientStmt = con.prepareStatement(patientQuery)) {
                    patientStmt.setString(1, currentPatient.getUsername());
                    ResultSet resultSet = patientStmt.executeQuery();

                    while (resultSet.next()) {
                        int id = resultSet.getInt("ID");
                        String vaccine = resultSet.getString("Vaccine");
                        String time = resultSet.getDate("Time").toString();
                        String caregiver = resultSet.getString("Caregiver");
                        System.out.println(id + " " + vaccine + " " + time + " " + caregiver);
                    }
                }
            } else if (currentCaregiver != null) {
                // Query appointments for the logged-in caregiver
                String caregiverQuery =
                        "SELECT ID, Vaccine, Time, Patient FROM Appointments WHERE Caregiver = ? ORDER BY ID ASC";
                try (PreparedStatement caregiverStmt = con.prepareStatement(caregiverQuery)) {
                    caregiverStmt.setString(1, currentCaregiver.getUsername());
                    ResultSet resultSet = caregiverStmt.executeQuery();

                    while (resultSet.next()) {
                        int id = resultSet.getInt("ID");
                        String vaccine = resultSet.getString("Vaccine");
                        String time = resultSet.getDate("Time").toString();
                        String patient = resultSet.getString("Patient");
                        System.out.println(id + " " + vaccine + " " + time + " " + patient);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // Ensure no unnecessary parameters are passed
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        // Check if a user is logged in
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }

        // Log out the user
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("Successfully logged out");
    }
}
