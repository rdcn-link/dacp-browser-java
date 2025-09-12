package link.rdcn

import link.rdcn.client.dacp.FairdClient
import link.rdcn.provider.{DataFrameDocument, DataFrameStatistics, DataProvider}
import link.rdcn.received.DataReceiver
import link.rdcn.server.dacp.DacpServer
import link.rdcn.struct.ValueType.{DoubleType, IntType, StringType}
import link.rdcn.struct.{DataFrame, DataStreamSource, Row, StructType}
import link.rdcn.user._
import link.rdcn.util.ClosableIterator
import link.rdcn.{ConfigLoader, FairdConfig}
import org.apache.jena.rdf.model.Model
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.xml.XmlBeanFactory
import org.springframework.core.io.ClassPathResource

import java.util
import java.util.Arrays
import scala.io.Source

class SpringIOCServerStartTest {
  @Test
  def serverStart(): Unit = {
    val f = new XmlBeanFactory(new ClassPathResource("applicationContext.xml"))
    val dataReceiver = f.getBean("dataReceiver").asInstanceOf[DataReceiver]
    val dataProvider = f.getBean("dataProvider").asInstanceOf[DataProvider]
    val authProvider = f.getBean("authProvider").asInstanceOf[AuthProvider]
    val fairdHome = getClass.getClassLoader.getResource("").getPath

    val server: DacpServer = new DacpServer(dataProvider, dataReceiver, authProvider)

    ConfigLoader.init(fairdHome)
    server.start(ConfigLoader.fairdConfig)
    val client = FairdClient.connect("dacp://0.0.0.0:3101", Credentials.ANONYMOUS)
    assert(client.listDataSetNames().head == "dataSet1")
    assert(client.listDataFrameNames("dataSet1").head == "dataFrame1")
  }

  @Test
  def serverDstpTest(): Unit = {
    val dacpServer = new DacpServer(new DataProviderTest, new DataReceiverTest, new AuthorProviderTest)
    dacpServer.start(new FairdConfig)
    val dacpClient = FairdClient.connect("dacp://0.0.0.0:3101", Credentials.ANONYMOUS)
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
//    val df = dacpClient.get("dacp://0.0.0.0:3101/dataFrame1")
//    println("###########println DataFrame")
//    val s: StructType = df.schema
//    df.foreach(println)
  }

}

class DataReceiverTest extends DataReceiver {
  /** Called once before receiving any rows */
  override def start(): Unit = ???

  /** Called for each received batch of rows */
  override def receiveRow(dataFrame: DataFrame): Unit = ???

  /** Called after all batches are received successfully */
  override def finish(): Unit = ???
}


class DataProviderTest extends DataProvider {

  /**
   * 列出所有数据集名称
   *
   * @return java.util.List[String]
   */
  override def listDataSetNames(): java.util.List[String] = {
    Arrays.asList((1 to 10).map(i => s"dataSet$i"): _*)
  }



  /**
   * 获取数据集的 RDF 元数据，填充到传入的 rdfModel 中
   *
   * @param dataSetId 数据集 ID
   * @param rdfModel  RDF 模型（由调用者传入，方法将其填充）
   */
  override def getDataSetMetaData(dataSetId: String, rdfModel: Model): Unit = {}

  /**
   * 列出指定数据集下的所有数据帧名称
   *
   * @param dataSetId 数据集 ID
   * @return java.util.List[String]
   */
  override def listDataFrameNames(dataSetId: String): java.util.List[String] = {
    Arrays.asList((1 to 8).map(i => s"dataFrame$i"): _*)
  }

  /**
   * 获取数据帧的数据流
   *
   * @param dataFrameName 数据帧名（如 mnt/a.csv)
   * @return 数据流源
   */
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
      val source = Source.fromFile("/Volumes/My Passport/archive/yellow_tripdata_2015-01.csv")
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



  /**
   * 获取数据帧详细信息
   *
   * @param dataFrameName 数据帧名
   * @return 数据帧的DataFrameDocument
   */
  override def getDocument(dataFrameName: String): DataFrameDocument = {
    new DataFrameDocument {
      override def getSchemaURL(): Option[String] = Some("")

      override def getColumnURL(colName: String): Option[String] = Some("")

      override def getColumnAlias(colName: String): Option[String] = Some("")

      override def getColumnTitle(colName: String): Option[String] = Some("")
    }
  }

  /** *
   * 获取数据帧统计信息
   *
   * @param dataFrameName 数据帧名
   * @return 数据帧的DataFrameStatistics
   */
  override def getStatistics(dataFrameName: String): DataFrameStatistics = {
    new DataFrameStatistics {
      override def rowCount: Long = -1

      override def byteSize: Long = -1
    }
  }
}

class AuthorProviderTest extends AuthProvider {
  /**
   * 用户认证，成功返回认证后的保持用户登录状态的凭证
   *
   * @throws AuthorizationException
   */
  override def authenticate(credentials: Credentials): AuthenticatedUser = new AuthenticatedUser{
    override def token: String = {
      credentials match {
        case UsernamePassword("Admin", "Admin") => "1"
        case _ => "2"
      }
    }
  }

  /**
   * 判断用户是否具有某项权限
   *
   * @param user          已认证用户
   * @param dataFrameName 数据帧名称
   * @param opList        操作类型列表（Java List）
   * @return 是否有权限
   */
  override def checkPermission(user: AuthenticatedUser, dataFrameName: String, opList: java.util.List[DataOperationType]): Boolean = {
    if(user.token == "1") true else false
//    true
  }
}
