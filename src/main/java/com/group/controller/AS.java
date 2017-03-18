package com.group.controller;

import static java.lang.Thread.sleep;

/**
 * Created by ahbbc on 2017/3/16.
 */
public class AS {
    public static void main(String[] args) throws InterruptedException {
//        String[] tmp = {};
//        ELB elb = new ELB();
//        elb.main(tmp);
//        sleep(5000);

        /*
         * Before executing AS.main(), please run ELB.main() first
         */
        String elbConfigFilePath = "C:/D/ecommerce/src/main/java/com/group/controller/asConfig.yaml";
        String policyArn;

        ASClients as = new ASClients(elbConfigFilePath);
        as.createConfiguration(as.asConfig.getConfigName(), as.asConfig.getImageID(), as.asConfig.getInstanceType(), as.asConfig.getSecurityGroupName());
        sleep(5000);
        as.createASGroup(as.asConfig.getAsgroupName(), as.asConfig.getConfigName(), as.asConfig.getAvailablityZone(), as.asConfig.getElbName());
        sleep(5000);
        policyArn = as.setPolicy(as.asConfig.getAsgroupName(), as.asConfig.getPolicyName());
        as.setAlarm(policyArn, as.asConfig.getAlarmName(), as.asConfig.getAsgroupName());
    }
}