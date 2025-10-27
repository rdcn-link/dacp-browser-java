package link.rdcn

import link.rdcn.dacp.FairdConfig
import link.rdcn.dacp.client.DacpClient
import link.rdcn.dacp.provider.DataProvider
import link.rdcn.dacp.receiver.DataReceiver
import link.rdcn.dacp.server.DacpServer
import link.rdcn.dacp.struct.{DataFrameDocument, DataFrameStatistics}
import link.rdcn.dacp.user.{AuthProvider, DataOperationType}
import link.rdcn.struct.ClosableIterator
import org.junit.jupiter.api.Test
import link.rdcn.struct.ValueType.{DoubleType, IntType, StringType}
import link.rdcn.struct.{DataFrame, DataStreamSource, Row, StructType}
import link.rdcn.user._
import org.apache.jena.rdf.model.Model

import java.util.Arrays
import scala.io.Source

class DacpServerStart {

  @Test
  def serverDftpTest(): Unit = {
    val dacpServer = new DacpServer(new DataProviderTest, new DataReceiverTest, new AuthorProviderTest)
    dacpServer.start(new FairdConfig)

    // new UsernamePassword("15117913512@126.com", "admin#U*Q!.")
    val dacpClient = DacpClient.connect("dacp://10.0.90.43:3102", Credentials.ANONYMOUS)
//    val dacpClient = DacpClient.connect("dacp://10.0.90.43:3102", new UsernamePassword("15117913512@126.com", "admin#U*Q!."))

    println("#########DataSet List")

//    val dfDataSets = dacpClient.get("dacp://0.0.0.0:3101/listDataSets")
    val dfDataSets = dacpClient.get("dacp://10.0.90.43:3102/listDataSets")

    println("#########DataSet List")
    dfDataSets.foreach(println)
    val dfNames = dacpClient.get("dacp://10.0.90.43:3102/listDataFrames/1971803545715953664")
    println("#########DataFrame List")
    dfNames.foreach(println)
    val df = dacpClient.get("dacp://10.0.90.43:3102/xls_mnt_data_metacat_faird_test_化合物数据.xls_2478_df")
    println("###########println DataFrame")
    val s: StructType = df.schema
    df.foreach(println)

  }

  @Test
  def clientTest(): Unit = {
    val dacpClient = DacpClient.connect("dacp://0.0.0.0:3101", Credentials.ANONYMOUS)
    val dfDataSets = dacpClient.get("dacp://0.0.0.0:3101/listDataSets")
    //    println(dfDataSets.schema.columns)
    println("#########DataSet List")
    dfDataSets.foreach(println)
    val dfNames = dacpClient.get("dacp://0.0.0.0:3101/listDataFrames/dataSet1")
    println("#########DataFrame List")
    dfNames.foreach(println)
    val hostInfos = dacpClient.get("dacp://0.0.0.0:3101/listHostInfo")
    println("#########Host List")
    hostInfos.foreach(println)
  }

}

class DataReceiverTest extends DataReceiver {
  override def receive(dataFrame: DataFrame): Unit = ???
}


class DataProviderTest extends DataProvider {

  override def listDataSetNames(): java.util.List[String] = {
    Arrays.asList((1 to 10).map(i => s"dataSet$i"): _*)
  }

  override def getDataSetMetaData(dataSetId: String, rdfModel: Model): Unit = {}

  override def listDataFrameNames(dataSetId: String): java.util.List[String] = {
    Arrays.asList((1 to 8).map(i => s"dataFrame$i"): _*)
  }

  def getDataStreamSource(dataFrameName: String): DataStreamSource = new DataStreamSource {

    override def rowCount: Long = -1 // 如果不知道总行数

    override def schema: StructType = StructType.empty
      .add("VendorID", IntType)
      .add("tpep_pickup_datetime", StringType)
      .add("tpep_dropoff_datetime", StringType)
      .add("passenger_count", IntType)
      .add("trip_distance", DoubleType)
      .add("pickup_longitude", DoubleType)
      .add("pickup_latitude", DoubleType)
      .add("RateCodeID", IntType)
      .add("store_and_fwd_flag", StringType)
      .add("dropoff_longitude", DoubleType)
      .add("dropoff_latitude", DoubleType)
      .add("payment_type", IntType)
      .add("fare_amount", DoubleType)
      .add("extra", DoubleType)
      .add("mta_tax", DoubleType)
      .add("tip_amount", DoubleType)
      .add("tolls_amount", DoubleType)
      .add("improvement_surcharge", DoubleType)
      .add("total_amount", DoubleType)

    // 安全转换函数
    private def safeToInt(s: String): Int = if (s == null || s.trim.isEmpty) 0 else s.toInt
    private def safeToDouble(s: String): Double = if (s == null || s.trim.isEmpty) 0.0 else s.toDouble

    override def iterator: ClosableIterator[Row] = {
      val source = Source.fromFile("/Volumes/MyPassport/archive/yellow_tripdata_2015-01.csv")
      val lines = source.getLines()

      // 跳过表头
      val dataLines = lines.drop(1)

      // 转换每一行成 Row
      val rows = dataLines.map { line =>
        // 使用正则处理引号包裹的字段，保证 split 安全
        val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1).map(_.replaceAll("^\"|\"$", ""))

        Row.fromSeq(Seq(
          safeToInt(parts(0)),
          parts(1),
          parts(2),
          safeToInt(parts(3)),
          safeToDouble(parts(4)),
          safeToDouble(parts(5)),
          safeToDouble(parts(6)),
          safeToInt(parts(7)),
          parts(8),
          safeToDouble(parts(9)),
          safeToDouble(parts(10)),
          safeToInt(parts(11)),
          safeToDouble(parts(12)),
          safeToDouble(parts(13)),
          safeToDouble(parts(14)),
          safeToDouble(parts(15)),
          safeToDouble(parts(16)),
          safeToDouble(parts(17)),
          safeToDouble(parts(18))
        ))
      }.toIterator

      // 使用 ClosableIterator 确保资源关闭
      ClosableIterator(rows) { source.close() }
    }
  }

  override def getDocument(dataFrameName: String): DataFrameDocument = {
    new DataFrameDocument {
      override def getSchemaURL(): Option[String] = Some("")

      override def getColumnURL(colName: String): Option[String] = Some("")

      override def getColumnAlias(colName: String): Option[String] = Some("")

      override def getColumnTitle(colName: String): Option[String] = Some("")

      override def getDataFrameTitle(): Option[String] = Some("")
    }
  }

  override def getStatistics(dataFrameName: String): DataFrameStatistics = {
    new DataFrameStatistics {
      override def rowCount: Long = -1L

      override def byteSize: Long = -1L
    }
  }

  override def getDataFrameMetaData(dataFrameName: String, rdfModel: Model): Unit = {}
}

class AuthorProviderTest extends AuthProvider {

  override def checkPermission(user: UserPrincipal, dataFrameName: String, opList: List[DataOperationType]): Boolean = {
    true
//    throw new Exception("wufan")
//    false
  }

  override def authenticate(credentials: Credentials): UserPrincipal =
    {

//      System.out.println("1111")
      if (Credentials.ANONYMOUS == credentials) {
        System.out.println("用户正在匿名登陆....")
        throw new RuntimeException("匿名用户无法认证")
      }
      new UserPrincipal {}
    }

}
