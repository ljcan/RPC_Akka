# RPC_Akka
**基于akka开发的一个小型RPC基于心跳机制的通信架构**

首先启动Master,传入参数为`masterHost masterPort`。

接着启动worker，传入参数为`host port masterHost masterPort core memory`

可以打成jar包运行或者在IDE中传入参数执行。
