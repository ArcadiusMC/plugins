plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":discord"))
}

pluginYml {
  name = "StaffChat"
  main = "net.arcadiusmc.staffhcat.StaffChatPlugin"
}