package cn.just.shinelon

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

import scala.collection.mutable

class Master(val masterName:String) extends Actor{

  var idToWorkerInfo = mutable.HashMap[String,WorkerInfo]()

  var workerInfoSet = mutable.HashSet[WorkerInfo]()

  //每个15秒检查一次心跳时间
  val CHECK_HEARTBEAT_INTERVAL = 15000

  override def preStart(): Unit = {
    println("preStart")
    import context.dispatcher
    //启动一个akka定时器定时检查worker的心跳报告
    context.system.scheduler.schedule(0 millis,CHECK_HEARTBEAT_INTERVAL millis,self,CheckHeartBeat)

  }

  override def receive = {
    case RegisterWorker(id,core,size)=>{
      println("注册worker....")
      if(!idToWorkerInfo.contains(id)){
        //将注册的Worker信息保存在本地内存，并且持久化保存，防止master宕机信息丢失
        val workerInfo = new WorkerInfo(id,core,size)
        idToWorkerInfo += (id->workerInfo)
        workerInfoSet += workerInfo
        println("注册成功")
        //注册成功，向worker中发送master的Id
        sender!ToRegisterWorker(masterName)
      }
    }

    case HeartBeat(id)=>{
      workerInfoSet.foreach(worker=>{
        if(worker.id.equals(id)){
          //设置worker上一次发送心跳报告的时间
          val time = System.currentTimeMillis()
          worker.lastTime=time
          println("收到worker的心跳报告")
        }
      })
    }

    case CheckHeartBeat=>{
      val currentTime = System.currentTimeMillis()
      //过滤出过期的worker
      val pastWorker = workerInfoSet.filter(worker=>currentTime-worker.lastTime>CHECK_HEARTBEAT_INTERVAL)
      //从缓存中删除过期的worker
      pastWorker.foreach(worker=>{
        workerInfoSet -= worker
        idToWorkerInfo -= worker.id
      })
      println("worker的数量："+workerInfoSet.size)
    }
  }




}

object Master{
  def main(args: Array[String]): Unit = {

    val host=args(0)
    val port=args(1)
    val masterName = "Master"
    //配置参数项
    val configStr=
      s"""
         |akka.actor.provider="akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname="$host"
         |akka.remote.netty.tcp.port="$port"
       """.stripMargin
    val config = ConfigFactory.parseString(configStr)
    val actorSystem = ActorSystem("MasterSystem",config)
    val master = actorSystem.actorOf(Props(new Master(masterName)),masterName)
//    master! "connect"
    actorSystem.awaitTermination()

  }
}
