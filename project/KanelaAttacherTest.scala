
import sbt.Def.Setting
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import sbt.internal.inc.Analysis
import xsbti.api.{Annotation, Projection}

object KanelaAttacherTest {

  import ExtractProps._

  lazy val settings: Seq[Setting[_]] = Seq(
    fork in Test := true,
    forkTestSettings,
    testGrouping in Test := Def.task {
      forkedJvmPerTest(
        (definedTests in Test).value,
        (javaOptions in Test).value,
        (forkTests in Test).value)
    }.value
  )

  protected def forkedJvmPerTest(testDefs: Seq[TestDefinition],
                                 jvmSettings: Seq[String],
                                 forkTestDescriptions: Seq[ForkTestDescription]): Seq[Group] = {
    val forkTestDescriptionByName: Map[String, ForkTestDescription] = forkTestDescriptions.map(t => t.className -> t).toMap
    val (forkTests, otherTests) = testDefs.partition { testDef => forkTestDescriptionByName.contains(testDef.name) }
    val otherTestsGroup = Group(name = "Single JVM tests", tests = otherTests, runPolicy = SubProcess(ForkOptions()))
    val forkedTestGroups = forkTests map { test =>
      Group(
        name = test.name,
        tests = Seq(test),
        runPolicy = SubProcess(config = forkTestDescriptionByName(test.name).buildForkOptions(jvmSettings)))
    }
    Seq(otherTestsGroup) ++ forkedTestGroups
  }

  protected lazy val forkTests: TaskKey[Seq[ForkTestDescription]] = taskKey[Seq[ForkTestDescription]]("Returns list of tests annotated with ForkTest")


  protected def forkTestSettings: Setting[Task[Seq[ForkTestDescription]]] = forkTests in Test := {
        val analysis:Analysis = (compile in Test).value.asInstanceOf[Analysis]
        analysis.apis.internal.values.flatMap(analysedClass => {
          val className = analysedClass.name()
          val annotations = analysedClass.api().classApi().annotations()

          forkTestsDescription2(className, annotations)

        }).toSeq
      }

  def forkTestsDescription2(className: String, annotations: Seq[Annotation]):  Option[ForkTestDescription] = {
    annotations
      .find { annotation: Annotation =>
        annotation.base match {
          case proj: Projection if proj.id() == "ForkTest" => true
          case _ => false
        }
      }
      .map { annotation: Annotation =>
        val params: Seq[String] = extractProperty[String](annotation)("extraJvmOptions").toSeq.flatMap(_.split(" "))
        val attachKanelaAgent: Boolean = extractProperty[Boolean](annotation)("attachKanelaAgent").getOrElse(true)
        ForkTestDescription(className, params, attachKanelaAgent)
      }
  }

  protected def extractProperty[T: AnnotationPropertyExtractor](annotation: xsbti.api.Annotation)(name: String): Option[T] = {
    annotation
      .arguments()
      .find(_.name() == name)
      .map(annotationArgument => {
        implicitly[AnnotationPropertyExtractor[T]].extract(annotationArgument.value())
      })
  }

  protected case class ForkTestDescription(className: String,
                                           extraJvmOptions: Seq[String],
                                           attachKanelaAgent: Boolean = true) {

    def buildForkOptions(jvmSettings: Seq[String]): ForkOptions = {

      val (jvmSettingsWithAgent: Seq[String], bootJarPaths: Seq[String]) = {
        if (attachKanelaAgent)
          (jvmSettings, Seq.empty)
        else
          settingsExcludingAgent(jvmSettings)
      }

      val allParameters = jvmSettingsWithAgent ++ extraJvmOptions
      ForkOptions().withRunJVMOptions(allParameters.toVector).withBootJars(bootJarPaths.map(new File(_)).toVector)
    }

    private def settingsExcludingAgent(jvmSettings: Seq[String]): (Seq[String], Seq[String]) = {
      val (settings, agent) = jvmSettings.partition(param => !(param.startsWith("-javaagent:") && param.contains("io.kamon/kanela-agent")))
      (settings, agent.headOption.map(_.replaceFirst("-javaagent:", "")).toSeq)
    }
  }

}

object ExtractProps {

  trait AnnotationPropertyExtractor[A] {
    def extract(value: String): A
  }

  implicit val StringExtractProp: AnnotationPropertyExtractor[String] = new AnnotationPropertyExtractor[String] {
    def extract(value: String): String = {
      if (value.charAt(0) == '"')
        value.substring(1, value.length - 1)
      else
        value
    }
  }

  implicit val BooleanExtractProp: AnnotationPropertyExtractor[Boolean] = new AnnotationPropertyExtractor[Boolean] {
    def extract(value: String): Boolean = {
      if (value.charAt(0) == '"')
        value.substring(1, value.length - 1).toBoolean
      else
        value.toBoolean
    }
  }
}
