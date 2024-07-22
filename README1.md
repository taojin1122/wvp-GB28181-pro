### sip 相关组件执行顺序
1、 优先加载 SipLayer，order= 10，根据ip和端口初始化sip配置
- SIPProcessorObserver sip的监听器，在初始化时配置
- 监听设备返回的消息，分别交给ISIPRequestProcessor 和ISIPResponseProcessor 处理
2、系统启动时控制上级平台重新注册，SipPlatformRunner，order=13
3、系统启动时控制设备，SipRunner，order=14
4、发送 SIP消息类 SIPSender
### 请求流程：
#### 1、设备主动发送注册请求 (REGISTER)
- 在sip服务器启动后，摄像机设备会主动向sip服务器发送注册REGISTER请求。

#### 2、设备发送心跳请求 (KEEPALIVE)
- 设备定期发送SIP OPTIONS请求作为心跳信号，以保持与服务器的连接并告知服务器设备在线。

#### 3、消息传递 (MESSAGE)
- 设备和服务器可以通过SIP MESSAGE方法发送文本消息或控制命令。

#### 4、服务器或设备发起呼叫 (INVITE)
- 当需要视频监控时，服务器或设备发起SIP INVITE请求，邀请对方进行会话。
在发送invite后开始推流
- sendPushStream
### 流媒体配置加载
4、MediaServerConfig，order=12

- 执行 mediaServerService.update() 初始化 ssrc加载到redis中，每次开启 预览都要从redis取出一个ssrc，并删除。

5、类ZLMMediaServerStatusManger
- 添加定时任务每10秒执行一次，注册待上线节点，重新连接
### 自定义线程配置类 
- ThreadPoolTaskConfig

### 概念
1、SSRC是一个32位的数值标识符，用于标识RTP包流的源，使其不依赖于网络地址。
2、摄像机推送的流在 zlm 服务器的rtp目录下，是流服务器自己构造的目录，rtp协议只关注ip、端口、数据包
