package org.jetbrains.plugins.scala.failed.typeInference

import org.jetbrains.plugins.scala.lang.typeInference.TypeInferenceTestBase

/**
  * Created by kate on 3/25/16.
  */

class PartialFunctionTest extends TypeInferenceTestBase {

  override protected def shouldPass: Boolean = false

  override def folderPath: String = super.folderPath + "bugs5/"

  def testSCL6716(): Unit = doTest()  // require PartialFunction, not function

  def testSCL10242(): Unit = doTest {
    """
      |class cl {
      |  def tapWith[A](f: A => Unit)(a: A): A = {
      |    f(a)
      |    a
      |  }
      |
      |  val g1: Int => Int = tapWith { /*start*/n => n.toLong/*end*/
      |  } _
      |}
      |//(_A) => Unit
    """.stripMargin.trim
  }
}
