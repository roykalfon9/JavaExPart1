package semulator.display;

public class RunRecord {

    private final int runNumber;     // מתחיל מ-1
    private final int degree;        // דרגת ההרצה (מהמנוע)
    private final String inputs;     // הקלטים שהוכנסו (ייצוג טקסטואלי)
    private final long yFinal;       // ערך y בגמר הריצה
    private final long cycles;       // כמות ה-cycles שנצרכו

    public RunRecord(int runNumber, int degree, String inputs, long yFinal, long cycles) {
        this.runNumber = runNumber;
        this.degree = degree;
        this.inputs = inputs;
        this.yFinal = yFinal;
        this.cycles = cycles;
    }

    public int getRunNumber() { return runNumber; }
    public int getDegree() { return degree; }
    public String getInputs() { return inputs; }
    public long getyFinal() { return yFinal; }
    public long getCycles() { return cycles; }
}
