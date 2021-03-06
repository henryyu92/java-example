##  TCP 协议

`TCP` 协议 (Transmission Control Protocol) 提供面向连接的、可靠的(没有数据重复和丢失)、端到端的、全双工的字节流传输服务。

- *面向连接*：应用进程间发送数据前需要建立连接，数据传输完成后可以由任意一方断开连接
- *可靠传输*：`TCP` 协议利用校验和、数据报确认以及超时重传机制保证数据不丢失，利用序列号以及累计确认机制保证数据的有序且不重复
- *端到端*：`TCP` 协议提供与远端计算机应用进程的连接，应用进程可以通过 TCP 创建连接、发送数据和关闭连接。所有的 TCP 连接都是点到点的通信，也就是 TCP 协议不支持多址通信(Multicast) 和广播通信 (Broadcast)
- *字节流传输*：`TCP` 协议提供流接口(Stream Interface)，应用进程通过接口发送连续的字节序列。TCP 不会在字节流中插入记录标识符，接收方只能每次接收窗口内的数据(半包问题)；TCP 对字节流的内容不做任何解释，对字节流的解释由应用层完成
- *全双工通信*：TCP 协议支持数据同时双向传输，也就是正在通信的客户端和服务器可以同时向对方发送数据。TCP 连接两端都设有发送缓冲和接收缓冲，在发送时应用程序的数据发送到缓冲后，TCP 在合适的时候将数据发送出去，在接收时数据存储在缓冲中，应用程序在合适的时候读取缓冲区的数据

### 报文格式

TCP 协议在两个进程之间传输的数据的传输单元称为报文段(Segment)，TCP 在收到应用层传递的数据后需要将其包装成报文段。

TCP 报文段由 20 字节的报文头和多个字节的的数据组成，数据长度最大不超过 `65535(IP 数据包长度)-20(IP 头)-20(TCP 报头)` 字节，数据的长度受限与 IP 协议的数据长度以及底层链路层的最大传输长度。

TCP 报头域占用 20 字节，TCP 协议的全部功能都体现在其各个字段中：

- 源端口和目的端口：16 bit 的源端口用于标识本机连接点，16 bit 的目的端口用于识别远程主机的连接点
- 序列号(Sequence Number)和确认号(Acknowledgment Number)：32 bit 的序列号是本报文段所携带的数据的第一个字节的序列号，用于指示当前数据块在整个消息中的位置；32 bit 的确认号用于接收端对发送端发出的数据的确认，表示最后接收到的数据块的序列号
- 报头长度：4 bit 的报头长度用于说明 TCP 报头的长度，

### 连接管理

TCP 是面向连接的协议，传输连接管理使连接的建立和释放都能正常进行。

#### 传输连接建立(三次握手)

TCP 协议采用客户端-服务器模式，在传输连接建立的过程中，主动发起建立连接的进程为客户端，等待接受连接建立请求的进程为服务器。

为提供可靠的连接服务，TCP 协议采用三次握手建立一个连接，三次握手需要客户端和服务端总共发送 3 个数据包。通过三次握手客户端和服务端同步了双方的序列号和确认号，并且交换了 TCP 窗口大小信息。

![三次握手](../asset/connect.png)

- 第一次握手(SYN=1, seq=x)：客户端发送同步报文段 (SYN = 1, ACK=0)，并且生成随机的序列号 seq=x 表示发送的数据从此序列号开始。TCP 规定同步报文段不能携带数据，但要消耗一个序号，发送完毕后客户端进入 `SYN_SEND` 状态。
- 第二次握手(SYN=1, ACK=1, seq=y, ack=x+1)：服务器返回 ACK+SYN 包确认客户端请求并同步服务端序列号，即 SYN 标志位和 ACK 标志位都为 1，seq 为服务器自己的序列号 y，同时将确认序列号 ack 设置为 x+1，发送完毕后服务器进入 `SYN_RCVD` 状态
- 第三次握手(ACK=1, ack=y+1, seq=x+1)：客户端再次发送确认包，SYN 标志位设置为 0，ACK 标志位设置为 1，ack=y+1，发送完毕后客户端进入 `ESTABLISHED` 状态，当服务器收到这个包时也进入 ESTABLISHED 状态，TCP 握手结束

