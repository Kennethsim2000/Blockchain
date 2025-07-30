package durable

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import durable.NodeDurable.{NodeEvent, ReceiveNewBlockEvent}

import scala.concurrent.duration.DurationInt

object Network {
    sealed trait NetworkEvent
    case class broadcastBlockEvent(block:Block, nodeId: String) extends NetworkEvent
    case class SendToNode(nodeId:String, event: NodeEvent) extends NetworkEvent

    def apply(): Behavior[NetworkEvent] =
        networkBehavior()

    private def networkBehavior(): Behavior[NetworkEvent] = {
        Behaviors.setup { context =>
            implicit val timeout: Timeout = 3.seconds
            val node1 = context.spawn(NodeDurable("node1", context.self), "node1")
            val node2 = context.spawn(NodeDurable("node2", context.self), "node2")
            val nodeMap = Map("node1" -> node1, "node2" -> node2)
            val peers = List("node1" -> node1, "node2" -> node2)
            Behaviors.receive { (context, message) =>
                message match {
                    case broadcastBlockEvent(block, senderId) =>
                        context.log.info(s"Received broadcasting event from $senderId")
                        val broadcast = peers.filterNot {
                            case (id, _) => id == senderId
                        }
                        broadcast.foreach {
                            case (id, actor) => actor ! ReceiveNewBlockEvent(block)
                        }
                        Behaviors.same
                    case SendToNode(nodeId, event) =>
                        nodeMap.get(nodeId).foreach(_ ! event)
                        Behaviors.same
                }
            }

        }
    }
}
