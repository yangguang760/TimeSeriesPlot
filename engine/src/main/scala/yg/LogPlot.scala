package yg

import java.io.{File => JFile}
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import better.files.File

import scala.collection.JavaConverters._
import com.typesafe.config.ConfigFactory
import plotly.{Bar, Plotly, Scatter}
import plotly.element.AxisReference.{Y1, Y2, Y3, Y4}
import plotly.element.{Marker, ScatterMode}
import plotly.element.Symbol._
import plotly.layout.{Axis, BarMode, Layout}
import scalatags.Text.all._

import scala.collection.immutable.Queue

object LogPlot {
  def main(args: Array[String]): Unit = {

    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    val sdf = new SimpleDateFormat("yyyyMMddHHmmss")
    var indiMap: Map[String, (String, String)] = Map()
    var scatterMap: Map[(String, String), Queue[(String, Double, String)]] = Map()
    val plotConfig = ConfigFactory.load("plot")
    val file = plotConfig.getString("fileName")
    val indicators = plotConfig.getStringList("indicators").asScala
    indicators.foreach(x => {
      val iconf = plotConfig.getConfig(x)
      indiMap += x -> (iconf.getString("YType"), iconf.getString("chartType"))
    })

    val mdss = File("./" + file)
    if (mdss.exists) {
      mdss
        .lines
        .foreach(x => {
          val s = x.split(",")
          val datestr = format.format(new Date(s(0).toLong))
          scatterMap += (s(1), s(2)) -> (scatterMap.getOrElse((s(1), s(2)), Queue[(String, Double, String)]()) :+ (datestr, s(3).toDouble, if (s.length > 4) {
            s(4)
          } else {
            ""
          }))
        })
    } else {
      throw new Exception("file not found.")
    }


    def getMode(x: String) = {
      if (x.startsWith("mark")) {
        ScatterMode(ScatterMode.Markers)
      }
      else {
        ScatterMode(ScatterMode.Lines)
      }
    }

    def getSymbol(x: String): Marker = {
      val sp = x.split("-")
      if (sp.length > 2) {
        val symbol = sp(2) match {
          case "O" => Circle()
          case "X" => Cross()
          case "D" => Diamond()
          case "S" => Square()
        }
        (sp(1).toDouble, symbol)
        Marker(size = sp(1).toInt, symbol = symbol)
      } else {
        Marker(size = 1, symbol = Circle())
      }
    }

    def getY(x: String) = x match {
      case "1" => Y1
      case "2" => Y2
      case "3" => Y3
      case _ => Y4
    }

    val L1scatterMaps = scatterMap.groupBy(x => x._1._1.split("@")(0))
    val L2scatterMaps: Map[String, Map[(String, String), Queue[(String, Double, String)]]] = scatterMap.groupBy(x => x._1._1.split("@")(1))
    val L3scatterMaps = scatterMap.groupBy(x => x._1._1.split("@")(2))
    val AllLscatterMaps = L1scatterMaps ++ L2scatterMaps ++ L3scatterMaps

    def genPlot(name: String, scatterMap: Map[(String, String), Queue[(String, Double, String)]]): Unit = {
      val fscatterMap = scatterMap.filterKeys(x => indiMap.keySet.contains(x._2))
      val finalMap = fscatterMap.map(y => {
        val x = indiMap.get(y._1._2).get
        if (x._2.startsWith("bar")) {
          Bar(
            y._2.map(_._1),
            y._2.map(_._2),
            y._1.toString,
            y._2.map(_._3),
            yaxis = getY(x._1)
          )
        } else {
          Scatter(
            y._2.map(_._1),
            y._2.map(_._2),
            y._2.map(_._3),
            getMode(x._2),
            marker = getSymbol(x._2),
            name = y._1.toString,
            yaxis = getY(x._1),
          )
        }
      }).toSeq

      val dataSnippet = Plotly.jsSnippet(
        "chart",
        finalMap,
        Layout(title = "BTPlot", width = 1280, height = 800, yaxis1 = Axis(domain = (0, 0.25)), yaxis2 = Axis(domain = (0.26, 0.5)), yaxis3 = Axis(domain = (0.51, 0.75)), yaxis4 = Axis(domain = (0.76, 1))))
      val plotlyVersion = "1.12.0"
      val outputPage = html(
        head(
          meta(httpEquiv := "Content-Type", content := "text/html; charset=utf-8"),
          title := "Backtest Rport",
          script(raw(Plotly.plotlyMinJs))
        ),
        body(
          p(s"Run Date: ${new Date}"),
          p(s"${name}"),
          div(id := "chart"),
          script(raw(dataSnippet))
        )
      )

      if (!new JFile("./report").exists()) {
        new JFile("./report").mkdir()
      }
      val filePath = "./report/" + name + "-" + sdf.format(new Date) + ".html"
      println(s"start gen file:$filePath")
      new JFile(filePath)
        .createNewFile()
      Files.write(
        new JFile(filePath).toPath,
        outputPage.render.getBytes("UTF-8"))
      println(s"file write done:$filePath")
    }
    AllLscatterMaps.par.foreach(x=>genPlot(x._1,x._2))
  }
}
