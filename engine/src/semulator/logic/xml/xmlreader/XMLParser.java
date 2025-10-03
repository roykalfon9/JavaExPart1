package semulator.logic.xml.xmlreader;

import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.functionInput.ConstNumberInput;
import semulator.logic.functionInput.FunctionInput;
import semulator.logic.functions.Function;
import semulator.logic.instruction.*;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.program.Sprogram;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;
import semulator.logic.xml.schema.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XMLParser implements IXMLParser {

    private static final String JAVX_XML_PACKAGE_NAME = "semulator.logic.xml.schema";
    private static final Pattern CONST_NUMBER = Pattern.compile("(?i)^const[-_]?(\\d+)$");

    @Override
    public Sprogram loadProgramFromXML(String filePath) throws Exception {
        validateXmlFilePath(filePath);

        SProgram xmlProgram = xmlToObject(filePath);
        if (xmlProgram == null) throw new IllegalStateException("Failed to deserialize XML into SProgram (null).");

        SprogramImpl program = new SprogramImpl(xmlProgram.getName());
        Map<String, Function> fnRegistry = new LinkedHashMap<>();

        // Pass 1: declare all functions
        SFunctions xfuncs = xmlProgram.getSFunctions();
        if (xfuncs != null && xfuncs.getSFunction() != null) {
            for (SFunction xf : xfuncs.getSFunction()) {
                if (!fnRegistry.containsKey(xf.getName())) {
                    SprogramImpl execBody = new SprogramImpl(xf.getName());
                    SprogramImpl rawBody  = new SprogramImpl(xf.getName());
                    Function f = new Function(xf.getName(), xf.getUserString(), execBody, rawBody);
                    fnRegistry.put(xf.getName(), f);
                    program.addFunction(f);
                }
            }
        }

        // Pass 2: fill function bodies
        if (xfuncs != null && xfuncs.getSFunction() != null) {
            for (SFunction xf : xfuncs.getSFunction()) {
                Function f = fnRegistry.get(xf.getName());
                Sprogram rawBody  = f.getInstructionProgram();
                Sprogram execBody = f.getInstructionExecuteProgram();
                if (xf.getSInstructions() != null && xf.getSInstructions().getSInstruction() != null) {
                    for (SInstruction xi : xf.getSInstructions().getSInstruction()) {
                        Sinstruction ins = mapXmlInstructionToDomain(xi, fnRegistry);
                        rawBody.addInstruction(ins);
                        execBody.addInstruction(ins); // mirror (or deep-copy if needed)
                    }
                }
            }
        }

        // Main program instructions
        if (xmlProgram.getSInstructions() != null && xmlProgram.getSInstructions().getSInstruction() != null) {
            for (SInstruction xmlIns : xmlProgram.getSInstructions().getSInstruction()) {
                Sinstruction domIns = mapXmlInstructionToDomain(xmlIns, fnRegistry);
                program.addInstruction(domIns);
            }
        }

        return program;
    }

    private SProgram xmlToObject(String filePath) throws FileNotFoundException, JAXBException {
        try (InputStream inputStream = new FileInputStream(new File(filePath))) {
            return deserializeFrom(inputStream);
        } catch (java.io.IOException e) {
            throw new JAXBException("I/O error reading XML file", e);
        }
    }

    private static SProgram deserializeFrom(InputStream in) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(JAVX_XML_PACKAGE_NAME);
        Unmarshaller v = jc.createUnmarshaller();
        return (SProgram) v.unmarshal(in);
    }

    private Sinstruction mapXmlInstructionToDomain(SInstruction xmlIns, Map<String, Function> fnRegistry) {
        String name = xmlIns.getName();

        switch (name) {
            case "INCREASE": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new IncreaseInstruction(v) : new IncreaseInstruction(v, lbl);
            }
            case "DECREASE": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new DecreaseInstruction(v) : new DecreaseInstruction(v, lbl);
            }
            case "ZERO_VARIABLE": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new ZeroVariableInstruction(v) : new ZeroVariableInstruction(v, lbl);
            }
            case "NEUTRAL": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new NeutralInstruction(v) : new NeutralInstruction(v, lbl);
            }
            case "ASSIGNMENT": {
                Variable dst = analyzeVariable(xmlIns.getSVariable());
                SInstructionArguments c = xmlIns.getSInstructionArguments();
                Variable src = analyzeVariable(c.getSInstructionArgument().get(0).getValue());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new AssigmentInstruction(dst, src) : new AssigmentInstruction(dst, src, lbl);
            }
            case "GOTO_LABEL": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                Label target = parseLabel(xmlIns.getSInstructionArguments().getSInstructionArgument().get(0).getValue());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new GoToLabelInstruction(v, target) : new GoToLabelInstruction(v, target, lbl);
            }
            case "CONSTANT_ASSIGNMENT": {
                Variable dst = analyzeVariable(xmlIns.getSVariable());
                long c = Long.parseLong(xmlIns.getSInstructionArguments().getSInstructionArgument().get(0).getValue());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new ConstantAssignmentInstruction(dst, c) : new ConstantAssignmentInstruction(dst, c, lbl);
            }
            case "JUMP_ZERO": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                Label target = parseLabel(xmlIns.getSInstructionArguments().getSInstructionArgument().get(0).getValue());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new JumpZeroInstruction(v, target) : new JumpZeroInstruction(v, target, lbl);
            }
            case "JUMP_NOT_ZERO": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                Label target = parseLabel(xmlIns.getSInstructionArguments().getSInstructionArgument().get(0).getValue());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new JumpNotZeroInstruction(v, target) : new JumpNotZeroInstruction(v, target, lbl);
            }
            case "JUMP_EQUAL_CONSTANT": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                List<SInstructionArgument> args = xmlIns.getSInstructionArguments().getSInstructionArgument();
                Label target = parseLabel(args.get(0).getValue());
                long c = Long.parseLong(args.get(1).getValue());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new JumpEqualConstantInstruction(v, target, c) : new JumpEqualConstantInstruction(v, target, c, lbl);
            }
            case "JUMP_EQUAL_VARIABLE": {
                Variable v = analyzeVariable(xmlIns.getSVariable());
                List<SInstructionArgument> args = xmlIns.getSInstructionArguments().getSInstructionArgument();
                Label target = parseLabel(args.get(0).getValue());
                Variable rhs = analyzeVariable(args.get(1).getValue());
                Label lbl = parseLabel(xmlIns.getSLabel());
                return (lbl == FixedLabel.EMPTY) ? new JumpEqualVariableInstruction(v, target, rhs) : new JumpEqualVariableInstruction(v, target, rhs, lbl);
            }
            case "QUOTE": {
                Variable dst = analyzeVariable(xmlIns.getSVariable());
                Label lbl = parseLabel(xmlIns.getSLabel());

                String fnName = null;
                String rawArgs = "";
                SInstructionArguments c = xmlIns.getSInstructionArguments();
                if (c != null && c.getSInstructionArgument() != null) {
                    for (SInstructionArgument a : c.getSInstructionArgument()) {
                        String an = a.getName() != null ? a.getName().trim() : "";
                        if ("functionName".equalsIgnoreCase(an)) {
                            fnName = a.getValue();
                        } else if ("functionArguments".equalsIgnoreCase(an)) {
                            rawArgs = a.getValue() != null ? a.getValue() : "";
                        }
                    }
                }
                if (fnName == null || fnName.isBlank()) throw new IllegalArgumentException("QUOTE missing functionName");

                Function targetFn = fnRegistry.get(fnName);
                if (targetFn == null) {
                    SprogramImpl execBody = new SprogramImpl(fnName);
                    SprogramImpl rawBody  = new SprogramImpl(fnName);
                    targetFn = new Function(fnName, fnName, execBody, rawBody);
                    fnRegistry.put(fnName, targetFn);
                }

                List<FunctionInput> finputs = parseFunctionInputs(rawArgs, fnRegistry);

                QuoteInstruction q = (lbl == FixedLabel.EMPTY) ? new QuoteInstruction(dst, targetFn)
                        : new QuoteInstruction(dst, targetFn, lbl);
                for (FunctionInput fi : finputs) q.addFunctionInput(fi);
                return q;
            }
            case "JUMP_EQUAL_FUNCTION": {
                // לפי הבקשה: אין יותר משתנה שני בקונסטרקטור
                Variable lhs = analyzeVariable(xmlIns.getSVariable());
                Label lbl = parseLabel(xmlIns.getSLabel());

                String fnName = null;
                String rawArgs = "";
                Label jumpTo = FixedLabel.EMPTY;

                SInstructionArguments c = xmlIns.getSInstructionArguments();
                if (c != null && c.getSInstructionArgument() != null) {
                    List<SInstructionArgument> args = c.getSInstructionArgument();
                    for (int i = 0; i < args.size(); i++) {
                        SInstructionArgument a = args.get(i);
                        String an = a.getName() == null ? "" : a.getName().trim().toLowerCase(Locale.ROOT);
                        String av = a.getValue();

                        if ("functionname".equals(an)) {
                            fnName = av;
                        } else if ("functionarguments".equals(an)) {
                            rawArgs = (av == null) ? "" : av;
                        } else if ("jumpto".equals(an)) {
                            jumpTo = parseLabel(av);
                        } else {
                            // תמיכה בפורמט פוזיציוני ישן: הארגומנט הראשון הוא ה־jumpTo
                            if (i == 0 && jumpTo == FixedLabel.EMPTY) jumpTo = parseLabel(av);
                            // שאר הארגומנטים (אם קיימים) מתעלמים כאן – אין RHS עוד.
                        }
                    }
                }

                if (fnName == null || fnName.isBlank()) throw new IllegalArgumentException("JUMP_EQUAL_FUNCTION missing functionName");
                if (jumpTo == FixedLabel.EMPTY)         throw new IllegalArgumentException("JUMP_EQUAL_FUNCTION missing jumpTo label");

                Function targetFn = fnRegistry.get(fnName);
                if (targetFn == null) {
                    SprogramImpl execBody = new SprogramImpl(fnName);
                    SprogramImpl rawBody  = new SprogramImpl(fnName);
                    targetFn = new Function(fnName, fnName, execBody, rawBody);
                    fnRegistry.put(fnName, targetFn);
                }

                List<FunctionInput> finputs = parseFunctionInputs(rawArgs, fnRegistry);

                JumpEqualFunctionInstruction ins = (lbl == FixedLabel.EMPTY)
                        ? new JumpEqualFunctionInstruction(lhs, jumpTo, targetFn)
                        : new JumpEqualFunctionInstruction(lhs, jumpTo, targetFn, lbl);

                for (FunctionInput fi : finputs) ins.addFunctionInput(fi);
                return ins;
            }

            default:
                throw new UnsupportedOperationException("Unrecognized instruction name: '" + xmlIns.getName() + "'");
        }
    }

    // -------- Function-args parsing (כולל תיקון NOT/Comma לפני '(') --------
    private List<FunctionInput> parseFunctionInputs(String raw, Map<String, Function> fnRegistry) {
        List<String> tokens = splitTopLevel(raw);
        List<FunctionInput> out = new ArrayList<>(tokens.size());

        for (String t : tokens) {
            String tok = t.trim();
            if (tok.isEmpty()) continue;

            // הסרת זוג סוגריים חיצוני אחד אם עוטף את כל הטוקן
            if (tok.startsWith("(") && tok.endsWith(")") && matchingParens(tok)) {
                tok = tok.substring(1, tok.length() - 1).trim();
                if (tok.isEmpty()) continue;
            }

            // מספר נקי
            if (tok.chars().allMatch(Character::isDigit)) {
                out.add(new ConstNumberInput(Long.parseLong(tok)));
                continue;
            }

            // CONSTn
            Matcher m = CONST_NUMBER.matcher(tok);
            if (m.matches()) {
                out.add(new ConstNumberInput(Long.parseLong(m.group(1))));
                continue;
            }

            int lp = tok.indexOf('(');
            int rp = tok.endsWith(")") ? tok.lastIndexOf(')') : -1;
            int firstComma = tok.indexOf(',');

            // אם יש פסיק לפני '(' — פרש כ-Name,rest (מקרה NOT,AND וכו')
            if (lp > 0 && firstComma >= 0 && firstComma < lp) {
                String fname = tok.substring(0, firstComma).trim();
                String inner = tok.substring(firstComma + 1).trim();
                if (!fname.isEmpty() && fnRegistry.containsKey(fname)) {
                    out.add(buildQuoteAsInput(fname, inner, fnRegistry));
                    continue;
                }
            }

            // Name(args) רגיל
            if (lp > 0 && rp == tok.length() - 1 && (firstComma < 0 || firstComma > lp)) {
                String fname = tok.substring(0, lp).trim();
                String inner = tok.substring(lp + 1, rp);
                out.add(buildQuoteAsInput(fname, inner, fnRegistry));
                continue;
            }

            // Name  או  Name,rest ללא סוגריים
            if (firstComma < 0) {
                if (fnRegistry.containsKey(tok)) {
                    out.add(buildQuoteAsInput(tok, "", fnRegistry));
                } else {
                    Variable v = analyzeVariable(tok);
                    out.add(varInput(v));
                }
            } else {
                String fname = tok.substring(0, firstComma).trim();
                String inner = tok.substring(firstComma + 1).trim();
                if (fnRegistry.containsKey(fname)) {
                    out.add(buildQuoteAsInput(fname, inner, fnRegistry));
                } else {
                    Variable v = analyzeVariable(tok);
                    out.add(varInput(v));
                }
            }
        }
        return out;
    }

    private boolean matchingParens(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth < 0) return false;
            }
        }
        return depth == 0;
    }

    private FunctionInput buildQuoteAsInput(String fname, String inner, Map<String, Function> reg) {
        Function f = reg.get(fname);
        if (f == null) {
            SprogramImpl execBody = new SprogramImpl(fname);
            SprogramImpl rawBody  = new SprogramImpl(fname);
            f = new Function(fname, fname, execBody, rawBody);
            reg.put(fname, f);
        }
        List<FunctionInput> innerInputs = parseFunctionInputs(inner, reg);
        Variable dummy = new VariableImpl(VariableType.WORK, 1); // יעד דמה ל-QUOTE מקונן
        QuoteInstruction nested = new QuoteInstruction(dummy, f);
        for (FunctionInput fi : innerInputs) nested.addFunctionInput(fi);
        return nested;
    }

    private FunctionInput varInput(Variable v) {
        return new FunctionInput() {
            @Override public Long getValue(ExecutionContext ctx) { return ctx.getVariablevalue(v); }
            @Override public String toDisplay() { return v.getRepresentation().toLowerCase(Locale.ROOT); }
            @Override public void addVar(Map<Variable, Long> programVariableState) { programVariableState.putIfAbsent(v, 0L); }
        };
    }

    // split by top-level commas only
    private List<String> splitTopLevel(String s) {
        if (s == null || s.isBlank()) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out.stream().map(String::trim).filter(t -> !t.isEmpty()).collect(Collectors.toList());
    }

    private Label parseLabel(String s) {
        if (s == null || s.isBlank()) return FixedLabel.EMPTY;
        String up = s.trim().toUpperCase(Locale.ROOT);
        if ("EXIT".equals(up)) return FixedLabel.EXIT;
        if (up.charAt(0) == 'L') {
            String digits = up.substring(1);
            if (!digits.isEmpty() && digits.chars().allMatch(Character::isDigit)) {
                return new LabelImp(Integer.parseInt(digits));
            }
        }
        if (up.chars().allMatch(Character::isDigit)) {
            return new LabelImp(Integer.parseInt(up));
        }
        throw new IllegalArgumentException("Invalid label: " + s);
    }

    private Variable analyzeVariable(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Missing S-Variable. Expected Xn/Yn/Wn, or just 'Y'.");
        }
        String up = name.trim().toUpperCase(Locale.ROOT);
        char kind = up.charAt(0);
        String digits = (up.length() > 1) ? up.substring(1) : "";

        if (kind == 'Y' && digits.isEmpty()) return new VariableImpl(VariableType.RESULT, 1);

        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid S-Variable '" + name + "'. Expected Xn/Yn/Wn (e.g., X1), or just 'Y'.");
        }

        int num = Integer.parseInt(digits);
        if (kind == 'X')      return new VariableImpl(VariableType.INPUT, num);
        else if (kind == 'Y') return new VariableImpl(VariableType.RESULT, num);
        else                  return new VariableImpl(VariableType.WORK, num);
    }

    public static void validateXmlFilePath(String filePath) throws Exception {
        if (filePath == null || filePath.isBlank()) throw new IllegalArgumentException("Path is null/blank.");
        Path p = Paths.get(filePath);
        if (!Files.exists(p)) throw new java.io.FileNotFoundException("File not found: " + filePath);
        if (!Files.isRegularFile(p)) throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        if (!Files.isReadable(p)) throw new java.nio.file.AccessDeniedException("File is not readable: " + filePath);
        String name = p.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xml")) throw new IllegalArgumentException("Expected a .xml file, got: " + name);
    }
}
