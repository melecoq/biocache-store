package au.org.ala.biocache.persistence

import scala.collection.mutable.{ListBuffer, HashMap}
import au.org.ala.biocache.util.Json
import java.util.UUID
import au.org.ala.biocache.dao.{OccurrenceDAOImpl, OccurrenceDAO}
import au.org.ala.biocache.index.{SolrIndexDAO, IndexDAO}
import org.slf4j.LoggerFactory

class MockPersistenceManager extends PersistenceManager {

  val logger = LoggerFactory.getLogger("MockPersistenceManager")

  override def toString = mockStore.toString

  private val mockStore = new HashMap[String, HashMap[String, HashMap[String, String]]]

  def clear = mockStore.clear

  def get(uuid: String, entityName: String, propertyName: String) = {
    logger.debug(s"Get for $uuid -  $entityName - $propertyName")
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap[String, HashMap[String, String]]())
    entityMap.get(uuid) match {
      case Some(map) => map.get(propertyName)
      case None => None
    }
  }

  def get(uuid: String, entityName: String) = {
    logger.debug(s"Get for $uuid -  $entityName")
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap[String, HashMap[String, String]]())
    entityMap.get(uuid) match {
      case Some(x) => Some(x.toMap)
      case None => None
    }
  }

  def getSelected(uuid: String, entityName: String, propertyNames: Seq[String]): Option[Map[String, String]] = {
    throw new RuntimeException("not implemented yet")
  }

  def pageOverColumnRange(entityName: String, proc: ((String, Map[String, String]) => Boolean), startUuid: String = "", endUuid: String = "", pageSize: Int = 1000, startColumn: String = "", endColumn: String = "") =
    throw new RuntimeException("not implemented yet")

  def getColumnsWithTimestamps(uuid: String, entityName: String): Option[Map[String, Long]] =
    throw new RuntimeException("not implemented yet")

  def getByIndex(uuid: String, entityName: String, idxColumn: String) =
    throw new RuntimeException("not implemented yet")

  def getByIndex(uuid: String, entityName: String, idxColumn: String, propertyName: String) =
    throw new RuntimeException("not implemented yet")

  def getList[A](uuid: String, entityName: String, propertyName: String, theClass: Class[_]): List[A] = {
    logger.debug(s"Get List for $uuid -  $entityName - $theClass")
    mockStore.get(entityName).get.get(uuid) match {
      case Some(x) => {
        val list = x.get(propertyName)
        if (list.isEmpty)
          List()
        else
          Json.toListWithGeneric(list.get, theClass)
      }
      case None => List()
    }
  }

  def put(uuid: String, entityName: String, propertyName: String, propertyValue: String, deleteIfNullValue: Boolean) = {
    logger.debug(s"Put for $uuid -  $entityName - $propertyName -  $propertyValue")
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap(uuid -> HashMap[String, String]()))
    val recordMap = entityMap.getOrElseUpdate(uuid, HashMap[String, String]())
    recordMap.put(propertyName, propertyValue)
    uuid
  }

  def put(uuid: String, entityName: String, keyValuePairs: Map[String, String], deleteIfNullValue: Boolean) = {
    logger.debug(s"Put for $uuid -  $entityName - $keyValuePairs")
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap(uuid -> HashMap[String, String]()))
    val recordMap = entityMap.getOrElse(uuid, HashMap[String, String]())
    entityMap.put(uuid, (recordMap ++ keyValuePairs).asInstanceOf[HashMap[String, String]])
    uuid
  }

  def putBatch(entityName: String, batch: Map[String, Map[String, String]], deleteIfNullValue: Boolean) =
    throw new RuntimeException("not implemented yet")

  def putList[A](uuid: String, entityName: String, propertyName: String, newList: Seq[A], theClass: Class[_], overwrite: Boolean, deleteIfNullValue: Boolean) = {
    logger.debug(s"PutList for $uuid -  $entityName - $propertyName - $newList - $theClass")
    val recordId = {
      if (uuid != null) uuid else UUID.randomUUID.toString
    }
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap(uuid -> HashMap[String, String]()))
    val recordMap = entityMap.getOrElse(uuid, HashMap[String, String]())
    if (overwrite) {
      val json: String = Json.toJSONWithGeneric(newList)
      recordMap.put(propertyName, json);
    } else {
      val currentList = getList(uuid, entityName, propertyName, theClass);
      var buffer = new ListBuffer[A]

      for (theObject <- currentList) {
        if (!newList.contains(theObject)) {
          //add to buffer
          buffer + theObject
        }
      }

      //PRESERVE UNIQUENESS
      buffer ++= newList

      // check equals
      //val newJson = Json.toJSON(buffer.toList)
      val newJson: String = Json.toJSONWithGeneric(buffer.toList)
      recordMap.put(propertyName, newJson)
    }
    recordId

  }

  def pageOverAll(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, endUuid: String, pageSize: Int) = {
    logger.debug(s"pageOverAll for  $entityName - $startUuid - $endUuid")
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap[String, HashMap[String, String]]())
    entityMap.keySet.foreach(key => {
      if (key.startsWith(startUuid) && !key.startsWith(endUuid)) {
        proc(key, entityMap.getOrElse(key, HashMap[String, String]()).toMap)
      }
    })
    //throw new RuntimeException("not implemented yet")
  }

  def pageOverSelect(entityName: String, proc: (String, Map[String, String]) => Boolean, startUuid: String, endUuid: String, pageSize: Int, columnName: String*) =
    throw new RuntimeException("not implemented yet")

  def selectRows(uuids: Seq[String], entityName: String, propertyNames: Seq[String], proc: (Map[String, String]) => Unit) =
    throw new RuntimeException("not implemented yet")

  def deleteColumns(uuid: String, entityName: String, columnName: String*) =
    throw new RuntimeException("not implemented yet")

  def delete(uuid: String, entityName: String) = {
    logger.debug(s"delete for  $uuid -  $entityName")
    val entityMap = mockStore.getOrElseUpdate(entityName, HashMap(uuid -> HashMap[String, String]()))
    entityMap.remove(uuid)
  }

  def shutdown = mockStore.clear
}