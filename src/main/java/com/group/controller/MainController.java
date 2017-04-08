package com.group.controller;

import com.amazonaws.services.ec2.model.SecurityGroup;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.apache.log4j.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
public class MainController {
	public static boolean ELBExist = false;
	public List<String> ec2List;
	public ELBv2Clients elbClients;
	String elbState;
	private static Logger log = Logger.getLogger(MainController.class);

	@RequestMapping("/")
	public String home() {
		log.info("logging test is good");
		System.out.println(log.getName());
		return "Welcome to our ecommerce system! Weiyu";
	}

	@RequestMapping(value = "/createelb", method = RequestMethod.POST)
	public Map<String, String> createELB() {
		final String PENDING_STATUS = "pending";
		final String ACTIVE_STATUS = "active";
		final String RUNNING_STATUS = "running";
		final String STOPPED_STATUS = "stopped";
		Map<String, String> ret = new HashMap<>();

		if (!ELBExist) {
			String ec2ConfigFilePath = this.getClass().getClassLoader()
					.getResource("ec2Config.yaml").toString();
			String elbConfigFilePath = this.getClass().getClassLoader()
					.getResource("elbConfig.yaml").toString();

			elbClients = new ELBv2Clients(elbConfigFilePath);

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
						subnetIdList.get(i % 2));

				System.out.println("The newly created ec2 instance has an ID: "
						+ instanceId);
				System.out.println(ec2Instance.getInstanceId());
				System.out.println("**********************");
			}

			try {
				System.out
						.println("Sleep 20 seconds starting all EC2 instances.");
				Thread.sleep(20000); // Wait until all ec2 instances are up.
			} catch (Exception e) {
				System.out.print(e);
			}

			ec2List = ec2Instance.listInstances(RUNNING_STATUS);

			// List<String> ec2List = ec2Instance.listInstances(STOPPED_STATUS);

			for (String inst : ec2List) {
				ec2Instance.startInstance(inst);
				// ec2Instance.stopInstance(inst);
			}

			try {
				Thread.sleep(20000); // Wait until all ec2 instances are up.
			} catch (Exception e) {
				System.out.print(e);
			}

			List<String> ec2InstanceSubnetIds = ec2Instance
					.listInstanceSubnetIds(RUNNING_STATUS);
			for (String s : ec2InstanceSubnetIds)
				System.out.println(s);

			List<SecurityGroup> securityGroups = ec2Instance
					.getSecurityGroupId(elbClients.elbConfig
							.getSecurityGroupName());
			List<String> securityGroupId = new ArrayList<>();
			for (SecurityGroup s : securityGroups) {
				securityGroupId.add(s.getGroupId());
			}

			List<String> ec2InstanceVpcIds = ec2Instance
					.listInstanceVpcIds(RUNNING_STATUS);
			for (String s : ec2InstanceVpcIds)
				System.out.println(s);

			List<String> ec2RunningInstances = ec2Instance
					.listInstances(RUNNING_STATUS);

			elbState = elbClients.createELB(elbClients.elbConfig.getName(),
					securityGroupId, ec2InstanceSubnetIds, ec2RunningInstances);

			int i = 0;
			while (!elbState.equals(ACTIVE_STATUS) && i++ < 10) {
				try {
					System.out
							.println("Sleep 20 seconds waiting ELB becoming Active.");
					Thread.sleep(20000); // Wait until all ec2 instances are up.
				} catch (Exception e) {
					System.out.println(e);
				}
				elbState = elbClients.getELBState(elbClients.elbArn);
				System.out.println(elbState);
			}

			if (elbState.equals(ACTIVE_STATUS)) {
				ELBExist = true;
			}
		}

		ret.put("ELBDNS", elbClients.elbDNSName);
		ret.put("ELBArn", elbClients.elbArn);
		ret.put("ELBState", elbState);
		return ret;
	}

}
