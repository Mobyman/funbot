import play.twirl.sbt.Import.TwirlKeys

name := "funbotplay"

version := "1.0"

lazy val `funbotplay` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(jdbc, cache, ws, specs2 % Test)

libraryDependencies ++= Seq("info.mukel" %% "telegrambot4s" % "1.0.3-SNAPSHOT")

libraryDependencies ++= Seq("net.cloudinsights" %% "play-plugins-salat" % "1.5.9")

libraryDependencies ++= Seq("org.scalaj" %% "scalaj-http" % "2.2.1")

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "0.1.2"

libraryDependencies += "com.enragedginger" %% "akka-quartz-scheduler" % "1.5.0-akka-2.4.x"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesImport += "se.radley.plugin.salat.Binders._"

TwirlKeys.templateImports += "org.bson.types.ObjectId"
