/*
package semulator.display;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ProgramExecutorImpl;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableType;
import semulator.logic.xml.xmlreader.XMLParser;



class SemulatorApp {

    private Sprogram currentProgram;                 // התוכנית הטעונה כרגע
    private final XMLParser xmlParser = new XMLParser(); // הפרסר שלך כפי שסיפקת
    private Path lastLoadedPath;


    private final Scanner scanner = new Scanner(System.in);
    private boolean running = true;

    private final List<RunRecord> runHistory = new ArrayList<>();
    private int runNumber = 0;
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
                case 4 -> handleRunProgram(currentProgram);
                case 5 -> handleShowHistory();
                case 6 -> handleExit();
                default -> System.out.println("Invalid choice. Please try again.");
            }
            System.out.println(); // spacing between loops
        }
    }

    private void handleExit() {
        running = false;
        return;
    }

    private void handleShowHistory() {

        if (runHistory.isEmpty())
        {
            System.out.println("No history to show. Please load a program and run it.");
        }
        else {
            for (RunRecord record : runHistory) {
                record.print();
                System.out.println("");
            }
        }
    }


    private void handleRunProgram(Sprogram currentProgram) {

        Sprogram programToRun;
//-------------------------------------------------------------------------------------------------------- האם קיימת תכנית
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
//-------------------------------------------------------------------------------------------------------- קליטת דרגה + ולידציות

        int maxDegree = currentProgram.calculateMaxDegree();

        System.out.println("=== Run Program ===");
        System.out.println("Max degree for this program: " + maxDegree);

        int degree;
        while (true) {
            System.out.print("Enter expansion degree (0 - " + maxDegree + "). "
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
                degree = Integer.parseInt(line);

                if (degree >= 0 && degree <= maxDegree) {
                    System.out.println(">> Expansion degree selected: " + degree);

                    programToRun = (degree == 0) ? currentProgram : currentProgram.expand(degree);
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
        }
//-------------------------------------------------------------------------------------------------------- לקחת ערכי אינפוט + ולידציות

        Long[] inputArray = null; // מערך האינפוט שיכנס בהמשך להרצה

        String[] parts = null;
        while (true) {

            System.out.print("The Input Variables Are: ");
            System.out.println(programToRun.stringInputVariable());
            System.out.print("");
            System.out.print(
                    (!programToRun.getInstructions().isEmpty()
                            ? "Enter comma-separated (x,y,z....) non-negative integers for " + programToRun.stringInputVariable()
                            : "Program uses no explicit input variables")
                            + ". Press ENTER for all zeros, or X to cancel: "
            );

            String csv;

            try {
                csv = scanner.nextLine(); // -------------מקבלים כסטרינג
            } catch (Exception e) {
                System.out.println("Input aborted. Returning to menu.");
                return;
            }
            if (csv == null) return;

            csv = csv.trim();
            if (csv.equalsIgnoreCase("X")) {
                return; // חזרה לתפריט הראשי
            }

            // ENTER ריק -> משאירים את ברירת המחדל (אפסים) וממשיכים
            if (csv.isEmpty()) {
                break;
            }

            parts = csv.split(",");
            boolean bad = false;
            int idx = 1;

            List<Long> inputs = new ArrayList<>();

            for (String raw : parts) {
                if (raw == null) continue;
                String t = raw.trim();
                if (t.isEmpty()) continue;

                long val = Long.parseLong(t); // -------------- ממירים ללונג
                inputs.add(val);

                try {
                    val = Long.parseLong(t);
                } catch (NumberFormatException e) {
                    System.out.println(">> Invalid number: '" + t + "'. Please try again.");
                    bad = true;
                    break;
                }
                if (val < 0) {
                    System.out.println(">> Inputs must be >= 0. Please try again.");
                    bad = true;
                    break;
                }

                inputArray = inputs.toArray(new Long[0]);
            }
            if (!bad) break;
        }

        //--------------------------------------------------------------------------------------------------------הצגת תכנית

        this.handleShowProgram(programToRun);

        System.out.println("");
        System.out.println("");
        System.out.println("");

        //--------------------------------------------------------------------------------------------------------הרצת תכנית

        runNumber++;
        ProgramExecutorImpl exe = new ProgramExecutorImpl(programToRun);

        long result = exe.run(inputArray);

        //--------------------------------------------------------------------------------------------------------הדפסת נתונים


        System.out.println("Program execution result: " + result);
        System.out.println("");

        String InputVarString = formatByTypeSorted(exe.VariableState(), VariableType.INPUT);
        String WorkVarString = formatByTypeSorted(exe.VariableState(), VariableType.WORK);

        System.out.println("Program Values:");
        System.out.println("");

        System.out.println(InputVarString);
        System.out.println(WorkVarString);

        System.out.println("");

        System.out.println("Program Total Cycles: :");
        System.out.println(programToRun.calculateCycle());

        //-------------------------------------------------------------------------------------------------------- עדכון היסטוריה

        RunRecord currentRecord = new RunRecord();
        currentRecord.record(lastLoadedPath ,runNumber, degree, parts, result,programToRun.calculateCycle());
        runHistory.add(currentRecord);
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

        currentProgram.setNumberInstructions();

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
            this.runHistory.clear();
            this.programValid = true;
            this.currentProgramName = currentProgram.getName();
            this.runNumber = 0;


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

    private static String formatByTypeSorted(Map<Variable, Long> state, VariableType type) {
        List<Map.Entry<Variable, Long>> items = new ArrayList<>();
        for (Map.Entry<Variable, Long> e : state.entrySet()) {
            Variable v = e.getKey();
            if (v != null && v.getType() == type) {
                items.add(e);
            }
        }

        items.sort(Comparator.comparingInt(e -> e.getKey().getNumber()));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Variable, Long> e : items) {
            sb.append(e.getKey().getRepresentation())
                    .append('=')
                    .append(e.getValue())
                    .append(System.lineSeparator());
        }
        return sb.toString();
    }


}
*/