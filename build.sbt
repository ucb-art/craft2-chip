organization := "edu.berkeley.cs"

version := "1.0"

name := "craft2-chip"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
val defaultVersions = Map(
  "rocket-dsp-utils" -> "1.0",
  "chisel3" -> "3.1-SNAPSHOT",
  "chisel-iotesters" -> "1.2-SNAPSHOT",
  "fft" -> "1.0",
  "pfb" -> "1.0",
  "tuner" -> "1.0",
  "filter" -> "1.0",
  "builtin-debugger" -> "0",
  "riscv-dma" -> "2.0"
  )

libraryDependencies ++= Seq("rocket-dsp-utils", "chisel3", "chisel-iotesters", "builtin-debugger", "pfb", "riscv-dma", "fft", "tuner", "filter").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) }

