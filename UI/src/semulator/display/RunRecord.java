package semulator.display;

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

    }

}
