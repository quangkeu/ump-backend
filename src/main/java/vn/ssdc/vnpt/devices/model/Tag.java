package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;
import vn.vnpt.ssdc.jdbc.annotations.Serialized;

import java.util.HashMap;
import java.util.Map;

/**
 * A tag is a collection of parameters <br/>
 * It is assigned to device type for dynamic configuration </br>
 * For example: VoIP tag, WAN tag, Device summary tag, ....
 *
 * Created by vietnq on 11/1/16.
 */
public class Tag extends SsdcEntity<Long>{
    public String name;
    public Map<String,Parameter> parameters;
    public Long deviceTypeVersionId;
    //0 : unassigned, 1 : assigned, use integer for cross-platform db
    public Integer assigned;
    public String assignedGroup;
    public Long rootTagId;
    public Integer synchronize; //0 : off auto synchronize, 1 : on auto synchronize

    public Tag() {
        assigned = 0;
        synchronize = 0;
        parameters = new HashMap<String, Parameter>();
    }

}
