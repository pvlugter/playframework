package sbt

import Keys._
import PlayKeys._
import PlayEclipse._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.SbtNativePackager._

trait PlaySettings {
  this: PlayCommands with PlayPositionMapper with PlayRun with PlaySourceGenerators =>

  protected def whichLang(name: String): Seq[Setting[_]] = {
    if (name == JAVA) {
      defaultJavaSettings
    } else if (name == SCALA) {
      defaultScalaSettings
    } else {
      Seq.empty
    }
  }

  lazy val defaultJavaSettings = Seq[Setting[_]](

    templatesImport ++= defaultJavaTemplatesImport,

    routesImport ++= Seq(
      "play.libs.F"
    ),

    ebeanEnabled := true

  )

  lazy val defaultScalaSettings = Seq[Setting[_]](
    templatesImport ++= defaultScalaTemplatesImport
  )

  def closureCompilerSettings(optionCompilerOptions: com.google.javascript.jscomp.CompilerOptions) = Seq[Setting[_]](
    resourceGenerators in Compile <<= JavascriptCompiler(Some(optionCompilerOptions))(Seq(_)),
    resourceGenerators in Compile <+= LessCompiler,
    resourceGenerators in Compile <+= CoffeescriptCompiler
  )

  lazy val defaultSettings = Seq[Setting[_]](

    scalaVersion := play.core.PlayVersion.scalaVersion,

    playPlugin := false,

    resolvers ++= Seq(
      "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"
    ),

    target <<= baseDirectory / "target",

    sourceDirectory in Compile <<= baseDirectory / "app",
    sourceDirectory in Test <<= baseDirectory / "test",

    confDirectory <<= baseDirectory / "conf",

    resourceDirectory in Compile <<= baseDirectory / "conf",

    scalaSource in Compile <<= baseDirectory / "app",
    scalaSource in Test <<= baseDirectory / "test",

    javaSource in Compile <<= baseDirectory / "app",
    javaSource in Test <<= baseDirectory / "test",

    javacOptions in (Compile, doc) := List("-encoding", "utf8"),

    libraryDependencies <+= (playPlugin) { isPlugin =>
      val d = "com.typesafe.play" %% "play" % play.core.PlayVersion.current
      if (isPlugin)
        d % "provided"
      else
        d
    },

    libraryDependencies += "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % "test",

    parallelExecution in Test := false,

    fork in Test := true,

    testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "sequential", "true", "junitxml", "console"),

    testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "--ignore-runners=org.specs2.runner.JUnitRunner"),

    testListeners <<= (target, streams).map((t, s) => Seq(new eu.henkelmann.sbt.JUnitXmlTestsListener(t.getAbsolutePath, s.log))),

    testResultReporter <<= testResultReporterTask,

    testResultReporterReset <<= testResultReporterResetTask,

    generateReverseRouter := true,

    namespaceReverseRouter := false,

    sourceGenerators in Compile <+= (state, confDirectory, sourceManaged in Compile, routesImport, generateReverseRouter, namespaceReverseRouter) map RouteFiles,

    // Adds config directory's source files to continuous hot reloading
    watchSources <+= confDirectory map { all => all },

    sourceGenerators in Compile <+= (state, sourceDirectory in Compile, sourceManaged in Compile, templatesTypes, templatesImport) map ScalaTemplates,

    // Adds app directory's source files to continuous hot reloading
    watchSources <++= baseDirectory map { path => ((path / "app") ** "*" --- (path / "app/assets") ** "*").get },

    commands ++= Seq(shCommand, playCommand, playStartCommand, h2Command, classpathCommand, licenseCommand, computeDependenciesCommand),

    run <<= playRunSetting,

    shellPrompt := playPrompt,

    copyResources in Compile <<= (copyResources in Compile, playCopyAssets) map { (r, pr) => r ++ pr },

    mainClass in (Compile, run) := Some("play.core.server.NettyServer"),

    compile in (Compile) <<= PostCompile(scope = Compile),

    computeDependencies <<= computeDependenciesTask,

    playVersion := play.core.PlayVersion.current,

    playCommonClassloader <<= playCommonClassloaderTask,

    playClassLoaderCreator := createURLClassLoader,

    playCopyAssets <<= playCopyAssetsTask,

    playCompileEverything <<= playCompileEverythingTask,

    playReload <<= playReloadTask,

    logManager <<= extraLoggers(PlayLogManager.default(playPositionMapper)),

    ivyLoggingLevel := UpdateLogging.DownloadOnly,

    routesImport := Seq.empty[String],

    playMonitoredFiles <<= playMonitoredFilesTask,

    playDefaultPort := 9000,

    // Default hooks    
    
    playOnStarted := Nil,

    playOnStopped := Nil,
    
    playRunHooks <++= playOnStarted map { funcs =>
       funcs map play.PlayRunHook.makeRunHookFromOnStarted  
    },
    
    playRunHooks <++= playOnStopped map { funcs =>
       funcs map play.PlayRunHook.makeRunHookFromOnStopped
    },
    
    // Assets

    playAssetsDirectories := Seq.empty[File],

    playExternalAssets := Seq.empty[(File, File => PathFinder, String)],

    playAssetsDirectories <+= baseDirectory / "public",

    requireJs := Nil,

    requireJsFolder := "",

    requireJsShim := "",

    requireNativePath := None,

    buildRequire <<= buildRequireTask,

    packageBin in Compile <<= (packageBin in Compile).dependsOn(buildRequire),

    resourceGenerators in Compile <+= LessCompiler,
    resourceGenerators in Compile <+= CoffeescriptCompiler,
    resourceGenerators in Compile <+= JavascriptCompiler(fullCompilerOptions = None),

    lessEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.less") --- base / "assets" ** "_*")),
    coffeescriptEntryPoints <<= (sourceDirectory in Compile)(base => base / "assets" ** "*.coffee"),
    javascriptEntryPoints <<= (sourceDirectory in Compile)(base => ((base / "assets" ** "*.js") --- (base / "assets" ** "_*"))),

    lessOptions := Seq.empty[String],
    coffeescriptOptions := Seq.empty[String],
    closureCompilerOptions := Seq.empty[String],

    // Settings

    devSettings := Nil,

    // Templates

    templatesImport := defaultTemplatesImport,

    scalaIdePlay2Prefs <<= (state, thisProjectRef, baseDirectory) map { (s, r, baseDir) => saveScalaIdePlay2Prefs(r, Project structure s, baseDir) },

    templatesTypes := Map(
      "html" -> "play.api.templates.HtmlFormat",
      "txt" -> "play.api.templates.TxtFormat",
      "xml" -> "play.api.templates.XmlFormat",
      "js" -> "play.api.templates.JavaScriptFormat"
    ),

    // Native packaging

    sourceDirectory in Universal <<= baseDirectory(_ / "dist"),

    mainClass in Compile := Some("play.core.server.NettyServer"),

    mappings in Universal <++= (confDirectory) map {
      confDirectory: File =>
        val confDirectoryLen = confDirectory.getCanonicalPath.length
        val pathFinder = confDirectory ** ("*" -- "routes")
        pathFinder.get map {
          confFile: File =>
            confFile -> ("conf/" + confFile.getCanonicalPath.substring(confDirectoryLen))
        }
    },

    mappings in Universal <++= (doc in Compile) map {
      docDirectory: File =>
        val docDirectoryLen = docDirectory.getCanonicalPath.length
        val pathFinder = docDirectory ** "*"
        pathFinder.get map {
          docFile: File =>
            docFile -> ("share/doc/api/" + docFile.getCanonicalPath.substring(docDirectoryLen))
        }
    },

    mappings in Universal <++= (baseDirectory) map {
      baseDirectory: File =>
        val pathFinder = baseDirectory * "README*"
        pathFinder.get map {
          readmeFile: File =>
            readmeFile -> readmeFile.getName
        }
    }

  )

}
