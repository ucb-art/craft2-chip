organization := "edu.berkeley.cs"

version := "1.0"

name := "craft2-chip"

scalaVersion := "2.12.7"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "rocket-dsptools" -> "1.2-SNAPSHOT",
  "chisel3" -> "3.1-SNAPSHOT",
  "chisel-iotesters" -> "1.2-SNAPSHOT",
  "fft" -> "1.0",
  "pfb" -> "1.0",
  "builtin-debugger" -> "0",
  "riscv-dma" -> "2.0",
  // barstools is called tapeout for now
  "tapeout" -> "0.1-SNAPSHOT",
  "testchipip" -> "1.0",
  )

libraryDependencies ++= Seq("testchipip", "rocket-dsptools", /*"riscv-dma", "pfb",*/ "fft", "tapeout").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) }

scalacOptions += "-Xsource:2.11"
