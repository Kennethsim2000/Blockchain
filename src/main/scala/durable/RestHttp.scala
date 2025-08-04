package durable

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol.jsonFormat4
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import durable.Network.SendToNode
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import durable.NodeDurable.{AddNewTransactionEvent, GetChainRequestEvent, MineEvent}
import durable.JsonFormats._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.io.StdIn
import scala.util.{Failure, Success}

object RestHttp {
    def main(args: Array[String]): Unit = {

        implicit val system : ActorSystem[Network.NetworkEvent] = ActorSystem(Network(), "network")
        implicit val executionContext = system.executionContext //needed for future flatmap/oncomplete
        implicit val timeout:Timeout = 3.seconds
        implicit val scheduler = system.scheduler

        final case class AddTransaction(nodeId: String,sender: String, receiver: String, amount: Int)
        implicit val addTransactionFormat: RootJsonFormat[AddTransaction] = jsonFormat4(AddTransaction.apply)
        //requires import spray.json.DefaultJsonProtocol._ for error no implicits found for default jsonprotocol

        //import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        // no implicits found for unmarshilling from request
        val route: Route =
            concat(
                post { //http://localhost:8080/transaction
                    path("transaction") {
                        entity(as[AddTransaction]) { transaction =>
                            system ! SendToNode(transaction.nodeId,
                                AddNewTransactionEvent(Transaction(transaction.sender, transaction.receiver, transaction.amount)))
                                complete("transaction created")
                        }
                    }
                },
                get { //http://localhost:8080/mine
                    path("mine" ) {
                        parameter("nodeId") { nodeId =>
                            system ! SendToNode(nodeId,
                                MineEvent)
                            complete("Block is mined")
                        }
                    }
                },
                get { //http://localhost:8080/chain/node1
                    pathPrefix("chain" / Segment) { nodeId =>
                        val futureChain: Future[List[Block]] =
                            system.ask(replyTo => SendToNode(nodeId, GetChainRequestEvent(replyTo)))

                        onComplete(futureChain) {
                            case Success(chain) =>
                                complete(chain)
                            case Failure(ex) =>
                                complete(StatusCodes.InternalServerError, s"Failed to get blockchain: ${ex.getMessage}")
                        }
                    }
                }

            )

        val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

        println(s"Server now online. Please navigate to http://localhost:8080")
        StdIn.readLine()
        bindingFuture
            .flatMap(_.unbind())
            .onComplete(_ => system.terminate())
    }

}

/*Endpoints i want to expose, get blockchain, POST transaction, mine current block in a node. */