客户端需要再次向服务端发送确认是为了防止已失效的连接请求报文段突然又传送到了 Server，因而产生错误。已失效的连接是由于连接请求报文并没有丢失而是在网络节点长时间滞留导致连接已经释放之后才到达，此时服务端以为是新的连接，建立连接发送确认给 Client 会被直接丢弃，如果不采用三次握手的方式，服务端会认为连接已经建立，从而会白白等待数据发送浪费资源。

#### 传输连接释放(四次挥手)

数据传输结束后需要释放传输连接，TCP 协议是双工的，可以在两个不同的方向进行独立的数据传输，因此通信的双方都可以释放连接。

TCP 释放连接需要通信双方发送四个数据包，也成为四次挥手。四次挥手之后通信的双方都释放了连接，通信双方都不能发送数据，也不能接收数据。

![四次挥手](../../asset/protocol/disconnect.png)

- 第一次挥手(FIN=1, seq=u)：A 发送 FIN 报文段请求关闭连接，seq=u 表示序列号为 u 之前的数据已经传送。此时 A 进入 `FIN-WAIT-1` 状态，只接收数据而不发送数据并且等待 B 的确认。TCP 规定，FIN 报文段即使不携带数据也消耗一个序号
- 第二次挥手(ACK=1, ack=u+1, seq=v)：B  如果同意释放连接则发送确认包，确认号是 ack=u+1，seq=v 表示 v 之前的数据已经发送。发送确认包之后 B 进入 `CLOSE_WAIT` 状态，从 A 到 B 这个方向的连接就释放了，这时 TCP 连接处于半释放状态，即 A 已经没有数据要发送了，但 B 若发送数据 A 仍要接收，也就是 B 到 A 方向的连接并未关闭。A 收到来自 B 的确认后，就进入 `FIN-WAIT-2` 状态，等待 B 发出的连接释放报文段
- 第三次挥手(FIN=1, ACK=1, seq=w, ack=u+1)：若 B 已经没有要向 A 发送的数据，其应用进程就通知 TCP 释放连接，这时 B 发出的释放连接报文段必须使 FIN=1，B 报文的序列号是 w，B 还必须重复上次已经发送过的确认号 ack=u+1，这时 B 就进入 `LAST-ACK` 状态，等待 A 的确认
- 第四次挥手(ACK=1, ack=w+1, seq=u+1)：A 在收到 B 的连接释放报文段后，必须对此发出确认，在确认报文中 ACK =1，确认号 ack=w+1，而自己的需要是 seq=u+1(根据 TCP 标准，前面发送的 FIN 报文段要消耗一个序号)，然后进入 `TIME-WATI` 状态，B 收到 A 的确认后进入 `CLOSED` 状态。现在 TCP 连接还并未释放掉，必须经过时间等待计时器(TIME-WAIT timer)设置的时间 2MSL 后，A 才进入到 CLOSED 状态，时间 MSL 叫做最长报文段寿命(Maxing Segment Lifgtime)

经过四次挥手后，通信的两个端点之间的连接就完全释放了，B 在收到了 A 发出的确认就进入 CLOSED 状态，而 A 需要等待 2 个 MSL 时间才能进入 `CLOSED` 状态，因此B 结束 TCP 连接的时间比 A 早些。

在四次挥手时需要 A 等待 2 个 MSL 主要有两个理由：

- 保证 A 发送的最后一个 ACK 报文段能够到达 B，这个 ACK 报文有可能丢失，因而使处在 LAST-ACK 状态的 B 不能对已发送的 FIN+ACK 报文段的确认，B 会超时重传这个 FIN_ACK 报文段，而 A 就能在 2MSL 时间内收到这个重传的 FIN+ACK 报文段。如果 A 不等待就直接释放连接，那么就无法收到 B 重传的 FIN-ACK 报文段，因而也就不会再次发送一次确认报文段，这样 B 就无法按照正常步骤进入 CLOSED 状态
- 防止已丢失的连接请求报文段出现在本次连接中。A 发送完最后一个 ACK 报文段后再经过 2MSL 时间就可以使本连接持续的时间内所产生的所有报文段都从网络中消失，这样可以使下一个新的连接中不会出现着这种旧的连接请求报文段。MSL 是指报文段在网络中最大的存活时间，如果经过 2MSL 时间内都没有接收到 B 重传的 FIN 则表示 A 返回的 ACK 已经被成功接收，此时可以关闭 TCP 连接

