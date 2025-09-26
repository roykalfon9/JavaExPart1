package semulator.display;

import javafx.stage.Stage;
import semulator.logic.variable.Variable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class RunRecord {

    private Map<Variable, Long> programVariableState;


    private String ProgramName;
    private Path path;
    private int runNumber;     // מתחיל מ-1
    private int degree;        // דרגת ההרצה (מהמנוע)
    private String[] inputs;   // הקלטים שהוכנסו (ייצוג טקסטואלי)
    private long yFinal;       // ערך y בגמר הריצה
    private long cycles;       // כמות ה-cycles שנצרכו


    public void record(int i, String ProgramName, Path path, int degree, String[] inputs, long yFinal, long cycles, Map<Variable, Long> programVariableState) {

        this.runNumber = i;
        this.ProgramName = ProgramName;
        this.path = path;
        this.degree = degree;
        this.inputs = inputs;
        this.yFinal = yFinal;
        this.cycles = cycles;
        this.programVariableState = programVariableState;
    }

    public String getProgramName() {return ProgramName;}
    public Path getPath() { return this.path; }
    public int getRunNumber() { return this.runNumber; }
    public int getDegree() { return this.degree; }
    public String[] getInputs() { return this.inputs; }
    public long getyFinal() { return this.yFinal; }
    public long getCycles() { return this.cycles; }

    public Map<Variable, Long> getProgramVariableState() {return programVariableState;}
}
