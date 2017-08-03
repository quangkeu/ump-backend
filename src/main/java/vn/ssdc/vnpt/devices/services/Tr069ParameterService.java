package vn.ssdc.vnpt.devices.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.dom4j.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Tr069Parameter;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;
import vn.vnpt.ssdc.utils.ObjectUtils;

import java.util.Arrays;
import java.util.List;


/**
 * Created by kiendt on 2/7/2017.
 */
@Service
public class Tr069ParameterService extends SsdcCrudService<Long, Tr069Parameter> {
    private static final Logger logger = LoggerFactory.getLogger(Tr069ParameterService.class);


    List<String> allowParams = Arrays.asList(new String[]{"int", "string", "unsignedInt", "object", "unsignedLong", "dateTime", "base64", "boolean"});
    List<String> allowRules = Arrays.asList(new String[]{"enumeration", "range", "size"});

    @Autowired
    public Tr069ParameterService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(Tr069Parameter.class);
    }

    public Tr069Parameter processChildObjectNode(List<Node> dataTypeListNode, Node objectNode, Node childNode, List<Node> listProfileNode, String baseProfileName,Integer type) {
        Tr069Parameter tr069Parameter = new Tr069Parameter();
        if(type ==  1){
            tr069Parameter.path = objectNode.valueOf("@name") + childNode.valueOf("@name");
            tr069Parameter.parentObject = objectNode.valueOf("@name");
        }else if(type == 2){
            tr069Parameter.path = "InternetGatewayDevice.Services." + objectNode.valueOf("@name") + childNode.valueOf("@name");
            tr069Parameter.parentObject = "InternetGatewayDevice.Services." + objectNode.valueOf("@name");
            baseProfileName = baseProfileName + ":InternetGatewayDevice";
        }else if(type == 3){
            tr069Parameter.path = "Device.Services." + objectNode.valueOf("@name") + childNode.valueOf("@name");
            tr069Parameter.parentObject = "Device.Services." + objectNode.valueOf("@name");
            baseProfileName = baseProfileName + ":Device";
        }
        tr069Parameter.access = childNode.valueOf("@access") != null ? childNode.valueOf("@access") : "";
        tr069Parameter.version = childNode.valueOf("@dmr:version") != null ? childNode.valueOf("@dmr:version") : "";
        tr069Parameter.description = childNode.selectSingleNode("description") != null ? childNode.selectSingleNode("description").getText() : "";
        tr069Parameter.profileNames = parseProfile(listProfileNode, baseProfileName, childNode, objectNode);
        tr069Parameter.defaultValue = "";
        tr069Parameter.otherAttributes = "";
        /// tuanha2
        String[] strReturn = parseDataType(dataTypeListNode, childNode).split("@@@");
        if (strReturn.length == 2) {
            tr069Parameter.dataType = strReturn[0];
            tr069Parameter.rule = strReturn[1];
        } else {
            tr069Parameter.dataType = strReturn[0];
        }
        //
        return tr069Parameter;
    }

    public String parseDataType(List<Node> dataTypeListNodes, Node parameterNode) {
        // if type is dataType
        if (parameterNode.selectSingleNode("syntax") != null) {
            Node dataTypeNode = parameterNode.selectSingleNode("syntax").selectSingleNode("dataType");
            // if dataType node existed and it has baseName -> reference to node base
            if (dataTypeNode != null) {
                String refName = dataTypeNode != null ? dataTypeNode.valueOf("@ref") : "";
                Node node = checkExist(dataTypeListNodes, refName);
                if (node != null && !node.valueOf("@base").isEmpty()) {
                    Node baseNode = checkExist(dataTypeListNodes, node.valueOf("@base"));
                    if (baseNode != null) {
                        List<Node> childNodesDataType = baseNode.selectNodes("//*");
                        for (Node tmp : childNodesDataType) {
                            if (allowParams.contains(tmp.getName())) {
                                // tam thoi bo qua phan size va range
                                return tmp.getName();
                            }
                        }
                    }
                } else if (node != null && node.valueOf("@base").isEmpty()) {
                    List<Node> childNodesDataType = node.selectNodes("//*");
                    for (Node tmp : childNodesDataType) {
                        if (allowParams.contains(tmp.getName())) {
                            // tam thoi bo qua phan size va range
                            return tmp.getName();
                        }
                    }

                }
            } else {
                //1st.Get All Child In Syntax
                String strReturn = "";
                //
                List<Node> lSyntaxChild = parameterNode.selectSingleNode("syntax").selectNodes("*");
                //2st.If Contain Type Return
                for (Node tmp : lSyntaxChild) {
                    if (allowParams.contains(tmp.getName())) {
                        strReturn += tmp.getName();
                        //3st.Get Rule
                        List<Node> lRule = tmp.selectNodes("*");
                        //
                        String strRuleType = "";
                        //Enum
                        String strRule_enum = "";
                        //Range
                        String strRule_rangeMin = "";
                        String strRule_rangeMax = "";
                        //Size
                        String strRule_sizeMin = "";
                        String strRule_sizeMax = "";


                        for (Node tmpRule : lRule) {
                            if (allowRules.contains(tmpRule.getName())) {
                                if ("enumeration".equals(tmpRule.getName())) {
                                    strRuleType = "enumeration";
                                    if (!tmpRule.valueOf("@value").equals("")) {
                                        strRule_enum += tmpRule.valueOf("@value") + ";";
                                    }
                                } else if ("range".equals(tmpRule.getName())) {
                                    strRuleType = "range";
                                    if (tmpRule.valueOf("@minInclusive") != null) {
                                        strRule_rangeMin = tmpRule.valueOf("@minInclusive");
                                    }
                                    if (tmpRule.valueOf("@maxInclusive") != null) {
                                        strRule_rangeMax = tmpRule.valueOf("@maxInclusive");
                                    }
                                } else if ("size".equals(tmpRule.getName())) {
                                    strRuleType = "size";
                                    if (tmpRule.valueOf("@minLength") != null) {
                                        strRule_sizeMin = tmpRule.valueOf("@minLength");
                                    }
                                    if (tmpRule.valueOf("@maxLength") != null) {
                                        strRule_sizeMax = tmpRule.valueOf("@maxLength");
                                    }
                                }
                            }
                        }

                        if (strRuleType.equals("enumeration")) {
                            //Bỏ đi kí tự ; ở cuối
                            if (strRule_enum.length() > 0) {
                                strRule_enum = strRule_enum.substring(0, strRule_enum.length() - 1);
                                strReturn += "@@@[" + strRule_enum + "]";
                            }
                        } else if (strRuleType.equals("range")) {
                            //Nếu có cả min và max
                            if (!strRule_rangeMin.equals("") && !strRule_rangeMax.equals("")) {
                                strReturn += "@@@[" + strRule_rangeMin + "-" + strRule_rangeMax + "]";
                            }// Nếu chỉ có min
                            else if (!strRule_rangeMin.equals("") && strRule_rangeMax.equals("")) {
                                strReturn += "@@@[>" + strRule_rangeMin + "]";
                            }// Nếu chỉ có max
                            else {
                                strReturn += "@@@[<" + strRule_rangeMax + "]";
                            }
                        } else if (strRuleType.equals("size")) {
                            //Nếu có cả min và max
                            if (!strRule_sizeMin.equals("") && !strRule_sizeMax.equals("")) {
                                strReturn += "@@@[" + strRule_sizeMin + "-" + strRule_sizeMax + "]";
                            }// Nếu chỉ có min
                            else if (!strRule_sizeMin.equals("") && strRule_sizeMax.equals("")) {
                                strReturn += "@@@[>" + strRule_sizeMin + "]";
                            } else {
                                strReturn += "@@@[<" + strRule_sizeMax + "]";
                            }
                        }
                    }
                }
                return strReturn;
            }

            // if type is not dataType
            for (String tmp : allowParams) {
                if (parameterNode.selectSingleNode("syntax").selectSingleNode(tmp) != null) {
                    return tmp;
                }
            }
        }
        return "";
    }

    private String parseProfile(List<Node> listProfiles, String baseName, Node childNode, Node objectNode) {
        StringBuilder result = new StringBuilder();
        for (Node node : listProfiles) {
            List<Node> listObjectNode = node.selectNodes("object");
            for (Node tmp : listObjectNode) {
                if (tmp.valueOf("@ref") != null && tmp.valueOf("@ref").equalsIgnoreCase(objectNode.valueOf("@name"))) {
                    List<Node> listParameterNodes = tmp.selectNodes("parameter");
                    for (Node tmpParameter : listParameterNodes) {
                        if (childNode.valueOf("@name").equalsIgnoreCase(tmpParameter.valueOf("@ref"))) {
                            result.append(node.valueOf("@name")).append(":").append(baseName).append(",");
                            break;
                        }
                    }
                }
                if (result.indexOf(node.valueOf("@name") + ":" + baseName) != -1) {
                    break;
                }
            }
        }
        if (result.length() > 0) {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    private String generateDataTypeStandart(String dataType, String min, String max, String size) {
        return "";
    }

    private Node checkExist(List<Node> listNode, String refName) {
        for (Node tmpDataType : listNode) {
            if (tmpDataType.valueOf("@name").equalsIgnoreCase(refName)) {
                return tmpDataType;
            }
        }
        return null;
    }

    public Tr069Parameter searchByPath(String path) {
        String whereExp = "path=? ";
        List<Tr069Parameter> list = this.repository.search(whereExp, path);
        if (!ObjectUtils.empty(list)) {
            return list.get(0);
        }
        return null;
    }

    public Tr069Parameter isTr069ParameterStandard(String tr069Path) {
        String whereExp = "path=? ";
        List<Tr069Parameter> list = this.repository.search(whereExp, tr069Path);
        if (!ObjectUtils.empty(list)) {
            return list.get(0);
        }
        return null;

    }

    public String convertToTr069Param(String path) {
        return path.replaceAll("\\.(\\d+)\\.", ".{i}.");
    }

    public String getProfileNameByPath(String tr069Path) {
        String whereExp = "path=? ";
        List<Tr069Parameter> list = this.repository.search(whereExp, tr069Path);
        if (!ObjectUtils.empty(list)) {
            return list.get(0).profileNames;
        }
        return null;
    }

}