#### TCP keepAlive

TCP 通信双方建立交互的连接，但是并不是一直存在数据交互，有些连接在数据交互完毕后会主动释放连接；在出现机器掉电、死机等意外会使得大量没有释放的 TCP 连接浪费系统资源。

TCP 设有一个保活计时器(keepalive timer)，服务器每收到一次客户的数据，就重新设置保活计时器，时间的设置通常是 2 小时，若计时器时间内没有收到客户端的数据，服务器就发送一个探测报文，以后则每隔 75 分钟发送一次，若连续 10 个探测报文后仍无客户端响应，则服务器认为客户端出现故障，接着就关闭连接。

### 可靠传输

IP 协议只能提供尽最大努力传输数据，也就是不可靠的，因此 TCP 必须采用适当的措施保证通信变得可靠。TCP 协议通过停止等待和超时重传机制保证消息的可靠性。

#### 超时重传

超时重传指的是发送方发送数据后需要等待接收方的确认响应，如果超过一定时间后没有收到确认则会重传之前的数据。超时重传需要设置一个定时器，定时器的超时时间必须比数据传输往返时间更长，超时重传需要缓存已发送但未收到确认的数据副本以及其编号用于重传。

- 如果由于网络问题可能会导致接收方返回确认响应但是发送方并未接收，此时发送方在超时后会重传数据，接收方在接收到重复的数据后会直接丢弃数据并向发送方返回接收确认；
- 如果由于网络延时导致发送方在重传数据后才收到上次的确认响应，此时发送方会接收到两次确认响应，发送方会直接丢弃重复的确认响应

超时重传机制能够保证数据不丢失、不重复，但是超时重传机制不能保证数据按序到达，这个问题需要停止等待机制解决。

TCP 的发送方在规定的时间内没有收到确认就要重传已发送的报文段，超时时间设置的太短会引起不必要的报文段重传，而超时时间设置的太长会使得网络空闲时间太长降低传输效率。TCP 采用了一种自适应算法，记录每个报文段的发送到接收到确认的时间间隔 RTT，TCP 保留了 RTT 的一个加权平均往返时间 RTTs，其计算方式为 ```RTTs = (1-a)*RTTs + a*RTT```，a 一般取值 0.125。TCP 的超时重试时间(RTO, RetransmissionTimeOut) 应略大于 RTTs，其计算方式为 ```RTO = RTTs + RTTd```，其中 RTTd 是 RTT 的偏差加权平均值，计算方式为 ```RTTd = (1-b)*RTTd + b * |RTTs - RTT|```，b 一般取值 0.25。当发生数据报文重传时，报文段每重传一次就把超时重传时间 RTO 增加为之前的 2 倍，直到没有发生重传时才按照公式计算重传时间。

#### 停止等待

停止等待指的是发送方在传输完一个分组数据并接收到确认响应之后再发送下一个分组数据。这种传输方式信道利用率较低，可以将停止等待过程改为流水线传输形式，即发送方发送完一个分组数据后不必等待确认响应而是继续发送下一个分组数据，这样使得信道上一直有数据传输，提高信道利用率。

流水线发送数据使用窗口来保证数据有序，窗口内分组数据可以发送，发送方每接收到一个确认响应就会滑动对应的位置，对于乱序到达的数据并不会导致窗口滑动。接收方一般采用累计确认的方式，即在收到几个分组数据之后才返回确认响应，表示在这之前的数据都已经正确接收。

TCP 的滑动窗口是以字节为单位的，在没有收到接收确认的情况下，发送者可以将滑动窗口中的数据都发送出去。发送窗口里面的序号表示允许发送的序号，窗口越大，发送方就可以在接收方确认之前连续发送越多的数据，从而获得越高的传输效率。

发送窗口的后沿的后面部分表示已经发送且收到的确认，发送窗口的前沿前面的部分表示不允许发送的，因为接收方没有为这部分数据保留临时缓存空间。发送窗口的后沿只有前移和不动两种可能，而前沿也有可能前移和不动两种可能。

后沿前移是收到了接收者的确认，前沿的不动可能是收到了确认但接收者通知的窗口缩小了使得发送窗口的前沿正好不动。

