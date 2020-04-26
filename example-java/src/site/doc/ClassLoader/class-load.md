## JVM 类加载机制
虚拟机把描述类的数据从 class 文件加载到内存，并对数据进行校验、转换解析和初始化，最终形成可以被虚拟机直接使用的 Java 类型，这就是虚拟机的类加载机制。

在 Java 语言里面，类型的加载、连接和初始化过程都是在程序运行期间完成的，这种策略虽然会令类加载时稍微增加一些性能开销，但是会为 Java 应用程序提供高度的灵活性，Java 里天生可以动态扩展的语言特性就是依赖运行期动态加载和动态连接这个特点实现的。

类从被加载到虚拟机内存开始到卸载出内存为止的整个生命周期包括：加载(Loading)、验证(Verification)、准备(Preparation)、解析(Resolution)、初始化(Initialization)、使用(Using)和卸载(Unloading)，其中验证、准备、解析统称为连接(Linking)。

### 类加载时机
Java 虚拟机规范规定了类必须进行初始化的情况：
- 遇到 new、getstatic、putstatic、invokestatic 这4条字节码指令时，如果类没有进行过初始化，则需要先触发其初始化。生成这4条指令的最常见 Java 代码场景是：使用 new 关键字实例化对象、读取或设置一个静态字段(被 final 修饰、已在编译期把结果放入常量池的静态字段除外)、调用一个类的静态方法
- 使用 java.lang.reflect 包的方法对类进行反射调用的时候，如果没有进行过初始化，则需要先触发其初始化
- 当初始化一个类的时候，如果发现其父类还没有进行过初始化，则需要先触发其父类的初始化
- 当虚拟机启动时，用户需要指定一个要执行的主类(包含 main() 方法的那个类)，虚拟机会先初始化这个主类
- 当使用 JDK1.7 的动态语言支持时，如果一个 java.lang.invoke.MethodHandle 实例最后的解析结果 REF_getStatic、REF_putStatic、REF_invokeStatic 的方法句柄，并且这个方法句柄所对应的类没有进行初始化，则需要先触发其初始化

接口与类初始化的区别：当一个类在初始化时，要求其父类全部都已经初始化，但一个接口在初始化时，并不要求其父接口全部都完成初始化，只有在真正使用到父接口时(如引用接口中的常量)才会初始化
### 类加载的过程
#### 加载(Loading)
在加载阶段，虚拟机完成三件事：
  - 通过类的全限定名获取类的二进制字节流
  - 将字节流代表的静态存储结构转换为方法区的运行时数据结构
  - 在内存中生成类对应的 java.lang.Class 对象，作为这个类的各种数据的访问入口
  
加载阶段完成之后，虚拟机外部的二进制字节流就按照虚拟机所需要的格式存储在方法区中，然后在内存中实例化一个 java.lang.Class 对象作为程序访问方法区中的这些类型数据的外部接口
#### 验证(Verification)
验证的目的是确保 class 文件的字节码流中包含的信息符合当前虚拟机的要求，并且不会危害虚拟机自身的安全。如果验证过程中发现字节码流不符合 class 文件格式的约束会抛出 java.lang.VerifyError 异常或其子类异常。

