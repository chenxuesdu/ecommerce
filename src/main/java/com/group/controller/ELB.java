package com.group.controller;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;

import java.util.*;

/**
 * Created by wtang on 3/4/17.
 */
public class ELB {
    /*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

    /**
     * Welcome to your new AWS Java SDK based project!
     *
     * This class is meant as a starting point for your console-based application that
     * makes one or more calls to the AWS services supported by the Java SDK, such as EC2,
     * SimpleDB, and S3.
     *
     * In order to use the services in this sample, you need:
     *
     *  - A valid Amazon Web Services account. You can register for AWS at:
     *       https://aws-portal.amazon.com/gp/aws/developer/registration/index.html
     *
     *  - Your account's Access Key ID and Secret Access Key:
     *       http://aws.amazon.com/security-credentials
     *
     *  - A subscription to Amazon EC2. You can sign up for EC2 at:
     *       http://aws.amazon.com/ec2/
     *
     */

    /*
     * Before running the code:
     *      Fill in your AWS access credentials in the provided credentials
     *      file template, and be sure to move the file to the default location
     *      (~/.aws/credentials) where the sample code will load the
     *      credentials from.
     *      https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING:
     *      To avoid accidental leakage of your credentials, DO NOT keep
     *      the credentials file in your source directory.
     */

        public static void main(String[] args) {

            final String PENDING_STATUS = "pending";
            final String ACTIVE_STATUS = "active";
            final String RUNNING_STATUS = "running";
            final String STOPPED_STATUS = "stopped";


            String ec2ConfigFilePath = "/Users/wtang/Documents/295/ecommerce/src/main/java/com/group/controller/ec2Config.yaml";
            String elbConfigFilePath = "/Users/wtang/Documents/295/ecommerce/src/main/java/com/group/controller/elbConfig.yaml";

            ELBv2Clients elbClients = new ELBv2Clients(elbConfigFilePath);

            EC2Clients ec2Instance = new EC2Clients(ec2ConfigFilePath);

            List<String> subnetIdList = ec2Instance.getSubnetId();

            for (int i = 0; i < 4; i++) {
                String instanceId = ec2Instance.createEC2Instance(
                        ec2Instance.ec2Config.getImageID(),
                        ec2Instance.ec2Config.getInstanceType(),
                        ec2Instance.ec2Config.getMinInstanceCount(),
                        ec2Instance.ec2Config.getMaxInstanceCount(),
                        ec2Instance.ec2Config.getKeyName(),
                        ec2Instance.ec2Config.getSecurityGroupId(),
                        subnetIdList.get(i%2));

                System.out.println("The newly created ec2 instance has an ID: " + instanceId);
                System.out.println(ec2Instance.getInstanceId());
                System.out.println("**********************");
            }

            try {
                System.out.println("Sleep 20 seconds starting all EC2 instances.");
                Thread.sleep(20000);    // Wait until all ec2 instances are up.
            } catch (Exception e) {
                System.out.print(e);
            }

            List<String> ec2List = ec2Instance.listInstances(RUNNING_STATUS);


            //List<String> ec2List = ec2Instance.listInstances(STOPPED_STATUS);

            for (String inst : ec2List) {
                ec2Instance.startInstance(inst);
                //ec2Instance.stopInstance(inst);
            }


            try {
                Thread.sleep(20000);    // Wait until all ec2 instances are up.
            } catch (Exception e) {
                System.out.print(e);
            }

            List<String> ec2InstanceSubnetIds = ec2Instance.listInstanceSubnetIds(RUNNING_STATUS);
            for (String s : ec2InstanceSubnetIds) System.out.println(s);

            List<SecurityGroup> securityGroups = ec2Instance.getSecurityGroupId(elbClients.elbConfig.getSecurityGroupName());
            List<String> securityGroupId = new ArrayList<>();
            for (SecurityGroup s : securityGroups) {
                securityGroupId.add(s.getGroupId());
            }

            List<String> ec2InstanceVpcIds = ec2Instance.listInstanceVpcIds(RUNNING_STATUS);
            for (String s : ec2InstanceVpcIds) System.out.println(s);

            List<String> ec2RunningInstances = ec2Instance.listInstances(RUNNING_STATUS);

            String elbState = elbClients.createELB(elbClients.elbConfig.getName(), securityGroupId, ec2InstanceSubnetIds, ec2RunningInstances);

            while (!elbState.equals(ACTIVE_STATUS)){
                try {
                    System.out.println("Sleep 20 seconds waiting ELB becoming Active.");
                    Thread.sleep(20000);    // Wait until all ec2 instances are up.
                } catch (Exception e) {
                    System.out.println(e);
                }
                elbState = elbClients.getELBState(elbClients.elbArn);
                System.out.println(elbState);
            }

            List<String> metricList = new ArrayList<>();
            metricList.add("ActiveConnectionCount");
            metricList.add("HealthyHostCount");
            metricList.add("HTTPCode_ELB_4XX_Count");
            metricList.add("HTTPCode_ELB_5XX_Count");
            metricList.add("HTTPCode_Target_2XX_Count");
            metricList.add("HTTPCode_Target_3XX_Count");
            metricList.add("HTTPCode_Target_4XX_Count");
            metricList.add("HTTPCode_Target_5XX_Count");
            metricList.add("RequestCount");
            metricList.add("TargetResponseTime");
            metricList.add("UnHealthyHostCount");
            metricList.add("ProcessedBytes");

            /*
            for (String tn : elbClients.elbTargetGroupArn) {
                elbClients.listELBMetrics(elbClients.elbArn, tn, metricList);
            }
            */

            try {
                System.out.println("Sleep 60 seconds collecting metric stats.");
                Thread.sleep(60000);    // Wait until all ec2 instances are up.
            } catch (Exception e) {
                System.out.println(e);
            }

            String metricName = "UnHealthyHostCount";
            for (String tn : elbClients.elbTargetGroupArn) {
                elbClients.getELBMetricStats(elbClients.elbArn, tn, metricName);
            }

            System.out.println(elbClients.getELBDNSName(elbClients.elbArn));

        }

}
