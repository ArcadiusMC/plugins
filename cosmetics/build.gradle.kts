
dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":menus"))
  compileOnly(project(":waypoints"))
}

pluginYml {
  prefixedName("Cosmetics")
  main = "net.arcadiusmc.cosmetics.CosmeticsPlugin"

  depends {
    optional("project:waypoints")
  }
}