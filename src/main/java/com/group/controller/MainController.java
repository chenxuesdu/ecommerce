package com.group.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amazonaws.services.ec2.model.SecurityGroup;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group.client.ASClients;
import com.group.client.EC2Clients;
import com.group.client.ELBv2Clients;


@RestController
public class MainController {
	public static boolean ELBExist = false;
	public List<String> ec2List;
	public ELBv2Clients elbClients;
	public EC2Clients ec2Clients;
	public ASClients asClients;
	String elbState;
	private static Logger log = Logger.getLogger(MainController.class);
    final static String PENDING_STATUS = "pending";
    final static String ACTIVE_STATUS = "active";
    final static String RUNNING_STATUS = "running";
    final static String STOPPED_STATUS = "stopped";

	@RequestMapping("/")
	public String home() {
		return "Welcome to our ecommerce system!";
	}

    @RequestMapping(value = "/createec2", method = RequestMethod.POST)
    public String createEC2() {
        String ec2ConfigFilePath = this.getClass().getClassLoader()
                .getResource("ec2Config.yaml").getFile();

        if (ec2Clients == null) ec2Clients = new EC2Clients(ec2ConfigFilePath);

        List<String> subnetIdList = ec2Clients.getSubnetId();

        String instanceId = "";
        instanceId= ec2Clients.createEC2Instance(
                    ec2Clients.ec2Config.getImageID(),
                    ec2Clients.ec2Config.getInstanceType(),
                    ec2Clients.ec2Config.getMinInstanceCount(),
                    ec2Clients.ec2Config.getMaxInstanceCount(),
                    ec2Clients.ec2Config.getKeyName(),
                    ec2Clients.ec2Config.getSecurityGroupId(),
                    subnetIdList.get(0));

        log.info("The newly created ec2 instance has an ID: "
                    + instanceId);
        log.info("**********************");

        return instanceId;
    }

