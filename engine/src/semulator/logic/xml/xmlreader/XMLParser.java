package semulator.logic.xml.xmlreader;

import semulator.logic.api.Sinstruction;
import semulator.logic.instruction.*;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.program.Sprogram;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;
import semulator.logic.xml.schema.SInstruction;
import semulator.logic.xml.schema.SInstructionArgument;
import semulator.logic.xml.schema.SInstructionArguments;
import semulator.logic.xml.schema.SProgram;

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
import java.util.Locale;

public class XMLParser implements IXMLParser {

    private final static String JAVX_XML_PACKAGE_NAME = "semulator.logic.xml.schema";

    @Override
    public Sprogram loadProgramFromXML(String filePath) throws Exception {
        validateXmlFilePath (filePath);

        SProgram XmlProgram = xmlToObject(filePath);
        if (XmlProgram == null) {
            throw new IllegalStateException("Failed to deserialize XML into SProgram (null).");
        }
        if (XmlProgram.getSInstructions() == null || XmlProgram.getSInstructions().getSInstruction().isEmpty()) {
            return new SprogramImpl(XmlProgram.getName());
        }

        SprogramImpl program = new SprogramImpl(XmlProgram.getName());

        for (SInstruction xmlIns : XmlProgram.getSInstructions().getSInstruction()) {
            Sinstruction domIns = mapXmlInstructionToDomain(xmlIns);
            program.addInstruction(domIns);
        }

        return program;
    }

    private SProgram xmlToObject (String filePath) throws FileNotFoundException, JAXBException {
        SProgram program = null;
        try {
            InputStream inputStream = new FileInputStream(new File(filePath));
            program = deserializeFrom(inputStream);
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }
        return program;
    }