验证阶段大概完成4个检验动作：
  - **文件格式验证**：验证字节流是否符合 class 文件格式的规范，并且能被当前版本的虚拟机处理，包括：是否以 0xCAFFEEBABE 开头；主、次版本是否在当前虚拟机处理范围之内等。该验证阶段主要目的是保证输入的字节流能够正确地解析并存储于方法区之内，这个阶段的验证是基于二进制字节流进行的，只有通过了这个阶段的验证后，字节流才会进入内存的方法区中进行存储
  - **元数据验证**：对字节码描述信息进行语义分析，以保证其描述的信息符合 Java 语言规范的要求，包括：这个类是否有父类、这个类是否继承了 final 类、是否实现抽象父类的方法等。
  - **字节码验证**：通过数据流和控制流分析，确定程序语义是合法的、符合逻辑的；这个阶段对类的方法体进行校验分析，保证被校验类的方法在运行时不会做出危害虚拟机安全的事件，如：对象赋值不合法
  - **符号引用验证**：这个验证阶段发生在虚拟机将符号引用转化为直接引用的时候，通常包含：符号引用字符串描述的全限定名能否找到对应的类、指定类是否存在符合方法描述的方法和字段、符号引用的访问性是否可被当前类访问。符号引用验证的目的是确保解析动作能够正常执行，如果验证错误抛出 java.lang.IllegalAccessError、java.lang.NoSuchFieldError、java.lang.NoSuchMethodError 等。如果不需要验证使用 -Xverify:none 参数关闭大部分验证措施以缩短虚拟机类加载的时间。
#### 准备(Preparation)
准备阶段是正式为类变量分配内存并设置变量初始值的阶段，这些变量所使用的内存都将在方法区中进行分配。此时进行内存分配的变量是类变量(static 修改的变量)，初始值指的是变量的零值，final 修饰的变量会在编译时指定值。
#### 解析(Resolution)
解析阶段是虚拟机将常量池内的符号引用替换为直接引用的过程。
  - 符号引用：以一组符号来描述所引用的目标，符号可以是任何形式的字面量，只要使用时能无歧义的定位到目标即可。符号引用与虚拟机的实现及内存布局无关。
  - 直接引用：直接引用可以是直接指向目标的指针、相对偏移量或是一个能间接定位到目标的句柄。直接引用是和虚拟机的实现和内存布局相关。
#### 初始化(Initialization)
初始化阶段才真正开始执行类中定义的 Java 程序代码，也就是执行类构造器```<clinit>``` 方法的过程。
  - ```<clinit>``` 方法是由编译器自动收集类中的所有类变量的赋值动作和静态语句块中的语句合并产生的，编译器收集的顺序是由语句在源文件中出现的顺序所决定的。静态语句块中只能访问到定义在静态语句块之前的变量，定义在静态语句块之后的变量可以在静态语句块内赋值(在准备阶段已经分配内存空间)，但是不能访问。
    ```java
    public class Test{
      static{
        i = 0;
        // Illegal Forward Reference
        System.out.println(i);
      }
      static int i = 1;
    }
    ```
  - ```<clinit>``` 方法与类的构造函数不同，它不需要显示地调用父类构造器，虚拟机保证在子类的 ```<clinit>```方法执行之前，父类的 ```<clinit>```方法已经执行完毕
  - 由于父类的 ```<clinit>```方法优先执行，也就意味着父类中定义的静态语句块和静态变量赋值优先于子类的静态语句块和静态变量赋值操作
    ```java
    public class Parent{
      static int i = 0;
      static{
        System.out.println("static in parent");
      }
      public Parent(){
        System.out.println(i);
      }
    }

    public class Sub extends Parent{
      static int i = 1;
      static{
        System.out.println("static in sub");
      }
      public Sub(){
        System.out.println(i);
      }
    }

    new Sub();

    // static in parent
    // static in sub
    // 0
    // 1
    ```
  - ```<clinit>```方法对于类或者接口来说并不是必需的，如果一个类中既没有静态代码块也没有静态变量赋值操作，那么编译器可以不为这个类生成 ```<clinit>```方法
  - 接口中不能使用静态语句块，但仍然有变量初始化的赋值操作，因此接口与类一样都会生成```<clinit>```方法。接口与类不同的是，执行接口的```<clinit>```方法不需要先执行父接口的```<clinit>```方法，另外接口的实现类在初始化时也不会执行接口的```<clinit>```方法
  - 虚拟机保证一个类的```<clinit>```方法在多线程的环境下被正确的加锁、同步，如果多个线程同时去初始化一个类，那么只有一个线程去执行这个类的```<clinit>```方法，其他线程需要阻塞等待直到```<clinit>```方法执行完毕。

**[Back](../)**