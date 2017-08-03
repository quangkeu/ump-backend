package vn.ssdc.vnpt.devices.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.Tr069Parameter;
import vn.ssdc.vnpt.devices.model.Tr069Profile;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.List;

/**
 * Created by kiendt on 2/7/2017.
 */
@Service
public class Tr069ProfileService extends SsdcCrudService<Long, Tr069Profile> {

    private static final Logger logger = LoggerFactory.getLogger(Tr069ProfileService.class);

    @Autowired
    public Tr069ProfileService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Tr069Profile.class);
    }


    @Autowired
    public Tr069ParameterService tr069ParameterService;


    public String parseParameter(Node profileNode, Integer type) {
        JsonArray arrayParameter = new JsonArray();
        List<Node> listObjectNode = profileNode.selectNodes("object");
        for (Node tmp : listObjectNode) {
            String baseObjectName = tmp.valueOf("@ref");
            List<Node> listParameterNodes = tmp.selectNodes("parameter");
            for (Node tmpParameter : listParameterNodes) {
                String pathParameter = baseObjectName + tmpParameter.valueOf("@ref");
                if(type == 2){
                    pathParameter = "InternetGatewayDevice.Services." + pathParameter;
                }
                else if( type == 3){
                    pathParameter = "Device.Services." + pathParameter;
                }
                Tr069Parameter tr069Parameter = tr069ParameterService.searchByPath(pathParameter);
                if(tr069Parameter != null) {
                    JsonObject objectParameter = new Gson().fromJson(new Gson().toJson(tr069Parameter), JsonObject.class);
                    arrayParameter.add(objectParameter);
                }
            }
        }
        return arrayParameter.toString();
    }

    public List<Tr069Profile> getProfileIsDiagnostics() {
        String whereExp = "diagnostics=1 ";
        List<Tr069Profile> list = this.repository.search(whereExp);
        if (!ObjectUtils.empty(list)) {
            return list;
        }
        return null;
    }

}
