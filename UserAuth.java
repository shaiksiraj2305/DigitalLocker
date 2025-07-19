package Locker_app;
import java.io.*;
import java.util.*;
public class UserAuth {
    private static final String USERS_FILE = "users.txt";

    public static boolean login(String username,String password){
        try(BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))){
            String line;
            while((line = reader.readLine())!=null){
                String[] parts = line.split(";");
                if(parts.length == 2 && parts[0].equals(username)&&parts[1].equals(password)){
                    return true;
                }
            }
        }catch(IOException e){
            System.out.println("Error readeing users file: " +e.getMessage());
        }
        return false;
    }

    public static boolean signup(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 2 && parts[0].equals(username)) {
                    return false; // User already exists
                }
            }
        } catch (IOException ignored) {}

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username + ";" + password);
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.out.println("Error writing to users file: " + e.getMessage());
        }
        return false;
    }
}
