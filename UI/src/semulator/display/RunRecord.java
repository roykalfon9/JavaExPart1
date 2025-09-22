package semulator.display;

import java.util.StringJoiner;

public class RunRecord {

    boolean isValid;

    private int runNumber;     // מתחיל מ-1
    private int degree;        // דרגת ההרצה (מהמנוע)
    private String[] inputs;     // הקלטים שהוכנסו (ייצוג טקסטואלי)
    private long yFinal;       // ערך y בגמר הריצה
    private long cycles;       // כמות ה-cycles שנצרכו

    public RunRecord()
    {
        isValid = false;
    }

    public void record(int runNumber, int degree, String[] inputs, long yFinal, long cycles) {

        isValid = true;

        this.runNumber = runNumber;
        this.degree = degree;
        this.inputs = inputs;
        this.yFinal = yFinal;
        this.cycles = cycles;
    }

    public void print()
    {
        if (!isValid) {
            System.out.println(">> No run recorded yet.");
            return;
        }

        String title = String.format("Run #%d Summary", runNumber);
        String bar   = new String(new char[Math.max(30, title.length())]).replace('\0', '=');

        System.out.println(bar);
        System.out.println(title);
        System.out.println(bar);

        System.out.printf("Degree : %d%n", degree);

        String inputsLine;
        if (inputs == null || inputs.length == 0) {
            inputsLine = "(none)  (treated as zeros)";
        } else {
            StringJoiner sj = new StringJoiner(", ");
            for (int i = 0; i < inputs.length; i++) {
                String val = (inputs[i] == null || inputs[i].trim().isEmpty()) ? "0" : inputs[i].trim();
                sj.add("x" + (i + 1) + "=" + val);
            }
            inputsLine = sj.toString();
        }
        System.out.println("Inputs : " + inputsLine);

        System.out.printf("y      : %d%n", yFinal);
        System.out.printf("Cycles : %d%n", cycles);

        System.out.println(bar);

    }

}
