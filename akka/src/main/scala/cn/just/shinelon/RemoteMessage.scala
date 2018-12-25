package cn.just.shinelon

trait RemoteMessage extends Serializable

//worker -> master
case class RegisterWorker(id:String,core:Int,size:Int) extends Serializable


//master -> worker
case class ToRegisterWorker(masterName:String) extends Serializable

case class MasterInfo(masterName:String) extends Serializable

//worker -> master
case class HeartBeat(id:String) extends Serializable

//master -> master
case class CheckHeartBeat