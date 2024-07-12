### sip 相关组件执行顺序
1、 优先加载 SipLayer，order= 10，根据ip和端口初始化sip配置
2、系统启动时控制上级平台重新注册，SipPlatformRunner，order=13
3、系统启动时控制设备，SipRunner，order=14

4、MediaServerConfig，order=12
