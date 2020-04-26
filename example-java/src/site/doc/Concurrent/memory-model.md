## 内存模型
并发编程中，需要处理两个关键问题：线程通信和线程同步。线程通信是指线程之间交换信息的机制，线程同步是指程序中用于控制不同线程间操作发生相对顺序的机制。

- 在共享内存模型中线程间共享程序的公共状态，通过读-写内存中的公共状态进行隐式通信；共享内存模型中同步是显式的，必须显式指定代码需要在线程间互斥
- 在消息传递模型中线程之间没有公共状态，必须显式发送消息进行通信；消息传递模型里，消息的发送必然在消息的接收之前所以同步是隐式的

Java 的并发采用的是共享内存模型，线程之间的通信都是隐式的，因此 Java 并发编程需要显式设定同步。
### JMM 
Java 内存模型（Java Memory Model, JMM）的主要目标是定义程序中各个变量的访问规则，即虚拟机中将变量存储到内存和从内存中取出变量这样的底层细节。

**JMM 规定了所有的变量都存储在主内存，每条线程有自己的工作内存，线程的工作内存中保存了被该线程使用到的变量在主内存的副本拷贝；线程对变量的所有操作(读取、赋值等)都必须在工作内存中进行而不能直接读写主内存中的变量，不同线程之间也无法直接访问对方工作内存中的变量，线程间变量值的传递需要通过主内存来完成。**

线程私有的本地内存是一个抽象概念，实际并不存在，它涵盖了缓存、写缓冲区、寄存器以及其他的硬件和编译器优化。根据 JMM 模型，线程 A 和线程 B 通信需要经过两个步骤：
- 线程 A 把本地线程中更新的共享变量刷新到主内存中
- 线程 B 从主内存中读取已经更新的共享变量到本地内存中

### 指令重排
在执行程序时为了提高性能，编译器和处理常常会对指令做重排序：
- **编译器优化的重排序**：编译器在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序
- **指令级并行的重排序**：现代处理器采用了指令级并行技术来将多条指令重叠执行，如果不存在数据依赖性，处理器可以改变语句对应机器指令的执行顺序
- **内存系统的重排序**：由于处理器使用缓存和读/写缓冲区，这使得 load 和 store 操作看上去可能是在乱序执行

编译器优化重排序属于编译器重排序，指令级并行重排序和内存系统重排序属于处理器重排序，这些重排序会导致多线程出现内存可见性问题。JMM 会禁止特定类型的编译器重排序并在生成指令序列时插入特定类型的内存屏障指令来禁止特定类型的处理器重排序，从而提供一致的内存可见性保证。

|内存屏障指令|说明|
|-|-|
|LoadLoad|确保内存屏障指令之前的 Load 指令先发生与之后的 Load 指令|
|StoreStore|确保内存屏障指令之前的 Store 指令先发生于之后的 Store 指令|
|LoadStore|确保内存屏障指令之前的 Load 指令先发生于之后的 Store 指令|
|StoreLoad|确保内存屏障指令之前的 Store 指令先发生于之后的 Load 指令|

### 先行发生原则
先行发生(hapends-before)是 Java 内存模型中定义的两项操作之间的偏序关系，**如果说操作 A 先行发生于操作 B，其实就是说在发生操作 B 之前，操作 A 产生的影响能被操作 B 观察到**。

Java 内存模型中存在“天然的”先行发生关系：
- 在一个线程内，按照代码控制流顺序前面的操作先行发生于后面的操作
- 对一个锁的 unlock 操作先行发生于后面对同一个锁的 lock 操作
- 对一个 volatile 变量的写操作先行发生于后面对这个变量的读操作
- Thread 对象的 start() 方法先行发生于此线程的每一个动作
- 线程的所有操作都先行发生于对此线程的终止检测
- 对线程的 interrupt() 方法的调用先行发生于被中断线程的代码检测到中断事件的发生
- 一个对象的初始化完成先行发生于它的 finalize() 方法
- 如果 A 操作先行发生于 B 操作，B 操作先行发生于 C 操作，则 A 操作先行发生于 C 操作

每个先行发生规则对应一个或多个编译器和处理器的重排序规则，封装了 JMM 提供的内存可见性规则和实现，从而可以在一些情况下可以不用考虑多线程情况下的可见性问题。

### volatile 内存语义
volatile 关键字是 Java 虚拟机提供的最轻量级的同步机制，Java 内存模型对 volatile 专门定义了一些特殊的访问规则使其具备两种特性：
- **可见性**：一个线程修改了 valatile 修饰的变量的值对其他线程来说是可见的，volatile 的可见性保证变量修改后立即同步到主内存而每次读取变量需要立即从主内存读取
- **禁止指令重排序优化**：volatile 关键字修饰的变量在写操作时会在本地代码中插入内存屏障指令来保证处理器不会发生重排序