    private static SProgram deserializeFrom(InputStream in) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(JAVX_XML_PACKAGE_NAME);
        Unmarshaller v = jc.createUnmarshaller();
        return (SProgram) v.unmarshal(in);
    }

    private Sinstruction mapXmlInstructionToDomain(SInstruction xmlIns) {
        Sinstruction PIns = null;

        switch (xmlIns.getName()) {

            case "INCREASE":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new IncreaseInstruction(Pvar);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new IncreaseInstruction(Pvar,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new IncreaseInstruction(Pvar,Plabel);
                }

                return PIns;
            }

            case "DECREASE":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                Label Plabel;
                String PlabelName = xmlIns.getSLabel();

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new DecreaseInstruction(Pvar);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new DecreaseInstruction(Pvar,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new DecreaseInstruction(Pvar,Plabel);
                }

                return PIns;
            }

            case "ZERO_VARIABLE":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                Label Plabel;
                String PlabelName = xmlIns.getSLabel();

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new ZeroVariableInstruction(Pvar);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new ZeroVariableInstruction(Pvar,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new ZeroVariableInstruction(Pvar,Plabel);
                }
                return PIns;
            }
            case "JUMP_NOT_ZERO":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                SInstructionArguments argContainer = xmlIns.getSInstructionArguments();
                SInstructionArgument firstArg = argContainer.getSInstructionArgument().get(0);
                String PArgumentLabelName = firstArg.getValue();
                Label PArgumentLabel;

                if  (PArgumentLabelName.toUpperCase().equals("EXIT"))
                {
                    PArgumentLabel = FixedLabel.EXIT;
                }
                else
                {
                    int n = Integer.parseInt(PArgumentLabelName.substring(1));
                    PArgumentLabel = new LabelImp(n);
                }

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new JumpNotZeroInstruction(Pvar, PArgumentLabel);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new JumpNotZeroInstruction(Pvar,PArgumentLabel,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new JumpNotZeroInstruction(Pvar,PArgumentLabel,Plabel);
                }
                return PIns;

            }
            case "NEUTRAL":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                Label Plabel;
                String PlabelName = xmlIns.getSLabel();

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new NeutralInstruction(Pvar);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new NeutralInstruction(Pvar,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new NeutralInstruction(Pvar,Plabel);
                }

                return PIns;

            }
            case "ASSIGNMENT":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                SInstructionArguments argContainer = xmlIns.getSInstructionArguments();
                SInstructionArgument firstArg = argContainer.getSInstructionArgument().get(0);
                String PArgumentVarName = firstArg.getValue();
                Variable PArgumentVar = analyzeVariable(PArgumentVarName);

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new AssigmentInstruction(Pvar, PArgumentVar);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new AssigmentInstruction(Pvar,PArgumentVar,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new AssigmentInstruction(Pvar,PArgumentVar,Plabel);
                }
                return PIns;
            }

            case "GOTO_LABEL":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                SInstructionArguments argContainer = xmlIns.getSInstructionArguments();
                SInstructionArgument firstArg = argContainer.getSInstructionArgument().get(0);
                String PArgumentLabelName = firstArg.getValue();
                Label PArgumentLabel;

                if  (PArgumentLabelName.toUpperCase().equals("EXIT"))
                {
                    PArgumentLabel = FixedLabel.EXIT;
                }
                else
                {
                    int n = Integer.parseInt(PArgumentLabelName.substring(1));
                    PArgumentLabel = new LabelImp(n);
                }

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new GoToLabelInstruction(Pvar, PArgumentLabel);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new GoToLabelInstruction(Pvar,PArgumentLabel,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new GoToLabelInstruction(Pvar,PArgumentLabel,Plabel);
                }
                return PIns;


            }

            case "CONSTANT_ASSIGNMENT":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                SInstructionArguments argContainer = xmlIns.getSInstructionArguments();
                SInstructionArgument firstArg = argContainer.getSInstructionArgument().get(0);
                String PArgumentLonglName = firstArg.getValue();
                long PArgumentLong = Long.parseLong(PArgumentLonglName);

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new ConstantAssignmentInstruction(Pvar, PArgumentLong);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new ConstantAssignmentInstruction(Pvar,PArgumentLong,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new ConstantAssignmentInstruction(Pvar,PArgumentLong,Plabel);
                }
                return PIns;
            }

            case "JUMP_ZERO":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                SInstructionArguments argContainer = xmlIns.getSInstructionArguments();
                SInstructionArgument firstArg = argContainer.getSInstructionArgument().get(0);
                String PArgumentLabelName = firstArg.getValue();
                Label PArgumentLabel;

                if  (PArgumentLabelName.toUpperCase().equals("EXIT"))
                {
                    PArgumentLabel = FixedLabel.EXIT;
                }
                else
                {
                    int n = Integer.parseInt(PArgumentLabelName.substring(1));
                    PArgumentLabel = new LabelImp(n);
                }

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new JumpZeroInstruction(Pvar, PArgumentLabel);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new JumpZeroInstruction(Pvar,PArgumentLabel,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new JumpZeroInstruction(Pvar,PArgumentLabel,Plabel);
                }
                return PIns;
            }

            case "JUMP_EQUAL_CONSTANT":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                SInstructionArguments argContainer = xmlIns.getSInstructionArguments();
                SInstructionArgument firstArg = argContainer.getSInstructionArgument().get(0);
                String PArgumentLabelName = firstArg.getValue();
                Label PArgumentLabel;
                SInstructionArgument secArg = argContainer.getSInstructionArgument().get(1);
                String PArgumentLongName = secArg.getValue();
                long PArgumentLong = Long.parseLong(PArgumentLongName);


                if  (PArgumentLabelName.toUpperCase().equals("EXIT"))
                {
                    PArgumentLabel = FixedLabel.EXIT;
                }
                else
                {
                    int n = Integer.parseInt(PArgumentLabelName.substring(1));
                    PArgumentLabel = new LabelImp(n);
                }

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new JumpEqualConstantInstruction(Pvar, PArgumentLabel,PArgumentLong);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new JumpEqualConstantInstruction(Pvar,PArgumentLabel,PArgumentLong,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new JumpEqualConstantInstruction(Pvar,PArgumentLabel,PArgumentLong,Plabel);
                }
                return PIns;
            }

            case "JUMP_EQUAL_VARIABLE":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                SInstructionArguments argContainer = xmlIns.getSInstructionArguments();
                SInstructionArgument firstArg = argContainer.getSInstructionArgument().get(0);
                String PArgumentLabelName = firstArg.getValue();
                Label PArgumentLabel;
                SInstructionArgument secArg = argContainer.getSInstructionArgument().get(1);
                String PArgumentVarName = secArg.getValue();
                Variable PArgumentVar = analyzeVariable(PArgumentVarName);


                if  (PArgumentLabelName.toUpperCase().equals("EXIT"))
                {
                    PArgumentLabel = FixedLabel.EXIT;
                }
                else
                {
                    int n = Integer.parseInt(PArgumentLabelName.substring(1));
                    PArgumentLabel = new LabelImp(n);
                }

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName == null || PlabelName.isEmpty())
                {
                    PIns = new JumpEqualVariableInstruction(Pvar, PArgumentLabel,PArgumentVar);
                }
                else if (PlabelName.toUpperCase().equals("EXIT"))
                {
                    Plabel = FixedLabel.EXIT;
                    PIns = new JumpEqualVariableInstruction(Pvar,PArgumentLabel,PArgumentVar,Plabel);
                }
                else
                {
                    int n = Integer.parseInt(PlabelName.substring(1));
                    Plabel = new LabelImp(n);
                    PIns = new JumpEqualVariableInstruction(Pvar,PArgumentLabel,PArgumentVar,Plabel);
                }
                return PIns;

            }

            default:
                throw new UnsupportedOperationException("Unrecognized instruction name: '" + xmlIns.getName() + "'");
        }
    }



    private Variable analyzeVariable(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Missing S-Variable. Expected Xn/Yn/Wn, or just 'Y'.");
        }

        String up = name.trim().toUpperCase(Locale.ROOT);
        char kind = up.charAt(0);
        String digits = (up.length() > 1) ? up.substring(1) : "";

        if (kind == 'Y' && digits.isEmpty()) {
            return new VariableImpl(VariableType.RESULT, 1);
        }

        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException(
                    "Invalid S-Variable '" + name + "'. Expected Xn/Yn/Wn (e.g., X1), or just 'Y'.");
        }

        int num = Integer.parseInt(digits);
        if (kind == 'X') {
            return new VariableImpl(VariableType.INPUT, num);
        } else if (kind == 'Y') {
            return new VariableImpl(VariableType.RESULT, num);
        } else {
            return new VariableImpl(VariableType.WORK, num);
        }
    }

    private static void validateXmlFilePath(String filePath) throws Exception {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Path is null/blank.");
        }
        Path p = Paths.get(filePath);
        if (!Files.exists(p)) {
            throw new java.io.FileNotFoundException("File not found: " + filePath);
        }
        if (!Files.isRegularFile(p)) {
            throw new IllegalArgumentException("Path is not a regular file: " + filePath);
        }
        if (!Files.isReadable(p)) {
            throw new java.nio.file.AccessDeniedException("File is not readable: " + filePath);
        }
        String name = p.getFileName().toString();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            throw new IllegalArgumentException("Expected a .xml file, got: " + name);
        }
    }
}
