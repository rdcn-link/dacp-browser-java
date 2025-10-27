package link.rdcn.server

import link.rdcn.dacp.FairdConfig
import link.rdcn.{AuthorProviderTest, DataProviderTest, DataReceiverTest}
import link.rdcn.dacp.client.DacpClient
import link.rdcn.dacp.server.DacpServer
import link.rdcn.user.{Credentials, UsernamePassword}

object ClientDemo {

  def main(args: Array[String]): Unit = {
    val dacpServer = new DacpServer(new DataProviderTest, new DataReceiverTest, new AuthorProviderTest)
    dacpServer.start(new FairdConfig)
    val dacpClient = DacpClient.connect("dacp://10.0.90.43:3102/listDataSets", UsernamePassword("15117913512@126.com", "admin#U*Q!."))

    val dfDataSets = dacpClient.get("dacp://10.0.90.43:3102/listDataFrames/1971803545715953664")
    //    println(dfDataSets.schema.columns)
    println("#########DataSet List")
    dfDataSets.foreach(println)
  }


}
