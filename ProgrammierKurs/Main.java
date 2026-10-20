package ProgrammierKurs;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Hier schreiben");

        boolean offen = true;

        while (offen) {

            String nachricht = scanner.nextLine();
            int size = nachricht.length();

            switch (nachricht.toLowerCase()) {
                case "hallo":
                    System.out.println("JP sagt hallo");

                    break;

                case "jp":
                    System.out.println("Ich komme ");

                    break;
                case "bergneels":
                    System.out.println("Gönergy Goat");
                    break;

                default:
                    if (size >= 10) {

                        System.out.println("Halts maul");

                    } else
                        System.out.println("Deine Nachricht: " + nachricht);

                    break;
            }

            if (nachricht.equalsIgnoreCase("ex"))
                offen = false;
        }

        scanner.close();

    }

}