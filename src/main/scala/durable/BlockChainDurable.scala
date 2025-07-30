package durable


import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}
import durable.Network.{NetworkEvent, broadcastBlockEvent}


//This is the actor that contains all the blocks
object BlockChainDurable {

    sealed trait BlockChainEvent

    case class GetChainEvent(replyTo: ActorRef[List[Block]]) extends BlockChainEvent
    case class AddBlockEvent(block: Block) extends BlockChainEvent
    case class AddNeighbourBlockEvent(block: Block) extends BlockChainEvent
    case class GetLastHashEvent(replyTo: ActorRef[String]) extends BlockChainEvent
    case class getIndexEvent(replyTo: ActorRef[Int]) extends BlockChainEvent

    case class BlockChainState(blocks: List[Block]) extends Serializable

    def apply(nodeId: String,  network: ActorRef[NetworkEvent]): Behavior[BlockChainEvent] = {
        val genesisBlock = BlockDurable.createGenesisBlock()
        blockchainDurableBehavior(List(genesisBlock), nodeId, network)
    }

    private def blockchainDurableBehavior(blocks: List[Block], nodeId: String,  network: ActorRef[NetworkEvent]): Behavior[BlockChainEvent] = {
        Behaviors.setup { context:ActorContext[BlockChainEvent] =>
            DurableStateBehavior[BlockChainEvent, BlockChainState](
                    persistenceId = PersistenceId.ofUniqueId(nodeId),
                    emptyState = BlockChainState(blocks),
                    commandHandler = (state, command) => {
                        command match {
                            case GetChainEvent(replyTo) =>
                                Effect.reply(replyTo)(state.blocks)
                            case AddBlockEvent(block)   =>
                                val valid = BlockDurable.isValidProof(block)
                                if(valid) {
                                    context.log.info("Block is valid, added")
                                    network ! broadcastBlockEvent(block, nodeId)
                                    Effect.persist(state.copy(blocks = state.blocks :+ block))
                                }
                                else {
                                    context.log.info("Block is invalid, rejected")
                                    Effect.none
                                }
                            case AddNeighbourBlockEvent(block) =>
                                val valid = BlockDurable.isValidProof(block)
                                if(valid) {
                                    context.log.info("Neighbour block is valid, added")
                                    Effect.persist(state.copy(blocks = state.blocks :+ block))
                                }
                                else {
                                    context.log.info("Block is invalid, rejected")
                                    Effect.none
                                }
                            case getIndexEvent(replyTo) =>
                                val lastBlock = state.blocks.last
                                Effect.reply(replyTo)(lastBlock.index + 1)
                            case GetLastHashEvent(replyTo) =>
                                val lastBlock = state.blocks.last
                                Effect.reply(replyTo)(lastBlock.hash)
                        }
                    }

                )
        }

    }
}