    @RequestMapping(value = "/createelb", method = RequestMethod.POST)
	public Map<String, String> createELB() {

		Map<String, String> ret = new HashMap<>();

		if (!ELBExist) {
			String ec2ConfigFilePath = this.getClass().getClassLoader()
					.getResource("ec2Config.yaml").getFile();
			log.info(ec2ConfigFilePath);
			String elbConfigFilePath = this.getClass().getClassLoader()
					.getResource("elbConfig.yaml").getFile();

			elbClients = new ELBv2Clients(elbConfigFilePath);

            ec2Clients = new EC2Clients(ec2ConfigFilePath);

			List<String> subnetIdList = ec2Clients.getSubnetId();

			for (int i = 0; i < 4; i++) {
				String instanceId = ec2Clients.createEC2Instance(
						ec2Clients.ec2Config.getImageID(),
						ec2Clients.ec2Config.getInstanceType(),
						ec2Clients.ec2Config.getMinInstanceCount(),
						ec2Clients.ec2Config.getMaxInstanceCount(),
						ec2Clients.ec2Config.getKeyName(),
						ec2Clients.ec2Config.getSecurityGroupId(),
						subnetIdList.get(i % 2));

				log.info("The newly created ec2 instance has an ID: "
						+ instanceId);
				log.info(ec2Clients.getInstanceId());
				log.info("**********************");
			}

			try {
				System.out
						.println("Sleep 20 seconds starting all EC2 instances.");
				Thread.sleep(20000); // Wait until all ec2 instances are up.
			} catch (Exception e) {
				log.error(e);
			}

			ec2List = ec2Clients.listInstances(RUNNING_STATUS);

			// List<String> ec2List = ec2Clients.listInstances(STOPPED_STATUS);

			for (String inst : ec2List) {
				ec2Clients.startInstance(inst);
				// ec2Clients.stopInstance(inst);
			}

			try {
				Thread.sleep(20000); // Wait until all ec2 instances are up.
			} catch (Exception e) {
				log.info(e);
			}

			List<String> ec2InstanceSubnetIds = ec2Clients
					.listInstanceSubnetIds(RUNNING_STATUS);
			for (String s : ec2InstanceSubnetIds)
				log.info(s);

			List<SecurityGroup> securityGroups = ec2Clients
					.getSecurityGroupId(elbClients.elbConfig
							.getSecurityGroupName());
			List<String> securityGroupId = new ArrayList<>();
			for (SecurityGroup s : securityGroups) {
				securityGroupId.add(s.getGroupId());
			}

			List<String> ec2InstanceVpcIds = ec2Clients
					.listInstanceVpcIds(RUNNING_STATUS);
			for (String s : ec2InstanceVpcIds)
				log.info(s);

			List<String> ec2RunningInstances = ec2Clients
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
					log.info(e);
				}
				elbState = elbClients.getELBState(elbClients.elbArn);
				log.info(elbState);
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

	@RequestMapping(value = "/searchelb", method = RequestMethod.GET)
	public Map<String, String> searchELB(@RequestParam(value="elbArn") String ELBArn) {
		return elbClients.searchELB(ELBArn);
	}

	@RequestMapping(value = "/listelb", method = RequestMethod.GET)
	public Map<String, String> searchELB() {
		return elbClients.listELB();
	}

	@RequestMapping(value = "/getelbdns", method = RequestMethod.GET)
	public String getELBDNS(@RequestParam(value="elbarn") String ELBArn) {
		return elbClients.getELBDNSName(ELBArn);
	}

	@RequestMapping(value = "/getelbstate", method = RequestMethod.GET)
	public String getELBState(@RequestParam(value="elbarn") String ELBArn) {
		return elbClients.getELBState(ELBArn);
	}

	@RequestMapping(value = "/getelbstats", method = RequestMethod.GET)
	public Map<String,Map<String, String>> getELBStats(@RequestParam(value="elbarn") String ELBArn,
                                           @RequestParam(value="metric", defaultValue = "UnHealthyHostCount") String metricName) {
		Map<String, Map<String, String>> ret = new HashMap<>();

		if (elbClients.elbTargetGroupArn.size() > 0 ) {
			for (String tgArn : elbClients.elbTargetGroupArn) {
				ret.put(tgArn, elbClients.getELBMetricStats(ELBArn, tgArn, metricName));
			}
		}
		return ret;
	}

	@RequestMapping(value = "/getelbstatsall", method = RequestMethod.GET)
	public Map<String,List<Map<String, String>>> getELBStatsAll() {
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

		Map<String,List<Map<String, String>>> ret = new HashMap<>();
		String json="";

		if (elbClients!=null && elbClients.elbTargetGroupArn.size() > 0 ) {
            for (String tgArn : elbClients.elbTargetGroupArn) {
                List<Map<String,String>> list = new ArrayList<>();
                for (String metric: metricList) {
                    Map<String, String> stats = elbClients.getELBMetricStats(elbClients.elbArn, tgArn, metric);
                    if(!stats.isEmpty()) list.add(stats);
                }
                ret.put(tgArn, list);
            }
		}

        try {
            json = new ObjectMapper().writeValueAsString(ret);
        } catch (JsonProcessingException e) {
            log.error(e);
        }
		return ret;
	}

	@RequestMapping(value = "/deleteelb", method = RequestMethod.DELETE)
	public String deleteELB(@RequestParam(value="elbarn") String ELBArn) {
		return elbClients.deleteELB(ELBArn);
	}

	@RequestMapping(value = "/listtg", method = RequestMethod.GET)
	public List<String> listTG(@RequestParam(value="elbarn") String ELBArn) {
		return elbClients.describeTargetGroup(ELBArn);
	}

	@RequestMapping(value = "/deletetg", method = RequestMethod.DELETE)
	public Map<String, String> deleteTG() {
		Map<String, String> ret = new HashMap<>();
        if (elbClients!=null && elbClients.elbTargetGroupArn.size() > 0 ) {
            for(String tgArn: elbClients.elbTargetGroupArn) {
                ret.put(tgArn, elbClients.deleteTargetGroup(tgArn));
            }
        }
		return ret;
	}

    @RequestMapping(value = "/listec2instance", method = RequestMethod.GET)
    public List<String> listEC2(@RequestParam(value="state", defaultValue = RUNNING_STATUS) String ec2state) {
        return ec2Clients.listInstances(ec2state);
    }

    @RequestMapping(value = "/stopec2instance", method = RequestMethod.POST)
    public String stopEC2(@RequestParam(value="id") String ec2InstanceId) {
        return ec2Clients.stopInstance(ec2InstanceId);
    }

    @RequestMapping(value = "/deleteec2instance", method = RequestMethod.DELETE)
    public String deleteEC2(@RequestParam(value="id") String ec2InstanceId) {
        return ec2Clients.terminateInstance(ec2InstanceId);
    }

    @RequestMapping(value = "/deleteallec2instance", method = RequestMethod.DELETE)
    public List<String> deleteAllEC2() {
	    List<String> ec2List = ec2Clients.listInstances(RUNNING_STATUS);
	    List<String> ret = new ArrayList<>();
	    for (String ec2 : ec2List) {
            ret.add(ec2Clients.terminateInstance(ec2));
        }
        return ret;
    }

    @RequestMapping(value = "/cleanupelbas", method = RequestMethod.DELETE)
    public Map<String, String> cleanUpAll() {

        Map<String, String> ret = new HashMap<>();
        if (elbClients!=null && asClients != null && !elbClients.elbArn.isEmpty()) {
            ret.put(elbClients.elbArn, elbClients.deleteELB(elbClients.elbArn));
            String asName = asClients.asConfig.getAsgroupName();
            ret.put(asName, deleteAS(asName));
            asClients.deleteLaunchConfiguration(asClients.asConfig.getConfigName());
			asClients.deletePolicy(asName,asClients.asConfig.getPolicyName());
			asClients.deleteAlarm(asClients.asConfig.getAlarmName());

            try {
                Thread.sleep(20000); // Wait until elb and listern to clean up.
            } catch (Exception e) {
                log.error(e);
            }

            if (elbClients.elbTargetGroupArn.size() > 0) {
                for (String tgArn : elbClients.elbTargetGroupArn) {
                    ret.put(tgArn, elbClients.deleteTargetGroup(tgArn));
                }
            }

            List<String> ec2List = ec2Clients.listInstances(RUNNING_STATUS);
            for (String ec2 : ec2List) {
                ret.put(ec2, ec2Clients.terminateInstance(ec2));
            }
        } else {
            ret.put("error", "ELB and AS have not been created yet.");
        }
        return ret;
    }

	@RequestMapping(value = "/createas", method = RequestMethod.POST)
	public Map<String, String> createAS(){
		int capacity = 1;
		String comparisonOperator = "up";
		Map<String, String> ret = new HashMap<>();

		String asConfigFilePath = this.getClass().getClassLoader()
				.getResource("asConfig.yaml").getFile();
		asClients = new ASClients(asConfigFilePath);

		asClients.createConfiguration(asClients.asConfig.getConfigName(),
				asClients.asConfig.getImageID(),
				asClients.asConfig.getInstanceType(),
				asClients.asConfig.getSecurityGroupName());

		try {
			System.out
					.println("Sleep 5 seconds for creating AS config.");
			Thread.sleep(5000); // Wait until all ec2 instances are up.
		} catch (Exception e) {
			log.error(e);
		}

		asClients.createASGroup(asClients.asConfig.getAsgroupName(),
				asClients.asConfig.getConfigName(),
				asClients.asConfig.getAvailablityZone(),
				asClients.asConfig.getElbName(),
                elbClients.elbTargetGroupArn);

		try {
			log.info("Sleep 5 seconds for creating AS group.");
			Thread.sleep(5000); // Wait until all ec2 instances are up.
		} catch (Exception e) {
			log.error(e);
		}
		String policyArn = asClients.setPolicy(asClients.asConfig.getAsgroupName(),
				asClients.asConfig.getPolicyName(),
				capacity,
				"ChangeInCapacity");

		asClients.setBasicAlarm(policyArn,
				asClients.asConfig.getAlarmName(),
				asClients.asConfig.getAsgroupName(),
				comparisonOperator);
		ret.put("ASName", asClients.asConfig.getAsgroupName());
		return ret;
	}

	/*
     * Attach instances with ASGroup
     */
	@RequestMapping(value = "/attachins", method = RequestMethod.POST)
	public String attachInstances(@RequestParam(value="asname") String asgName) {
		String asConfigFilePath = this.getClass().getClassLoader()
				.getResource("asConfig.yaml").getFile();
		asClients = new ASClients(asConfigFilePath);
		return asClients.attachInstances(asgName);
	}

	/*
     * List ASGroup
     */
	@RequestMapping(value = "/listas", method = RequestMethod.POST)
	public Map<String, String> listAS() {
		String asConfigFilePath = this.getClass().getClassLoader()
				.getResource("asConfig.yaml").getFile();
		asClients = new ASClients(asConfigFilePath);
		return asClients.listAS();
	}

	/*
     * Delete specific ASGroup
     */
	@RequestMapping(value = "/deleteas", method = RequestMethod.DELETE)
	public String deleteAS(@RequestParam(value="asname") String ASName) {
		String asConfigFilePath = this.getClass().getClassLoader()
				.getResource("asConfig.yaml").getFile();
		asClients = new ASClients(asConfigFilePath);

		return asClients.deleteAS(ASName);
	}

    @RequestMapping(value = "/launchelbas", method = RequestMethod.POST)
    public Map<String, Map<String, String>> launchELBAS(){
        Map<String, Map<String, String>> ret = new HashMap<>();
        log.info("Creating Application Load Balancer, please wait about 3 minutes......");

        Map<String, String> elb = createELB();
        ret.put("LoadBalancer", elb);

        log.info("Creating Auto Scaling ......");

        Map<String, String> as = createAS();
        ret.put("AutoScaling", as);

        return ret;
    }

	/*
     * Get ASGroup metrics, metricsName can be found: http://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/as-metricscollected.html
     */
	@RequestMapping(value = "/getasmetric", method = RequestMethod.GET)
	public Map<String, String> getAsMetrics(@RequestParam(value="metric") String metricname) {
		return asClients.getASMetricStats(metricname);
	}

    @RequestMapping(value = "/getasstatsall", method = RequestMethod.GET)
    public Map<String,List<Map<String, String>>> getASStatsAll() {
        List<String> metricList = new ArrayList<>();
        metricList.add("GroupMinSize");
        metricList.add("GroupMaxSize");
        metricList.add("GroupDesiredCapacity");
        metricList.add("GroupInServiceInstances");
        metricList.add("GroupPendingInstances");
        metricList.add("GroupStandbyInstances");
        metricList.add("GroupTerminatingInstances");
        metricList.add("GroupTotalInstances");

        Map<String,List<Map<String, String>>> ret = new HashMap<>();
        String json="";

        if (asClients != null ) {
            List<Map<String,String>> list = new ArrayList<>();
            for (String metric: metricList) {
                Map<String, String> stats = asClients.getASMetricStats(metric);
                if(!stats.isEmpty()) list.add(stats);
            }
            ret.put(asClients.asConfig.getAsgroupName(), list);
        }

        try {
            json = new ObjectMapper().writeValueAsString(ret);
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        return ret;
    }

    @RequestMapping(value = "/getec2statsall", method = RequestMethod.GET)
    public Map<String,List<Map<String, String>>> getEC2StatsAll() {
        Map<String,List<Map<String, String>>> ret = new HashMap<>();
        String json="";

        List<String> metricList = new ArrayList<>();
        metricList.add("CPUUtilization");
//        metricList.add("DiskReadOps");
//        metricList.add("DiskWriteOps");
//        metricList.add("DiskReadBytes");
//        metricList.add("DiskWriteBytes");
        metricList.add("NetworkIn");
        metricList.add("NetworkOut");
        metricList.add("NetworkPacketsIn");
        metricList.add("NetworkPacketsOut");

        if (asClients != null ) {
            List<String> ec2List = ec2Clients.listInstances(RUNNING_STATUS);
            List<Map<String,String>> list = new ArrayList<>();

            for (String ec2 : ec2List) {
                for (String metric : metricList) {
                    Map<String, String> stats = ec2Clients.getEC2MetricStats(ec2, metric);
                    if (!stats.isEmpty()) list.add(stats);
                }
                ret.put(ec2, list);
            }
        }

        try {
            json = new ObjectMapper().writeValueAsString(ret);
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        return ret;
    }

    @RequestMapping(value = "/monitor", method = RequestMethod.GET)
    public String monitorELBEC2AS() {
	    List<Map<String,List<Map<String, String>>>> list = new ArrayList<>();
	    list.add(getELBStatsAll());
	    list.add(getEC2StatsAll());
	    list.add(getASStatsAll());

        String json = "";
        try {
            json = new ObjectMapper().writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error(e);
        }

        return json;
    }
}
