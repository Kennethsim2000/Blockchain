package durable.provider

import akka.Done
import akka.actor.ExtendedActorSystem
import akka.persistence.state.scaladsl.{DurableStateUpdateStore, GetObjectResult}
import com.typesafe.config.Config

import java.io.{File, FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.concurrent.{ExecutionContext, Future}

class FileDurableStateStore[T](system: ExtendedActorSystem, config: Config, cfgPath: String) extends DurableStateUpdateStore[T]{

    private implicit val ec: ExecutionContext = system.dispatcher
    private val baseDir : String = config.getString("dir") match {
        case path if path.nonEmpty => path
        case _ => "target/durable-state"
    }

    private def stateFile(persistenceId: String): File = new File(s"$baseDir/$persistenceId.state")

    override def upsertObject(persistenceId: String, revision: Long, value: T, tag: String): Future[Done] = Future {
        val file = stateFile(persistenceId)
        file.getParentFile.mkdirs()
        //Ensures that all the directories leading up to the file path exist — creates them if not.
        val oos = new ObjectOutputStream(new FileOutputStream(file)) //Creates an output stream to write binary data to the file.
        try {
            oos.writeLong(revision)
            oos.writeUTF(tag)
            oos.writeObject(value)
        }finally {
            oos.close()
        }
        Done
    }
    //This writes 3 things to the file in order:
    //A 64-bit long (the revision)
    //A UTF-8 string (the tag)
    //A full serialized object (value)

    override def deleteObject(persistenceId: String): Future[Done] = Future {
        val file = stateFile(persistenceId)
        if(file.exists()) {
            file.delete()
        }
        Done
    }

    override def deleteObject(persistenceId: String, revision: Long): Future[Done] = Future {
        val file = stateFile(persistenceId)
        if(file.exists()) {
            val ois = new ObjectInputStream(new FileInputStream(file))
            val storedRevision = try {
                ois.readLong() // reads the first 8 bytes
            } finally {
                ois.close()
            }
            if(storedRevision == revision) file.delete()
        }
        Done

    }

    override def getObject(persistenceId: String): Future[GetObjectResult[T]] = Future {
        val file = stateFile(persistenceId)
        if(!file.exists()) {
            GetObjectResult(None, 0L)
        } else {
            val ois = new ObjectInputStream(new FileInputStream(file))
            try {
                val revision = ois.readLong()
                val tag = ois.readUTF()
                val value = ois.readObject().asInstanceOf[T]
                GetObjectResult(Some(value), revision)
            } finally {
                ois.close()
            }
        }
    }
}
