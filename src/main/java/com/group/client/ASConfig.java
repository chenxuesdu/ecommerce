package com.group.client;

/**
 * Created by ahbbc on 2017/3/16.
 */
public class ASConfig {
    private String elbName;
    private String securityGroupName;
    private String configName;
    private String asgroupName;
    private String policyName;
    private String region;
    private String imageID;
    private String alarmName;
    private String instanceType;
    private  String availablityZone;

    public String getElbName() {
        return elbName;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public String getConfigName() {
        return configName;
    }

    public String getAsgroupName() {
        return asgroupName;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public String getRegion() {
        return region;
    }

    public String getImageID() {
        return imageID;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getAvailablityZone() {
        return availablityZone;
    }

    public void setElbName(String elbName) {
        this.elbName = elbName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public void setAsgroupName(String asgroupName) {
        this.asgroupName = asgroupName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setImageID(String imageID) {
        this.imageID = imageID;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public void setAvailablityZone(String availablityZone) {
        this.availablityZone = availablityZone;
    }

}
