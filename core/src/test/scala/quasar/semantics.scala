package quasar

import quasar.Predef._
import quasar.sql.SQLParser
import quasar.recursionschemes._, Recursive.ops._
import quasar.specs2._

import org.specs2.matcher.{Matcher, Expectable}
import org.specs2.mutable._

class SemanticsSpec extends Specification with PendingWithAccurateCoverage with TreeMatchers {

  "TransformSelect" should {
    import quasar.SemanticAnalysis._
    import quasar.sql._

    val compiler = Compiler.trampoline

    def transform(q: Expr): Expr = q.cata(projectSortKeysƒ)

    "add single field for order by" in {
      val q = Select(SelectAll,
                     Row(List(Ident("name"))),
                     Some(TableRelationAST("person", None)),
                     None,
                     None,
                     Some(OrderBy((ASC, Ident("height")) :: Nil)))
      transform(q) must beTree(
               Select(SelectAll,
                      Row(List(Ident("name"), As(Ident("height"), StringLiteral("__sd__0")))),
                      Some(TableRelationAST("person", None)),
                      None,
                      None,
                      Some(OrderBy((ASC, Ident("__sd__0")) :: Nil)))
               )
    }

    "not add a field that appears in the projections" in {
      val q = Select(SelectAll,
                     Row(List(Ident("name"))),
                     Some(TableRelationAST("person", None)),
                     None,
                     None,
                     Some(OrderBy((ASC, Ident("name")) :: Nil)))
      transform(q) must beTree(q)
    }

    "not add a field that appears as an alias in the projections" in {
      val q = Select(SelectAll,
                     Row(List(As(Ident("foo"), StringLiteral("name")))),
                     Some(TableRelationAST("person", None)),
                     None,
                     None,
                     Some(OrderBy((ASC, Ident("name")) :: Nil)))
      transform(q) must beTree(q)
    }

    "not add a field with wildcard present" in {
      val q = Select(SelectAll,
                     Row(List(Splice(None))),
                     Some(TableRelationAST("person", None)),
                     None,
                     None,
                     Some(OrderBy((ASC, Ident("height")) :: Nil)))
      transform(q) must beTree(q)
    }

    "add single field for order by" in {
      val q = Select(SelectAll,
                     Row(List(Ident("name"))),
                     Some(TableRelationAST("person", None)),
                     None,
                     None,
                     Some(OrderBy((ASC, Ident("height")) ::
                                  (ASC, Ident("name")) ::
                                  Nil)))
      transform(q) must beTree(
               Select(SelectAll,
                      Row(List(
                        Ident("name"),
                        As(Ident("height"), StringLiteral("__sd__0")))),
                      Some(TableRelationAST("person", None)),
                      None,
                      None,
                      Some(OrderBy((ASC, Ident("__sd__0")) ::
                                   (ASC, Ident("name")) ::
                                   Nil))))
    }

    "transform sub-select" in {
      val q = Select(SelectAll,
                     Row(List(Splice(None))),
                     Some(TableRelationAST("foo", None)),
                     Some(
                       Binop(
                         Ident("a"),
                         Select(SelectAll,
                                Row(List(Ident("a"))),
                                Some(TableRelationAST("bar", None)),
                                None,
                                None,
                                Some(OrderBy((ASC, Ident("b")) :: Nil))),
                         In)),
                     None,
                     None)
      transform(q) must beTree(
              Select(SelectAll,
                     Row(List(Splice(None))),
                     Some(TableRelationAST("foo", None)),
                     Some(
                       Binop(
                         Ident("a"),
                         Select(SelectAll,
                                Row(List(
                                  Ident("a"),
                                  As(Ident("b"), StringLiteral("__sd__0")))),
                                Some(TableRelationAST("bar", None)),
                                None,
                                None,
                                Some(OrderBy((ASC, Ident("__sd__0")) :: Nil))),
                         In)),
                     None,
                     None))
    }

  }
}
