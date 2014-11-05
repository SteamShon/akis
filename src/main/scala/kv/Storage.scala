package kv

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.collection.mutable.ListBuffer
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

object Storage {
  case class Value(value: String)
  case object Get
  case class Put(value: String)
  case class GetRes(values: ListBuffer[String])
}
class Storage extends Actor with ActorLogging {

  import Storage._
  
  val stored = ListBuffer.empty[String]
  
  val cluster = Cluster(context.system)

  private def logMsg(msg: Any) = {
    log.info(s"$self $msg")
  }
  // subscribe to cluster changes, re-subscribe when restart 
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    
        
    case Get =>
      logMsg("got get request")
      sender ! GetRes(stored)
      
    case Put(value) => 
      logMsg(s"got put request: $value")
      stored += value
      
    case _: MemberEvent => // ignore
  }
}