```
  +----------------+-----------------+-----------------+---------------+
  |  已经发送确认  |  尚未确认的数据  |  可以发送的数据  | 不能发送的数据 |
  +----------------+-----------------+-----------------+---------------+
                P1               P2              P3
```

发送窗口需要三个指针描述状态，指针都指向字节的序号：

- 小于 P1 的是已经发送并已收到确认的部分
- 大于 P3 的是不允许发送的部分
- P3-P1 是发送窗口大小
- P2-P1 是已经发送但尚未收到确认的字节数
- P3-P2 是允许发送但尚未发送的字节数

接收窗口后沿后面的是已经发送过确认的数据，窗口内的序号是允许接收的，接收方只能对按序接收的数据中的最高序号给出确认。接收方在返回确认之前还会返回可以接收的窗口大小，发送方需要根据大小调整发送窗口，当发送窗口内的数据全部发送但未收到确认就需要等待直到收到确认或者超时重试。

发送方的应用进程把字节流写入 TCP 的发送缓存，接收方的应用进程从 TCP 的接收缓存中读取字节流。缓存空间是循环使用的，且缓存空间的大小一般比滑动窗口大。

发送缓存用来存放发送应用程序传送给发送方 TCP 准备发送的数据 和 TCP 已经发送出但尚未接收到确认的数据。发送窗口通常只是发送缓存的一部分，已经确认的数据应当从发送缓存中删除，因此发送缓存和发送窗口的后沿是重合的。发送应用程序最后写入发送缓存的字节减去最后被确认的字节即使还保留在发送缓存中的被写入的字节数，发送应用程序必须控制写入缓存的速率，否则发送缓存就会没有存放数据的空间。

接收缓存用来存放按序到达的、尚未被接收应用程序读取的数据 和 未按序到达的数据。如果接收应用程序来不及读取收到的数据，接收缓存就会被填满，使接收窗口减小到零。

### 流量控制

流量控制采用接收端控制发送端的数据流量机制，用于保证发送端发送数据的速率不要太快从而使得接收端的接收缓冲区溢出。

TCP 的流量控制时基于滑动窗口实现，发送方与接收方建立连接后接收方在确认时会返回接收窗口大小(单位为字节)，发送方的发送窗口不能超过接收方给出的接收窗口的数据。

当接收方处理速度较慢导致接收缓存满了就会返回零窗口确认响应，此时发送方的发送窗口需要调整为 0，当接收方可以处理数据是会给发送方发送非零窗口通知，为了防止发送方没有收到接收方返回的非零窗口通知，TCP 为每个连接设有一个持续计时器，只要 TCP 连接的一方收到对方的零窗口通知，就启动持续计时器，若持续计时器设置的时间到期，就发送一个零窗口探测报文段，而对方就在确认这个探测报文段时给出了现在的窗口值，如果窗口仍然是 0 那么收到这个报文段的一方就重新设置持续计时器，如果不是 0 那么就可以继续发送数据了。

TCP 可以用不同的机制来控制报文段发送的时机，TCP 实现中广泛使用 Nagle 算法：若发送应用进程把要发送的数据逐个字节地发送到 TCP 的发送缓存，则发送方就把第一个数据字节先发送出去，把后面到达的数据字节都缓存起来，当发送方接收到对第一个数据字符的确认后，再把发送缓存中的所有数据组装成一个报文段发送出去，同时继续对随后到达的数据进行缓存，只有在收到对前一个报文段的确认后才继续发送下一个报文段，当数据到达较快而网络速率较慢时，用这样的方法可以明显地减少所用的网络带宽。Nagle 算法规定当到达的数据已达到发送窗口大小的一半或以达到报文段的最大长度时，就立即发送一个报文段，这样可以有效地提高网络的吞吐量。

### 拥塞控制

在计算机网络中的带宽、交换节点中的患处和处理及等，都是网络的资源，当对网络中某一资源的需求超过了该资源所能提供的可用部分，网络性能就会变坏，这种情况叫做拥塞。

拥塞控制与流量控制相关，拥塞控制就是防止过多的数据注入到网络中，这样可以使网络中的路由器或链路不致过载。

