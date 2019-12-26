import ammonite.ops.cp
import mill._
import mill.define.{Target, Task}
import mill.scalalib._
import $ivy.`com.lihaoyi::mill-contrib-scalapblib:$MILL_VERSION`
import contrib.scalapblib._
import scalalib._
import mill.modules.Jvm
import mill.define.Task
import ammonite.ops._
import $ivy.`com.jfinal:jfinal:4.1`
import com.jfinal.template.Engine
import os.copy.over
import coursier.maven.MavenRepository

import scala.collection.JavaConverters._
import scala.util.Try

object CustomZincWorkerModule extends ZincWorkerModule {
  def repositories() = super.repositories ++ Seq(
MavenRepository("https://dl.bintray.com/johnreed2/maven/")
)
}


trait PackModule extends mill.Module with JavaModule {
  def packMain:Map[String,String]
  def packExtraClasspath:Map[String,Seq[String]]
  def name:String = "defaultName"
  def version:String = "defaultVersion"
  def gitRevision: String = Try {
    if(os.exists(os.pwd/".git")) {
      sys.process.Process("git rev-parse HEAD").!!
    }
    else {
      "unknown"
    }
  }.getOrElse("unknown").trim



  def pack = T {
    val dest = T.ctx().dest
    val libDir = dest / 'lib
    val binDir = dest / 'bin

    mkdir(libDir)
    mkdir(binDir)
    rm(libDir/'*)
    rm(binDir/'*)

    val allJars = packageSelfModules() ++ runClasspath()
        .map(_.path)
        .filter(path => exists(path) && !path.isDir)
        .toSeq

    allJars.foreach(cp.into(_,libDir))

    var engine = Engine.use(Engine.MAIN_ENGINE_NAME)
    packMain
      .map(x=>(x,collection.mutable.Map[String,String](
    "MAIN_CLASS" -> x._2,
    "PROG_NAME" -> x._1,
    "PROG_VERSION" -> version,
    "PROG_REVERSION" -> gitRevision,
    "JVM_OPTS" -> forkArgs().mkString(" "),
    "EXTRA_CLASSPATH" -> packExtraClasspath.getOrElse(x._1,Seq()).mkString("", "${PSEP}","${PSEP}"),
    "MAC_ICON_FILE" -> "testIcon.ico"
      )))
      .foreach(x=>{
        engine.getTemplateByString(os.read(os.pwd/'template/"launcher.txt")).render(x._2.asJava,(binDir/x._1._1).toIO)
      })

    packMain
      .map(x=>(x,collection.mutable.Map[String,String](
        "MAIN_CLASS" -> x._2,
        "PROG_NAME" -> x._1,
        "PROG_VERSION" -> version,
        "PROG_REVERSION" -> gitRevision,
        "JVM_OPTS" -> forkArgs().mkString(" "),
        "EXTRA_CLASSPATH" -> packExtraClasspath.getOrElse(x._1,Seq()).mkString("", "%PSEP%","%PSEP%").replaceAll("""\$\{PROG_HOME\}""", "%PROG_HOME%"),
        "MAC_ICON_FILE" -> "testIcon.ico"
      )))
      .foreach(x=>{
        engine.getTemplateByString(os.read(os.pwd/'template/"launcherBat.txt")).render(x._2.asJava,(binDir/(x._1._1+".bat")).toIO)
      })
/*
    val runnerFile = Jvm.createLauncher(
      finalMainClass(),
      Agg.from(ls(libDir)),
      forkArgs()
    )

    mv.into(runnerFile.path, binDir)
*/
    PathRef(dest)
  }

  def packageSelfModules = T {
    Task.traverse(moduleDeps :+ this) { module =>
      module.jar
        .zip(module.artifactId)
        .map {
          case (jar, name) =>
            val namedJar = jar.path / up / s"${name}.jar"

            over(jar.path, namedJar)
            namedJar
        }
    }
  }

}


object engine extends SbtModule{
  def zincWorker= CustomZincWorkerModule
  def scalaVersion = "2.12.10"

  def ivyDeps = Agg(
    ivy"org.slf4j:slf4j-api:1.7.29",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.2",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.typesafe:config:1.4.0",
    ivy"com.lihaoyi::scalatags:0.7.0",
    ivy"com.github.pathikrit::better-files:3.8.0",
    ivy"org.plotly-scala::plotly-render:0.7.2"
)
}
