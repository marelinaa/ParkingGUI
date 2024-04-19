package parking;

import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

public class Parking implements Serializable {
    // class release version:
    private static final long serialVersionUID = 1L;
    // areas with prompts:
    String carNumber;
    public static final String P_carNumber = "Car Number";
    String name;
    public static final String P_name = "Name";
    LocalDateTime startTime;
    public static final String P_start = "Start";
    LocalDateTime endTime;
    public static final String P_end = "End";
    double price;
    public static final String P_price = "Price";
    double totalTime;
    public static final String P_totalTime = "Total time, min";



    // validation methods:
    static Boolean isValidCarNumber(String str) {
        if (str.length() != 7) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        for (int i = 4; i < 6; i++) {
            char ch = str.charAt(i);
            if (!Character.isLetter(ch) || !Character.isUpperCase(ch)) {
                return false;
            }
        }
        char lastChar = str.charAt(6);
        return Character.isDigit(lastChar);
    }

    static boolean validDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        try {
            LocalDateTime.parse(date, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static double calculateTotalTime(LocalDateTime startTime, LocalDateTime endTime) {
        Duration duration = Duration.between(startTime, endTime);
        long minutes = duration.toMinutes();
        return (double) minutes;
    }
    private static GregorianCalendar curCalendar = new GregorianCalendar();

    public static boolean nextRead(Scanner fin, PrintStream out) {
        return nextRead(P_carNumber, fin, out);
    }

    static boolean nextRead(final String prompt, Scanner fin, PrintStream out) {
        out.print(prompt);
        out.print(": ");
        return fin.hasNextLine();
    }

    public String getCarNumber() {
        return carNumber;
    }

    public final void setCarNumber(String carNumber) {
        if (!isValidCarNumber(carNumber)) {
            throw new IllegalArgumentException("Illegal Car Number!");
        }
        this.carNumber = carNumber;
    }

    public String getName() {
        return name;
    }

    public final void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Illegal name");
        }
        this.name = name;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public final void setStartTime(String str) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        if (str == null || str.isEmpty() || !Parking.validDate(str))  {
            throw new IllegalArgumentException("Illegal Parking.startTime value");
        }
        this.startTime = LocalDateTime.parse(str, formatter);
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public final void setEndTime(String str) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        if (str == null || str.isEmpty() || !Parking.validDate(str))  {
            throw new IllegalArgumentException("Illegal Parking.endTime value");
        }
        this.endTime = LocalDateTime.parse(str, formatter);
    }

    public double getTime() { return totalTime; }
    public void setTime() {
        this.totalTime = calculateTotalTime(this.startTime, this.endTime);
    }

    public double getPrice() { return price; }

    public final void setPrice(String strPrice) {
        boolean isError = false;
        double p = 0;
        try {
            p = Double.parseDouble(strPrice);
        } catch (Error | Exception e) {
            isError = true;
        }
        if (isError || p <= 0) {
            throw new IllegalArgumentException("Illegal price");
        }
        this.price = p;
    }

    public static Parking read(Scanner fin, PrintStream out) throws IOException,
            NumberFormatException {
        String str;
        Parking parking = new Parking();
        parking.carNumber = fin.nextLine().trim();
        if (!Parking.isValidCarNumber(parking.carNumber)) {
            throw new IOException("Invalid car number: " + parking.carNumber);
        }

        if (!nextRead(P_name, fin, out)) {
            return null;
        }
        parking.name = fin.nextLine();

        if (!nextRead(P_start, fin, out)) {
            return null;
        }
        str = fin.nextLine();
        if (!Parking.validDate(str)) {
            throw new IOException("Invalid Parking.startTime value");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        parking.startTime = LocalDateTime.parse(str, formatter);

        if (!nextRead(P_end, fin, out)) {
            return null;
        }
        str = fin.nextLine();
        if (Parking.validDate(str) == false) {
            throw new IOException("Invalid Parking.endTime value");
        }
        parking.endTime = LocalDateTime.parse(str, formatter);

        if (!nextRead(P_price, fin, out)) {
            return null;
        }
        str = fin.nextLine();
        parking.price = Double.parseDouble(str);

        parking.totalTime = calculateTotalTime(parking.startTime, parking.endTime);
        return parking;
    }

    public Parking() {
    }

    public static final String AREA_DEL = "\n";

    public String toString() {
        return new String(
                carNumber + AREA_DEL +
                        name + AREA_DEL +
                        startTime + AREA_DEL +
                        endTime + AREA_DEL +
                        totalTime + AREA_DEL +
                        price
        );
    }
}
