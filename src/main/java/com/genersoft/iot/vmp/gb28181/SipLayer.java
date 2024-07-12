package com.genersoft.iot.vmp.gb28181;

import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.GbStringMsgParserFactory;
import com.genersoft.iot.vmp.gb28181.conf.DefaultProperties;
import com.genersoft.iot.vmp.gb28181.transmit.ISIPProcessorObserver;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  run方法是一个Spring Boot的CommandLineRunner接口的实现，该方法在Spring Boot应用启动后自动执行
 *
 *  它负责 SIP（会话初始化协议）层的初始化和配置
 */
@Component
@Order(value=10)
public class SipLayer implements CommandLineRunner {

	private final static Logger logger = LoggerFactory.getLogger(SipLayer.class);

	@Autowired
	private SipConfig sipConfig;

	@Autowired
	private ISIPProcessorObserver sipProcessorObserver;

	@Autowired
	private UserSetting userSetting;
    /**
     分别存储 TCP 和 UDP 的 SipProviderImpl 实例。
     */
	private final Map<String, SipProviderImpl> tcpSipProviderMap = new ConcurrentHashMap<>();
	private final Map<String, SipProviderImpl> udpSipProviderMap = new ConcurrentHashMap<>();
	/**
	 * 存储监控的 IP 地址。
	 */
	private final List<String> monitorIps = new ArrayList<>();

	/**
	 * 这个方法在应用启动时执行，用于配置 SIP 监听的 IP 地址和端口。
	 * @param args
	 */
	@Override
	public void run(String... args) {
		/*
		 如果 sipConfig.getIp() 为空，则自动获取本机的 IPv4 地址，忽略本地环回地址和 Docker 地址。
		 */
		if (ObjectUtils.isEmpty(sipConfig.getIp())) {
			try {
				// 获得本机的所有网络接口
				Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
				while (nifs.hasMoreElements()) {
					NetworkInterface nif = nifs.nextElement();
					// 获得与该网络接口绑定的 IP 地址，一般只有一个
					Enumeration<InetAddress> addresses = nif.getInetAddresses();
					while (addresses.hasMoreElements()) {
						InetAddress addr = addresses.nextElement();
						if (addr instanceof Inet4Address) {
							if (addr.getHostAddress().equals("127.0.0.1")){
								continue;
							}
							if (nif.getName().startsWith("docker")) {
								continue;
							}
							logger.info("[自动配置SIP监听网卡] 网卡接口地址： {}", addr.getHostAddress());// 只关心 IPv4 地址
							monitorIps.add(addr.getHostAddress());
						}
					}
				}
			}catch (Exception e) {
				logger.error("[读取网卡信息失败]", e);
			}
			if (monitorIps.isEmpty()) {
				logger.error("[自动配置SIP监听网卡信息失败]， 请手动配置SIP.IP后重新启动");
				System.exit(1);
			}
		}else {
			// 使用逗号分割多个ip
			String separator = ",";
			if (sipConfig.getIp().indexOf(separator) > 0) {
				String[] split = sipConfig.getIp().split(separator);
				monitorIps.addAll(Arrays.asList(split));
			}else {
				monitorIps.add(sipConfig.getIp());
			}
		}

		sipConfig.setShowIp(String.join(",", monitorIps));
		// 设置 SipFactory 的路径。
		SipFactory.getInstance().setPathName("gov.nist");
		if (monitorIps.size() > 0) {
			for (String monitorIp : monitorIps) {
				//为每个 monitorIp 添加监听点，如果没有成功添加任何监听点，则退出程序。
				addListeningPoint(monitorIp, sipConfig.getPort());
			}
			if (udpSipProviderMap.size() + tcpSipProviderMap.size() == 0) {
				System.exit(1);
			}
		}
	}

