package link.rdcn.util

import link.rdcn.struct.{Column, DataFrame, DefaultDataFrame, Row, StructType, ValueType}
import io.circe.{DecodingFailure, parser}
import org.apache.arrow.vector.types.Types
import org.apache.arrow.flight.Result
import org.apache.arrow.memory.BufferAllocator
import org.apache.arrow.vector.ipc.ArrowStreamWriter
import org.apache.arrow.vector._
import org.apache.jena.rdf.model.Model

import java.io.ByteArrayOutputStream
import scala.collection.JavaConverters.{asScalaBufferConverter, asScalaIteratorConverter, seqAsJavaListConverter}

object ClientUtils {

  def parseFlightActionResults(resultIterator: java.util.Iterator[Result]): DataFrame = {
    val allRows = scala.collection.mutable.ArrayBuffer[Row]()
    var schema: StructType = StructType.empty
    while (resultIterator.hasNext) {
      val result = resultIterator.next()
      val vectorSchemaRootReceived = ServerUtils.getVectorSchemaRootFromBytes(result.getBody, allocator)
      schema = arrowSchemaToStructType(vectorSchemaRootReceived.getSchema)
      val rowCount = vectorSchemaRootReceived.getRowCount
      val fieldVectors = vectorSchemaRootReceived.getFieldVectors.asScala

      Seq.range(0, rowCount).foreach { rowIndex =>
        val rowValues = fieldVectors.map { vector =>
          if (vector.isNull(rowIndex)) {
            null
          } else {
            vector match {
              case v: VarCharVector   => v.getObject(rowIndex).toString
              case v: IntVector      => v.get(rowIndex)
              case v: BigIntVector    => v.get(rowIndex)
              case v: Float4Vector    => v.get(rowIndex)
              case v: Float8Vector    => v.get(rowIndex)
              case v: BitVector       => v.get(rowIndex) != 0
              case v: VarBinaryVector => v.get(rowIndex)
              case _ => throw new UnsupportedOperationException(
                s"Unsupported type: ${vector.getClass}"
              )
            }
          }
        }
        allRows += Row.fromSeq(rowValues)
      }
    }
    DefaultDataFrame(schema, ClosableIterator(allRows.toIterator)())
  }

  def arrowSchemaToStructType(schema: org.apache.arrow.vector.types.pojo.Schema): StructType = {
    val columns = schema.getFields.asScala.map { field =>
      val colType = field.getType match {
        case t if t == Types.MinorType.INT.getType  => ValueType.IntType
        case t if t == Types.MinorType.BIGINT.getType => ValueType.LongType
        case t if t == Types.MinorType.FLOAT4.getType => ValueType.FloatType
        case t if t == Types.MinorType.FLOAT8.getType => ValueType.DoubleType
        case t if t == Types.MinorType.VARCHAR.getType => ValueType.StringType
        case t if t == Types.MinorType.BIT.getType => ValueType.BooleanType
        case t if t == Types.MinorType.VARBINARY.getType => ValueType.BinaryType
        case _ => throw new UnsupportedOperationException(s"Unsupported Arrow type: ${field.getType}")
      }
      Column(field.getName, colType)
    }
    StructType(columns.toList)
  }

  def getBytesFromVectorSchemaRoot(root: VectorSchemaRoot): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()
    val writer = new ArrowStreamWriter(root, null, outputStream)
    writer.start()
    writer.writeBatch()
    writer.end()
    writer.close()
    outputStream.toByteArray
  }

  def getListStringByResult(resultIterator: Iterator[Result]): Seq[String] = {
    if (resultIterator.hasNext) {
      val result = resultIterator.next
      val vectorSchemaRootReceived = ServerUtils.getVectorSchemaRootFromBytes(result.getBody, allocator)
      val rowCount = vectorSchemaRootReceived.getRowCount
      val schema = vectorSchemaRootReceived.getSchema
      val fieldVectors = vectorSchemaRootReceived.getFieldVectors.asScala
      Seq.range(0, rowCount).map(index => {
        val rowMap = fieldVectors.map(vec => {
          vec.asInstanceOf[VarCharVector].getObject(index).toString
        }).head
        rowMap
      })
    } else null
  }

  def getSingleStringByResult(resultIterator: Iterator[Result]): String = {
    if (resultIterator.hasNext) {
      val result = resultIterator.next
      val vectorSchemaRootReceived = ServerUtils.getVectorSchemaRootFromBytes(result.getBody, allocator)
      val fieldVectors = vectorSchemaRootReceived.getFieldVectors.asScala
      fieldVectors.head.asInstanceOf[VarCharVector].getObject(0).toString
    } else null
  }

  def getSingleLongByResult(resultIterator: Iterator[Result]): Long = {
    if (resultIterator.hasNext) {
      val result = resultIterator.next
      val vectorSchemaRootReceived = ServerUtils.getVectorSchemaRootFromBytes(result.getBody, allocator)
      val fieldVectors = vectorSchemaRootReceived.getFieldVectors.asScala
      fieldVectors.head.asInstanceOf[BigIntVector].getObject(0)
    } else 0L
  }

  def getMapByJsonString(hostInfoString: String): Map[String, String] = {
    val parseResult: Either[io.circe.Error, Map[String, String]] = parser.parse(hostInfoString).flatMap { json =>
      json.asObject.toRight(
        // 如果不是对象，则返回一个 DecodingFailure
        DecodingFailure("JSON is not an object", List.empty)
      ).map { jsonObject =>
        // 将 JsonObject 转换为 Map[String, Json]
        jsonObject.toMap.mapValues {
          case jsonValue if jsonValue.isString => jsonValue.asString.get // 如果是 JSON 字符串，直接取其值
          case other => other.toString() // 修改为 toString() 以兼容其他 Json 类型
        }
      }
    }
    parseResult.fold(
      error => throw new RuntimeException(s"Error parsing JSON: $error"),
      identity // 如果解析成功，直接返回结果
    )
  }

  def getArrayBytesResult(resultIterator: Iterator[Result]): Array[Byte] = {
    if (resultIterator.hasNext) {
      val result = resultIterator.next
      val vectorSchemaRootReceived = ServerUtils.getVectorSchemaRootFromBytes(result.getBody, allocator)
      val fieldVectors = vectorSchemaRootReceived.getFieldVectors.asScala
      fieldVectors.head.asInstanceOf[VarBinaryVector].getObject(0)
    } else null
  }

  def init(allocatorServer: BufferAllocator): Unit = {
    allocator = allocatorServer
  }

  private var allocator: BufferAllocator = _
}
