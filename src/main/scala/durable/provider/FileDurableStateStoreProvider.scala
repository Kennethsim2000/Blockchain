package durable.provider

import akka.actor.ExtendedActorSystem
import akka.persistence.state.DurableStateStoreProvider
import akka.persistence.state.javadsl.DurableStateStore
import akka.persistence.state.scaladsl.DurableStateStore
import com.typesafe.config.Config

class FileDurableStateStoreProvider(system: ExtendedActorSystem, config: Config, cfgPath: String) extends DurableStateStoreProvider {

    override def scaladslDurableStateStore(): akka.persistence.state.scaladsl.DurableStateStore[Any] =
        new FileDurableStateStore(system, config, cfgPath)

    override def javadslDurableStateStore(): akka.persistence.state.javadsl.DurableStateStore[AnyRef] =
        new JavaFileDurableStateStoreAdapter(system, config, cfgPath)
}
