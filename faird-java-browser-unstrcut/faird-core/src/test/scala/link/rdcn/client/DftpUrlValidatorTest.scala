package link.rdcn.client

import link.rdcn.client.dacp.DacpUrlValidator
import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test

/**
 * @Author renhao
 * @Description:
 * @Date 2025/8/14 15:54
 * @Modified By:
 */
class DftpUrlValidatorTest {

  @Test
  def testValidateValidUrl(): Unit = {
    val result = DacpUrlValidator.validate("dftp://0.0.0.0:3101/listDataFrameNames/mydataset")
    result match {
      case Right((host, port, path)) =>
        assertEquals("0.0.0.0", host)
        assertEquals(Some(3101), port)
        assertEquals("/listDataFrameNames/mydataset", path)
      case Left(err) => fail(s"验证失败: $err")
    }
  }

  // 路径前缀验证测试
  @Test
  def testValidateWithPathPrefixSuccess(): Unit = {
    val result = DacpUrlValidator.validateWithPathPrefix(
      "dftp://example.com/getDataFrameSize/myframe",
      "/getDataFrameSize/"
    )
    assertTrue(result.isRight)
  }

  @Test
  def testValidateWithPathPrefixFailure(): Unit = {
    val result = DacpUrlValidator.validateWithPathPrefix(
      "dftp://example.com/wrongPrefix/myframe",
      "/getDataFrameSize/"
    )
    assertTrue(result.isLeft)
  }

  @Test
  def testValidateAndExtractParamSuccess(): Unit = {
    val result = DacpUrlValidator.validateAndExtractParam(
      "dftp://localhost:9090/listDataFrameNames/mydataset",
      "/listDataFrameNames/"
    )
    result match {
      case Right((host, port, param)) =>
        assertEquals("localhost", host)
        assertEquals(Some(9090), port)
        assertEquals("mydataset", param)
      case Left(err) => fail(s"参数提取失败: $err")
    }
  }

  @Test
  def testValidateAndExtractParamMissing(): Unit = {
    val result = DacpUrlValidator.validateAndExtractParam(
      "dftp://localhost:9090/listDataFrameNames/",
      "/listDataFrameNames/"
    )
    assertTrue(result.isLeft)
  }

  // 快速检查测试
  @Test
  def testIsValidPositive(): Unit = {
    assertTrue(DacpUrlValidator.isValid("dftp://example.com:8080"))
  }

  @Test
  def testIsValidNegative(): Unit = {
    assertFalse(DacpUrlValidator.isValid("dftp://bad:port:abc/path"))
  }

  // 边缘情况测试
  @Test
  def testEmptyPath(): Unit = {
    val result = DacpUrlValidator.validate("dftp://example.com")
    result match {
      case Right((_, _, path)) => assertEquals("/", path)
      case Left(err) => fail(s"空路径处理失败: $err")
    }
  }
}
