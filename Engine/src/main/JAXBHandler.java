package main;

import main.jaxbClasses.MagitRepository;
import org.apache.commons.io.FileUtils;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;

public class JAXBHandler {
    private final static String JAXB_XML_GENERATED_CLASSES_PACKAGE_NAME = "main/jaxbClasses";

    public static MagitRepository deserializeRepoXML(InputStream i_inputStream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(JAXB_XML_GENERATED_CLASSES_PACKAGE_NAME);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return (MagitRepository) unmarshaller.unmarshal(i_inputStream);
    }

    public static MagitRepository loadXML(String i_xmlPath) throws IOException {
        InputStream inputStream = FileUtils.openInputStream(FileUtils.getFile(i_xmlPath));
        try {
            return deserializeRepoXML(inputStream);
            // System.out.println("name of first country is: " + countries.getCountry().get(0).getName());
        } catch (JAXBException e) {
           e.printStackTrace();
            return null;
        }
    }
}


