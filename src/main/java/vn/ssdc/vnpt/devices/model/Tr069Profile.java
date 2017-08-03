package vn.ssdc.vnpt.devices.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 * Created by kiendt on 2/7/2017.
 */
public class Tr069Profile  extends SsdcEntity<Long>{

    public String name;
    public String version;
    public String parameters;
    //tuanha2
    public boolean diagnostics;
}
