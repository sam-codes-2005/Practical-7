import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.io.*;

public class DOMEcho {
    private static final String OUTPUT_ENCODING = "UTF-8";
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    private final PrintWriter out;
    private int indent = 0;
    private final String basicIndent = " ";

    public DOMEcho(PrintWriter out) {
        this.out = out;
    }

    private void printNodeDetails(Node node) {
        out.print(" nodeName=\"" + node.getNodeName() + "\"");
        String value = node.getNamespaceURI();
        if (value != null) out.print(" uri=\"" + value + "\"");

        value = node.getPrefix();
        if (value != null) out.print(" prefix=\"" + value + "\"");

        value = node.getLocalName();
        if (value != null) out.print(" localName=\"" + value + "\"");

        value = node.getNodeValue();
        if (value != null) {
            out.print(" nodeValue=");
            out.print(value.trim().isEmpty() ? "[Whitespace]" : "\"" + value + "\"");
        }
        out.println();
    }

    private void indentOutput() {
        for (int i = 0; i < indent; i++) {
            out.print(basicIndent);
        }
    }

    private void traverseDOMTree(Node node) {
        indentOutput();
        int type = node.getNodeType();

        switch (type) {
            case Node.ELEMENT_NODE:
                out.print("Element: ");
                printNodeDetails(node);
                NamedNodeMap attributes = node.getAttributes();
                indent += 2;
                for (int i = 0; i < attributes.getLength(); i++) {
                    traverseDOMTree(attributes.item(i));
                }
                indent -= 2;
                break;
            case Node.TEXT_NODE:
                out.print("Text: ");
                printNodeDetails(node);
                break;
            case Node.COMMENT_NODE:
                out.print("Comment: ");
                printNodeDetails(node);
                break;
            case Node.DOCUMENT_NODE:
                out.print("Document: ");
                printNodeDetails(node);
                break;
            default:
                out.print("Other Node: ");
                printNodeDetails(node);
                break;
        }

        indent++;
        Node child = node.getFirstChild();
        while (child != null) {
            traverseDOMTree(child);
            child = child.getNextSibling();
        }
        indent--;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            showUsage();
        }

        boolean validateDTD = false, validateXSD = false;
        String schemaSource = null;
        String filename = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-dtd":
                    validateDTD = true;
                    break;
                case "-xsd":
                    validateXSD = true;
                    break;
                case "-xsdss":
                    if (i == args.length - 1) showUsage();
                    validateXSD = true;
                    schemaSource = args[++i];
                    break;
                default:
                    filename = args[i];
                    break;
            }
        }

        if (filename == null) {
            showUsage();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(validateDTD || validateXSD);

        if (validateXSD) {
            factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            if (schemaSource != null) {
                factory.setAttribute(JAXP_SCHEMA_SOURCE, new File(schemaSource));
            }
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new ErrorHandlerImpl(new PrintWriter(System.err, true)));
        Document document = builder.parse(new File(filename));

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out, OUTPUT_ENCODING), true);
        new DOMEcho(writer).traverseDOMTree(document);
    }

    private static void showUsage() {
        System.err.println("Usage: DOMEcho [-options] <file.xml>");
        System.err.println("Options:");
        System.err.println(" -dtd     Validate using DTD");
        System.err.println(" -xsd     Validate using W3C XML Schema");
        System.err.println(" -xsdss <file.xsd> Specify schema source for validation");
        System.exit(1);
    }

    private static class ErrorHandlerImpl implements ErrorHandler {
        private final PrintWriter out;

        public ErrorHandlerImpl(PrintWriter out) {
            this.out = out;
        }

        private String getExceptionDetails(SAXParseException e) {
            return "URI=" + e.getSystemId() + " Line=" + e.getLineNumber() + ": " + e.getMessage();
        }

        @Override
        public void warning(SAXParseException e) {
            out.println("Warning: " + getExceptionDetails(e));
        }

        @Override
        public void error(SAXParseException e) {
            out.println("Error: " + getExceptionDetails(e));
        }

        @Override
        public void fatalError(SAXParseException e) {
            out.println("Fatal Error: " + getExceptionDetails(e));
        }
    }
}
