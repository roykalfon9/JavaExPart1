package semulator.logic.xml.xmlreader;


import org.w3c.dom.Element;
import semulator.logic.api.Sinstruction;
import semulator.logic.execution.ExecutionContext;
import semulator.logic.instruction.DecreaseInstruction;
import semulator.logic.instruction.IncreaseInstruction;
import semulator.logic.instruction.JumpNotZeroInstruction;
import semulator.logic.instruction.ZeroVariableInstruction;
import semulator.logic.label.FixedLabel;
import semulator.logic.label.Label;
import semulator.logic.label.LabelImp;
import semulator.logic.program.Sprogram;
import semulator.logic.program.SprogramImpl;
import semulator.logic.variable.Variable;
import semulator.logic.variable.VariableImpl;
import semulator.logic.variable.VariableType;
import semulator.logic.xml.schema.SInstruction;
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
    public Sprogram loadProgramFromXML(String filePath) throws Exception
    {
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

    private SProgram xmlToObject (String filePath) throws FileNotFoundException, JAXBException
    {
        SProgram program = null;
        try {
            InputStream inputStream = new FileInputStream(new File(filePath));
            program = deserializeFrom(inputStream);
        } catch (JAXBException | FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return program;
    }

    private static SProgram deserializeFrom(InputStream in) throws JAXBException
    {
        JAXBContext jc = JAXBContext.newInstance(JAVX_XML_PACKAGE_NAME);
        Unmarshaller v = jc.createUnmarshaller();
        return (SProgram) v.unmarshal(in);
    }

    private Sinstruction mapXmlInstructionToDomain(SInstruction xmlIns)
    {
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
                    Plabel = new LabelImp(PlabelName.charAt(1));
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

                if (PlabelName.isEmpty())
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
                    Plabel = new LabelImp(PlabelName.charAt(1));
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

                if (PlabelName.isEmpty())
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
                    Plabel = new LabelImp(PlabelName.charAt(1));
                    PIns = new ZeroVariableInstruction(Pvar,Plabel);
                }
                return PIns;
            }
            case "JUMP_NOT_ZERO":
            {
                String PvarName = xmlIns.getSVariable();
                Variable Pvar = analyzeVariable(PvarName);

                String PArgumentLabelName = xmlIns.getSInstructionArguments().getSInstructionArgument().getFirst().getValue();
                Label PArgumentLabel;

                if  (PArgumentLabelName.toUpperCase().equals("EXIT"))
                {
                     PArgumentLabel = FixedLabel.EXIT;
                }
                else
                {
                     PArgumentLabel = new LabelImp(PArgumentLabelName.charAt(1));
                }

                String PlabelName = xmlIns.getSLabel();
                Label Plabel;

                if (PlabelName.isEmpty())
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
                    Plabel = new LabelImp(PlabelName.charAt(1));
                    PIns = new JumpNotZeroInstruction(Pvar,PArgumentLabel,Plabel);
                }
                return PIns;

            }
            /*
            case "NEUTRAL":
            {

            }

            case "JUMP_ZERO":
            {

            }
            case "GOTO_LABEL":
            {

            }
            case "ASSIGNMENT":
            {

            }
            case "CONSTANT_ASSIGNMENT":
            {
            }

            case "JUMP_EQUAL_CONSTANT":
            {
            }

            case "JUMP_EQUAL_VARIABLE":
            {
            }
*/
            default:
                throw new UnsupportedOperationException("Unrecognized instruction name: '" + xmlIns.getName() + "'");
        }
    }
     private Variable analyzeVariable (String PvarName)
     {

         if (PvarName.toUpperCase().charAt(0) == 'X')
         {
             return new VariableImpl(VariableType.INPUT,PvarName.charAt(1));
         }
         else if (PvarName.toUpperCase().charAt(0) == 'Y')
         {
             return new VariableImpl(VariableType.RESULT,PvarName.charAt(1));
         }
         else
         {
             return new VariableImpl(VariableType.WORK,PvarName.charAt(1));
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
