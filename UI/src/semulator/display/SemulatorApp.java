package semulator.display;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.program.Sprogram;
import semulator.logic.xml.xmlreader.XMLParser;



class SemulatorApp {

    private Sprogram currentProgram;                 // התוכנית הטעונה כרגע
    private final XMLParser xmlParser = new XMLParser(); // הפרסר שלך כפי שסיפקת
    private Path lastLoadedPath;


    private final Scanner scanner = new Scanner(System.in);
    private final List<String> history = new ArrayList<>();
    private boolean running = true;

    private final List<RunRecord> runHistory = new ArrayList<>();
    private boolean programValid = false;

    private String currentProgramName = "No program loaded.";


    public void start() {



        showIntroOnce();  // <-- one-time intro before the menu
        while (running) {
            printMenu();
            int choice = readMenuChoice(1, 6);
            switch (choice) {
                case 1 -> handleLoadProgramFile();
                case 2 -> handleShowProgram(currentProgram);
                case 3 -> handleExpand(currentProgram);
                //case 4 -> handleRunProgram();
                //case 5 -> handleShowHistory();
                //case 6 -> handleExit();
                default -> System.out.println("Invalid choice. Please try again.");
            }
            System.out.println(); // spacing between loops
        }
    }

    private void handleRunProgram() {

    }

    private void handleExpand(Sprogram currentProgram) {

        if (currentProgram == null || !programValid) {
            System.out.println(">> No valid program loaded. Please load a program first.");
            pauseForEnter();
            return;
        }
        if (currentProgram.getInstructions() == null || currentProgram.getInstructions().isEmpty()) {
            System.out.println(">> Program has no instructions to expand.");
            pauseForEnter();
            return;
        }

        int maxDegree = currentProgram.calculateMaxDegree();

        System.out.println("=== Expand Program ===");
        System.out.println("Max degree for this program: " + maxDegree);

        while (true) {
            System.out.print("Enter expansion degree (0 - " + maxDegree +"). "
                    + "Press N to load a new program, or X to return: ");

            String line;
            try {
                line = scanner.nextLine();
            } catch (Exception e) {
                System.out.println("Input aborted. Returning to menu.");
                return;
            }
            if (line == null) return;

            line = line.trim();
            if (line.equalsIgnoreCase("X")) {
                return; // חזרה לתפריט
            }
            if (line.equalsIgnoreCase("N")) {
                handleLoadProgramFile(); // טעינת קובץ חדש
                return; // בסיום – חזרה לתפריט
            }

            try {
                int degree = Integer.parseInt(line);
                if (degree >= 0 && degree <= maxDegree) {
                    System.out.println(">> Expansion degree selected: " + degree);

                    if (degree>0)
                    {
                        Sprogram expandProgram = currentProgram.expand(degree);
                        handleShowProgram(expandProgram);
                    }
                    else
                    {
                        currentProgram.setNumberInstructions();
                        handleShowProgram(currentProgram);
                    }

                    pauseForEnter();
                    return;
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.printf("Invalid input. Please enter a number between %d and %d, or N/X.%n",
                    0, maxDegree);
        }
    }

    private void handleShowProgram (Sprogram currentProgram)
    {
        if (currentProgram == null || !programValid) {
            System.out.println(">> No valid program loaded. Please load a program first.");
            pauseForEnter();
            return;
        }

        System.out.println("=== Program ===");
        System.out.println("Name: " + currentProgram.getName());

        if (currentProgram.getInstructions() == null || currentProgram.getInstructions().isEmpty()) {
            System.out.println("(Program has no instructions)");
            pauseForEnter();
            return;
        }

        System.out.println("Input variables used by the program (in order): ");
        System.out.println(currentProgram.stringInputVariable());
        System.out.println("Labels used by the program (in order; EXIT listed last if used): ");
        System.out.println(currentProgram.stringLabelNamesWithExitLast());
        System.out.println("");

        for (int i=0; i<currentProgram.getInstructions().size(); i++)
        {
            System.out.print(currentProgram.getInstructions().get(i).toDisplayString());
            if (currentProgram.getInstructions().get(i).getParentInstruction() != null)
            {
                printaParents(currentProgram.getInstructions().get(i).getParentInstruction());
            }
            System.out.println("");
        }

        pauseForEnter();
    }

    private void printaParents(Sinstruction instruction) {
        if (instruction.getParentInstruction() == null) {
            System.out.print(" >>> " + instruction.toDisplayString());;
        }
        else{
            System.out.print( " >>> " + instruction.toDisplayString());;
            printaParents(instruction.getParentInstruction());

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

        String input = sanitizePathInput(raw);
        if (input.isEmpty()) {
            System.out.println(">> Canceled.");
            pauseForEnter();
            return;
        }

        Path path;
        try {
            path = Paths.get(input).toAbsolutePath().normalize();
        } catch (Exception ex) {
            System.out.println(">> Invalid path: " + ex.getMessage());
            pauseForEnter();
            return; // שומר על התוכנית התקינה הקיימת, אם יש
        }

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
            System.out.println(">> Warning: file extension is not .xml ");
            pauseForEnter();
            return;
        }

        try {
            Sprogram program = xmlParser.loadProgramFromXML(path.toString());

            if (!program.validate())
            {
                System.out.println(">> Program validation failed: a jump targets an undefined label.");
                pauseForEnter();
                return;
            }

            this.currentProgram = program;
            this.lastLoadedPath = path;
            runHistory.clear();
            this.programValid = true;
            this.currentProgramName = currentProgram.getName();

            System.out.println(">> Program loaded successfully from:");
            System.out.println("   " + path);

        } catch (Exception ex) {
            System.out.println(">> Failed to load program: " +
                    ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage()));
        }

        pauseForEnter();
    }



    private static String sanitizePathInput(String s) {
        if (s == null) return "";
        String t = s.trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            t = t.substring(1, t.length() - 1);
        }
        t = t.replace("\uFEFF","").replaceAll("\\p{Cf}","").replace('\u00A0',' ').trim();
        return t;
    }


    private void showIntroOnce() {
        System.out.println("Hello! How are you? Hope you're doing well...");
        System.out.println("Welcome to Semulator.");
        System.out.println("When you're ready to begin, press ENTER!");
        pauseForEnter();
    }

    private void printMenu() {
        System.out.println("Current program: " + currentProgramName);
        System.out.println("");
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




