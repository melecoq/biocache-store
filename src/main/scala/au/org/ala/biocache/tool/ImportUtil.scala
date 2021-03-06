package au.org.ala.biocache.tool

import scala.util.parsing.json.JSON
import org.apache.commons.io.FileUtils
import java.io.{File, FileReader}
import au.com.bytecode.opencsv.CSVReader
import org.apache.commons.lang.StringUtils
import au.org.ala.biocache.Config
import au.org.ala.biocache.util.OptionParser
import au.org.ala.biocache.cmd.Tool

/**
 * Load a CSV file into the BioCache store.
 *
 * Note: This is only intended to be used for development purposes.
 */
object ImportUtil extends Tool {

  def cmd = "import"
  def desc = "Import data (not for production use)"

  def main(args: Array[String]) {

    var entity = ""
    var fieldsToImport = List[String]()
    var filesToImport = List[String]()
    var linesToSkip = 0
    var quoteChar: Option[Char] = None
    var charSeparator= '\t'
    var idColumnIdx = 0
    var json =false

    val parser = new OptionParser(help) {
      arg("entity", "the entity (column family in cassandra) to export from", { v: String => entity = v })
      arg("files-to-import", "the file(s) to import, space separated", { v: String => filesToImport = v.split(" ").toList })
      opt("c", "columns", "<column1 column2 ...>", "column headers", { columns: String => fieldsToImport = columns.split(",").toList })
      opt("sp", "separator", "column separator", "column separator for file to import", { v: String => charSeparator = v.charAt(0) })
      opt("qc", "quotechar", "quote character", "column separator for file to import", { v: String => quoteChar = Some(v.charAt(0)) })
      opt("cf", "column header file", "e.g. /data/headers.txt", "column headers", {
        v: String => fieldsToImport = FileUtils.readFileToString(new File(v)).trim.split(',').toList
      })
      opt("json","import the values as json",{json=true})
      intOpt("s", "skip-line", "number of lines to skip before importing", { v: Int => linesToSkip = v })
      intOpt("id", "id-column-idx", "id column index. indexed from 0", { v: Int => idColumnIdx = v })
    }

    if (parser.parse(args)) {
      val pm = Config.persistenceManager
      //process each file
      filesToImport.foreach {
        if(json)
          importJson(entity,_)
        else
          importFile(entity, fieldsToImport, charSeparator, quoteChar, _, idColumnIdx)
      }
      pm.shutdown
    }
  }
  
  def importJson(entity: String, filepath:String){
    for(line <- scala.io.Source.fromFile(filepath).getLines()){
      //now turn the line into JSON
     val map =JSON.parseFull(line).get.asInstanceOf[Map[String, String]]
     val guid= map.get(entity+"rowKey")
     if(guid.isDefined){
       val finalMap:Map[String,String] = map - (entity+"rowKey")
       //println(finalMap)
       //now add the record
       Config.persistenceManager.put(guid.get, entity, finalMap, false)
     }
    }
  }
  
  def importFile(entity: java.lang.String, fieldsToImport: List[String], separator: Char,  quotechar: Option[Char], filepath: String, idColumnIdx:Int = 0) {
    val reader = quotechar.isEmpty match {
      case false => new CSVReader(new FileReader(filepath), separator, quotechar.get)
      case _ => new CSVReader(new FileReader(filepath), separator)
    }

    var currentLine = reader.readNext
    var counter = 1
    while (currentLine != null) {
      //println("Reading line: " + currentLine)
      val columns = currentLine.toList
      //println(columns)
      if (columns.length == fieldsToImport.length && StringUtils.isNotEmpty(columns(idColumnIdx))) {
        val map = (fieldsToImport zip columns).toMap[String, String].filter {
          case (key, value) => value != null && value.toString.trim.length > 0
        }
        Config.persistenceManager.put(columns(idColumnIdx), entity, map, false)
      } else {
        println("Problem loading line: " + counter + ", cols:fields = " + columns.length +":"+ fieldsToImport.length)
      }
      counter += 1
      //read next
      currentLine = reader.readNext
    }
  }
}