	/**
	 * addListeningPoint 方法是配置和启动 SIP 服务的关键部分。
	 * 这个方法负责为指定的 IP 地址和端口创建 SIP 监听点，并将其添加到相应的提供者 (SipProviderImpl) 中。
	 * @param monitorIp
	 * @param port
	 */
	private void addListeningPoint(String monitorIp, int port){
		SipStackImpl sipStack;
		try {
//			创建一个 SipStack 实例，这是 SIP 协议栈的核心。
			sipStack = (SipStackImpl)SipFactory.getInstance()
					// 加载默认配置。
					.createSipStack(DefaultProperties.getProperties("GB28181_SIP", userSetting.getSipLog()));
//			设置消息解析工厂。
			sipStack.setMessageParserFactory(new GbStringMsgParserFactory());
		} catch (PeerUnavailableException e) {
			logger.error("[SIP SERVER] SIP服务启动失败， 监听地址{}失败,请检查ip是否正确", monitorIp);
			return;
		}
       // . 创建 TCP 监听点并启动
		try {
			// 创建一个 TCP 类型的 ListeningPoint 监听点
			ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "TCP");
			// 为 ListeningPoint 创建 SipProvider 实例。
			SipProviderImpl tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);
			// 设置自动处理对话错误。
			tcpSipProvider.setDialogErrorsAutomaticallyHandled();
			// 为 SipProvider 添加 SIP 监听器。
			tcpSipProvider.addSipListener(sipProcessorObserver);
			// 将 SipProvider 添加到 tcpSipProviderMap 中。
			tcpSipProviderMap.put(monitorIp, tcpSipProvider);
			logger.info("[SIP SERVER] tcp://{}:{} 启动成功", monitorIp, port);
		} catch (TransportNotSupportedException
				 | TooManyListenersException
				 | ObjectInUseException
				 | InvalidArgumentException e) {
			logger.error("[SIP SERVER] tcp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
					, monitorIp, port);
		}
		/**
		 * 创建 UDP 监听点并启动
		 */
		try {
			//创建一个 UDP 类型的 udpListeningPoint 监听点
			ListeningPoint udpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "UDP");
			// 为 udpListeningPoint 创建 SipProvider 实例。
			SipProviderImpl udpSipProvider = (SipProviderImpl)sipStack.createSipProvider(udpListeningPoint);
			// 为 SipProvider 添加 SIP 监听器。
			udpSipProvider.addSipListener(sipProcessorObserver);
			// 将 SipProvider 添加到 udpSipProviderMap
			udpSipProviderMap.put(monitorIp, udpSipProvider);

			logger.info("[SIP SERVER] udp://{}:{} 启动成功", monitorIp, port);
		} catch (TransportNotSupportedException
				 | TooManyListenersException
				 | ObjectInUseException
				 | InvalidArgumentException e) {
			logger.error("[SIP SERVER] udp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
					, monitorIp, port);
		}
	}

	public SipProviderImpl getUdpSipProvider(String ip) {
		if (udpSipProviderMap.size() == 1) {
			return udpSipProviderMap.values().stream().findFirst().get();
		}
		if (ObjectUtils.isEmpty(ip)) {
			return null;
		}
		return udpSipProviderMap.get(ip);
	}

	public SipProviderImpl getUdpSipProvider() {
		if (udpSipProviderMap.size() != 1) {
			return null;
		}
		return udpSipProviderMap.values().stream().findFirst().get();
	}

	public SipProviderImpl getTcpSipProvider() {
		if (tcpSipProviderMap.size() != 1) {
			return null;
		}
		return tcpSipProviderMap.values().stream().findFirst().get();
	}

	public SipProviderImpl getTcpSipProvider(String ip) {
		if (tcpSipProviderMap.size() == 1) {
			return tcpSipProviderMap.values().stream().findFirst().get();
		}
		if (ObjectUtils.isEmpty(ip)) {
			return null;
		}
		return tcpSipProviderMap.get(ip);
	}

	/**
	 * 获取本地 IP 的方法
	 * @param deviceLocalIp
	 * @return
	 */
	public String getLocalIp(String deviceLocalIp) {
		if (monitorIps.size() == 1) {
			return monitorIps.get(0);
		}
		if (!ObjectUtils.isEmpty(deviceLocalIp)) {
			return deviceLocalIp;
		}
		return getUdpSipProvider().getListeningPoint().getIPAddress();
	}
}
