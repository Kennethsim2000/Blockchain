package durable.provider

import akka.Done
import akka.actor.ExtendedActorSystem
import akka.persistence.state.javadsl.{DurableStateUpdateStore, GetObjectResult => JavaGetObjectResult}
import com.typesafe.config.Config

import java.util.concurrent.CompletionStage
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global

class JavaFileDurableStateStoreAdapter(system: ExtendedActorSystem, config: Config, cfgPath: String)
    extends DurableStateUpdateStore[AnyRef] {

    private val scalaStore = new FileDurableStateStore[AnyRef](system, config, cfgPath)

    override def upsertObject(persistenceId: String, revision: Long, value: AnyRef, tag: String): CompletionStage[Done] =
        scalaStore.upsertObject(persistenceId, revision, value, tag).toJava

    override def deleteObject(persistenceId: String): CompletionStage[Done] =
        scalaStore.deleteObject(persistenceId).toJava

    override def deleteObject(persistenceId: String, revision: Long): CompletionStage[Done] =
        scalaStore.deleteObject(persistenceId, revision).toJava

    override def getObject(persistenceId: String): CompletionStage[JavaGetObjectResult[AnyRef]] = {
        scalaStore.getObject(persistenceId).map { scalaResult =>
            val javaOptional = scalaResult.value match {
                case Some(v) => java.util.Optional.of(v)
                case None => java.util.Optional.empty[AnyRef]()
            }
            JavaGetObjectResult(javaOptional, scalaResult.revision)
        }.toJava
    }

}