拥塞控制是一个全局性的过程，涉及到降低网络传输性能有关的所有因素；流量控制往往指点对点通信量的控制，是端到端的问题，流量控制所要做的就是抑制发送端发送数据的速率，以便使接收端来得及接收。

拥塞控制有四种算法：慢开始、拥塞避免、快重传 和 快恢复。发送方维持一个拥塞窗口 cwnd 的状态变量，拥塞窗口的大小取决于网络的拥塞程度，并且动态的变化，发送方让自己的发送窗口大小等于或者小于拥塞窗口。

发送方控制拥塞窗口的原则是：只要网络没有出现拥塞就把拥塞窗口增大些以便把更多的分组发送出去，只要网络出现了拥塞，拥塞窗口就减小一些，以减少注入到网络中的分组数。只要发送方没有按时接收到接收方的确认报文，就认为网络出现了拥塞。

#### 慢开始

**慢开始算法**原理是当主机开始发送数据时，由小到大逐渐增大发送窗，即逐渐增大拥塞窗口，通常在刚开始发送报文时拥塞窗口 cwnd 设置为一个报文段 MSS(最大报文段长度) 的数值，在每收到一个对新的报文段的确认后，把拥塞窗口增加一个 MSS 数值。

为了防止拥塞窗口 cwnd 增长过大引起网络拥塞，需要设置慢开始门限 ssthresh 状态变量，当 cwnd < ssthresh 时，使用慢开始算法，当 cwnd > ssthresh 时停止使用慢开始算法而改用拥塞避免算法。

#### 拥塞避免

**拥塞避免算法**的思路是让拥塞窗口 cwnd 缓慢地增大，即每经过一个往返时间 RTT(如果拥塞窗口 cwnd 大小是 4 个报文段，那么这时的往返时间 RTT 就是发送方连续发送 4 个报文段并接收到这 4 个报文段的确认总共经历的时间) 就把发送方地拥塞窗口 cwnd 增加 1 个 MSS 数值，这样拥塞窗口 cwnd 按线性规律缓慢增长(加法增大)。

无论在慢开始阶段还是在拥塞避免阶段，只要发送方判断网络出现了拥塞(没有按时接收到确认)，就要把慢开始门限 ssthresh 设置为出现拥塞时发送方窗口值的一半(乘法减小)，然后把拥塞窗口 cwnd 重新设置为 1，执行慢开始算法，这样可以迅速减少主机发送到网络中地分组数，使得发生拥塞地路由器有足够地时间把积压在队列中地分组处理完毕。

#### 快重传

**快重传算法**首先要求接收方每收到一个失序地报文段后就立即发出重复确认，如接收方收到了 M1 和 M2 后都分别发出了确认，但是在接收 M4 之前没有接收到 M3，此时快重传算法规定接收方需要及时发送对 M2 的重复确认，这样可以让发送方及早直到报文段 M3 没有到达接收方。接着发送方发送 M5 和 M6，接收方接收后也都需要及时返回对 M2 的重复确认。快重传算法规定，发送方只要一连收到三个重复确认就应当立即重传对方尚未收到的报文段 M3，而不必等待为 M3 设置的重传计时器到期。由于发送方能尽早重传未被确认的报文段，因此采用快重传后可以使整个网络的吞吐量提升 20%。

#### 快恢复

**快恢复算法有**两个要点：

- 当发送方连续收到三个重复确认时，就把慢开始门限 sshtresh 减半，这是为了预防网络发生拥塞，但之后并不执行慢开始算法
- 发送方现在认为网络很可能没有发生网络阻塞(连续几个报文段都发送到接收方并返回重复确认)，因此不执行慢开始算法(拥塞窗口 cwnd 不设置为 1)，而是把 cwnd 值设置为慢开始门限 sshtresh 减半后的值，然后开始执行拥塞避免算法(加法增大)，使拥塞窗口缓慢的线性增大。


接收方的缓存空间时有限的，接收方根据自己的接收能力设定了接收窗口 rwnd 并把这个窗口值写入 TCP 首部中的窗口字段传送给发送方，从接收方对发送方的流量控制角度考虑，发送方的发送窗口一定不能超过接收方的接收窗口值。

发送窗口的上限应当为接收方窗口 rwnd 和 网络拥塞窗口 cwnd 这两个中较小的一个。