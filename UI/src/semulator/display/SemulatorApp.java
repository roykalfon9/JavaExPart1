package semulator.display;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import semulator.logic.program.Sprogram;
import semulator.logic.xml.xmlreader.XMLParser;



class SemulatorApp {

    private Sprogram currentProgram;                 // התוכנית הטעונה כרגע
    private final XMLParser xmlParser = new XMLParser(); // הפרסר שלך כפי שסיפקת
    private Path lastLoadedPath;


    private final Scanner scanner = new Scanner(System.in);
    private final List<String> history = new ArrayList<>();
    private boolean running = true;

    public void start() {
        showIntroOnce();  // <-- one-time intro before the menu
        while (running) {
            printMenu();
            int choice = readMenuChoice(1, 6);
            switch (choice) {
                case 1 -> handleLoadProgramFile();
                //case 2 -> handleShowProgram();
                //case 3 -> handleExpand();
                //case 4 -> handleRunProgram();
                //case 5 -> handleShowHistory();
                //case 6 -> handleExit();
                default -> System.out.println("Invalid choice. Please try again.");
            }
            System.out.println(); // spacing between loops
        }
    }

    private void handleLoadProgramFile() {

        System.out.print("Enter program file path (XML). Leave empty to cancel: ");
        String raw;
        try {
            raw = scanner.nextLine();
        } catch (Exception e) {
            System.out.println(">> Could not read input. Returning to menu.");
            pauseForEnter();
            return;
        }

        String input = (raw == null) ? "" : raw.trim();
        if (input.isEmpty()) {
            System.out.println(">> Canceled.");
            pauseForEnter();
            return;
        }

        // Strip surrounding quotes if the user pasted a quoted path
        if ((input.startsWith("\"") && input.endsWith("\"")) ||
                (input.startsWith("'") && input.endsWith("'"))) {
            input = input.substring(1, input.length() - 1);
        }

        Path path = Paths.get(input).toAbsolutePath().normalize();

        // בדיקות קלילות לפני הקריאה לפרסר (הפרסר גם בודק בעצמו, אבל זה נותן הודעות מוקדמות ונוחות)
        if (!Files.exists(path)) {
            System.out.println(">> File not found: " + path);
            pauseForEnter();
            return;
        }
        if (!Files.isRegularFile(path)) {
            System.out.println(">> Path is not a regular file: " + path);
            pauseForEnter();
            return;
        }
        if (!path.getFileName().toString().toLowerCase().endsWith(".xml")) {
            System.out.println(">> Warning: file extension is not .xml (continuing anyway).");
        }

        try {
            // שימוש ישיר ב-XMLParser שלך: מקבל String path ומחזיר Sprogram
            Sprogram program = xmlParser.loadProgramFromXML(path.toString());
            this.currentProgram = program;
            this.lastLoadedPath = path;

            System.out.println(">> Program loaded successfully from:");
            System.out.println("   " + path);

        } catch (Exception ex) {
            System.out.println(">> Failed to load program: " +
                    ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()));
        }

        pauseForEnter();
    }






    private void showIntroOnce() {
        System.out.println("Hello! How are you? Hope you're doing well...");
        System.out.println("Welcome to Semulator.");
        System.out.println("When you're ready to begin, press ENTER!");
        pauseForEnter();
    }

    private void printMenu() {
        System.out.println("=== Main Menu ===");
        System.out.println("1. Load program file");
        System.out.println("2. Show program");
        System.out.println("3. Expand (macro expansion)");
        System.out.println("4. Run program");
        System.out.println("5. Show history / statistics");
        System.out.println("6. Exit");
        System.out.print("Select an option (1-6): ");
    }
    private int readMenuChoice(int min, int max) {
        while (true) {
            String line;
            try {
                line = scanner.nextLine();
            } catch (Exception e) {
                System.out.println("\nUnable to read input. Exiting.");
                return max; // default to exit
            }
            int num;
            try {
                num = Integer.parseInt(line.trim());
            } catch (NumberFormatException nfe) {
                System.out.printf("Invalid choice. Please enter a number between %d and %d.%n", min, max);
                System.out.print("Choose again: ");
                continue;
            }
            if (num < min || num > max) {
                System.out.printf("Invalid choice. Please enter a number between %d and %d.%n", min, max);
                System.out.print("Choose again: ");
                continue;
            }
            return num;
        }
    }

    private void pauseForEnter() {
        System.out.print("Press ENTER to continue...");
        try {
            scanner.nextLine();
        } catch (Exception ignored) { }
    }

}




