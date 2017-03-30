package com.group.controller;
import com.amazonaws.services.ec2.model.InstanceType;
/**
 * Created by wtang on 3/12/17.
 */
public class EC2Config {
    private String name;
    private String securityGroupName;
    private String securityGroupId;
    private String region;
    private String imageID;
    private InstanceType instanceType = InstanceType.T2Micro;
    private String keyName;
    private int minInstanceCount;
    private int maxInstanceCount;

    public String getName() {
        return name;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public String getSecurityGroupId() {
        return securityGroupId;
    }


    public String getRegion() {
        return region;
    }

    public String getImageID() {
        return imageID;
    }

    public InstanceType getInstanceType() {
        return instanceType;
    }

    public String getKeyName() {
        return keyName;
    }

    public int getMinInstanceCount() {
        return minInstanceCount;
    }

    public int getMaxInstanceCount() {
        return maxInstanceCount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    public void setSecurityGroupId(String securityGroupId) {
        this.securityGroupId = securityGroupId;
    }


    public void setRegion(String region) {
        this.region = region;
    }

    public void setImageID(String imageID) {
        this.imageID = imageID;
    }

    public void setInstanceType(InstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public void setMinInstanceCount(int minInstanceCount) {
        this.minInstanceCount = minInstanceCount;
    }

    public void setMaxInstanceCount(int maxInstanceCount) {
        this.maxInstanceCount = maxInstanceCount;
    }

}
