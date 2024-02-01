plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":scripting"))
  //compileOnly(project(":usables"))
}

pluginYml {
  name = "Dialogues"
  main = "net.arcadiusmc.dialogues.DialoguesPlugin"
}