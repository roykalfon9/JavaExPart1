package semulator.logic.debug;

import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.program.Sprogram;
import semulator.logic.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public final class Trace {
    // הפעלה/כיבוי גלובלי
    public static volatile boolean ENABLED = true;

    // מקס’ צעדים לפני בלימה (כדי לזהות לופ)
    public static volatile int MAX_STEPS = 2000;

    // כמה W/X/Y להציג בכל צעד (רק מה שנצפה עד עכשיו)
    public static volatile int SHOW_UP_TO = 6;

    private static final Map<Variable,Boolean> seenVars = new HashMap<>();

    public static void seen(Variable v) {
        if (v != null) seenVars.put(v, Boolean.TRUE);
    }

    public static void stepHeader(int step, int pc, Sprogram prog) {
        if (!ENABLED) return;
        System.out.printf("#%d  pc=%d  program=%s%n", step, pc, prog.getName());
    }

    public static void instr(Sinstruction ins) {
        if (!ENABLED) return;
        System.out.println("   " + ins.toDisplayString());
    }

    public static void vars(ExecutionContext ctx, Variable v1, Variable v2) {
        if (!ENABLED) return;
        String s1 = (v1 == null) ? "-" : v1.getRepresentation().toLowerCase() + "=" + ctx.getVariablevalue(v1);
        String s2 = (v2 == null) ? "-" : v2.getRepresentation().toLowerCase() + "=" + ctx.getVariablevalue(v2);
        System.out.println("   vars: " + s1 + " | " + s2);
    }

    public static void jump(Label j) {
        if (!ENABLED) return;
        String t = (j == null) ? "null" :
                (j == FixedLabel.EMPTY) ? "EMPTY" :
                        (j == FixedLabel.EXIT)  ? "EXIT"  :
                                ("L" + j.getNumber());
        System.out.println("   jump -> " + t);
    }

    public static void state(ExecutionContext ctx) {
        if (!ENABLED) return;
        // מציגים רק משתנים שכבר “נראינו” בהם + מעט X/Y/W ראשונים
        StringBuilder sb = new StringBuilder("   state: ");
        int shown = 0;
        for (Map.Entry<Variable,Boolean> e : seenVars.entrySet()) {
            Variable v = e.getKey();
            sb.append(v.getRepresentation().toLowerCase())
                    .append("=")
                    .append(ctx.getVariablevalue(v))
                    .append("  ");
            if (++shown >= SHOW_UP_TO) break;
        }
        System.out.println(sb.toString());
    }

    public static void warn(String msg) {
        if (ENABLED) System.out.println("   [warn] " + msg);
    }
}
