package semulator.logic.xml.xmlreader;

import semulator.logic.api.Sinstruction;
import semulator.logic.program.Sprogram;
import org.w3c.dom.Element;

public interface IXMLParser {

    Sprogram loadProgramFromXML(String filePath) throws Exception;
}

