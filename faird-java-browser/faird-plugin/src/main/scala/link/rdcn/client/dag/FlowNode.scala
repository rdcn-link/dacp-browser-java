package link.rdcn.client.dag

import link.rdcn.struct.DataFrame

/**
 * @Author renhao
 * @Description:
 * @Data 2025/7/12 21:07
 * @Modified By:
 */
trait FlowNode

trait Transformer11 extends FlowNode with Serializable {
  def transform(dataFrame: DataFrame): DataFrame
}

case class RepositoryNode(
                    functionId: String,
                    args: Map[String, String] = Map.empty
                  ) extends FlowNode


//只为DAG执行提供dataFrameName
case class SourceNode (dataFrameName: String) extends FlowNode

object FlowNode {
  def source(dataFrameName: String): SourceNode = {
    SourceNode(dataFrameName)
  }

  def ofTransfomer(transformer11: Transformer11): Transformer11 = {
    transformer11
  }

  def ofScalaFunction(func: DataFrame => DataFrame): Transformer11 = {
    (dataFrame: DataFrame) => func(dataFrame)
  }

  def stocked(functionId: String, args: Map[String, String] = Map.empty): RepositoryNode = {
    RepositoryNode(functionId, args)
  }

}