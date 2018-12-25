package cn.just.shinelon

import java.util.UUID

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.duration._

class Worker(val masterHost:String,val masterPort:Int,val Id:String,val Core:Int,val Size:Int) extends Actor{

  var master :ActorSelection=_
  var masterInfoSet = new mutable.HashSet[MasterInfo]()
  val HEARTBEAT_INTERVAL = 10000

  //连接master
  override def preStart(): Unit = {
    // /user+Master的名称
    println("masterHost:"+masterHost)
    println("masterPort:"+masterPort)
    master = context.actorSelection("akka.tcp://MasterSystem@"+masterHost+":"+masterPort+"/user/Master")
    println("向master注册")
    //向Master注册Worker
    master!RegisterWorker(Id,Core,Size)
  }

  override def receive = {
    case ToRegisterWorker(masterName)=>{
      //将master的信息加入worker的缓存中
      masterInfoSet += MasterInfo(masterName)
      println("注册成功，此时master为："+masterName)
      //启动akka定时器，向master定时发送心跳报告
      //加入隐式转换
      import context.dispatcher
      //自己给自己发送消息，然后在receive中转发发送给master
      context.system.scheduler.schedule(0 millis,HEARTBEAT_INTERVAL millis,self,HeartBeat(Id))
    }

    case HeartBeat(id)=>{
      //向master发送心跳报告
      master!HeartBeat(id)
      println("发送心跳报告")
    }
  }

}

object Worker{
  def main(args: Array[String]): Unit = {

    val host = args(0)
    val port = args(1)
    val masterHost = args(2)
    val masterPort= args(3).toInt
    //Worker的CPU核数目
    val core = args(4).toInt
    //硬盘大小
    val size = args(5).toInt
    //Worker的ID
    val Id = UUID.randomUUID().toString
    val configStr=
      s"""
         |akka.actor.provider="akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname="$host"
         |akka.remote.netty.tcp.port="$port"
       """.stripMargin
    val config = ConfigFactory.parseString(configStr)
    val actorSystem = ActorSystem("WorkerSystem",config)
    val worker = actorSystem.actorOf(Props(new Worker(masterHost,masterPort,Id,core,size)),"Worker")
    actorSystem.awaitTermination()
  }
}