为了实现 volatile 内存语义，JMM 在指令序列中插入内存屏障：
- 在每个 volatile 写操作前插入一个 StoreStore 屏障，保证在 volatile 写之前所有的普通写操作可见，即所有的普通写都已经刷新到主内存
- 在每个 volatile 写操作后插入一个 StoreLoad 屏障，避免 volatile 写与后面的 volatile 读操作重排序
- 在每个 volatile 读操作后插入一个 LoadLoad 屏障，用来禁止 volatile 读和之后的普通读重排序
- 在每个 volatile 读操作后插入一个 LoadStore 屏障，用来禁止volatile 读和之后的普通写重排序

### final 内存语义
对于 final 关键字，编译器和处理器要遵守两个重排序规则：
- 在构造函数内对一个 final 变量的写入与随后把这个被构造的对象的引用赋值给一个变量这两个操作不能重排序
- 初次读一个包含 final 变量的对象的引用与随后初次读 final 变量这两个操作不能重排序

JMM 在构造函数写 final 变量后插入 StoreStore 内存屏障禁止处理器把 final 变量的写操作重排序到构造函数之外，对于普通变量则没有这个保证；在 final 变量读操作之前插入 LoadLoad 内存屏障禁止将读 final 变量操作重排序到读对象引用操作前。

### 原子性、可见性、有序性
- **原子性**：基本数据类型的读写是具备原子性的，如果需要更大范围的原子性保证，需要使用 Java 同步块 synchronized 关键字(使用 monitorenter 和 monitorexit 指令)
- **可见性**：可见性是指当一个线程修改了共享变量的值，其他线程能够立即得知这个修改。Java 内存模型是通过变量修改后同步到主内存，变量读取前从主内存读取新值来实现可见性。volatile（变量发生修改同步到主内存）、synchroized（退出同步代码块之前将变量值同步回主内存）、final（final 修饰的变量在构造器中一旦初始化完成就不能修改） 均可实现可见性
- **有序性**：Java 的有序指的是线程内是有序的，线程间是无序的；volatile（禁止指令重排） 和 synchronized（一个变量在同一时刻只允许一条线程对其加锁）可以保证有序性
### long 和 double 类型
虚拟机允许将没有 volatile 修饰的 64 位数据的读写操作划分为两次 32 位的操作进行，这就会导致多线程环境下并发的修改和读取可能会读取到“半个变量”的数值。

### 双重检查锁定
双重检查锁定是一种延迟实例化的技术，通过双重检查锁定来降低同步的开销，但是双重检查锁定很容易会错误使用。

双重检查锁定的错误用法：
```java
public class DoubleCheckLocking{

    private static Instance instance;
    public static Instance getInstance(){
        // 不为 null 时 instance 可能并没有完成初始化
        if(instance == null){
            synchronized(DoubleCheckLocking.class){
                if(instance == null){
                    instance = new Instance();
                }
            }
        }
        return instance;
    }
}
```
首先检查 instance 实例是否为 null，如果是则加锁再次判断创建实例，如果不是则直接返回；双重加锁降低了锁的粒度而降低 synchronized 的性能开销，但是存在一个严重的问题：**判断对象不为 null 时对象可能还未完全初始化！**

Java 中创建对象实例可以理解为 3 步：分配内存空间、初始化对象、将变量引用指向对象内存地址。由于重排序这 3 步的执行顺序可能发生乱序，因此会导致变量引用指向了对象的内存地址但是该内存还没有初始化，此时访问该内存不会返回 null。

为了避免由于指令重排导致双重检查锁定延迟初始化对象发生错误，有两种方案：volatile 关键字 和 静态内部类。

volatile 关键字可以禁止指令重排，因此可以避免发生错误
```java
public class DoubleCheckLocking{

  private static volatile Instance instance;
  public static Instance getInstance(){
    if(instance == null){
        synchronized(DoubleCheckLocking.class){
            if(instance == null){
                instance = new Instance();
            }
        }
    }
    return instance;
  }
}
```

静态类在加载的时候已经完成初始化并且 JVM 保证多线程初始化时只会有一个线程能完成初始化
```java
public class InstanceFactory{
  private static class InstanceHolder{
    public static Instance instance = new Instance();
  }

  public static Instance getInstance(){
	return InstanceHolder.instance;
  }
}
```

**[Back](../)**