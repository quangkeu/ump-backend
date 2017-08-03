package vn.ssdc.vnpt;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.Tr069Profile;
import vn.ssdc.vnpt.devices.services.Tr069ParameterService;
import vn.ssdc.vnpt.devices.services.Tr069ProfileService;

import java.io.InputStream;
import java.util.List;

/**
 * Created by THANHLX on 2/7/2017.
 */
@Component
public class Tr069DataLoader implements ApplicationRunner {

    Logger logger = LoggerFactory.getLogger(Tr069DataLoader.class);
    private static final String ROOT_FILE = "tr069-data/dataModel-include.xml";

    private static final String KEY_NAME = "@name";
    private static final String KEY_PATH = "@path";

    @Autowired
    Tr069ParameterService tr069ParameterService;

    @Autowired
    Tr069ProfileService tr069ProfileService;

    public void run(ApplicationArguments args) {
        try {
            if (tr069ParameterService.getAll().size() == 0) {
                Document document = readFile(ROOT_FILE);
                List<Node> listFileImport = document.selectNodes("//tr069Import/root/dataModel");
                for (Node node : listFileImport) {
                    processNodeImport(node,1);
                }

                List<Node> listFileImport_serviceDataModel = document.selectNodes("//tr069Import/servicedatamodel/dataModel");
                for (Node node : listFileImport_serviceDataModel) {
                    processNodeImport(node, 2);
                }

            }
        } catch (DocumentException e) {
            logger.error("DocumentException when importing data");
        }
    }

    private void processNodeImport(Node node, Integer type) throws DocumentException, DuplicateKeyException {
        String nameFile = node.valueOf(KEY_NAME);
        List<Node> listFile = node.selectNodes("include");

        for (Node file : listFile) {
            String filePath = file.valueOf(KEY_PATH);
            Document document = readFile(filePath);

            List<Node> objects = document.selectNodes("//dm:document/model/object");
            List<Node> dataTypeNodes = document.selectNodes("//dm:document/dataType");
            List<Node> profiles = document.selectNodes("//dm:document/model/profile");
            logger.info("{}", "Start insert to paramters table");
            for (Node object : objects) {
                List<Node> listParameterNode = object.selectNodes("parameter");
                for (Node childNode : listParameterNode) {
                    if(type == 1){
                        tr069ParameterService.create(tr069ParameterService.processChildObjectNode(dataTypeNodes, object, childNode, profiles, nameFile,1));
                    }else{
                        tr069ParameterService.create(tr069ParameterService.processChildObjectNode(dataTypeNodes, object, childNode, profiles, nameFile,2));
                        tr069ParameterService.create(tr069ParameterService.processChildObjectNode(dataTypeNodes, object, childNode, profiles, nameFile,3));
                    }
                }
            }
            logger.info("{}", "Finish insert to parametes table");
            logger.info("{}", "Start insert to profiles");
            for (Node object : profiles) {
                Tr069Profile tr069Profile = new Tr069Profile();
                String uniqueProfileName = object.valueOf(KEY_NAME);
                tr069Profile.version = object.valueOf("@dmr:version");
                if (type == 1) {
                    tr069Profile.name = uniqueProfileName + ":" + nameFile;
                    tr069Profile.parameters = tr069ProfileService.parseParameter(object, 1);
                    if (tr069Profile.name.contains("Diagnostics") || tr069Profile.parameters.contains("Diagnostics")) {
                        tr069Profile.diagnostics = true;
                    } else {
                        tr069Profile.diagnostics = false;
                    }
                    tr069ProfileService.create(tr069Profile);
                } else if (type == 2) {
                    //Add services for InternetGatewayDevice Root
                    tr069Profile.name = uniqueProfileName + ":" + nameFile + ":InternetGatewayDevice";
                    tr069Profile.parameters = tr069ProfileService.parseParameter(object, 2);
                    if (tr069Profile.name.contains("Diagnostics") || tr069Profile.parameters.contains("Diagnostics")) {
                        tr069Profile.diagnostics = true;
                    } else {
                        tr069Profile.diagnostics = false;
                    }
                    tr069ProfileService.create(tr069Profile);
                    //Add services for Device Root
                    tr069Profile.id = null;
                    tr069Profile.name = uniqueProfileName + ":" + nameFile + ":Device";
                    tr069Profile.parameters = tr069ProfileService.parseParameter(object, 3);
                    if (tr069Profile.name.contains("Diagnostics") || tr069Profile.parameters.contains("Diagnostics")) {
                        tr069Profile.diagnostics = true;
                    } else {
                        tr069Profile.diagnostics = false;
                    }
                    tr069ProfileService.create(tr069Profile);
                }
            }
            logger.info("{}", "Finish insert to profiles");
        }
    }

    private Document readFile(String fileName) throws DocumentException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        SAXReader reader = new SAXReader();
        Document document = reader.read(is);
        return document;

    }

}
