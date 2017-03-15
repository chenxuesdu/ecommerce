package com.group.controller;
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

        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
            final Object PENDING_STATUS = "pending";
            String ec2ConfigFilePath = "/Users/wtang/Documents/295/ecommerce/src/main/java/com/group/controller/ec2Config.yaml";
            String elbConfigFilePath = "/Users/wtang/Documents/295/ecommerce/src/main/java/com/group/controller/elbConfig.yaml";


            //AWSCredentials credentials = null;

            ELBClients elbClients = new ELBClients(elbConfigFilePath);

            EC2Clients ec2Instance = new EC2Clients(ec2ConfigFilePath);

            /*
            String instanceId = ec2Instance.createEC2Instance(
                    ec2Instance.ec2Config.getImageID(),
                    ec2Instance.ec2Config.getInstanceType(),
                    ec2Instance.ec2Config.getMinInstanceCount(),
                    ec2Instance.ec2Config.getMaxInstanceCount(),
                    ec2Instance.ec2Config.getKeyName(),
                    ec2Instance.ec2Config.getSecurityGroupName());

            System.out.println("The newly created ec2 instance has an ID: " + instanceId);
            System.out.println(ec2Instance.getInstanceId());
            System.out.println("**********************");
            */

            List<String> ec2List = ec2Instance.listInstances("stopped");

            for (String inst : ec2List) {
                ec2Instance.startInstance(inst);
                //ec2Instance.stopInstance(inst);
            }

            try {
                Thread.sleep(20000);    // Wait until all ec2 instances are up.
            } catch (Exception e) {
                System.out.print(e);
            }

            List<String> ec2InstanceSubnetIds = ec2Instance.listInstanceSubnetIds("running");
            for (String s : ec2InstanceSubnetIds)   System.out.println(s);

            List<SecurityGroup> securityGroups = ec2Instance.getSecurityGroupId(elbClients.elbConfig.getSecurityGroupName());
            List<String> securityGroupId = new ArrayList<String>();
            for (SecurityGroup s : securityGroups) {
                securityGroupId.add(s.getGroupId());
            }

            elbClients.createELB(elbClients.elbConfig.getName(), securityGroupId, ec2InstanceSubnetIds);

            elbClients.listELB();



            //ec2Instance.getInstanceStatus();

            /*
            if (instanceId != null) {
                ec2Instance.getInstanceStatus(instanceId);
            }
            */

        }

}