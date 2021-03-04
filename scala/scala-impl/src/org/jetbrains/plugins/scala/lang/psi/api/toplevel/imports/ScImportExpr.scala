package org.jetbrains.plugins.scala
package lang
package psi
package api
package toplevel
package imports

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReference
import org.jetbrains.plugins.scala.lang.psi.api.expr

/**
 * @author Alexander Podkhalyuzin
 * Date: 20.02.2008
 */

trait ScImportExpr extends ScalaPsiElement {
  def reference: Option[ScStableCodeReference]

  def selectorSet: Option[ScImportSelectors]

  def selectors: Seq[ScImportSelector] = selectorSet.toSeq.flatMap {
    _.selectors.toSeq
  }

  def isSingleWildcard: Boolean

  def wildcardElement: Option[PsiElement]

  def qualifier: Option[ScStableCodeReference]

  def deleteExpr(): Unit

  def importedNames: Seq[String] = selectorSet match {
    case Some(set) => set.selectors.flatMap(_.importedName)
    case _ if isSingleWildcard => Seq("_")
    case _ => reference.toSeq.map(_.refName)
  }

  override protected def acceptScala(visitor: ScalaElementVisitor): Unit = visitor.visitImportExpr(this)
}

object ScImportExpr {
  object qualifier {
    def unapply(expr: ScImportExpr): Option[ScStableCodeReference] = expr.qualifier
  }
}