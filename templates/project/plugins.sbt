resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.5")

libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.4.0-M1"
