package vn.ssdc.vnpt.subscriber.model;

import vn.vnpt.ssdc.jdbc.SsdcEntity;

import java.util.Set;

public class SubscriberTemplate extends SsdcEntity<Long> {

    public String name;
    public Set<String> templateKeys;

}